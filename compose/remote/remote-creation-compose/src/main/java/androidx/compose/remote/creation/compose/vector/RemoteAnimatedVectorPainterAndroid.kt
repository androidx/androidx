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

@file:JvmName("RemoteAnimatedVectorPainterKt")
@file:JvmMultifileClass

package androidx.compose.remote.creation.compose.vector

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.painter.RemotePainter
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources

/**
 * Creates a [RemotePainter] to render an XML Animated Vector Drawable resource at [progress].
 * Defaults to animating continually based on [RemoteTimeVariables.animationTime].
 *
 * @param context The Android context used to load the resource.
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun painterRemoteAnimatedVector(
    context: Context,
    @DrawableRes id: Int,
    progress: RemoteFloat = defaultProgress(RemoteAnimatedVector.fromXml(context.resources, id)),
): RemotePainter {
    val avd = RemoteAnimatedVector.fromXml(context.resources, id)
    return RemoteAnimatedVectorPainter(avd, progress)
}

/**
 * Remembers a [RemotePainter] for an XML Animated Vector Drawable resource at the given [progress].
 * Defaults to animating continually based on [RemoteTimeVariables.animationTime].
 *
 * @param id The XML drawable resource ID.
 * @param progress The [RemoteFloat] animation progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteAnimatedVectorPainter(
    @DrawableRes id: Int,
    progress: RemoteFloat =
        defaultProgress(RemoteAnimatedVector.fromXml(LocalResources.current, id)),
): RemotePainter {
    val resources = LocalResources.current
    return remember(id, progress) {
        val avd = RemoteAnimatedVector.fromXml(resources, id)
        RemoteAnimatedVectorPainter(avd, progress)
    }
}
