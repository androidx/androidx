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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.MotionTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * The Wear Material [RadioButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and selection control (such as [Radio]).
 * The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots in the middle and a slot for the selection control at the end.
 *
 * The [RadioButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [RadioButton] height adjusts to
 * accommodate the contents. The label and secondary label should be start aligned.
 *
 * Note that Modifier.selectableGroup() must be present on the parent control (such as
 * Column) to ensure correct accessibility behavior.
 *
 * Samples:
 * Example of a RadioButton with a [Radio] control:
 * @sample androidx.wear.compose.material3.samples.RadioButton
 *
 * [RadioButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [RadioButton] colors can be obtained from
 * [RadioButtonDefaults], e.g. [RadioButtonDefaults.radioButtonColors].
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelected Callback to be invoked when this button has been selected by clicking.
 * @param selectionControl A slot for the button's selection control.
 * The [Radio] selection control is provided for this purpose.
 * @param modifier Modifier to be applied to the [RadioButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [RadioButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this radio button. You can use this to change the radio button's
 * appearance or preview the radio button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button.
 * The contents are expected to be center-aligned, both horizontally and vertically, and should be
 * an icon of size 24.dp. In order to correctly render when the Button is not enabled the
 * icon must set its alpha value to [LocalContentAlpha].
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 * expected to be text which is "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 * which is "start" aligned.
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectionControl: @Composable SelectionControlScope.() -> Unit = {
        Radio()
    },
    shape: Shape = MaterialTheme.shapes.large,
    colors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    contentPadding: PaddingValues = RadioButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    // Stadium/Pill shaped toggle button
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = MIN_HEIGHT)
            .height(IntrinsicSize.Min)
            .width(IntrinsicSize.Max)
            .clip(shape = shape)
            .background(colors.containerColor(enabled = enabled, selected = selected).value)
            .selectable(
                enabled = enabled,
                selected = selected,
                onClick = onSelected,
                indication = rippleOrFallbackImplementation(),
                interactionSource = interactionSource
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                content = provideScopeContent(
                    color = colors.iconColor(enabled = enabled, selected = selected),
                    content = icon
                )
            )
            Spacer(modifier = Modifier.size(ICON_SPACING))
        }
        Labels(
            label = provideScopeContent(
                contentColor = colors.contentColor(enabled = enabled, selected = selected),
                textStyle = MaterialTheme.typography.labelMedium,
                content = label
            ),
            secondaryLabel = provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled = enabled, selected = selected),
                textStyle = MaterialTheme.typography.labelSmall,
                content = secondaryLabel
            )
        )
        Spacer(
            modifier = Modifier.size(
                SELECTION_CONTROL_SPACING
            )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(width = SELECTION_CONTROL_WIDTH, height = SELECTION_CONTROL_HEIGHT)
                .wrapContentWidth(align = Alignment.End),
        ) {
            val scope = remember(enabled, selected) { SelectionControlScope(enabled, selected) }
            selectionControl(scope)
        }
    }
}

