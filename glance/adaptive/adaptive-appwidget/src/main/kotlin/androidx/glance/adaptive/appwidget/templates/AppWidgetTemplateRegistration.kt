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

package androidx.glance.adaptive.appwidget.templates

import androidx.glance.adaptive.appwidget.matrix.MatrixContent
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.renderer.AppWidgetRenderOutput
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.core.renderer.DefaultRendererRegistration
import androidx.glance.adaptive.core.renderer.TemplateRenderer
import androidx.glance.adaptive.core.renderer.defaultRenderer
import androidx.glance.adaptive.core.selection.ArchetypeSelector
import androidx.glance.adaptive.core.templates.GlanceAdaptiveTemplate

/**
 * Bundles the built-in default [AppWidgetRenderOutput] renderer and [MatrixContent] selector
 * registrations for a [GlanceAdaptiveTemplate] of type [T] so both registries in
 * [androidx.glance.adaptive.appwidget.AppWidgetTemplateRegistry] stay in sync by construction.
 */
internal class AppWidgetTemplateRegistration<T : GlanceAdaptiveTemplate>(
    val renderer: DefaultRendererRegistration<T, AppWidgetSurface, AppWidgetRenderOutput>,
    val selector: DefaultRendererRegistration<T, AppWidgetSurface, MatrixContent>,
)

/**
 * Creates the built-in [AppWidgetTemplateRegistration] for a matrix-based template [T] driven by
 * [selector].
 */
internal inline fun <reified T : GlanceAdaptiveTemplate> matrixTemplateRegistration(
    selector: ArchetypeSelector<T, AppWidgetSurface, MatrixContent>
): AppWidgetTemplateRegistration<T> =
    AppWidgetTemplateRegistration(
        renderer = defaultRenderer(TemplateRenderer(selector, RemoteViewsMatrixRenderer)),
        selector = defaultRenderer(selector::select),
    )
