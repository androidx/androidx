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
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.icu.util.Calendar
import android.icu.util.GregorianCalendar
import android.text.format.DateFormat
import android.util.FloatProperty
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.animation.AnimationUtils
import android.view.animation.PathInterpolator
import androidx.annotation.ColorInt
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.SystemProviders
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.CanvasComplicationFactory
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Complication
import androidx.wear.watchface.ComplicationsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.complications.rendering.CanvasComplicationDrawable
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema
import androidx.wear.watchface.style.UserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.Option
import androidx.wear.watchface.style.WatchFaceLayer
import kotlin.math.max
import kotlin.math.min

// Render at approximately 60fps in interactive mode.
private const val INTERACTIVE_UPDATE_RATE_MS = 16L

internal class Vec2f(val x: Float, val y: Float)

// Constants for the size of complication.
internal val CIRCLE_COMPLICATION_DIAMETER_FRACTION = Vec2f(0.252f, 0.252f)
internal val ROUND_RECT_COMPLICATION_SIZE_FRACTION = Vec2f(0.645f, 0.168f)

// Constants for the upper complication location.
internal val UPPER_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.21f)
internal val UPPER_ROUND_RECT_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.21f)

// Constants for the lower complication location.
internal val LOWER_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.79f)
internal val LOWER_ROUND_RECT_COMPLICATION_CENTER_FRACTION = PointF(0.5f, 0.79f)

// Constants for the left complication location.
internal val LEFT_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.177f, 0.5f)

// Constants for the right complication location.
internal val RIGHT_CIRCLE_COMPLICATION_CENTER_FRACTION = PointF(0.823f, 0.5f)

// Constants for the clock digits' position, based on the height and width of given bounds.
internal val MARGIN_FRACTION_WITHOUT_COMPLICATION = Vec2f(0.2f, 0.2f)
internal val MARGIN_FRACTION_WITH_COMPLICATION = Vec2f(0.4f, 0.4f)

// If the lightness in HSL color space is greater than this threshold, this color would be regarded
// as a light color.
internal const val BACKGROUND_COLOR_LIGHTNESS_THRESHOLD = 0.5f

enum class ComplicationID {
    UPPER,
    RIGHT,
    LOWER,
    LEFT,
    BACKGROUND
}

internal val VERTICAL_COMPLICATION_IDS = arrayOf(
    ComplicationID.UPPER.ordinal,
    ComplicationID.LOWER.ordinal
)
internal val HORIZONTAL_COMPLICATION_IDS = arrayOf(
    ComplicationID.LEFT.ordinal,
    ComplicationID.RIGHT.ordinal
)
internal val FOREGROUND_COMPLICATION_IDS = arrayOf(
    ComplicationID.UPPER.ordinal, ComplicationID.RIGHT.ordinal,
    ComplicationID.LOWER.ordinal, ComplicationID.LEFT.ordinal
)

// The name of the font used for drawing the text in the digit watch face.
private const val DIGITAL_TYPE_FACE = "sans-serif-condensed-light"

// The width of the large digit bitmaps, as a fraction of their height.
private const val DIGIT_WIDTH_FRACTION = 0.65f

// The height of the small digits (used for minutes and seconds), given as a fraction of the  height
// of the large digits.
private const val SMALL_DIGIT_SIZE_FRACTION = 0.45f

// The width of the small digit bitmaps, as a fraction of their height.
private const val SMALL_DIGIT_WIDTH_FRACTION = 0.7f

// The padding at the top and bottom of the digit bitmaps, given as a fraction of the height.
// Needed as some characters may ascend or descend slightly (e.g. "8").
private const val DIGIT_PADDING_FRACTION = 0.05f

// The gap between the hours and the minutes/seconds, given as a fraction of the width of the large
// digits.
private const val GAP_WIDTH_FRACTION = 0.1f

// A string containing all digits, used to measure their height.
private const val ALL_DIGITS = "0123456789"

internal val DIGITS = ALL_DIGITS.toCharArray().map { it.toString() }

// Changing digits are animated. This enum is used to label the start and end animation parameters.
internal enum class DigitMode {
    OUTGOING,
    INCOMING
}

