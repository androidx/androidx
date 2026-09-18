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

import androidx.compose.remote.core.operations.layout.managers.TextLayout
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.ImageScaling
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

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

internal fun TileMode.toTileModeInt(): Int =
    when (this) {
        TileMode.Clamp -> 0
        TileMode.Repeated -> 1
        TileMode.Mirror -> 2
        TileMode.Decal -> 3
        else -> 0
    }
