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
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteFilterQuality
import androidx.compose.remote.creation.compose.state.RemotePaintingStyle
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteHyphens
import androidx.compose.remote.creation.compose.text.RemoteLineBreak
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextDecoration
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/** Converts a Compose [BlendMode] to an Android framework [android.graphics.BlendMode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode {
    return when (this) {
        BlendMode.Clear -> AndroidBlendMode.CLEAR
        BlendMode.Src -> AndroidBlendMode.SRC
        BlendMode.Dst -> AndroidBlendMode.DST
        BlendMode.SrcOver -> AndroidBlendMode.SRC_OVER
        BlendMode.DstOver -> AndroidBlendMode.DST_OVER
        BlendMode.SrcIn -> AndroidBlendMode.SRC_IN
        BlendMode.DstIn -> AndroidBlendMode.DST_IN
        BlendMode.SrcOut -> AndroidBlendMode.SRC_OUT
        BlendMode.DstOut -> AndroidBlendMode.DST_OUT
        BlendMode.SrcAtop -> AndroidBlendMode.SRC_ATOP
        BlendMode.DstAtop -> AndroidBlendMode.DST_ATOP
        BlendMode.Xor -> AndroidBlendMode.XOR
        BlendMode.Plus -> AndroidBlendMode.PLUS
        BlendMode.Modulate -> AndroidBlendMode.MODULATE
        BlendMode.Screen -> AndroidBlendMode.SCREEN
        BlendMode.Overlay -> AndroidBlendMode.OVERLAY
        BlendMode.Darken -> AndroidBlendMode.DARKEN
        BlendMode.Lighten -> AndroidBlendMode.LIGHTEN
        BlendMode.ColorDodge -> AndroidBlendMode.COLOR_DODGE
        BlendMode.ColorBurn -> AndroidBlendMode.COLOR_BURN
        BlendMode.Hardlight -> AndroidBlendMode.HARD_LIGHT
        BlendMode.Softlight -> AndroidBlendMode.SOFT_LIGHT
        BlendMode.Difference -> AndroidBlendMode.DIFFERENCE
        BlendMode.Exclusion -> AndroidBlendMode.EXCLUSION
        BlendMode.Multiply -> AndroidBlendMode.MULTIPLY
        BlendMode.Hue -> AndroidBlendMode.HUE
        BlendMode.Saturation -> AndroidBlendMode.SATURATION
        BlendMode.Color -> AndroidBlendMode.COLOR
        BlendMode.Luminosity -> AndroidBlendMode.LUMINOSITY
        else -> AndroidBlendMode.SRC_OVER
    }
}

/** Converts a [RemoteBlendMode] to an Android framework [AndroidBlendMode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteBlendMode.toAndroidBlendMode(): AndroidBlendMode =
    toComposeBlendMode().toAndroidBlendMode()

/** Converts a Compose [StrokeCap] to an Android framework [AndroidPaint.Cap]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun StrokeCap.toAndroidCap(): AndroidPaint.Cap =
    when (this) {
        StrokeCap.Butt -> AndroidPaint.Cap.BUTT
        StrokeCap.Round -> AndroidPaint.Cap.ROUND
        StrokeCap.Square -> AndroidPaint.Cap.SQUARE
        else -> AndroidPaint.Cap.BUTT
    }

/** Converts a Compose [StrokeJoin] to an Android framework [AndroidPaint.Join]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun StrokeJoin.toAndroidJoin(): AndroidPaint.Join =
    when (this) {
        StrokeJoin.Miter -> AndroidPaint.Join.MITER
        StrokeJoin.Round -> AndroidPaint.Join.ROUND
        StrokeJoin.Bevel -> AndroidPaint.Join.BEVEL
        else -> AndroidPaint.Join.MITER
    }

/** Converts a Compose [PaintingStyle] to an Android framework [AndroidPaint.Style]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun PaintingStyle.toAndroidStyle(): AndroidPaint.Style =
    when (this) {
        PaintingStyle.Fill -> AndroidPaint.Style.FILL
        PaintingStyle.Stroke -> AndroidPaint.Style.STROKE
        else -> AndroidPaint.Style.FILL
    }

internal fun AndroidBlendMode.toComposeBlendMode(): BlendMode {
    return when (this) {
        AndroidBlendMode.CLEAR -> BlendMode.Clear
        AndroidBlendMode.SRC -> BlendMode.Src
        AndroidBlendMode.DST -> BlendMode.Dst
        AndroidBlendMode.SRC_OVER -> BlendMode.SrcOver
        AndroidBlendMode.DST_OVER -> BlendMode.DstOver
        AndroidBlendMode.SRC_IN -> BlendMode.SrcIn
        AndroidBlendMode.DST_IN -> BlendMode.DstIn
        AndroidBlendMode.SRC_OUT -> BlendMode.SrcOut
        AndroidBlendMode.DST_OUT -> BlendMode.DstOut
        AndroidBlendMode.SRC_ATOP -> BlendMode.SrcAtop
        AndroidBlendMode.DST_ATOP -> BlendMode.DstAtop
        AndroidBlendMode.XOR -> BlendMode.Xor
        AndroidBlendMode.PLUS -> BlendMode.Plus
        AndroidBlendMode.MODULATE -> BlendMode.Modulate
        AndroidBlendMode.SCREEN -> BlendMode.Screen
        AndroidBlendMode.OVERLAY -> BlendMode.Overlay
        AndroidBlendMode.DARKEN -> BlendMode.Darken
        AndroidBlendMode.LIGHTEN -> BlendMode.Lighten
        AndroidBlendMode.COLOR_DODGE -> BlendMode.ColorDodge
        AndroidBlendMode.COLOR_BURN -> BlendMode.ColorBurn
        AndroidBlendMode.HARD_LIGHT -> BlendMode.Hardlight
        AndroidBlendMode.SOFT_LIGHT -> BlendMode.Softlight
        AndroidBlendMode.DIFFERENCE -> BlendMode.Difference
        AndroidBlendMode.EXCLUSION -> BlendMode.Exclusion
        AndroidBlendMode.MULTIPLY -> BlendMode.Multiply
        AndroidBlendMode.HUE -> BlendMode.Hue
        AndroidBlendMode.SATURATION -> BlendMode.Saturation
        AndroidBlendMode.COLOR -> BlendMode.Color
        AndroidBlendMode.LUMINOSITY -> BlendMode.Luminosity
    }
}

/** Converts an Android framework [AndroidPaint.Style] to a Compose [PaintingStyle]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun AndroidPaint.Style?.toPaintingStyle(): PaintingStyle =
    when (this) {
        AndroidPaint.Style.FILL -> PaintingStyle.Fill
        AndroidPaint.Style.STROKE -> PaintingStyle.Stroke
        else -> PaintingStyle.Fill
    }

/** Converts an Android framework [AndroidPaint.Cap] to a Compose [StrokeCap]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun AndroidPaint.Cap?.toStrokeCap(): StrokeCap =
    when (this) {
        AndroidPaint.Cap.BUTT -> StrokeCap.Butt
        AndroidPaint.Cap.ROUND -> StrokeCap.Round
        AndroidPaint.Cap.SQUARE -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

/** Converts an Android framework [AndroidPaint.Join] to a Compose [StrokeJoin]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun AndroidPaint.Join?.toStrokeJoin(): StrokeJoin =
    when (this) {
        AndroidPaint.Join.MITER -> StrokeJoin.Miter
        AndroidPaint.Join.ROUND -> StrokeJoin.Round
        AndroidPaint.Join.BEVEL -> StrokeJoin.Bevel
        else -> StrokeJoin.Miter
    }

internal fun TextOverflow.toRemoteTextOverflow(): RemoteTextOverflow =
    when (this) {
        TextOverflow.Clip -> RemoteTextOverflow.Clip
        TextOverflow.Visible -> RemoteTextOverflow.Visible
        TextOverflow.Ellipsis -> RemoteTextOverflow.Ellipsis
        TextOverflow.StartEllipsis -> RemoteTextOverflow.StartEllipsis
        TextOverflow.MiddleEllipsis -> RemoteTextOverflow.MiddleEllipsis
        else -> RemoteTextOverflow.Clip
    }

internal fun TextAlign.toRemoteTextAlign(): RemoteTextAlign =
    when (this) {
        TextAlign.Left,
        TextAlign.Unspecified -> RemoteTextAlign.Left
        TextAlign.Right -> RemoteTextAlign.Right
        TextAlign.Center -> RemoteTextAlign.Center
        TextAlign.Justify -> RemoteTextAlign.Justify
        TextAlign.Start -> RemoteTextAlign.Start
        TextAlign.End -> RemoteTextAlign.End
        else -> RemoteTextAlign.Left
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

internal fun RemoteBlendMode.toComposeBlendMode(): BlendMode =
    when (this) {
        RemoteBlendMode.Clear -> BlendMode.Clear
        RemoteBlendMode.Src -> BlendMode.Src
        RemoteBlendMode.Dst -> BlendMode.Dst
        RemoteBlendMode.SrcOver -> BlendMode.SrcOver
        RemoteBlendMode.DstOver -> BlendMode.DstOver
        RemoteBlendMode.SrcIn -> BlendMode.SrcIn
        RemoteBlendMode.DstIn -> BlendMode.DstIn
        RemoteBlendMode.SrcOut -> BlendMode.SrcOut
        RemoteBlendMode.DstOut -> BlendMode.DstOut
        RemoteBlendMode.SrcAtop -> BlendMode.SrcAtop
        RemoteBlendMode.DstAtop -> BlendMode.DstAtop
        RemoteBlendMode.Xor -> BlendMode.Xor
        RemoteBlendMode.Plus -> BlendMode.Plus
        RemoteBlendMode.Modulate -> BlendMode.Modulate
        RemoteBlendMode.Screen -> BlendMode.Screen
        RemoteBlendMode.Overlay -> BlendMode.Overlay
        RemoteBlendMode.Darken -> BlendMode.Darken
        RemoteBlendMode.Lighten -> BlendMode.Lighten
        RemoteBlendMode.ColorDodge -> BlendMode.ColorDodge
        RemoteBlendMode.ColorBurn -> BlendMode.ColorBurn
        RemoteBlendMode.Hardlight -> BlendMode.Hardlight
        RemoteBlendMode.Softlight -> BlendMode.Softlight
        RemoteBlendMode.Difference -> BlendMode.Difference
        RemoteBlendMode.Exclusion -> BlendMode.Exclusion
        RemoteBlendMode.Multiply -> BlendMode.Multiply
        RemoteBlendMode.Hue -> BlendMode.Hue
        RemoteBlendMode.Saturation -> BlendMode.Saturation
        RemoteBlendMode.Color -> BlendMode.Color
        RemoteBlendMode.Luminosity -> BlendMode.Luminosity
        else -> BlendMode.SrcOver
    }

internal fun StrokeCap.toRemoteStrokeCap(): RemoteStrokeCap =
    when (this) {
        StrokeCap.Butt -> RemoteStrokeCap.Butt
        StrokeCap.Round -> RemoteStrokeCap.Round
        StrokeCap.Square -> RemoteStrokeCap.Square
        else -> RemoteStrokeCap.Butt
    }

internal fun RemoteStrokeCap.toComposeStrokeCap(): StrokeCap =
    when (this) {
        RemoteStrokeCap.Butt -> StrokeCap.Butt
        RemoteStrokeCap.Round -> StrokeCap.Round
        RemoteStrokeCap.Square -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

internal fun StrokeJoin.toRemoteStrokeJoin(): RemoteStrokeJoin =
    when (this) {
        StrokeJoin.Miter -> RemoteStrokeJoin.Miter
        StrokeJoin.Round -> RemoteStrokeJoin.Round
        StrokeJoin.Bevel -> RemoteStrokeJoin.Bevel
        else -> RemoteStrokeJoin.Miter
    }

internal fun RemoteStrokeJoin.toComposeStrokeJoin(): StrokeJoin =
    when (this) {
        RemoteStrokeJoin.Miter -> StrokeJoin.Miter
        RemoteStrokeJoin.Round -> StrokeJoin.Round
        RemoteStrokeJoin.Bevel -> StrokeJoin.Bevel
        else -> StrokeJoin.Miter
    }

internal fun FilterQuality.toRemoteFilterQuality(): RemoteFilterQuality =
    when (this) {
        FilterQuality.None -> RemoteFilterQuality.None
        FilterQuality.Low -> RemoteFilterQuality.Low
        FilterQuality.Medium -> RemoteFilterQuality.Medium
        FilterQuality.High -> RemoteFilterQuality.High
        else -> RemoteFilterQuality.Low
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

internal fun TileMode.toRemoteTileMode(): RemoteTileMode =
    when (this) {
        TileMode.Clamp -> RemoteTileMode.Clamp
        TileMode.Repeated -> RemoteTileMode.Repeated
        TileMode.Mirror -> RemoteTileMode.Mirror
        TileMode.Decal -> RemoteTileMode.Decal
        else -> RemoteTileMode.Clamp
    }

internal fun FontStyle.toRemoteFontStyle(): RemoteFontStyle =
    when (this) {
        FontStyle.Normal -> RemoteFontStyle.Normal
        FontStyle.Italic -> RemoteFontStyle.Italic
        else -> RemoteFontStyle.Normal
    }

internal fun FontWeight.toRemoteFontWeight(): RemoteFontWeight = RemoteFontWeight(weight)

internal fun PathFillType.toRemotePathFillType(): RemotePathFillType =
    when (this) {
        PathFillType.NonZero -> RemotePathFillType.NonZero
        PathFillType.EvenOdd -> RemotePathFillType.EvenOdd
        else -> RemotePathFillType.NonZero
    }

internal fun PaintingStyle.toRemotePaintingStyle(): RemotePaintingStyle =
    when (this) {
        PaintingStyle.Fill -> RemotePaintingStyle.Fill
        PaintingStyle.Stroke -> RemotePaintingStyle.Stroke
        else -> RemotePaintingStyle.Fill
    }

internal fun RemotePaintingStyle.toComposePaintingStyle(): PaintingStyle =
    when (this) {
        RemotePaintingStyle.Fill -> PaintingStyle.Fill
        RemotePaintingStyle.Stroke -> PaintingStyle.Stroke
        else -> PaintingStyle.Fill
    }