// The start and end times of the animations, expressed as a fraction of a second.
// (So 0.5 means that the animation of that digit will begin half-way through the second).
// Note that because we only cache one digit of each type, the current and next times must
// not overlap.
internal val DIGIT_ANIMATION_START_TIME_FRACTION = mapOf(
    DigitMode.OUTGOING to 0.5f,
    DigitMode.INCOMING to 0.667f
)
internal val DIGIT_ANIMATION_END_TIME = mapOf(
    DigitMode.OUTGOING to 0.667f,
    DigitMode.INCOMING to 1f
)
internal const val POSITION_ANIMATION_START_TIME = 0.0833f
private const val POSITION_ANIMATION_END_TIME = 0.5833f

// Parameters governing the animation of the current and next digits. NB Scale is a size multiplier.
// The first index is the values for the outgoing digit, and the second index for the incoming
// digit. If seconds are changing from 1 -> 2 for example, the 1 will scale from 1f to 0.65f, and
// rotate from 0f to 82f. The 2 will scale from 0.65f to 1f, and rotate from -97f to 0f.
private val DIGIT_SCALE_START = mapOf(DigitMode.OUTGOING to 1f, DigitMode.INCOMING to 0.65f)
private val DIGIT_SCALE_END = mapOf(DigitMode.OUTGOING to 0.65f, DigitMode.INCOMING to 1f)
private val DIGIT_ROTATE_START_DEGREES = mapOf(
    DigitMode.OUTGOING to 0f,
    DigitMode.INCOMING to -97f
)
private val DIGIT_ROTATE_END_DEGREES = mapOf(DigitMode.OUTGOING to 82f, DigitMode.INCOMING to 0f)
private val DIGIT_OPACITY_START = mapOf(DigitMode.OUTGOING to 1f, DigitMode.INCOMING to 0.07f)
private val DIGIT_OPACITY_END = mapOf(DigitMode.OUTGOING to 0f, DigitMode.INCOMING to 1f)

// The offset used to stagger the animation when multiple digits are animating at the same time.
private const val TIME_OFFSET_SECONDS_PER_DIGIT_TYPE = -5 / 60f

// The duration of the ambient mode change animation.
private const val AMBIENT_TRANSITION_MS = 333L

private val DIGIT_SCALE_INTERPOLATOR = mapOf(
    DigitMode.OUTGOING to PathInterpolator(0.4f, 0f, 0.67f, 1f),
    DigitMode.INCOMING to PathInterpolator(0.33f, 0f, 0.2f, 1f)
)
private val DIGIT_ROTATION_INTERPOLATOR = mapOf(
    DigitMode.OUTGOING to PathInterpolator(0.57f, 0f, 0.73f, 0.49f),
    DigitMode.INCOMING to PathInterpolator(0.15f, 0.49f, 0.37f, 1f)
)
private val DIGIT_OPACITY_INTERPOLATOR = mapOf(
    DigitMode.OUTGOING to PathInterpolator(0.4f, 0f, 1f, 1f),
    DigitMode.INCOMING to PathInterpolator(0f, 0f, 0.2f, 1f)
)
internal val CENTERING_ADJUSTMENT_INTERPOLATOR =
    PathInterpolator(0.4f, 0f, 0.2f, 1f)

@ColorInt
internal fun colorRgb(red: Float, green: Float, blue: Float) =
    0xff000000.toInt() or
        ((red * 255.0f + 0.5f).toInt() shl 16) or
        ((green * 255.0f + 0.5f).toInt() shl 8) or
        (blue * 255.0f + 0.5f).toInt()

internal fun redFraction(@ColorInt color: Int) = Color.red(color).toFloat() / 255.0f

internal fun greenFraction(@ColorInt color: Int) = Color.green(color).toFloat() / 255.0f

internal fun blueFraction(@ColorInt color: Int) = Color.blue(color).toFloat() / 255.0f

/**
 * Returns an RGB color that has the same effect as drawing `color` with `alphaFraction` over a
 * `backgroundColor` background.
 *
 * @param color the foreground color
 * @param alphaFraction the fraction of the alpha value, range from 0 to 1
 * @param backgroundColor the background color
 */
