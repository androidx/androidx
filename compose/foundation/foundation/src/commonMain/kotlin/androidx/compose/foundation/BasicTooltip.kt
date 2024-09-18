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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
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
@ExperimentalFoundationApi
expect fun BasicTooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: BasicTooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = true,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit
)

/**
 * Create and remember the default [BasicTooltipState].
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [BasicTooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@Composable
@ExperimentalFoundationApi
fun rememberBasicTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): BasicTooltipState =
    remember(isPersistent, mutatorMutex) {
        BasicTooltipStateImpl(
            initialIsVisible = initialIsVisible,
            isPersistent = isPersistent,
            mutatorMutex = mutatorMutex
        )
    }

/**
 * Constructor extension function for [BasicTooltipState]
 *
 * @param initialIsVisible the initial value for the tooltip's visibility when drawn.
 * @param isPersistent [Boolean] that determines if the tooltip associated with this will be
 *   persistent or not. If isPersistent is true, then the tooltip will only be dismissed when the
 *   user clicks outside the bounds of the tooltip or if [BasicTooltipState.dismiss] is called. When
 *   isPersistent is false, the tooltip will dismiss after a short duration. Ideally, this should be
 *   set to true when there is actionable content being displayed within a tooltip.
 * @param mutatorMutex [MutatorMutex] used to ensure that for all of the tooltips associated with
 *   the mutator mutex, only one will be shown on the screen at any time.
 */
@Stable
@ExperimentalFoundationApi
fun BasicTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = true,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex
): BasicTooltipState =
    BasicTooltipStateImpl(
        initialIsVisible = initialIsVisible,
        isPersistent = isPersistent,
        mutatorMutex = mutatorMutex
    )

@Stable
@OptIn(ExperimentalFoundationApi::class)
private class BasicTooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex
) : BasicTooltipState {
    override var isVisible by mutableStateOf(initialIsVisible)

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

/**
 * The state that is associated with an instance of a tooltip. Each instance of tooltips should have
 * its own [BasicTooltipState].
 */
@Stable
@ExperimentalFoundationApi
interface BasicTooltipState {
    /** [Boolean] that indicates if the tooltip is currently being shown or not. */
    val isVisible: Boolean

    /**
     * [Boolean] that determines if the tooltip associated with this will be persistent or not. If
     * isPersistent is true, then the tooltip will only be dismissed when the user clicks outside
     * the bounds of the tooltip or if [BasicTooltipState.dismiss] is called. When isPersistent is
     * false, the tooltip will dismiss after a short duration. Ideally, this should be set to true
     * when there is actionable content being displayed within a tooltip.
     */
    val isPersistent: Boolean

    /**
     * Show the tooltip associated with the current [BasicTooltipState]. When this method is called
     * all of the other tooltips currently being shown will dismiss.
     *
     * @param mutatePriority [MutatePriority] to be used.
     */
    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)

    /**
     * Dismiss the tooltip associated with this [BasicTooltipState] if it's currently being shown.
     */
    fun dismiss()

    /** Clean up when the this state leaves Composition. */
    fun onDispose()
}

/** BasicTooltip defaults that contain default values for tooltips created. */
@ExperimentalFoundationApi
object BasicTooltipDefaults {
    /** The global/default [MutatorMutex] used to sync Tooltips. */
    val GlobalMutatorMutex: MutatorMutex = MutatorMutex()

    /**
     * The default duration, in milliseconds, that non-persistent tooltips will show on the screen
     * before dismissing.
     */
    const val TooltipDuration = 1500L
}
