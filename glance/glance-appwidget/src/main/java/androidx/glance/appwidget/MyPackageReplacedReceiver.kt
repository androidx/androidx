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
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver handling updates after a package update. Useful to know if classes appeared or
 * disappeared and need cleaning.
 */
public open class MyPackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        checkNotNull(context) { "onReceive context is null" }
        checkNotNull(intent) { "onReceive intent is null" }
        // This is not tied to a widget, so there is no UpdateStrategy to defer to.
        if (isGoAsyncBroken()) {
            // Run without goAsync (and without WorkManager); the cleanup is short and best-effort.
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    GlanceAppWidgetManager(context).cleanReceivers()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logException(e)
                }
            }
        } else {
            goAsync {
                val manager = GlanceAppWidgetManager(context)
                manager.cleanReceivers()
            }
        }
    }
}
