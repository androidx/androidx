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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.tokens.ShapeTokens
import androidx.wear.compose.material3.tokens.SplitSwitchButtonTokens
import androidx.wear.compose.material3.tokens.SwitchButtonTokens
import androidx.wear.compose.materialcore.SelectionStage
import androidx.wear.compose.materialcore.animateSelectionColor
import androidx.wear.compose.materialcore.animateTick
import androidx.wear.compose.materialcore.isLayoutDirectionRtl

/**
 * The Wear Material [SwitchButton] offers three slots and a specific layout for an icon, a label,
 * and a secondaryLabel. The icon and secondaryLabel are optional. The items are laid out in a row
 * with the optional icon at the start and a column containing the two label slots in the middle.
 *
 * The [SwitchButton] is Stadium shaped. The label should take no more than 3 lines of text. The
 * secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [SwitchButton] height adjusts to accommodate the contents. The label and secondary
 * label are start aligned by default.
 *
 * Example of a SwitchButton:
 *
 * @sample androidx.wear.compose.material3.samples.SwitchButtonSample
 *
 * [SwitchButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * The recommended set of [SwitchButton] colors can be obtained from [SwitchButtonDefaults], e.g.
 * [SwitchButtonDefaults.switchButtonColors].
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is changed.
 * @param modifier Modifier to be applied to the [SwitchButton].
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [SwitchButtonColors] that will be used to resolve the background and content color
 *   for this button in different states.
 * @param contentPadding The spacing values to apply internally between the container and the
 *   content.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 *   button's appearance or preview the button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param icon An optional slot for providing an icon to indicate the purpose of the button. The
 *   contents are expected to be a horizontally and vertically center aligned icon of size 24.dp.
 * @param secondaryLabel A slot for providing the button's secondary label. The contents are
 *   expected to be text which is "start" aligned.
 * @param label A slot for providing the button's main label. The contents are expected to be text
 *   which is "start" aligned and no more than 3 lines of text.
 */
