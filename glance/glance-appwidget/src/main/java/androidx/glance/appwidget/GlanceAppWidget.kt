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
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.action.ActionTrampolineActivity
import androidx.glance.appwidget.action.InvisibleActionTrampolineActivity
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.session.GlanceSessionManager
import androidx.glance.session.SessionManager
import androidx.glance.session.SessionManagerScope
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException

/**
 * Object handling the composition and the communication with [AppWidgetManager].
 *
 * The UI is defined by calling [provideContent] from within [provideGlance]. When the widget is
 * requested, the composition is run and translated into a [RemoteViews] which is then sent to the
 * [AppWidgetManager].
 *
 * @param errorUiLayout Used by [onCompositionError]. When [onCompositionError] is called, it will,
 *   unless overridden, update the appwidget to display error UI using this layout resource ID,
 *   unless [errorUiLayout] is 0, in which case the error will be rethrown. If [onCompositionError]
 *   is overridden, [errorUiLayout] will not be read..
 */
abstract class GlanceAppWidget(
    @LayoutRes internal open val errorUiLayout: Int = R.layout.glance_error_layout,
) {
    @RestrictTo(Scope.LIBRARY_GROUP)
    protected open fun getSessionManager(context: Context): SessionManager = GlanceSessionManager

    /**
     * Override this function to provide the Glance Composable.
     *
     * This is a good place to load any data needed to render the Composable. Use [provideContent]
     * to provide the Composable once the data is ready.
     *
     * [provideGlance] is run in the background as a [androidx.work.CoroutineWorker] in response to
     * calls to [update] and [updateAll], as well as requests from the Launcher. Before
     * `provideContent` is called, `provideGlance` is subject to the typical
     * [androidx.work.WorkManager] time limit (currently ten minutes). After `provideContent` is
     * called, the composition continues to run and recompose for about 45 seconds. When UI
     * interactions or update requests are received, additional time is added to process these
     * requests.
     *
     * Note: [update] and [updateAll] do not restart `provideGlance` if it is already running. As a
     * result, you should load initial data before calling `provideContent`, and then observe your
     * sources of data within the composition (e.g. [androidx.compose.runtime.collectAsState]). This
     * ensures that your widget will continue to update while the composition is active. When you
     * update your data source from elsewhere in the app, make sure to call `update` in case a
     * Worker for this widget is not currently running.
     *
     * @sample androidx.glance.appwidget.samples.provideGlanceSample
     * @sample androidx.glance.appwidget.samples.provideGlancePeriodicWorkSample
     */
    abstract suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    )

    /**
     * Override this function to provide a Glance Composable that will be used when running this
     * widget in preview mode. Use [provideContent] to provide the composable once the data is
     * ready.
     *
     * In order to generate and publish the previews for a provider, use [setWidgetPreviews]. You
     * can use [composeForPreview] to generate a [RemoteViews] from this Composable without
     * publishing it.
     *
     * The given [widgetCategory] value will be one of
     * [AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN],
     * [AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD], or
     * [AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX], or some combination of all three. This
     * indicates what kind of widget host this preview can be used for. [widgetCategory] corresponds
     * to the categories passed to [setWidgetPreviews].
     *
     * @sample androidx.glance.appwidget.samples.providePreviewSample
     * @see AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
     */
    open suspend fun providePreview(context: Context, widgetCategory: Int) {}

    /** Defines the handling of sizes. */
    open val sizeMode: SizeMode = SizeMode.Single

    /** Defines handling of sizes for previews. */
    open val previewSizeMode: PreviewSizeMode = SizeMode.Single

    /** Data store for widget data specific to the view. */
    open val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    /**
     * Method called by the framework when an App Widget has been removed from its host.
     *
     * When the method returns, the state associated with the [glanceId] will be deleted.
     */
    open suspend fun onDelete(context: Context, glanceId: GlanceId) {}

    /** Run the composition in [provideGlance] and send the result to [AppWidgetManager]. */
    suspend fun update(context: Context, id: GlanceId) {
        require(id is AppWidgetId && id.isRealId) { "Invalid Glance ID" }
        update(context, id.appWidgetId)
    }

    /**
     * Calls [onDelete], then clear local data associated with the [appWidgetId].
     *
     * This is meant to be called when the App Widget instance has been deleted from the host.
     */
    internal suspend fun deleted(context: Context, appWidgetId: Int) {
        val glanceId = AppWidgetId(appWidgetId)
        getSessionManager(context).runWithLock { closeSession(glanceId.toSessionKey()) }
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
            LayoutConfiguration.delete(context, glanceId)
        }
    }

    /** Internal version of [update], to be used by the broadcast receiver directly. */
    internal suspend fun update(
        context: Context,
        appWidgetId: Int,
        options: Bundle? = null,
    ) {
        Tracing.beginGlanceAppWidgetUpdate()
        val glanceId = AppWidgetId(appWidgetId)
        getSessionManager(context).runWithLock {
            if (!isSessionRunning(context, glanceId.toSessionKey())) {
                startSession(context, createAppWidgetSession(context, glanceId, options))
                return@runWithLock
            }
            val session = getSession(glanceId.toSessionKey()) as AppWidgetSession
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
        getSessionManager(context).getOrCreateAppWidgetSession(context, glanceId, options) { session
            ->
            session.runLambda(actionKey)
        }
    }

    /** Internal method called when a resize event is detected. */
    internal suspend fun resize(context: Context, appWidgetId: Int, options: Bundle) {
        // Note, on Android S, if the mode is `Responsive`, then all the sizes are specified from
        // the start and we don't need to update the AppWidget when the size changes.
        if (
            sizeMode is SizeMode.Single ||
                (Build.VERSION.SDK_INT > Build.VERSION_CODES.S && sizeMode is SizeMode.Responsive)
        ) {
            return
        }
        val glanceId = AppWidgetId(appWidgetId)
        getSessionManager(context).getOrCreateAppWidgetSession(context, glanceId, options) { session
            ->
            session.updateAppWidgetOptions(options)
        }
    }

    /**
     * A callback invoked when the [AppWidgetSession] encounters an exception. At this point, the
     * session will be closed down. The default implementation of this method creates a
     * [RemoteViews] from [errorUiLayout] and sets this as the widget's content.
     *
     * This method should be overridden if you want to log the error, create a custom error layout,
     * or attempt to recover from or ignore the error by updating the widget's view state and then
     * restarting composition.
     *
     * @param context Context.
     * @param glanceId The [GlanceId] of the widget experiencing the error.
     * @param appWidgetId The appWidgetId of the widget experiencing the error. This is provided as
     *   a convenience in addition to [GlanceId].
     * @param throwable The exception that was caught by [AppWidgetSession]
     */
    @Suppress("GenericException")
    @Throws(Throwable::class)
    open fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable
    ) {
        if (errorUiLayout == 0) {
            throw throwable // Maintains consistency with Glance 1.0 behavior.
        } else {
            val rv =
                RemoteViews(
                    context.packageName,
                    errorUiLayout
                ) // default impl: inflate the error layout
            AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, rv)
        }
    }

    private suspend fun SessionManager.getOrCreateAppWidgetSession(
        context: Context,
        glanceId: AppWidgetId,
        options: Bundle? = null,
        block: suspend SessionManagerScope.(AppWidgetSession) -> Unit
    ) = runWithLock {
        if (!isSessionRunning(context, glanceId.toSessionKey())) {
            startSession(context, createAppWidgetSession(context, glanceId, options))
        }
        val session = getSession(glanceId.toSessionKey()) as AppWidgetSession
        block(session)
    }

    /**
     * Override this function to specify the components that will be used for actions and
     * RemoteViewsService. All of the components must run in the same process.
     *
     * If null, then the default components will be used.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    open fun getComponents(context: Context): GlanceComponents? = null

    @RestrictTo(Scope.LIBRARY_GROUP)
    protected open fun createAppWidgetSession(
        context: Context,
        id: AppWidgetId,
        options: Bundle? = null
    ) = AppWidgetSession(this@GlanceAppWidget, id, options)
}

@RestrictTo(Scope.LIBRARY_GROUP) data class AppWidgetId(val appWidgetId: Int) : GlanceId

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
 * Provides [content] to the Glance host, suspending until the Glance session is shut down.
 *
 * If this function is called concurrently with itself, the previous call will throw
 * [CancellationException] and the new content will replace it. This function should only be called
 * from [GlanceAppWidget.provideGlance].
 *
 * TODO: make this a protected member once b/206013293 is fixed.
 */
