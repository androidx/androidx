/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.view

import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.VisibleForTesting
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.newHandlerExecutor
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.ShaderProvider
import androidx.core.util.Preconditions.checkState
import java.util.concurrent.Executor

/**
 * A processor that applies tone mapping on camera output.
 *
 * <p>The thread safety is guaranteed by using the main thread.
 */
class ToneMappingSurfaceProcessor : SurfaceProcessor, OnFrameAvailableListener {

    companion object {
        // A fragment shader that applies a yellow hue.
        private val TONE_MAPPING_SHADER_PROVIDER = object : ShaderProvider {
            override fun createFragmentShader(sampler: String, fragCoords: String): String {
                return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES $sampler;
                    varying vec2 $fragCoords;
                    void main() {
                      vec4 sampleColor = texture2D($sampler, $fragCoords);
                      gl_FragColor = vec4(
                           sampleColor.r * 0.5 + sampleColor.g * 0.8 + sampleColor.b * 0.3,
                           sampleColor.r * 0.4 + sampleColor.g * 0.7 + sampleColor.b * 0.2,
                           sampleColor.r * 0.3 + sampleColor.g * 0.5 + sampleColor.b * 0.1,
                           1.0);
                     }
                    """
            }
        }

        private const val GL_THREAD_NAME = "ToneMappingSurfaceProcessor"
    }

    private val glThread: HandlerThread = HandlerThread(GL_THREAD_NAME)
    private var glHandler: Handler
    private var glExecutor: Executor

    // Members below are only accessed on GL thread.
    private val glRenderer: OpenGlRenderer = OpenGlRenderer()
    private val outputSurfaces: MutableMap<SurfaceOutput, Surface> = mutableMapOf()
    private val textureTransform: FloatArray = FloatArray(16)
    private val surfaceTransform: FloatArray = FloatArray(16)
    private var isReleased = false

    // For testing only
    private var surfaceRequested = false
    private var outputSurfaceProvided = false

    init {
        glThread.start()
        glHandler = Handler(glThread.looper)
        glExecutor = newHandlerExecutor(glHandler)
        glExecutor.execute {
            glRenderer.init(DynamicRange.SDR, TONE_MAPPING_SHADER_PROVIDER)
        }
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        checkGlThread()
        if (isReleased) {
            surfaceRequest.willNotProvideSurface()
            return
        }
        surfaceRequested = true
        val surfaceTexture = SurfaceTexture(glRenderer.textureName)
        surfaceTexture.setDefaultBufferSize(
            surfaceRequest.resolution.width, surfaceRequest.resolution.height
        )
        val surface = Surface(surfaceTexture)
        surfaceRequest.provideSurface(surface, glExecutor) {
            surfaceTexture.setOnFrameAvailableListener(null)
            surfaceTexture.release()
            surface.release()
        }
        surfaceTexture.setOnFrameAvailableListener(this, glHandler)
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        checkGlThread()
        outputSurfaceProvided = true
        if (isReleased) {
            surfaceOutput.close()
            return
        }
        val surface = surfaceOutput.getSurface(glExecutor) {
            surfaceOutput.close()
            outputSurfaces.remove(surfaceOutput)?.let { removedSurface ->
                glRenderer.unregisterOutputSurface(removedSurface)
            }
        }
        glRenderer.registerOutputSurface(surface)
        outputSurfaces[surfaceOutput] = surface
    }

    @VisibleForTesting
    fun isSurfaceRequestedAndProvided(): Boolean {
        return surfaceRequested && outputSurfaceProvided
    }

    fun release() {
        glExecutor.execute {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        checkGlThread()
        if (!isReleased) {
            // Once release is called, we can stop sending frame to output surfaces.
            for (surfaceOutput in outputSurfaces.keys) {
                surfaceOutput.close()
            }
            outputSurfaces.clear()
            glRenderer.release()
            glThread.quitSafely()
            isReleased = true
        }
    }

    private fun checkGlThread() {
        checkState(GL_THREAD_NAME == Thread.currentThread().name)
    }

    fun getGlExecutor(): Executor {
        return glExecutor
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        checkGlThread()
        if (isReleased) {
            return
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureTransform)
        for (entry in outputSurfaces.entries.iterator()) {
            val surface = entry.value
            val surfaceOutput = entry.key

            surfaceOutput.updateTransformMatrix(surfaceTransform, textureTransform)
            glRenderer.render(surfaceTexture.timestamp, surfaceTransform, surface)
        }
    }
}
