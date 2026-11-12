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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteDrawScope
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.layout.rememberRemoteTextMeasurer
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clearAndSetSemantics
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.RemoteTimeDefaults
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.text.toRemoteTypeface
import androidx.compose.runtime.Composable
import androidx.wear.compose.remote.material3.internal.setLuminance

/**
 * Displays the current time and optional surrounding status text curved along the top of a circular
 * screen.
 *
 * [RemoteTimeText] is typically placed at the top of the screen and is the remote equivalent of
 * [androidx.wear.compose.material3.TimeText].
 *
 * Commonly used properties from [RemoteTextStyle] are also exposed directly as parameters: when
 * non-`null`, a styling parameter overrides the corresponding property in [style]; when `null`, the
 * value from [style] is used instead.
 *
 * Example of displaying the current time at the top of a circular screen:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTimeTextSample
 *
 * Example of displaying [RemoteTimeText] with leading status text:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTimeTextWithLeadingTextSample
 * @param modifier The [RemoteModifier] to be applied to the [RemoteTimeText].
 * @param time The text to display as the time. Defaults to a formatted time string from the remote
 *   context.
 * @param fontSize The size of glyphs to use when painting the text. If `null`, uses
 *   [RemoteTextStyle.fontSize] from [style], or `14.sp` if also `null` in [style].
 * @param fontFamily The font family to be used when rendering the text. If `null`, uses
 *   [RemoteTextStyle.fontFamily] from [style], or the default typeface if also `null` in [style].
 * @param leadingText Text to be displayed before the time, or `null` if not present.
 * @param trailingText Text to be displayed after the time, or `null` if not present.
 * @param separator The separator to be used between the leading/trailing text and the time.
 *   Defaults to `"·"`.
 * @param color The color of the text. If `null`, uses [RemoteTextStyle.color] from [style], or
 *   [RemoteColorScheme.onBackground] from [RemoteMaterialTheme.colorScheme] with its luminance set
 *   to 80 (matching [androidx.wear.compose.material3.TimeText]) if also `null` in [style].
 * @param backgroundColor The color of the arc drawn behind the text, so it stays visible on top of
 *   other content. Defaults to [RemoteTimeTextDefaults.backgroundColor].
 * @param style The [RemoteTextStyle] to be applied to the text. Defaults to
 *   [RemoteTimeTextDefaults.TimeTextStyle].
 * @param fontFeatureSettings The OpenType font feature settings applied to the text. If `null`,
 *   uses [RemoteTextStyle.fontFeatureSettings] from [style] (which defaults to
 *   [RemoteTimeTextDefaults.FontFeatureSettings]), or applies no custom feature settings if also
 *   `null` in [style].
 * @param fontVariationSettings The OpenType font variation settings applied to the text. If `null`,
 *   uses [RemoteTextStyle.fontVariationSettings] from [style] (which defaults to
 *   [RemoteTimeTextDefaults.FontVariationSettings]), or applies no custom variation settings if
 *   also `null` in [style].
 */
@RemoteComposable
@Composable
public fun RemoteTimeText(
    modifier: RemoteModifier = RemoteModifier,
    time: RemoteString = RemoteTimeDefaults.defaultTimeString(),
    fontSize: RemoteTextUnit? = null,
    fontFamily: RemoteFontFamily? = null,
    leadingText: RemoteString? = null,
    trailingText: RemoteString? = null,
    separator: RemoteString = "·".rs,
    color: RemoteColor? = null,
    backgroundColor: RemoteColor = RemoteTimeTextDefaults.backgroundColor,
    style: RemoteTextStyle = RemoteTimeTextDefaults.TimeTextStyle,
    fontFeatureSettings: String? = null,
    fontVariationSettings: RemoteFontVariation.Settings? = null,
) {
    val text =
        buildTimeTextString(
            time = time,
            leadingText = leadingText ?: "".rs,
            trailingText = trailingText ?: "".rs,
            separator = separator,
        )
    val mergedStyle =
        style.merge(
            color = color,
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            fontVariationSettings = fontVariationSettings,
        )
    val resolvedFontSize = mergedStyle.fontSize ?: 14.rsp
    val resolvedColor =
        mergedStyle.color
            ?: RemoteMaterialTheme.colorScheme.onBackground.setLuminance(DefaultColorLuminance)
    // A fully transparent background isn't drawn, so the text doesn't need to be measured.
    val textWidth =
        if (backgroundColor.alpha.constantValueOrNull == 0f) {
            null
        } else {
            rememberRemoteTextMeasurer()
                .measureSingleLine(text, RemoteTextStyle(fontSize = resolvedFontSize))
                .size
                .width
        }

    RemoteBox(modifier.clearAndSetSemantics {}) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val fontSizePx = resolvedFontSize.toPx()
            if (textWidth != null) {
                drawTimeTextBackground(
                    textWidth = textWidth,
                    backgroundColor = backgroundColor,
                    fontSize = fontSizePx,
                )
            }
            drawTimeText(
                text = text,
                textColor = resolvedColor,
                fontSize = fontSizePx,
                fontFamily = mergedStyle.fontFamily,
                fontVariationSettings = mergedStyle.fontVariationSettings,
                fontFeatureSettings = mergedStyle.fontFeatureSettings,
            )
        }
    }
}

