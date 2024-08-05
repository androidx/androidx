/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3

import android.graphics.Canvas
import android.graphics.Paint.FontMetrics
import android.graphics.fonts.Font
import android.graphics.fonts.FontVariationAxis
import android.graphics.fonts.FontVariationAxis.toFontVariationSettings
import android.graphics.text.PositionedGlyphs
import android.graphics.text.TextRunShaper
import android.text.TextPaint
import android.util.LruCache
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.lerp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A composable that displays an animated text.
 *
 * AnimatedText can be used to animate a text along font variation axes and size. It requires an
 * [AnimatedTextFontRegistry] to improve performance.
 *
 * [AnimatedTextFontRegistry] can be generated using [rememberAnimatedTextFontRegistry] method,
 * which requires start and end font variation axes, and start and end font sizes for the animation.
 *
 * Start of the animation is when the animatable is at 0f and end of the animation is when the
 * animatable is at 1f. Current animation progress is provided by the [progressFraction] function.
 * This should be between 0f and 1f, but might go beyond in some cases such as overshooting spring
 * animations.
 *
 * Example of a one-shot animation with AnimatedText
 *
 * @sample androidx.wear.compose.material3.samples.AnimatedTextSample
 *
 * Example of an animation in response to a button press with AnimatedText
 *
 * @sample androidx.wear.compose.material3.samples.AnimatedTextSampleButtonResponse
 *
 * Example showing how [AnimatedTextFontRegistry] can be reused and shared between two
 * [AnimatedText] composables
 *
 * @sample androidx.wear.compose.material3.samples.AnimatedTextSampleSharedFontRegistry
 * @param text The text to be displayed.
 * @param fontRegistry The font registry to be used to animate the text.
 * @param progressFraction A provider for the current state of the animation. Provided value should
 *   be between 0f and 1f, but might go beyond in some cases such as overshooting spring animations.
 * @param modifier Modifier to be applied to the composable.
 * @param contentAlignment Alignment within the bounds of the Canvas.
 */
@Composable
@RequiresApi(31)
fun AnimatedText(
    text: String,
    fontRegistry: AnimatedTextFontRegistry,
    progressFraction: () -> Float,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val animatedTextState =
        remember(fontRegistry, layoutDirection, density) {
            AnimatedTextState(fontRegistry, layoutDirection, density)
        }
    // Update before composing Canvas to make sure size gets updated
    animatedTextState.updateText(text)
    Canvas(modifier.size(animatedTextState.size)) {
        animatedTextState.draw(
            drawContext.canvas.nativeCanvas,
            contentAlignment,
            progressFraction()
        )
    }
}

/**
 * Generates an [AnimatedTextFontRegistry] to use within composition.
 *
 * Start and end of the animation is when the animatable is at 0f and 1f, respectively. This API
 * supports overshooting, so a generated font can be extrapolated outside
 * [startFontVariationSettings] and [endFontVariationSettings] range, developers need to make sure
 * the given font supports possible font variation settings throughout the animation.
 *
 * @param startFontVariationSettings Font variation settings at the start of the animation
 * @param endFontVariationSettings Font variation settings at the end of the animation
 * @param textStyle Text style to be used for the animation
 * @param startFontSize Font size at the start of the animation
 * @param endFontSize Font size at the end of the animation
 */
@Composable
@RequiresApi(31)
fun rememberAnimatedTextFontRegistry(
    startFontVariationSettings: FontVariation.Settings,
    endFontVariationSettings: FontVariation.Settings,
    textStyle: TextStyle = LocalTextStyle.current,
    startFontSize: TextUnit = textStyle.fontSize,
    endFontSize: TextUnit = textStyle.fontSize,
): AnimatedTextFontRegistry {
    // Convert current compose values to types we can use on Canvas
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val contentColor = textStyle.color.takeOrElse { LocalContentColor.current }
    return remember(
        textStyle,
        startFontVariationSettings.toString(),
        endFontVariationSettings.toString(),
        startFontSize,
        endFontSize,
        contentColor,
        fontFamilyResolver,
    ) {
        AnimatedTextFontRegistry(
            textStyle,
            startFontVariationSettings,
            endFontVariationSettings,
            startFontSize,
            endFontSize,
            density,
            fontFamilyResolver,
            contentColor,
        )
    }
}

