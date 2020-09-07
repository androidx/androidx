/*
 * Copyright 2020 The Android Open Source Project
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

import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationsHolder
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Gles2Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceHost
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyleManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/** Expected frame rate in interactive mode.  */
private const val FPS: Long = 60

/** Z distance from the camera to the watchface.  */
private const val EYE_Z = -2.3f

/** How long each frame is displayed at expected frame rate.  */
private const val FRAME_PERIOD_MS: Long = 1000 / FPS

/** Cycle time before the camera motion repeats.  */
private const val CYCLE_PERIOD_SECONDS: Long = 5

/** Number of camera angles to precompute. */
private const val numCameraAngles = (CYCLE_PERIOD_SECONDS * FPS).toInt()

/**
 * Sample watch face using OpenGL. The watch face is rendered using
 * {@link Gles2ColoredTriangleList}s. The camera moves around in interactive mode and stops moving
 * when the watch enters ambient mode.
 */
class ExampleOpenGLWatchFaceService : WatchFaceService() {
    override fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchFaceHost: WatchFaceHost,
        watchState: WatchState
    ): WatchFace {
        val colorStyleCategory = ListUserStyleCategory(
            "color_style_category",
            "Colors",
            "Watchface colorization",
            icon = null,
            options = listOf(
                ListUserStyleCategory.ListOption(
                    "red_style",
                    "Red",
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListUserStyleCategory.ListOption(
                    "green_style",
                    "Green",
                    Icon.createWithResource(this, R.drawable.green_style)
                )
            )
        )
        val styleManager = UserStyleManager(listOf(colorStyleCategory))
        val complicationSlots = ComplicationsHolder(emptyList())
        val renderer = ExampleOpenGLRenderer(
            surfaceHolder,
            styleManager,
            watchState,
            colorStyleCategory
        )
        return WatchFace.Builder(
            WatchFaceType.ANALOG,
            FRAME_PERIOD_MS,
            styleManager,
            complicationSlots,
            renderer,
            watchFaceHost,
            watchState
        ).setStatusBarGravity(Gravity.RIGHT or Gravity.TOP).build()
    }
}

