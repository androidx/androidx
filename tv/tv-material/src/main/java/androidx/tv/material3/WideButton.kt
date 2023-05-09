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

package androidx.tv.material3

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.tokens.Elevation

/**
 * Material Design wide button for TV.
 *
 * @param onClick called when this button is clicked
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param background the background to be applied to the [WideButton]
 * @param scale Defines size of the Button relative to its original size.
 * @param glow Shadow to be shown behind the Button.
 * @param shape Defines the Button's shape.
 * @param contentColor Color to be used for the text content of the Button
 * @param tonalElevation tonal elevation used to apply a color shift to the button to give the it
 * higher emphasis
 * @param border Defines a border around the Button.
 * @param contentPadding the spacing values to apply internally between the container and the
 * content
 * @param content the content of the button
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun WideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    background: @Composable () -> Unit = {
        WideButtonDefaults.Background(
            enabled = enabled,
            interactionSource = interactionSource,
        )
    },
    scale: ButtonScale = WideButtonDefaults.scale(),
    glow: ButtonGlow = WideButtonDefaults.glow(),
    shape: ButtonShape = WideButtonDefaults.shape(),
    contentColor: WideButtonContentColor = WideButtonDefaults.contentColor(),
    tonalElevation: Dp = Elevation.Level0,
    border: ButtonBorder = WideButtonDefaults.border(),
    contentPadding: PaddingValues = WideButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    WideButtonImpl(
        onClick = onClick,
        enabled = enabled,
        scale = scale,
        glow = glow,
        shape = shape,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        modifier = modifier,
        background = background,
        content = content
    )
}

/**
 * Material Design wide button for TV.
 *
 * @param onClick called when this button is clicked
 * @param title the title content of the button, typically a [Text]
 * @param modifier the [Modifier] to be applied to this button
 * @param enabled controls the enabled state of this button. When `false`, this component will not
 * respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param icon the leading icon content of the button, typically an [Icon]
 * @param subtitle the subtitle content of the button, typically a [Text]
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this button. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this button in different states.
 * @param background the background to be applied to the [WideButton]
 * @param scale Defines size of the Button relative to its original size.
 * @param glow Shadow to be shown behind the Button.
 * @param shape Defines the Button's shape.
 * @param contentColor Color to be used for the text content of the Button
 * @param tonalElevation tonal elevation used to apply a color shift to the button to give the it
 * higher emphasis
 * @param border Defines a border around the Button.
 * @param contentPadding the spacing values to apply internally between the container and the
 * content
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun WideButton(
    onClick: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    background: @Composable () -> Unit = {
        WideButtonDefaults.Background(
            enabled = enabled,
            interactionSource = interactionSource
        )
    },
    scale: ButtonScale = WideButtonDefaults.scale(),
    glow: ButtonGlow = WideButtonDefaults.glow(),
    shape: ButtonShape = WideButtonDefaults.shape(),
    contentColor: WideButtonContentColor = WideButtonDefaults.contentColor(),
    tonalElevation: Dp = Elevation.Level0,
    border: ButtonBorder = WideButtonDefaults.border(),
    contentPadding: PaddingValues = WideButtonDefaults.ContentPadding,
) {

    WideButtonImpl(
        onClick = onClick,
        enabled = enabled,
        scale = scale,
        glow = glow,
        shape = shape,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        modifier = modifier,
        minHeight = if (subtitle == null)
            BaseWideButtonDefaults.MinHeight
        else
            BaseWideButtonDefaults.MinHeightWithSubtitle,
        background = background
    ) {
        if (icon != null) {
            icon()
            Spacer(
                modifier = Modifier.padding(end = BaseWideButtonDefaults.HorizontalContentGap)
            )
        }
        Column {
            ProvideTextStyle(
                value = MaterialTheme.typography.titleMedium,
                content = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = BaseWideButtonDefaults.VerticalContentGap)
                    ) {
                        title()
                    }
                }
            )
            if (subtitle != null) {
                ProvideTextStyle(
                    value = MaterialTheme.typography.bodySmall.copy(
                        color = LocalContentColor.current.copy(
                            alpha = BaseWideButtonDefaults.SubtitleAlpha
                        )
                    ),
                    content = subtitle
                )
            }
        }
    }
}

@ExperimentalTvMaterial3Api
@Composable
private fun WideButtonImpl(
    onClick: () -> Unit,
    enabled: Boolean,
    scale: ButtonScale,
    glow: ButtonGlow,
    shape: ButtonShape,
    contentColor: WideButtonContentColor,
    tonalElevation: Dp,
    border: ButtonBorder,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource,
    background: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = BaseWideButtonDefaults.MinHeight,
    content: @Composable RowScope.() -> Unit
) {
    val density = LocalDensity.current
    var buttonWidth by remember { mutableStateOf(0.dp) }
    var buttonHeight by remember { mutableStateOf(0.dp) }

    Surface(
        modifier = modifier.semantics { role = Role.Button },
        onClick = onClick,
        enabled = enabled,
        scale = scale.toClickableSurfaceScale(),
        glow = glow.toClickableSurfaceGlow(),
        shape = shape.toClickableSurfaceShape(),
        colors = contentColor.toClickableSurfaceColors(),
        tonalElevation = tonalElevation,
        border = border.toClickableSurfaceBorder(),
        interactionSource = interactionSource
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
            Box(
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = BaseWideButtonDefaults.MinWidth,
                        minHeight = minHeight,
                    )
                    .onPlaced {
                        with(density) {
                            buttonWidth = it.size.width.toDp()
                            buttonHeight = it.size.height.toDp()
                        }
                    }
            ) {
                Box(modifier = Modifier.size(buttonWidth, buttonHeight)) {
                    background()
                }

                Row(
                    modifier = Modifier
                        .size(buttonWidth, buttonHeight)
                        .padding(contentPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
