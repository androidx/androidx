/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.effects.playservices.lowlight

import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.core.processing.OpenGlRenderer

/**
 * Manages the dual-bridge OpenGL rendering pipeline and the downstream [SurfaceOutput] of the Low
 * Light Boost surface processor.
 *
 * The pipeline consists of two [OpenGlBridge] stages sharing the GL thread of `glHandler`:
 * 1. [inputBridge] (`LlbInputBridge`): Receives raw camera frames from the upstream
 *    [SurfaceRequest]. When Low Light Boost is active and a valid Low Light Boost session exists,
 *    it renders camera frames into the session's camera surface. In passthrough mode, and while the
 *    session warms up until its first boosted frame, it also renders them directly to the
 *    downstream [SurfaceOutput].
 * 2. [outputBridge] (`LlbOutputBridge`): Provides a [SessionTargetSurface] that receives boosted
 *    frames from the Low Light Boost session and renders them to the downstream [SurfaceOutput].
 *
 * The effect outputs one Surface for all its targets (see
 * [androidx.camera.core.CameraEffect.OUTPUT_OPTION_ONE_FOR_ALL_TARGETS]), so there is a single
 * PRIVATE-format output. It is registered on the bridge that rendered the latest frame.
 *
 * All methods must be called on the GL thread.
 */
