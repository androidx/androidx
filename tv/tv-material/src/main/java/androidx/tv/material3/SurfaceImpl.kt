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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
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
): Modifier =
    this.then(
        HandleDPadEnterElement(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
        )
    )

private class HandleDPadEnterElement(
    private val enabled: Boolean,
    private val interactionSource: MutableInteractionSource,
    private val onClick: (() -> Unit)?,
    private val onLongClick: (() -> Unit)?,
    private val selected: Boolean,
) : ModifierNodeElement<HandleDPadEnterNode>() {
    override fun create(): HandleDPadEnterNode =
        HandleDPadEnterNode(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
        )

    override fun update(node: HandleDPadEnterNode) {
        node.update(
            enabled = enabled,
            interactionSource = interactionSource,
            onClick = onClick,
            onLongClick = onLongClick,
            selected = selected,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "handleDPadEnter"
        properties["enabled"] = enabled
        properties["interactionSource"] = interactionSource
        properties["onClick"] = onClick
        properties["onLongClick"] = onLongClick
        properties["selected"] = selected
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HandleDPadEnterElement) return false
        if (enabled != other.enabled) return false
        if (interactionSource != other.interactionSource) return false
        if (onClick !== other.onClick) return false
        if (onLongClick !== other.onLongClick) return false
        if (selected != other.selected) return false
        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + interactionSource.hashCode()
        result = 31 * result + (onClick?.hashCode() ?: 0)
        result = 31 * result + (onLongClick?.hashCode() ?: 0)
        result = 31 * result + selected.hashCode()
        return result
    }
}

private class HandleDPadEnterNode(
    private var enabled: Boolean,
    private var interactionSource: MutableInteractionSource,
    private var onClick: (() -> Unit)?,
    private var onLongClick: (() -> Unit)?,
    private var selected: Boolean,
) : Modifier.Node(), FocusEventModifierNode, KeyInputModifierNode {
    private val pressInteraction = PressInteraction.Press(Offset.Zero)
    private var isLongClick = false
    private var isPressed = false
    private var collectJob: Job? = null

    override fun onAttach() {
        if (enabled) {
            startCollectingPressed()
        }
    }

    override fun onDetach() {
        collectJob?.cancel()
        collectJob = null
        isPressed = false
        isLongClick = false
    }

    fun update(
        enabled: Boolean,
        interactionSource: MutableInteractionSource,
        onClick: (() -> Unit)?,
        onLongClick: (() -> Unit)?,
        selected: Boolean,
    ) {
        val enabledChanged = this.enabled != enabled
        val interactionSourceChanged = this.interactionSource != interactionSource
        this.enabled = enabled
        this.interactionSource = interactionSource
        this.onClick = onClick
        this.onLongClick = onLongClick
        this.selected = selected

        if (enabledChanged || interactionSourceChanged) {
            collectJob?.cancel()
            collectJob = null
            isPressed = false
            isLongClick = false
            if (isAttached && enabled) {
                startCollectingPressed()
            }
        }
    }

    private fun startCollectingPressed() {
        val currentInteractionSource = interactionSource
        collectJob = coroutineScope.launch {
            val pressInteractions = mutableListOf<PressInteraction.Press>()
            currentInteractionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressInteractions.add(interaction)
                    is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                    is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                }
                isPressed = pressInteractions.isNotEmpty()
            }
        }
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (!enabled) return
        if (!focusState.isFocused && isPressed) {
            val currentInteractionSource = interactionSource
            coroutineScope.launch {
                currentInteractionSource.emit(PressInteraction.Release(pressInteraction))
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return KeyEventPropagation.ContinuePropagation
        if (AcceptableEnterClickKeys.contains(event.nativeKeyEvent.keyCode)) {
            val currentInteractionSource = interactionSource
            when (event.nativeKeyEvent.action) {
                NativeKeyEvent.ACTION_DOWN -> {
                    when (event.nativeKeyEvent.repeatCount) {
                        0 ->
                            coroutineScope.launch {
                                currentInteractionSource.emit(pressInteraction)
                            }
                        1 ->
                            onLongClick?.let {
                                isLongClick = true
                                coroutineScope.launch {
                                    currentInteractionSource.emit(
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
                            currentInteractionSource.emit(
                                PressInteraction.Release(pressInteraction)
                            )
                        }
                        onClick?.invoke()
                    } else {
                        isLongClick = false
                    }
                }
            }
            return KeyEventPropagation.StopPropagation
        }
        return KeyEventPropagation.ContinuePropagation
    }

    override fun onPreKeyEvent(event: KeyEvent): Boolean = false
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
