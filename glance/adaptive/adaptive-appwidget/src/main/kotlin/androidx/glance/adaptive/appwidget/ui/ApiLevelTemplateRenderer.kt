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

import android.os.Build
import androidx.glance.adaptive.appwidget.ui.selection.AppWidgetSurface
import androidx.glance.adaptive.core.ui.ArchetypeRenderer
import androidx.glance.adaptive.core.ui.TemplateRenderer
import androidx.glance.adaptive.core.ui.templates.GlanceAdaptiveTemplate

/**
 * Routes built-in template rendering to the backend supported on the device's
 * [Build.VERSION.SDK_INT].
 *
 * Pair with an [androidx.glance.adaptive.core.ui.selection.ArchetypeSelector] via
 * [TemplateRenderer] on [AppWidgetSurface.MOBILE_HOME_SCREEN] so one selector serves every backend.
 *
 * @param T template data payload consumed by the renderers
 * @param A layout archetype drawn by the renderers
 * @param remoteCompose renderer for the RemoteCompose backend
 * @param remoteViews renderer for the [android.widget.RemoteViews] backend
 */
internal class ApiLevelTemplateRenderer<in T : GlanceAdaptiveTemplate, in A : Any>(
    private val remoteCompose: RemoteComposeTemplateRenderer<T, AppWidgetSurface, A>,
    private val remoteViews: RemoteViewsTemplateRenderer<T, AppWidgetSurface, A>,
) : ArchetypeRenderer<T, AppWidgetSurface, A, AppWidgetRenderOutput> {

    /**
     * Draws [template] as [archetype] on [surface], delegating to [remoteCompose] on
     * [REMOTE_COMPOSE_MIN_SDK] and above, and to [remoteViews] below.
     */
    override fun render(
        template: T,
        archetype: A,
        surface: AppWidgetSurface,
    ): AppWidgetRenderOutput =
        if (Build.VERSION.SDK_INT >= REMOTE_COMPOSE_MIN_SDK) {
            remoteCompose.render(template, archetype, surface)
        } else {
            remoteViews.render(template, archetype, surface)
        }

    private companion object {
        /**
         * Minimum SDK level supporting RemoteCompose AppWidget rendering: API 37
         * ([Build.VERSION_CODES.CINNAMON_BUN]), targeting [GlanceAppWidgetProfiles.widgetsV7].
         */
        private const val REMOTE_COMPOSE_MIN_SDK = Build.VERSION_CODES.CINNAMON_BUN
    }
}
