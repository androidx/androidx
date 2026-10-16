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

@file:JvmName("RemoteImageBrushKt")

package androidx.compose.remote.creation.compose.shaders

import androidx.compose.remote.creation.compose.layout.toRemoteContentScale
import androidx.compose.remote.creation.compose.layout.toRemoteTileMode
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.TileMode as ComposeTileMode
import androidx.compose.ui.layout.ContentScale

/**
 * Creates a texture brush with a specified [image].
 *
 * @param image The [RemoteImageBitmap] to use
 * @param tileModeX The [ComposeTileMode] to use for the bitmap in the x-axis. Defaults to
 *   [ComposeTileMode.Clamp] to repeat the edge pixels
 * @param tileModeY The [ComposeTileMode] to use for the bitmap in the y-axis. Defaults to
 *   [ComposeTileMode.Clamp] to repeat the edge pixels
 * @param contentScale The [ContentScale] to use when scaling the bitmap.
 */
@Stable
public fun RemoteBrush.Companion.image(
    image: RemoteImageBitmap,
    tileModeX: ComposeTileMode = ComposeTileMode.Clamp,
    tileModeY: ComposeTileMode = ComposeTileMode.Clamp,
    contentScale: ContentScale = ContentScale.None,
): RemoteShaderBrush =
    RemoteImageBrush(
        bitmap = image,
        tileModeX = tileModeX.toRemoteTileMode(),
        tileModeY = tileModeY.toRemoteTileMode(),
        contentScale = contentScale.toRemoteContentScale(),
    )
