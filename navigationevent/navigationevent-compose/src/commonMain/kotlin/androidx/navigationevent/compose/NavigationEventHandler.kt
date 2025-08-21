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

package androidx.navigationevent.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventCallback
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInfo.NotProvided
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Handles predictive back navigation gestures.
 *
 * This effect registers a callback to receive updates on the progress of system back gestures as a
 * [Flow] of [NavigationEvent].
 *
 * The [onEvent] lambda should be structured to handle the start, progress, completion, and
 * cancellation of the gesture:
 * ```kotlin
 * NavigationEventHandler { progress: Flow<NavigationEvent> ->
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
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * * **last** among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use the `enabled` parameter to
 * control whether the handler is active. This is preferable to conditionally calling
 * [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change the order
 * of composition, leading to unpredictable behavior where different handlers are invoked after
 * recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back gesture may be dispatched within a rendering frame before
 * the [enabled] flag is updated, which can cause unexpected behavior (see b/375343407,
 * b/384186542). For example, if `enabled` is set to `false`, a gesture initiated in the same frame
 * may still trigger this handler because the system sees the stale `true` value.
 *
 * @param enabled Controls whether this handler is active. **Important**: Due to the timing issue
 *   described above, a gesture starting immediately after `enabled` is set to `false` may still
 *   trigger this handler.
 * @param onEvent The lambda that receives the flow of back gesture events when a gesture begins.
 *   You **must** `collect` the flow within this lambda.
 * @see NavigationEventHandler
 */
@Composable
public fun NavigationEventHandler(
    enabled: Boolean = true,
    onEvent: suspend (progress: Flow<NavigationEvent>) -> Unit,
) {
    NavigationEventHandler(currentInfo = NotProvided, previousInfo = null, enabled, onEvent)
}

/**
 * Handles predictive back navigation gestures.
 *
 * This overload allows associating specific [NavigationEventInfo] with the current state (from
 * which the user is navigating) and the previous state (to which the user may return). This is
 * useful for creating animations that are specific to the content being displayed.
 *
 * This effect registers a callback to receive updates on the progress of system back gestures as a
 * [Flow] of [NavigationEvent].
 *
 * The [onEvent] lambda should be structured to handle the start, progress, completion, and
 * cancellation of the gesture:
 * ```kotlin
 * NavigationEventHandler { progress: Flow<NavigationEvent> ->
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
 * ## Precedence
 * When multiple [NavigationEventHandler] are present in the composition, the one that is composed
 * * **last** among all enabled handlers will be invoked.
 *
 * ## Usage
 * It is important to call this composable **unconditionally**. Use the `enabled` parameter to
 * control whether the handler is active. This is preferable to conditionally calling
 * [NavigationEventHandler] (e.g., inside an `if` block), as conditional calls can change the order
 * of composition, leading to unpredictable behavior where different handlers are invoked after
 * recomposition.
 *
 * ## Timing Consideration
 * There are cases where a predictive back gesture may be dispatched within a rendering frame before
 * the [enabled] flag is updated, which can cause unexpected behavior (see b/375343407,
 * b/384186542). For example, if `enabled` is set to `false`, a gesture initiated in the same frame
 * may still trigger this handler because the system sees the stale `true` value.
 *
 * @param T The type of the navigation information.
 * @param currentInfo An object containing information about the current destination.
 * @param previousInfo An object containing information about the destination the user is navigating
 *   back to. Can be `null` if the information is not available.
 * @param enabled Controls whether this handler is active. **Important**: Due to the timing issue
 *   described above, a gesture starting immediately after `enabled` is set to `false` may still
 *   trigger this handler.
 * @param onEvent The lambda that receives the flow of back gesture events when a gesture begins.
 *   You **must** `collect` the flow within this lambda.
 * @see NavigationEventHandler
 */
@Composable
public fun <T : NavigationEventInfo> NavigationEventHandler(
    currentInfo: T,
    previousInfo: T?,
    enabled: Boolean = true,
    onEvent: suspend (progress: Flow<NavigationEvent>) -> Unit,
) {
    // ensure we don't re-register callbacks when onBack changes
    val currentOnBack by rememberUpdatedState(onEvent)
    val navEventScope = rememberCoroutineScope()

    val navEventCallBack = remember {
        NavigationEventHandlerCallback<T>(enabled, navEventScope, currentOnBack)
    }

    // we want to use the same callback, but ensure we adjust the variable on recomposition
    SideEffect {
        navEventCallBack.currentOnBack = currentOnBack
        navEventCallBack.onBackScope = navEventScope
        navEventCallBack.setInfo(currentInfo, previousInfo)
    }

    LaunchedEffect(enabled) { navEventCallBack.setIsEnabled(enabled) }

    val navEventDispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
                "No NavigationEventDispatcher was provided via LocalNavigationEventDispatcherOwner"
            }
            .navigationEventDispatcher

    DisposableEffect(navEventDispatcher) {
        navEventDispatcher.addCallback(navEventCallBack)

        onDispose { navEventCallBack.remove() }
    }
}

