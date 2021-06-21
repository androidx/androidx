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

import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.opengl.GLES20
import android.opengl.GLU
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import android.view.Gravity
import android.view.SurfaceHolder
import androidx.wear.complications.ComplicationSlotBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.GlesTextureComplication
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
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

const val EXAMPLE_OPENGL_COMPLICATION_ID = 101

/**
 * Sample watch face using OpenGL. The watch face is rendered using
 * [Gles2ColoredTriangleList]s. The camera moves around in interactive mode and stops moving
 * when the watch enters ambient mode.
 *
 * NB this is open for testing.
 */
open class ExampleOpenGLWatchFaceService : WatchFaceService() {
    // Lazy because the context isn't initialized til later.
    private val watchFaceStyle by lazy {
        WatchFaceColorStyle.create(this, "white_style")
    }

    private val colorStyleSetting by lazy {
        ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            "Colors",
            "Watchface colorization",
            icon = null,
            options = listOf(
                ListUserStyleSetting.ListOption(
                    Option.Id("red_style"),
                    "Red",
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListUserStyleSetting.ListOption(
                    Option.Id("green_style"),
                    "Green",
                    Icon.createWithResource(this, R.drawable.green_style)
                )
            ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
    }

    private val complication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
        EXAMPLE_OPENGL_COMPLICATION_ID,
        { watchState, listener ->
            CanvasComplicationDrawable(
                watchFaceStyle.getDrawable(this@ExampleOpenGLWatchFaceService)!!,
                watchState,
                listener
            )
        },
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        ),
        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DAY_OF_WEEK),
        ComplicationSlotBounds(RectF(0.2f, 0.7f, 0.4f, 0.9f))
    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
        .build()

    public override fun createUserStyleSchema() = UserStyleSchema(listOf(colorStyleSetting))

    public override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = ComplicationSlotsManager(listOf(complication), currentUserStyleRepository)

    public override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.ANALOG,
        ExampleOpenGLRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            colorStyleSetting,
            complication
        )
    ).setLegacyWatchFaceStyle(
        WatchFace.LegacyWatchFaceOverlayStyle(
            0,
            Gravity.RIGHT or Gravity.TOP,
            true
        )
    )
}

