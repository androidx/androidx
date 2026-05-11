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

package androidx.compose.remote.creation.compose.layout

import android.content.res.Configuration
import android.graphics.fonts.FontStyle as AndroidFontStyle
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.state.MutableRemoteString
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteIntReference
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteText(
    text: String,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = RemoteColor(Color.Black),
    fontSize: RemoteTextUnit? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: RemoteTextStyle = RemoteTextStyle.Default,
) {
    RemoteText(
        text = text.rs,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}

/**
 * Remote composable that displays text.
 *
 * Note that density-dependent values like [fontSize], [style#letterSpacing], and [style#lineHeight]
 * are converted to pixels using [RemoteDensity] from the environment where the [RemoteText] is
 * being *created*, not the remote environment where it will be displayed. This means these values
 * are fixed at creation time based on the local density.
 *
 * @param text The text to be displayed.
 * @param modifier The [RemoteModifier] to be applied to this text.
 * @param color [RemoteColor] to apply to the text. If [color] is not specified, and it is not
 *   provided in [style], then [Color.Black] will be used.
 * @param fontSize The size of the font.
 * @param fontStyle The font style to be applied to the text.
 * @param fontWeight The font weight to be applied to the text.
 * @param fontFamily The font family to be applied to the text.
 * @param textAlign The alignment of the text within its container.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text.
 * @param style The [RemoteTextStyle] to be applied to the text.
 * @param fontVariationSettings The font variation settings to be applied to the text.
 */
@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: RemoteTextUnit? = null,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: RemoteTextStyle = RemoteTextStyle.Default,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    val style =
        style.merge(
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            fontFamily = fontFamily,
            fontStyle = fontStyle,
        )
    val fontSizeUnit = style.fontSize ?: 12.rsp
    // TODO handles dynamic letter spacing and line height in CoreText
    val letterSpacing: RemoteFloat =
        if (style.letterSpacing == null || !style.letterSpacing.hasConstantValue) 0f.rf
        else style.letterSpacing.value / fontSizeUnit.value

    val lineHeightMultiply =
        if (style.lineHeight == null || !style.lineHeight.hasConstantValue) 1f.rf
        else // default lineHeight is descent — ascent
         style.lineHeight.value / fontSizeUnit.value
    val unadjustedFontWeight = style.fontWeight?.weight ?: 400

    RemoteText(
        text = text,
        modifier = modifier,
        color = color ?: Color.White.rc,
        fontSize = fontSizeUnit,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontWeight = unadjustedFontWeight.rf,
        fontFamily = style.fontFamily?.name,
        textAlign = style.textAlign ?: TextAlign.Start,
        overflow = overflow,
        maxLines = maxLines,
        textDecoration = style.textDecoration,
        letterSpacing = letterSpacing,
        lineHeightMultiply = lineHeightMultiply,
        fontVariationSettings = fontVariationSettings,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    color: RemoteColor,
    fontSize: RemoteTextUnit,
    minFontSize: Float? = null,
    maxFontSize: Float? = null,
    modifier: RemoteModifier = RemoteModifier,
    fontStyle: FontStyle = FontStyle.Normal,
    fontWeight: RemoteFloat = 400.rf,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: String? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: RemoteFloat = 0f.rf,
    lineHeightAdd: Float? = null,
    lineHeightMultiply: RemoteFloat = 1f.rf,
    textDecoration: TextDecoration? = null,
    fontVariationSettings: FontVariation.Settings? = null,
) {

    val localConfiguration = LocalConfiguration.current

    val fontWeightAdjustment =
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                localConfiguration.fontWeightAdjustment !=
                    Configuration.FONT_WEIGHT_ADJUSTMENT_UNDEFINED
        ) {
            localConfiguration.fontWeightAdjustment
        } else {
            0
        }
    RemoteText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeightAdjustment = fontWeightAdjustment,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        textDecoration = textDecoration,
        letterSpacing = letterSpacing,
        lineHeightMultiply = lineHeightMultiply,
        fontVariationSettings = fontVariationSettings,
    )
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
internal fun RemoteText(
    text: RemoteString,
    color: RemoteColor,
    fontWeightAdjustment: Int,
    fontSize: RemoteTextUnit,
    minFontSize: Float? = null,
    maxFontSize: Float? = null,
    modifier: RemoteModifier = RemoteModifier,
    fontStyle: FontStyle = FontStyle.Normal,
    fontWeight: RemoteFloat = 400.rf,
    textAlign: TextAlign = TextAlign.Start,
    fontFamily: String? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    letterSpacing: RemoteFloat = 0f.rf,
    lineHeightAdd: Float? = null,
    lineHeightMultiply: RemoteFloat = 1f.rf,
    textDecoration: TextDecoration? = null,
    fontVariationSettings: FontVariation.Settings? = null,
) {
    val localDensity = LocalDensity.current
    RemoteComposeNode(
        factory = ::RemoteTextNode,
        update = {
            set(text) { this.text = it }
            set(modifier) { this.modifier = it }
            set(color) { this.color = it }
            set(localDensity) { this.localDensity = it }
            set(fontWeightAdjustment) { this.fontWeightAdjustment = it }
            set(fontWeight) { this.fontWeight = it }
            set(fontStyle) { this.fontStyle = it }
            set(fontSize) { this.fontSize = it }
            set(fontFamily) { this.fontFamily = it }
            set(textAlign) { this.textAlign = it }
            set(overflow) { this.overflow = it }
            set(maxLines) { this.maxLines = it }
            set(minFontSize) { this.minFontSize = it }
            set(maxFontSize) { this.maxFontSize = it }
            set(letterSpacing) { this.letterSpacing = it }
            set(lineHeightAdd) { this.lineHeightAdd = it }
            set(lineHeightMultiply) { this.lineHeightMultiply = it }
            set(textDecoration ?: TextDecoration.None) { this.textDecoration = it }
            set(fontVariationSettings) { this.fontVariationSettings = it }
        },
    )
}

