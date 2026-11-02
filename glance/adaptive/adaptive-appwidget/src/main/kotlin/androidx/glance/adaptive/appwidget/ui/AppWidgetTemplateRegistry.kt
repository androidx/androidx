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

package androidx.glance.adaptive.appwidget.ui

import androidx.annotation.VisibleForTesting
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.ui.templates.track.TrackSizeSelector
import androidx.glance.adaptive.appwidget.ui.templates.track.TrackTemplateRenderer
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/** A [TemplateRenderer] with its template type erased, so renderers can share a map. */
private typealias AnyTemplateRenderer =
    TemplateRenderer<GlanceAdaptiveTemplate, AppWidgetSurface, AppWidgetRenderOutput>

/**
 * Registry mapping each `(templateClass, surface)` pair to the [TemplateRenderer] that produces its
 * [AppWidgetRenderOutput].
 *
 * Built-in defaults live in one map and per-widget overrides in another, both keyed the same way.
 * [render] checks the override for the widget first and falls back to the default. Within each map,
 * the last registration for a `(templateClass, surface)` pair wins, so register for all surfaces
 * first and for specific surfaces after.
 */
internal object AppWidgetTemplateRegistry {
    private val lock = Any()

    private data class Key(
        val templateClass: Class<out GlanceAdaptiveTemplate>,
        val surface: AppWidgetSurface,
    )

    /** Built-in defaults, registered by the library. */
    private val defaultMap = mutableMapOf<Key, AnyTemplateRenderer>()

    /** Per-widget overrides keyed by `widgetName`, empty unless a widget registers one. */
    private val overrideMap = mutableMapOf<Pair<String, Key>, AnyTemplateRenderer>()

    init {
        synchronized(lock) { registerDefaultTemplates() }
    }

    private fun registerDefaultTemplates() {
        registerDefault(
            TrackTemplate::class.java,
            TemplateRenderer(TrackSizeSelector, TrackTemplateRenderer),
        )
    }

    @VisibleForTesting
    fun resetForTesting() {
        synchronized(lock) {
            overrideMap.clear()
            defaultMap.clear()
            registerDefaultTemplates()
        }
    }

    /**
     * Registers the built-in default [renderer] for [templateClass] on [surface], or on every
     * [AppWidgetSurface] when [surface] is `null`.
     */
    fun <T : GlanceAdaptiveTemplate> registerDefault(
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        val untyped = renderer.asUntyped()
        synchronized(lock) { forEachKey(templateClass, surface) { defaultMap[it] = untyped } }
    }

    /**
     * Registers a [renderer] override for [widgetName], taking precedence over the built-in default
     * for [templateClass] on [surface], or on every [AppWidgetSurface] when [surface] is `null`.
     * Apps reach it through [androidx.glance.adaptive.appwidget.registerRenderer].
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param widgetName Developer widget identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param templateClass The runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param renderer Draws [templateClass] for [widgetName] as [AppWidgetRenderOutput]. Note that
     *   the [androidx.glance.adaptive.core.ui.selection.GlanceSurface.dimensions] of the surface it
     *   receives may be `(0, 0)` when the host has not reported a positive size or during widget
     *   picker preview rendering, so renderers must handle zero dimensions gracefully.
     * @param surface Optional [AppWidgetSurface] to target, or `null` for all surfaces.
     */
    fun <T : GlanceAdaptiveTemplate> register(
        widgetName: String,
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        val untyped = renderer.asUntyped()
        synchronized(lock) {
            forEachKey(templateClass, surface) { overrideMap[widgetName to it] = untyped }
        }
    }

    /** Calls [action] with the key for [surface], or for every surface when [surface] is `null`. */
    private inline fun forEachKey(
        templateClass: Class<out GlanceAdaptiveTemplate>,
        surface: AppWidgetSurface?,
        action: (Key) -> Unit,
    ) {
        if (surface != null) {
            action(Key(templateClass, surface))
            return
        }
        val surfaces = AppWidgetSurface.ALL
        for (i in surfaces.indices) action(Key(templateClass, surfaces[i]))
    }

    /**
     * Casts to an untyped [AnyTemplateRenderer] so renderers for different templates can share a
     * map. Safe because [render] only hands a renderer data of the class it was registered for.
     */
    @Suppress("UNCHECKED_CAST")
    private fun TemplateRenderer<*, AppWidgetSurface, AppWidgetRenderOutput>.asUntyped():
        AnyTemplateRenderer = this as AnyTemplateRenderer

    /**
     * Resolves the override for [widgetName] (if any) or the default for the runtime class of
     * [data] on [surface] and renders [data] with it.
     *
     * @param data Declarative template data payload.
     * @param surface Target AppWidget surface, carrying the container size of the placement as
     *   [androidx.glance.adaptive.core.ui.selection.GlanceSurface.dimensions].
     * @param widgetName Optional widget identifier to check for a per-widget override first.
     * @return The [AppWidgetRenderOutput] produced by the registered renderer.
     * @throws IllegalArgumentException if no renderer is registered for the type of [data] on
     *   [surface].
     */
    fun <T : GlanceAdaptiveTemplate> render(
        data: T,
        surface: AppWidgetSurface,
        widgetName: String? = null,
    ): AppWidgetRenderOutput {
        val key = Key(data.javaClass, surface)
        val renderer =
            synchronized(lock) {
                requireNotNull(widgetName?.let { overrideMap[it to key] } ?: defaultMap[key]) {
                    "No renderer registered for template: ${data.javaClass.name} on surface: " +
                        "$surface"
                }
            }
        return renderer.render(data, surface)
    }
}
