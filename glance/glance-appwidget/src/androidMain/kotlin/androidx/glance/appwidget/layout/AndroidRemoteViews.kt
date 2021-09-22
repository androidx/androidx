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

import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.glance.Applier
import androidx.glance.Emittable
import androidx.glance.GlanceInternalApi
import androidx.glance.Modifier

/**
 * Add [RemoteViews] into a glance composition.
 *
 * @param remoteViews the views to add to the composition
 */
@OptIn(GlanceInternalApi::class)
@Composable
fun AndroidRemoteViews(remoteViews: RemoteViews) {
    ComposeNode<EmittableAndroidRemoteViews, Applier>(
        factory = ::EmittableAndroidRemoteViews,
        update = {
            this.set(remoteViews) { this.remoteViews = it }
        },
    )
}

@OptIn(GlanceInternalApi::class)
internal class EmittableAndroidRemoteViews : Emittable {
    override var modifier: Modifier = Modifier

    lateinit var remoteViews: RemoteViews
}
