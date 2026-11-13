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

import android.graphics.BlendMode as AndroidBlendMode
import android.graphics.Paint as AndroidPaint
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteHyphens
import androidx.compose.remote.creation.compose.text.RemoteLineBreak
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextDecoration
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

/** Converts a [RemoteBlendMode] to an Android framework [AndroidBlendMode]. */
internal fun RemoteBlendMode.toAndroidBlendMode(): AndroidBlendMode =
    when (this) {
        RemoteBlendMode.Clear -> AndroidBlendMode.CLEAR
        RemoteBlendMode.Src -> AndroidBlendMode.SRC
        RemoteBlendMode.Dst -> AndroidBlendMode.DST
        RemoteBlendMode.SrcOver -> AndroidBlendMode.SRC_OVER
        RemoteBlendMode.DstOver -> AndroidBlendMode.DST_OVER
        RemoteBlendMode.SrcIn -> AndroidBlendMode.SRC_IN
        RemoteBlendMode.DstIn -> AndroidBlendMode.DST_IN
        RemoteBlendMode.SrcOut -> AndroidBlendMode.SRC_OUT
        RemoteBlendMode.DstOut -> AndroidBlendMode.DST_OUT
        RemoteBlendMode.SrcAtop -> AndroidBlendMode.SRC_ATOP
        RemoteBlendMode.DstAtop -> AndroidBlendMode.DST_ATOP
        RemoteBlendMode.Xor -> AndroidBlendMode.XOR
        RemoteBlendMode.Plus -> AndroidBlendMode.PLUS
        RemoteBlendMode.Modulate -> AndroidBlendMode.MODULATE
        RemoteBlendMode.Screen -> AndroidBlendMode.SCREEN
        RemoteBlendMode.Overlay -> AndroidBlendMode.OVERLAY
        RemoteBlendMode.Darken -> AndroidBlendMode.DARKEN
        RemoteBlendMode.Lighten -> AndroidBlendMode.LIGHTEN
        RemoteBlendMode.ColorDodge -> AndroidBlendMode.COLOR_DODGE
        RemoteBlendMode.ColorBurn -> AndroidBlendMode.COLOR_BURN
        RemoteBlendMode.Hardlight -> AndroidBlendMode.HARD_LIGHT
        RemoteBlendMode.Softlight -> AndroidBlendMode.SOFT_LIGHT
        RemoteBlendMode.Difference -> AndroidBlendMode.DIFFERENCE
        RemoteBlendMode.Exclusion -> AndroidBlendMode.EXCLUSION
        RemoteBlendMode.Multiply -> AndroidBlendMode.MULTIPLY
        RemoteBlendMode.Hue -> AndroidBlendMode.HUE
        RemoteBlendMode.Saturation -> AndroidBlendMode.SATURATION
        RemoteBlendMode.Color -> AndroidBlendMode.COLOR
        RemoteBlendMode.Luminosity -> AndroidBlendMode.LUMINOSITY
        else -> AndroidBlendMode.SRC_OVER
    }

/** Converts an Android framework [AndroidBlendMode] to a [RemoteBlendMode]. */
internal fun AndroidBlendMode.toRemoteBlendMode(): RemoteBlendMode =
    when (this) {
        AndroidBlendMode.CLEAR -> RemoteBlendMode.Clear
        AndroidBlendMode.SRC -> RemoteBlendMode.Src
        AndroidBlendMode.DST -> RemoteBlendMode.Dst
        AndroidBlendMode.SRC_OVER -> RemoteBlendMode.SrcOver
        AndroidBlendMode.DST_OVER -> RemoteBlendMode.DstOver
        AndroidBlendMode.SRC_IN -> RemoteBlendMode.SrcIn
        AndroidBlendMode.DST_IN -> RemoteBlendMode.DstIn
        AndroidBlendMode.SRC_OUT -> RemoteBlendMode.SrcOut
        AndroidBlendMode.DST_OUT -> RemoteBlendMode.DstOut
        AndroidBlendMode.SRC_ATOP -> RemoteBlendMode.SrcAtop
        AndroidBlendMode.DST_ATOP -> RemoteBlendMode.DstAtop
        AndroidBlendMode.XOR -> RemoteBlendMode.Xor
        AndroidBlendMode.PLUS -> RemoteBlendMode.Plus
        AndroidBlendMode.MODULATE -> RemoteBlendMode.Modulate
        AndroidBlendMode.SCREEN -> RemoteBlendMode.Screen
        AndroidBlendMode.OVERLAY -> RemoteBlendMode.Overlay
        AndroidBlendMode.DARKEN -> RemoteBlendMode.Darken
        AndroidBlendMode.LIGHTEN -> RemoteBlendMode.Lighten
        AndroidBlendMode.COLOR_DODGE -> RemoteBlendMode.ColorDodge
        AndroidBlendMode.COLOR_BURN -> RemoteBlendMode.ColorBurn
        AndroidBlendMode.HARD_LIGHT -> RemoteBlendMode.Hardlight
        AndroidBlendMode.SOFT_LIGHT -> RemoteBlendMode.Softlight
        AndroidBlendMode.DIFFERENCE -> RemoteBlendMode.Difference
        AndroidBlendMode.EXCLUSION -> RemoteBlendMode.Exclusion
        AndroidBlendMode.MULTIPLY -> RemoteBlendMode.Multiply
        AndroidBlendMode.HUE -> RemoteBlendMode.Hue
        AndroidBlendMode.SATURATION -> RemoteBlendMode.Saturation
        AndroidBlendMode.COLOR -> RemoteBlendMode.Color
        AndroidBlendMode.LUMINOSITY -> RemoteBlendMode.Luminosity
    }

