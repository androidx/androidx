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

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.toComposeUiLayout
import androidx.compose.remote.creation.compose.modifier.toRecordingModifier
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
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.creation.compose.v2.RemoteComposeApplierV2
import androidx.compose.remote.creation.compose.v2.RemoteTextV2
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

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
    fontFamily: FontFamily? = null,
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
    fontFamily: FontFamily? = null,
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

    val fontSize = style.fontSize ?: 12.rsp
    val fontSizePx = fontSize.toPx()

    // TODO handles dynamic letter spacing and line height in CoreText
    val letterSpacing: RemoteFloat =
        if (style.letterSpacing == null || !style.letterSpacing.hasConstantValue) 0f.rf
        else style.letterSpacing.value / fontSize.value

    val lineHeightMultiply =
        if (style.lineHeight == null || !style.lineHeight.hasConstantValue) 1f.rf
        else // default lineHeight is descent — ascent
         style.lineHeight.value / fontSize.value

    RemoteText(
        text = text,
        modifier = modifier,
        color = color ?: Color.White.rc,
        fontSize = fontSizePx,
        fontStyle = style.fontStyle ?: FontStyle.Normal,
        fontWeight = style.fontWeight?.weight?.rf ?: 400.rf,
        fontFamily = style.fontFamily.encode(),
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
    fontSize: RemoteFloat,
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
    val captureMode = LocalRemoteComposeCreationState.current

    if (currentComposer.applier is RemoteComposeApplierV2) {
        RemoteTextV2(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontFamily = fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
            minFontSize = minFontSize,
            maxFontSize = maxFontSize,
            letterSpacing = letterSpacing,
            lineHeightAdd = lineHeightAdd,
            lineHeightMultiply = lineHeightMultiply,
            textDecoration = textDecoration ?: TextDecoration.None,
            fontVariationSettings = fontVariationSettings,
        )
        return
    }

    val useCoreTextComponent =
        LocalRemoteComposeCreationState.current.profile.supportedOperations.contains(
            Operations.CORE_TEXT
        )
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
    if (useCoreTextComponent) {
        androidx.compose.foundation.layout.Box(
            RemoteComposeCoreTextComponentModifier(
                    modifier = captureMode.toRecordingModifier(modifier),
                    id = text,
                    color = color,
                    fontSize = fontSize,
                    minFontSize = minFontSize,
                    maxFontSize = maxFontSize,
                    fontStyle = fontStyle.encode(),
                    fontWeight = fontWeight,
                    fontFamily = fontFamily,
                    textAlign = textAlign.encode(),
                    overflow = overflow.encode(),
                    maxLines = maxLines,
                    textDecoration = textDecoration ?: TextDecoration.None,
                    letterSpacing = letterSpacing,
                    lineHeightAdd = lineHeightAdd,
                    lineHeightMultiply = lineHeightMultiply,
                    fontVariationSettings = fontVariationSettings,
                )
                .then(modifier.toComposeUiLayout())
        )
    } else {
        androidx.compose.foundation.layout.Box(
            RemoteComposeTextComponentModifier(
                    modifier = captureMode.toRecordingModifier(modifier),
                    id = RemoteIntReference(text.getIdForCreationState(captureMode)),
                    color =
                        color.constantValueOrNull?.toArgb()
                            ?: color.getIdForCreationState(captureMode),
                    isColorConstant = color.hasConstantValue,
                    fontSize = with(LocalRemoteComposeCreationState.current) { fontSize.floatId },
                    fontStyle = fontStyle.encode(),
                    fontWeight = fontWeight.constantValueOrNull ?: 400f,
                    fontFamily = fontFamily,
                    textAlign = textAlign.encode(),
                    overflow = overflow.encode(),
                    maxLines = maxLines,
                )
                .then(modifier.toComposeUiLayout())
        )
    }
}

/** Utility modifier to record the layout information */
internal class RemoteComposeTextComponentModifier(
    public var modifier: RecordingModifier,
    public var id: RemoteIntReference,
    public var color: Int,
    public val isColorConstant: Boolean,
    public var fontSize: Float,
    public var fontStyle: Int,
    public var fontWeight: Float,
    public var fontFamily: String?,
    public var textAlign: Int,
    public var overflow: Int,
    public var maxLines: Int,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoRemoteCanvas { canvas ->
            val flags =
                if (isColorConstant) {
                    0
                } else {
                    TextLayout.FLAG_IS_DYNAMIC_COLOR.toShort()
                }

            canvas.document.startTextComponent(
                modifier,
                id.toInt(),
                color,
                fontSize,
                fontStyle,
                fontWeight,
                fontFamily,
                flags,
                textAlign.toShort(),
                overflow,
                maxLines,
            )

            this@draw.drawContent()
            canvas.document.endTextComponent()
        }
    }
}

/** Utility modifier to record the layout information */
internal class RemoteComposeCoreTextComponentModifier(
    public val modifier: RecordingModifier,
    public val id: RemoteString,
    public val color: RemoteColor,
    public val fontSize: RemoteFloat,
    public val minFontSize: Float? = null,
    public val maxFontSize: Float? = null,
    public val fontStyle: Int,
    public val fontWeight: RemoteFloat,
    public val fontFamily: String?,
    public val textAlign: Int,
    public val overflow: Int,
    public val maxLines: Int,
    public val textDecoration: TextDecoration,
    public val letterSpacing: RemoteFloat,
    public val lineHeightAdd: Float? = null,
    public val lineHeightMultiply: RemoteFloat,
    public val fontVariationSettings: FontVariation.Settings?,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        val settings = fontVariationSettings?.settings
        drawIntoRemoteCanvas { canvas ->
            val (fontAxisNames, fontAxisValues) = extractFontSettings(settings)
            canvas.document.startTextComponent(
                modifier,
                id.getIdForCreationState(canvas.creationState),
                color.constantValueOrNull?.toArgb() ?: Color.Black.toArgb(),
                if (color.hasConstantValue) -1
                else color.getIdForCreationState(canvas.creationState),
                fontSize.getFloatIdForCreationState(canvas.creationState),
                minFontSize ?: -1f,
                maxFontSize ?: -1f,
                fontStyle,
                fontWeight.getFloatIdForCreationState(canvas.creationState),
                fontFamily,
                textAlign,
                overflow,
                maxLines,
                letterSpacing.getFloatIdForCreationState(canvas.creationState),
                lineHeightAdd ?: 0f,
                lineHeightMultiply.getFloatIdForCreationState(canvas.creationState),
                0,
                0,
                0,
                textDecoration.contains(TextDecoration.Underline),
                textDecoration.contains(TextDecoration.LineThrough),
                fontAxisNames,
                fontAxisValues,
                false,
                0,
            )

            this@draw.drawContent()
            canvas.document.endTextComponent()
        }
    }

    private fun extractFontSettings(
        settings: List<FontVariation.Setting>?
    ): Pair<Array<String>?, FloatArray?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues = FloatArray(size) { settings[it].toVariationValue(null) }

        return Pair(fontAxisNames, fontAxisValues)
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
    fontFamily: FontFamily? = null,
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
