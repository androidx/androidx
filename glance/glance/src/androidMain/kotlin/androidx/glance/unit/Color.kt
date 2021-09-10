/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.unit

import androidx.compose.runtime.Stable

/**
 * The [Color] class contains color information to be used while drawing elements. [Color] supports
 * colors encoded in the ARGB format.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class Color(public val value: UInt) {
    public constructor(value: Long) : this(value.toUInt())

    /**
     * Returns the value of the red component of this [Color]. Return values are in the range
     * 0.0 - 1.0.
     */
    @Stable
    public val red: Float
        get() {
            return ((value shr 16) and 0xFFu).toFloat() / 255.0f
        }

    /**
     * Returns the value of the green component of this [Color]. Return values are in the range
     * 0.0 - 1.0.
     */
    @Stable
    public val green: Float
        get() {
            return ((value shr 8) and 0xFFu).toFloat() / 255.0f
        }

    /**
     * Returns the value of the blue component of this [Color]. Return values are in the range
     * 0.0 - 1.0.
     */
    @Stable
    public val blue: Float
        get() {
            return ((value shr 0) and 0xFFu).toFloat() / 255.0f
        }

    /**
     * Returns the value of the alpha] component of this [Color]. Return values are in the range
     * 0.0 - 1.0.
     */
    @Stable
    public val alpha: Float
        get() {
            return ((value shr 24) and 0xFFu).toFloat() / 255.0f
        }

    override fun toString(): String {
        return "Color($red, $green, $blue, $alpha)"
    }

    public companion object {
        @Stable
        public val Black: Color = Color(0xFF000000u)

        @Stable
        public val DarkGray: Color = Color(0xFF444444u)

        @Stable
        public val Gray: Color = Color(0xFF888888u)

        @Stable
        public val LightGray: Color = Color(0xFFCCCCCCu)

        @Stable
        public val White: Color = Color(0xFFFFFFFFu)

        @Stable
        public val Red: Color = Color(0xFFFF0000u)

        @Stable
        public val Green: Color = Color(0xFF00FF00u)

        @Stable
        public val Blue: Color = Color(0xFF0000FFu)

        @Stable
        public val Yellow: Color = Color(0xFFFFFF00u)

        @Stable
        public val Cyan: Color = Color(0xFF00FFFFu)

        @Stable
        public val Magenta: Color = Color(0xFFFF00FFu)

        @Stable
        public val Transparent: Color = Color(0x00000000u)
    }
}

/**
 * Create a [Color] by passing individual [red], [green], [blue] and [alpha] components. The
 * default [alpha] is `1.0` if omitted.
 */
@Stable
public fun Color(
    red: Float,
    green: Float,
    blue: Float,
    alpha: Float = 1.0f
): Color {
    val argb = (
        ((alpha * 255.0f + 0.5f).toInt() shl 24) or
            ((red * 255.0f + 0.5f).toInt() shl 16) or
            ((green * 255.0f + 0.5f).toInt() shl 8) or
            (blue * 255.0f + 0.5f).toInt()
        )

    return Color(value = argb.toUInt())
}
