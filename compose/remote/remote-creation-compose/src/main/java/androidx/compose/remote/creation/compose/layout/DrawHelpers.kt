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

import android.graphics.Paint
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.layout.ContentScale

/** Converts a Compose [BlendMode] to an Android framework [android.graphics.BlendMode]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun BlendMode.toAndroidBlendMode(): android.graphics.BlendMode {
    return when (this) {
        BlendMode.SrcOver -> android.graphics.BlendMode.SRC_OVER
        BlendMode.SrcIn -> android.graphics.BlendMode.SRC_IN
        BlendMode.SrcOut -> android.graphics.BlendMode.SRC_OUT
        BlendMode.SrcAtop -> android.graphics.BlendMode.SRC_ATOP
        BlendMode.DstOver -> android.graphics.BlendMode.DST_OVER
        BlendMode.DstIn -> android.graphics.BlendMode.DST_IN
        BlendMode.DstOut -> android.graphics.BlendMode.DST_OUT
        BlendMode.DstAtop -> android.graphics.BlendMode.DST_ATOP
        BlendMode.Xor -> android.graphics.BlendMode.XOR
        BlendMode.Plus -> android.graphics.BlendMode.PLUS
        BlendMode.Modulate -> android.graphics.BlendMode.MODULATE
        BlendMode.Screen -> android.graphics.BlendMode.SCREEN
        BlendMode.Overlay -> android.graphics.BlendMode.OVERLAY
        BlendMode.Darken -> android.graphics.BlendMode.DARKEN
        BlendMode.Lighten -> android.graphics.BlendMode.LIGHTEN
        BlendMode.ColorDodge -> android.graphics.BlendMode.COLOR_DODGE
        BlendMode.ColorBurn -> android.graphics.BlendMode.COLOR_BURN
        BlendMode.Hardlight -> android.graphics.BlendMode.HARD_LIGHT
        BlendMode.Softlight -> android.graphics.BlendMode.SOFT_LIGHT
        BlendMode.Difference -> android.graphics.BlendMode.DIFFERENCE
        BlendMode.Exclusion -> android.graphics.BlendMode.EXCLUSION
        BlendMode.Multiply -> android.graphics.BlendMode.MULTIPLY
        BlendMode.Hue -> android.graphics.BlendMode.HUE
        BlendMode.Saturation -> android.graphics.BlendMode.SATURATION
        BlendMode.Color -> android.graphics.BlendMode.COLOR
        BlendMode.Luminosity -> android.graphics.BlendMode.LUMINOSITY
        else -> android.graphics.BlendMode.SRC_OVER
    }
}

/** Converts a Compose [StrokeCap] to an Android framework [android.graphics.Paint.Cap]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun StrokeCap.toAndroidCap(): android.graphics.Paint.Cap =
    when (this) {
        StrokeCap.Butt -> android.graphics.Paint.Cap.BUTT
        StrokeCap.Round -> android.graphics.Paint.Cap.ROUND
        StrokeCap.Square -> android.graphics.Paint.Cap.SQUARE
        else -> android.graphics.Paint.Cap.BUTT
    }

/** Converts a Compose [StrokeJoin] to an Android framework [android.graphics.Paint.Join]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun StrokeJoin.toAndroidJoin(): android.graphics.Paint.Join =
    when (this) {
        StrokeJoin.Miter -> android.graphics.Paint.Join.MITER
        StrokeJoin.Round -> android.graphics.Paint.Join.ROUND
        StrokeJoin.Bevel -> android.graphics.Paint.Join.BEVEL
        else -> android.graphics.Paint.Join.MITER
    }

/** Converts [ContentScale] to [ImageScaling]. */
internal fun ContentScale.toRemoteCompose(): Int {
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
