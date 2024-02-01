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
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.material3.tokens.SplitToggleButtonTokens
import androidx.wear.compose.material3.tokens.ToggleButtonTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * The Wear Material [ToggleButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and toggle control (such as [Checkbox] or [Switch]).
 * The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots in the middle and a slot for the toggle control at the end.
 *
 * The [ToggleButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [ToggleButton] height adjusts to
 * accommodate the contents. The label and secondary label should be start aligned.
 *
 * Samples:
 * Example of a ToggleButton with a Checkbox:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithCheckbox
 *
 * Example of a ToggleButton with a Switch:
 * @sample androidx.wear.compose.material3.samples.ToggleButtonWithSwitch
 *
 * [ToggleButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [ToggleButton] colors can be obtained from
 * [ToggleButtonDefaults], e.g. [ToggleButtonDefaults.toggleButtonColors].
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is changed.
 * @param toggleControl A slot for providing the button's toggle control.
 * Two built-in types of toggle control are supported: [Checkbox] and [Switch].
 * @param modifier Modifier to be applied to the [ToggleButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [ToggleButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 * button's appearance or preview the button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button. The
 * contents are expected to be a horizontally and vertically center aligned icon of size
 * 24.dp.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned.
 */
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    toggleControl: @Composable ToggleControlScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ToggleButtonTokens.ContainerShape.value,
    colors: ToggleButtonColors = ToggleButtonDefaults.toggleButtonColors(),
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) =
    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label = provideScopeContent(
            contentColor = colors.contentColor(enabled = enabled, checked),
            textStyle = ToggleButtonTokens.LabelFont.value,
            content = label
        ),
        toggleControl = {
            val scope = remember(enabled, checked) { ToggleControlScope(enabled, checked) }
            toggleControl(scope)
        },
        modifier = modifier
            .defaultMinSize(minHeight = MIN_HEIGHT)
            .height(IntrinsicSize.Min),
        icon = provideNullableScopeContent(
            contentColor = colors.iconColor(enabled = enabled, checked),
            content = icon
        ),
        secondaryLabel = provideNullableScopeContent(
            contentColor = colors.secondaryContentColor(enabled = enabled, checked),
            textStyle = ToggleButtonTokens.SecondaryLabelFont.value,
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
        toggleControlWidth = TOGGLE_CONTROL_WIDTH,
        toggleControlHeight = TOGGLE_CONTROL_HEIGHT,
        ripple = rippleOrFallbackImplementation()
    )

/**
 * The Wear Material [SplitToggleButton] offers three slots and a specific layout for a label,
 * secondaryLabel and toggle control. The secondaryLabel is optional. The items are laid out
 * with a column containing the two label slots and a slot for the toggle control at the
 * end.
 *
 * The [SplitToggleButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [SplitToggleButton] height adjusts to
 * accommodate the contents. The label and secondary label should be start aligned.
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
 * For a SplitToggleButton the background of the tappable background area behind the toggle control
 * will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from
 * [ToggleButtonDefaults], e.g. [ToggleButtonDefaults.splitToggleButtonColors].
 *
 * [SplitToggleButton] can be enabled or disabled. A disabled button will not respond to
 * click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is
 * changed.
 * @param onClick Click listener called when the user clicks the main body of the button, the area
 * behind the labels.
 * @param toggleControl A slot for providing the button's toggle control.
 * Two built-in types of toggle control are supported: [Checkbox] and [Switch].
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [SplitToggleButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param checkedInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 * button's appearance or preview the button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param clickInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button's "clickable" tap area. You can use this to change the
 * button's appearance or preview the button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned.
 */
@Composable
fun SplitToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    toggleControl: @Composable ToggleControlScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SplitToggleButtonTokens.ContainerShape.value,
    colors: SplitToggleButtonColors = ToggleButtonDefaults.splitToggleButtonColors(),
    checkedInteractionSource: MutableInteractionSource? = null,
    clickInteractionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = ToggleButtonDefaults.ContentPadding,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) = androidx.wear.compose.materialcore.SplitToggleButton(
    checked = checked,
    onCheckedChange = onCheckedChange,
    label = provideScopeContent(
        contentColor = colors.contentColor(enabled = enabled, checked = checked),
        textStyle = SplitToggleButtonTokens.LabelFont.value,
        content = label
    ),
    onClick = onClick,
    toggleControl = {
        val scope = remember(enabled, checked) { ToggleControlScope(enabled, checked) }
        toggleControl(scope)
    },
    modifier = modifier
        .defaultMinSize(minHeight = MIN_HEIGHT)
        .height(IntrinsicSize.Min),
    secondaryLabel = provideNullableScopeContent(
        contentColor = colors.secondaryContentColor(enabled = enabled, checked = checked),
        textStyle = SplitToggleButtonTokens.SecondaryLabelFont.value,
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
    shape = shape,
    ripple = rippleOrFallbackImplementation()
)

/**
 * Contains the default values used by [ToggleButton]s and [SplitToggleButton]s
 */
object ToggleButtonDefaults {

    /**
     * Creates a [ToggleButtonColors] for use in a [ToggleButton].
     *
     * @param checkedContainerColor The container color of the [ToggleButton]
     * when enabled and checked.
     * @param checkedContentColor The content color of the [ToggleButton]
     * when enabled and checked.
     * @param checkedSecondaryContentColor The secondary content color of the [ToggleButton]
     * when enabled and checked, used for secondaryLabel content.
     * @param checkedIconColor The icon color of the [ToggleButton]
     * when enabled and checked.
     * @param uncheckedContainerColor  The container color of the [ToggleButton]
     * when enabled and unchecked.
     * @param uncheckedContentColor The content color of a [ToggleButton]
     * when enabled and unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of this [ToggleButton]
     * when enabled and unchecked, used for secondaryLabel content
     * @param uncheckedIconColor The icon color of the [ToggleButton]
     * when enabled and unchecked.
     * @param disabledCheckedContainerColor The container color of the [ToggleButton]
     * when disabled and checked.
     * @param disabledCheckedContentColor The content color of the [ToggleButton]
     * when disabled and checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the
     * [ToggleButton] when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedIconColor The icon color of the [ToggleButton]
     * when disabled and checked.
     * @param disabledUncheckedContainerColor  The container color of the [ToggleButton]
     * when disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of a [ToggleButton]
     * when disabled and unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of this
     * [ToggleButton] when disabled and unchecked, used for secondaryLabel content
     * @param disabledUncheckedIconColor The icon color of the [ToggleButton]
     * when disabled and unchecked.
     */
    @Composable
    fun toggleButtonColors(
        checkedContainerColor: Color = ToggleButtonTokens.CheckedContainerColor.value,
        checkedContentColor: Color = ToggleButtonTokens.CheckedContentColor.value,
        checkedSecondaryContentColor: Color = ToggleButtonTokens.CheckedSecondaryLabelColor.value
            .copy(alpha = ToggleButtonTokens.CheckedSecondaryLabelOpacity),
        checkedIconColor: Color = ToggleButtonTokens.CheckedIconColor.value,
        uncheckedContainerColor: Color = ToggleButtonTokens.UncheckedContainerColor.value,
        uncheckedContentColor: Color = ToggleButtonTokens.UncheckedContentColor.value,
        uncheckedSecondaryContentColor: Color =
            ToggleButtonTokens.UncheckedSecondaryLabelColor.value,
        uncheckedIconColor: Color = ToggleButtonTokens.UncheckedIconColor.value,
        disabledCheckedContainerColor: Color =
            ToggleButtonTokens.DisabledCheckedContainerColor.value.toDisabledColor(
                disabledAlpha = ToggleButtonTokens.DisabledOpacity
            ),
        disabledCheckedContentColor: Color = ToggleButtonTokens.DisabledCheckedContentColor.value
            .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
        disabledCheckedSecondaryContentColor: Color =
            ToggleButtonTokens.DisabledCheckedSecondaryLabelColor.value
                .copy(alpha = ToggleButtonTokens.DisabledCheckedSecondaryLabelOpacity)
                .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
        disabledCheckedIconColor: Color = ToggleButtonTokens.DisabledCheckedIconColor.value
            .toDisabledColor(
                disabledAlpha = ToggleButtonTokens.DisabledOpacity
            ),
        disabledUncheckedContainerColor: Color =
            ToggleButtonTokens.DisabledUncheckedContainerColor.value
                .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
        disabledUncheckedContentColor: Color =
            ToggleButtonTokens.DisabledUncheckedContentColor.value
                .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
        disabledUncheckedSecondaryContentColor: Color =
            ToggleButtonTokens.DisabledUncheckedSecondaryLabelColor.value
                .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
        disabledUncheckedIconColor: Color = ToggleButtonTokens.DisabledUncheckedIconColor.value
            .toDisabledColor(disabledAlpha = ToggleButtonTokens.DisabledOpacity),
    ) =
        ToggleButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
        )

    /**
     * Creates a [SplitToggleButtonColors] for use in a [SplitToggleButton].
     *
     * @param checkedContainerColor The container color of the [SplitToggleButton] when enabled and
     * checked.
     * @param checkedContentColor The content color of the [SplitToggleButton] when enabled and
     * checked.
     * @param checkedSecondaryContentColor The secondary content color of the [SplitToggleButton]
     * when enabled and checked, used for secondaryLabel content.
     * @param checkedSplitContainerColor The split container color of the [SplitToggleButton]
     * when enabled and checked.
     * @param uncheckedContainerColor The container color of the [SplitToggleButton] when enabled
     * and unchecked.
     * @param uncheckedContentColor The content color of the [SplitToggleButton] when enabled and
     * unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of the [SplitToggleButton]
     * when enabled and unchecked, used for secondaryLabel content.
     * @param uncheckedSplitContainerColor The split container color of the [SplitToggleButton] when
     * enabled and unchecked.
     * @param disabledCheckedContainerColor The container color of the [SplitToggleButton] when
     * disabled and checked.
     * @param disabledCheckedContentColor The content color of the [SplitToggleButton] when
     * disabled and checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the
     * [SplitToggleButton] when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedSplitContainerColor The split container color of the [
     * SplitToggleButton] when disabled and checked.
     * @param disabledUncheckedContainerColor The container color of the [SplitToggleButton] when
     * disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of the [SplitToggleButton] when
     * disabled and unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of the
     * [SplitToggleButton] when disabled and unchecked, used for secondaryLabel content.
     * @param disabledUncheckedSplitContainerColor The split container color of the
     * [SplitToggleButton] when disabled and unchecked.
     */
    @Composable
    fun splitToggleButtonColors(
        checkedContainerColor: Color = SplitToggleButtonTokens.CheckedContainerColor.value,
        checkedContentColor: Color = SplitToggleButtonTokens.CheckedContentColor.value,
        checkedSecondaryContentColor: Color = SplitToggleButtonTokens.CheckedSecondaryLabelColor
            .value
            .copy(alpha = SplitToggleButtonTokens.CheckedSecondaryLabelOpacity),
        checkedSplitContainerColor: Color = SplitToggleButtonTokens.CheckedSplitContainerColor
            .value
            .copy(alpha = SplitToggleButtonTokens.CheckedSplitContainerOpacity),
        uncheckedContainerColor: Color = SplitToggleButtonTokens.UncheckedContainerColor.value,
        uncheckedContentColor: Color = SplitToggleButtonTokens.UncheckedContentColor.value,
        uncheckedSecondaryContentColor: Color =
            SplitToggleButtonTokens.UncheckedSecondaryLabelColor.value,
        uncheckedSplitContainerColor: Color =
            SplitToggleButtonTokens.UncheckedSplitContainerColor.value,
        disabledCheckedContainerColor: Color =
            SplitToggleButtonTokens.DisabledCheckedContainerColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledCheckedContentColor: Color =
            SplitToggleButtonTokens.DisabledCheckedContentColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledCheckedSecondaryContentColor: Color =
            SplitToggleButtonTokens.DisabledCheckedSecondaryLabelColor.value
                .copy(alpha = SplitToggleButtonTokens.DisabledCheckedSecondaryLabelOpacity)
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledCheckedSplitContainerColor: Color =
            SplitToggleButtonTokens.DisabledCheckedSplitContainerColor.value
                .copy(alpha = SplitToggleButtonTokens.DisabledCheckedSplitContainerOpacity)
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledUncheckedContainerColor: Color =
            SplitToggleButtonTokens.DisabledUncheckedContainerColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledUncheckedContentColor: Color =
            SplitToggleButtonTokens.DisabledUncheckedContentColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledUncheckedSecondaryContentColor: Color =
            SplitToggleButtonTokens.DisabledUncheckedSecondaryLabelColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity),
        disabledUncheckedSplitContainerColor: Color =
            SplitToggleButtonTokens.DisabledUncheckedSplitContainerColor.value
                .toDisabledColor(disabledAlpha = SplitToggleButtonTokens.DisabledOpacity)
    ) =
        SplitToggleButtonColors(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedSplitContainerColor = checkedSplitContainerColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor = uncheckedSplitContainerColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor = disabledCheckedSplitContainerColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedSplitContainerColor = disabledUncheckedSplitContainerColor
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
 *
 * @constructor [ToggleButtonColors] constructor to be used with [ToggleButton]
 * @param checkedContainerColor Container or background color when the toggle button is checked
 * @param checkedContentColor Color of the content like label when the toggle button is checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 * toggle button is checked
 * @param checkedIconColor Color of the icon when the toggle button is checked
 * @param uncheckedContainerColor Container or background color when the toggle button is unchecked
 * @param uncheckedContentColor Color of the content like label when the toggle button is unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 * the toggle button is unchecked
 * @param uncheckedIconColor Color of the icon when the toggle button is unchecked
 * @param disabledCheckedContainerColor Container or background color when the toggle button is
 * disabled and checked
 * @param disabledCheckedContentColor Color of content like label when the toggle button is
 * disabled and checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 * when the toggle button is disabled and checked
 * @param disabledCheckedIconColor Icon color when the toggle button is disabled and checked
 * @param disabledUncheckedContainerColor Container or background color when the toggle button is
 * disabled and unchecked
 * @param disabledUncheckedContentColor Color of the content like label when the toggle button is
 * disabled and unchecked
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary label
 * when the toggle button is disabled and unchecked
 * @param disabledUncheckedIconColor Icon color when the toggle button is disabled and unchecked
 */
@Immutable
class ToggleButtonColors constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedIconColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedIconColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedIconColor: Color,
) {
    /**
     * [ToggleButtonColors] constructor for [IconToggleButton] and [TextToggleButton].
     *
     * @param checkedContainerColor Container or background color of the toggle button when checked
     * @param checkedContentColor Color of the content (text or icon) of the toggle button when
     * checked
     * @param uncheckedContainerColor Container or background color of the toggle button when
     * unchecked
     * @param uncheckedContentColor Color of the content (text or icon) of the toggle button when
     * unchecked
     * @param disabledCheckedContainerColor Container or background color of the toggle button when
     * disabled and checked
     * @param disabledCheckedContentColor Color of the content (icon or text) toggle button when
     * disabled and unchecked
     * @param disabledUncheckedContainerColor Container or background color of the toggle button
     * when disabled and unchecked
     * @param disabledUncheckedContentColor Color of the content (icon or text) toggle button when
     * disabled and unchecked
     */
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
        uncheckedContainerColor = uncheckedContainerColor,
        uncheckedContentColor = uncheckedContentColor,
        uncheckedSecondaryContentColor = uncheckedContentColor,
        uncheckedIconColor = uncheckedContentColor,
        disabledCheckedContainerColor = disabledCheckedContainerColor,
        disabledCheckedContentColor = disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor = disabledCheckedContentColor,
        disabledCheckedIconColor = disabledCheckedContentColor,
        disabledUncheckedContainerColor = disabledUncheckedContainerColor,
        disabledUncheckedContentColor = disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor = disabledUncheckedContentColor,
        disabledUncheckedIconColor = disabledUncheckedContentColor,
    )

    /**
     * Determines the container color based on whether the toggle button is [enabled]
     * and [checked].
     *
     * @param enabled Whether the toggle button is enabled
     * @param checked Whether the toggle button is checked
     */
    @Composable
    internal fun containerColor(enabled: Boolean, checked: Boolean): State<Color> =
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
    internal fun contentColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
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
     * @param checked Whether the ToggleButton is currently checked or unchecked.
     */
    @Composable
    internal fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color> =
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
     * @param checked Whether the ToggleButton is currently checked or unchecked.
     */
    @Composable
    internal fun iconColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedIconColor,
            uncheckedColor = uncheckedIconColor,
            disabledCheckedColor = disabledCheckedIconColor,
            disabledUncheckedColor = disabledUncheckedIconColor,
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
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor !=
            other.disabledCheckedSecondaryContentColor
        ) return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor !=
            other.disabledUncheckedSecondaryContentColor
        ) return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        return result
    }
}

