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

@file:JvmName("GuavaExrImage")

package androidx.xr.scenecore.guava

import android.net.Uri
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.runtime.Session
import androidx.xr.scenecore.ExrImage
import com.google.common.util.concurrent.ListenableFuture
import java.nio.file.Path

/**
 * Public factory for an ExrImage, asynchronously loading a preprocessed skybox from a [Path]
 * relative to the application's `assets/` folder.
 *
 * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data, typically
 * generated from an `.exr` or `.hdr` environment map using a tool like Filament's `cmgen`. See:
 * https://github.com/google/filament/tree/main/tools/cmgen
 *
 * @param session The [Session] to use for loading the asset.
 * @param path The Path of the preprocessed `.zip` skybox file to be loaded, relative to the
 *   application's `assets/` folder.
 * @return a [ListenableFuture] which will provide the [ExrImage] upon completion. Listeners will be
 *   called on the main thread if Runnable::run is supplied when adding a listener to the
 *   ListenableFuture.
 *     @throws IllegalArgumentException if [Path.isAbsolute] is true, as this method requires a
 *       relative path, or if the path does not specify a `.zip` file.
 */
@MainThread
public fun createExrImageFromZipAsync(session: Session, path: Path): ListenableFuture<ExrImage> =
    SuspendToFutureAdapter.launchFuture(session.coroutineScope.coroutineContext, true) {
        ExrImage.createFromZip(session, path)
    }

/**
 * Public factory for an ExrImage, asynchronously loading a preprocessed skybox from a [Uri].
 *
 * The input `.zip` file should contain the preprocessed image-based lighting (IBL) data, typically
 * generated from an `.exr` or `.hdr` environment map using a tool like Filament's `cmgen`. See:
 * https://github.com/google/filament/tree/main/tools/cmgen
 *
 * @param session The [Session] to use for loading the asset.
 * @param uri The Uri of the preprocessed `.zip` skybox file to be loaded.
 * @return a [ListenableFuture] which will provide the [ExrImage] upon completion. Listeners will be
 *   called on the main thread if Runnable::run is supplied when adding a listener to the
 *   ListenableFuture.
 *     @throws IllegalArgumentException if the Uri does not specify a `.zip` file.
 */
@MainThread
public fun createExrImageFromZipAsync(session: Session, uri: Uri): ListenableFuture<ExrImage> =
    SuspendToFutureAdapter.launchFuture(session.coroutineScope.coroutineContext, true) {
        ExrImage.createFromZip(session, uri)
    }

/**
 * Public factory function for a preprocessed EXRImage, where the preprocessed EXRImage is
 * asynchronously loaded.
 *
 * This method must be called from the main thread.
 * https://developer.android.com/guide/components/processes-and-threads
 *
 * @param session The [Session] to use for loading the asset.
 * @param assetData The byte array of the preprocessed EXR image to be loaded.
 * @param assetKey The key of the preprocessed EXR image to be loaded. This is used to identify the
 *   asset in the SceneCore cache.
 * @return a [ListenableFuture] which will provide the [ExrImage] upon completion. Listeners will be
 *   called on the main thread if Runnable::run is supplied when adding a listener to the
 *   ListenableFuture.
 */
@MainThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun createExrImageFromZipAsync(
    session: Session,
    assetData: ByteArray,
    assetKey: String,
): ListenableFuture<ExrImage> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        ExrImage.createFromZip(session, assetData, assetKey)
    }