internal class LowLightBoostRenderPipeline(
    glHandler: Handler,
    rendererFactory: () -> OpenGlRenderer,
    onInputFrameAvailable: (timestampNs: Long, textureTransform: FloatArray) -> Unit,
    onOutputFrameAvailable: (timestampNs: Long, textureTransform: FloatArray) -> Unit,
) {
    internal val inputBridge =
        OpenGlBridge<Surface>(
            name = "LlbInputBridge",
            glHandler = glHandler,
            recreateOnResolutionChange = true,
            surfaceFactory = { Surface(it) },
            rendererFactory = rendererFactory,
            onFrameAvailable = onInputFrameAvailable,
        )

    internal val outputBridge =
        OpenGlBridge(
            name = "LlbOutputBridge",
            glHandler = glHandler,
            recreateOnResolutionChange = false,
            surfaceFactory = { SessionTargetSurface(it) },
            rendererFactory = rendererFactory,
            onFrameAvailable = onOutputFrameAvailable,
            isUsable = { !it.isTerminated },
            onDetached = { it.markTerminated() },
        )

    // Pre-allocated 4x4 transformation matrix scratch buffers.
    private val passthroughOutputMatrix = FloatArray(16)
    private val boostedOutputMatrix = FloatArray(16)

    private var output: Pair<SurfaceOutput, Surface>? = null
    private var registeredCamSurface: Surface? = null

    /** Which bridge the output is registered on. */
    private enum class OutputRoutingTarget {
        NONE,
        PASSTHROUGH,
        BOOSTED,
    }

    private var currentOutputRouting = OutputRoutingTarget.NONE

    /** Returns true if no downstream [SurfaceOutput] is registered. */
    internal fun isOutputSurfacesEmpty(): Boolean = output == null

    /**
     * Registers the downstream [SurfaceOutput] and its [Surface], replacing the previous output, if
     * any.
     *
     * @return false if [surfaceOutput] isn't a PRIVATE-format output, which the OpenGL pipeline
     *   can't render to.
     */
    @WorkerThread
    internal fun registerOutputSurface(surfaceOutput: SurfaceOutput, surface: Surface): Boolean {
        if (surfaceOutput.format != INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) {
            Logger.w(TAG, "Ignoring non-PRIVATE SurfaceOutput format (${surfaceOutput.format})")
            return false
        }
        output?.let { (_, oldSurface) -> unregisterFromBridges(oldSurface) }
        output = surfaceOutput to surface
        currentOutputRouting = OutputRoutingTarget.NONE
        return true
    }

    /**
     * Removes [surfaceOutput] if it is the registered output, and unregisters its [Surface] from
     * both OpenGL bridges.
     *
     * @return The removed [Surface], or null if [surfaceOutput] was not registered.
     */
    @WorkerThread
    internal fun unregisterOutputSurface(surfaceOutput: SurfaceOutput): Surface? {
        val (registeredOutput, surface) = output ?: return null
        if (registeredOutput !== surfaceOutput) {
            return null
        }
        output = null
        currentOutputRouting = OutputRoutingTarget.NONE
        unregisterFromBridges(surface)
        return surface
    }

    private fun unregisterFromBridges(surface: Surface) {
        outputBridge.unregisterOutputSurface(surface)
        inputBridge.unregisterOutputSurface(surface)
    }

    /**
     * Returns true if [camSurface] is already bound as the output of [inputBridge] and registered
     * on its renderer.
     */
    @WorkerThread
    internal fun isSessionCameraSurfaceBound(camSurface: Surface): Boolean =
        registeredCamSurface === camSurface && inputBridge.isOutputRegistered(camSurface)

    /**
     * Registers the Low Light Boost session's camera surface as an output of [inputBridge],
     * unregistering the previous camera surface if it changed.
     */
    @WorkerThread
    internal fun bindSessionCameraSurfaceToInputBridge(camSurface: Surface) {
        val oldCamSurface = registeredCamSurface
        registeredCamSurface = camSurface
        if (oldCamSurface !== camSurface && oldCamSurface != null) {
            inputBridge.unregisterOutputSurface(oldCamSurface)
        }
        inputBridge.registerOutputSurface(camSurface)
    }

    /** Moves the output from [inputBridge] to [outputBridge] to render boosted frames. */
    @WorkerThread
    private fun routeOutputToBoostedRenderer(surface: Surface) {
        inputBridge.unregisterOutputSurface(surface)
        outputBridge.registerOutputSurface(surface)
        currentOutputRouting = OutputRoutingTarget.BOOSTED
    }

    /** Moves the output from [outputBridge] to [inputBridge] to render passthrough frames. */
    @WorkerThread
    private fun routeOutputToPassthroughRenderer(surface: Surface) {
        outputBridge.unregisterOutputSurface(surface)
        inputBridge.registerOutputSurface(surface)
        currentOutputRouting = OutputRoutingTarget.PASSTHROUGH
    }

    /**
     * Ensures [outputBridge] is initialized, creating new resources if [forceNewIntermediate] is
     * true or the existing ones are no longer usable. A [resolution] change alone doesn't recreate
     * them; it only updates the default buffer size of the bridge's input
     * [android.graphics.SurfaceTexture].
     *
     * @return true if [outputBridge] has a usable input surface.
     */
    @WorkerThread
    internal fun ensureBoostedGlInitialized(
        resolution: Size,
        forceNewIntermediate: Boolean = false,
    ): Boolean {
        outputBridge.ensureInitialized(
            resolution,
            forceRecreate = forceNewIntermediate,
            onResourcesDetached = { detached ->
                // The output was registered on the released renderer, if any, even if creating
                // the new resources fails. The next boosted frame registers it on the new one.
                if (currentOutputRouting == OutputRoutingTarget.BOOSTED) {
                    currentOutputRouting = OutputRoutingTarget.NONE
                }
                detached.release()
            },
        )
        return outputBridge.isValid()
    }

    /**
     * Ensures [inputBridge] is initialized for [resolution], creating new resources if
     * [forceNewInput] is true. If a new [OpenGlRenderer] was created, re-binds [activeCamSurface]
     * if it is valid.
     *
     * @param onOldResourcesDetached Receives the previous resources of [inputBridge] when they are
     *   replaced. Releases them by default. See [OpenGlBridge.ensureInitialized].
     */
    @WorkerThread
    internal fun ensureInputGlInitialized(
        resolution: Size,
        activeCamSurface: Surface? = null,
        forceNewInput: Boolean = false,
        onOldResourcesDetached: (OpenGlBridge.DetachedResources) -> Unit = { it.release() },
    ) {
        val recreated =
            inputBridge.ensureInitialized(
                resolution,
                forceRecreate = forceNewInput,
                onResourcesDetached = { detached ->
                    // Nothing is registered on the input renderer anymore, even if creating the
                    // new resources fails.
                    registeredCamSurface = null
                    if (currentOutputRouting == OutputRoutingTarget.PASSTHROUGH) {
                        currentOutputRouting = OutputRoutingTarget.NONE
                    }
                    onOldResourcesDetached(detached)
                },
            )
        if (recreated && activeCamSurface != null && activeCamSurface.isValid) {
            bindSessionCameraSurfaceToInputBridge(activeCamSurface)
        }
    }

    /**
     * Unregisters the session camera surface from [inputBridge], moves the output back to
     * passthrough rendering, and detaches the [outputBridge] resources. Called when tearing down
     * the Low Light Boost session while keeping [inputBridge] alive.
     */
    @WorkerThread
    internal fun detachBoostedSessionResources(
        fallbackCamSurface: Surface?
    ): OpenGlBridge.DetachedResources {
        val camSurfaceToUnregister = registeredCamSurface ?: fallbackCamSurface
        registeredCamSurface = null
        if (camSurfaceToUnregister != null) {
            inputBridge.unregisterOutputSurface(camSurfaceToUnregister)
        }
        // Move the output to the passthrough renderer before detaching the boosted renderer, so
        // its EGL surface is released from the boosted renderer first. Otherwise, the output
        // can't be connected to the passthrough renderer until the boosted renderer is released.
        output?.let { (_, surface) -> routeOutputToPassthroughRenderer(surface) }
        return outputBridge.detachResources()
    }

    /** Renders a boosted frame from [outputBridge] to the downstream [SurfaceOutput]. */
    @WorkerThread
    internal fun renderBoostedFrameToOutputs(timestampNs: Long, textureTransform: FloatArray) {
        val (surfaceOutput, surface) = output ?: return
        if (currentOutputRouting != OutputRoutingTarget.BOOSTED) {
            routeOutputToBoostedRenderer(surface)
        }
        try {
            surfaceOutput.updateTransformMatrix(boostedOutputMatrix, textureTransform)
            outputBridge.render(timestampNs, boostedOutputMatrix, surface)
        } catch (e: RuntimeException) {
            Logger.w(TAG, "Failed to render boosted frame to the output surface", e)
        }
    }

    /** Renders a passthrough frame from [inputBridge] to the downstream [SurfaceOutput]. */
    @WorkerThread
    internal fun renderPassthroughFrameToOutputs(timestampNs: Long, textureTransform: FloatArray) {
        val (surfaceOutput, surface) = output ?: return
        if (currentOutputRouting != OutputRoutingTarget.PASSTHROUGH) {
            routeOutputToPassthroughRenderer(surface)
        }
        try {
            surfaceOutput.updateTransformMatrix(passthroughOutputMatrix, textureTransform)
            inputBridge.render(timestampNs, passthroughOutputMatrix, surface)
        } catch (e: RuntimeException) {
            Logger.w(TAG, "Failed to render passthrough frame to the output surface", e)
        }
    }

    /** Releases [inputBridge] and [outputBridge]. The output stays registered. */
    @WorkerThread
    internal fun releaseBridges() {
        registeredCamSurface = null
        currentOutputRouting = OutputRoutingTarget.NONE
        outputBridge.release()
        inputBridge.release()
    }

    private companion object {
        private const val TAG = "LlbRenderPipeline"
    }
}
