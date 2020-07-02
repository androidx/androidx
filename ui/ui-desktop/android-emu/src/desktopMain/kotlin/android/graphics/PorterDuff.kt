/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

import org.jetbrains.skija.BlendMode

@Suppress("unused")
class PorterDuff {
    enum class Mode {
        ADD,
        CLEAR,
        DARKEN,
        DST,
        DST_ATOP,
        DST_IN,
        DST_OUT,
        DST_OVER,
        LIGHTEN,
        MULTIPLY,
        OVERLAY,
        SCREEN,
        SRC,
        SRC_ATOP,
        SRC_IN,
        SRC_OUT,
        SRC_OVER,
        XOR
    }
}

internal fun PorterDuff.Mode.toSkia() = when (this) {
    PorterDuff.Mode.ADD -> BlendMode.PLUS
    PorterDuff.Mode.CLEAR -> BlendMode.CLEAR
    PorterDuff.Mode.DARKEN -> BlendMode.DARKEN
    PorterDuff.Mode.DST -> BlendMode.DST
    PorterDuff.Mode.DST_ATOP -> BlendMode.DST_ATOP
    PorterDuff.Mode.DST_IN -> BlendMode.DST_IN
    PorterDuff.Mode.DST_OUT -> BlendMode.DST_OUT
    PorterDuff.Mode.DST_OVER -> BlendMode.DST_OVER
    PorterDuff.Mode.LIGHTEN -> BlendMode.LIGHTEN
    PorterDuff.Mode.MULTIPLY -> BlendMode.MULTIPLY
    PorterDuff.Mode.OVERLAY -> BlendMode.OVERLAY
    PorterDuff.Mode.SCREEN -> BlendMode.SCREEN
    PorterDuff.Mode.SRC -> BlendMode.SRC
    PorterDuff.Mode.SRC_ATOP -> BlendMode.SRC_ATOP
    PorterDuff.Mode.SRC_IN -> BlendMode.SRC_IN
    PorterDuff.Mode.SRC_OUT -> BlendMode.SRC_OUT
    PorterDuff.Mode.SRC_OVER -> BlendMode.SRC_OVER
    PorterDuff.Mode.XOR -> BlendMode.XOR
}