internal class RemoteTextNode : RemoteComposeNode() {
    lateinit var text: RemoteString
    lateinit var color: RemoteColor
    lateinit var fontSize: RemoteTextUnit
    lateinit var localDensity: Density
    var fontWeightAdjustment: Int = 0
    var fontWeight: RemoteFloat = 400f.rf
    var fontStyle: FontStyle = FontStyle.Normal
    var fontFamily: String? = null
    var textAlign: TextAlign = TextAlign.Start
    var overflow: TextOverflow = TextOverflow.Clip
    var maxLines: Int = Int.MAX_VALUE
    var minFontSize: Float? = null
    var maxFontSize: Float? = null
    var letterSpacing: RemoteFloat = 0f.rf
    var lineHeightAdd: Float? = null
    var lineHeightMultiply: RemoteFloat = 1f.rf
    var textDecoration: TextDecoration = TextDecoration.None
    var fontVariationSettings: FontVariation.Settings? = null

    private fun extractFontSettings(
        settings: List<FontVariation.Setting>?
    ): Pair<Array<String>?, FloatArray?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues = FloatArray(size) { settings[it].toVariationValue(null) }

        return Pair(fontAxisNames, fontAxisValues)
    }

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val textIdValue = text.getIdForCreationState(creationState)

        val colorInt = color.constantValueOrNull?.toArgb() ?: Color.Black.toArgb()
        val colorId =
            if (!color.hasConstantValue) {
                color.getIdForCreationState(creationState)
            } else {
                -1
            }

        val (fontAxisNames, fontAxisValues) = extractFontSettings(fontVariationSettings?.settings)

        val effectiveDensity =
            if (localDensity.density != creationState.creationDisplayInfo.density.density) {
                localDensity.density.rf
            } else {
                creationState.remoteDensity.density
            }

        val effectiveFontScale =
            if (localDensity.fontScale != creationState.creationDisplayInfo.density.fontScale) {
                localDensity.fontScale.rf
            } else {
                creationState.remoteDensity.fontScale
            }

        val fontSizePxId =
            fontSize
                .toPx(RemoteDensity(effectiveDensity, effectiveFontScale))
                .getFloatIdForCreationState(creationState)

        val letterSpacingId = letterSpacing.getFloatIdForCreationState(creationState)
        val lineHeightMultiplyId = lineHeightMultiply.getFloatIdForCreationState(creationState)

        val resolvedTextAlign =
            when (textAlign) {
                TextAlign.Start ->
                    if (creationState.layoutDirection == LayoutDirection.Rtl) TextAlign.End
                    else TextAlign.Start
                TextAlign.End ->
                    if (creationState.layoutDirection == LayoutDirection.Rtl) TextAlign.Start
                    else TextAlign.End
                else -> textAlign
            }

        val unadjustedFontWeight = fontWeight.getFloatIdForCreationState(creationState)
        val adjustedFontWeight =
            if (unadjustedFontWeight.isNaN()) {
                // If it's a variable, we currently don't support host-side adjustment.
                unadjustedFontWeight
            } else {
                unadjustedFontWeight
                    .plus(fontWeightAdjustment)
                    .coerceIn(
                        AndroidFontStyle.FONT_WEIGHT_MIN.toFloat(),
                        AndroidFontStyle.FONT_WEIGHT_MAX.toFloat(),
                    )
            }

        creationState.document.startTextComponent(
            with(modifier) { creationState.toRecordingModifier() },
            textIdValue,
            -1,
            colorInt,
            colorId,
            fontSizePxId,
            minFontSize ?: -1f,
            maxFontSize ?: -1f,
            fontStyle.encode(),
            adjustedFontWeight,
            fontFamily,
            resolvedTextAlign.encode(),
            overflow.encode(),
            maxLines,
            letterSpacingId,
            lineHeightAdd ?: 0f,
            lineHeightMultiplyId,
            0, // lineBreakStrategy
            0, // hyphenationFrequency
            0, // justificationMode
            textDecoration.contains(TextDecoration.Underline),
            textDecoration.contains(TextDecoration.LineThrough),
            fontAxisNames,
            fontAxisValues,
            false, // autosize
            0, // flags
        )
        creationState.document.endTextComponent()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteText(
    textId: RemoteIntReference,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor = RemoteColor(Color.Black),
    fontSize: RemoteTextUnit = 12.rsp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: TextAlign = TextAlign.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: RemoteTextStyle = RemoteTextStyle.Default,
) {
    RemoteText(
        text = MutableRemoteString(textId.toInt()),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines,
        style = style,
    )
}