internal fun getRGBColor(
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

internal enum class DigitType {
    HOUR_TENS,
    HOUR_UNITS,
    MINUTE_TENS,
    MINUTE_UNITS,
    SECOND_TENS,
    SECOND_UNITS
}

/** A class to provide string representations of each of the digits of a given time. */
internal class DigitStrings {
    private var hourTens = ""
    private var hourUnits = ""
    private var minuteTens = ""
    private var minuteUnits = ""
    private var secondTens = ""
    private var secondUnits = ""

    /** Sets the time represented by this instance. */
    fun set(time: Calendar, is24Hour: Boolean) {
        if (is24Hour) {
            val hourValue = time.get(Calendar.HOUR_OF_DAY)
            hourTens = getTensDigitString(hourValue, true)
            hourUnits = getUnitsDigitString(hourValue)
        } else {
            var hourValue = time.get(Calendar.HOUR)
            // We should show 12 for noon and midnight.
            if (hourValue == 0) {
                hourValue = 12
            }
            hourTens = getTensDigitString(hourValue, false)
            hourUnits = getUnitsDigitString(hourValue)
        }

        val minuteValue = time.get(Calendar.MINUTE)
        minuteTens = getTensDigitString(minuteValue, true)
        minuteUnits = getUnitsDigitString(minuteValue)
        val secondsValue = time.get(Calendar.SECOND)
        secondTens = getTensDigitString(secondsValue, true)
        secondUnits = getUnitsDigitString(secondsValue)
    }

    /** Returns a string representing the specified digit of the time represented by this object. */
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
     * Returns the number of hour digits in this object. If the representation is 24-hour, this will
     * always return 2. If 12-hour, this will return 1 or 2.
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
        // We don't use toString() because during draw calls we don't want to avoid allocating objects.
        return DIGITS[(value / 10) % 10]
    }

    /**
     * Returns a {@link String} representing the units digit of the provided non-negative {@code
     * value}.
     */
    private fun getUnitsDigitString(value: Int): String {
        // We don't use toString() because during draw calls we don't want to avoid allocating objects.
        return DIGITS[value % 10]
    }
}

/** Returns a linear interpolation between a and b using the scalar s.  */
private fun lerp(a: Float, b: Float, s: Float) = a + s * (b - a)

/**
 * Returns the interpolation scalar (s) that satisfies the equation: `value = lerp(a, b, s)`
 *
 * If `a == b`, then this function will return 0.
 */
private fun lerpInv(a: Float, b: Float, value: Float) = if (a != b) (value - a) / (b - a) else 0.0f

internal fun getInterpolatedValue(
    startValue: Float,
    endValue: Float,
    startTime: Float,
    endTime: Float,
    currentTime: Float,
    interpolator: TimeInterpolator
): Float {
    val progress = when {
        currentTime < startTime -> 0f
        currentTime > endTime -> 1f
        else -> interpolator.getInterpolation(lerpInv(startTime, endTime, currentTime))
    }
    return lerp(startValue, endValue, progress)
}

internal data class DigitDrawProperties(
    var shouldDraw: Boolean = false,
    var scale: Float = 0f,
    var rotation: Float = 0f,
    var opacity: Float = 0f
)

/**
 * Sets the [DigitDrawProperties] that should be used for drawing, given the specified
 * parameters.
 *
 * @param secondProgress the sub-second part of the current time, where 0 means the current second
 * has just begun, and 1 means the current second has just ended
 * @param offsetSeconds a value added to the start and end time of the animations
 * @param digitMode whether the digit is OUTGOING or INCOMING
 * @param output the [DigitDrawProperties] that will be set
 */
