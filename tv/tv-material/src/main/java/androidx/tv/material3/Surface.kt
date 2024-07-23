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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.tv.material3.tokens.Elevation
import kotlinx.coroutines.launch

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
    content: @Composable (BoxScope.() -> Unit)
) {
    SurfaceImpl(
        modifier = modifier,
        selected = false,
        enabled = true,
        tonalElevation = tonalElevation,
        shape = shape,
        color = colors.containerColor,
        contentColor = colors.contentColor,
        scale = 1.0f,
        border = border,
        glow = glow,
        content = content
    )
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
    content: @Composable (BoxScope.() -> Unit)
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
        shape =
            ClickableSurfaceDefaults.shape(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                shape = shape
            ),
        color =
            ClickableSurfaceDefaults.containerColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                colors = colors
            ),
        contentColor =
            ClickableSurfaceDefaults.contentColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                colors = colors
            ),
        scale =
            ClickableSurfaceDefaults.scale(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                scale = scale
            ),
        border =
            ClickableSurfaceDefaults.border(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                border = border
            ),
        glow =
            ClickableSurfaceDefaults.glow(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                glow = glow
            ),
        interactionSource = interactionSource,
        content = content
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
    content: @Composable (BoxScope.() -> Unit)
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
                onLongClick = onLongClick
            ),
        selected = selected,
        enabled = enabled,
        tonalElevation = tonalElevation,
        shape =
            SelectableSurfaceDefaults.shape(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                shape = shape
            ),
        color =
            SelectableSurfaceDefaults.containerColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                colors = colors
            ),
        contentColor =
            SelectableSurfaceDefaults.contentColor(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                colors = colors
            ),
        scale =
            SelectableSurfaceDefaults.scale(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                scale = scale
            ),
        border =
            SelectableSurfaceDefaults.border(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                border = border
            ),
        glow =
            SelectableSurfaceDefaults.glow(
                enabled = enabled,
                focused = focused,
                pressed = pressed,
                selected = selected,
                glow = glow
            ),
        interactionSource = interactionSource,
        content = content
    )
}

@Composable
private fun SurfaceImpl(
    modifier: Modifier,
    selected: Boolean,
    enabled: Boolean,
    shape: Shape,
    color: Color,
    contentColor: Color,
    scale: Float,
    border: Border,
    glow: Glow,
    tonalElevation: Dp,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (BoxScope.() -> Unit)
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val surfaceAlpha =
        stateAlpha(enabled = enabled, focused = focused, pressed = pressed, selected = selected)

    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation
    ) {
        val zIndex by
            animateFloatAsState(
                targetValue = if (focused) FocusedZIndex else NonFocusedZIndex,
                label = "zIndex"
            )

        val backgroundColorByState =
            surfaceColorAtElevation(color = color, elevation = LocalAbsoluteTonalElevation.current)

        Box(
            modifier =
                modifier
                    .tvSurfaceScale(
                        scale = scale,
                        interactionSource = interactionSource,
                    )
                    .ifElse(API_28_OR_ABOVE, Modifier.tvSurfaceGlow(shape, glow))
                    // Increasing the zIndex of this Surface when it is in the focused state to
                    // avoid the glowIndication from being overlapped by subsequent items if
                    // this Surface is inside a list composable (like a Row/Column).
                    .zIndex(zIndex)
                    .ifElse(border != Border.None, Modifier.tvSurfaceBorder(shape, border))
                    .background(backgroundColorByState, shape)
                    .graphicsLayer {
                        this.alpha = surfaceAlpha
                        this.shape = shape
                        this.clip = true
                        this.compositingStrategy = CompositingStrategy.Offscreen
                    },
            propagateMinConstraints = true
        ) {
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        this.alpha = if (!enabled) DisabledContentAlpha else EnabledContentAlpha
                    },
                content = content
            )
        }
    }
}

/**
 * This modifier handles click, press, and focus events for a TV composable.
 *
 * @param enabled decides whether [onClick] is executed
 * @param onClick executes the provided lambda on click.
 * @param onLongClick executes the provided lambda on long press.
 * @param interactionSource used to emit [PressInteraction] events
 */
private fun Modifier.tvClickable(
    enabled: Boolean,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource
) =
    handleDPadEnter(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick
        )
        // We are not using "clickable" modifier here because if we set "enabled" to false
        // then the Surface won't be focusable as well. But, in TV use case, a disabled surface
        // should be focusable
        .focusable(interactionSource = interactionSource)
        .semantics(mergeDescendants = true) {
            onClick {
                onClick?.let { nnOnClick ->
                    nnOnClick()
                    return@onClick true
                }
                false
            }
            onLongClick {
                onLongClick?.let { nnOnLongClick ->
                    nnOnLongClick()
                    return@onLongClick true
                }
                false
            }
            if (!enabled) {
                disabled()
            }
        }

