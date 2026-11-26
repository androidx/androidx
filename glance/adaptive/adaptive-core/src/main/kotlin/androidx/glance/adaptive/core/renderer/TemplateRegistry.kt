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
import java.lang.reflect.Modifier

/** A [TemplateRenderer] with its template type erased, so renderers can share a map. */
private typealias AnyTemplateRenderer<S, R> = TemplateRenderer<GlanceAdaptiveTemplate, S, R>

private fun requireConcreteTemplateClass(templateClass: Class<out GlanceAdaptiveTemplate>) {
    require(!Modifier.isAbstract(templateClass.modifiers)) {
        "Template renderer registration requires a concrete template class, " +
            "was ${templateClass.name}"
    }
}

/**
 * Binds [templateClass] to its built-in default [renderer] on every surface of type [S].
 *
 * The same [renderer] instance is registered across all surfaces of the registry; surface-specific
 * variation belongs inside [TemplateRenderer.render], which receives the target surface.
 *
 * @param T The concrete template type implementing [GlanceAdaptiveTemplate].
 * @param S The surface type implementing [GlanceSurface].
 * @param R The host content type produced by the renderer.
 * @property templateClass The concrete runtime class of the [GlanceAdaptiveTemplate] data payload.
 * @property renderer The built-in default [TemplateRenderer] for [templateClass].
 * @throws IllegalArgumentException if [templateClass] is an abstract class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DefaultRendererRegistration<
    T : GlanceAdaptiveTemplate,
    in S : GlanceSurface,
    out R,
>(
    public val templateClass: Class<T>,
    public val renderer: TemplateRenderer<T, S, R>,
) {
    init {
        requireConcreteTemplateClass(templateClass)
    }
}

/**
 * Creates a [DefaultRendererRegistration] binding [T] to [renderer] on every surface of type [S].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline fun <reified T : GlanceAdaptiveTemplate, S : GlanceSurface, R> defaultRenderer(
    renderer: TemplateRenderer<T, S, R>
): DefaultRendererRegistration<T, S, R> = DefaultRendererRegistration(T::class.java, renderer)

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
 * Each host registry is responsible for verifying in tests that every built-in template has a
 * default registration on every surface.
 *
 * Registrations and lookups are thread-safe.
 *
 * @param S The surface type implementing [GlanceSurface].
 * @param R The host content type produced by the registered renderers.
 * @param surfaces Every surface of type [S], targeted by registrations that don't name a surface.
 * @param defaultRenderers Built-in default [DefaultRendererRegistration] entries registered on
 *   construction and restored by [resetForTesting]. Must not contain duplicate
 *   [DefaultRendererRegistration.templateClass] entries.
 * @throws IllegalArgumentException if [defaultRenderers] contains more than one registration for
 *   the same [GlanceAdaptiveTemplate] class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TemplateRegistry<S : GlanceSurface, R>(
    surfaces: List<S>,
    defaultRenderers: List<DefaultRendererRegistration<*, S, R>>,
) {
    private val surfaces = surfaces.toList()
    private val defaultRenderers = defaultRenderers.toList()
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
        val seenClasses = HashSet<Class<out GlanceAdaptiveTemplate>>(this.defaultRenderers.size)
        val duplicateNames = LinkedHashSet<String>()
        for (i in this.defaultRenderers.indices) {
            val templateClass = this.defaultRenderers[i].templateClass
            if (!seenClasses.add(templateClass)) {
                duplicateNames.add(templateClass.name)
            }
        }
        require(duplicateNames.isEmpty()) {
            "Duplicate default renderer registrations: ${duplicateNames.joinToString()}"
        }
        registerBuiltInDefaults()
    }

    private fun registerBuiltInDefaults() {
        for (i in defaultRenderers.indices) {
            val registration = defaultRenderers[i]
            val untyped = registration.renderer.asUntyped()
            for (j in surfaces.indices) {
                defaultMap[TemplateSurfaceKey(registration.templateClass, surfaces[j])] = untyped
            }
        }
    }

    /**
     * Registers a default [renderer] for [templateClass] on [surface], or on every surface when
     * [surface] is `null`.
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param templateClass The concrete runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param renderer Draws [templateClass].
     * @param surface Optional surface to target, or `null` for all surfaces.
     * @throws IllegalArgumentException if [templateClass] is an abstract class.
     */
    @VisibleForTesting
    public fun <T : GlanceAdaptiveTemplate> registerDefault(
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, S, R>,
        surface: S? = null,
    ) {
        requireConcreteTemplateClass(templateClass)
        val untyped = renderer.asUntyped()
        synchronized(lock) { forEachKey(templateClass, surface) { defaultMap[it] = untyped } }
    }

    /**
     * Registers a [renderer] override for [widgetName], taking precedence over the default for
     * [templateClass] on [surface], or on every surface when [surface] is `null`.
     *
     * @param T The template type implementing [GlanceAdaptiveTemplate].
     * @param widgetName Developer widget identifier the override applies to.
     * @param templateClass The concrete runtime class of the [GlanceAdaptiveTemplate] data payload.
     * @param renderer Draws [templateClass] for [widgetName].
     * @param surface Optional surface to target, or `null` for all surfaces.
     * @throws IllegalArgumentException if [templateClass] is an abstract class.
     */
    public fun <T : GlanceAdaptiveTemplate> registerOverride(
        widgetName: String,
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, S, R>,
        surface: S? = null,
    ) {
        requireConcreteTemplateClass(templateClass)
        val untyped = renderer.asUntyped()
        synchronized(lock) {
            forEachKey(templateClass, surface) { overrideMap[widgetName to it] = untyped }
        }
    }

    /**
     * Removes the override for [widgetName] and [templateClass] on [surface], or on every surface
     * when [surface] is `null`. Does nothing if none is registered.
     *
     * @throws IllegalArgumentException if [widgetName] is blank or [templateClass] is abstract.
     */
    public fun <T : GlanceAdaptiveTemplate> removeOverride(
        widgetName: String,
        templateClass: Class<T>,
        surface: S? = null,
    ) {
        require(widgetName.isNotBlank()) { "widgetName must not be blank" }
        requireConcreteTemplateClass(templateClass)
        synchronized(lock) {
            forEachKey(templateClass, surface) { overrideMap.remove(widgetName to it) }
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
