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

package androidx.graphics.lowlatency

import android.hardware.HardwareBuffer
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [GLRenderer.RenderCallback] implementation that renders content into a frame buffer object
 * backed by a [HardwareBuffer] object
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class HardwareBufferRenderer(
    private val hardwareBufferRendererCallbacks: RenderCallbacks
) : GLRenderer.RenderCallback {

    private val mClear = AtomicBoolean(false)

    override fun onSurfaceCreated(
        spec: EGLSpec,
        config: EGLConfig,
        surface: Surface,
        width: Int,
        height: Int
    ): EGLSurface? = null

    fun clear() {
        mClear.set(true)
    }

    override fun onDrawFrame(eglManager: EGLManager) {
        val egl = eglManager.eglSpec
        val buffer = hardwareBufferRendererCallbacks.obtainRenderBuffer(egl)
        var renderFence: RenderFence? = null
        try {
            buffer.makeCurrent()
            if (mClear.getAndSet(false)) {
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            } else {
                hardwareBufferRendererCallbacks.onDraw(eglManager)
            }
            GLES20.glFlush()
            renderFence = RenderFence(egl)
            renderFence.awaitForever()
            // At this point the HardwareBuffer has the contents of the GL rendering
            // Create a surface Control transaction to dispatch this request
            hardwareBufferRendererCallbacks.onDrawComplete(buffer)
        } finally {
            renderFence?.close()
        }
    }

    /**
     * Callbacks invoked to render content leveraging a [HardwareBufferRenderer]
     */
    interface RenderCallbacks {

        /**
         * Obtain a [RenderBuffer] to render content into. The [RenderBuffer] obtained here
         * is expected to be managed by the consumer of [HardwareBufferRenderer]. That is
         * callers of this API are expected to be maintaining a reference to the returned
         * [RenderBuffer] here and calling [RenderBuffer.close] where appropriate as this will
         * these instances will not be released by [HardwareBufferRenderer]
         */
        fun obtainRenderBuffer(egl: EGLSpec): RenderBuffer

        /**
         * Draw contents into the [HardwareBuffer]
         */
        fun onDraw(eglManager: EGLManager)

        /**
         * Callback when [onDraw] is complete and the contents of the draw
         * are reflected in the corresponding [HardwareBuffer]
         */
        fun onDrawComplete(renderBuffer: RenderBuffer)
    }
}