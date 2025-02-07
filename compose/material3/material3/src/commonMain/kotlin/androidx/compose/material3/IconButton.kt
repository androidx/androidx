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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.internal.childSemantics
import androidx.compose.material3.internal.rememberAnimatedShape
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Simple Usage
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * IconButton with a color tint
 *
 * @sample androidx.compose.material3.samples.TintedIconButtonSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconButtonVibrantColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Deprecated(
    message = "Use overload with `shape`",
    replaceWith =
        ReplaceWith(
            "IconButton(onClick, modifier, enabled, colors, interactionSource, shape, content)"
        ),
    level = DeprecationLevel.HIDDEN
)
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick,
        modifier,
        enabled,
        colors,
        interactionSource,
        IconButtonDefaults.standardShape,
        content
    )
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Simple Usage
 *
 * @sample androidx.compose.material3.samples.IconButtonSample
 *
 * IconButton with a color tint
 *
 * @sample androidx.compose.material3.samples.TintedIconButtonSample
 *
 * Small-sized narrow round shape IconButton
 *
 * @sample androidx.compose.material3.samples.XSmallNarrowSquareIconButtonsSample
 *
 * Medium / default size round-shaped icon button
 *
 * @sample androidx.compose.material3.samples.MediumRoundWideIconButtonSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconButtonVibrantColors] and
 *   [IconButtonDefaults.iconButtonColors] .
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the [Shape] of this icon button.
 * @param content the content of this icon button, typically an [Icon]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.standardShape,
    content: @Composable () -> Unit
) =
    IconButtonImpl(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shape = shape,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_icon_button_round_enabled_pressed.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Simple Usage
 *
 * @sample androidx.compose.material3.samples.IconButtonWithAnimatedShapeSample
 * @param onClick called when this icon button is clicked
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconButtonVibrantColors] and
 *   [IconButtonDefaults.iconButtonColors] .
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun IconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    IconButtonImpl(
        modifier,
        onClick = onClick,
        enabled = enabled,
        shape = shapeForInteraction(shapes, interactionSource),
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun IconButtonImpl(
    modifier: Modifier,
    onClick: () -> Unit,
    enabled: Boolean,
    shape: Shape,
    colors: IconButtonColors,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.smallContainerSize())
                .clip(shape)
                .background(color = colors.containerColor(enabled), shape = shape)
                .clickable(
                    onClick = onClick,
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = ripple()
                )
                .childSemantics()
                .interactionSourceData(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colors.contentColor(enabled)
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconToggleButtonVibrantColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@Deprecated(
    message = "Use overload with `shape`",
    replaceWith =
        ReplaceWith(
            "IconToggleButton(checked, onCheckedChange, modifier, enabled, colors," +
                " interactionSource, shape, content)"
        ),
    level = DeprecationLevel.HIDDEN
)
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
    IconToggleButton(
        checked,
        onCheckedChange,
        modifier,
        enabled,
        colors,
        interactionSource,
        IconButtonDefaults.standardShape,
        content
    )
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/standard-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconToggleButtonVibrantColors] and
 *   [IconButtonDefaults.iconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param shape the [Shape] of this icon button.
 * @param content the content of this icon button, typically an [Icon]
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.standardShape,
    content: @Composable () -> Unit
) =
    IconToggleButtonImpl(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        shape = shape,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design standard icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Standard icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_icon_button_round_unselected_select.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.IconToggleButtonWithAnimatedShapeSample
 * @param checked whether this button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param shapes the [IconToggleButtonShapes] that the icon toggle button will morph between
 *   depending on the user's interaction with this button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.iconToggleButtonVibrantColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    IconToggleButtonImpl(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shape = shapeForInteraction(checked, shapes, interactionSource),
        colors = colors,
        interactionSource = interactionSource,
        content = content
    )
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun IconToggleButtonImpl(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.iconToggleButtonVibrantColors(),
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = IconButtonDefaults.standardShape,
    content: @Composable () -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(IconButtonDefaults.smallContainerSize())
                .clip(shape)
                .background(color = colors.containerColor(enabled, checked).value)
                .toggleable(
                    value = checked,
                    onValueChange = onCheckedChange,
                    enabled = enabled,
                    role = Role.Checkbox,
                    interactionSource = interactionSource,
                    indication = ripple()
                )
                .interactionSourceData(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        val contentColor = colors.contentColor(enabled, checked).value
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Filled icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledIconButtonSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
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
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_filled_icon_button_round_enabled_pressed.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Filled icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledIconButtonWithAnimatedShapeSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Toggleable filled icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledIconToggleButtonSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
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
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_filled_icon_button_round_unselected_select.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Toggleable filled icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledIconToggleButtonWithAnimatedShapeSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FilledIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.filledIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled tonal icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-button.png)
 *
 * A filled tonal icon button is a medium-emphasis icon button that is an alternative middle ground
 * between the default [FilledIconButton] and [OutlinedIconButton]. They can be used in contexts
 * where the lower-priority icon button requires slightly more emphasis than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Filled tonal icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledTonalIconButtonSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
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
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled tonal icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_tonal_filled_icon_button_round_enabled_pressed.png)
 *
 * A filled tonal icon button is a medium-emphasis icon button that is an alternative middle ground
 * between the default [FilledIconButton] and [OutlinedIconButton]. They can be used in contexts
 * where the lower-priority icon button requires slightly more emphasis than an outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Filled tonal icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledTonalIconButtonWithAnimatedShapeSample
 * @param onClick called when this icon button is clicked
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FilledTonalIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled tonal icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/filled-tonal-icon-toggle-button.png)
 *
 * A filled tonal toggle icon button is a medium-emphasis icon button that is an alternative middle
 * ground between the default [FilledIconToggleButton] and [OutlinedIconToggleButton]. They can be
 * used in contexts where the lower-priority icon button requires slightly more emphasis than an
 * outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Toggleable filled tonal icon button sample:
 *
 * @sample androidx.compose.material3.samples.FilledTonalIconToggleButtonSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
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
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design filled tonal icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Filled tonal icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_tonal_filled_icon_button_round_unselected_select.png)
 *
 * A filled tonal toggle icon button is a medium-emphasis icon button that is an alternative middle
 * ground between the default [FilledIconToggleButton] and [OutlinedIconToggleButton]. They can be
 * used in contexts where the lower-priority icon button requires slightly more emphasis than an
 * outline would give.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Toggleable filled tonal icon button with animatable shape sample:
 *
 * @sample androidx.compose.material3.samples.FilledTonalIconToggleButtonWithAnimatedShapeSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.filledIconToggleButtonColors].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun FilledTonalIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.filledTonalIconToggleButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = null,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design outlined icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-button.png)
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. The outlined icon
 * button has an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconButtonSample
 *
 * Large-sized uniform rounded shape
 *
 * @sample androidx.compose.material3.samples.LargeRoundUniformOutlinedIconButtonSample
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container and border (when [border] is not
 *   null)
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.outlinedIconButtonVibrantColors] and
 *   [IconButtonDefaults.outlinedIconButtonColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 *   border. See [IconButtonDefaults.outlinedIconButtonBorder] and
 *   [IconButtonDefaults.outlinedIconButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
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
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design outlined icon button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_outlined_icon_button_round_enabled_pressed.png)
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * Use this "contained" icon button when the component requires more visual separation from the
 * background.
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. The outlined icon
 * button has an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * Toggleable filled tonal icon button with animatable shape sample:
 *
 * @sample androidx.compose.material3.samples.OutlinedIconButtonWithAnimatedShapeSample
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param onClick called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.outlinedIconButtonVibrantColors] and
 *   [IconButtonDefaults.outlinedIconButtonColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 *   border. See [IconButtonDefaults.outlinedIconButtonBorder] and
 *   [IconButtonDefaults.outlinedIconButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun OutlinedIconButton(
    onClick: () -> Unit,
    shapes: IconButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.outlinedIconButtonColors(),
    border: BorderStroke? = IconButtonDefaults.outlinedIconButtonBorder(enabled),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design outlined icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/outlined-icon-toggle-button.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconToggleButtonSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param shape defines the shape of this icon button's container and border (when [border] is not
 *   null)
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.outlinedIconToggleButtonVibrantColors] and
 *   [IconButtonDefaults.outlinedIconToggleButtonColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 *   border. See [IconButtonDefaults.outlinedIconToggleButtonVibrantBorder] and
 *   [IconButtonDefaults.outlinedIconToggleButtonBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
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
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )

/**
 * <a href="https://m3.material.io/components/icon-button/overview" class="external"
 * target="_blank">Material Design outlined icon toggle button</a>.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * ![Outlined icon toggle button
 * image](https://developer.android.com/images/reference/androidx/compose/material3/small_outlined_icon_button_round_unselected_select.png)
 *
 * [content] should typically be an [Icon] (see [androidx.compose.material.icons.Icons]). If using a
 * custom icon, note that the typical size for the internal icon is 24 x 24 dp. This icon button has
 * an overall minimum touch target size of 48 x 48dp, to meet accessibility guidelines.
 *
 * @sample androidx.compose.material3.samples.OutlinedIconToggleButtonWithAnimatedShapeSample
 * @param checked whether this icon button is toggled on or off
 * @param onCheckedChange called when this icon button is clicked
 * @param shapes the [IconButtonShapes] that the icon button will morph between depending on the
 *   user's interaction with the icon button.
 * @param modifier the [Modifier] to be applied to this icon button
 * @param enabled controls the enabled state of this icon button. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [IconToggleButtonColors] that will be used to resolve the colors used for this icon
 *   button in different states. See [IconButtonDefaults.outlinedIconToggleButtonVibrantColors].
 * @param border the border to draw around the container of this icon button. Pass `null` for no
 *   border. See [IconButtonDefaults.outlinedIconToggleButtonVibrantBorder].
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this icon button. You can use this to change the icon button's
 *   appearance or preview the icon button in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 * @param content the content of this icon button, typically an [Icon]
 */
@ExperimentalMaterial3ExpressiveApi
@Composable
fun OutlinedIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: IconToggleButtonShapes,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconToggleButtonColors = IconButtonDefaults.outlinedIconToggleButtonVibrantColors(),
    border: BorderStroke? =
        IconButtonDefaults.outlinedIconToggleButtonVibrantBorder(enabled, checked),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) =
    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { role = Role.Checkbox },
        enabled = enabled,
        shapes = shapes,
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SurfaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: IconButtonColors,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit
) =
    Surface(
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
            modifier = Modifier.size(IconButtonDefaults.smallContainerSize()),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun SurfaceIconButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shapes: IconButtonShapes,
    colors: IconButtonColors,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit
) {

    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    SurfaceIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shapeForInteraction(shapes, interactionSource),
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SurfaceIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: IconToggleButtonColors,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit
) {
    Surface(
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
            modifier =
                Modifier.size(
                        IconButtonDefaults.smallContainerSize(),
                    )
                    .then(
                        when (shape) {
                            is ShapeWithHorizontalCenterOptically -> {
                                Modifier.horizontalCenterOptically(
                                    shape = shape,
                                    maxStartOffset = Int.MAX_VALUE.dp,
                                    maxEndOffset = Int.MAX_VALUE.dp
                                )
                            }
                            is CornerBasedShape -> {
                                Modifier.horizontalCenterOptically(
                                    shape = shape,
                                    maxStartOffset = Int.MAX_VALUE.dp,
                                    maxEndOffset = Int.MAX_VALUE.dp
                                )
                            }
                            else -> {
                                Modifier
                            }
                        }
                    ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun SurfaceIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shapes: IconToggleButtonShapes,
    colors: IconToggleButtonColors,
    border: BorderStroke?,
    interactionSource: MutableInteractionSource?,
    content: @Composable () -> Unit
) {

    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    SurfaceIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        shape = shapeForInteraction(checked, shapes, interactionSource),
        colors = colors,
        border = border,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Represents the container and content colors used in an icon button in different states.
 *
 * @param containerColor the container color of this icon button when enabled.
 * @param contentColor the content color of this icon button when enabled.
 * @param disabledContainerColor the container color of this icon button when not enabled.
 * @param disabledContentColor the content color of this icon button when not enabled.
 * @constructor create an instance with arbitrary colors.
 * - See [IconButtonDefaults.filledIconButtonColors] and
 *   [IconButtonDefaults.filledTonalIconButtonColors] for the default colors used in a
 *   [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconButtonVibrantColors] for the default colors used in an
 *   [OutlinedIconButton].
 */
@Immutable
class IconButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {

    /**
     * Returns a copy of this IconButtonColors, optionally overriding some of the values. This uses
     * the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
    ) =
        IconButtonColors(
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
 * Represents the container and content colors used in a toggleable icon button in different states.
 *
 * @param containerColor the container color of this icon button when enabled.
 * @param contentColor the content color of this icon button when enabled.
 * @param disabledContainerColor the container color of this icon button when not enabled.
 * @param disabledContentColor the content color of this icon button when not enabled.
 * @param checkedContainerColor the container color of this icon button when checked.
 * @param checkedContentColor the content color of this icon button when checked.
 * @constructor create an instance with arbitrary colors.
 * - See [IconButtonDefaults.filledIconToggleButtonColors] and
 *   [IconButtonDefaults.filledTonalIconToggleButtonColors] for the default colors used in a
 *   [FilledIconButton].
 * - See [IconButtonDefaults.outlinedIconToggleButtonVibrantColors] for the default colors used in a
 *   toggleable [OutlinedIconButton].
 */
@Immutable
class IconToggleButtonColors(
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
    val checkedContainerColor: Color,
    val checkedContentColor: Color,
) {

    /**
     * Returns a copy of this IconToggleButtonColors, optionally overriding some of the values. This
     * uses the Color.Unspecified to mean “use the value from the source”
     */
    fun copy(
        containerColor: Color = this.containerColor,
        contentColor: Color = this.contentColor,
        disabledContainerColor: Color = this.disabledContainerColor,
        disabledContentColor: Color = this.disabledContentColor,
        checkedContainerColor: Color = this.checkedContainerColor,
        checkedContentColor: Color = this.checkedContentColor
    ) =
        IconToggleButtonColors(
            containerColor.takeOrElse { this.containerColor },
            contentColor.takeOrElse { this.contentColor },
            disabledContainerColor.takeOrElse { this.disabledContainerColor },
            disabledContentColor.takeOrElse { this.disabledContentColor },
            checkedContainerColor.takeOrElse { this.checkedContainerColor },
            checkedContentColor.takeOrElse { this.checkedContentColor }
        )

    /**
     * Represents the container color for this icon button, depending on [enabled] and [checked].
     *
     * @param enabled whether the icon button is enabled
     * @param checked whether the icon button is checked
     */
    @Composable
    internal fun containerColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target =
            when {
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
        val target =
            when {
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

/**
 * The shapes that will be used in icon buttons. Icon button will morph between these shapes
 * depending on the interaction of the icon button, assuming all of the shapes are
 * [CornerBasedShape]s.
 *
 * @property shape is the unchecked shape.
 * @property pressedShape is the pressed shape.
 */
@ExperimentalMaterial3ExpressiveApi
class IconButtonShapes(val shape: Shape, val pressedShape: Shape = shape) {

    /** Returns a copy of this IconButtonShapes, optionally overriding some of the values. */
    fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
    ) =
        IconButtonShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()

        return result
    }
}

/**
 * The shapes that will be used in toggle buttons. Toggle button will morph between these three
 * shapes depending on the interaction of the toggle button, assuming all of the shapes are
 * [CornerBasedShape]s.
 *
 * @property shape is the unchecked shape.
 * @property pressedShape is the pressed shape.
 * @property checkedShape is the checked shape.
 */
@ExperimentalMaterial3ExpressiveApi
class IconToggleButtonShapes(
    val shape: Shape,
    val pressedShape: Shape = shape,
    val checkedShape: Shape = shape
) {

    /** Returns a copy of this IconButtonShapes, optionally overriding some of the values. */
    fun copy(
        shape: Shape? = this.shape,
        pressedShape: Shape? = this.pressedShape,
        checkedShape: Shape? = this.checkedShape
    ) =
        IconToggleButtonShapes(
            shape = shape.takeOrElse { this.shape },
            pressedShape = pressedShape.takeOrElse { this.pressedShape },
            checkedShape = checkedShape.takeOrElse { this.checkedShape }
        )

    internal fun Shape?.takeOrElse(block: () -> Shape): Shape = this ?: block()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is IconToggleButtonShapes) return false

        if (shape != other.shape) return false
        if (pressedShape != other.pressedShape) return false
        if (checkedShape != other.checkedShape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + pressedShape.hashCode()
        result = 31 * result + checkedShape.hashCode()

        return result
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun shapeForInteraction(
    shapes: IconButtonShapes,
    interactionSource: MutableInteractionSource,
): Shape {
    if (shapes.isStatic) {
        return shapes.shape
    }
    // TODO Load the motionScheme tokens from the component tokens file
    // MotionSchemeKeyTokens.DefaultEffects is intentional here to prevent
    // any bounce in this component.
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
    val pressed by interactionSource.collectIsPressedAsState()

    return shapeByInteraction(shapes, pressed, defaultAnimationSpec)
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun shapeForInteraction(
    checked: Boolean,
    shapes: IconToggleButtonShapes,
    interactionSource: MutableInteractionSource,
): Shape {
    if (shapes.isStatic) {
        return shapes.shape
    }
    // TODO Load the motionScheme tokens from the component tokens file
    // MotionSchemeKeyTokens.DefaultEffects is intentional here to prevent
    // any bounce in this component.
    val defaultAnimationSpec = MotionSchemeKeyTokens.DefaultEffects.value<Float>()
    val pressed by interactionSource.collectIsPressedAsState()

    return shapeByInteraction(shapes, pressed, checked, defaultAnimationSpec)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val IconButtonShapes.isCornerBasedShape: Boolean
    get() = shape is RoundedCornerShape && pressedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val IconButtonShapes.isStatic: Boolean
    get() = shape === pressedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val IconToggleButtonShapes.isCornerBasedShape: Boolean
    get() =
        shape is RoundedCornerShape &&
            pressedShape is CornerBasedShape &&
            checkedShape is CornerBasedShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal val IconToggleButtonShapes.isStatic: Boolean
    get() = shape === pressedShape && shape === checkedShape

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun shapeByInteraction(
    shapes: IconButtonShapes,
    pressed: Boolean,
    animationSpec: FiniteAnimationSpec<Float>
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape
        } else shapes.shape

    if (shapes.isCornerBasedShape) {
        return key(shapes) { rememberAnimatedShape(shape as RoundedCornerShape, animationSpec) }
    }
    return shape
}

@ExperimentalMaterial3ExpressiveApi
@Composable
private fun shapeByInteraction(
    shapes: IconToggleButtonShapes,
    pressed: Boolean,
    checked: Boolean,
    animationSpec: FiniteAnimationSpec<Float>
): Shape {
    val shape =
        if (pressed) {
            shapes.pressedShape
        } else if (checked) {
            shapes.checkedShape
        } else shapes.shape

    if (shapes.isCornerBasedShape) {
        return key(shapes) { rememberAnimatedShape(shape as RoundedCornerShape, animationSpec) }
    }
    return shape
}
