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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.xr.glimmer.internal.color.withTone

/**
 * A Jetpack Compose Glimmer toggle button that changes its appearance depending on the [checked]
 * value, used for exposing actions to a user. By default, the button transitions its shape and
 * color between states. Unlike [IconToggleButton], its primary content is text.
 *
 * @sample androidx.xr.glimmer.samples.ToggleButtonSample
 *
 * A [ToggleButton] can have optional [leadingIcon] and [trailingIcon] slots, which are used to
 * provide more context about the action. It is also recommended to vary the icons to visually
 * indicate the [checked] state.
 *
 * @sample androidx.xr.glimmer.samples.ToggleButtonWithLeadingIconSample
 * @sample androidx.xr.glimmer.samples.ToggleButtonWithTrailingIconSample
 *
 * There are multiple button size variants. Providing a different [ButtonSize] will affect the
 * default values used inside this button, such as the minimum height. Note that you can still
 * provide a size modifier such as [androidx.compose.foundation.layout.size] to change the layout
 * size of this button, [buttonSize] affects default values and values internal to the button.
 *
 * @sample androidx.xr.glimmer.samples.LargeToggleButtonSample
 *
 * Focused and checked background colors can be customized via [ToggleButtonDefaults.colors] using
 * [ToggleButtonDefaults.focusedBackgroundColor], [ToggleButtonDefaults.checkedBackgroundColor], and
 * [ToggleButtonDefaults.focusedCheckedBackgroundColor]:
 *
 * @sample androidx.xr.glimmer.samples.CustomColorsToggleButtonSample
 * @param checked a boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange A callback to be invoked when this toggle button is clicked, receiving the
 *   inverted [checked] value.
 * @param modifier the [Modifier] to be applied to this button.
 * @param enabled controls the enabled state of this button. When `false`, this button will not
 *   respond to user input.
 * @param buttonSize the size variant of this button, represented as a [ButtonSize]. Changing
 *   [buttonSize] will affect some default values used by this button, but the final resulting size
 *   of the button will still be calculated based on the content of the button and any provided size
 *   modifiers such as [androidx.compose.foundation.layout.size]. For example, setting a 100.dp size
 *   using a size modifier will result in the same layout size regardless of [buttonSize], but the
 *   provided [buttonSize] will affect other properties such as padding values and the size of
 *   icons.
 * @param leadingIcon an optional leading icon to be placed before the [content]. This is typically
 *   an [Icon] tinted with the content color resolved from [colors] by default.
 * @param trailingIcon an optional trailing icon to be placed after the [content]. This is typically
 *   an [Icon] tinted with the content color resolved from [colors] by default.
 * @param shape the [Shape] of this toggle button. It is recommended to change the shape depending
 *   on the [checked] state. [ToggleButtonDefaults] provides both animated and non-animated
 *   versions, see [ToggleButtonDefaults.animatedShape] and [ToggleButtonDefaults.shape] for more
 *   details.
 * @param colors the [ToggleButtonColors] that will be used to resolve the container and content
 *   colors based on the toggle button state.
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content the main content, typically [Text], to display inside this button.
 */
@Composable
public fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.Medium,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ToggleButtonDefaults.animatedShape(checked),
    colors: ToggleButtonColors = ToggleButtonDefaults.colors(),
    contentPadding: PaddingValues = ToggleButtonDefaults.contentPadding(buttonSize),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    // Share the same specs that non-toggleable button uses.
    val iconSize = ButtonDefaults.iconSize
    val iconSpacing = ButtonDefaults.iconSpacing
    val minHeight = ButtonDefaults.minimumHeight(buttonSize)

    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    val color = colors.resolveBackgroundColor(checked)
    val focusedColor = colors.resolveFocusedBackgroundColor(checked)
    val contentColor = colors.resolveContentColor(checked)
    val focusedContentColor = colors.resolveFocusedContentColor(checked)

    CompositionLocalProvider(LocalTextStyle provides GlimmerTheme.typography.bodySmall) {
        Row(
            modifier
                .surface(
                    enabled = enabled,
                    shape = shape,
                    color = color,
                    focusedColor = focusedColor,
                    contentColor = contentColor,
                    focusedContentColor = focusedContentColor,
                    depthEffect = null,
                    interactionSource = internalInteractionSource,
                )
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = internalInteractionSource,
                    onValueChange = onCheckedChange,
                )
                .defaultMinSize(minHeight = minHeight)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(Modifier.padding(end = iconSpacing)) {
                    CompositionLocalProvider(LocalIconSize provides iconSize, content = leadingIcon)
                }
            }
            content()
            if (trailingIcon != null) {
                Box(Modifier.padding(start = iconSpacing)) {
                    CompositionLocalProvider(
                        LocalIconSize provides iconSize,
                        content = trailingIcon,
                    )
                }
            }
        }
    }
}

