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

import android.util.Size
import android.view.Surface
import androidx.annotation.WorkerThread
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceRequest
import java.util.concurrent.Executor

/**
 * Provides a new camera input [Surface] to each [SurfaceRequest] through the input bridge of
 * [renderPipeline].
 *
 * A Surface must not be reused by a later SurfaceRequest, so each request gets new input OpenGL
 * resources, even for the same resolution. The replaced resources keep being drained until the
 * SurfaceRequest they were provided to completes, or the pipeline is torn down.
 *
 * **Threading:** used on the GL thread of [glExecutor] only, where SurfaceRequest results are also
 * delivered.
 */
@WorkerThread
internal class InputSurfaceProvider(
    private val renderPipeline: LowLightBoostRenderPipeline,
    private val glExecutor: Executor,
) {
    // The input Surfaces whose SurfaceRequests haven't completed yet.
    private val inputSurfacesInUse = mutableSetOf<Surface>()

    // Input resources replaced while a SurfaceRequest may still use their Surface. They keep being
    // drained, and each is released when the SurfaceRequest it was provided to completes, or on
    // teardown.
    private val detachedInputResources = mutableMapOf<Surface, OpenGlBridge.DetachedResources>()

    /**
     * Creates new input OpenGL resources for [resolution] and provides their producer [Surface] to
     * [surfaceRequest]. [activeCamSurface], if any, is bound to the new input renderer.
     *
     * @param onRequestCompleted Called on the GL thread when [surfaceRequest] completes.
     * @return false if the OpenGL pipeline couldn't be initialized. [surfaceRequest] then gets no
     *   Surface and won't complete.
     */
    internal fun provideNewInputSurface(
        surfaceRequest: SurfaceRequest,
        resolution: Size,
        activeCamSurface: Surface?,
        onRequestCompleted: () -> Unit,
    ): Boolean {
        renderPipeline.ensureInputGlInitialized(
            resolution = resolution,
            activeCamSurface = activeCamSurface,
            forceNewInput = true,
            onOldResourcesDetached = ::onInputResourcesDetached,
        )
        val inputSurface = renderPipeline.inputBridge.inputSurface
        if (inputSurface == null || !inputSurface.isValid) {
            surfaceRequest.willNotProvideSurface()
            return false
        }
        inputSurfacesInUse.add(inputSurface)
        surfaceRequest.provideSurface(inputSurface, glExecutor) { result ->
            Logger.d(
                TAG,
                "SurfaceRequest provideSurface completed: resultCode=${result.resultCode}",
            )
            onInputSurfaceUseEnded(inputSurface)
            onRequestCompleted()
        }
        return true
    }

    /**
     * Handles input resources replaced for a new [SurfaceRequest]. If the earlier [SurfaceRequest]
     * may still use their [Surface], e.g. because the camera hasn't stopped producing frames into
     * it yet, they are kept until that request completes. Otherwise, they are released right away.
     *
     * Kept resources unregister the output and the session camera surface from their renderer, so
     * these can be connected to the new renderer, and keep draining the old
     * [android.graphics.SurfaceTexture]. Otherwise, the camera could block waiting for a free
     * buffer of the old stream, which can stall the whole capture pipeline while the old capture
     * session closes.
     */
    private fun onInputResourcesDetached(detached: OpenGlBridge.DetachedResources) {
        val surface = detached.surface
        if (surface != null && inputSurfacesInUse.contains(surface)) {
            detached.drainUntilReleased()
            detachedInputResources[surface] = detached
        } else {
            detached.release()
        }
    }

    /**
     * Called when the [SurfaceRequest] that was given [surface] completes. Releases the detached
     * input resources of [surface], if any. A [surface] that is still the input surface is kept.
     */
    private fun onInputSurfaceUseEnded(surface: Surface) {
        if (inputSurfacesInUse.remove(surface)) {
            detachedInputResources.remove(surface)?.release()
        }
    }

    /** Releases all detached input resources, e.g. when tearing down the OpenGL pipeline. */
    internal fun releaseDetachedResources() {
        detachedInputResources.values.forEach { it.release() }
        detachedInputResources.clear()
    }

    private companion object {
        private const val TAG = "LlbInputSurface"
    }
}
