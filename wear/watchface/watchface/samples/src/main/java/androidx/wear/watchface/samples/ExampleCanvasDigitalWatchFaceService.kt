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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.text.format.DateFormat
import android.util.FloatProperty
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.animation.AnimationUtils
import android.view.animation.PathInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
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
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch

/** A simple example canvas based digital watch face. */
class ExampleCanvasDigitalWatchFaceService : SampleWatchFaceService() {
    // Lazy because the context isn't initialized til later.
    private val watchFaceStyle by lazy { WatchFaceColorStyle.create(this, RED_STYLE) }

    private val colorStyleSetting by lazy {
        UserStyleSetting.ListUserStyleSetting(
            UserStyleSetting.Id(COLOR_STYLE_SETTING),
            resources,
            R.string.colors_style_setting,
            R.string.colors_style_setting_description,
            icon = null,
            options =
                listOf(
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        Option.Id(RED_STYLE),
                        resources,
                        R.string.colors_style_red,
                        R.string.colors_style_red_screen_reader,
                        { Icon.createWithResource(this, R.drawable.red_style) }
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        Option.Id(GREEN_STYLE),
                        resources,
                        R.string.colors_style_green,
                        R.string.colors_style_green_screen_reader,
                        { Icon.createWithResource(this, R.drawable.green_style) }
                    ),
                    UserStyleSetting.ListUserStyleSetting.ListOption(
                        Option.Id(BLUE_STYLE),
                        resources,
                        R.string.colors_style_blue,
                        R.string.colors_style_blue_screen_reader,
                        { Icon.createWithResource(this, R.drawable.blue_style) }
                    )
                ),
            listOf(
                WatchFaceLayer.BASE,
                WatchFaceLayer.COMPLICATIONS,
                WatchFaceLayer.COMPLICATIONS_OVERLAY
            )
        )
    }

    private val canvasComplicationFactory = CanvasComplicationFactory { watchState, listener ->
        CanvasComplicationDrawable(
            watchFaceStyle.getDrawable(this@ExampleCanvasDigitalWatchFaceService)!!,
            watchState,
            listener
        )
    }

    @OptIn(ComplicationExperimental::class)
    private val leftComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationID.LEFT.ordinal,
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
                    ComponentName(COMPLICATION_PACKAGE, "$COMPLICATION_CLASS_PREFIX\$Steps"),
                    ComplicationType.RANGED_VALUE,
                    SystemDataSources.DATA_SOURCE_WATCH_BATTERY,
                    ComplicationType.SHORT_TEXT
                ),
                ComplicationSlotBounds(
                    createBoundsRect(
                        LEFT_CIRCLE_COMPLICATION_CENTER_FRACTION,
                        CIRCLE_COMPLICATION_DIAMETER_FRACTION
                    )
                )
            )
            .setNameResourceId(R.string.left_complication_screen_name)
            .setScreenReaderNameResourceId(R.string.left_complication_screen_reader_name)
            .build()

    @OptIn(ComplicationExperimental::class)
    private val rightComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationID.RIGHT.ordinal,
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
                    ComponentName(COMPLICATION_PACKAGE, "$COMPLICATION_CLASS_PREFIX\$HeartRate"),
                    ComplicationType.RANGED_VALUE,
                    SystemDataSources.DATA_SOURCE_DATE,
                    ComplicationType.SHORT_TEXT
                ),
                ComplicationSlotBounds(
                    createBoundsRect(
                        RIGHT_CIRCLE_COMPLICATION_CENTER_FRACTION,
                        CIRCLE_COMPLICATION_DIAMETER_FRACTION
                    )
                )
            )
            .setNameResourceId(R.string.right_complication_screen_name)
            .setScreenReaderNameResourceId(R.string.right_complication_screen_reader_name)
            .build()

    private val upperAndLowerComplicationTypes =
        listOf(
            ComplicationType.LONG_TEXT,
            ComplicationType.RANGED_VALUE,
            ComplicationType.GOAL_PROGRESS,
            ComplicationType.WEIGHTED_ELEMENTS,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

    // The upper and lower complicationSlots change shape depending on the complication's type.
    @OptIn(ComplicationExperimental::class)
    private val upperComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationID.UPPER.ordinal,
                canvasComplicationFactory,
                upperAndLowerComplicationTypes,
                DefaultComplicationDataSourcePolicy(
                    ComponentName(COMPLICATION_PACKAGE, "$COMPLICATION_CLASS_PREFIX\$Calories"),
                    ComplicationType.RANGED_VALUE,
                    SystemDataSources.DATA_SOURCE_WORLD_CLOCK,
                    ComplicationType.LONG_TEXT
                ),
                ComplicationSlotBounds(
                    ComplicationType.values().associateWith {
                        if (it == ComplicationType.LONG_TEXT || it == ComplicationType.NO_DATA) {
                            createBoundsRect(
                                UPPER_ROUND_RECT_COMPLICATION_CENTER_FRACTION,
                                ROUND_RECT_COMPLICATION_SIZE_FRACTION
                            )
                        } else {
                            createBoundsRect(
                                UPPER_CIRCLE_COMPLICATION_CENTER_FRACTION,
                                CIRCLE_COMPLICATION_DIAMETER_FRACTION
                            )
                        }
                    },
                    ComplicationType.values().associateWith { RectF() }
                )
            )
            .setNameResourceId(R.string.upper_complication_screen_name)
            .setScreenReaderNameResourceId(R.string.upper_complication_screen_reader_name)
            .build()

    @OptIn(ComplicationExperimental::class)
    private val lowerComplication =
        ComplicationSlot.createRoundRectComplicationSlotBuilder(
                ComplicationID.LOWER.ordinal,
                canvasComplicationFactory,
                upperAndLowerComplicationTypes,
                DefaultComplicationDataSourcePolicy(
                    ComponentName(COMPLICATION_PACKAGE, "$COMPLICATION_CLASS_PREFIX\$Distance"),
                    ComplicationType.RANGED_VALUE,
                    SystemDataSources.DATA_SOURCE_NEXT_EVENT,
                    ComplicationType.LONG_TEXT
                ),
                ComplicationSlotBounds(
                    ComplicationType.values().associateWith {
                        if (it == ComplicationType.LONG_TEXT || it == ComplicationType.NO_DATA) {
                            createBoundsRect(
                                LOWER_ROUND_RECT_COMPLICATION_CENTER_FRACTION,
                                ROUND_RECT_COMPLICATION_SIZE_FRACTION
                            )
                        } else {
                            createBoundsRect(
                                LOWER_CIRCLE_COMPLICATION_CENTER_FRACTION,
                                CIRCLE_COMPLICATION_DIAMETER_FRACTION
                            )
                        }
                    },
                    ComplicationType.values().associateWith { RectF() }
                )
            )
            .setNameResourceId(R.string.lower_complication_screen_name)
            .setScreenReaderNameResourceId(R.string.lower_complication_screen_reader_name)
            .build()

    @OptIn(ComplicationExperimental::class)
    private val backgroundComplication =
        ComplicationSlot.createBackgroundComplicationSlotBuilder(
                ComplicationID.BACKGROUND.ordinal,
                canvasComplicationFactory,
                listOf(ComplicationType.PHOTO_IMAGE),
                DefaultComplicationDataSourcePolicy()
            )
            .setNameResourceId(R.string.background_complication_screen_name)
            .setScreenReaderNameResourceId(R.string.background_complication_screen_reader_name)
            .build()

    override fun createUserStyleSchema() = UserStyleSchema(listOf(colorStyleSetting))

    @OptIn(ComplicationExperimental::class)
    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) =
        ComplicationSlotsManager(
            listOf(
                leftComplication,
                rightComplication,
                upperComplication,
                lowerComplication,
                backgroundComplication
            ),
            currentUserStyleRepository
        )

    @OptIn(ComplicationExperimental::class)
    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer =
            ExampleDigitalWatchCanvasRenderer(
                surfaceHolder,
                this,
                watchFaceStyle,
                currentUserStyleRepository,
                watchState,
                colorStyleSetting,
                complicationSlotsManager
            )

        // createWatchFace is called on a worker thread but the observers should be called from the
        // UiThread.
        val uiScope = CoroutineScope(getUiThreadHandler().asCoroutineDispatcher())

        uiScope.launch {
            upperComplication.complicationData.collect {
                // Force bounds recalculation, because this can affect the size of the central time
                // display.
                renderer.oldBounds.set(0, 0, 0, 0)
            }
        }

        uiScope.launch {
            lowerComplication.complicationData.collect() {
                // Force bounds recalculation, because this can affect the size of the central time
                // display.
                renderer.oldBounds.set(0, 0, 0, 0)
            }
        }
        return WatchFace(WatchFaceType.DIGITAL, renderer)
            .setComplicationDeniedDialogIntent(Intent(this, ComplicationDeniedActivity::class.java))
            .setComplicationRationaleDialogIntent(
                Intent(this, ComplicationRationalActivity::class.java)
            )
    }

    @OptIn(WatchFaceExperimental::class)
    @Suppress("Deprecation")
    @RequiresApi(27)
    private class ExampleDigitalWatchCanvasRenderer(
        surfaceHolder: SurfaceHolder,
        private val context: Context,
        private var watchFaceColorStyle: WatchFaceColorStyle,
        currentUserStyleRepository: CurrentUserStyleRepository,
        watchState: WatchState,
        private val colorStyleSetting: UserStyleSetting.ListUserStyleSetting,
        private val complicationSlotsManager: ComplicationSlotsManager
    ) :
        Renderer.CanvasRenderer(
            surfaceHolder,
            currentUserStyleRepository,
            watchState,
            CanvasType.HARDWARE,
            INTERACTIVE_UPDATE_RATE_MS,
            clearWithBackgroundTintBeforeRenderingHighlightLayer = true
        ) {
        internal var oldBounds = Rect(0, 0, 0, 0)

        private fun getBaseDigitPaint() =
            Paint().apply {
                typeface = Typeface.create(DIGITAL_TYPE_FACE, Typeface.NORMAL)
                isAntiAlias = true
            }

        private val digitTextHoursPaint = getBaseDigitPaint()
        private val digitTextMinutesPaint = getBaseDigitPaint()
        private val digitTextSecondsPaint = getBaseDigitPaint()

        // Used for drawing the cached digits to the watchface.
        private val digitBitmapPaint = Paint().apply { isFilterBitmap = true }

        // Used for computing text sizes, not directly used for rendering.
        private var digitTextPaint = getBaseDigitPaint()

        private val clockBounds = Rect()
        private val digitBounds = Rect()
        private var digitTextSize = 0f
        private var smallDigitTextSize = 0f
        private var digitHeight = 0
        private var digitWidth = 0
        private var smallDigitHeight = 0
        private var smallDigitWidth = 0
        private var digitVerticalPadding = 0
        private var gapWidth = 0f
        private val currentDigitStrings = DigitStrings()
        private val nextDigitStrings = DigitStrings()
        private val digitDrawProperties = DigitDrawProperties()
        private var drawProperties = DrawProperties()
        private var prevDrawMode = DrawMode.INTERACTIVE

        // Animation played when exiting ambient mode.
        private val ambientExitAnimator =
            AnimatorSet().apply {
                val linearOutSlow =
                    AnimationUtils.loadInterpolator(
                        context,
                        android.R.interpolator.linear_out_slow_in
                    )
                playTogether(
                    ObjectAnimator.ofFloat(drawProperties, DrawProperties.TIME_SCALE, 1.0f).apply {
                        duration = AMBIENT_TRANSITION_MS
                        interpolator = linearOutSlow
                        setAutoCancel(true)
                    },
                    ObjectAnimator.ofFloat(drawProperties, DrawProperties.SECONDS_SCALE, 1.0f)
                        .apply {
                            duration = AMBIENT_TRANSITION_MS
                            interpolator = linearOutSlow
                            setAutoCancel(true)
                        }
                )
            }

        // Animation played when entering ambient mode.
        private val ambientEnterAnimator =
            AnimatorSet().apply {
                val fastOutLinearIn =
                    AnimationUtils.loadInterpolator(
                        context,
                        android.R.interpolator.fast_out_linear_in
                    )
                playTogether(
                    ObjectAnimator.ofFloat(drawProperties, DrawProperties.TIME_SCALE, 1.0f).apply {
                        duration = AMBIENT_TRANSITION_MS
                        interpolator = fastOutLinearIn
                        setAutoCancel(true)
                    },
                    ObjectAnimator.ofFloat(drawProperties, DrawProperties.SECONDS_SCALE, 0.0f)
                        .apply {
                            duration = AMBIENT_TRANSITION_MS
                            interpolator = fastOutLinearIn
                            setAutoCancel(true)
                        }
                )
            }

        // A mapping from digit type to cached bitmap. One bitmap is cached per digit type, and the
        // digit shown in the cached image is stored in [currentCachedDigits].
        private val digitBitmapCache = SparseArray<Bitmap>()

        // A mapping from digit type to the digit that the cached bitmap
        // (stored in [digitBitmapCache]) displays.
        private val currentCachedDigits = SparseArray<String>()

        private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

        init {
            // Listen for style changes.
            coroutineScope.launch {
                currentUserStyleRepository.userStyle.collect { userStyle ->
                    watchFaceColorStyle =
                        WatchFaceColorStyle.create(
                            context,
                            userStyle[colorStyleSetting]!!.toString()
                        )

                    watchfaceColors =
                        WatchFaceColors(
                            Color.valueOf(watchFaceColorStyle.activeStyle.primaryColor),
                            Color.valueOf(watchFaceColorStyle.activeStyle.secondaryColor),
                            Color.valueOf(Color.DKGRAY)
                        )

                    // Apply the userStyle to the complicationSlots. ComplicationDrawables for each
                    // of the styles are defined in XML so we need to replace the complication's
                    // drawables.
                    for ((_, complication) in complicationSlotsManager.complicationSlots) {
                        (complication.renderer as CanvasComplicationDrawable).drawable =
                            watchFaceColorStyle.getDrawable(context)!!
                    }

                    clearDigitBitmapCache()
                }
            }

            // Listen for ambient state changes.
            coroutineScope.launch {
                watchState.isAmbient.collect {
                    if (it!!) {
                        ambientEnterAnimator.start()
                    } else {
                        ambientExitAnimator.start()
                    }

                    // Trigger recomputation of bounds.
                    oldBounds.set(0, 0, 0, 0)
                    val antiAlias = !(it && watchState.hasLowBitAmbient)
                    digitTextHoursPaint.isAntiAlias = antiAlias
                    digitTextMinutesPaint.isAntiAlias = antiAlias
                    digitTextSecondsPaint.isAntiAlias = antiAlias
                }
            }
        }

        override fun shouldAnimate(): Boolean {
            // Make sure we keep animating while ambientEnterAnimator is running.
            return ambientEnterAnimator.isRunning || super.shouldAnimate()
        }

        private fun applyColorStyleAndDrawMode(drawMode: DrawMode) {
            digitTextHoursPaint.color =
                when (drawMode) {
                    DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.primaryColor
                    DrawMode.LOW_BATTERY_INTERACTIVE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.6f)
                    DrawMode.MUTE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.8f)
                    DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.primaryColor
                }

            digitTextMinutesPaint.color =
                when (drawMode) {
                    DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.primaryColor
                    DrawMode.LOW_BATTERY_INTERACTIVE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.6f)
                    DrawMode.MUTE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.8f)
                    DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.primaryColor
                }

            digitTextSecondsPaint.color =
                when (drawMode) {
                    DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.secondaryColor
                    DrawMode.LOW_BATTERY_INTERACTIVE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.secondaryColor, 0.6f)
                    DrawMode.MUTE ->
                        multiplyColor(watchFaceColorStyle.activeStyle.secondaryColor, 0.8f)
                    DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.secondaryColor
                }

            if (prevDrawMode != drawMode) {
                prevDrawMode = drawMode
                clearDigitBitmapCache()
            }
        }

        override fun render(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
            recalculateBoundsIfChanged(bounds, zonedDateTime)

            applyColorStyleAndDrawMode(renderParameters.drawMode)

            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
                drawBackground(canvas)
            }

            drawComplications(canvas, zonedDateTime)

            if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
                val is24Hour: Boolean = DateFormat.is24HourFormat(context)

                currentDigitStrings.set(zonedDateTime, is24Hour)
                nextDigitStrings.set(zonedDateTime.plusSeconds(1), is24Hour)

                val secondProgress = (zonedDateTime.nano.toDouble() / 1000000000.0).toFloat()

                val animationStartFraction =
                    DIGIT_ANIMATION_START_TIME_FRACTION[DigitMode.OUTGOING]!!
                this.interactiveDrawModeUpdateDelayMillis =
                    if (
                        secondProgress < animationStartFraction && !ambientEnterAnimator.isRunning
                    ) {
                        // The seconds only animate part of the time so we can sleep until the
                        // seconds next need to
                        // animate, which improves battery life.
                        max(
                            INTERACTIVE_UPDATE_RATE_MS,
                            ((animationStartFraction - secondProgress) * 1000f).toLong()
                        )
                    } else {
                        INTERACTIVE_UPDATE_RATE_MS
                    }

                // Move the left position to the left if there are fewer than two hour digits, to
                // ensure it is centered. If the clock is in transition from one to two hour digits
                // or
                // vice versa, interpolate to animate the clock's position.
                // Move the left position to the left if there are fewer than two hour digits, to
                // ensure
                // it is centered. If the clock is in transition from one to two hour digits or
                // vice versa, interpolate to animate the clock's position.
                val centeringAdjustment =
                    (getInterpolatedValue(
                        (2 - currentDigitStrings.getNumberOfHoursDigits()).toFloat(),
                        (2 - nextDigitStrings.getNumberOfHoursDigits()).toFloat(),
                        POSITION_ANIMATION_START_TIME,
                        POSITION_ANIMATION_END_TIME,
                        secondProgress,
                        CENTERING_ADJUSTMENT_INTERPOLATOR
                    ) * digitWidth)

                // This total width assumes two hours digits.
                val totalWidth = 2f * digitWidth + gapWidth + 2f * smallDigitWidth
                val left = clockBounds.exactCenterX() - 0.5f * (totalWidth + centeringAdjustment)
                val top = clockBounds.exactCenterY() - 0.5f * digitHeight

                val wholeTimeSaveCount = canvas.save()
                try {
                    canvas.scale(
                        drawProperties.timeScale,
                        drawProperties.timeScale,
                        clockBounds.exactCenterX(),
                        clockBounds.exactCenterY()
                    )

                    // Draw hours.
                    drawDigit(
                        canvas,
                        left,
                        top,
                        currentDigitStrings,
                        nextDigitStrings,
                        DigitType.HOUR_TENS,
                        secondProgress
                    )
                    drawDigit(
                        canvas,
                        left + digitWidth,
                        top,
                        currentDigitStrings,
                        nextDigitStrings,
                        DigitType.HOUR_UNITS,
                        secondProgress
                    )

                    // Draw minutes.
                    val minutesLeft = left + digitWidth * 2.0f + gapWidth
                    drawDigit(
                        canvas,
                        minutesLeft,
                        top,
                        currentDigitStrings,
                        nextDigitStrings,
                        DigitType.MINUTE_TENS,
                        secondProgress
                    )
                    drawDigit(
                        canvas,
                        minutesLeft + smallDigitWidth,
                        top,
                        currentDigitStrings,
                        nextDigitStrings,
                        DigitType.MINUTE_UNITS,
                        secondProgress
                    )

                    // Scale the seconds if they're not fully showing, in and out of ambient for
                    // example.
                    val scaleSeconds = drawProperties.secondsScale < 1.0f
                    if (
                        drawProperties.secondsScale > 0f &&
                            (renderParameters.drawMode != DrawMode.AMBIENT || scaleSeconds)
                    ) {
                        val restoreCount = canvas.save()
                        if (scaleSeconds) {
                            // Scale the canvas around the center of the seconds bounds.
                            canvas.scale(
                                drawProperties.secondsScale,
                                drawProperties.secondsScale,
                                minutesLeft + smallDigitWidth,
                                top + digitHeight - smallDigitHeight / 2
                            )
                        }

                        // Draw seconds.
                        val secondsTop = top + digitHeight - smallDigitHeight
                        drawDigit(
                            canvas,
                            minutesLeft,
                            secondsTop,
                            currentDigitStrings,
                            nextDigitStrings,
                            DigitType.SECOND_TENS,
                            secondProgress
                        )
                        drawDigit(
                            canvas,
                            minutesLeft + smallDigitWidth,
                            secondsTop,
                            currentDigitStrings,
                            nextDigitStrings,
                            DigitType.SECOND_UNITS,
                            secondProgress
                        )

                        canvas.restoreToCount(restoreCount)
                    }
                } finally {
                    canvas.restoreToCount(wholeTimeSaveCount)
                }
            }
        }

        override fun renderHighlightLayer(
            canvas: Canvas,
            bounds: Rect,
            zonedDateTime: ZonedDateTime
        ) {
            drawComplicationHighlights(canvas, zonedDateTime)
        }

        override fun getMainClockElementBounds() = clockBounds

        private fun recalculateBoundsIfChanged(bounds: Rect, zonedDateTime: ZonedDateTime) {
            if (oldBounds == bounds) {
                return
            }

            oldBounds.set(bounds)
            calculateClockBound(bounds, zonedDateTime)
        }

        private fun calculateClockBound(bounds: Rect, zonedDateTime: ZonedDateTime) {
            val hasVerticalComplication =
                VERTICAL_COMPLICATION_IDS.any {
                    complicationSlotsManager[it]!!.isActiveAt(zonedDateTime.toInstant())
                }
            val hasHorizontalComplication =
                HORIZONTAL_COMPLICATION_IDS.any {
                    complicationSlotsManager[it]!!.isActiveAt(zonedDateTime.toInstant())
                }

            val marginX =
                if (hasHorizontalComplication) {
                    (MARGIN_FRACTION_WITH_COMPLICATION.x * bounds.width().toFloat()).toInt()
                } else {
                    (MARGIN_FRACTION_WITHOUT_COMPLICATION.x * bounds.width().toFloat()).toInt()
                }

            val marginY =
                if (hasVerticalComplication) {
                    (MARGIN_FRACTION_WITH_COMPLICATION.y * bounds.height().toFloat()).toInt()
                } else {
                    (MARGIN_FRACTION_WITHOUT_COMPLICATION.y * bounds.height().toFloat()).toInt()
                }

            clockBounds.set(
                bounds.left + marginX,
                bounds.top + marginY,
                bounds.right - marginX,
                bounds.bottom - marginY
            )

            recalculateTextSize()
        }

        private fun recalculateTextSize() {
            // Determine the font size to fit by measuring the text for a sample size and compare to
            // the size. That assume the height of the text scales linearly.
            val sampleTextSize = clockBounds.height().toFloat()
            digitTextPaint.setTextSize(sampleTextSize)
            digitTextPaint.getTextBounds(ALL_DIGITS, 0, ALL_DIGITS.length, digitBounds)
            var textSize = clockBounds.height() * sampleTextSize / digitBounds.height().toFloat()
            val textRatio = textSize / clockBounds.height().toFloat()

            // Remain a top and bottom padding.
            textSize *= (1f - 2f * DIGIT_PADDING_FRACTION)

            // Calculate the total width fraction base on the text height.
            val totalWidthFraction: Float =
                (2f * DIGIT_WIDTH_FRACTION) +
                    (DIGIT_WIDTH_FRACTION * GAP_WIDTH_FRACTION) +
                    (2f * SMALL_DIGIT_SIZE_FRACTION * SMALL_DIGIT_WIDTH_FRACTION)

            textSize =
                min(textSize, (clockBounds.width().toFloat() / totalWidthFraction) * textRatio)

            setDigitTextSize(textSize)
        }

        private fun setDigitTextSize(textSize: Float) {
            clearDigitBitmapCache()

            digitTextSize = textSize
            digitTextPaint.textSize = digitTextSize
            digitTextPaint.getTextBounds(ALL_DIGITS, 0, 1, digitBounds)
            digitVerticalPadding = (digitBounds.height() * DIGIT_PADDING_FRACTION).toInt()
            digitWidth = (digitBounds.height() * DIGIT_WIDTH_FRACTION).toInt()
            digitHeight = digitBounds.height() + 2 * digitVerticalPadding

            smallDigitTextSize = textSize * SMALL_DIGIT_SIZE_FRACTION
            digitTextPaint.setTextSize(smallDigitTextSize)
            digitTextPaint.getTextBounds(ALL_DIGITS, 0, 1, digitBounds)
            smallDigitHeight = digitBounds.height() + 2 * digitVerticalPadding
            smallDigitWidth = (digitBounds.height() * SMALL_DIGIT_WIDTH_FRACTION).toInt()

            gapWidth = digitHeight * GAP_WIDTH_FRACTION

            digitTextHoursPaint.textSize = digitTextSize
            digitTextMinutesPaint.textSize = smallDigitTextSize
            digitTextSecondsPaint.textSize = smallDigitTextSize
        }

        private fun drawBackground(canvas: Canvas) {
            val backgroundColor =
                if (renderParameters.drawMode == DrawMode.AMBIENT) {
                    watchFaceColorStyle.ambientStyle.backgroundColor
                } else {
                    watchFaceColorStyle.activeStyle.backgroundColor
                }
            canvas.drawColor(
                getRGBColor(backgroundColor, drawProperties.backgroundAlpha, Color.BLACK)
            )
        }

        private fun drawComplications(canvas: Canvas, zonedDateTime: ZonedDateTime) {
            // First, draw the background complication if not in ambient mode
            if (renderParameters.drawMode != DrawMode.AMBIENT) {
                complicationSlotsManager[ComplicationID.BACKGROUND.ordinal]?.let {
                    if (it.complicationData.value.type != ComplicationType.NO_DATA) {
                        it.render(canvas, zonedDateTime, renderParameters)
                    }
                }
            }
            for (i in FOREGROUND_COMPLICATION_IDS) {
                val complication = complicationSlotsManager[i] as ComplicationSlot
                complication.render(canvas, zonedDateTime, renderParameters)
            }
        }

        private fun drawComplicationHighlights(canvas: Canvas, zonedDateTime: ZonedDateTime) {
            for (i in FOREGROUND_COMPLICATION_IDS) {
                val complication = complicationSlotsManager[i] as ComplicationSlot
                complication.renderHighlightLayer(canvas, zonedDateTime, renderParameters)
            }
        }

        private fun clearDigitBitmapCache() {
            currentCachedDigits.clear()
            digitBitmapCache.clear()
        }

        private fun drawDigit(
            canvas: Canvas,
            left: Float,
            top: Float,
            currentDigitStrings: DigitStrings,
            nextDigitStrings: DigitStrings,
            digitType: DigitType,
            secondProgress: Float
        ) {
            val currentDigit = currentDigitStrings.get(digitType)
            val nextDigit = nextDigitStrings.get(digitType)

            // Draw the current digit bitmap.
            if (currentDigit != nextDigit) {
                drawDigitWithAnimation(
                    canvas,
                    left,
                    top,
                    secondProgress,
                    currentDigit,
                    digitType,
                    DigitMode.OUTGOING
                )
                drawDigitWithAnimation(
                    canvas,
                    left,
                    top,
                    secondProgress,
                    nextDigit,
                    digitType,
                    DigitMode.INCOMING
                )
            } else {
                canvas.drawBitmap(
                    getDigitBitmap(currentDigit, digitType),
                    left,
                    top,
                    digitBitmapPaint
                )
            }
        }

        private fun drawDigitWithAnimation(
            canvas: Canvas,
            left: Float,
            top: Float,
            secondProgress: Float,
            digit: String,
            digitType: DigitType,
            digitMode: DigitMode
        ) {
            getDigitDrawProperties(
                secondProgress,
                getTimeOffsetSeconds(digitType),
                digitMode,
                digitDrawProperties
            )

            if (!digitDrawProperties.shouldDraw) {
                return
            }

            val bitmap = getDigitBitmap(digit, digitType)
            val centerX = left + bitmap.width / 2f
            val centerY = top + bitmap.height / 2f
            val restoreCount = canvas.save()
            canvas.scale(digitDrawProperties.scale, digitDrawProperties.scale, centerX, centerY)
            canvas.rotate(digitDrawProperties.rotation, centerX, centerY)
            val bitmapPaint = digitBitmapPaint
            bitmapPaint.alpha = (digitDrawProperties.opacity * 255.0f).toInt()
            canvas.drawBitmap(bitmap, left, top, bitmapPaint)
            bitmapPaint.alpha = 255
            canvas.restoreToCount(restoreCount)
        }

        private fun createBitmap(width: Int, height: Int, digitType: DigitType): Bitmap {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            digitBitmapCache.put(digitType.ordinal, bitmap)
            return bitmap
        }

        /**
         * Returns a {@link Bitmap} representing the given {@code digit}, suitable for use as the
         * given {@code digitType}.
         */
        private fun getDigitBitmap(digit: String, digitType: DigitType): Bitmap {
            val width: Int
            val height: Int
            val paint: Paint
            when (digitType) {
                DigitType.HOUR_TENS,
                DigitType.HOUR_UNITS -> {
                    width = digitWidth
                    height = digitHeight
                    paint = digitTextHoursPaint
                }
                DigitType.MINUTE_TENS,
                DigitType.MINUTE_UNITS -> {
                    width = smallDigitWidth
                    height = smallDigitHeight
                    paint = digitTextMinutesPaint
                }
                DigitType.SECOND_TENS,
                DigitType.SECOND_UNITS -> {
                    width = smallDigitWidth
                    height = smallDigitHeight
                    paint = digitTextSecondsPaint
                }
            }

            val bitmap =
                digitBitmapCache.get(digitType.ordinal) ?: createBitmap(width, height, digitType)
            if (digit != currentCachedDigits.get(digitType.ordinal)) {
                currentCachedDigits.put(digitType.ordinal, digit)

                bitmap.eraseColor(Color.TRANSPARENT)
                val canvas = Canvas(bitmap)
                val textWidth = paint.measureText(digit)

                // We use the same vertical padding here for all types and sizes of digit so that
                // their tops and bottoms can be aligned.
                canvas.drawText(
                    digit,
                    (width - textWidth) / 2,
                    height.toFloat() - digitVerticalPadding,
                    paint
                )
            }
            return bitmap
        }
    }

    private data class Vec2f(val x: Float, val y: Float)

    private enum class ComplicationID {
        UPPER,
        RIGHT,
        LOWER,
        LEFT,
        BACKGROUND
    }

    // Changing digits are animated. This enum is used to label the start and end animation
    // parameters.
    private enum class DigitMode {
        OUTGOING,
        INCOMING
    }

    private enum class DigitType {
        HOUR_TENS,
        HOUR_UNITS,
        MINUTE_TENS,
        MINUTE_UNITS,
        SECOND_TENS,
        SECOND_UNITS
    }

    /** A class to provide string representations of each of the digits of a given time. */
    private class DigitStrings {
        private var hourTens = ""
        private var hourUnits = ""
        private var minuteTens = ""
        private var minuteUnits = ""
        private var secondTens = ""
        private var secondUnits = ""

        /** Sets the time represented by this instance. */
        fun set(zonedDateTime: ZonedDateTime, is24Hour: Boolean) {
            if (is24Hour) {
                val hourValue = zonedDateTime.hour
                hourTens = getTensDigitString(hourValue, true)
                hourUnits = getUnitsDigitString(hourValue)
            } else {
                var hourValue = zonedDateTime.hour % 12
                // We should show 12 for noon and midnight.
                if (hourValue == 0) {
                    hourValue = 12
                }
                hourTens = getTensDigitString(hourValue, false)
                hourUnits = getUnitsDigitString(hourValue)
            }

            val minuteValue = zonedDateTime.minute
            minuteTens = getTensDigitString(minuteValue, true)
            minuteUnits = getUnitsDigitString(minuteValue)
            val secondsValue = zonedDateTime.second
            secondTens = getTensDigitString(secondsValue, true)
            secondUnits = getUnitsDigitString(secondsValue)
        }

        /**
         * Returns a string representing the specified digit of the time represented by this object.
         */
        fun get(digitType: DigitType): String {
            return when (digitType) {
                DigitType.HOUR_TENS -> hourTens
                DigitType.HOUR_UNITS -> hourUnits
                DigitType.MINUTE_TENS -> minuteTens
                DigitType.MINUTE_UNITS -> minuteUnits
                DigitType.SECOND_TENS -> secondTens
                DigitType.SECOND_UNITS -> secondUnits
            }
        }

        /**
         * Returns the number of hour digits in this object. If the representation is 24-hour, this
         * will always return 2. If 12-hour, this will return 1 or 2.
         */
        fun getNumberOfHoursDigits(): Int {
            return if (hourTens == "") 1 else 2
        }

        /**
         * Returns a {@link String} representing the tens digit of the provided non-negative {@code
         * value}. If {@code padWithZeroes} is true, returns zero if {@code value} < 10. If {@code
         * padWithZeroes} is false, returns an empty string if {@code value} < 10.
         */
        private fun getTensDigitString(value: Int, padWithZeroes: Boolean): String {
            if (value < 10 && !padWithZeroes) {
                return ""
            }
            // We don't use toString() because during draw calls we don't want to avoid allocating
            // objects.
            return DIGITS[(value / 10) % 10]
        }

        /**
         * Returns a {@link String} representing the units digit of the provided non-negative {@code
         * value}.
         */
        private fun getUnitsDigitString(value: Int): String {
            // We don't use toString() because during draw calls we don't want to avoid allocating
            // objects.
            return DIGITS[value % 10]
        }
    }

    private data class DigitDrawProperties(
        var shouldDraw: Boolean = false,
        var scale: Float = 0f,
        var rotation: Float = 0f,
        var opacity: Float = 0f
    )

    private class DrawProperties(
        var backgroundAlpha: Float = 1f,
        var timeScale: Float = 1f,
        var secondsScale: Float = 1f
    ) {
        companion object {
            val TIME_SCALE =
                object : FloatProperty<DrawProperties>("timeScale") {
                    override fun setValue(obj: DrawProperties, value: Float) {
                        obj.timeScale = value
                    }

                    override fun get(obj: DrawProperties): Float {
                        return obj.timeScale
                    }
                }

            val SECONDS_SCALE =
                object : FloatProperty<DrawProperties>("secondsScale") {
                    override fun setValue(obj: DrawProperties, value: Float) {
                        obj.secondsScale = value
                    }

                    override fun get(obj: DrawProperties): Float {
                        return obj.secondsScale
                    }
                }
        }
    }

    companion object {
        private const val COLOR_STYLE_SETTING = "color_style_setting"
        private const val RED_STYLE = "red_style"
        private const val GREEN_STYLE = "green_style"
        private const val BLUE_STYLE = "blue_style"

        // Render at approximately 60fps in interactive mode.
        private const val INTERACTIVE_UPDATE_RATE_MS = 16L

        private const val COMPLICATION_PACKAGE =
            "androidx.wear.watchface.complications.datasource.samples"
        private const val COMPLICATION_CLASS_PREFIX =
            "$COMPLICATION_PACKAGE.dynamic.HealthDataSourceServices"

        // Constants for the size of complication.
        private val CIRCLE_COMPLICATION_DIAMETER_FRACTION = Vec2f(0.252f, 0.252f)
        private val ROUND_RECT_COMPLICATION_SIZE_FRACTION = Vec2f(0.645f, 0.168f)

        // Constants for the upper complication location.
        private val UPPER_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.21f)
        private val UPPER_ROUND_RECT_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.21f)

        // Constants for the lower complication location.
        private val LOWER_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.79f)
        private val LOWER_ROUND_RECT_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.79f)

        // Constants for the left complication location.
        private val LEFT_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.177f, 0.5f)

        // Constants for the right complication location.
        private val RIGHT_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.823f, 0.5f)

        // Constants for the clock digits' position, based on the height and width of given bounds.
        private val MARGIN_FRACTION_WITHOUT_COMPLICATION = Vec2f(0.2f, 0.2f)
        private val MARGIN_FRACTION_WITH_COMPLICATION = Vec2f(0.4f, 0.4f)

        private val VERTICAL_COMPLICATION_IDS =
            arrayOf(ComplicationID.UPPER.ordinal, ComplicationID.LOWER.ordinal)
        private val HORIZONTAL_COMPLICATION_IDS =
            arrayOf(ComplicationID.LEFT.ordinal, ComplicationID.RIGHT.ordinal)
        private val FOREGROUND_COMPLICATION_IDS =
            arrayOf(
                ComplicationID.UPPER.ordinal,
                ComplicationID.RIGHT.ordinal,
                ComplicationID.LOWER.ordinal,
                ComplicationID.LEFT.ordinal
            )

        // The name of the font used for drawing the text in the digit watch face.
        private const val DIGITAL_TYPE_FACE = "sans-serif-condensed-light"

        // The width of the large digit bitmaps, as a fraction of their height.
        private const val DIGIT_WIDTH_FRACTION = 0.65f

        // The height of the small digits (used for minutes and seconds), given as a fraction of the
        //  height
        // of the large digits.
        private const val SMALL_DIGIT_SIZE_FRACTION = 0.45f

        // The width of the small digit bitmaps, as a fraction of their height.
        private const val SMALL_DIGIT_WIDTH_FRACTION = 0.7f

        // The padding at the top and bottom of the digit bitmaps, given as a fraction of the
        // height.
        // Needed as some characters may ascend or descend slightly (e.g. "8").
        private const val DIGIT_PADDING_FRACTION = 0.05f

        // The gap between the hours and the minutes/seconds, given as a fraction of the width of
        // the large
        // digits.
        private const val GAP_WIDTH_FRACTION = 0.1f

        // A string containing all digits, used to measure their height.
        private const val ALL_DIGITS = "0123456789"

        private val DIGITS = ALL_DIGITS.toCharArray().map { it.toString() }

        // The start and end times of the animations, expressed as a fraction of a second.
        // (So 0.5 means that the animation of that digit will begin half-way through the second).
        // Note that because we only cache one digit of each type, the current and next times must
        // not overlap.
        private val DIGIT_ANIMATION_START_TIME_FRACTION =
            mapOf(DigitMode.OUTGOING to 0.5f, DigitMode.INCOMING to 0.667f)
        private val DIGIT_ANIMATION_END_TIME =
            mapOf(DigitMode.OUTGOING to 0.667f, DigitMode.INCOMING to 1f)
        private const val POSITION_ANIMATION_START_TIME = 0.0833f
        private const val POSITION_ANIMATION_END_TIME = 0.5833f

        // Parameters governing the animation of the current and next digits. NB Scale is a size
        // multiplier.
        // The first index is the values for the outgoing digit, and the second index for the
        // incoming
        // digit. If seconds are changing from 1 -> 2 for example, the 1 will scale from 1f to
        // 0.65f, and
        // rotate from 0f to 82f. The 2 will scale from 0.65f to 1f, and rotate from -97f to 0f.
        private val DIGIT_SCALE_START = mapOf(DigitMode.OUTGOING to 1f, DigitMode.INCOMING to 0.65f)
        private val DIGIT_SCALE_END = mapOf(DigitMode.OUTGOING to 0.65f, DigitMode.INCOMING to 1f)
        private val DIGIT_ROTATE_START_DEGREES =
            mapOf(DigitMode.OUTGOING to 0f, DigitMode.INCOMING to -97f)
        private val DIGIT_ROTATE_END_DEGREES =
            mapOf(DigitMode.OUTGOING to 82f, DigitMode.INCOMING to 0f)
        private val DIGIT_OPACITY_START =
            mapOf(DigitMode.OUTGOING to 1f, DigitMode.INCOMING to 0.07f)
        private val DIGIT_OPACITY_END = mapOf(DigitMode.OUTGOING to 0f, DigitMode.INCOMING to 1f)

        // The offset used to stagger the animation when multiple digits are animating at the same
        // time.
        private const val TIME_OFFSET_SECONDS_PER_DIGIT_TYPE = -5 / 60f

        // The duration of the ambient mode change animation.
        private const val AMBIENT_TRANSITION_MS = 333L

        private val DIGIT_SCALE_INTERPOLATOR =
            mapOf(
                DigitMode.OUTGOING to PathInterpolator(0.4f, 0f, 0.67f, 1f),
                DigitMode.INCOMING to PathInterpolator(0.33f, 0f, 0.2f, 1f)
            )
        private val DIGIT_ROTATION_INTERPOLATOR =
            mapOf(
                DigitMode.OUTGOING to PathInterpolator(0.57f, 0f, 0.73f, 0.49f),
                DigitMode.INCOMING to PathInterpolator(0.15f, 0.49f, 0.37f, 1f)
            )
        private val DIGIT_OPACITY_INTERPOLATOR =
            mapOf(
                DigitMode.OUTGOING to PathInterpolator(0.4f, 0f, 1f, 1f),
                DigitMode.INCOMING to PathInterpolator(0f, 0f, 0.2f, 1f)
            )
        private val CENTERING_ADJUSTMENT_INTERPOLATOR = PathInterpolator(0.4f, 0f, 0.2f, 1f)

        @ColorInt
        private fun colorRgb(red: Float, green: Float, blue: Float) =
            0xff000000.toInt() or
                ((red * 255.0f + 0.5f).toInt() shl 16) or
                ((green * 255.0f + 0.5f).toInt() shl 8) or
                (blue * 255.0f + 0.5f).toInt()

        private fun redFraction(@ColorInt color: Int) = Color.red(color).toFloat() / 255.0f

        private fun greenFraction(@ColorInt color: Int) = Color.green(color).toFloat() / 255.0f

        private fun blueFraction(@ColorInt color: Int) = Color.blue(color).toFloat() / 255.0f

        /**
         * Returns an RGB color that has the same effect as drawing `color` with `alphaFraction`
         * over a `backgroundColor` background.
         *
         * @param color the foreground color
         * @param alphaFraction the fraction of the alpha value, range from 0 to 1
         * @param backgroundColor the background color
         */
        private fun getRGBColor(
            @ColorInt color: Int,
            alphaFraction: Float,
            @ColorInt backgroundColor: Int
        ): Int {
            return colorRgb(
                lerp(redFraction(backgroundColor), redFraction(color), alphaFraction),
                lerp(greenFraction(backgroundColor), greenFraction(color), alphaFraction),
                lerp(blueFraction(backgroundColor), blueFraction(color), alphaFraction)
            )
        }

        /** Returns a linear interpolation between a and b using the scalar s. */
        private fun lerp(a: Float, b: Float, s: Float) = a + s * (b - a)

        /**
         * Returns the interpolation scalar (s) that satisfies the equation: `value = lerp(a, b, s)`
         *
         * If `a == b`, then this function will return 0.
         */
        private fun lerpInv(a: Float, b: Float, value: Float) =
            if (a != b) (value - a) / (b - a) else 0.0f

        private fun getInterpolatedValue(
            startValue: Float,
            endValue: Float,
            startTime: Float,
            endTime: Float,
            currentTime: Float,
            interpolator: TimeInterpolator
        ): Float {
            val progress =
                when {
                    currentTime < startTime -> 0f
                    currentTime > endTime -> 1f
                    else -> interpolator.getInterpolation(lerpInv(startTime, endTime, currentTime))
                }
            return lerp(startValue, endValue, progress)
        }

        /**
         * Sets the [DigitDrawProperties] that should be used for drawing, given the specified
         * parameters.
         *
         * @param secondProgress the sub-second part of the current time, where 0 means the current
         *   second has just begun, and 1 means the current second has just ended
         * @param offsetSeconds a value added to the start and end time of the animations
         * @param digitMode whether the digit is OUTGOING or INCOMING
         * @param output the [DigitDrawProperties] that will be set
         */
        private fun getDigitDrawProperties(
            secondProgress: Float,
            offsetSeconds: Float,
            digitMode: DigitMode,
            output: DigitDrawProperties
        ) {
            val startTime = DIGIT_ANIMATION_START_TIME_FRACTION[digitMode]!! + offsetSeconds
            val endTime = DIGIT_ANIMATION_END_TIME[digitMode]!! + offsetSeconds
            output.shouldDraw =
                if (digitMode == DigitMode.OUTGOING) {
                    secondProgress < endTime
                } else {
                    secondProgress >= startTime
                }
            output.scale =
                getInterpolatedValue(
                    DIGIT_SCALE_START[digitMode]!!,
                    DIGIT_SCALE_END[digitMode]!!,
                    startTime,
                    endTime,
                    secondProgress,
                    DIGIT_SCALE_INTERPOLATOR[digitMode]!!
                )
            output.rotation =
                getInterpolatedValue(
                    DIGIT_ROTATE_START_DEGREES[digitMode]!!,
                    DIGIT_ROTATE_END_DEGREES[digitMode]!!,
                    startTime,
                    endTime,
                    secondProgress,
                    DIGIT_ROTATION_INTERPOLATOR[digitMode]!!
                )
            output.opacity =
                getInterpolatedValue(
                    DIGIT_OPACITY_START[digitMode]!!,
                    DIGIT_OPACITY_END[digitMode]!!,
                    startTime,
                    endTime,
                    secondProgress,
                    DIGIT_OPACITY_INTERPOLATOR[digitMode]!!
                )
        }

        private fun getTimeOffsetSeconds(digitType: DigitType): Float {
            return when (digitType) {
                DigitType.HOUR_TENS -> 5f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
                DigitType.HOUR_UNITS -> 4f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
                DigitType.MINUTE_TENS -> 3f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
                DigitType.MINUTE_UNITS -> 2f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
                DigitType.SECOND_TENS -> 1f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
                DigitType.SECOND_UNITS -> 0f
            }
        }

        /** Applies a multiplier to a color, e.g. to darken if it's < 1.0 */
        private fun multiplyColor(colorInt: Int, multiplier: Float): Int {
            val adjustedMultiplier = multiplier / 255.0f
            return colorRgb(
                Color.red(colorInt).toFloat() * adjustedMultiplier,
                Color.green(colorInt).toFloat() * adjustedMultiplier,
                Color.blue(colorInt).toFloat() * adjustedMultiplier,
            )
        }

        private fun createBoundsRect(centerFraction: PointF, size: Vec2f): RectF {
            val halfWidth = size.x / 2.0f
            val halfHeight = size.y / 2.0f
            return RectF(
                (centerFraction.x - halfWidth),
                (centerFraction.y - halfHeight),
                (centerFraction.x + halfWidth),
                (centerFraction.y + halfHeight)
            )
        }
    }
}
