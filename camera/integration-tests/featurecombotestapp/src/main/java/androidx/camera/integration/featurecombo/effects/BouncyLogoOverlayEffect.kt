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

package androidx.camera.integration.featurecombo.effects

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.effects.OverlayEffect
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.core.util.Consumer
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * An overlay effect that draws a logo bouncing around on the screen.
 *
 * <p>This is like the classic DVD logo bouncing around the screen, but with a CameraX logo.
 */
class BouncyLogoOverlayEffect(
    useCases: Set<AppUseCase>,
    private val logoText: String,
    private val bgColor: Int,
    private val textColor: Int,
    private val containerWidth: Int,
    private val containerHeight: Int,
    private val sensorToViewTransformer: () -> Matrix?,
) :
    OverlayEffect(
        useCases.toTargets(),
        0,
        Handler(Looper.getMainLooper()),
        Consumer { t -> Log.d(TAG, "Effect error", t) },
    ) {

    private var overlayEffectLogo: BouncyLogo? = null

    private val textPaint =
        Paint().apply {
            color = textColor
            textSize = 64F
            typeface = Typeface.DEFAULT_BOLD
        }
    private val logoPaint =
        Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
        }

    private val uiToSensor = Matrix()
    private val bounds = Rect()

    init {
        setOnDrawListener { frame ->
            val sensorToUi = sensorToViewTransformer()
            if (sensorToUi != null) {
                // Transform the Canvas to use PreviewView coordinates.
                uiToSensor.reset()
                sensorToUi.invert(uiToSensor)
                uiToSensor.postConcat(frame.sensorToBufferTransform)
                val canvas = frame.overlayCanvas
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                canvas.setMatrix(uiToSensor)

                // measure the size of the text
                textPaint.getTextBounds(logoText, 0, logoText.length, bounds)
                val logoHalfWidth = bounds.width().toFloat() * 0.8F
                val logoHalfHeight = bounds.height().toFloat() * 1.2F

                // Gets the next position of the logo after creating if needed.
                if (overlayEffectLogo == null) {
                    overlayEffectLogo =
                        BouncyLogo(
                            containerWidth,
                            containerHeight,
                            logoHalfWidth.toInt(),
                            logoHalfHeight.toInt(),
                        )
                }
                val position = overlayEffectLogo!!.getNextPosition()

                // Draw an oval and the text within.
                canvas.drawOval(
                    position.x.toFloat() - logoHalfWidth,
                    position.y.toFloat() - logoHalfHeight,
                    position.x.toFloat() + logoHalfWidth,
                    position.y.toFloat() + logoHalfHeight,
                    logoPaint,
                )
                canvas.drawText(
                    logoText,
                    position.x.toFloat() - bounds.width().toFloat() / 2,
                    position.y.toFloat() + bounds.height().toFloat() / 2,
                    textPaint,
                )
            }
            true
        }
    }

    companion object {
        private const val TAG = "CamXFcqBouncyLogoEffect"

        private fun Collection<AppUseCase>.toTargets(): Int =
            fold(0) { acc, useCase ->
                acc or
                    when (useCase) {
                        AppUseCase.PREVIEW -> PREVIEW
                        AppUseCase.VIDEO_CAPTURE -> VIDEO_CAPTURE
                        AppUseCase.IMAGE_CAPTURE -> IMAGE_CAPTURE
                        AppUseCase.IMAGE_ANALYSIS -> 0
                    }
            }

        fun Collection<AppUseCase>.supportsEffect(): Boolean {
            return listOf(
                    PREVIEW,
                    VIDEO_CAPTURE,
                    PREVIEW or VIDEO_CAPTURE,
                    PREVIEW or VIDEO_CAPTURE or IMAGE_CAPTURE,
                )
                .contains(toTargets())
        }
    }
}

/**
 * A logo that bounces around, within a container view.
 *
 * <p> Each time the logo reaches the edge of the container, it will bounce off and move in a new
 * direction. The new destination is a random point on the next edge.
 *
 * @param containerWidth The width of the container view.
 * @param containerHeight The height of the container view.
 * @param logoHalfWidth Half of the width of the logo.
 * @param logoHalfHeight Half of the height of the logo.
 */
class BouncyLogo(
    private val containerWidth: Int,
    private val containerHeight: Int,
    private val logoHalfWidth: Int,
    private val logoHalfHeight: Int,
) {

    companion object {
        private const val STEP_PERCENTAGE = 0.01F
    }

    private var currentPos: Point = Point(containerWidth / 2, containerHeight / 2)
    private var currentEdge: Edge = Edge.TOP
    private var destination: Point = getRandomPointOnEdge(currentEdge)

    // Each step is 0.5% of the screen diagonal.
    private val stepSize: Double = calculateStepSize()

    /** Gets the next position of the logo. */
    fun getNextPosition(): Point {
        if (isAtEdge()) {
            // The logo has reached the destination. Now randomly choose a new destination on the
            // next edge.
            currentEdge = getNextEdge(currentEdge)
            destination = getRandomPointOnEdge(currentEdge)
        }

        // Move the logo towards the destination by one step.
        val deltaX = destination.x - currentPos.x
        val deltaY = destination.y - currentPos.y
        val distance = hypot(deltaX.toDouble(), deltaY.toDouble())

        val stepX = (stepSize * deltaX / distance).toInt()
        val stepY = (stepSize * deltaY / distance).toInt()
        currentPos = Point(currentPos.x + stepX, currentPos.y + stepY)

        return currentPos
    }

    private fun calculateStepSize(): Double {
        return sqrt(
            (containerWidth * containerWidth + containerHeight * containerHeight).toDouble()
        ) * STEP_PERCENTAGE
    }

    private fun getNextEdge(currentEdge: Edge): Edge {
        return when (currentEdge) {
            Edge.TOP -> Edge.RIGHT
            Edge.RIGHT -> Edge.BOTTOM
            Edge.BOTTOM -> Edge.LEFT
            Edge.LEFT -> Edge.TOP
        }
    }

    private fun getRandomPointOnEdge(edge: Edge): Point {
        return when (edge) {
            Edge.TOP -> Point(Random.nextInt(containerWidth), 0)
            Edge.RIGHT -> Point(containerWidth - 1, Random.nextInt(containerHeight))
            Edge.BOTTOM -> Point(Random.nextInt(containerWidth), containerHeight - 1)
            Edge.LEFT -> Point(0, Random.nextInt(containerHeight))
        }
    }

    private fun isAtEdge(): Boolean {
        val offset = 2

        return when {
            (currentPos.x - logoHalfWidth - offset) <= 0 -> true
            (currentPos.x + logoHalfWidth + offset) >= containerWidth -> true
            (currentPos.y - logoHalfHeight - offset) <= 0 -> true
            (currentPos.y + logoHalfHeight + offset) >= containerHeight -> true
            else -> false
        }
    }

    enum class Edge {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT,
    }
}