internal fun getDigitDrawProperties(
    secondProgress: Float,
    offsetSeconds: Float,
    digitMode: DigitMode,
    output: DigitDrawProperties
) {
    val startTime = DIGIT_ANIMATION_START_TIME_FRACTION[digitMode]!! + offsetSeconds
    val endTime = DIGIT_ANIMATION_END_TIME[digitMode]!! + offsetSeconds
    output.shouldDraw = if (digitMode == DigitMode.OUTGOING) {
        secondProgress < endTime
    } else {
        secondProgress >= startTime
    }
    output.scale = getInterpolatedValue(
        DIGIT_SCALE_START[digitMode]!!,
        DIGIT_SCALE_END[digitMode]!!,
        startTime,
        endTime,
        secondProgress,
        DIGIT_SCALE_INTERPOLATOR[digitMode]!!
    )
    output.rotation = getInterpolatedValue(
        DIGIT_ROTATE_START_DEGREES[digitMode]!!,
        DIGIT_ROTATE_END_DEGREES[digitMode]!!,
        startTime,
        endTime,
        secondProgress,
        DIGIT_ROTATION_INTERPOLATOR[digitMode]!!
    )
    output.opacity = getInterpolatedValue(
        DIGIT_OPACITY_START[digitMode]!!,
        DIGIT_OPACITY_END[digitMode]!!,
        startTime,
        endTime,
        secondProgress,
        DIGIT_OPACITY_INTERPOLATOR[digitMode]!!
    )
}

internal fun getTimeOffsetSeconds(digitType: DigitType): Float {
    return when (digitType) {
        DigitType.HOUR_TENS -> 5f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
        DigitType.HOUR_UNITS -> 4f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
        DigitType.MINUTE_TENS -> 3f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
        DigitType.MINUTE_UNITS -> 2f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
        DigitType.SECOND_TENS -> 1f * TIME_OFFSET_SECONDS_PER_DIGIT_TYPE
        DigitType.SECOND_UNITS -> 0f
    }
}

private class DrawProperties(
    var backgroundAlpha: Float = 1f,
    var timeScale: Float = 1f,
    var secondsScale: Float = 1f
) {
    companion object {
        val TIME_SCALE = object : FloatProperty<DrawProperties>("timeScale") {
            override fun setValue(obj: DrawProperties, value: Float) {
                obj.timeScale = value
            }

            override fun get(obj: DrawProperties): Float {
                return obj.timeScale
            }
        }

        val SECONDS_SCALE = object : FloatProperty<DrawProperties>("secondsScale") {
            override fun setValue(obj: DrawProperties, value: Float) {
                obj.secondsScale = value
            }

            override fun get(obj: DrawProperties): Float {
                return obj.secondsScale
            }
        }
    }
}

/** Applies a multiplier to a color, e.g. to darken if it's < 1.0 */
internal fun multiplyColor(colorInt: Int, multiplier: Float): Int {
    val adjustedMultiplier = multiplier / 255.0f
    return colorRgb(
        Color.red(colorInt).toFloat() * adjustedMultiplier,
        Color.green(colorInt).toFloat() * adjustedMultiplier,
        Color.blue(colorInt).toFloat() * adjustedMultiplier,
    )
}

internal fun createBoundsRect(
    centerFraction: PointF,
    size: Vec2f
): RectF {
    val halfWidth = size.x / 2.0f
    val halfHeight = size.y / 2.0f
    return RectF(
        (centerFraction.x - halfWidth),
        (centerFraction.y - halfHeight),
        (centerFraction.x + halfWidth),
        (centerFraction.y + halfHeight)
    )
}

fun Byte.toUnsigned(): Int {
    return if (this < 0) this + 256 else this.toInt()
}

/** A simple example canvas based digital watch face. */
class ExampleCanvasDigitalWatchFaceService : WatchFaceService() {
    // Lazy because the context isn't initialized til later.
    private val watchFaceStyle by lazy { WatchFaceColorStyle.create(this, RED_STYLE) }

    private val colorStyleSetting = UserStyleSetting.ListUserStyleSetting(
        UserStyleSetting.Id(COLOR_STYLE_SETTING),
        getString(R.string.colors_style_setting),
        getString(R.string.colors_style_setting_description),
        icon = null,
        options = listOf(
            UserStyleSetting.ListUserStyleSetting.ListOption(
                Option.Id(RED_STYLE),
                getString(R.string.colors_style_red),
                Icon.createWithResource(this, R.drawable.red_style)
            ),
            UserStyleSetting.ListUserStyleSetting.ListOption(
                Option.Id(GREEN_STYLE),
                getString(R.string.colors_style_green),
                Icon.createWithResource(this, R.drawable.green_style)
            ),
            UserStyleSetting.ListUserStyleSetting.ListOption(
                Option.Id(BLUE_STYLE),
                getString(R.string.colors_style_blue),
                Icon.createWithResource(this, R.drawable.blue_style)
            )
        ),
        listOf(
            WatchFaceLayer.BASE,
            WatchFaceLayer.COMPLICATIONS,
            WatchFaceLayer.COMPLICATIONS_OVERLAY
        )
    )

