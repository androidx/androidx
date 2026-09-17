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
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Rendering contract turning a template of type [T], placed under [HostConstraints] of surface type
 * [S], into host content of type [R].
 *
 * The output type is a parameter because this module deliberately carries no UI dependency: it
 * cannot name `@Composable`, `RemoteViews`, or any other host effect type. Leaving [R] open lets
 * each host substitute its own, so the contract stays shared while the rendering mechanism stays
 * host-specific:
 * - Glance AppWidget hosts substitute `@Composable () -> Unit`.
 * - Plain `RemoteViews` hosts substitute `RemoteViews`.
 * - Tests substitute a value they can assert on.
 *
 * Implementations must be pure: [render] is expected to *produce* content rather than emit it, so
 * that resolution can be exercised in a plain unit test, outside of any host runtime.
 *
 * @param T The template type implementing [AdaptiveGlanceTemplate].
 * @param S The surface type implementing [GlanceSurface].
 * @param R The host content type produced by [render].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface TemplateRenderer<in T : AdaptiveGlanceTemplate, in S : GlanceSurface, out R> {

    /**
     * Produces host content for [template] under [constraints].
     *
     * @param template Declarative template data payload.
     * @param constraints Target host placement: container size plus surface.
     * @return Host content of type [R].
     */
    public fun render(template: T, constraints: HostConstraints<S>): R
}