/**
 * The Wear Material [SplitRadioButton] offers three slots and a specific layout for a label,
 * secondaryLabel and selection control. The secondaryLabel is optional. The items are laid out
 * with a column containing the two label slots and a slot for the selection control at the
 * end.
 *
 * The [SplitRadioButton] is Stadium shaped and has a max height designed to take no more than
 * two lines of text.
 * With localisation and/or large font sizes, the [SplitRadioButton] height adjusts to
 * accommodate the contents. The label and secondary label should be consistently aligned.
 *
 * A [SplitRadioButton] has two tappable areas, one tap area for the labels and another for the
 * selection control. The [onClick] listener will be associated with the main body of the
 * split radio button with the [onSelected] listener associated with the selection control
 * area only.
 *
 * Samples:
 * Example of a SplitRadioButton with a [Radio] control:
 * @sample androidx.wear.compose.material3.samples.SplitRadioButton
 *
 * For a SplitRadioButton the background of the tappable background area behind the selection
 * control will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from
 * [RadioButtonDefaults], e.g. [RadioButtonDefaults.splitRadioButtonColors].
 *
 * [SplitRadioButton] can be enabled or disabled. A disabled button will not respond to
 * click events.
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelected Callback to be invoked when this button has been selected by clicking.
 * @param onClick Click listener called when the user clicks the main body of the button, the area
 * behind the labels.
 * @param selectionControl A slot for providing the button's selection control.
 * The [Radio] selection control is provided for this purpose.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 * shape is a key characteristic of the Wear Material Theme.
 * @param colors [SplitRadioButtonColors] that will be used to resolve the background and
 * content color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content.
 * @param selectionInteractionSource an optional hoisted [MutableInteractionSource] for observing
 * and emitting [Interaction]s for this button's "selectable" tap area. You can use this to change
 * the button's appearance or preview the button in different states. Note that if `null` is
 * provided, interactions will still happen internally.
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
fun SplitRadioButton(
    selected: Boolean,
    onSelected: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectionControl: @Composable SelectionControlScope.() -> Unit = {
        Radio()
    },
    shape: Shape = MaterialTheme.shapes.large,
    colors: SplitRadioButtonColors = RadioButtonDefaults.splitRadioButtonColors(),
    selectionInteractionSource: MutableInteractionSource? = null,
    clickInteractionSource: MutableInteractionSource? = null,
    contentPadding: PaddingValues = RadioButtonDefaults.ContentPadding,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    val (startPadding, endPadding) = contentPadding.splitHorizontally()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .defaultMinSize(minHeight = MIN_HEIGHT)
            .height(IntrinsicSize.Min)
            .width(IntrinsicSize.Max)
            .clip(shape = shape)
            .background(colors.containerColor(enabled, selected).value)
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = rippleOrFallbackImplementation(),
                    interactionSource = clickInteractionSource,
                )
                .semantics {
                    role = Role.Button
                }
                .fillMaxHeight()
                .then(startPadding)
                .weight(1.0f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Labels(
                label = provideScopeContent(
                    contentColor = colors.contentColor(enabled = enabled, selected = selected),
                    textStyle = MaterialTheme.typography.labelMedium,
                    content = label
                ),
                secondaryLabel = provideNullableScopeContent(
                    contentColor =
                        colors.secondaryContentColor(enabled = enabled, selected = selected),
                    textStyle = MaterialTheme.typography.labelSmall,
                    content = secondaryLabel
                ),
            )
            Spacer(
                modifier = Modifier
                    .size(SELECTION_CONTROL_SPACING)
            )
        }

        val splitContainerColor =
            colors.splitContainerColor(enabled = enabled, selected = selected).value
        Box(
            modifier = Modifier
                .selectable(
                    enabled = enabled,
                    selected = selected,
                    onClick = onSelected,
                    indication = rippleOrFallbackImplementation(),
                    interactionSource = selectionInteractionSource
                )
                .fillMaxHeight()
                .drawWithCache {
                    onDrawWithContent {
                        drawRect(color = splitContainerColor)
                        drawContent()
                    }
                }
                .align(Alignment.CenterVertically)
                .width(SPLIT_WIDTH)
                .wrapContentHeight(align = Alignment.CenterVertically)
                .wrapContentWidth(align = Alignment.End)
                .then(endPadding),
        ) {
            val scope = remember(enabled, selected) { SelectionControlScope(enabled, selected) }
            selectionControl(scope)
        }
    }
}

/**
 * Contains the default values used by [RadioButton]s and [SplitRadioButton]s
 */
object RadioButtonDefaults {