@Composable
private fun buildTimeTextString(
    time: RemoteString,
    leadingText: RemoteString,
    trailingText: RemoteString,
    separator: RemoteString,
): RemoteString {
    val leadingWithSeparator = leadingText.isNotEmpty.select(leadingText + separator, "".rs)
    val trailingWithSeparator = trailingText.isNotEmpty.select(separator + trailingText, "".rs)
    return leadingWithSeparator + time + trailingWithSeparator
}

private fun RemoteDrawScope.drawTimeTextBackground(
    textWidth: RemoteFloat,
    backgroundColor: RemoteColor,
    fontSize: RemoteFloat,
) {
    // The text baseline lies on this circle, with the glyphs drawn outside it.
    val radius = width / 2f.rf - fontSize
    // Like the curved row background of TimeText, the arc is as thick as a line of text and
    // sweeps as far as the text, with round caps extending past both ends.
    val backgroundRadius = radius + fontSize * BackgroundCenterOffset.rf
    val sweepDegrees = textWidth / radius * (180f / PI_FLOAT).rf
    val backgroundPaint = RemotePaint {
        color = backgroundColor
        style = RemotePaintingStyle.Stroke
        strokeWidth = fontSize * BackgroundThickness.rf
        strokeCap = RemoteStrokeCap.Round
    }
    drawArc(
        paint = backgroundPaint,
        startAngle = 270f.rf - sweepDegrees / 2f.rf,
        sweepAngle = sweepDegrees,
        useCenter = false,
        topLeft = RemoteOffset(width / 2f.rf - backgroundRadius, height / 2f.rf - backgroundRadius),
        size = RemoteSize(backgroundRadius * 2f.rf, backgroundRadius * 2f.rf),
    )
}

@Suppress("RestrictedApiAndroidX")
private fun RemoteDrawScope.drawTimeText(
    text: RemoteString,
    textColor: RemoteColor,
    fontSize: RemoteFloat,
    fontFamily: RemoteFontFamily?,
    fontVariationSettings: RemoteFontVariation.Settings?,
    fontFeatureSettings: String?,
) {
    val width = width
    val height = height

    val textPaint = RemotePaint {
        textSize = fontSize
        typeface = fontFamily?.toRemoteTypeface() ?: RemoteTypeface.Default
        color = textColor
        this.fontVariationSettings = fontVariationSettings
        this.fontFeatureSettings = fontFeatureSettings
    }

    drawTextOnCircle(
        text,
        width / 2f.rf,
        height / 2f.rf,
        width / 2f.rf - fontSize,
        270f.rf,
        0f.rf,
        textPaint,
    )
}

/** Luminance of the default text color, matching `TimeTextDefaults.timeTextStyle`. */
private const val DefaultColorLuminance = 80f

/** Thickness of the background, relative to the font size: the arc typography line height. */
private const val BackgroundThickness = 1.2f

/**
 * Distance from the text baseline to the middle of the background, relative to the font size: half
 * the difference between the ascent and descent of Roboto, centering the background on the text.
 */
private const val BackgroundCenterOffset = 0.35f

private const val PI_FLOAT = 3.1415927f

/** Contains the default values used by [RemoteTimeText]. */
public object RemoteTimeTextDefaults {
    /** Default font family used by [RemoteTimeText]. */
    public val FontFamily: RemoteFontFamily = RemoteFontFamily.Named("google:Roboto Flex")

    /** Default font feature settings used by [RemoteTimeText]. */
    public val FontFeatureSettings: String? = "tnum"

    /** Default font variation settings used by [RemoteTimeText]. */
    public val FontVariationSettings: RemoteFontVariation.Settings? = null

    /**
     * Default background color used by [RemoteTimeText], so the text stays visible on top of other
     * content.
     */
    public val backgroundColor: RemoteColor
        @Composable
        @RemoteComposable
        get() = RemoteMaterialTheme.colorScheme.background.copy(alpha = 0.85f.rf)

    /** Default text style used by [RemoteTimeText]. */
    public val TimeTextStyle: RemoteTextStyle =
        RemoteTextStyle(
            fontSize = 14.rsp,
            fontFamily = FontFamily,
            fontFeatureSettings = FontFeatureSettings,
            fontVariationSettings = FontVariationSettings,
        )
}