@Composable
fun SwitchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SwitchButtonDefaults.switchButtonShape,
    colors: SwitchButtonColors = SwitchButtonDefaults.switchButtonColors(),
    contentPadding: PaddingValues = SwitchButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    icon: @Composable (BoxScope.() -> Unit)? = null,
    secondaryLabel: @Composable (RowScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    androidx.wear.compose.materialcore.ToggleButton(
        checked = checked,
        onCheckedChange = {
            hapticFeedback.performHapticFeedback(
                if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
            )
            onCheckedChange(it)
        },
        label =
            provideScopeContent(
                contentColor = colors.contentColor(enabled = enabled, checked),
                textStyle = SwitchButtonTokens.LabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 3,
                        textAlign = TextAlign.Start,
                    ),
                content = label
            ),
        toggleControl = {
            Switch(
                checked = checked,
                enabled = enabled,
                thumbColor = { enabled, checked ->
                    colors.thumbColor(enabled = enabled, checked = checked)
                },
                thumbIconColor = { enabled, checked ->
                    colors.thumbIconColor(enabled = enabled, checked = checked)
                },
                trackColor = { enabled, checked ->
                    colors.trackColor(enabled = enabled, checked = checked)
                },
                trackBorderColor = { enabled, checked ->
                    colors.trackBorderColor(enabled = enabled, checked = checked)
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
                textStyle = SwitchButtonTokens.SecondaryLabelFont.value,
                textConfiguration =
                    TextConfiguration(
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        textAlign = TextAlign.Start,
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
        toggleControlWidth = SWITCH_WIDTH,
        toggleControlHeight = SWITCH_OUTER_HEIGHT,
        labelSpacerSize = SwitchButtonDefaults.LabelSpacerSize,
        toggleControlSpacing = TOGGLE_CONTROL_SPACING,
        iconSpacing = ICON_SPACING,
        ripple = ripple()
    )
}

/**
 * The Wear Material [SplitSwitchButton] offers slots and a specific layout for a label and
 * secondary label. The secondaryLabel is optional. The items are laid out with a column containing
 * the two label slots and a Switch at the end.
 *
 * The [SplitSwitchButton] is Stadium shaped. The label should take no more than 3 lines of text.
 * The secondary label should take no more than 2 lines of text. With localisation and/or large font
 * sizes, the [SplitSwitchButton] height adjusts to accommodate the contents. The label and
 * secondary label are start aligned by default.
 *
 * A [SplitSwitchButton] has two tappable areas, one tap area for the labels and another for the
 * switch. The [onContainerClick] listener will be associated with the main body of the
 * [SplitSwitchButton] with the [onCheckedChange] listener associated with the switch area only.
 *
 * Example of a SplitSwitchButton:
 *
 * @sample androidx.wear.compose.material3.samples.SplitSwitchButtonSample
 *
 * For a SplitSwitchButton the background of the tappable background area behind the switch will
 * have a visual effect applied to provide a "divider" between the two tappable areas.
 *
 * The recommended set of colors can be obtained from [SwitchButtonDefaults], e.g.
 * [SwitchButtonDefaults.splitSwitchButtonColors].
 *
 * [SplitSwitchButton] can be enabled or disabled. A disabled button will not respond to click
 * events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is changed.
 * @param toggleContentDescription The content description for the switch control part of the
 *   component.
 * @param onContainerClick Click listener called when the user clicks the main body of the button,
 *   the area behind the labels.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not be
 *   clickable.
 * @param shape Defines the button's shape. It is strongly recommended to use the default as this
 *   shape is a key characteristic of the Wear Material Theme.
 * @param colors [SplitSwitchButtonColors] that will be used to resolve the background and content
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
fun SplitSwitchButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    toggleContentDescription: String?,
    onContainerClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = SwitchButtonDefaults.splitSwitchButtonShape,
    colors: SplitSwitchButtonColors = SwitchButtonDefaults.splitSwitchButtonColors(),
    toggleInteractionSource: MutableInteractionSource? = null,
    containerInteractionSource: MutableInteractionSource? = null,
    containerClickLabel: String? = null,
    contentPadding: PaddingValues = SwitchButtonDefaults.ContentPadding,
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
                        textStyle = SplitSwitchButtonTokens.LabelFont.value,
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
                            colors.secondaryContentColor(enabled = enabled, checked = checked),
                        textStyle = SplitSwitchButtonTokens.SecondaryLabelFont.value,
                        textConfiguration =
                            TextConfiguration(
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                textAlign = TextAlign.Start,
                            ),
                        content = secondaryLabel
                    ),
                spacerSize = SwitchButtonDefaults.LabelSpacerSize
            )
        }

        Spacer(modifier = Modifier.size(2.dp))

        val splitBackground = if (enabled) containerColor else Color.Black
        val splitBackgroundOverlay = colors.splitContainerColor(enabled, checked).value
        val hapticFeedback = LocalHapticFeedback.current
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier.toggleable(
                        enabled = enabled,
                        value = checked,
                        onValueChange = {
                            hapticFeedback.performHapticFeedback(
                                if (it) HapticFeedbackType.ToggleOn
                                else HapticFeedbackType.ToggleOff
                            )
                            onCheckedChange(it)
                        },
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
            Switch(
                checked = checked,
                enabled = enabled,
                modifier =
                    if (toggleContentDescription == null) {
                        Modifier
                    } else {
                        Modifier.semantics { contentDescription = toggleContentDescription }
                    },
                thumbColor = { enabled, checked ->
                    colors.thumbColor(enabled = enabled, checked = checked)
                },
                thumbIconColor = { enabled, checked ->
                    colors.thumbIconColor(enabled = enabled, checked = checked)
                },
                trackColor = { enabled, checked ->
                    colors.trackColor(enabled = enabled, checked = checked)
                },
                trackBorderColor = { enabled, checked ->
                    colors.trackBorderColor(enabled = enabled, checked = checked)
                }
            )
        }
    }
}

/** Contains the default values used by [SwitchButton]s and [SplitSwitchButton]s */
object SwitchButtonDefaults {
    /** Recommended [Shape] for [SwitchButton]. */
    val switchButtonShape: Shape
        @Composable get() = SwitchButtonTokens.ContainerShape.value

    /** Recommended [Shape] for [SplitSwitchButton]. */
    val splitSwitchButtonShape: Shape
        @Composable get() = SplitSwitchButtonTokens.ContainerShape.value

    /** Creates a [SwitchButtonColors] for use in a [SwitchButton]. */
    @Composable fun switchButtonColors() = MaterialTheme.colorScheme.defaultSwitchButtonColors

    /**
     * Creates a [SwitchButtonColors] for use in a [SwitchButton].
     *
     * @param checkedContainerColor The container color of the [SwitchButton] when enabled and
     *   checked.
     * @param checkedContentColor The content color of the [SwitchButton] when enabled and checked.
     * @param checkedSecondaryContentColor The secondary content color of the [SwitchButton] when
     *   enabled and checked, used for secondaryLabel content.
     * @param checkedIconColor The icon color of the [SwitchButton] when enabled and checked.
     * @param checkedThumbColor The thumb color of the [SwitchButton] when enabled and checked.
     * @param checkedThumbIconColor The thumb icon color of the [SwitchButton] when enabled and
     *   checked.
     * @param checkedTrackColor The track color of the [SwitchButton] when enabled and checked.
     * @param checkedTrackBorderColor The track border color of the [SwitchButton] when enabled and
     *   checked
     * @param uncheckedContainerColor The container color of the [SwitchButton] when enabled and
     *   unchecked.
     * @param uncheckedContentColor The content color of a [SwitchButton] when enabled and
     *   unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of this [SwitchButton] when
     *   enabled and unchecked, used for secondaryLabel content
     * @param uncheckedIconColor The icon color of the [SwitchButton] when enabled and unchecked.
     * @param uncheckedThumbColor The thumb color of the [SwitchButton] when enabled and unchecked.
     * @param uncheckedTrackColor The track color of the [SwitchButton] when enabled and unchecked.
     * @param uncheckedTrackBorderColor The track border color of the [SwitchButton] when enabled
     *   and unchecked
     * @param disabledCheckedContainerColor The container color of the [SwitchButton] when disabled
     *   and checked.
     * @param disabledCheckedContentColor The content color of the [SwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the [SwitchButton]
     *   when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedIconColor The icon color of the [SwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedThumbColor The thumb color of the [SwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedThumbIconColor The thumb icon color of the [SwitchButton] when disabled
     *   and checked.
     * @param disabledCheckedTrackColor The track color of the [SwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedTrackBorderColor The track border color of the [SwitchButton] when
     *   disabled and checked
     * @param disabledUncheckedContainerColor The container color of the [SwitchButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of a [SwitchButton] when disabled and
     *   unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of this
     *   [SwitchButton] when disabled and unchecked, used for secondaryLabel content
     * @param disabledUncheckedIconColor The icon color of the [SwitchButton] when disabled and
     *   unchecked.
     * @param disabledUncheckedThumbColor The thumb color of the [SwitchButton] when disabled and
     *   unchecked.
     * @param disabledUncheckedTrackBorderColor The track border color of the [SwitchButton] when
     *   disabled and unchecked
     */
    @Composable
    fun switchButtonColors(
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
        checkedSecondaryContentColor: Color = Color.Unspecified,
        checkedIconColor: Color = Color.Unspecified,
        checkedThumbColor: Color = Color.Unspecified,
        checkedThumbIconColor: Color = Color.Unspecified,
        checkedTrackColor: Color = Color.Unspecified,
        checkedTrackBorderColor: Color = Color.Unspecified,
        uncheckedContainerColor: Color = Color.Unspecified,
        uncheckedContentColor: Color = Color.Unspecified,
        uncheckedSecondaryContentColor: Color = Color.Unspecified,
        uncheckedIconColor: Color = Color.Unspecified,
        uncheckedThumbColor: Color = Color.Unspecified,
        uncheckedTrackColor: Color = Color.Unspecified,
        uncheckedTrackBorderColor: Color = Color.Unspecified,
        disabledCheckedContainerColor: Color = Color.Unspecified,
        disabledCheckedContentColor: Color = Color.Unspecified,
        disabledCheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledCheckedIconColor: Color = Color.Unspecified,
        disabledCheckedThumbColor: Color = Color.Unspecified,
        disabledCheckedThumbIconColor: Color = Color.Unspecified,
        disabledCheckedTrackColor: Color = Color.Unspecified,
        disabledCheckedTrackBorderColor: Color = Color.Unspecified,
        disabledUncheckedContainerColor: Color = Color.Unspecified,
        disabledUncheckedContentColor: Color = Color.Unspecified,
        disabledUncheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledUncheckedIconColor: Color = Color.Unspecified,
        disabledUncheckedThumbColor: Color = Color.Unspecified,
        disabledUncheckedTrackBorderColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultSwitchButtonColors.copy(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedIconColor = checkedIconColor,
            checkedThumbColor = checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedIconColor = uncheckedIconColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedTrackBorderColor = uncheckedTrackBorderColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedIconColor = disabledCheckedIconColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledCheckedThumbIconColor = disabledCheckedThumbIconColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledCheckedTrackBorderColor = disabledCheckedTrackBorderColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedIconColor = disabledUncheckedIconColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor = disabledUncheckedTrackBorderColor,
        )

    /** Creates a [SplitSwitchButtonColors] for use in a [SplitSwitchButton]. */
    @Composable
    fun splitSwitchButtonColors() = MaterialTheme.colorScheme.defaultSplitSwitchButtonColors

    /**
     * Creates a [SplitSwitchButtonColors] for use in a [SplitSwitchButton].
     *
     * @param checkedContainerColor The container color of the [SplitSwitchButton] when enabled and
     *   checked.
     * @param checkedContentColor The content color of the [SplitSwitchButton] when enabled and
     *   checked.
     * @param checkedSecondaryContentColor The secondary content color of the [SplitSwitchButton]
     *   when enabled and checked, used for secondaryLabel content.
     * @param checkedSplitContainerColor The split container color of the [SplitSwitchButton] when
     *   enabled and checked.
     * @param checkedThumbColor The thumb color of the [SplitSwitchButton] when enabled and checked.
     * @param checkedThumbIconColor The thumb icon color of the [SplitSwitchButton] when enabled and
     *   checked.
     * @param checkedTrackColor The track color of the [SplitSwitchButton] when enabled and checked.
     * @param checkedTrackBorderColor The track border color of the [SplitSwitchButton] when enabled
     *   and checked
     * @param uncheckedContainerColor The container color of the [SplitSwitchButton] when enabled
     *   and unchecked.
     * @param uncheckedContentColor The content color of the [SplitSwitchButton] when enabled and
     *   unchecked.
     * @param uncheckedSecondaryContentColor The secondary content color of the [SplitSwitchButton]
     *   when enabled and unchecked, used for secondaryLabel content.
     * @param uncheckedSplitContainerColor The split container color of the [SplitSwitchButton] when
     *   enabled and unchecked.
     * @param uncheckedThumbColor The thumb color of the [SplitSwitchButton] when enabled and
     *   unchecked.
     * @param uncheckedTrackColor The track color of the [SplitSwitchButton] when enabled and
     *   unchecked.
     * @param uncheckedTrackBorderColor The track border color of the [SplitSwitchButton] when
     *   enabled and unchecked
     * @param disabledCheckedContainerColor The container color of the [SplitSwitchButton] when
     *   disabled and checked.
     * @param disabledCheckedContentColor The content color of the [SplitSwitchButton] when disabled
     *   and checked.
     * @param disabledCheckedSecondaryContentColor The secondary content color of the
     *   [SplitSwitchButton] when disabled and checked, used for secondaryLabel content.
     * @param disabledCheckedSplitContainerColor The split container color of the
     *   [ SplitSwitchButton] when disabled and checked.
     * @param disabledCheckedThumbColor The thumb color of the [SplitSwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedThumbIconColor The thumb icon color of the [SplitSwitchButton] when
     *   disabled and checked.
     * @param disabledCheckedTrackColor The track color of the [SplitSwitchButton] when disabled and
     *   checked.
     * @param disabledCheckedTrackBorderColor The track border color of the [SplitSwitchButton] when
     *   disabled and checked
     * @param disabledUncheckedContainerColor The container color of the [SplitSwitchButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedContentColor The content color of the [SplitSwitchButton] when
     *   disabled and unchecked.
     * @param disabledUncheckedSecondaryContentColor The secondary content color of the
     *   [SplitSwitchButton] when disabled and unchecked, used for secondaryLabel content.
     * @param disabledUncheckedSplitContainerColor The split container color of the
     *   [SplitSwitchButton] when disabled and unchecked.
     * @param disabledUncheckedThumbColor The thumb color of the [SplitSwitchButton] when disabled
     *   and unchecked.
     * @param disabledUncheckedTrackBorderColor The track border color of the [SplitSwitchButton]
     *   when disabled and unchecked
     */
    @Composable
    fun splitSwitchButtonColors(
        checkedContainerColor: Color = Color.Unspecified,
        checkedContentColor: Color = Color.Unspecified,
        checkedSecondaryContentColor: Color = Color.Unspecified,
        checkedSplitContainerColor: Color = Color.Unspecified,
        checkedThumbColor: Color = Color.Unspecified,
        checkedThumbIconColor: Color = Color.Unspecified,
        checkedTrackColor: Color = Color.Unspecified,
        checkedTrackBorderColor: Color = Color.Unspecified,
        uncheckedContainerColor: Color = Color.Unspecified,
        uncheckedContentColor: Color = Color.Unspecified,
        uncheckedSecondaryContentColor: Color = Color.Unspecified,
        uncheckedSplitContainerColor: Color = Color.Unspecified,
        uncheckedThumbColor: Color = Color.Unspecified,
        uncheckedTrackColor: Color = Color.Unspecified,
        uncheckedTrackBorderColor: Color = Color.Unspecified,
        disabledCheckedContainerColor: Color = Color.Unspecified,
        disabledCheckedContentColor: Color = Color.Unspecified,
        disabledCheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledCheckedSplitContainerColor: Color = Color.Unspecified,
        disabledCheckedThumbColor: Color = Color.Unspecified,
        disabledCheckedThumbIconColor: Color = Color.Unspecified,
        disabledCheckedTrackColor: Color = Color.Unspecified,
        disabledCheckedTrackBorderColor: Color = Color.Unspecified,
        disabledUncheckedContainerColor: Color = Color.Unspecified,
        disabledUncheckedContentColor: Color = Color.Unspecified,
        disabledUncheckedSecondaryContentColor: Color = Color.Unspecified,
        disabledUncheckedSplitContainerColor: Color = Color.Unspecified,
        disabledUncheckedThumbColor: Color = Color.Unspecified,
        disabledUncheckedTrackBorderColor: Color = Color.Unspecified,
    ) =
        MaterialTheme.colorScheme.defaultSplitSwitchButtonColors.copy(
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
            checkedSecondaryContentColor = checkedSecondaryContentColor,
            checkedSplitContainerColor = checkedSplitContainerColor,
            checkedThumbColor = checkedThumbColor,
            checkedThumbIconColor = checkedThumbIconColor,
            checkedTrackColor = checkedTrackColor,
            checkedTrackBorderColor = checkedTrackBorderColor,
            uncheckedContainerColor = uncheckedContainerColor,
            uncheckedContentColor = uncheckedContentColor,
            uncheckedSecondaryContentColor = uncheckedSecondaryContentColor,
            uncheckedSplitContainerColor = uncheckedSplitContainerColor,
            uncheckedThumbColor = uncheckedThumbColor,
            uncheckedTrackColor = uncheckedTrackColor,
            uncheckedTrackBorderColor = uncheckedTrackBorderColor,
            disabledCheckedContainerColor = disabledCheckedContainerColor,
            disabledCheckedContentColor = disabledCheckedContentColor,
            disabledCheckedSecondaryContentColor = disabledCheckedSecondaryContentColor,
            disabledCheckedSplitContainerColor = disabledCheckedSplitContainerColor,
            disabledCheckedThumbColor = disabledCheckedThumbColor,
            disabledCheckedThumbIconColor = disabledCheckedThumbIconColor,
            disabledCheckedTrackColor = disabledCheckedTrackColor,
            disabledCheckedTrackBorderColor = disabledCheckedTrackBorderColor,
            disabledUncheckedContainerColor = disabledUncheckedContainerColor,
            disabledUncheckedContentColor = disabledUncheckedContentColor,
            disabledUncheckedSecondaryContentColor = disabledUncheckedSecondaryContentColor,
            disabledUncheckedSplitContainerColor = disabledUncheckedSplitContainerColor,
            disabledUncheckedThumbColor = disabledUncheckedThumbColor,
            disabledUncheckedTrackBorderColor = disabledUncheckedTrackBorderColor,
        )

    internal val LabelSpacerSize = 2.dp
    private val HorizontalPadding = 14.dp
    private val VerticalPadding = 8.dp

    /** The default content padding used by [SwitchButton] */
    val ContentPadding: PaddingValues =
        PaddingValues(
            start = HorizontalPadding,
            top = VerticalPadding,
            end = HorizontalPadding,
            bottom = VerticalPadding
        )

    private val ColorScheme.defaultSwitchButtonColors: SwitchButtonColors
        get() {
            return defaultSwitchButtonColorsCached
                ?: SwitchButtonColors(
                        checkedContainerColor = fromToken(SwitchButtonTokens.CheckedContainerColor),
                        checkedContentColor = fromToken(SwitchButtonTokens.CheckedContentColor),
                        checkedSecondaryContentColor =
                            fromToken(SwitchButtonTokens.CheckedSecondaryLabelColor)
                                .copy(alpha = SwitchButtonTokens.CheckedSecondaryLabelOpacity),
                        checkedIconColor = fromToken(SwitchButtonTokens.CheckedIconColor),
                        checkedThumbColor = fromToken(SwitchButtonTokens.CheckedThumbColor),
                        checkedThumbIconColor = fromToken(SwitchButtonTokens.CheckedThumbIconColor),
                        checkedTrackColor = fromToken(SwitchButtonTokens.CheckedTrackColor),
                        checkedTrackBorderColor =
                            fromToken(SwitchButtonTokens.CheckedTrackBorderColor),
                        uncheckedContainerColor =
                            fromToken(SwitchButtonTokens.UncheckedContainerColor),
                        uncheckedContentColor = fromToken(SwitchButtonTokens.UncheckedContentColor),
                        uncheckedSecondaryContentColor =
                            fromToken(SwitchButtonTokens.UncheckedSecondaryLabelColor),
                        uncheckedThumbColor = fromToken(SwitchButtonTokens.UncheckedThumbColor),
                        uncheckedTrackColor = fromToken(SwitchButtonTokens.UncheckedTrackColor),
                        uncheckedTrackBorderColor =
                            fromToken(SwitchButtonTokens.UncheckedTrackBorderColor),
                        uncheckedIconColor = fromToken(SwitchButtonTokens.UncheckedIconColor),
                        disabledCheckedContainerColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SwitchButtonTokens.DisabledCheckedContainerOpacity
                                ),
                        disabledCheckedContentColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSecondaryContentColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedIconColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedIconColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedThumbColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedThumbColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledCheckedThumbOpacity
                                ),
                        disabledCheckedThumbIconColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedThumbIconColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SwitchButtonTokens.DisabledCheckedThumbIconOpacity
                                ),
                        disabledCheckedTrackColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedTrackColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledCheckedTrackOpacity
                                ),
                        disabledCheckedTrackBorderColor =
                            fromToken(SwitchButtonTokens.DisabledCheckedTrackBorderColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SwitchButtonTokens.DisabledCheckedTrackBorderOpacity
                                ),
                        disabledUncheckedContainerColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedContainerColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SwitchButtonTokens.DisabledUncheckedContainerOpacity
                                ),
                        disabledUncheckedContentColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSecondaryContentColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedIconColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedIconColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedThumbColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedThumbColor)
                                .toDisabledColor(
                                    disabledAlpha = SwitchButtonTokens.DisabledUncheckedThumbOpacity
                                ),
                        disabledUncheckedTrackBorderColor =
                            fromToken(SwitchButtonTokens.DisabledUncheckedTrackBorderColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SwitchButtonTokens.DisabledUncheckedTrackBorderOpacity
                                )
                    )
                    .also { defaultSwitchButtonColorsCached = it }
        }

    private val ColorScheme.defaultSplitSwitchButtonColors: SplitSwitchButtonColors
        get() {
            return defaultSplitSwitchButtonColorsCached
                ?: SplitSwitchButtonColors(
                        checkedContainerColor =
                            fromToken(SplitSwitchButtonTokens.CheckedContainerColor),
                        checkedContentColor =
                            fromToken(SplitSwitchButtonTokens.CheckedContentColor),
                        checkedSecondaryContentColor =
                            fromToken(SplitSwitchButtonTokens.CheckedSecondaryLabelColor)
                                .copy(alpha = SplitSwitchButtonTokens.CheckedSecondaryLabelOpacity),
                        checkedSplitContainerColor =
                            fromToken(SplitSwitchButtonTokens.CheckedSplitContainerColor)
                                .copy(alpha = SplitSwitchButtonTokens.CheckedSplitContainerOpacity),
                        checkedThumbColor = fromToken(SplitSwitchButtonTokens.CheckedThumbColor),
                        checkedThumbIconColor =
                            fromToken(SplitSwitchButtonTokens.CheckedThumbIconColor),
                        checkedTrackColor = fromToken(SplitSwitchButtonTokens.CheckedTrackColor),
                        checkedTrackBorderColor =
                            fromToken(SplitSwitchButtonTokens.CheckedTrackBorderColor),
                        uncheckedContainerColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedContainerColor),
                        uncheckedContentColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedContentColor),
                        uncheckedSecondaryContentColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedSecondaryLabelColor),
                        uncheckedSplitContainerColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedSplitContainerColor),
                        uncheckedThumbColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedThumbColor),
                        uncheckedTrackColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedTrackColor),
                        uncheckedTrackBorderColor =
                            fromToken(SplitSwitchButtonTokens.UncheckedTrackBorderColor),
                        disabledCheckedContainerColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedContainerColor)
                                .copy(
                                    alpha = SplitSwitchButtonTokens.DisabledCheckedContainerOpacity
                                ),
                        disabledCheckedContentColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitSwitchButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSecondaryContentColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitSwitchButtonTokens.DisabledOpacity
                                ),
                        disabledCheckedSplitContainerColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedSplitContainerColor)
                                .copy(
                                    alpha =
                                        SplitSwitchButtonTokens.DisabledCheckedSplitContainerOpacity
                                ),
                        disabledCheckedThumbColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedThumbColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledCheckedThumbOpacity
                                ),
                        disabledCheckedThumbIconColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedThumbIconColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledCheckedThumbIconOpacity
                                ),
                        disabledCheckedTrackColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedTrackColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledCheckedTrackOpacity
                                ),
                        disabledCheckedTrackBorderColor =
                            fromToken(SplitSwitchButtonTokens.DisabledCheckedTrackBorderColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledCheckedTrackBorderOpacity
                                ),
                        disabledUncheckedContainerColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedContainerColor)
                                .copy(
                                    alpha =
                                        SplitSwitchButtonTokens.DisabledUncheckedContainerOpacity
                                ),
                        disabledUncheckedContentColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedContentColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitSwitchButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSecondaryContentColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedSecondaryLabelColor)
                                .toDisabledColor(
                                    disabledAlpha = SplitSwitchButtonTokens.DisabledOpacity
                                ),
                        disabledUncheckedSplitContainerColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedSplitContainerColor)
                                .copy(
                                    alpha =
                                        SplitSwitchButtonTokens
                                            .DisabledUncheckedSplitContainerOpacity
                                ),
                        disabledUncheckedThumbColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedThumbColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledUncheckedThumbOpacity
                                ),
                        disabledUncheckedTrackBorderColor =
                            fromToken(SplitSwitchButtonTokens.DisabledUncheckedTrackBorderColor)
                                .toDisabledColor(
                                    disabledAlpha =
                                        SplitSwitchButtonTokens.DisabledUncheckedTrackBorderOpacity
                                ),
                    )
                    .also { defaultSplitSwitchButtonColorsCached = it }
        }
}

