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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.support.wearable.complications.ComplicationData
import android.view.SurfaceHolder
import androidx.wear.complications.SystemProviders
import androidx.wear.watchface.CanvasRenderer
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationSet
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.UnitSquareBoundsProvider
import androidx.wear.watchface.WatchFaceHost
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.style.BooleanUserStyleCategory
import androidx.wear.watchface.style.DoubleRangeUserStyleCategory
import androidx.wear.watchface.style.ListUserStyleCategory
import androidx.wear.watchface.style.UserStyleCategory
import androidx.wear.watchface.style.UserStyleManager
import kotlin.math.cos
import kotlin.math.sin

private const val CENTER_CIRCLE_DIAMETER_FRACTION = 0.03738f
private const val OUTER_CIRCLE_STROKE_THICKNESS_FRACTION = 0.00467f
private const val NUMBER_STYLE_OUTER_CIRCLE_RADIUS_FRACTION = 0.00584f

private const val GAP_BETWEEN_OUTER_CIRCLE_AND_BORDER_FRACTION = 0.03738f
private const val GAP_BETWEEN_HAND_AND_CENTER_FRACTION =
    0.01869f + CENTER_CIRCLE_DIAMETER_FRACTION / 2.0f

private const val HOUR_HAND_LENGTH_FRACTION = 0.21028f
private const val HOUR_HAND_THICKNESS_FRACTION = 0.02336f
private const val MINUTE_HAND_LENGTH_FRACTION = 0.3783f
private const val MINUTE_HAND_THICKNESS_FRACTION = 0.0163f
private const val SECOND_HAND_LENGTH_FRACTION = 0.37383f
private const val SECOND_HAND_THICKNESS_FRACTION = 0.00934f

private const val NUMBER_RADIUS_FRACTION = 0.45f

/** A simple example canvas based watch face. */
class ExampleCanvasWatchFaceService : WatchFaceService() {
    override fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchFaceHost: WatchFaceHost,
        watchState: WatchState
    ): WatchFace {
        val watchFaceStyle = WatchFaceColorStyle.create(this, "red_style")
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
        val drawHourPipsStyleCategory =
            BooleanUserStyleCategory(
                "draw_hour_pips_style_category",
                "Hour Pips",
                "Whether to draw or not",
                null,
                true
            )
        val watchHandLengthStyleCategory =
            DoubleRangeUserStyleCategory(
                "watch_hand_length_style_category",
                "Hand length",
                "Scale of watch hands",
                null,
                0.25,
                1.0,
                0.75
            )
        val styleManager = UserStyleManager(
            listOf(colorStyleCategory, drawHourPipsStyleCategory, watchHandLengthStyleCategory)
        )
        val complicationSlots = ComplicationSet(
            listOf(
                Complication(
                    EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                    UnitSquareBoundsProvider(RectF(0.2f, 0.4f, 0.4f, 0.6f)),
                    watchFaceStyle.getComplicationDrawableRenderer(this, watchState),
                    intArrayOf(
                        ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_LONG_TEXT,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SMALL_IMAGE
                    ),
                    Complication.DefaultComplicationProvider(SystemProviders.DAY_OF_WEEK),
                    ComplicationData.TYPE_SHORT_TEXT
                ),
                Complication(
                    EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                    UnitSquareBoundsProvider(RectF(0.6f, 0.4f, 0.8f, 0.6f)),
                    watchFaceStyle.getComplicationDrawableRenderer(this, watchState),
                    intArrayOf(
                        ComplicationData.TYPE_RANGED_VALUE,
                        ComplicationData.TYPE_LONG_TEXT,
                        ComplicationData.TYPE_SHORT_TEXT,
                        ComplicationData.TYPE_ICON,
                        ComplicationData.TYPE_SMALL_IMAGE
                    ),
                    Complication.DefaultComplicationProvider(SystemProviders.STEP_COUNT),
                    ComplicationData.TYPE_SHORT_TEXT
                )
            )
        )
        val renderer = ExampleCanvasRenderer(
            surfaceHolder,
            this,
            watchFaceStyle,
            styleManager,
            watchState,
            colorStyleCategory,
            drawHourPipsStyleCategory,
            watchHandLengthStyleCategory,
            complicationSlots
        )

        return WatchFace.Builder(
            WatchFaceType.ANALOG,
            /** mInteractiveUpdateRateMillis */
            16,
            styleManager,
            complicationSlots,
            renderer,
            watchFaceHost,
            watchState
        ).build()
    }
}

