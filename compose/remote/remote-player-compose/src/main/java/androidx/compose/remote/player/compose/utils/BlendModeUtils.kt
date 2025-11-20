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
package androidx.compose.remote.player.compose.utils

import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.ui.graphics.BlendMode

/** Get a [BlendMode] from a [PaintBundle] */
internal fun remoteToBlendMode(mode: Int): BlendMode? =
    when (mode) {
        PaintBundle.BLEND_MODE_CLEAR -> BlendMode.Clear
        PaintBundle.BLEND_MODE_SRC -> BlendMode.Src
        PaintBundle.BLEND_MODE_DST -> BlendMode.Dst
        PaintBundle.BLEND_MODE_SRC_OVER -> BlendMode.SrcOver
        PaintBundle.BLEND_MODE_DST_OVER -> BlendMode.DstOver
        PaintBundle.BLEND_MODE_SRC_IN -> BlendMode.SrcIn
        PaintBundle.BLEND_MODE_DST_IN -> BlendMode.DstIn
        PaintBundle.BLEND_MODE_SRC_OUT -> BlendMode.SrcOut
        PaintBundle.BLEND_MODE_DST_OUT -> BlendMode.DstOut
        PaintBundle.BLEND_MODE_SRC_ATOP -> BlendMode.SrcAtop
        PaintBundle.BLEND_MODE_DST_ATOP -> BlendMode.DstAtop
        PaintBundle.BLEND_MODE_XOR -> BlendMode.Xor
        PaintBundle.BLEND_MODE_PLUS -> BlendMode.Plus
        PaintBundle.BLEND_MODE_MODULATE -> BlendMode.Modulate
        PaintBundle.BLEND_MODE_SCREEN -> BlendMode.Screen
        PaintBundle.BLEND_MODE_OVERLAY -> BlendMode.Overlay
        PaintBundle.BLEND_MODE_DARKEN -> BlendMode.Darken
        PaintBundle.BLEND_MODE_LIGHTEN -> BlendMode.Lighten
        PaintBundle.BLEND_MODE_COLOR_DODGE -> BlendMode.ColorDodge
        PaintBundle.BLEND_MODE_COLOR_BURN -> BlendMode.ColorBurn
        PaintBundle.BLEND_MODE_HARD_LIGHT -> BlendMode.Hardlight
        PaintBundle.BLEND_MODE_SOFT_LIGHT -> BlendMode.Softlight
        PaintBundle.BLEND_MODE_DIFFERENCE -> BlendMode.Difference
        PaintBundle.BLEND_MODE_EXCLUSION -> BlendMode.Exclusion
        PaintBundle.BLEND_MODE_MULTIPLY -> BlendMode.Multiply
        PaintBundle.BLEND_MODE_HUE -> BlendMode.Hue
        PaintBundle.BLEND_MODE_SATURATION -> BlendMode.Saturation
        PaintBundle.BLEND_MODE_COLOR -> BlendMode.Color
        PaintBundle.BLEND_MODE_LUMINOSITY -> BlendMode.Luminosity
        PaintBundle.BLEND_MODE_NULL -> null
        else -> null
    }
