@file:OptIn(GlanceInternalApi::class)

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

package androidx.glance.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier
import androidx.glance.unit.Sp

/**
 * Adds a text view to the glance view.
 */
@Composable
public fun Text(text: String, modifier: Modifier = Modifier, style: TextStyle? = null) {
    ComposeNode<EmittableText, Applier>(
        factory = ::EmittableText,
        update = {
            this.set(text) { this.text = it }
            this.set(modifier) { this.modifier = it }
            this.set(style) { this.style = it }
        }
    )
}

/**
 * Description of a text style for the [Text] composable.
 */
@Immutable
public class TextStyle(
    public val fontSize: Sp? = null,
    public val fontWeight: FontWeight? = null,
    public val fontStyle: FontStyle? = null,
    public val textDecoration: TextDecoration? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextStyle

        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false
        if (textDecoration != other.textDecoration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fontSize.hashCode()
        result = 31 * result + fontWeight.hashCode()
        result = 31 * result + fontStyle.hashCode()
        result = 31 * result + textDecoration.hashCode()
        return result
    }

    override fun toString() =
        "TextStyle(size=$fontSize, fontWeight=$fontWeight, fontStyle=$fontStyle, " +
            "textDecoration=$textDecoration)"
}

/**
 * Weight of a font.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class FontWeight private constructor(
    /** numerical value for the weight (a number from 0 to 1000) **/
    val value: Int,
) {
    public companion object {
        public val Normal: FontWeight = FontWeight(400)
        public val Medium: FontWeight = FontWeight(500)
        public val Bold: FontWeight = FontWeight(700)
    }
}

/**
 * Describes the style of the font: [Normal]] or [Italic].
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class FontStyle private constructor(private val style: Int) {
    public companion object {
        /** Use the upright glyphs */
        public val Normal: FontStyle = FontStyle(0)

        /** Use glyphs designed for slanting */
        public val Italic: FontStyle = FontStyle(1)

        /** Returns a list of possible values of [FontStyle]. */
        public fun values(): List<FontStyle> = listOf(Normal, Italic)
    }

    override fun toString(): String {
        return when (this) {
            Normal -> "Normal"
            Italic -> "Italic"
            else -> "Invalid"
        }
    }
}

/**
 * Defines a horizontal line to be drawn on the text.
 */
@Suppress("INLINE_CLASS_DEPRECATED")
public inline class TextDecoration internal constructor(private val mask: Int) {
    public companion object {
        public val None: TextDecoration = TextDecoration(0x0)

        /**
         * Draws a horizontal line below the text.
         */
        public val Underline: TextDecoration = TextDecoration(0x1)

        /**
         * Draws a horizontal line over the text.
         *
         * Note: This will have no effect if used on Wear Tiles.
         */
        public val LineThrough: TextDecoration = TextDecoration(0x2)

        /**
         * Creates a decoration that includes all the given decorations.
         *
         * @param decorations The decorations to be added
         */
        public fun combine(decorations: List<TextDecoration>): TextDecoration {
            val mask = decorations.fold(0) { acc, decoration ->
                acc or decoration.mask
            }
            return TextDecoration(mask)
        }
    }
    /**
     * Creates a decoration that includes both of the TextDecorations.
     */
    @Stable
    public operator fun plus(decoration: TextDecoration): TextDecoration {
        return TextDecoration(this.mask or decoration.mask)
    }

    /**
     * Check whether this [TextDecoration] contains the given decoration.
     */
    @Stable
    public operator fun contains(other: TextDecoration): Boolean {
        return (mask or other.mask) == mask
    }

    override fun toString(): String {
        if (mask == 0) {
            return "TextDecoration.None"
        }

        val values: MutableList<String> = mutableListOf()
        if ((mask and Underline.mask) != 0) {
            values.add("Underline")
        }
        if ((mask and LineThrough.mask) != 0) {
            values.add("LineThrough")
        }
        if ((values.size == 1)) {
            return "TextDecoration.${values[0]}"
        }
        return "TextDecoration[${values.joinToString(separator = ", ")}]"
    }
}

@GlanceInternalApi
public class EmittableText : Emittable {
    override var modifier: Modifier = Modifier
    public var text: String = ""
    public var style: TextStyle? = null

    override fun toString(): String = "EmittableText($text, style=$style, modifier=$modifier)"
}
