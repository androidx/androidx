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
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceRemoteViewsService
import androidx.glance.appwidget.action.RunCallbackAction
import androidx.glance.session.GlanceSessionManager
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/**
 * Controls how a [GlanceAppWidget] runs its composition.
 *
 * @see GlanceAppWidget.updateStrategy
 */
@ExperimentalGlanceApi
public abstract class UpdateStrategy @RestrictTo(Scope.LIBRARY_GROUP) constructor() {
    /**
     * Handles an update request for [glanceId]. An update runs [GlanceAppWidget.provideGlance] and
     * sends the resulting UI to [android.appwidget.AppWidgetManager].
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract suspend fun handleUpdate(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle? = null,
    )

    /** Handles a resize request for [glanceId] with [options]. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract suspend fun handleResize(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        options: Bundle,
    )

    /** Handles deletion of [glanceId]. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract suspend fun handleDelete(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    )

    /** Handles running the lambda action with [actionKey] for [glanceId]. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract suspend fun handleLambda(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
        actionKey: String,
        options: Bundle? = null,
    )

    /** Loads list items into memory for [GlanceRemoteViewsService] on API 31 (S) and below. */
    @DeprecatedSinceApi(
        api = Build.VERSION_CODES.S_V2,
        message = "Only used by GlanceRemoteViewsService on API 31 (S) and below.",
    )
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract suspend fun handleLoadListData(
        context: Context,
        widget: GlanceAppWidget,
        glanceId: AppWidgetId,
    )

    /**
     * Handles a broadcast for a widget.
     *
     * @param context [Context] passed to [BroadcastReceiver.onReceive]
     * @param request [BroadcastRequest] describing the broadcast to handle
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public abstract fun handleBroadcast(context: Context, request: BroadcastRequest)

    public companion object {
        /**
         * Runs the composition in a WorkManager worker. The composition keeps running for a short
         * time to handle state changes and further requests.
         */
        internal val SESSION: UpdateStrategy = SessionBasedUpdateStrategy(GlanceSessionManager)

        /**
         * Runs the composition in the current process without WorkManager. Each request composes
         * once, pushes the result, and ends the composition. Broadcasts are held with `goAsync`
         * until the result is pushed.
         */
        @JvmField public val IMMEDIATE: UpdateStrategy = ImmediateUpdateStrategy

        /**
         * The default strategy. Runs the composition in a WorkManager worker and keeps it alive for
         * a short time to handle state changes and further requests.
         */
        @JvmField public val DEFAULT: UpdateStrategy = SESSION
    }
}

/**
 * Describes a broadcast received by a widget or action receiver.
 *
 * @param receiver the [BroadcastReceiver] that received the broadcast, used for `goAsync`; null if
 *   unavailable
 * @param widgetOwner the [GlanceAppWidgetReceiver] of the widget; null if unknown
 * @param coroutineContext where the request runs; defaults to the [widgetOwner]'s
 *   [GlanceAppWidgetReceiver.coroutineContext]
 * @see UpdateStrategy.handleBroadcast
 */
@OptIn(ExperimentalGlanceApi::class)
@RestrictTo(Scope.LIBRARY_GROUP)
public sealed class BroadcastRequest(
    internal val receiver: BroadcastReceiver?,
    internal val widgetOwner: GlanceAppWidgetReceiver?,
    internal val coroutineContext: CoroutineContext =
        widgetOwner?.coroutineContext ?: Dispatchers.Default,
) {
    /** Runs the action for this broadcast. */
    internal abstract suspend fun execute(context: Context)

    /**
     * Requests an update of [appWidgetIds]. Received only by [GlanceAppWidgetReceiver].
     *
     * @param widgetReceiver the [GlanceAppWidgetReceiver] that received the broadcast
     * @param appWidgetIds the widgets to update
     */
    internal class Update(val widgetReceiver: GlanceAppWidgetReceiver, val appWidgetIds: IntArray) :
        BroadcastRequest(receiver = widgetReceiver, widgetOwner = widgetReceiver) {
        override suspend fun execute(context: Context) {
            widgetReceiver.doUpdate(context, appWidgetIds)
        }
    }

    /**
     * Reports new options, such as a new size, for [appWidgetId]. Received only by
     * [GlanceAppWidgetReceiver].
     *
     * @param widgetReceiver the [GlanceAppWidgetReceiver] that received the broadcast
     * @param appWidgetId the widget whose options changed
     * @param newOptions the new options
     */
    internal class OptionsChanged(
        val widgetReceiver: GlanceAppWidgetReceiver,
        val appWidgetId: Int,
        val newOptions: Bundle,
    ) : BroadcastRequest(receiver = widgetReceiver, widgetOwner = widgetReceiver) {
        override suspend fun execute(context: Context) {
            widgetReceiver.doOptionsChanged(context, appWidgetId, newOptions)
        }
    }

    /**
     * Reports that [appWidgetIds] were removed from the host. Received only by
     * [GlanceAppWidgetReceiver].
     *
     * @param widgetReceiver the [GlanceAppWidgetReceiver] that received the broadcast
     * @param appWidgetIds the deleted widgets
     */
    internal class Delete(val widgetReceiver: GlanceAppWidgetReceiver, val appWidgetIds: IntArray) :
        BroadcastRequest(receiver = widgetReceiver, widgetOwner = widgetReceiver) {
        override suspend fun execute(context: Context) {
            widgetReceiver.doDelete(context, appWidgetIds)
        }
    }

    /**
     * Runs the lambda action with [actionKey] after a click. Received only by
     * [GlanceAppWidgetReceiver].
     *
     * @param widgetReceiver the [GlanceAppWidgetReceiver] that received the broadcast
     * @param appWidgetId the widget that was clicked
     * @param actionKey the key of the lambda to run
     */
    internal class Lambda(
        val widgetReceiver: GlanceAppWidgetReceiver,
        val appWidgetId: Int,
        val actionKey: String,
    ) : BroadcastRequest(receiver = widgetReceiver, widgetOwner = widgetReceiver) {
        override suspend fun execute(context: Context) {
            widgetReceiver.doLambda(context, appWidgetId, actionKey)
        }
    }

    /**
     * Runs an [androidx.glance.appwidget.action.ActionCallback] after a click. Received only by
     * [androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver].
     *
     * @param receiver the receiver that received the broadcast; null if unavailable
     * @param widgetOwner the [GlanceAppWidgetReceiver] of the clicked widget; null if unknown
     * @param className the class name of the callback to run
     * @param glanceId the widget that was clicked
     * @param parameters the parameters passed to the callback
     */
    internal class RunCallback(
        receiver: BroadcastReceiver?,
        widgetOwner: GlanceAppWidgetReceiver?,
        val className: String,
        val glanceId: AppWidgetId,
        val parameters: ActionParameters,
    ) : BroadcastRequest(receiver = receiver, widgetOwner = widgetOwner) {
        override suspend fun execute(context: Context) {
            RunCallbackAction.run(context, className, glanceId, parameters)
        }
    }
}
