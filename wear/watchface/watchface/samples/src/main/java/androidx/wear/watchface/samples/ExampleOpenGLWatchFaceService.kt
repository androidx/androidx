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

import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.opengl.EGL14
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.Gravity
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceColors
import androidx.wear.watchface.WatchFaceExperimental
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationDeniedActivity
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationRationalActivity
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.complications.rendering.GlesTextureComplication
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Sample watch face using OpenGL. The watch face is rendered using [Gles2ColoredTriangleList]s. The
 * camera moves around in interactive mode and stops moving when the watch enters ambient mode.
 *
 * NB this is open for testing.
 */
open class ExampleOpenGLWatchFaceService : SampleWatchFaceService() {
    // Lazy because the context isn't initialized till later.
    private val watchFaceStyle by lazy { WatchFaceColorStyle.create(this, "white_style") }

    private val colorStyleSetting by lazy {
        ListUserStyleSetting(
            UserStyleSetting.Id("color_style_setting"),
            resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options =
                listOf(
                    ListUserStyleSetting.ListOption(
                        Option.Id("red_style"),
                        resources,
                        R.string.colors_style_red,
                        R.string.colors_style_red_screen_reader,
                        { Icon.createWithResource(this, R.drawable.red_style) }
                    ),
                    ListUserStyleSetting.ListOption(
                        Option.Id("green_style"),
                        resources,
                        R.string.colors_style_green,
                        R.string.colors_style_green_screen_reader,
                        { Icon.createWithResource(this, R.drawable.green_style) }
                    )
                ),
            listOf(WatchFaceLayer.BASE, WatchFaceLayer.COMPLICATIONS_OVERLAY)
        )
    }

