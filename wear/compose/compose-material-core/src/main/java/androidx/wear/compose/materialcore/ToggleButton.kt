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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Round [ToggleButton] that offers a single slot to take any content
 * (text, icon or image).
 *
 * [ToggleButton]s can be enabled or disabled. A disabled toggle button will not respond to click
 * events.
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons#toggle-button)
 * guide.
 *
 * @param checked Boolean flag indicating whether this toggle button is currently checked.
 * @param onCheckedChange Callback to be invoked when this toggle button is clicked.
 * @param modifier Modifier to be applied to the toggle button.
 * @param enabled Controls the enabled state of the toggle button. When `false`,
 * this toggle button will not be clickable.
 * @param backgroundColor Resolves the background for this toggle button in different states.
 * @param border Resolves the border for this toggle button in different states.
 * @param toggleButtonSize The default size of the toggle button unless overridden by
 * [Modifier.size].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 * appearance or preview the toggle button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme.
 * @param ripple Ripple used for this toggle button
 * @param content The icon, image or text to be drawn inside the toggle button.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    backgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    border: @Composable (enabled: Boolean, checked: Boolean) -> BorderStroke?,
    toggleButtonSize: Dp,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    ripple: Indication,
    content: @Composable BoxScope.() -> Unit,
) {
    // Round toggle button
    val borderStroke = border(enabled, checked)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { role = Role.Checkbox }
            .size(toggleButtonSize)
            .clip(shape) // Clip for the touch area (e.g. for Ripple).
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple
            )
            .then(
                if (borderStroke != null) Modifier.border(border = borderStroke, shape = shape)
                else Modifier
            )
            .background(
                color = backgroundColor(enabled, checked).value,
                shape = shape
            ),
        content = content
    )
}

