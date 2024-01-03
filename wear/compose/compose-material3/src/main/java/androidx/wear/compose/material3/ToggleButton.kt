/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * The Wear Material [ToggleButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and selection control. The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots in the middle and a slot for the selection control at the end.
 *
 * The [ToggleButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [ToggleButton] height adjusts to
 * accommodate the contents. The label and secondary label should be consistently aligned.
 *
 * Samples:
 * Example of a ToggleButton with a Checkbox:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithCheckbox
 *
 * Example of a ToggleButton with a Switch:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithSwitch
 *
 * Example of a ToggleButton with a RadioButton:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithRadioButton
 *
 * [ToggleButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [SplitToggleButton] can be obtained from
 * [ToggleButtonDefaults], e.g. [ToggleButtonDefaults.toggleButtonColors].
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is changed.
 * @param selectionControl A slot for providing the button's selection control.
 * Three built-in types of selection control are supported: [Checkbox] ,[RadioButton], and [Switch].
 * @param modifier Modifier to be applied to the [ToggleButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this button's "toggleable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Chip in different [Interaction]s.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button. The
 * contents are expected to be a horizontally and vertically center aligned icon of size
 * 24.dp. In order to correctly render when the Chip is not enabled the
 * icon must set its alpha value to [LocalContentAlpha].
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are expected
 * to be text which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 */
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    selectionControl: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) =
    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = provideScopeContent(
            contentColor = colors.contentColor(enabled = enabled, checked),
            textStyle = MaterialTheme.typography.labelMedium,
            content = label
        ),
        selectionControl = selectionControl,
        modifier = modifier
            .defaultMinSize(minHeight = MIN_HEIGHT)
            .height(IntrinsicSize.Min),
        icon = provideNullableScopeContent(
            contentColor = colors.iconColor(enabled = enabled, checked),
            content = icon
        ),
        secondaryLabel = provideNullableScopeContent(
            contentColor = colors.secondaryContentColor(enabled = enabled, checked),
            textStyle = MaterialTheme.typography.labelSmall,
            content = secondaryLabel
        ),
        background = { isEnabled, isChecked ->
            val backgroundColor =
                colors.containerColor(enabled = isEnabled, checked = isChecked).value

            Modifier.background(backgroundColor)
        },
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = contentPadding,
        shape = shape,
        selectionControlWidth = SELECTION_CONTROL_WIDTH,
        selectionControlHeight = SELECTION_CONTROL_HEIGHT
    )

/**
 * The Wear Material [SplitToggleButton] offers three slots and a specific layout for a label,
 * secondaryLabel and selection control. The secondaryLabel is optional. The items are laid out
 * with a column containing the two label slots and a slot for the selection control at the
 * end.
 *
 * The [SplitToggleButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [SplitToggleButton] height adjusts to
 * accommodate the contents. The label and secondary label should be consistently aligned.
 *
 * A [SplitToggleButton] has two tappable areas, one tap area for the labels and another for the
 * toggle control. The [onClick] listener will be associated with the main body of the split toggle
 * button with the [onCheckedChange] listener associated with the toggle control area only.
 *
 * Samples:
 * Example of a SplitToggleButton with a Checkbox:
 * @sample androidx.wear.compose.material3.samples.SplitToggleButtonWithCheckbox
 *
 * Example of a SplitToggleButton with a Switch:
 * @sample androidx.wear.compose.material3.samples.SplitToggleButtonWithSwitch
 *
 * Example of a SplitToggleButton with a RadioButton:
 * @sample androidx.wear.compose.material3.samples.SplitToggleButtonWithRadioButton
 *
 * For a SplitToggleButton the background of the tappable background area behind the selection control
 * will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from
 * [ToggleButtonDefaults], e.g. [ToggleButtonDefaults.toggleButtonColors].
 *
 * [SplitToggleButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked/selected status is
 * changed.
 * @param onClick Click listener called when the user clicks the main body of the button, the area
 * behind the labels.
 * @param selectionControl A slot for providing the button's selection control.
 * Three built-in types of selection control are supported: [Checkbox] ,[RadioButton], and [Switch].
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param checkedInteractionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this button's "toggleable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this button in different [Interaction]s.
 * @param clickInteractionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this button's "clickable" tap area. You can create and pass in your own
 * remembered [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this button in different [Interaction]s.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are expected
 * to be "start" or "center" aligned. label and secondaryLabel contents should be consistently
 * aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned if there is an icon preset and "start" or "center" aligned if
 * not. label and secondaryLabel contents should be consistently aligned.
 */
@Composable
fun SplitToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    selectionControl: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    checkedInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    clickInteractionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) = androidx.wear.compose.materialcore.SplitToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    label = provideScopeContent(
        contentColor = colors.contentColor(enabled = enabled, checked = checked),
        textStyle = MaterialTheme.typography.labelMedium,
        content = label
    ),
    onClick = onClick,
    selectionControl = selectionControl,
    modifier = modifier
        .defaultMinSize(minHeight = MIN_HEIGHT)
        .height(IntrinsicSize.Min),
    secondaryLabel = provideNullableScopeContent(
        contentColor = colors.secondaryContentColor(enabled = enabled, checked = checked),
        textStyle = MaterialTheme.typography.labelSmall,
        content = secondaryLabel
    ),
    backgroundColor = { isEnabled, isChecked ->
        colors.containerColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    splitBackgroundColor = { isEnabled, isChecked ->
        colors.splitContainerColor(
            enabled = isEnabled,
            checked = isChecked
        )
    },
    enabled = enabled,
    checkedInteractionSource = checkedInteractionSource,
    clickInteractionSource = clickInteractionSource,
    contentPadding = contentPadding,
    shape = shape
)

/**
 * Contains the default values used by [ToggleButton]s and [SplitToggleButton]s
 */
object ToggleButtonDefaults {

    /**
     * Creates a [ToggleButtonColors] for use in a [ToggleButton] and [SplitToggleButton].
     *
     * @param checkedContainerColor The container color of the [ToggleButton] or
     * the [SplitToggleButton] when enabled and checked/selected.
     * @param checkedContentColor The content color of the [ToggleButton]
     * or the [SplitToggleButton] when enabled and checked/selected.
     * @param checkedSecondaryContentColor The secondary content color of the [ToggleButton]
     * or the [SplitToggleButton] when enabled and checked/selected,
     * used for secondaryLabel content.
     * @param checkedIconColor The icon color of the [ToggleButton]
     * when enabled and checked/selected.
     * @param checkedSplitContainerColor The split container color of the [SplitToggleButton]
     * when enabled and checked/selected.
     * @param uncheckedContainerColor  The container color of the [ToggleButton]
     * or the [SplitToggleButton] when enabled and unchecked/not selected.
     * @param uncheckedContentColor The content color of a [ToggleButton]
     * or the [SplitToggleButton] when enabled and unchecked/not selected.
     * @param uncheckedSecondaryContentColor The secondary content color of this [ToggleButton]
     * or the [SplitToggleButton] when enabled and unchecked/not selected,
     * used for secondaryLabel content
     * @param uncheckedIconColor The icon color of the [ToggleButton]
     * when enabled and unchecked/not selected.
     * @param uncheckedSplitContainerColor The split container color
     * of the [SplitToggleButton] when enabled and unchecked/not selected.
     */
    @Composable
    fun toggleButtonColors(
        checkedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        checkedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        checkedSecondaryContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
            alpha = 0.8f
        ),
        checkedIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        checkedSplitContainerColor: Color = MaterialTheme.colorScheme.primary.copy(.15f),
        uncheckedContainerColor: Color = MaterialTheme.colorScheme.surface,
        uncheckedContentColor: Color = MaterialTheme.colorScheme.onSurface,
        uncheckedSecondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        uncheckedIconColor: Color = MaterialTheme.colorScheme.primary,
        uncheckedSplitContainerColor: Color = MaterialTheme.colorScheme.surfaceBright
    ) =
        ToggleButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            checkedSplitContainerColor = checkedSplitContainerColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            uncheckedSplitContainerColor = uncheckedSplitContainerColor,
            disabledCheckedContainerColor = checkedContainerColor.toDisabledColor(),
            disabledCheckedContentColor = checkedContentColor.toDisabledColor(),
            disabledCheckedSecondaryContentColor = checkedSecondaryContentColor.toDisabledColor(),
            disabledCheckedIconColor = checkedIconColor.toDisabledColor(),
            disabledCheckedSplitContainerColor = checkedSplitContainerColor.toDisabledColor(),
            disabledUncheckedContainerColor = uncheckedContainerColor.toDisabledColor(),
            disabledUncheckedContentColor = uncheckedContentColor.toDisabledColor(),
            disabledUncheckedSecondaryContentColor =
            uncheckedSecondaryContentColor.toDisabledColor(),
            disabledUncheckedIconColor = uncheckedIconColor.toDisabledColor(),
            disabledUncheckedSplitContainerColor = uncheckedSplitContainerColor.toDisabledColor()
        )

    private val ChipHorizontalPadding = 14.dp
    private val ChipVerticalPadding = 6.dp

    val ContentPadding: PaddingValues = PaddingValues(
        start = ChipHorizontalPadding,
        top = ChipVerticalPadding,
        end = ChipHorizontalPadding,
        bottom = ChipVerticalPadding
    )
}

