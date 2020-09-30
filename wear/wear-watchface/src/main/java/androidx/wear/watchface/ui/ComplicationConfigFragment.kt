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

package androidx.wear.watchface.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.icu.util.Calendar
import android.os.Bundle
import android.support.wearable.watchface.Constants
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.wear.watchface.DrawMode
import androidx.wear.widget.SwipeDismissFrameLayout
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * This fragment lets the user to select a non-background complication to configure.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ComplicationConfigFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ConfigView(
            requireContext(),
            activity as WatchFaceConfigActivity
        ).apply {
            setWillNotDraw(false)
            isSwipeable = true
            addCallback(
                object : SwipeDismissFrameLayout.Callback() {
                    override fun onDismissed(layout: SwipeDismissFrameLayout) {
                        parentFragmentManager.popBackStackImmediate()
                    }
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.PROVIDER_CHOOSER_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK
        ) {
            // Exit the configuration flow.
            activity?.finish()
        }
    }
}

/**
 * Configuration view for watch faces with multiple complications.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings(
    "ViewConstructor", // Internal view, not intended for use by tools.
    "ClickableViewAccessibility" // performClick would be ambiguous.
)
internal class ConfigView(
    context: Context,

    val watchFaceConfigActivity: WatchFaceConfigActivity
) : SwipeDismissFrameLayout(context) {

    /** @hide */
    private companion object {
        // Dashed lines are used for complication selection.
        private val DASH_WIDTH = 10.0f
        private var DASH_GAP = 2.0f
        private var DASH_LENGTH = 5.0f
    }

    private val dashPaint = Paint().apply {
        strokeWidth = DASH_WIDTH
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        color = Color.RED
    }

    /**
     * Event info class to hold the position and type of the event.
     * We will use this structure to cache the details of the last
     * motion event
     */
    data class EventInfo(
        var eventPositionX: Int,
        var eventPositionY: Int,
        var eventType: Int
    )

    private val snapshottedTime = Calendar.getInstance().apply {
        timeZone = watchFaceConfigActivity.watchFaceConfigDelegate.getCalendar().timeZone
        timeInMillis = watchFaceConfigActivity.watchFaceConfigDelegate.getCalendar().timeInMillis
    }

    private val drawRect = Rect()
    private var lastEventInfo: EventInfo? = null
    private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
        @SuppressWarnings("SyntheticAccessor")
        override fun onDown(e: MotionEvent): Boolean {
            lastEventInfo =
                EventInfo(
                    e.x.toInt(),
                    e.y.toInt(),
                    MotionEvent.ACTION_DOWN
                )
            return true
        }

        @SuppressWarnings("SyntheticAccessor")
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return if (onTap(e.x.toInt(), e.y.toInt())) {
                invalidate()
                true
            } else {
                super.onSingleTapUp(e)
            }
        }
    }
    private val gestureDetector = GestureDetector(context, gestureListener)

    /**
     * Called when the user taps on the view.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @SuppressWarnings("unused")
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onTap(
        tapX: Int,
        tapY: Int
    ): Boolean {
        var iWatchFaceConfig = watchFaceConfigActivity.watchFaceConfigDelegate
        // Check if the user tapped on any of the complications, but with the supplied calendar.
        // This is to support freezing of animated complications while the user selects one to
        // configure.
        val complicationId =
            iWatchFaceConfig.getComplicationIdAt(tapX, tapY) ?: return false
        val complication = iWatchFaceConfig.getComplicationsMap()[complicationId]!!
        iWatchFaceConfig.brieflyHighlightComplicationId(complicationId)
        watchFaceConfigActivity.fragmentController.showComplicationConfig(
            complicationId,
            *complication.supportedTypes
        )
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        // This is to handle strange gestureDetector behavior logged here: http://b/153992719
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            lastEventInfo?.let {
                if ((it.eventType == MotionEvent.ACTION_DOWN) &&
                    (abs(it.eventPositionX - event.rawX.toInt()) < 1) &&
                    (abs(it.eventPositionY - event.rawY.toInt()) < 1)
                ) {
                    onTap(event.rawX.toInt(), event.rawY.toInt())
                }
            }
            lastEventInfo = null
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        drawRect.set(0, 0, width, height)
    }

    override fun onDraw(canvas: Canvas) {
        // TODO(alexclarke): We should take screenshots for the different layers and composite.
        val bitmap = watchFaceConfigActivity.watchFaceConfigDelegate.takeScreenshot(
            drawRect,
            snapshottedTime,
            DrawMode.INTERACTIVE
        )
        canvas.drawBitmap(bitmap, drawRect, drawRect, null)

        val complications = watchFaceConfigActivity.watchFaceConfigDelegate.getComplicationsMap()
        for ((_, complication) in complications) {
            if (complication.enabled) {
                drawComplicationSelectDashBorders(
                    canvas,
                    complication.computeBounds(drawRect)
                )
            }
        }
    }

    private fun drawComplicationSelectDashBorders(canvas: Canvas, bounds: Rect) {
        if (bounds.width() == bounds.height()) {
            drawCircleDashBorder(canvas, bounds)
            return
        }
        val radius = bounds.height() / 2.0f

        // Draw left arc dash.
        var cx = bounds.left + radius
        var cy = bounds.centerY().toFloat()
        var startAngle = (Math.PI / 2.0f).toFloat()
        val dashCount = (Math.PI * radius / (DASH_WIDTH + DASH_GAP)).toInt()
        drawArcDashBorder(canvas, cx, cy, radius, startAngle,
            DASH_LENGTH, dashCount)

        // Draw right arc dash.
        cx = bounds.right - radius
        cy = bounds.centerY().toFloat()
        startAngle = (Math.PI / 2.0f).toFloat() * 3.0f
        drawArcDashBorder(canvas, cx, cy, radius, startAngle,
            DASH_LENGTH, dashCount)

        // Draw straight line dash.
        val rectangleWidth = bounds.width() - 2.0f * radius - 2.0f * DASH_GAP
        val cnt = (rectangleWidth / (DASH_WIDTH + DASH_GAP)).toInt()
        val baseX: Float = bounds.left + radius + DASH_GAP
        val fixGap: Float = (rectangleWidth - cnt * DASH_WIDTH) / (cnt - 1)
        for (i in 0 until cnt) {
            val startX: Float = baseX + i * (fixGap + DASH_WIDTH) + DASH_WIDTH / 2
            var startY = bounds.top.toFloat()
            var endY: Float = bounds.top - DASH_LENGTH
            canvas.drawLine(startX, startY, startX, endY, dashPaint)
            startY = bounds.bottom.toFloat()
            endY = startY + DASH_LENGTH
            canvas.drawLine(startX, startY, startX, endY, dashPaint)
        }
    }

    private fun drawArcDashBorder(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        startAngle: Float,
        dashLength: Float,
        dashCount: Int
    ) {
        for (i in 0 until dashCount) {
            val rot = (2.0 * Math.PI / (2.0 * (dashCount - 1).toDouble()) * i + startAngle)
            val startX = (r * cos(rot)).toFloat() + cx
            val startY = (r * sin(rot)).toFloat() + cy
            val endX = ((r + dashLength) * cos(rot).toFloat()) + cx
            val endY = ((r + dashLength) * sin(rot).toFloat()) + cy
            canvas.drawLine(startX, startY, endX, endY, dashPaint)
        }
    }

    private fun drawCircleDashBorder(canvas: Canvas, bounds: Rect) {
        val radius = bounds.width() / 2.0f
        val dashCount = (2.0 * Math.PI * radius / (DASH_WIDTH + DASH_GAP)).toInt()
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        for (i in 0 until dashCount) {
            val rot = (i * 2.0 * Math.PI / dashCount)
            val startX = (radius * cos(rot).toFloat()) + cx
            val startY = (radius * sin(rot).toFloat()) + cy
            val endX = ((radius + DASH_LENGTH) * cos(rot).toFloat()) + cx
            val endY = ((radius + DASH_LENGTH) * sin(rot).toFloat()) + cy
            canvas.drawLine(startX, startY, endX, endY, dashPaint)
        }
    }
}