/** Converts an Android framework [AndroidPaint.Style] to a [RemotePaintingStyle]. */
internal fun AndroidPaint.Style?.toRemotePaintingStyle(): RemotePaintingStyle =
    when (this) {
        AndroidPaint.Style.FILL -> RemotePaintingStyle.Fill
        AndroidPaint.Style.STROKE -> RemotePaintingStyle.Stroke
        else -> RemotePaintingStyle.Fill
    }

/** Converts an Android framework [AndroidPaint.Cap] to a [RemoteStrokeCap]. */
internal fun AndroidPaint.Cap?.toRemoteStrokeCap(): RemoteStrokeCap =
    when (this) {
        AndroidPaint.Cap.BUTT -> RemoteStrokeCap.Butt
        AndroidPaint.Cap.ROUND -> RemoteStrokeCap.Round
        AndroidPaint.Cap.SQUARE -> RemoteStrokeCap.Square
        else -> RemoteStrokeCap.Butt
    }

/** Converts an Android framework [AndroidPaint.Join] to a [RemoteStrokeJoin]. */
internal fun AndroidPaint.Join?.toRemoteStrokeJoin(): RemoteStrokeJoin =
    when (this) {
        AndroidPaint.Join.MITER -> RemoteStrokeJoin.Miter
        AndroidPaint.Join.ROUND -> RemoteStrokeJoin.Round
        AndroidPaint.Join.BEVEL -> RemoteStrokeJoin.Bevel
        else -> RemoteStrokeJoin.Miter
    }

internal fun TextAlign.toRemoteTextAlign(): RemoteTextAlign =
    when (this) {
        TextAlign.Left -> RemoteTextAlign.Left
        TextAlign.Right -> RemoteTextAlign.Right
        TextAlign.Center -> RemoteTextAlign.Center
        TextAlign.Justify -> RemoteTextAlign.Justify
        TextAlign.Start -> RemoteTextAlign.Start
        TextAlign.End -> RemoteTextAlign.End
        TextAlign.Unspecified -> RemoteTextAlign.Unspecified
        else -> RemoteTextAlign.Unspecified
    }

