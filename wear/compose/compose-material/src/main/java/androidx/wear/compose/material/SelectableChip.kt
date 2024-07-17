/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.wear.compose.material

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A [SelectableChip] is a specialized type of [Chip] that includes a slot for a bi-state selection
 * control such as a radio button. This overload provides suitable accessibility semantics for a
 * selectable control like [RadioButton]. For toggleable controls like [Checkbox] and [Switch], use
 * [ToggleChip] instead.
 *
 * The Wear Material [SelectableChip] offers four slots and a specific layout for an application
 * icon, a label, a secondaryLabel and selection control. The application icon and secondaryLabel
 * are optional. The items are laid out in a row with the optional icon at the start, a column
 * containing the two label slots in the middle and a slot for the selection control at the end.
 *
 * The [SelectableChip] is Stadium shaped and has a max height designed to take no more than two
 * lines of text of [Typography.button] style. With localisation and/or large font sizes, the
 * [SelectableChip] height adjusts to accommodate the contents. The label and secondary label should
 * be consistently aligned.
 *
 * The recommended set of [SelectableChipColors] can be obtained from [SelectableChipDefaults], e.g.
 * [SelectableChipDefaults.selectableChipColors].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [SelectableChip] with an icon, label and secondary label (defaults to radio button):
 *
 * @sample androidx.wear.compose.material.samples.SelectableChipWithRadioButton
 *
 * For more information, see the
 * [Toggle Chips](https://developer.android.com/training/wearables/components/toggle-chips) guide.
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onClick Callback to be invoked when this button is selected.
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 *   which is "start" aligned.
 * @param modifier Modifier to be applied to the chip
 * @param appIcon An optional slot for providing an icon to indicate the purpose of the chip. The
 *   contents are expected to be a horizontally and vertically centre aligned icon of size
 *   [SelectableChipDefaults.IconSize]. In order to correctly render when the Chip is not enabled
 *   the icon must set its alpha value to [LocalContentAlpha].
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 *   to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned
 *   if not. label and secondaryLabel contents should be consistently aligned.
 * @param colors [SelectableChipColors] that will be used to resolve the background and content
 *   color for this chip in different states, see [SelectableChipDefaults.selectableChipColors].
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not be
 *   clickable
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this chip's "selectable" tap area. You can use this to change the
 *   chip's appearance or preview the chip in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param selectionControl A slot for providing the chip's selection control. One built-in type of
 *   selection control is supported, see [RadioButton]. For [Checkbox] and [Switch], use
 *   [ToggleChip] in order to provide the correct semantics for accessibility.
 */
@Composable
public fun SelectableChip(
    selected: Boolean,
    onClick: (Boolean) -> Unit,
    label: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    appIcon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    colors: SelectableChipColors = SelectableChipDefaults.selectableChipColors(),
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = SelectableChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.large,
    selectionControl: @Composable () -> Unit = {
        RadioButton(selected = selected, enabled = enabled)
    }
) =
    androidx.wear.compose.materialcore.ToggleButton(
        checked = selected,
        onCheckedChange = onClick,
        label =
            provideScopeContent(
                contentColor = colors.contentColor(enabled = enabled, selected),
                textStyle = MaterialTheme.typography.button,
                content = label
            ),
        toggleControl = null,
        selectionControl =
            provideContent(
                contentColor = colors.selectionControlColor(enabled, selected),
                content = selectionControl
            ),
        modifier =
            modifier
                .defaultMinSize(minHeight = SelectableChipDefaults.Height)
                .height(IntrinsicSize.Min),
        icon =
            provideNullableScopeContent(
                contentColor = colors.contentColor(enabled = enabled, selected = selected),
                content = appIcon
            ),
        secondaryLabel =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled = enabled, selected),
                textStyle = MaterialTheme.typography.caption2,
                content = secondaryLabel
            ),
        background = { isEnabled, isSelected ->
            val painter = colors.background(enabled = isEnabled, selected = isSelected).value

            Modifier.paint(painter = painter, contentScale = ContentScale.Crop)
        },
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        toggleControlHeight = SELECTABLE_CONTROL_HEIGHT,
        toggleControlWidth = SELECTABLE_CONTROL_WIDTH,
        labelSpacerSize = 0.dp,
        toggleControlSpacing = TOGGLE_CONTROL_SPACING,
        iconSpacing = ICON_SPACING,
        ripple = ripple()
    )

