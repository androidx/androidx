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

package androidx.glance.adaptive.samples

import android.app.Application
import android.os.Build
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.appwidget.matrix.remotecompose.RemoteComposeMatrixRenderer
import androidx.glance.adaptive.appwidget.registerRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.renderer.TemplateRenderer

/**
 * Routes [TrackRemoteComposeWidgetReceiver] to [RemoteComposeMatrixRenderer] on every surface on
 * API 37 (`CINNAMON_BUN`) and above.
 *
 * - Declared on `<application>` in `AndroidManifest.xml` so [onCreate] runs before any activity or
 *   widget broadcast renders in the process.
 * - [registerRenderer] scopes this override to [TrackRemoteComposeWidgetReceiver.WIDGET_NAME];
 *   other demo widgets need no registration here and keep using the library's default RemoteViews
 *   renderers.
 * - Skipped below API 37 (where RemoteCompose receivers are disabled in the manifest) so the
 *   library's default RemoteViews renderer remains in place as a fallback.
 *
 * [registerRenderer], [TemplateRenderer], [TrackMatrixSelector] and [RemoteComposeMatrixRenderer]
 * are restricted to the library group, so this is not a pattern apps can copy: the sample uses them
 * only to compare renderers side by side.
 */
// TODO(b/581392406): Replace with a public way to choose a renderer per widget.
class SamplesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
        val manager = GlanceAdaptiveWidgetManager(this)
        val renderer =
            TemplateRenderer(
                TrackMatrixSelector,
                RemoteComposeMatrixRenderer.forWidget(TrackRemoteComposeWidgetReceiver.WIDGET_NAME),
            )
        for (surface in
            listOf(AppWidgetSurface.MOBILE_HOME_SCREEN, AppWidgetSurface.MOBILE_LOCK_SCREEN)) {
            manager.registerRenderer(
                widgetName = TrackRemoteComposeWidgetReceiver.WIDGET_NAME,
                surface = surface,
                renderer = renderer,
            )
        }
    }
}
