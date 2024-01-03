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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp

/**
 * Wear Material [ToggleButton] that offers a single slot to take any content
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
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * [Interaction]s for this toggle button. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this ToggleButton in different [Interaction]s.
 * @param shape Defines the shape for this toggle button. It is strongly recommended to use the
 * default as this shape is a key characteristic of the Wear Material Theme.
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
    border: @Composable (enabled: Boolean, checked: Boolean) -> State<BorderStroke?>?,
    toggleButtonSize: Dp,
    interactionSource: MutableInteractionSource,
    shape: Shape,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderStroke = border(enabled, checked)?.value
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape) // Clip for the touch area (e.g. for Ripple).
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = null, // // Provide the role via Modifier.semantics
                interactionSource = interactionSource,
                indication = rememberRipple()
            )
            .then(
                // Make sure modifier ordering is clip > toggleable > padding > size,
                // so that the ripple applies to the entire button shape and size.
                modifier.semantics { role = Role.Checkbox }
            )
            .size(toggleButtonSize)
            .clip(shape) // Clip for the painted background area after size has been applied.
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