    private val userStyleSchema = UserStyleSchema(listOf(colorStyleSetting))

    private val canvasComplicationFactory =
        CanvasComplicationFactory { watchState, listener ->
            CanvasComplicationDrawable(
                watchFaceStyle.getDrawable(this@ExampleCanvasDigitalWatchFaceService)!!,
                watchState,
                listener
            )
        }

    private val leftComplication = Complication.createRoundRectComplicationBuilder(
        ComplicationID.LEFT.ordinal,
        canvasComplicationFactory,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        ),
        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_WATCH_BATTERY),
        ComplicationBounds(
            createBoundsRect(
                LEFT_CIRCLE_COMPLICATION_CENTER_FRACTION,
                CIRCLE_COMPLICATION_DIAMETER_FRACTION
            )
        )
    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
        .build()

    private val rightComplication = Complication.createRoundRectComplicationBuilder(
        ComplicationID.RIGHT.ordinal,
        canvasComplicationFactory,
        listOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        ),
        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_DATE),
        ComplicationBounds(
            createBoundsRect(
                RIGHT_CIRCLE_COMPLICATION_CENTER_FRACTION,
                CIRCLE_COMPLICATION_DIAMETER_FRACTION
            )
        )
    ).setDefaultProviderType(ComplicationType.SHORT_TEXT)
        .build()

    private val upperAndLowerComplicationTypes = listOf(
        ComplicationType.LONG_TEXT,
        ComplicationType.RANGED_VALUE,
        ComplicationType.SHORT_TEXT,
        ComplicationType.MONOCHROMATIC_IMAGE,
        ComplicationType.SMALL_IMAGE
    )
    // The upper and lower complications change shape depending on the complication's type.
    private val upperComplication = Complication.createRoundRectComplicationBuilder(
        ComplicationID.UPPER.ordinal,
        canvasComplicationFactory,
        upperAndLowerComplicationTypes,
        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_WORLD_CLOCK),
        ComplicationBounds(
            ComplicationType.values().associateWith {
                if (it == ComplicationType.LONG_TEXT) {
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
            }
        )
    ).setDefaultProviderType(ComplicationType.LONG_TEXT)
        .build()

    private val lowerComplication = Complication.createRoundRectComplicationBuilder(
        ComplicationID.LOWER.ordinal,
        canvasComplicationFactory,
        upperAndLowerComplicationTypes,
        DefaultComplicationProviderPolicy(SystemProviders.PROVIDER_NEXT_EVENT),
        ComplicationBounds(
            ComplicationType.values().associateWith {
                if (it == ComplicationType.LONG_TEXT) {
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
            }
        )
    ).setDefaultProviderType(ComplicationType.LONG_TEXT)
        .build()

    private val backgroundComplication = Complication.createBackgroundComplicationBuilder(
        ComplicationID.BACKGROUND.ordinal,
        canvasComplicationFactory,
        listOf(ComplicationType.PHOTO_IMAGE),
        DefaultComplicationProviderPolicy()
    ).build()

    override fun createUserStyleSchema() = userStyleSchema

    override fun createComplicationsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = ComplicationsManager(
        listOf(
            leftComplication,
            rightComplication,
            upperComplication,
            lowerComplication,
            backgroundComplication
        ),
        currentUserStyleRepository
    )

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ): WatchFace {
        val renderer = ExampleDigitalWatchCanvasRenderer(
            surfaceHolder,
            this,
            watchFaceStyle,
            currentUserStyleRepository,
            watchState,
            colorStyleSetting,
            complicationsManager
        )

        // createWatchFace is called on a worker thread but the observers should be called from the
        // UiThread.
        getUiThreadHandler().post {
            upperComplication.complicationData.addObserver {
                // Force bounds recalculation, because this can affect the size of the central time
                // display.
                renderer.oldBounds.set(0, 0, 0, 0)
            }
            lowerComplication.complicationData.addObserver {
                // Force bounds recalculation, because this can affect the size of the central time
                // display.
                renderer.oldBounds.set(0, 0, 0, 0)
            }
        }
        return WatchFace(WatchFaceType.DIGITAL, renderer)
    }
}