const val EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID = 101
const val EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID = 102

class ExampleCanvasRenderer(
    surfaceHolder: SurfaceHolder,
    private val context: Context,
    private var watchFaceColorStyle: WatchFaceColorStyle,
    userStyleManager: UserStyleManager,
    private val watchState: WatchState,
    private val colorStyleCategory: ListUserStyleCategory,
    private val drawPipsStyleCategory: BooleanUserStyleCategory,
    private val watchHandLengthStyleCategoryDouble: DoubleRangeUserStyleCategory,
    private val complicationSet: ComplicationSet
) : CanvasRenderer(surfaceHolder, userStyleManager, watchState, CanvasType.HARDWARE) {

    private val clockHandPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = context.resources.getDimensionPixelSize(
            R.dimen.clock_hand_stroke_width
        ).toFloat()
    }

    private val outerElementPaint = Paint().apply {
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = context.resources.getDimensionPixelSize(R.dimen.hour_mark_size).toFloat()
    }

    companion object {
        private val HOUR_MARKS = arrayOf("3", "6", "9", "12")

        // Dashed lines are used for complication selection.
        private val DASH_WIDTH = 10.0f
        private var DASH_GAP = 2.0f
        private var DASH_LENGTH = 5.0f
    }

    private val mDashPaint = Paint().apply {
        strokeWidth = DASH_WIDTH
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
        color = Color.RED
    }

    private lateinit var hourHandFill: Path
    private lateinit var hourHandBorder: Path
    private lateinit var minuteHandFill: Path
    private lateinit var minuteHandBorder: Path
    private lateinit var secondHand: Path

    private var drawHourPips = true
    private var watchHandScale = 1.0f

    init {
        userStyleManager.addUserStyleListener(
            object : UserStyleManager.UserStyleListener {
                @SuppressLint("SyntheticAccessor")
                override fun onUserStyleChanged(
                    userStyle: Map<UserStyleCategory, UserStyleCategory.Option>
                ) {
                    watchFaceColorStyle =
                        WatchFaceColorStyle.create(context, userStyle[colorStyleCategory]!!.id)

                    // Apply the userStyle to the complications. ComplicationDrawables for each of the
                    // styles are defined in XML so we need to replace the complication's drawables.
                    for ((_, complication) in complicationSet.complications) {
                        complication.setRenderer(
                            watchFaceColorStyle.getComplicationDrawableRenderer(context, watchState)
                        )
                    }

                    val drawPipsOption =
                        userStyle[drawPipsStyleCategory]!! as BooleanUserStyleCategory.BooleanOption
                    val watchHandLengthOption =
                        userStyle[watchHandLengthStyleCategoryDouble]!! as
                                DoubleRangeUserStyleCategory.DoubleRangeOption

                    drawHourPips = drawPipsOption.value
                    watchHandScale = watchHandLengthOption.value.toFloat()
                }
            }
        )
    }

    override fun onDraw(canvas: Canvas, bounds: Rect, calendar: Calendar) {
        val style = if (drawMode == DrawMode.AMBIENT) {
            watchFaceColorStyle.ambientStyle
        } else {
            watchFaceColorStyle.activeStyle
        }

        canvas.drawColor(style.backgroundColor)

        drawComplications(canvas, calendar)
        drawClockHands(canvas, bounds, calendar, style)

        if (drawMode != DrawMode.AMBIENT && drawHourPips) {
            drawNumberStyleOuterElement(canvas, bounds, style)
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        calendar: Calendar,
        style: ColorStyle
    ) {
        recalculateClockHands(bounds)
        val hours = calendar.get(Calendar.HOUR).toFloat()
        val minutes = calendar.get(Calendar.MINUTE).toFloat()
        val seconds = calendar.get(Calendar.SECOND).toFloat() +
                (calendar.get(Calendar.MILLISECOND).toFloat() / 1000f)

        val hourRot = (hours + minutes / 60.0f + seconds / 3600.0f) / 12.0f * 360.0f
        val minuteRot = (minutes + seconds / 60.0f) / 60.0f * 360.0f

        canvas.save()

        recalculateClockHands(bounds)

        if (drawMode == DrawMode.AMBIENT) {
            clockHandPaint.style = Paint.Style.STROKE
            clockHandPaint.color = style.hourHandColor
            canvas.scale(
                watchHandScale,
                watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            canvas.rotate(hourRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.drawPath(hourHandBorder, clockHandPaint)
            canvas.rotate(-hourRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.scale(
                1.0f / watchHandScale,
                1.0f / watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )

            clockHandPaint.color = style.minuteHandColor
            canvas.scale(
                watchHandScale,
                watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            canvas.rotate(minuteRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.drawPath(minuteHandBorder, clockHandPaint)
            canvas.rotate(-minuteRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.scale(
                1.0f / watchHandScale,
                1.0f / watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
        } else {
            clockHandPaint.style = Paint.Style.FILL
            clockHandPaint.color = style.hourHandColor
            canvas.scale(
                watchHandScale,
                watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            canvas.rotate(hourRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.drawPath(hourHandFill, clockHandPaint)
            canvas.rotate(-hourRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.scale(
                1.0f / watchHandScale,
                1.0f / watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )

            clockHandPaint.color = style.minuteHandColor
            canvas.scale(
                watchHandScale,
                watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            canvas.rotate(minuteRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.drawPath(minuteHandFill, clockHandPaint)
            canvas.rotate(-minuteRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.scale(
                1.0f / watchHandScale,
                1.0f / watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )

            val secondsRot = seconds / 60.0f * 360.0f

            clockHandPaint.color = style.secondsHandColor
            canvas.scale(
                watchHandScale,
                watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
            canvas.rotate(secondsRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.drawPath(secondHand, clockHandPaint)
            canvas.rotate(-secondsRot, bounds.exactCenterX(), bounds.exactCenterY())
            canvas.scale(
                1.0f / watchHandScale,
                1.0f / watchHandScale,
                bounds.exactCenterX(),
                bounds.exactCenterY()
            )
        }

        canvas.restore()
    }

    private fun recalculateClockHands(bounds: Rect) {
        val rx = 1.5f
        val ry = 1.5f

        hourHandBorder =
            createClockHand(bounds, HOUR_HAND_LENGTH_FRACTION, HOUR_HAND_THICKNESS_FRACTION, rx, ry)

        minuteHandBorder =
            createClockHand(
                bounds, MINUTE_HAND_LENGTH_FRACTION, MINUTE_HAND_THICKNESS_FRACTION, rx, ry
            )

        hourHandFill =
            createClockHand(
                bounds,
                HOUR_HAND_LENGTH_FRACTION,
                HOUR_HAND_THICKNESS_FRACTION,
                rx,
                ry
            )

        minuteHandFill =
            createClockHand(
                bounds,
                MINUTE_HAND_LENGTH_FRACTION,
                MINUTE_HAND_THICKNESS_FRACTION,
                rx,
                ry
            )

        secondHand =
            createClockHand(
                bounds,
                SECOND_HAND_LENGTH_FRACTION,
                SECOND_HAND_THICKNESS_FRACTION,
                0.0f,
                0.0f
            )
    }

    /**
     * Returns a round rect clock hand if {@code rx} and {@code ry} equals to 0, otherwise return a
     * rect clock hand.
     *
     * @param bounds The bounds use to determine the coordinate of the clock hand.
     * @param length Clock hand's length, in fraction of {@code bounds.width()}.
     * @param thickness Clock hand's thickness, in fraction of {@code bounds.width()}.
     * @param rx The x-radius of the rounded corners on the round-rectangle.
     * @param ry The y-radius of the rounded corners on the round-rectangle.
     */
    private fun createClockHand(
        bounds: Rect,
        length: Float,
        thickness: Float,
        rx: Float,
        ry: Float
    ): Path {
        val width = bounds.width()
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val left = cx - thickness / 2 * width
        val top = cy - (GAP_BETWEEN_HAND_AND_CENTER_FRACTION + length) * width
        val right = cx + thickness / 2 * width
        val bottom = cy - GAP_BETWEEN_HAND_AND_CENTER_FRACTION * width
        val path = Path()
        if (rx != 0.0f || ry != 0.0f) {
            path.addRoundRect(left, top, right, bottom, rx, ry, Path.Direction.CW)
        } else {
            path.addRect(left, top, right, bottom, Path.Direction.CW)
        }
        return path
    }

    private fun drawComplications(canvas: Canvas, calendar: Calendar) {
        val screen = Rect(0, 0, canvas.width, canvas.height)
        for ((_, complication) in complicationSet.complications) {
            complication.draw(canvas, calendar, drawMode)
            if (drawMode == DrawMode.COMPLICATION_SELECT) {
                drawComplicationSelectDashBorders(
                    canvas,
                    complication.boundsProvider.computeBounds(complication, screen)
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
        drawArcDashBorder(canvas, cx, cy, radius, startAngle, DASH_LENGTH, dashCount)

        // Draw right arc dash.
        cx = bounds.right - radius
        cy = bounds.centerY().toFloat()
        startAngle = (Math.PI / 2.0f).toFloat() * 3.0f
        drawArcDashBorder(canvas, cx, cy, radius, startAngle, DASH_LENGTH, dashCount)

        // Draw straight line dash.
        val rectangleWidth = bounds.width() - 2.0f * radius - 2.0f * DASH_GAP
        val cnt = (rectangleWidth / (DASH_WIDTH + DASH_GAP)).toInt()
        val baseX: Float = bounds.left + radius + DASH_GAP
        val fixGap: Float = (rectangleWidth - cnt * DASH_WIDTH) / (cnt - 1)
        for (i in 0 until cnt) {
            val startX: Float = baseX + i * (fixGap + DASH_WIDTH) + DASH_WIDTH / 2
            var startY = bounds.top.toFloat()
            var endY: Float = bounds.top - DASH_LENGTH
            canvas.drawLine(startX, startY, startX, endY, mDashPaint)
            startY = bounds.bottom.toFloat()
            endY = startY + DASH_LENGTH
            canvas.drawLine(startX, startY, startX, endY, mDashPaint)
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
            canvas.drawLine(startX, startY, endX, endY, mDashPaint)
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
            canvas.drawLine(startX, startY, endX, endY, mDashPaint)
        }
    }

    private fun drawNumberStyleOuterElement(canvas: Canvas, bounds: Rect, style: ColorStyle) {
        val textBounds = Rect()
        textPaint.color = style.outerElementColor
        for (i in 0 until 4) {
            val rot = 0.5f * (i + 1).toFloat() * Math.PI
            val dx = sin(rot).toFloat() * NUMBER_RADIUS_FRACTION * bounds.width().toFloat()
            val dy = -cos(rot).toFloat() * NUMBER_RADIUS_FRACTION * bounds.width().toFloat()
            textPaint.getTextBounds(HOUR_MARKS[i], 0, HOUR_MARKS[i].length, textBounds)
            canvas.drawText(
                HOUR_MARKS[i],
                bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                textPaint
            )
        }

        // Draws the circle for the remain hour indicators.
        outerElementPaint.strokeWidth = OUTER_CIRCLE_STROKE_THICKNESS_FRACTION * bounds.width()
        outerElementPaint.color = style.outerElementColor
        canvas.save()
        for (i in 0 until 12) {
            if (i % 3 != 0) {
                drawTopMiddleCircle(
                    canvas,
                    bounds,
                    NUMBER_STYLE_OUTER_CIRCLE_RADIUS_FRACTION
                )
            }
            canvas.rotate(360.0f / 12.0f, bounds.exactCenterX(), bounds.exactCenterY())
        }
        canvas.restore()
    }

    /** Draws the outer circle on the top middle of the given bounds. */
    private fun drawTopMiddleCircle(
        canvas: Canvas,
        bounds: Rect,
        radiusFraction: Float
    ) {
        outerElementPaint.style = Paint.Style.FILL_AND_STROKE

        val cx = 0.5f * bounds.width().toFloat()
        val cy = bounds.width() * (GAP_BETWEEN_OUTER_CIRCLE_AND_BORDER_FRACTION + radiusFraction)

        canvas.drawCircle(
            cx,
            cy,
            radiusFraction * bounds.width(),
            outerElementPaint
        )
    }
}