/**
 * Represents the different container and content colors used for [SwitchButton], in various states,
 * that are checked, unchecked, enabled and disabled.
 *
 * @param checkedContainerColor Container or background color when the [SwitchButton] is checked
 * @param checkedContentColor Color of the content like label when the [SwitchButton] is checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 *   [SwitchButton] is checked
 * @param checkedIconColor Color of the icon when the [SwitchButton] is checked
 * @param checkedThumbColor Color of the thumb when the [SwitchButton] is checked
 * @param checkedThumbIconColor Color of the thumb icon when the [SwitchButton] is checked
 * @param checkedTrackColor Color of the track when the [SwitchButton] is checked
 * @param checkedTrackBorderColor Color of the track boarder when the [SwitchButton] is checked
 * @param uncheckedContainerColor Container or background color when the [SwitchButton] is unchecked
 * @param uncheckedContentColor Color of the content like label when the [SwitchButton] is unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 *   the [SwitchButton] is unchecked
 * @param uncheckedIconColor Color of the icon when the [SwitchButton] is unchecked
 * @param uncheckedThumbColor Color of the thumb when the [SwitchButton] is unchecked
 * @param uncheckedTrackColor Color of the track when the [SwitchButton] is unchecked
 * @param uncheckedTrackBorderColor Color of the track border when the [SwitchButton] is unchecked
 * @param disabledCheckedContainerColor Container or background color when the [SwitchButton] is
 *   disabled and checked
 * @param disabledCheckedContentColor Color of content like label when the [SwitchButton] is
 *   disabled and checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the [SwitchButton] is disabled and checked
 * @param disabledCheckedIconColor Icon color when the [SwitchButton] is disabled and checked
 * @param disabledCheckedThumbColor Thumb color when the [SwitchButton] is disabled and checked
 * @param disabledCheckedThumbIconColor Thumb icon color when the [SwitchButton] is disabled and
 *   checked
 * @param disabledCheckedTrackColor Track color when the [SwitchButton] is disabled and checked
 * @param disabledCheckedTrackBorderColor Track border color when the [SwitchButton] is disabled and
 *   checked
 * @param disabledUncheckedContainerColor Container or background color when the [SwitchButton] is
 *   disabled and unchecked
 * @param disabledUncheckedContentColor Color of the content like label when the [SwitchButton] is
 *   disabled and unchecked
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the [SwitchButton] is disabled and unchecked
 * @param disabledUncheckedIconColor Icon color when the [SwitchButton] is disabled and unchecked
 * @param disabledUncheckedThumbColor Thumb color when the [SwitchButton] is disabled and unchecked
 * @param disabledUncheckedTrackBorderColor Track border color when the [SwitchButton] is disabled
 *   and unchecked
 */