class ExampleDigitalWatchCanvasRenderer(
    surfaceHolder: SurfaceHolder,
    private val context: Context,
    private var watchFaceColorStyle: WatchFaceColorStyle,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
    private val colorStyleSetting: UserStyleSetting.ListUserStyleSetting,
    private val complicationsManager: ComplicationsManager
) : Renderer.CanvasRenderer(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    INTERACTIVE_UPDATE_RATE_MS
) {
    internal var oldBounds = Rect(0, 0, 0, 0)

    private fun getBaseDigitPaint() = Paint().apply {
        typeface = Typeface.create(DIGITAL_TYPE_FACE, Typeface.NORMAL)
        isAntiAlias = true
    }

    private val digitTextHoursPaint = getBaseDigitPaint()
    private val digitTextMinutesPaint = getBaseDigitPaint()
    private val digitTextSecondsPaint = getBaseDigitPaint()

    // Used for drawing the cached digits to the watchface.
    private val digitBitmapPaint = Paint().apply {
        isFilterBitmap = true
    }

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
    private val nextSecondTime = GregorianCalendar()
    private val currentDigitStrings = DigitStrings()
    private val nextDigitStrings = DigitStrings()
    private val digitDrawProperties = DigitDrawProperties()
    private var drawProperties = DrawProperties()
    private var prevDrawMode = DrawMode.INTERACTIVE

    // Animation played when exiting ambient mode.
    private val ambientExitAnimator = AnimatorSet().apply {
        val linearOutSlow = AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.linear_out_slow_in
        )
        playTogether(
            ObjectAnimator.ofFloat(
                drawProperties,
                DrawProperties.TIME_SCALE,
                1.0f
            ).apply {
                duration = AMBIENT_TRANSITION_MS
                interpolator = linearOutSlow
                setAutoCancel(true)
            },
            ObjectAnimator.ofFloat(
                drawProperties,
                DrawProperties.SECONDS_SCALE,
                1.0f
            ).apply {
                duration = AMBIENT_TRANSITION_MS
                interpolator = linearOutSlow
                setAutoCancel(true)
            }
        )
    }

    // Animation played when entering ambient mode.
    private val ambientEnterAnimator = AnimatorSet().apply {
        val fastOutLinearIn = AnimationUtils.loadInterpolator(
            context,
            android.R.interpolator.fast_out_linear_in
        )
        playTogether(
            ObjectAnimator.ofFloat(
                drawProperties,
                DrawProperties.TIME_SCALE,
                1.0f
            ).apply {
                duration = AMBIENT_TRANSITION_MS
                interpolator = fastOutLinearIn
                setAutoCancel(true)
            },
            ObjectAnimator.ofFloat(
                drawProperties,
                DrawProperties.SECONDS_SCALE,
                0.0f
            ).apply {
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
    // (stored in [][digitBitmapCache]) displays.
    private val currentCachedDigits = SparseArray<String>()

    init {
        // Listen for style changes.
        currentUserStyleRepository.addUserStyleChangeListener(
            object : CurrentUserStyleRepository.UserStyleChangeListener {
                @SuppressLint("SyntheticAccessor")
                override fun onUserStyleChanged(userStyle: UserStyle) {
                    watchFaceColorStyle =
                        WatchFaceColorStyle.create(
                            context,
                            userStyle[colorStyleSetting]!!.toString()
                        )

                    // Apply the userStyle to the complications. ComplicationDrawables for each of
                    // the styles are defined in XML so we need to replace the complication's
                    // drawables.
                    for ((_, complication) in complicationsManager.complications) {
                        (complication.renderer as CanvasComplicationDrawable).drawable =
                            watchFaceColorStyle.getDrawable(context)!!
                    }

                    clearDigitBitmapCache()
                }
            }
        )

        watchState.isAmbient.addObserver {
            if (it) {
                ambientEnterAnimator.start()
            } else {
                ambientExitAnimator.start()
            }

            // Trigger recomputation of bounds.
            oldBounds.set(0, 0, 0, 0)
            val antiAlias = !(it && watchState.hasLowBitAmbient)
            digitTextHoursPaint.setAntiAlias(antiAlias)
            digitTextMinutesPaint.setAntiAlias(antiAlias)
            digitTextSecondsPaint.setAntiAlias(antiAlias)
        }
    }

    override fun shouldAnimate(): Boolean {
        // Make sure we keep animating while ambientEnterAnimator is running.
        return ambientEnterAnimator.isRunning || super.shouldAnimate()
    }

    private fun applyColorStyleAndDrawMode(drawMode: DrawMode) {
        digitTextHoursPaint.color = when (drawMode) {
            DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.primaryColor
            DrawMode.LOW_BATTERY_INTERACTIVE ->
                multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.6f)
            DrawMode.MUTE -> multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.8f)
            DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.primaryColor
        }

        digitTextMinutesPaint.color = when (drawMode) {
            DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.primaryColor
            DrawMode.LOW_BATTERY_INTERACTIVE ->
                multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.6f)
            DrawMode.MUTE -> multiplyColor(watchFaceColorStyle.activeStyle.primaryColor, 0.8f)
            DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.primaryColor
        }

        digitTextSecondsPaint.color = when (drawMode) {
            DrawMode.INTERACTIVE -> watchFaceColorStyle.activeStyle.secondaryColor
            DrawMode.LOW_BATTERY_INTERACTIVE ->
                multiplyColor(watchFaceColorStyle.activeStyle.secondaryColor, 0.6f)
            DrawMode.MUTE -> multiplyColor(watchFaceColorStyle.activeStyle.secondaryColor, 0.8f)
            DrawMode.AMBIENT -> watchFaceColorStyle.ambientStyle.secondaryColor
        }

        if (prevDrawMode != drawMode) {
            prevDrawMode = drawMode
            clearDigitBitmapCache()
        }
    }

    override fun render(canvas: Canvas, bounds: Rect, calendar: Calendar) {
        recalculateBoundsIfChanged(bounds, calendar)

        applyColorStyleAndDrawMode(renderParameters.drawMode)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            drawBackground(canvas)
        }

        drawComplications(canvas, calendar)

        if (renderParameters.watchFaceLayers.contains(WatchFaceLayer.BASE)) {
            val is24Hour: Boolean = DateFormat.is24HourFormat(context)

            nextSecondTime.timeInMillis = calendar.timeInMillis
            nextSecondTime.timeZone = calendar.timeZone
            nextSecondTime.add(Calendar.SECOND, 1)

            currentDigitStrings.set(calendar, is24Hour)
            nextDigitStrings.set(nextSecondTime, is24Hour)

            val secondProgress = calendar.get(Calendar.MILLISECOND) / 1000f

            val animationStartFraction = DIGIT_ANIMATION_START_TIME_FRACTION[DigitMode.OUTGOING]!!
            this.interactiveDrawModeUpdateDelayMillis =
                if (secondProgress < animationStartFraction &&
                    !ambientEnterAnimator.isRunning
                ) {
                    // The seconds only animate part of the time so we can sleep until the seconds next need to
                    // animate, which improves battery life.
                    max(
                        INTERACTIVE_UPDATE_RATE_MS,
                        ((animationStartFraction - secondProgress) * 1000f).toLong()
                    )
                } else {
                    INTERACTIVE_UPDATE_RATE_MS
                }

            // Move the left position to the left if there are fewer than two hour digits, to
            // ensure it is centered. If the clock is in transition from one to two hour digits or
            // vice versa, interpolate to animate the clock's position.
            // Move the left position to the left if there are fewer than two hour digits, to ensure
            // it is centered. If the clock is in transition from one to two hour digits or
            // vice versa, interpolate to animate the clock's position.
            val centeringAdjustment = (
                getInterpolatedValue(
                    (2 - currentDigitStrings.getNumberOfHoursDigits()).toFloat(),
                    (2 - nextDigitStrings.getNumberOfHoursDigits()).toFloat(),
                    POSITION_ANIMATION_START_TIME,
                    POSITION_ANIMATION_END_TIME,
                    secondProgress,
                    CENTERING_ADJUSTMENT_INTERPOLATOR
                ) *
                    digitWidth
                )

            // This total width assumes two hours digits.
            val totalWidth = 2f * digitWidth + gapWidth + 2f * smallDigitWidth
            val left = clockBounds.exactCenterX() - 0.5f * (totalWidth + centeringAdjustment)
            val top = clockBounds.exactCenterY() - 0.5f * digitHeight

            val wholeTimeSaveCount = canvas.save()
            try {
                canvas.scale(
                    drawProperties.timeScale, drawProperties.timeScale,
                    clockBounds.exactCenterX(), clockBounds.exactCenterY()
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
                if (drawProperties.secondsScale > 0f &&
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

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, calendar: Calendar) {
        canvas.drawColor(renderParameters.highlightLayer!!.backgroundTint)

        drawComplicationHighlights(canvas, calendar)
    }

    override fun getMainClockElementBounds() = clockBounds

    private fun recalculateBoundsIfChanged(bounds: Rect, calendar: Calendar) {
        if (oldBounds == bounds) {
            return
        }

        oldBounds.set(bounds)
        calculateClockBound(bounds, calendar)
    }

    private fun calculateClockBound(bounds: Rect, calendar: Calendar) {
        val hasVerticalComplication =
            VERTICAL_COMPLICATION_IDS.any {
                complicationsManager[it]!!.isActiveAt(calendar.timeInMillis)
            }
        val hasHorizontalComplication =
            HORIZONTAL_COMPLICATION_IDS.any {
                complicationsManager[it]!!.isActiveAt(calendar.timeInMillis)
            }

        val marginX = if (hasHorizontalComplication) {
            (MARGIN_FRACTION_WITH_COMPLICATION.x * bounds.width().toFloat()).toInt()
        } else {
            (MARGIN_FRACTION_WITHOUT_COMPLICATION.x * bounds.width().toFloat()).toInt()
        }

        val marginY = if (hasVerticalComplication) {
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
            (2f * DIGIT_WIDTH_FRACTION) + (DIGIT_WIDTH_FRACTION * GAP_WIDTH_FRACTION) +
                (2f * SMALL_DIGIT_SIZE_FRACTION * SMALL_DIGIT_WIDTH_FRACTION)

        textSize = min(textSize, (clockBounds.width().toFloat() / totalWidthFraction) * textRatio)

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
        val backgroundColor = if (renderParameters.drawMode == DrawMode.AMBIENT) {
            watchFaceColorStyle.ambientStyle.backgroundColor
        } else {
            watchFaceColorStyle.activeStyle.backgroundColor
        }
        canvas.drawColor(
            getRGBColor(
                backgroundColor,
                drawProperties.backgroundAlpha,
                Color.BLACK
            )
        )
    }

    private fun drawComplications(canvas: Canvas, calendar: Calendar) {
        // First, draw the background complication if not in ambient mode
        if (renderParameters.drawMode != DrawMode.AMBIENT) {
            complicationsManager[ComplicationID.BACKGROUND.ordinal]!!.render(
                canvas,
                calendar,
                renderParameters
            )
        }
        for (i in FOREGROUND_COMPLICATION_IDS) {
            val complication = complicationsManager[i] as Complication
            complication.render(canvas, calendar, renderParameters)
        }
    }

    private fun drawComplicationHighlights(canvas: Canvas, calendar: Calendar) {
        for (i in FOREGROUND_COMPLICATION_IDS) {
            val complication = complicationsManager[i] as Complication
            complication.renderHighlightLayer(canvas, calendar, renderParameters)
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
            secondProgress, getTimeOffsetSeconds(digitType), digitMode, digitDrawProperties
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

    private fun createBitmap(
        width: Int,
        height: Int,
        digitType: DigitType
    ): Bitmap {
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
            DigitType.HOUR_TENS, DigitType.HOUR_UNITS -> {
                width = digitWidth
                height = digitHeight
                paint = digitTextHoursPaint
            }
            DigitType.MINUTE_TENS, DigitType.MINUTE_UNITS -> {
                width = smallDigitWidth
                height = smallDigitHeight
                paint = digitTextMinutesPaint
            }
            DigitType.SECOND_TENS, DigitType.SECOND_UNITS -> {
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
