/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface

import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.util.Log
import androidx.annotation.Px
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Whether to check for GL errors. This is slow, so not appropriate for production builds.
 */
internal const val CHECK_GL_ERRORS = false

private const val TAG = "RenderBufferTexture"

/**
 * Checks if any of the GL calls since the last time this method was called set an error
 * condition. Call this method immediately after calling a GL method. Pass the name of the
 * GL operation. For example:
 *
 * <pre>
 * mColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
 * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
 *
 * If the operation is not successful, the check throws an exception.
 *
 * *Note* This is quite slow so it's best to use it sparingly in production builds.
 *
 * @param glOperation name of the OpenGL call to check
 */
internal fun checkGlError(glOperation: String) {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
        var errorString = GLU.gluErrorString(error)
        if (errorString == null) {
            errorString = GLUtils.getEGLErrorString(error)
        }
        val message =
            glOperation + " caused GL error 0x" + Integer.toHexString(error) +
                ": " + errorString
        Log.e(TAG, message)
        throw RuntimeException(message)
    }
}

/**
 * Handles a framebuffer and texture for rendering to texture. Also handles drawing a full screen
 * quad to apply the texture as an overlay.
 */
internal class RenderBufferTexture(
    @Px
    private val width: Int,

    @Px
    private val height: Int
) {
    val framebuffer = IntArray(1)
    val textureId = IntArray(1)

    val fullScreenQuad = Gles2TexturedTriangleList(
        Gles2TexturedTriangleList.Program(),
        // List of (x,y,z) coordinates for two triangles to make a quad that covers the whole screen
        floatArrayOf(
            -1.0f,
            -1.0f,
            0.5f,

            -1.0f,
            1.0f,
            0.5f,

            1.0f,
            -1.0f,
            0.5f,

            -1.0f,
            1.0f,
            0.5f,

            1.0f,
            -1.0f,
            0.5f,

            1.0f,
            1.0f,
            0.5f
        ),

        // List of (u, v) texture coordinates.
        floatArrayOf(
            0.0f,
            0.0f,

            0.0f,
            1.0f,

            1.0f,
            0.0f,

            0.0f,
            1.0f,

            1.0f,
            0.0f,

            1.0f,
            1.0f
        )
    )

    init {
        // Create the texture
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null
        )
        if (CHECK_GL_ERRORS) {
            checkGlError("glTexImage2D")
        }

        // Create the frame buffer
        GLES20.glGenFramebuffers(1, framebuffer, 0)
        if (CHECK_GL_ERRORS) {
            checkGlError("glGenFramebuffers")
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer[0])
        if (CHECK_GL_ERRORS) {
            checkGlError("glBindFramebuffer")
        }
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            textureId[0],
            0
        )
        if (CHECK_GL_ERRORS) {
            checkGlError("glFramebufferTexture2D")
        }

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Failed to create framebuffer")
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun bindFrameBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer[0])
        if (CHECK_GL_ERRORS) {
            checkGlError("glBindFramebuffer")
        }
        GLES20.glViewport(0, 0, width, height)
        if (CHECK_GL_ERRORS) {
            checkGlError("glFramebufferTexture2D")
        }
    }

    fun compositeQuad() {
        fullScreenQuad.program.bindProgramAndAttribs()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        if (CHECK_GL_ERRORS) {
            checkGlError("glBindTexture")
        }
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        if (CHECK_GL_ERRORS) {
            checkGlError("glBlendFunc")
        }
        fullScreenQuad.draw()
    }
}

/**
 * A list of triangles drawn with a texture using OpenGL ES 2.0.
 */