/**
 * Generates fonts to be used by [AnimatedText] throughout the animation.
 *
 * Start and end of the animation is when the animatable is at 0f and 1f, respectively. This API
 * supports overshooting, so a generated font can be extrapolated outside
 * [startFontVariationSettings] and [endFontVariationSettings] range, developers need to make sure
 * the given font supports possible font variation settings throughout the animation.
 *
 * [AnimatedTextFontRegistry] can be re-used between multiple [AnimatedText] composables to save
 * memory and improve performance where needed and feasible.
 *
 * @param startFontVariationSettings Font variation settings at the start of the animation
 * @param endFontVariationSettings Font variation settings at the end of the animation
 * @param textStyle Text style to be used for the animation
 * @param startFontSize Font size at the start of the animation
 * @param endFontSize Font size at the end of the animation
 * @param density Current density, used to to convert font sizes
 * @param contentColor Content color of the animated text
 * @param fontFamilyResolver Current Resolver to use to resolve font families
 * @param cacheSize Size of the cache used to store animated variable fonts, this can be increased
 *   to improve animation performance if needed, but it also increases the memory usage.
 */
@RequiresApi(31)
class AnimatedTextFontRegistry(
    private val textStyle: TextStyle,
    private val startFontVariationSettings: FontVariation.Settings,
    private val endFontVariationSettings: FontVariation.Settings,
    private val startFontSize: TextUnit,
    private val endFontSize: TextUnit,
    private val density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    private val contentColor: Color = textStyle.color,
    cacheSize: Int = AnimatedTextDefaults.CacheSize,
) {
    private val startFontSizePx = with(density) { startFontSize.toPx() }
    private val endFontSizePx = with(density) { endFontSize.toPx() }

    /**
     * Returns the font at a certain [fraction] of the animation. [text] parameter is required to
     * extract the initial font to draw the text animation.
     */
    internal fun getFont(text: String, fraction: Float): Font {
        val snappedFraction =
            floor(fraction / AnimatedTextDefaults.FractionStep) * AnimatedTextDefaults.FractionStep
        return when (fraction) {
            0f -> getStartFont(text)
            1f -> getEndFont(text)
            else -> {
                val font =
                    fontCache[fraction]
                        ?: Font.Builder(getStartFont(text))
                            .setFontVariationSettings(
                                lerpFontVariationSettings(
                                    startFontVariationSettings,
                                    endFontVariationSettings,
                                    snappedFraction
                                )
                            )
                            .build()
                fontCache.put(fraction, font)
                font
            }
        }
    }

    /** Returns the font size at a certain point in the animation. */
    internal fun getFontSize(fraction: Float): Float =
        lerp(startFontSizePx, endFontSizePx, fraction)

    /** Font cache for animation steps, between start font at 0f and end font at 1f */
    private var fontCache = LruCache<Float, Font>(cacheSize)

    /**
     * Array to store current font variation axes, empty at the start.
     *
     * This helps reduce allocations during the draw phase.
     */
    private var currentAxes =
        Array(startFontVariationSettings.settings.size) { FontVariationAxis("null", 0f) }

    /** Font at the start of the animation. */
    private var startFont: Font? = null

    /** Font at the end of the animation. */
    private var endFont: Font? = null

    /** TextPaint to be used when drawing onto canvas, this is initially set to the start font. */
    internal var startWorkingPaint: TextPaint

    /** TextPaint that reflects how the end font should look. */
    internal var endWorkingPaint: TextPaint

    init {
        startWorkingPaint = generateStartWorkingPaint(fontFamilyResolver)
        endWorkingPaint = generateEndWorkingPaint(startWorkingPaint)
    }

    private fun generateStartWorkingPaint(fontFamilyResolver: FontFamily.Resolver): TextPaint {
        val textPaint =
            TextPaint().apply {
                typeface =
                    fontFamilyResolver
                        .resolveAsTypeface(
                            fontFamily = textStyle.fontFamily,
                            textStyle.fontWeight ?: FontWeight.Normal,
                            textStyle.fontStyle ?: FontStyle.Normal,
                            textStyle.fontSynthesis ?: FontSynthesis.All,
                        )
                        .value
                color = contentColor.toArgb()
            }
        textPaint.textSize = startFontSizePx
        textPaint.setFontVariationSettings(
            toFontVariationSettings(
                lerpFontVariationSettings(startFontVariationSettings, endFontVariationSettings, 0f)
            )
        )
        return textPaint
    }

    private fun generateEndWorkingPaint(startWorkingPaint: TextPaint): TextPaint {
        val textPaint = TextPaint(startWorkingPaint)
        textPaint.textSize = endFontSizePx
        textPaint.setFontVariationSettings(
            toFontVariationSettings(
                lerpFontVariationSettings(startFontVariationSettings, endFontVariationSettings, 1f)
            )
        )
        return textPaint
    }

    private fun lerpFontVariationSettings(
        startFontVariationSettings: FontVariation.Settings,
        endFontVariationSettings: FontVariation.Settings,
        fraction: Float
    ): Array<FontVariationAxis> {
        startFontVariationSettings.settings.indices.forEach { startIndex ->
            // Find the corresponding FontVariation.Setting in endFontVariationSettings
            var endSetting = startFontVariationSettings.settings[startIndex]
            for (endIndex in endFontVariationSettings.settings.indices) {
                if (
                    endFontVariationSettings.settings[endIndex].axisName ==
                        startFontVariationSettings.settings[startIndex].axisName
                ) {
                    endSetting = endFontVariationSettings.settings[endIndex]
                    break
                }
            }
            currentAxes[startIndex] =
                FontVariationAxis(
                    startFontVariationSettings.settings[startIndex].axisName,
                    lerp(
                        startFontVariationSettings.settings[startIndex].toVariationValue(density),
                        endSetting.toVariationValue(density),
                        fraction
                    )
                )
        }
        return currentAxes
    }

    private fun getEndFont(text: String): Font {
        if (endFont != null) {
            return endFont as Font
        }
        endFont =
            Font.Builder(getStartFont(text))
                .setFontVariationSettings(
                    lerpFontVariationSettings(
                        startFontVariationSettings,
                        endFontVariationSettings,
                        1f
                    )
                )
                .build()
        return endFont as Font
    }

    private fun getStartFont(text: String): Font {
        if (startFont != null) {
            return startFont as Font
        }
        // This is required to generate font from the Typeface using Native APIs
        // Maybe we can find another way without running shapeTextRun?
        val glyphs =
            TextRunShaper.shapeTextRun(
                text,
                0,
                text.length,
                0,
                text.length,
                0f,
                0f,
                false, // Correct layout direction isn't needed for generating the font
                startWorkingPaint
            )
        startFont =
            Font.Builder(glyphs.getFont(0))
                .setFontVariationSettings(
                    lerpFontVariationSettings(
                        startFontVariationSettings,
                        endFontVariationSettings,
                        0f
                    )
                )
                .build()
        return startFont as Font
    }
}

