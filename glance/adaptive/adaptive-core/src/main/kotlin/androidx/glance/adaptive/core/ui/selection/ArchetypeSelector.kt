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

package androidx.glance.adaptive.core.ui.selection

import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Resolves which layout archetype of type [A] a template of type [T] should use under given
 * [HostConstraints].
 *
 * Selection is data in, data out: it maps a payload plus a size and surface to a semantic layout
 * choice, and never touches a rendering API. That is why this contract lives in core alongside
 * [SizeTiers] while rendering does not — and it is what makes archetype resolution assertable in a
 * plain unit test, with no composition and no host runtime.
 *
 * Implementations must be pure and free of side effects.
 *
 * @param T The template type implementing [AdaptiveGlanceTemplate].
 * @param S The surface type implementing [GlanceSurface].
 * @param A The archetype type produced by [select], typically an enum.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface ArchetypeSelector<
    in T : AdaptiveGlanceTemplate,
    in S : GlanceSurface,
    out A : Any,
> {

    /**
     * Selects the optimal archetype for [template] under [constraints].
     *
     * @param template Declarative template data payload.
     * @param constraints Target host placement: container size plus surface.
     * @return The resolved archetype.
     */
    public fun select(template: T, constraints: HostConstraints<S>): A
}
