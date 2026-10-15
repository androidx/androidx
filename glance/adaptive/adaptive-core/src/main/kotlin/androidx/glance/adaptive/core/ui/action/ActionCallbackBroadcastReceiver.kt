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

package androidx.glance.adaptive.core.ui.action

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Tag length must not exceed 23 characters for pre-API 24 log compatibility.
private const val TAG = "GlanceActionReceiver"

/**
 * Dispatches [GlanceWidgetAction.Callback] broadcast triggers to the target
 * [GlanceAdaptiveActionCallback] implementation.
 *
 * Declared with `android:exported="false"` in the library manifest so that only [PendingIntent]s
 * created by this application can invoke [GlanceAdaptiveActionCallback.onAction], preventing
 * third-party applications from spoofing callback broadcasts against an exported widget receiver or
 * provider.
 */
internal class ActionCallbackBroadcastReceiver(
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_CALLBACK) {
            Log.w(TAG, "Ignoring unexpected broadcast action: ${intent.action}")
            return
        }
        val callbackClassName = intent.getStringExtra(EXTRA_CALLBACK_CLASS)
        if (callbackClassName.isNullOrBlank()) {
            Log.w(TAG, "Action callback broadcast is missing EXTRA_CALLBACK_CLASS")
            return
        }
        val widgetName = intent.getStringExtra(EXTRA_WIDGET_NAME)
        if (widgetName.isNullOrBlank()) {
            Log.w(
                TAG,
                "Action callback broadcast for $callbackClassName is missing EXTRA_WIDGET_NAME",
            )
            return
        }
        val widgetId = intent.getStringExtra(EXTRA_WIDGET_ID)?.takeIf { it.isNotBlank() }
        val parameters = intent.getBundleExtra(EXTRA_PARAMETERS) ?: Bundle()

        // goAsync logs any failure, including a missing class or constructor, and always finishes
        // the broadcast.
        goAsync {
            val clazz = Class.forName(callbackClassName, false, context.classLoader)
            if (!GlanceAdaptiveActionCallback::class.java.isAssignableFrom(clazz)) {
                Log.e(
                    TAG,
                    "Class $callbackClassName does not implement GlanceAdaptiveActionCallback",
                )
                return@goAsync
            }
            clazz
                .asSubclass(GlanceAdaptiveActionCallback::class.java)
                .getDeclaredConstructor()
                .newInstance()
                .onAction(
                    context = context,
                    widgetName = widgetName,
                    widgetId = widgetId,
                    parameters = parameters,
                )
        }
    }

    private fun goAsync(block: suspend CoroutineScope.() -> Unit) {
        val parentScope = CoroutineScope(coroutineContext + Job())
        val pendingResult = goAsync()

        parentScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                // Regular cancellation, do not log as failure.
            } catch (e: Throwable) {
                Log.e(TAG, "BroadcastReceiver execution failed", e)
            } finally {
                parentScope.cancel()
                try {
                    pendingResult?.finish()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error thrown when trying to finish broadcast", e)
                }
            }
        }
    }

    companion object {
        internal const val ACTION_TRIGGER_CALLBACK =
            "androidx.glance.adaptive.core.action.TRIGGER_CALLBACK"
        internal const val EXTRA_CALLBACK_CLASS =
            "androidx.glance.adaptive.core.extra.CALLBACK_CLASS"
        internal const val EXTRA_WIDGET_NAME = "androidx.glance.adaptive.core.extra.WIDGET_NAME"
        internal const val EXTRA_WIDGET_ID = "androidx.glance.adaptive.core.extra.WIDGET_ID"
        internal const val EXTRA_PARAMETERS =
            "androidx.glance.adaptive.core.extra.ACTION_PARAMETERS"

        /**
         * Creates the broadcast [Intent] that runs [action] for the widget instance identified by
         * [widgetName] and [widgetId].
         */
        fun createIntent(
            context: Context,
            action: GlanceWidgetAction.Callback,
            widgetName: String,
            widgetId: String?,
        ): Intent {
            val instanceId = widgetId?.takeIf { it.isNotBlank() }
            return Intent(context, ActionCallbackBroadcastReceiver::class.java)
                .setAction(ACTION_TRIGGER_CALLBACK)
                // Extras are not part of PendingIntent identity, so the data URI keeps different
                // instances and actions from sharing, and overwriting, a single PendingIntent.
                .setData(
                    Uri.Builder()
                        .scheme("androidx-glance-adaptive-action")
                        .appendPath(widgetName)
                        .appendPath(instanceId.orEmpty())
                        .appendPath(action.hashCode().toString())
                        .build()
                )
                .putExtra(EXTRA_CALLBACK_CLASS, action.actionCallbackClass.name)
                .putExtra(EXTRA_WIDGET_NAME, widgetName)
                .putExtra(EXTRA_WIDGET_ID, instanceId)
                .putExtra(EXTRA_PARAMETERS, Bundle(action.parameters))
        }
    }
}