/**
 * Represents the different container and content colors used for toggle buttons
 * ([ToggleButton], [IconToggleButton], and [TextToggleButton]) in various states,
 * that are checked, unchecked, enabled and disabled.
 */
@Immutable
class ToggleButtonColors constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedIconColor: Color,
    val checkedSplitContainerColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedIconColor: Color,
    val uncheckedSplitContainerColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledCheckedSplitContainerColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedIconColor: Color,
    val disabledUncheckedSplitContainerColor: Color,
) {
    constructor(
        checkedContainerColor: Color,
        checkedContentColor: Color,
        uncheckedContainerColor: Color,
        uncheckedContentColor: Color,
        disabledCheckedContainerColor: Color,
        disabledCheckedContentColor: Color,
        disabledUncheckedContainerColor: Color,
        disabledUncheckedContentColor: Color
    ) : this(
        checkedContainerColor = checkedContainerColor,
        checkedContentColor = checkedContentColor,
        checkedSecondaryContentColor = checkedContainerColor,
        checkedIconColor = checkedContentColor,
        checkedSplitContainerColor = checkedContentColor,
        uncheckedContainerColor = uncheckedContainerColor,
        uncheckedContentColor = uncheckedContentColor,
        uncheckedSecondaryContentColor = uncheckedContentColor,
        uncheckedIconColor = uncheckedContentColor,
        uncheckedSplitContainerColor = uncheckedContainerColor,
        disabledCheckedContainerColor = disabledCheckedContainerColor,
        disabledCheckedContentColor = disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor = disabledCheckedContentColor,
        disabledCheckedIconColor = disabledCheckedContentColor,
        disabledCheckedSplitContainerColor = disabledCheckedContainerColor,
        disabledUncheckedContainerColor = disabledUncheckedContainerColor,
        disabledUncheckedContentColor = disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor = disabledUncheckedContentColor,
        disabledUncheckedIconColor = disabledUncheckedContentColor,
        disabledUncheckedSplitContainerColor = disabledUncheckedContainerColor,
    )

    /**
     * Determines the container color based on whether the toggle button is [enabled]
     * and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    fun containerColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedContainerColor,
            uncheckedColor = uncheckedContainerColor,
            disabledCheckedColor = disabledCheckedContainerColor,
            disabledUncheckedColor = disabledUncheckedContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Determines the content color based on whether the toggle button is [enabled]
     * and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    fun contentColor(enabled: Boolean, checked: Boolean): State<Color> = animateSelectionColor(
        enabled = enabled,
        checked = checked,
        checkedColor = checkedContentColor,
        uncheckedColor = uncheckedContentColor,
        disabledCheckedColor = disabledCheckedContentColor,
        disabledUncheckedColor = disabledUncheckedContentColor,
        animationSpec = COLOR_ANIMATION_SPEC
    )

    /**
     * Represents the secondary content color depending on the [enabled] and [checked] properties.
     *
     * @param enabled Whether the ToggleButton is enabled.
     * @param checked Whether the ToggleButton is currently checked/selected
     * or unchecked/not selected.
     */
    @Composable
    fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedSecondaryContentColor,
            uncheckedColor = uncheckedSecondaryContentColor,
            disabledCheckedColor = disabledCheckedSecondaryContentColor,
            disabledUncheckedColor = disabledUncheckedSecondaryContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the icon color for the [ToggleButton] depending on the
     * [enabled] and [checked] properties.
     *
     * @param enabled Whether the ToggleButton is enabled.
     * @param checked Whether the ToggleButton is currently checked/selected
     * or unchecked/not selected.
     */
    @Composable
    fun iconColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedIconColor,
            uncheckedColor = uncheckedIconColor,
            disabledCheckedColor = disabledCheckedIconColor,
            disabledUncheckedColor = disabledUncheckedIconColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the split container for the [SplitToggleButton] color depending on the
     * [enabled] and [checked] properties.
     *
     * @param enabled Whether the SplitToggleButton is enabled.
     * @param checked Whether the SplitToggleButton is currently checked/selected
     * or unchecked/not selected.
     */
    @Composable
    fun splitContainerColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedSplitContainerColor,
            uncheckedColor = uncheckedSplitContainerColor,
            disabledCheckedColor = disabledCheckedSplitContainerColor,
            disabledUncheckedColor = disabledUncheckedSplitContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as ToggleButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor !=
            other.disabledCheckedSecondaryContentColor
        ) return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor !=
            other.disabledUncheckedSecondaryContentColor
        ) return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor)
            return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        return result
    }
}

private val SELECTION_CONTROL_WIDTH = 32.dp
private val SELECTION_CONTROL_HEIGHT = 24.dp
private val MIN_HEIGHT = 52.dp

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MEDIUM_1, 0, STANDARD_DECELERATE)
