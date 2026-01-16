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

package androidx.activity.compose

import android.annotation.SuppressLint
import androidx.activity.ActivityFlags
import androidx.activity.BackEventCompat
import androidx.activity.ExperimentalActivityApi
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.compose.internal.BackHandlerCompat
import androidx.activity.compose.internal.BackHandlerDispatcherCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * An effect for handling predictive system back gestures.
 *
 * This effect registers a callback to receive updates on the progress of system back gestures as a
 * [Flow] of [BackEventCompat].
 *
 * The [onBack] lambda should be structured to handle the start, progress, completion, and
 * cancellation of the gesture:
 * ```kotlin
 * PredictiveBackHandler { progress: Flow<BackEventCompat> ->
 *   // This block is executed when the back gesture begins.
 *     try {
 *       progress.collect { backEvent ->
 *         // Handle gesture progress updates here.
 *       }
 *       // This block is executed if the gesture completes successfully.
 *     } catch (e: CancellationException) {
 *       // This block is executed if the gesture is cancelled.
 *       throw e
 *     } finally {
 *       // This block is executed either the gesture is completed or cancelled.
 *   }
 * }
 * ```
 *
 * The handler is registered once and stays attached for the lifetime of the [LifecycleOwner]. Its
 * [OnBackPressedCallback.isEnabled] state automatically follows the lifecycle: it becomes enabled
 * when the lifecycle is at least [Lifecycle.State.STARTED] and disabled otherwise.
 *
 * ## Precedence
 * If multiple [PredictiveBackHandler] are present in the composition, the one that is composed
 * **last** among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use the `enabled` parameter to
 * control whether the handler is active. This is preferable to conditionally calling
 * [PredictiveBackHandler] (e.g., inside an `if` block), as conditional calls can change the order
 * of composition, leading to unpredictable behavior where different handlers are invoked after
 * recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back gesture may be dispatched within a rendering frame before
 * the [enabled] flag is updated, which can cause unexpected behavior (see
 * [b/375343407](https://issuetracker.google.com/375343407),
 * [b/384186542](https://issuetracker.google.com/384186542)). For example, if `enabled` is set to
 * `false`, a gesture initiated in the same frame may still trigger this handler because the system
 * sees the stale `true` value.
 *
 * ## Legacy Behavior
 * To restore the legacy add/remove behavior, set
 * [ActivityFlags.isOnBackPressedLifecycleOrderMaintained] to `false`. In legacy mode, the handler
 * is added on [Lifecycle.Event.ON_START] and removed on [Lifecycle.Event.ON_STOP], which may change
 * dispatch ordering across lifecycle transitions.
 *
 * @sample androidx.activity.compose.samples.PredictiveBack
 * @param enabled Controls whether this handler is active. **Important**: Due to the timing issue
 *   described above, a gesture starting immediately after `enabled` is set to `false` may still
 *   trigger this handler.
 * @param onBack The suspending lambda to be invoked by the back gesture. It receives a `Flow` that
 *   can be collected to track the gesture's progress.
 */
@SuppressLint("RememberReturnType") // TODO: b/372566999
@OptIn(ExperimentalActivityApi::class)
@Composable
public fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack:
        suspend (progress: @JvmSuppressWildcards Flow<BackEventCompat>) -> @JvmSuppressWildcards
            Unit,
) {
    // Short-circuit: Only read the legacy owner if the new one is missing.
    val owner =
        LocalNavigationEventDispatcherOwner.current
            ?: LocalOnBackPressedDispatcherOwner.current
            ?: error(
                "No NavigationEventDispatcherOwner was provided via " +
                    "LocalNavigationEventDispatcherOwner and no OnBackPressedDispatcherOwner was " +
                    "provided via LocalOnBackPressedDispatcherOwner. Please provide one of the two."
            )

    val dispatcher =
        remember(owner) {
            // Create a dispatcher compatibility layer that decides whether to use the new
            // 'NavigationEventDispatcher' or the legacy 'OnBackPressedDispatcher'.
            BackHandlerDispatcherCompat(
                (owner as? NavigationEventDispatcherOwner)?.navigationEventDispatcher,
                (owner as? OnBackPressedDispatcherOwner)?.onBackPressedDispatcher,
            )
        }

    val scope = rememberCoroutineScope()
    val compositeKey = currentCompositeKeyHashCode
    val handler =
        remember(dispatcher, compositeKey) {
            ComposePredictiveBackHandler(
                scope,
                info = PredictiveBackHandlerInfo(owner, compositeKey),
            )
        }

    if (ActivityFlags.isOnBackPressedLifecycleOrderMaintained) {
        // Keep the handler instance stable across recompositions, but update the active parameters.
        SideEffect { handler.currentOnBack = onBack }

        // Use LifecycleStartEffect to add the handler in sync with the lifecycle,
        // avoiding the frame delay that happens with state-based APIs like collectAsState().
        LifecycleStartEffect(enabled, handler) {
            handler.isBackEnabled = enabled
            onStopOrDispose { handler.isBackEnabled = false }
        }

        DisposableEffect(dispatcher, handler) {
            dispatcher.addHandler(handler)
            onDispose { dispatcher.removeHandler(handler) }
        }
    } else {
        // Keep the handler instance stable across recompositions, but update the active parameters.
        SideEffect {
            handler.isBackEnabled = enabled
            handler.currentOnBack = onBack
        }

        // Use LifecycleStartEffect to add the handler in sync with the lifecycle,
        // avoiding the frame delay that happens with state-based APIs like collectAsState().
        LifecycleStartEffect(dispatcher, handler) {
            dispatcher.addHandler(handler)
            onStopOrDispose { dispatcher.removeHandler(handler) }
        }
    }
}

/**
 * State holder that bridges navigation back events to the user-supplied `onBack`.
 *
 * One instance services at most one active gesture. It exposes two mutable inputs that are updated
 * by the composable:
 * - [isBackEnabled]: maps to `isBackEnabled` in the base class.
 * - [currentOnBack]: the lambda to invoke for each gesture; it must consume the progress flow.
 *
 * Internally we create a new [Channel] and [Job] per gesture. Closing/cancelling them signals
 * completion/cancellation to the collector.
 */
private class ComposePredictiveBackHandler(
    val scope: CoroutineScope,
    info: PredictiveBackHandlerInfo,
) : BackHandlerCompat(info) {

    /** Latest `onBack` implementation to run for the next gesture. */
    var currentOnBack: suspend (progress: Flow<BackEventCompat>) -> Unit = {}

    /**
     * Mirrors the public `enabled` flag. When disabling and a prior gesture's job has already
     * finished (`isActive == false`), perform best-effort cleanup of any lingering resources. (If
     * the job is `null`, there’s nothing to clean.)
     */
    override var isBackEnabled: Boolean
        get() = super.isBackEnabled
        set(value) {
            // If the handler is being disabled with no active gesture, ensure we clean up any
            // leftover resources from a prior gesture.
            if (!value && super.isBackEnabled && activeJob?.isActive == false) {
                onBackCancelled()
            }
            super.isBackEnabled = value
        }

    // Gesture-scoped resources. A new channel/job is created per gesture and torn down on end.
    private var activeChannel: Channel<BackEventCompat>? = null
    private var activeJob: Job? = null
    private var isPredictiveBack: Boolean = false

    /**
     * Start a gesture session: create a fresh channel and invoke the consumer lambda with a Flow.
     * The lambda **must** collect; we enforce this via an `onCompletion` flag.
     */
    private fun launchNewGesture() {
        activeChannel = Channel(capacity = BUFFERED, onBufferOverflow = SUSPEND)
        activeJob =
            scope.launch {
                if (isBackEnabled) {
                    var completed = false
                    currentOnBack(activeChannel!!.consumeAsFlow().onCompletion { completed = true })
                    check(completed) { "You must collect the progress flow" }
                }
            }
    }

    override fun onBackStarted(event: BackEventCompat) {
        // Defensive: if a previous gesture wasn't fully cleaned up, cancel it first.
        onBackCancelled()
        if (isBackEnabled) {
            isPredictiveBack = true
            launchNewGesture()
        }
    }

    override fun onBackProgressed(event: BackEventCompat) {
        // Non-blocking send. With BUFFERED+SUSPEND, trySend will **not** suspend; on a full buffer,
        // the send fails and the event is dropped. This is intentional to avoid blocking callbacks.
        activeChannel?.trySend(element = event)
    }

    override fun onBackCompleted() {
        // For a non-predictive flow, deliver completion through a fresh session.
        if (activeChannel != null && !isPredictiveBack) {
            onBackCancelled()
        }

        // If nothing is active, spin up a non-predictive session so the collector completes.
        if (activeChannel == null) {
            isPredictiveBack = false
            launchNewGesture()
        }

        // Closing the channel signals end-of-stream (i.e., completion) to the collector.
        activeChannel?.close()
        isPredictiveBack = false
    }

    override fun onBackCancelled() {
        // Best-effort cancellation of both the channel and the running job.
        activeChannel?.cancel(cause = CancellationException(/* message= */ "onBack cancelled"))
        activeJob?.cancel()
        activeChannel = null
        activeJob = null
        isPredictiveBack = false
    }
}

private data class PredictiveBackHandlerInfo(val owner: Any, val compositeKey: Long) :
    NavigationEventInfo()