    /**
     * Creates a [RadioButtonColors] for use in a [RadioButton].
     *
     * @param selectedContainerColor The container color of the [RadioButton]
     * when enabled and selected.
     * @param selectedContentColor The content color of the [RadioButton]
     * when enabled and selected.
     * @param selectedSecondaryContentColor The secondary content color of the [RadioButton]
     * when enabled and selected, used for secondaryLabel content.
     * @param selectedIconColor The icon color of the [RadioButton]
     * when enabled and selected.
     * @param unselectedContainerColor  The container color of the [RadioButton]
     * when enabled and not selected.
     * @param unselectedContentColor The content color of a [RadioButton]
     * when enabled and not selected.
     * @param unselectedSecondaryContentColor The secondary content color of this [RadioButton]
     * when enabled and not selected, used for secondaryLabel content
     * @param unselectedIconColor The icon color of the [RadioButton]
     * when enabled and not selected.
     */
    @Composable
    fun radioButtonColors(
        selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedSecondaryContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
            alpha = 0.8f
        ),
        selectedIconColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContainerColor: Color = MaterialTheme.colorScheme.surface,
        unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
        unselectedSecondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedIconColor: Color = MaterialTheme.colorScheme.primary,
    ) =
        RadioButtonColors(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedIconColor = selectedIconColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedIconColor = unselectedIconColor,
            disabledSelectedContainerColor = selectedContainerColor.toDisabledColor(),
            disabledSelectedContentColor = selectedContentColor.toDisabledColor(),
            disabledSelectedSecondaryContentColor = selectedSecondaryContentColor.toDisabledColor(),
            disabledSelectedIconColor = selectedIconColor.toDisabledColor(),
            disabledUnselectedContainerColor = unselectedContainerColor.toDisabledColor(),
            disabledUnselectedContentColor = unselectedContentColor.toDisabledColor(),
            disabledUnselectedSecondaryContentColor =
                unselectedSecondaryContentColor.toDisabledColor(),
            disabledUnselectedIconColor = unselectedIconColor.toDisabledColor(),
        )

    /**
     * Creates a [SplitRadioButtonColors] for use in a [SplitRadioButton].
     *
     * @param selectedContainerColor The container color of the [SplitRadioButton] when enabled and
     * selected.
     * @param selectedContentColor The content color of the [SplitRadioButton] when enabled and
     * selected.
     * @param selectedSecondaryContentColor The secondary content color of the [SplitRadioButton]
     * when enabled and selected, used for secondaryLabel content.
     * @param selectedSplitContainerColor The split container color of the [SplitRadioButton]
     * when enabled and selected.
     * @param unselectedContainerColor The container color of the [SplitRadioButton] when enabled
     * and unselected.
     * @param unselectedContentColor The content color of the [SplitRadioButton] when enabled and
     * unselected.
     * @param unselectedSecondaryContentColor The secondary content color of the [SplitRadioButton]
     * when enabled and unselected, used for secondaryLabel content.
     * @param unselectedSplitContainerColor The split container color of the [SplitRadioButton] when
     * enabled and unselected.
     */
    @Composable
    fun splitRadioButtonColors(
        selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedSecondaryContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
            alpha = 0.8f
        ),
        selectedSplitContainerColor: Color = MaterialTheme.colorScheme.primary.copy(.15f),
        unselectedContainerColor: Color = MaterialTheme.colorScheme.surface,
        unselectedContentColor: Color = MaterialTheme.colorScheme.onSurface,
        unselectedSecondaryContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedSplitContainerColor: Color = MaterialTheme.colorScheme.surfaceBright
    ) =
        SplitRadioButtonColors(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedSplitContainerColor = selectedSplitContainerColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedSplitContainerColor = unselectedSplitContainerColor,
            disabledSelectedContainerColor = selectedContainerColor.toDisabledColor(),
            disabledSelectedContentColor = selectedContentColor.toDisabledColor(),
            disabledSelectedSecondaryContentColor = selectedSecondaryContentColor.toDisabledColor(),
            disabledSelectedSplitContainerColor = selectedSplitContainerColor.toDisabledColor(),
            disabledUnselectedContainerColor = unselectedContainerColor.toDisabledColor(),
            disabledUnselectedContentColor = unselectedContentColor.toDisabledColor(),
            disabledUnselectedSecondaryContentColor =
                unselectedSecondaryContentColor.toDisabledColor(),
            disabledUnselectedSplitContainerColor = unselectedSplitContainerColor.toDisabledColor()
        )

    private val HorizontalPadding = 14.dp
    private val VerticalPadding = 6.dp

    val ContentPadding: PaddingValues = PaddingValues(
        start = HorizontalPadding,
        top = VerticalPadding,
        end = HorizontalPadding,
        bottom = VerticalPadding
    )
}

