/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.content

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.TypedArray
import android.os.Handler
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Return the handle to a system-level service by class.
 *
 * @see ContextCompat.getSystemService
 */
public inline fun <reified T : Any> Context.getSystemService(): T? =
    ContextCompat.getSystemService(this, T::class.java)

/**
 * Executes [block] on a [TypedArray] receiver. The [TypedArray] holds the attribute values in [set]
 * that are listed in [attrs]. In addition, if the given [AttributeSet] specifies a style class
 * (through the `style` attribute), that style will be applied on top of the base attributes it
 * defines.
 *
 * @param set The base set of attribute values.
 * @param attrs The desired attributes to be retrieved. These attribute IDs must be sorted in
 *   ascending order.
 * @param defStyleAttr An attribute in the current theme that contains a reference to a style
 *   resource that supplies defaults values for the [TypedArray]. Can be 0 to not look for defaults.
 * @param defStyleRes A resource identifier of a style resource that supplies default values for the
 *   [TypedArray], used only if [defStyleAttr] is 0 or can not be found in the theme. Can be 0 to
 *   not look for defaults.
 * @param block The block that will be executed.
 * @see Context.obtainStyledAttributes
 * @see android.content.res.Resources.Theme.obtainStyledAttributes
 */
public inline fun Context.withStyledAttributes(
    set: AttributeSet? = null,
    attrs: IntArray,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    block: TypedArray.() -> Unit,
) {
    obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes).apply(block).recycle()
}

/**
 * Executes [block] on a [TypedArray] receiver. The [TypedArray] holds the values defined by the
 * style resource [resourceId] which are listed in [attrs].
 *
 * @param resourceId The desired style resource.
 * @param attrs The desired attributes. These attribute IDs must be sorted in ascending order.
 * @param block The block that will be executed.
 * @see Context.obtainStyledAttributes
 * @see android.content.res.Resources.Theme.obtainStyledAttributes
 */
public inline fun Context.withStyledAttributes(
    @StyleRes resourceId: Int,
    attrs: IntArray,
    block: TypedArray.() -> Unit,
) {
    obtainStyledAttributes(resourceId, attrs).apply(block).recycle()
}

/**
 * Registers a [BroadcastReceiver] until the coroutine is cancelled.
 *
 * Equivalent to calling [ContextCompat.registerReceiver], and when the coroutine is cancelled
 * calling [Context.unregisterReceiver], but exceptions from [onReceive] will propagate to the
 * caller (unregistering the receiver in the process).
 *
 * The rules of [BroadcastReceiver.onReceive] apply here. You can use the receiver
 * [BroadcastReceiver] to view/modify the current result values.
 *
 * [onReceive] runs on the provided [scheduler] thread, or the main thread if it's null. This means
 * it can continue running even after [receiveBroadcasts] is cancelled. To propagate cancellation,
 * consider using [receiveBroadcastsAsync].
 *
 * If you wish to process broadcasts asynchronously (and concurrently), consider using
 * [receiveBroadcastsAsync], or manually use [BroadcastReceiver.goAsync].
 *
 * Example usage:
 * ```
 * import android.app.Activity.RESULT_OK
 * import android.content.Intent
 * import android.content.IntentFilter
 * import androidx.core.content.ContextCompat
 * import androidx.core.content.receiveBroadcasts
 * import kotlinx.coroutines.coroutineScope
 *
 * coroutineScope {
 *   val job = launch {
 *     context.receiveBroadcasts(
 *       filter = IntentFilter(Intent.ACTION_TIME_CHANGED),
 *       flags = ContextCompat.RECEIVER_EXPORTED,
 *     ) { intent: Intent? ->
 *       // Process intent
 *       setResultCode(RESULT_OK) // Set broadcast result
 *     }
 *   }
 *   ...
 *   job.cancel() // Unregister
 * }
 * ```
 *
 * @param filter Selects the Intent broadcasts to be received.
 * @param flags If this receiver is listening for broadcasts sent from other apps - even other apps
 *   that you own - use the [ContextCompat.RECEIVER_EXPORTED] flag. If instead this receiver is
 *   listening only for broadcasts sent by your app, or from the system UID, use the
 *   [ContextCompat.RECEIVER_NOT_EXPORTED] flag.
 * @param broadcastPermission String naming a permission that a broadcaster must hold in order to
 *   send and Intent to you. If null, no permission is required.
 * @param scheduler Handler identifying the thread will receive the Intent. If null, the main thread
 *   of the process will be used.
 * @param onReceive The callback equivalent of [BroadcastReceiver.onReceive], taking the
 *   [BroadcastReceiver] as a receiver.
 * @see ContextCompat.registerReceiver
 * @see BroadcastReceiver.onReceive
 */
public suspend fun Context.receiveBroadcasts(
    filter: IntentFilter,
    flags: Int,
    broadcastPermission: String? = null,
    scheduler: Handler? = null,
    onReceive: BroadcastReceiver.(Intent?) -> Unit,
): Nothing {
    var receiver: BroadcastReceiver? = null
    try {
        suspendCancellableCoroutine<Nothing> { continuation ->
            receiver = ContinuationBroadcastReceiver(continuation, onReceive)
            ContextCompat.registerReceiver(
                this,
                receiver,
                filter,
                broadcastPermission,
                scheduler,
                flags,
            )
        }
    } finally {
        if (receiver != null) unregisterReceiver(receiver)
    }
}

