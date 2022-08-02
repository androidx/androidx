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

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import org.junit.Assert.assertEquals

/**
 * OpenGL Renderer class responsible for drawing lines
 */
class LineRenderer {

    private var mVertexShader: Int = -1
    private var mFragmentShader: Int = -1
    private var mGlProgram: Int = -1

    private var mPositionHandle: Int = -1
    private var mMvpMatrixHandle: Int = -1

    private var mVertexBuffer: FloatBuffer? = null
    private val mLineCoords = FloatArray(6)

    fun initialize() {
        release()
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShaderCode)
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode)

        mGlProgram = GLES20.glCreateProgram()
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

        GLES20.glAttachShader(mGlProgram, mVertexShader)
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
        GLES20.glAttachShader(mGlProgram, mFragmentShader)
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

        GLES20.glLinkProgram(mGlProgram)
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

        val bb: ByteBuffer =
            ByteBuffer.allocateDirect( // (number of coordinate values * 4 bytes per float)
                LineCoordsSize * 4
            )
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder())

        // create a floating point buffer from the ByteBuffer
        mVertexBuffer = bb.asFloatBuffer().apply {
            put(mLineCoords)
            position(0)
        }

        mPositionHandle = GLES20.glGetAttribLocation(mGlProgram, vPosition)
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())

        mMvpMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, uMVPMatrix)
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
    }

    fun release() {
        if (mVertexShader != -1) {
            GLES20.glDeleteShader(mVertexShader)
            mVertexShader = -1
        }

        if (mFragmentShader != -1) {
            GLES20.glDeleteShader(mFragmentShader)
            mFragmentShader = -1
        }

        if (mGlProgram != -1) {
            GLES20.glDeleteProgram(mGlProgram)
            mGlProgram = -1
        }
    }

    fun drawLines(mvpMatrix: FloatArray, lines: FloatArray) {
        assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
        GLES20.glUseProgram(mGlProgram)

        val buff = FloatBuffer.allocate(2)
        GLES20.glGetFloatv(GLES20.GL_ALIASED_LINE_WIDTH_RANGE, buff)
        GLES20.glLineWidth(100.0f)

        GLES20.glEnableVertexAttribArray(mPositionHandle)

        GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1, false, mvpMatrix, 0)

        mVertexBuffer?.let { buffer ->
            for (i in 0 until lines.size step 4) {
                mLineCoords[0] = lines[i]
                mLineCoords[1] = lines[i + 1]
                mLineCoords[2] = 0f
                mLineCoords[3] = lines[i + 2]
                mLineCoords[4] = lines[i + 3]
                mLineCoords[5] = 0f
                buffer.put(mLineCoords)
                buffer.position(0)
            }

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                mPositionHandle, CoordsPerVertex,
                GLES20.GL_FLOAT, false,
                VertexStride, buffer
            )
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, VertexCount)

            GLES20.glDisableVertexAttribArray(mPositionHandle)
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
        }
    }

    companion object {

        const val CoordsPerVertex = 3
        const val LineCoordsSize = 6
        private val VertexCount: Int = LineCoordsSize / CoordsPerVertex
        private val VertexStride: Int = CoordsPerVertex * 4 // 4 bytes per vertex

        private const val uMVPMatrix = "uMVPMatrix"
        private const val vPosition = "vPosition"
        private const val VertexShaderCode =
            """
                uniform mat4 $uMVPMatrix;
                attribute vec4 $vPosition;
                void main() { // the matrix must be included as a modifier of gl_Position
                  gl_Position = $uMVPMatrix * $vPosition;
                }
            """

        private const val FragmentShaderCode =
            """
                precision highp float;

                void main() {
                    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
                }
            """

        fun loadShader(type: Int, shaderCode: String?): Int {
            val shader = GLES20.glCreateShader(type)

            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)

            return shader
        }
    }
}