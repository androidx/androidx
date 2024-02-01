/*
 * Copyright 2022 The Android Open Source Project
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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
 * Wear Material [RoundButton] that offers a single slot to take any content (text, icon or image)
 * and is round/circular in shape.
 *
 * [RoundButton] can be enabled or disabled. A disabled button will not respond to click events.
 *
 * For more information, see the
 * [Buttons](https://developer.android.com/training/wearables/components/buttons)
 * guide.
 *
 * @param onClick Will be called when the user clicks the button.
 * @param modifier Modifier to be applied to the button.
 * @param enabled Controls the enabled state of the button. When `false`, this button will not
 * be clickable.
 * @param backgroundColor Resolves the background for this button in different states.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this button. You can use this to change the button's appearance
 * or preview the button in different states. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param shape Defines the button's shape.
 * @param border Resolves the border for this button in different states.
 * @param buttonSize The default size of the button unless overridden by Modifier.size.
 * @param ripple Ripple used for this button.
 * @param content The content displayed on the [RoundButton] such as text, icon or image.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    backgroundColor: @Composable (enabled: Boolean) -> Color,
    interactionSource: MutableInteractionSource?,
    shape: Shape,
    border: @Composable (enabled: Boolean) -> BorderStroke?,
    buttonSize: Dp,
    ripple: Indication,
    content: @Composable BoxScope.() -> Unit,
) {
    val borderStroke = border(enabled)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics { role = Role.Button }
            .size(buttonSize)
            .clip(shape) // Clip for the touch area (e.g. for Ripple).
            .clickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple,
            )
            .then(
                if (borderStroke != null) Modifier.border(border = borderStroke, shape = shape)
                else Modifier
            )
            .background(
                color = backgroundColor(enabled),
                shape = shape
            ),
        content = content
    )
}
