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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.tokens.RadioButtonTokens
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.material3.tokens.SplitRadioButtonTokens
import androidx.wear.compose.materialcore.animateSelectionColor

/**
 * The Wear Material [RadioButton] offers slots and a specific layout for an icon, a label and a
 * secondaryLabel. The icon and secondaryLabel are optional. The items are laid out in a row with
 * the optional icon at the start, a column containing the two label slots in the middle and the
 * selection control at the end.
 *
 * The [RadioButton] is Stadium shaped. The label should take no more than 3 lines of text. The
 * secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [RadioButton] height adjusts to accommodate the contents. The label and secondary
 * label are start aligned by default.
 *
 * Note that Modifier.selectableGroup() must be present on the parent control (such as Column) to
 * ensure correct accessibility behavior.
 *
 * Samples: Example of a [RadioButton]:
 *
 * @sample androidx.wear.compose.material3.samples.RadioButtonSample
 *
 * [RadioButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [RadioButton] colors can be obtained from [RadioButtonDefaults], e.g.
 * [RadioButtonDefaults.radioButtonColors].
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelect Callback to be invoked when this button has been selected by clicking.
 * @param modifier Modifier to be applied to the [RadioButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [RadioButtonColors] that will be used to resolve the background and content color
 *   for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this radio button. You can use this to change the radio button's
 *   appearance or preview the radio button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button. The
 *   contents are expected to be center-aligned, both horizontally and vertically, and should be an
 *   icon of size 24.dp.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned.
 */
@Composable
fun RadioButton(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RadioButtonDefaults.radioButtonShape,
    colors: RadioButtonColors = RadioButtonDefaults.radioButtonColors(),
    contentPadding: PaddingValues = RadioButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Stadium/Pill shaped toggle button
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = MIN_HEIGHT)
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Max)
                .clip(shape = shape)
                .background(colors.containerColor(enabled = enabled, selected = selected).value)
                .selectable(
                    enabled = enabled,
                    selected = selected,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onSelect()
                    },
                    indication = ripple(),
                    interactionSource = interactionSource
                )
                .padding(contentPadding)
                .semantics {
                    // For a selectable button, the role is always RadioButton.
                    // See also b/330869742 for issue with setting the SelectableButton role
                    // within the selection control.
                    role = Role.RadioButton
                },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                content =
                    provideScopeContent(
                        color = colors.iconColor(enabled = enabled, selected = selected),
                        content = icon
                    )
            )
            Spacer(modifier = Modifier.size(ICON_SPACING))
        }
        Labels(
            label =
                provideScopeContent(
                    contentColor = colors.contentColor(enabled = enabled, selected = selected),
                    textStyle = RadioButtonTokens.LabelFont.value,
                    textConfiguration =
                        TextConfiguration(
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 3,
                            textAlign = TextAlign.Start,
                        ),
                    content = label
                ),
            secondaryLabel =
                provideNullableScopeContent(
                    contentColor =
                        colors.secondaryContentColor(enabled = enabled, selected = selected),
                    textStyle = RadioButtonTokens.SecondaryLabelFont.value,
                    textConfiguration =
                        TextConfiguration(
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            textAlign = TextAlign.Start,
                        ),
                    content = secondaryLabel
                )
        )
        Spacer(modifier = Modifier.size(SELECTION_CONTROL_SPACING))
        Box(
            modifier =
                Modifier.align(Alignment.CenterVertically)
                    .size(width = SELECTION_CONTROL_WIDTH, height = SELECTION_CONTROL_HEIGHT)
                    .wrapContentWidth(align = Alignment.End),
        ) {
            RadioControl(
                selected = selected,
                enabled = enabled,
            ) { enabled, selected ->
                colors.controlColor(enabled = enabled, selected = selected)
            }
        }
    }
}

/**
 * The Wear Material [SplitRadioButton] offers two slots and a specific layout for a label and
 * secondaryLabel. The secondaryLabel is optional. The items are laid out with a column containing
 * the two label slots and radio button control at the end.
 *
 * The [SplitRadioButton] is Stadium shaped. The label should take no more than 3 lines of text. The
 * secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [SplitRadioButton] height adjusts to accommodate the contents. The label and secondary
 * label are start aligned by default.
 *
 * A [SplitRadioButton] has two tappable areas, one tap area for the labels and another for the
 * selection control. The [onContainerClick] listener will be associated with the main body of the
 * split radio button with the [onSelectionClick] listener associated with the selection control
 * area only.
 *
 * Samples: Example of a [SplitRadioButton]:
 *
 * @sample androidx.wear.compose.material3.samples.SplitRadioButtonSample
 *
 * For a [SplitRadioButton] the background of the tappable background area behind the selection
 * control will have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from [RadioButtonDefaults], e.g.
 * [RadioButtonDefaults.splitRadioButtonColors].
 *
 * [SplitRadioButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * @param selected Boolean flag indicating whether this button is currently selected.
 * @param onSelectionClick Callback to be invoked when this button has been selected.
 * @param selectionContentDescription The content description for the selection control part of the
 *   component
 * @param onContainerClick Click listener called when the user clicks the main body of the button,
 *   the area containing the labels.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [SplitRadioButtonColors] that will be used to resolve the background and content
 *   color for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param selectionInteractionSource an optional hoisted [MutableInteractionSource] for observing
 *   and emitting [Interaction]s for this button's "selectable" tap area. You can use this to change
 *   the button's appearance or preview the button in different states. Note that if `null` is
 *   provided, interactions will still happen internally.
 * @param containerInteractionSource an optional hoisted [MutableInteractionSource] for observing
 *   and emitting [Interaction]s for this button's "clickable" tap area. You can use this to change
 *   the button's appearance or preview the button in different states. Note that if `null` is
 *   provided, interactions will still happen internally.
 * @param containerClickLabel Optional click label on the main body of the button for accessibility.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned.
 */
