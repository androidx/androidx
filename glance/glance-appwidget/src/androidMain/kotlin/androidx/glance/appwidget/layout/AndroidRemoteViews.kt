/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.layout

import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier

/**
 * Add [RemoteViews] into a glance composition.
 *
 * @param remoteViews the views to add to the composition.
 */
@Composable
fun AndroidRemoteViews(remoteViews: RemoteViews) {
    AndroidRemoteViews(remoteViews, View.NO_ID) { }
}

/**
 * Add [RemoteViews] into a glance composition as a container.
 *
 * @param remoteViews the views to add to the composition.
 * @param containerViewId defines the view id of the container in the [RemoteViews] provided. Any
 * pre-existing children of that view will be removed with [RemoteViews.removeAllViews], and
 * any children defined in the [content] block will be added with [RemoteViews.addView] (or
 * [RemoteViews.addStableView] if available on the system).
 */
@Composable
fun AndroidRemoteViews(
    remoteViews: RemoteViews,
    @IdRes containerViewId: Int,
    content: @Composable () -> Unit,
) {
    ComposeNode<EmittableAndroidRemoteViews, Applier>(
        factory = ::EmittableAndroidRemoteViews,
        update = {
            this.set(remoteViews) { this.remoteViews = it }
            this.set(containerViewId) { this.containerViewId = it }
        },
        content = content
    )
}

internal class EmittableAndroidRemoteViews : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier

    lateinit var remoteViews: RemoteViews

    var containerViewId: Int = View.NO_ID
}
