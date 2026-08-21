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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.BlendMode
import android.graphics.PorterDuff
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.ink.brush.BrushPaint.TextureLayer

/**
 * Returns the Android [PorterDuff.Mode] that is equivalent to this Ink [TextureLayer.BlendMode].
 */
internal fun TextureLayer.BlendMode.toPorterDuffMode() =
    when (this) {
        // Note that the MODULATE behavior is incorrectly called MULTIPLY in [PorterDuff.Mode].
        TextureLayer.BlendMode.MODULATE -> PorterDuff.Mode.MULTIPLY
        TextureLayer.BlendMode.DST_IN -> PorterDuff.Mode.DST_IN
        TextureLayer.BlendMode.DST_OUT -> PorterDuff.Mode.DST_OUT
        TextureLayer.BlendMode.SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
        TextureLayer.BlendMode.SRC_IN -> PorterDuff.Mode.SRC_IN
        TextureLayer.BlendMode.SRC_OVER -> PorterDuff.Mode.SRC_OVER
        TextureLayer.BlendMode.DST_OVER -> PorterDuff.Mode.DST_OVER
        TextureLayer.BlendMode.SRC -> PorterDuff.Mode.SRC
        TextureLayer.BlendMode.DST -> PorterDuff.Mode.DST
        TextureLayer.BlendMode.SRC_OUT -> PorterDuff.Mode.SRC_OUT
        TextureLayer.BlendMode.DST_ATOP -> PorterDuff.Mode.DST_ATOP
        TextureLayer.BlendMode.XOR -> PorterDuff.Mode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported BlendMode: $this. Using PorterDuff.Mode.MULTIPLY instead.",
            )
            PorterDuff.Mode.MULTIPLY
        }
    }

/** Like [toPorterDuffMode], but with SRC and DST swapped. */
internal fun TextureLayer.BlendMode.toReversePorterDuffMode() =
    when (this) {
        // Note that the MODULATE behavior is incorrectly called MULTIPLY in [PorterDuff.Mode].
        TextureLayer.BlendMode.MODULATE -> PorterDuff.Mode.MULTIPLY
        TextureLayer.BlendMode.DST_IN -> PorterDuff.Mode.SRC_IN
        TextureLayer.BlendMode.DST_OUT -> PorterDuff.Mode.SRC_OUT
        TextureLayer.BlendMode.SRC_ATOP -> PorterDuff.Mode.DST_ATOP
        TextureLayer.BlendMode.SRC_IN -> PorterDuff.Mode.DST_IN
        TextureLayer.BlendMode.SRC_OVER -> PorterDuff.Mode.DST_OVER
        TextureLayer.BlendMode.DST_OVER -> PorterDuff.Mode.SRC_OVER
        TextureLayer.BlendMode.SRC -> PorterDuff.Mode.DST
        TextureLayer.BlendMode.DST -> PorterDuff.Mode.SRC
        TextureLayer.BlendMode.SRC_OUT -> PorterDuff.Mode.DST_OUT
        TextureLayer.BlendMode.DST_ATOP -> PorterDuff.Mode.SRC_ATOP
        TextureLayer.BlendMode.XOR -> PorterDuff.Mode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported TextureBlendMode: $this. Using PorterDuff.Mode.MULTIPLY instead.",
            )
            PorterDuff.Mode.MULTIPLY
        }
    }

/** Returns the Android [BlendMode] that is equivalent to this Ink [TextureLayer.BlendMode]. */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal fun TextureLayer.BlendMode.toBlendMode() =
    when (this) {
        TextureLayer.BlendMode.MODULATE -> BlendMode.MODULATE
        TextureLayer.BlendMode.DST_IN -> BlendMode.DST_IN
        TextureLayer.BlendMode.DST_OUT -> BlendMode.DST_OUT
        TextureLayer.BlendMode.SRC_ATOP -> BlendMode.SRC_ATOP
        TextureLayer.BlendMode.SRC_IN -> BlendMode.SRC_IN
        TextureLayer.BlendMode.SRC_OVER -> BlendMode.SRC_OVER
        TextureLayer.BlendMode.DST_OVER -> BlendMode.DST_OVER
        TextureLayer.BlendMode.SRC -> BlendMode.SRC
        TextureLayer.BlendMode.DST -> BlendMode.DST
        TextureLayer.BlendMode.SRC_OUT -> BlendMode.SRC_OUT
        TextureLayer.BlendMode.DST_ATOP -> BlendMode.DST_ATOP
        TextureLayer.BlendMode.XOR -> BlendMode.XOR
        else -> {
            Log.e(
                "BlendModeConversion",
                "Unsupported BlendMode: $this. Using BlendMode.MODULATE instead.",
            )
            BlendMode.MODULATE
        }
    }
