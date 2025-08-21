/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.frontend.layout

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.capture.NoRemoteCompose
import androidx.compose.remote.frontend.capture.RecordingCanvas
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.remote.frontend.modifier.semantics
import androidx.compose.remote.frontend.modifier.toComposeUi
import androidx.compose.remote.frontend.modifier.toComposeUiLayout
import androidx.compose.remote.frontend.state.RemoteIntReference
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.remote.frontend.state.rememberRemoteString
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
@RemoteComposable
fun RemoteText(
    text: String,
    modifier: RemoteModifier = RemoteModifier,
    color: Color = Color.Black,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    val remoteText = rememberRemoteString { text }
    RemoteText(
        remoteText,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        textAlign,
        overflow,
        maxLines,
        style,
    )
}

@Composable
@RemoteComposable
fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: Color = Color.Black,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
) {
    if (style != LocalTextStyle.current) {
        RemoteText(
            text,
            modifier,
            style.color,
            style.fontSize,
            style.fontStyle,
            style.fontWeight,
            style.fontFamily,
            style.textAlign,
            overflow,
            maxLines,
        )
    } else {
        RemoteText(
            text,
            modifier,
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            textAlign,
            overflow,
            maxLines,
        )
    }
}

/** Utility modifier to record the layout information */
class RemoteComposeTextComponentModifier(
    var modifier: RecordingModifier,
    var id: RemoteIntReference,
    var color: Color,
    var fontSize: Float,
    var fontStyle: Int,
    var fontWeight: Float,
    var fontFamily: String?,
    var textAlign: Int,
    var overflow: Int,
    var maxLines: Int,
) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawIntoCanvas {
            if (it.nativeCanvas is RecordingCanvas) {
                (it.nativeCanvas as RecordingCanvas).let {
                    it.document.startTextComponent(
                        modifier,
                        id.toInt(),
                        color.toArgb(),
                        fontSize,
                        fontStyle,
                        fontWeight,
                        fontFamily,
                        textAlign,
                        overflow,
                        maxLines,
                    )
                    drawContent()
                    it.document.endTextComponent()
                }
            }
        }
    }
}

// TODO: b/373614081 - Add support for textAlign, overflow, and maxLines.
@Composable
@RemoteComposable
fun RemoteText(
    text: RemoteString,
    modifier: RemoteModifier = RemoteModifier,
    color: Color = Color.Black,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    val rFontSize =
        with(LocalDensity.current) {
            if (fontSize == TextUnit.Unspecified) 12.sp.toPx() else fontSize.toPx()
        }
    val rFontStyle =
        when (fontStyle) {
            FontStyle.Normal -> 0
            FontStyle.Italic -> 1
            else -> -1
        }
    val rFontWeight = fontWeight?.weight?.toFloat() ?: 400f
    val rFontFamily =
        when (fontFamily) {
            FontFamily.Default -> "default"
            FontFamily.SansSerif -> "sans-serif"
            FontFamily.Serif -> "serif"
            FontFamily.Monospace -> "monospace"
            FontFamily.Cursive -> "cursive"
            else -> {
                if (fontFamily != null && (fontFamily is GenericFontFamily)) {
                    fontFamily.name
                }
                null
            }
        }
    val rTextAlign =
        when (textAlign) {
            TextAlign.Left -> TextLayout.TEXT_ALIGN_LEFT
            TextAlign.Right -> TextLayout.TEXT_ALIGN_RIGHT
            TextAlign.Center -> TextLayout.TEXT_ALIGN_CENTER
            TextAlign.Justify -> TextLayout.TEXT_ALIGN_JUSTIFY
            TextAlign.Start -> TextLayout.TEXT_ALIGN_START
            TextAlign.End -> TextLayout.TEXT_ALIGN_END
            TextAlign.Unspecified -> Int.MIN_VALUE
            else -> -1
        }
    val rOverflow =
        when (overflow) {
            TextOverflow.Clip -> TextLayout.OVERFLOW_CLIP
            TextOverflow.Visible -> TextLayout.OVERFLOW_VISIBLE
            TextOverflow.Ellipsis -> TextLayout.OVERFLOW_ELLIPSIS
            TextOverflow.StartEllipsis -> TextLayout.OVERFLOW_START_ELLIPSIS
            TextOverflow.MiddleEllipsis -> TextLayout.OVERFLOW_MIDDLE_ELLIPSIS
            else -> -1
        }
    if (captureMode is NoRemoteCompose) {
        Text(
            text = text.value,
            modifier = modifier.toComposeUi(),
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            RemoteComposeTextComponentModifier(
                    modifier.toRemoteCompose(),
                    RemoteIntReference(text.getIdForCreationState(captureMode)),
                    color,
                    rFontSize,
                    rFontStyle,
                    rFontWeight,
                    rFontFamily,
                    rTextAlign,
                    rOverflow,
                    maxLines,
                )
                .then(modifier.semantics { this.text = text }.toComposeUiLayout())
        )
    }
}

