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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.capture.shaders

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.core.operations.paint.PaintBundle

fun colorFilterModeToInt(mode: android.graphics.BlendMode): Int {
    return when (mode) {
        android.graphics.BlendMode.CLEAR -> PaintBundle.BLEND_MODE_CLEAR
        android.graphics.BlendMode.SRC -> PaintBundle.BLEND_MODE_SRC
        android.graphics.BlendMode.DST -> PaintBundle.BLEND_MODE_DST
        android.graphics.BlendMode.SRC_OVER -> PaintBundle.BLEND_MODE_SRC_OVER
        android.graphics.BlendMode.DST_OVER -> PaintBundle.BLEND_MODE_DST_OVER
        android.graphics.BlendMode.SRC_IN -> PaintBundle.BLEND_MODE_SRC_IN
        android.graphics.BlendMode.DST_IN -> PaintBundle.BLEND_MODE_DST_IN
        android.graphics.BlendMode.SRC_OUT -> PaintBundle.BLEND_MODE_SRC_OUT
        android.graphics.BlendMode.DST_OUT -> PaintBundle.BLEND_MODE_DST_OUT
        android.graphics.BlendMode.SRC_ATOP -> PaintBundle.BLEND_MODE_SRC_ATOP
        android.graphics.BlendMode.DST_ATOP -> PaintBundle.BLEND_MODE_DST_ATOP
        android.graphics.BlendMode.XOR -> PaintBundle.BLEND_MODE_XOR
        android.graphics.BlendMode.PLUS -> PaintBundle.BLEND_MODE_PLUS
        android.graphics.BlendMode.MODULATE -> PaintBundle.BLEND_MODE_MODULATE
        android.graphics.BlendMode.SCREEN -> PaintBundle.BLEND_MODE_SCREEN
        android.graphics.BlendMode.OVERLAY -> PaintBundle.BLEND_MODE_OVERLAY
        android.graphics.BlendMode.DARKEN -> PaintBundle.BLEND_MODE_DARKEN
        android.graphics.BlendMode.LIGHTEN -> PaintBundle.BLEND_MODE_LIGHTEN
        android.graphics.BlendMode.COLOR_DODGE -> PaintBundle.BLEND_MODE_COLOR_DODGE
        android.graphics.BlendMode.COLOR_BURN -> PaintBundle.BLEND_MODE_COLOR_BURN
        android.graphics.BlendMode.HARD_LIGHT -> PaintBundle.BLEND_MODE_HARD_LIGHT
        android.graphics.BlendMode.SOFT_LIGHT -> PaintBundle.BLEND_MODE_SOFT_LIGHT
        android.graphics.BlendMode.DIFFERENCE -> PaintBundle.BLEND_MODE_DIFFERENCE
        android.graphics.BlendMode.EXCLUSION -> PaintBundle.BLEND_MODE_EXCLUSION
        android.graphics.BlendMode.MULTIPLY -> PaintBundle.BLEND_MODE_MULTIPLY
        android.graphics.BlendMode.HUE -> PaintBundle.BLEND_MODE_HUE
        android.graphics.BlendMode.SATURATION -> PaintBundle.BLEND_MODE_SATURATION
        android.graphics.BlendMode.COLOR -> PaintBundle.BLEND_MODE_COLOR
        android.graphics.BlendMode.LUMINOSITY -> PaintBundle.BLEND_MODE_LUMINOSITY
    }
}