    @OptIn(ComplicationExperimental::class)
    private val complication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
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
                    ComplicationType.GOAL_PROGRESS,
                    ComplicationType.WEIGHTED_ELEMENTS,
                    ComplicationType.LONG_TEXT,
                    ComplicationType.SHORT_TEXT,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    ComplicationType.SMALL_IMAGE
                ),
                DefaultComplicationDataSourcePolicy(
                    SystemDataSources.DATA_SOURCE_DAY_OF_WEEK,
                    ComplicationType.SHORT_TEXT
                ),
                ComplicationSlotBounds(RectF(0.2f, 0.7f, 0.4f, 0.9f))
            )
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
    ) =
        WatchFace(
                WatchFaceType.ANALOG,
                ExampleOpenGLRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    colorStyleSetting,
                    complication
                )
            )
            .setLegacyWatchFaceStyle(
                WatchFace.LegacyWatchFaceOverlayStyle(0, Gravity.RIGHT or Gravity.TOP, true)
            )
            .setComplicationDeniedDialogIntent(Intent(this, ComplicationDeniedActivity::class.java))
            .setComplicationRationaleDialogIntent(
                Intent(this, ComplicationRationalActivity::class.java)
            )

    class ExampleSharedAssets : Renderer.SharedAssets {
        override fun onDestroy() {}
    }

    @OptIn(WatchFaceExperimental::class)
    @RequiresApi(27)
    private class ExampleOpenGLRenderer(
        surfaceHolder: SurfaceHolder,
        private val currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        private val colorStyleSetting: ListUserStyleSetting,
        private val complicationSlot: ComplicationSlot
    ) :
        Renderer.GlesRenderer2<ExampleSharedAssets>(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            FRAME_PERIOD_MS,
            // Try a config with 4x MSAA if supported and if necessary fall back to one without.
            eglConfigAttribListList =
                listOf(
                    intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE,
                        EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_RED_SIZE,
                        8,
                        EGL14.EGL_GREEN_SIZE,
                        8,
                        EGL14.EGL_BLUE_SIZE,
                        8,
                        EGL14.EGL_ALPHA_SIZE,
                        8,
                        EGL14.EGL_SAMPLES, // 4x MSAA (anti-aliasing)
                        4,
                        EGL14.EGL_NONE
                    ),
                    intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE,
                        EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_RED_SIZE,
                        8,
                        EGL14.EGL_GREEN_SIZE,
                        8,
                        EGL14.EGL_BLUE_SIZE,
                        8,
                        EGL14.EGL_ALPHA_SIZE,
                        8,
                        EGL14.EGL_NONE
                    )
                ),
            eglSurfaceAttribList = intArrayOf(EGL14.EGL_NONE),
            eglContextAttribList = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        ) {

        /** Projection transformation matrix. Converts from 3D to 2D. */
        private val projectionMatrix = FloatArray(16)

        /**
         * View transformation matrices to use in interactive mode. Converts from world to camera-
         * relative coordinates. One matrix per camera position.
         */
        private val viewMatrices = Array(numCameraAngles) { FloatArray(16) }

        /** The view transformation matrix to use in ambient mode */
        private val ambientViewMatrix = FloatArray(16)

        /**
         * Model transformation matrices. Converts from model-relative coordinates to world
         * coordinates. One matrix per degree of rotation.
         */
        private val modelMatrices = Array(360) { FloatArray(16) }

        /** Products of [viewMatrices] and [projectionMatrix]. One matrix per camera position. */
        private val vpMatrices = Array(numCameraAngles) { FloatArray(16) }

        /** The product of [ambientViewMatrix] and [projectionMatrix] */
        private val ambientVpMatrix = FloatArray(16)

        /** Product of [modelMatrices], [viewMatrices], and [projectionMatrix]. */
        private val mvpMatrix = FloatArray(16)

        /** Triangles for the 4 major ticks. These are grouped together to speed up rendering. */
        private lateinit var majorTickTriangles: Gles2ColoredTriangleList

        /** Triangles for the 8 minor ticks. These are grouped together to speed up rendering. */
        private lateinit var minorTickTriangles: Gles2ColoredTriangleList

        /** Triangle for the second hand. */
        private lateinit var secondHandTriangleMap: Map<String, Gles2ColoredTriangleList>

        /** Triangle for the minute hand. */
        private lateinit var minuteHandTriangle: Gles2ColoredTriangleList

        /** Triangle for the hour hand. */
        private lateinit var hourHandTriangle: Gles2ColoredTriangleList

        private lateinit var complicationTexture: GlesTextureComplication

        private lateinit var coloredTriangleProgram: Gles2ColoredTriangleList.Program
        private lateinit var textureTriangleProgram: Gles2TexturedTriangleList.Program

        private lateinit var complicationTriangles: Gles2TexturedTriangleList
        private lateinit var complicationHighlightTriangles: Gles2ColoredTriangleList

        init {
            CoroutineScope(Dispatchers.Main.immediate).launch {
                currentUserStyleRepository.userStyle.collect { userStyle ->
                    watchfaceColors =
                        when (userStyle[colorStyleSetting]!!.toString()) {
                            "red_style" ->
                                WatchFaceColors(
                                    Color.valueOf(0.5f, 0.2f, 0.2f, 1f),
                                    Color.valueOf(0.4f, 0.15f, 0.15f, 1f),
                                    Color.valueOf(0.1f, 0.1f, 0.1f, 1f)
                                )
                            "green_style" ->
                                WatchFaceColors(
                                    Color.valueOf(0.2f, 0.5f, 0.2f, 1f),
                                    Color.valueOf(0.15f, 0.4f, 0.15f, 1f),
                                    Color.valueOf(0.1f, 0.1f, 0.1f, 1f)
                                )
                            else -> null
                        }
                }
            }
        }

        override suspend fun onBackgroundThreadGlContextCreated() {
            // Create program for drawing triangles.
            coloredTriangleProgram = Gles2ColoredTriangleList.Program()

            // Create triangles for the ticks.
            majorTickTriangles = createMajorTicks(coloredTriangleProgram)
            minorTickTriangles = createMinorTicks(coloredTriangleProgram)

            // Create program for drawing triangles.
            textureTriangleProgram = Gles2TexturedTriangleList.Program()
            complicationTriangles =
                createComplicationQuad(textureTriangleProgram, -0.9f, -0.1f, 0.6f, 0.6f)

            complicationHighlightTriangles =
                createComplicationHighlightQuad(coloredTriangleProgram, -0.9f, -0.1f, 0.6f, 0.6f)

            // Create triangles for the hands.
            secondHandTriangleMap =
                mapOf(
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
            minuteHandTriangle =
                createHand(
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
            hourHandTriangle =
                createHand(
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
                val cameraAngle = (i.toFloat() / numCameraAngles * 2 * Math.PI).toFloat()
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

            complicationTexture =
                GlesTextureComplication(complicationSlot, 128, 128, GLES20.GL_TEXTURE_2D)
        }

        override suspend fun onUiThreadGlSurfaceCreated(width: Int, height: Int) {
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
        ) =
            Gles2ColoredTriangleList(
                program,
                floatArrayOf(0f, length, 0f, -width / 2, 0f, 0f, width / 2, 0f, 0f),
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
                System.arraycopy(triangleCoords, 0, trianglesCoords, i * 9, triangleCoords.size)
            }
            return Gles2ColoredTriangleList(
                program,
                trianglesCoords,
                floatArrayOf(1.0f /* red */, 1.0f /* green */, 1.0f /* blue */, 1.0f /* alpha */)
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
                System.arraycopy(triangleCoords, 0, trianglesCoords, index, triangleCoords.size)
                index += 9
            }
            return Gles2ColoredTriangleList(
                program,
                trianglesCoords,
                floatArrayOf(0.5f /* red */, 0.5f /* green */, 0.5f /* blue */, 1.0f /* alpha */)
            )
        }

        /** Creates a triangle list for the complication. */
        private fun createComplicationQuad(
            program: Gles2TexturedTriangleList.Program,
            left: Float,
            top: Float,
            width: Float,
            height: Float
        ) =
            Gles2TexturedTriangleList(
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
                floatArrayOf(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            )

        /** Creates a triangle list for the complication highlight quad. */
        private fun createComplicationHighlightQuad(
            program: Gles2ColoredTriangleList.Program,
            left: Float,
            top: Float,
            width: Float,
            height: Float
        ) =
            Gles2ColoredTriangleList(
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
                floatArrayOf(1.0f /* red */, 1.0f /* green */, 1.0f /* blue */, 0.0f /* alpha */)
            )

        private fun getMajorTickTriangleCoords(index: Int): FloatArray {
            return getTickTriangleCoords(
                0.03f /* width */,
                0.09f /* length */,
                index * 360 / 4 /* angleDegrees */
            )
        }

        private fun getMinorTickTriangleCoords(index: Int): FloatArray {
            return getTickTriangleCoords(
                0.02f /* width */,
                0.06f /* length */,
                index * 360 / 12 /* angleDegrees */
            )
        }

        private fun getTickTriangleCoords(
            width: Float,
            length: Float,
            angleDegrees: Int
        ): FloatArray {
            // Create the data for the VBO.
            val coords =
                floatArrayOf(0f, 1f, 0f, width / 2, length + 1, 0f, -width / 2, length + 1, 0f)
            rotateCoords(coords, angleDegrees)
            return coords
        }

        /**
         * Destructively rotates the given coordinates in the XY plane about the origin by the given
         * angle.
         *
         * @param coords flattened 3D coordinates
         * @param angleDegrees angle in degrees clockwise when viewed from negative infinity on the
         *   Z axis
         */
        private fun rotateCoords(coords: FloatArray, angleDegrees: Int) {
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

        override suspend fun createSharedAssets() = ExampleSharedAssets()

        override fun render(zonedDateTime: ZonedDateTime, sharedAssets: ExampleSharedAssets) {
            // Draw background color and select the appropriate view projection matrix. The
            // background
            // should always be black in ambient mode. The view projection matrix used is overhead
            // in
            // ambient. In interactive mode, it's tilted depending on the current time.
            val vpMatrix =
                if (renderParameters.drawMode == DrawMode.AMBIENT) {
                    GLES20.glClearColor(0f, 0f, 0f, 1f)
                    ambientVpMatrix
                } else {
                    when (
                        currentUserStyleRepository.userStyle.value[colorStyleSetting]!!.toString()
                    ) {
                        "red_style" -> GLES20.glClearColor(0.5f, 0.2f, 0.2f, 1f)
                        "green_style" -> GLES20.glClearColor(0.2f, 0.5f, 0.2f, 1f)
                    }
                    val cameraIndex =
                        (zonedDateTime.toInstant().toEpochMilli() / FRAME_PERIOD_MS %
                                numCameraAngles)
                            .toInt()
                    vpMatrices[cameraIndex]
                }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // Draw the complication first.
            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS)) {
                complicationTexture.renderToTexture(zonedDateTime, renderParameters)

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
            val seconds = zonedDateTime.second + (zonedDateTime.nano / 1000000000.0)
            val minutes = zonedDateTime.minute + seconds / 60f
            val hours = (zonedDateTime.hour % 12) + minutes / 60f
            val secIndex = (seconds / 60f * 360f).toInt()
            val minIndex = (minutes / 60f * 360f).toInt()
            val hoursIndex = (hours / 12f * 360f).toInt()

            // Render hands.
            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)) {
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrices[hoursIndex], 0)
                hourHandTriangle.draw(mvpMatrix)

                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrices[minIndex], 0)
                minuteHandTriangle.draw(mvpMatrix)

                if (renderParameters.drawMode != DrawMode.AMBIENT) {
                    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrices[secIndex], 0)
                    secondHandTriangleMap[
                            currentUserStyleRepository.userStyle.value[colorStyleSetting]!!
                                .toString()]
                        ?.draw(mvpMatrix)
                }
            }

            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
                majorTickTriangles.draw(vpMatrix)
                minorTickTriangles.draw(vpMatrix)
                coloredTriangleProgram.unbindAttribs()
            }
        }

        override fun renderHighlightLayer(
            zonedDateTime: ZonedDateTime,
            sharedAssets: ExampleSharedAssets
        ) {
            val cameraIndex =
                (zonedDateTime.toInstant().toEpochMilli() / FRAME_PERIOD_MS % numCameraAngles)
                    .toInt()
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

    companion object {
        /** Expected frame rate in interactive mode. */
        private const val FPS: Long = 60

        /** Z distance from the camera to the watchface. */
        private const val EYE_Z = -2.3f

        /** How long each frame is displayed at expected frame rate. */
        private const val FRAME_PERIOD_MS: Long = 1000 / FPS

        /** Cycle time before the camera motion repeats. */
        private const val CYCLE_PERIOD_SECONDS: Long = 5

        /** Number of camera angles to precompute. */
        private const val numCameraAngles = (CYCLE_PERIOD_SECONDS * FPS).toInt()

        const val EXAMPLE_OPENGL_COMPLICATION_ID = 101
    }
}
