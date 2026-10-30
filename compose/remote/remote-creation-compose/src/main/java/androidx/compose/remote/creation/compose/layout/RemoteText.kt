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

@file:JvmName("RemoteTextKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.LocalFontWeightAdjustment
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteHyphens
import androidx.compose.remote.creation.compose.text.RemoteLineBreak
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextDecoration
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.text.combineFontSettings
import androidx.compose.runtime.Composable

/**
 * Remote composable that displays text.
 *
 * Note that density-dependent values like [fontSize], [style#letterSpacing], and [style#lineHeight]
 * are converted to pixels using [RemoteDensity] from the environment where the [RemoteText] is
 * being *created*, not the remote environment where it will be displayed. This means these values
 * are fixed at creation time based on the local density.
 *
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextSample
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextStylingSample
 * @sample androidx.compose.remote.creation.compose.samples.RemoteTextFontFamilySample
 * @param text The text to be displayed.
 * @param modifier The [RemoteModifier] to be applied to this text.
 * @param color [RemoteColor] to apply to the text. If [color] is not specified, and it is not
 *   provided in [style], then [androidx.compose.ui.graphics.Color.Black] will be used.
 * @param fontSize The size of the font.
 * @param fontStyle The font style to be applied to the text.
 * @param fontWeight The font weight to be applied to the text.
 * @param fontFamily The font family to be applied to the text.
 * @param textAlign The alignment of the text within its container.
 * @param overflow How visual overflow should be handled.
 * @param maxLines An optional maximum number of lines for the text.
 * @param fontFeatureSettings The advanced typography settings provided by font in CSS format (e.g.
 *   "smcp" or "tnum").
 * @param fontVariationSettings The font variation settings to be applied to the text.
 * @param style The [RemoteTextStyle] to be applied to the text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: RemoteColor? = null,
    fontSize: RemoteTextUnit? = null,
    fontStyle: RemoteFontStyle? = null,
    fontWeight: RemoteFontWeight? = null,
    fontFamily: RemoteFontFamily? = null,
    textAlign: RemoteTextAlign? = null,
    overflow: RemoteTextOverflow,
    @IntRange(from = 1) maxLines: Int = Int.MAX_VALUE,
    fontFeatureSettings: String? = null,
    fontVariationSettings: RemoteFontVariation.Settings? = null,
    style: RemoteTextStyle = RemoteTextStyle.Default,
) {
    val resolvedFontSize = fontSize ?: style.fontSize ?: 12.rsp
    // TODO handles dynamic letter spacing and line height in CoreText
    val letterSpacing: RemoteFloat =
        if (style.letterSpacing == null || !style.letterSpacing.hasConstantValue) 0f.rf
        else style.letterSpacing.value / resolvedFontSize.value

    val lineHeightMultiply =
        if (style.lineHeight == null || !style.lineHeight.hasConstantValue) 1f.rf
        else // default lineHeight is descent — ascent
         style.lineHeight.value / resolvedFontSize.value
    val unadjustedFontWeight = (fontWeight ?: style.remoteFontWeight)?.weight ?: 400f.rf

    RemoteText(
        text = text,
        color = color ?: style.color ?: RemoteColor(BlackArgb),
        fontSize = resolvedFontSize,
        modifier = modifier,
        fontStyle = fontStyle ?: style.remoteFontStyle ?: RemoteFontStyle.Normal,
        fontWeight = unadjustedFontWeight,
        textAlign = textAlign ?: style.remoteTextAlign ?: RemoteTextAlign.Start,
        fontFamily = (fontFamily ?: style.fontFamily)?.name,
        overflow = overflow,
        maxLines = maxLines,
        letterSpacing = letterSpacing,
        lineHeightMultiply = lineHeightMultiply,
        lineBreak = style.remoteLineBreak ?: RemoteLineBreak.Simple,
        hyphens = style.remoteHyphens ?: RemoteHyphens.None,
        textDecoration = style.remoteTextDecoration,
        fontVariationSettings = fontVariationSettings ?: style.remoteFontVariationSettings,
        fontFeatureSettings = fontFeatureSettings ?: style.fontFeatureSettings,
    )
}

@Composable
@RemoteComposable
@Suppress("RemoteCompositionLocalUsage")
internal fun RemoteText(
    text: RemoteString,
    color: RemoteColor,
    fontSize: RemoteTextUnit,
    modifier: RemoteModifier,
    minFontSize: Float? = null,
    maxFontSize: Float? = null,
    fontStyle: RemoteFontStyle,
    fontWeight: RemoteFloat,
    textAlign: RemoteTextAlign,
    fontFamily: String?,
    overflow: RemoteTextOverflow,
    maxLines: Int,
    letterSpacing: RemoteFloat,
    lineHeightAdd: Float? = null,
    lineHeightMultiply: RemoteFloat,
    lineBreak: RemoteLineBreak,
    hyphens: RemoteHyphens,
    textDecoration: RemoteTextDecoration?,
    fontVariationSettings: RemoteFontVariation.Settings?,
    fontFeatureSettings: String?,
) {
    val fontWeightAdjustment = LocalFontWeightAdjustment.current

    RemoteComposeNode(
        factory = ::RemoteTextNode,
        update = {
            set(text) { this.text = it }
            set(modifier) { this.modifier = it }
            set(color) { this.color = it }
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
            set(textDecoration ?: RemoteTextDecoration.None) { this.textDecoration = it }
            set(lineBreak) { this.lineBreakStrategy = lineBreak.value }
            set(hyphens) { this.hyphenationFrequency = hyphens.value }
            set(fontVariationSettings) { this.fontVariationSettings = it }
            set(fontFeatureSettings) { this.fontFeatureSettings = it }
        },
    )
}

internal class RemoteTextNode : RemoteComposeNode() {
    lateinit var text: RemoteString
    lateinit var color: RemoteColor
    lateinit var fontSize: RemoteTextUnit
    var fontWeightAdjustment: Int = 0
    var fontWeight: RemoteFloat = 400f.rf
    var fontStyle: RemoteFontStyle = RemoteFontStyle.Normal
    var fontFamily: String? = null
    var textAlign: RemoteTextAlign = RemoteTextAlign.Start
    var overflow: RemoteTextOverflow = RemoteTextOverflow.Clip
    var maxLines: Int = Int.MAX_VALUE
    var minFontSize: Float? = null
    var maxFontSize: Float? = null
    var letterSpacing: RemoteFloat = 0f.rf
    var lineHeightAdd: Float? = null
    var lineHeightMultiply: RemoteFloat = 1f.rf
    var textDecoration: RemoteTextDecoration = RemoteTextDecoration.None
    var lineBreakStrategy: Int = 0
    var hyphenationFrequency: Int = 0
    var fontVariationSettings: RemoteFontVariation.Settings? = null
    var fontFeatureSettings: String? = null

    private fun extractFontSettings(
        settings: List<RemoteFontVariation.Setting>?,
        creationState: RemoteComposeCreationState,
    ): Pair<Array<String>?, FloatArray?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues =
            FloatArray(size) { settings[it].value.getFloatIdForCreationState(creationState) }

        return Pair(fontAxisNames, fontAxisValues)
    }

    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        val textIdValue = text.getIdForCreationState(creationState)

        val colorInt = color.constantValueOrNull ?: BlackArgb
        val colorId =
            if (!color.hasConstantValue) {
                color.getIdForCreationState(creationState)
            } else {
                -1
            }

        val combinedSettings = combineFontSettings(fontFeatureSettings, fontVariationSettings)
        val (fontAxisNames, fontAxisValues) =
            extractFontSettings(combinedSettings?.settings, creationState)

        val fontSizePxId = fontSize.toPx(remoteDensity).getFloatIdForCreationState(creationState)

        val letterSpacingId = letterSpacing.getFloatIdForCreationState(creationState)
        val lineHeightMultiplyId = lineHeightMultiply.getFloatIdForCreationState(creationState)

        val resolvedTextAlign =
            when (textAlign) {
                RemoteTextAlign.Start ->
                    if (creationState.remoteLayoutDirection == RemoteLayoutDirection.Rtl) {
                        RemoteTextAlign.End
                    } else {
                        RemoteTextAlign.Start
                    }
                RemoteTextAlign.End ->
                    if (creationState.remoteLayoutDirection == RemoteLayoutDirection.Rtl) {
                        RemoteTextAlign.Start
                    } else {
                        RemoteTextAlign.End
                    }
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
                    .coerceIn(FONT_WEIGHT_MIN, FONT_WEIGHT_MAX)
            }

        val scope = overriddenScope(creationState)
        creationState.document.startTextComponent(
            scope.toRecordingModifier(modifier),
            textIdValue,
            -1,
            colorInt,
            colorId,
            fontSizePxId,
            minFontSize ?: -1f,
            maxFontSize ?: -1f,
            fontStyle.value,
            adjustedFontWeight,
            fontFamily,
            resolvedTextAlign.value,
            overflow.value,
            maxLines,
            letterSpacingId,
            lineHeightAdd ?: 0f,
            lineHeightMultiplyId,
            lineBreakStrategy,
            hyphenationFrequency,
            0, // justificationMode
            textDecoration.contains(RemoteTextDecoration.Underline),
            textDecoration.contains(RemoteTextDecoration.LineThrough),
            fontAxisNames,
            fontAxisValues,
            false, // autosize
            0, // flags
        )
        creationState.document.endTextComponent()
    }
}

/**
 * The minimum allowable font weight value (1.0f).
 *
 * Conforms to [androidx.compose.ui.text.font.FontWeight] which enforces `weight in 1..1000`.
 * Replaces [android.graphics.fonts.FontStyle.FONT_WEIGHT_MIN] to avoid coupling layout nodes to
 * Android framework classes.
 */
private const val FONT_WEIGHT_MIN: Float = 1f

/**
 * The maximum allowable font weight value (1000.0f).
 *
 * Conforms to [androidx.compose.ui.text.font.FontWeight] which enforces `weight in 1..1000`.
 * Replaces [android.graphics.fonts.FontStyle.FONT_WEIGHT_MAX] to avoid coupling layout nodes to
 * Android framework classes.
 */
private const val FONT_WEIGHT_MAX: Float = 1000f

internal const val BlackArgb: Int = 0xFF000000.toInt()
