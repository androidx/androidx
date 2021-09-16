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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.view.SurfaceHolder
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.BooleanUserStyleSetting.BooleanOption
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
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

const val COLOR_STYLE_SETTING = "color_style_setting"
const val RED_STYLE = "red_style"
const val GREEN_STYLE = "green_style"
const val BLUE_STYLE = "blue_style"
const val DRAW_HOUR_PIPS_STYLE_SETTING = "draw_hour_pips_style_setting"
const val WATCH_HAND_LENGTH_STYLE_SETTING = "watch_hand_length_style_setting"
const val COMPLICATIONS_STYLE_SETTING = "complications_style_setting"
const val NO_COMPLICATIONS = "NO_COMPLICATIONS"
const val LEFT_COMPLICATION = "LEFT_COMPLICATION"
const val RIGHT_COMPLICATION = "RIGHT_COMPLICATION"
const val LEFT_AND_RIGHT_COMPLICATIONS = "LEFT_AND_RIGHT_COMPLICATIONS"

/** How long each frame is displayed at expected frame rate.  */
private const val FRAME_PERIOD_MS: Long = 16L

const val EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID = 101
const val EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID = 102

/** A simple example canvas based analog watch face. NB this is open for testing. */
open class ExampleCanvasAnalogWatchFaceService : WatchFaceService() {
    // Lazy because the context isn't initialized til later.
    private val watchFaceStyle by lazy { WatchFaceColorStyle.create(this, RED_STYLE) }

    private val colorStyleSetting by lazy {
        ListUserStyleSetting(
            UserStyleSetting.Id(COLOR_STYLE_SETTING),
            resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options = listOf(
                ListUserStyleSetting.ListOption(
                    Option.Id(RED_STYLE),
                    resources,
                    R.string.colors_style_red,
                    Icon.createWithResource(this, R.drawable.red_style)
                ),
                ListUserStyleSetting.ListOption(
                    Option.Id(GREEN_STYLE),
                    resources,
                    R.string.colors_style_green,
                    Icon.createWithResource(this, R.drawable.green_style)
                ),
                ListUserStyleSetting.ListOption(
                    Option.Id(BLUE_STYLE),
                    resources,
                    R.string.colors_style_blue,
                    Icon.createWithResource(this, R.drawable.blue_style)
                )
            ),
            listOf(
                WatchFaceLayer.BASE,
                WatchFaceLayer.COMPLICATIONS,
                WatchFaceLayer.COMPLICATIONS_OVERLAY
            )
        )
    }

    private val drawHourPipsStyleSetting by lazy {
        BooleanUserStyleSetting(
            UserStyleSetting.Id(DRAW_HOUR_PIPS_STYLE_SETTING),
            resources,
            R.string.watchface_pips_setting,
            R.string.watchface_pips_setting_description,
            null,
            listOf(WatchFaceLayer.BASE),
            true
        )
    }

    private val watchHandLengthStyleSetting by lazy {
        DoubleRangeUserStyleSetting(
            UserStyleSetting.Id(WATCH_HAND_LENGTH_STYLE_SETTING),
            resources,
            R.string.watchface_hand_length_setting,
            R.string.watchface_hand_length_setting_description,
            null,
            0.25,
            1.0,
            listOf(WatchFaceLayer.COMPLICATIONS_OVERLAY),
            0.75
        )
    }

    // These are style overrides applied on top of the complicationSlots passed into
    // complicationSlotsManager below.
    private val complicationsStyleSetting by lazy {
        ComplicationSlotsUserStyleSetting(
            UserStyleSetting.Id(COMPLICATIONS_STYLE_SETTING),
            resources,
            R.string.watchface_complications_setting,
            R.string.watchface_complications_setting_description,
            icon = null,
            complicationConfig = listOf(
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id(LEFT_AND_RIGHT_COMPLICATIONS),
                    resources,
                    R.string.watchface_complications_setting_both,
                    null,
                    // NB this list is empty because each [ComplicationSlotOverlay] is applied on
                    // top of the initial config.
                    listOf()
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id(NO_COMPLICATIONS),
                    resources,
                    R.string.watchface_complications_setting_none,
                    null,
                    listOf(
                        ComplicationSlotOverlay(
                            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                            enabled = false
                        ),
                        ComplicationSlotOverlay(
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            enabled = false
                        )
                    )
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id(LEFT_COMPLICATION),
                    resources,
                    R.string.watchface_complications_setting_left,
                    null,
                    listOf(
                        ComplicationSlotOverlay(
                            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
                            enabled = false
                        )
                    )
                ),
                ComplicationSlotsUserStyleSetting.ComplicationSlotsOption(
                    Option.Id(RIGHT_COMPLICATION),
                    resources,
                    R.string.watchface_complications_setting_right,
                    null,
                    listOf(
                        ComplicationSlotOverlay(
                            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
                            enabled = false
                        )
                    )
                )
            ),
            listOf(WatchFaceLayer.COMPLICATIONS)
        )
    }

    public override fun createUserStyleSchema() = UserStyleSchema(
        listOf(
            colorStyleSetting,
            drawHourPipsStyleSetting,
            watchHandLengthStyleSetting,
            complicationsStyleSetting
        )
    )

    public override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val canvasComplicationFactory =
            CanvasComplicationFactory { watchState, listener ->
                CanvasComplicationDrawable(
                    watchFaceStyle.getDrawable(this@ExampleCanvasAnalogWatchFaceService)!!,
                    watchState,
                    listener
                )
            }
        val leftComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID,
            canvasComplicationFactory,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK),
            ComplicationSlotBounds(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        ).setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .build()
        val rightComplication = ComplicationSlot.createRoundRectComplicationSlotBuilder(
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID,
            canvasComplicationFactory,
            listOf(
                ComplicationType.RANGED_VALUE,
                ComplicationType.LONG_TEXT,
                ComplicationType.SHORT_TEXT,
                ComplicationType.MONOCHROMATIC_IMAGE,
                ComplicationType.SMALL_IMAGE
            ),
            DefaultComplicationDataSourcePolicy(SystemDataSources.DATA_SOURCE_STEP_COUNT),
            ComplicationSlotBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        ).setDefaultDataSourceType(ComplicationType.SHORT_TEXT)
            .build()
        return ComplicationSlotsManager(
            listOf(leftComplication, rightComplication),
            currentUserStyleRepository
        )
    }

    public override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.ANALOG,
        ExampleAnalogWatchCanvasRenderer(
            surfaceHolder,
            this,
            watchFaceStyle,
            currentUserStyleRepository,
            watchState,
            colorStyleSetting,
            drawHourPipsStyleSetting,
            watchHandLengthStyleSetting,
            complicationSlotsManager
        )
    )
}

