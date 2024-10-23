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

package androidx.wear.compose.material3

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.CheckboxButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.material3.tokens.SplitCheckboxButtonTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * The Wear Material [CheckboxButton] offers three slots and a specific layout for an icon, a label,
 * and a secondaryLabel. The icon and secondaryLabel are optional. The items are laid out in a row
 * with the optional icon at the start and a column containing the two label slots in the middle.
 *
 * The [CheckboxButton] is Stadium shaped. The label should take no more than 3 lines of text. The
 * secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [CheckboxButton] height adjusts to accommodate the contents. The label and secondary
 * label are start aligned by default.
 *
 * Samples: Example of a CheckboxButton:
 *
 * @sample androidx.wear.compose.material3.samples.CheckboxButtonSample
 *
 * [CheckboxButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [CheckboxButton] colors can be obtained from [CheckboxButtonDefaults],
 * e.g. [CheckboxButtonDefaults.checkboxButtonColors].
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this button's checked status is changed.
 * @param modifier Modifier to be applied to the [CheckboxButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [CheckboxButtonColors] that will be used to resolve the background and content
 *   color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 *   button's appearance or preview the button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button. The
 *   contents are expected to be a horizontally and vertically center aligned icon of size 24.dp.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned and no more than 2 lines of text.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned and no more than 3 lines of text.
 */
@Composable
fun CheckboxButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CheckboxButtonDefaults.checkboxButtonShape,
    colors: CheckboxButtonColors = CheckboxButtonDefaults.checkboxButtonColors(),
    contentPadding: PaddingValues = CheckboxButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) =
    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        label =
            provideScopeContent(
                contentColor = colors.contentColor(enabled = enabled, checked),
                textStyle = CheckboxButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                    ),
                content = label
            ),
        toggleControl = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                boxColor = { enabled, checked ->
                    colors.boxColor(enabled = enabled, checked = checked)
                },
                checkmarkColor = { enabled, checked ->
                    colors.checkmarkColor(enabled = enabled, checked = checked)
                }
            )
        },
        selectionControl = null,
        modifier = modifier.defaultMinSize(minHeight = MIN_HEIGHT).height(IntrinsicSize.Min),
        icon =
            provideNullableScopeContent(
                contentColor = colors.iconColor(enabled = enabled, checked),
                content = icon
            ),
        secondaryLabel =
            provideNullableScopeContent(
                contentColor = colors.secondaryContentColor(enabled = enabled, checked),
                textStyle = CheckboxButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                    ),
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
        toggleControlWidth = CHECKBOX_WIDTH,
        toggleControlHeight = CHECKBOX_HEIGHT,
        labelSpacerSize = CheckboxButtonDefaults.LabelSpacerSize,
        toggleControlSpacing = TOGGLE_CONTROL_SPACING,
        iconSpacing = ICON_SPACING,
        ripple = ripple()
    )

/**
 * The Wear Material [SplitCheckboxButton] offers slots and a specific layout for a label and
 * secondaryLabel. The secondaryLabel is optional. The items are laid out with a column containing
 * the two label slots and a Checkbox at the end.
 *
 * The [SplitCheckboxButton] is Stadium shaped. The label should take no more than 3 lines of text.
 * The secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [SplitCheckboxButton] height adjusts to accommodate the contents. The label and
 * secondary label are start aligned by default.
 *
 * A [SplitCheckboxButton] has two tappable areas, one tap area for the labels and another for the
 * checkbox. The [onContainerClick] listener will be associated with the main body of the
 * [SplitCheckboxButton] and the [onCheckedChange] listener associated with the checkbox area only.
 *
 * Samples: Example of a SplitCheckboxButton:
 *
 * @sample androidx.wear.compose.material3.samples.SplitCheckboxButtonSample
 *
 * For a SplitCheckboxButton the background of the tappable background area behind the toggle
 * control will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from [CheckboxButtonDefaults], e.g.
 * [CheckboxButtonDefaults.splitCheckboxButtonColors].
 *
 * [SplitCheckboxButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is changed.
 * @param toggleContentDescription The content description for the checkbox control part of the
 *   component.
 * @param onContainerClick Click listener called when the user clicks the main body of the button,
 *   the area behind the labels.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [SplitCheckboxButtonColors] that will be used to resolve the background and content
 *   color for this button in different states.
 * @param toggleInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 *   button's appearance or preview the button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param containerInteractionSource an optional hoisted [MutableInteractionSource] for observing
 *   and emitting [Interaction]s for this button's main body "clickable" tap area. You can use this
 *   to change the button's appearance or preview the button in different states. Note that if
 *   `null` is provided, interactions will still happen internally.
 * @param containerClickLabel Optional click label on the main body of the button for accessibility.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned.
 */
