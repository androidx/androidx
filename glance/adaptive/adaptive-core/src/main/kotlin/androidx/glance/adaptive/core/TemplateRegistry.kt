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

package androidx.glance.adaptive.core

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.core.templates.AdaptiveGlanceTemplate

/** Registry for mapping template classes to their Composable renderers. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object TemplateRegistry {
    @get:VisibleForTesting
    internal val registryMap:
        MutableMap<
            Class<out AdaptiveGlanceTemplate>,
            @Composable
            (AdaptiveGlanceTemplate) -> Unit,
        > =
        mutableMapOf()

    public fun <T : AdaptiveGlanceTemplate> register(
        templateClass: Class<T>,
        renderer: @Composable (T) -> Unit,
    ) {
        @Suppress("UNCHECKED_CAST")
        registryMap[templateClass] = { template ->
            renderer(template as T)
        }
    }

    public fun getRenderer(
        templateClass: Class<out AdaptiveGlanceTemplate>
    ): @Composable (AdaptiveGlanceTemplate) -> Unit {
        return requireNotNull(registryMap[templateClass]) {
            "No Composable renderer registered for template class: ${templateClass.name}. Did you forget to register it in TemplateRegistry?"
        }
    }

    @Composable
    public fun render(template: AdaptiveGlanceTemplate) {
        getRenderer(template.javaClass)(template)
    }
}