class ExampleAnalogWatchCanvasRenderer(
    surfaceHolder: SurfaceHolder,
    private val context: Context,
    private var watchFaceColorStyle: WatchFaceColorStyle,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val colorStyleSetting: ListUserStyleSetting,
    private val drawPipsStyleSetting: BooleanUserStyleSetting,
    private val watchHandLengthStyleSettingDouble: DoubleRangeUserStyleSetting,
    private val complicationSlotsManager: ComplicationSlotsManager
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    FRAME_PERIOD_MS
) {
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
    }

    private lateinit var hourHandFill: Path
    private lateinit var hourHandBorder: Path
    private lateinit var minuteHandFill: Path
    private lateinit var minuteHandBorder: Path
    private lateinit var secondHand: Path

    private var drawHourPips = true
    private var watchHandScale = 1.0f

    init {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            currentUserStyleRepository.userStyle.collect { userStyle ->
                watchFaceColorStyle = WatchFaceColorStyle.create(
                    context,
                    userStyle[colorStyleSetting]!!.toString()
                )

                // Apply the userStyle to the complicationSlots. ComplicationDrawables for each
                // of the styles are defined in XML so we need to replace the complication's
                // drawables.
                for ((_, complication) in complicationSlotsManager.complicationSlots) {
                    (complication.renderer as CanvasComplicationDrawable).drawable =
                        watchFaceColorStyle.getDrawable(context)!!
                }

                drawHourPips = (userStyle[drawPipsStyleSetting]!! as BooleanOption).value
                watchHandScale =
                    (userStyle[watchHandLengthStyleSettingDouble]!! as DoubleRangeOption)
                        .value.toFloat()
            }
        }
    }

    override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        val style = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            watchFaceColorStyle.ambientStyle
        } else {
            watchFaceColorStyle.activeStyle
        }

        canvas.drawColor(style.backgroundColor)

        // We don't need to check renderParameters.watchFaceWatchFaceLayers because
        // CanvasComplicationDrawable does that for us.
        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.COMPLICATIONS_OVERLAY)
        ) {
            drawClockHands(canvas, bounds, zonedDateTime, style)
        }

        if (renderParameters.drawMode != DrawMode.AMBIENT &&
            renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE) && drawHourPips
        ) {
            drawNumberStyleOuterElement(canvas, bounds, style)
        }
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        for ((_, complication) in complicationSlotsManager.complicationSlots) {
            if (complication.enabled) {
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }
    }

    private fun drawClockHands(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        style: ColorStyle
    ) {
        recalculateClockHands(bounds)
        val hours = (zonedDateTime.hour % 12).toFloat()
        val minutes = zonedDateTime.minute.toFloat()
        val seconds = zonedDateTime.second.toFloat() +
            (zonedDateTime.nano.toDouble() / 1000000000.0).toFloat()

        val hourRot = (hours + minutes / 60.0f + seconds / 3600.0f) / 12.0f * 360.0f
        val minuteRot = (minutes + seconds / 60.0f) / 60.0f * 360.0f

        canvas.save()

        recalculateClockHands(bounds)

        if (renderParameters.drawMode == DrawMode.AMBIENT) {
            clockHandPaint.style = Paint.Style.STROKE
            clockHandPaint.color = style.primaryColor
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

            clockHandPaint.color = style.secondaryColor
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
            clockHandPaint.color = style.primaryColor
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

            clockHandPaint.color = style.secondaryColor
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

            clockHandPaint.color = style.secondaryColor
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
