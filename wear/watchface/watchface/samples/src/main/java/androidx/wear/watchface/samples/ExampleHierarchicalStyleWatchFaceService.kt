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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.view.SurfaceHolder
import androidx.annotation.Px
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.ExperimentalHierarchicalStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.DoubleRangeUserStyleSetting.DoubleRangeOption
import androidx.wear.watchface.style.UserStyleSetting.ListUserStyleSetting.ListOption
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.LongRangeUserStyleSetting.LongRangeOption
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.sin

private val timeText = charArrayOf('1', '0', ':', '0', '9')
private val secondsText = charArrayOf('3', '0')

@Px
private val SECONDS_TEXT_HEIGHT = 180f

@Px
private val TIME_TEXT_ACTIVE_HEIGHT = 64f

@Px
private val TIME_TEXT_AMBIENT_HEIGHT = 96f

@Px
private val TEXT_PADDING = 12

open class ExampleHierarchicalStyleWatchFaceService : WatchFaceService() {

    internal val twelveHourClockOption by lazy {
        ListOption(
            UserStyleSetting.Option.Id("12_style"),
            resources,
            R.string.digital_clock_style_12,
            icon = null
        )
    }

    internal val twentyFourHourClockOption by lazy {
        ListOption(
            UserStyleSetting.Option.Id("24_style"),
            resources,
            R.string.digital_clock_style_24,
            icon = null
        )
    }

    internal val digitalClockStyleSetting by lazy {
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id("digital_clock_style"),
            resources,
            R.string.digital_clock_style_name,
            R.string.digital_clock_style_description,
            null,
            listOf(twelveHourClockOption, twentyFourHourClockOption),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )
    }

    internal val redStyle by lazy {
        ListOption(
            UserStyleSetting.Option.Id(RED_STYLE),
            resources,
            R.string.colors_style_red,
            Icon.createWithResource(this, R.drawable.red_style)
        )
    }

    internal val greenStyle by lazy {
        ListOption(
            UserStyleSetting.Option.Id(GREEN_STYLE),
            resources,
            R.string.colors_style_green,
            Icon.createWithResource(this, R.drawable.green_style)
        )
    }

    internal val blueStyle by lazy {
        ListOption(
            UserStyleSetting.Option.Id(BLUE_STYLE),
            resources,
            R.string.colors_style_blue,
            Icon.createWithResource(this, R.drawable.blue_style)
        )
    }

    internal val colorStyleSetting by lazy {
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id(COLOR_STYLE_SETTING),
            resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options = listOf(redStyle, greenStyle, blueStyle),
            listOf(
                WatchFaceLayer.BASE,
                WatchFaceLayer.COMPLICATIONS,
                WatchFaceLayer.COMPLICATIONS_OVERLAY
            )
        )
    }

    internal val watchHandLengthStyleSetting by lazy {
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

    internal val hoursDrawFreqStyleSetting by lazy {
        LongRangeUserStyleSetting(
            UserStyleSetting.Id(HOURS_DRAW_FREQ_STYLE_SETTING),
            resources,
            R.string.watchface_draw_hours_freq_setting,
            R.string.watchface_draw_hours_freq_setting_description,
            null,
            HOURS_DRAW_FREQ_MIN,
            HOURS_DRAW_FREQ_MAX,
            listOf(WatchFaceLayer.BASE),
            HOURS_DRAW_FREQ_DEFAULT
        )
    }

    @OptIn(ExperimentalHierarchicalStyle::class)
    internal val digitalWatchFaceType by lazy {
        ListOption(
            UserStyleSetting.Option.Id("digital"),
            resources,
            R.string.style_digital_watch,
            icon = null,
            childSettings = listOf(digitalClockStyleSetting, colorStyleSetting)
        )
    }

    @OptIn(ExperimentalHierarchicalStyle::class)
    internal val analogWatchFaceType by lazy {
        ListOption(
            UserStyleSetting.Option.Id("analog"),
            resources,
            R.string.style_analog_watch,
            icon = null,
            childSettings = listOf(watchHandLengthStyleSetting, hoursDrawFreqStyleSetting)
        )
    }

    internal val watchFaceType by lazy {
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id("clock_type"),
            resources,
            R.string.clock_type,
            R.string.clock_type_description,
            icon = null,
            options = listOf(digitalWatchFaceType, analogWatchFaceType),
            WatchFaceLayer.ALL_WATCH_FACE_LAYERS
        )
    }

    public override fun createUserStyleSchema() = UserStyleSchema(
        listOf(
            watchFaceType,
            digitalClockStyleSetting,
            colorStyleSetting,
            watchHandLengthStyleSetting,
            hoursDrawFreqStyleSetting
        )
    )

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = WatchFace(
        WatchFaceType.ANALOG,
        @Suppress("Deprecation")
        object : Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            16L
        ) {
            val renderer = ExampleHierarchicalStyleWatchFaceRenderer()
            override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
                val currentStyle = currentUserStyleRepository.userStyle.value
                when (currentStyle[watchFaceType]) {
                    digitalWatchFaceType -> renderer.renderDigital(
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
                            else -> throw IllegalArgumentException(
                                "Unrecognized colorStyleSetting " +
                                    currentStyle[colorStyleSetting]
                            )
                        }
                    )

                    analogWatchFaceType -> renderer.renderAnalog(
                        canvas,
                        bounds,
                        zonedDateTime,
                        renderParameters,
                        (currentStyle[hoursDrawFreqStyleSetting]!!
                            as LongRangeOption).value.toInt(),
                        (currentStyle[watchHandLengthStyleSetting]!!
                            as DoubleRangeOption).value.toFloat()
                    )

                    else -> {
                        throw IllegalStateException(
                            "Unrecognized chosenSettingId " +
                                currentUserStyleRepository.userStyle.value
                        )
                    }
                }
            }

            override fun renderHighlightLayer(
                canvas: Canvas,
                bounds: Rect,
                zonedDateTime: ZonedDateTime
            ) {
                // Nothing to do.
            }
        }
    )
}

