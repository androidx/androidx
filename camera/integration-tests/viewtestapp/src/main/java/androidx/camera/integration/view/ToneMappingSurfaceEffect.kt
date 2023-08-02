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
import android.os.Looper
import android.view.Surface
import androidx.annotation.VisibleForTesting
import androidx.camera.core.SurfaceEffect
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.Threads.checkMainThread
import androidx.camera.core.impl.utils.executor.CameraXExecutors.mainThreadExecutor
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.ShaderProvider

/**
 * A effect that applies tone mapping on camera output.
 *
 * <p>The thread safety is guaranteed by using the main thread.
 */
class ToneMappingSurfaceEffect : SurfaceEffect, OnFrameAvailableListener {

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
    }

    private val mainThreadHandler: Handler = Handler(Looper.getMainLooper())
    private val glRenderer: OpenGlRenderer = OpenGlRenderer()
    private val outputSurfaces: MutableMap<SurfaceOutput, Surface> = mutableMapOf()
    private val textureTransform: FloatArray = FloatArray(16)
    private val surfaceTransform: FloatArray = FloatArray(16)
    private var isReleased = false

    // For testing.
    private var surfaceRequested = false
    // For testing.
    private var outputSurfaceProvided = false

    init {
        mainThreadExecutor().execute {
            glRenderer.init(TONE_MAPPING_SHADER_PROVIDER)
        }
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        checkMainThread()
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
        surfaceRequest.provideSurface(surface, mainThreadExecutor()) {
            surfaceTexture.setOnFrameAvailableListener(null)
            surfaceTexture.release()
            surface.release()
        }
        surfaceTexture.setOnFrameAvailableListener(this, mainThreadHandler)
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        checkMainThread()
        outputSurfaceProvided = true
        if (isReleased) {
            surfaceOutput.close()
            return
        }
        outputSurfaces[surfaceOutput] = surfaceOutput.getSurface(mainThreadExecutor()) {
            surfaceOutput.close()
            outputSurfaces.remove(surfaceOutput)
        }
    }

    @VisibleForTesting
    fun isSurfaceRequestedAndProvided(): Boolean {
        return surfaceRequested && outputSurfaceProvided
    }

    fun release() {
        checkMainThread()
        if (isReleased) {
            return
        }
        glRenderer.release()
        isReleased = true
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        checkMainThread()
        if (isReleased) {
            return
        }
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(textureTransform)
        for (entry in outputSurfaces.entries.iterator()) {
            val surface = entry.value
            val surfaceOutput = entry.key
            glRenderer.setOutputSurface(surface)
            surfaceOutput.updateTransformMatrix(surfaceTransform, textureTransform)
            glRenderer.render(surfaceTexture.timestamp, surfaceTransform)
        }
    }
}