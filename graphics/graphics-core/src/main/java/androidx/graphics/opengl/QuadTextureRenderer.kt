/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.graphics.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

@RequiresApi(Build.VERSION_CODES.O)
internal class QuadTextureRenderer {

    private var mSurfaceTexture: SurfaceTexture? = null

    /**
     * Array used to store 4 vertices of x and y coordinates
     */
    private val mQuadCoords = FloatArray(8)

    /**
     * Transform to apply to the corresponding texture source
     */
    private val mTextureTransform = FloatArray(16)

    /**
     * Handle to the quad position attribute
     */
    private var mQuadPositionHandle = -1

    /**
     * Handle to the texture coordinate attribute
     */
    private var mTexPositionHandle = -1

    /**
     * Handle to the texture sampler uniform
     */
    private var mTextureUniformHandle: Int = -1

    /**
     * Handle to the MVP matrix uniform
     */
    private var mViewProjectionMatrixHandle: Int = -1

    /**
     * Handle to texture transform matrix
     */
    private var mTextureTransformHandle: Int = -1

    /**
     * GL Program used for rendering a quad with a texture
     */
    private var mProgram: Int = -1

    /**
     * Handle to the vertex shader
     */
    private var mVertexShader = -1

    /**
     * Handle to the fragment shader
     */
    private var mFragmentShader = -1

    /**
     * Flag to indicate the resources associated with the shaders/texture has been
     * released. If this is true all subsequent attempts to draw should be ignored
     */
    private var mIsReleased = false

    /**
     * FloatBuffer used to specify quad coordinates
     */
    private val mQuadrantCoordinatesBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(mQuadCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                position(0)
            }
        }

    init {
        mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VertexShader)
        mFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FragmentShader)
        mProgram = GLES20.glCreateProgram()

        GLES20.glAttachShader(mProgram, mVertexShader)
        GLES20.glAttachShader(mProgram, mFragmentShader)
        GLES20.glLinkProgram(mProgram)
        GLES20.glUseProgram(mProgram)

        mQuadPositionHandle = GLES20.glGetAttribLocation(mProgram, aPosition)
        mTexPositionHandle = GLES20.glGetAttribLocation(mProgram, aTexCoord)

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, uTexture)
        mViewProjectionMatrixHandle = GLES20.glGetUniformLocation(mProgram, uVPMatrix)
        mTextureTransformHandle = GLES20.glGetUniformLocation(mProgram, tVPMatrix)

        // Enable blend
        GLES20.glEnable(GLES20.GL_BLEND)
        // Uses to prevent transparent area to turn in black
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun release() {
        if (!mIsReleased) {
            if (mVertexShader != -1) {
                GLES20.glDeleteShader(mVertexShader)
                mVertexShader = -1
            }

            if (mFragmentShader != -1) {
                GLES20.glDeleteShader(mFragmentShader)
                mFragmentShader = -1
            }

            if (mProgram != -1) {
                GLES20.glDeleteProgram(mProgram)
                mProgram = -1
            }

            mIsReleased = true
        }
    }

    private fun configureQuad(width: Float, height: Float): FloatBuffer =
        mQuadrantCoordinatesBuffer.apply {
            put(mQuadCoords.apply {
                this[0] = 0f // top left
                this[1] = height
                this[2] = 0f // bottom left
                this[3] = 0f
                this[4] = width // top right
                this[5] = 0f
                this[6] = width // bottom right
                this[7] = height
            })
            position(0)
        }

    internal fun setSurfaceTexture(surfaceTexture: SurfaceTexture) {
        mSurfaceTexture = surfaceTexture
    }

    fun draw(
        mvpMatrix: FloatArray,
        width: Float,
        height: Float
    ) {
        if (mIsReleased) {
            Log.w(TAG, "Attempt to render when TextureRenderer has been released")
            return
        }

        val textureSource = mSurfaceTexture
        if (textureSource == null) {
            Log.w(TAG, "Attempt to render without texture source")
            return
        }

        GLES20.glUseProgram(mProgram)
        textureSource.updateTexImage()

        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR)

        GLES20.glUniform1i(mTextureUniformHandle, 0)

        GLES20.glUniformMatrix4fv(
            mViewProjectionMatrixHandle,
            1,
            false,
            mvpMatrix,
            0)

        GLES20.glUniformMatrix4fv(
            mTextureTransformHandle,
            1,
            false,
            mTextureTransform.apply {
                textureSource.getTransformMatrix(this)
            },
            0
        )

        GLES20.glVertexAttribPointer(
            mQuadPositionHandle,
            CoordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            VertexStride,
            configureQuad(width, height)
        )

        GLES20.glVertexAttribPointer(
            mTexPositionHandle,
            CoordsPerVertex,
            GLES20.GL_FLOAT,
            false,
            VertexStride,
            TextureCoordinatesBuffer
        )

        GLES20.glEnableVertexAttribArray(mQuadPositionHandle)
        GLES20.glEnableVertexAttribArray(mTexPositionHandle)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            DrawOrder.size,
            GLES20.GL_UNSIGNED_SHORT,
            DrawOrderBuffer
        )

        GLES20.glDisableVertexAttribArray(mQuadPositionHandle)
        GLES20.glDisableVertexAttribArray(mTexPositionHandle)
    }

    companion object {

        private val TAG = "TextureRenderer"

        internal const val uVPMatrix = "uVPMatrix"
        internal const val tVPMatrix = "tVPMatrix"
        internal const val aPosition = "aPosition"
        internal const val aTexCoord = "aTexCoord"

        private const val vTexCoord = "vTexCoord"
        internal const val uTexture = "uTexture"

        internal const val VertexShader =
            """
            uniform mat4 $uVPMatrix;
            uniform mat4 $tVPMatrix;
            attribute vec4 $aPosition;
            attribute vec2 $aTexCoord;
            varying vec2 $vTexCoord;

            void main(void)
            {
                gl_Position = $uVPMatrix * $aPosition;
                $vTexCoord = vec2($tVPMatrix * vec4($aTexCoord.x, 1.0 - $aTexCoord.y, 1.0, 1.0));
            }
            """

        internal const val FragmentShader =
            """
            #extension GL_OES_EGL_image_external : require
            precision highp float;

            uniform samplerExternalOES $uTexture;

            varying vec2 $vTexCoord;

            void main(void){
                gl_FragColor = texture2D($uTexture, $vTexCoord);
            }
            """

        internal const val CoordsPerVertex = 2
        internal const val VertexStride = 4 * CoordsPerVertex

        private val TextureCoordinates = floatArrayOf(
            // x,    y
            0.0f, 1.0f, // top left
            0.0f, 0.0f, // bottom left
            1.0f, 0.0f, // bottom right
            1.0f, 1.0f, // top right
        )

        /**
         * FloatBuffer used to specify the texture coordinates
         */
        private val TextureCoordinatesBuffer: FloatBuffer =
            ByteBuffer.allocateDirect(TextureCoordinates.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    put(TextureCoordinates)
                    position(0)
                }
            }

        private val DrawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)

        /**
         * Convert short array to short buffer
         */
        private val DrawOrderBuffer: ShortBuffer =
            ByteBuffer.allocateDirect(DrawOrder.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    put(DrawOrder)
                    position(0)
                }
            }

        fun checkError(msg: String) {
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                Log.v(TAG, "GLError $msg: $error")
            }
        }

        internal fun loadShader(type: Int, shaderCode: String): Int =
            GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
            }
    }
}