@Composable
fun SplitCheckboxButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    toggleContentDescription: String?,
    onContainerClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CheckboxButtonDefaults.splitCheckboxButtonShape,
    colors: SplitCheckboxButtonColors = CheckboxButtonDefaults.splitCheckboxButtonColors(),
    toggleInteractionSource: MutableInteractionSource? = null,
    containerInteractionSource: MutableInteractionSource? = null,
    containerClickLabel: String? = null,
    contentPadding: PaddingValues = CheckboxButtonDefaults.ContentPadding,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    val containerColor = colors.containerColor(enabled, checked).value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .defaultMinSize(minHeight = MIN_HEIGHT)
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Max)
                .clip(shape = shape)
    ) {
        Row(
            modifier =
                Modifier.clickable(
                        enabled = enabled,
                        onClick = onContainerClick,
                        indication = ripple(),
                        interactionSource = containerInteractionSource,
                        onClickLabel = containerClickLabel,
                    )
                    .semantics { role = Role.Button }
                    .fillMaxHeight()
                    .clip(SPLIT_SECTIONS_SHAPE)
                    .background(containerColor)
                    .padding(contentPadding)
                    .weight(1.0f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Labels(
                label =
                    provideScopeContent(
                        contentColor = colors.contentColor(enabled = enabled, checked = checked),
                        textStyle = SplitCheckboxButtonTokens.LabelFont.value,
                        textConfiguration =
                            TextConfiguration(
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                            ),
                        content = label
                    ),
                secondaryLabel =
                    provideNullableScopeContent(
                        contentColor =
                            colors.secondaryContentColor(enabled = enabled, checked = checked),
                        textStyle = SplitCheckboxButtonTokens.SecondaryLabelFont.value,
                        textConfiguration =
                            TextConfiguration(
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                textAlign = TextAlign.Start,
                            ),
                        content = secondaryLabel
                    ),
            )
        }

        Spacer(modifier = Modifier.size(2.dp))

        val splitBackground = if (enabled) containerColor else Color.Black
        val splitBackgroundOverlay = colors.splitContainerColor(enabled, checked).value
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.toggleable(
                        enabled = enabled,
                        value = checked,
                        onValueChange = onCheckedChange,
                        indication = ripple(),
                        interactionSource = toggleInteractionSource
                    )
                    .fillMaxHeight()
                    .clip(SPLIT_SECTIONS_SHAPE)
                    .background(splitBackground)
                    .drawWithCache {
                        onDrawWithContent {
                            drawRect(color = splitBackgroundOverlay)
                            drawContent()
                        }
                    }
                    .defaultMinSize(minWidth = SPLIT_MIN_WIDTH)
                    .wrapContentHeight(align = Alignment.CenterVertically)
                    .padding(contentPadding)
        ) {
            Checkbox(
                checked = checked,
                enabled = enabled,
                modifier =
                    if (toggleContentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics { contentDescription = toggleContentDescription }
                    },
                boxColor = { enabled, checked ->
                    colors.boxColor(enabled = enabled, checked = checked)
                },
                checkmarkColor = { enabled, checked ->
                    colors.checkmarkColor(enabled = enabled, checked = checked)
                }
            )
        }
    }
}

/** Contains the default values used by [CheckboxButton]s and [SplitCheckboxButton]s */
object CheckboxButtonDefaults {
    /** Recommended [Shape] for [CheckboxButton]. */
    val checkboxButtonShape: Shape
        @Composable get() = CheckboxButtonTokens.ContainerShape.value

    /** Recommended [Shape] for [SplitCheckboxButton]. */
    val splitCheckboxButtonShape: Shape
        @Composable get() = SplitCheckboxButtonTokens.ContainerShape.value

    /** Creates a [CheckboxButtonColors] for use in a [CheckboxButton]. */
    @Composable fun checkboxButtonColors() = MaterialTheme.colorScheme.defaultCheckboxButtonColors

    /**
     * Creates a [CheckboxButtonColors] for use in a [CheckboxButton].
     *
     * @param checkedContainerColor The container color of the [CheckboxButton] when enabled and
     *   checked.
     * @param checkedContentColor The content color of the [CheckboxButton] when enabled and
     *   checked.
     * @param checkedSecondaryContentColor The secondary content color of the [CheckboxButton] when
     *   enabled and checked, used for secondaryLabel content.
     * @param checkedIconColor The icon color of the [CheckboxButton] when enabled and checked.
     * @param checkedBoxColor The box color of the checkbox when enabled and checked.
     * @param checkedCheckmarkColor The checkmark color of the checkbox when enabled and checked.
     * @param uncheckedContainerColor The container color of the [CheckboxButton] when enabled and
     *   unchecked.
     * @param uncheckedContentColor The content color of a [CheckboxButton] when enabled and
     *   unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of this [CheckboxButton]
     *   when enabled and unchecked, used for secondaryLabel content
     * @param uncheckedIconColor The icon color of the [CheckboxButton] when enabled and unchecked.
     * @param uncheckedBoxColor The box color of the checkbox when enabled and unchecked.
     * @param disabledCheckedContainerColor The container color of the [CheckboxButton] when
     *   disabled and checked.
     * @param disabledCheckedContentColor The content color of the [CheckboxButton] when disabled
     *   and checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the
     *   [CheckboxButton] when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedIconColor The icon color of the [CheckboxButton] when disabled and
     *   checked.
     * @param disabledCheckedBoxColor The box color of the checkbox when disabled and checked.
     * @param disabledCheckedCheckmarkColor The checkmark color of the checkbox when disabled and
     *   checked.
     * @param disabledUncheckedContainerColor The container color of the [CheckboxButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of a [CheckboxButton] when disabled
     *   and unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of this
     *   [CheckboxButton] when disabled and unchecked, used for secondaryLabel content
     * @param disabledUncheckedIconColor The icon color of the [CheckboxButton] when disabled and
     *   unchecked.
     * @param disabledUncheckedBoxColor The box color of the checkbox when disabled and unchecked.
     */
    @Composable
    fun checkboxButtonColors(
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
        checkedSecondaryContentColor: Color = Color.Unspecified,
        checkedIconColor: Color = Color.Unspecified,
        checkedBoxColor: Color = Color.Unspecified,
        checkedCheckmarkColor: Color = Color.Unspecified,
        uncheckedContainerColor: Color = Color.Unspecified,
        uncheckedContentColor: Color = Color.Unspecified,
        uncheckedSecondaryContentColor: Color = Color.Unspecified,
        uncheckedIconColor: Color = Color.Unspecified,
        uncheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedContainerColor: Color = Color.Unspecified,
        disabledCheckedContentColor: Color = Color.Unspecified,
        disabledCheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledCheckedIconColor: Color = Color.Unspecified,
        disabledCheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedCheckmarkColor: Color = Color.Unspecified,
        disabledUncheckedContainerColor: Color = Color.Unspecified,
        disabledUncheckedContentColor: Color = Color.Unspecified,
        disabledUncheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledUncheckedIconColor: Color = Color.Unspecified,
        disabledUncheckedBoxColor: Color = Color.Unspecified
    ) =
        MaterialTheme.colorScheme.defaultCheckboxButtonColors.copy(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            checkedBoxColor = checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            uncheckedBoxColor = uncheckedBoxColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor,
            disabledCheckedCheckmarkColor = disabledCheckedCheckmarkColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor
        )

