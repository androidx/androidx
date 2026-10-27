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
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.selection.Dimensions
import androidx.glance.adaptive.core.ui.selection.GlanceSurface
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate

/**
 * Render output backed by a [@RemoteComposable][RemoteComposable] `@Composable` lambda.
 *
 * Kept as a concrete internal class so preview tooling can inspect [content] directly after
 * resolving a template through [AppWidgetTemplateRegistry.render], while the runtime pipeline
 * invokes [toRemoteViews] to capture the document into `RemoteViews(DrawInstructions)` on API 37+.
 *
 * @property content The `@RemoteComposable @Composable` UI definition to capture.
 */
internal class RemoteComposeRenderOutput(val content: @RemoteComposable @Composable () -> Unit) :
    AppWidgetRenderOutput {

    override suspend fun toRemoteViews(
        context: Context,
        dimensions: Dimensions,
    ): RemoteViews {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) {
            throw IllegalStateException(
                "RemoteCompose app widget rendering requires API 37 (CINNAMON_BUN) or higher, " +
                    "but current SDK is ${Build.VERSION.SDK_INT}."
            )
        }
        return Api37Impl.composeRemoteCompose(context, dimensions, content)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private object Api37Impl {
        @OptIn(ExperimentalRemoteCreationComposeApi::class)
        suspend fun composeRemoteCompose(
            context: Context,
            dimensions: Dimensions,
            content: @RemoteComposable @Composable () -> Unit,
        ): RemoteViews {
            val bytes =
                captureSingleRemoteDocument(
                        context = context,
                        creationDisplayInfo = creationDisplayInfo(context, dimensions),
                        profile = GlanceAppWidgetProfiles.widgetsV7,
                        content = content,
                    )
                    .bytes
            val drawInstructions = RemoteViews.DrawInstructions.Builder(listOf(bytes)).build()
            return RemoteViews(drawInstructions)
        }

        /**
         * Describes the virtual display [captureSingleRemoteDocument] lays the content out on.
         *
         * Sized to the host-reported container [dimensions] (the same ones the renderer received as
         * [GlanceSurface.dimensions]), converted from dp to px. Density and font scale come from
         * [context] because the host renders on the same device. When the host has not reported a
         * size, falls back to the library default, which is the full display.
         */
        private fun creationDisplayInfo(
            context: Context,
            dimensions: Dimensions,
        ): RemoteCreationDisplayInfo {
            if (dimensions.widthDp <= 0 || dimensions.heightDp <= 0) {
                return createCreationDisplayInfo(context)
            }
            val density = context.resources.displayMetrics.density
            return createCreationDisplayInfo(
                context = context,
                size = Size(dimensions.widthDp * density, dimensions.heightDp * density),
            )
        }
    }
}

/**
 * A [TemplateRenderer] that produces [@RemoteComposable][RemoteComposable] `@Composable` content
 * for a template [T], archetype [A], and surface [S].
 *
 * @param T The type of [GlanceAdaptiveTemplate] data payload this renderer consumes.
 * @param S The type of [GlanceSurface] this renderer targets.
 * @param A The layout archetype resolved for this render pass.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface RemoteComposeTemplateRenderer<
    in T : GlanceAdaptiveTemplate,
    in S : GlanceSurface,
    in A : Any,
> : TemplateRenderer<T, S, A, AppWidgetRenderOutput> {

    /**
     * Produces the [@RemoteComposable][RemoteComposable] `@Composable` content representation of
     * [template] for [archetype] on [surface].
     *
     * @param template The declarative data payload describing the widget's content.
     * @param archetype The layout archetype resolved for [template] on [surface].
     * @param surface The target surface, carrying the container size of the placement as
     *   [GlanceSurface.dimensions].
     * @return The `@RemoteComposable @Composable` content lambda to capture into a RemoteCompose
     *   document.
     */
    public fun renderRemoteCompose(
        template: T,
        archetype: A,
        surface: S,
    ): @RemoteComposable @Composable () -> Unit

    override fun render(template: T, archetype: A, surface: S): AppWidgetRenderOutput =
        RemoteComposeRenderOutput(renderRemoteCompose(template, archetype, surface))
}