/**
 * A [SplitSelectableChip] is a specialized type of [Chip] that includes a slot for a selection
 * control, such as a radio button. The [SplitSelectableChip] differs from the [SelectableChip] by
 * having two "tappable" areas, one clickable and one selectable.
 *
 * The [SplitSelectableChip] provides suitable accessibility semantics for a selectable control like
 * [RadioButton]. For toggleable controls like [Checkbox] and [Switch], use [SplitToggleChip].
 *
 * The Wear Material [SplitSelectableChip] offers three slots and a specific layout for a label,
 * secondaryLabel and selection control. The secondaryLabel is optional. The items are laid out with
 * a column containing the two label slots and a slot for the selection control at the end.
 *
 * A [SplitSelectableChip] has two tappable areas, one tap area for the labels and another for the
 * selection control. The [onContainerClick] callback will be associated with the main body of the
 * [SplitSelectableChip] with the [onSelectionClick] callback associated with the selection control
 * area only.
 *
 * For a [SplitSelectableChip] the background of the tappable background area behind the selection
 * control will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The [SplitSelectableChip] is Stadium shaped and has a max height designed to take no more than
 * two lines of text of [Typography.button] style. With localisation and/or large font sizes, the
 * [SplitSelectableChip] height adjusts to accommodate the contents. The label and secondary label
 * should be consistently aligned.
 *
 * The recommended set of [SplitSelectableChipColors] can be obtained from [SelectableChipDefaults],
 * e.g. [SelectableChipDefaults.splitSelectableChipColors].
 *
 * Chips can be enabled or disabled. A disabled chip will not respond to click events.
 *
 * Example of a [SplitSelectableChip] with a label and the radio button selection control:
 *
 * @sample androidx.wear.compose.material.samples.SplitSelectableChipWithRadioButton
 *
 * For more information, see the
 * [Toggle Chips](https://developer.android.com/training/wearables/components/toggle-chips) guide.
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelectionClick Callback to be invoked when this button is selected.
 * @param label A slot for providing the chip's main label. The contents are expected to be text
 *   which is "start" aligned.
 * @param onContainerClick Callback to be invoked when the user clicks the main body of the chip,
 *   the area containing the labels.
 * @param modifier Modifier to be applied to the chip
 * @param secondaryLabel A slot for providing the chip's secondary label. The contents are expected
 *   to be "start" or "center" aligned. label and secondaryLabel contents should be consistently
 *   aligned.
 * @param colors [SplitSelectableChipColors] that will be used to resolve the background and content
 *   color for this chip in different states, see
 *   [SelectableChipDefaults.splitSelectableChipColors].
 * @param enabled Controls the enabled state of the chip. When `false`, this chip will not be
 *   clickable
 * @param selectionInteractionSource an optional hoisted [MutableInteractionSource] for observing
 *   and emitting [Interaction]s for this chip's "selectable" tap area. You can use this to change
 *   the chip's appearance or preview the chip in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param containerInteractionSource an optional hoisted [MutableInteractionSource] for observing
 *   and emitting [Interaction]s for this chip's "clickable" tap area. You can use this to change
 *   the chip's appearance or preview the chip in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content
 * @param shape Defines the chip's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme
 * @param selectionControl A slot for providing the chip's selection control. One built-in selection
 *   control is provided, see [RadioButton]. For [Checkbox] and [Switch], use [SplitToggleChip]
 *   instead.
 */