/**
 * This modifier handles click, press, and focus events for a TV composable.
 *
 * @param enabled decides whether [onClick] is executed
 * @param selected differentiates whether the current item is selected or unselected
 * @param onClick executes the provided lambda on click, while returning the inverse state of
 *   [selected].
 * @param onLongClick executes the provided lambda on long press.
 * @param interactionSource used to emit [PressInteraction] events
 */
private fun Modifier.tvSelectable(
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    interactionSource: MutableInteractionSource,
) =
    handleDPadEnter(
            enabled = enabled,
            interactionSource = interactionSource,
            selected = selected,
            onClick = onClick,
            onLongClick = onLongClick
        )
        // We are not using "selectable" modifier here because if we set "enabled" to false
        // then the Surface won't be focusable as well. But, in TV use case, a disabled surface
        // should be focusable
        .focusable(interactionSource = interactionSource)
        .semantics(mergeDescendants = true) {
            this.selected = selected
            onClick {
                onClick()
                true
            }
            onLongClick {
                onLongClick?.let { nnOnLongClick ->
                    nnOnLongClick()
                    return@onLongClick true
                }
                false
            }
            if (!enabled) {
                disabled()
            }
        }

/**
 * This modifier is used to perform some actions when the user clicks the D-PAD enter button
 *
 * @param enabled if this is false, the D-PAD enter event is ignored
 * @param interactionSource used to emit [PressInteraction] events
 * @param onClick this lambda will be triggered on D-PAD enter event
 * @param onLongClick this lambda will be triggered when D-PAD enter is long pressed.
 * @param selected differentiates whether the current item is selected or unselected
 */
private fun Modifier.handleDPadEnter(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
) =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "handleDPadEnter"
                properties["enabled"] = enabled
                properties["interactionSource"] = interactionSource
                properties["onClick"] = onClick
                properties["onLongClick"] = onLongClick
                properties["selected"] = selected
            }
    ) {
        if (!enabled) return@composed this

        val coroutineScope = rememberCoroutineScope()
        val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
        var isLongClick by remember { mutableStateOf(false) }
        val isPressed by interactionSource.collectIsPressedAsState()

        this.onFocusChanged {
                if (!it.isFocused && isPressed) {
                    coroutineScope.launch {
                        interactionSource.emit(PressInteraction.Release(pressInteraction))
                    }
                }
            }
            .onKeyEvent { keyEvent ->
                if (AcceptableKeys.contains(keyEvent.nativeKeyEvent.keyCode)) {
                    when (keyEvent.nativeKeyEvent.action) {
                        NativeKeyEvent.ACTION_DOWN -> {
                            when (keyEvent.nativeKeyEvent.repeatCount) {
                                0 ->
                                    coroutineScope.launch {
                                        interactionSource.emit(pressInteraction)
                                    }
                                1 ->
                                    onLongClick?.let {
                                        isLongClick = true
                                        coroutineScope.launch {
                                            interactionSource.emit(
                                                PressInteraction.Release(pressInteraction)
                                            )
                                        }
                                        it.invoke()
                                    }
                            }
                        }
                        NativeKeyEvent.ACTION_UP -> {
                            if (!isLongClick) {
                                coroutineScope.launch {
                                    interactionSource.emit(
                                        PressInteraction.Release(pressInteraction)
                                    )
                                }
                                onClick?.invoke()
                            } else isLongClick = false
                        }
                    }
                    return@onKeyEvent KeyEventPropagation.StopPropagation
                }
                KeyEventPropagation.ContinuePropagation
            }
    }

@Composable
internal fun surfaceColorAtElevation(color: Color, elevation: Dp): Color {
    return if (color == MaterialTheme.colorScheme.surface) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    } else {
        color
    }
}

/**
 * Returns the alpha value for Surface's background based on its current indication state. The value
 * ranges between 0f and 1f.
 */
private fun stateAlpha(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean
): Float {
    return when {
        !enabled && pressed -> DisabledPressedStateAlpha
        !enabled && focused -> DisabledFocusedStateAlpha
        !enabled && selected -> DisabledSelectedStateAlpha
        enabled -> EnabledContentAlpha
        else -> DisabledDefaultStateAlpha
    }
}

private const val DisabledPressedStateAlpha = 0.8f
private const val DisabledFocusedStateAlpha = 0.8f
private const val DisabledSelectedStateAlpha = 0.8f
private const val DisabledDefaultStateAlpha = 0.6f

private const val FocusedZIndex = 0.5f
private const val NonFocusedZIndex = 0f

private const val DisabledContentAlpha = 0.8f
internal const val EnabledContentAlpha = 1f

/**
 * CompositionLocal containing the current absolute elevation provided by Surface components. This
 * absolute elevation is a sum of all the previous elevations. Absolute elevation is only used for
 * calculating surface tonal colors, and is *not* used for drawing the shadow in a [SurfaceImpl].
 */
internal val LocalAbsoluteTonalElevation = compositionLocalOf { 0.dp }

private val AcceptableKeys =
    intArrayOf(
        NativeKeyEvent.KEYCODE_DPAD_CENTER,
        NativeKeyEvent.KEYCODE_ENTER,
        NativeKeyEvent.KEYCODE_NUMPAD_ENTER
    )