/**
 * Represents the different colors used in [SplitToggleButton] in different states.
 *
 * @constructor [SplitToggleButtonColors] constructor to be used with [SplitToggleButton]
 * @param checkedContainerColor Container or background color when the split toggle button is
 * checked
 * @param checkedContentColor Color of the content like label when the split toggle button is
 * checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 * split toggle button is checked
 * @param checkedSplitContainerColor Split container color when the split toggle button is checked
 * @param uncheckedContainerColor Container or background color when the split toggle button is
 * unchecked
 * @param uncheckedContentColor Color of the content like label when the split toggle button is
 * unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 * the split toggle button is unchecked
 * @param uncheckedSplitContainerColor Split container color when the split toggle button is
 * unchecked
 * @param disabledCheckedContainerColor Container color when the split toggle button is disabled
 * and checked
 * @param disabledCheckedContentColor Color of the content like label when the split toggle button
 * is disabled and checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 * when the split toggle button is disabled and checked
 * @param disabledCheckedSplitContainerColor Split container color when the split toggle button is
 * disabled and checked
 * @param disabledUncheckedContainerColor Container color when the split toggle button is unchecked
 * and disabled
 * @param disabledUncheckedContentColor Color of the content like label when the split toggle
 * button is unchecked and disabled
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary
 * label when the split toggle button is unchecked and disabled
 * @param disabledUncheckedSplitContainerColor Split container color when the split toggle button
 * is unchecked and disabled
 */
