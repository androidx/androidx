/*
 * Copyright 2022 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.view.SurfaceHolder
import androidx.annotation.Px
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlot
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.DefaultComplicationDataSourcePolicy
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotOverlay
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class ExampleHierarchicalStyleWatchFaceService : SampleWatchFaceService() {

    internal val twelveHourClockOption by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id("12_style"),
                resources,
                R.string.digital_clock_style_12,
                R.string.digital_clock_style_12_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.red_style) }
            .build()
    }

    internal val twentyFourHourClockOption by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id("24_style"),
                resources,
                R.string.digital_clock_style_24,
                R.string.digital_clock_style_24_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.red_style) }
            .build()
    }

    @Suppress("Deprecation")
    private val digitalComplicationSettings by lazy {
        ComplicationSlotsUserStyleSetting.Builder(
                UserStyleSetting.Id("DigitalComplications"),
                listOf(
                    ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                            UserStyleSetting.Option.Id("On"),
                            listOf(
                                ComplicationSlotOverlay(
                                    COMPLICATION1_ID,
                                    enabled = true,
                                    complicationSlotBounds =
                                        ComplicationSlotBounds(RectF(0.1f, 0.4f, 0.3f, 0.6f))
                                ),
                                ComplicationSlotOverlay(COMPLICATION2_ID, enabled = false),
                                ComplicationSlotOverlay(COMPLICATION3_ID, enabled = false)
                            ),
                            resources,
                            R.string.digital_complication_on_screen_name,
                            R.string.digital_complication_on_screen_name
                        )
                        .setIcon { Icon.createWithResource(this, R.drawable.on) }
                        .build(),
                    ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                            UserStyleSetting.Option.Id("Off"),
                            listOf(
                                ComplicationSlotOverlay(COMPLICATION1_ID, enabled = false),
                                ComplicationSlotOverlay(COMPLICATION2_ID, enabled = false),
                                ComplicationSlotOverlay(COMPLICATION3_ID, enabled = false)
                            ),
                            resources,
                            R.string.digital_complication_off_screen_name,
                            R.string.digital_complication_off_screen_name
                        )
                        .setIcon { Icon.createWithResource(this, R.drawable.off) }
                        .build()
                ),
                listOf(WatchFaceLayer.COMPLICATIONS),
                resources,
                R.string.digital_complications_setting,
                R.string.digital_complications_setting_description
            )
            .build()
    }

    internal val digitalClockStyleSetting by lazy {
        UserStyleSetting.ListUserStyleSetting.Builder(
                UserStyleSetting.Id("digital_clock_style"),
                listOf(twelveHourClockOption, twentyFourHourClockOption),
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                resources,
                R.string.digital_clock_style_name,
                R.string.digital_clock_style_description
            )
            .build()
    }

    internal val redStyle by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id(RED_STYLE),
                resources,
                R.string.colors_style_red,
                R.string.colors_style_red_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.red_style) }
            .build()
    }

    internal val greenStyle by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id(GREEN_STYLE),
                resources,
                R.string.colors_style_green,
                R.string.colors_style_green_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.green_style) }
            .build()
    }

    internal val blueStyle by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id(BLUE_STYLE),
                resources,
                R.string.colors_style_blue,
                R.string.colors_style_blue_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.blue_style) }
            .build()
    }

    internal val colorStyleSetting by lazy {
        UserStyleSetting.ListUserStyleSetting.Builder(
                UserStyleSetting.Id(COLOR_STYLE_SETTING),
                listOf(redStyle, greenStyle, blueStyle),
                listOf(
                    WatchFaceLayer.BASE,
                    WatchFaceLayer.COMPLICATIONS,
                    WatchFaceLayer.COMPLICATIONS_OVERLAY
                ),
                resources,
                R.string.colors_style_setting,
                R.string.colors_style_setting_description
            )
            .build()
    }

    internal val drawHoursSetting by lazy {
        UserStyleSetting.BooleanUserStyleSetting.Builder(
                UserStyleSetting.Id(HOURS_STYLE_SETTING),
                listOf(WatchFaceLayer.BASE),
                defaultValue = true,
                resources,
                R.string.watchface_draw_hours_setting,
                R.string.watchface_draw_hours_setting_description
            )
            .build()
    }

    private val analogComplicationSettings by lazy {
        ComplicationSlotsUserStyleSetting.Builder(
                UserStyleSetting.Id("AnalogComplications"),
                listOf(
                    ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                            UserStyleSetting.Option.Id("One"),
                            listOf(
                                ComplicationSlotOverlay.Builder(COMPLICATION1_ID)
                                    .setEnabled(true)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION2_ID)
                                    .setEnabled(false)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION3_ID)
                                    .setEnabled(false)
                                    .build()
                            ),
                            resources,
                            R.string.analog_complication_one_screen_name,
                            R.string.analog_complication_one_screen_name
                        )
                        .setIcon { Icon.createWithResource(this, R.drawable.one) }
                        .build(),
                    ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                            UserStyleSetting.Option.Id("Two"),
                            listOf(
                                ComplicationSlotOverlay.Builder(COMPLICATION1_ID)
                                    .setEnabled(true)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION2_ID)
                                    .setEnabled(true)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION3_ID)
                                    .setEnabled(false)
                                    .build(),
                            ),
                            resources,
                            R.string.analog_complication_two_screen_name,
                            R.string.analog_complication_two_screen_name
                        )
                        .setIcon { Icon.createWithResource(this, R.drawable.two) }
                        .build(),
                    ComplicationSlotsUserStyleSetting.ComplicationSlotsOption.Builder(
                            UserStyleSetting.Option.Id("Three"),
                            listOf(
                                ComplicationSlotOverlay.Builder(COMPLICATION1_ID)
                                    .setEnabled(true)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION2_ID)
                                    .setEnabled(true)
                                    .build(),
                                ComplicationSlotOverlay.Builder(COMPLICATION3_ID)
                                    .setEnabled(true)
                                    .build()
                            ),
                            resources,
                            R.string.analog_complication_three_screen_name,
                            R.string.analog_complication_three_screen_name
                        )
                        .setIcon { Icon.createWithResource(this, R.drawable.three) }
                        .build()
                ),
                listOf(WatchFaceLayer.COMPLICATIONS),
                resources,
                R.string.watchface_complications_setting,
                R.string.watchface_complications_setting_description
            )
            .build()
    }

    internal val digitalWatchFaceType by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id("digital"),
                resources,
                R.string.style_digital_watch,
                R.string.style_digital_watch_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.d) }
            .setChildSettings(
                listOf(digitalClockStyleSetting, colorStyleSetting, digitalComplicationSettings)
            )
            .build()
    }

    internal val analogWatchFaceType by lazy {
        ListOption.Builder(
                UserStyleSetting.Option.Id("analog"),
                resources,
                R.string.style_analog_watch,
                R.string.style_analog_watch_screen_reader
            )
            .setIcon { Icon.createWithResource(this, R.drawable.a) }
            .setChildSettings(
                listOf(digitalClockStyleSetting, colorStyleSetting, digitalComplicationSettings)
            )
            .build()
    }

    internal val watchFaceType by lazy {
        UserStyleSetting.ListUserStyleSetting.Builder(
                UserStyleSetting.Id("clock_type"),
                options = listOf(digitalWatchFaceType, analogWatchFaceType),
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                resources,
                R.string.clock_type,
                R.string.clock_type_description
            )
            .build()
    }

    public override fun createUserStyleSchema() =
        UserStyleSchema(
            listOf(
                watchFaceType,
                digitalClockStyleSetting,
                colorStyleSetting,
                drawHoursSetting,
                digitalComplicationSettings,
                analogComplicationSettings
            )
        )

    private val watchFaceStyle by lazy { WatchFaceColorStyle.create(this, "red_style") }

    @OptIn(ComplicationExperimental::class)
    public override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ): ComplicationSlotsManager {
        val canvasComplicationFactory = CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                watchFaceStyle.getDrawable(this@ExampleHierarchicalStyleWatchFaceService)!!,
                watchState,
                listener
            )
        }

        val complicationOne =
            ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    COMPLICATION1_ID,
                    canvasComplicationFactory,
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.GOAL_PROGRESS,
                        ComplicationType.WEIGHTED_ELEMENTS,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_WATCH_BATTERY,
                        ComplicationType.RANGED_VALUE
                    ),
                    ComplicationSlotBounds(RectF(0.6f, 0.1f, 0.8f, 0.3f))
                )
                .setNameResourceId(R.string.hierarchical_complication1_screen_name)
                .setScreenReaderNameResourceId(
                    R.string.hierarchical_complication1_screen_reader_name
                )
                .build()

        val complicationTwo =
            ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    COMPLICATION2_ID,
                    canvasComplicationFactory,
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.GOAL_PROGRESS,
                        ComplicationType.WEIGHTED_ELEMENTS,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_TIME_AND_DATE,
                        ComplicationType.SHORT_TEXT
                    ),
                    ComplicationSlotBounds(RectF(0.6f, 0.4f, 0.8f, 0.6f))
                )
                .setNameResourceId(R.string.hierarchical_complication2_screen_name)
                .setScreenReaderNameResourceId(
                    R.string.hierarchical_complication2_screen_reader_name
                )
                .build()

        val complicationThree =
            ComplicationSlot.createRoundRectComplicationSlotBuilder(
                    COMPLICATION3_ID,
                    canvasComplicationFactory,
                    listOf(
                        ComplicationType.RANGED_VALUE,
                        ComplicationType.GOAL_PROGRESS,
                        ComplicationType.WEIGHTED_ELEMENTS,
                        ComplicationType.SHORT_TEXT,
                        ComplicationType.MONOCHROMATIC_IMAGE,
                        ComplicationType.SMALL_IMAGE
                    ),
                    DefaultComplicationDataSourcePolicy(
                        SystemDataSources.DATA_SOURCE_SUNRISE_SUNSET,
                        ComplicationType.SHORT_TEXT
                    ),
                    ComplicationSlotBounds(RectF(0.6f, 0.7f, 0.8f, 0.9f))
                )
                .setNameResourceId(R.string.hierarchical_complication3_screen_name)
                .setScreenReaderNameResourceId(
                    R.string.hierarchical_complication3_screen_reader_name
                )
                .build()

        return ComplicationSlotsManager(
            listOf(complicationOne, complicationTwo, complicationThree),
            currentUserStyleRepository
        )
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        WatchFace(
            WatchFaceType.ANALOG,
            @Suppress("Deprecation")
            object :
                Renderer.CanvasRenderer(
                    surfaceHolder,
                    currentUserStyleRepository,
                    watchState,
                    CanvasType.HARDWARE,
                    16L
                ) {
                val renderer = ExampleHierarchicalStyleWatchFaceRenderer()
                val context: Context = this@ExampleHierarchicalStyleWatchFaceService

                init {
                    CoroutineScope(Dispatchers.Main.immediate).launch {
                        currentUserStyleRepository.userStyle.collect { userStyle ->
                            for ((_, complication) in complicationSlotsManager.complicationSlots) {
                                (complication.renderer as CanvasComplicationDrawable).drawable =
                                    when (userStyle[colorStyleSetting]) {
                                        redStyle ->
                                            WatchFaceColorStyle.create(context, "red_style")
                                                .getDrawable(context)!!
                                        greenStyle ->
                                            WatchFaceColorStyle.create(context, "green_style")
                                                .getDrawable(context)!!
                                        blueStyle ->
                                            WatchFaceColorStyle.create(context, "blue_style")
                                                .getDrawable(context)!!
                                        else ->
                                            throw IllegalArgumentException(
                                                "Unrecognized colorStyleSetting "
                                            )
                                    }
                            }
                        }
                    }
                }

                override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                    val currentStyle = currentUserStyleRepository.userStyle.value
                    when (currentStyle[watchFaceType]) {
                        digitalWatchFaceType ->
                            renderer.renderDigital(
                                canvas,
                                bounds,
                                zonedDateTime,
                                renderParameters,
                                watchState,
                                currentStyle[digitalClockStyleSetting] == twentyFourHourClockOption,
                                when (currentStyle[colorStyleSetting]) {
                                    redStyle -> intArrayOf(1, 0, 0)
                                    greenStyle -> intArrayOf(0, 1, 0)
                                    blueStyle -> intArrayOf(0, 0, 1)
                                    else ->
                                        throw IllegalArgumentException(
                                            "Unrecognized colorStyleSetting " +
                                                currentStyle[colorStyleSetting]
                                        )
                                }
                            )
                        analogWatchFaceType ->
                            renderer.renderAnalog(
                                canvas,
                                bounds,
                                zonedDateTime,
                                renderParameters,
                                (currentStyle[drawHoursSetting]!!
                                        as UserStyleSetting.BooleanUserStyleSetting.BooleanOption)
                                    .value,
                            )
                        else -> {
                            throw IllegalStateException(
                                "Unrecognized chosenSettingId " +
                                    currentUserStyleRepository.userStyle.value
                            )
                        }
                    }

                    for ((_, complication) in complicationSlotsManager.complicationSlots) {
                        if (complication.enabled) {
                            complication.render(canvas, zonedDateTime, renderParameters)
                        }
                    }
                }

                override fun renderHighlightLayer(
                    canvas: Canvas,
                    bounds: Rect,
                    zonedDateTime: ZonedDateTime
                ) {
                    for ((_, complication) in complicationSlotsManager.complicationSlots) {
                        if (complication.enabled) {
                            complication.renderHighlightLayer(
                                canvas,
                                zonedDateTime,
                                renderParameters
                            )
                        }
                    }
                }
            }
        )

    private class ExampleHierarchicalStyleWatchFaceRenderer {
        internal val paint =
            Paint().apply {
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

        internal val textBounds = Rect()

        @Px
        internal val timeActiveHeight =
            Rect()
                .apply {
                    paint.textSize = TIME_TEXT_ACTIVE_HEIGHT
                    paint.getTextBounds(timeText, 0, timeText.size, this)
                }
                .height()

        @Px
        internal val timeAmbientHeight =
            Rect()
                .apply {
                    paint.textSize = TIME_TEXT_AMBIENT_HEIGHT
                    paint.getTextBounds(timeText, 0, timeText.size, this)
                }
                .height()

        @Px
        internal val secondsHeight =
            Rect()
                .apply {
                    paint.textSize = SECONDS_TEXT_HEIGHT
                    paint.getTextBounds(secondsText, 0, secondsText.size, this)
                }
                .height()

        @Px
        internal val timeActiveOffset =
            (timeActiveHeight + secondsHeight + TEXT_PADDING) / 2 - timeActiveHeight

        @Px internal val timeAmbientOffset = timeAmbientHeight / 2 - timeAmbientHeight

        @Px internal val secondsActiveOffset = timeActiveOffset - secondsHeight - TEXT_PADDING

        fun renderDigital(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters,
            watchState: WatchState,
            twentyFourHour: Boolean,
            color: IntArray
        ) {
            val isActive = renderParameters.drawMode !== DrawMode.AMBIENT
            val hour = zonedDateTime.hour % if (twentyFourHour) 24 else 12
            val minute = zonedDateTime.minute
            val second = zonedDateTime.second

            paint.color =
                if (isActive) {
                    val scale = 64 + 192 * second / 60
                    Color.rgb(color[0] * scale, color[1] * scale, color[2] * scale)
                } else {
                    Color.BLACK
                }
            canvas.drawRect(bounds, paint)

            paint.color = Color.WHITE
            timeText[0] = ('0' + hour / 10)
            timeText[1] = ('0' + hour % 10)
            timeText[2] = if (second % 2 == 0) ':' else ' '
            timeText[3] = ('0' + minute / 10)
            timeText[4] = ('0' + minute % 10)
            paint.textSize = if (isActive) TIME_TEXT_ACTIVE_HEIGHT else TIME_TEXT_AMBIENT_HEIGHT
            val timeOffset = if (isActive) timeActiveOffset else timeAmbientOffset
            canvas.drawText(
                timeText,
                0,
                timeText.size,
                bounds.centerX().toFloat(),
                (bounds.centerY() - watchState.chinHeight - timeOffset).toFloat(),
                paint
            )

            paint.textSize = SECONDS_TEXT_HEIGHT
            if (isActive) {
                secondsText[0] = ('0' + second / 10)
                secondsText[1] = ('0' + second % 10)
                canvas.drawText(
                    secondsText,
                    0,
                    secondsText.size,
                    bounds.centerX().toFloat(),
                    (bounds.centerY() - watchState.chinHeight - secondsActiveOffset).toFloat(),
                    paint
                )
            }
        }

        fun renderAnalog(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime,
            renderParameters: RenderParameters,
            drawHourPips: Boolean
        ) {
            val isActive = renderParameters.drawMode !== DrawMode.AMBIENT

            paint.color = Color.BLACK
            canvas.drawRect(bounds, paint)

            paint.color = Color.WHITE
            paint.textSize = 20.0f
            if (isActive && drawHourPips) {
                for (i in 12 downTo 1 step 3) {
                    val rot = i.toFloat() / 12.0f * 2.0f * Math.PI
                    val dx = sin(rot).toFloat() * NUMBER_RADIUS_FRACTION * bounds.width().toFloat()
                    val dy = -cos(rot).toFloat() * NUMBER_RADIUS_FRACTION * bounds.width().toFloat()
                    val mark = i.toString()
                    paint.getTextBounds(mark, 0, mark.length, textBounds)
                    canvas.drawText(
                        mark,
                        bounds.exactCenterX() + dx - textBounds.width() / 2.0f,
                        bounds.exactCenterY() + dy + textBounds.height() / 2.0f,
                        paint
                    )
                }
            }

            val hours = (zonedDateTime.hour % 12).toFloat()
            val minutes = zonedDateTime.minute.toFloat()
            val seconds =
                zonedDateTime.second.toFloat() +
                    (zonedDateTime.nano.toDouble() / 1000000000.0).toFloat()

            val hourRot = (hours + minutes / 60.0f + seconds / 3600.0f) / 12.0f * 360.0f
            val minuteRot = (minutes + seconds / 60.0f) / 60.0f * 360.0f

            val hourXRadius = bounds.width() * 0.3f
            val hourYRadius = bounds.height() * 0.3f

            paint.strokeWidth = if (isActive) 8f else 5f
            canvas.drawLine(
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                bounds.exactCenterX() + sin(hourRot) * hourXRadius,
                bounds.exactCenterY() - cos(hourRot) * hourYRadius,
                paint
            )

            val minuteXRadius = bounds.width() * 0.4f
            val minuteYRadius = bounds.height() * 0.4f

            paint.strokeWidth = if (isActive) 4f else 2.5f
            canvas.drawLine(
                bounds.exactCenterX(),
                bounds.exactCenterY(),
                bounds.exactCenterX() + sin(minuteRot) * minuteXRadius,
                bounds.exactCenterY() - cos(minuteRot) * minuteYRadius,
                paint
            )
        }
    }

    companion object {
        private const val COLOR_STYLE_SETTING = "color_style_setting"
        private const val RED_STYLE = "red_style"
        private const val GREEN_STYLE = "green_style"
        private const val BLUE_STYLE = "blue_style"

        private const val HOURS_STYLE_SETTING = "hours_style_setting"
        private const val NUMBER_RADIUS_FRACTION = 0.45f

        private val timeText = charArrayOf('1', '0', ':', '0', '9')
        private val secondsText = charArrayOf('3', '0')

        @Px private val SECONDS_TEXT_HEIGHT = 180f

        @Px private val TIME_TEXT_ACTIVE_HEIGHT = 64f

        @Px private val TIME_TEXT_AMBIENT_HEIGHT = 96f

        @Px private val TEXT_PADDING = 12

        const val COMPLICATION1_ID = 101
        const val COMPLICATION2_ID = 102
        const val COMPLICATION3_ID = 103
    }
}
