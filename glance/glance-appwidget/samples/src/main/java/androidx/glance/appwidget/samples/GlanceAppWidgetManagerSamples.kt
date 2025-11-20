/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.glance.appwidget.samples

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.Sampled
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetManager.Companion.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Sampled
suspend fun setWidgetPreviews(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        return
    }
    fun Class<GlanceAppWidgetReceiver>.hasPreviewForCategory(widgetCategory: Int): Boolean {
        val component = ComponentName(context, this)
        val providerInfo =
            (context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager)
                .installedProviders
                .first { providerInfo -> providerInfo.provider == component }
        return providerInfo.generatedPreviewCategories.and(widgetCategory) != 0
    }
    val receiverClasses = listOf<Class<GlanceAppWidgetReceiver>>()
    val glanceAppWidgetManager = GlanceAppWidgetManager(context)
    withContext(Dispatchers.Default) {
        try {
            for (receiver in receiverClasses) {
                if (receiver.hasPreviewForCategory(WIDGET_CATEGORY_HOME_SCREEN)) {
                    Log.i("Widget", "Skipped updating previews for $receiver")
                    continue
                }
                if (
                    glanceAppWidgetManager.setWidgetPreviews(receiver.kotlin) ==
                        SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED
                ) {
                    Log.e("Widget", "Failed to set previews for $receiver, rate limited")
                }
            }
        } catch (e: Exception) {
            Log.e("Widget", "Error thrown when calling setWidgetPreview", e)
        }
    }
}
