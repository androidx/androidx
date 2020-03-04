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

import androidx.annotation.FloatRange
import androidx.compose.Ambient
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
 * EmphasisLevels represents the different levels of [Emphasis] that can be applied to a component.
 *
 * By default, the [Emphasis] implementation for each level varies depending on the color being
 * emphasized (typically [contentColor]). This ensures that the [Emphasis] has the correct
 * contrast for the background they are on, as [ColorPalette.primary] surfaces typically require
 * higher contrast for the content color than [ColorPalette.surface] surfaces to ensure they are
 * accessible.
 *
 * This typically should not be customized as the default implementation is optimized for
 * correct accessibility and contrast on different surfaces.
 *
 * See [MaterialTheme.emphasisLevels] to retrieve the current [EmphasisLevels]
 */
interface EmphasisLevels {
    /**
     * Emphasis used to express high emphasis, such as for selected text fields.
     */
    @Composable
    val high: Emphasis
    /**
     * Emphasis used to express medium emphasis, such as for placeholder text in a text field.
     */
    @Composable
    val medium: Emphasis
    /**
     * Emphasis used to express disabled state, such as for a disabled button.
     */
    @Composable
    val disabled: Emphasis
}

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
val EmphasisAmbient: Ambient<EmphasisLevels> = staticAmbientOf { DefaultEmphasisLevels }

private object DefaultEmphasisLevels : EmphasisLevels {

    private class AlphaEmphasis(
        private val colorPalette: ColorPalette,
        @FloatRange(from = 0.0, to = 1.0) private val onPrimaryAlpha: Float,
        @FloatRange(from = 0.0, to = 1.0) private val onSurfaceAlpha: Float
    ) : Emphasis {
        override fun emphasize(color: Color): Color {
            val alpha = when (color) {
                colorPalette.onPrimary -> onPrimaryAlpha
                else -> onSurfaceAlpha
            }
            return color.copy(alpha = alpha)
        }
    }

    @Composable
    override val high: Emphasis
        get() = AlphaEmphasis(
            colorPalette = MaterialTheme.colors(),
            onPrimaryAlpha = OnPrimaryAlphaLevels.high,
            onSurfaceAlpha = OnSurfaceAlphaLevels.high
        )

    @Composable
    override val medium: Emphasis
        get() = AlphaEmphasis(
            colorPalette = MaterialTheme.colors(),
            onPrimaryAlpha = OnPrimaryAlphaLevels.medium,
            onSurfaceAlpha = OnSurfaceAlphaLevels.medium
        )

    @Composable
    override val disabled: Emphasis
        get() = AlphaEmphasis(
            colorPalette = MaterialTheme.colors(),
            onPrimaryAlpha = OnPrimaryAlphaLevels.disabled,
            onSurfaceAlpha = OnSurfaceAlphaLevels.disabled
        )
}

private object OnPrimaryAlphaLevels {
    const val high: Float = 1.00f
    const val medium: Float = 0.74f
    const val disabled: Float = 0.38f
}

private object OnSurfaceAlphaLevels {
    const val high: Float = 0.87f
    const val medium: Float = 0.60f
    const val disabled: Float = 0.38f
}
