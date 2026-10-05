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

package androidx.glance.adaptive.appwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.UUID

/**
 * Stores the widgetId and options requested through [requestPin] once the launcher has pinned the
 * widget, then sends the caller's own success callback.
 *
 * [AppWidgetManager.requestPinAppWidget] only hands its extras to the launcher, and the appWidgetId
 * of the pinned widget is not known until the launcher accepts the request. The requested options
 * therefore travel inside the success callback instead, which the platform sends with the new
 * appWidgetId, and are written to the options of the new widget here.
 *
 * This receiver is not exported, so other apps cannot change the options of this app's widgets
 * through it.
 */
internal class RequestPinCallbackReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        val options = intent.getBundleExtra(EXTRA_OPTIONS)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.w(TAG, "Pin confirmation has no appWidgetId to store the requested options for")
        } else if (options != null) {
            try {
                // Merged into the options the launcher placed the widget with.
                AppWidgetManager.getInstance(context).updateAppWidgetOptions(appWidgetId, options)
            } catch (e: Exception) {
                // Like GlanceAdaptiveWidgetReceiver, logs rather than crashes the app in the
                // background.
                Log.e(TAG, "Error storing requested options for appWidgetId $appWidgetId", e)
            }
        }
        sendSuccessCallback(context, intent, appWidgetId)
    }

    private fun sendSuccessCallback(context: Context, intent: Intent, appWidgetId: Int) {
        val successCallback =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Api33Impl.getParcelableExtra(
                    intent,
                    EXTRA_SUCCESS_CALLBACK,
                    PendingIntent::class.java,
                )
            } else {
                // The type-safe getParcelableExtra(String, Class) overload requires API 33.
                @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_SUCCESS_CALLBACK)
            } ?: return
        val fillIn =
            Intent().apply {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            }
        try {
            successCallback.send(context, /* code= */ 0, fillIn)
        } catch (e: PendingIntent.CanceledException) {
            Log.w(TAG, "Success callback of the pin request was cancelled", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private object Api33Impl {
        fun <T> getParcelableExtra(intent: Intent, name: String, clazz: Class<T>): T? =
            intent.getParcelableExtra(name, clazz)
    }

    companion object {
        private const val TAG = "RequestPinCallback"
        private const val EXTRA_OPTIONS = "androidx.glance.adaptive.appwidget.extra.PIN_OPTIONS"
        private const val EXTRA_SUCCESS_CALLBACK =
            "androidx.glance.adaptive.appwidget.extra.PIN_SUCCESS_CALLBACK"

        /**
         * Creates the success callback of a pin request, which stores [options] in the options of
         * the pinned widget and then sends [successCallback], if any.
         */
        fun createSuccessCallback(
            context: Context,
            options: Bundle,
            successCallback: PendingIntent?,
        ): PendingIntent {
            val intent =
                Intent(context, RequestPinCallbackReceiver::class.java)
                    // Unique data keeps concurrent requests from sharing, and overwriting, a single
                    // PendingIntent.
                    .setData(Uri.parse("androidx-glance-adaptive-pin://${UUID.randomUUID()}"))
                    // Nested in a single extra, so that the platform can add the appWidgetId
                    // without unparcelling the options.
                    .putExtra(EXTRA_OPTIONS, options)
                    .putExtra(EXTRA_SUCCESS_CALLBACK, successCallback)
            return PendingIntent.getBroadcast(
                context,
                /* requestCode= */ 0,
                intent,
                // Mutable so that the platform can add the new appWidgetId, which is safe because
                // the intent is explicit. One shot, as the request is accepted at most once.
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT,
            )
        }
    }
}
