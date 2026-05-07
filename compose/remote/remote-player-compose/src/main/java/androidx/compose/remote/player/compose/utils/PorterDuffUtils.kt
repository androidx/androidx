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

import android.graphics.PorterDuff
import androidx.compose.remote.core.operations.paint.PaintBundle

/** Get a [PorterDuff.Mode] from a [PaintBundle] */
internal fun remoteToPorterDuffMode(mode: Int): PorterDuff.Mode =
    when (mode) {
        PaintBundle.BLEND_MODE_CLEAR -> PorterDuff.Mode.CLEAR
        PaintBundle.BLEND_MODE_SRC -> PorterDuff.Mode.SRC
        PaintBundle.BLEND_MODE_DST -> PorterDuff.Mode.DST
        PaintBundle.BLEND_MODE_SRC_OVER -> PorterDuff.Mode.SRC_OVER
        PaintBundle.BLEND_MODE_DST_OVER -> PorterDuff.Mode.DST_OVER
        PaintBundle.BLEND_MODE_SRC_IN -> PorterDuff.Mode.SRC_IN
        PaintBundle.BLEND_MODE_DST_IN -> PorterDuff.Mode.DST_IN
        PaintBundle.BLEND_MODE_SRC_OUT -> PorterDuff.Mode.SRC_OUT
        PaintBundle.BLEND_MODE_DST_OUT -> PorterDuff.Mode.DST_OUT
        PaintBundle.BLEND_MODE_SRC_ATOP -> PorterDuff.Mode.SRC_ATOP
        PaintBundle.BLEND_MODE_DST_ATOP -> PorterDuff.Mode.DST_ATOP
        PaintBundle.BLEND_MODE_XOR -> PorterDuff.Mode.XOR
        PaintBundle.BLEND_MODE_SCREEN -> PorterDuff.Mode.SCREEN
        PaintBundle.BLEND_MODE_OVERLAY -> PorterDuff.Mode.OVERLAY
        PaintBundle.BLEND_MODE_DARKEN -> PorterDuff.Mode.DARKEN
        PaintBundle.BLEND_MODE_LIGHTEN -> PorterDuff.Mode.LIGHTEN
        PaintBundle.BLEND_MODE_MULTIPLY -> PorterDuff.Mode.MULTIPLY
        PaintBundle.PORTER_MODE_ADD -> PorterDuff.Mode.ADD
        else -> PorterDuff.Mode.SRC_OVER
    }
