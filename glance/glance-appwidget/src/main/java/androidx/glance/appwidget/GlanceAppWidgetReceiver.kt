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
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.action.LambdaActionBroadcasts
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
abstract class GlanceAppWidgetReceiver : AppWidgetProvider() {

    companion object {
        private const val TAG = "GlanceAppWidgetReceiver"

        /**
         * Action for a broadcast intent that will try to update all instances of a Glance App
         * Widget for debugging.
         * <pre>
         * adb shell am broadcast -a androidx.glance.appwidget.action.DEBUG_UPDATE -n APP/COMPONENT
         * </pre>
         * where APP/COMPONENT is the manifest component for the GlanceAppWidgetReceiver subclass.
         * This only works if the Receiver is exported (or the target device has adb running as
         * root), and has androidx.glance.appwidget.DEBUG_UPDATE in its intent-filter.
         * This should only be done for debug builds and disabled for release.
         */
        const val ACTION_DEBUG_UPDATE = "androidx.glance.appwidget.action.DEBUG_UPDATE"
    }

    /**
     * Instance of the [GlanceAppWidget] to use to generate the App Widget and send it to the
     * [AppWidgetManager]
     */
    abstract val glanceAppWidget: GlanceAppWidget

    /**
     * Override [coroutineContext] to provide custom [CoroutineContext] in which to run
     * update requests.
     *
     * Note: This does not set the [CoroutineContext] for the GlanceAppWidget, which will always run
     * on the main thread.
     */
    @get:ExperimentalGlanceApi
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalGlanceApi
    open val coroutineContext: CoroutineContext = Dispatchers.Default

    @CallSuper
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w(
                TAG,
                "Using Glance in devices with API<23 is untested and might behave unexpectedly."
            )
        }
        goAsync(coroutineContext) {
            updateManager(context)
            appWidgetIds.map { async { glanceAppWidget.update(context, it) } }
                .awaitAll()
        }
    }

    @CallSuper
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        goAsync(coroutineContext) {
            updateManager(context)
            glanceAppWidget.resize(context, appWidgetId, newOptions)
        }
    }

    @CallSuper
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        goAsync(coroutineContext) {
            updateManager(context)
            appWidgetIds.forEach { glanceAppWidget.deleted(context, it) }
        }
    }

    private fun CoroutineScope.updateManager(context: Context) {
        launch {
            runAndLogExceptions {
                GlanceAppWidgetManager(context)
                    .updateReceiver(this@GlanceAppWidgetReceiver, glanceAppWidget)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        runAndLogExceptions {
            when (intent.action) {
                Intent.ACTION_LOCALE_CHANGED, ACTION_DEBUG_UPDATE -> {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(
                        context.packageName,
                        checkNotNull(javaClass.canonicalName) { "no canonical name" }
                    )
                    val ids = if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
                        intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)!!
                    } else {
                        appWidgetManager.getAppWidgetIds(componentName)
                    }
                    onUpdate(
                        context,
                        appWidgetManager,
                        ids,
                    )
                }
                LambdaActionBroadcasts.ActionTriggerLambda -> {
                    val actionKey = intent.getStringExtra(LambdaActionBroadcasts.ExtraActionKey)
                            ?: error("Intent is missing ActionKey extra")
                    val id = intent.getIntExtra(LambdaActionBroadcasts.ExtraAppWidgetId, -1)
                    if (id == -1) error("Intent is missing AppWidgetId extra")
                    goAsync(coroutineContext) {
                        updateManager(context)
                        glanceAppWidget.triggerAction(context, id, actionKey)
                    }
                }
                else -> super.onReceive(context, intent)
            }
        }
    }
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
