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

package androidx.glance

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.glance.action.Action
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.text.EmittableText
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Adds a button view to the glance view.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param style The style to be applied to the text in this button.
 * @param colors The colors to use for the background and content of the button.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Button(
    text: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    style: TextStyle? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    maxLines: Int = Int.MAX_VALUE,
) = ButtonElement(text, onClick, modifier, enabled, style, colors, maxLines)

/**
 * Adds a button view to the glance view.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param style The style to be applied to the text in this button.
 * @param colors The colors to use for the background and content of the button.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    style: TextStyle? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    maxLines: Int = Int.MAX_VALUE,
) = ButtonElement(text, action(block = onClick), modifier, enabled, style, colors, maxLines)

@Composable
internal fun ButtonElement(
    text: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    style: TextStyle? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    maxLines: Int = Int.MAX_VALUE,
) {
    var finalModifier = if (enabled) modifier.clickable(onClick) else modifier
    finalModifier = finalModifier.background(colors.backgroundColor)
    val finalStyle =
        style?.copy(color = colors.contentColor) ?: TextStyle(color = colors.contentColor)

    GlanceNode(
        factory = ::EmittableButton,
        update = {
            this.set(text) { this.text = it }
            this.set(finalModifier) { this.modifier = it }
            this.set(finalStyle) { this.style = it }
            this.set(colors) { this.colors = it }
            this.set(enabled) { this.enabled = it }
            this.set(maxLines) { this.maxLines = it }
        }
    )
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmittableButton : Emittable {
    override var modifier: GlanceModifier = GlanceModifier
    var text: String = ""
    var style: TextStyle? = null
    var colors: ButtonColors? = null
    var enabled: Boolean = true
    var maxLines: Int = Int.MAX_VALUE

    override fun copy(): Emittable = EmittableButton().also {
        it.modifier = modifier
        it.text = text
        it.style = style
        it.colors = colors
        it.enabled = enabled
        it.maxLines = maxLines
    }

    override fun toString(): String = "EmittableButton('$text', enabled=$enabled, style=$style, " +
        "colors=$colors modifier=$modifier, maxLines=$maxLines)"
}

/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun EmittableButton.toEmittableText() = EmittableText().also {
    it.modifier = modifier
    it.text = text
    it.style = style
    it.maxLines = maxLines
}

/** Represents the colors used to style a button, prefer this to using the modifier. */
class ButtonColors internal constructor(
    val backgroundColor: ColorProvider,
    val contentColor: ColorProvider
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        return result
    }
}

/**
 * Contains the default values used by [Button].
 */
object ButtonDefaults {
    @Composable
    /**
    * Creates a [ButtonColors] that represents the default background and content colors used in
    * a [Button].
    *
    * @param backgroundColor the background color of this [Button]
    * @param contentColor the content color of this [Button]
    */
    fun buttonColors(
        backgroundColor: ColorProvider = GlanceTheme.colors.primary,
        contentColor: ColorProvider = GlanceTheme.colors.onPrimary
    ) = ButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor
    )
}