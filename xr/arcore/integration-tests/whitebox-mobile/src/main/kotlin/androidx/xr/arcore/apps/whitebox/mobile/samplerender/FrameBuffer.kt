/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender

import android.opengl.GLES30
import android.util.Log
import java.io.Closeable

/**
 * A framebuffer associated with a texture.
 *
 * In order to render to the [Framebuffer], use [SampleRender.draw(Mesh, Shader, Framebuffer)].
 *
 * @param render The [SampleRender] instance to which this buffer belongs.
 * @param width The initial width of the framebuffer.
 * @param height The initial height of the framebuffer.
 */
class Framebuffer(val render: SampleRender, width: Int, height: Int) : Closeable {

    internal val framebufferId: IntArray = intArrayOf(0)

    public var colorTexture: Texture
        private set

    public var depthTexture: Texture
        private set

    public var width: Int = 0
        private set

    public var height: Int = 0
        private set

    init {
        try {
            colorTexture =
                Texture(
                    render,
                    Texture.Target.TEXTURE_2D,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    useMipmaps = false,
                )
            depthTexture =
                Texture(
                    render,
                    Texture.Target.TEXTURE_2D,
                    Texture.WrapMode.CLAMP_TO_EDGE,
                    useMipmaps = false,
                )

            // Set parameters of the depth texture so that it's readable by shaders.
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.textureId)
            maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_COMPARE_MODE,
                GLES30.GL_NONE,
            )
            maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST,
            )
            maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_NEAREST,
            )
            maybeThrowGLException("Failed to set texture parameter", "glTexParameteri")

            // Set initial dimensions.
            resize(width, height)

            // Create framebuffer object and bind to the color and depth textures.
            GLES30.glGenFramebuffers(1, framebufferId, 0)
            maybeThrowGLException("Framebuffer creation failed", "glGenFramebuffers")
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebufferId[0])
            maybeThrowGLException("Failed to bind framebuffer", "glBindFramebuffer")
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                colorTexture.textureId,
                /*level=*/ 0,
            )
            maybeThrowGLException(
                "Failed to bind color texture to framebuffer",
                "glFramebufferTexture2D",
            )
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_DEPTH_ATTACHMENT,
                GLES30.GL_TEXTURE_2D,
                depthTexture.textureId,
                /*level=*/ 0,
            )
            maybeThrowGLException(
                "Failed to bind depth texture to framebuffer",
                "glFramebufferTexture2D",
            )

            val status = GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER)
            check(status == GLES30.GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer construction not complete: code " + status
            }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    override fun close() {
        if (framebufferId[0] != 0) {
            GLES30.glDeleteFramebuffers(1, framebufferId, 0)
            maybeLogGLError(Log.WARN, TAG, "Failed to free framebuffer", "glDeleteFramebuffers")
            framebufferId[0] = 0
        }
        colorTexture.close()
        depthTexture.close()
    }

    /** Resizes the framebuffer to the given dimensions. */
    fun resize(width: Int, height: Int) {
        if (this.width == width && this.height == height) {
            return
        }
        this.width = width
        this.height = height

        // Color texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, colorTexture.textureId)
        maybeThrowGLException("Failed to bind color texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            /*level=*/ 0,
            GLES30.GL_RGBA,
            width,
            height,
            /*border=*/ 0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            /*pixels=*/ null,
        )
        maybeThrowGLException("Failed to specify color texture format", "glTexImage2D")

        // Depth texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTexture.textureId)
        maybeThrowGLException("Failed to bind depth texture", "glBindTexture")
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            /*level=*/ 0,
            GLES30.GL_DEPTH_COMPONENT32F,
            width,
            height,
            /*border=*/ 0,
            GLES30.GL_DEPTH_COMPONENT,
            GLES30.GL_FLOAT,
            /*pixels=*/ null,
        )
        maybeThrowGLException("Failed to specify depth texture format", "glTexImage2D")
    }

    internal fun getFramebufferId(): Int {
        return framebufferId[0]
    }

    companion object {
        private const val TAG: String = "FrameBuffer"
    }
}
