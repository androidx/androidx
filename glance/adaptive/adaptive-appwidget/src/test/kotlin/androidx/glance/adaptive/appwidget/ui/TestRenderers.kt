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

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.ArchetypeSelector
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate

/** A [RemoteViewsTemplateRenderer] paired with a selector that resolves no archetype. */
internal fun <T : GlanceAdaptiveTemplate> remoteViewsRenderer(
    render: (context: Context, template: T, surface: AppWidgetSurface) -> RemoteViews
): TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput> =
    TemplateRenderer(
        ArchetypeSelector<T, AppWidgetSurface, Unit> { _, _ -> },
        RemoteViewsTemplateRenderer { context, template, _, surface ->
            render(context, template, surface)
        },
    )

/** A [RemoteComposeTemplateRenderer] paired with a selector that resolves no archetype. */
internal fun <T : GlanceAdaptiveTemplate> remoteComposeRenderer(
    render: (template: T, surface: AppWidgetSurface) -> (@RemoteComposable @Composable () -> Unit)
): TemplateRenderer<T, AppWidgetSurface, AppWidgetRenderOutput> =
    TemplateRenderer(
        ArchetypeSelector<T, AppWidgetSurface, Unit> { _, _ -> },
        RemoteComposeTemplateRenderer { template, _, surface -> render(template, surface) },
    )