class ExampleOpenGLRenderer(
    surfaceHolder: SurfaceHolder,
    private val userStyleManager: UserStyleManager,
    watchState: WatchState,
    private val colorStyleCategory: ListUserStyleCategory
) : Gles2Renderer(surfaceHolder, userStyleManager, watchState) {

    /** Projection transformation matrix. Converts from 3D to 2D.  */
    private val projectionMatrix = FloatArray(16)

    /**
     * View transformation matrices to use in interactive mode. Converts from world to camera-
     * relative coordinates. One matrix per camera position.
     */
    private val viewMatrices = Array(numCameraAngles) { FloatArray(16) }

    /** The view transformation matrix to use in ambient mode  */
    private val ambientViewMatrix = FloatArray(16)

    /**
     * Model transformation matrices. Converts from model-relative coordinates to world
     * coordinates. One matrix per degree of rotation.
     */
    private val modelMatrices = Array(360) { FloatArray(16) }

    /**
     * Products of [.mViewMatrices] and [.mProjectionMatrix]. One matrix per camera
     * position.
     */
    private val vpMatrices = Array(numCameraAngles) { FloatArray(16) }

    /** The product of [.mAmbientViewMatrix] and [.mProjectionMatrix]  */
    private val ambientVpMatrix = FloatArray(16)

    /**
     * Product of [.mModelMatrices], [.mViewMatrices], and
     * [.mProjectionMatrix].
     */
    private val mvpMatrix = FloatArray(16)

    /** Triangles for the 4 major ticks. These are grouped together to speed up rendering.  */
    private lateinit var majorTickTriangles: Gles2ColoredTriangleList

    /** Triangles for the 8 minor ticks. These are grouped together to speed up rendering.  */
    private lateinit var minorTickTriangles: Gles2ColoredTriangleList

    /** Triangle for the second hand.  */
    private lateinit var secondHandTriangleMap: Map<String, Gles2ColoredTriangleList>

    /** Triangle for the minute hand.  */
    private lateinit var minuteHandTriangle: Gles2ColoredTriangleList

    /** Triangle for the hour hand.  */
    private lateinit var hourHandTriangle: Gles2ColoredTriangleList

    override fun onGlContextCreated() {
        // Create program for drawing triangles.
        val triangleProgram = Gles2ColoredTriangleList.Program()

        // We only draw triangles which all use the same program so we don't need to switch
        // programs mid-frame. This means we can tell OpenGL to use this program only once
        // rather than having to do so for each frame. This makes OpenGL draw faster.
        triangleProgram.use()

        // Create triangles for the ticks.
        majorTickTriangles = createMajorTicks(triangleProgram)
        minorTickTriangles = createMinorTicks(triangleProgram)

        // Create triangles for the hands.
        secondHandTriangleMap = mapOf(
            "red_style" to
                    createHand(
                        triangleProgram,
                        0.02f /* width */,
                        1.0f /* height */, floatArrayOf(
                            1.0f /* red */,
                            0.0f /* green */,
                            0.0f /* blue */,
                            1.0f /* alpha */
                        )
                    ),
            "greenstyle" to
                    createHand(
                        triangleProgram,
                        0.02f /* width */,
                        1.0f /* height */, floatArrayOf(
                            0.0f /* red */,
                            1.0f /* green */,
                            0.0f /* blue */,
                            1.0f /* alpha */
                        )
                    )
        )
        minuteHandTriangle = createHand(
            triangleProgram,
            0.06f /* width */,
            1f /* height */, floatArrayOf(
                0.7f /* red */,
                0.7f /* green */,
                0.7f /* blue */,
                1.0f /* alpha */
            )
        )
        hourHandTriangle = createHand(
            triangleProgram,
            0.1f /* width */,
            0.6f /* height */, floatArrayOf(
                0.9f /* red */,
                0.9f /* green */,
                0.9f /* blue */,
                1.0f /* alpha */
            )
        )

        // Precompute the clock angles.
        for (i in modelMatrices.indices) {
            Matrix.setRotateM(modelMatrices[i], 0, i.toFloat(), 0f, 0f, 1f)
        }

        // Precompute the camera angles.
        for (i in 0 until numCameraAngles) {
            // Set the camera position (View matrix). When active, move the eye around to show
            // off that this is 3D.
            val cameraAngle =
                (i.toFloat() / numCameraAngles * 2 * Math.PI).toFloat()
            val eyeX = Math.cos(cameraAngle.toDouble()).toFloat()
            val eyeY = Math.sin(cameraAngle.toDouble()).toFloat()
            Matrix.setLookAtM(
                viewMatrices[i],
                0,
                eyeX,
                eyeY,
                EYE_Z,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f
            ) // up vector
        }
        Matrix.setLookAtM(
            ambientViewMatrix,
            0,
            0f,
            0f,
            EYE_Z,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f
        ) // up vector
    }

    override fun onGlSurfaceCreated(width: Int, height: Int) {
        // Update the projection matrix based on the new aspect ratio.
        val aspectRatio = width.toFloat() / height
        Matrix.frustumM(
            projectionMatrix,
            0 /* offset */,
            -aspectRatio /* left */,
            aspectRatio /* right */,
            -1f /* bottom */,
            1f /* top */,
            2f /* near */,
            7f /* far */
        )

        // Precompute the products of Projection and View matrices for each camera angle.
        for (i in 0 until numCameraAngles) {
            Matrix.multiplyMM(vpMatrices[i], 0, projectionMatrix, 0, viewMatrices[i], 0)
        }
        Matrix.multiplyMM(ambientVpMatrix, 0, projectionMatrix, 0, ambientViewMatrix, 0)
    }

    /**
     * Creates a triangle for a hand on the watch face.
     *
     * @param program program for drawing triangles
     * @param width width of base of triangle
     * @param length length of triangle
     * @param color color in RGBA order, each in the range [0, 1]
     */
    private fun createHand(
        program: Gles2ColoredTriangleList.Program,
        width: Float,
        length: Float,
        color: FloatArray
    ) = Gles2ColoredTriangleList(
        program,
        floatArrayOf(
            0f,
            length,
            0f,
            -width / 2,
            0f,
            0f,
            width / 2,
            0f,
            0f
        ),
        color
    )

    /**
     * Creates a triangle list for the major ticks on the watch face.
     *
     * @param program program for drawing triangles
     */
    private fun createMajorTicks(
        program: Gles2ColoredTriangleList.Program
    ): Gles2ColoredTriangleList {
        // Create the data for the VBO.
        val trianglesCoords = FloatArray(9 * 4)
        for (i in 0..3) {
            val triangleCoords = getMajorTickTriangleCoords(i)
            System.arraycopy(
                triangleCoords,
                0,
                trianglesCoords,
                i * 9,
                triangleCoords.size
            )
        }
        return Gles2ColoredTriangleList(
            program, trianglesCoords, floatArrayOf(
                1.0f /* red */,
                1.0f /* green */,
                1.0f /* blue */,
                1.0f /* alpha */
            )
        )
    }

    /**
     * Creates a triangle list for the minor ticks on the watch face.
     *
     * @param program program for drawing triangles
     */
    private fun createMinorTicks(
        program: Gles2ColoredTriangleList.Program
    ): Gles2ColoredTriangleList {
        // Create the data for the VBO.
        val trianglesCoords = FloatArray(9 * (12 - 4))
        var index = 0
        for (i in 0..11) {
            if (i % 3 == 0) {
                // This is where a major tick goes, so skip it.
                continue
            }
            val triangleCoords = getMinorTickTriangleCoords(i)
            System.arraycopy(
                triangleCoords,
                0,
                trianglesCoords,
                index,
                triangleCoords.size
            )
            index += 9
        }
        return Gles2ColoredTriangleList(
            program, trianglesCoords, floatArrayOf(
                0.5f /* red */,
                0.5f /* green */,
                0.5f /* blue */,
                1.0f /* alpha */
            )
        )
    }

    private fun getMajorTickTriangleCoords(index: Int): FloatArray {
        return getTickTriangleCoords(
            0.03f /* width */, 0.09f /* length */,
            index * 360 / 4 /* angleDegrees */
        )
    }

    private fun getMinorTickTriangleCoords(index: Int): FloatArray {
        return getTickTriangleCoords(
            0.02f /* width */, 0.06f /* length */,
            index * 360 / 12 /* angleDegrees */
        )
    }

    private fun getTickTriangleCoords(
        width: Float,
        length: Float,
        angleDegrees: Int
    ): FloatArray {
        // Create the data for the VBO.
        val coords = floatArrayOf(
            0f,
            1f,
            0f,
            width / 2,
            length + 1,
            0f,
            -width / 2,
            length + 1,
            0f
        )
        rotateCoords(coords, angleDegrees)
        return coords
    }

    /**
     * Destructively rotates the given coordinates in the XY plane about the origin by the given
     * angle.
     *
     * @param coords flattened 3D coordinates
     * @param angleDegrees angle in degrees clockwise when viewed from negative infinity on the
     * Z axis
     */
    private fun rotateCoords(
        coords: FloatArray,
        angleDegrees: Int
    ) {
        val angleRadians = Math.toRadians(angleDegrees.toDouble())
        val cos = cos(angleRadians)
        val sin = sin(angleRadians)
        var i = 0
        while (i < coords.size) {
            val x = coords[i]
            val y = coords[i + 1]
            coords[i] = (cos * x - sin * y).toFloat()
            coords[i + 1] = (sin * x + cos * y).toFloat()
            i += 3
        }
    }

    override fun onDraw(calendar: Calendar) {
        // Draw background color and select the appropriate view projection matrix. The background
        // should always be black in ambient mode. The view projection matrix used is overhead in
        // ambient. In interactive mode, it's tilted depending on the current time.
        val vpMatrix = if (drawMode == DrawMode.AMBIENT) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            ambientVpMatrix
        } else {
            when (userStyleManager.userStyle[colorStyleCategory]!!.id) {
                "red_style" -> GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1f)
                "green_style" -> GLES20.glClearColor(0.2f, 0.5f, 0.2f, 1f)
            }
            val cameraIndex = (calendar.timeInMillis / FRAME_PERIOD_MS % numCameraAngles).toInt()
            vpMatrices[cameraIndex]
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Compute angle indices for the three hands.
        val seconds = calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f
        val minutes = calendar.get(Calendar.MINUTE) + seconds / 60f
        val hours = calendar.get(Calendar.HOUR) + minutes / 60f
        val secIndex = (seconds / 60f * 360f).toInt()
        val minIndex = (minutes / 60f * 360f).toInt()
        val hoursIndex = (hours / 12f * 360f).toInt()

        Matrix.multiplyMM(
            mvpMatrix,
            0,
            vpMatrix,
            0,
            modelMatrices[hoursIndex],
            0
        )
        hourHandTriangle.draw(mvpMatrix)

        Matrix.multiplyMM(
            mvpMatrix,
            0,
            vpMatrix,
            0,
            modelMatrices[minIndex],
            0
        )
        minuteHandTriangle.draw(mvpMatrix)

        if (drawMode != DrawMode.AMBIENT) {
            Matrix.multiplyMM(
                mvpMatrix,
                0,
                vpMatrix,
                0,
                modelMatrices[secIndex],
                0
            )
            secondHandTriangleMap[userStyleManager.userStyle[colorStyleCategory]!!.id]
                ?.draw(mvpMatrix)
        }

        majorTickTriangles.draw(vpMatrix)
        minorTickTriangles.draw(vpMatrix)
    }
}

