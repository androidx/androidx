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
import androidx.collection.MutableIntList
import androidx.collection.MutableObjectIntMap
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurfaceDetector
import androidx.glance.adaptive.core.WidgetInstanceInfo
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Platform AppWidget implementation of [GlanceAdaptiveAppWidgetDelegate].
 *
 * Translates cross-surface declarative [GlanceAdaptiveTemplate] updates into concrete platform
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
     * @param currentData Declarative template data payload implementing [GlanceAdaptiveTemplate].
     * @param widgetIds Optional collection of developer target widget instance String identifiers.
     *   If an explicit empty collection is passed, no widgets will be updated. Placements are
     *   updated independently: a placement whose options cannot be read, or a [RenderTarget] that
     *   fails to compose or deliver, does not prevent the others from being updated. Once every
     *   placement has been attempted, the failures are thrown together.
     * @throws IllegalArgumentException if [widgetIds] contains a blank identifier, or if
     *   [widgetName] cannot be resolved, see [GlanceAdaptiveWidgetReceiver.widgetName].
     * @throws IllegalStateException if [widgetName] cannot be resolved.
     * @throws androidx.glance.adaptive.core.GlanceAdaptiveUpdateException if some placements could
     *   not be updated, listing their widgetIds.
     */
    override suspend fun pushUpdate(
        widgetName: String,
        currentData: GlanceAdaptiveTemplate,
        widgetIds: Set<String>?,
    ): Unit =
        withContext(Dispatchers.IO) {
            // Validated before any IPC, so that developer errors fail even when no widget is
            // placed.
            if (widgetIds != null) {
                for (widgetId in widgetIds) {
                    require(widgetId.isNotBlank()) {
                        "widgetIds must not contain a blank widgetId, which no widget can have"
                    }
                }
            }

            val failures = PushUpdateFailures(widgetName)
            // Listing the placements is a single call made before any widget is touched, so its
            // failures propagate unchanged.
            val componentToAppWidgetIds =
                repository.findAppWidgetIdsForWidgetName(widgetName, widgetIds) { appWidgetId, e ->
                    // Its widgetId is unknown, so it may or may not have been targeted.
                    failures.addUnreadableOptions(appWidgetId, widgetId = null, error = e)
                }
            val instances =
                composer.groupInstancesByRenderTarget(componentToAppWidgetIds) { appWidgetId, e ->
                    failures.addUnreadableOptions(
                        appWidgetId,
                        WidgetOptionsCache.getCachedWidgetId(appWidgetId),
                        error = e,
                    )
                }
            telemetry.ensureTelemetryOptions(instances.targetToInstances, currentData)

            for ((target, targetAppWidgetIds) in instances.targetToInstances) {
                // Composing and delivering never suspends, so cancellation is checked here.
                ensureActive()
                val appWidgetIds = targetAppWidgetIds.toIntArray()
                try {
                    val remoteViews = composer.compose(currentData, target, widgetName)
                    appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Every placement of the target shares the failed composition or delivery,
                    // so retrying them one by one would fail the same way.
                    failures.addFailedRenderedTarget(
                        target = target,
                        appWidgetIds = appWidgetIds,
                        intIdsToStringIds = instances.intIdsToStringIds,
                        error = e,
                    )
                }
            }
            // A cancelled call throws CancellationException rather than the collected failures.
            ensureActive()
            failures.throwIfAny()
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
     * The returned options do not contain [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID], which is
     * library-internal: the identifier is returned as [WidgetInstanceInfo.widgetId] instead.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @return Active instances for [widgetName], or an empty list if none are placed.
     * @throws IllegalArgumentException if [widgetName] cannot be resolved, see
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @throws IllegalStateException if [widgetName] cannot be resolved.
     * @throws RuntimeException if the placements or their options cannot be read, rather than
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
                val accumulator =
                    accumulators.getOrPut(widgetId) {
                        InstanceAccumulator(
                            Bundle(options).apply {
                                remove(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
                            }
                        )
                    }
                accumulator.placementsPerSurface[surface] =
                    accumulator.placementsPerSurface.getOrDefault(surface, 0) + 1
            }

            accumulators.map { (widgetId, accumulator) ->
                WidgetInstanceInfo(
                    widgetName = widgetName,
                    widgetId = widgetId,
                    options = accumulator.options,
                    placementsPerSurface = accumulator.placementsPerSurface,
                )
            }
        }

    /** Collects the placements sharing one widget instance String identifier. */
    private class InstanceAccumulator(val options: Bundle) {
        val placementsPerSurface: MutableObjectIntMap<GlanceSurface> = MutableObjectIntMap()
    }

    /**
     * Sets dynamic preview data rendered in host widget pickers for the specified widget
     * definition.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param previewData Declarative template data payload implementing [GlanceAdaptiveTemplate].
     * @throws IllegalArgumentException if [widgetName] cannot be resolved, see
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @throws IllegalStateException if [widgetName] cannot be resolved.
     */
    override suspend fun setPreview(widgetName: String, previewData: GlanceAdaptiveTemplate): Unit =
        withContext(Dispatchers.IO) {
            // Resolved before the API level check, so that developer errors fail on every API
            // level, including in tests on older devices.
            val componentName = repository.resolveReceiverComponent(widgetName)
            // Early return on API < 35: AppWidgetManager.setWidgetPreview is not supported.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                return@withContext
            }

            try {
                // Match against the installed provider before expensive composition.
                val providerInfo = findInstalledProvider(componentName) ?: return@withContext

                val targetToPreviews = composer.groupPreviewsByRenderTarget(listOf(providerInfo))
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
     * Requests that the launcher pin the receiver declaring [widgetName] to the home screen.
     *
     * Mirrors `GlanceAppWidgetManager.requestPinGlanceAppWidget`, apart from [widgetId] and
     * [options]. [AppWidgetManager.requestPinAppWidget] only hands its extras to the launcher, so
     * they cannot reach the pinned widget that way. When either is set, the request is instead sent
     * with a callback handled by [RequestPinCallbackReceiver], which stores them in the options of
     * the pinned widget. A widgetId is generated here if only [options] are set, so that the
     * callback always stores an explicit widgetId.
     *
     * There is no success or failure callback: once pinned, the widget is updated through
     * [GlanceAdaptiveWidgetReceiver] like a widget placed from the picker, and the platform reports
     * nothing if the user dismisses the request.
     *
     * @param widgetName Developer widget definition String identifier matching
     *   [GlanceAdaptiveWidgetReceiver.widgetName].
     * @param widgetId Widget instance String identifier to assign to the pinned widget. If null,
     *   one is generated.
     * @param initialData Declarative template data payload implementing [GlanceAdaptiveTemplate] to
     *   render as the preview shown while the launcher asks the user to confirm, if any.
     * @param options Configuration options to store in the options of the pinned widget.
     * @return true if the request was handed to the launcher, false if pinning is not supported or
     *   the launcher refused the request.
     * @throws IllegalArgumentException if [widgetId] is blank, if [widgetName] cannot be resolved
     *   (see [GlanceAdaptiveWidgetReceiver.widgetName]), or if its provider cannot be placed on the
     *   home screen.
     * @throws IllegalStateException if [widgetName] cannot be resolved.
     */
    override suspend fun requestPin(
        widgetName: String,
        widgetId: String?,
        initialData: GlanceAdaptiveTemplate?,
        options: Bundle,
    ): Boolean =
        withContext(Dispatchers.IO) {
            // Validated before the API level and launcher checks, so that developer errors fail on
            // every device.
            val requestedWidgetId = requireValidRequestedWidgetId(widgetId)
            val componentName = repository.resolveReceiverComponent(widgetName)
            // A disabled receiver is not an installed provider, so it cannot be pinned.
            val providerInfo = findInstalledProvider(componentName) ?: return@withContext false
            // The platform rejects pin requests for providers that cannot be placed on the home
            // screen, so this developer error is reported here, on every API level.
            require(providerInfo.isHomeScreenProvider) {
                "widgetName $widgetName cannot be pinned: ${componentName.className} does not " +
                    "declare the home_screen widgetCategory"
            }
            // Early return on API < 26: AppWidgetManager.requestPinAppWidget is not supported.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return@withContext false
            }
            if (!appWidgetManager.isRequestPinAppWidgetSupported) return@withContext false

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
                pinCallback(requestedWidgetId, options),
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
     * @param widgetId Widget instance String identifier to assign to the configured widget. If
     *   null, the one already stored for the widget is kept, or one is generated if there is none.
     * @param options Configuration options to store in the options of the configured widget.
     * @throws IllegalArgumentException if [activity]'s intent does not carry a valid
     *   [AppWidgetManager.EXTRA_APPWIDGET_ID], or if [widgetId] is blank.
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
            requireValidRequestedWidgetId(widgetId)
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
     * Returns the callback of a pin request, which stores [requestedWidgetId] and [options] in the
     * options of the pinned widget, or null if there is nothing to store.
     */
    private fun pinCallback(requestedWidgetId: String?, options: Bundle): PendingIntent? {
        if (requestedWidgetId == null && options.isEmpty) {
            // Nothing to store: the widget keeps the widgetId that GlanceAdaptiveWidgetReceiver
            // generates once it is placed. Otherwise, the widgetId is generated now, see
            // optionsWithWidgetId.
            return null
        }
        return RequestPinCallbackReceiver.createCallback(
            context = context,
            options = optionsWithWidgetId(requestedWidgetId, options),
        )
    }

    /**
     * Validates the widget instance identifier requested through [widgetId] by [requestPin] and
     * [setConfigurationResult]. `null` means that none is requested, while a blank identifier can
     * never be assigned, so it can only be a caller bug.
     *
     * @return [widgetId], or null if none is requested.
     * @throws IllegalArgumentException if [widgetId] is blank.
     */
    private fun requireValidRequestedWidgetId(widgetId: String?): String? {
        require(widgetId == null || widgetId.isNotBlank()) {
            "widgetId must not be blank. Pass null to keep the current one or to generate one."
        }
        return widgetId
    }

    /**
     * Returns a copy of [options] with [GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID] set to
     * [requestedWidgetId], or a newly generated identifier if [requestedWidgetId] is null.
     *
     * The identifier is generated now rather than left to the receiver once the widget is placed,
     * so that the stored options always carry an explicit final widgetId and never depend on a
     * fallback one the receiver may already have assigned.
     */
    private fun optionsWithWidgetId(requestedWidgetId: String?, options: Bundle): Bundle =
        Bundle(options).apply {
            putString(
                GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID,
                requestedWidgetId ?: WidgetOptionsCache.generateWidgetId(),
            )
        }

    /**
     * Returns the installed [AppWidgetProviderInfo] of the receiver [componentName], or null if it
     * is not an installed AppWidget provider, for example because it is disabled.
     */
    private fun findInstalledProvider(componentName: ComponentName): AppWidgetProviderInfo? {
        val installedProviders =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appWidgetManager.getInstalledProvidersForPackage(
                    /* packageName= */ context.packageName,
                    /* profile= */ null,
                )
            } else {
                // getInstalledProvidersForPackage is only available from API 26.
                appWidgetManager.installedProviders
            }
        for (idx in installedProviders.indices) {
            val info = installedProviders[idx]
            if (info.provider == componentName) return info
        }
        Log.w(TAG, "Component $componentName is not an installed AppWidgetProvider")
        return null
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
