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
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/** Converts a Compose [BlendMode] to an Android framework [android.graphics.BlendMode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode {
    return when (this) {
        BlendMode.SrcOver -> AndroidBlendMode.SRC_OVER
        BlendMode.SrcIn -> AndroidBlendMode.SRC_IN
        BlendMode.SrcOut -> AndroidBlendMode.SRC_OUT
        BlendMode.SrcAtop -> AndroidBlendMode.SRC_ATOP
        BlendMode.DstOver -> AndroidBlendMode.DST_OVER
        BlendMode.DstIn -> AndroidBlendMode.DST_IN
        BlendMode.DstOut -> AndroidBlendMode.DST_OUT
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

/** Converts [ContentScale] to [ImageScaling]. */
internal fun ContentScale.toImageScalingInt(): Int {
    return when (this) {
        ContentScale.Fit -> ImageScaling.SCALE_FIT
        ContentScale.Crop -> ImageScaling.SCALE_CROP
        ContentScale.None -> ImageScaling.SCALE_NONE
        ContentScale.Inside -> ImageScaling.SCALE_INSIDE
        ContentScale.FillWidth -> ImageScaling.SCALE_FILL_WIDTH
        ContentScale.FillHeight -> ImageScaling.SCALE_FILL_HEIGHT
        ContentScale.FillBounds -> ImageScaling.SCALE_FILL_BOUNDS
        else -> ImageScaling.SCALE_NONE
    }
}

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

internal fun FontFamily?.encode(): String? =
    when (this) {
        FontFamily.Default -> "default"
        FontFamily.SansSerif -> "sans-serif"
        FontFamily.Serif -> "serif"
        FontFamily.Monospace -> "monospace"
        FontFamily.Cursive -> "cursive"
        is GenericFontFamily -> name
        else -> null
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
        AndroidBlendMode.SRC_OUT -> BlendMode.SrcIn
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

internal fun BlendMode.toInt(): Int {
    return when (this) {
        BlendMode.Clear -> PaintBundle.BLEND_MODE_CLEAR
        BlendMode.Src -> PaintBundle.BLEND_MODE_SRC
        BlendMode.Dst -> PaintBundle.BLEND_MODE_DST
        BlendMode.SrcOver -> PaintBundle.BLEND_MODE_SRC_OVER
        BlendMode.DstOver -> PaintBundle.BLEND_MODE_DST_OVER
        BlendMode.SrcIn -> PaintBundle.BLEND_MODE_SRC_IN
        BlendMode.DstIn -> PaintBundle.BLEND_MODE_DST_IN
        BlendMode.SrcOut -> PaintBundle.BLEND_MODE_SRC_OUT
        BlendMode.DstOut -> PaintBundle.BLEND_MODE_DST_OUT
        BlendMode.SrcAtop -> PaintBundle.BLEND_MODE_SRC_ATOP
        BlendMode.DstAtop -> PaintBundle.BLEND_MODE_DST_ATOP
        BlendMode.Xor -> PaintBundle.BLEND_MODE_XOR
        BlendMode.Plus -> PaintBundle.BLEND_MODE_PLUS
        BlendMode.Modulate -> PaintBundle.BLEND_MODE_MODULATE
        BlendMode.Screen -> PaintBundle.BLEND_MODE_SCREEN
        BlendMode.Overlay -> PaintBundle.BLEND_MODE_OVERLAY
        BlendMode.Darken -> PaintBundle.BLEND_MODE_DARKEN
        BlendMode.Lighten -> PaintBundle.BLEND_MODE_LIGHTEN
        BlendMode.ColorDodge -> PaintBundle.BLEND_MODE_COLOR_DODGE
        BlendMode.ColorBurn -> PaintBundle.BLEND_MODE_COLOR_BURN
        BlendMode.Hardlight -> PaintBundle.BLEND_MODE_HARD_LIGHT
        BlendMode.Softlight -> PaintBundle.BLEND_MODE_SOFT_LIGHT
        BlendMode.Difference -> PaintBundle.BLEND_MODE_DIFFERENCE
        BlendMode.Exclusion -> PaintBundle.BLEND_MODE_EXCLUSION
        BlendMode.Multiply -> PaintBundle.BLEND_MODE_MULTIPLY
        BlendMode.Hue -> PaintBundle.BLEND_MODE_HUE
        BlendMode.Saturation -> PaintBundle.BLEND_MODE_SATURATION
        BlendMode.Color -> PaintBundle.BLEND_MODE_COLOR
        BlendMode.Luminosity -> PaintBundle.BLEND_MODE_LUMINOSITY
        else -> PaintBundle.BLEND_MODE_SRC_OVER
    }
}
