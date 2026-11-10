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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.RemoteRole
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Wear Material [RemoteTextToggleButton] is a filled text toggle button which switches between
 * primary colors and tonal colors depending on [checked] value, and offers a single slot for text.
 *
 * Set the size of the [RemoteTextToggleButton] with
 * [RemoteModifier.size][androidx.compose.remote.creation.compose.modifier.size] to one of the
 * recommended sizes in [RemoteTextToggleButtonDefaults] (for example
 * [RemoteTextToggleButtonDefaults.Size]). The recommended text styles for each corresponding size
 * are [RemoteTextToggleButtonDefaults.textStyle], [RemoteTextToggleButtonDefaults.largeTextStyle]
 * and [RemoteTextToggleButtonDefaults.extraLargeTextStyle].
 *
 * [RemoteTextToggleButton] can be enabled or disabled. A disabled button will not respond to click
 * events. When enabled, the [onCheckedChange] action is triggered on click, and is typically used
 * to flip the [checked] state, for example with `valueChangeAction(checked, !checked)`.
 *
 * Example of a simple [RemoteTextToggleButton]:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextToggleButtonSample
 *
 * Example of a [RemoteTextToggleButton] that changes shape when checked:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextToggleButtonVariantSample
 *
 * Example of a [RemoteTextToggleButton] that changes shape when checked and animates its shape when
 * pressed:
 *
 * @sample androidx.wear.compose.remote.material3.samples.RemoteTextToggleButtonVariantAnimatedSample
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange [RemoteAction] to be performed when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`, this toggle button
 *   will not be clickable. It must be a constant value.
 * @param colors [RemoteTextToggleButtonColors] that will be used to resolve the container and
 *   content color for this toggle button.
 * @param shapes Defines the shapes for this toggle button in the checked and unchecked states, and
 *   while pressed. Defaults to a static shape based on [RemoteTextToggleButtonDefaults.shape]. A
 *   variant that changes shape when checked is available through
 *   [RemoteTextToggleButtonDefaults.variantShapes], and shapes that animate when pressed are
 *   available through [RemoteTextToggleButtonDefaults.animatedShapes] and
 *   [RemoteTextToggleButtonDefaults.variantAnimatedShapes]. The press animation is driven by touch
 *   events on the player, and is not shown by players that don't dispatch them.
 * @param border Optional [RemoteBorderStroke] for this toggle button.
 * @param content The text to be drawn inside the toggle button.
 */
@Composable
@RemoteComposable
public fun RemoteTextToggleButton(
    checked: RemoteBoolean,
    onCheckedChange: RemoteAction,
    modifier: RemoteModifier = RemoteModifier,
    enabled: RemoteBoolean = true.rb,
    colors: RemoteTextToggleButtonColors = RemoteTextToggleButtonDefaults.colors(),
    shapes: RemoteTextToggleButtonShapes = RemoteTextToggleButtonDefaults.shapes(),
    border: RemoteBorderStroke? = null,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val (shape, pressedShape) =
        resolveToggleButtonShapes(
            checked = checked,
            uncheckedShape = shapes.uncheckedShape,
            checkedShape = shapes.checkedShape,
            uncheckedPressedShape = shapes.uncheckedPressedShape,
            checkedPressedShape = shapes.checkedPressedShape,
        )
    val checkedProgress = animatedCheckedProgress(checked)
    RemoteRoundButton(
        onClick = onCheckedChange,
        modifier =
            modifier.defaultMinSize(
                minWidth = RemoteTextToggleButtonDefaults.Size,
                minHeight = RemoteTextToggleButtonDefaults.Size,
            ),
        backgroundColor =
            colors.containerColor(
                enabled = enabled,
                checked = checked,
                checkedProgress = checkedProgress,
            ),
        enabled = enabled,
        border = border,
        shape = shape,
        pressedShape = pressedShape,
        pressAnimationSpec = RemoteMaterialTheme.motionScheme.fastEffectsSpec(),
        releaseAnimationSpec = RemoteMaterialTheme.motionScheme.defaultSpatialSpec(),
        role = RemoteRole.Checkbox,
        content =
            provideScopeContent(
                colors.contentColor(
                    enabled = enabled,
                    checked = checked,
                    checkedProgress = checkedProgress,
                ),
                RemoteTextToggleButtonDefaults.textStyle,
                content,
            ),
    )
}

/** Contains the default values used by [RemoteTextToggleButton]. */
public object RemoteTextToggleButtonDefaults {
    /** Recommended [RemoteShape] for [RemoteTextToggleButton]. */
    public val shape: RemoteRoundedCornerShape = RemoteCircleShape

    /**
     * Recommended checked [RemoteShape] for [RemoteTextToggleButton] when using [variantShapes] or
     * [variantAnimatedShapes].
     */
    public val checkedShape: RemoteCornerBasedShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.medium

    /**
     * Recommended pressed [RemoteShape] for [RemoteTextToggleButton] when using [animatedShapes].
     */
    public val pressedShape: RemoteCornerBasedShape
        @Composable @RemoteComposable get() = RemoteMaterialTheme.shapes.small

    /** The default size applied for [RemoteTextToggleButton]. */
    public val Size: RemoteDp = 52.rdp

    /** The recommended size for a large [RemoteTextToggleButton]. */
    public val LargeSize: RemoteDp = 60.rdp

    /** The recommended size for an extra large [RemoteTextToggleButton]. */
    public val ExtraLargeSize: RemoteDp = 72.rdp

    /** The default text style applied for [RemoteTextToggleButton] of size [Size]. */
    public val textStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

    /** The recommended text style for a [RemoteTextToggleButton] of size [LargeSize]. */
    public val largeTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelMedium

    /** The recommended text style for a [RemoteTextToggleButton] of size [ExtraLargeSize]. */
    public val extraLargeTextStyle: RemoteTextStyle
        @Composable @RemoteComposable get() = RemoteMaterialTheme.typography.labelLarge

    /**
     * Creates a [RemoteTextToggleButtonShapes] with a static [shape] that doesn't change between
     * the checked and unchecked states.
     *
     * @return a [RemoteTextToggleButtonShapes] with the default static [shape]
     */
    @Composable @RemoteComposable public fun shapes(): RemoteTextToggleButtonShapes = shapes(shape)

    /**
     * Creates a [RemoteTextToggleButtonShapes] with a static [shape] that doesn't change between
     * the checked and unchecked states.
     *
     * @param shape The shape of the toggle button.
     * @return a [RemoteTextToggleButtonShapes] with the given static [shape]
     */
    @Composable
    @RemoteComposable
    public fun shapes(shape: RemoteShape): RemoteTextToggleButtonShapes =
        RemoteTextToggleButtonShapes(uncheckedShape = shape)

    /**
     * Creates a [RemoteTextToggleButtonShapes] where the toggle button changes from
     * [uncheckedShape] to [checkedShape] when checked.
     *
     * The shape is selected at playback time when [RemoteTextToggleButton]'s `checked` is not a
     * constant, which requires both shapes to be [RemoteRoundedCornerShape]s.
     *
     * @param uncheckedShape The shape of the toggle button when unchecked.
     * @param checkedShape The shape of the toggle button when checked.
     * @return a [RemoteTextToggleButtonShapes] with the configured unchecked and checked shapes
     */
    @Composable
    @RemoteComposable
    public fun variantShapes(
        uncheckedShape: RemoteCornerBasedShape = shape,
        checkedShape: RemoteCornerBasedShape = this.checkedShape,
    ): RemoteTextToggleButtonShapes =
        RemoteTextToggleButtonShapes(uncheckedShape = uncheckedShape, checkedShape = checkedShape)

    /**
     * Creates a [RemoteTextToggleButtonShapes] with a static [shape] that animates to
     * [pressedShape] when pressed.
     *
     * The press animation requires both shapes to be [RemoteRoundedCornerShape]s. It is driven by
     * touch events on the player, and is not shown by players that don't dispatch them.
     *
     * @param shape The shape of the toggle button when not pressed.
     * @param pressedShape The shape of the toggle button when pressed.
     * @return a [RemoteTextToggleButtonShapes] with the configured resting and pressed shapes
     */
    @Composable
    @RemoteComposable
    public fun animatedShapes(
        shape: RemoteCornerBasedShape = this.shape,
        pressedShape: RemoteCornerBasedShape = this.pressedShape,
    ): RemoteTextToggleButtonShapes =
        RemoteTextToggleButtonShapes(
            uncheckedShape = shape,
            checkedShape = shape,
            uncheckedPressedShape = pressedShape,
            checkedPressedShape = pressedShape,
        )

    /**
     * Creates a [RemoteTextToggleButtonShapes] where the toggle button changes from
     * [uncheckedShape] to [checkedShape] when checked, and animates to a shape with smaller corners
     * when pressed.
     *
     * Dynamic shape selection and the press animation require both shapes to be
     * [RemoteRoundedCornerShape]s. The press animation is driven by touch events on the player, and
     * is not shown by players that don't dispatch them.
     *
     * @param uncheckedShape The shape of the toggle button when unchecked.
     * @param checkedShape The shape of the toggle button when checked.
     * @return a [RemoteTextToggleButtonShapes] with the configured unchecked, checked and pressed
     *   shapes
     */
    @Composable
    @RemoteComposable
    public fun variantAnimatedShapes(
        uncheckedShape: RemoteCornerBasedShape = shape,
        checkedShape: RemoteCornerBasedShape = this.checkedShape,
    ): RemoteTextToggleButtonShapes =
        RemoteTextToggleButtonShapes(
            uncheckedShape = uncheckedShape,
            checkedShape = checkedShape,
            uncheckedPressedShape = uncheckedShape.scaleCorners(PressedShapeCornerSizeFraction),
            checkedPressedShape = checkedShape.scaleCorners(PressedShapeCornerSizeFraction),
        )

    /**
     * Creates a [RemoteTextToggleButtonColors] for a [RemoteTextToggleButton] - by default, a
     * primary container when checked and a tonal container when unchecked.
     *
     * @return a [RemoteTextToggleButtonColors] with the default colors for [RemoteTextToggleButton]
     */
    @Composable
    @RemoteComposable
    public fun colors(): RemoteTextToggleButtonColors =
        RemoteMaterialTheme.colorScheme.defaultTextToggleButtonColors

    /**
     * Creates a [RemoteTextToggleButtonColors] for a [RemoteTextToggleButton] - by default, a
     * primary container when checked and a tonal container when unchecked.
     *
     * @param checkedContainerColor The container color of this toggle button when enabled and
     *   checked, or `null` to use the theme default.
     * @param checkedContentColor The content color of this toggle button when enabled and checked,
     *   or `null` to use the theme default.
     * @param uncheckedContainerColor The container color of this toggle button when enabled and
     *   unchecked, or `null` to use the theme default.
     * @param uncheckedContentColor The content color of this toggle button when enabled and
     *   unchecked, or `null` to use the theme default.
     * @param disabledCheckedContainerColor The container color of this toggle button when checked
     *   and not enabled, or `null` to use the theme default.
     * @param disabledCheckedContentColor The content color of this toggle button when checked and
     *   not enabled, or `null` to use the theme default.
     * @param disabledUncheckedContainerColor The container color of this toggle button when
     *   unchecked and not enabled, or `null` to use the theme default.
     * @param disabledUncheckedContentColor The content color of this toggle button when unchecked
     *   and not enabled, or `null` to use the theme default.
     * @return a [RemoteTextToggleButtonColors] with the configured colors for
     *   [RemoteTextToggleButton]
     */
    @Composable
    @RemoteComposable
    public fun colors(
        checkedContainerColor: RemoteColor? = null,
        checkedContentColor: RemoteColor? = null,
        uncheckedContainerColor: RemoteColor? = null,
        uncheckedContentColor: RemoteColor? = null,
        disabledCheckedContainerColor: RemoteColor? = null,
        disabledCheckedContentColor: RemoteColor? = null,
        disabledUncheckedContainerColor: RemoteColor? = null,
        disabledUncheckedContentColor: RemoteColor? = null,
    ): RemoteTextToggleButtonColors {
        val default = RemoteMaterialTheme.colorScheme.defaultTextToggleButtonColors
        return default.copy(
            checkedContainerColor = checkedContainerColor ?: default.checkedContainerColor,
            checkedContentColor = checkedContentColor ?: default.checkedContentColor,
            uncheckedContainerColor = uncheckedContainerColor ?: default.uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor ?: default.uncheckedContentColor,
            disabledCheckedContainerColor =
                disabledCheckedContainerColor ?: default.disabledCheckedContainerColor,
            disabledCheckedContentColor =
                disabledCheckedContentColor ?: default.disabledCheckedContentColor,
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor ?: default.disabledUncheckedContainerColor,
            disabledUncheckedContentColor =
                disabledUncheckedContentColor ?: default.disabledUncheckedContentColor,
        )
    }

    private val RemoteColorScheme.defaultTextToggleButtonColors: RemoteTextToggleButtonColors
        @Composable
        @RemoteComposable
        get() =
            RemoteTextToggleButtonColors(
                checkedContainerColor = primary,
                checkedContentColor = onPrimary,
                uncheckedContainerColor = surfaceContainer,
                uncheckedContentColor = onSurfaceVariant,
                disabledCheckedContainerColor = onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledCheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
                disabledUncheckedContainerColor =
                    onSurface.toDisabledColor(disabledAlpha = 0.12f.rf),
                disabledUncheckedContentColor = onSurface.toDisabledColor(disabledAlpha = 0.38f.rf),
            )
}

/**
 * Represents the different container and content colors used for [RemoteTextToggleButton] in
 * various states, that are checked, unchecked, enabled and disabled.
 *
 * See [RemoteTextToggleButtonDefaults.colors] for the default colors used in a
 * [RemoteTextToggleButton].
 *
 * @param checkedContainerColor Container or background color when the toggle button is checked
 * @param checkedContentColor Color of the content (text) when the toggle button is checked
 * @param uncheckedContainerColor Container or background color when the toggle button is unchecked
 * @param uncheckedContentColor Color of the content (text) when the toggle button is unchecked
 * @param disabledCheckedContainerColor Container or background color when the toggle button is
 *   disabled and checked
 * @param disabledCheckedContentColor Color of content (text) when the toggle button is disabled and
 *   checked
 * @param disabledUncheckedContainerColor Container or background color when the toggle button is
 *   disabled and unchecked
 * @param disabledUncheckedContentColor Color of the content (text) when the toggle button is
 *   disabled and unchecked
 */
@Immutable
public class RemoteTextToggleButtonColors(
    public val checkedContainerColor: RemoteColor,
    public val checkedContentColor: RemoteColor,
    public val uncheckedContainerColor: RemoteColor,
    public val uncheckedContentColor: RemoteColor,
    public val disabledCheckedContainerColor: RemoteColor,
    public val disabledCheckedContentColor: RemoteColor,
    public val disabledUncheckedContainerColor: RemoteColor,
    public val disabledUncheckedContentColor: RemoteColor,
) {
    /**
     * Returns a copy of this [RemoteTextToggleButtonColors] optionally overriding some of the
     * values.
     *
     * @param checkedContainerColor Container or background color when the toggle button is checked
     * @param checkedContentColor Color of the content (text) when the toggle button is checked
     * @param uncheckedContainerColor Container or background color when the toggle button is
     *   unchecked
     * @param uncheckedContentColor Color of the content (text) when the toggle button is unchecked
     * @param disabledCheckedContainerColor Container or background color when the toggle button is
     *   disabled and checked
     * @param disabledCheckedContentColor Color of content (text) when the toggle button is disabled
     *   and checked
     * @param disabledUncheckedContainerColor Container or background color when the toggle button
     *   is disabled and unchecked
     * @param disabledUncheckedContentColor Color of the content (text) when the toggle button is
     *   disabled and unchecked
     * @return a copy of this [RemoteTextToggleButtonColors] with the specified values overridden
     */
    public fun copy(
        checkedContainerColor: RemoteColor = this.checkedContainerColor,
        checkedContentColor: RemoteColor = this.checkedContentColor,
        uncheckedContainerColor: RemoteColor = this.uncheckedContainerColor,
        uncheckedContentColor: RemoteColor = this.uncheckedContentColor,
        disabledCheckedContainerColor: RemoteColor = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: RemoteColor = this.disabledCheckedContentColor,
        disabledUncheckedContainerColor: RemoteColor = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: RemoteColor = this.disabledUncheckedContentColor,
    ): RemoteTextToggleButtonColors =
        RemoteTextToggleButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
        )

    /**
     * Resolves the container color. When [checkedProgress] is not `null`, the color is interpolated
     * between the unchecked and checked colors by it, instead of being selected from [checked].
     *
     * @return the resolved container [RemoteColor]
     */
    @Stable
    internal fun containerColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        checkedProgress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                checkedColor(
                    checked,
                    checkedProgress,
                    checkedContainerColor,
                    uncheckedContainerColor,
                ),
            ifFalse =
                checkedColor(
                    checked,
                    checkedProgress,
                    disabledCheckedContainerColor,
                    disabledUncheckedContainerColor,
                ),
        )

    /**
     * Resolves the content color. When [checkedProgress] is not `null`, the color is interpolated
     * between the unchecked and checked colors by it, instead of being selected from [checked].
     *
     * @return the resolved content [RemoteColor]
     */
    @Stable
    internal fun contentColor(
        enabled: RemoteBoolean,
        checked: RemoteBoolean,
        checkedProgress: RemoteFloat? = null,
    ): RemoteColor =
        enabled.select(
            ifTrue =
                checkedColor(checked, checkedProgress, checkedContentColor, uncheckedContentColor),
            ifFalse =
                checkedColor(
                    checked,
                    checkedProgress,
                    disabledCheckedContentColor,
                    disabledUncheckedContentColor,
                ),
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteTextToggleButtonColors) return false

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        return result
    }
}

/**
 * Represents the shapes used for [RemoteTextToggleButton] in the checked and unchecked states, and
 * while pressed.
 *
 * When the `checked` state of [RemoteTextToggleButton] is not a constant, a change of shape between
 * [uncheckedShape] and [checkedShape] is only supported when both are [RemoteRoundedCornerShape]s;
 * otherwise [uncheckedShape] is used.
 *
 * When the pressed shapes differ from the resting shapes, [RemoteTextToggleButton] animates to
 * [uncheckedPressedShape] or [checkedPressedShape] while it is pressed. The press animation
 * requires all the shapes to be [RemoteRoundedCornerShape]s and the toggle button to be enabled
 * with a constant value.
 *
 * @param uncheckedShape The shape of the toggle button when unchecked.
 * @param checkedShape The shape of the toggle button when checked.
 * @param uncheckedPressedShape The shape of the toggle button when unchecked and pressed.
 * @param checkedPressedShape The shape of the toggle button when checked and pressed.
 */
@Immutable
public class RemoteTextToggleButtonShapes(
    public val uncheckedShape: RemoteShape,
    public val checkedShape: RemoteShape = uncheckedShape,
    public val uncheckedPressedShape: RemoteShape = uncheckedShape,
    public val checkedPressedShape: RemoteShape = checkedShape,
) {
    /**
     * Returns a copy of this [RemoteTextToggleButtonShapes] optionally overriding some of the
     * values.
     *
     * @param uncheckedShape The shape of the toggle button when unchecked.
     * @param checkedShape The shape of the toggle button when checked.
     * @param uncheckedPressedShape The shape of the toggle button when unchecked and pressed.
     * @param checkedPressedShape The shape of the toggle button when checked and pressed.
     * @return a copy of this [RemoteTextToggleButtonShapes] with the specified values overridden
     */
    public fun copy(
        uncheckedShape: RemoteShape = this.uncheckedShape,
        checkedShape: RemoteShape = this.checkedShape,
        uncheckedPressedShape: RemoteShape = this.uncheckedPressedShape,
        checkedPressedShape: RemoteShape = this.checkedPressedShape,
    ): RemoteTextToggleButtonShapes =
        RemoteTextToggleButtonShapes(
            uncheckedShape = uncheckedShape,
            checkedShape = checkedShape,
            uncheckedPressedShape = uncheckedPressedShape,
            checkedPressedShape = checkedPressedShape,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteTextToggleButtonShapes) return false

        if (uncheckedShape != other.uncheckedShape) return false
        if (checkedShape != other.checkedShape) return false
        if (uncheckedPressedShape != other.uncheckedPressedShape) return false
        if (checkedPressedShape != other.checkedPressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uncheckedShape.hashCode()
        result = 31 * result + checkedShape.hashCode()
        result = 31 * result + uncheckedPressedShape.hashCode()
        result = 31 * result + checkedPressedShape.hashCode()
        return result
    }
}
