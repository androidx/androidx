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

package androidx.glance.adaptive.core.renderer

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate
import androidx.glance.adaptive.core.templates.TrackTemplate

/** A [TemplateRenderer] with its template type erased, so renderers can share a map. */
private typealias AnyTemplateRenderer<S, R> = TemplateRenderer<GlanceAdaptiveTemplate, S, R>

/**
 * Maps each `(templateClass, surface)` pair to the [TemplateRenderer] that turns a template into
 * host content of type [R].
 *
 * Each surface module keeps one registry for its own surface type [S] and output type [R], so the
 * registration logic is shared while the output stays host-specific, as with [TemplateRenderer].
 * - Built-in defaults live in one map and per-widget overrides in another, both keyed the same way.
 * - [render] checks the override for the widget first and falls back to the default.
 * - Within each map, the last registration for a `(templateClass, surface)` pair wins, so register
 *   for all surfaces first and for specific surfaces after.
 * - Surfaces match by tag, so a surface placed at a size finds the entries of its surface constant.
 *
 * Registrations and lookups are thread-safe.
 *
 * @param S The surface type implementing [GlanceSurface].
 * @param R The host content type produced by the registered renderers.
 * @param surfaces Every surface of type [S], targeted by registrations that don't name a surface.
 * @param defaultRenderers Supplies the built-in default [TemplateRenderer] for every library
 *   template.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateRegistry<S : GlanceSurface, R>(
    surfaces: List<S>,
    private val defaultRenderers: DefaultRenderers<S, R>,
) {
    /**
     * Supplies the built-in default [TemplateRenderer] for every library template on [S]. Adding a
     * built-in template adds an abstract method here so missing defaults fail at compile time.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface DefaultRenderers<S : GlanceSurface, R> {
        /** Returns the built-in default [TemplateRenderer] for [TrackTemplate] on [surface]. */
        public fun trackRenderer(surface: S): TemplateRenderer<TrackTemplate, S, R>
    }

    private val surfaces = surfaces.toList()
    private val lock = Any()

    /** Key of [defaultMap], shared by every widget unless overridden in [overrideMap]. */
    private data class TemplateSurfaceKey(
        val templateClass: Class<out GlanceAdaptiveTemplate>,
        val surface: GlanceSurface,
    )

    /** Built-in defaults, registered by the library. */
    private val defaultMap = mutableMapOf<TemplateSurfaceKey, AnyTemplateRenderer<S, R>>()

    /** Per-widget overrides keyed by `widgetName`, empty unless a widget registers one. */
    private val overrideMap =
        mutableMapOf<Pair<String, TemplateSurfaceKey>, AnyTemplateRenderer<S, R>>()

    init {
        registerBuiltInDefaults()
    }

    private fun registerBuiltInDefaults() {
        for (i in surfaces.indices) {
            val surface = surfaces[i]
            defaultMap[TemplateSurfaceKey(TrackTemplate::class.java, surface)] =
                defaultRenderers.trackRenderer(surface).asUntyped()
        }
    }

    /**
     * Registers a default [renderer] for [templateClass] on [surface], or on every surface when
     * [surface] is `null`.
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param templateClass The runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param renderer Draws [templateClass].
     * @param surface Optional surface to target, or `null` for all surfaces.
     */
    @VisibleForTesting
    public fun <T : GlanceAdaptiveTemplate> registerDefault(
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, S, R>,
        surface: S? = null,
    ) {
        val untyped = renderer.asUntyped()
        synchronized(lock) { forEachKey(templateClass, surface) { defaultMap[it] = untyped } }
    }

    /**
     * Registers a [renderer] override for [widgetName], taking precedence over the default for
     * [templateClass] on [surface], or on every surface when [surface] is `null`.
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param widgetName Developer widget identifier the override applies to.
     * @param templateClass The runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param renderer Draws [templateClass] for [widgetName].
     * @param surface Optional surface to target, or `null` for all surfaces.
     */
    public fun <T : GlanceAdaptiveTemplate> registerOverride(
        widgetName: String,
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, S, R>,
        surface: S? = null,
    ) {
        val untyped = renderer.asUntyped()
        synchronized(lock) {
            forEachKey(templateClass, surface) { overrideMap[widgetName to it] = untyped }
        }
    }

    /**
     * Resolves the override for [widgetName] (if any) or the default for the runtime class of
     * [data] on [surface] and renders [data] with it.
     *
     * @param data Declarative template data payload.
     * @param surface Target surface, carrying the container size of the placement as
     *   [GlanceSurface.dimensions].
     * @param widgetName Optional widget identifier to check for a per-widget override first.
     * @return The host content produced by the registered renderer.
     * @throws IllegalStateException if no renderer is registered for the type of [data] on
     *   [surface].
     */
    public fun render(data: GlanceAdaptiveTemplate, surface: S, widgetName: String? = null): R {
        val key = TemplateSurfaceKey(data.javaClass, surface)
        val renderer =
            synchronized(lock) {
                checkNotNull(widgetName?.let { overrideMap[it to key] } ?: defaultMap[key]) {
                    "No renderer registered for template: ${data.javaClass.name} on surface: " +
                        "$surface"
                }
            }
        return renderer.render(data, surface)
    }

    /** Clears every override and test default, restoring the built-in [defaultRenderers]. */
    @VisibleForTesting
    public fun resetForTesting() {
        synchronized(lock) {
            overrideMap.clear()
            defaultMap.clear()
            registerBuiltInDefaults()
        }
    }

    /** Calls [action] with the key for [surface], or for every surface when [surface] is `null`. */
    private inline fun forEachKey(
        templateClass: Class<out GlanceAdaptiveTemplate>,
        surface: S?,
        action: (TemplateSurfaceKey) -> Unit,
    ) {
        if (surface != null) {
            action(TemplateSurfaceKey(templateClass, surface))
            return
        }
        for (i in surfaces.indices) action(TemplateSurfaceKey(templateClass, surfaces[i]))
    }

    /**
     * Casts to an untyped [AnyTemplateRenderer] so renderers for different templates can share a
     * map. Safe because [render] only hands a renderer data of the class it was registered for.
     */
    @Suppress("UNCHECKED_CAST")
    private fun TemplateRenderer<*, S, R>.asUntyped(): AnyTemplateRenderer<S, R> =
        this as AnyTemplateRenderer<S, R>
}
