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

package androidx.wear.watchface.samples

import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** A list of triangles drawn with a texture using OpenGL ES 2.0. */
internal class Gles2TexturedTriangleList(
    private val program: Program,
    triangleCoords: FloatArray,
    private val textureCoords: FloatArray
) {
    init {
        require(triangleCoords.size % (VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX coordinates")
        }
        require(textureCoords.size % (VERTICES_PER_TRIANGLE * TEXTURE_COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICES_PER_TRIANGLE * NUM_TEXTURE_COMPONENTS texture " +
                "coordinates")
        }
    }

    /** The VBO containing the vertex coordinates. */
    private val vertexBuffer =
        ByteBuffer.allocateDirect(triangleCoords.size * BYTES_PER_FLOAT)
            .apply { order(ByteOrder.nativeOrder()) }
            .asFloatBuffer()
            .apply {
                put(triangleCoords)
                position(0)
            }

    /** The VBO containing the vertex coordinates. */
    private val textureCoordsBuffer =
        ByteBuffer.allocateDirect(textureCoords.size * BYTES_PER_FLOAT)
            .apply { order(ByteOrder.nativeOrder()) }
            .asFloatBuffer()
            .apply {
                put(textureCoords)
                position(0)
            }

    /** Number of coordinates in this triangle list. */
    private val numCoords = triangleCoords.size / COORDS_PER_VERTEX

    /**
     * Draws this triangle list using OpenGL commands.
     *
     * @param mvpMatrix the Model View Project matrix to draw this triangle list
     */
    fun draw(mvpMatrix: FloatArray?) {
        // Pass the MVP matrix, vertex data, and color to OpenGL.
        program.bind(mvpMatrix, vertexBuffer, textureCoordsBuffer)

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numCoords)
        if (CHECK_GL_ERRORS) checkGlError("glDrawArrays")
    }

    /** OpenGL shaders for drawing textured triangle lists. */
    class Program {
        /** ID OpenGL uses to identify this program. */
        private val programId: Int

        /** Handle for uMvpMatrix uniform in vertex shader. */
        private val mvpMatrixHandle: Int

        /** Handle for aPosition attribute in vertex shader. */
        private val positionHandle: Int

        /** Handle for aTextureCoordinate uniform in fragment shader. */
        private val textureCoordinateHandle: Int

        companion object {
            /** Trivial vertex shader that transforms the input vertex by the MVP matrix. */
            private const val VERTEX_SHADER_CODE =
                "" +
                    "uniform mat4 uMvpMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMvpMatrix * aPosition;\n" +
                    "    textureCoordinate = aTextureCoordinate.xy;" +
                    "}\n"

            /** Trivial fragment shader that draws with a texture. */
            private const val FRAGMENT_SHADER_CODE =
                "" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "uniform sampler2D texture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(texture, textureCoordinate);\n" +
                    "}\n"
        }

        /**
         * Tells OpenGL to use this program. Call this method before drawing a sequence of triangle
         * lists.
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

        fun onDestroy() {
            GLES20.glDeleteProgram(programId)
            if (CHECK_GL_ERRORS) {
                checkGlError("glDeleteProgram")
            }
        }

        /** Sends the given MVP matrix, vertex data, and color to OpenGL. */
        fun bind(
            mvpMatrix: FloatArray?,
            vertexBuffer: FloatBuffer?,
            textureCoordinatesBuffer: FloatBuffer?
        ) {
            // Pass the MVP matrix to OpenGL.
            GLES20.glUniformMatrix4fv(
                mvpMatrixHandle,
                1 /* count */,
                false /* transpose */,
                mvpMatrix,
                0 /* offset */
            )
            if (CHECK_GL_ERRORS) {
                checkGlError("glUniformMatrix4fv")
            }

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
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)

            // Create empty OpenGL Program.
            programId = GLES20.glCreateProgram()
            if (CHECK_GL_ERRORS) checkGlError("glCreateProgram")
            check(programId != 0) { "glCreateProgram failed" }

            // Add the shaders to the program.
            GLES20.glAttachShader(programId, vertexShader)
            if (CHECK_GL_ERRORS) {
                checkGlError("glAttachShader")
            }
            GLES20.glAttachShader(programId, fragmentShader)
            if (CHECK_GL_ERRORS) {
                checkGlError("glAttachShader")
            }

            // Link the program so it can be executed.
            GLES20.glLinkProgram(programId)
            if (CHECK_GL_ERRORS) {
                checkGlError("glLinkProgram")
            }

            // Get a handle to the uMvpMatrix uniform in the vertex shader.
            mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMvpMatrix")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetUniformLocation")
            }

            // Get a handle to the vertex shader's aPosition attribute.
            positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetAttribLocation")
            }

            // Get a handle to vertex shader's aUV attribute.
            textureCoordinateHandle = GLES20.glGetAttribLocation(programId, "aTextureCoordinate")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetAttribLocation")
            }

            // Enable vertex array (VBO).
            GLES20.glEnableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
            }
        }
    }

    companion object {
        private const val TAG = "Gles2TexturedTriangleList"

        /**
         * Whether to check for GL errors. This is slow, so not appropriate for production builds.
         */
        private const val CHECK_GL_ERRORS = false

        /** Number of coordinates per vertex in this array: one for each of x, y, and z. */
        private const val COORDS_PER_VERTEX = 3

        /** Number of texture coordinates per vertex in this array: one for u & v */
        private const val TEXTURE_COORDS_PER_VERTEX = 2

        /** Number of bytes to store a float in GL. */
        const val BYTES_PER_FLOAT = 4

        /** Number of bytes per vertex. */
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Number of bytes per vertex for texture coords. */
        private const val TEXTURE_COORDS_VERTEX_STRIDE = TEXTURE_COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Triangles have three vertices. */
        private const val VERTICES_PER_TRIANGLE = 3

        /**
         * Checks if any of the GL calls since the last time this method was called set an error
         * condition. Call this method immediately after calling a GL method. Pass the name of the
         * GL operation. For example:
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
                    glOperation +
                        " caused GL error 0x" +
                        Integer.toHexString(error) +
                        ": " +
                        errorString
                Log.e(TAG, message)
                throw RuntimeException(message)
            }
        }

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
            if (CHECK_GL_ERRORS) checkGlError("glCreateShader")
            check(shader != 0) { "glCreateShader failed" }

            // Add the source code to the shader and compile it.
            GLES20.glShaderSource(shader, shaderCode)
            if (CHECK_GL_ERRORS) checkGlError("glShaderSource")
            GLES20.glCompileShader(shader)
            if (CHECK_GL_ERRORS) checkGlError("glCompileShader")
            return shader
        }
    }
}
