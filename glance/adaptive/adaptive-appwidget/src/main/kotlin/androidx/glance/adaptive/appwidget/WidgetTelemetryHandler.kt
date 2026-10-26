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
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import kotlinx.coroutines.CancellationException

/**
 * Writes xFF telemetry extras into the [AppWidgetManager] options bundle of placed widgets. The
 * platform (`AppWidgetServiceImpl`) reads allowlisted keys during StatsD pulls
 * (`AppWidgetStatusPulled`) to attribute each placement to a template type, library version code,
 * and responsive compaction state.
 *
 * Each write is one blocking `updateAppWidgetOptions` Binder call to `system_server`, after which
 * the system broadcasts `ACTION_APPWIDGET_OPTIONS_CHANGED` back to the provider. Writes are skipped
 * when the bundle already holds the current values. Failures are logged and contained per
 * [RenderTarget] and per widget, so telemetry never blocks the RemoteViews update that follows it
 * in [BaseWidgetDelegate.pushUpdate].
 */
internal class WidgetTelemetryHandler(private val appWidgetManager: AppWidgetManager) {

    /**
     * Ensures every widget in [targetToInstances] carries the telemetry extras for [data].
     *
     * Must not be called on the main thread.
     *
     * @param targetToInstances appWidgetIds as grouped by
     *   [GlanceRemoteViewsComposer.groupInstancesByRenderTarget].
     * @param data Template payload being pushed.
     */
    fun ensureTelemetryOptions(
        targetToInstances: Map<RenderTarget, List<Int>>,
        data: GlanceAdaptiveTemplate,
    ) {
        val templateId = data.templateId
        for ((_, appWidgetIds) in targetToInstances) {
            // TODO(b/561481040): Detect responsive slot compaction per target.
            val hasDroppedSlots = false
            for (i in appWidgetIds.indices) {
                val appWidgetId = appWidgetIds[i]
                try {
                    ensureTelemetryOptionsForWidget(appWidgetId, templateId, hasDroppedSlots)
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
        templateId: Int,
        hasDroppedSlots: Boolean,
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId) ?: return
        if (!needsTelemetryUpdate(options, templateId, hasDroppedSlots)) return

        // Pre-populate the receiver's options cache before calling updateAppWidgetOptions so that
        // the system's resulting ACTION_APPWIDGET_OPTIONS_CHANGED broadcast does not mistake a
        // cold cache for a dimension change and trigger a recursive onUpdate.
        val widgetId = WidgetOptionsCache.cacheWidgetOptionsIfMissing(appWidgetId, options)

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
                putBoolean(OPTION_ADAPTIVE_WIDGET_ENABLED, true)
                putInt(OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE, templateId)
                putInt(OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE, XFF_SDK_VERSION_CODE)
                putBoolean(OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS, hasDroppedSlots)
            }
        appWidgetManager.updateAppWidgetOptions(appWidgetId, telemetryOptions)
    }

    private fun needsTelemetryUpdate(
        options: Bundle,
        templateId: Int,
        hasDroppedSlots: Boolean,
    ): Boolean =
        !options.getBoolean(OPTION_ADAPTIVE_WIDGET_ENABLED, false) ||
            !options.containsKey(OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE) ||
            options.getInt(
                OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE,
                GlanceAdaptiveTemplate.TEMPLATE_ID_UNKNOWN,
            ) != templateId ||
            !options.containsKey(OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE) ||
            options.getInt(OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE, 0) != XFF_SDK_VERSION_CODE ||
            !options.containsKey(OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS) ||
            options.getBoolean(OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS, false) != hasDroppedSlots

    companion object {
        private const val TAG = "WidgetTelemetryHandler"

        // The platform (`AppWidgetServiceImpl`) reads the keys below from the options bundle when
        // allowlisted via DeviceConfig (`widget_logged_options_allowlist`) and logs them in
        // `AppWidgetStatusPulled`. Changing any literal value breaks platform telemetry collection.

        /**
         * Bundle option boolean flag indicating that this widget is managed by the xFF declarative
         * adaptive framework.
         */
        const val OPTION_ADAPTIVE_WIDGET_ENABLED: String =
            "android.appwidget.option.ADAPTIVE_WIDGET_ENABLED"

        /** Bundle option integer indicating the template type for this adaptive widget. */
        const val OPTION_ADAPTIVE_WIDGET_TEMPLATE_TYPE: String =
            "android.appwidget.option.ADAPTIVE_WIDGET_TEMPLATE_TYPE"

        /**
         * Bundle option integer indicating the compiled version code of the xFF Jetpack library.
         */
        const val OPTION_ADAPTIVE_WIDGET_SDK_VERSION_CODE: String =
            "android.appwidget.option.ADAPTIVE_WIDGET_SDK_VERSION_CODE"

        /**
         * Bundle option boolean flag indicating if secondary optional template slots were omitted
         * due to responsive compaction.
         */
        const val OPTION_ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS: String =
            "android.appwidget.option.ADAPTIVE_WIDGET_HAS_DROPPED_SLOTS"

        /**
         * Default fallback SDK version code (`1.0.0` -> `10000`) used in host JVM unit tests where
         * AAR `META-INF` resources are not packaged.
         */
        internal const val DEFAULT_SDK_VERSION_CODE: Int = 10_000

        /**
         * Parses a semantic version string (e.g., `"1.2.3-alpha01"`) into an integer version code
         * (`major * 10_000 + minor * 100 + patch`, e.g., `10203`), or returns `null` if malformed
         * or if `minor` or `patch` is outside `0..99`.
         */
        internal fun parseSdkVersionCode(version: String): Int? {
            val core = version.trim().substringBefore('-')
            val parts = core.split('.')
            val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            if (major < 0 || minor !in 0..99 || patch !in 0..99) return null
            return major * 10_000 + minor * 100 + patch
        }

        /**
         * Current xFF SDK version code written to widget options bundles.
         *
         * Dynamically loaded and parsed from the version metadata file generated by the AndroidX
         * build system (`META-INF/androidx.glance.adaptive_adaptive-appwidget.version`), as
         * `BuildConfig` generation is disabled in AndroidX libraries. Falls back to
         * [DEFAULT_SDK_VERSION_CODE] in host unit test environments where AAR resources are not
         * packaged.
         */
        val XFF_SDK_VERSION_CODE: Int by
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
                        ?.let { parseSdkVersionCode(it) }
                }
                    .getOrNull() ?: DEFAULT_SDK_VERSION_CODE
            }
    }
}
