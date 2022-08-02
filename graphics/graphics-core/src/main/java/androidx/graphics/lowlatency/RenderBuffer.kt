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
import android.opengl.GLES20
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLSpec
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR

/**
 * Object that enables rendering into a [HardwareBuffer] by
 * creating a frame buffer object from it by leveraging Android
 * specific EGL extensions to create an [EGLImageKHR] object
 * that is loaded as a texture.
 */
@RequiresApi(Build.VERSION_CODES.O)
internal class RenderBuffer(
    private val egl: EGLSpec,
    val hardwareBuffer: HardwareBuffer,
) {

    private var eglImage: EGLImageKHR?
    private var texture: Int = -1
    private var frameBuffer: Int = -1
    var isClosed = false
        private set

    // Int array used for creation of fbos/textures
    private val buffer = IntArray(1)

    init {
        val image: EGLImageKHR = egl.eglCreateImageFromHardwareBuffer(hardwareBuffer)
            ?: throw IllegalArgumentException("Unable to create EGLImage from HardwareBuffer")
        eglImage = image

        GLES20.glGenTextures(1, buffer, 0)
        texture = buffer[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        EGLExt.glEGLImageTargetTexture2DOES(GLES20.GL_TEXTURE_2D, image)

        GLES20.glGenFramebuffers(1, buffer, 0)
        frameBuffer = buffer[0]
    }

    fun makeCurrent() {
        if (!isClosed) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                texture,
                0
            )
        }
    }

    fun close() {
        buffer[0] = frameBuffer
        GLES20.glDeleteBuffers(1, buffer, 0)
        frameBuffer = -1

        buffer[0] = texture
        GLES20.glDeleteTextures(1, buffer, 0)
        texture = -1

        eglImage?.let { egl.eglDestroyImageKHR(it) }
        eglImage = null
        hardwareBuffer.close()
        isClosed = true
    }
}