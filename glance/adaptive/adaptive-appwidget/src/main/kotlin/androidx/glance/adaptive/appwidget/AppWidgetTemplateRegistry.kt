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

import androidx.annotation.VisibleForTesting
import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.renderer.TemplateRegistry
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TrackTemplate

/**
 * The [TemplateRegistry] of platform AppWidget surfaces, holding the library's built-in templates.
 */
internal object AppWidgetTemplateRegistry {
    private val defaultTrackRenderer =
        TemplateRenderer(TrackMatrixSelector, RemoteViewsMatrixRenderer)
    private val defaultTrackSelector =
        TemplateRenderer<TrackTemplate, AppWidgetSurface, MatrixContent>(
            TrackMatrixSelector::select
        )

    private val registry =
        TemplateRegistry(
            surfaces = AppWidgetSurface.ALL,
            defaultRenderers =
                object :
                    TemplateRegistry.DefaultRenderers<AppWidgetSurface, AppWidgetRenderOutput> {
                    override fun trackRenderer(
                        surface: AppWidgetSurface
                    ): TemplateRenderer<TrackTemplate, AppWidgetSurface, AppWidgetRenderOutput> =
                        defaultTrackRenderer
                },
        )

    private val selectorRegistry =
        TemplateRegistry(
            surfaces = AppWidgetSurface.ALL,
            defaultRenderers =
                object : TemplateRegistry.DefaultRenderers<AppWidgetSurface, MatrixContent> {
                    override fun trackRenderer(
                        surface: AppWidgetSurface
                    ): TemplateRenderer<TrackTemplate, AppWidgetSurface, MatrixContent> =
                        defaultTrackSelector
                },
        )

    @VisibleForTesting
    fun resetForTesting() {
        registry.resetForTesting()
    }

    /**
     * Registers a default [renderer] for [templateClass] on [surface], or on every
     * [AppWidgetSurface] when [surface] is `null`.
     */
    @VisibleForTesting
    fun <T : GlanceAdaptiveTemplate> registerDefault(
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        registry.registerDefault(templateClass, renderer, surface)
    }

    /**
     * Registers a [renderer] override for [widgetName], taking precedence over the built-in default
     * for [templateClass] on [surface], or on every [AppWidgetSurface] when [surface] is `null`.
     * Apps reach it through [androidx.glance.adaptive.appwidget.registerRenderer].
     *
     * @param widgetName Developer widget identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     */
    fun <T : GlanceAdaptiveTemplate> register(
        widgetName: String,
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        registry.registerOverride(widgetName, templateClass, renderer, surface)
    }

    /**
     * Renders [data] on [surface] with the override for [widgetName] (if any) or the default.
     *
     * @see TemplateRegistry.render
     */
    fun render(
        data: GlanceAdaptiveTemplate,
        surface: AppWidgetSurface,
        widgetName: String? = null,
    ): AppWidgetRenderOutput = registry.render(data, surface, widgetName)

    /**
     * Returns `true` when [surface]'s container size causes at least one non-blank optional slot in
     * [data] to be omitted from the built-in template's [MatrixContent], or `false` if no built-in
     * selector is registered for [data]'s class.
     */
    fun hasDroppedSlots(
        data: GlanceAdaptiveTemplate,
        surface: AppWidgetSurface,
    ): Boolean =
        try {
            selectorRegistry.render(data, surface).hasDroppedSlots
        } catch (_: IllegalStateException) {
            false
        }
}
