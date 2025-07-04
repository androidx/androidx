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

package androidx.tv.material3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
internal fun SurfaceImpl(
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
    content: @Composable (BoxScope.() -> Unit),
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val surfaceAlpha =
        calculateAlphaBasedOnState(
            enabled = enabled,
            focused = focused,
            pressed = pressed,
            selected = selected,
        )

    val absoluteElevation = LocalAbsoluteTonalElevation.current + tonalElevation

    CompositionLocalProvider(
        LocalContentColor provides contentColor,
        LocalAbsoluteTonalElevation provides absoluteElevation,
    ) {
        val zIndex by
            animateFloatAsState(
                targetValue = if (focused) FocusedZIndex else NonFocusedZIndex,
                label = "zIndex",
            )

        val backgroundColorByState =
            calculateSurfaceColorAtElevation(
                color = color,
                elevation = LocalAbsoluteTonalElevation.current,
            )

        Box(
            modifier =
                modifier
                    .tvSurfaceScale(scale = scale, interactionSource = interactionSource)
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
                    },
            propagateMinConstraints = true,
        ) {
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        this.alpha = if (!enabled) DisabledContentAlpha else EnabledContentAlpha
                    },
                content = content,
            )
        }
    }
}

internal fun Modifier.handleDPadEnter(
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
                if (AcceptableEnterClickKeys.contains(keyEvent.nativeKeyEvent.keyCode)) {
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

private fun calculateAlphaBasedOnState(
    enabled: Boolean,
    focused: Boolean,
    pressed: Boolean,
    selected: Boolean,
): Float {
    return when {
        !enabled && pressed -> DisabledPressedStateAlpha
        !enabled && focused -> DisabledFocusedStateAlpha
        !enabled && selected -> DisabledSelectedStateAlpha
        enabled -> EnabledContentAlpha
        else -> DisabledDefaultStateAlpha
    }
}

@Composable
internal fun calculateSurfaceColorAtElevation(color: Color, elevation: Dp): Color {
    return if (color == MaterialTheme.colorScheme.surface) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(elevation)
    } else {
        color
    }
}

private const val DisabledPressedStateAlpha = 0.8f
private const val DisabledFocusedStateAlpha = 0.8f
private const val DisabledSelectedStateAlpha = 0.8f
private const val DisabledDefaultStateAlpha = 0.6f

private const val FocusedZIndex = 0.5f
private const val NonFocusedZIndex = 0f

private const val DisabledContentAlpha = 0.8f
private const val EnabledContentAlpha = 1f

private val AcceptableEnterClickKeys =
    intArrayOf(
        NativeKeyEvent.KEYCODE_DPAD_CENTER,
        NativeKeyEvent.KEYCODE_ENTER,
        NativeKeyEvent.KEYCODE_NUMPAD_ENTER,
    )

/**
 * CompositionLocal containing the current absolute elevation provided by Surface components. This
 * absolute elevation is a sum of all the previous elevations. Absolute elevation is only used for
 * calculating surface tonal colors, and is *not* used for drawing the shadow in a [SurfaceImpl].
 */
internal val LocalAbsoluteTonalElevation = compositionLocalOf { 0.dp }
