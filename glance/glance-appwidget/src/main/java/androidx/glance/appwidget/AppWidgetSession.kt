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

import android.content.ComponentName
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
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.MutableStateFlow

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
 * @property configManager used by the session to retrieve configuration state.
 * @property lambdaReceiver the BroadcastReceiver that will receive lambda action broadcasts.
 * @property sizeMode optional override for the widget's specified SizeMode.
 * @property shouldPublish if true, we will publish RemoteViews with
 * [android.appwidget.AppWidgetManager.updateAppWidget]. The [id] must be valid
 * ([AppWidgetId.isRealId]) in that case.
 * @param initialOptions options to be provided to the composition and determine sizing.
 * @param initialGlanceState initial value of Glance state
 */
internal class AppWidgetSession(
    private val widget: GlanceAppWidget,
    private val id: AppWidgetId,
    initialOptions: Bundle? = null,
    private val configManager: ConfigManager = GlanceState,
    private val lambdaReceiver: ComponentName? = null,
    private val sizeMode: SizeMode = widget.sizeMode,
    private val shouldPublish: Boolean = true,
    initialGlanceState: Any? = null,
) : Session(id.toSessionKey()) {
    init {
        if (id.isFakeId) {
            require(lambdaReceiver != null) {
                "If the AppWidgetSession is not created for a bound widget, you must provide " +
                    "a lambda action receiver"
            }
            require(!shouldPublish) {
                "Cannot publish RemoteViews to AppWidgetManager since we are not running for a " +
                    "bound widget"
            }
        }
    }

    private companion object {
        const val TAG = "AppWidgetSession"
        const val DEBUG = false
    }

    private var glanceState by mutableStateOf(initialGlanceState, neverEqualPolicy())
    private var options by mutableStateOf(initialOptions, neverEqualPolicy())
    private var lambdas = mapOf<String, List<LambdaAction>>()

    internal val lastRemoteViews = MutableStateFlow<RemoteViews?>(null)

    override fun createRootEmittable() = RemoteViewsRoot(MaxComposeTreeDepth)

    override fun provideGlance(context: Context): @Composable @GlanceComposable () -> Unit = {
        CompositionLocalProvider(
            LocalContext provides context,
            LocalGlanceId provides id,
            LocalAppWidgetOptions provides (options ?: Bundle.EMPTY),
            LocalState provides glanceState,
        ) {
            var minSize by remember { mutableStateOf(DpSize.Zero) }
            val configIsReady by produceState(false) {
                Snapshot.withMutableSnapshot {
                    if (id.isRealId) {
                        // Only get sizing info from app widget manager if we are composing for
                        // a real bound widget.
                        val manager = context.appWidgetManager
                        minSize = appWidgetMinSize(
                            context.resources.displayMetrics,
                            manager,
                            id.appWidgetId
                        )
                        if (options == null) {
                            options = manager.getAppWidgetOptions(id.appWidgetId)
                        }
                    }
                    // Only get a Glance state value if we did not receive an initial value.
                    widget.stateDefinition
                        ?.takeIf { glanceState == null }
                        ?.let { stateDefinition ->
                            glanceState =
                                configManager.getValue(context, stateDefinition, key)
                        }
                    value = true
                }
            }
            if (configIsReady) {
                remember { widget.runGlance(context, id) }
                    .collectAsState(null)
                    .value?.let { ForEachSize(sizeMode, minSize, it) }
                    ?: IgnoreResult()
            } else {
                IgnoreResult()
            }
            // The following line ensures that when glanceState is updated, it increases the
            // Recomposer.changeCount and triggers processEmittableTree.
            SideEffect { glanceState }
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
            val receiver = lambdaReceiver
                ?: requireNotNull(appWidgetManager.getAppWidgetInfo(id.appWidgetId)) {
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
            if (shouldPublish) {
                appWidgetManager.updateAppWidget(id.appWidgetId, rv)
            }
            lastRemoteViews.value = rv
        } catch (ex: CancellationException) {
            // Nothing to do
        } catch (throwable: Throwable) {
            notifyWidgetOfError(context, throwable)
        } finally {
            layoutConfig.save()
            Tracing.endGlanceAppWidgetUpdate()
        }
        return true
    }

    override suspend fun onCompositionError(context: Context, throwable: Throwable) {
        notifyWidgetOfError(context, throwable)
    }

    override suspend fun processEvent(context: Context, event: Any) {
        when (event) {
            is UpdateGlanceState -> {
                if (DEBUG) Log.i(TAG, "Received UpdateGlanceState event for session($key)")
                val newGlanceState = widget.stateDefinition?.let {
                    configManager.getValue(context, it, key)
                }
                Snapshot.withMutableSnapshot {
                    glanceState = newGlanceState
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
                    options = event.newOptions
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

    private fun notifyWidgetOfError(context: Context, throwable: Throwable) {
        logException(throwable)
        if (shouldPublish) {
            widget.onCompositionError(
                context,
                glanceId = id,
                appWidgetId = id.appWidgetId,
                throwable = throwable
            )
        } else {
            throw throwable // rethrow the error if we can't display it
        }
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
