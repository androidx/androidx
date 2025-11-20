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

@file:JvmName("GuavaAnchor")

package androidx.xr.arcore.guava

import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.xr.arcore.Anchor
import androidx.xr.runtime.Session
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID

/**
 * Stores this anchor in the application's local storage so that it can be shared across sessions.
 *
 * @return a [ListenableFuture] that returns a [UUID] that uniquely identifies this anchor.
 * @throws [IllegalStateException] if [Session.config] is set to
 *   [Config.AnchorPersistenceMode.DISABLED], or if there was an unexpected error persisting the
 *   anchor (e.g. ran out of memory).
 */
public fun Anchor.persistAsync(session: Session): ListenableFuture<UUID> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        persist()
    }

internal fun Anchor.updateAsync(session: Session): ListenableFuture<Unit> =
    SuspendToFutureAdapter.launchFuture(
        context = session.coroutineScope.coroutineContext,
        launchUndispatched = true,
    ) {
        update()
    }
