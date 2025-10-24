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

@file:JvmName("GuavaKhronosPbrMaterial")

package androidx.xr.scenecore.guava

import androidx.annotation.MainThread
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.runtime.Session
import androidx.xr.scenecore.AlphaMode
import androidx.xr.scenecore.KhronosPbrMaterial
import com.google.common.util.concurrent.ListenableFuture

/**
 * Asynchronously creates a [KhronosPbrMaterial].
 *
 * @param session The active [Session] in which to create the material.
 * @param alphaMode The [AlphaMode] to use for the material.
 * @return a ListenableFuture<KhronosPbrMaterial>. Listeners will be called on the main thread if
 *   Runnable::run is supplied when adding a listener to the [ListenableFuture].
 */
@MainThread
public fun createKhronosPbrMaterialAsync(
    session: Session,
    alphaMode: AlphaMode,
): ListenableFuture<KhronosPbrMaterial> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        KhronosPbrMaterial.create(session, alphaMode)
    }