    /** Creates a [SplitCheckboxButtonColors] for use in a [SplitCheckboxButton]. */
    @Composable
    fun splitCheckboxButtonColors() = MaterialTheme.colorScheme.defaultSplitCheckboxButtonColors

    /**
     * Creates a [SplitCheckboxButtonColors] for use in a [SplitCheckboxButton].
     *
     * @param checkedContainerColor The container color of the [SplitCheckboxButton] when enabled
     *   and checked.
     * @param checkedContentColor The content color of the [SplitCheckboxButton] when enabled and
     *   checked.
     * @param checkedSecondaryContentColor The secondary content color of the [SplitCheckboxButton]
     *   when enabled and checked, used for secondaryLabel content.
     * @param checkedSplitContainerColor The split container color of the [SplitCheckboxButton] when
     *   enabled and checked.
     * @param checkedBoxColor The box color of the [SplitCheckboxButton] when enabled and checked.
     * @param checkedCheckmarkColor The checkmark color of the [SplitCheckboxButton] when enabled
     *   and checked
     * @param uncheckedContainerColor The container color of the [SplitCheckboxButton] when enabled
     *   and unchecked.
     * @param uncheckedContentColor The content color of the [SplitCheckboxButton] when enabled and
     *   unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of the
     *   [SplitCheckboxButton] when enabled and unchecked, used for secondaryLabel content.
     * @param uncheckedSplitContainerColor The split container color of the [SplitCheckboxButton]
     *   when enabled and unchecked.
     * @param uncheckedBoxColor The box color of the [SplitCheckboxButton] when enabled and
     *   unchecked
     * @param disabledCheckedContainerColor The container color of the [SplitCheckboxButton] when
     *   disabled and checked.
     * @param disabledCheckedContentColor The content color of the [SplitCheckboxButton] when
     *   disabled and checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the
     *   [SplitCheckboxButton] when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedSplitContainerColor The split container color of the
     *   [SplitCheckboxButton] when disabled and checked.
     * @param disabledCheckedBoxColor The box color of the [SplitCheckboxButton] when disabled and
     *   checked.
     * @param disabledCheckedCheckmarkColor The checkmark color of the [SplitCheckboxButton] when
     *   disabled and checked.
     * @param disabledUncheckedContainerColor The container color of the [SplitCheckboxButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of the [SplitCheckboxButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of the
     *   [SplitCheckboxButton] when disabled and unchecked, used for secondaryLabel content.
     * @param disabledUncheckedSplitContainerColor The split container color of the
     *   [SplitCheckboxButton] when disabled and unchecked.
     * @param disabledUncheckedBoxColor The box color of the [SplitCheckboxButton] when disabled and
     *   unchecked.
     */
    @Composable
    fun splitCheckboxButtonColors(
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
        checkedSecondaryContentColor: Color = Color.Unspecified,
        checkedSplitContainerColor: Color = Color.Unspecified,
        checkedBoxColor: Color = Color.Unspecified,
        checkedCheckmarkColor: Color = Color.Unspecified,
        uncheckedContainerColor: Color = Color.Unspecified,
        uncheckedContentColor: Color = Color.Unspecified,
        uncheckedSecondaryContentColor: Color = Color.Unspecified,
        uncheckedSplitContainerColor: Color = Color.Unspecified,
        uncheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedContainerColor: Color = Color.Unspecified,
        disabledCheckedContentColor: Color = Color.Unspecified,
        disabledCheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledCheckedSplitContainerColor: Color = Color.Unspecified,
        disabledCheckedBoxColor: Color = Color.Unspecified,
        disabledCheckedCheckmarkColor: Color = Color.Unspecified,
        disabledUncheckedContainerColor: Color = Color.Unspecified,
        disabledUncheckedContentColor: Color = Color.Unspecified,
        disabledUncheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledUncheckedSplitContainerColor: Color = Color.Unspecified,
        disabledUncheckedBoxColor: Color = Color.Unspecified
    ) =
        MaterialTheme.colorScheme.defaultSplitCheckboxButtonColors.copy(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedSplitContainerColor = checkedSplitContainerColor,
            checkedBoxColor = checkedBoxColor,
            checkedCheckmarkColor = checkedCheckmarkColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor = uncheckedSplitContainerColor,
            uncheckedBoxColor = uncheckedBoxColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor = disabledCheckedSplitContainerColor,
            disabledCheckedBoxColor = disabledCheckedBoxColor,
            disabledCheckedCheckmarkColor = disabledCheckedCheckmarkColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedSplitContainerColor = disabledUncheckedSplitContainerColor,
            disabledUncheckedBoxColor = disabledUncheckedBoxColor,
        )

    internal val LabelSpacerSize = 2.dp
    private val HorizontalPadding = 14.dp
    private val VerticalPadding = 8.dp