@Composable
public fun SplitSelectableChip(
    selected: Boolean,
    onSelectionClick: (Boolean) -> Unit,
    label: @Composable RowScope.() -> Unit,
    onContainerClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    colors: SplitSelectableChipColors = SelectableChipDefaults.splitSelectableChipColors(),
    enabled: Boolean = true,
    selectionInteractionSource: MutableInteractionSource? = null,
    containerInteractionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = SelectableChipDefaults.ContentPadding,
    shape: Shape = MaterialTheme.shapes.large,
    selectionControl: @Composable BoxScope.() -> Unit = {
        RadioButton(selected = selected, enabled = enabled)
    },
) =
    androidx.wear.compose.materialcore.SplitToggleButton(
        checked = selected,
        onCheckedChange = onSelectionClick,
        label =
            provideScopeContent(
                contentColor = colors.contentColor(enabled = enabled),
                textStyle = MaterialTheme.typography.button,
                content = label
            ),
        onClick = onContainerClick,
        toggleControl = null,
        selectionControl =
            provideScopeContent(
                contentColor = colors.selectionControlColor(enabled = enabled, selected = selected),
                content = selectionControl
            ),
        modifier =
            modifier
                .defaultMinSize(minHeight = SelectableChipDefaults.Height)
                .height(IntrinsicSize.Min),
        secondaryLabel =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled = enabled),
                textStyle = MaterialTheme.typography.caption2,
                content = secondaryLabel
            ),
        backgroundColor = { isEnabled, _ -> colors.backgroundColor(enabled = isEnabled) },
        splitBackgroundColor = { isEnabled, isSelected ->
            colors.splitBackgroundOverlay(enabled = isEnabled, selected = isSelected)
        },
        enabled = enabled,
        checkedInteractionSource = selectionInteractionSource,
        clickInteractionSource = containerInteractionSource,
        onClickLabel = null,
        contentPadding = contentPadding,
        shape = shape,
        labelSpacerSize = 0.dp,
        ripple = ripple()
    )

/** Represents the background and content colors used in [SelectableChip]s in different states. */
@Stable
public interface SelectableChipColors {
    /**
     * Represents the background treatment for this chip, depending on the [enabled] and [selected]
     * properties. Backgrounds are typically a linear gradient when the chip is selected and solid
     * when it is not.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun background(enabled: Boolean, selected: Boolean): State<Painter>

    /**
     * Represents the content color for this chip, depending on the [enabled] and [selected]
     * properties.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun contentColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the secondary content color for this chip, depending on the [enabled] and
     * [selected] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun secondaryContentColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the color for the selection control content for this chip, depending on the
     * [enabled] and [selected] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun selectionControlColor(enabled: Boolean, selected: Boolean): State<Color>
}

/**
 * Represents the background and content colors used in [SplitSelectableChip]s in different states.
 */
@Stable
public interface SplitSelectableChipColors {
    /**
     * Represents the background color for this chip, depending on whether it is [enabled].
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable public fun backgroundColor(enabled: Boolean): State<Color>

    /**
     * Represents the content color for this chip, depending on whether it is [enabled]
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable public fun contentColor(enabled: Boolean): State<Color>

    /**
     * Represents the secondary content color for this chip, depending on whether it is [enabled]
     *
     * @param enabled Whether the chip is enabled
     */
    @Composable public fun secondaryContentColor(enabled: Boolean): State<Color>

    /**
     * Represents the color for the selection control content for this chip, depending on the
     * [enabled] and [selected] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun selectionControlColor(enabled: Boolean, selected: Boolean): State<Color>

    /**
     * Represents the overlay to apply to a split background SplitSelectableChip to distinguish
     * between the two tappable areas. The overlay will be applied to "lighten" the background of
     * area under the selection control, depending on the [enabled] and [selected] properties.
     *
     * @param enabled Whether the chip is enabled
     * @param selected Whether the chip is currently selected or unselected
     */
    @Composable public fun splitBackgroundOverlay(enabled: Boolean, selected: Boolean): State<Color>
}

/** Contains the default values used by [SelectableChip]s and [SplitSelectableChip]s */
public object SelectableChipDefaults {