class SplitToggleButtonColors constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedSplitContainerColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedSplitContainerColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedSplitContainerColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedSplitContainerColor: Color,
) {

    /**
     * Determines the container color based on whether the [SplitToggleButton] is [enabled]
     * and [checked].
     *
     * @param enabled Whether the [SplitToggleButton] is enabled
     * @param checked Whether the [SplitToggleButton] is currently checked
     */
    @Composable
    internal fun containerColor(enabled: Boolean, checked: Boolean): State<Color> =
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
     * Determines the content color based on whether the [SplitToggleButton] is [enabled]
     * and [checked].
     *
     * @param enabled Whether the [SplitToggleButton] is enabled
     * @param checked Whether the [SplitToggleButton] is currently checked
     */
    @Composable
    internal fun contentColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedContentColor,
            uncheckedColor = uncheckedContentColor,
            disabledCheckedColor = disabledCheckedContentColor,
            disabledUncheckedColor = disabledUncheckedContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the secondary content color for the [SplitToggleButton] depending on the
     * [enabled] and [checked] properties.
     *
     * @param enabled Whether the [SplitToggleButton] is enabled.
     * @param checked Whether the [SplitToggleButton] is currently checked or unchecked.
     */
    @Composable
    internal fun secondaryContentColor(enabled: Boolean, checked: Boolean): State<Color> =
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
     * Represents the split container for the [SplitToggleButton] color depending on the
     * [enabled] and [checked] properties.
     *
     * @param enabled Whether the [SplitToggleButton] is enabled.
     * @param checked Whether the [SplitToggleButton] is currently checked or unchecked.
     */
    @Composable
    internal fun splitContainerColor(enabled: Boolean, checked: Boolean): State<Color> =
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

        other as SplitToggleButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor !=
            other.disabledCheckedSecondaryContentColor
        ) return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor !=
            other.disabledUncheckedSecondaryContentColor
        ) return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        return result
    }
}

/**
 * [ToggleControlScope] provides enabled and checked properties.
 * This allows toggle controls to omit enabled/checked parameters as they given by the scope.
 *
 * @param isEnabled Controls the enabled state of the toggle control.
 * When `false`, the control is displayed with disabled colors.
 * @param isChecked Indicates whether the control is currently checked.
 */
class ToggleControlScope(
    val isEnabled: Boolean,
    val isChecked: Boolean
)

private val TOGGLE_CONTROL_WIDTH = 32.dp
private val TOGGLE_CONTROL_HEIGHT = 24.dp
private val MIN_HEIGHT = 52.dp

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)
