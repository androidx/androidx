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
import androidx.glance.adaptive.core.selection.GlanceSurface
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate

/**
 * Renders a [GlanceAdaptiveTemplate] of type [T] on a [GlanceSurface] of type [S] into host content
 * of type [R].
 *
 * [render] must be free of side effects and thread-safe: it may run concurrently on background
 * threads and multiple times per update for different placement sizes.
 *
 * @param T template type implementing [GlanceAdaptiveTemplate]
 * @param S surface type implementing [GlanceSurface]
 * @param R host content type produced by [render]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface TemplateRenderer<
    in T : GlanceAdaptiveTemplate,
    in S : GlanceSurface,
    out R,
> {

    /**
     * Produces host content for [template] on [surface].
     *
     * @param template template data payload
     * @param surface target host surface
     * @return host content of type [R]
     */
    public fun render(template: T, surface: S): R
}