private class OnBackInstance(
    scope: CoroutineScope,
    var isPredictiveBack: Boolean,
    onBack: suspend (progress: Flow<NavigationEvent>) -> Unit,
    callback: NavigationEventCallback<*>,
) {
    val channel =
        Channel<NavigationEvent>(capacity = BUFFERED, onBufferOverflow = BufferOverflow.SUSPEND)
    val job =
        scope.launch {
            if (callback.isEnabled) {
                var completed = false
                onBack(channel.consumeAsFlow().onCompletion { completed = true })
                check(completed) { "You must collect the progress flow" }
            }
        }

    fun send(backEvent: NavigationEvent) = channel.trySend(backEvent)

    // idempotent if invoked more than once
    fun close() = channel.close()

    fun cancel() {
        channel.cancel(CancellationException("navEvent cancelled"))
        job.cancel()
    }
}

private class NavigationEventHandlerCallback<T : NavigationEventInfo>(
    isEnabled: Boolean,
    var onBackScope: CoroutineScope,
    var currentOnBack: suspend (progress: Flow<NavigationEvent>) -> Unit,
) : NavigationEventCallback<T>(isEnabled) {
    private var onBackInstance: OnBackInstance? = null
    private var isActive = false

    fun setIsEnabled(enabled: Boolean) {
        // We are disabling a callback that was enabled.
        if (!enabled && !isActive && isEnabled) {
            onBackInstance?.cancel()
        }
        isEnabled = enabled
    }

    override fun onEventStarted(event: NavigationEvent) {
        // in case the previous onBackInstance was started by a normal back gesture
        // we want to make sure it's still cancelled before we start a predictive
        // back gesture
        onBackInstance?.cancel()
        if (isEnabled) {
            onBackInstance = OnBackInstance(onBackScope, true, currentOnBack, this)
        }
        isActive = true
    }

    override fun onEventProgressed(event: NavigationEvent) {
        onBackInstance?.send(event)
    }

    override fun onEventCompleted() {
        // handleOnBackPressed could be called by regular back to restart
        // a new back instance. If this is the case (where current back instance
        // was NOT started by handleOnBackStarted) then we need to reset the previous
        // regular back.
        onBackInstance?.apply {
            if (!isPredictiveBack) {
                cancel()
                onBackInstance = null
            }
        }
        if (onBackInstance == null) {
            onBackInstance = OnBackInstance(onBackScope, false, currentOnBack, this)
        }

        // finally, we close the channel to ensure no more events can be sent
        // but let the job complete normally
        onBackInstance?.close()
        onBackInstance?.isPredictiveBack = false
        isActive = false
    }

    override fun onEventCancelled() {
        // cancel will purge the channel of any sent events that are yet to be received
        onBackInstance?.cancel()
        onBackInstance?.isPredictiveBack = false
        isActive = false
    }
}
