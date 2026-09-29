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
import android.os.Bundle
import android.util.Log
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import kotlinx.coroutines.CancellationException

/**
 * Writes xFF telemetry extras into the [AppWidgetManager] options bundle of placed widgets. The
 * host reads them from there to attribute each placement to a template, layout archetype and
 * library version.
 *
 * Each write is one blocking `updateAppWidgetOptions` Binder call to system_server, after which the
 * system broadcasts `ACTION_APPWIDGET_OPTIONS_CHANGED` back to the provider. Writes are skipped
 * when the bundle already holds the current values. Failures are logged and contained per
 * [RenderTarget] and per widget, so telemetry never blocks the RemoteViews update that follows it
 * in [BaseWidgetDelegate.pushUpdate].
 */
internal class WidgetTelemetryHandler(private val appWidgetManager: AppWidgetManager) {

    /**
     * Ensures every widget in [targetToInstances] carries the telemetry extras for [data].
     *
     * The archetype is resolved once per [RenderTarget], from the same surface and normalized
     * dimensions that [GlanceRemoteViewsComposer.compose] renders with, so the reported archetype
     * is the rendered one. Must not be called on the main thread.
     *
     * @param targetToInstances appWidgetIds as grouped by
     *   [GlanceRemoteViewsComposer.groupInstancesByRenderTarget].
     * @param data Template payload being pushed.
     */
    fun ensureTelemetryOptions(
        targetToInstances: Map<RenderTarget, List<Int>>,
        data: AdaptiveGlanceTemplate,
    ) {
        val templateId = data.templateId
        for ((target, appWidgetIds) in targetToInstances) {
            val archetypeId =
                try {
                    AppWidgetTemplateRegistry.resolveArchetypeId(
                        data = data,
                        surface = target.surface,
                        dimensions = target.dimensions,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Leave this target's extras unchanged rather than write partial values.
                    Log.w(
                        TAG,
                        "Error resolving archetype for surface ${target.surface}, size " +
                            "${target.dimensions}",
                        e,
                    )
                    continue
                }
            for (i in appWidgetIds.indices) {
                val appWidgetId = appWidgetIds[i]
                try {
                    ensureTelemetryOptionsForWidget(appWidgetId, templateId, archetypeId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating telemetry options for appWidgetId $appWidgetId", e)
                }
            }
        }
    }

    private fun ensureTelemetryOptionsForWidget(
        appWidgetId: Int,
        templateId: String,
        archetypeId: String?,
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId) ?: return
        if (!needsTelemetryUpdate(options, templateId, archetypeId)) return

        // Pre-populate the receiver's options cache before calling updateAppWidgetOptions so that
        // the system's resulting ACTION_APPWIDGET_OPTIONS_CHANGED broadcast does not mistake a
        // cold cache for a dimension change and trigger a recursive onUpdate.
        val widgetId =
            GlanceAdaptiveWidgetReceiver.cacheWidgetOptionsIfMissing(appWidgetId, options)

        // Create a delta bundle containing only the telemetry keys (and EXTRA_WIDGET_ID if not yet
        // persisted). Since AppWidgetManager.updateAppWidgetOptions merges via Bundle.putAll(),
        // passing only delta keys avoids overwriting concurrent host updates (e.g. dimension
        // changes).
        val telemetryOptions =
            Bundle().apply {
                if (
                    options.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID).isNullOrBlank()
                ) {
                    putString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID, widgetId)
                }
                putBoolean(EXTRA_XFF_ENABLED, true)
                putString(EXTRA_XFF_TEMPLATE_ID, templateId)
                putString(EXTRA_XFF_SDK_VERSION, XFF_SDK_VERSION)
                // Note: Because updateAppWidgetOptions merges via Bundle.putAll(), Bundle.remove()
                // would not clear an existing key in system_server. Writing null explicitly
                // overwrites any stale archetype ID during putAll().
                putString(EXTRA_XFF_ARCHETYPE_ID, archetypeId)
            }
        appWidgetManager.updateAppWidgetOptions(appWidgetId, telemetryOptions)
    }

    private fun needsTelemetryUpdate(
        options: Bundle,
        templateId: String,
        archetypeId: String?,
    ): Boolean =
        !options.getBoolean(EXTRA_XFF_ENABLED, false) ||
            options.getString(EXTRA_XFF_TEMPLATE_ID) != templateId ||
            options.getString(EXTRA_XFF_SDK_VERSION) != XFF_SDK_VERSION ||
            options.getString(EXTRA_XFF_ARCHETYPE_ID) != archetypeId

    companion object {
        private const val TAG = "WidgetTelemetryHandler"

        // Hosts read the keys below from the options bundle by their literal string values, so the
        // values are a host contract: changing one breaks host-side telemetry. The constants stay
        // internal because no app code reads them.

        /**
         * Extra boolean flag indicating that this widget is managed by the xFF declarative adaptive
         * framework.
         */
        const val EXTRA_XFF_ENABLED: String = "androidx.glance.adaptive.extra.XFF_ENABLED"

        /** Extra string indicating the active template identifier for this adaptive widget. */
        const val EXTRA_XFF_TEMPLATE_ID: String = "androidx.glance.adaptive.extra.XFF_TEMPLATE_ID"

        /** Extra string indicating the compiled version of the xFF Jetpack library. */
        const val EXTRA_XFF_SDK_VERSION: String = "androidx.glance.adaptive.extra.XFF_SDK_VERSION"

        /**
         * Extra string indicating the selected layout archetype for the current widget dimensions.
         */
        const val EXTRA_XFF_ARCHETYPE_ID: String = "androidx.glance.adaptive.extra.XFF_ARCHETYPE_ID"

        // TODO(b/561481040): Populate EXTRA_XFF_DROPPED_SLOTS once responsive slot compaction
        // detection is implemented.
        /**
         * Extra boolean flag indicating if secondary optional template slots were omitted due to
         * responsive compaction.
         */
        const val EXTRA_XFF_DROPPED_SLOTS: String =
            "androidx.glance.adaptive.extra.XFF_DROPPED_SLOTS"

        /**
         * Current xFF SDK version string written to widget options bundles.
         *
         * Dynamically loaded from the version metadata file generated by the AndroidX build system
         * (`META-INF/androidx.glance.adaptive_adaptive-appwidget.version`), as `BuildConfig`
         * generation is disabled in AndroidX libraries. The literal fallback is used only in host
         * unit test environments where AAR resources are not packaged.
         */
        val XFF_SDK_VERSION: String by
            lazy(LazyThreadSafetyMode.PUBLICATION) {
                runCatching {
                    WidgetTelemetryHandler::class
                        .java
                        .classLoader
                        ?.getResourceAsStream(
                            "META-INF/androidx.glance.adaptive_adaptive-appwidget.version"
                        )
                        ?.bufferedReader()
                        ?.use { it.readLine()?.trim() }
                        ?.takeIf { it.isNotEmpty() }
                }
                    .getOrNull()
                    // Generic fallback for host-side JVM unit tests where AAR META-INF resources
                    // are not packaged; avoids manual updates across library version bumps.
                    ?: "0.0.0"
            }
    }
}
