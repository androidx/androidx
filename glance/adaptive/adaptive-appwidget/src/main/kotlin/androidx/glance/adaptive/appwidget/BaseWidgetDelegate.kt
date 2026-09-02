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
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.ui.unit.DpSize
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.ui.TemplateRegistry
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
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

    /**
     * Sets dynamic preview data rendered in host widget pickers for the specified widget
     * definition.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param previewData Declarative template data payload implementing [AdaptiveGlanceTemplate].
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    override suspend fun setPreview(widgetName: String, previewData: AdaptiveGlanceTemplate): Unit =
        withContext(Dispatchers.IO) {
            // Early return on API < 35: AppWidgetManager.setWidgetPreview is not supported.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return@withContext
            }

            try {
                val matchingComponents = repository.findReceiverComponentsForWidgetName(widgetName)
                if (matchingComponents.isEmpty()) return@withContext

                val installedProviders =
                    appWidgetManager.getInstalledProvidersForPackage(
                        /* packageName= */ context.packageName,
                        /* profile= */ null,
                    )
                val providerMap =
                    HashMap<ComponentName, AppWidgetProviderInfo>(installedProviders.size)
                for (idx in installedProviders.indices) {
                    val info = installedProviders[idx]
                    providerMap[info.provider] = info
                }

                // Match and filter against active installed providers before expensive composition
                val validProviders =
                    matchingComponents.mapNotNull { component ->
                        providerMap[component]
                            ?: run {
                                Log.w(
                                    TAG,
                                    "Component $component is not an installed AppWidgetProvider",
                                )
                                null
                            }
                    }
                if (validProviders.isEmpty()) return@withContext

                val compositionResult =
                    GlanceRemoteViews().compose(context = context, size = DpSize.Unspecified) {
                        TemplateRegistry.render(previewData)
                    }
                val remoteViews = compositionResult.remoteViews

                for (idx in validProviders.indices) {
                    val providerInfo = validProviders[idx]
                    val categories =
                        providerInfo.widgetCategory.takeIf { it != 0 }
                            ?: AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN

                    try {
                        val success =
                            appWidgetManager.setWidgetPreview(
                                providerInfo.provider,
                                categories,
                                remoteViews,
                            )
                        if (!success) {
                            Log.w(
                                TAG,
                                "AppWidgetManager.setWidgetPreview returned false for ${providerInfo.provider}",
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting widget preview for ${providerInfo.provider}", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO(b/550323432): Re-evaluate error propagation vs logging for widget failures.
                Log.e(TAG, "Error setting widget preview for widgetName $widgetName", e)
            }
        }

    companion object {
        private const val TAG = "BaseWidgetDelegate"
    }
}
