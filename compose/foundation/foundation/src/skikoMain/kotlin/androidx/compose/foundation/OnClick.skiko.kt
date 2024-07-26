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

package androidx.compose.foundation

import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Configure component to receive clicks, double clicks and long clicks via input only (no accessibility "click" event)
 * within the component's bounds.
 *
 * It allows configuration based on a pointer type via [matcher].
 * By default, matcher uses [PointerMatcher.Primary].
 * [matcher] should declare supported pointer types (mouse, touch, stylus, eraser) by listing them and
 * declaring required properties for them, such as: required button (primary, secondary, etc.).
 *
 * Consider using [clickable] if it's necessary to handle only primary clicks. Unlike [clickable],
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], [Modifier.focusable], click by Enter key, etc.
 * If necessary, one has to add those manually when using [onClick].
 *
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param matcher defines supported pointer types and required properties
 * @param keyboardModifiers defines a condition that [PointerEvent.keyboardModifiers] has to match
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
@ExperimentalFoundationApi
fun Modifier.onClick(
    enabled: Boolean = true,
    matcher: PointerMatcher = PointerMatcher.Primary,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed {
    Modifier.onClick(
        enabled = enabled,
        matcher = matcher,
        keyboardModifiers = keyboardModifiers,
        interactionSource = remember { MutableInteractionSource() },
        onDoubleClick = onDoubleClick,
        onLongClick = onLongClick,
        onClick = onClick
    )
}

/**
 * Configure component to receive clicks, double clicks and long clicks via input only (no accessibility "click" event)
 * within the component's bounds.
 *
 * It allows configuration based on a pointer type via [matcher].
 * By default, matcher uses [PointerMatcher.Primary].
 * [matcher] should declare supported pointer types (mouse, touch, stylus, eraser) by listing them and
 * declaring required properties for them, such as: required button (primary, secondary, etc.).
 *
 * Consider using [clickable] if it's necessary to handle only primary clicks. Unlike [clickable],
 * [onClick] doesn't add [Modifier.indication], [Modifier.hoverable], [Modifier.focusable], click by Enter key, etc.
 * If necessary, one has to add those manually when using [onClick].
 *
 * @param interactionSource [MutableInteractionSource] that will be used to emit
 * [PressInteraction.Press] when this clickable is pressed. Only the initial (first) press will be
 * recorded and emitted with [MutableInteractionSource].
 * @param enabled Controls the enabled state. When `false`, [onClick], [onLongClick] or
 * [onDoubleClick] won't be invoked
 * @param matcher defines supported pointer types and required properties
 * @param keyboardModifiers defines a condition that [PointerEvent.keyboardModifiers] has to match
 * @param onLongClick will be called when user long presses on the element
 * @param onDoubleClick will be called when user double clicks on the element
 * @param onClick will be called when user clicks on the element
 */
// TODO(https://youtrack.jetbrains.com/issue/COMPOSE-156) rewrite to Modifier.Node
@ExperimentalFoundationApi
fun Modifier.onClick(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource,
    matcher: PointerMatcher = PointerMatcher.Primary,
    keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true },
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) = composed(
    inspectorInfo = {
        name = "onClick"
        properties["enabled"] = enabled
        properties["matcher"] = matcher
        properties["keyboardModifiers"] = keyboardModifiers
        properties["onDoubleClick"] = onDoubleClick
        properties["onLongClick"] = onLongClick
        properties["onClick"] = onClick
        properties["interactionSource"] = interactionSource
    },
    factory = {

        val gestureModifier = if (enabled) {
            val interactionData = remember { InteractionData() }
            val onClickState = rememberUpdatedState(onClick)
            val on2xClickState = rememberUpdatedState(onDoubleClick)
            val onLongClickState = rememberUpdatedState(onLongClick)
            val keyboardModifiersState = rememberUpdatedState(keyboardModifiers)
            val focusRequester = remember { FocusRequester() }
            val currentKeyPressInteractions = remember { mutableMapOf<Key, PressInteraction.Press>() }

            val hasLongClick = onLongClick != null
            val hasDoubleClick = onDoubleClick != null

            DisposableEffect(hasLongClick) {
                onDispose {
                    interactionData.pressInteraction?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        interactionData.pressInteraction = null
                    }
                }
            }
            DisposableEffect(interactionSource) {
                onDispose {
                    interactionData.pressInteraction?.let { oldValue ->
                        val interaction = PressInteraction.Cancel(oldValue)
                        interactionSource.tryEmit(interaction)
                        interactionData.pressInteraction = null
                    }
                    currentKeyPressInteractions.values.forEach {
                        interactionSource.tryEmit(PressInteraction.Cancel(it))
                    }
                    currentKeyPressInteractions.clear()
                }
            }

            val matcherState = rememberUpdatedState(matcher)

            Modifier.pointerInput(interactionSource, hasLongClick, hasDoubleClick) {
                detectTapGestures(
                    matcher = matcherState.value,
                    keyboardModifiers = {
                        keyboardModifiersState.value(this)
                    },
                    onDoubleTap = if (hasDoubleClick) {
                        {
                            if (isRequestFocusOnClickEnabled()) {
                                focusRequester.requestFocus()
                            }
                            on2xClickState.value!!.invoke()
                        }
                    } else {
                        null
                    },
                    onLongPress = if (hasLongClick) {
                        {
                            if (isRequestFocusOnClickEnabled()) {
                                focusRequester.requestFocus()
                            }
                            onLongClickState.value!!.invoke()
                        }
                    } else {
                        null
                    },
                    onTap = {
                        if (isRequestFocusOnClickEnabled()) {
                            focusRequester.requestFocus()
                        }
                        onClickState.value()
                    },
                    onPress = {
                        handlePressInteraction(
                            pressPoint = it,
                            interactionSource = interactionSource,
                            interactionData = interactionData,
                            delayPressInteraction = { false }
                        )
                    }
                )
            }.focusRequester(focusRequester)
        } else {
            Modifier
        }

        gestureModifier
    }
)

// todo https://youtrack.jetbrains.com/issue/COMPOSE-1268/Refactor-Modifier.onClick-get-rid-of-InteractionData Refactor the same way as in 2e1799e0

private class InteractionData {
    var pressInteraction: PressInteraction.Press? = null
}

private suspend fun PressGestureScope.handlePressInteraction(
    pressPoint: Offset,
    interactionSource: MutableInteractionSource,
    interactionData: InteractionData,
    delayPressInteraction: () -> Boolean
) {
    coroutineScope {
        val delayJob = launch {
            if (delayPressInteraction()) {
                delay(TapIndicationDelay)
            }
            val press = PressInteraction.Press(pressPoint)
            interactionSource.emit(press)
            interactionData.pressInteraction = press
        }
        val success = tryAwaitRelease()
        if (delayJob.isActive) {
            delayJob.cancelAndJoin()
            // The press released successfully, before the timeout duration - emit the press
            // interaction instantly. No else branch - if the press was cancelled before the
            // timeout, we don't want to emit a press interaction.
            if (success) {
                val press = PressInteraction.Press(pressPoint)
                val release = PressInteraction.Release(press)
                interactionSource.emit(press)
                interactionSource.emit(release)
            }
        } else {
            interactionData.pressInteraction?.let { pressInteraction ->
                val endInteraction = if (success) {
                    PressInteraction.Release(pressInteraction)
                } else {
                    PressInteraction.Cancel(pressInteraction)
                }
                interactionSource.emit(endInteraction)
            }
        }
        interactionData.pressInteraction = null
    }
}