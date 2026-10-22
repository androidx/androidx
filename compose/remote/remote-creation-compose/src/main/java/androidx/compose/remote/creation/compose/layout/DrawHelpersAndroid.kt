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
import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemotePathFillType
import androidx.compose.remote.creation.compose.state.RemoteStrokeCap
import androidx.compose.remote.creation.compose.state.RemoteStrokeJoin
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
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

/** Converts [ContentScale] to [androidx.compose.remote.core.operations.utilities.ImageScaling]. */
internal fun ContentScale.toImageScalingInt(): Int = toRemoteContentScale().value

internal fun TextOverflow.encode(): Int =
    when (this) {
        TextOverflow.Clip -> TextLayout.OVERFLOW_CLIP
        TextOverflow.Visible -> TextLayout.OVERFLOW_VISIBLE
        TextOverflow.Ellipsis -> TextLayout.OVERFLOW_ELLIPSIS
        TextOverflow.StartEllipsis -> TextLayout.OVERFLOW_START_ELLIPSIS
        TextOverflow.MiddleEllipsis -> TextLayout.OVERFLOW_MIDDLE_ELLIPSIS
        else -> -1
    }

internal fun TextAlign.encode(): Int =
    when (this) {
        TextAlign.Left -> TextLayout.TEXT_ALIGN_LEFT
        TextAlign.Right -> TextLayout.TEXT_ALIGN_RIGHT
        TextAlign.Center -> TextLayout.TEXT_ALIGN_CENTER
        TextAlign.Justify -> TextLayout.TEXT_ALIGN_JUSTIFY
        TextAlign.Start -> TextLayout.TEXT_ALIGN_START
        TextAlign.End -> TextLayout.TEXT_ALIGN_END
        TextAlign.Unspecified -> TextLayout.TEXT_ALIGN_LEFT
        else -> -1
    }

internal fun FontStyle.encode(): Int =
    when (this) {
        FontStyle.Normal -> 0
        FontStyle.Italic -> 1
        else -> -1
    }

internal fun BlendMode.toInt(): Int = toRemoteBlendMode().value

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

internal fun StrokeJoin.toRemoteStrokeJoin(): RemoteStrokeJoin =
    when (this) {
        StrokeJoin.Miter -> RemoteStrokeJoin.Miter
        StrokeJoin.Round -> RemoteStrokeJoin.Round
        StrokeJoin.Bevel -> RemoteStrokeJoin.Bevel
        else -> RemoteStrokeJoin.Miter
    }

internal fun PaintingStyle.toInt(): Int =
    when (this) {
        PaintingStyle.Fill -> PaintBundle.STYLE_FILL
        PaintingStyle.Stroke -> PaintBundle.STYLE_STROKE
        else -> PaintBundle.STYLE_FILL
    }

internal fun LineBreak.encode(): Int =
    when (this.strategy) {
        LineBreak.Strategy.Simple -> 0
        LineBreak.Strategy.HighQuality -> 1
        LineBreak.Strategy.Balanced -> 2
        else -> 0
    }

internal fun Hyphens.encode(): Int =
    when (this) {
        Hyphens.Auto -> 2 // Layout.HYPHENATION_FREQUENCY_FULL
        Hyphens.None,
        Hyphens.Unspecified -> 0 // Layout.HYPHENATION_FREQUENCY_NONE
        else -> 0
    }

internal fun TileMode.toTileModeInt(): Int = toRemoteTileMode().value

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
