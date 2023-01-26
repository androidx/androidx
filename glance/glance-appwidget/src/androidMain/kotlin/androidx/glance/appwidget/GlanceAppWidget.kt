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
import androidx.annotation.DoNotInline
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.glance.Applier
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.LocalState
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.session.SessionManager
import androidx.glance.state.GlanceState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Object handling the composition and the communication with [AppWidgetManager].
 *
 * The UI is defined by the [Content] composable function. Calling [update] will start
 * the composition and translate [Content] into a [RemoteViews] which is then sent to the
 * [AppWidgetManager].
 *
 * @param errorUiLayout If different from 0 and an error occurs within this GlanceAppWidget,
 * the App Widget is updated with an error UI using this layout resource ID.
 */
abstract class GlanceAppWidget(
    @LayoutRes
    internal val errorUiLayout: Int = R.layout.glance_error_layout,
) {
    /**
     * Override this function to provide the Glance Composable.
     *
     * This is a good place to load any data needed to render the Composable. Use
     * [provideContent] to provide the Composable once it is ready.
     *
     * TODO(b/239747024) make abstract once Content() is removed.
     */
    open suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    /**
     * Definition of the UI.
     * TODO(b/239747024) remove and update any usage to the new provideGlance API.
     */
    @Composable
    @GlanceComposable
    abstract fun Content()

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

    // TODO(b/239747024) remove once SessionManager is the default
    open val sessionManager: SessionManager? = null

    /**
     * Triggers the composition of [Content] and sends the result to the [AppWidgetManager].
     */
    suspend fun update(
        context: Context,
        glanceId: GlanceId
    ) {
        require(glanceId is AppWidgetId) {
            "The glanceId '$glanceId' is not a valid App Widget glance id"
        }
        update(context, AppWidgetManager.getInstance(context), glanceId.appWidgetId)
    }

    /**
     * Calls [onDelete], then clear local data associated with the [appWidgetId].
     *
     * This is meant to be called when the App Widget instance has been deleted from the host.
     */
    internal suspend fun deleted(context: Context, appWidgetId: Int) {
        val glanceId = AppWidgetId(appWidgetId)
        sessionManager?.closeSession(glanceId.toSessionKey())
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
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle? = null,
    ) {
        Tracing.beginGlanceAppWidgetUpdate()
        sessionManager?.let {
            val glanceId = AppWidgetId(appWidgetId)
            if (!it.isSessionRunning(context, glanceId.toSessionKey())) {
                it.startSession(context, AppWidgetSession(this, glanceId, options))
            } else {
                val session = it.getSession(glanceId.toSessionKey()) as AppWidgetSession
                session.updateGlance()
            }
            return
        }
        safeRun(context, appWidgetManager, appWidgetId) {
            val opts = options ?: appWidgetManager.getAppWidgetOptions(appWidgetId)!!
            val state = stateDefinition?.let {
                GlanceState.getValue(context, it, createUniqueRemoteUiName(appWidgetId))
            }
            appWidgetManager.updateAppWidget(
                appWidgetId,
                compose(context, appWidgetManager, appWidgetId, state, opts)
            )
        }
    }

    /**
     * Trigger an action to be run in the AppWidgetSession for this widget, starting the session if
     * necessary.
     */
    internal suspend fun triggerAction(
        context: Context,
        appWidgetId: Int,
        actionKey: String,
        options: Bundle? = null,
    ) {
        sessionManager?.let { manager ->
            val glanceId = AppWidgetId(appWidgetId)
            val session = if (!manager.isSessionRunning(context, glanceId.toSessionKey())) {
                AppWidgetSession(this, glanceId, options).also { session ->
                    manager.startSession(context, session)
                }
            } else {
                manager.getSession(glanceId.toSessionKey()) as AppWidgetSession
            }
            session.runLambda(actionKey)
        } ?: error(
            "GlanceAppWidget.triggerAction may only be used when a SessionManager is provided"
        )
    }

    /**
     * Internal method called when a resize event is detected.
     */
    internal suspend fun resize(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        options: Bundle
    ) {
        sessionManager?.let { manager ->
            val glanceId = AppWidgetId(appWidgetId)
            if (!manager.isSessionRunning(context, glanceId.toSessionKey())) {
                manager.startSession(context, AppWidgetSession(this, glanceId, options))
            } else {
                val session = manager.getSession(glanceId.toSessionKey()) as AppWidgetSession
                session.updateAppWidgetOptions(options)
            }
            return
        }
        // Note, on Android S, if the mode is `Responsive`, then all the sizes are specified from
        // the start and we don't need to update the AppWidget when the size changes.
        if (sizeMode is SizeMode.Exact ||
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && sizeMode is SizeMode.Responsive)
        ) {
            update(context, appWidgetManager, appWidgetId, options)
        }
    }

    // Trigger the composition of the View to create the RemoteViews.
    @VisibleForTesting
    internal suspend fun compose(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle
    ): RemoteViews {
        val layoutConfig = LayoutConfiguration.load(context, appWidgetId)
        return try {
            compose(context, appWidgetManager, appWidgetId, state, options, layoutConfig)
        } finally {
            layoutConfig.save()
        }
    }

    @VisibleForTesting
    internal suspend fun compose(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        layoutConfig: LayoutConfiguration,
    ): RemoteViews =
        when (val localSizeMode = this.sizeMode) {
            is SizeMode.Single -> {
                composeForSize(
                    context,
                    appWidgetId,
                    state,
                    options,
                    appWidgetMinSize(
                        context.resources.displayMetrics,
                        appWidgetManager,
                        appWidgetId
                    ),
                    layoutConfig,
                )
            }

            is SizeMode.Exact -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Impl.composeAllSizes(
                        this,
                        context,
                        appWidgetId,
                        state,
                        options,
                        options.extractAllSizes {
                            appWidgetMinSize(
                                context.resources.displayMetrics,
                                appWidgetManager,
                                appWidgetId
                            )
                        },
                        layoutConfig,
                    )
                } else {
                    composeExactMode(
                        context,
                        appWidgetManager,
                        appWidgetId,
                        state,
                        options,
                        layoutConfig,
                    )
                }
            }

            is SizeMode.Responsive -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Api31Impl.composeAllSizes(
                        this,
                        context,
                        appWidgetId,
                        state,
                        options,
                        localSizeMode.sizes,
                        layoutConfig,
                    )
                } else {
                    composeResponsiveMode(
                        context,
                        appWidgetId,
                        state,
                        options,
                        localSizeMode.sizes,
                        layoutConfig,
                    )
                }
            }
        }

    private suspend fun composeExactMode(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        layoutConfig: LayoutConfiguration,
    ) = coroutineScope {
        val views =
            options.extractOrientationSizes()
                .map { size ->
                    async {
                        composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
        combineLandscapeAndPortrait(views) ?: composeForSize(
            context,
            appWidgetId,
            state,
            options,
            appWidgetMinSize(context.resources.displayMetrics, appWidgetManager, appWidgetId),
            layoutConfig,
        )
    }

    // Combine the views, which should be landscape and portrait, in that order, if they are
    // available.
    private fun combineLandscapeAndPortrait(views: List<RemoteViews>): RemoteViews? =
        when (views.size) {
            2 -> RemoteViews(views[0], views[1])
            1 -> views[0]
            0 -> null
            else -> throw IllegalArgumentException("There must be between 0 and 2 views.")
        }

    private suspend fun composeResponsiveMode(
        context: Context,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        sizes: Set<DpSize>,
        layoutConfig: LayoutConfiguration,
    ) = coroutineScope {
        // Find the best view, emulating what Android S+ would do.
        val orderedSizes = sizes.sortedBySize()
        val smallestSize = orderedSizes[0]
        val views =
            options.extractOrientationSizes()
                .map { size ->
                    findBestSize(size, sizes) ?: smallestSize
                }
                .map { size ->
                    async {
                        composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
        combineLandscapeAndPortrait(views) ?: composeForSize(
            context,
            appWidgetId,
            state,
            options,
            smallestSize,
            layoutConfig,
        )
    }

    @VisibleForTesting
    internal suspend fun composeForSize(
        context: Context,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        size: DpSize,
        layoutConfig: LayoutConfiguration,
    ): RemoteViews = withContext(BroadcastFrameClock()) {
        // The maximum depth must be reduced if the compositions are combined
        val root = RemoteViewsRoot(maxDepth = MaxComposeTreeDepth)
        val applier = Applier(root)
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(applier, recomposer)
        val glanceId = AppWidgetId(appWidgetId)
        composition.setContent(context, glanceId, options, state, size)

        launch { recomposer.runRecomposeAndApplyChanges() }
        recomposer.close()
        recomposer.join()

        normalizeCompositionTree(root)

        translateComposition(
            context,
            appWidgetId,
            root,
            layoutConfig,
            layoutConfig.addLayout(root),
            size
        )
    }

    private fun Composition.setContent(
        context: Context,
        glanceId: AppWidgetId,
        options: Bundle,
        state: Any?,
        size: DpSize
    ) {
        setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalGlanceId provides glanceId,
                LocalAppWidgetOptions provides options,
                LocalState provides state,
                LocalSize provides size,
            ) { Content() }
        }
    }

    private companion object {
        /**
         * Maximum depth for a composition. Although there is no hard limit, this should avoid deep
         * recursions, which would create [RemoteViews] too large to be sent.
         */
        private const val MaxComposeTreeDepth = 50
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31Impl {
        @DoNotInline
        suspend fun composeAllSizes(
            glance: GlanceAppWidget,
            context: Context,
            appWidgetId: Int,
            state: Any?,
            options: Bundle,
            allSizes: Collection<DpSize>,
            layoutConfig: LayoutConfiguration
        ): RemoteViews = coroutineScope {
            val allViews =
                allSizes.map { size ->
                    async {
                        size.toSizeF() to glance.composeForSize(
                            context,
                            appWidgetId,
                            state,
                            options,
                            size,
                            layoutConfig,
                        )
                    }
                }.awaitAll()
            allViews.singleOrNull()?.second ?: RemoteViews(allViews.toMap())
        }
    }

    private suspend fun safeRun(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (ex: CancellationException) {
            // Nothing to do
        } catch (throwable: Throwable) {
            if (errorUiLayout == 0) {
                throw throwable
            }
            logException(throwable)
            val rv = RemoteViews(context.packageName, errorUiLayout)
            appWidgetManager.updateAppWidget(appWidgetId, rv)
        } finally {
            Tracing.endGlanceAppWidgetUpdate()
        }
    }

    /**
     * Creates a snapshot of the GlanceAppWidget content without running recomposition.
     * Useful to only generate once the composed RemoteViews instance.
     *
     * @see GlanceAppWidget.composeForSize
     * @see GlanceAppWidgetManager.requestPinGlanceAppWidget
     */
    internal fun snapshot(
        context: Context,
        appWidgetId: Int,
        state: Any?,
        options: Bundle,
        size: DpSize,
    ): RemoteViews {
        // The maximum depth must be reduced if the compositions are combined
        val root = RemoteViewsRoot(maxDepth = MaxComposeTreeDepth)
        val applier = Applier(root)
        val scope = CoroutineScope(Job() + Dispatchers.Main)
        val recomposer = Recomposer(scope.coroutineContext)
        val composition = Composition(applier, recomposer)
        val glanceId = AppWidgetId(appWidgetId)

        composition.setContent(context, glanceId, options, state, size)
        normalizeCompositionTree(root)
        return translateComposition(context, appWidgetId, root, null, 0, size)
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