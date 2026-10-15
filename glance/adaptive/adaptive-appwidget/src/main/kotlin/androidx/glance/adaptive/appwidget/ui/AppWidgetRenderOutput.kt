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

/**
 * Output produced by a [TemplateRenderer] targeting platform AppWidget surfaces.
 *
 * Encapsulates the UI representations that can be translated into platform [RemoteViews].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface AppWidgetRenderOutput {
    /**
     * Translates this render output into a platform [RemoteViews] for the given application
     * [context] and target container [dimensions].
     *
     * @param context The application [Context] used to construct or compose the [RemoteViews].
     * @param dimensions The target container [Dimensions] in dp.
     * @return The [RemoteViews] to deliver to the host.
     */
    public suspend fun toRemoteViews(
        context: Context,
        dimensions: Dimensions,
    ): RemoteViews
}