    /**
     * Creates a [SelectableChipColors] for use in a [SelectableChip]. [SelectableChip]s are
     * expected to have a linear gradient background when selected, similar to a
     * [ChipDefaults.gradientBackgroundChipColors] and a solid neutral background when not selected
     * (similar to a [ChipDefaults.secondaryChipColors])
     *
     * @param selectedStartBackgroundColor The background color used at the start of the gradient of
     *   a [SelectableChip] when enabled and selected.
     * @param selectedEndBackgroundColor The background color used at the end of the gradient of a
     *   [SelectableChip] when enabled and selected.
     * @param selectedContentColor The content color of a [SelectableChip] when enabled and
     *   selected.
     * @param selectedSecondaryContentColor The secondary content color of this [SelectableChip]
     *   when enabled and selected, used for secondaryLabel content
     * @param selectedSelectionControlColor The selection control color of this [SelectableChip]
     *   when enabled and selected, used for selectionControl content
     * @param unselectedStartBackgroundColor The background color used at the start of the gradient
     *   of a [SelectableChip] when enabled and unselected.
     * @param unselectedEndBackgroundColor The background color used at the end of the gradient of a
     *   [SelectableChip] when enabled and unselected.
     * @param unselectedContentColor The content color of a [SelectableChip] when enabled and
     *   unselected.
     * @param unselectedSecondaryContentColor The secondary content color of this [SelectableChip]
     *   when enabled and unselected, used for secondaryLabel content
     * @param unselectedSelectionControlColor The selection control color of this [SelectableChip]
     *   when enabled and unselected.
     * @param gradientDirection Whether the chips gradient should be start to end (indicated by
     *   [LayoutDirection.Ltr]) or end to start (indicated by [LayoutDirection.Rtl]).
     */
    @Suppress("PrimitiveInCollection")
    @Composable
    public fun selectableChipColors(
        selectedStartBackgroundColor: Color =
            MaterialTheme.colors.surface
                .copy(alpha = 0f)
                .compositeOver(MaterialTheme.colors.surface),
        selectedEndBackgroundColor: Color =
            MaterialTheme.colors.primary
                .copy(alpha = 0.5f)
                .compositeOver(MaterialTheme.colors.surface),
        selectedContentColor: Color = MaterialTheme.colors.onSurface,
        selectedSecondaryContentColor: Color = MaterialTheme.colors.onSurfaceVariant,
        selectedSelectionControlColor: Color = MaterialTheme.colors.secondary,
        unselectedStartBackgroundColor: Color = MaterialTheme.colors.surface,
        unselectedEndBackgroundColor: Color = unselectedStartBackgroundColor,
        unselectedContentColor: Color = contentColorFor(selectedEndBackgroundColor),
        unselectedSecondaryContentColor: Color = unselectedContentColor,
        unselectedSelectionControlColor: Color = unselectedContentColor,
        gradientDirection: LayoutDirection = LocalLayoutDirection.current
    ): SelectableChipColors {
        val selectedBackgroundColors: List<Color>
        val disabledSelectedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            selectedBackgroundColors =
                listOf(selectedStartBackgroundColor, selectedEndBackgroundColor)
            disabledSelectedBackgroundColors =
                listOf(
                    selectedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                    selectedEndBackgroundColor.copy(alpha = ContentAlpha.disabled)
                )
        } else {
            selectedBackgroundColors =
                listOf(selectedEndBackgroundColor, selectedStartBackgroundColor)
            disabledSelectedBackgroundColors =
                listOf(
                    selectedEndBackgroundColor.copy(alpha = ContentAlpha.disabled),
                    selectedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                )
        }
        val unselectedBackgroundColors: List<Color>
        val disabledUnselectedBackgroundColors: List<Color>
        if (gradientDirection == LayoutDirection.Ltr) {
            unselectedBackgroundColors =
                listOf(unselectedStartBackgroundColor, unselectedEndBackgroundColor)
            disabledUnselectedBackgroundColors =
                listOf(
                    unselectedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                    unselectedEndBackgroundColor.copy(alpha = ContentAlpha.disabled)
                )
        } else {
            unselectedBackgroundColors =
                listOf(unselectedEndBackgroundColor, unselectedStartBackgroundColor)
            disabledUnselectedBackgroundColors =
                listOf(
                    unselectedEndBackgroundColor.copy(alpha = ContentAlpha.disabled),
                    unselectedStartBackgroundColor.copy(alpha = ContentAlpha.disabled),
                )
        }

