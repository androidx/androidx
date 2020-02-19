/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.compose.Composable
import androidx.compose.Immutable
import androidx.compose.staticAmbientOf
import androidx.ui.foundation.ProvideContentColor
import androidx.ui.foundation.contentColor
import androidx.ui.graphics.Color

/**
 * Emphasis allows certain parts of a component to be accentuated, or shown with lower contrast
 * to reflect importance / state inside a component. For example, inside a disabled button, text
 * should have an emphasis level of [EmphasisLevels.disabled], to show that the button is
 * currently not active / able to be interacted with.
 *
 * Emphasis works by adjusting the color provided by [contentColor], so that emphasis levels
 * cascade through a subtree without requiring components to be aware of their context.
 *
 * The default implementations convey emphasis by changing the alpha / opacity of [color], to
 * increase / reduce contrast for a particular element.
 *
 * To set emphasis for a particular subtree, see [ProvideEmphasis].
 *
 * To define the emphasis levels in your application, see [EmphasisLevels] and [EmphasisAmbient]
 * - note that this should not typically be customized, as the default values are optimized for
 * accessibility and contrast on different surfaces.
 *
 * For more information on emphasis and ensuring legibility for content, see
 * [Text legibility](https://material.io/design/color/text-legibility.html)
 */
@Immutable
interface Emphasis {
    /**
     * Applies emphasis to the given [color].
     */
    fun emphasize(color: Color): Color
}

/**
 * Class holding the different levels of [Emphasis] that will be applied to components in a
 * [MaterialTheme].
 *
 * @see MaterialTheme.emphasisLevels
 */
data class EmphasisLevels(
    /**
     * Emphasis used to express high emphasis, such as for selected text fields.
     */
    val high: Emphasis = DefaultHighEmphasis,
    /**
     * Emphasis used to express medium emphasis, such as for placeholder text in a text field.
     */
    val medium: Emphasis = DefaultMediumEmphasis,
    /**
     * Emphasis used to express disabled state, such as for a disabled button.
     */
    val disabled: Emphasis = DefaultDisabledEmphasis
)

/**
 * Applies [emphasis] to [children], by modifying the value of [contentColor].
 *
 * See [MaterialTheme.emphasisLevels] to retrieve the levels of emphasis provided in the theme,
 * so they can be applied with this function.
 *
 * @sample androidx.ui.material.samples.EmphasisSample
 */
@Composable
fun ProvideEmphasis(emphasis: Emphasis, children: @Composable() () -> Unit) {
    val emphasizedColor = emphasis.emphasize(contentColor())
    ProvideContentColor(emphasizedColor, children)
}

/**
 * Ambient containing the current [EmphasisLevels] in this hierarchy.
 */
val EmphasisAmbient = staticAmbientOf { EmphasisLevels() }

/**
 * Default implementation for expressing high emphasis.
 */
private val DefaultHighEmphasis: Emphasis = object : Emphasis {
    override fun emphasize(color: Color) = color.copy(alpha = HighEmphasisAlpha)
}

/**
 * Default implementation for expressing medium emphasis.
 */
private val DefaultMediumEmphasis: Emphasis = object : Emphasis {
    override fun emphasize(color: Color) = color.copy(alpha = MediumEmphasisAlpha)
}

/**
 * Default implementation for expressing disabled emphasis.
 */
private val DefaultDisabledEmphasis: Emphasis = object : Emphasis {
    override fun emphasize(color: Color) = color.copy(alpha = DisabledEmphasisAlpha)
}

private const val HighEmphasisAlpha = 0.87f
private const val MediumEmphasisAlpha = 0.60f
private const val DisabledEmphasisAlpha = 0.38f
