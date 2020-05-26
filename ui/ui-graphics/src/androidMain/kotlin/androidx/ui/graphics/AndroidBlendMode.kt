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

package androidx.ui.graphics

internal fun BlendMode.toPorterDuffMode(): android.graphics.PorterDuff.Mode = when (this) {
    BlendMode.clear -> android.graphics.PorterDuff.Mode.CLEAR
    BlendMode.src -> android.graphics.PorterDuff.Mode.SRC
    BlendMode.dst -> android.graphics.PorterDuff.Mode.DST
    BlendMode.srcOver -> android.graphics.PorterDuff.Mode.SRC_OVER
    BlendMode.dstOver -> android.graphics.PorterDuff.Mode.DST_OVER
    BlendMode.srcIn -> android.graphics.PorterDuff.Mode.SRC_IN
    BlendMode.dstIn -> android.graphics.PorterDuff.Mode.DST_IN
    BlendMode.srcOut -> android.graphics.PorterDuff.Mode.SRC_OUT
    BlendMode.dstOut -> android.graphics.PorterDuff.Mode.DST_OUT
    BlendMode.srcATop -> android.graphics.PorterDuff.Mode.SRC_ATOP
    BlendMode.dstATop -> android.graphics.PorterDuff.Mode.DST_ATOP
    BlendMode.xor -> android.graphics.PorterDuff.Mode.XOR
    BlendMode.plus -> android.graphics.PorterDuff.Mode.ADD
    BlendMode.screen -> android.graphics.PorterDuff.Mode.SCREEN
    BlendMode.overlay -> android.graphics.PorterDuff.Mode.OVERLAY
    BlendMode.darken -> android.graphics.PorterDuff.Mode.DARKEN
    BlendMode.lighten -> android.graphics.PorterDuff.Mode.LIGHTEN
    BlendMode.multiply -> android.graphics.PorterDuff.Mode.MULTIPLY
    else -> TODO("$this is unsupported")
}