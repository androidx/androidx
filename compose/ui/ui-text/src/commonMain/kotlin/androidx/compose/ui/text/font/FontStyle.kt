/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.compose.ui.text.font

import androidx.compose.ui.text.internal.requirePrecondition

/**
 * Defines whether the font is [Italic] or [Normal].
 *
 * @see Font
 * @see FontFamily
 */
@kotlin.jvm.JvmInline
public value class FontStyle
/**
 * Constructs a [FontStyle] with the given integer [value].
 *
 * @param value the integer representation of the font style. Must be `0` ([Normal]) or `1`
 *   ([Italic]).
 * @throws IllegalArgumentException if [value] is outside `0..1`.
 */
@Deprecated(
    "Please use FontStyle.Normal or FontStyle.Italic",
    replaceWith = ReplaceWith("FontStyle."),
)
public constructor(public val value: Int) {

    init {
        requirePrecondition(value in 0..MAX_FONT_STYLE_VALUE) {
            "Invalid FontStyle value ($value). Must be Normal (0) or Italic (1)."
        }
    }

    public override fun toString(): String {
        return when (this) {
            Normal -> "Normal"
            Italic -> "Italic"
            else -> "Invalid"
        }
    }

    public companion object {
        /** Use the upright glyphs */
        @Suppress("DEPRECATION")
        public val Normal: FontStyle
            get() = FontStyle(0)

        /** Use glyphs designed for slanting */
        @Suppress("DEPRECATION")
        public val Italic: FontStyle
            get() = FontStyle(1)

        /**
         * Max valid [FontStyle.value] (`Italic = 1`). Update [androidx.compose.ui.text.SpanStyle]
         * bit pack code if you add a new style.
         */
        internal const val MAX_FONT_STYLE_VALUE = 1

        /** Returns a list of possible values of [FontStyle]. */
        public fun values(): List<FontStyle> = listOf(Normal, Italic)
    }
}