        return DefaultSelectableChipColors(
            selectedBackgroundPainter =
                BrushPainter(Brush.linearGradient(selectedBackgroundColors)),
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedIconColor = selectedSelectionControlColor,
            unselectedBackgroundPainter =
                BrushPainter(Brush.linearGradient(unselectedBackgroundColors)),
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedIconColor = unselectedSelectionControlColor,
            disabledSelectedBackgroundPainter =
                BrushPainter(Brush.linearGradient(disabledSelectedBackgroundColors)),
            disabledSelectedContentColor = selectedContentColor.copy(alpha = ContentAlpha.disabled),
            disabledSelectedSecondaryContentColor =
                selectedSecondaryContentColor.copy(alpha = ContentAlpha.disabled),
            disabledSelectedIconColor =
                selectedSelectionControlColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedBackgroundPainter =
                BrushPainter(Brush.linearGradient(disabledUnselectedBackgroundColors)),
            disabledUnselectedContentColor =
                unselectedContentColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedSecondaryContentColor =
                unselectedSecondaryContentColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedIconColor =
                unselectedSelectionControlColor.copy(alpha = ContentAlpha.disabled),
        )
    }

    /**
     * Creates a [SplitSelectableChipColors] for use in a [SplitSelectableChip].
     *
     * @param backgroundColor The background color of this [SplitSelectableChip] when enabled
     * @param contentColor The content color of this [SplitSelectableChip] when enabled.
     * @param secondaryContentColor The secondary content color of this[SplitSelectableChip] when
     *   enabled
     * @param selectedSelectionControlColor The selection control content color of this
     *   [SplitSelectableChip] when enabled.
     * @param unselectedSelectionControlColor The selection control content color of this
     *   [SplitSelectableChip] when enabled.
     * @param splitBackgroundOverlayColor The color to use to lighten/distinguish the background
     *   behind the selection control for a split background chip. A split background chip has two
     *   tappable areas, one for the main body of the chip and one for area around the selection
     *   control icon.
     */
    @Composable
    public fun splitSelectableChipColors(
        backgroundColor: Color = MaterialTheme.colors.surface,
        contentColor: Color = MaterialTheme.colors.onSurface,
        secondaryContentColor: Color = MaterialTheme.colors.onSurfaceVariant,
        selectedSelectionControlColor: Color = MaterialTheme.colors.secondary,
        unselectedSelectionControlColor: Color = contentColor,
        splitBackgroundOverlayColor: Color = Color.White.copy(alpha = 0.05f),
    ): SplitSelectableChipColors {
        return DefaultSplitSelectableChipColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            secondaryContentColor = secondaryContentColor,
            selectedIconColor = selectedSelectionControlColor,
            selectedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            unselectedIconColor = unselectedSelectionControlColor,
            unselectedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            disabledBackgroundColor = backgroundColor.copy(alpha = ContentAlpha.disabled),
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled),
            disabledSecondaryContentColor =
                secondaryContentColor.copy(alpha = ContentAlpha.disabled),
            disabledSelectedIconColor =
                selectedSelectionControlColor.copy(alpha = ContentAlpha.disabled),
            disabledSelectedSplitBackgroundOverlay = splitBackgroundOverlayColor,
            disabledUnselectedIconColor =
                unselectedSelectionControlColor.copy(alpha = ContentAlpha.disabled),
            disabledUnselectedSplitBackgroundOverlay = splitBackgroundOverlayColor,
        )
    }

    private val ChipHorizontalPadding = 14.dp
    private val ChipVerticalPadding = 6.dp

    /** The default content padding used by [SelectableChip] and [SplitSelectableChip] */
    public val ContentPadding: PaddingValues =
        PaddingValues(
            start = ChipHorizontalPadding,
            top = ChipVerticalPadding,
            end = ChipHorizontalPadding,
            bottom = ChipVerticalPadding
        )

    /**
     * The default height applied for the [SelectableChip] or [SplitSelectableChip]. Note that you
     * can override it by applying Modifier.heightIn directly on [SelectableChip] or
     * [SplitSelectableChip].
     */
    public val Height = 52.dp

    /**
     * The default size of app icons or selection controls when used inside a [SelectableChip] or
     * [SplitSelectableChip].
     */
    public val IconSize: Dp = 24.dp
}

