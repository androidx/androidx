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
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.ui.AppWidgetTemplateRegistry
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurfaceDetector
import androidx.glance.adaptive.appwidget.ui.selection.LocalContainerDimensions
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import kotlin.math.roundToInt

/**
 * Identifies one RemoteViews composition: everything sharing a surface and container dimensions
 * resolves to the same layout and can be rendered once.
 */
internal data class RenderTarget(
    val surface: AppWidgetGlanceSurface,
    val dimensions: Dimensions,
)

/** A single widget picker entry: one provider's preview for one host category. */
internal data class PreviewTarget(
    val provider: ComponentName,
    val category: Int,
)

/**
 * Translates [AdaptiveGlanceTemplate] payloads into [RemoteViews].
 *
 * Owns every decision about *how* a composition is produced: which host surface and container
 * dimensions it targets, what can share a single composition, and how that context reaches
 * templates through [LocalContainerDimensions] and [androidx.glance.LocalSize]. Placed instances
 * and picker previews differ only in where their [RenderTarget] comes from, so both funnel through
 * the same [compose] call. Callers retain the orchestration concerns of resolving what to render
 * and handing the resulting [RemoteViews] to [AppWidgetManager].
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
     */
    fun groupInstancesByRenderTarget(
        componentToAppWidgetIds: Map<ComponentName, IntArray>
    ): Map<RenderTarget, List<Int>> {
        val targetToInstances = mutableMapOf<RenderTarget, MutableList<Int>>()
        for ((_, appWidgetIds) in componentToAppWidgetIds) {
            for (appWidgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val target =
                    RenderTarget(
                        surface = AppWidgetSurfaceDetector.fromAppWidgetOptions(options),
                        dimensions = hostReportedDimensions(options),
                    )
                targetToInstances.getOrPut(target) { mutableListOf() }.add(appWidgetId)
            }
        }
        return targetToInstances
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
     * Renders [data] for [target], keeping [androidx.glance.LocalSize] and
     * [LocalContainerDimensions] consistent so templates and the Glance layout pass agree on the
     * space available.
     */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    suspend fun compose(data: AdaptiveGlanceTemplate, target: RenderTarget): RemoteViews {
        val dimensions = target.dimensions
        val compositionSize =
            if (dimensions == UNSPECIFIED_DIMENSIONS) {
                DpSize.Unspecified
            } else {
                DpSize(dimensions.widthDp.dp, dimensions.heightDp.dp)
            }
        return glanceRemoteViews
            .compose(context = context.applicationContext ?: context, size = compositionSize) {
                CompositionLocalProvider(LocalContainerDimensions provides dimensions) {
                    AppWidgetTemplateRegistry.render(data, target.surface)
                }
            }
            .remoteViews
    }

    /** Reads the guaranteed minimum size the host reports for a placed instance, in dp. */
    private fun hostReportedDimensions(options: Bundle): Dimensions {
        return dimensionsOf(
            widthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0),
            heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        )
    }

    /**
     * Reads the guaranteed minimum size [providerInfo] declares in the manifest, in dp.
     *
     * A picker preview has no placed instance and therefore no host-reported size, so the declared
     * size stands in for it. Using the smallest placement the provider allows matches the
     * guaranteed-minimum semantics of [hostReportedDimensions], so the archetype shown in the
     * picker is the one the widget is guaranteed to be able to render once placed.
     */
    private fun declaredDimensions(providerInfo: AppWidgetProviderInfo): Dimensions {
        val density = context.resources.displayMetrics.density
        if (density <= 0f) return UNSPECIFIED_DIMENSIONS
        return dimensionsOf(
            widthDp = (providerInfo.smallestWidthPx / density).roundToInt(),
            heightDp = (providerInfo.smallestHeightPx / density).roundToInt(),
        )
    }

    companion object {
        /** Used whenever no usable container size is available. */
        private val UNSPECIFIED_DIMENSIONS = Dimensions(0, 0)

        /**
         * Process-wide [GlanceRemoteViews] instance shared across [GlanceRemoteViewsComposer]
         * instances so cached layout-index assignments survive across
         * `GlanceAdaptiveWidgetManager(context)` calls (such as widget resize updates), allowing
         * hosts to rebind unchanged trees and forcing re-inflation when a resized widget switches
         * layout structure.
         */
        @OptIn(ExperimentalGlanceRemoteViewsApi::class)
        @Volatile
        private var glanceRemoteViews = GlanceRemoteViews()

        @OptIn(ExperimentalGlanceRemoteViewsApi::class)
        @VisibleForTesting
        internal fun resetForTesting() {
            glanceRemoteViews = GlanceRemoteViews()
        }

        /**
         * Normalizes a resolved dp pair into [Dimensions].
         *
         * Returns [UNSPECIFIED_DIMENSIONS] when either bound is missing or non-positive so
         * templates fall back to their most compact tier and [androidx.glance.LocalSize] stays
         * consistent with [LocalContainerDimensions].
         */
        private fun dimensionsOf(widthDp: Int, heightDp: Int): Dimensions =
            if (widthDp > 0 && heightDp > 0) {
                Dimensions(widthDp = widthDp, heightDp = heightDp)
            } else {
                UNSPECIFIED_DIMENSIONS
            }
    }
}

/**
 * Smallest width, in pixels, at which a host may place this provider.
 *
 * `minResizeWidth` only narrows the guarantee when the provider actually opts into horizontal
 * resizing, and is ignored when left unset in the manifest.
 */
private val AppWidgetProviderInfo.smallestWidthPx: Int
    get() =
        if (resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 && minResizeWidth > 0) {
            minOf(minWidth, minResizeWidth)
        } else {
            minWidth
        }

/** Smallest height, in pixels, at which a host may place this provider. */
private val AppWidgetProviderInfo.smallestHeightPx: Int
    get() =
        if (resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0 && minResizeHeight > 0) {
            minOf(minHeight, minResizeHeight)
        } else {
            minHeight
        }
