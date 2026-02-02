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
import androidx.glance.appwidget.AsyncRequestWorker.Companion.toBytes
import androidx.glance.appwidget.action.LambdaActionBroadcasts
import androidx.glance.appwidget.proto.LayoutProto.AsyncRequest
import androidx.glance.appwidget.protobuf.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        val handled =
            maybeLaunchAsyncRequestWorker(context) {
                update =
                    AsyncRequest.Update.newBuilder().run {
                        receiver = this@GlanceAppWidgetReceiver::class.java.canonicalName
                        addAllAppWidgetIds(appWidgetIds.toList())
                        build()
                    }
            }
        if (!handled) {
            goAsync(coroutineContext) { doUpdate(context, appWidgetIds) }
        }
    }

    internal suspend fun CoroutineScope.doUpdate(context: Context, appWidgetIds: IntArray) {
        updateManager(context)
        appWidgetIds.map { async { glanceAppWidget.update(context, it) } }.awaitAll()
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        val handled =
            maybeLaunchAsyncRequestWorker(context) {
                optionsChanged =
                    AsyncRequest.OptionsChanged.newBuilder().run {
                        receiver = this@GlanceAppWidgetReceiver::class.java.canonicalName
                        setAppWidgetId(appWidgetId)
                        bundle = ByteString.copyFrom(newOptions.toBytes())
                        build()
                    }
            }
        if (!handled) {
            goAsync(coroutineContext) { doOptionsChanged(context, appWidgetId, newOptions) }
        }
    }

    internal suspend fun CoroutineScope.doOptionsChanged(
        context: Context,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        updateManager(context)
        glanceAppWidget.resize(context, appWidgetId, newOptions)
    }

    @CallSuper
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val handled =
            maybeLaunchAsyncRequestWorker(context) {
                delete =
                    AsyncRequest.Delete.newBuilder().run {
                        receiver = this@GlanceAppWidgetReceiver::class.java.canonicalName
                        addAllAppWidgetIds(appWidgetIds.toList())
                        build()
                    }
            }
        if (!handled) {
            goAsync(coroutineContext) { doDelete(context, appWidgetIds) }
        }
    }

    internal suspend fun CoroutineScope.doDelete(context: Context, appWidgetIds: IntArray) {
        updateManager(context)
        appWidgetIds.forEach { glanceAppWidget.deleted(context, it) }
    }

    private fun CoroutineScope.updateManager(context: Context) {
        launch {
            runAndLogExceptions {
                GlanceAppWidgetManager(context)
                    .updateReceiver(this@GlanceAppWidgetReceiver, glanceAppWidget)
            }
        }
    }

    internal suspend fun CoroutineScope.doLambda(context: Context, id: Int, actionKey: String) {
        updateManager(context)
        glanceAppWidget.triggerAction(context, id, actionKey)
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
                    val handled =
                        maybeLaunchAsyncRequestWorker(context) {
                            lambda =
                                AsyncRequest.Lambda.newBuilder().run {
                                    receiver =
                                        this@GlanceAppWidgetReceiver::class.java.canonicalName
                                    appWidgetId = id
                                    setActionKey(actionKey)
                                    build()
                                }
                        }
                    if (!handled) {
                        goAsync(coroutineContext) { doLambda(context, id, actionKey) }
                    }
                }
                else -> super.onReceive(context, intent)
            }
        }
    }
}

/**
 * goAsync is broken on certain OEMs, so we start a Worker in order to have a CoroutineScope in
 * which to call suspend functions.
 *
 * @param context Context to use to start the service
 * @param request the request to be run by the worker
 * @return true if the worker was launched and no further action needs to be taken by the receiver.
 *   If false, the receiver should continue processing the request using
 *   [android.content.BroadcastReceiver.goAsync].
 */
internal fun maybeLaunchAsyncRequestWorker(
    context: Context,
    request: AsyncRequest.Builder.() -> Unit,
): Boolean {
    if (
        ForceAsyncRequestWorker.get() ||
            (Build.MANUFACTURER == "vivo" && Build.VERSION.SDK_INT < 35)
    ) {
        AsyncRequestWorker.launchAsyncRequestWorker(
            context,
            AsyncRequest.newBuilder().apply(request).build(),
        )
        return true
    }
    return false
}

private inline fun runAndLogExceptions(block: () -> Unit) {
    try {
        block()
    } catch (ex: CancellationException) {
        // Nothing to do
    } catch (throwable: Throwable) {
        logException(throwable)
    }
}

/** This is used to force using the service for all receivers during testing. */
@VisibleForTesting
internal object ForceAsyncRequestWorker {
    private val forceService = AtomicBoolean(false)

    fun get() = forceService.get()

    fun set(newValue: Boolean) = forceService.set(newValue)
}