/**
 * Register a [BroadcastReceiver] until the coroutine is cancelled.
 *
 * Equivalent to calling [ContextCompat.registerReceiver], and when the coroutine is cancelled
 * calling [Context.unregisterReceiver], but uses [BroadcastReceiver.goAsync] to allow suspending
 * [onReceive] and calls [BroadcastReceiver.PendingResult.finish] when [onReceive] returns or
 * throws.
 *
 * The rules of [BroadcastReceiver.goAsync] apply here. You can use the receiver
 * [BroadcastReceiver.PendingResult] to view/modify the current result values. If you wish to use
 * the [BroadcastReceiver], consider using [receiveBroadcasts].
 *
 * Each [onReceive] is invoked concurrently in a child coroutine of the current context, and is
 * cancelled when [receiveBroadcastsAsync] is cancelled. If any [onReceive] throws, all other
 * concurrent invocations are cancelled and the exception is propagated to the caller (unregistering
 * the receiver in the process).
 *
 * **Do not call [BroadcastReceiver.PendingResult.finish] yourself, this is done for you when
 * [onReceive] returns or throws.**
 *
 * Example usage:
 * ```
 * import android.app.Activity.RESULT_OK
 * import android.content.Intent
 * import android.content.IntentFilter
 * import androidx.core.content.ContextCompat
 * import androidx.core.content.receiveBroadcasts
 * import kotlinx.coroutines.coroutineScope
 * import kotlinx.coroutines.delay
 *
 * coroutineScope {
 *   val job = launch {
 *     context.receiveBroadcastsAsync(
 *       filter = IntentFilter(Intent.ACTION_TIME_CHANGED),
 *       flags = ContextCompat.RECEIVER_EXPORTED,
 *     ) { intent: Intent? ->
 *       delay(100) // Process intent with suspending work
 *       setResultCode(RESULT_OK) // Set broadcast result
 *     }
 *   }
 *   ...
 *   job.cancel() // Unregister and cancel ongoing callbacks.
 * }
 * ```
 *
 * @param filter Selects the Intent broadcasts to be received.
 * @param flags If this receiver is listening for broadcasts sent from other apps - even other apps
 *   that you own - use the [ContextCompat.RECEIVER_EXPORTED] flag. If instead this receiver is
 *   listening only for broadcasts sent by your app, or from the system UID, use the
 *   [ContextCompat.RECEIVER_NOT_EXPORTED] flag.
 * @param broadcastPermission String naming a permission that a broadcaster must hold in order to
 *   send and Intent to you. If null, no permission is required.
 * @param scheduler Handler identifying the thread will receive the Intent. If null, the main thread
 *   of the process will be used. Note that this is not used to run [onReceive], but if the handler
 *   is blocked [onReceive] will not execute.
 * @param onReceive The callback equivalent of [BroadcastReceiver.onReceive], taking the
 *   [BroadcastReceiver.PendingResult] as receiver.
 * @see ContextCompat.registerReceiver
 * @see BroadcastReceiver.onReceive
 */
public suspend fun Context.receiveBroadcastsAsync(
    filter: IntentFilter,
    flags: Int,
    broadcastPermission: String? = null,
    scheduler: Handler? = null,
    onReceive: suspend BroadcastReceiver.PendingResult.(Intent?) -> Unit,
): Nothing = coroutineScope {
    receiveBroadcasts(filter, flags, broadcastPermission, scheduler) { intent ->
        val pendingResult = goAsync()
        // Using ATOMIC ensures that if the coroutine is cancelled, we will still correctly finish
        // the outstanding PendingResult before joining with our outer scope.
        // Using ensureActive() to mitigates executing synchronous code in a cancelled coroutine.
        @OptIn(DelicateCoroutinesApi::class)
        launch(start = CoroutineStart.ATOMIC) {
            try {
                ensureActive()
                pendingResult.onReceive(intent)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/** Broadcast receiver that fails the provided [continuation] when [onReceiveChecked] fails. */
private class ContinuationBroadcastReceiver(
    continuation: Continuation<*>,
    private val onReceiveChecked: BroadcastReceiver.(Intent?) -> Unit,
) : BroadcastReceiver() {

    private var continuation: Continuation<*>? = continuation

    override fun onReceive(context: Context?, intent: Intent?) {
        try {
            onReceiveChecked(intent)
        } catch (t: Throwable) {
            // Redirect an exception to the continuation.
            // If we've already resumed it with a different exception, panic-throw to the
            // BroadcastReceiver's caller.
            // BroadcastReceivers will always be invoked on the same Handler so this is thread-safe
            // to check and mutate this way.
            val continuation = this.continuation ?: throw t
            continuation.resumeWithException(t)
            this.continuation = null
        }
    }
}
