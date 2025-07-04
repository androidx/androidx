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

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

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
 *   component. For certain a11y cases, such as when the tooltip has an action and Talkback is on,
 *   focusable will be forced to true to allow for the correct a11y behavior.
 * @param enableUserInput [Boolean] which determines if this BasicTooltipBox will handle long press
 *   and mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
internal fun BasicTooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    hasAction: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val shouldForceFocusableForA11y =
        hasAction && rememberTouchExplorationOrSwitchAccessServiceState().value
    Box {
        if (state.isVisible) {
            TooltipPopup(
                positionProvider = positionProvider,
                state = state,
                onDismissRequest = onDismissRequest,
                scope = scope,
                focusable = focusable || shouldForceFocusableForA11y,
                content = tooltip,
            )
        }

        WrappedAnchor(
            enableUserInput = enableUserInput,
            state = state,
            modifier = modifier,
            content = content,
        )
    }

    DisposableEffect(state) { onDispose { state.onDispose() } }
}

@Composable
private fun WrappedAnchor(
    enableUserInput: Boolean,
    state: TooltipState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val longPressLabel = BasicTooltipStrings.label()
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
    onDismissRequest: (() -> Unit)?,
    scope: CoroutineScope,
    focusable: Boolean,
    content: @Composable () -> Unit,
) {
    val tooltipDescription = BasicTooltipStrings.description()
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = {
            if (onDismissRequest == null) {
                if (state.isVisible) {
                    scope.launch { state.dismiss() }
                }
            } else {
                onDismissRequest()
            }
        },
        properties = PopupProperties(focusable = focusable),
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
    scope: CoroutineScope,
): Modifier =
    if (enabled) {
        this.parentSemantics {
            onLongClick(
                label = label,
                action = {
                    scope.launch { state.show() }
                    true
                },
            )
        }
    } else this

/**
 * Create and remember the default [BasicTooltipState].
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [TooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@Composable
internal fun rememberBasicTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    remember(isPersistent, mutatorMutex) {
        BasicTooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex,
        )
    }

/**
 * Constructor extension function for [BasicTooltipState]
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [TooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@Stable
internal fun BasicTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState =
    BasicTooltipStateImpl(
        initialIsVisible = initialIsVisible,
        isPersistent = isPersistent,
        mutatorMutex = mutatorMutex,
    )

@Stable
private class BasicTooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
) : TooltipState {
    override var isVisible by mutableStateOf(initialIsVisible)
    override val transition: MutableTransitionState<Boolean> = MutableTransitionState(false)

    /** continuation used to clean up */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Show the tooltip associated with the current [BasicTooltipState]. When this method is called,
     * all of the other tooltips associated with [mutatorMutex] will be dismissed.
     *
     * @param mutatePriority [MutatePriority] to be used with [mutatorMutex].
     */
    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                isVisible = true
                job = continuation
            }
        }

        // Show associated tooltip for [TooltipDuration] amount of time
        // or until tooltip is explicitly dismissed depending on [isPersistent].
        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent) {
                    cancellableShow()
                } else {
                    withTimeout(BasicTooltipDefaults.TooltipDuration) { cancellableShow() }
                }
            } finally {
                // timeout or cancellation has occurred
                // and we close out the current tooltip.
                isVisible = false
            }
        }
    }

    /**
     * Dismiss the tooltip associated with this [BasicTooltipState] if it's currently being shown.
     */
    override fun dismiss() {
        isVisible = false
    }

    /** Cleans up [mutatorMutex] when the tooltip associated with this state leaves Composition. */
    override fun onDispose() {
        job?.cancel()
    }
}

/** BasicTooltip defaults that contain default values for tooltips created. */
internal object BasicTooltipDefaults {
    /** The global/default [MutatorMutex] used to sync Tooltips. */
    val GlobalMutatorMutex: MutatorMutex = MutatorMutex()

    /**
     * The default duration, in milliseconds, that non-persistent tooltips will show on the screen
     * before dismissing.
     */
    const val TooltipDuration = 1500L
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object BasicTooltipStrings {
    @Composable fun label(): String

    @Composable fun description(): String
}

/** Returns the current accessibility touch exploration or switch access service [State]. */
@Composable
private fun rememberTouchExplorationOrSwitchAccessServiceState() =
    rememberAccessibilityServiceState(
        listenToTouchExplorationState = true,
        listenToSwitchAccessState = true,
        listenToVoiceAccessState = false,
    )
