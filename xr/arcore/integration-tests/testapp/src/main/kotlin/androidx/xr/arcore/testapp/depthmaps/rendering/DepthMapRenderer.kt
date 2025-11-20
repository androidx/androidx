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

package androidx.xr.arcore.testapp.depthmaps.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.GL_CLAMP_TO_EDGE
import android.opengl.GLES20.GL_LINEAR
import android.opengl.GLES20.GL_TEXTURE_2D
import android.opengl.GLES20.GL_TEXTURE_MAG_FILTER
import android.opengl.GLES20.GL_TEXTURE_MIN_FILTER
import android.opengl.GLES20.GL_TEXTURE_WRAP_S
import android.opengl.GLES20.GL_TEXTURE_WRAP_T
import android.opengl.GLES20.glBindTexture
import android.opengl.GLES20.glGenTextures
import android.opengl.GLES20.glTexParameteri
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class DepthMapRenderer {

    private var depthProgram: Int = 0
    private var depthTextureParam: Int = 0
    private var gradientTextureParam: Int = 0
    private var depthTextureId: Int = -1

    private var gradientTextureId: Int = -1
    private var depthQuadPositionParam: Int = 0
    private var depthQuadTexCoordParam: Int = 0
    private var textureId: Int = -1

    private var depthTextureWidth: Int = -1
    private var depthTextureHeight: Int = -1

    private lateinit var quadCoords: FloatBuffer
    private lateinit var quadTexCoords: FloatBuffer

    init {
        val numVertices: Int = 4
        if (numVertices != QUAD_COORDS.size / COORDS_PER_VERTEX) {
            throw RuntimeException("Unexpected number of quad vertices in DepthMapRenderer.")
        }

        if (numVertices != QUAD_TEX_COORDS.size / TEXCOORDS_PER_VERTEX) {
            throw RuntimeException(
                "Unexpected number of quad texture vertices in DepthMapRenderer."
            )
        }

        val bbQuadCoords: ByteBuffer = ByteBuffer.allocateDirect(QUAD_COORDS.size * FLOAT_SIZE)
        bbQuadCoords.order(ByteOrder.nativeOrder())
        quadCoords = bbQuadCoords.asFloatBuffer()
        quadCoords.put(QUAD_COORDS)
        quadCoords.position(0)

        val bbQuadTexCoords: ByteBuffer =
            ByteBuffer.allocateDirect(QUAD_TEX_COORDS.size * FLOAT_SIZE)
        bbQuadTexCoords.order(ByteOrder.nativeOrder())
        quadTexCoords = bbQuadTexCoords.asFloatBuffer()
        quadTexCoords.put(QUAD_TEX_COORDS)
        quadCoords.position(0)
    }

    public fun createDepthShaders(context: Context, depthTextureId: Int) {
        val vertexShader: Int =
            loadGLShader(TAG, context, GLES30.GL_VERTEX_SHADER, DEPTH_VERTEX_SHADER_NAME)
        val fragmentShader: Int =
            loadGLShader(TAG, context, GLES30.GL_FRAGMENT_SHADER, DEPTH_FRAGMENT_SHADER_NAME)

        depthProgram = GLES30.glCreateProgram()
        GLES30.glAttachShader(depthProgram, vertexShader)
        GLES30.glAttachShader(depthProgram, fragmentShader)
        GLES30.glLinkProgram(depthProgram)
        GLES30.glUseProgram(depthProgram)
        checkGLError(TAG, "Program creation")

        depthTextureParam = GLES30.glGetUniformLocation(depthProgram, "u_DepthTexture")
        gradientTextureParam = GLES30.glGetUniformLocation(depthProgram, "u_GradientTexture")
        checkGLError(TAG, "Program parameters")

        depthQuadPositionParam = GLES30.glGetAttribLocation(depthProgram, "a_Position")
        depthQuadTexCoordParam = GLES30.glGetAttribLocation(depthProgram, "a_TexCoord")
        checkGLError(TAG, "Program parameters")

        this.depthTextureId = depthTextureId
    }

    public fun drawDepth() {
        // Ensure position is rewound before use.
        quadCoords.position(0)
        quadTexCoords.position(0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, depthTextureId)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, gradientTextureId)
        GLES30.glUniform1i(depthTextureParam, 0)
        GLES30.glUniform1i(gradientTextureParam, 1)
        checkGLError(TAG, "Setting depth uniform texture")

        // Set the vertex positions and texture coordinates.
        GLES30.glVertexAttribPointer(
            depthQuadPositionParam,
            COORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            quadCoords,
        )
        GLES30.glVertexAttribPointer(
            depthQuadTexCoordParam,
            TEXCOORDS_PER_VERTEX,
            GLES30.GL_FLOAT,
            false,
            0,
            quadTexCoords,
        )
        checkGLError(TAG, "Setting vertex positions and texture coordinates")

        // Draws the quad.
        GLES30.glEnableVertexAttribArray(depthQuadPositionParam)
        GLES30.glEnableVertexAttribArray(depthQuadTexCoordParam)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glDisableVertexAttribArray(depthQuadPositionParam)
        GLES30.glDisableVertexAttribArray(depthQuadTexCoordParam)
        checkGLError(TAG, "Drawing the depth map quad")
    }

    public fun createDepthGradientTexture(context: Context) {
        val textureIds: IntArray = IntArray(1)
        glGenTextures(1, textureIds, 0)
        gradientTextureId = textureIds[0]
        glBindTexture(GL_TEXTURE_2D, gradientTextureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        var bitmap: Bitmap? = null
        try {
            bitmap =
                convertBitmapToConfig(
                    BitmapFactory.decodeStream(context.assets.open(DEPTH_GRADIENT_PNG_NAME)),
                    Bitmap.Config.ARGB_8888,
                )
            val buffer: ByteBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(buffer)
            buffer.rewind()

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, gradientTextureId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                /*level=*/ 0,
                GLES30.GL_RGBA,
                bitmap.width,
                bitmap.height,
                /*border=*/ 0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                buffer,
            )
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        } catch (t: Throwable) {
            throw t
        } finally {
            if (bitmap != null) {
                bitmap.recycle()
            }
        }
    }

    companion object {
        private const val TAG = "DepthMapRenderer"

        private const val DEPTH_VERTEX_SHADER_NAME: String = "shaders/depth_shader.vert"
        private const val DEPTH_FRAGMENT_SHADER_NAME: String = "shaders/depth_shader.frag"

        private const val DEPTH_GRADIENT_PNG_NAME: String = "models/depth_gradient.png"

        private val COORDS_PER_VERTEX: Int = 2
        private val TEXCOORDS_PER_VERTEX: Int = 2
        private val FLOAT_SIZE: Int = 4

        private val QUAD_COORDS: FloatArray =
            floatArrayOf(-1.0f, -1.0f, +1.0f, -1.0f, -1.0f, +1.0f, +1.0f, +1.0f)
        private val QUAD_TEX_COORDS: FloatArray =
            floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)

        public fun loadGLShader(tag: String, context: Context, type: Int, filename: String): Int {
            val code: String = readShaderFileFromAssets(context, filename)
            var shader: Int = GLES30.glCreateShader(type)
            GLES30.glShaderSource(shader, code)
            GLES30.glCompileShader(shader)

            // Get the compilation status.
            val compileStatus: IntArray = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

            // If the compilation failed, delete the shader.
            if (compileStatus[0] == 0) {
                Log.e(tag, "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
                GLES30.glDeleteShader(shader)
                shader = 0
            }

            if (shader == 0) {
                throw RuntimeException("Error creating shader.")
            }

            return shader
        }

        public fun checkGLError(tag: String, label: String) {
            var lastError: Int = GLES30.GL_NO_ERROR
            // Drain the queue of all errors.
            var error: Int = GLES30.glGetError()
            while (error != GLES30.GL_NO_ERROR) {
                Log.e(tag, label + ": glError " + error)
                lastError = error
                error = GLES30.glGetError()
            }
            if (lastError != GLES30.GL_NO_ERROR) {
                throw RuntimeException(label + ": glError " + lastError)
            }
        }

        private fun readShaderFileFromAssets(context: Context, filename: String): String {
            try {
                val inputStream: InputStream = context.assets.open(filename)
                val reader: BufferedReader = BufferedReader(InputStreamReader(inputStream))
                val sb: StringBuilder = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    val tokens: List<String> = line.split(" ")
                    if (tokens[0] == "#include") {
                        var includeFilename: String = tokens[1]
                        includeFilename = includeFilename.replace("\"", "")
                        if (includeFilename == filename) {
                            throw RuntimeException("Do not include the calling file.")
                        }
                        sb.append(readShaderFileFromAssets(context, includeFilename))
                    } else {
                        sb.append(line).append("\n")
                    }
                    line = reader.readLine()
                }
                return sb.toString()
            } catch (e: Exception) {
                throw RuntimeException("Failed to read shader file.")
            }
        }

        fun convertBitmapToConfig(bitmap: Bitmap, config: Bitmap.Config): Bitmap {
            // We use this method instead of BitmapFactory.Options.outConfig to support a minimum of
            // Android API level 24.
            if (bitmap.config == config) {
                return bitmap
            }
            val result = bitmap.copy(config, /* isMutable= */ false)
            bitmap.recycle()
            return result
        }
    }
}
