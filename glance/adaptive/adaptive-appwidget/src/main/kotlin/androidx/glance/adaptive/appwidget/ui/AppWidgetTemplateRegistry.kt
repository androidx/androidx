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

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.ui.templates.track.TrackSizeSelector
import androidx.glance.adaptive.appwidget.ui.templates.track.TrackTemplateRenderer
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Assembles template data into [AppWidgetRenderOutput] by running an [ArchetypeSelector] and
 * handing its archetype to the paired [TemplateRenderer]. The archetype type is erased so
 * assemblers for different templates can share a map.
 */
private fun interface TemplateAssembler {
    fun assemble(
        data: GlanceAdaptiveTemplate,
        constraints: HostConstraints<AppWidgetSurface>,
    ): AppWidgetRenderOutput
}

/**
 * Registry mapping each `(templateClass, surface)` pair to the [ArchetypeSelector] and
 * [TemplateRenderer] that produce its [AppWidgetRenderOutput].
 *
 * Built-in defaults live in one map and per-widget overrides in another, both keyed the same way.
 * [render] checks the override for the widget first and falls back to the default. Within each map,
 * the last registration for a `(templateClass, surface)` pair wins, so register for all surfaces
 * first and for specific surfaces after.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AppWidgetTemplateRegistry {
    private val lock = Any()

    private data class Key(
        val templateClass: Class<out GlanceAdaptiveTemplate>,
        val surface: AppWidgetSurface,
    )

    /** Built-in defaults, registered by the library. */
    private val defaultMap = mutableMapOf<Key, TemplateAssembler>()

    /** Per-widget overrides keyed by `widgetName`, empty unless a widget registers one. */
    private val overrideMap = mutableMapOf<Pair<String, Key>, TemplateAssembler>()

    init {
        synchronized(lock) { registerDefaultTemplates() }
    }

    internal fun registerDefaultTemplates() {
        register(TrackTemplate::class.java, TrackSizeSelector, TrackTemplateRenderer)
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun resetForTesting() {
        synchronized(lock) {
            overrideMap.clear()
            defaultMap.clear()
            registerDefaultTemplates()
        }
    }

    /**
     * Registers the built-in default [selector] and [renderer] for [templateClass] on [surface], or
     * on every [AppWidgetSurface] when [surface] is `null`.
     */
    internal fun <T : GlanceAdaptiveTemplate, A : Any> register(
        templateClass: Class<T>,
        selector: ArchetypeSelector<T, AppWidgetSurface, A>,
        renderer: TemplateRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        val assembler = assembler(selector, renderer)
        synchronized(lock) { forEachKey(templateClass, surface) { defaultMap[it] = assembler } }
    }

    /**
     * Registers a [selector] and [renderer] override for [widgetName], taking precedence over the
     * built-in default for [templateClass] on [surface], or on every [AppWidgetSurface] when
     * [surface] is `null`.
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param A The archetype type produced by [selector] and drawn by [renderer].
     * @param widgetName Developer widget identifier matching
     *   [androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetReceiver.widgetName].
     * @param templateClass The runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param selector Resolves the layout archetype for [widgetName]. Note that
     *   [HostConstraints.dimensions] may be `(0, 0)` when the host has not reported a positive size
     *   or during widget picker preview rendering, so selectors must handle zero dimensions
     *   gracefully.
     * @param renderer Draws the archetype chosen by [selector] as [AppWidgetRenderOutput].
     * @param surface Optional [AppWidgetSurface] to target, or `null` for all surfaces.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : GlanceAdaptiveTemplate, A : Any> register(
        widgetName: String,
        templateClass: Class<T>,
        selector: ArchetypeSelector<T, AppWidgetSurface, A>,
        renderer: TemplateRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput>,
        surface: AppWidgetSurface? = null,
    ) {
        val assembler = assembler(selector, renderer)
        synchronized(lock) {
            forEachKey(templateClass, surface) { overrideMap[widgetName to it] = assembler }
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

    private fun <T : GlanceAdaptiveTemplate, A : Any> assembler(
        selector: ArchetypeSelector<T, AppWidgetSurface, A>,
        renderer: TemplateRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput>,
    ): TemplateAssembler = TemplateAssembler { data, constraints ->
        @Suppress("UNCHECKED_CAST") val typed = data as T
        renderer.render(typed, selector.select(typed, constraints), constraints)
    }

    /**
     * Resolves the override for [widgetName] (if any) or the default for the runtime class of
     * [data] on [HostConstraints.surface], runs its selector, and hands the resulting archetype to
     * its renderer.
     *
     * @param data Declarative template data payload.
     * @param constraints Host layout constraints and target AppWidget surface.
     * @param widgetName Optional widget identifier to check for a per-widget override first.
     * @return The [AppWidgetRenderOutput] produced by the registered renderer.
     * @throws IllegalArgumentException if no renderer is registered for the type of [data] on
     *   [HostConstraints.surface].
     */
    public fun <T : GlanceAdaptiveTemplate> render(
        data: T,
        constraints: HostConstraints<AppWidgetSurface>,
        widgetName: String? = null,
    ): AppWidgetRenderOutput {
        val key = Key(data.javaClass, constraints.surface)
        val assembler =
            synchronized(lock) {
                requireNotNull(widgetName?.let { overrideMap[it to key] } ?: defaultMap[key]) {
                    "No renderer registered for template: ${data.javaClass.name} on surface: " +
                        "${constraints.surface}"
                }
            }
        return assembler.assemble(data, constraints)
    }
}