@Immutable
class SwitchButtonColors(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedIconColor: Color,
    val checkedThumbColor: Color,
    val checkedThumbIconColor: Color,
    val checkedTrackBorderColor: Color,
    val checkedTrackColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedIconColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedTrackBorderColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedIconColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledCheckedThumbIconColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledCheckedTrackBorderColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedIconColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledUncheckedTrackBorderColor: Color
) {
    /**
     * Returns a copy of this SwitchButtonColors optionally overriding some of the values.
     *
     * @param checkedContainerColor Container or background color when the [SwitchButton] is checked
     * @param checkedContentColor Color of the content like label when the [SwitchButton] is checked
     * @param checkedSecondaryContentColor Color of the secondary content like secondary label when
     *   the [SwitchButton] is checked
     * @param checkedIconColor Color of the icon when the [SwitchButton] is checked
     * @param checkedThumbColor Color of the thumb when the [SwitchButton] is checked
     * @param checkedThumbIconColor Color of the thumb icon when the [SwitchButton] is checked
     * @param checkedTrackColor Color of the track when the [SwitchButton] is checked
     * @param checkedTrackBorderColor Color of the track boarder when the [SwitchButton] is checked
     * @param uncheckedContainerColor Container or background color when the [SwitchButton] is
     *   unchecked
     * @param uncheckedContentColor Color of the content like label when the [SwitchButton] is
     *   unchecked
     * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label
     *   when the [SwitchButton] is unchecked
     * @param uncheckedIconColor Color of the icon when the [SwitchButton] is unchecked
     * @param uncheckedThumbColor Color of the thumb when the [SwitchButton] is unchecked
     * @param uncheckedTrackColor Color of the track when the [SwitchButton] is unchecked
     * @param uncheckedTrackBorderColor Color of the track border when the [SwitchButton] is
     *   unchecked
     * @param disabledCheckedContainerColor Container or background color when the [SwitchButton] is
     *   disabled and checked
     * @param disabledCheckedContentColor Color of content like label when the [SwitchButton] is
     *   disabled and checked
     * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the [SwitchButton] is disabled and checked
     * @param disabledCheckedIconColor Icon color when the [SwitchButton] is disabled and checked
     * @param disabledCheckedThumbColor Thumb color when the [SwitchButton] is disabled and checked
     * @param disabledCheckedThumbIconColor Thumb icon color when the [SwitchButton] is disabled and
     *   checked
     * @param disabledCheckedTrackColor Track color when the [SwitchButton] is disabled and checked
     * @param disabledCheckedTrackBorderColor Track border color when the [SwitchButton] is disabled
     *   and checked
     * @param disabledUncheckedContainerColor Container or background color when the [SwitchButton]
     *   is disabled and unchecked
     * @param disabledUncheckedContentColor Color of the content like label when the [SwitchButton]
     *   is disabled and unchecked
     * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the [SwitchButton] is disabled and unchecked
     * @param disabledUncheckedIconColor Icon color when the [SwitchButton] is disabled and
     *   unchecked
     * @param disabledUncheckedThumbColor Thumb color when the [SwitchButton] is disabled and
     *   unchecked
     * @param disabledUncheckedTrackBorderColor Track border color when the [SwitchButton] is
     *   disabled and unchecked
     */
    fun copy(
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
        checkedSecondaryContentColor: Color = this.checkedSecondaryContentColor,
        checkedIconColor: Color = this.checkedIconColor,
        checkedThumbColor: Color = this.checkedThumbColor,
        checkedThumbIconColor: Color = this.checkedThumbIconColor,
        checkedTrackColor: Color = this.checkedTrackColor,
        checkedTrackBorderColor: Color = this.checkedTrackBorderColor,
        uncheckedContainerColor: Color = this.uncheckedContainerColor,
        uncheckedContentColor: Color = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: Color = this.uncheckedSecondaryContentColor,
        uncheckedIconColor: Color = this.uncheckedIconColor,
        uncheckedThumbColor: Color = this.uncheckedThumbColor,
        uncheckedTrackColor: Color = this.uncheckedTrackColor,
        uncheckedTrackBorderColor: Color = this.uncheckedTrackBorderColor,
        disabledCheckedContainerColor: Color = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: Color = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: Color = this.disabledCheckedSecondaryContentColor,
        disabledCheckedIconColor: Color = this.disabledCheckedIconColor,
        disabledCheckedThumbColor: Color = this.disabledCheckedThumbColor,
        disabledCheckedThumbIconColor: Color = this.disabledCheckedThumbIconColor,
        disabledCheckedTrackColor: Color = this.disabledCheckedTrackColor,
        disabledCheckedTrackBorderColor: Color = this.disabledCheckedTrackBorderColor,
        disabledUncheckedContainerColor: Color = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: Color = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: Color = this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedIconColor: Color = this.disabledUncheckedIconColor,
        disabledUncheckedThumbColor: Color = this.disabledUncheckedThumbColor,
        disabledUncheckedTrackBorderColor: Color = this.disabledUncheckedTrackBorderColor,
    ): SwitchButtonColors =
        SwitchButtonColors(
            checkedContainerColor = checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor = checkedContentColor.takeOrElse { this.checkedContentColor },
            checkedSecondaryContentColor =
                checkedSecondaryContentColor.takeOrElse { this.checkedSecondaryContentColor },
            checkedIconColor = checkedIconColor.takeOrElse { this.checkedIconColor },
            checkedThumbColor = checkedThumbColor.takeOrElse { this.checkedThumbColor },
            checkedThumbIconColor = checkedThumbIconColor.takeOrElse { this.checkedThumbIconColor },
            checkedTrackColor = checkedTrackColor.takeOrElse { this.checkedTrackColor },
            checkedTrackBorderColor =
                checkedTrackBorderColor.takeOrElse { this.checkedTrackBorderColor },
            uncheckedContainerColor =
                uncheckedContainerColor.takeOrElse { this.uncheckedContainerColor },
            uncheckedContentColor = uncheckedContentColor.takeOrElse { this.uncheckedContentColor },
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor.takeOrElse { this.uncheckedSecondaryContentColor },
            uncheckedIconColor = uncheckedIconColor.takeOrElse { this.uncheckedIconColor },
            uncheckedThumbColor = uncheckedThumbColor.takeOrElse { this.uncheckedThumbColor },
            uncheckedTrackColor = uncheckedTrackColor.takeOrElse { this.uncheckedTrackColor },
            uncheckedTrackBorderColor =
                uncheckedTrackBorderColor.takeOrElse { this.uncheckedTrackBorderColor },
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
            disabledCheckedThumbColor =
                disabledCheckedThumbColor.takeOrElse { this.disabledCheckedThumbColor },
            disabledCheckedThumbIconColor =
                disabledCheckedThumbIconColor.takeOrElse { this.disabledCheckedThumbIconColor },
            disabledCheckedTrackColor =
                disabledCheckedTrackColor.takeOrElse { this.disabledCheckedTrackColor },
            disabledCheckedTrackBorderColor =
                disabledCheckedTrackBorderColor.takeOrElse { this.disabledCheckedTrackBorderColor },
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
            disabledUncheckedThumbColor =
                disabledUncheckedThumbColor.takeOrElse { this.disabledUncheckedThumbColor },
            disabledUncheckedTrackBorderColor =
                disabledUncheckedTrackBorderColor.takeOrElse {
                    this.disabledUncheckedTrackBorderColor
                },
        )

    /**
     * Determines the container color based on whether the [SwitchButton] is [enabled] and
     * [checked].
     *
     * @param enabled Whether the [SwitchButton] is enabled
     * @param checked Whether the [SwitchButton] is checked
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
     * Determines the content color based on whether the [SwitchButton] is [enabled] and [checked].
     *
     * @param enabled Whether the [SwitchButton] is enabled
     * @param checked Whether the [SwitchButton] is checked
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
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
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
     * Represents the icon color for the [SwitchButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
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
     * Represents the thumb color for the [SwitchButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbColor,
            uncheckedColor = uncheckedThumbColor,
            disabledCheckedColor = disabledCheckedThumbColor,
            disabledUncheckedColor = disabledUncheckedThumbColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the thumb icon color for the [SwitchButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun thumbIconColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbIconColor,
            uncheckedColor = Color.Transparent,
            disabledCheckedColor = disabledCheckedThumbIconColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the track color for the [SwitchButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun trackColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackColor,
            uncheckedColor = uncheckedTrackColor,
            disabledCheckedColor = disabledCheckedTrackColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the track border color for the [SwitchButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun trackBorderColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackBorderColor,
            uncheckedColor = uncheckedTrackBorderColor,
            disabledCheckedColor = disabledCheckedTrackBorderColor,
            disabledUncheckedColor = disabledUncheckedTrackBorderColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SwitchButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedIconColor != other.checkedIconColor) return false
        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedThumbIconColor != other.checkedThumbIconColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedTrackBorderColor != other.checkedTrackBorderColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedIconColor != other.uncheckedIconColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedTrackBorderColor != other.uncheckedTrackBorderColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedIconColor != other.disabledCheckedIconColor) return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedThumbIconColor != other.disabledCheckedThumbIconColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedIconColor != other.disabledUncheckedIconColor) return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackBorderColor != other.disabledUncheckedTrackBorderColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedIconColor.hashCode()
        result = 31 * result + checkedThumbColor.hashCode()
        result = 31 * result + checkedThumbIconColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedTrackBorderColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedIconColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedIconColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedThumbIconColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedIconColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackBorderColor.hashCode()
        return result
    }
}

/**
 * Represents the different colors used in [SplitSwitchButton] in different states.
 *
 * @param checkedContainerColor Container or background color when the [SplitSwitchButton] is
 *   checked
 * @param checkedContentColor Color of the content like label when the [SplitSwitchButton] is
 *   checked
 * @param checkedSecondaryContentColor Color of the secondary content like secondary label when the
 *   [SplitSwitchButton] is checked
 * @param checkedSplitContainerColor Split container color when the split toggle button is checked
 * @param checkedThumbColor Color of the thumb when the [SplitSwitchButton] is checked
 * @param checkedThumbIconColor Color of the thumb icon when the [SplitSwitchButton] is checked
 * @param checkedTrackColor Color of the track when the [SplitSwitchButton] is checked
 * @param checkedTrackBorderColor Color of the track border when the [SplitSwitchButton] is checked
 * @param uncheckedContainerColor Container or background color when the [SplitSwitchButton] is
 *   unchecked
 * @param uncheckedContentColor Color of the content like label when the [SplitSwitchButton] is
 *   unchecked
 * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label when
 *   the split toggle button is unchecked
 * @param uncheckedSplitContainerColor Split container color when the split toggle button is
 *   unchecked
 * @param uncheckedThumbColor Color of the thumb when the [SplitSwitchButton] is unchecked
 * @param uncheckedTrackColor Color of the track when the [SplitSwitchButton] is unchecked
 * @param uncheckedTrackBorderColor Color of the track border when the [SplitSwitchButton] is
 *   unchecked
 * @param disabledCheckedContainerColor Container color when the split toggle button is disabled and
 *   checked
 * @param disabledCheckedContentColor Color of the content like label when the split toggle button
 *   is disabled and checked
 * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the split toggle button is disabled and checked
 * @param disabledCheckedSplitContainerColor Split container color when the split toggle button is
 *   disabled and checked
 * @param disabledCheckedThumbColor Color of the thumb when the [SplitSwitchButton] is disabled and
 *   checked
 * @param disabledCheckedThumbIconColor Color of the thumb icon when the [SplitSwitchButton] is
 *   disabled and checked
 * @param disabledCheckedTrackColor Color of the track when the [SplitSwitchButton] is disabled and
 *   checked
 * @param disabledCheckedTrackBorderColor Color of the track border when the [SplitSwitchButton] is
 *   disabled and checked
 * @param disabledUncheckedContainerColor Container color when the split toggle button is unchecked
 *   and disabled
 * @param disabledUncheckedContentColor Color of the content like label when the split toggle button
 *   is unchecked and disabled
 * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary label
 *   when the split toggle button is unchecked and disabled
 * @param disabledUncheckedSplitContainerColor Split container color when the split toggle button is
 *   unchecked and disabled
 * @param disabledUncheckedThumbColor Color of the thumb when the [SplitSwitchButton] is disabled
 *   and unchecked
 * @param disabledUncheckedTrackBorderColor Color of the track border when the [SplitSwitchButton]
 *   is disabled and unchecked
 */
class SplitSwitchButtonColors(
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
    val checkedSecondaryContentColor: Color,
    val checkedSplitContainerColor: Color,
    val checkedThumbColor: Color,
    val checkedThumbIconColor: Color,
    val checkedTrackColor: Color,
    val checkedTrackBorderColor: Color,
    val uncheckedContainerColor: Color,
    val uncheckedContentColor: Color,
    val uncheckedSecondaryContentColor: Color,
    val uncheckedSplitContainerColor: Color,
    val uncheckedThumbColor: Color,
    val uncheckedTrackColor: Color,
    val uncheckedTrackBorderColor: Color,
    val disabledCheckedContainerColor: Color,
    val disabledCheckedContentColor: Color,
    val disabledCheckedSecondaryContentColor: Color,
    val disabledCheckedSplitContainerColor: Color,
    val disabledCheckedThumbColor: Color,
    val disabledCheckedThumbIconColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledCheckedTrackBorderColor: Color,
    val disabledUncheckedContainerColor: Color,
    val disabledUncheckedContentColor: Color,
    val disabledUncheckedSecondaryContentColor: Color,
    val disabledUncheckedSplitContainerColor: Color,
    val disabledUncheckedThumbColor: Color,
    val disabledUncheckedTrackBorderColor: Color,
) {
    /**
     * Returns a copy of this SplitSwitchButtonColors optionally overriding some of the values.
     *
     * @param checkedContainerColor Container or background color when the [SplitSwitchButton] is
     *   checked
     * @param checkedContentColor Color of the content like label when the [SplitSwitchButton] is
     *   checked
     * @param checkedSecondaryContentColor Color of the secondary content like secondary label when
     *   the [SplitSwitchButton] is checked
     * @param checkedSplitContainerColor Split container color when the split toggle button is
     *   checked
     * @param checkedThumbColor Color of the thumb when the [SplitSwitchButton] is checked
     * @param checkedThumbIconColor Color of the thumb icon when the [SplitSwitchButton] is checked
     * @param checkedTrackColor Color of the track when the [SplitSwitchButton] is checked
     * @param checkedTrackBorderColor Color of the track border when the [SplitSwitchButton] is
     *   checked
     * @param uncheckedContainerColor Container or background color when the [SplitSwitchButton] is
     *   unchecked
     * @param uncheckedContentColor Color of the content like label when the [SplitSwitchButton] is
     *   unchecked
     * @param uncheckedSecondaryContentColor Color of the secondary content like secondary label
     *   when the split toggle button is unchecked
     * @param uncheckedSplitContainerColor Split container color when the split toggle button is
     *   unchecked
     * @param uncheckedThumbColor Color of the thumb when the [SplitSwitchButton] is unchecked
     * @param uncheckedTrackColor Color of the track when the [SplitSwitchButton] is unchecked
     * @param uncheckedTrackBorderColor Color of the track border when the [SplitSwitchButton] is
     *   unchecked
     * @param disabledCheckedContainerColor Container color when the split toggle button is disabled
     *   and checked
     * @param disabledCheckedContentColor Color of the content like label when the split toggle
     *   button is disabled and checked
     * @param disabledCheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the split toggle button is disabled and checked
     * @param disabledCheckedSplitContainerColor Split container color when the split toggle button
     *   is disabled and checked
     * @param disabledCheckedThumbColor Color of the thumb when the [SplitSwitchButton] is disabled
     *   and checked
     * @param disabledCheckedThumbIconColor Color of the thumb icon when the [SplitSwitchButton] is
     *   disabled and checked
     * @param disabledCheckedTrackColor Color of the track when the [SplitSwitchButton] is disabled
     *   and checked
     * @param disabledCheckedTrackBorderColor Color of the track border when the [SplitSwitchButton]
     *   is disabled and checked
     * @param disabledUncheckedContainerColor Container color when the split toggle button is
     *   unchecked and disabled
     * @param disabledUncheckedContentColor Color of the content like label when the split toggle
     *   button is unchecked and disabled
     * @param disabledUncheckedSecondaryContentColor Color of the secondary content like secondary
     *   label when the split toggle button is unchecked and disabled
     * @param disabledUncheckedSplitContainerColor Split container color when the split toggle
     *   button is unchecked and disabled
     * @param disabledUncheckedThumbColor Color of the thumb when the [SplitSwitchButton] is
     *   disabled and unchecked
     * @param disabledUncheckedTrackBorderColor Color of the track border when the
     *   [SplitSwitchButton] is disabled and unchecked
     */
    fun copy(
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor,
        checkedSecondaryContentColor: Color = this.checkedSecondaryContentColor,
        checkedSplitContainerColor: Color = this.checkedSplitContainerColor,
        checkedThumbColor: Color = this.checkedThumbColor,
        checkedThumbIconColor: Color = this.checkedThumbIconColor,
        checkedTrackColor: Color = this.checkedTrackColor,
        checkedTrackBorderColor: Color = this.checkedTrackBorderColor,
        uncheckedContainerColor: Color = this.uncheckedContainerColor,
        uncheckedContentColor: Color = this.uncheckedContentColor,
        uncheckedSecondaryContentColor: Color = this.uncheckedSecondaryContentColor,
        uncheckedSplitContainerColor: Color = this.uncheckedSplitContainerColor,
        uncheckedThumbColor: Color = this.uncheckedThumbColor,
        uncheckedTrackColor: Color = this.uncheckedTrackColor,
        uncheckedTrackBorderColor: Color = this.uncheckedTrackBorderColor,
        disabledCheckedContainerColor: Color = this.disabledCheckedContainerColor,
        disabledCheckedContentColor: Color = this.disabledCheckedContentColor,
        disabledCheckedSecondaryContentColor: Color = this.disabledCheckedSecondaryContentColor,
        disabledCheckedSplitContainerColor: Color = this.disabledCheckedSplitContainerColor,
        disabledCheckedThumbColor: Color = this.disabledCheckedThumbColor,
        disabledCheckedThumbIconColor: Color = this.disabledCheckedThumbIconColor,
        disabledCheckedTrackColor: Color = this.disabledCheckedTrackColor,
        disabledCheckedTrackBorderColor: Color = this.disabledCheckedTrackBorderColor,
        disabledUncheckedContainerColor: Color = this.disabledUncheckedContainerColor,
        disabledUncheckedContentColor: Color = this.disabledUncheckedContentColor,
        disabledUncheckedSecondaryContentColor: Color = this.disabledUncheckedSecondaryContentColor,
        disabledUncheckedSplitContainerColor: Color = this.disabledUncheckedSplitContainerColor,
        disabledUncheckedThumbColor: Color = this.disabledUncheckedThumbColor,
        disabledUncheckedTrackBorderColor: Color = this.disabledUncheckedTrackBorderColor,
    ): SplitSwitchButtonColors =
        SplitSwitchButtonColors(
            checkedContainerColor = checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor = checkedContentColor.takeOrElse { this.checkedContentColor },
            checkedSecondaryContentColor =
                checkedSecondaryContentColor.takeOrElse { this.checkedSecondaryContentColor },
            checkedSplitContainerColor =
                checkedSplitContainerColor.takeOrElse { this.checkedSplitContainerColor },
            checkedThumbColor = checkedThumbColor.takeOrElse { this.checkedThumbColor },
            checkedThumbIconColor = checkedThumbIconColor.takeOrElse { this.checkedThumbIconColor },
            checkedTrackColor = checkedTrackColor.takeOrElse { this.checkedTrackColor },
            checkedTrackBorderColor =
                checkedTrackBorderColor.takeOrElse { this.checkedTrackBorderColor },
            uncheckedContainerColor =
                uncheckedContainerColor.takeOrElse { this.uncheckedContainerColor },
            uncheckedContentColor = uncheckedContentColor.takeOrElse { this.uncheckedContentColor },
            uncheckedSecondaryContentColor =
                uncheckedSecondaryContentColor.takeOrElse { this.uncheckedSecondaryContentColor },
            uncheckedSplitContainerColor =
                uncheckedSplitContainerColor.takeOrElse { this.uncheckedSplitContainerColor },
            uncheckedThumbColor = uncheckedThumbColor.takeOrElse { this.uncheckedThumbColor },
            uncheckedTrackColor = uncheckedTrackColor.takeOrElse { this.uncheckedTrackColor },
            uncheckedTrackBorderColor =
                uncheckedTrackBorderColor.takeOrElse { this.uncheckedTrackBorderColor },
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
            disabledCheckedThumbColor =
                disabledCheckedThumbColor.takeOrElse { this.disabledCheckedThumbColor },
            disabledCheckedThumbIconColor =
                disabledCheckedThumbIconColor.takeOrElse { this.disabledCheckedThumbIconColor },
            disabledCheckedTrackColor =
                disabledCheckedTrackColor.takeOrElse { this.disabledCheckedTrackColor },
            disabledCheckedTrackBorderColor =
                disabledCheckedTrackBorderColor.takeOrElse { this.disabledCheckedTrackBorderColor },
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
            disabledUncheckedThumbColor =
                disabledUncheckedThumbColor.takeOrElse { this.disabledUncheckedThumbColor },
            disabledUncheckedTrackBorderColor =
                disabledUncheckedTrackBorderColor.takeOrElse {
                    this.disabledUncheckedTrackBorderColor
                },
        )

    /**
     * Determines the container color based on whether the [SplitSwitchButton] is [enabled] and
     * [checked].
     *
     * @param enabled Whether the [SplitSwitchButton] is enabled
     * @param checked Whether the [SplitSwitchButton] is currently checked
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
     * Determines the content color based on whether the [SplitSwitchButton] is [enabled] and
     * [checked].
     *
     * @param enabled Whether the [SplitSwitchButton] is enabled
     * @param checked Whether the [SplitSwitchButton] is currently checked
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
     * Represents the secondary content color for the [SplitSwitchButton] depending on the [enabled]
     * and [checked] properties.
     *
     * @param enabled Whether the [SplitSwitchButton] is enabled.
     * @param checked Whether the [SplitSwitchButton] is currently checked or unchecked.
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
     * Represents the split container for the [SplitSwitchButton] color depending on the [enabled]
     * and [checked] properties.
     *
     * @param enabled Whether the [SplitSwitchButton] is enabled.
     * @param checked Whether the [SplitSwitchButton] is currently checked or unchecked.
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
     * Represents the thumb color for the [SwitchButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbColor,
            uncheckedColor = uncheckedThumbColor,
            disabledCheckedColor = disabledCheckedThumbColor,
            disabledUncheckedColor = disabledUncheckedThumbColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the thumb icon color for the [SwitchButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun thumbIconColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedThumbIconColor,
            uncheckedColor = Color.Transparent,
            disabledCheckedColor = disabledCheckedThumbIconColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the track color for the [SwitchButton] depending on the [enabled] and [checked]
     * properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun trackColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackColor,
            uncheckedColor = uncheckedTrackColor,
            disabledCheckedColor = disabledCheckedTrackColor,
            disabledUncheckedColor = Color.Transparent,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    /**
     * Represents the track border color for the [SwitchButton] depending on the [enabled] and
     * [checked] properties.
     *
     * @param enabled Whether the [SwitchButton] is enabled.
     * @param checked Whether the [SwitchButton] is currently checked or unchecked.
     */
    @Composable
    internal fun trackBorderColor(enabled: Boolean, checked: Boolean): State<Color> =
        animateSelectionColor(
            enabled = enabled,
            checked = checked,
            checkedColor = checkedTrackBorderColor,
            uncheckedColor = uncheckedTrackBorderColor,
            disabledCheckedColor = disabledCheckedTrackBorderColor,
            disabledUncheckedColor = disabledUncheckedTrackBorderColor,
            animationSpec = COLOR_ANIMATION_SPEC
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        other as SplitSwitchButtonColors

        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false
        if (checkedSecondaryContentColor != other.checkedSecondaryContentColor) return false
        if (checkedSplitContainerColor != other.checkedSplitContainerColor) return false
        if (checkedThumbColor != other.checkedThumbColor) return false
        if (checkedThumbIconColor != other.checkedThumbIconColor) return false
        if (checkedTrackColor != other.checkedTrackColor) return false
        if (checkedTrackBorderColor != other.checkedTrackBorderColor) return false
        if (uncheckedContainerColor != other.uncheckedContainerColor) return false
        if (uncheckedContentColor != other.uncheckedContentColor) return false
        if (uncheckedSecondaryContentColor != other.uncheckedSecondaryContentColor) return false
        if (uncheckedSplitContainerColor != other.uncheckedSplitContainerColor) return false
        if (uncheckedThumbColor != other.uncheckedThumbColor) return false
        if (uncheckedTrackColor != other.uncheckedTrackColor) return false
        if (uncheckedTrackBorderColor != other.uncheckedTrackBorderColor) return false
        if (disabledCheckedContainerColor != other.disabledCheckedContainerColor) return false
        if (disabledCheckedContentColor != other.disabledCheckedContentColor) return false
        if (disabledCheckedSecondaryContentColor != other.disabledCheckedSecondaryContentColor)
            return false
        if (disabledCheckedSplitContainerColor != other.disabledCheckedSplitContainerColor)
            return false
        if (disabledCheckedThumbColor != other.disabledCheckedThumbColor) return false
        if (disabledCheckedThumbIconColor != other.disabledCheckedThumbIconColor) return false
        if (disabledCheckedTrackColor != other.disabledCheckedTrackColor) return false
        if (disabledCheckedTrackBorderColor != other.disabledCheckedTrackBorderColor) return false
        if (disabledUncheckedContainerColor != other.disabledUncheckedContainerColor) return false
        if (disabledUncheckedContentColor != other.disabledUncheckedContentColor) return false
        if (disabledUncheckedSecondaryContentColor != other.disabledUncheckedSecondaryContentColor)
            return false
        if (disabledUncheckedSplitContainerColor != other.disabledUncheckedSplitContainerColor)
            return false
        if (disabledUncheckedThumbColor != other.disabledUncheckedThumbColor) return false
        if (disabledUncheckedTrackBorderColor != other.disabledUncheckedTrackBorderColor)
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()
        result = 31 * result + checkedSecondaryContentColor.hashCode()
        result = 31 * result + checkedSplitContainerColor.hashCode()
        result = 31 * result + checkedThumbColor.hashCode()
        result = 31 * result + checkedThumbIconColor.hashCode()
        result = 31 * result + checkedTrackColor.hashCode()
        result = 31 * result + checkedTrackBorderColor.hashCode()
        result = 31 * result + uncheckedContainerColor.hashCode()
        result = 31 * result + uncheckedContentColor.hashCode()
        result = 31 * result + uncheckedSecondaryContentColor.hashCode()
        result = 31 * result + uncheckedSplitContainerColor.hashCode()
        result = 31 * result + uncheckedThumbColor.hashCode()
        result = 31 * result + uncheckedTrackColor.hashCode()
        result = 31 * result + uncheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledCheckedContainerColor.hashCode()
        result = 31 * result + disabledCheckedContentColor.hashCode()
        result = 31 * result + disabledCheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledCheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledCheckedThumbColor.hashCode()
        result = 31 * result + disabledCheckedThumbIconColor.hashCode()
        result = 31 * result + disabledCheckedTrackColor.hashCode()
        result = 31 * result + disabledCheckedTrackBorderColor.hashCode()
        result = 31 * result + disabledUncheckedContainerColor.hashCode()
        result = 31 * result + disabledUncheckedContentColor.hashCode()
        result = 31 * result + disabledUncheckedSecondaryContentColor.hashCode()
        result = 31 * result + disabledUncheckedSplitContainerColor.hashCode()
        result = 31 * result + disabledUncheckedThumbColor.hashCode()
        result = 31 * result + disabledUncheckedTrackBorderColor.hashCode()
        return result
    }
}

// [Switch] provides an animated switch for use as a toggle control in
// [SwitchButton] or [SplitSwitchButton].
@Composable
private fun Switch(
    checked: Boolean,
    enabled: Boolean,
    thumbColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    thumbIconColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    trackColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    trackBorderColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    modifier: Modifier = Modifier,
) {
    val isRtl = isLayoutDirectionRtl()
    val targetState = if (checked) SelectionStage.Checked else SelectionStage.Unchecked
    val transition = updateTransition(targetState, label = "switchTransition")
    val thumbProgress =
        transition.animateFloat(
            transitionSpec = { SWITCH_PROGRESS_ANIMATION_SPEC },
            label = "switchTransition"
        ) {
            when (it) {
                SelectionStage.Unchecked -> 0f
                SelectionStage.Checked -> 1f
            }
        }
    val actualThumbColor = thumbColor(enabled, checked).value
    val actualThumbIconColor = thumbIconColor(enabled, checked).value
    val actualTrackColor = trackColor(enabled, checked).value
    val actualTrackBorderColor = trackBorderColor(enabled, checked).value
    Box(
        modifier =
            modifier
                .semantics { this.role = Role.Switch }
                .height(SWITCH_INNER_HEIGHT)
                .width(SWITCH_WIDTH)
                .border(
                    width = SWITCH_TRACK_WIDTH,
                    shape = CircleShape,
                    color =
                        if (actualTrackColor == actualTrackBorderColor) {
                            Color.Transparent
                        } else {
                            actualTrackBorderColor
                        }
                )
                .background(color = actualTrackColor, shape = CircleShape)
                .drawBehind {
                    drawThumbAndTick(
                        enabled,
                        checked,
                        actualThumbColor,
                        thumbProgress.value,
                        actualThumbIconColor,
                        isRtl
                    )
                }
                .wrapContentSize(Alignment.CenterEnd)
    )
}

private fun DrawScope.drawThumbAndTick(
    enabled: Boolean,
    checked: Boolean,
    thumbColor: Color,
    progress: Float,
    thumbIconColor: Color,
    isRtl: Boolean
) {

    val thumbPaddingUnchecked = SWITCH_INNER_HEIGHT / 2 - THUMB_RADIUS_UNCHECKED
    val thumbPaddingChecked = SWITCH_INNER_HEIGHT / 2 - THUMB_RADIUS_CHECKED

    val switchThumbRadiusPx =
        lerp(
            start = THUMB_RADIUS_UNCHECKED.toPx(),
            stop = THUMB_RADIUS_CHECKED.toPx(),
            fraction = progress
        )

    val switchTrackLengthPx = SWITCH_WIDTH.toPx()

    // For Rtl mode the thumb progress will start from the end of the switch.
    val thumbProgressPx =
        if (isRtl)
            lerp(
                start = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingUnchecked.toPx(),
                stop = switchThumbRadiusPx + thumbPaddingChecked.toPx(),
                fraction = progress
            )
        else
            lerp(
                start = switchThumbRadiusPx + thumbPaddingUnchecked.toPx(),
                stop = switchTrackLengthPx - switchThumbRadiusPx - thumbPaddingChecked.toPx(),
                fraction = progress
            )

    drawCircle(
        color = thumbColor,
        radius = switchThumbRadiusPx,
        center = Offset(thumbProgressPx, center.y)
    )

    val ltrAdditionalOffset = 5.dp.toPx()
    val rtlAdditionalOffset = 6.dp.toPx()

    val totalDist = switchTrackLengthPx - 2 * switchThumbRadiusPx - ltrAdditionalOffset

    // Offset value to be added if RTL mode is enabled.
    // We need to move the tick to the checked position in ltr mode when unchecked.
    val rtlOffset = switchTrackLengthPx - 2 * THUMB_RADIUS_CHECKED.toPx() - rtlAdditionalOffset

    val distMoved = if (isRtl) rtlOffset - progress * totalDist else progress * totalDist

    // Draw tick icon
    animateTick(
        enabled = enabled,
        checked = checked,
        tickColor = thumbIconColor,
        tickProgress = progress,
        startXOffset = distMoved.toDp()
    )
}

@Composable
private fun RowScope.Labels(
    label: @Composable RowScope.() -> Unit,
    secondaryLabel: @Composable (RowScope.() -> Unit)?,
    spacerSize: Dp
) {
    Column(modifier = Modifier.weight(1.0f)) {
        Row(content = label)
        if (secondaryLabel != null) {
            Spacer(modifier = Modifier.size(spacerSize))
            Row(content = secondaryLabel)
        }
    }
}

private val SWITCH_WIDTH = 32.dp
private val SWITCH_OUTER_HEIGHT = 24.dp
private val SWITCH_INNER_HEIGHT = 22.dp
private val SWITCH_TRACK_WIDTH = 2.dp
private val THUMB_RADIUS_UNCHECKED = 6.dp
private val THUMB_RADIUS_CHECKED = 9.dp
private val TOGGLE_CONTROL_SPACING = 6.dp
private val ICON_SPACING = 6.dp
private val MIN_HEIGHT = 52.dp

private val SPLIT_MIN_WIDTH = 48.dp
private val SPLIT_SECTIONS_SHAPE = ShapeTokens.CornerExtraSmall

private val COLOR_ANIMATION_SPEC: AnimationSpec<Color>
    @Composable get() = MaterialTheme.motionScheme.slowEffectsSpec()
private val SWITCH_PROGRESS_ANIMATION_SPEC: FiniteAnimationSpec<Float>
    @Composable get() = MaterialTheme.motionScheme.fastEffectsSpec()
