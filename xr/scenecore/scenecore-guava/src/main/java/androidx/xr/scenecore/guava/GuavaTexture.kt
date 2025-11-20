/*
 * Copyright 2025 The Android Open Source Project
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

@file:JvmName("GuavaTexture")

package androidx.xr.scenecore.guava

import androidx.annotation.MainThread
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.runtime.Session
import androidx.xr.scenecore.Texture
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Path

/**
 * Public factory for a [Texture], asynchronously loading a preprocessed texture from a [Path]
 * relative to the application's `assets/` folder.
 *
 * Currently, only URLs and relative paths from the `assets/` directory are supported.
 *
 * @param session The [Session] to use for loading the [Texture].
 * @param path The [Path] of the `.png` texture file to be loaded, relative to the application's
 *   `assets/` folder.
 * @return a [ListenableFuture<Texture>]. Listeners will be called on the main thread if
 *   Runnable::run is supplied when adding a listener to the [ListenableFuture].
 * @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method requires a relative
 *   path.
 */
@MainThread
public fun createTextureAsync(session: Session, path: Path): ListenableFuture<Texture> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        Texture.create(session, path)
    }
