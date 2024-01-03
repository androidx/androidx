
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

import android.graphics.Color
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL Renderer class responsible for drawing lines
 */
class LineRenderer {

    private var mVertexShader: Int = -1
    private var mFragmentShader: Int = -1
    private var mGlProgram: Int = -1

    private var mPositionHandle: Int = -1
    private var mMvpMatrixHandle: Int = -1

    private var mColorHandle: Int = -1
    private val mColorArray = FloatArray(4)

    private var mVertexBuffer: FloatBuffer? = null
    private val mLineCoords = FloatArray(6)

    fun initialize() {
        release()
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShaderCode)
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShaderCode)

        mGlProgram = GLES20.glCreateProgram()

        GLES20.glAttachShader(mGlProgram, mVertexShader)
        GLES20.glAttachShader(mGlProgram, mFragmentShader)

        GLES20.glLinkProgram(mGlProgram)

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
        mMvpMatrixHandle = GLES20.glGetUniformLocation(mGlProgram, uMVPMatrix)
        mColorHandle = GLES20.glGetUniformLocation(mGlProgram, vColor)
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

    fun drawLines(
        mvpMatrix: FloatArray,
        lines: FloatArray,
        color: Int = Color.RED,
        lineWidth: Float = 10f
    ) {
        GLES20.glUseProgram(mGlProgram)
        GLES20.glLineWidth(lineWidth)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        mColorArray[0] = Color.red(color).toFloat()
        mColorArray[1] = Color.green(color).toFloat()
        mColorArray[2] = Color.blue(color).toFloat()
        mColorArray[3] = Color.alpha(color).toFloat()
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, mColorArray, 0)
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
        }
        GLES20.glDisableVertexAttribArray(mPositionHandle)
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
        private const val vColor = "vColor"
        private const val FragmentShaderCode =
            """
                precision highp float;

                uniform vec4 $vColor;
                void main() {
                    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
                    gl_FragColor = $vColor;
                }
            """
    }
}
