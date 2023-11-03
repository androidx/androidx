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

package androidx.glance.appwidget

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.action.LambdaActionBroadcasts
import java.lang.IllegalStateException
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * This receiver responds to lambda action clicks for unmanaged sessions (created by
 * [GlanceAppWidget.runComposition]). In managed sessions that compose UI for a bound widget, the
 * widget's [GlanceAppWidgetReceiver] is used as the receiver for lambda actions. However, when
 * running a session with [GlanceAppWidget.runComposition], there is no guarantee that the widget
 * is attached to some GlanceAppWidgetReceiver. Instead, unmanaged sessions register themselves to
 * receive lambdas while they are running (with [UnmanagedSessionReceiver.registerSession]), and set
 * their lambda target to [UnmanagedSessionReceiver]. This is also used by
 * [GlanceRemoteViewsService] to provide list items for unmanaged sessions.
 */
internal class UnmanagedSessionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LambdaActionBroadcasts.ActionTriggerLambda) {
            val actionKey = intent.getStringExtra(LambdaActionBroadcasts.ExtraActionKey)
                ?: error("Intent is missing ActionKey extra")
            val id = intent.getIntExtra(LambdaActionBroadcasts.ExtraAppWidgetId, -1)
            if (id == -1) error("Intent is missing AppWidgetId extra")
            getSession(id)?.let { session ->
                goAsync(Dispatchers.Main) {
                    session.runLambda(actionKey)
                }
            }
                ?: Log.e(
                    GlanceAppWidgetTag,
                    "A lambda created by an unmanaged glance session cannot be serviced" +
                        "because that session is no longer running."
                )
        }
    }

    companion object {
        @SuppressLint("PrimitiveInCollection")
        private val activeSessions = mutableMapOf<Int, Registration>()
        private class Registration(
            val session: AppWidgetSession,
            val coroutine: CancellableContinuation<Nothing>
        )

        /**
         * Registers [session] to handle lambdas created from an unmanaged session running for
         * [appWidgetId].
         *
         * This call will suspend once the session is registered. On cancellation, this session will
         * be unregistered. That way, the registration is tied to the surrounding coroutine scope
         * and does not need to be manually unregistered.
         *
         * If called from another coroutine with the same [appWidgetId], this call will resume with
         * an exception, and the new registration will succeed. (i.e., only one session per
         * [appWidgetId] can be registered at the same time). By default, [runComposition] uses
         * random fake IDs, so this could only happen if the user calls [runComposition] with two
         * identical real IDs.
         */
        suspend fun registerSession(
            appWidgetId: Int,
            session: AppWidgetSession
        ): Nothing = suspendCancellableCoroutine { coroutine ->
            synchronized(UnmanagedSessionReceiver) {
                activeSessions[appWidgetId]?.coroutine?.resumeWithException(
                    IllegalStateException("Another session for $appWidgetId has started")
                )
                activeSessions[appWidgetId] = Registration(session, coroutine)
            }
            coroutine.invokeOnCancellation {
                synchronized(UnmanagedSessionReceiver) {
                    activeSessions.remove(appWidgetId)
                }
            }
        }

        fun getSession(appWidgetId: Int): AppWidgetSession? =
            synchronized(UnmanagedSessionReceiver) {
                activeSessions[appWidgetId]?.session
            }
    }
}
