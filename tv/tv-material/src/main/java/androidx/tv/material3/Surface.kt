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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * The [Surface] is a building block component that will be used for any focusable
 * element on TV such as buttons, cards, navigation, etc. This clickable Surface is similar to
 * Compose Material's Surface composable but will have more functionality that will make focus
 * management easier. [Surface] will automatically apply the relevant modifier(s) based on
 * the current interaction state.
 *
 * @param onClick callback to be called when the surface is clicked. Note: DPad Enter button won't
 * work if this value is null
 * @param modifier Modifier to be applied to the layout corresponding to the surface
 * @param enabled Controls the enabled state of the surface. When `false`, this Surface will not be
 * clickable or focusable.
 * @param tonalElevation When [color] is [ColorScheme.surface], a higher the elevation will result
 * in a darker color in light theme and lighter color in dark theme.
 * @param shadowElevation The size of the shadow below the surface. Note that It will not affect z
 * index of the Surface. If you want to change the drawing order you can use `Modifier.zIndex`.
 * @param role The type of user interface element. Accessibility services might use this to describe
 * the element or do customizations.
 * @param shape Defines the surface's shape.
 * @param color Color to be used on background of the Surface
 * @param contentColor The preferred content color provided by this Surface to its children.
 * @param border Optional border to draw on top of the surface
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this Surface. You can create and pass in your own remembered [MutableInteractionSource] if
 * you want to observe [Interaction]s and customize the appearance / behavior of this Surface in
 * different [Interaction]s.
 * @param content defines the [Composable] content inside the surface
 */
@ExperimentalTvMaterial3Api
@NonRestartableComposable
@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: ClickableSurfaceShape = ClickableSurfaceDefaults.shape(),
    color: ClickableSurfaceColor = ClickableSurfaceDefaults.color(),
    contentColor: ClickableSurfaceColor = ClickableSurfaceDefaults.contentColor(),
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    role: Role? = null,
    shadowElevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    SurfaceImpl(
        modifier = modifier.tvClickable(
            enabled = enabled,
            onClick = onClick,
            interactionSource = interactionSource,
            role = role
        ),
        shape = ClickableSurfaceDefaults.shape(
            enabled = enabled,
            focused = focused,
            pressed = pressed,
            shape = shape
        ),
        color = ClickableSurfaceDefaults.color(
            enabled = enabled,
            focused = focused,
            pressed = pressed,
            color = color
        ),
        contentColor = ClickableSurfaceDefaults.color(
            enabled = enabled,
            focused = focused,
            pressed = pressed,
            color = contentColor
        ),
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        content = content
    )
}

@ExperimentalTvMaterial3Api
@Composable
private fun SurfaceImpl(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation
    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation
    ) {
        Box(
            modifier = modifier
                .surface(
                    shape = shape,
                    backgroundColor = surfaceColorAtElevation(
                        color = color,
                        elevation = absoluteElevation
                    ),
                    border = border,
                    shadowElevation = shadowElevation
                ),
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}

private fun Modifier.surface(
    shape: Shape,
    backgroundColor: Color,
    border: BorderStroke?,
    shadowElevation: Dp
) = this
    .shadow(shadowElevation, shape, clip = false)
    .then(if (border != null) Modifier.border(border, shape) else Modifier)
    .background(color = backgroundColor, shape = shape)
    .clip(shape)

/**
 * This modifier handles click, press, and focus events for a TV composable.
 * @param enabled decides whether [onClick] or [onValueChanged] is executed
 * @param onClick executes the provided lambda
 * @param value differentiates whether the current item is selected or unselected
 * @param onValueChanged executes the provided lambda while returning the inverse state of [value]
 * @param interactionSource used to emit [PressInteraction] events
 * @param role used to define this composable's semantic role (for Accessibility purposes)
 */
private fun Modifier.tvClickable(
    enabled: Boolean,
    onClick: (() -> Unit)? = null,
    value: Boolean = false,
    onValueChanged: ((Boolean) -> Unit)? = null,
    interactionSource: MutableInteractionSource,
    role: Role?
) = this
    .handleDPadEnter(
        enabled = enabled,
        interactionSource = interactionSource,
        onClick = onClick,
        value = value,
        onValueChanged = onValueChanged
    )
    .focusable(interactionSource = interactionSource)
    .semantics(mergeDescendants = true) {
        onClick {
            onClick?.let { nnOnClick ->
                nnOnClick()
                return@onClick true
            } ?: onValueChanged?.let { nnOnValueChanged ->
                nnOnValueChanged(!value)
                return@onClick true
            }
            false
        }
        role?.let { nnRole -> this.role = nnRole }
        if (!enabled) {
            disabled()
        }
    }

private fun Modifier.handleDPadEnter(
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: (() -> Unit)?,
    value: Boolean,
    onValueChanged: ((Boolean) -> Unit)?
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "handleDPadEnter"
        properties["enabled"] = enabled
        properties["interactionSource"] = interactionSource
        properties["onClick"] = onClick
        properties["onValueChanged"] = onValueChanged
        properties["value"] = value
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val pressInteraction = remember { PressInteraction.Press(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    this.then(
        onKeyEvent { keyEvent ->
            if (AcceptableKeys.any { keyEvent.nativeKeyEvent.keyCode == it } && enabled) {
                when (keyEvent.nativeKeyEvent.action) {
                    NativeKeyEvent.ACTION_DOWN -> {
                        if (!isPressed) {
                            isPressed = true
                            coroutineScope.launch {
                                interactionSource.emit(pressInteraction)
                            }
                        }
                    }

                    NativeKeyEvent.ACTION_UP -> {
                        if (isPressed) {
                            isPressed = false
                            coroutineScope.launch {
                                interactionSource.emit(PressInteraction.Release(pressInteraction))
                            }
                            onClick?.invoke()
                            onValueChanged?.invoke(!value)
                        }
                    }
                }
                return@onKeyEvent KeyEventPropagation.StopPropagation
            }
            KeyEventPropagation.ContinuePropagation
        }
    )
}

@Composable
@ExperimentalTvMaterial3Api
private fun surfaceColorAtElevation(color: Color, elevation: Dp): Color {
    return if (color == MaterialTheme.colorScheme.surface) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    } else {
        color
    }
}

/**
 * CompositionLocal containing the current absolute elevation provided by Surface components. This
 * absolute elevation is a sum of all the previous elevations. Absolute elevation is only used for
 * calculating surface tonal colors, and is *not* used for drawing the shadow in a [SurfaceImpl].
 */
val LocalAbsoluteTonalElevation = compositionLocalOf { 0.dp }
private val AcceptableKeys = listOf(
    NativeKeyEvent.KEYCODE_DPAD_CENTER,
    NativeKeyEvent.KEYCODE_ENTER,
    NativeKeyEvent.KEYCODE_NUMPAD_ENTER
)
