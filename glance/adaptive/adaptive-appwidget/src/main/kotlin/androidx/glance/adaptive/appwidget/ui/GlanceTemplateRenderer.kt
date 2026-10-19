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
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.appwidget.GlanceRemoteViewsComposer
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.selection.HostConstraints
import androidx.glance.adaptive.core.ui.templates.AdaptiveGlanceTemplate

/**
 * Render output backed by a Glance `@Composable` lambda.
 *
 * @property content The Glance `@Composable` hierarchy to compose.
 */
// TODO(b/569862874): Remove GlanceRenderOutput once TrackTemplateRenderer is migrated to
//  RemoteViews / RemoteCompose.
internal class GlanceRenderOutput(val content: @Composable () -> Unit) : AppWidgetRenderOutput {

    override suspend fun toRemoteViews(
        context: Context,
        dimensions: Dimensions,
    ): RemoteViews = GlanceRemoteViewsComposer.composeGlance(context, dimensions, content)
}

/**
 * A [TemplateRenderer] that produces Glance `@Composable` content for a template [T], archetype
 * [A], and surface [S].
 *
 * @param T The type of [AdaptiveGlanceTemplate] data payload this renderer consumes.
 * @param S The type of [GlanceSurface] this renderer targets.
 * @param A The layout archetype resolved for this render pass.
 */
// TODO(b/569862874): Temporary bridge for TrackTemplateRenderer. Remove GlanceTemplateRenderer once
//  TrackTemplateRenderer is migrated to RemoteViews / RemoteCompose. Kept internal so nothing
//  outside this module can take a dependency on Glance-backed rendering before it is removed.
internal fun interface GlanceTemplateRenderer<
    in T : AdaptiveGlanceTemplate,
    in S : GlanceSurface,
    in A : Any,
> : TemplateRenderer<T, S, A, AppWidgetRenderOutput> {

    /**
     * Produces the Glance `@Composable` content representation of [template] for [archetype] and
     * [constraints].
     *
     * @param template The declarative data payload describing the widget's content.
     * @param archetype The layout archetype resolved for [template] under [constraints].
     * @param constraints The layout constraints and target surface provided by the host.
     * @return The Glance `@Composable` content lambda to compose.
     */
    fun renderGlance(
        template: T,
        archetype: A,
        constraints: HostConstraints<S>,
    ): @Composable () -> Unit

    override fun render(
        template: T,
        archetype: A,
        constraints: HostConstraints<S>,
    ): AppWidgetRenderOutput = GlanceRenderOutput(renderGlance(template, archetype, constraints))
}
