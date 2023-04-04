
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

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.Emittable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode
import androidx.glance.text.TextDefaults.defaultTextStyle
import androidx.glance.unit.ColorProvider

/**
 * Adds a text view to the glance view.
 *
 * @param text The text to be displayed.
 * @param modifier [GlanceModifier] to apply to this layout node.
 * @param style [TextStyle]] configuration for the text such as color, font, text align etc.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Text(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
    style: TextStyle = defaultTextStyle,
    maxLines: Int = Int.MAX_VALUE,
) {
    GlanceNode(
        factory = ::EmittableText,
        update = {
            this.set(text) { this.text = it }
            this.set(modifier) { this.modifier = it }
            this.set(style) { this.style = it }
            this.set(maxLines) { this.maxLines = it }
        }
    )
}

object TextDefaults {
    val defaultTextColor = ColorProvider(Color.Black)
    val defaultTextStyle: TextStyle = TextStyle(color = defaultTextColor)
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableText : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var text: String = ""
    var style: TextStyle? = null
    var maxLines: Int = Int.MAX_VALUE

    override fun copy(): Emittable = EmittableText().also {
        it.modifier = modifier
        it.text = text
        it.style = style
        it.maxLines = maxLines
    }

    override fun toString(): String =
        "EmittableText($text, style=$style, modifier=$modifier, maxLines=$maxLines)"
}
