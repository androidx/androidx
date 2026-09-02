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

package androidx.glance.adaptive.core.ui

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.selection.LocalContainerDimensions
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Registry that holds the mapping between [AdaptiveGlanceTemplate] and their corresponding layout
 * selector and archetype renderer.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TemplateRegistry {
    private val lock = Any()

    @VisibleForTesting
    internal data class Entry<T : AdaptiveGlanceTemplate, A : Any>(
        val selectArchetype: (data: T, surface: GlanceSurface, dimensions: Dimensions) -> A,
        val renderArchetype: @Composable (data: T, archetype: A) -> Unit,
    )

    @get:VisibleForTesting
    internal val registryMap: MutableMap<Class<out AdaptiveGlanceTemplate>, Entry<*, *>> =
        mutableMapOf()

    init {
        synchronized(lock) { registerDefaultTemplates() }
    }

    internal fun registerDefaultTemplates() {
        // Default templates registered by feature modules or templates
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun resetForTesting() {
        synchronized(lock) {
            registryMap.clear()
            registerDefaultTemplates()
        }
    }

    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun <T : AdaptiveGlanceTemplate, A : Any> register(
        templateClass: Class<T>,
        selectArchetype: (data: T, surface: GlanceSurface, dimensions: Dimensions) -> A,
        renderArchetype: @Composable (data: T, archetype: A) -> Unit,
    ) {
        synchronized(lock) { registryMap[templateClass] = Entry(selectArchetype, renderArchetype) }
    }

    /**
     * Central rendering entry-point for Glance Adaptive widgets.
     *
     * Inspects active [LocalContainerDimensions], resolves the optimal archetype via the registered
     * selector, and invokes the archetype renderer.
     */
    @Composable
    @Suppress("UNCHECKED_CAST", "ComposableNaming")
    public fun <T : AdaptiveGlanceTemplate> render(
        data: T,
        surface: GlanceSurface = GlanceSurface.MOBILE_HOME_SCREEN,
    ) {
        val entry =
            synchronized(lock) {
                requireNotNull(registryMap[data.javaClass] as? Entry<T, Any>) {
                    "No renderer registered for template: ${data.javaClass.name}"
                }
            }
        val dimensions = LocalContainerDimensions.current
        val archetype = entry.selectArchetype(data, surface, dimensions)
        entry.renderArchetype(data, archetype)
    }
}
