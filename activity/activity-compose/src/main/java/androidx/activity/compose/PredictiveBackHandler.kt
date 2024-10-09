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
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineScope
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
 * Calling this in your composable adds the given lambda to the [OnBackPressedDispatcher] of the
 * [LocalOnBackPressedDispatcherOwner]. The lambda passes in a Flow<BackEventCompat> where each
 * [BackEventCompat] reflects the progress of current gesture back. The lambda content should follow
 * this structure:
 * ```
 * PredictiveBackHandler { progress: Flow<BackEventCompat> ->
 *      // code for gesture back started
 *      try {
 *         progress.collect { backevent ->
 *              // code for progress
 *         }
 *         // code for completion
 *      } catch (e: CancellationException) {
 *         // code for cancellation
 *      }
 * }
 * ```
 *
 * If this is called by nested composables, if enabled, the inner most composable will consume the
 * call to system back and invoke its lambda. The call will continue to propagate up until it finds
 * an enabled BackHandler.
 *
 * @sample androidx.activity.compose.samples.PredictiveBack
 * @param enabled if this BackHandler should be enabled, true by default
 * @param onBack the action invoked by back gesture
 */
@SuppressLint("RememberReturnType") // TODO: b/372566999
@Composable
public fun PredictiveBackHandler(
    enabled: Boolean = true,
    onBack:
        suspend (progress: @JvmSuppressWildcards Flow<BackEventCompat>) -> @JvmSuppressWildcards
            Unit
) {
    // ensure we don't re-register callbacks when onBack changes
    val currentOnBack by rememberUpdatedState(onBack)
    val onBackScope = rememberCoroutineScope()

    val backCallBack = remember {
        PredictiveBackHandlerCallback(enabled, onBackScope, currentOnBack)
    }

    // we want to use the same callback, but ensure we adjust the variable on recomposition
    remember(currentOnBack, onBackScope) {
        backCallBack.currentOnBack = currentOnBack
        backCallBack.onBackScope = onBackScope
    }

    LaunchedEffect(enabled) { backCallBack.setIsEnabled(enabled) }

    val backDispatcher =
        checkNotNull(LocalOnBackPressedDispatcherOwner.current) {
                "No OnBackPressedDispatcherOwner was provided via LocalOnBackPressedDispatcherOwner"
            }
            .onBackPressedDispatcher

    @Suppress("deprecation", "KotlinRedundantDiagnosticSuppress") // TODO b/330570365
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, backDispatcher) {
        backDispatcher.addCallback(lifecycleOwner, backCallBack)

        onDispose { backCallBack.remove() }
    }
}

private class OnBackInstance(
    scope: CoroutineScope,
    var isPredictiveBack: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
    callback: OnBackPressedCallback
) {
    val channel = Channel<BackEventCompat>(capacity = BUFFERED, onBufferOverflow = SUSPEND)
    val job =
        scope.launch {
            if (callback.isEnabled) {
                var completed = false
                onBack(channel.consumeAsFlow().onCompletion { completed = true })
                check(completed) { "You must collect the progress flow" }
            }
        }

    fun send(backEvent: BackEventCompat) = channel.trySend(backEvent)

    // idempotent if invoked more than once
    fun close() = channel.close()

    fun cancel() {
        channel.cancel(CancellationException("onBack cancelled"))
        job.cancel()
    }
}

private class PredictiveBackHandlerCallback(
    enabled: Boolean,
    var onBackScope: CoroutineScope,
    var currentOnBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
) : OnBackPressedCallback(enabled) {
    private var onBackInstance: OnBackInstance? = null

    fun setIsEnabled(enabled: Boolean) {
        // We are disabling a callback that was enabled.
        if (!enabled && isEnabled) {
            onBackInstance?.cancel()
        }
        isEnabled = enabled
    }

    override fun handleOnBackStarted(backEvent: BackEventCompat) {
        super.handleOnBackStarted(backEvent)
        // in case the previous onBackInstance was started by a normal back gesture
        // we want to make sure it's still cancelled before we start a predictive
        // back gesture
        onBackInstance?.cancel()
        if (isEnabled) {
            onBackInstance = OnBackInstance(onBackScope, true, currentOnBack, this)
        }
    }

    override fun handleOnBackProgressed(backEvent: BackEventCompat) {
        super.handleOnBackProgressed(backEvent)
        onBackInstance?.send(backEvent)
    }

    override fun handleOnBackPressed() {
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
    }

    override fun handleOnBackCancelled() {
        super.handleOnBackCancelled()
        // cancel will purge the channel of any sent events that are yet to be received
        onBackInstance?.cancel()
        onBackInstance?.isPredictiveBack = false
    }
}
