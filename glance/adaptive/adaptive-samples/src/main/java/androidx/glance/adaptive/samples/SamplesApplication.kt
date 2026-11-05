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

import android.annotation.SuppressLint
import android.app.Application
import androidx.glance.adaptive.appwidget.GlanceAdaptiveWidgetManager
import androidx.glance.adaptive.appwidget.matrix.remoteviews.RemoteViewsMatrixRenderer
import androidx.glance.adaptive.appwidget.registerRenderer
import androidx.glance.adaptive.appwidget.selection.AppWidgetSurface
import androidx.glance.adaptive.appwidget.templates.track.TrackMatrixSelector
import androidx.glance.adaptive.core.renderer.TemplateRenderer

/**
 * Routes [TrackRemoteViewsWidgetReceiver] to [RemoteViewsMatrixRenderer] on every surface.
 *
 * - Declared on `<application>` in `AndroidManifest.xml` so [onCreate] runs before any activity or
 *   widget broadcast renders in the process.
 * - [registerRenderer] scopes this override to [TrackRemoteViewsWidgetReceiver.WIDGET_NAME];
 *   [TrackWidgetReceiver] needs no registration here and keeps using the library's default Glance
 *   renderer.
 *
 * [registerRenderer], [TemplateRenderer], [TrackMatrixSelector] and [RemoteViewsMatrixRenderer] are
 * restricted to the library group, so this is not a pattern apps can copy: the sample uses them
 * only to compare renderers side by side.
 */
// TODO(b/581392406): Replace with a public way to choose a renderer per widget.
class SamplesApplication : Application() {
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()
        val manager = GlanceAdaptiveWidgetManager(this)
        val renderer = TemplateRenderer(TrackMatrixSelector, RemoteViewsMatrixRenderer)
        for (surface in
            listOf(AppWidgetSurface.MOBILE_HOME_SCREEN, AppWidgetSurface.MOBILE_LOCK_SCREEN)) {
            manager.registerRenderer(
                widgetName = TrackRemoteViewsWidgetReceiver.WIDGET_NAME,
                surface = surface,
                renderer = renderer,
            )
        }
    }
}