@Composable
@RemoteComposable
fun RemoteText(
    textId: RemoteIntReference,
    modifier: RemoteModifier = RemoteModifier,
    color: Color = Color.Black,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
) {
    val captureMode = LocalRemoteComposeCreationState.current
    val rFontSize =
        with(LocalDensity.current) {
            if (fontSize == TextUnit.Unspecified) 12.sp.toPx() else fontSize.toPx()
        }
    val rFontStyle =
        when (fontStyle) {
            FontStyle.Normal -> 0
            FontStyle.Italic -> 1
            else -> -1
        }
    val rFontWeight = fontWeight?.weight?.toFloat() ?: 400f
    val rFontFamily =
        when (fontFamily) {
            FontFamily.Default -> "default"
            FontFamily.SansSerif -> "sans-serif"
            FontFamily.Serif -> "serif"
            FontFamily.Monospace -> "monospace"
            FontFamily.Cursive -> "cursive"
            else -> {
                if (fontFamily != null && (fontFamily is GenericFontFamily)) {
                    fontFamily.name
                }
                null
            }
        }
    val rTextAlign =
        when (textAlign) {
            TextAlign.Left -> TextLayout.TEXT_ALIGN_LEFT
            TextAlign.Right -> TextLayout.TEXT_ALIGN_RIGHT
            TextAlign.Center -> TextLayout.TEXT_ALIGN_CENTER
            TextAlign.Justify -> TextLayout.TEXT_ALIGN_JUSTIFY
            TextAlign.Start -> TextLayout.TEXT_ALIGN_START
            TextAlign.End -> TextLayout.TEXT_ALIGN_END
            TextAlign.Unspecified -> Int.MIN_VALUE
            else -> -1
        }
    val rOverflow =
        when (overflow) {
            TextOverflow.Clip -> TextLayout.OVERFLOW_CLIP
            TextOverflow.Visible -> TextLayout.OVERFLOW_VISIBLE
            TextOverflow.Ellipsis -> TextLayout.OVERFLOW_ELLIPSIS
            TextOverflow.StartEllipsis -> TextLayout.OVERFLOW_START_ELLIPSIS
            TextOverflow.MiddleEllipsis -> TextLayout.OVERFLOW_MIDDLE_ELLIPSIS
            else -> -1
        }
    if (captureMode is NoRemoteCompose) {
        Text(
            text = "XX",
            modifier = modifier.toComposeUi(),
            color,
            fontSize,
            fontStyle,
            fontWeight,
            fontFamily,
            textAlign = textAlign,
            overflow = overflow,
            maxLines = maxLines,
        )
    } else {
        androidx.compose.foundation.layout.Box(
            RemoteComposeTextComponentModifier(
                    modifier.toRemoteCompose(),
                    textId,
                    color,
                    rFontSize,
                    rFontStyle,
                    rFontWeight,
                    rFontFamily,
                    rTextAlign,
                    rOverflow,
                    maxLines,
                )
                .then(modifier.toComposeUiLayout())
        )
    }
}
