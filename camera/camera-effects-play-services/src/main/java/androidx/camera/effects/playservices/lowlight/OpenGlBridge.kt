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

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.os.Handler
import android.util.Size
import android.view.Surface
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.processing.OpenGlRenderer

/**
 * An [OpenGlRenderer] subclass for multiple [OpenGlRenderer] instances sharing a single GL thread.
 *
 * It restores its own EGL context before unregistering an output surface or releasing, and resets
 * the current output surface when rendering after another renderer was active. [OpenGlBridge] and
 * [OpenGlBridge.DetachedResources] call [ensureContextCurrent] before updating the texture of their
 * input [SurfaceTexture].
 */
@WorkerThread
internal class ContextAwareOpenGlRenderer : OpenGlRenderer() {
    /**
     * Restores this renderer's EGL context onto the calling thread using its internal pbuffer
     * surface (`mTempSurface`) when another [OpenGlRenderer] on the same thread was most recently
     * active.
     */
    @WorkerThread
    internal fun ensureContextCurrent() {
        // mInitialized, mEglDisplay, mEglContext, mTempSurface, mCurrentSurface, and
        // mOutputSurfaceMap are protected members inherited from OpenGlRenderer in camera-core.
        if (
            mInitialized.get() &&
                mEglDisplay != EGL14.EGL_NO_DISPLAY &&
                mEglContext != EGL14.EGL_NO_CONTEXT &&
                mTempSurface != EGL14.EGL_NO_SURFACE &&
                EGL14.eglGetCurrentContext() != mEglContext
        ) {
            // Throws if the context can't be made current.
            makeCurrent(mTempSurface)
            // Clearing mCurrentSurface only forces OpenGlRenderer.render() to call makeCurrent
            // with the already-cached EGLSurface from mOutputSurfaceMap; it does not recreate or
            // destroy any EGLSurface.
            mCurrentSurface = null
        }
    }

    @WorkerThread
    override fun unregisterOutputSurface(surface: Surface) {
        if (!mInitialized.get() || !mOutputSurfaceMap.containsKey(surface)) {
            return
        }
        try {
            ensureContextCurrent()
        } catch (e: IllegalStateException) {
            // Still destroy the EGL surface below, so the output can be connected elsewhere.
            // Another context is current, so this renderer's EGL surface isn't. Clearing
            // mCurrentSurface keeps the base class from trying to make the context current again,
            // which would throw before the EGL surface is destroyed.
            Logger.w(TAG, "Failed to make the EGL context current to unregister a surface", e)
            mCurrentSurface = null
        }
        super.unregisterOutputSurface(surface)
    }

    @WorkerThread
    override fun render(timestampNs: Long, textureTransform: FloatArray, surface: Surface) {
        // Another renderer may have made its context current since this renderer's last use, e.g.
        // when an output moves between bridges or a detached input is drained. Clearing
        // mCurrentSurface makes the base OpenGlRenderer.render() make this renderer's output
        // EGLSurface current again.
        if (EGL14.eglGetCurrentContext() != mEglContext) {
            mCurrentSurface = null
        }
        super.render(timestampNs, textureTransform, surface)
    }

    @WorkerThread
    override fun release() {
        try {
            ensureContextCurrent()
        } catch (e: IllegalStateException) {
            Logger.w(TAG, "Failed to make the EGL context current to release", e)
            // Unbind the context of another renderer, if any, so the GL deletes of the base class
            // can't affect it. They become no-ops, and the EGL context and surfaces of this
            // renderer are still destroyed below.
            if (mEglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    mEglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
            }
            mCurrentSurface = null
        } finally {
            super.release()
        }
    }

    private companion object {
        private const val TAG = "ContextAwareGlRenderer"
    }
}

/**
 * Encapsulates an OpenGL texture-to-surface rendering bridge consisting of an [OpenGlRenderer], an
 * input [SurfaceTexture], its producer [Surface], and the set of output [Surface]s currently
 * registered on the renderer's EGL context.
 *
 * All methods must be called on the GL thread of [glHandler], where frame callbacks are also
 * delivered.
 *
 * @param isUsable Returns false if an input surface can no longer be used and must be recreated.
 * @param onDetached Called when an input surface is detached from this bridge.
 */
