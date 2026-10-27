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
import androidx.annotation.RestrictTo
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate

/**
 * Render output backed directly by a [RemoteViews] instance or provider.
 *
 * Building a [RemoteViews] needs a [Context] (for its package name and resources), but a
 * [TemplateRenderer] only receives the template, archetype, and surface. The output therefore
 * carries a [provider] that the composer invokes with the application [Context] once it is ready to
 * deliver the widget. Use the secondary constructor when the [RemoteViews] has already been built.
 *
 * @property provider Factory invoked with the application [Context] to construct the [RemoteViews]
 *   for the target widget placement.
 */
internal class RemoteViewsRenderOutput(val provider: (Context) -> RemoteViews) :
    AppWidgetRenderOutput {

    /**
     * Creates a [RemoteViewsRenderOutput] that always returns the pre-built [remoteViews].
     *
     * @param remoteViews The [RemoteViews] to deliver to the host.
     */
    constructor(remoteViews: RemoteViews) : this({ remoteViews })

    override suspend fun toRemoteViews(
        context: Context,
        dimensions: Dimensions,
    ): RemoteViews = provider(context)
}

/**
 * A [TemplateRenderer] that produces [RemoteViews] directly for a template [T], archetype [A], and
 * surface [S].
 *
 * @param T The type of [GlanceAdaptiveTemplate] data payload this renderer consumes.
 * @param S The type of [GlanceSurface] this renderer targets.
 * @param A The layout archetype resolved for this render pass.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemoteViewsTemplateRenderer<
    in T : GlanceAdaptiveTemplate,
    in S : GlanceSurface,
    in A : Any,
> : TemplateRenderer<T, S, A, AppWidgetRenderOutput> {

    /**
     * Builds the [RemoteViews] representation of [template] for [archetype] on [surface] using
     * [context].
     *
     * @param context The application [Context] used to construct the [RemoteViews].
     * @param template The declarative data payload describing the widget's content.
     * @param archetype The layout archetype resolved for [template] on [surface].
     * @param surface The target surface, carrying the container size of the placement as
     *   [GlanceSurface.dimensions].
     * @return The [RemoteViews] to deliver to the host.
     */
    public fun renderRemoteViews(
        context: Context,
        template: T,
        archetype: A,
        surface: S,
    ): RemoteViews

    override fun render(template: T, archetype: A, surface: S): AppWidgetRenderOutput =
        RemoteViewsRenderOutput { context ->
            renderRemoteViews(context, template, archetype, surface)
        }
}
