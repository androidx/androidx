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
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Interface defining the rendering contract for a template of type [T] on a surface of type [S].
 *
 * Platform host modules (e.g. adaptive-appwidget) provide concrete rendering mechanisms (such as
 * RemoteViews and Compose) implementing or adapting this contract.
 *
 * @param T The template type implementing [AdaptiveGlanceTemplate].
 * @param S The surface type implementing [GlanceSurface].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TemplateRenderer<in T : AdaptiveGlanceTemplate, in S : GlanceSurface> {

    /**
     * Renders [template] for the given [surface].
     *
     * @param template Declarative template data payload.
     * @param surface Target host placement surface.
     */
    public fun render(template: T, surface: S)
}