suspend fun GlanceAppWidget.provideContent(
    content: @Composable @GlanceComposable () -> Unit
): Nothing {
    coroutineContext[ContentReceiver]?.provideContent(content)
        ?: error(
            "provideContent requires a ContentReceiver and should only be called from " +
                "GlanceAppWidget.provideGlance"
        )
}

/**
 * Specifies which components will be used as targets for action trampolines, RunCallback actions,
 * and RemoteViewsService when creating RemoteViews. These components must all run in the same
 * process.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
class GlanceComponents(
    val actionTrampolineActivity: ComponentName,
    val invisibleActionTrampolineActivity: ComponentName,
    val actionCallbackBroadcastReceiver: ComponentName,
    val remoteViewsService: ComponentName,
) {
    companion object {
        /** The default components used for GlanceAppWidget. */
        fun getDefault(context: Context) =
            GlanceComponents(
                actionTrampolineActivity =
                    ComponentName(context, ActionTrampolineActivity::class.java),
                invisibleActionTrampolineActivity =
                    ComponentName(context, InvisibleActionTrampolineActivity::class.java),
                actionCallbackBroadcastReceiver =
                    ComponentName(context, ActionCallbackBroadcastReceiver::class.java),
                remoteViewsService = ComponentName(context, GlanceRemoteViewsService::class.java),
            )
    }
}
