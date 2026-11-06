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
import android.os.Bundle
import android.widget.RemoteViews
import androidx.collection.IntObjectMap
import androidx.collection.MutableIntObjectMap
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurfaceDetector
import androidx.glance.adaptive.core.selection.Dimensions
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

/**
 * Identifies one RemoteViews composition: everything sharing a surface and container dimensions
 * resolves to the same layout and can be rendered once.
 */
internal data class RenderTarget(
    val surface: AppWidgetSurface,
    val dimensions: Dimensions,
)

/**
 * The placements of one update, bucketed by [RenderTarget].
 *
 * @property targetToInstances The appWidgetIds sharing each [RenderTarget].
 * @property intIdsToStringIds The widget instance String identifier stored in the options read for
 *   each grouped appWidgetId, for the placements that have one.
 */
internal class GroupedInstances(
    val targetToInstances: Map<RenderTarget, List<Int>>,
    val intIdsToStringIds: IntObjectMap<String>,
)

/** A single widget picker entry: one provider's preview for one host category. */
internal data class PreviewTarget(
    val provider: ComponentName,
    val category: Int,
)

/**
 * Translates [GlanceAdaptiveTemplate] payloads into [RemoteViews].
 *
 * Owns every decision about *how* a composition is produced: which host surface and container
 * dimensions it targets, what can share a single composition, and how that context reaches
 * templates through the placed surface's
 * [androidx.glance.adaptive.core.selection.GlanceSurface.dimensions]. Placed instances and picker
 * previews differ only in where their [RenderTarget] comes from, so both funnel through the same
 * [compose] call. Callers retain the orchestration concerns of resolving what to render and handing
 * the resulting [RemoteViews] to [AppWidgetManager].
 */
internal class GlanceRemoteViewsComposer(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context),
) {
    /**
     * Buckets every appWidgetId in [componentToAppWidgetIds] by the [RenderTarget] the host has
     * sized it to.
     *
     * Grouping by surface *and* container dimensions keeps the number of compositions minimal while
     * ensuring differently sized instances resolve their own layout tier and slot dimensions:
     * grouping by surface alone would collapse them into one composition and give one instance the
     * other's layout.
     *
     * A placement whose options cannot be read is left out and reported to [onUnreadableOptions],
     * so that it does not prevent the others from being grouped.
     */
    fun groupInstancesByRenderTarget(
        componentToAppWidgetIds: Map<ComponentName, IntArray>,
        onUnreadableOptions: (appWidgetId: Int, error: Exception) -> Unit,
    ): GroupedInstances {
        val targetToInstances = mutableMapOf<RenderTarget, MutableList<Int>>()
        val intIdsToStringIds = MutableIntObjectMap<String>()
        for ((_, appWidgetIds) in componentToAppWidgetIds) {
            for (appWidgetId in appWidgetIds) {
                val options =
                    try {
                        appWidgetManager.getAppWidgetOptions(appWidgetId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        onUnreadableOptions(appWidgetId, e)
                        continue
                    }
                options
                    ?.getString(GlanceAdaptiveWidgetReceiver.EXTRA_WIDGET_ID)
                    ?.takeIf { it.isNotBlank() }
                    ?.let { intIdsToStringIds[appWidgetId] = it }
                val target =
                    RenderTarget(
                        surface = AppWidgetSurfaceDetector.fromAppWidgetOptions(options),
                        dimensions = hostReportedDimensions(options),
                    )
                targetToInstances.getOrPut(target) { mutableListOf() }.add(appWidgetId)
            }
        }
        return GroupedInstances(targetToInstances, intIdsToStringIds)
    }

    /**
     * Buckets every picker entry declared by [providers] by the [RenderTarget] it resolves to.
     *
     * A provider can declare several categories (for example home screen and keyguard), each of
     * which is a separate picker entry that may target a different surface. Grouping by declared
     * size as well means two providers of the same shape still share one composition, while a
     * larger provider gets its own layout tier instead of inheriting a smaller one's.
     */
    fun groupPreviewsByRenderTarget(
        providers: List<AppWidgetProviderInfo>
    ): Map<RenderTarget, List<PreviewTarget>> {
        val targetToPreviews = mutableMapOf<RenderTarget, MutableList<PreviewTarget>>()
        for (i in providers.indices) {
            val providerInfo = providers[i]
            val dimensions = declaredDimensions(providerInfo)
            val previewCategories =
                AppWidgetSurfaceDetector.resolvePreviewCategories(providerInfo.widgetCategory)
            for (j in previewCategories.indices) {
                val (surface, category) = previewCategories[j]
                targetToPreviews
                    .getOrPut(RenderTarget(surface, dimensions)) { mutableListOf() }
                    .add(PreviewTarget(providerInfo.provider, category))
            }
        }
        return targetToPreviews
    }

    /**
     * Resolves the [RenderTarget] of the preview shown while the launcher asks the user to pin
     * [providerInfo].
     *
     * A pinned widget is always placed on the home screen. Like a picker preview, it has no placed
     * instance yet, so its declared size stands in for a host-reported one.
     */
    fun pinPreviewRenderTarget(providerInfo: AppWidgetProviderInfo): RenderTarget =
        RenderTarget(
            surface = AppWidgetSurface.MOBILE_HOME_SCREEN,
            dimensions = declaredDimensions(providerInfo),
        )

    /**
     * Renders [data] for [target], dispatching the resulting
     * [androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput] into a platform
     * [RemoteViews].
     */
    suspend fun compose(
        data: GlanceAdaptiveTemplate,
        target: RenderTarget,
        widgetName: String? = null,
    ): RemoteViews {
        val appContext = context.applicationContext ?: context
        val dimensions = target.dimensions
        val surface = target.surface.withDimensions(dimensions)
        return AppWidgetTemplateRegistry.render(data, surface, widgetName)
            .toRemoteViews(appContext, dimensions)
    }

    /** Reads the guaranteed minimum size the host reports for a placed instance, in dp. */
    private fun hostReportedDimensions(options: Bundle): Dimensions {
        return dimensionsOf(
            widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        )
    }

    /**
     * Reads the default placement size declared inside the [providerInfo] in the XML, in dp.
     *
     * A picker or pin preview has no placed instance and therefore no host-reported size, so the
     * declared default size (`minWidth` by `minHeight`) stands in for it. `minResizeWidth` and
     * `minResizeHeight` are ignored here: hosts size preview slots to the widget's initial
     * placement dimensions, while resize bounds only apply after placement (where
     * [hostReportedDimensions] takes over).
     */
    private fun declaredDimensions(providerInfo: AppWidgetProviderInfo): Dimensions {
        val density = context.resources.displayMetrics.density
        if (density <= 0f) return UNSPECIFIED_DIMENSIONS
        return dimensionsOf(
            widthDp = (providerInfo.minWidth / density).roundToInt(),
            heightDp = (providerInfo.minHeight / density).roundToInt(),
        )
    }

    companion object {
        /** Used whenever no usable container size is available. */
        private val UNSPECIFIED_DIMENSIONS = Dimensions(0, 0)

        /**
         * Normalizes a resolved dp pair into [Dimensions].
         *
         * Returns [UNSPECIFIED_DIMENSIONS] when either bound is missing or non-positive so
         * templates fall back to their most compact tier.
         */
        private fun dimensionsOf(widthDp: Int, heightDp: Int): Dimensions =
            if (widthDp > 0 && heightDp > 0) {
                Dimensions(widthDp = widthDp, heightDp = heightDp)
            } else {
                UNSPECIFIED_DIMENSIONS
            }
    }
}