class ExampleHierarchicalStyleWatchFaceRenderer {
    internal val paint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    internal val textBounds = Rect()

    @Px
    internal val timeActiveHeight = Rect().apply {
        paint.textSize = TIME_TEXT_ACTIVE_HEIGHT
        paint.getTextBounds(timeText, 0, timeText.size, this)
    }.height()

    @Px
    internal val timeAmbientHeight = Rect().apply {
        paint.textSize = TIME_TEXT_AMBIENT_HEIGHT
        paint.getTextBounds(timeText, 0, timeText.size, this)
    }.height()

    @Px
    internal val secondsHeight = Rect().apply {
        paint.textSize = SECONDS_TEXT_HEIGHT
        paint.getTextBounds(secondsText, 0, secondsText.size, this)
    }.height()

    @Px
    internal val timeActiveOffset =
        (timeActiveHeight + secondsHeight + TEXT_PADDING) / 2 - timeActiveHeight

    @Px
    internal val timeAmbientOffset = timeAmbientHeight / 2 - timeAmbientHeight

    @Px
    internal val secondsActiveOffset = timeActiveOffset - secondsHeight - TEXT_PADDING

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

        paint.color = if (isActive) {
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
        hoursDrawFreq: Int,
        watchHandLength: Float
    ) {
        val isActive = renderParameters.drawMode !== DrawMode.AMBIENT

        paint.color = Color.BLACK
        canvas.drawRect(bounds, paint)

        paint.color = Color.WHITE
        paint.textSize = 20.0f
        if (isActive) {
            for (i in 12 downTo 1 step hoursDrawFreq) {
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
        val seconds = zonedDateTime.second.toFloat() +
            (zonedDateTime.nano.toDouble() / 1000000000.0).toFloat()

        val hourRot = (hours + minutes / 60.0f + seconds / 3600.0f) / 12.0f * 360.0f
        val minuteRot = (minutes + seconds / 60.0f) / 60.0f * 360.0f

        val hourXRadius = bounds.width() * watchHandLength * 0.35f
        val hourYRadius = bounds.height() * watchHandLength * 0.35f

        paint.strokeWidth = if (isActive) 8f else 5f
        canvas.drawLine(
            bounds.exactCenterX(),
            bounds.exactCenterY(),
            bounds.exactCenterX() + sin(hourRot) * hourXRadius,
            bounds.exactCenterY() - cos(hourRot) * hourYRadius,
            paint
        )

        val minuteXRadius = bounds.width() * watchHandLength * 0.499f
        val minuteYRadius = bounds.height() * watchHandLength * 0.499f

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
