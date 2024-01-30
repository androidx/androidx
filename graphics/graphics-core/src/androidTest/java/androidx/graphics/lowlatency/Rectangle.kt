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
import java.nio.ShortBuffer

class Rectangle {

    private val mVertexBuffer: FloatBuffer
    private val mDrawListBuffer: ShortBuffer
    private val mProgram: Int
    private var mPositionHandle = 0
    private var mColorHandle = 0
    private var mMVPMatrixHandle = 0
    private var mColor = floatArrayOf(1f, 0f, 0f, 1f)
    private var mSquareCoords = FloatArray(12)

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect( // (# of coordinate values * 4 bytes per float)
            mSquareCoords.size * 4
        )
        bb.order(ByteOrder.nativeOrder())
        mVertexBuffer = bb.asFloatBuffer()
        mVertexBuffer.put(mSquareCoords)
        mVertexBuffer.position(0)
        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect( // (# of coordinate values * 2 bytes per short)
            DRAW_ORDER.size * 2
        )
        dlb.order(ByteOrder.nativeOrder())
        mDrawListBuffer = dlb.asShortBuffer()
        mDrawListBuffer.put(DRAW_ORDER)
        mDrawListBuffer.position(0)
        // prepare shaders and OpenGL program
        val vertexShader = loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexShaderCode
        )
        val fragmentShader = loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            fragmentShaderCode
        )
        mProgram = GLES20.glCreateProgram() // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(mProgram) // create OpenGL program executables
    }

    fun draw(
        mvpMatrix: FloatArray?,
        color: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ) {

        mColor[0] = Color.red(color) / 255f
        mColor[1] = Color.green(color) / 255f
        mColor[2] = Color.blue(color) / 255f
        mColor[3] = Color.alpha(color) / 255f

        // top left
        mSquareCoords[0] = left
        mSquareCoords[1] = top
        mSquareCoords[2] = 0f
        // bottom left
        mSquareCoords[3] = left
        mSquareCoords[4] = bottom
        mSquareCoords[5] = 0f
        // bottom right
        mSquareCoords[6] = right
        mSquareCoords[7] = bottom
        mSquareCoords[8] = 0f
        // top right
        mSquareCoords[9] = right
        mSquareCoords[10] = top
        mSquareCoords[11] = 0f

        mVertexBuffer.clear()
        mVertexBuffer.put(mSquareCoords)
        mVertexBuffer.position(0)

        GLES20.glUseProgram(mProgram)
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        // Prepare the triangle coordinate data
        // 4 bytes per vertex
        val vertexStride = COORDS_PER_VERTEX * 4
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, mVertexBuffer
        )
        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor")
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0)
        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        // Draw the square
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, DRAW_ORDER.size,
            GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer
        )
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    companion object {
        private val vertexShaderCode =
            """
                uniform mat4 uMVPMatrix;
                attribute vec4 vPosition;
                void main() {
                  gl_Position = uMVPMatrix * vPosition;
                }
            """
        private val fragmentShaderCode =
            """
                precision mediump float;
                uniform vec4 vColor;
                void main() {
                  gl_FragColor = vColor;
                }
            """

        // number of coordinates per vertex in this array
        val COORDS_PER_VERTEX = 3
        val DRAW_ORDER = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices
    }
}