/**
 * The Stadium-shaped [ToggleButton] offers four slots and a specific layout for an icon, a
 * label, a secondaryLabel and toggle control. The icon and secondaryLabel are optional.
 * The items are laid out in a row with the optional icon at the start, a column containing the two
 * label slots in the middle and a slot for the toggle control at the end.
 *
 * ToggleButtons can be enabled or disabled. A disabled ToggleButton will not respond to
 * click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is
 * @param label A slot for providing the ToggleButton's main label. The contents are expected
 * to be text which is "start" aligned.
 * @param toggleControl A slot for providing the ToggleButton's toggle control.
 * Three built-in types of toggle control are supported.
 * @param modifier Modifier to be applied to the ToggleButton. Pass Modifier.height(height)
 * or Modifier.defaultMinSize(minHeight = minHeight) to set a fixed height or a minimum height
 * for the button respectively.
 * @param icon An optional slot for providing an icon to indicate the purpose of the ToggleButton.
 * @param secondaryLabel A slot for providing the ToggleButton's secondary label.
 * The contents are expected to be text which is "start" aligned if there is an icon preset and
 * "start" or "center" aligned if not. label and secondaryLabel contents should be
 * consistently aligned.
 * @param background Composable lambda to set the background of the toggle button.
 * This expects to return Modifier.paint or Modifier.background for the background treatment.
 * @param enabled Controls the enabled state of the ToggleButton. When `false`,
 * this ToggleButton will not be clickable
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this toggle button. You can use this to change the toggle button's
 * appearance or preview the toggle button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the ToggleButton's shape. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme
 * @param toggleControlWidth Width for the toggle control.
 * @param toggleControlHeight Height for the toggle control.
 * @param ripple Ripple used for this toggle button
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: @Composable RowScope.() -> Unit,
    toggleControl: @Composable () -> Unit,
    modifier: Modifier,
    icon: @Composable (BoxScope.() -> Unit)?,
    secondaryLabel: @Composable (RowScope.() -> Unit)?,
    background: @Composable (enabled: Boolean, checked: Boolean) -> Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    contentPadding: PaddingValues,
    shape: Shape,
    toggleControlWidth: Dp,
    toggleControlHeight: Dp,
    ripple: Indication
) {
    // Stadium/Chip shaped toggle button
    Row(
        modifier = modifier
            .clip(shape = shape)
            .width(IntrinsicSize.Max)
            .then(background(enabled, checked))
            .toggleable(
                enabled = enabled,
                value = checked,
                onValueChange = onCheckedChange,
                indication = ripple,
                interactionSource = interactionSource
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToggleButtonIcon(content = icon)
        Labels(
            label = label,
            secondaryLabel = secondaryLabel
        )
        Spacer(
            modifier = Modifier.size(
                TOGGLE_CONTROL_SPACING
            )
        )
        ToggleControl(
            width = toggleControlWidth,
            height = toggleControlHeight,
            content = toggleControl
        )
    }
}

/**
 * The [SplitToggleButton] offers three slots and a specific layout for a label,
 * secondaryLabel and toggle control. The secondaryLabel is optional. The items are laid out
 * with a column containing the two label slots and a slot for the toggle control at the
 * end.
 *
 * A [SplitToggleButton] has two tappable areas, one tap area for the labels and another for the
 * toggle control. The [onClick] listener will be associated with the main body of the
 * SplitToggleButton with the [onCheckedChange] listener associated with the toggle
 * control area only.
 *
 * For a SplitToggleButton the background of the tappable background area behind
 * the toggle control will have a visual effect applied to provide a "divider" between the two
 * tappable areas.
 *
 * SplitToggleButton can be enabled or disabled. A disabled SplitToggleButton will not
 * respond to click events.
 *
 * @param checked Boolean flag indicating whether this button is currently checked.
 * @param onCheckedChange Callback to be invoked when this buttons checked status is
 * changed.
 * @param label A slot for providing the SplitToggleButton's main label.
 * The contents are expected to be text which is "start" aligned.
 * @param onClick Click listener called when the user clicks the main body of the
 * SplitToggleButton, the area behind the labels.
 * @param toggleControl A slot for providing the SplitToggleButton's toggle control.
 * @param modifier Modifier to be applied to the SplitToggleButton
 * @param secondaryLabel A slot for providing the SplitToggleButton's secondary label.
 * The contents are expected to be "start" or "center" aligned. label and secondaryLabel
 * contents should be consistently aligned.
 * @param backgroundColor Composable lambda from which the backgroundColor will be obtained.
 * @param splitBackgroundColor Composable lambda from which the splitBackgroundOverlay will be
 * obtained.
 * @param enabled Controls the enabled state of the SplitToggleButton. When `false`,
 * this SplitToggleButton will not be clickable
 * @param checkedInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button's "toggleable" tap area. You can use this to change the
 * button's appearance or preview the button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param clickInteractionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button's "clickable" tap area. You can use this to change the
 * button's appearance or preview the button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param contentPadding The spacing values to apply internally between the container and the
 * content
 * @param shape Defines the SplitToggleButton's shape. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme
 * @param ripple Ripple used for this toggle button
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun SplitToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: @Composable RowScope.() -> Unit,
    onClick: () -> Unit,
    toggleControl: @Composable BoxScope.() -> Unit,
    modifier: Modifier,
    secondaryLabel: @Composable (RowScope.() -> Unit)?,
    backgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    splitBackgroundColor: @Composable (enabled: Boolean, checked: Boolean) -> State<Color>,
    enabled: Boolean,
    checkedInteractionSource: MutableInteractionSource?,
    clickInteractionSource: MutableInteractionSource?,
    contentPadding: PaddingValues,
    shape: Shape,
    ripple: Indication
) {
    val (startPadding, endPadding) = contentPadding.splitHorizontally()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .width(IntrinsicSize.Max)
            .clip(shape = shape)
            .background(backgroundColor(enabled, checked).value)
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = ripple,
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
                label = label,
                secondaryLabel = secondaryLabel,
            )
            Spacer(
                modifier = Modifier
                    .size(TOGGLE_CONTROL_SPACING)
            )
        }

        val splitBackground = splitBackgroundColor(
            enabled,
            checked,
        ).value

        Box(
            modifier = Modifier
                .toggleable(
                    enabled = enabled,
                    value = checked,
                    onValueChange = onCheckedChange,
                    indication = ripple,
                    interactionSource = checkedInteractionSource
                )
                .fillMaxHeight()
                .drawWithCache {
                    onDrawWithContent {
                        drawRect(color = splitBackground)
                        drawContent()
                    }
                }
                .align(Alignment.CenterVertically)
                .width(SPLIT_WIDTH)
                .wrapContentHeight(align = Alignment.CenterVertically)
                .wrapContentWidth(align = Alignment.End)
                .then(endPadding),
            content = toggleControl
        )
    }
}

@Composable
private fun ToggleButtonIcon(
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    if (content != null) {
        Box(
            modifier = Modifier.wrapContentSize(align = Alignment.Center),
            content = content
        )
        Spacer(modifier = Modifier.size(ICON_SPACING))
    }
}

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
private fun RowScope.ToggleControl(
    width: Dp,
    height: Dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .size(width = width, height = height)
            .wrapContentWidth(align = Alignment.End),
    ) {
        content()
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

private val TOGGLE_CONTROL_SPACING = 4.dp
private val ICON_SPACING = 6.dp
private val SPLIT_WIDTH = 52.dp
