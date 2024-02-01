/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.compose.material3

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.tokens.FilledIconButtonTokens
import androidx.compose.material3.tokens.FilledTonalIconButtonTokens
import androidx.compose.material3.tokens.IconButtonTokens
import androidx.compose.material3.tokens.OutlinedIconButtonTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design standard icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.iconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonTokens.StateLayerSize)
            .clip(IconButtonTokens.StateLayerShape.value)
            .background(color = colors.containerColor(enabled))
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = rippleOrFallbackImplementation(
                    bounded = false,
                    radius = IconButtonTokens.StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colors.contentColor(enabled)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design standard icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon toggle button image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 *
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.iconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(IconButtonTokens.StateLayerSize)
            .clip(IconButtonTokens.StateLayerShape.value)
            .background(color = colors.containerColor(enabled, checked).value)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = rippleOrFallbackImplementation(
                    bounded = false,
                    radius = IconButtonTokens.StateLayerSize / 2
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colors.contentColor(enabled, checked).value
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Filled icon button sample:
 * @sample androidx.compose.material3.samples.FilledIconButtonSample
 *
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier.semantics { role = Role.Button },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled),
    contentColor = colors.contentColor(enabled),
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled tonal icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-button.png)
 *
 * A filled tonal icon button is a medium-emphasis icon button that is an alternative middle
 * ground between the default [FilledIconButton] and [OutlinedIconButton].
 * They can be used in contexts where the lower-priority icon button requires slightly more emphasis
 * than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Filled tonal icon button sample:
 * @sample androidx.compose.material3.samples.FilledTonalIconButtonSample
 *
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier.semantics { role = Role.Button },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled),
    contentColor = colors.contentColor(enabled),
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledTonalIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon toggle button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Toggleable filled icon button sample:
 * @sample androidx.compose.material3.samples.FilledIconToggleButtonSample
 *
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilledIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconToggleButtonColors = IconButtonDefaults.filledIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design filled tonal icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon toggle button image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-toggle-button.png)
 *
 * A filled tonal toggle icon button is a medium-emphasis icon button that is an alternative
 * middle ground between the default [FilledIconToggleButton] and [OutlinedIconToggleButton].
 * They can be used in contexts where the lower-priority icon button requires slightly more emphasis
 * than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * Toggleable filled tonal icon button sample:
 * @sample androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
 *
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(FilledTonalIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design outlined icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon button image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-button.png)
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * The outlined icon button has an overall minimum touch target size of 48 x 48dp, to meet
 * accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconButtonSample
 *
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container and border (when [border] is not
 * null)
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.outlinedIconButtonColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 * border. See [IconButtonDefaults.outlinedIconButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.outlinedShape,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    onClick = onClick,
    modifier = modifier.semantics { role = Role.Button },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled),
    contentColor = colors.contentColor(enabled),
    border = border,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(OutlinedIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external" target="_blank">Material Design outlined icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon toggle button image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp.
 * This icon button has an overall minimum touch target size of 48 x 48dp, to meet accessibility
 * guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconToggleButtonSample
 *
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param shape defines the shape of this icon button's container and border (when [border] is not
 * null)
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 * button in different states. See [IconButtonDefaults.outlinedIconToggleButtonColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 * border. See [IconButtonDefaults.outlinedIconToggleButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 * emitting [Interaction]s for this icon button. You can use this to change the icon button's
 * appearance or preview the icon button in different states. Note that if `null` is provided,
 * interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.outlinedShape,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconToggleButtonBorder(enabled, checked),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) = Surface(
    checked = checked,
    onCheckedChange = onCheckedChange,
    modifier = modifier.semantics { role = Role.Checkbox },
    enabled = enabled,
    shape = shape,
    color = colors.containerColor(enabled, checked).value,
    contentColor = colors.contentColor(enabled, checked).value,
    border = border,
    interactionSource = interactionSource
) {
    Box(
        modifier = Modifier.size(OutlinedIconButtonTokens.ContainerSize),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Contains the default values used by all icon button types.
 */
object IconButtonDefaults {
    /** Default shape for a filled icon button. */
    val filledShape: Shape @Composable get() = FilledIconButtonTokens.ContainerShape.value

    /** Default shape for an outlined icon button. */
    val outlinedShape: Shape
        @Composable get() =
            OutlinedIconButtonTokens.ContainerShape.value

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [IconButton].
     */
    @Composable
    fun iconButtonColors(): IconButtonColors {
        val colors = MaterialTheme.colorScheme.defaultIconButtonColors
        val contentColor = LocalContentColor.current
        if (colors.contentColor == contentColor) {
            return colors
        } else {
            return colors.copy(
                contentColor = contentColor,
                disabledContentColor =
                contentColor.copy(alpha = IconButtonTokens.DisabledIconOpacity)
            )
        }
    }

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [IconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun iconButtonColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Unspecified,
        disabledContentColor: Color =
            contentColor.copy(alpha = IconButtonTokens.DisabledIconOpacity)
    ): IconButtonColors = MaterialTheme.colorScheme.defaultIconButtonColors.copy(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    internal val ColorScheme.defaultIconButtonColors: IconButtonColors
        @Composable
        get() {
            return defaultIconButtonColorsCached ?: run {
                val localContentColor = LocalContentColor.current
                IconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = localContentColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor =
                    localContentColor.copy(alpha = IconButtonTokens.DisabledIconOpacity)
                ).also {
                    defaultIconButtonColorsCached = it
                }
            }
        }

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [IconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun iconToggleButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color =
            contentColor.copy(alpha = IconButtonTokens.DisabledIconOpacity),
        checkedContainerColor: Color = Color.Transparent,
        checkedContentColor: Color = IconButtonTokens.SelectedIconColor.value
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a [FilledIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledIconButtonColors(
        containerColor: Color = FilledIconButtonTokens.ContainerColor.value,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledIconButtonTokens.DisabledContainerColor.value
            .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledIconButtonTokens.DisabledColor.value
            .copy(alpha = FilledIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        IconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledIconToggleButtonColors(
        containerColor: Color = FilledIconButtonTokens.UnselectedContainerColor.value,
        // TODO(b/228455081): Using contentColorFor here will return OnSurfaceVariant,
        //  while the token value is Primary.
        contentColor: Color = FilledIconButtonTokens.ToggleUnselectedColor.value,
        disabledContainerColor: Color = FilledIconButtonTokens.DisabledContainerColor.value
            .copy(alpha = FilledIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledIconButtonTokens.DisabledColor.value
            .copy(alpha = FilledIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color = FilledIconButtonTokens.SelectedContainerColor.value,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [FilledTonalIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun filledTonalIconButtonColors(
        containerColor: Color = FilledTonalIconButtonTokens.ContainerColor.value,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledTonalIconButtonTokens.DisabledContainerColor.value
            .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledTonalIconButtonTokens.DisabledColor.value
            .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        IconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [FilledTonalIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun filledTonalIconToggleButtonColors(
        containerColor: Color = FilledTonalIconButtonTokens.UnselectedContainerColor.value,
        contentColor: Color = contentColorFor(containerColor),
        disabledContainerColor: Color = FilledTonalIconButtonTokens.DisabledContainerColor.value
            .copy(alpha = FilledTonalIconButtonTokens.DisabledContainerOpacity),
        disabledContentColor: Color = FilledTonalIconButtonTokens.DisabledColor.value
            .copy(alpha = FilledTonalIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color =
            FilledTonalIconButtonTokens.SelectedContainerColor.value,
        checkedContentColor: Color = FilledTonalIconButtonTokens.ToggleSelectedColor.value
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Creates a [IconButtonColors] that represents the default colors used in a
     * [OutlinedIconButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     */
    @Composable
    fun outlinedIconButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity)
    ): IconButtonColors =
        IconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )

    /**
     * Creates a [IconToggleButtonColors] that represents the default colors used in a
     * [OutlinedIconToggleButton].
     *
     * @param containerColor the container color of this icon button when enabled.
     * @param contentColor the content color of this icon button when enabled.
     * @param disabledContainerColor the container color of this icon button when not enabled.
     * @param disabledContentColor the content color of this icon button when not enabled.
     * @param checkedContainerColor the container color of this icon button when checked.
     * @param checkedContentColor the content color of this icon button when checked.
     */
    @Composable
    fun outlinedIconToggleButtonColors(
        containerColor: Color = Color.Transparent,
        contentColor: Color = LocalContentColor.current,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color =
            contentColor.copy(alpha = OutlinedIconButtonTokens.DisabledOpacity),
        checkedContainerColor: Color =
            OutlinedIconButtonTokens.SelectedContainerColor.value,
        checkedContentColor: Color = contentColorFor(checkedContainerColor)
    ): IconToggleButtonColors =
        IconToggleButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
            checkedContainerColor = checkedContainerColor,
            checkedContentColor = checkedContentColor,
        )

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] and
     * [checked] state.
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    fun outlinedIconToggleButtonBorder(enabled: Boolean, checked: Boolean): BorderStroke? {
        if (checked) {
            return null
        }
        return outlinedIconButtonBorder(enabled)
    }

    /**
     * Represents the [BorderStroke] for an [OutlinedIconButton], depending on its [enabled] state.
     *
     * @param enabled whether the icon button is enabled
     */
    @Composable
    fun outlinedIconButtonBorder(enabled: Boolean): BorderStroke {
        val color: Color = if (enabled) {
            LocalContentColor.current
        } else {
            LocalContentColor.current
                .copy(alpha = OutlinedIconButtonTokens.DisabledUnselectedOutlineOpacity)
        }
        return remember(color) {
            BorderStroke(OutlinedIconButtonTokens.UnselectedOutlineWidth, color)
        }
    }
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * @constructor create an instance with arbitrary colors.
 * - See [IconButtonDefaults.filledIconButtonColors] and
 * [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonColors] for the default colors used in an
 * [OutlinedIconButton].
 *
 * @param containerColor the container color of this icon button when enabled.
 * @param contentColor the content color of this icon button when enabled.
 * @param disabledContainerColor the container color of this icon button when not enabled.
 * @param disabledContentColor the content color of this icon button when not enabled.
 */
@Immutable
class IconButtonColors constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {

    /**
     * Returns a copy of this IconButtonColors, optionally overriding some of the values.
     * This uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
    ) = IconButtonColors(
        containerColor.takeOrElse { this.containerColor },
        contentColor.takeOrElse { this.contentColor },
        disabledContainerColor.takeOrElse { this.disabledContainerColor },
        disabledContentColor.takeOrElse { this.disabledContentColor },
    )

    /**
     * Represents the container color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Stable
    internal fun containerColor(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    /**
     * Represents the content color for this icon button, depending on [enabled].
     *
     * @param enabled whether the icon button is enabled
     */
    @Stable
    internal fun contentColor(enabled: Boolean): Color =
        if (enabled) contentColor else disabledContentColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()

        return result
    }
}

/**
 * Represents the container and content colors used in a toggleable icon button in
 * different states.
 *
 * @constructor create an instance with arbitrary colors.
 * - See [IconButtonDefaults.filledIconToggleButtonColors] and
 * [IconButtonDefaults.filledTonalIconToggleButtonColors] for the default colors used in a
 * [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconToggleButtonColors] for the default colors used in a
 *  toggleable [OutlinedIconButton].
 *
 * @param containerColor the container color of this icon button when enabled.
 * @param contentColor the content color of this icon button when enabled.
 * @param disabledContainerColor the container color of this icon button when not enabled.
 * @param disabledContentColor the content color of this icon button when not enabled.
 * @param checkedContainerColor the container color of this icon button when checked.
 * @param checkedContentColor the content color of this icon button when checked.
 */
@Immutable
class IconToggleButtonColors constructor(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
) {
    /**
     * Represents the container color for this icon button, depending on [enabled] and [checked].
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    internal fun containerColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledContainerColor
            !checked -> containerColor
            else -> checkedContainerColor
        }
        return rememberUpdatedState(target)
    }

    /**
     * Represents the content color for this icon button, depending on [enabled] and [checked].
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    internal fun contentColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledContentColor
            !checked -> contentColor
            else -> checkedContentColor
        }
        return rememberUpdatedState(target)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconToggleButtonColors) return false

        if (containerColor != other.containerColor) return false
        if (contentColor != other.contentColor) return false
        if (disabledContainerColor != other.disabledContainerColor) return false
        if (disabledContentColor != other.disabledContentColor) return false
        if (checkedContainerColor != other.checkedContainerColor) return false
        if (checkedContentColor != other.checkedContentColor) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + disabledContainerColor.hashCode()
        result = 31 * result + disabledContentColor.hashCode()
        result = 31 * result + checkedContainerColor.hashCode()
        result = 31 * result + checkedContentColor.hashCode()

        return result
    }
}