class ExampleOpenGLRenderer(
    surfaceHolder: SurfaceHolder,
    private val currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val colorStyleSetting: ListUserStyleSetting,
    private val complicationSlot: ComplicationSlot
) : Renderer.GlesRenderer(surfaceHolder, currentUserStyleRepository, watchState, FRAME_PERIOD_MS) {

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
     * Products of [viewMatrices] and [projectionMatrix]. One matrix per camera
     * position.
     */
    private val vpMatrices = Array(numCameraAngles) { FloatArray(16) }

    /** The product of [ambientViewMatrix] and [projectionMatrix]  */
    private val ambientVpMatrix = FloatArray(16)

    /**
     * Product of [modelMatrices], [viewMatrices], and
     * [projectionMatrix].
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

    private lateinit var complicationTexture: GlesTextureComplication

    private lateinit var coloredTriangleProgram: Gles2ColoredTriangleList.Program
    private lateinit var textureTriangleProgram: Gles2TexturedTriangleList.Program

    private lateinit var complicationTriangles: Gles2TexturedTriangleList
    private lateinit var complicationHighlightTriangles: Gles2ColoredTriangleList

    override fun onBackgroundThreadGlContextCreated() {
        // Create program for drawing triangles.
        coloredTriangleProgram = Gles2ColoredTriangleList.Program()

        // Create triangles for the ticks.
        majorTickTriangles = createMajorTicks(coloredTriangleProgram)
        minorTickTriangles = createMinorTicks(coloredTriangleProgram)

        // Create program for drawing triangles.
        textureTriangleProgram = Gles2TexturedTriangleList.Program()
        complicationTriangles = createComplicationQuad(
            textureTriangleProgram,
            -0.9f,
            -0.1f,
            0.6f,
            0.6f
        )

        complicationHighlightTriangles = createComplicationHighlightQuad(
            coloredTriangleProgram,
            -0.9f,
            -0.1f,
            0.6f,
            0.6f
        )

        // Create triangles for the hands.
        secondHandTriangleMap = mapOf(
            "red_style" to
                createHand(
                    coloredTriangleProgram,
                    0.02f /* width */,
                    1.0f /* height */,
                    floatArrayOf(
                        1.0f /* red */,
                        0.0f /* green */,
                        0.0f /* blue */,
                        1.0f /* alpha */
                    )
                ),
            "greenstyle" to
                createHand(
                    coloredTriangleProgram,
                    0.02f /* width */,
                    1.0f /* height */,
                    floatArrayOf(
                        0.0f /* red */,
                        1.0f /* green */,
                        0.0f /* blue */,
                        1.0f /* alpha */
                    )
                )
        )
        minuteHandTriangle = createHand(
            coloredTriangleProgram,
            0.06f /* width */,
            1f /* height */,
            floatArrayOf(
                0.7f /* red */,
                0.7f /* green */,
                0.7f /* blue */,
                1.0f /* alpha */
            )
        )
        hourHandTriangle = createHand(
            coloredTriangleProgram,
            0.1f /* width */,
            0.6f /* height */,
            floatArrayOf(
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

        complicationTexture = GlesTextureComplication(
            complicationSlot.renderer,
            128,
            128,
            GLES20.GL_TEXTURE_2D
        )
    }

    override fun onUiThreadGlSurfaceCreated(width: Int, height: Int) {
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
            program, trianglesCoords,
            floatArrayOf(
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
            program, trianglesCoords,
            floatArrayOf(
                0.5f /* red */,
                0.5f /* green */,
                0.5f /* blue */,
                1.0f /* alpha */
            )
        )
    }

    /**
     * Creates a triangle list for the complication.
     */
    private fun createComplicationQuad(
        program: Gles2TexturedTriangleList.Program,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) = Gles2TexturedTriangleList(
        program,
        floatArrayOf(
            top + 0.0f,
            left + 0.0f,
            0.0f,

            top + 0.0f,
            left + width,
            0.0f,

            top + height,
            left + 0.0f,
            0.0f,

            top + 0.0f,
            left + width,
            0.0f,

            top + height,
            left + 0.0f,
            0.0f,

            top + height,
            left + width,
            0.0f
        ),
        floatArrayOf(
            1.0f,
            1.0f,

            1.0f,
            0.0f,

            0.0f,
            1.0f,

            1.0f,
            0.0f,

            0.0f,
            1.0f,

            0.0f,
            0.0f
        )
    )

    /**
     * Creates a triangle list for the complication highlight quad.
     */
    private fun createComplicationHighlightQuad(
        program: Gles2ColoredTriangleList.Program,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) = Gles2ColoredTriangleList(
        program,
        floatArrayOf(
            top + 0.0f,
            left + 0.0f,
            0.0f,

            top + 0.0f,
            left + width,
            0.0f,

            top + height,
            left + 0.0f,
            0.0f,

            top + 0.0f,
            left + width,
            0.0f,

            top + height,
            left + 0.0f,
            0.0f,

            top + height,
            left + width,
            0.0f
        ),
        floatArrayOf(
            1.0f /* red */,
            1.0f /* green */,
            1.0f /* blue */,
            0.0f /* alpha */
        )
    )

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

    override fun render(calendar: Calendar) {
        // Draw background color and select the appropriate view projection matrix. The background
        // should always be black in ambient mode. The view projection matrix used is overhead in
        // ambient. In interactive mode, it's tilted depending on the current time.
        val vpMatrix = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            GLES20.glClearColor(0f, 0f, 0f, 1f)
            ambientVpMatrix
        } else {
            when (currentUserStyleRepository.userStyle[colorStyleSetting]!!.toString()) {
                "red_style" -> GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1f)
                "green_style" -> GLES20.glClearColor(0.2f, 0.5f, 0.2f, 1f)
            }
            val cameraIndex = (calendar.timeInMillis / FRAME_PERIOD_MS % numCameraAngles).toInt()
            vpMatrices[cameraIndex]
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Draw the complication first.
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS)) {
            complicationTexture.renderToTexture(calendar, renderParameters)

            textureTriangleProgram.bindProgramAndAttribs()
            complicationTexture.bind()

            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
            complicationTriangles.draw(vpMatrix)
            textureTriangleProgram.unbindAttribs()

            GLES20.glDisable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
            coloredTriangleProgram.bindProgramAndAttribs()
        }

        // Compute angle indices for the three hands.
        val seconds = calendar.get(Calendar.SECOND) + calendar.get(Calendar.MILLISECOND) / 1000f
        val minutes = calendar.get(Calendar.MINUTE) + seconds / 60f
        val hours = calendar.get(Calendar.HOUR) + minutes / 60f
        val secIndex = (seconds / 60f * 360f).toInt()
        val minIndex = (minutes / 60f * 360f).toInt()
        val hoursIndex = (hours / 12f * 360f).toInt()

        // Render hands.
        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        ) {
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

            if (renderParameters.drawMode != DrawMode.AMBIENT) {
                Matrix.multiplyMM(
                    mvpMatrix,
                    0,
                    vpMatrix,
                    0,
                    modelMatrices[secIndex],
                    0
                )
                secondHandTriangleMap[
                    currentUserStyleRepository.userStyle[colorStyleSetting]!!.toString()
                ]?.draw(mvpMatrix)
            }
        }

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            majorTickTriangles.draw(vpMatrix)
            minorTickTriangles.draw(vpMatrix)
            coloredTriangleProgram.unbindAttribs()
        }
    }

    override fun renderHighlightLayer(calendar: Calendar) {
        val cameraIndex = (calendar.timeInMillis / FRAME_PERIOD_MS % numCameraAngles).toInt()
        val vpMatrix = vpMatrices[cameraIndex]

        val highlightLayer = renderParameters.highlightLayer!!
        GLES20.glClearColor(
            Color.red(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.green(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.blue(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.alpha(highlightLayer.backgroundTint).toFloat() / 256.0f
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS)) {
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)
            complicationHighlightTriangles.draw(vpMatrix)
            coloredTriangleProgram.unbindAttribs()
        }
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
        require(triangleCoords.size % (VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX) == 0) {
            ("must be multiple of VERTICES_PER_TRIANGLE * COORDS_PER_VERTEX coordinates")
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
        private const val VERTICES_PER_TRIANGLE = 3

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

/**
 * A list of triangles drawn with a texture using OpenGL ES 2.0.
 */
class Gles2TexturedTriangleList(
    private val program: Program,
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
     *
     * @param mvpMatrix the Model View Project matrix to draw this triangle list
     */
    fun draw(mvpMatrix: FloatArray?) {
        // Pass the MVP matrix, vertex data, and color to OpenGL.
        program.bind(mvpMatrix, vertexBuffer, textureCoordsBuffer)

        // Draw the triangle list.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numCoords)
        if (CHECK_GL_ERRORS) checkGlError(
            "glDrawArrays"
        )
    }

    /** OpenGL shaders for drawing textured triangle lists.  */
    class Program {
        /** ID OpenGL uses to identify this program.  */
        private val programId: Int

        /** Handle for uMvpMatrix uniform in vertex shader.  */
        private val mvpMatrixHandle: Int

        /** Handle for aPosition attribute in vertex shader.  */
        private val positionHandle: Int

        /** Handle for aTextureCoordinate uniform in fragment shader.  */
        private val textureCoordinateHandle: Int

        companion object {
            /** Trivial vertex shader that transforms the input vertex by the MVP matrix.  */
            private const val VERTEX_SHADER_CODE = "" +
                "uniform mat4 uMvpMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoordinate;\n" +
                "varying vec2 textureCoordinate;\n" +
                "void main() {\n" +
                "    gl_Position = uMvpMatrix * aPosition;\n" +
                "    textureCoordinate = aTextureCoordinate.xy;" +
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
            mvpMatrix: FloatArray?,
            vertexBuffer: FloatBuffer?,
            textureCoordinatesBuffer: FloatBuffer?
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
            textureCoordinateHandle =
                GLES20.glGetAttribLocation(programId, "aTextureCoordinate")
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