@Composable
fun SplitRadioButton(
    selected: Boolean,
    onSelectionClick: () -> Unit,
    selectionContentDescription: String?,
    onContainerClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RadioButtonDefaults.splitRadioButtonShape,
    colors: SplitRadioButtonColors = RadioButtonDefaults.splitRadioButtonColors(),
    selectionInteractionSource: MutableInteractionSource? = null,
    containerInteractionSource: MutableInteractionSource? = null,
    containerClickLabel: String? = null,
    contentPadding: PaddingValues = RadioButtonDefaults.ContentPadding,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    val containerColor = colors.containerColor(enabled, selected).value

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
                        contentColor = colors.contentColor(enabled = enabled, selected = selected),
                        textStyle = SplitRadioButtonTokens.LabelFont.value,
                        textConfiguration =
                            TextConfiguration(
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 3,
                                textAlign = TextAlign.Start,
                            ),
                        content = label
                    ),
                secondaryLabel =
                    provideNullableScopeContent(
                        contentColor =
                            colors.secondaryContentColor(enabled = enabled, selected = selected),
                        textStyle = SplitRadioButtonTokens.SecondaryLabelFont.value,
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
        val splitBackgroundOverlay =
            colors.splitContainerColor(enabled = enabled, selected = selected).value
        val hapticFeedback = LocalHapticFeedback.current

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.selectable(
                        enabled = enabled,
                        selected = selected,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onSelectionClick()
                        },
                        indication = ripple(),
                        interactionSource = selectionInteractionSource
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
                    .semantics {
                        // For a selectable button, the role is always RadioButton.
                        // See also b/330869742 for issue with setting the SelectableButton role
                        // within the selection control.
                        role = Role.RadioButton
                    },
        ) {
            RadioControl(
                selected = selected,
                enabled = enabled,
                modifier =
                    if (selectionContentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics { contentDescription = selectionContentDescription }
                    }
            ) { enabled, selected ->
                colors.controlColor(enabled = enabled, selected = selected)
            }
        }
    }
}

/** Contains the default values used by [RadioButton]s and [SplitRadioButton]s */
object RadioButtonDefaults {
    /** Recommended [Shape] for [RadioButton]. */
    val radioButtonShape: Shape
        @Composable get() = RadioButtonTokens.Shape.value

    /** Recommended [Shape] for [SplitRadioButton]. */
    val splitRadioButtonShape: Shape
        @Composable get() = SplitRadioButtonTokens.Shape.value

    /** Creates a [RadioButtonColors] for use in a [RadioButton]. */
    @Composable fun radioButtonColors() = MaterialTheme.colorScheme.defaultRadioButtonColors

    /**
     * Creates a [RadioButtonColors] for use in a [RadioButton].
     *
     * @param selectedContainerColor The container color of the [RadioButton] when enabled and
     *   selected.
     * @param selectedContentColor The content color of the [RadioButton] when enabled and selected.
     * @param selectedSecondaryContentColor The secondary content color of the [RadioButton] when
     *   enabled and selected, used for secondaryLabel content.
     * @param selectedIconColor The icon color of the [RadioButton] when enabled and selected.
     * @param selectedControlColor The selection control color of the [RadioButton] when enabled and
     *   selected.
     * @param unselectedContainerColor The container color of the [RadioButton] when enabled and not
     *   selected.
     * @param unselectedContentColor The content color of a [RadioButton] when enabled and not
     *   selected.
     * @param unselectedSecondaryContentColor The secondary content color of this [RadioButton] when
     *   enabled and not selected, used for secondaryLabel content
     * @param unselectedIconColor The icon color of the [RadioButton] when enabled and not selected.
     * @param unselectedControlColor The selection control color of the [RadioButton] when enabled
     *   and not selected.
     * @param disabledSelectedContainerColor The container color of the [RadioButton] when disabled
     *   and selected.
     * @param disabledSelectedContentColor The content color of the [RadioButton] when disabled and
     *   selected.
     * @param disabledSelectedSecondaryContentColor The secondary content color of the [RadioButton]
     *   when disabled and selected, used for the secondary content.
     * @param disabledSelectedIconColor The icon color of the [RadioButton] when disabled and
     *   selected.
     * @param disabledSelectedControlColor The selection control color of the [RadioButton] when
     *   disabled and selected.
     * @param disabledUnselectedContainerColor The container color of the [RadioButton] when
     *   disabled and not selected.
     * @param disabledUnselectedContentColor The content color of the [RadioButton] when disabled
     *   and not selected.
     * @param disabledUnselectedSecondaryContentColor The secondary content color of the
     *   [RadioButton] when disabled and not selected, used for secondary label content.
     * @param disabledUnselectedIconColor The icon color of the [RadioButton] when disabled and not
     *   selected.
     * @param disabledUnselectedControlColor The selection control color of the [RadioButton] when
     *   disabled and not selected.
     */
    @Composable
    fun radioButtonColors(
        selectedContainerColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
        selectedSecondaryContentColor: Color = Color.Unspecified,
        selectedIconColor: Color = Color.Unspecified,
        selectedControlColor: Color = Color.Unspecified,
        unselectedContainerColor: Color = Color.Unspecified,
        unselectedContentColor: Color = Color.Unspecified,
        unselectedSecondaryContentColor: Color = Color.Unspecified,
        unselectedIconColor: Color = Color.Unspecified,
        unselectedControlColor: Color = Color.Unspecified,
        disabledSelectedContainerColor: Color = Color.Unspecified,
        disabledSelectedContentColor: Color = Color.Unspecified,
        disabledSelectedSecondaryContentColor: Color = Color.Unspecified,
        disabledSelectedIconColor: Color = Color.Unspecified,
        disabledSelectedControlColor: Color = Color.Unspecified,
        disabledUnselectedContainerColor: Color = Color.Unspecified,
        disabledUnselectedContentColor: Color = Color.Unspecified,
        disabledUnselectedSecondaryContentColor: Color = Color.Unspecified,
        disabledUnselectedIconColor: Color = Color.Unspecified,
        disabledUnselectedControlColor: Color = Color.Unspecified
    ) =
        MaterialTheme.colorScheme.defaultRadioButtonColors.copy(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedIconColor = selectedIconColor,
            selectedControlColor = selectedControlColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedIconColor = unselectedIconColor,
            unselectedControlColor = unselectedControlColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor = disabledSelectedSecondaryContentColor,
            disabledSelectedIconColor = disabledSelectedIconColor,
            disabledSelectedControlColor = disabledSelectedControlColor,
            disabledUnselectedContainerColor = disabledUnselectedContainerColor,
            disabledUnselectedContentColor = disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor = disabledUnselectedSecondaryContentColor,
            disabledUnselectedIconColor = disabledUnselectedIconColor,
            disabledUnselectedControlColor = disabledUnselectedControlColor,
        )

