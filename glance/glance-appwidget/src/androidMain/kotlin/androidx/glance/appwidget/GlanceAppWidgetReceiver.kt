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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
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
abstract class GlanceAppWidgetReceiver : AppWidgetProvider() {

    private companion object {
        private const val TAG = "GlanceAppWidgetReceiver"
    }

    /**
     * Instance of the [GlanceAppWidget] to use to generate the App Widget and send it to the
     * [AppWidgetManager]
     */
    abstract val glanceAppWidget: GlanceAppWidget

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
        goAsync {
            updateManager(context)
            appWidgetIds.map { async { glanceAppWidget.update(context, appWidgetManager, it) } }
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
        goAsync {
            updateManager(context)
            glanceAppWidget.resize(context, appWidgetManager, appWidgetId, newOptions)
        }
    }

    @CallSuper
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        goAsync {
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
            if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName =
                    ComponentName(context.packageName, checkNotNull(javaClass.canonicalName))
                onUpdate(
                    context,
                    appWidgetManager,
                    appWidgetManager.getAppWidgetIds(componentName)
                )
                return
            }
            super.onReceive(context, intent)
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
