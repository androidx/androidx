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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.session.GlanceSessionManager
import androidx.glance.session.SessionManager
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException

/**
 * Object handling the composition and the communication with [AppWidgetManager].
 *
 * The UI is defined by calling [provideContent] from within [provideGlance].
 * When the widget is requested, the composition is run and translated into a [RemoteViews] which is
 * then sent to the [AppWidgetManager].
 *
 * @param errorUiLayout If different from 0 and an error occurs within this GlanceAppWidget,
 * the App Widget is updated with an error UI using this layout resource ID.
 */
abstract class GlanceAppWidget(
    @LayoutRes
    internal val errorUiLayout: Int = R.layout.glance_error_layout,
) {
    private val sessionManager: SessionManager = GlanceSessionManager

    /**
     * Override this function to provide the Glance Composable.
     *
     * This is a good place to load any data needed to render the Composable. Use
     * [provideContent] to provide the Composable once the data is ready.
     *
     * @sample androidx.glance.appwidget.samples.provideGlanceSample
     */
    abstract suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    )

    /**
     * Defines the handling of sizes.
     */
    open val sizeMode: SizeMode = SizeMode.Single

    /**
     * Data store for widget data specific to the view.
     */
    open val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    /**
     * Method called by the framework when an App Widget has been removed from its host.
     *
     * When the method returns, the state associated with the [glanceId] will be deleted.
     */
    open suspend fun onDelete(context: Context, glanceId: GlanceId) {}

    /**
     * Run the composition in [provideGlance] and send the result to [AppWidgetManager].
     */
    suspend fun update(
        context: Context,
        id: GlanceId
    ) {
        require(id is AppWidgetId)
        update(context, id.appWidgetId)
    }

    /**
     * Calls [onDelete], then clear local data associated with the [appWidgetId].
     *
     * This is meant to be called when the App Widget instance has been deleted from the host.
     */
    internal suspend fun deleted(context: Context, appWidgetId: Int) {
        val glanceId = AppWidgetId(appWidgetId)
        sessionManager.closeSession(glanceId.toSessionKey())
        try {
            onDelete(context, glanceId)
        } catch (cancelled: CancellationException) {
            // Nothing to do here
        } catch (t: Throwable) {
            Log.e(GlanceAppWidgetTag, "Error in user-provided deletion callback", t)
        } finally {
            stateDefinition?.let {
                GlanceState.deleteStore(context, it, createUniqueRemoteUiName(appWidgetId))
            }
        }
    }

    /**
     * Internal version of [update], to be used by the broadcast receiver directly.
     */
    internal suspend fun update(
        context: Context,
        appWidgetId: Int,
        options: Bundle? = null,
    ) {
        Tracing.beginGlanceAppWidgetUpdate()
        val glanceId = AppWidgetId(appWidgetId)
        if (!sessionManager.isSessionRunning(context, glanceId.toSessionKey())) {
            sessionManager.startSession(context, AppWidgetSession(this, glanceId, options))
        } else {
            val session = sessionManager.getSession(glanceId.toSessionKey()) as AppWidgetSession
            session.updateGlance()
        }
    }

    /**
     * Trigger an action to be run in the [AppWidgetSession] for this widget, starting the session
     * if necessary.
     */
    internal suspend fun triggerAction(
        context: Context,
        appWidgetId: Int,
        actionKey: String,
        options: Bundle? = null,
    ) {
        val glanceId = AppWidgetId(appWidgetId)
        val session = if (!sessionManager.isSessionRunning(context, glanceId.toSessionKey())) {
            AppWidgetSession(this, glanceId, options).also { session ->
                sessionManager.startSession(context, session)
            }
        } else {
            sessionManager.getSession(glanceId.toSessionKey()) as AppWidgetSession
        }
        session.runLambda(actionKey)
    }

    /**
     * Internal method called when a resize event is detected.
     */
    internal suspend fun resize(
        context: Context,
        appWidgetId: Int,
        options: Bundle
    ) {
        // Note, on Android S, if the mode is `Responsive`, then all the sizes are specified from
        // the start and we don't need to update the AppWidget when the size changes.
        if (sizeMode is SizeMode.Single ||
            (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && sizeMode is SizeMode.Responsive)
        ) {
            return
        }
        val glanceId = AppWidgetId(appWidgetId)
        if (!sessionManager.isSessionRunning(context, glanceId.toSessionKey())) {
            sessionManager.startSession(context, AppWidgetSession(this, glanceId, options))
        } else {
            val session = sessionManager.getSession(glanceId.toSessionKey()) as AppWidgetSession
            session.updateAppWidgetOptions(options)
        }
    }
}

internal data class AppWidgetId(val appWidgetId: Int) : GlanceId

/** Update all App Widgets managed by the [GlanceAppWidget] class. */
suspend fun GlanceAppWidget.updateAll(@Suppress("ContextFirst") context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(javaClass).forEach { update(context, it) }
}

/**
 * Update all App Widgets managed by the [GlanceAppWidget] class, if they fulfill some condition.
 */
suspend inline fun <reified State> GlanceAppWidget.updateIf(
    @Suppress("ContextFirst") context: Context,
    predicate: (State) -> Boolean
) {
    val stateDef = stateDefinition
    requireNotNull(stateDef) { "GlanceAppWidget.updateIf cannot be used if no state is defined." }
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(javaClass).forEach { glanceId ->
        val state = getAppWidgetState(context, stateDef, glanceId) as State
        if (predicate(state)) update(context, glanceId)
    }
}

/**
 * Provides [content] to the Glance host, suspending until the Glance session is
 * shut down.
 *
 * If this function is called concurrently with itself, the previous call will throw
 * [CancellationException] and the new content will replace it. This function should only be called
 * from [GlanceAppWidget.provideGlance].
 *
 *
 * TODO: make this a protected member once b/206013293 is fixed.
 */
suspend fun GlanceAppWidget.provideContent(
    content: @Composable @GlanceComposable () -> Unit
): Nothing {
    coroutineContext[ContentReceiver]?.provideContent(content)
        ?: error("provideContent requires a ContentReceiver and should only be called from " +
            "GlanceAppWidget.provideGlance")
}
