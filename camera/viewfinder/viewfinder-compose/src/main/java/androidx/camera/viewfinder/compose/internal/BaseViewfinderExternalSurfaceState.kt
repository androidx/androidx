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

import androidx.camera.viewfinder.compose.SurfaceReplacedCancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Base class for [ViewfinderExternalSurface] and [ViewfinderEmbeddedExternalSurface] state. */
internal abstract class BaseViewfinderExternalSurfaceState(val scope: CoroutineScope) :
    ViewfinderExternalSurfaceScope {

    private var onSurface:
        (suspend ViewfinderSurfaceCoroutineScope.(
            viewfinderSurfaceHolder: ViewfinderSurfaceHolder
        ) -> Unit)? =
        null

    private var job: Job? = null

    override fun onSurface(
        onSurface:
            suspend ViewfinderSurfaceCoroutineScope.(
                viewfinderSurfaceHolder: ViewfinderSurfaceHolder
            ) -> Unit
    ) {
        this.onSurface = onSurface
    }

    /**
     * Dispatch a surface creation event by launching a new coroutine in [scope]. Any previous job
     * from a previous surface creation dispatch is cancelled.
     */
    fun dispatchSurfaceCreated(holder: ViewfinderSurfaceHolder) {
        if (onSurface != null) {
            job =
                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                    job?.apply {
                        cancel(SurfaceReplacedCancellationException())
                        join()
                    }
                    if (isActive) {
                        val receiver =
                            object : ViewfinderSurfaceCoroutineScope, CoroutineScope by this {}
                        onSurface?.invoke(receiver, holder)
                    }
                }
        }
    }
}