/**
 * Represents the different container and content colors used for [RadioButton] in various states,
 * that are selected, unselected, enabled and disabled.
 *
 * @constructor [RadioButtonColors] constructor to be used with [RadioButton]
 * @param selectedContainerColor Container or background color when the radio button is selected
 * @param selectedContentColor Color of the content (e.g. label) when the radio button is selected
 * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
 * when the radio button is selected
 * @param selectedIconColor Color of the icon when the radio button is selected
 * @param unselectedContainerColor Container or background color when the radio button is unselected
 * @param unselectedContentColor Color of the content (e.g. label) when the radio button is
 * unselected
 * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
 * when the radio button is unselected
 * @param unselectedIconColor Color of the icon when the radio button is unselected
 * @param disabledSelectedContainerColor Container or background color when the radio button is
 * disabled and selected
 * @param disabledSelectedContentColor Color of content (e.g. label) when the radio button is
 * disabled and selected
 * @param disabledSelectedSecondaryContentColor Color of the secondary content like secondary label
 * when the radio button is disabled and selected
 * @param disabledSelectedIconColor Icon color when the radio button is disabled and selected
 * @param disabledUnselectedContainerColor Container or background color when the radio button is
 * disabled and unselected
 * @param disabledUnselectedContentColor Color of the content (e.g. label) when the radio button is
 * disabled and unselected
 * @param disabledUnselectedSecondaryContentColor Color of the secondary content like secondary
 * label when the radio button is disabled and unselected
 * @param disabledUnselectedIconColor Icon color when the radio button is disabled and unselected
 */
@Immutable
class RadioButtonColors constructor(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val selectedSecondaryContentColor: Color,
    val selectedIconColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val unselectedSecondaryContentColor: Color,
    val unselectedIconColor: Color,
    val disabledSelectedContainerColor: Color,
    val disabledSelectedContentColor: Color,
    val disabledSelectedSecondaryContentColor: Color,
    val disabledSelectedIconColor: Color,
    val disabledUnselectedContainerColor: Color,
    val disabledUnselectedContentColor: Color,
    val disabledUnselectedSecondaryContentColor: Color,
    val disabledUnselectedIconColor: Color,
) {
    /**
     * Determines the container color based on whether the radio button is [enabled]
     * and [selected].
     *
     * @param enabled Whether the radio button is enabled
     * @param selected Whether the radio button is checked
     */
    @Composable
    fun containerColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedContainerColor,
            uncheckedColor = unselectedContainerColor,
            disabledCheckedColor = disabledSelectedContainerColor,
            disabledUncheckedColor = disabledUnselectedContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Determines the content color based on whether the radio button is [enabled]
     * and [selected].
     *
     * @param enabled Whether the radio button is enabled
     * @param selected Whether the radio button is checked
     */
    @Composable
    fun contentColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedContentColor,
            uncheckedColor = unselectedContentColor,
            disabledCheckedColor = disabledSelectedContentColor,
            disabledUncheckedColor = disabledUnselectedContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the secondary content color depending on the [enabled] and [selected] properties.
     *
     * @param enabled Whether the RadioButton is enabled.
     * @param selected Whether the RadioButton is currently selected or unselected.
     */
    @Composable
    fun secondaryContentColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedSecondaryContentColor,
            uncheckedColor = unselectedSecondaryContentColor,
            disabledCheckedColor = disabledSelectedSecondaryContentColor,
            disabledUncheckedColor = disabledUnselectedSecondaryContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the icon color for the [RadioButton] depending on the
     * [enabled] and [selected] properties.
     *
     * @param enabled Whether the RadioButton is enabled.
     * @param selected Whether the RadioButton is currently selected or unselected.
     */
    @Composable
    fun iconColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedIconColor,
            uncheckedColor = unselectedIconColor,
            disabledCheckedColor = disabledSelectedIconColor,
            disabledUncheckedColor = disabledUnselectedIconColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as RadioButtonColors

        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedSecondaryContentColor != other.selectedSecondaryContentColor) return false
        if (selectedIconColor != other.selectedIconColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor !=
            other.disabledSelectedSecondaryContentColor
        ) return false
        if (disabledSelectedIconColor != other.disabledSelectedIconColor) return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (disabledUnselectedSecondaryContentColor !=
            other.disabledUnselectedSecondaryContentColor
        ) return false
        if (disabledUnselectedIconColor != other.disabledUnselectedIconColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedIconColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedIconColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedIconColor.hashCode()
        return result
    }
}

