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
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.DeprecatedSinceApi
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetTag
import androidx.glance.appwidget.GlanceRemoteViewsService
import androidx.glance.appwidget.appWidgetManager
import androidx.glance.appwidget.goAsync
import androidx.glance.appwidget.isGoAsyncBroken
import androidx.glance.appwidget.logException
import androidx.glance.appwidget.runImmediateComposition
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs widget composition in-process in the caller's [CoroutineContext] without a background worker
 * or session.
 */
@OptIn(ExperimentalGlanceApi::class)
internal object ImmediateUpdateStrategy : UpdateStrategy() {
    /** Composes [widget] once for [glanceId] and publishes the resulting [RemoteViews]. */
    override suspend fun handleUpdate(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle?,
    ) {
        composeAndPublish(context, widget, glanceId, options)
    }

    /**
     * Composes [widget] once with the updated [options] and publishes the resulting [RemoteViews].
     */
    override suspend fun handleResize(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle,
    ) {
        composeAndPublish(context, widget, glanceId, options)
    }

    /** Does nothing, as [ImmediateUpdateStrategy] keeps no active session for [glanceId]. */
    override suspend fun handleDelete(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    ) {}

    /**
     * Rehydrates [widget] composition, runs the lambda for [actionKey], and publishes the updated
     * [RemoteViews].
     */
    override suspend fun handleLambda(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        actionKey: String,
        options: Bundle?,
    ) {
        composeAndPublish(context, widget, glanceId, options, actionKey = actionKey)
    }

    /**
     * Composes [widget] once to populate in-memory list items for [GlanceRemoteViewsService]
     * without publishing.
     */
    @DeprecatedSinceApi(
        api = Build.VERSION_CODES.S_V2,
        message = "Only used by GlanceRemoteViewsService on API 31 (S) and below.",
    )
    override suspend fun handleLoadListData(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    ) {
        // Translating the composition stores the list items; there is no need to publish.
        compose(context, widget, glanceId, options = null)
    }

    /**
     * Runs [request] in its [BroadcastRequest.coroutineContext], holding the broadcast with
     * [BroadcastReceiver.goAsync] until it completes.
     */
    override fun handleBroadcast(context: Context, request: BroadcastRequest) {
        val receiver = request.receiver
        if (receiver != null && !isGoAsyncBroken()) {
            receiver.goAsync(request.coroutineContext) { request.execute(context) }
        } else {
            // goAsync is unavailable (no receiver, or broken on this device).
            CoroutineScope(request.coroutineContext).launch {
                try {
                    coroutineScope { request.execute(context) }
                } catch (e: Throwable) {
                    if (e !is CancellationException || e.cause != null) {
                        Log.e(GlanceAppWidgetTag, "BroadcastReceiver execution failed", e)
                    }
                }
            }
        }
    }

    /**
     * Composes [widget] for [glanceId] and pushes the resulting [RemoteViews] to
     * [android.appwidget.AppWidgetManager].
     */
    private suspend fun composeAndPublish(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle?,
        actionKey: String? = null,
    ) {
        val remoteViews = compose(context, widget, glanceId, options, actionKey) ?: return
        context.appWidgetManager.updateAppWidget(glanceId.appWidgetId, remoteViews)
    }

    /**
     * Runs the composition of [widget] until the first result (after running the lambda action with
     * [actionKey], if given).
     *
     * @return the composed [RemoteViews], or `null` if composition failed and
     *   [GlanceAppWidget.onCompositionError] handled the error without rethrowing.
     */
    private suspend fun compose(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle?,
        actionKey: String? = null,
    ): RemoteViews? {
        val appWidgetId = glanceId.appWidgetId
        val appWidgetManager = context.appWidgetManager
        return try {
            val receiver =
                requireNotNull(appWidgetManager.getAppWidgetInfo(appWidgetId)) {
                        "No app widget info for $appWidgetId"
                    }
                    .provider
            widget
                .runImmediateComposition(
                    context = context,
                    id = glanceId,
                    options = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId),
                    lambdaReceiver = receiver,
                    actionKey = actionKey,
                )
                .first()
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            logException(throwable)
            widget.onCompositionError(context, glanceId, appWidgetId, throwable)
            null
        }
    }
}
