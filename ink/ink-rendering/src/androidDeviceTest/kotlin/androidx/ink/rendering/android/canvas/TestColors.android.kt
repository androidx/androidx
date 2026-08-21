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

package androidx.ink.rendering.android.canvas

import androidx.annotation.ColorInt

/**
 * [ColorInt] constants for use in tests.
 *
 * Channels are in ARGB order, per the definition of [ColorInt]. Use the helper functions defined
 * below to convert to other channel orders.
 *
 * These colors have different values for all RGB channels and at least one channel with a value
 * strictly between 0.0 (0x00) and 1.0 (0xff). These properties help check for channel order
 * scrambling (for example, incorrect mixing of RGB and BGR formats) and gamma correction errors.
 */
object TestColors {
    /**
     * Near-white color for backgrounds and elements without textures. For textured elements that
     * need a 100% white base color, use [WHITE_FOR_TEXTURE].
     */
    @ColorInt const val WHITE = 0xfff5f8ff.toInt()
    // Gray and black are not pure desaturated tones, because we need different values in the
    // different channels.
    @ColorInt const val LIGHT_GRAY = 0xffbaccc0.toInt()
    @ColorInt const val DARK_GRAY = 0xff4d4239.toInt()
    @ColorInt const val BLACK = 0xff290e1c.toInt()
    @ColorInt const val RED = 0xfff7251e.toInt()
    @ColorInt const val ORANGE = 0xffff6e40.toInt()
    @ColorInt const val LIGHT_ORANGE = 0xffffccbc.toInt()
    @ColorInt const val YELLOW = 0xfff7f12d.toInt()
    @ColorInt const val AVOCADO_GREEN = 0xff558b2f.toInt()
    @ColorInt const val GREEN = 0xff00c853.toInt()
    @ColorInt const val CYAN = 0xff2be3f0.toInt()
    @ColorInt const val LIGHT_BLUE = 0xff4fb5e8.toInt()
    @ColorInt const val BLUE = 0xff304ffe.toInt()
    @ColorInt const val COBALT_BLUE = 0xff01579b.toInt()
    @ColorInt const val DEEP_PURPLE = 0xff8e24aa.toInt()
    @ColorInt const val MAGENTA = 0xffed26e0.toInt()
    @ColorInt const val HOT_PINK = 0xffff4081.toInt()

    /** White base color for elements that have a texture applied. */
    @ColorInt const val WHITE_FOR_TEXTURE = 0xffffffff.toInt()

    @ColorInt const val TRANSLUCENT_ORANGE = 0x80ffbf00.toInt()

    @JvmStatic
    fun colorIntToRgba(@ColorInt argb: Int): Int = (argb shl 8) or ((argb shr 24) and 0xff)
}
