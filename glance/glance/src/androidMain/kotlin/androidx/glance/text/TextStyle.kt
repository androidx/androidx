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

package androidx.glance.text

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.TextUnit
import androidx.glance.unit.ColorProvider

/**
 * Description of a text style for the [androidx.glance.layout.Text] composable.
 */
@Immutable
class TextStyle(
    val color: ColorProvider? = null,
    val fontSize: TextUnit? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val textAlign: TextAlign? = null,
    val textDecoration: TextDecoration? = null,
) {
    fun copy(
        color: ColorProvider? = this.color,
        fontSize: TextUnit? = this.fontSize,
        fontWeight: FontWeight? = this.fontWeight,
        fontStyle: FontStyle? = this.fontStyle,
        textAlign: TextAlign? = this.textAlign,
        textDecoration: TextDecoration? = this.textDecoration
    ) = TextStyle(
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textAlign = textAlign,
        textDecoration = textDecoration
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TextStyle) return false
        if (color != other.color) return false
        if (fontSize != other.fontSize) return false
        if (fontWeight != other.fontWeight) return false
        if (fontStyle != other.fontStyle) return false
        if (textDecoration != other.textDecoration) return false
        if (textAlign != other.textAlign) return false
        return true
    }

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + fontSize.hashCode()
        result = 31 * result + fontWeight.hashCode()
        result = 31 * result + fontStyle.hashCode()
        result = 31 * result + textDecoration.hashCode()
        result = 31 * result + textAlign.hashCode()
        return result
    }

    override fun toString() =
        "TextStyle(color=$color, fontSize=$fontSize, fontWeight=$fontWeight, " +
            "fontStyle=$fontStyle, textDecoration=$textDecoration, textAlign=$textAlign)"
}
