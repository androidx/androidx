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
import androidx.glance.adaptive.core.GlanceAdaptiveWidgetDelegate
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
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
    private val composer: GlanceRemoteViewsComposer =
        GlanceRemoteViewsComposer(context, appWidgetManager),
) : GlanceAdaptiveWidgetDelegate {

    /**
     * Resolves active target widget instances for the given [widgetName] and optional [widgetIds],
     * renders [currentData] via [GlanceRemoteViewsComposer], and updates matching platform
     * AppWidgets directly.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param currentData Declarative template data payload implementing [AdaptiveGlanceTemplate].
     * @param widgetIds Optional collection of developer target widget instance String identifiers.
     *   If an explicit empty collection is passed, no widgets will be updated.
     */
    override suspend fun pushUpdate(
        widgetName: String,
        currentData: AdaptiveGlanceTemplate,
        widgetIds: Set<String>?,
    ): Unit =
        withContext(Dispatchers.IO) {
            try {
                val componentToAppWidgetIds =
                    repository.findAppWidgetIdsForWidgetName(widgetName, widgetIds)
                if (componentToAppWidgetIds.isEmpty()) return@withContext

                val targetToInstances =
                    composer.groupInstancesByRenderTarget(componentToAppWidgetIds)
                for ((target, appWidgetIds) in targetToInstances) {
                    try {
                        val remoteViews = composer.compose(currentData, target)
                        appWidgetManager.updateAppWidget(appWidgetIds.toIntArray(), remoteViews)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error updating widgets for surface ${target.surface}, size " +
                                "${target.dimensions} and widgetName $widgetName",
                            e,
                        )
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

                val targetToPreviews = composer.groupPreviewsByRenderTarget(validProviders)
                for ((target, previews) in targetToPreviews) {
                    val remoteViews =
                        try {
                            composer.compose(previewData, target)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error composing preview for surface ${target.surface}, size " +
                                    "${target.dimensions} and widgetName $widgetName",
                                e,
                            )
                            continue
                        }

                    for (i in previews.indices) {
                        val preview = previews[i]
                        try {
                            val success =
                                appWidgetManager.setWidgetPreview(
                                    preview.provider,
                                    preview.category,
                                    remoteViews,
                                )
                            if (!success) {
                                Log.w(
                                    TAG,
                                    "AppWidgetManager.setWidgetPreview returned false for " +
                                        "${preview.provider} with category ${preview.category}",
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error setting widget preview for ${preview.provider} with " +
                                    "category ${preview.category}",
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