internal fun BlendMode.toRemoteBlendMode(): RemoteBlendMode =
    when (this) {
        BlendMode.Clear -> RemoteBlendMode.Clear
        BlendMode.Src -> RemoteBlendMode.Src
        BlendMode.Dst -> RemoteBlendMode.Dst
        BlendMode.SrcOver -> RemoteBlendMode.SrcOver
        BlendMode.DstOver -> RemoteBlendMode.DstOver
        BlendMode.SrcIn -> RemoteBlendMode.SrcIn
        BlendMode.DstIn -> RemoteBlendMode.DstIn
        BlendMode.SrcOut -> RemoteBlendMode.SrcOut
        BlendMode.DstOut -> RemoteBlendMode.DstOut
        BlendMode.SrcAtop -> RemoteBlendMode.SrcAtop
        BlendMode.DstAtop -> RemoteBlendMode.DstAtop
        BlendMode.Xor -> RemoteBlendMode.Xor
        BlendMode.Plus -> RemoteBlendMode.Plus
        BlendMode.Modulate -> RemoteBlendMode.Modulate
        BlendMode.Screen -> RemoteBlendMode.Screen
        BlendMode.Overlay -> RemoteBlendMode.Overlay
        BlendMode.Darken -> RemoteBlendMode.Darken
        BlendMode.Lighten -> RemoteBlendMode.Lighten
        BlendMode.ColorDodge -> RemoteBlendMode.ColorDodge
        BlendMode.ColorBurn -> RemoteBlendMode.ColorBurn
        BlendMode.Hardlight -> RemoteBlendMode.Hardlight
        BlendMode.Softlight -> RemoteBlendMode.Softlight
        BlendMode.Difference -> RemoteBlendMode.Difference
        BlendMode.Exclusion -> RemoteBlendMode.Exclusion
        BlendMode.Multiply -> RemoteBlendMode.Multiply
        BlendMode.Hue -> RemoteBlendMode.Hue
        BlendMode.Saturation -> RemoteBlendMode.Saturation
        BlendMode.Color -> RemoteBlendMode.Color
        BlendMode.Luminosity -> RemoteBlendMode.Luminosity
        else -> RemoteBlendMode.SrcOver
    }

internal fun StrokeCap.toRemoteStrokeCap(): RemoteStrokeCap =
    when (this) {
        StrokeCap.Butt -> RemoteStrokeCap.Butt
        StrokeCap.Round -> RemoteStrokeCap.Round
        StrokeCap.Square -> RemoteStrokeCap.Square
        else -> RemoteStrokeCap.Butt
    }

internal fun StrokeJoin.toRemoteStrokeJoin(): RemoteStrokeJoin =
    when (this) {
        StrokeJoin.Miter -> RemoteStrokeJoin.Miter
        StrokeJoin.Round -> RemoteStrokeJoin.Round
        StrokeJoin.Bevel -> RemoteStrokeJoin.Bevel
        else -> RemoteStrokeJoin.Miter
    }

internal fun LineBreak.toRemoteLineBreak(): RemoteLineBreak =
    when (this.strategy) {
        LineBreak.Strategy.Simple -> RemoteLineBreak.Simple
        LineBreak.Strategy.HighQuality -> RemoteLineBreak.Paragraph
        LineBreak.Strategy.Balanced -> RemoteLineBreak.Heading
        else -> RemoteLineBreak.Simple
    }

internal fun Hyphens.toRemoteHyphens(): RemoteHyphens =
    when (this) {
        Hyphens.Auto -> RemoteHyphens.Auto
        Hyphens.None,
        Hyphens.Unspecified -> RemoteHyphens.None
        else -> RemoteHyphens.None
    }

internal fun TextDecoration.toRemoteTextDecoration(): RemoteTextDecoration {
    val underline =
        if (contains(TextDecoration.Underline)) {
            RemoteTextDecoration.Underline
        } else {
            RemoteTextDecoration.None
        }
    val lineThrough =
        if (contains(TextDecoration.LineThrough)) {
            RemoteTextDecoration.LineThrough
        } else {
            RemoteTextDecoration.None
        }
    return underline + lineThrough
}

internal fun FontStyle.toRemoteFontStyle(): RemoteFontStyle =
    when (this) {
        FontStyle.Normal -> RemoteFontStyle.Normal
        FontStyle.Italic -> RemoteFontStyle.Italic
        else -> RemoteFontStyle.Normal
    }

internal fun FontWeight.toRemoteFontWeight(): RemoteFontWeight = RemoteFontWeight(weight)

internal fun FontFamily.toRemoteFontFamily(): RemoteFontFamily? =
    when (this) {
        FontFamily.Default -> RemoteFontFamily.Default
        FontFamily.SansSerif -> RemoteFontFamily.SansSerif
        FontFamily.Serif -> RemoteFontFamily.Serif
        FontFamily.Monospace -> RemoteFontFamily.Monospace
        FontFamily.Cursive -> RemoteFontFamily.Cursive
        is GenericFontFamily -> RemoteFontFamily.Named(name)
        else -> null
    }

internal fun PathFillType.toRemotePathFillType(): RemotePathFillType =
    when (this) {
        PathFillType.NonZero -> RemotePathFillType.NonZero
        PathFillType.EvenOdd -> RemotePathFillType.EvenOdd
        else -> RemotePathFillType.NonZero
    }
