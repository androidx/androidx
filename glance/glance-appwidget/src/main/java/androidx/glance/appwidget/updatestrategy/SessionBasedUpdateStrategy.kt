/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.appwidget.updatestrategy

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.DeprecatedSinceApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.AppWidgetSession
import androidx.glance.appwidget.AsyncRequestWorker
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.goAsync
import androidx.glance.appwidget.isGoAsyncBroken
import androidx.glance.appwidget.toSessionKey
import androidx.glance.session.SessionManager
import androidx.glance.session.SessionManagerScope

/** An [UpdateStrategy] implementation backed by a [SessionManager] and WorkManager. */
@OptIn(ExperimentalGlanceApi::class)
@RestrictTo(Scope.LIBRARY_GROUP)
public class SessionBasedUpdateStrategy(
    private val sessionManagerProvider: (Context) -> SessionManager
) : UpdateStrategy() {
    /** Creates a [SessionBasedUpdateStrategy] backed by a fixed [sessionManager]. */
    public constructor(sessionManager: SessionManager) : this({ sessionManager })

    /** Returns the [SessionManager] for [context]. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public fun getSessionManager(context: Context): SessionManager = sessionManagerProvider(context)

    /** Starts a session for [glanceId], or triggers an update if a session is already running. */
    override suspend fun handleUpdate(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle?,
    ) {
        getOrCreateAppWidgetSession(context, widget, glanceId, options) { session, wasRunning ->
            if (wasRunning) session.updateGlance()
        }
    }

    /** Starts a session for [glanceId] or updates the running session with new [options]. */
    override suspend fun handleResize(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle,
    ) {
        getOrCreateAppWidgetSession(context, widget, glanceId, options) { session, _ ->
            session.updateAppWidgetOptions(options)
        }
    }

    /** Closes any active session for [glanceId]. */
    override suspend fun handleDelete(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    ) {
        getSessionManager(context).runWithLock { closeSession(glanceId.toSessionKey()) }
    }

    /** Starts a session for [glanceId] if needed and runs the lambda for [actionKey]. */
    override suspend fun handleLambda(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        actionKey: String,
        options: Bundle?,
    ) {
        getOrCreateAppWidgetSession(context, widget, glanceId, options) { session, _ ->
            session.runLambda(actionKey)
        }
    }

    /** Starts a session for [glanceId] if needed and waits for list items to load. */
    @DeprecatedSinceApi(
        api = Build.VERSION_CODES.S_V2,
        message = "Only used by GlanceRemoteViewsService on API 31 (S) and below.",
    )
    override suspend fun handleLoadListData(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    ) {
        val job =
            getOrCreateAppWidgetSession(context, widget, glanceId, options = null) {
                session,
                wasRunning ->
                // If session is already running, data must have already been loaded into the store
                // during composition.
                if (wasRunning) return@getOrCreateAppWidgetSession null
                session.waitForReady()
            }
        job?.join()
    }

    /**
     * Dispatches [request] using [BroadcastReceiver.goAsync] or [AsyncRequestWorker].
     *
     * Enqueues an [AsyncRequestWorker] when the owning [GlanceAppWidgetReceiver] requests it via
     * [GlanceAppWidgetReceiver.shouldLaunchAsyncRequestWorker], or when `goAsync` is broken on the
     * device.
     */
    override fun handleBroadcast(context: Context, request: BroadcastRequest) {
        // Only the widget's GlanceAppWidgetReceiver honors shouldLaunchAsyncRequestWorker;
        // ActionCallbackBroadcastReceiver uses goAsync unless goAsync is broken.
        val widgetOwner = request.widgetOwner
        val shouldUseAsyncWorker =
            widgetOwner != null &&
                request.receiver === widgetOwner &&
                widgetOwner.requestsAsyncRequestWorker()
        if (shouldUseAsyncWorker || isGoAsyncBroken() || request.receiver == null) {
            AsyncRequestWorker.launch(context, request)
        } else {
            request.receiver.goAsync(request.coroutineContext) { request.execute(context) }
        }
    }

    /** Runs [block] on the active [AppWidgetSession] for [glanceId], starting one if needed. */
    private suspend fun <T> getOrCreateAppWidgetSession(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle? = null,
        block: suspend SessionManagerScope.(AppWidgetSession, Boolean) -> T,
    ): T =
        getSessionManager(context).runWithLock {
            val wasRunning = isSessionRunning(context, glanceId.toSessionKey())
            if (!wasRunning) {
                startSession(context, widget.createAppWidgetSession(context, glanceId, options))
            }
            val session = getSession(glanceId.toSessionKey()) as AppWidgetSession
            return@runWithLock block(session, wasRunning)
        }
}
