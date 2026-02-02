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

package androidx.glance.appwidget

import android.view.View
import android.widget.RemoteViews
import androidx.annotation.IdRes
import androidx.compose.runtime.Composable
import androidx.glance.Emittable
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceModifier
import androidx.glance.GlanceNode

/**
 * Add [RemoteViews] into a glance composition.
 *
 * @param remoteViews the views to add to the composition.
 * @param modifier modifier used to adjust the layout algorithm or draw decoration content.
 */
@Composable
fun AndroidRemoteViews(remoteViews: RemoteViews, modifier: GlanceModifier = GlanceModifier) {
    AndroidRemoteViews(remoteViews, View.NO_ID, modifier) {}
}

/**
 * Add [RemoteViews] into a glance composition as a container.
 *
 * @param remoteViews the views to add to the composition.
 * @param containerViewId defines the view id of the container in the [RemoteViews] provided. Any
 *   pre-existing children of that view will be removed with [RemoteViews.removeAllViews], and any
 *   children defined in the [content] block will be added with [RemoteViews.addView] (or
 *   [RemoteViews.addStableView] if available on the system).
 * @param modifier modifier used to adjust the layout algorithm or draw decoration content.
 * @param content the content that will be added to the provided container.
 */
@Composable
fun AndroidRemoteViews(
    remoteViews: RemoteViews,
    @IdRes containerViewId: Int,
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit,
) {
    GlanceNode(
        factory = ::EmittableAndroidRemoteViews,
        update = {
            this.set(remoteViews) { this.remoteViews = it }
            this.set(containerViewId) { this.containerViewId = it }
            this.set(modifier) { this.modifier = it }
        },
        content = content
    )
}

internal class EmittableAndroidRemoteViews : EmittableWithChildren() {
    override var modifier: GlanceModifier = GlanceModifier
    var containerViewId: Int = View.NO_ID
    lateinit var remoteViews: RemoteViews

    override fun copy(): Emittable =
        EmittableAndroidRemoteViews().also {
            it.modifier = modifier
            if (::remoteViews.isInitialized) {
                it.remoteViews = remoteViews
            }
            it.containerViewId = containerViewId
            it.children.addAll(children.map { it.copy() })
        }

    override fun toString(): String =
        "AndroidRemoteViews(" +
            "modifier=$modifier, " +
            "containerViewId=$containerViewId, " +
            "remoteViews=${if (::remoteViews.isInitialized) remoteViews else null}, " +
            "children=[\n${childrenToString()}\n]" +
            ")"
}
