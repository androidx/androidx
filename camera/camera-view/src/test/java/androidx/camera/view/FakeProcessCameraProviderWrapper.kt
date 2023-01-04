/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.view

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.futures.Futures
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture

/**
 * Fake [ProcessCameraProviderWrapper].
 */
class FakeProcessCameraProviderWrapper(private val camera: Camera) : ProcessCameraProviderWrapper {

    override fun hasCamera(cameraSelector: CameraSelector): Boolean {
        return true
    }

    override fun unbind(vararg useCases: UseCase?) {
        // no-op.
    }

    override fun unbindAll() {
        // no-op.
    }

    override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup
    ): Camera {
        return camera
    }

    override fun shutdown(): ListenableFuture<Void> {
        return Futures.immediateFuture(null)
    }
}