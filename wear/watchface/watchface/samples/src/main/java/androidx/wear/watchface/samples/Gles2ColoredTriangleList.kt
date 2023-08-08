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

/** A list of triangles drawn in a single solid color using OpenGL ES 2.0. */
internal class Gles2ColoredTriangleList(
    private val program: Program,
    triangleCoords: FloatArray,
    private val color: FloatArray
) {
    init {
        require(triangleCoords.size % (VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX coordinates")
        }
        require(color.size == NUM_COLOR_COMPONENTS) { "wrong number of color components" }
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

    /** Number of coordinates in this triangle list. */
    private val numCoords = triangleCoords.size / COORDS_PER_VERTEX

    /**
     * Draws this triangle list using OpenGL commands.
     *
     * @param mvpMatrix the Model View Project matrix to draw this triangle list
     */
    fun draw(mvpMatrix: FloatArray?) {
        // Pass the MVP matrix, vertex data, and color to OpenGL.
        program.bind(mvpMatrix, vertexBuffer, color)

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numCoords)
        if (CHECK_GL_ERRORS) checkGlError("glDrawArrays")
    }

    /** OpenGL shaders for drawing solid colored triangle lists. */
    class Program {
        /** ID OpenGL uses to identify this program. */
        private val programId: Int

        /** Handle for uMvpMatrix uniform in vertex shader. */
        private val mvpMatrixHandle: Int

        /** Handle for aPosition attribute in vertex shader. */
        private val positionHandle: Int

        /** Handle for uColor uniform in fragment shader. */
        private val colorHandle: Int

        companion object {
            /** Trivial vertex shader that transforms the input vertex by the MVP matrix. */
            private const val VERTEX_SHADER_CODE =
                "" +
                    "uniform mat4 uMvpMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMvpMatrix * aPosition;\n" +
                    "}\n"

            /** Trivial fragment shader that draws with a fixed color. */
            private const val FRAGMENT_SHADER_CODE =
                "" +
                    "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = uColor;\n" +
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
        }

        fun unbindAttribs() {
            GLES20.glDisableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glDisableVertexAttribArray")
            }
        }

        /** Sends the given MVP matrix, vertex data, and color to OpenGL. */
        fun bind(mvpMatrix: FloatArray?, vertexBuffer: FloatBuffer?, color: FloatArray?) {
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
            GLES20.glEnableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
            }
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

            // Pass the triangle list's color to OpenGL.
            GLES20.glUniform4fv(colorHandle, 1 /* count */, color, 0 /* offset */)
            if (CHECK_GL_ERRORS) {
                checkGlError("glUniform4fv")
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

            // Get a handle to fragment shader's uColor uniform.
            colorHandle = GLES20.glGetUniformLocation(programId, "uColor")
            if (CHECK_GL_ERRORS) {
                checkGlError("glGetUniformLocation")
            }
        }
    }

    companion object {
        private const val TAG = "GlColoredTriangleList"

        /**
         * Whether to check for GL errors. This is slow, so not appropriate for production builds.
         */
        private const val CHECK_GL_ERRORS = false

        /** Number of coordinates per vertex in this array: one for each of x, y, and z. */
        private const val COORDS_PER_VERTEX = 3

        /** Number of bytes to store a float in GL. */
        const val BYTES_PER_FLOAT = 4

        /** Number of bytes per vertex. */
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Triangles have three vertices. */
        private const val VERTICES_PER_TRIANGLE = 3

        /**
         * Number of components in an OpenGL color. The components are:
         * 1. red
         * 1. green
         * 1. blue
         * 1. alpha
         */
        private const val NUM_COLOR_COMPONENTS = 4

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
