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

import android.graphics.Typeface
import android.os.Build

/**
 * A sealed interface that represent the concept of a Typeface in Remote Compose.
 *
 * Custom or named system fonts are represented by [Named] type and are registered in the document
 * as Strings to be referenced.
 */
public sealed interface RemoteTypeface {
    /** The name used to identify the font in the document and player */
    public val name: String

    /** Converts this RemoteTypeface to an Android framework Typeface. */
    public fun toAndroidTypeface(): Typeface

    /** Represents the default system font. */
    public object Default : RemoteTypeface {
        override val name: String = "default"

        override fun toAndroidTypeface(): Typeface = Typeface.DEFAULT
    }

    /** Represents the default bold system font. */
    public object DefaultBold : RemoteTypeface {
        override val name: String = "default-bold"

        override fun toAndroidTypeface(): Typeface = Typeface.DEFAULT_BOLD
    }

    /** Represents the sans-serif system font. */
    public object SansSerif : RemoteTypeface {
        override val name: String = "sans-serif"

        override fun toAndroidTypeface(): Typeface = Typeface.SANS_SERIF
    }

    /** Represents the serif system font. */
    public object Serif : RemoteTypeface {
        override val name: String = "serif"

        override fun toAndroidTypeface(): Typeface = Typeface.SERIF
    }

    /** Represents the monospace system font. */
    public object Monospace : RemoteTypeface {
        override val name: String = "monospace"

        override fun toAndroidTypeface(): Typeface = Typeface.MONOSPACE
    }

    /**
     * Represents a system font referenced by name.
     *
     * @param fontName The string name of the font family (e.g., "roboto-flex").
     * @param weight The weight of the font (e.g., 400 for normal, 700 for bold).
     * @param isItalic Whether the font is italic.
     */
    public class Named(
        private val fontName: String,
        public val weight: Int = 400,
        public val isItalic: Boolean = false,
    ) : RemoteTypeface {
        override val name: String = fontName

        override fun toAndroidTypeface(): Typeface {
            val base = Typeface.create(fontName, Typeface.NORMAL)
            return Typeface.create(base, weight, isItalic)
        }

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
    }

    /** Represents the style of a RemoteTypeface (e.g., Normal, Bold, Italic). */
    public enum class Style {
        Normal,
        Bold,
        Italic,
        BoldItalic,
    }

    public companion object {
        /** Creates a RemoteTypeface with the given font name and style. */
        public fun create(fontName: String?, style: Style = Style.Normal): RemoteTypeface {
            val weight = if (style == Style.Bold || style == Style.BoldItalic) 700 else 400
            val isItalic = style == Style.Italic || style == Style.BoldItalic
            return Named(fontName ?: "default", weight, isItalic)
        }

        /** Maps an Android framework Typeface to a RemoteTypeface. */
        public fun fromAndroidTypeface(typeface: Typeface?): RemoteTypeface {
            return when (typeface) {
                null -> Default
                Typeface.DEFAULT -> Default
                Typeface.DEFAULT_BOLD -> DefaultBold
                Typeface.SANS_SERIF -> SansSerif
                Typeface.SERIF -> Serif
                Typeface.MONOSPACE -> Monospace
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val name = typeface.systemFontFamilyName
                        val weight = typeface.weight
                        val italic = typeface.isItalic
                        if (name != null) {
                            Named(name, weight, italic)
                        } else {
                            Default
                        }
                    } else {
                        Default
                    }
                }
            }
        }
    }
}