    /** Creates a [SplitRadioButtonColors] for use in a [SplitRadioButton]. */
    @Composable
    fun splitRadioButtonColors() = MaterialTheme.colorScheme.defaultSplitRadioButtonColors

    /**
     * Creates a [SplitRadioButtonColors] for use in a [SplitRadioButton].
     *
     * @param selectedContainerColor The container color of the [SplitRadioButton] when enabled and
     *   selected.
     * @param selectedContentColor The content color of the [SplitRadioButton] when enabled and
     *   selected.
     * @param selectedSecondaryContentColor The secondary content color of the [SplitRadioButton]
     *   when enabled and selected, used for secondaryLabel content.
     * @param selectedSplitContainerColor The split container color of the [SplitRadioButton] when
     *   enabled and selected.
     * @param selectedControlColor The color of the radio control when selected
     * @param unselectedContainerColor The container color of the [SplitRadioButton] when enabled
     *   and not selected.
     * @param unselectedContentColor The content color of the [SplitRadioButton] when enabled and
     *   not selected.
     * @param unselectedSecondaryContentColor The secondary content color of the [SplitRadioButton]
     *   when enabled and not selected, used for secondaryLabel content.
     * @param unselectedSplitContainerColor The split container color of the [SplitRadioButton] when
     *   enabled and not selected.
     * @param unselectedControlColor The color of the radio control when unselected
     * @param disabledSelectedContainerColor The container color of the [SplitRadioButton] when
     *   disabled and selected.
     * @param disabledSelectedContentColor The content color of the [SplitRadioButton] when disabled
     *   and selected.
     * @param disabledSelectedSecondaryContentColor The secondary content color of the
     *   [SplitRadioButton] when disabled and selected, used for secondaryLabel content.
     * @param disabledSelectedSplitContainerColor The split container color of the
     *   [SplitRadioButton] when disabled and selected.
     * @param disabledSelectedControlColor The radio control color when disabled and selected.
     * @param disabledUnselectedContainerColor The container color of the [SplitRadioButton] when
     *   disabled and not selected.
     * @param disabledUnselectedContentColor The content color of the [SplitRadioButton] when
     *   disabled and not selected.
     * @param disabledUnselectedSecondaryContentColor The secondary content color of the
     *   [SplitRadioButton] when disabled and not selected, used for secondaryLabel content.
     * @param disabledUnselectedSplitContainerColor The split container color of the
     *   [SplitRadioButton] when disabled and not selected.
     * @param disabledUnselectedControlColor The radio control color when disabled and unselected.
     */
    @Composable
    fun splitRadioButtonColors(
        selectedContainerColor: Color = Color.Unspecified,
        selectedContentColor: Color = Color.Unspecified,
        selectedSecondaryContentColor: Color = Color.Unspecified,
        selectedSplitContainerColor: Color = Color.Unspecified,
        selectedControlColor: Color = Color.Unspecified,
        unselectedContainerColor: Color = Color.Unspecified,
        unselectedContentColor: Color = Color.Unspecified,
        unselectedSecondaryContentColor: Color = Color.Unspecified,
        unselectedSplitContainerColor: Color = Color.Unspecified,
        unselectedControlColor: Color = Color.Unspecified,
        disabledSelectedContainerColor: Color = Color.Unspecified,
        disabledSelectedContentColor: Color = Color.Unspecified,
        disabledSelectedSecondaryContentColor: Color = Color.Unspecified,
        disabledSelectedSplitContainerColor: Color = Color.Unspecified,
        disabledSelectedControlColor: Color = Color.Unspecified,
        disabledUnselectedContainerColor: Color = Color.Unspecified,
        disabledUnselectedContentColor: Color = Color.Unspecified,
        disabledUnselectedSecondaryContentColor: Color = Color.Unspecified,
        disabledUnselectedSplitContainerColor: Color = Color.Unspecified,
        disabledUnselectedControlColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultSplitRadioButtonColors.copy(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            selectedSecondaryContentColor = selectedSecondaryContentColor,
            selectedSplitContainerColor = selectedSplitContainerColor,
            selectedControlColor = selectedControlColor,
            unselectedContainerColor = unselectedContainerColor,
            unselectedContentColor = unselectedContentColor,
            unselectedSecondaryContentColor = unselectedSecondaryContentColor,
            unselectedSplitContainerColor = unselectedSplitContainerColor,
            unselectedControlColor = unselectedControlColor,
            disabledSelectedContainerColor = disabledSelectedContainerColor,
            disabledSelectedContentColor = disabledSelectedContentColor,
            disabledSelectedSecondaryContentColor = disabledSelectedSecondaryContentColor,
            disabledSelectedSplitContainerColor = disabledSelectedSplitContainerColor,
            disabledSelectedControlColor = disabledSelectedControlColor,
            disabledUnselectedContainerColor = disabledUnselectedContainerColor,
            disabledUnselectedContentColor = disabledUnselectedContentColor,
            disabledUnselectedSecondaryContentColor = disabledUnselectedSecondaryContentColor,
            disabledUnselectedSplitContainerColor = disabledUnselectedSplitContainerColor,
            disabledUnselectedControlColor = disabledUnselectedControlColor,
        )

    internal val LabelSpacerSize = 2.dp
    private val HorizontalPadding = 14.dp
    private val VerticalPadding = 8.dp

    /** The default content padding used by [RadioButton] */
    val ContentPadding: PaddingValues =
        PaddingValues(
            start = HorizontalPadding,
            top = VerticalPadding,
            end = HorizontalPadding,
            bottom = VerticalPadding
        )

    private val ColorScheme.defaultRadioButtonColors: RadioButtonColors
        get() {
            return defaultRadioButtonColorsCached
                ?: RadioButtonColors(
                        selectedContainerColor =
                            fromToken(RadioButtonTokens.SelectedContainerColor),
                        selectedContentColor = fromToken(RadioButtonTokens.SelectedContentColor),
                        selectedSecondaryContentColor =
                            fromToken(RadioButtonTokens.SelectedSecondaryLabelColor)
                                .copy(alpha = RadioButtonTokens.SelectedSecondaryLabelOpacity),
                        selectedIconColor = fromToken(RadioButtonTokens.SelectedIconColor),
                        selectedControlColor = fromToken(RadioButtonTokens.SelectedControlColor),
                        unselectedContainerColor =
                            fromToken(RadioButtonTokens.UnselectedContainerColor),
                        unselectedContentColor =
                            fromToken(RadioButtonTokens.UnselectedContentColor),
                        unselectedSecondaryContentColor =
                            fromToken(RadioButtonTokens.UnselectedSecondaryLabelColor),
                        unselectedIconColor = fromToken(RadioButtonTokens.UnselectedIconColor),
                        unselectedControlColor =
                            fromToken(RadioButtonTokens.UnselectedControlColor),
                        disabledSelectedContainerColor =
                            fromToken(RadioButtonTokens.DisabledSelectedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        RadioButtonTokens.DisabledSelectedContainerOpacity
                                ),
                        disabledSelectedContentColor =
                            fromToken(RadioButtonTokens.DisabledSelectedContentColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledSelectedSecondaryContentColor =
                            fromToken(RadioButtonTokens.DisabledSelectedSecondaryLabelColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledSelectedIconColor =
                            fromToken(RadioButtonTokens.DisabledSelectedIconColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledSelectedControlColor =
                            fromToken(RadioButtonTokens.DisabledSelectedControlColor)
                                .toDisabledColor(
                                    disabledAlpha = RadioButtonTokens.DisabledSelectedControlOpacity
                                ),
                        disabledUnselectedContainerColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        RadioButtonTokens.DisabledUnselectedContainerOpacity
                                ),
                        disabledUnselectedContentColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedContentColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledUnselectedSecondaryContentColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedSecondaryLabelColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledUnselectedIconColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedIconColor)
                                .toDisabledColor(disabledAlpha = RadioButtonTokens.DisabledOpacity),
                        disabledUnselectedControlColor =
                            fromToken(RadioButtonTokens.DisabledUnselectedControlColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        RadioButtonTokens.DisabledUnselectedControlOpacity
                                ),
                    )
                    .also { defaultRadioButtonColorsCached = it }
        }

    private val ColorScheme.defaultSplitRadioButtonColors: SplitRadioButtonColors
        get() {
            return defaultSplitRadioButtonColorsCached
                ?: SplitRadioButtonColors(
                        selectedContainerColor =
                            fromToken(SplitRadioButtonTokens.SelectedContainerColor),
                        selectedContentColor =
                            fromToken(SplitRadioButtonTokens.SelectedContentColor),
                        selectedSecondaryContentColor =
                            fromToken(SplitRadioButtonTokens.SelectedSecondaryLabelColor)
                                .copy(alpha = SplitRadioButtonTokens.SelectedSecondaryLabelOpacity),
                        selectedSplitContainerColor =
                            fromToken(SplitRadioButtonTokens.SelectedSplitContainerColor)
                                .copy(alpha = SplitRadioButtonTokens.SelectedSplitContainerOpacity),
                        selectedControlColor =
                            fromToken(SplitRadioButtonTokens.SelectedControlColor),
                        unselectedContainerColor =
                            fromToken(SplitRadioButtonTokens.UnselectedContainerColor),
                        unselectedContentColor =
                            fromToken(SplitRadioButtonTokens.UnselectedContentColor),
                        unselectedSecondaryContentColor =
                            fromToken(SplitRadioButtonTokens.UnselectedSecondaryLabelColor),
                        unselectedSplitContainerColor =
                            fromToken(SplitRadioButtonTokens.UnselectedSplitContainerColor),
                        unselectedControlColor =
                            fromToken(SplitRadioButtonTokens.UnselectedControlColor),
                        disabledSelectedContainerColor =
                            fromToken(SplitRadioButtonTokens.DisabledSelectedContainerColor)
                                .copy(
                                    alpha = SplitRadioButtonTokens.DisabledSelectedContainerOpacity
                                ),
                        disabledSelectedContentColor =
                            fromToken(SplitRadioButtonTokens.DisabledSelectedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitRadioButtonTokens.DisabledOpacity
                                ),
                        disabledSelectedSecondaryContentColor =
                            fromToken(SplitRadioButtonTokens.DisabledSelectedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitRadioButtonTokens.DisabledOpacity
                                ),
                        disabledSelectedSplitContainerColor =
                            fromToken(SplitRadioButtonTokens.DisabledSelectedSplitContainerColor)
                                .copy(
                                    alpha =
                                        SplitRadioButtonTokens.DisabledSelectedSplitContainerOpacity
                                ),
                        disabledSelectedControlColor =
                            fromToken(SplitRadioButtonTokens.DisabledSelectedControlColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitRadioButtonTokens.DisabledSelectedControlOpacity
                                ),
                        disabledUnselectedContainerColor =
                            fromToken(SplitRadioButtonTokens.DisabledUnselectedContainerColor)
                                .copy(
                                    alpha =
                                        SplitRadioButtonTokens.DisabledUnselectedContainerOpacity
                                ),
                        disabledUnselectedContentColor =
                            fromToken(SplitRadioButtonTokens.DisabledUnselectedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitRadioButtonTokens.DisabledOpacity
                                ),
                        disabledUnselectedSecondaryContentColor =
                            fromToken(SplitRadioButtonTokens.DisabledUnselectedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitRadioButtonTokens.DisabledOpacity
                                ),
                        disabledUnselectedSplitContainerColor =
                            fromToken(SplitRadioButtonTokens.DisabledUnselectedSplitContainerColor)
                                .copy(
                                    alpha =
                                        SplitRadioButtonTokens
                                            .DisabledUnselectedSplitContainerOpacity
                                ),
                        disabledUnselectedControlColor =
                            fromToken(SplitRadioButtonTokens.DisabledUnselectedControlColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitRadioButtonTokens.DisabledUnselectedControlOpacity
                                ),
                    )
                    .also { defaultSplitRadioButtonColorsCached = it }
        }
}

/**
 * Represents the different container and content colors used for [RadioButton] in various states,
 * that are selected, unselected, enabled and disabled.
 *
 * @param selectedContainerColor Container or background color when the radio button is selected
 * @param selectedContentColor Color of the content (e.g. label) when the radio button is selected
 * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label) when
 *   the radio button is selected
 * @param selectedIconColor Color of the icon when the radio button is selected
 * @param selectedControlColor Color of the radio selection control when the radio button is
 *   selected
 * @param unselectedContainerColor Container or background color when the radio button is unselected
 * @param unselectedContentColor Color of the content (e.g. label) when the radio button is
 *   unselected
 * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label) when
 *   the radio button is unselected
 * @param unselectedIconColor Color of the icon when the radio button is unselected
 * @param unselectedControlColor Color of the radio selection control when the radio button is
 *   unselected
 * @param disabledSelectedContainerColor Container or background color when the radio button is
 *   disabled and selected
 * @param disabledSelectedContentColor Color of content (e.g. label) when the radio button is
 *   disabled and selected
 * @param disabledSelectedSecondaryContentColor Color of the secondary content like secondary label
 *   when the radio button is disabled and selected
 * @param disabledSelectedIconColor Icon color when the radio button is disabled and selected
 * @param disabledSelectedControlColor Radio selection control color when the radio button is
 *   disabled and selected
 * @param disabledUnselectedContainerColor Container or background color when the radio button is
 *   disabled and unselected
 * @param disabledUnselectedContentColor Color of the content (e.g. label) when the radio button is
 *   disabled and unselected
 * @param disabledUnselectedSecondaryContentColor Color of the secondary content like secondary
 *   label when the radio button is disabled and unselected
 * @param disabledUnselectedIconColor Icon color when the radio button is disabled and unselected
 * @param disabledUnselectedControlColor Radio selection control color when the radio button is
 *   disabled and unselected
 * @constructor [RadioButtonColors] constructor to be used with [RadioButton]
 */
@Immutable
class RadioButtonColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val selectedSecondaryContentColor: Color,
    val selectedIconColor: Color,
    val selectedControlColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val unselectedSecondaryContentColor: Color,
    val unselectedIconColor: Color,
    val unselectedControlColor: Color,
    val disabledSelectedContainerColor: Color,
    val disabledSelectedContentColor: Color,
    val disabledSelectedSecondaryContentColor: Color,
    val disabledSelectedIconColor: Color,
    val disabledSelectedControlColor: Color,
    val disabledUnselectedContainerColor: Color,
    val disabledUnselectedContentColor: Color,
    val disabledUnselectedSecondaryContentColor: Color,
    val disabledUnselectedIconColor: Color,
    val disabledUnselectedControlColor: Color,
) {
    /**
     * Returns a copy of this RadioButtonColors optionally overriding some of the values.
     *
     * @param selectedContainerColor Container or background color when the radio button is selected
     * @param selectedContentColor Color of the content (e.g. label) when the radio button is
     *   selected
     * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
     *   when the radio button is selected
     * @param selectedIconColor Color of the icon when the radio button is selected
     * @param selectedControlColor Color of the radio selection control when the radio button is
     *   selected
     * @param unselectedContainerColor Container or background color when the radio button is
     *   unselected
     * @param unselectedContentColor Color of the content (e.g. label) when the radio button is
     *   unselected
     * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
     *   when the radio button is unselected
     * @param unselectedIconColor Color of the icon when the radio button is unselected
     * @param unselectedControlColor Color of the radio selection control when the radio button is
     *   unselected
     * @param disabledSelectedContainerColor Container or background color when the radio button is
     *   disabled and selected
     * @param disabledSelectedContentColor Color of content (e.g. label) when the radio button is
     *   disabled and selected
     * @param disabledSelectedSecondaryContentColor Color of the secondary content like secondary
     *   label when the radio button is disabled and selected
     * @param disabledSelectedIconColor Icon color when the radio button is disabled and selected
     * @param disabledSelectedControlColor Radio selection control color when the radio button is
     *   disabled and selected
     * @param disabledUnselectedContainerColor Container or background color when the radio button
     *   is disabled and unselected
     * @param disabledUnselectedContentColor Color of the content (e.g. label) when the radio button
     *   is disabled and unselected
     * @param disabledUnselectedSecondaryContentColor Color of the secondary content like secondary
     *   label when the radio button is disabled and unselected
     * @param disabledUnselectedIconColor Icon color when the radio button is disabled and
     *   unselected
     * @param disabledUnselectedControlColor Radio selection control color when the radio button is
     *   disabled and unselected
     */
    fun copy(
        selectedContainerColor: Color = this.selectedContainerColor,
        selectedContentColor: Color = this.selectedContentColor,
        selectedSecondaryContentColor: Color = this.selectedSecondaryContentColor,
        selectedIconColor: Color = this.selectedIconColor,
        selectedControlColor: Color = this.selectedControlColor,
        unselectedContainerColor: Color = this.unselectedContainerColor,
        unselectedContentColor: Color = this.unselectedContentColor,
        unselectedSecondaryContentColor: Color = this.unselectedSecondaryContentColor,
        unselectedIconColor: Color = this.unselectedIconColor,
        unselectedControlColor: Color = this.unselectedControlColor,
        disabledSelectedContainerColor: Color = this.disabledSelectedContainerColor,
        disabledSelectedContentColor: Color = this.disabledSelectedContentColor,
        disabledSelectedSecondaryContentColor: Color = this.disabledSelectedSecondaryContentColor,
        disabledSelectedIconColor: Color = this.disabledSelectedIconColor,
        disabledSelectedControlColor: Color = this.disabledSelectedControlColor,
        disabledUnselectedContainerColor: Color = this.disabledUnselectedContainerColor,
        disabledUnselectedContentColor: Color = this.disabledUnselectedContentColor,
        disabledUnselectedSecondaryContentColor: Color =
            this.disabledUnselectedSecondaryContentColor,
        disabledUnselectedIconColor: Color = this.disabledUnselectedIconColor,
        disabledUnselectedControlColor: Color = this.disabledUnselectedControlColor,
    ): RadioButtonColors =
        RadioButtonColors(
            selectedContainerColor =
                selectedContainerColor.takeOrElse { this.selectedContainerColor },
            selectedContentColor = selectedContentColor.takeOrElse { this.selectedContentColor },
            selectedSecondaryContentColor =
                selectedSecondaryContentColor.takeOrElse { this.selectedSecondaryContentColor },
            selectedIconColor = selectedIconColor.takeOrElse { this.selectedIconColor },
            selectedControlColor = selectedControlColor.takeOrElse { this.selectedControlColor },
            unselectedContainerColor =
                unselectedContainerColor.takeOrElse { this.unselectedContainerColor },
            unselectedContentColor =
                unselectedContentColor.takeOrElse { this.unselectedContentColor },
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor.takeOrElse { this.unselectedSecondaryContentColor },
            unselectedIconColor = unselectedIconColor.takeOrElse { this.unselectedIconColor },
            unselectedControlColor =
                unselectedControlColor.takeOrElse { this.unselectedControlColor },
            disabledSelectedContainerColor =
                disabledSelectedContainerColor.takeOrElse { this.disabledSelectedContainerColor },
            disabledSelectedContentColor =
                disabledSelectedContentColor.takeOrElse { this.disabledSelectedContentColor },
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor.takeOrElse {
                    this.disabledSelectedSecondaryContentColor
                },
            disabledSelectedIconColor =
                disabledSelectedIconColor.takeOrElse { this.disabledSelectedIconColor },
            disabledSelectedControlColor =
                disabledSelectedControlColor.takeOrElse { this.disabledSelectedControlColor },
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor.takeOrElse {
                    this.disabledUnselectedContainerColor
                },
            disabledUnselectedContentColor =
                disabledUnselectedContentColor.takeOrElse { this.disabledUnselectedContentColor },
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor.takeOrElse {
                    this.disabledUnselectedSecondaryContentColor
                },
            disabledUnselectedIconColor =
                disabledUnselectedIconColor.takeOrElse { this.disabledUnselectedIconColor },
            disabledUnselectedControlColor =
                disabledUnselectedControlColor.takeOrElse { this.disabledUnselectedControlColor },
        )

    /**
     * Determines the container color based on whether the radio button is [enabled] and [selected].
     *
     * @param enabled Whether the [RadioButton] is enabled
     * @param selected Whether the [RadioButton] is checked
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
     * Determines the content color based on whether the radio button is [enabled] and [selected].
     *
     * @param enabled Whether the [RadioButton] is enabled
     * @param selected Whether the [RadioButton] is checked
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
     * Represents the secondary content color depending on the [enabled] and [selected] properties.
     *
     * @param enabled Whether the [RadioButton] is enabled.
     * @param selected Whether the [RadioButton] is currently selected or unselected.
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
     * Represents the icon color for the [RadioButton] depending on the [enabled] and [selected]
     * properties.
     *
     * @param enabled Whether the [RadioButton] is enabled.
     * @param selected Whether the [RadioButton] is currently selected or unselected.
     */
    @Composable
    internal fun iconColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedIconColor,
            uncheckedColor = unselectedIconColor,
            disabledCheckedColor = disabledSelectedIconColor,
            disabledUncheckedColor = disabledUnselectedIconColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the selection control color for the [RadioButton] depending on the [enabled] and
     * [selected] properties.
     *
     * @param enabled Whether the [RadioButton] is enabled.
     * @param selected Whether the [RadioButton] is currently selected or unselected.
     */
    @Composable
    internal fun controlColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedControlColor,
            uncheckedColor = unselectedControlColor,
            disabledCheckedColor = disabledSelectedControlColor,
            disabledUncheckedColor = disabledUnselectedControlColor,
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
        if (selectedControlColor != other.selectedControlColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedIconColor != other.unselectedIconColor) return false
        if (unselectedControlColor != other.unselectedControlColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor != other.disabledSelectedSecondaryContentColor)
            return false
        if (disabledSelectedIconColor != other.disabledSelectedIconColor) return false
        if (disabledSelectedControlColor != other.disabledSelectedControlColor) return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (
            disabledUnselectedSecondaryContentColor != other.disabledUnselectedSecondaryContentColor
        )
            return false
        if (disabledUnselectedIconColor != other.disabledUnselectedIconColor) return false
        if (disabledUnselectedControlColor != other.disabledUnselectedControlColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedIconColor.hashCode()
        result = 31 * result + selectedControlColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedIconColor.hashCode()
        result = 31 * result + unselectedControlColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedIconColor.hashCode()
        result = 31 * result + disabledSelectedControlColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedIconColor.hashCode()
        result = 31 * result + disabledUnselectedControlColor.hashCode()
        return result
    }
}

/**
 * Represents the different colors used in [SplitRadioButton] in different states.
 *
 * @param selectedContainerColor Container or background color when the [SplitRadioButton] is
 *   selected
 * @param selectedContentColor Color of the content (e.g. label) when the [SplitRadioButton] is
 *   selected
 * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label) when
 *   the [SplitRadioButton] is selected
 * @param selectedSplitContainerColor Split container color when the [SplitRadioButton] is selected
 * @param selectedControlColor Selection control color when the [SplitRadioButton] is selected
 * @param unselectedContainerColor Container or background color when the [SplitRadioButton] is
 *   unselected
 * @param unselectedContentColor Color of the content (e.g. label) when the [SplitRadioButton] is
 *   unselected
 * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label) when
 *   the [SplitRadioButton] is unselected
 * @param unselectedSplitContainerColor Split container color when the [SplitRadioButton] is
 *   unselected
 * @param unselectedControlColor Selection control color when the [SplitRadioButton] is unselected
 * @param disabledSelectedContainerColor Container color when the [SplitRadioButton] is disabled and
 *   selected
 * @param disabledSelectedContentColor Color of the content (e.g. label) when the [SplitRadioButton]
 *   is disabled and selected
 * @param disabledSelectedSecondaryContentColor Color of the secondary content (e.g. secondary
 *   label) when the [SplitRadioButton] is disabled and selected
 * @param disabledSelectedSplitContainerColor Split container color when the [SplitRadioButton] is
 *   disabled and selected
 * @param disabledSelectedControlColor Selection control color when the [SplitRadioButton] is
 *   disabled and selected
 * @param disabledUnselectedContainerColor Container color when the [SplitRadioButton] is unselected
 *   and disabled
 * @param disabledUnselectedContentColor Color of the content (e.g. label) when the split radio
 *   button is unselected and disabled
 * @param disabledUnselectedSecondaryContentColor Color of the secondary content (e.g. secondary
 *   label) when the [SplitRadioButton] is unselected and disabled
 * @param disabledUnselectedSplitContainerColor Split container color when the [SplitRadioButton] is
 *   unselected and disabled
 * @param disabledUnselectedControlColor Selection control color when the [SplitRadioButton] is
 *   unselected and disabled
 * @constructor [SplitRadioButtonColors] constructor to be used with [SplitRadioButton]
 */
class SplitRadioButtonColors
constructor(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val selectedSecondaryContentColor: Color,
    val selectedSplitContainerColor: Color,
    val selectedControlColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val unselectedSecondaryContentColor: Color,
    val unselectedSplitContainerColor: Color,
    val unselectedControlColor: Color,
    val disabledSelectedContainerColor: Color,
    val disabledSelectedContentColor: Color,
    val disabledSelectedSecondaryContentColor: Color,
    val disabledSelectedSplitContainerColor: Color,
    val disabledSelectedControlColor: Color,
    val disabledUnselectedContainerColor: Color,
    val disabledUnselectedContentColor: Color,
    val disabledUnselectedSecondaryContentColor: Color,
    val disabledUnselectedSplitContainerColor: Color,
    val disabledUnselectedControlColor: Color,
) {
    /**
     * Returns a copy of this SplitRadioButtonColors optionally overriding some of the values.
     *
     * @param selectedContainerColor Container or background color when the [SplitRadioButton] is
     *   selected
     * @param selectedContentColor Color of the content (e.g. label) when the [SplitRadioButton] is
     *   selected
     * @param selectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
     *   when the [SplitRadioButton] is selected
     * @param selectedSplitContainerColor Split container color when the [SplitRadioButton] is
     *   selected
     * @param selectedControlColor Selection control color when the [SplitRadioButton] is selected
     * @param unselectedContainerColor Container or background color when the [SplitRadioButton] is
     *   unselected
     * @param unselectedContentColor Color of the content (e.g. label) when the [SplitRadioButton]
     *   is unselected
     * @param unselectedSecondaryContentColor Color of the secondary content (e.g. secondary label)
     *   when the [SplitRadioButton] is unselected
     * @param unselectedSplitContainerColor Split container color when the [SplitRadioButton] is
     *   unselected
     * @param unselectedControlColor Selection control color when the [SplitRadioButton] is
     *   unselected
     * @param disabledSelectedContainerColor Container color when the [SplitRadioButton] is disabled
     *   and selected
     * @param disabledSelectedContentColor Color of the content (e.g. label) when the
     *   [SplitRadioButton] is disabled and selected
     * @param disabledSelectedSecondaryContentColor Color of the secondary content (e.g. secondary
     *   label) when the [SplitRadioButton] is disabled and selected
     * @param disabledSelectedSplitContainerColor Split container color when the [SplitRadioButton]
     *   is disabled and selected
     * @param disabledSelectedControlColor Selection control color when the [SplitRadioButton] is
     *   disabled and selected
     * @param disabledUnselectedContainerColor Container color when the [SplitRadioButton] is
     *   unselected and disabled
     * @param disabledUnselectedContentColor Color of the content (e.g. label) when the split radio
     *   button is unselected and disabled
     * @param disabledUnselectedSecondaryContentColor Color of the secondary content (e.g. secondary
     *   label) when the [SplitRadioButton] is unselected and disabled
     * @param disabledUnselectedSplitContainerColor Split container color when the
     *   [SplitRadioButton] is unselected and disabled
     * @param disabledUnselectedControlColor Selection control color when the [SplitRadioButton] is
     *   unselected and disabled
     */
    fun copy(
        selectedContainerColor: Color = this.selectedContainerColor,
        selectedContentColor: Color = this.selectedContentColor,
        selectedSecondaryContentColor: Color = this.selectedSecondaryContentColor,
        selectedSplitContainerColor: Color = this.selectedSplitContainerColor,
        selectedControlColor: Color = this.selectedControlColor,
        unselectedContainerColor: Color = this.unselectedContainerColor,
        unselectedContentColor: Color = this.unselectedContentColor,
        unselectedSecondaryContentColor: Color = this.unselectedSecondaryContentColor,
        unselectedSplitContainerColor: Color = this.unselectedSplitContainerColor,
        unselectedControlColor: Color = this.unselectedControlColor,
        disabledSelectedContainerColor: Color = this.disabledSelectedContainerColor,
        disabledSelectedContentColor: Color = this.disabledSelectedContentColor,
        disabledSelectedSecondaryContentColor: Color = this.disabledSelectedSecondaryContentColor,
        disabledSelectedSplitContainerColor: Color = this.disabledSelectedSplitContainerColor,
        disabledSelectedControlColor: Color = this.disabledSelectedControlColor,
        disabledUnselectedContainerColor: Color = this.disabledUnselectedContainerColor,
        disabledUnselectedContentColor: Color = this.disabledUnselectedContentColor,
        disabledUnselectedSecondaryContentColor: Color =
            this.disabledUnselectedSecondaryContentColor,
        disabledUnselectedSplitContainerColor: Color = this.disabledUnselectedSplitContainerColor,
        disabledUnselectedControlColor: Color = this.disabledUnselectedControlColor,
    ): SplitRadioButtonColors =
        SplitRadioButtonColors(
            selectedContainerColor =
                selectedContainerColor.takeOrElse { this.selectedContainerColor },
            selectedContentColor = selectedContentColor.takeOrElse { this.selectedContentColor },
            selectedSecondaryContentColor =
                selectedSecondaryContentColor.takeOrElse { this.selectedSecondaryContentColor },
            selectedSplitContainerColor =
                selectedSplitContainerColor.takeOrElse { this.selectedSplitContainerColor },
            selectedControlColor = selectedControlColor.takeOrElse { this.selectedControlColor },
            unselectedContainerColor =
                unselectedContainerColor.takeOrElse { this.unselectedContainerColor },
            unselectedContentColor =
                unselectedContentColor.takeOrElse { this.unselectedContentColor },
            unselectedSecondaryContentColor =
                unselectedSecondaryContentColor.takeOrElse { this.unselectedSecondaryContentColor },
            unselectedSplitContainerColor =
                unselectedSplitContainerColor.takeOrElse { this.unselectedSplitContainerColor },
            unselectedControlColor =
                unselectedControlColor.takeOrElse { this.unselectedControlColor },
            disabledSelectedContainerColor =
                disabledSelectedContainerColor.takeOrElse { this.disabledSelectedContainerColor },
            disabledSelectedContentColor =
                disabledSelectedContentColor.takeOrElse { this.disabledSelectedContentColor },
            disabledSelectedSecondaryContentColor =
                disabledSelectedSecondaryContentColor.takeOrElse {
                    this.disabledSelectedSecondaryContentColor
                },
            disabledSelectedSplitContainerColor =
                disabledSelectedSplitContainerColor.takeOrElse {
                    this.disabledSelectedSplitContainerColor
                },
            disabledSelectedControlColor =
                disabledSelectedControlColor.takeOrElse { this.disabledSelectedControlColor },
            disabledUnselectedContainerColor =
                disabledUnselectedContainerColor.takeOrElse {
                    this.disabledUnselectedContainerColor
                },
            disabledUnselectedContentColor =
                disabledUnselectedContentColor.takeOrElse { this.disabledUnselectedContentColor },
            disabledUnselectedSecondaryContentColor =
                disabledUnselectedSecondaryContentColor.takeOrElse {
                    this.disabledUnselectedSecondaryContentColor
                },
            disabledUnselectedSplitContainerColor =
                disabledUnselectedSplitContainerColor.takeOrElse {
                    this.disabledUnselectedSplitContainerColor
                },
            disabledUnselectedControlColor =
                disabledUnselectedControlColor.takeOrElse { this.disabledUnselectedControlColor },
        )

    /**
     * Determines the container color based on whether the [SplitRadioButton] is [enabled] and
     * [selected].
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
     * Determines the content color based on whether the [SplitRadioButton] is [enabled] and
     * [selected].
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
     * Represents the secondary content color for the [SplitRadioButton] depending on the [enabled]
     * and [selected] properties.
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
     * Represents the split container for the [SplitRadioButton] color depending on the [enabled]
     * and [selected] properties.
     *
     * @param enabled Whether the [SplitRadioButton] is enabled.
     * @param selected Whether the [SplitRadioButton] is currently selected.
     */
    @Composable
    internal fun splitContainerColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedSplitContainerColor,
            uncheckedColor = unselectedSplitContainerColor,
            disabledCheckedColor = disabledSelectedSplitContainerColor,
            disabledUncheckedColor = disabledUnselectedSplitContainerColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the selection control color for the [SplitRadioButton] depending on the [enabled]
     * and [selected] properties.
     *
     * @param enabled Whether the [SplitRadioButton] is enabled.
     * @param selected Whether the [SplitRadioButton] is currently selected or unselected.
     */
    @Composable
    internal fun controlColor(enabled: Boolean, selected: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = selected,
            checkedColor = selectedControlColor,
            uncheckedColor = unselectedControlColor,
            disabledCheckedColor = disabledSelectedControlColor,
            disabledUncheckedColor = disabledUnselectedControlColor,
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
        if (selectedControlColor != other.selectedControlColor) return false
        if (unselectedContainerColor != other.unselectedContainerColor) return false
        if (unselectedContentColor != other.unselectedContentColor) return false
        if (unselectedSecondaryContentColor != other.unselectedSecondaryContentColor) return false
        if (unselectedSplitContainerColor != other.unselectedSplitContainerColor) return false
        if (unselectedControlColor != other.unselectedControlColor) return false
        if (disabledSelectedContainerColor != other.disabledSelectedContainerColor) return false
        if (disabledSelectedContentColor != other.disabledSelectedContentColor) return false
        if (disabledSelectedSecondaryContentColor != other.disabledSelectedSecondaryContentColor)
            return false
        if (disabledSelectedSplitContainerColor != other.disabledSelectedSplitContainerColor)
            return false
        if (disabledSelectedControlColor != other.disabledSelectedControlColor) return false
        if (disabledUnselectedContainerColor != other.disabledUnselectedContainerColor) return false
        if (disabledUnselectedContentColor != other.disabledUnselectedContentColor) return false
        if (
            disabledUnselectedSecondaryContentColor != other.disabledUnselectedSecondaryContentColor
        )
            return false
        if (disabledUnselectedSplitContainerColor != other.disabledUnselectedSplitContainerColor)
            return false
        if (disabledUnselectedControlColor != other.disabledUnselectedControlColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selectedContainerColor.hashCode()
        result = 31 * result + selectedContentColor.hashCode()
        result = 31 * result + selectedSecondaryContentColor.hashCode()
        result = 31 * result + selectedSplitContainerColor.hashCode()
        result = 31 * result + selectedControlColor.hashCode()
        result = 31 * result + unselectedContainerColor.hashCode()
        result = 31 * result + unselectedContentColor.hashCode()
        result = 31 * result + unselectedSecondaryContentColor.hashCode()
        result = 31 * result + unselectedSplitContainerColor.hashCode()
        result = 31 * result + unselectedControlColor.hashCode()
        result = 31 * result + disabledSelectedContainerColor.hashCode()
        result = 31 * result + disabledSelectedContentColor.hashCode()
        result = 31 * result + disabledSelectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledSelectedSplitContainerColor.hashCode()
        result = 31 * result + disabledSelectedControlColor.hashCode()
        result = 31 * result + disabledUnselectedContainerColor.hashCode()
        result = 31 * result + disabledUnselectedContentColor.hashCode()
        result = 31 * result + disabledUnselectedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUnselectedSplitContainerColor.hashCode()
        result = 31 * result + disabledUnselectedControlColor.hashCode()
        return result
    }
}

// RadioControl provides an animated radio selection control for use in
// [RadioButton] or [SplitRadioButton].
@Composable
internal fun RadioControl(
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    color: @Composable (enabled: Boolean, selected: Boolean) -> State<Color>,
) =
    androidx.wear.compose.materialcore.RadioButton(
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        ringColor = color,
        dotColor = color,
        onClick = null,
        interactionSource = null,
        dotRadiusAnimationSpec = PROGRESS_ANIMATION_SPEC,
        dotAlphaAnimationSpec = PROGRESS_ANIMATION_SPEC,
        width = CONTROL_WIDTH,
        height = CONTROL_HEIGHT,
        ripple = ripple()
    )

@Composable
private fun RowScope.Labels(
    label: @Composable RowScope.() -> Unit,
    secondaryLabel: @Composable (RowScope.() -> Unit)?
) {
    Column(modifier = Modifier.weight(1.0f)) {
        Row(content = label)
        if (secondaryLabel != null) {
            Spacer(modifier = Modifier.size(RadioButtonDefaults.LabelSpacerSize))
            Row(content = secondaryLabel)
        }
    }
}

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color>
    @Composable get() = MaterialTheme.motionScheme.slowEffectsSpec()
private val PROGRESS_ANIMATION_SPEC: FiniteAnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
private val SELECTION_CONTROL_WIDTH = 32.dp
private val SELECTION_CONTROL_HEIGHT = 24.dp
private val SELECTION_CONTROL_SPACING = 6.dp
private val ICON_SPACING = 6.dp
private val MIN_HEIGHT = 52.dp
private val SPLIT_MIN_WIDTH = 48.dp
private val CONTROL_WIDTH = 24.dp
private val CONTROL_HEIGHT = 24.dp
private val SPLIT_SECTIONS_SHAPE = ShapeTokens.CornerExtraSmall