internal class Gles2TexturedTriangleList(
    internal val program: Program,
    triangleCoords: FloatArray,
    private val textureCoords: FloatArray
) {
    init {
        require(triangleCoords.size % (VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX coordinates")
        }
        require(textureCoords.size % (VERTICES_PER_TRIANGLE * TEXTURE_COORDS_PER_VERTEX) == 0) {
            (
                "must be multiple of VERTICES_PER_TRIANGLE * NUM_TEXTURE_COMPONENTS texture " +
                    "coordinates"
                )
        }
    }

    /** The VBO containing the vertex coordinates. */
    private val vertexBuffer =
        ByteBuffer.allocateDirect(triangleCoords.size * BYTES_PER_FLOAT)
            .apply { order(ByteOrder.nativeOrder()) }
            .asFloatBuffer().apply {
                put(triangleCoords)
                position(0)
            }

    /** The VBO containing the vertex coordinates. */
    private val textureCoordsBuffer =
        ByteBuffer.allocateDirect(textureCoords.size * BYTES_PER_FLOAT)
            .apply { order(ByteOrder.nativeOrder()) }
            .asFloatBuffer().apply {
                put(textureCoords)
                position(0)
            }

    /** Number of coordinates in this triangle list.  */
    private val numCoords = triangleCoords.size / COORDS_PER_VERTEX

    /**
     * Draws this triangle list using OpenGL commands.
     */
    internal fun draw() {
        // Pass vertex data, and texture coordinates to OpenGL.
        program.bind(vertexBuffer, textureCoordsBuffer)

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numCoords)
        if (CHECK_GL_ERRORS) checkGlError(
            "glDrawArrays"
        )

        program.unbindAttribs()
    }

    /** OpenGL shaders for drawing textured triangle lists.  */
    internal class Program {
        /** ID OpenGL uses to identify this program.  */
        private val programId: Int

        /** Handle for aPosition attribute in vertex shader.  */
        private val positionHandle: Int

        /** Handle for aTextureCoordinate uniform in fragment shader.  */
        private val textureCoordinateHandle: Int

        companion object {
            /** Trivial pass through vertex shader. */
            private const val VERTEX_SHADER_CODE = "" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoordinate;\n" +
                "varying vec2 textureCoordinate;\n" +
                "void main() {\n" +
                "    gl_Position = aPosition;\n" +
                "    textureCoordinate = aTextureCoordinate.xy;\n" +
                "}\n"

            /** Trivial fragment shader that draws with a texture.  */
            private const val FRAGMENT_SHADER_CODE = "" +
                "varying highp vec2 textureCoordinate;\n" +
                "uniform sampler2D texture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(texture, textureCoordinate);\n" +
                "}\n"
        }

        /**
         * Tells OpenGL to use this program. Call this method before drawing a sequence of
         * triangle lists.
         */
        fun bindProgramAndAttribs() {
            GLES20.glUseProgram(programId)
            if (CHECK_GL_ERRORS) {
                checkGlError("glUseProgram")
            }

            // Enable vertex array (VBO).
            GLES20.glEnableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
            }

            GLES20.glEnableVertexAttribArray(textureCoordinateHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
            }
        }

        fun unbindAttribs() {
            GLES20.glDisableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glDisableVertexAttribArray")
            }

            GLES20.glDisableVertexAttribArray(textureCoordinateHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glDisableVertexAttribArray")
            }
        }

        /** Sends the given MVP matrix, vertex data, and color to OpenGL.  */
        fun bind(
            vertexBuffer: FloatBuffer?,
            textureCoordinatesBuffer: FloatBuffer?
        ) {
            // Pass the VBO with the triangle list's vertices to OpenGL.
            GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false /* normalized */,
                VERTEX_STRIDE,
                vertexBuffer
            )
            if (CHECK_GL_ERRORS) {
                checkGlError("glVertexAttribPointer")
            }

            // Pass the VBO with the triangle list's texture coordinates to OpenGL.
            GLES20.glVertexAttribPointer(
                textureCoordinateHandle,
                TEXTURE_COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false /* normalized */,
                TEXTURE_COORDS_VERTEX_STRIDE,
                textureCoordinatesBuffer
            )
            if (CHECK_GL_ERRORS) {
                checkGlError("glVertexAttribPointer")
            }
        }

        /**
         * Creates a program to draw triangle lists. For optimal drawing efficiency, one program
         * should be used for all triangle lists being drawn.
         */
        init {
            // Prepare shaders.
            val vertexShader = loadShader(
                GLES20.GL_VERTEX_SHADER,
                VERTEX_SHADER_CODE
            )
            val fragmentShader = loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                FRAGMENT_SHADER_CODE
            )

            // Create empty OpenGL Program.
            programId = GLES20.glCreateProgram()
            if (CHECK_GL_ERRORS) checkGlError(
                "glCreateProgram"
            )
            check(programId != 0) { "glCreateProgram failed" }

            // Add the shaders to the program.
            GLES20.glAttachShader(programId, vertexShader)
            if (CHECK_GL_ERRORS) {
                checkGlError("glAttachShader")
            }
            GLES20.glAttachShader(programId, fragmentShader)

            // Link the program so it can be executed.
            GLES20.glLinkProgram(programId)
            if (CHECK_GL_ERRORS) {
                checkGlError("glLinkProgram")
            }

            // Get a handle to the vertex shader's aPosition attribute.
            positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetAttribLocation positionHandle")
            }

            // Get a handle to vertex shader's aUV attribute.
            textureCoordinateHandle =
                GLES20.glGetAttribLocation(programId, "aTextureCoordinate")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetAttribLocation textureCoordinateHandle")
            }

            // Enable vertex array (VBO).
            GLES20.glEnableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
            }
        }
    }

    internal companion object {
        /** Number of coordinates per vertex in this array: one for each of x, y, and z.  */
        private const val COORDS_PER_VERTEX = 3

        /** Number of texture coordinates per vertex in this array: one for u & v */
        private const val TEXTURE_COORDS_PER_VERTEX = 2

        /** Number of bytes to store a float in GL.  */
        const val BYTES_PER_FLOAT = 4

        /** Number of bytes per vertex.  */
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Number of bytes per vertex for texture coords.  */
        private const val TEXTURE_COORDS_VERTEX_STRIDE = TEXTURE_COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Triangles have three vertices. */
        private const val VERTICES_PER_TRIANGLE = 3

        /**
         * Compiles an OpenGL shader.
         *
         * @param type [GLES20.GL_VERTEX_SHADER] or [GLES20.GL_FRAGMENT_SHADER]
         * @param shaderCode string containing the shader source code
         * @return ID for the shader
         */
        internal fun loadShader(type: Int, shaderCode: String): Int {
            // Create a vertex or fragment shader.
            val shader = GLES20.glCreateShader(type)
            if (CHECK_GL_ERRORS) checkGlError(
                "glCreateShader"
            )
            check(shader != 0) { "glCreateShader failed" }

            // Add the source code to the shader and compile it.
            GLES20.glShaderSource(shader, shaderCode)
            if (CHECK_GL_ERRORS) checkGlError(
                "glShaderSource"
            )
            GLES20.glCompileShader(shader)
            if (CHECK_GL_ERRORS) checkGlError(
                "glCompileShader"
            )
            return shader
        }
    }
}