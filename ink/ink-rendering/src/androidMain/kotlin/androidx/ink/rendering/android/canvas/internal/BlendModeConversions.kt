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

@file:OptIn(ExperimentalInkCustomBrushApi::class)

package androidx.ink.rendering.android.canvas.internal

import android.graphics.BlendMode
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.ExperimentalInkCustomBrushApi

/** Returns the Android [PorterDuff.Mode] that is equivalent to this Ink [BrushPaint.BlendMode]. */
internal fun BrushPaint.BlendMode.toPorterDuffMode() =
    when (this) {
        // Note that the MODULATE behavior is incorrectly called MULTIPLY in [PorterDuff.Mode].
        BrushPaint.BlendMode.MODULATE -> PorterDuff.Mode.MULTIPLY
        BrushPaint.BlendMode.DST_IN -> PorterDuff.Mode.DST_IN
        BrushPaint.BlendMode.DST_OUT -> PorterDuff.Mode.DST_OUT
        BrushPaint.BlendMode.SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
        BrushPaint.BlendMode.SRC_IN -> PorterDuff.Mode.SRC_IN
        BrushPaint.BlendMode.SRC_OVER -> PorterDuff.Mode.SRC_OVER
        BrushPaint.BlendMode.DST_OVER -> PorterDuff.Mode.DST_OVER
        BrushPaint.BlendMode.SRC -> PorterDuff.Mode.SRC
        BrushPaint.BlendMode.DST -> PorterDuff.Mode.DST
        BrushPaint.BlendMode.SRC_OUT -> PorterDuff.Mode.SRC_OUT
        BrushPaint.BlendMode.DST_ATOP -> PorterDuff.Mode.DST_ATOP
        BrushPaint.BlendMode.XOR -> PorterDuff.Mode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported BlendMode: $this. Using PorterDuff.Mode.MULTIPLY instead.",
            )
            PorterDuff.Mode.MULTIPLY
        }
    }

/** Like [toPorterDuffMode], but with SRC and DST swapped. */
internal fun BrushPaint.BlendMode.toReversePorterDuffMode() =
    when (this) {
        // Note that the MODULATE behavior is incorrectly called MULTIPLY in [PorterDuff.Mode].
        BrushPaint.BlendMode.MODULATE -> PorterDuff.Mode.MULTIPLY
        BrushPaint.BlendMode.DST_IN -> PorterDuff.Mode.SRC_IN
        BrushPaint.BlendMode.DST_OUT -> PorterDuff.Mode.SRC_OUT
        BrushPaint.BlendMode.SRC_ATOP -> PorterDuff.Mode.DST_ATOP
        BrushPaint.BlendMode.SRC_IN -> PorterDuff.Mode.DST_IN
        BrushPaint.BlendMode.SRC_OVER -> PorterDuff.Mode.DST_OVER
        BrushPaint.BlendMode.DST_OVER -> PorterDuff.Mode.SRC_OVER
        BrushPaint.BlendMode.SRC -> PorterDuff.Mode.DST
        BrushPaint.BlendMode.DST -> PorterDuff.Mode.SRC
        BrushPaint.BlendMode.SRC_OUT -> PorterDuff.Mode.DST_OUT
        BrushPaint.BlendMode.DST_ATOP -> PorterDuff.Mode.SRC_ATOP
        BrushPaint.BlendMode.XOR -> PorterDuff.Mode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported TextureBlendMode: $this. Using PorterDuff.Mode.MULTIPLY instead.",
            )
            PorterDuff.Mode.MULTIPLY
        }
    }

/** Returns the Android [BlendMode] that is equivalent to this Ink [BrushPaint.BlendMode]. */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal fun BrushPaint.BlendMode.toBlendMode() =
    when (this) {
        BrushPaint.BlendMode.MODULATE -> BlendMode.MODULATE
        BrushPaint.BlendMode.DST_IN -> BlendMode.DST_IN
        BrushPaint.BlendMode.DST_OUT -> BlendMode.DST_OUT
        BrushPaint.BlendMode.SRC_ATOP -> BlendMode.SRC_ATOP
        BrushPaint.BlendMode.SRC_IN -> BlendMode.SRC_IN
        BrushPaint.BlendMode.SRC_OVER -> BlendMode.SRC_OVER
        BrushPaint.BlendMode.DST_OVER -> BlendMode.DST_OVER
        BrushPaint.BlendMode.SRC -> BlendMode.SRC
        BrushPaint.BlendMode.DST -> BlendMode.DST
        BrushPaint.BlendMode.SRC_OUT -> BlendMode.SRC_OUT
        BrushPaint.BlendMode.DST_ATOP -> BlendMode.DST_ATOP
        BrushPaint.BlendMode.XOR -> BlendMode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported BlendMode: $this. Using BlendMode.MODULATE instead.",
            )
            BlendMode.MODULATE
        }
    }
