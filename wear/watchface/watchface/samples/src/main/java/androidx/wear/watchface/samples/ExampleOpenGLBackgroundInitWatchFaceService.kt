/*
 * Copyright 2021 The Android Open Source Project
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
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationDeniedActivity
import androidx.wear.watchface.complications.permission.dialogs.sample.ComplicationRationalActivity
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime

/** Expected frame rate in interactive mode.  */
private const val FPS: Long = 60

/** How long each frame is displayed at expected frame rate.  */
private const val FRAME_PERIOD_MS: Long = 1000 / FPS

/**
 * Sample watch face using OpenGL with textures loaded on a background thread by [createWatchFace]
 * which are used for rendering on the main thread.
 */
class ExampleOpenGLBackgroundInitWatchFaceService() : WatchFaceService() {
    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        // Init is performed on a worker thread, however all rendering is done on the UiThread so
        // it's expected to construct the MainThreadRenderer here.
        val renderer =
            MainThreadRenderer(surfaceHolder, currentUserStyleRepository, watchState, resources)
        return WatchFace(WatchFaceType.ANALOG, renderer)
            .setComplicationDeniedDialogIntent(
                Intent(this, ComplicationDeniedActivity::class.java)
            )
            .setComplicationRationaleDialogIntent(
                Intent(this, ComplicationRationalActivity::class.java)
            )
    }
}

internal class MainThreadRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val resources: Resources
) : Renderer.GlesRenderer(surfaceHolder, currentUserStyleRepository, watchState, FRAME_PERIOD_MS) {

    internal var watchBodyTexture: Int = -1
    internal var watchHandTexture: Int = -1

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16).apply {
        Matrix.setLookAtM(
            this,
            0,
            0f,
            0f,
            -1.0f,
            0f,
            0f,
            0f,
            0f,
            1f,
            0f
        )
    }
    private val handPositionMatrix = FloatArray(16)
    private val handViewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private lateinit var triangleTextureProgram: Gles2TexturedTriangleList.Program
    private lateinit var backgroundQuad: Gles2TexturedTriangleList
    private lateinit var secondHandQuad: Gles2TexturedTriangleList
    private lateinit var minuteHandQuad: Gles2TexturedTriangleList
    private lateinit var hourHandQuad: Gles2TexturedTriangleList

    override suspend fun onBackgroundThreadGlContextCreated() {
        triangleTextureProgram = Gles2TexturedTriangleList.Program()
        backgroundQuad = createTexturedQuad(
            triangleTextureProgram, -10f, -10f, 20f, 20f
        )
        secondHandQuad = createTexturedQuad(
            triangleTextureProgram, -0.75f, -6f, 1.5f, 8f
        )
        minuteHandQuad = createTexturedQuad(
            triangleTextureProgram, -0.33f, -4.5f, 0.66f, 6f
        )
        hourHandQuad = createTexturedQuad(
            triangleTextureProgram, -0.25f, -3f, 0.5f, 4f
        )
        watchBodyTexture = loadTextureFromResource(R.drawable.wf_background)
        checkGLError("Load watchBodyTexture")
        watchHandTexture = loadTextureFromResource(R.drawable.hand)
        checkGLError("Load watchHandTexture")
    }

    private fun loadTextureFromResource(resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            val bitmap = BitmapFactory.decodeResource(
                resources,
                resourceId,
                BitmapFactory.Options().apply {
                    inScaled = false // No pre-scaling
                }
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
            )
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
            bitmap.recycle()
        }
        return textureHandle[0]
    }

    fun checkGLError(op: String) {
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            System.err.println("OpenGL Error $op: glError $error")
        }
    }

    override suspend fun onUiThreadGlSurfaceCreated(width: Int, height: Int) {
        GLES20.glEnable(GLES20.GL_TEXTURE_2D)

        // Update the projection matrix based on the new aspect ratio.
        val aspectRatio = width.toFloat() / height.toFloat()
        Matrix.frustumM(
            projectionMatrix,
            0 /* offset */,
            -aspectRatio /* left */,
            aspectRatio /* right */,
            -1f /* bottom */,
            1f /* top */,
            0.1f /* near */,
            100f /* far */
        )
    }

    private fun createTexturedQuad(
        program: Gles2TexturedTriangleList.Program,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) = Gles2TexturedTriangleList(
        program,
        floatArrayOf(
            top + 0f,
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

    override fun render(zonedDateTime: ZonedDateTime) {
        checkGLError("renders")
        GLES20.glClearColor(0f, 1f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        triangleTextureProgram.bindProgramAndAttribs()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, watchBodyTexture)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO)

        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        backgroundQuad.draw(vpMatrix)

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, watchHandTexture)

        val hours = (zonedDateTime.hour % 12).toFloat()
        val minutes = zonedDateTime.minute.toFloat()
        val seconds = zonedDateTime.second.toFloat() +
            (zonedDateTime.nano.toDouble() / 1000000000.0).toFloat()

        val secondsRot = seconds / 60.0f * 360.0f
        Matrix.setRotateM(handPositionMatrix, 0, secondsRot, 0f, 0f, 1f)
        Matrix.multiplyMM(handViewMatrix, 0, viewMatrix, 0, handPositionMatrix, 0)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, handViewMatrix, 0)
        secondHandQuad.draw(vpMatrix)

        val minuteRot = (minutes + seconds / 60.0f) / 60.0f * 360.0f
        Matrix.setRotateM(handPositionMatrix, 0, minuteRot, 0f, 0f, 1f)
        Matrix.multiplyMM(handViewMatrix, 0, viewMatrix, 0, handPositionMatrix, 0)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, handViewMatrix, 0)
        minuteHandQuad.draw(vpMatrix)

        val hourRot = (hours + minutes / 60.0f + seconds / 3600.0f) / 12.0f * 360.0f
        Matrix.setRotateM(handPositionMatrix, 0, hourRot, 0f, 0f, 1f)
        Matrix.multiplyMM(handViewMatrix, 0, viewMatrix, 0, handPositionMatrix, 0)
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, handViewMatrix, 0)
        hourHandQuad.draw(vpMatrix)

        triangleTextureProgram.unbindAttribs()
    }

    override fun renderHighlightLayer(zonedDateTime: ZonedDateTime) {
        val highlightLayer = renderParameters.highlightLayer!!
        GLES20.glClearColor(
            Color.red(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.green(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.blue(highlightLayer.backgroundTint).toFloat() / 256.0f,
            Color.alpha(highlightLayer.backgroundTint).toFloat() / 256.0f
        )
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }
}
