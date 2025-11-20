/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Button is a component used for exposing actions to a user.
 *
 * @sample androidx.xr.glimmer.samples.ButtonSample
 *
 * Buttons can use icons to provide more context about the action:
 *
 * @sample androidx.xr.glimmer.samples.ButtonWithLeadingIconSample
 *
 * There are multiple button size variants - providing a different [ButtonSize] will affect default
 * values used inside this button, such as the minimum height and the size of icons inside this
 * button. Note that you can still provide a size modifier such as
 * [androidx.compose.foundation.layout.size] to change the layout size of this button, [buttonSize]
 * affects default values and values internal to the button.
 *
 * @sample androidx.xr.glimmer.samples.LargeButtonSample
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this button will not
 *   respond to user input
 * @param buttonSize the size variant of this button, represented as a [ButtonSize]. Changing
 *   [buttonSize] will affect some default values used by this button - but the final resulting size
 *   of the button will still be calculated based on the content of the button, and any provided
 *   size modifiers such as [androidx.compose.foundation.layout.size]. For example, setting a 100.dp
 *   size using a size modifier will result in the same layout size regardless of [buttonSize], but
 *   the provided [buttonSize] will affect other properties such as padding values and the size of
 *   icons.
 * @param leadingIcon optional leading icon to be placed before the [content]. This is typically an
 *   [Icon].
 * @param trailingIcon optional trailing icon to be placed after the [content]. This is typically an
 *   [Icon].
 * @param shape the [Shape] used to clip this button, and also used to draw the background and
 *   border
 * @param color background color of this button
 * @param contentColor content color used by components inside [content]
 * @param border the border to draw around this button
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this button. You can use this to change the button's appearance or
 *   preview the button in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content the main content, typically [Text], to display inside this button
 */
@Composable
public fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: ButtonSize = ButtonSize.Medium,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = GlimmerTheme.shapes.large,
    color: Color = GlimmerTheme.colors.surface,
    contentColor: Color = calculateContentColor(color),
    border: BorderStroke? = SurfaceDefaults.border(),
    contentPadding: PaddingValues = ButtonDefaults.contentPadding(buttonSize),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = GlimmerTheme.colors
    val iconSizes = GlimmerTheme.iconSizes
    val iconSize =
        if (buttonSize == ButtonSize.Medium) {
            iconSizes.medium
        } else {
            iconSizes.large
        }

    val minHeight =
        if (buttonSize == ButtonSize.Medium) {
            MediumMinimumHeight
        } else {
            LargeMinimumHeight
        }

    val depth = SurfaceDepth(depth = null, focusedDepth = GlimmerTheme.depthLevels.level1)

    CompositionLocalProvider(LocalTextStyle provides GlimmerTheme.typography.bodySmall) {
        Row(
            modifier
                .semantics { role = Role.Button }
                .surface(
                    enabled = enabled,
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    depth = depth,
                    border = border,
                    interactionSource = interactionSource,
                    onClick = onClick,
                )
                .defaultMinSize(minHeight = minHeight)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(Modifier.padding(end = IconSpacing).contentColorProvider(colors.primary)) {
                    CompositionLocalProvider(LocalIconSize provides iconSize, content = leadingIcon)
                }
            }
            content()
            if (trailingIcon != null) {
                Box(Modifier.padding(start = IconSpacing).contentColorProvider(colors.primary)) {
                    CompositionLocalProvider(
                        LocalIconSize provides iconSize,
                        content = trailingIcon,
                    )
                }
            }
        }
    }
}

/**
 * ButtonSize represents the different size variants of a [Button]. ButtonSize will affect default
 * values used inside a [Button], such as the minimum height and the size of icons.
 */
@Immutable
@JvmInline
public value class ButtonSize internal constructor(private val value: Int) {
    public companion object {
        /** ButtonSize representing a medium [Button]. This is the default size. */
        public val Medium: ButtonSize = ButtonSize(1)
        /** ButtonSize representing a large [Button]. */
        public val Large: ButtonSize = ButtonSize(2)
    }
}

/** Default values used for [Button]. */
public object ButtonDefaults {
    /** Default content padding used for a [Button] with the specified [buttonSize]. */
    public fun contentPadding(buttonSize: ButtonSize): PaddingValues {
        return if (buttonSize == ButtonSize.Medium) {
            MediumContentPadding
        } else {
            LargeContentPadding
        }
    }
}

/** Default content padding for a medium [Button] */
private val MediumContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

/** Default content padding for a large [Button] */
private val LargeContentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)

/** Default minimum height for a medium [Button] */
private val MediumMinimumHeight = 56.dp

/** Default minimum height for a large [Button] */
private val LargeMinimumHeight = 72.dp

/** Spacing between icons and the text in a [Button] */
private val IconSpacing = 8.dp
