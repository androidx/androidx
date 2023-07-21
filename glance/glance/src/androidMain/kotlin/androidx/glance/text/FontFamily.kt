/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.text

/**
 * Describes the family of the font.
 * Defaults are provided, but it is also possible to supply a custom family. If this is found on
 * the system it will be used, otherwise it will fallback to a system default.
 */
class FontFamily constructor(val family: String) {
    companion object {
        /**
         * The formal text style for scripts.
         */
        val Serif = FontFamily("serif")

        /**
         * Font family with low contrast and plain stroke endings.
         */
        val SansSerif = FontFamily("sans-serif")

        /**
         * Font family where glyphs have the same fixed width.
         */
        val Monospace = FontFamily("monospace")

        /**
         * Cursive, hand-written like font family.
         */
        val Cursive = FontFamily("cursive")
    }

    override fun toString(): String {
        return family
    }
}