/**
 * Represents the different colors used in [SplitRadioButton] in different states.
 *
 * @constructor [SplitRadioButtonColors] constructor to be used with [SplitRadioButton]
 * @param selectedContainerColor Container or background color when the [SplitRadioButton] is
 * selected
 * @param selectedContentColor Color of the content (e.g. label) when the [SplitRadioButton] is
 * selected
 * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
 * when the [SplitRadioButton] is selected
 * @param selectedSplitContainerColor Split container color when the [SplitRadioButton] is selected
 * @param unselectedContainerColor Container or background color when the [SplitRadioButton] is
 * unselected
 * @param unselectedContentColor Color of the content (e.g. label) when the [SplitRadioButton] is
 * unselected
 * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
 * when the [SplitRadioButton] is unselected
 * @param unselectedSplitContainerColor Split container color when the [SplitRadioButton] is
 * unselected
 * @param disabledSelectedContainerColor Container color when the [SplitRadioButton] is disabled
 * and selected
 * @param disabledSelectedContentColor Color of the content (e.g. label) when the [SplitRadioButton]
 * is disabled and selected
 * @param disabledSelectedSecondaryContentColor Color of the secondary content
 * (e.g. secondary label) when the [SplitRadioButton] is disabled and selected
 * @param disabledSelectedSplitContainerColor Split container color when the [SplitRadioButton] is
 * disabled and selected
 * @param disabledUnselectedContainerColor Container color when the [SplitRadioButton] is unselected
 * and disabled
 * @param disabledUnselectedContentColor Color of the content (e.g. label) when the split radio
 * button is unselected and disabled
 * @param disabledUnselectedSecondaryContentColor Color of the secondary content (e.g. secondary
 * label) when the [SplitRadioButton] is unselected and disabled
 * @param disabledUnselectedSplitContainerColor Split container color when the [SplitRadioButton]
 * is unselected and disabled
 */
