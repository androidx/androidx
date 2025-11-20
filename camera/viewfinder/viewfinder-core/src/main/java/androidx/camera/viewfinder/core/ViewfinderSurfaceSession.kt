/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.viewfinder.core

import android.view.Surface
import kotlinx.coroutines.CoroutineScope

/**
 * A session of a [Surface] provided by a viewfinder.
 *
 * The enclosed [Surface] is where pixels can be written by the client to be shown in the viewfinder
 * that created this session. When the client no longer needs to write into the surface, the session
 * must be closed with [close]. Failure to call [close] could potentially delay releasing
 * significant resources.
 *
 * @property surface The [android.view.Surface] available for this session. Users of this surface
 *   should not call [Surface.release], and should close the session with [close] instead when the
 *   surface is no longer in use.
 * @property request The [ViewfinderSurfaceRequest] responsible for this session.
 */
interface ViewfinderSurfaceSession : AutoCloseable {
    val surface: Surface
    val request: ViewfinderSurfaceRequest
}

/**
 * A coroutine variant of a [ViewfinderSurfaceSession].
 *
 * The scope will generally be active for as long as the surface needs to be written into, so
 * [surface] should not be used outside of this scope.
 *
 * @property surface The [android.view.Surface] available for this session. Users of this surface
 *   should not call [Surface.release]. It will automatically be closed at some time after this
 *   scope has exited.
 * @property request The [ViewfinderSurfaceRequest] responsible for this session.
 */
interface ViewfinderSurfaceSessionScope : CoroutineScope {
    val surface: Surface
    val request: ViewfinderSurfaceRequest
}