    /** The default content padding used by [CheckboxButton] */
    val ContentPadding: PaddingValues =
        PaddingValues(
            start = HorizontalPadding,
            top = VerticalPadding,
            end = HorizontalPadding,
            bottom = VerticalPadding
        )

    private val ColorScheme.defaultCheckboxButtonColors: CheckboxButtonColors
        get() {
            return defaultCheckboxButtonColorsCached
                ?: CheckboxButtonColors(
                        checkedContainerColor =
                            fromToken(CheckboxButtonTokens.CheckedContainerColor),
                        checkedContentColor = fromToken(CheckboxButtonTokens.CheckedContentColor),
                        checkedSecondaryContentColor =
                            fromToken(CheckboxButtonTokens.CheckedSecondaryLabelColor)
                                .copy(alpha = CheckboxButtonTokens.CheckedSecondaryLabelOpacity),
                        checkedIconColor = fromToken(CheckboxButtonTokens.CheckedIconColor),
                        checkedBoxColor = fromToken(CheckboxButtonTokens.CheckedBoxColor),
                        checkedCheckmarkColor =
                            fromToken(CheckboxButtonTokens.CheckedCheckmarkColor),
                        uncheckedContainerColor =
                            fromToken(CheckboxButtonTokens.UncheckedContainerColor),
                        uncheckedContentColor =
                            fromToken(CheckboxButtonTokens.UncheckedContentColor),
                        uncheckedSecondaryContentColor =
                            fromToken(CheckboxButtonTokens.UncheckedSecondaryLabelColor),
                        uncheckedIconColor = fromToken(CheckboxButtonTokens.UncheckedIconColor),
                        uncheckedBoxColor = fromToken(CheckboxButtonTokens.UncheckedBoxColor),
                        disabledCheckedContainerColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        CheckboxButtonTokens.DisabledCheckedContainerOpacity
                                ),
                        disabledCheckedContentColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSecondaryContentColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedIconColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedIconColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedBoxColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedBoxColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledCheckedBoxOpacity
                                ),
                        disabledCheckedCheckmarkColor =
                            fromToken(CheckboxButtonTokens.DisabledCheckedCheckmarkColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        CheckboxButtonTokens.DisabledCheckedCheckmarkOpacity
                                ),
                        disabledUncheckedContainerColor =
                            fromToken(CheckboxButtonTokens.DisabledUncheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        CheckboxButtonTokens.DisabledUncheckedContainerOpacity
                                ),
                        disabledUncheckedContentColor =
                            fromToken(CheckboxButtonTokens.DisabledUncheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSecondaryContentColor =
                            fromToken(CheckboxButtonTokens.DisabledUncheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedIconColor =
                            fromToken(CheckboxButtonTokens.DisabledUncheckedIconColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedBoxColor =
                            fromToken(CheckboxButtonTokens.DisabledUncheckedBoxColor)
                                .toDisabledColor(
                                    disabledAlpha = CheckboxButtonTokens.DisabledUncheckedBoxOpacity
                                )
                    )
                    .also { defaultCheckboxButtonColorsCached = it }
        }

    private val ColorScheme.defaultSplitCheckboxButtonColors: SplitCheckboxButtonColors
        get() {
            return defaultSplitCheckboxButtonColorsCached
                ?: SplitCheckboxButtonColors(
                        checkedContainerColor =
                            fromToken(SplitCheckboxButtonTokens.CheckedContainerColor),
                        checkedContentColor =
                            fromToken(SplitCheckboxButtonTokens.CheckedContentColor),
                        checkedSecondaryContentColor =
                            fromToken(SplitCheckboxButtonTokens.CheckedSecondaryLabelColor)
                                .copy(
                                    alpha = SplitCheckboxButtonTokens.CheckedSecondaryLabelOpacity
                                ),
                        checkedSplitContainerColor =
                            fromToken(SplitCheckboxButtonTokens.CheckedSplitContainerColor)
                                .copy(
                                    alpha = SplitCheckboxButtonTokens.CheckedSplitContainerOpacity
                                ),
                        checkedBoxColor = fromToken(SplitCheckboxButtonTokens.CheckedBoxColor),
                        checkedCheckmarkColor =
                            fromToken(SplitCheckboxButtonTokens.CheckedCheckmarkColor),
                        uncheckedContainerColor =
                            fromToken(SplitCheckboxButtonTokens.UncheckedContainerColor),
                        uncheckedContentColor =
                            fromToken(SplitCheckboxButtonTokens.UncheckedContentColor),
                        uncheckedSecondaryContentColor =
                            fromToken(SplitCheckboxButtonTokens.UncheckedSecondaryLabelColor),
                        uncheckedSplitContainerColor =
                            fromToken(SplitCheckboxButtonTokens.UncheckedSplitContainerColor),
                        uncheckedBoxColor = fromToken(SplitCheckboxButtonTokens.UncheckedBoxColor),
                        disabledCheckedContainerColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedContainerColor)
                                .copy(
                                    alpha =
                                        SplitCheckboxButtonTokens.DisabledCheckedContainerOpacity
                                ),
                        disabledCheckedContentColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitCheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSecondaryContentColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitCheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSplitContainerColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedSplitContainerColor)
                                .copy(
                                    alpha =
                                        SplitCheckboxButtonTokens
                                            .DisabledCheckedSplitContainerOpacity
                                ),
                        disabledCheckedBoxColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedBoxColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitCheckboxButtonTokens.DisabledCheckedBoxOpacity
                                ),
                        disabledCheckedCheckmarkColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledCheckedCheckmarkColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitCheckboxButtonTokens.DisabledCheckedCheckmarkOpacity
                                ),
                        disabledUncheckedContainerColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledUncheckedContainerColor)
                                .copy(
                                    alpha =
                                        SplitCheckboxButtonTokens.DisabledUncheckedContainerOpacity
                                ),
                        disabledUncheckedContentColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledUncheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitCheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSecondaryContentColor =
                            fromToken(
                                    SplitCheckboxButtonTokens.DisabledUncheckedSecondaryLabelColor
                                )
                                .toDisabledColor(
                                    disabledAlpha = SplitCheckboxButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSplitContainerColor =
                            fromToken(
                                    SplitCheckboxButtonTokens.DisabledUncheckedSplitContainerColor
                                )
                                .copy(
                                    alpha =
                                        SplitCheckboxButtonTokens
                                            .DisabledUncheckedSplitContainerOpacity
                                ),
                        disabledUncheckedBoxColor =
                            fromToken(SplitCheckboxButtonTokens.DisabledUncheckedBoxColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitCheckboxButtonTokens.DisabledUncheckedBoxOpacity
                                )
                    )
                    .also { defaultSplitCheckboxButtonColorsCached = it }
        }
}

