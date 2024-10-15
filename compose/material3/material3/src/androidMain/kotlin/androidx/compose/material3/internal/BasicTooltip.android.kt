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

@file:OptIn(ExperimentalMaterial3Api::class)

package androidx.compose.material3.internal

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.R
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * NOTICE: Fork from androidx.compose.foundation.BasicTooltip box since those are experimental
 *
 * BasicTooltipBox that wraps a composable with a tooltip.
 *
 * Tooltip that provides a descriptive message for an anchor. It can be used to call the users
 * attention to the anchor.
 *
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip relative
 *   to the anchor content.
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to this BasicTooltipBox.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true, the tooltip
 *   will consume touch events while it's shown and will have accessibility focus move to the first
 *   element of the component. When false, the tooltip won't consume touch events while it's shown
 *   but assistive-tech users will need to swipe or drag to get to the first element of the
 *   component.
 * @param enableUserInput [Boolean] which determines if this BasicTooltipBox will handle long press
 *   and mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
@ExperimentalMaterial3Api
internal actual fun BasicTooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    modifier: Modifier,
    focusable: Boolean,
    enableUserInput: Boolean,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    Box {
        if (state.isVisible) {
            TooltipPopup(
                positionProvider = positionProvider,
                state = state,
                scope = scope,
                focusable = focusable,
                content = tooltip
            )
        }

        WrappedAnchor(
            enableUserInput = enableUserInput,
            state = state,
            modifier = modifier,
            content = content
        )
    }

    DisposableEffect(state) { onDispose { state.onDispose() } }
}

@Composable
private fun WrappedAnchor(
    enableUserInput: Boolean,
    state: TooltipState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val longPressLabel = stringResource(R.string.tooltip_label)
    Box(
        modifier =
            modifier
                .handleGestures(enableUserInput, state)
                .anchorSemantics(longPressLabel, enableUserInput, state, scope)
    ) {
        content()
    }
}

@Composable
private fun TooltipPopup(
    positionProvider: PopupPositionProvider,
    state: TooltipState,
    scope: CoroutineScope,
    focusable: Boolean,
    content: @Composable () -> Unit
) {
    val tooltipDescription = stringResource(R.string.tooltip_description)
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = {
            if (state.isVisible) {
                scope.launch { state.dismiss() }
            }
        },
        properties = PopupProperties(focusable = focusable)
    ) {
        Box(
            modifier =
                Modifier.semantics {
                    liveRegion = LiveRegionMode.Assertive
                    paneTitle = tooltipDescription
                }
        ) {
            content()
        }
    }
}

private fun Modifier.handleGestures(enabled: Boolean, state: TooltipState): Modifier =
    if (enabled) {
        this.pointerInput(state) {
                coroutineScope {
                    awaitEachGesture {
                        // Long press will finish before or after show so keep track of it, in a
                        // flow to handle both cases
                        val isLongPressedFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
                        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                        val pass = PointerEventPass.Initial
                        // wait for the first down press
                        val inputType = awaitFirstDown(pass = pass).type

                        if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                            try {
                                // listen to if there is up gesture
                                // within the longPressTimeout limit
                                withTimeout(longPressTimeout) {
                                    waitForUpOrCancellation(pass = pass)
                                }
                            } catch (_: PointerEventTimeoutCancellationException) {
                                // handle long press - Show the tooltip
                                launch(start = CoroutineStart.UNDISPATCHED) {
                                    try {
                                        isLongPressedFlow.tryEmit(true)
                                        state.show(MutatePriority.PreventUserInput)
                                    } finally {
                                        if (state.isVisible) {
                                            isLongPressedFlow.collectLatest { isLongPressed ->
                                                if (!isLongPressed) {
                                                    state.dismiss()
                                                }
                                            }
                                        }
                                    }
                                }

                                // consume the children's click handling
                                // Long press may still be in progress
                                val upEvent = waitForUpOrCancellation(pass = pass)
                                upEvent?.consume()
                            } finally {
                                isLongPressedFlow.tryEmit(false)
                            }
                        }
                    }
                }
            }
            .pointerInput(state) {
                coroutineScope {
                    awaitPointerEventScope {
                        val pass = PointerEventPass.Main

                        while (true) {
                            val event = awaitPointerEvent(pass)
                            val inputType = event.changes[0].type
                            if (inputType == PointerType.Mouse) {
                                when (event.type) {
                                    PointerEventType.Enter -> {
                                        launch { state.show(MutatePriority.UserInput) }
                                    }
                                    PointerEventType.Exit -> {
                                        state.dismiss()
                                    }
                                }
                            }
                        }
                    }
                }
            }
    } else this

private fun Modifier.anchorSemantics(
    label: String,
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope
): Modifier =
    if (enabled) {
        this.parentSemantics {
            onLongClick(
                label = label,
                action = {
                    scope.launch { state.show() }
                    true
                }
            )
        }
    } else this
