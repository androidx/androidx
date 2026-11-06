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
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.VisibleForTesting
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.action.LambdaActionBroadcasts
import androidx.glance.appwidget.updatestrategy.BroadcastRequest
import androidx.glance.appwidget.updatestrategy.UpdateStrategy
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * [AppWidgetProvider] using the given [GlanceAppWidget] to generate the remote views when needed.
 *
 * This should typically used as:
 *
 *     class MyGlanceAppWidgetProvider : GlanceAppWidgetProvider() {
 *       override val glanceAppWidget: GlanceAppWidget()
 *         get() = MyGlanceAppWidget()
 *     }
 *
 * Note: If you override any of the [AppWidgetProvider] methods, ensure you call their super-class
 * implementation.
 *
 * Important: if you override any of the methods of this class, you must call the super
 * implementation, and you must not call [AppWidgetProvider.goAsync], as it will be called by the
 * super implementation. This means your processing time must be short.
 */
@OptIn(ExperimentalGlanceApi::class)
public abstract class GlanceAppWidgetReceiver : AppWidgetProvider() {

    public companion object {
        private const val TAG = "GlanceAppWidgetReceiver"

        /**
         * Action for a broadcast intent that will try to update all instances of a Glance App
         * Widget for debugging.
         * <pre>
         * adb shell am broadcast -a androidx.glance.appwidget.action.DEBUG_UPDATE -n APP/COMPONENT
         * </pre>
         * where APP/COMPONENT is the manifest component for the GlanceAppWidgetReceiver subclass.
         * This only works if the Receiver is exported (or the target device has adb running as
         * root), and has androidx.glance.appwidget.DEBUG_UPDATE in its intent-filter. This should
         * only be done for debug builds and disabled for release.
         */
        public const val ACTION_DEBUG_UPDATE: String =
            "androidx.glance.appwidget.action.DEBUG_UPDATE"
    }

    /**
     * Instance of the [GlanceAppWidget] to use to generate the App Widget and send it to the
     * [AppWidgetManager]
     */
    public abstract val glanceAppWidget: GlanceAppWidget

    /**
     * Override [coroutineContext] to provide custom [CoroutineContext] in which to run update
     * requests.
     *
     * Note: This does not set the [CoroutineContext] for the GlanceAppWidget, which will always run
     * on the main thread.
     */
    @ExperimentalGlanceApi public open val coroutineContext: CoroutineContext = Dispatchers.Default

    @CallSuper
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(
                TAG,
                "Using Glance in devices with API<23 is untested and might behave unexpectedly.",
            )
        }
        glanceAppWidget.updateStrategy.handleBroadcast(
            context,
            BroadcastRequest.Update(this, appWidgetIds),
        )
    }

    internal suspend fun doUpdate(context: Context, appWidgetIds: IntArray) {
        coroutineScope {
            updateManager(this, context)
            appWidgetIds.map { async { glanceAppWidget.update(context, it) } }.awaitAll()
        }
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        glanceAppWidget.updateStrategy.handleBroadcast(
            context,
            BroadcastRequest.OptionsChanged(this, appWidgetId, newOptions),
        )
    }

    internal suspend fun doOptionsChanged(
        context: Context,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        coroutineScope {
            updateManager(this, context)
            glanceAppWidget.resize(context, appWidgetId, newOptions)
        }
    }

    @CallSuper
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        glanceAppWidget.updateStrategy.handleBroadcast(
            context,
            BroadcastRequest.Delete(this, appWidgetIds),
        )
    }

    internal suspend fun doDelete(context: Context, appWidgetIds: IntArray) {
        coroutineScope {
            updateManager(this, context)
            appWidgetIds.forEach { glanceAppWidget.deleted(context, it) }
        }
    }

    private fun updateManager(scope: CoroutineScope, context: Context) {
        scope.launch {
            runAndLogExceptions {
                GlanceAppWidgetManager(context)
                    .updateReceiver(this@GlanceAppWidgetReceiver, glanceAppWidget)
            }
        }
    }

    internal suspend fun doLambda(context: Context, id: Int, actionKey: String) {
        coroutineScope {
            updateManager(this, context)
            glanceAppWidget.triggerAction(context, id, actionKey)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runAndLogExceptions {
            when (intent.action) {
                Intent.ACTION_LOCALE_CHANGED,
                ACTION_DEBUG_UPDATE -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName =
                        ComponentName(
                            context.packageName,
                            checkNotNull(javaClass.canonicalName) { "no canonical name" },
                        )
                    val ids =
                        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                            intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)!!
                        } else {
                            appWidgetManager.getAppWidgetIds(componentName)
                        }
                    onUpdate(context, appWidgetManager, ids)
                }
                LambdaActionBroadcasts.ActionTriggerLambda -> {
                    val actionKey =
                        intent.getStringExtra(LambdaActionBroadcasts.ExtraActionKey)
                            ?: error("Intent is missing ActionKey extra")
                    val id = intent.getIntExtra(LambdaActionBroadcasts.ExtraAppWidgetId, -1)
                    if (id == -1) error("Intent is missing AppWidgetId extra")
                    glanceAppWidget.updateStrategy.handleBroadcast(
                        context,
                        BroadcastRequest.Lambda(this, id, actionKey),
                    )
                }
                else -> super.onReceive(context, intent)
            }
        }
    }

    /**
     * Override this to respond to broadcasts in a WorkManager worker rather than in the broadcast
     * receiver. This might be useful if your broadcast receiver frequently times out for reasons
     * outside of your control.
     *
     * This will incur a slight performance penalty compared to responding within the broadcast
     * receiver's goAsync because it has to serialize parameters.
     *
     * Only applies when using WorkManager; ignored by [UpdateStrategy.IMMEDIATE].
     *
     * @return true if the worker should be launched. Apps may choose to return true on certain
     *   devices, or according to some other internal business logic.
     */
    @ExperimentalGlanceApi
    protected open fun shouldLaunchAsyncRequestWorker(): Boolean {
        return false
    }

    /** Exposes the protected [shouldLaunchAsyncRequestWorker] to [UpdateStrategy]. */
    internal fun requestsAsyncRequestWorker(): Boolean = shouldLaunchAsyncRequestWorker()
}

/**
 * Whether goAsync must not be used: it is broken on certain OEMs, or disabled for testing (see
 * [DisableGoAsyncForTesting]).
 */
internal fun isGoAsyncBroken(): Boolean =
    DisableGoAsyncForTesting.get() || (Build.MANUFACTURER == "vivo" && Build.VERSION.SDK_INT < 35)

private inline fun runAndLogExceptions(block: () -> Unit) {
    try {
        block()
    } catch (ex: CancellationException) {
        // Nothing to do
    } catch (throwable: Throwable) {
        logException(throwable)
    }
}

/** Disables goAsync for all receivers during testing to exercise fallback paths. */
@VisibleForTesting
internal object DisableGoAsyncForTesting {
    private val disabled = AtomicBoolean(false)

    fun get() = disabled.get()

    fun set(newValue: Boolean) = disabled.set(newValue)
}
