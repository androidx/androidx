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
import android.os.Bundle
import android.util.SizeF
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
 *
 * @property surface Host surface the composition targets.
 * @property dimensions Guaranteed minimum container size, used whenever [sizes] is not.
 * @property sizes Distinct exact sizes the host can display the instance at
 *   ([AppWidgetManager.OPTION_APPWIDGET_SIZES], API 31+), or empty if the host does not report
 *   them.
 */
internal data class RenderTarget(
    val surface: AppWidgetSurface,
    val dimensions: Dimensions,
    val sizes: List<Dimensions> = emptyList(),
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
                        sizes = hostReportedSizes(options),
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
     *
     * On API 31+, a home screen [target] with [RenderTarget.sizes] is rendered once per size into a
     * size-mapped [RemoteViews], so the host picks the layout for its current size and orientation.
     * Otherwise [data] is rendered once at [RenderTarget.dimensions].
     */
    suspend fun compose(
        data: GlanceAdaptiveTemplate,
        target: RenderTarget,
        widgetName: String? = null,
    ): RemoteViews {
        val appContext = context.applicationContext ?: context
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                target.surface == AppWidgetSurface.MOBILE_HOME_SCREEN &&
                target.sizes.isNotEmpty()
        ) {
            // The minimum dimensions pair the portrait width with the landscape height, so a
            // single RemoteViews rendered at them fits neither orientation and the host stretches
            // it. Let the host pick the rendering for each of its sizes instead.
            // TODO(b/576689014): Capture RemoteCompose output once instead of once per size when
            //  RemoteCompose renderers select their layout inside the document.
            val sizeToRemoteViews = LinkedHashMap<SizeF, RemoteViews>(target.sizes.size)
            for (i in target.sizes.indices) {
                val size = target.sizes[i]
                sizeToRemoteViews[SizeF(size.widthDp.toFloat(), size.heightDp.toFloat())] =
                    composeAt(data, target.surface, size, widgetName, appContext)
            }
            return RemoteViews(sizeToRemoteViews)
        }
        return composeAt(data, target.surface, target.dimensions, widgetName, appContext)
    }

    /** Renders [data] once for [surface] placed at [dimensions]. */
    private suspend fun composeAt(
        data: GlanceAdaptiveTemplate,
        surface: AppWidgetSurface,
        dimensions: Dimensions,
        widgetName: String?,
        appContext: Context,
    ): RemoteViews =
        AppWidgetTemplateRegistry.render(data, surface.withDimensions(dimensions), widgetName)
            .toRemoteViews(appContext, dimensions)

    /** Reads the guaranteed minimum size the host reports for a placed instance, in dp. */
    private fun hostReportedDimensions(options: Bundle): Dimensions {
        return dimensionsOf(
            widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        )
    }

    /**
     * Reads the distinct exact sizes the host reports for a placed instance, in dp, in the order
     * the host reports them.
     *
     * Returns an empty list before API 31 or when the host reports no usable size. Keeps at most
     * [MAX_SIZED_REMOTE_VIEWS] sizes, the most a size-mapped [RemoteViews] accepts.
     */
    private fun hostReportedSizes(options: Bundle): List<Dimensions> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        val sizes =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                options.getParcelableArrayList(
                    AppWidgetManager.OPTION_APPWIDGET_SIZES,
                    SizeF::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            } ?: return emptyList()
        val distinctSizes = LinkedHashSet<Dimensions>()
        for (i in sizes.indices) {
            val dimensions =
                dimensionsOf(
                    widthDp = sizes[i].width.roundToInt(),
                    heightDp = sizes[i].height.roundToInt(),
                )
            if (dimensions != UNSPECIFIED_DIMENSIONS) distinctSizes.add(dimensions)
            if (distinctSizes.size == MAX_SIZED_REMOTE_VIEWS) break
        }
        return distinctSizes.toList()
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

        /** Most sizes a size-mapped [RemoteViews] accepts. */
        private const val MAX_SIZED_REMOTE_VIEWS = 16

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
