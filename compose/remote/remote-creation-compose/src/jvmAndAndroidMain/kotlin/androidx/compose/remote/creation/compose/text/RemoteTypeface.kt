/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.text

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable

/**
 * A sealed interface that represent the concept of a Typeface in Remote Compose.
 *
 * Custom or named system fonts are represented by [Named] type and are registered in the document
 * as Strings to be referenced.
 */
@Immutable
public sealed interface RemoteTypeface {
    /** The name used to identify the font in the document and player */
    public val name: String

    /** Represents the default system font. */
    public object Default : RemoteTypeface {
        override val name: String = "default"
    }

    /** Represents the default bold system font. */
    public object DefaultBold : RemoteTypeface {
        override val name: String = "default-bold"
    }

    /** Represents the sans-serif system font. */
    public object SansSerif : RemoteTypeface {
        override val name: String = "sans-serif"
    }

    /** Represents the serif system font. */
    public object Serif : RemoteTypeface {
        override val name: String = "serif"
    }

    /** Represents the monospace system font. */
    public object Monospace : RemoteTypeface {
        override val name: String = "monospace"
    }

    /**
     * Represents a system font referenced by name.
     *
     * @param fontName The string name of the font family (e.g., "roboto-flex").
     * @param weight The weight of the font (e.g., 400 for normal, 700 for bold).
     * @param isItalic Whether the font is italic.
     */
    @Immutable
    public class Named(
        private val fontName: String,
        @IntRange(from = 1, to = 1000) public val weight: Int = 400,
        public val isItalic: Boolean = false,
    ) : RemoteTypeface {
        override val name: String = fontName

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Named) return false
            if (name != other.name) return false
            if (weight != other.weight) return false
            if (isItalic != other.isItalic) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + weight
            result = 31 * result + isItalic.hashCode()
            return result
        }

        override fun toString(): String =
            "RemoteTypeface.Named(name='$name', weight=$weight, isItalic=$isItalic)"
    }

    /** Represents the style of a [RemoteTypeface] (e.g., Normal, Bold, Italic). */
    public enum class Style {
        Normal,
        Bold,
        Italic,
        BoldItalic,
    }

    public companion object {
        /**
         * Creates a [RemoteTypeface] with the given [fontName] and [style].
         *
         * @param fontName The font family name, or `null` for the default font.
         * @param style The [Style] of the typeface.
         * @return a [RemoteTypeface] matching [fontName] and [style]
         */
        public fun create(fontName: String?, style: Style = Style.Normal): RemoteTypeface {
            if (fontName == null) {
                return when (style) {
                    Style.Normal -> Default
                    Style.Bold -> DefaultBold
                    Style.Italic -> Named("default", weight = 400, isItalic = true)
                    Style.BoldItalic -> Named("default", weight = 700, isItalic = true)
                }
            }
            val weight = if (style == Style.Bold || style == Style.BoldItalic) 700 else 400
            val isItalic = style == Style.Italic || style == Style.BoldItalic
            return Named(fontName, weight, isItalic)
        }
    }
}
