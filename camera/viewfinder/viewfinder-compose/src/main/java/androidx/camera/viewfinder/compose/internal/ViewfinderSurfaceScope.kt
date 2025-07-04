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

package androidx.camera.viewfinder.compose.internal

import kotlinx.coroutines.CoroutineScope

internal interface ViewfinderSurfaceCoroutineScope : CoroutineScope

internal interface ViewfinderExternalSurfaceScope {
    /**
     * Invokes [onSurface] when a new [ViewfinderSurfaceHolder] is created. The [onSurface] lambda
     * is invoked on the main thread as part of a [ViewfinderSurfaceCoroutineScope] to provide a
     * coroutine context. Always invoked on the main thread.
     *
     * @param onSurface Callback invoked when a new [ViewfinderSurfaceHolder] is created.
     */
    fun onSurface(
        onSurface:
            suspend ViewfinderSurfaceCoroutineScope.(
                viewfinderSurfaceHolder: ViewfinderSurfaceHolder
            ) -> Unit
    )
}
