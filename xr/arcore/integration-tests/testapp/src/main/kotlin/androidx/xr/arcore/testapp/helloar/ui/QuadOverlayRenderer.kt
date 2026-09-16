/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.arcore.testapp.helloar.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import android.view.Surface
import androidx.xr.arcore.TrackingState

/** Interaction hover state for a tracked quad surface. */
internal enum class InteractionState {
    NORMAL,
    HOVERED,
}

/** Visual placement of tracking dots on the quad surface. */
internal enum class DotPlacement {
    NONE,
    CENTER,
    TOP,
    BOTH,
}

/** Renders 2D visual indicators and status overlays onto a quad surface canvas. */
internal class QuadOverlayRenderer {

    private val paintLock = Any()

    private val standardStrokePaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = BASE_STROKE_WIDTH_STANDARD
        }
    private val hoverOuterPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = BASE_STROKE_WIDTH_HOVER_OUTER
        }
    private val hoverInnerPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = BASE_STROKE_WIDTH_HOVER_INNER
        }
    private val fillPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
    private val textPaint =
        Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = OVERLAY_TEXT_SIZE_PX
            textAlign = Paint.Align.CENTER
        }

    private fun calculateBaselineY(paint: Paint, height: Float): Float =
        (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)

    private inline fun drawDots(
        dotPlacement: DotPlacement,
        width: Int,
        height: Int,
        topCy: Float,
        drawAction: (cx: Float, cy: Float) -> Unit,
    ) {
        when (dotPlacement) {
            DotPlacement.NONE -> Unit
            DotPlacement.CENTER -> drawAction(width / 2f, height / 2f)
            DotPlacement.TOP -> drawAction(width / 2f, topCy)
            DotPlacement.BOTH -> {
                drawAction(width / 2f, height / 2f)
                drawAction(width / 2f, topCy)
            }
        }
    }

    private fun drawStatusOverlay(
        canvas: Canvas,
        width: Int,
        height: Int,
        interactionState: InteractionState,
        dotPlacement: DotPlacement,
        dotRadius: Float,
        renderAsDot: Boolean,
        color: Int,
        text: String,
        distance: Float,
    ) {
        if (renderAsDot) {
            fillPaint.color = color
            val outerRadius = dotRadius + (hoverOuterPaint.strokeWidth / 2f)
            val topCy = if (interactionState == InteractionState.HOVERED) outerRadius else dotRadius
            drawDots(dotPlacement, width, height, topCy = topCy) { cx, cy ->
                if (interactionState == InteractionState.HOVERED) {
                    canvas.drawCircle(cx, cy, outerRadius, hoverOuterPaint)
                }
                canvas.drawCircle(cx, cy, dotRadius, fillPaint)
            }
        } else {
            if (interactionState == InteractionState.HOVERED) {
                val halfOuter = hoverOuterPaint.strokeWidth / 2f
                if (width.toFloat() > 2f * halfOuter && height.toFloat() > 2f * halfOuter) {
                    canvas.drawRect(
                        halfOuter,
                        halfOuter,
                        width.toFloat() - halfOuter,
                        height.toFloat() - halfOuter,
                        hoverOuterPaint,
                    )
                }
                val innerInset =
                    hoverOuterPaint.strokeWidth +
                        (HOVER_INNER_STROKE_INSET_PX * distance) +
                        (hoverInnerPaint.strokeWidth / 2f)
                if (width.toFloat() > 2f * innerInset && height.toFloat() > 2f * innerInset) {
                    hoverInnerPaint.color = color
                    canvas.drawRect(
                        innerInset,
                        innerInset,
                        width.toFloat() - innerInset,
                        height.toFloat() - innerInset,
                        hoverInnerPaint,
                    )
                }
            } else {
                standardStrokePaint.color = color
                val halfStroke = standardStrokePaint.strokeWidth / 2f
                if (width.toFloat() > 2f * halfStroke && height.toFloat() > 2f * halfStroke) {
                    canvas.drawRect(
                        halfStroke,
                        halfStroke,
                        width.toFloat() - halfStroke,
                        height.toFloat() - halfStroke,
                        standardStrokePaint,
                    )
                }
            }
            textPaint.color =
                if (interactionState == InteractionState.HOVERED) Color.WHITE else color
            val textY = calculateBaselineY(textPaint, height.toFloat())
            val displayText =
                if (interactionState == InteractionState.HOVERED) OVERLAY_TEXT_STOP else text
            canvas.drawText(displayText, width / 2f, textY, textPaint)
        }
    }

    /**
     * Draws status outlines, text, and tracking dots onto the given [Surface].
     *
     * @param surface the target Android [Surface] on which overlays are rendered.
     * @param width width of the quad surface canvas in pixels; must be positive.
     * @param height height of the quad surface canvas in pixels; must be positive.
     * @param interactionState visual hover state determining border styling and controls.
     * @param trackingState current tracking status from Spatial Annotation Tracking.
     * @param distance distance in meters from the observer to the quad surface, used to scale
     *   strokes and text for perspective consistency; must be positive and finite.
     * @param dotPlacement visual placement of tracking dots on the quad surface.
     */
    fun drawQuadOverlay(
        surface: Surface,
        width: Int,
        height: Int,
        interactionState: InteractionState,
        trackingState: TrackingState,
        distance: Float,
        dotPlacement: DotPlacement = DotPlacement.NONE,
    ) {
        require(width > 0) { "Width must be positive, was $width" }
        require(height > 0) { "Height must be positive, was $height" }
        require(distance > 0f && distance.isFinite()) {
            "Distance must be positive and finite, was $distance"
        }

        if (!surface.isValid) {
            Log.w(TAG, "Surface is not valid or has been released, skipping quad overlay render")
            return
        }

        val canvas =
            try {
                surface.lockCanvas(null)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Surface is already locked or invalid, skipping quad overlay render", e)
                return
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Surface is in an illegal state, skipping quad overlay render", e)
                return
            }
        if (canvas == null) {
            Log.w(TAG, "Failed to lock surface canvas for rendering quad overlay")
            return
        }
        try {
            synchronized(paintLock) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

                standardStrokePaint.strokeWidth = BASE_STROKE_WIDTH_STANDARD * distance
                hoverOuterPaint.strokeWidth = BASE_STROKE_WIDTH_HOVER_OUTER * distance
                hoverInnerPaint.strokeWidth = BASE_STROKE_WIDTH_HOVER_INNER * distance
                textPaint.textSize = OVERLAY_TEXT_SIZE_PX * distance

                val isSmall =
                    width <= MIN_TRACKING_SIZE_PIXELS || height <= MIN_TRACKING_SIZE_PIXELS
                val effectiveDotPlacement =
                    if (isSmall && dotPlacement == DotPlacement.NONE) {
                        DotPlacement.CENTER
                    } else {
                        dotPlacement
                    }
                val dotRadius = DOT_RADIUS_PX * distance

                when (trackingState) {
                    TrackingState.TRACKING -> {
                        if (isSmall) {
                            val outerRadius = dotRadius + (hoverOuterPaint.strokeWidth / 2f)
                            val topDotY =
                                if (interactionState == InteractionState.HOVERED) outerRadius
                                else dotRadius
                            fillPaint.color = Color.GREEN
                            drawDots(effectiveDotPlacement, width, height, topDotY) { cx, cy ->
                                if (interactionState == InteractionState.HOVERED) {
                                    canvas.drawCircle(cx, cy, outerRadius, hoverOuterPaint)
                                }
                                canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                            }
                        } else {
                            if (interactionState == InteractionState.HOVERED) {
                                hoverInnerPaint.color = Color.WHITE
                                val halfOuter = hoverOuterPaint.strokeWidth / 2f
                                if (
                                    width.toFloat() > 2f * halfOuter &&
                                        height.toFloat() > 2f * halfOuter
                                ) {
                                    canvas.drawRect(
                                        halfOuter,
                                        halfOuter,
                                        width.toFloat() - halfOuter,
                                        height.toFloat() - halfOuter,
                                        hoverOuterPaint,
                                    )
                                }
                                val innerInset =
                                    hoverOuterPaint.strokeWidth +
                                        (HOVER_INNER_STROKE_INSET_PX * distance) +
                                        (hoverInnerPaint.strokeWidth / 2f)
                                if (
                                    width.toFloat() > 2f * innerInset &&
                                        height.toFloat() > 2f * innerInset
                                ) {
                                    canvas.drawRect(
                                        innerInset,
                                        innerInset,
                                        width.toFloat() - innerInset,
                                        height.toFloat() - innerInset,
                                        hoverInnerPaint,
                                    )
                                }
                                textPaint.color = Color.WHITE
                                val textY = calculateBaselineY(textPaint, height.toFloat())
                                canvas.drawText(OVERLAY_TEXT_STOP, width / 2f, textY, textPaint)
                            } else {
                                standardStrokePaint.color = Color.WHITE
                                val halfStroke = standardStrokePaint.strokeWidth / 2f
                                if (
                                    width.toFloat() > 2f * halfStroke &&
                                        height.toFloat() > 2f * halfStroke
                                ) {
                                    canvas.drawRect(
                                        halfStroke,
                                        halfStroke,
                                        width.toFloat() - halfStroke,
                                        height.toFloat() - halfStroke,
                                        standardStrokePaint,
                                    )
                                }
                            }

                            if (dotPlacement != DotPlacement.NONE) {
                                fillPaint.color = Color.GREEN
                                val topDotY = dotRadius + standardStrokePaint.strokeWidth
                                drawDots(dotPlacement, width, height, topDotY) { cx, cy ->
                                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                                }
                            }
                        }
                    }
                    TrackingState.PAUSED -> {
                        drawStatusOverlay(
                            canvas = canvas,
                            width = width,
                            height = height,
                            interactionState = interactionState,
                            dotPlacement = effectiveDotPlacement,
                            dotRadius = dotRadius,
                            renderAsDot = isSmall,
                            color = Color.RED,
                            text = OVERLAY_TEXT_PAUSED,
                            distance = distance,
                        )
                    }
                    TrackingState.STOPPED -> {
                        drawStatusOverlay(
                            canvas = canvas,
                            width = width,
                            height = height,
                            interactionState = interactionState,
                            dotPlacement = effectiveDotPlacement,
                            dotRadius = dotRadius,
                            renderAsDot = isSmall,
                            color = Color.RED,
                            text = OVERLAY_TEXT_STOPPED,
                            distance = distance,
                        )
                    }
                }
            }
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    companion object {
        private const val TAG = "QuadOverlayRenderer"
        private const val DOT_RADIUS_PX = 15f
        private const val OVERLAY_TEXT_STOP = "Stop Tracking"
        private const val OVERLAY_TEXT_PAUSED = "PAUSED"
        private const val OVERLAY_TEXT_STOPPED = "STOPPED"
        private const val OVERLAY_TEXT_SIZE_PX = 20f
        private const val BASE_STROKE_WIDTH_STANDARD = 16f
        private const val BASE_STROKE_WIDTH_HOVER_OUTER = 32f
        private const val BASE_STROKE_WIDTH_HOVER_INNER = 16f
        private const val HOVER_INNER_STROKE_INSET_PX = 6f

        /**
         * Minimum canvas dimension in pixels below which quad overlays collapse into tracking dots.
         */
        internal const val MIN_TRACKING_SIZE_PIXELS = 50
    }
}
