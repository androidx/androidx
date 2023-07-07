/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget

import android.content.BroadcastReceiver
import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Execute the block asynchronously in a scope with the lifetime of the broadcast.
 *
 * The coroutine scope will finish once the block return, as the broadcast will finish at that
 * point too, allowing the system to kill the broadcast.
 */
internal fun BroadcastReceiver.goAsync(
    coroutineContext: CoroutineContext = Dispatchers.Default,
    block: suspend CoroutineScope.() -> Unit,
) {
    val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    val pendingResult = goAsync()

    coroutineScope.launch {
        try {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(GlanceAppWidgetTag, "BroadcastReceiver execution failed", t)
            } finally {
                // Nothing can be in the `finally` block after this, as this throws a
                // `CancellationException`
                coroutineScope.cancel()
            }
        } finally {
            // This must be the last call, as the process may be killed after calling this.
            try {
                pendingResult.finish()
            } catch (e: IllegalStateException) {
                // On some OEM devices, this may throw an error about "Broadcast already finished".
                // See b/257513022.
                Log.e(GlanceAppWidgetTag, "Error thrown when trying to finish broadcast", e)
            }
        }
    }
}