/** Contains default values used by [ToggleButton]s. */
public object ToggleButtonDefaults {

    /** Default content padding used for a [ToggleButton] with the specified [buttonSize]. */
    @Composable
    public fun contentPadding(buttonSize: ButtonSize): PaddingValues {
        return ButtonDefaults.contentPadding(buttonSize)
    }

    /**
     * Chooses a [Shape] based on the [checked] state and can be used to override the default
     * Glimmer button shapes. Note that it simply switches shapes without animation.
     *
     * If you require an animated version, please refer to [ToggleButtonDefaults.animatedShape],
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
        checkedShape: Shape = CheckedShape,
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
     * Creates [ToggleButtonColors] with default values for a [ToggleButton].
     *
     * @param backgroundColor the background color when the button is unchecked and unfocused
     * @param focusedBackgroundColor the background color when the button is unchecked and focused
     * @param checkedBackgroundColor the background color when the button is checked and unfocused
     * @param focusedCheckedBackgroundColor the background color when the button is checked and
     *   focused. Defaults to a color derived from [checkedBackgroundColor]
     * @param contentColor the content color when the button is unchecked and unfocused
     * @param focusedContentColor the content color when the button is unchecked and focused
     * @param checkedContentColor the content color when the button is checked and unfocused
     * @param focusedCheckedContentColor the content color when the button is checked and focused
     */
    @Composable
    public fun colors(
        backgroundColor: Color = GlimmerTheme.colors.surface,
        focusedBackgroundColor: Color = focusedBackgroundColor(backgroundColor),
        checkedBackgroundColor: Color = checkedBackgroundColor(),
        focusedCheckedBackgroundColor: Color =
            focusedCheckedBackgroundColor(checkedBackgroundColor),
        contentColor: Color = calculateContentColor(backgroundColor),
        focusedContentColor: Color = calculateContentColor(focusedBackgroundColor),
        checkedContentColor: Color = calculateContentColor(checkedBackgroundColor),
        focusedCheckedContentColor: Color = calculateContentColor(focusedCheckedBackgroundColor),
    ): ToggleButtonColors =
        ToggleButtonColors(
            backgroundColor = backgroundColor,
            focusedBackgroundColor = focusedBackgroundColor,
            checkedBackgroundColor = checkedBackgroundColor,
            focusedCheckedBackgroundColor = focusedCheckedBackgroundColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor,
            checkedContentColor = checkedContentColor,
            focusedCheckedContentColor = focusedCheckedContentColor,
        )

    /**
     * Returns the unchecked, focused background [Color] for a [ToggleButton] derived from the
     * provided [baseColor].
     *
     * [baseColor] is not the final color. It is the unchecked, unfocused background color of the
     * button, which is adjusted so that it is suitable for use as a focused background.
     *
     * @param baseColor the unchecked, unfocused background [Color] of the button, such as
     *   [Colors.surface] or a custom background color
     * @return the background [Color] for the button when unchecked and focused
     */
    @Composable
    public fun focusedBackgroundColor(baseColor: Color = GlimmerTheme.colors.surface): Color =
        SurfaceDefaults.focusedColor(baseColor)

    /**
     * Returns the checked, unfocused background [Color] for a [ToggleButton] derived from the
     * provided [baseColor].
     *
     * [baseColor] is not the final color. It is a brand or accent color, which is adjusted to a
     * tone that is suitable for use as a checked background.
     *
     * @param baseColor the brand or accent [Color] to derive from, such as [Colors.primary]
     * @return the background [Color] for the button when checked and unfocused
     */
    @Composable
    public fun checkedBackgroundColor(baseColor: Color = GlimmerTheme.colors.primary): Color =
        baseColor.withTone(newTone = CheckedBackgroundColorTone)

    /**
     * Returns the checked, focused background [Color] for a [ToggleButton] derived from the
     * provided [baseColor].
     *
     * [baseColor] is not the final color. It is the same brand or accent color passed to
     * [checkedBackgroundColor], which is adjusted to a tone that is suitable for use as a checked,
     * focused background.
     *
     * @param baseColor the brand or accent [Color] to derive from, such as [Colors.primary]
     * @return the background [Color] for the button when checked and focused
     */
    @Composable
    public fun focusedCheckedBackgroundColor(
        baseColor: Color = GlimmerTheme.colors.primary
    ): Color = baseColor.withTone(newTone = FocusedCheckedBackgroundColorTone)

    private const val CheckedBackgroundColorTone = 70f
    private const val FocusedCheckedBackgroundColorTone = 82f

    /** Default shape for [ToggleButton] in the checked state. */
    public val CheckedShape: Shape = RoundedCornerShape(20.dp)
}