/** Defaults for AnimatedText. */
@RequiresApi(31)
object AnimatedTextDefaults {
    /** Default font cache size to be used in AnimatedTextFontRegistry. */
    const val CacheSize = 5

    /**
     * Default step size used to snap progress fractions. Progress fractions will be rounded down to
     * a multiple of this to increase cache efficiency.
     *
     * 0.016f is chosen to divide a 1 second animation into 60 animation steps.
     */
    internal const val FractionStep = 0.016f
}

/**
 * Animated text state.
 *
 * Generates the fonts required using the given [animatedFontRegistry] and draws the animation
 * inside an [AnimatedText] controlled Canvas.
 *
 * This is generated by [AnimatedText].
 */
@RequiresApi(31)
internal class AnimatedTextState
internal constructor(
    private val animatedFontRegistry: AnimatedTextFontRegistry,
    private val layoutDirection: LayoutDirection,
    private val density: Density,
) {
    /**
     * Required size for the canvas.
     *
     * It's a mutable state to make sure it triggers recomposition when the text changes.
     */
    internal var size: DpSize by mutableStateOf(DpSize.Zero)

    /** Updates the text to draw. */
    internal fun updateText(text: String) {
        if (currentText == text) {
            return
        }
        currentText = text
        recalculateSizeAndPositions()
    }

    /** Draws the text onto a canvas. */
    internal fun draw(canvas: Canvas, contentAlignment: Alignment, fraction: Float) {
        // Nothing to draw, return early
        if (size == DpSize.Zero) {
            return
        }
        val widthPx = lerp(startWidthPx, endWidthPx, fraction)
        val heightPx = lerp(startHeightPx, endHeightPx, fraction)
        val offset =
            contentAlignment.align(
                IntSize(widthPx.roundToInt(), heightPx.roundToInt()),
                intSize,
                layoutDirection
            )
        canvas.translate(
            offset.x.toFloat(),
            offset.y.toFloat() +
                heightPx / 2 +
                lerp(startBaselineOffset, endBaselineOffset, fraction) / 2 +
                lerp(startAscentPx, endAscentPx, fraction) / 4
        )
        val currentFont = animatedFontRegistry.getFont(currentText, fraction)
        animatedFontRegistry.startWorkingPaint.textSize = animatedFontRegistry.getFontSize(fraction)
        val startGlyphs = startPositionedGlyphs!!
        val endGlyphs = endPositionedGlyphs!!
        for (i in 0 until startGlyphs.glyphCount()) {
            val glyphFont = startGlyphs.getFont(i)
            canvas.drawGlyphs(
                intArrayOf(startGlyphs.getGlyphId(i)),
                0,
                floatArrayOf(
                    lerp(startGlyphs.getGlyphX(i), endGlyphs.getGlyphX(i), fraction),
                    lerp(startGlyphs.getGlyphY(i), endGlyphs.getGlyphY(i), fraction)
                ),
                0,
                1,
                if (currentFont.file?.name != glyphFont.file?.name) {
                    glyphFont
                } else {
                    currentFont
                },
                animatedFontRegistry.startWorkingPaint,
            )
        }
    }

    /**
     * Same as [size], but in pixels, used to calculate content offset according to content
     * alignment.
     */
    private var intSize = IntSize(0, 0)

    /** Content height at the start of the animation, in px */
    private var startHeightPx = 0f

    /** Content height at the end of the animation, in px */
    private var endHeightPx = 0f

    /** Baseline offset at the start, used for content alignment. */
    private var startBaselineOffset: Int = 0

    /** Baseline offset at the end, used for content alignment. */
    private var endBaselineOffset: Int = 0

    /** Font ascent at the start of the animation, used for content alignment. */
    private var startAscentPx = 0

    /** Font ascent at the end of the animation, used for content alignment. */
    private var endAscentPx = 0

    /** Current text. */
    private var currentText: String = ""

    /** Content width at the start of the animation, in px */
    private var startWidthPx = 0f

    /** Content width at the end of the animation, in px */
    private var endWidthPx = 0f

    /** Positions of the glyphs at the start, used to calculate lerped glyph positions */
    private var startPositionedGlyphs: PositionedGlyphs? = null

    /** Positions of the glyphs at the end, used to calculate lerped glyph positions */
    private var endPositionedGlyphs: PositionedGlyphs? = null

    /**
     * Calculates required canvas size to draw the text, font ascent and baseline offset for the
     * font, positions for the glyphs at the start and the end of the animation.
     */
    private fun recalculateSizeAndPositions() {
        with(density) {
            intSize = IntSize(calculateMaxWidth().roundToInt(), calculateMaxHeight().roundToInt())
            size = DpSize(intSize.width.toDp(), intSize.height.toDp())
        }
    }

    private fun calculateMaxWidth(): Float {
        if (currentText.isEmpty()) {
            startWidthPx = 0f
            startPositionedGlyphs = null
            endWidthPx = 0f
            endPositionedGlyphs = null
            return 0f
        }
        startWidthPx = 0f
        endWidthPx = 0f
        startPositionedGlyphs =
            TextRunShaper.shapeTextRun(
                currentText,
                0,
                currentText.length,
                0,
                currentText.length,
                0f,
                0f,
                layoutDirection == LayoutDirection.Rtl,
                animatedFontRegistry.startWorkingPaint
            )
        startWidthPx = startPositionedGlyphs!!.advance
        endPositionedGlyphs =
            TextRunShaper.shapeTextRun(
                currentText,
                0,
                currentText.length,
                0,
                currentText.length,
                0f,
                0f,
                layoutDirection == LayoutDirection.Rtl,
                animatedFontRegistry.endWorkingPaint
            )
        endWidthPx = endPositionedGlyphs!!.advance
        return max(startWidthPx, endWidthPx)
    }

    private fun calculateMaxHeight(): Float {
        if (currentText.isEmpty()) {
            startHeightPx = 0f
            startAscentPx = 0
            startBaselineOffset = 0
            endHeightPx = 0f
            endAscentPx = 0
            endBaselineOffset = 0
            return 0f
        }
        val fontMetrics = FontMetrics()
        val startFont = animatedFontRegistry.getFont(currentText, 0f)
        val endFont = animatedFontRegistry.getFont(currentText, 1f)
        startFont.getMetrics(animatedFontRegistry.startWorkingPaint, fontMetrics)
        startHeightPx = fontMetrics.descent - fontMetrics.ascent
        startAscentPx = fontMetrics.ascent.roundToInt()
        startBaselineOffset = -fontMetrics.top.roundToInt()
        endFont.getMetrics(animatedFontRegistry.endWorkingPaint, fontMetrics)
        endHeightPx = fontMetrics.descent - fontMetrics.ascent
        endAscentPx = fontMetrics.ascent.roundToInt()
        endBaselineOffset = -fontMetrics.top.roundToInt()
        return max(startHeightPx, endHeightPx)
    }
}
