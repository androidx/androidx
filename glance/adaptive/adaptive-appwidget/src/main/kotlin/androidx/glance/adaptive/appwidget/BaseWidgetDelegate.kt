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

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.MutableIntList
import androidx.collection.MutableObjectIntMap
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurfaceDetector
import androidx.glance.adaptive.core.WidgetInstanceInfo
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Platform AppWidget implementation of [GlanceAdaptiveAppWidgetDelegate].
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
    private val telemetry: WidgetTelemetryHandler = WidgetTelemetryHandler(appWidgetManager),
) : GlanceAdaptiveAppWidgetDelegate {

    /**
     * Resolves active target widget instances for the given [widgetName] and optional [widgetIds],
     * renders [currentData] via [GlanceRemoteViewsComposer], and updates matching platform
     * AppWidgets directly. Telemetry extras are written through [WidgetTelemetryHandler] before the
     * update.
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
                telemetry.ensureTelemetryOptions(targetToInstances, currentData)

                for ((target, appWidgetIds) in targetToInstances) {
                    try {
                        val remoteViews = composer.compose(currentData, target, widgetName)
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
     * Enumerates active platform AppWidgets for [widgetName] and folds them into one
     * [WidgetInstanceInfo] per widget instance String identifier.
     *
     * Several platform AppWidgets can carry the same identifier, so placements are counted per
     * resolved host surface rather than returned individually. Placements with no stored identifier
     * are skipped: they cannot be targeted by [pushUpdate], so reporting them would describe an
     * instance the caller has no way to address.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @return Active instances for [widgetName], or an empty list if none are placed.
     * @throws RuntimeException if the options of any placement cannot be read, rather than
     *   returning a partial result.
     */
    override suspend fun getActiveInstances(widgetName: String): List<WidgetInstanceInfo> =
        withContext(Dispatchers.IO) {
            val componentToAppWidgetIds =
                repository.findAppWidgetIdsForWidgetName(widgetName, widgetIds = null)
            if (componentToAppWidgetIds.isEmpty()) return@withContext emptyList()

            // Sorted so that the options bundle retained for an identifier placed more than
            // once is always the lowest appWidgetId's, making the result deterministic.
            val sortedAppWidgetIds = MutableIntList()
            componentToAppWidgetIds.values.forEach { sortedAppWidgetIds.addAll(it) }
            sortedAppWidgetIds.sort()

            val accumulators = LinkedHashMap<String, InstanceAccumulator>()
            for (i in 0 until sortedAppWidgetIds.size) {
                val appWidgetId = sortedAppWidgetIds[i]
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId) ?: continue
                // A blank identifier is treated as absent, matching how the receiver decides
                // that an identifier still needs to be generated for a placement.
                val widgetId =
                    options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)?.takeIf {
                        it.isNotBlank()
                    } ?: continue
                val surface = AppWidgetSurfaceDetector.fromAppWidgetOptions(options)
                val accumulator = accumulators.getOrPut(widgetId) { InstanceAccumulator(options) }
                accumulator.surfacePlacements[surface] =
                    accumulator.surfacePlacements.getOrDefault(surface, 0) + 1
            }

            accumulators.map { (widgetId, accumulator) ->
                WidgetInstanceInfo(
                    widgetName = widgetName,
                    widgetId = widgetId,
                    options = accumulator.options,
                    surfacePlacements = accumulator.surfacePlacements,
                )
            }
        }

    /** Collects the placements sharing one widget instance String identifier. */
    private class InstanceAccumulator(val options: Bundle) {
        val surfacePlacements: MutableObjectIntMap<GlanceSurface> = MutableObjectIntMap()
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
                // Match and filter against active installed providers before expensive composition
                val validProviders = findInstalledProviders(widgetName)
                if (validProviders.isEmpty()) return@withContext

                val targetToPreviews = composer.groupPreviewsByRenderTarget(validProviders)
                for ((target, previews) in targetToPreviews) {
                    val remoteViews =
                        try {
                            composer.compose(previewData, target, widgetName)
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

    /**
     * Requests that the launcher pin the first receiver for [widgetName] that can be placed on the
     * home screen, logging a warning if several receivers match [widgetName].
     *
     * Mirrors [androidx.glance.appwidget.GlanceAppWidgetManager.requestPinGlanceAppWidget], apart
     * from [widgetId] and [options]. [AppWidgetManager.requestPinAppWidget] only hands its extras
     * to the launcher, so they cannot reach the pinned widget that way. When either is set, the
     * request is instead sent with a success callback handled by [RequestPinCallbackReceiver],
     * which stores them in the options of the pinned widget and then sends [successCallback].
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param widgetId Widget instance String identifier to assign to the pinned widget. If null or
     *   blank, the one in [options] is used, or one is generated if there is none.
     * @param initialData Declarative template data payload implementing [AdaptiveGlanceTemplate] to
     *   render as the preview shown while the launcher asks the user to confirm, if any.
     * @param options Configuration options to store in the options of the pinned widget.
     * @param successCallback [PendingIntent] to send once the widget is pinned, if any.
     * @return true if the request was sent to the launcher, false otherwise.
     */
    override suspend fun requestPin(
        widgetName: String,
        widgetId: String?,
        initialData: AdaptiveGlanceTemplate?,
        options: Bundle,
        successCallback: PendingIntent?,
    ): Boolean =
        withContext(Dispatchers.IO) {
            // Early return on API < 26: AppWidgetManager.requestPinAppWidget is not supported.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return@withContext false
            }
            if (!appWidgetManager.isRequestPinAppWidgetSupported) return@withContext false

            val providers = findInstalledProviders(widgetName)
            if (providers.size > 1) {
                Log.w(
                    TAG,
                    "Several receivers match widgetName $widgetName, which should be unique per " +
                        "app. Pinning the first one that can be placed on the home screen.",
                )
            }
            @Suppress("ListIterator")
            val providerInfo =
                providers.firstOrNull { it.isHomeScreenProvider } ?: return@withContext false
            val extras =
                Bundle().apply {
                    if (initialData != null) {
                        val preview =
                            composer.compose(
                                initialData,
                                composer.pinPreviewRenderTarget(providerInfo),
                                widgetName,
                            )
                        putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview)
                    }
                }
            appWidgetManager.requestPinAppWidget(
                providerInfo.provider,
                extras,
                pinSuccessCallback(widgetId, options, successCallback),
            )
        }

    /**
     * Completes the configuration of the widget identified by [AppWidgetManager.EXTRA_APPWIDGET_ID]
     * in [activity]'s intent, storing [widgetId] and [options] in its options, then setting
     * [Activity.RESULT_OK] with the widget's `appWidgetId`.
     *
     * Failures to read or store the options of the widget are propagated rather than logged, so
     * that the host does not keep a widget that is not configured as requested. The result is not
     * set if this throws. Exceptions reach the caller in [activity], never the host: if [activity]
     * then finishes without [Activity.RESULT_OK], the host removes a newly placed widget, or keeps
     * the previous configuration of a reconfigured one.
     *
     * This is a blocking call: it reads and stores the options of the widget through synchronous
     * [AppWidgetManager] Binder calls, and returns only once they are stored and the result is set.
     * It is not a suspend function so that the result is always set before the caller finishes
     * [activity].
     *
     * @param activity Configuration [Activity] started with
     *   [AppWidgetManager.ACTION_APPWIDGET_CONFIGURE].
     * @param widgetId Widget instance String identifier to assign to the configured widget. If null
     *   or blank, the one in [options] or already stored for the widget is used, or one is
     *   generated if there is none.
     * @param options Configuration options to store in the options of the configured widget.
     * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
     *   [AppWidgetManager.EXTRA_APPWIDGET_ID].
     */
    override fun setConfigurationResult(activity: Activity, widgetId: String?, options: Bundle) {
        val appWidgetId =
            activity.intent?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        require(appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            "setConfigurationResult must be called from a configuration activity started with a " +
                "valid EXTRA_APPWIDGET_ID"
        }

        val requestedWidgetId =
            resolveRequestedWidgetId(widgetId, options)
                ?: appWidgetManager
                    .getAppWidgetOptions(appWidgetId)
                    ?.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
                    ?.takeIf { it.isNotBlank() }
        // Merged into the options the host placed the widget with. Stored before the result is
        // set, so that the result is not set if storing fails.
        appWidgetManager.updateAppWidgetOptions(
            appWidgetId,
            optionsWithWidgetId(requestedWidgetId, options),
        )

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        activity.setResult(Activity.RESULT_OK, resultValue)
    }

    /**
     * Returns the success callback of a pin request: [successCallback] itself if there is nothing
     * to store, or otherwise one that first stores [widgetId] and [options] in the options of the
     * pinned widget.
     */
    private fun pinSuccessCallback(
        widgetId: String?,
        options: Bundle,
        successCallback: PendingIntent?,
    ): PendingIntent? {
        val requestedWidgetId = resolveRequestedWidgetId(widgetId, options)
        if (requestedWidgetId == null && options.isEmpty) {
            // A default widgetId is still generated later by GlanceAdaptiveWidgetReceiver once the
            // widget is placed, so there is nothing to store here.
            return successCallback
        }

        return RequestPinCallbackReceiver.createSuccessCallback(
            context = context,
            options = optionsWithWidgetId(requestedWidgetId, options),
            successCallback = successCallback,
        )
    }

    /**
     * Resolves the requested widget identifier from [widgetId] or [options], treating a blank
     * identifier as absent, as [GlanceAdaptiveWidgetReceiver] does.
     */
    private fun resolveRequestedWidgetId(widgetId: String?, options: Bundle): String? =
        widgetId?.takeIf { it.isNotBlank() }
            ?: options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)?.takeIf {
                it.isNotBlank()
            }

    /**
     * Returns a copy of [options] with [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID] set to
     * [requestedWidgetId], or a newly generated identifier if [requestedWidgetId] is null.
     *
     * The identifier is generated now if not requested, rather than by the receiver once the widget
     * is placed: the receiver updates a placed widget again when its identifier changes, but not
     * when only other options do, so this makes sure the widget is updated with [options] even if
     * it was first updated before they were stored.
     */
    private fun optionsWithWidgetId(requestedWidgetId: String?, options: Bundle): Bundle =
        Bundle(options).apply {
            putString(
                GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID,
                requestedWidgetId ?: WidgetOptionsCache.generateWidgetId(),
            )
        }

    /**
     * Resolves the installed [AppWidgetProviderInfo] of each receiver matching [widgetName],
     * skipping receivers that are not installed AppWidget providers.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun findInstalledProviders(widgetName: String): List<AppWidgetProviderInfo> {
        val matchingComponents = repository.findReceiverComponentsForWidgetName(widgetName)
        if (matchingComponents.isEmpty()) return emptyList()

        val installedProviders =
            appWidgetManager.getInstalledProvidersForPackage(
                /* packageName= */ context.packageName,
                /* profile= */ null,
            )
        val providerMap = HashMap<ComponentName, AppWidgetProviderInfo>(installedProviders.size)
        for (idx in installedProviders.indices) {
            val info = installedProviders[idx]
            providerMap[info.provider] = info
        }

        return matchingComponents.mapNotNull { component ->
            providerMap[component]
                ?: run {
                    Log.w(TAG, "Component $component is not an installed AppWidgetProvider")
                    null
                }
        }
    }

    companion object {
        private const val TAG = "BaseWidgetDelegate"
    }
}

/**
 * Whether hosts may place this provider on the home screen, without which the platform rejects
 * requests to pin it.
 */
private val AppWidgetProviderInfo.isHomeScreenProvider: Boolean
    get() = (widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0
