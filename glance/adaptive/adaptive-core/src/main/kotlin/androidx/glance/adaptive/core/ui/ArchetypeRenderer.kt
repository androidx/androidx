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
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate

/**
 * Draws a template of type [T], laid out as archetype [A] on a surface of type [S], into host
 * content of type [R].
 *
 * Used by the library's built-in templates. The archetype is resolved by an [ArchetypeSelector] and
 * handed in, not recomputed here: the selector decides *what* to show, the renderer only decides
 * *how* to draw it. Pair the two with `TemplateRenderer(selector, renderer)` to get a single
 * [TemplateRenderer].
 *
 * @param T The template type implementing [GlanceAdaptiveTemplate].
 * @param S The surface type implementing [GlanceSurface].
 * @param A The archetype type produced by the paired [ArchetypeSelector].
 * @param R The host content type produced by [render].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface ArchetypeRenderer<
    in T : GlanceAdaptiveTemplate,
    in S : GlanceSurface,
    in A : Any,
    out R,
> {

    /**
     * Produces host content for [template], laid out as [archetype], on [surface].
     *
     * @param template Declarative template data payload.
     * @param archetype Layout archetype resolved for [template] on [surface].
     * @param surface Target placement: the host surface, carrying its container size as
     *   [GlanceSurface.dimensions].
     * @return Host content of type [R].
     */
    public fun render(template: T, archetype: A, surface: S): R
}

/**
 * Pairs [selector] with [renderer] into a [TemplateRenderer] that runs the selector once per render
 * and draws the archetype it picks.
 *
 * @param selector Resolves the layout archetype.
 * @param renderer Draws the archetype chosen by [selector].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T : GlanceAdaptiveTemplate, S : GlanceSurface, A : Any, R> TemplateRenderer(
    selector: ArchetypeSelector<T, S, A>,
    renderer: ArchetypeRenderer<T, S, A, R>,
): TemplateRenderer<T, S, R> = TemplateRenderer { template, surface ->
    renderer.render(template, selector.select(template, surface), surface)
}
