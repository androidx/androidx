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

package androidx.xr.glimmer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role

/**
 * A Jetpack Compose Glimmer icon toggle button that changes its appearance depending on the
 * [checked] value, and offers a single slot for an icon or image. By default, the button
 * transitions its shape and color between states. It is also recommended to vary the [content] to
 * visually indicate the [checked] state.
 *
 * Icon toggle buttons are used when a compact button is required.
 *
 * The [content] should typically be an [Icon]. If using a custom icon, note that the typical size
 * for the internal icon is 32 x 32 dp. The container has an overall minimum size of 48 x 48 dp.
 *
 * @sample androidx.xr.glimmer.samples.IconToggleButtonSample
 * @param checked a boolean flag indicating whether this icon toggle button is currently checked.
 * @param onCheckedChange a callback to be invoked when this button is clicked, receiving the
 *   inverted [checked] value.
 * @param modifier the [Modifier] to be applied to this icon toggle button.
 * @param shape the [Shape] of this icon toggle button. It is recommended to change the shape
 *   depending on the [checked] state. [IconToggleButtonDefaults] provides both animated and
 *   non-animated versions, see [IconToggleButtonDefaults.animatedShape] and
 *   [IconToggleButtonDefaults.shape] for more details.
 * @param colors the [IconToggleButtonColors] providing color variants for all icon toggle button
 *   states.
 * @param border the border to draw around this icon toggle button.
 * @param enabled controls the enabled state of this icon toggle button. When `false`, this button
 *   will not respond to user input.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this button. You can use this to change the icon toggle button's
 *   appearance or preview it in different states. Note that if `null` is provided, interactions
 *   will still happen internally.
 * @param content the content of this icon toggle button, typically an [Icon].
 */
@Composable
public fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = IconToggleButtonDefaults.animatedShape(checked),
    colors: IconToggleButtonColors = IconToggleButtonDefaults.colors(),
    border: BorderStroke? = SurfaceDefaults.border(),
    enabled: Boolean = true,
    contentPadding: PaddingValues = IconToggleButtonDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val depthEffect =
        SurfaceDepthEffect(
            depthEffect = null,
            focusedDepthEffect = GlimmerTheme.depthEffectLevels.level1,
        )

    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    Box(
        modifier
            .surface(
                shape = shape,
                color = colors.resolveBackgroundColor(checked),
                contentColor = colors.resolveContentColor(checked),
                depthEffect = depthEffect,
                border = border,
                interactionSource = internalInteractionSource,
            )
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = internalInteractionSource,
                onValueChange = onCheckedChange,
            )
            .defaultMinSize(IconButtonDefaults.minimumSize)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(
            LocalIconSize provides IconButtonDefaults.iconSize,
            content = content,
        )
    }
}

/** Contains default values used by [IconToggleButton]s. */
public object IconToggleButtonDefaults {

    /** Default content padding used for an [IconToggleButton]. */
    @get:Composable
    public val contentPadding: PaddingValues
        get() = IconButtonDefaults.contentPadding

    /**
     * Chooses a [Shape] based on the [checked] state and can be used to override the default
     * Glimmer button shapes. Note that it simply switches shapes without animation.
     *
     * If you require an animated version, please refer to [IconToggleButtonDefaults.animatedShape],
     * which uses default Glimmer animations and shapes, or consider creating a custom animated
     * shape.
     *
     * @param checked whether the button is in the checked state
     * @param checkedShape the shape of the button when it is checked
     * @param uncheckedShape the shape of the button when it is unchecked
     */
    @Composable
    public fun shape(
        checked: Boolean,
        checkedShape: Shape = ToggleButtonDefaults.CheckedShape,
        uncheckedShape: Shape = GlimmerTheme.shapes.large,
    ): Shape = if (checked) checkedShape else uncheckedShape

    /**
     * Provides a [Shape] that uses the default Glimmer shapes for buttons and animates transitions
     * between states. If you require an animated transition between your own custom shapes, please
     * consider creating a custom animated shape.
     *
     * @param checked whether the button is in the checked state
     */
    @Composable
    public fun animatedShape(checked: Boolean): Shape {
        val progress =
            animateFloatAsState(
                targetValue = if (checked) 1f else 0f,
                animationSpec = ToggleButtonAnimationSpec,
            )
        return remember(progress) { ToggleButtonAnimatedShape(progress) }
    }

    /**
     * Creates [IconToggleButtonColors] with default values for an [IconToggleButton].
     *
     * @param backgroundColor The background color when the button is unchecked.
     * @param checkedBackgroundColor The background color when the button is checked.
     * @param contentColor The content color when the button is unchecked.
     * @param checkedContentColor The content color when the button is checked.
     */
    @Composable
    public fun colors(
        backgroundColor: Color = GlimmerTheme.colors.surface,
        checkedBackgroundColor: Color = GlimmerTheme.colors.outline,
        contentColor: Color = calculateContentColor(backgroundColor),
        checkedContentColor: Color = calculateContentColor(checkedBackgroundColor),
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            backgroundColor = backgroundColor,
            checkedBackgroundColor = checkedBackgroundColor,
            contentColor = contentColor,
            checkedContentColor = checkedContentColor,
        )
}

/**
 * Represents the colors used by an [IconToggleButton] in different states.
 *
 * @property backgroundColor the background color when the button is unchecked
 * @property checkedBackgroundColor the background color when the button is checked
 * @property contentColor the content color when the button is unchecked
 * @property checkedContentColor the content color when the button is checked
 */
@Immutable
public class IconToggleButtonColors(
    public val backgroundColor: Color,
    public val checkedBackgroundColor: Color,
    public val contentColor: Color,
    public val checkedContentColor: Color,
) {
    internal fun resolveContentColor(checked: Boolean): Color {
        return if (checked) checkedContentColor else contentColor
    }

    internal fun resolveBackgroundColor(checked: Boolean): Color {
        return if (checked) checkedBackgroundColor else backgroundColor
    }

    public fun copy(
        backgroundColor: Color = this.backgroundColor,
        checkedBackgroundColor: Color = this.checkedBackgroundColor,
        contentColor: Color = this.contentColor,
        checkedContentColor: Color = this.checkedContentColor,
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            backgroundColor = backgroundColor,
            checkedBackgroundColor = checkedBackgroundColor,
            contentColor = contentColor,
            checkedContentColor = checkedContentColor,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as IconToggleButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (checkedBackgroundColor != other.checkedBackgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (checkedContentColor != other.checkedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + checkedBackgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        return result
    }
}
