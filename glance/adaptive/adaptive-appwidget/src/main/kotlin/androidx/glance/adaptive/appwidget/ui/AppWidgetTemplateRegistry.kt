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
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetGlanceSurface
import androidx.glance.adaptive.appwidget.ui.templates.track.TrackTemplateRenderer
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate
import androidx.glance.adaptive.core.ui.templates.TrackTemplate

/**
 * Registry mapping each [AdaptiveGlanceTemplate] to the [TemplateRenderer] that turns it into
 * [AppWidgetRenderOutput] for platform AppWidgets.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object AppWidgetTemplateRegistry {
    private val lock = Any()

    @get:VisibleForTesting
    internal val registryMap:
        MutableMap<
            Class<out AdaptiveGlanceTemplate>,
            TemplateRenderer<*, AppWidgetGlanceSurface, AppWidgetRenderOutput>,
        > =
        mutableMapOf()

    init {
        synchronized(lock) { registerDefaultTemplates() }
    }

    internal fun registerDefaultTemplates() {
        register(TrackTemplate::class.java, TrackTemplateRenderer)
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun resetForTesting() {
        synchronized(lock) {
            registryMap.clear()
            registerDefaultTemplates()
        }
    }

    /**
     * Registers [renderer] for [templateClass], replacing any previous registration.
     *
     * @param T The template type implementing [AdaptiveGlanceTemplate].
     * @param templateClass The runtime class of the [AdaptiveGlanceTemplate] data payload.
     * @param renderer Produces [AppWidgetRenderOutput] for that template on AppWidget surfaces.
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : AdaptiveGlanceTemplate> register(
        templateClass: Class<T>,
        renderer: TemplateRenderer<T, AppWidgetGlanceSurface, AppWidgetRenderOutput>,
    ) {
        synchronized(lock) { registryMap[templateClass] = renderer }
    }

    /**
     * Resolves the registered [TemplateRenderer] for [data] and invokes it with [constraints] to
     * produce an [AppWidgetRenderOutput].
     *
     * @param data Declarative template data payload.
     * @param constraints Host layout constraints and target AppWidget surface.
     * @return The [AppWidgetRenderOutput] produced by the registered renderer.
     * @throws IllegalArgumentException if no renderer is registered for the type of [data].
     */
    @Suppress("UNCHECKED_CAST")
    public fun <T : AdaptiveGlanceTemplate> render(
        data: T,
        constraints: HostConstraints<AppWidgetGlanceSurface>,
    ): AppWidgetRenderOutput {
        val renderer =
            synchronized(lock) {
                requireNotNull(
                    registryMap[data.javaClass]
                        as? TemplateRenderer<T, AppWidgetGlanceSurface, AppWidgetRenderOutput>
                ) {
                    "No renderer registered for template: ${data.javaClass.name}"
                }
            }
        return renderer.render(data, constraints)
    }
}