class SplitRadioButtonColors constructor(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val selectedSecondaryContentColor: Color,
    val selectedSplitContainerColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val unselectedSecondaryContentColor: Color,
    val unselectedSplitContainerColor: Color,
    val disabledSelectedContainerColor: Color,
    val disabledSelectedContentColor: Color,
    val disabledSelectedSecondaryContentColor: Color,
    val disabledSelectedSplitContainerColor: Color,
    val disabledUnselectedContainerColor: Color,
    val disabledUnselectedContentColor: Color,
    val disabledUnselectedSecondaryContentColor: Color,
    val disabledUnselectedSplitContainerColor: Color,
) {
    /**
     * Determines the container color based on whether the [SplitRadioButton] is [enabled]
     * and [selected].
     *
     * @param enabled Whether the [SplitRadioButton] is enabled
     * @param selected Whether the [SplitRadioButton] is currently checked/selected
     */
    @Composable
    internal fun containerColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedContainerColor,
            uncheckedColor = unselectedContainerColor,
            disabledCheckedColor = disabledSelectedContainerColor,
            disabledUncheckedColor = disabledUnselectedContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Determines the content color based on whether the [SplitRadioButton] is [enabled]
     * and [selected].
     *
     * @param enabled Whether the [SplitRadioButton] is enabled
     * @param selected Whether the [SplitRadioButton] is currently selected
     */
    @Composable
    internal fun contentColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedContentColor,
            uncheckedColor = unselectedContentColor,
            disabledCheckedColor = disabledSelectedContentColor,
            disabledUncheckedColor = disabledUnselectedContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the secondary content color for the [SplitRadioButton] depending on the
     * [enabled] and [selected] properties.
     *
     * @param enabled Whether the [SplitRadioButton] is enabled.
     * @param selected Whether the [SplitRadioButton] is currently selected.
     */
    @Composable
    internal fun secondaryContentColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedSecondaryContentColor,
            uncheckedColor = unselectedSecondaryContentColor,
            disabledCheckedColor = disabledSelectedSecondaryContentColor,
            disabledUncheckedColor = disabledUnselectedSecondaryContentColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the split container for the [SplitRadioButton] color depending on the
     * [enabled] and [selected] properties.
     *
     * @param enabled Whether the [SplitRadioButton] is enabled.
     * @param selected Whether the [SplitRadioButton] is currently selected.
     */
    @Composable
    fun splitContainerColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedSplitContainerColor,
            uncheckedColor = unselectedSplitContainerColor,
            disabledCheckedColor = disabledSelectedSplitContainerColor,
            disabledUncheckedColor = disabledUnselectedSplitContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SplitRadioButtonColors

        if (selectedContainerColor != other.selectedContainerColor) return false
        if (selectedContentColor != other.selectedContentColor) return false
        if (selectedSecondaryContentColor != other.selectedSecondaryContentColor) return false
        if (selectedSplitContainerColor != other.selectedSplitContainerColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedSplitContainerColor != other.unselectedSplitContainerColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor !=
            other.disabledSelectedSecondaryContentColor
        ) return false
        if (disabledSelectedSplitContainerColor != other.disabledSelectedSplitContainerColor)
            return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (disabledUnselectedSecondaryContentColor !=
            other.disabledUnselectedSecondaryContentColor
        ) return false
        if (disabledUnselectedSplitContainerColor != other.disabledUnselectedSplitContainerColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedSplitContainerColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedSplitContainerColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedSplitContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedSplitContainerColor.hashCode()
        return result
    }
}

/**
 * [SelectionControlScope] provides enabled and selected properties. This allows
 * selection controls to omit enabled/selected parameters as they given by the scope.
 *
 * @param isEnabled Controls the enabled state of the selection control.
 * When `false`, the control is displayed with disabled colors
 * @param isSelected Indicates whether the control is currently selected
 */
class SelectionControlScope(
    val isEnabled: Boolean,
    val isSelected: Boolean
)

@Composable
private fun RowScope.Labels(
    label: @Composable RowScope.() -> Unit,
    secondaryLabel: @Composable (RowScope.() -> Unit)?
) {
    Column(modifier = Modifier.weight(1.0f)) {
        Row(content = label)
        if (secondaryLabel != null) {
            Row(content = secondaryLabel)
        }
    }
}

@Composable
private fun PaddingValues.splitHorizontally() =
    Modifier.padding(
        start = calculateStartPadding(LocalLayoutDirection.current),
        end = 0.dp,
        top = calculateTopPadding(),
        bottom = calculateBottomPadding()
    ) to Modifier.padding(
        start = 0.dp,
        end = calculateEndPadding(
            layoutDirection = LocalLayoutDirection.current
        ),
        top = calculateTopPadding(),
        bottom = calculateBottomPadding()
    )

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color> =
    tween(MotionTokens.DurationMedium1, 0, MotionTokens.EasingStandardDecelerate)
private val SELECTION_CONTROL_WIDTH = 32.dp
private val SELECTION_CONTROL_HEIGHT = 24.dp
private val SELECTION_CONTROL_SPACING = 4.dp
private val ICON_SPACING = 6.dp
private val MIN_HEIGHT = 52.dp
private val SPLIT_WIDTH = 52.dp
