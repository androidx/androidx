/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.unit.DpSize
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalState
import androidx.glance.action.LambdaAction
import androidx.glance.session.Session
import androidx.glance.state.ConfigManager
import androidx.glance.state.GlanceState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel

/**
 * A session that composes UI for a single app widget.
 *
 * This class represents the lifecycle of composition for an app widget. This is started by
 * [GlanceAppWidget] in response to APPWIDGET_UPDATE broadcasts. The session is run in
 * [androidx.glance.session.SessionWorker] on a background thread (WorkManager). While it is active,
 * the session will continue to recompose in response to composition state changes or external
 * events (e.g. [AppWidgetSession.updateGlance]). If a session is already running, GlanceAppWidget
 * will trigger events on the session instead of starting a new one.
 *
 * @property widget the GlanceAppWidget that contains the composable for this session.
 * @property id identifies which widget will be updated when the UI is ready.
 * @property initialOptions options to be provided to the composition and determine sizing.
 * @property configManager used by the session to retrieve configuration state.
 */
internal class AppWidgetSession(
    private val widget: GlanceAppWidget,
    private val id: AppWidgetId,
    private val initialOptions: Bundle? = null,
    private val configManager: ConfigManager = GlanceState,
) : Session(id.toSessionKey()) {

    private companion object {
        const val TAG = "AppWidgetSession"
        const val DEBUG = false
    }

    private val glanceState = mutableStateOf<Any?>(null, neverEqualPolicy())
    private val options = mutableStateOf(Bundle(), neverEqualPolicy())
    private var lambdas = mapOf<String, List<LambdaAction>>()

    @VisibleForTesting
    internal var lastRemoteViews: RemoteViews? = null
        private set

    override fun createRootEmittable() = RemoteViewsRoot(MaxComposeTreeDepth)

    override fun provideGlance(context: Context): @Composable @GlanceComposable () -> Unit = {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalGlanceId provides id,
            LocalAppWidgetOptions provides options.value,
            LocalState provides glanceState.value,
        ) {
            val manager = remember { context.appWidgetManager }
            val minSize = remember {
                appWidgetMinSize(
                    context.resources.displayMetrics,
                    manager,
                    id.appWidgetId
                )
            }
            val configIsReady by produceState(false) {
                options.value = initialOptions ?: manager.getAppWidgetOptions(id.appWidgetId)
                widget.stateDefinition?.let {
                    glanceState.value =
                        configManager.getValue(context, it, key)
                }
                value = true
            }
            remember { widget.runGlance(context, id) }
                .collectAsState(null)
                .takeIf { configIsReady }
                ?.value?.let { ForEachSize(widget.sizeMode, minSize, it) }
                ?: IgnoreResult()
            // The following line ensures that when glanceState is updated, it increases the
            // Recomposer.changeCount and triggers processEmittableTree.
            SideEffect { glanceState.value }
        }
    }

    override suspend fun processEmittableTree(
        context: Context,
        root: EmittableWithChildren
    ): Boolean {
        if (root.shouldIgnoreResult()) return false
        root as RemoteViewsRoot
        val layoutConfig = LayoutConfiguration.load(context, id.appWidgetId)
        val appWidgetManager = context.appWidgetManager
        try {
            val receiver = requireNotNull(appWidgetManager.getAppWidgetInfo(id.appWidgetId)) {
                "No app widget info for ${id.appWidgetId}"
            }.provider
            normalizeCompositionTree(root)
            lambdas = root.updateLambdaActionKeys()
            val rv = translateComposition(
                context,
                id.appWidgetId,
                root,
                layoutConfig,
                layoutConfig.addLayout(root),
                DpSize.Unspecified,
                receiver
            )
            appWidgetManager.updateAppWidget(id.appWidgetId, rv)
            lastRemoteViews = rv
        } catch (ex: CancellationException) {
            // Nothing to do
        } catch (throwable: Throwable) {
            sendErrorLayoutIfPresent(context, throwable)
        } finally {
            layoutConfig.save()
            Tracing.endGlanceAppWidgetUpdate()
        }
        return true
    }

    override suspend fun onCompositionError(context: Context, throwable: Throwable) {
        sendErrorLayoutIfPresent(context, throwable)
    }

    override suspend fun processEvent(context: Context, event: Any) {
        when (event) {
            is UpdateGlanceState -> {
                if (DEBUG) Log.i(TAG, "Received UpdateGlanceState event for session($key)")
                val newGlanceState = widget.stateDefinition?.let {
                    configManager.getValue(context, it, key)
                }
                Snapshot.withMutableSnapshot {
                    glanceState.value = newGlanceState
                }
            }
            is UpdateAppWidgetOptions -> {
                if (DEBUG) {
                    Log.i(
                        TAG,
                        "Received UpdateAppWidgetOptions(${event.newOptions}) event" +
                            "for session($key)"
                    )
                }
                Snapshot.withMutableSnapshot {
                    options.value = event.newOptions
                }
            }
            is RunLambda -> {
                if (DEBUG) Log.i(TAG, "Received RunLambda(${event.key}) action for session($key)")
                Snapshot.withMutableSnapshot {
                    lambdas[event.key]?.forEach { it.block() }
                } ?: Log.w(TAG, "Triggering Action(${event.key}) for session($key) failed")
            }
            is WaitForReady -> event.resume.send(Unit)
            else -> {
                throw IllegalArgumentException(
                    "Sent unrecognized event type ${event.javaClass} to AppWidgetSession"
                )
            }
        }
    }

    suspend fun updateGlance() {
        sendEvent(UpdateGlanceState)
    }

    suspend fun updateAppWidgetOptions(newOptions: Bundle) {
        sendEvent(UpdateAppWidgetOptions(newOptions))
    }

    suspend fun runLambda(key: String) {
        sendEvent(RunLambda(key))
    }

    suspend fun waitForReady() {
        WaitForReady().let {
            sendEvent(it)
            it.resume.receive()
        }
    }

    private fun sendErrorLayoutIfPresent(context: Context, throwable: Throwable) {
        if (widget.errorUiLayout == 0) {
            throw throwable
        }
        logException(throwable)
        val rv = RemoteViews(context.packageName, widget.errorUiLayout)
        context.appWidgetManager.updateAppWidget(id.appWidgetId, rv)
        lastRemoteViews = rv
    }

    // Event types that this session supports.
    @VisibleForTesting
    internal object UpdateGlanceState
    @VisibleForTesting
    internal class UpdateAppWidgetOptions(val newOptions: Bundle)
    @VisibleForTesting
    internal class RunLambda(val key: String)
    @VisibleForTesting
    internal class WaitForReady(
        val resume: Channel<Unit> = Channel(Channel.CONFLATED)
    )
}
