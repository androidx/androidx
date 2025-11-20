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

@file:JvmName("GuavaGltfModel")

package androidx.xr.scenecore.guava

import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.runtime.Session
import androidx.xr.scenecore.GltfModel
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Path

/**
 * Public factory for a GltfModel, where the glTF is asynchronously loaded from a [Path] relative to
 * the application's `assets/` folder.
 *
 * Currently, only binary glTF (.glb) files are supported.
 *
 * @param session The [Session] to use for loading the model.
 * @param path The Path of the binary glTF (.glb) model to be loaded, relative to the application's
 *   `assets/` folder.
 * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
 *   Runnable::run is supplied when adding a listener to the [ListenableFuture].
 * @throws IllegalArgumentException if [path.isAbsolute] is true, as this method requires a relative
 *   path.
 */
@MainThread
public fun createGltfModelAsync(session: Session, path: Path): ListenableFuture<GltfModel> =
    SuspendToFutureAdapter.launchFuture(session.coroutineScope.coroutineContext, true) {
        GltfModel.create(session, path)
    }

/**
 * Public factory for a GltfModel, where the glTF is asynchronously loaded from a [Uri].
 *
 * Currently, only binary glTF (.glb) files are supported.
 *
 * @param session The [Session] to use for loading the model.
 * @param uri The Uri for a binary glTF (.glb) model to be loaded.
 * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
 *   Runnable::run is supplied when adding a listener to the [ListenableFuture].
 */
@MainThread
public fun createGltfModelAsync(session: Session, uri: Uri): ListenableFuture<GltfModel> =
    SuspendToFutureAdapter.launchFuture(session.coroutineScope.coroutineContext, true) {
        GltfModel.create(session, uri)
    }

/**
 * Public factory for a GltfModel, where the glTF is asynchronously loaded.
 *
 * Currently, only binary glTF files are supported.
 *
 * @param session The [Session] to use for loading the model.
 * @param assetData The byte array data of a binary glTF (`.glb`) model to be loaded.
 * @param assetKey The key to use for the model. This is used to identify the model in the SceneCore
 *   cache.
 * @return a ListenableFuture<GltfModel>. Listeners will be called on the main thread if
 *   Runnable::run is supplied when adding a listener to the [ListenableFuture].
 */
@MainThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun createGltfModelAsync(
    session: Session,
    assetData: ByteArray,
    assetKey: String,
): ListenableFuture<GltfModel> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        GltfModel.create(session, assetData, assetKey)
    }