/**
 * Represents the different container and content colors used for [CheckboxButton] in various
 * states, that are checked, unchecked, enabled and disabled.
 *
 * @param checkedContainerColor Container or background color when the checkbox is checked
 * @param checkedContentColor Color of the content like label when the checkbox is checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 *   checkbox is checked
 * @param checkedIconColor Color of the icon when the checkbox is checked
 * @param checkedBoxColor Color of the box when the checkbox is checked
 * @param checkedCheckmarkColor Color of the checkmark when the checkbox is checked
 * @param uncheckedContainerColor Container or background color when the checkbox is unchecked
 * @param uncheckedContentColor Color of the content like label when the checkbox is unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 *   the checkbox is unchecked
 * @param uncheckedIconColor Color of the icon when the checkbox is unchecked
 * @param uncheckedBoxColor Color of the box when the checkbox is unchecked
 * @param disabledCheckedContainerColor Container or background color when the checkbox is disabled
 *   and checked
 * @param disabledCheckedContentColor Color of content like label when the checkbox is disabled and
 *   checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the checkbox is disabled and checked
 * @param disabledCheckedIconColor Icon color when the checkbox is disabled and checked
 * @param disabledCheckedBoxColor Box color when the checkbox is disabled and checked
 * @param disabledCheckedCheckmarkColor Checkmark color when the checkbox is disabled and checked
 * @param disabledUncheckedContainerColor Container or background color when the checkbox is
 *   disabled and unchecked
 * @param disabledUncheckedContentColor Color of the content like label when the checkbox is
 *   disabled and unchecked
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the checkbox is disabled and unchecked
 * @param disabledUncheckedIconColor Icon color when the checkbox is disabled and unchecked
 * @param disabledUncheckedBoxColor Box color when the checkbox is disabled and unchecked
 * @constructor [CheckboxButtonColors] constructor to be used with [CheckboxButton]
 */
