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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

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
