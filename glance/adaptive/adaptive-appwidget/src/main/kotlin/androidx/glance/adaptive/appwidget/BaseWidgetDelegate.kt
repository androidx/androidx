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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.DpSize
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.TemplateRegistry
import androidx.glance.adaptive.core.templates.AdaptiveGlanceTemplate
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base implementation of [GlanceAdaptiveWidgetDelegate] responsible for resolving active widget
 * placements and dispatching template updates to platform AppWidget infrastructure.
 */
internal class BaseWidgetDelegate(
    private val context: Context,
    private val repository: WidgetInstanceRepository = WidgetInstanceRepository(context),
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
) : GlanceAdaptiveWidgetDelegate {

    /**
     * Resolves active target widget instances for the given [widgetName] and optional [widgetIds],
     * renders [currentData] via [TemplateRegistry.render], and updates matching platform AppWidgets
     * directly.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param currentData Declarative template data payload implementing [AdaptiveGlanceTemplate].
     * @param widgetIds Optional collection of developer target widget instance String identifiers.
     *   If an explicit empty collection is passed, no widgets will be updated.
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    override suspend fun pushUpdate(
        widgetName: String,
        currentData: AdaptiveGlanceTemplate,
        widgetIds: Set<String>?,
    ): Unit =
        withContext(Dispatchers.IO) {
            try {
                val componentToAppWidgetIds =
                    repository.findAppWidgetIdsForWidgetName(widgetName, widgetIds)

                if (componentToAppWidgetIds.isNotEmpty()) {
                    // Compose template into RemoteViews using DpSize.Unspecified for global
                    // broadcast updates across matching widget instances.
                    val compositionResult =
                        GlanceRemoteViews().compose(context = context, size = DpSize.Unspecified) {
                            TemplateRegistry.render(currentData)
                        }
                    val remoteViews = compositionResult.remoteViews

                    for ((_, appWidgetIds) in componentToAppWidgetIds) {
                        appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO(b/550323432): Re-evaluate error propagation vs logging for pushUpdate
                // failures.
                Log.e(TAG, "Error pushing widget update for widgetName $widgetName", e)
            }
        }

    companion object {
        private const val TAG = "BaseWidgetDelegate"
    }
}