/** Default [SelectableChipColors] implementation. */
@Immutable
private class DefaultSelectableChipColors(
    private val selectedBackgroundPainter: Painter,
    private val selectedContentColor: Color,
    private val selectedSecondaryContentColor: Color,
    private val selectedIconColor: Color,
    private val disabledSelectedBackgroundPainter: Painter,
    private val disabledSelectedContentColor: Color,
    private val disabledSelectedSecondaryContentColor: Color,
    private val disabledSelectedIconColor: Color,
    private val unselectedBackgroundPainter: Painter,
    private val unselectedContentColor: Color,
    private val unselectedSecondaryContentColor: Color,
    private val unselectedIconColor: Color,
    private val disabledUnselectedBackgroundPainter: Painter,
    private val disabledUnselectedContentColor: Color,
    private val disabledUnselectedSecondaryContentColor: Color,
    private val disabledUnselectedIconColor: Color,
) : SelectableChipColors {

    @Composable
    override fun background(enabled: Boolean, selected: Boolean): State<Painter> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedBackgroundPainter else unselectedBackgroundPainter
            } else {
                if (selected) disabledSelectedBackgroundPainter
                else disabledUnselectedBackgroundPainter
            }
        )
    }

    @Composable
    override fun contentColor(enabled: Boolean, selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedContentColor else unselectedContentColor
            } else {
                if (selected) disabledSelectedContentColor else disabledUnselectedContentColor
            }
        )
    }

    @Composable
    override fun secondaryContentColor(enabled: Boolean, selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedSecondaryContentColor else unselectedSecondaryContentColor
            } else {
                if (selected) disabledSelectedSecondaryContentColor
                else disabledUnselectedSecondaryContentColor
            }
        )
    }

    @Composable
    override fun selectionControlColor(enabled: Boolean, selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedIconColor else unselectedIconColor
            } else {
                if (selected) disabledSelectedIconColor else disabledUnselectedIconColor
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultSelectableChipColors

        if (selectedBackgroundPainter != other.selectedBackgroundPainter) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedIconColor != other.selectedIconColor) return false
        if (selectedSecondaryContentColor != other.selectedSecondaryContentColor) return false
        if (unselectedBackgroundPainter != other.unselectedBackgroundPainter) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (disabledSelectedBackgroundPainter != other.disabledSelectedBackgroundPainter)
            return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedIconColor != other.disabledSelectedIconColor) return false
        if (disabledSelectedSecondaryContentColor != other.disabledSelectedSecondaryContentColor)
            return false
        if (disabledUnselectedBackgroundPainter != other.disabledUnselectedBackgroundPainter)
            return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (disabledUnselectedIconColor != other.disabledUnselectedIconColor) return false
        if (
            disabledUnselectedSecondaryContentColor != other.disabledUnselectedSecondaryContentColor
        )
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedBackgroundPainter.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedIconColor.hashCode()
        result = 31 * result + unselectedBackgroundPainter.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + disabledSelectedBackgroundPainter.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedIconColor.hashCode()
        result = 31 * result + disabledUnselectedBackgroundPainter.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedIconColor.hashCode()
        return result
    }
}