/**
 * A list of triangles drawn in a single solid color using OpenGL ES 2.0.
 */
class Gles2ColoredTriangleList(
    private val program: Program,
    triangleCoords: FloatArray,
    private val color: FloatArray
) {
    init {
        require(triangleCoords.size % (VERTICE_PER_TRIANGLE * COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICE_PER_TRIANGLE * COORDS_PER_VERTEX coordinates")
        }
        require(color.size == NUM_COLOR_COMPONENTS) { "wrong number of color components" }
    }

    /** The VBO containing the vertex coordinates.  */
    private val vertexBuffer =
        ByteBuffer.allocateDirect(triangleCoords.size * BYTES_PER_FLOAT)
            .apply { order(ByteOrder.nativeOrder()) }
            .asFloatBuffer().apply {
                put(triangleCoords)
                position(0)
            }

    /** Number of coordinates in this triangle list.  */
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
        if (CHECK_GL_ERRORS) checkGlError(
            "glDrawArrays"
        )
    }

    /** OpenGL shaders for drawing solid colored triangle lists.  */
    class Program {
        /** ID OpenGL uses to identify this program.  */
        private val programId: Int

        /** Handle for uMvpMatrix uniform in vertex shader.  */
        private val mvpMatrixHandle: Int

        /** Handle for aPosition attribute in vertex shader.  */
        private val positionHandle: Int

        /** Handle for uColor uniform in fragment shader.  */
        private val colorHandle: Int

        companion object {
            /** Trivial vertex shader that transforms the input vertex by the MVP matrix.  */
            private const val VERTEX_SHADER_CODE = "" +
                    "uniform mat4 uMvpMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMvpMatrix * aPosition;\n" +
                    "}\n"

            /** Trivial fragment shader that draws with a fixed color.  */
            private const val FRAGMENT_SHADER_CODE = "" +
                    "precision mediump float;\n" +
                    "uniform vec4 uColor;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = uColor;\n" +
                    "}\n"
        }

        /**
         * Tells OpenGL to use this program. Call this method before drawing a sequence of
         * triangle lists.
         */
        fun use() {
            GLES20.glUseProgram(programId)
            if (CHECK_GL_ERRORS) {
                checkGlError("glUseProgram")
            }
        }

        /** Sends the given MVP matrix, vertex data, and color to OpenGL.  */
        fun bind(
            mvpMatrix: FloatArray?,
            vertexBuffer: FloatBuffer?,
            color: FloatArray?
        ) {
            // Pass the MVP matrix to OpenGL.
            GLES20.glUniformMatrix4fv(
                mvpMatrixHandle, 1 /* count */, false /* transpose */,
                mvpMatrix, 0 /* offset */
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

            // Enable vertex array (VBO).
            GLES20.glEnableVertexAttribArray(positionHandle)
            if (CHECK_GL_ERRORS) {
                checkGlError("glEnableVertexAttribArray")
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

        /** Whether to check for GL errors. This is slow, so not appropriate for production builds.  */
        private const val CHECK_GL_ERRORS = false

        /** Number of coordinates per vertex in this array: one for each of x, y, and z.  */
        private const val COORDS_PER_VERTEX = 3

        /** Number of bytes to store a float in GL.  */
        const val BYTES_PER_FLOAT = 4

        /** Number of bytes per vertex.  */
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * BYTES_PER_FLOAT

        /** Triangles have three vertices.  */
        private const val VERTICE_PER_TRIANGLE = 3

        /**
         * Number of components in an OpenGL color. The components are:
         *  1. red
         *  1. green
         *  1. blue
         *  1. alpha
         *
         */
        private const val NUM_COLOR_COMPONENTS = 4

        /**
         * Checks if any of the GL calls since the last time this method was called set an error
         * condition. Call this method immediately after calling a GL method. Pass the name of the GL
         * operation. For example:
         *
         * <pre>
         * mColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
         * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
         *
         * If the operation is not successful, the check throws an exception.
         *
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