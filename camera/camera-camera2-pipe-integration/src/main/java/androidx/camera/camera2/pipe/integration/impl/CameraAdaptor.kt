/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CameraControlInternal
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.futures.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Adapt the [CameraInternal] class to one or more [CameraPipe] based Camera instances.
 */
class CameraAdaptor(
    private val cameraPipe: CameraPipe,
    private val cameraId: CameraId
) : CameraInternal {

    init {
        debug { "Created CameraAdaptor from $cameraPipe for $cameraId" }
        // TODO: Consider preloading the list of camera ids and metadata.
    }

    // Load / unload methods
    override fun open() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun release(): ListenableFuture<Void> {
        // TODO: Determine what the correct way to invoke release is.
        return Futures.immediateFuture(null)
    }

    // Static properties of this camera
    override fun getCameraInfoInternal(): CameraInfoInternal {
        TODO("Not yet implemented")
    }

    override fun getCameraQuirks(): Quirks {
        TODO("Not yet implemented")
    }

    // Controls for interacting with or observing the state of the camera.
    override fun getCameraState(): Observable<CameraInternal.State> {
        TODO("Not yet implemented")
    }

    override fun getCameraControlInternal(): CameraControlInternal {
        TODO("Not yet implemented")
    }

    // UseCase attach / detach behaviors.
    override fun attachUseCases(useCases: MutableCollection<UseCase>) {
        TODO("Not yet implemented")
    }

    override fun detachUseCases(useCases: MutableCollection<UseCase>) {
        TODO("Not yet implemented")
    }

    // UseCase state callbacks
    override fun onUseCaseActive(useCase: UseCase) {
        TODO("Not yet implemented")
    }

    override fun onUseCaseUpdated(useCase: UseCase) {
        TODO("Not yet implemented")
    }

    override fun onUseCaseReset(useCase: UseCase) {
        TODO("Not yet implemented")
    }

    override fun onUseCaseInactive(useCase: UseCase) {
        TODO("Not yet implemented")
    }
}