internal class OpenGlBridge<S : Surface>(
    private val name: String,
    private val glHandler: Handler,
    private val recreateOnResolutionChange: Boolean,
    private val surfaceFactory: (SurfaceTexture) -> S,
    private val rendererFactory: () -> OpenGlRenderer,
    private val onFrameAvailable: (timestampNs: Long, textureTransform: FloatArray) -> Unit,
    private val isUsable: (S) -> Boolean = { true },
    private val onDetached: (S) -> Unit = {},
) {
    private var renderer: OpenGlRenderer? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var resolution: Size? = null
    private val registeredOutputs = mutableSetOf<Surface>()

    // The detached resources of input SurfaceTextures that aren't released yet.
    private val detachedResourcesByTexture = mutableMapOf<SurfaceTexture, DetachedResources>()
    private val textureTransform = FloatArray(16)
    private var updateTexImageFailureCount = 0

    @VisibleForTesting
    internal val glRenderer: OpenGlRenderer?
        get() = renderer

    /** The producer [Surface] of the input [SurfaceTexture], or null if not initialized. */
    internal var inputSurface: S? = null
        private set

    internal fun isOutputRegistered(surface: Surface): Boolean = registeredOutputs.contains(surface)

    internal fun isValid(): Boolean {
        val surface = inputSurface
        return renderer != null && surface != null && surface.isValid && isUsable(surface)
    }

    private val frameAvailableListener =
        SurfaceTexture.OnFrameAvailableListener { availableTexture ->
            if (availableTexture !== surfaceTexture) {
                // A frame of a detached texture. Hand it to the detached resources, which consume
                // it if they are draining.
                detachedResourcesByTexture[availableTexture]?.onFrameAvailableAfterDetach(
                    availableTexture
                )
                return@OnFrameAvailableListener
            }
            val activeRenderer = renderer ?: return@OnFrameAvailableListener
            try {
                (activeRenderer as? ContextAwareOpenGlRenderer)?.ensureContextCurrent()
                availableTexture.updateTexImage()
                updateTexImageFailureCount = 0
            } catch (e: RuntimeException) {
                // Log the first failure and then every 100th one, to avoid a log per frame.
                if (updateTexImageFailureCount++ % 100 == 0) {
                    Logger.w(
                        TAG,
                        "[$name] Failed to updateTexImage; skipping frame " +
                            "(failures=$updateTexImageFailureCount)",
                        e,
                    )
                }
                return@OnFrameAvailableListener
            }
            availableTexture.getTransformMatrix(textureTransform)
            onFrameAvailable(availableTexture.timestamp, textureTransform)
        }

    /**
     * Ensures the [OpenGlRenderer], [SurfaceTexture], and producer [Surface] are initialized and
     * valid. If [forceRecreate] is true or the underlying surface is no longer usable (or if
     * [recreateOnResolutionChange] is true and [targetResolution] changed), any existing resources
     * are detached and passed to `onResourcesDetached`, then replaced with fresh instances.
     *
     * If the existing resources are kept and [targetResolution] is given, only the default buffer
     * size of the input [SurfaceTexture] is updated to it.
     *
     * If the initialization fails, the bridge is left without an [inputSurface].
     *
     * @param onResourcesDetached Receives the existing resources when they are replaced, before the
     *   new ones are created. Releases them by default. A caller whose [inputSurface] may still be
     *   used by a producer can keep draining them (see [DetachedResources.drainUntilReleased]) and
     *   release them later.
     * @return true if new resources were created, false if the existing ones are kept or the
     *   initialization failed.
     */
    @WorkerThread
    internal fun ensureInitialized(
        targetResolution: Size? = null,
        forceRecreate: Boolean = false,
        onResourcesDetached: (DetachedResources) -> Unit = { it.release() },
    ): Boolean {
        val resolutionChanged =
            recreateOnResolutionChange && targetResolution != null && resolution != targetResolution
        if (!isValid() || resolutionChanged || forceRecreate) {
            onResourcesDetached(detachResources())
            var newRenderer: OpenGlRenderer? = null
            var newTexture: SurfaceTexture? = null
            var newSurface: S? = null
            try {
                // Assign before init() so the renderer is released if a later step fails.
                // OpenGlRenderer.init() cleans up after its own failures.
                val createdRenderer = rendererFactory()
                newRenderer = createdRenderer
                createdRenderer.init(DynamicRange.SDR)
                newTexture =
                    SurfaceTexture(createdRenderer.textureName).apply {
                        if (targetResolution != null) {
                            setDefaultBufferSize(targetResolution.width, targetResolution.height)
                        }
                        setOnFrameAvailableListener(frameAvailableListener, glHandler)
                    }
                newSurface = surfaceFactory(newTexture)
                renderer = newRenderer
                surfaceTexture = newTexture
                inputSurface = newSurface
                if (targetResolution != null) {
                    resolution = targetResolution
                }
                return true
            } catch (e: RuntimeException) {
                onInitializationFailed(e, newSurface, newTexture, newRenderer)
            } catch (e: LinkageError) {
                // Native EGL/GLES libraries may be unavailable, e.g. on JVM test hosts.
                onInitializationFailed(e, newSurface, newTexture, newRenderer)
            }
        } else if (targetResolution != null) {
            resolution = targetResolution
            surfaceTexture?.setDefaultBufferSize(targetResolution.width, targetResolution.height)
        }
        return false
    }

    /**
     * Releases the partially initialized resources after an initialization failure. The bridge is
     * left without an input surface so callers can report the failure.
     */
    private fun onInitializationFailed(
        error: Throwable,
        surface: S?,
        texture: SurfaceTexture?,
        renderer: OpenGlRenderer?,
    ) {
        Logger.w(TAG, "[$name] Error initializing OpenGlRenderer", error)
        DetachedResources(name, surface, texture, renderer, emptySet()).release()
    }

    /** Registers [outputSurface] with this bridge's [OpenGlRenderer] if not already registered. */
    @WorkerThread
    internal fun registerOutputSurface(outputSurface: Surface) {
        val activeRenderer = renderer ?: return
        if (!registeredOutputs.add(outputSurface)) {
            return
        }
        try {
            activeRenderer.registerOutputSurface(outputSurface)
        } catch (e: RuntimeException) {
            registeredOutputs.remove(outputSurface)
            Logger.w(TAG, "[$name] Error registering output surface", e)
        }
    }

    /** Unregisters [outputSurface] from this bridge's [OpenGlRenderer] if currently registered. */
    @WorkerThread
    internal fun unregisterOutputSurface(outputSurface: Surface) {
        if (!registeredOutputs.remove(outputSurface)) {
            return
        }
        try {
            renderer?.unregisterOutputSurface(outputSurface)
        } catch (e: RuntimeException) {
            Logger.w(TAG, "[$name] Error unregistering output surface", e)
        }
    }

    /**
     * Renders the current texture frame to [outputSurface] using [transformMatrix], registering
     * [outputSurface] first if needed, e.g. after the renderer was recreated.
     */
    @WorkerThread
    internal fun render(timestampNs: Long, transformMatrix: FloatArray, outputSurface: Surface) {
        val activeRenderer = renderer ?: return
        registerOutputSurface(outputSurface)
        try {
            activeRenderer.render(timestampNs, transformMatrix, outputSurface)
        } catch (e: RuntimeException) {
            Logger.w(TAG, "[$name] Failed to render frame to output surface", e)
        }
    }

    /**
     * Detaches the current producer [Surface], [SurfaceTexture], and [OpenGlRenderer] from this
     * bridge and calls `onDetached` for the producer [Surface]. Frames of the [SurfaceTexture] are
     * no longer rendered by this bridge; they are handed to the [DetachedResources], which consume
     * them only while draining (see [DetachedResources.drainUntilReleased]). Returns a
     * [DetachedResources] handle whose [DetachedResources.release] method releases the underlying
     * native resources.
     */
    @WorkerThread
    internal fun detachResources(): DetachedResources {
        val detached =
            DetachedResources(
                name,
                inputSurface,
                surfaceTexture,
                renderer,
                registeredOutputs.toSet(),
                onReleased = { detachedResourcesByTexture.remove(it) },
            )
        // The frame listener stays installed, so no frame callback is lost before draining
        // starts. DetachedResources.release() clears it.
        surfaceTexture?.let { texture -> detachedResourcesByTexture[texture] = detached }
        inputSurface?.let(onDetached)
        inputSurface = null
        surfaceTexture = null
        renderer = null
        resolution = null
        registeredOutputs.clear()
        return detached
    }

    /** Releases all OpenGL, [SurfaceTexture], and [Surface] resources owned by this bridge. */
    @WorkerThread
    internal fun release() {
        detachResources().release()
    }

    /**
     * OpenGL, [SurfaceTexture], and [Surface] resources detached from an [OpenGlBridge]. Each
     * resource is released at most once.
     *
     * @param outputs The output surfaces that were registered on [renderer].
     * @param onReleased Called with the [SurfaceTexture], if any, when it is released.
     */
    internal class DetachedResources(
        private val bridgeName: String,
        surface: Surface?,
        private var surfaceTexture: SurfaceTexture?,
        private var renderer: OpenGlRenderer?,
        private val outputs: Set<Surface>,
        private val onReleased: (SurfaceTexture) -> Unit = {},
    ) {
        /** The detached producer [Surface], or null if it was released or there was none. */
        internal var surface: Surface? = surface
            private set

        /** The number of frames consumed while draining. */
        @VisibleForTesting
        internal var drainedFrameCount = 0
            private set

        private var isDraining = false

        private var drainFailureCount = 0

        /**
         * Unregisters the output surfaces from the [OpenGlRenderer], so they can be registered on
         * another renderer, and keeps consuming the frames of the [SurfaceTexture] until [release]
         * is called. The frames are discarded. A producer that still uses [surface] isn't blocked
         * waiting for a free buffer meanwhile.
         */
        @WorkerThread
        internal fun drainUntilReleased() {
            val activeRenderer = renderer ?: return
            for (output in outputs) {
                try {
                    activeRenderer.unregisterOutputSurface(output)
                } catch (e: RuntimeException) {
                    Logger.w(TAG, "[$bridgeName] Error unregistering a detached output surface", e)
                }
            }
            // The bridge's frame listener stays installed and hands the frames to
            // onFrameAvailableAfterDetach().
            isDraining = surfaceTexture != null
        }

        /**
         * Called by the bridge for each frame of the detached [texture]. Consumed while draining.
         */
        @WorkerThread
        internal fun onFrameAvailableAfterDetach(texture: SurfaceTexture) {
            if (isDraining) {
                drainFrame(texture)
            }
        }

        private fun drainFrame(availableTexture: SurfaceTexture) {
            val activeRenderer = renderer
            if (availableTexture !== surfaceTexture || activeRenderer == null) {
                return
            }
            try {
                // updateTexImage() needs the renderer's EGL context. Each renderer makes its own
                // context current again before its next use.
                (activeRenderer as? ContextAwareOpenGlRenderer)?.ensureContextCurrent()
                availableTexture.updateTexImage()
                drainedFrameCount++
            } catch (e: RuntimeException) {
                // Log the first failure and then every 100th one, to avoid a log per frame.
                if (drainFailureCount++ % 100 == 0) {
                    Logger.d(
                        TAG,
                        "[$bridgeName] Failed to drain a frame of a detached surface " +
                            "(failures=$drainFailureCount)",
                        e,
                    )
                }
            }
        }

        /** Releases all remaining resources. */
        @WorkerThread
        internal fun release() {
            val surface = surface
            val surfaceTexture = surfaceTexture
            val renderer = renderer
            this.surface = null
            this.surfaceTexture = null
            this.renderer = null
            isDraining = false
            surfaceTexture?.let(onReleased)
            try {
                surfaceTexture?.setOnFrameAvailableListener(null)
            } catch (e: RuntimeException) {
                Logger.d(TAG, "[$bridgeName] Ignored exception clearing the frame listener", e)
            }
            try {
                surface?.release()
            } catch (e: RuntimeException) {
                Logger.d(
                    TAG,
                    "[$bridgeName] Ignored ${e.javaClass.simpleName} releasing surface",
                    e,
                )
            }
            try {
                surfaceTexture?.release()
            } catch (e: RuntimeException) {
                Logger.d(
                    TAG,
                    "[$bridgeName] Ignored ${e.javaClass.simpleName} releasing surfaceTexture",
                    e,
                )
            }
            try {
                renderer?.release()
            } catch (e: RuntimeException) {
                Logger.w(TAG, "[$bridgeName] Error releasing OpenGlRenderer", e)
            }
        }
    }

    private companion object {
        private const val TAG = "OpenGlBridge"
    }
}