/**
 * Represents the colors used by a [ToggleButton] in different states.
 *
 * @property backgroundColor the background color when the button is unchecked and unfocused
 * @property focusedBackgroundColor the background color when the button is unchecked and focused
 * @property checkedBackgroundColor the background color when the button is checked and unfocused
 * @property focusedCheckedBackgroundColor the background color when the button is checked and
 *   focused
 * @property contentColor the content color when the button is unchecked and unfocused
 * @property focusedContentColor the content color when the button is unchecked and focused
 * @property checkedContentColor the content color when the button is checked and unfocused
 * @property focusedCheckedContentColor the content color when the button is checked and focused
 */
@Immutable
public class ToggleButtonColors(
    public val backgroundColor: Color,
    public val focusedBackgroundColor: Color,
    public val checkedBackgroundColor: Color,
    public val focusedCheckedBackgroundColor: Color,
    public val contentColor: Color,
    public val focusedContentColor: Color,
    public val checkedContentColor: Color,
    public val focusedCheckedContentColor: Color,
) {

    /** Chooses a content color of the button based on the [checked] state. */
    internal fun resolveContentColor(checked: Boolean): Color {
        return if (checked) checkedContentColor else contentColor
    }

    /** Chooses a focused content color of the button based on the [checked] state. */
    internal fun resolveFocusedContentColor(checked: Boolean): Color {
        return if (checked) focusedCheckedContentColor else focusedContentColor
    }

    /** Chooses a background color of the button based on the [checked] state. */
    internal fun resolveBackgroundColor(checked: Boolean): Color {
        return if (checked) checkedBackgroundColor else backgroundColor
    }

    /** Chooses a focused background color of the button based on the [checked] state. */
    internal fun resolveFocusedBackgroundColor(checked: Boolean): Color {
        return if (checked) focusedCheckedBackgroundColor else focusedBackgroundColor
    }

    /** Returns a copy of this [ToggleButtonColors], optionally overriding some of the values. */
    public fun copy(
        backgroundColor: Color = this.backgroundColor,
        focusedBackgroundColor: Color = this.focusedBackgroundColor,
        checkedBackgroundColor: Color = this.checkedBackgroundColor,
        focusedCheckedBackgroundColor: Color = this.focusedCheckedBackgroundColor,
        contentColor: Color = this.contentColor,
        focusedContentColor: Color = this.focusedContentColor,
        checkedContentColor: Color = this.checkedContentColor,
        focusedCheckedContentColor: Color = this.focusedCheckedContentColor,
    ): ToggleButtonColors =
        ToggleButtonColors(
            backgroundColor = backgroundColor,
            focusedBackgroundColor = focusedBackgroundColor,
            checkedBackgroundColor = checkedBackgroundColor,
            focusedCheckedBackgroundColor = focusedCheckedBackgroundColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor,
            checkedContentColor = checkedContentColor,
            focusedCheckedContentColor = focusedCheckedContentColor,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as ToggleButtonColors

        if (backgroundColor != other.backgroundColor) return false
        if (focusedBackgroundColor != other.focusedBackgroundColor) return false
        if (checkedBackgroundColor != other.checkedBackgroundColor) return false
        if (focusedCheckedBackgroundColor != other.focusedCheckedBackgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (focusedCheckedContentColor != other.focusedCheckedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + focusedBackgroundColor.hashCode()
        result = 31 * result + checkedBackgroundColor.hashCode()
        result = 31 * result + focusedCheckedBackgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + focusedCheckedContentColor.hashCode()
        return result
    }
}

/**
 * A [Shape] that animates its corner sizes based on the [checkedProgress]. This implementation
 * interpolates between the default Glimmer checked and unchecked shapes. For custom shapes, please
 * consider creating your own animated shape.
 */
@Stable
internal class ToggleButtonAnimatedShape(private val checkedProgress: State<Float>) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val minDimension = size.minDimension
        val checkedCornerSizePx = CheckedCornerSize.toPx(size, density)
        val uncheckedCornerSizePx = UncheckedCornerSize.toPx(size, density)
        var currentCornerSizePx =
            lerp(uncheckedCornerSizePx, checkedCornerSizePx, checkedProgress.value)

        if (2 * currentCornerSizePx > minDimension) {
            currentCornerSizePx = minDimension / 2
        }

        val cornerRadius = CornerRadius(currentCornerSizePx)
        val roundRect =
            RoundRect(
                rect = size.toRect(),
                topLeft = cornerRadius,
                topRight = cornerRadius,
                bottomRight = cornerRadius,
                bottomLeft = cornerRadius,
            )

        return Outline.Rounded(roundRect)
    }
}

/** Rounded corner size derived from [ToggleButtonDefaults.CheckedShape]. */
internal val CheckedCornerSize: CornerSize = CornerSize(20.dp)

/** Circle corner size derived from [Shapes.large]. */
internal val UncheckedCornerSize: CornerSize = CornerSize(50)

/** Animation spec for transitions between button shapes. */
internal val ToggleButtonAnimationSpec =
    spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 600f)
