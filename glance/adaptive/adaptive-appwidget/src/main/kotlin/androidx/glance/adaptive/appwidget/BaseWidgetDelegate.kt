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
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurfaceDetector
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.ui.TemplateRegistry
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Platform AppWidget implementation of [GlanceAdaptiveWidgetDelegate].
 *
 * Translates cross-surface declarative [AdaptiveGlanceTemplate] updates into concrete platform
 * [android.widget.RemoteViews] updates targeting active [AppWidgetManager] widget instances and
 * widget previews.
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
                    // Group widget IDs by detected host surface to minimize RemoteViews
                    // compositions
                    val surfaceToAppWidgetIds =
                        mutableMapOf<AppWidgetGlanceSurface, MutableList<Int>>()
                    for ((_, appWidgetIds) in componentToAppWidgetIds) {
                        for (appWidgetId in appWidgetIds) {
                            val options =
                                try {
                                    appWidgetManager.getAppWidgetOptions(appWidgetId)
                                } catch (e: Exception) {
                                    null
                                }
                            val surface = AppWidgetSurfaceDetector.fromAppWidgetOptions(options)
                            surfaceToAppWidgetIds
                                .getOrPut(surface) { mutableListOf() }
                                .add(appWidgetId)
                        }
                    }

                    for ((surface, appWidgetIds) in surfaceToAppWidgetIds) {
                        try {
                            val compositionResult =
                                GlanceRemoteViews().compose(
                                    context = context,
                                    size = DpSize.Unspecified,
                                ) {
                                    TemplateRegistry.render(currentData, surface)
                                }
                            val remoteViews = compositionResult.remoteViews
                            appWidgetManager.updateAppWidget(appWidgetIds.toIntArray(), remoteViews)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error updating widgets for surface $surface and widgetName $widgetName",
                                e,
                            )
                        }
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
                val validProviders = matchingComponents.mapNotNull { component ->
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

                // Group providers by detected surface based on their widgetCategory.
                // A provider can declare multiple categories (e.g. HOME_SCREEN and KEYGUARD),
                // so we expand each into its target surface and individual category flag.
                val surfaceToTargets =
                    mutableMapOf<
                        AppWidgetGlanceSurface,
                        MutableList<Pair<AppWidgetProviderInfo, Int>>,
                    >()
                for (i in validProviders.indices) {
                    val providerInfo = validProviders[i]
                    val previewTargets =
                        AppWidgetSurfaceDetector.resolvePreviewCategories(
                            providerInfo.widgetCategory
                        )
                    for (j in previewTargets.indices) {
                        val (surface, category) = previewTargets[j]
                        surfaceToTargets
                            .getOrPut(surface) { mutableListOf() }
                            .add(providerInfo to category)
                    }
                }

                for ((surface, targets) in surfaceToTargets) {
                    val compositionResult =
                        try {
                            GlanceRemoteViews().compose(
                                context = context,
                                size = DpSize.Unspecified,
                            ) {
                                TemplateRegistry.render(previewData, surface)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error composing preview for surface $surface and widgetName $widgetName",
                                e,
                            )
                            continue
                        }
                    val remoteViews = compositionResult.remoteViews

                    for (i in targets.indices) {
                        val (providerInfo, category) = targets[i]
                        try {
                            val success =
                                appWidgetManager.setWidgetPreview(
                                    providerInfo.provider,
                                    category,
                                    remoteViews,
                                )
                            if (!success) {
                                Log.w(
                                    TAG,
                                    "AppWidgetManager.setWidgetPreview returned false for ${providerInfo.provider} with category $category",
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error setting widget preview for ${providerInfo.provider} with category $category",
                                e,
                            )
                        }
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