/** Default [SplitSelectableChipColors] implementation. */
@Immutable
private class DefaultSplitSelectableChipColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val secondaryContentColor: Color,
    private val selectedIconColor: Color,
    private val selectedSplitBackgroundOverlay: Color,
    private val disabledBackgroundColor: Color,
    private val disabledContentColor: Color,
    private val disabledSecondaryContentColor: Color,
    private val disabledSelectedIconColor: Color,
    private val disabledSelectedSplitBackgroundOverlay: Color,
    private val unselectedIconColor: Color,
    private val unselectedSplitBackgroundOverlay: Color,
    private val disabledUnselectedIconColor: Color,
    private val disabledUnselectedSplitBackgroundOverlay: Color,
) : SplitSelectableChipColors {

    @Composable
    override fun backgroundColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) backgroundColor else disabledBackgroundColor)
    }

    @Composable
    override fun contentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(if (enabled) contentColor else disabledContentColor)
    }

    @Composable
    override fun secondaryContentColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) secondaryContentColor else disabledSecondaryContentColor
        )
    }

    @Composable
    override fun selectionControlColor(enabled: Boolean, selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedIconColor else unselectedIconColor
            } else {
                if (selected) disabledSelectedIconColor else disabledUnselectedIconColor
            }
        )
    }

    @Composable
    override fun splitBackgroundOverlay(enabled: Boolean, selected: Boolean): State<Color> {
        return rememberUpdatedState(
            if (enabled) {
                if (selected) selectedSplitBackgroundOverlay else unselectedSplitBackgroundOverlay
            } else {
                if (selected) disabledSelectedSplitBackgroundOverlay
                else disabledUnselectedSplitBackgroundOverlay
            }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as DefaultSplitSelectableChipColors

        if (backgroundColor != other.backgroundColor) return false
        if (contentColor != other.contentColor) return false
        if (selectedIconColor != other.selectedIconColor) return false
        if (selectedSplitBackgroundOverlay != other.selectedSplitBackgroundOverlay) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (unselectedSplitBackgroundOverlay != other.unselectedSplitBackgroundOverlay) return false
        if (disabledBackgroundColor != other.disabledBackgroundColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (disabledSelectedIconColor != other.disabledSelectedIconColor) return false
        if (disabledSecondaryContentColor != other.disabledSecondaryContentColor) return false
        if (disabledSelectedSplitBackgroundOverlay != other.disabledSelectedSplitBackgroundOverlay)
            return false
        if (disabledUnselectedIconColor != other.disabledUnselectedIconColor) return false
        if (
            disabledUnselectedSplitBackgroundOverlay !=
                other.disabledUnselectedSplitBackgroundOverlay
        )
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + secondaryContentColor.hashCode()
        result = 31 * result + selectedIconColor.hashCode()
        result = 31 * result + selectedSplitBackgroundOverlay.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + unselectedSplitBackgroundOverlay.hashCode()
        result = 31 * result + disabledBackgroundColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + disabledSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedIconColor.hashCode()
        result = 31 * result + disabledSelectedSplitBackgroundOverlay.hashCode()
        result = 31 * result + disabledUnselectedIconColor.hashCode()
        result = 31 * result + disabledUnselectedSplitBackgroundOverlay.hashCode()
        return result
    }
}

private val SELECTABLE_CONTROL_HEIGHT = 24.dp
private val SELECTABLE_CONTROL_WIDTH = 24.dp
private val TOGGLE_CONTROL_SPACING = 4.dp
private val ICON_SPACING = 6.dp
