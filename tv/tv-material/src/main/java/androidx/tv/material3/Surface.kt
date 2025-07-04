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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.tokens.Elevation

/**
 * The [Surface] is a building block component that will be used for any element on TV such as
 * buttons, cards, navigation, or a simple background etc. This non-interactive Surface is similar
 * to Compose Material's Surface composable
 *
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 *   in a darker color in light theme and lighter color in dark theme.
 * @param shape Defines the surface's shape.
 * @param colors Defines the background & content color to be used in this Surface. See
 *   [SurfaceDefaults.colors].
 * @param border Defines a border around the Surface.
 * @param glow Diffused shadow to be shown behind the Surface. Note that glow is disabled for API
 *   levels below 28 as it is not supported by the underlying OS
 * @param content defines the [Composable] content inside the surface
 */
@NonRestartableComposable
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    shape: Shape = SurfaceDefaults.shape,
    colors: SurfaceColors = SurfaceDefaults.colors(),
    border: Border = SurfaceDefaults.border,
    glow: Glow = SurfaceDefaults.glow,
    content: @Composable (BoxScope.() -> Unit),
) {
    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation

    CompositionLocalProvider(
        LocalContentColor provides colors.contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation,
    ) {
        val backgroundColorByState =
            calculateSurfaceColorAtElevation(
                color = colors.containerColor,
                elevation = LocalAbsoluteTonalElevation.current,
            )

        Box(
            modifier =
                modifier
                    .ifElse(API_28_OR_ABOVE, Modifier.tvSurfaceGlow(shape, glow))
                    .ifElse(border != Border.None, Modifier.tvSurfaceBorder(shape, border))
                    .background(backgroundColorByState, shape)
                    .clip(shape),
            propagateMinConstraints = true,
            content = content,
        )
    }
}

/**
 * The [Surface] is a building block component that will be used for any focusable element on TV
 * such as buttons, cards, navigation, etc. This clickable Surface is similar to Compose Material's
 * Surface composable but will have more functionality that will make focus management easier.
 * [Surface] will automatically apply the relevant modifier(s) based on the current interaction
 * state.
 *
 * @param onClick callback to be called when the surface is clicked. Note: DPad Enter button won't
 *   work if this value is null
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param onLongClick callback to be called when the surface is long clicked (long-pressed).
 * @param enabled Controls the enabled state of the surface. When `false`, this Surface will not be
 *   clickable. A disabled surface will still be focusable (reason:
 *   https://issuetracker.google.com/302955429). If you still want it to not be focusable, consider
 *   using the Non-interactive variant of the Surface.
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 *   in a darker color in light theme and lighter color in dark theme.
 * @param shape Defines the surface's shape.
 * @param colors Defines the background & content colors to be used in this surface for different
 *   interaction states. See [ClickableSurfaceDefaults.colors].
 * @param scale Defines size of the Surface relative to its original size.
 * @param border Defines a border around the Surface.
 * @param glow Diffused shadow to be shown behind the Surface. Note that glow is disabled for API
 *   levels below 28 as it is not supported by the underlying OS
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this surface. You can use this to change the surface's appearance
 *   or preview the surface in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content defines the [Composable] content inside the surface
 */
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    tonalElevation: Dp = 0.dp,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(),
    colors: ClickableSurfaceColors = ClickableSurfaceDefaults.colors(),
    scale: ClickableSurfaceScale = ClickableSurfaceDefaults.scale(),
    border: ClickableSurfaceBorder = ClickableSurfaceDefaults.border(),
    glow: ClickableSurfaceGlow = ClickableSurfaceDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (BoxScope.() -> Unit),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    SurfaceImpl(
        modifier =
            modifier.tvClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
            ),
        selected = false,
        enabled = enabled,
        tonalElevation = tonalElevation,
        shape = shape.currentShape(enabled = enabled, focused = focused, pressed = pressed),
        color =
            colors.currentContainerColor(enabled = enabled, focused = focused, pressed = pressed),
        contentColor =
            colors.currentContentColor(enabled = enabled, focused = focused, pressed = pressed),
        scale = scale.currentScale(enabled = enabled, focused = focused, pressed = pressed),
        border = border.currentBorder(enabled = enabled, focused = focused, pressed = pressed),
        glow = glow.currentGlow(enabled = enabled, focused = focused, pressed = pressed),
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * The Surface is a building block component that will be used for any focusable element on TV such
 * as buttons, cards, navigation, etc.
 *
 * This version of Surface is responsible for a toggling its selected state as well as everything
 * else that a regular Surface does:
 *
 * This version of surface will react to the select toggles, calling [onClick] lambda, updating the
 * [interactionSource] when [PressInteraction] occurs.
 *
 * To manually retrieve the content color inside a surface, use [LocalContentColor].
 *
 * @param selected whether or not this Surface is selected
 * @param onClick callback to be invoked when the selectable Surface is clicked.
 * @param modifier [Modifier] to be applied to the layout corresponding to the surface
 * @param onLongClick callback to be called when the selectable surface is long clicked
 *   (long-pressed).
 * @param enabled Controls the enabled state of the surface. When `false`, this Surface will not be
 *   clickable. A disabled surface will still be focusable (reason:
 *   https://issuetracker.google.com/302955429). If you still want it to not be focusable, consider
 *   using the Non-interactive variant of the Surface.
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 *   in a darker color in light theme and lighter color in dark theme.
 * @param shape Defines the surface's shape.
 * @param colors Defines the background & content colors to be used in this surface for different
 *   interaction states. See [SelectableSurfaceDefaults.colors].
 * @param scale Defines size of the Surface relative to its original size.
 * @param border Defines a border around the Surface.
 * @param glow Diffused shadow to be shown behind the Surface. Note that glow is disabled for API
 *   levels below 28 as it is not supported by the underlying OS
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this surface. You can use this to change the surface's appearance
 *   or preview the surface in different states. Note that if `null` is provided, interactions will
 *   still happen internally.
 * @param content defines the [Composable] content inside the surface
 */
@Composable
fun Surface(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    tonalElevation: Dp = Elevation.Level0,
    shape: SelectableSurfaceShape = SelectableSurfaceDefaults.shape(),
    colors: SelectableSurfaceColors = SelectableSurfaceDefaults.colors(),
    scale: SelectableSurfaceScale = SelectableSurfaceDefaults.scale(),
    border: SelectableSurfaceBorder = SelectableSurfaceDefaults.border(),
    glow: SelectableSurfaceGlow = SelectableSurfaceDefaults.glow(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (BoxScope.() -> Unit),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    SurfaceImpl(
        modifier =
            modifier.tvSelectable(
                enabled = enabled,
                selected = selected,
                onClick = onClick,
                interactionSource = interactionSource,
                onLongClick = onLongClick,
            ),
        selected = selected,
        enabled = enabled,
        tonalElevation = tonalElevation,
        shape =
            shape.currentShape(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        color =
            colors.currentContainerColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        contentColor =
            colors.currentContentColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        scale =
            scale.currentScale(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        border =
            border.currentBorder(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        glow =
            glow.currentGlow(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
            ),
        interactionSource = interactionSource,
        content = content,
    )
}