@Immutable
class CheckboxButtonColors
constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedIconColor: Color,
    val checkedBoxColor: Color,
    val checkedCheckmarkColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedIconColor: Color,
    val uncheckedBoxColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledCheckedCheckmarkColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedIconColor: Color,
    val disabledUncheckedBoxColor: Color,
) {
    /** Returns a copy of this CheckboxButtonColors, optionally overriding some of the values. */
    fun copy(
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
        checkedSecondaryContentColor: Color = this.checkedSecondaryContentColor,
        checkedIconColor: Color = this.checkedIconColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedContainerColor: Color = this.uncheckedContainerColor,
        uncheckedContentColor: Color = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: Color = this.uncheckedSecondaryContentColor,
        uncheckedIconColor: Color = this.uncheckedIconColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedContainerColor: Color = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: Color = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: Color = this.disabledCheckedSecondaryContentColor,
        disabledCheckedIconColor: Color = this.disabledCheckedIconColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledCheckedCheckmarkColor: Color = this.disabledCheckedCheckmarkColor,
        disabledUncheckedContainerColor: Color = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: Color = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: Color = this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedIconColor: Color = this.disabledUncheckedIconColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
    ): CheckboxButtonColors =
        CheckboxButtonColors(
            checkedContainerColor = checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor = checkedContentColor.takeOrElse { this.checkedContentColor },
            checkedSecondaryContentColor =
                checkedSecondaryContentColor.takeOrElse { this.checkedSecondaryContentColor },
            checkedIconColor = checkedIconColor.takeOrElse { this.checkedIconColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedContainerColor =
                uncheckedContainerColor.takeOrElse { this.uncheckedContainerColor },
            uncheckedContentColor = uncheckedContentColor.takeOrElse { this.uncheckedContentColor },
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor.takeOrElse { this.uncheckedSecondaryContentColor },
            uncheckedIconColor = uncheckedIconColor.takeOrElse { this.uncheckedIconColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedContainerColor =
                disabledCheckedContainerColor.takeOrElse { this.disabledCheckedContainerColor },
            disabledCheckedContentColor =
                disabledCheckedContentColor.takeOrElse { this.disabledCheckedContentColor },
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor.takeOrElse {
                    this.disabledCheckedSecondaryContentColor
                },
            disabledCheckedIconColor =
                disabledCheckedIconColor.takeOrElse { this.disabledCheckedIconColor },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledCheckedCheckmarkColor =
                disabledCheckedCheckmarkColor.takeOrElse { this.disabledCheckedCheckmarkColor },
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor.takeOrElse { this.disabledUncheckedContainerColor },
            disabledUncheckedContentColor =
                disabledUncheckedContentColor.takeOrElse { this.disabledUncheckedContentColor },
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor.takeOrElse {
                    this.disabledUncheckedSecondaryContentColor
                },
            disabledUncheckedIconColor =
                disabledUncheckedIconColor.takeOrElse { this.disabledUncheckedIconColor },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor },
        )

    /**
     * Determines the container color based on whether the toggle button is [enabled] and [checked].
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
     * Determines the content color based on whether the toggle button is [enabled] and [checked].
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
     * @param enabled Whether the CheckboxButton is enabled.
     * @param checked Whether the CheckboxButton is currently checked or unchecked.
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
     * Represents the icon color for the [CheckboxButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the CheckboxButton is enabled.
     * @param checked Whether the CheckboxButton is currently checked or unchecked.
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

    /**
     * Represents the box color for the [CheckboxButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the CheckboxButton is enabled.
     * @param checked Whether the CheckboxButton is currently checked or unchecked.
     */
    @Composable
    internal fun boxColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedBoxColor,
            uncheckedColor = uncheckedBoxColor,
            disabledCheckedColor = disabledCheckedBoxColor,
            disabledUncheckedColor = disabledUncheckedBoxColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the checkmark color for the [CheckboxButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the CheckboxButton is enabled.
     * @param checked Whether the CheckboxButton is currently checked or unchecked.
     */
    @Composable
    internal fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedCheckmarkColor,
            uncheckedColor = Color.Transparent,
            disabledCheckedColor = disabledCheckedCheckmarkColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as CheckboxButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledCheckedCheckmarkColor != other.disabledCheckedCheckmarkColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        return result
    }
}

/**
 * Represents the different colors used in [SplitCheckboxButton] in different states.
 *
 * @param checkedContainerColor Container or background color when the [SplitCheckboxButton] is
 *   checked
 * @param checkedContentColor Color of the content like label when the [SplitCheckboxButton] is
 *   checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 *   [SplitCheckboxButton] is checked
 * @param checkedSplitContainerColor Split container color when the [SplitCheckboxButton] is checked
 * @param checkedBoxColor Box color when [SplitCheckboxButton] is checked
 * @param checkedCheckmarkColor Checkmark color when [SplitCheckboxButton] is checked
 * @param uncheckedContainerColor Container or background color when the [SplitCheckboxButton] is
 *   unchecked
 * @param uncheckedContentColor Color of the content like label when the [SplitCheckboxButton] is
 *   unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 *   the [SplitCheckboxButton] is unchecked
 * @param uncheckedSplitContainerColor Split container color when the [SplitCheckboxButton] is
 *   unchecked
 * @param uncheckedBoxColor Box color when the [SplitCheckboxButton] is unchecked
 * @param disabledCheckedContainerColor Container color when the [SplitCheckboxButton] is disabled
 *   and checked
 * @param disabledCheckedContentColor Color of the content like label when the [SplitCheckboxButton]
 *   is disabled and checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the [SplitCheckboxButton] is disabled and checked
 * @param disabledCheckedSplitContainerColor Split container color when the [SplitCheckboxButton] is
 *   disabled and checked
 * @param disabledCheckedBoxColor Box color when the [SplitCheckboxButton] is disabled and checked
 * @param disabledCheckedCheckmarkColor Checkmark color when the [SplitCheckboxButton] is disabled
 *   and checked
 * @param disabledUncheckedContainerColor Container color when the [SplitCheckboxButton] is
 *   unchecked and disabled
 * @param disabledUncheckedContentColor Color of the content like label when the
 *   [SplitCheckboxButton] is unchecked and disabled
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the [SplitCheckboxButton] is unchecked and disabled
 * @param disabledUncheckedSplitContainerColor Split container color when the [SplitCheckboxButton]
 *   is unchecked and disabled
 * @param disabledUncheckedBoxColor Box color when the [SplitCheckboxButton] is disabled and
 *   unchecked
 * @constructor [SplitCheckboxButtonColors] constructor to be used with [SplitCheckboxButton]
 */
class SplitCheckboxButtonColors
constructor(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedSplitContainerColor: Color,
    val checkedBoxColor: Color,
    val checkedCheckmarkColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedSplitContainerColor: Color,
    val uncheckedBoxColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedSplitContainerColor: Color,
    val disabledCheckedBoxColor: Color,
    val disabledCheckedCheckmarkColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedSplitContainerColor: Color,
    val disabledUncheckedBoxColor: Color,
) {

    /**
     * Returns a copy of this SplitCheckboxButtonColors optionally overriding some of the values.
     *
     * @param checkedContainerColor Container or background color when the [SplitCheckboxButton] is
     *   checked
     * @param checkedContentColor Color of the content like label when the [SplitCheckboxButton] is
     *   checked
     * @param checkedSecondaryContentColor Color of the secondary content like secondary label when
     *   the [SplitCheckboxButton] is checked
     * @param checkedSplitContainerColor Split container color when the [SplitCheckboxButton] is
     *   checked
     * @param checkedBoxColor Box color when [SplitCheckboxButton] is checked
     * @param checkedCheckmarkColor Checkmark color when [SplitCheckboxButton] is checked
     * @param uncheckedContainerColor Container or background color when the [SplitCheckboxButton]
     *   is unchecked
     * @param uncheckedContentColor Color of the content like label when the [SplitCheckboxButton]
     *   is unchecked
     * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label
     *   when the [SplitCheckboxButton] is unchecked
     * @param uncheckedSplitContainerColor Split container color when the [SplitCheckboxButton] is
     *   unchecked
     * @param uncheckedBoxColor Box color when the [SplitCheckboxButton] is unchecked
     * @param disabledCheckedContainerColor Container color when the [SplitCheckboxButton] is
     *   disabled and checked
     * @param disabledCheckedContentColor Color of the content like label when the
     *   [SplitCheckboxButton] is disabled and checked
     * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the [SplitCheckboxButton] is disabled and checked
     * @param disabledCheckedSplitContainerColor Split container color when the
     *   [SplitCheckboxButton] is disabled and checked
     * @param disabledCheckedBoxColor Box color when the [SplitCheckboxButton] is disabled and
     *   checked
     * @param disabledCheckedCheckmarkColor Checkmark color when the [SplitCheckboxButton] is
     *   disabled and checked
     * @param disabledUncheckedContainerColor Container color when the [SplitCheckboxButton] is
     *   unchecked and disabled
     * @param disabledUncheckedContentColor Color of the content like label when the
     *   [SplitCheckboxButton] is unchecked and disabled
     * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the [SplitCheckboxButton] is unchecked and disabled
     * @param disabledUncheckedSplitContainerColor Split container color when the
     *   [SplitCheckboxButton] is unchecked and disabled
     * @param disabledUncheckedBoxColor Box color when the [SplitCheckboxButton] is disabled and
     *   unchecked
     */
    fun copy(
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
        checkedSecondaryContentColor: Color = this.checkedSecondaryContentColor,
        checkedSplitContainerColor: Color = this.checkedSplitContainerColor,
        checkedBoxColor: Color = this.checkedBoxColor,
        checkedCheckmarkColor: Color = this.checkedCheckmarkColor,
        uncheckedContainerColor: Color = this.uncheckedContainerColor,
        uncheckedContentColor: Color = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: Color = this.uncheckedSecondaryContentColor,
        uncheckedSplitContainerColor: Color = this.uncheckedSplitContainerColor,
        uncheckedBoxColor: Color = this.uncheckedBoxColor,
        disabledCheckedContainerColor: Color = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: Color = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: Color = this.disabledCheckedSecondaryContentColor,
        disabledCheckedSplitContainerColor: Color = this.disabledCheckedSplitContainerColor,
        disabledCheckedBoxColor: Color = this.disabledCheckedBoxColor,
        disabledCheckedCheckmarkColor: Color = this.disabledCheckedCheckmarkColor,
        disabledUncheckedContainerColor: Color = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: Color = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: Color = this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedSplitContainerColor: Color = this.disabledUncheckedSplitContainerColor,
        disabledUncheckedBoxColor: Color = this.disabledUncheckedBoxColor,
    ): SplitCheckboxButtonColors =
        SplitCheckboxButtonColors(
            checkedContainerColor = checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor = checkedContentColor.takeOrElse { this.checkedContentColor },
            checkedSecondaryContentColor =
                checkedSecondaryContentColor.takeOrElse { this.checkedSecondaryContentColor },
            checkedSplitContainerColor =
                checkedSplitContainerColor.takeOrElse { this.checkedSplitContainerColor },
            checkedBoxColor = checkedBoxColor.takeOrElse { this.checkedBoxColor },
            checkedCheckmarkColor = checkedCheckmarkColor.takeOrElse { this.checkedCheckmarkColor },
            uncheckedContainerColor =
                uncheckedContainerColor.takeOrElse { this.uncheckedContainerColor },
            uncheckedContentColor = uncheckedContentColor.takeOrElse { this.uncheckedContentColor },
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor.takeOrElse { this.uncheckedSecondaryContentColor },
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor.takeOrElse { this.uncheckedSplitContainerColor },
            uncheckedBoxColor = uncheckedBoxColor.takeOrElse { this.uncheckedBoxColor },
            disabledCheckedContainerColor =
                disabledCheckedContainerColor.takeOrElse { this.disabledCheckedContainerColor },
            disabledCheckedContentColor =
                disabledCheckedContentColor.takeOrElse { this.disabledCheckedContentColor },
            disabledCheckedSecondaryContentColor =
                disabledCheckedSecondaryContentColor.takeOrElse {
                    this.disabledCheckedSecondaryContentColor
                },
            disabledCheckedSplitContainerColor =
                disabledCheckedSplitContainerColor.takeOrElse {
                    this.disabledCheckedSplitContainerColor
                },
            disabledCheckedBoxColor =
                disabledCheckedBoxColor.takeOrElse { this.disabledCheckedBoxColor },
            disabledCheckedCheckmarkColor =
                disabledCheckedCheckmarkColor.takeOrElse { this.disabledCheckedCheckmarkColor },
            disabledUncheckedContainerColor =
                disabledUncheckedContainerColor.takeOrElse { this.disabledUncheckedContainerColor },
            disabledUncheckedContentColor =
                disabledUncheckedContentColor.takeOrElse { this.disabledUncheckedContentColor },
            disabledUncheckedSecondaryContentColor =
                disabledUncheckedSecondaryContentColor.takeOrElse {
                    this.disabledUncheckedSecondaryContentColor
                },
            disabledUncheckedSplitContainerColor =
                disabledUncheckedSplitContainerColor.takeOrElse {
                    this.disabledUncheckedSplitContainerColor
                },
            disabledUncheckedBoxColor =
                disabledUncheckedBoxColor.takeOrElse { this.disabledUncheckedBoxColor }
        )

    /**
     * Determines the container color based on whether the [SplitCheckboxButton] is [enabled] and
     * [checked].
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled
     * @param checked Whether the [SplitCheckboxButton] is currently checked
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
     * Determines the content color based on whether the [SplitCheckboxButton] is [enabled] and
     * [checked].
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled
     * @param checked Whether the [SplitCheckboxButton] is currently checked
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
     * Represents the secondary content color for the [SplitCheckboxButton] depending on the
     * [enabled] and [checked] properties.
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled.
     * @param checked Whether the [SplitCheckboxButton] is currently checked or unchecked.
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
     * Represents the split container color for the [SplitCheckboxButton] depending on the [enabled]
     * and [checked] properties.
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled.
     * @param checked Whether the [SplitCheckboxButton] is currently checked or unchecked.
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

    /**
     * Represents the box color for the [SplitCheckboxButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled.
     * @param checked Whether the [SplitCheckboxButton] is currently checked or unchecked.
     */
    @Composable
    internal fun boxColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedBoxColor,
            uncheckedColor = uncheckedBoxColor,
            disabledCheckedColor = disabledCheckedBoxColor,
            disabledUncheckedColor = disabledUncheckedBoxColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the checkmark color for the [SplitCheckboxButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SplitCheckboxButton] is enabled.
     * @param checked Whether the [SplitCheckboxButton] is currently checked or unchecked.
     */
    @Composable
    internal fun checkmarkColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedCheckmarkColor,
            uncheckedColor = Color.Transparent,
            disabledCheckedColor = disabledCheckedCheckmarkColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SplitCheckboxButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (checkedBoxColor != other.checkedBoxColor) return false
        if (checkedCheckmarkColor != other.checkedCheckmarkColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (uncheckedBoxColor != other.uncheckedBoxColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledCheckedBoxColor != other.disabledCheckedBoxColor) return false
        if (disabledCheckedCheckmarkColor != other.disabledCheckedCheckmarkColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false
        if (disabledUncheckedBoxColor != other.disabledUncheckedBoxColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + checkedBoxColor.hashCode()
        result = 31 * result + checkedCheckmarkColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedBoxColor.hashCode()
        result = 31 * result + disabledCheckedCheckmarkColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedBoxColor.hashCode()
        return result
    }
}

// [Checkbox] provides an animated checkbox for use as a toggle control in [CheckboxButton] or
// [SplitCheckboxButton].
@Composable
private fun Checkbox(
    checked: Boolean,
    enabled: Boolean,
    boxColor: @Composable (enabled: Boolean, selected: Boolean) -> State<Color>,
    checkmarkColor: @Composable (enabled: Boolean, selected: Boolean) -> State<Color>,
    modifier: Modifier = Modifier,
) =
    androidx.wear.compose.materialcore.Checkbox(
        checked = checked,
        modifier = modifier,
        boxColor = boxColor,
        checkmarkColor = checkmarkColor,
        enabled = enabled,
        onCheckedChange = null,
        interactionSource = null,
        drawBox = { drawScope, color, progress, isRtl ->
            drawScope.drawBox(color = color, progress = progress, isRtl = isRtl)
        },
        progressAnimationSpec = PROGRESS_ANIMATION_SPEC,
        width = CHECKBOX_WIDTH,
        height = CHECKBOX_HEIGHT,
        ripple = ripple()
    )

private fun DrawScope.drawBox(color: Color, progress: Float, isRtl: Boolean) {
    // Centering vertically.
    val topCornerPx = (CHECKBOX_HEIGHT - BOX_SIZE).toPx() / 2
    val strokeWidthPx = BOX_STROKE.toPx()
    val halfStrokeWidthPx = strokeWidthPx / 2.0f
    val radiusPx = BOX_RADIUS.toPx()
    val checkboxSizePx = BOX_SIZE.toPx()
    // Aligning the box to the end.
    val startXOffsetPx = if (isRtl) 0f else (CHECKBOX_WIDTH - CHECKBOX_HEIGHT).toPx()

    // Draw the outline of the box.
    drawRoundRect(
        color,
        topLeft =
            Offset(
                topCornerPx + halfStrokeWidthPx + startXOffsetPx,
                topCornerPx + halfStrokeWidthPx
            ),
        size = Size(checkboxSizePx - strokeWidthPx, checkboxSizePx - strokeWidthPx),
        cornerRadius = CornerRadius(radiusPx - halfStrokeWidthPx),
        alpha = 1 - progress,
        style = Stroke(strokeWidthPx)
    )

    // Fills the box.
    drawRoundRect(
        color,
        topLeft = Offset(topCornerPx + startXOffsetPx, topCornerPx),
        size = Size(checkboxSizePx, checkboxSizePx),
        cornerRadius = CornerRadius(radiusPx),
        alpha = progress,
        style = Fill
    )
}

@Composable
private fun RowScope.Labels(
    label: @Composable RowScope.() -> Unit,
    secondaryLabel: @Composable (RowScope.() -> Unit)?
) {
    Column(modifier = Modifier.weight(1.0f)) {
        Row(content = label)
        if (secondaryLabel != null) {
            Spacer(modifier = Modifier.size(CheckboxButtonDefaults.LabelSpacerSize))
            Row(content = secondaryLabel)
        }
    }
}

private val TOGGLE_CONTROL_SPACING = 6.dp
private val ICON_SPACING = 6.dp
private val MIN_HEIGHT = 52.dp

private val CHECKBOX_WIDTH = 24.dp
private val CHECKBOX_HEIGHT = 24.dp
private val BOX_STROKE = 2.dp
private val BOX_RADIUS = 2.dp
private val BOX_SIZE = 18.dp

private val SPLIT_MIN_WIDTH = 48.dp
private val SPLIT_SECTIONS_SHAPE = ShapeTokens.CornerExtraSmall

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color>
    @Composable get() = MaterialTheme.motionScheme.slowEffectsSpec()
private val PROGRESS_ANIMATION_SPEC: FiniteAnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
