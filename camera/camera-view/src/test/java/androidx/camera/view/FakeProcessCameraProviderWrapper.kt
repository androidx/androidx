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
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.UseCase
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.testing.fakes.FakeCamera
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture

/**
 * Fake [ProcessCameraProviderWrapper].
 *
 * @param bindToLifecycleException the [Exception] to throw when [bindToLifecycle] is called. If
 *   null, [bindToLifecycle] will not throw any error.
 */
class FakeProcessCameraProviderWrapper(
    private val camera: Camera = FakeCamera(),
    private val bindToLifecycleException: Throwable? = null,
) : ProcessCameraProviderWrapper {

    private var unbindInvokedUseCases: List<UseCase> = emptyList()
    private var shouldThrowOnGetCameraInfo = false
    private var boundUseCases: List<UseCase> = emptyList()

    /** Obtains the UseCases that were unbind()'d. */
    fun getUnbindInvokedUseCases() = unbindInvokedUseCases

    /** Resets the unbind()'d UseCases list. */
    fun resetUnbindInvokedUseCases() {
        unbindInvokedUseCases = emptyList()
    }

    /** Obtains the UseCases that were bound. */
    fun getBoundUseCases() = boundUseCases

    override fun hasCamera(cameraSelector: CameraSelector): Boolean {
        return true
    }

    override fun unbind(vararg useCases: UseCase?) {
        unbindInvokedUseCases = useCases.toList().filterNotNull()
    }

    override fun unbindAll() {
        // no-op.
    }

    override fun bindToLifecycle(
        lifecycleOwner: LifecycleOwner,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup,
    ): Camera {
        if (bindToLifecycleException != null) {
            throw bindToLifecycleException
        }
        boundUseCases = useCaseGroup.useCases
        return camera
    }

    override fun shutdownAsync(): ListenableFuture<Void> {
        return Futures.immediateFuture(null)
    }

    override fun getCameraInfo(cameraSelector: CameraSelector?): CameraInfo {
        if (shouldThrowOnGetCameraInfo) {
            throw IllegalArgumentException("Fake error: No camera available for selector")
        }
        return camera.cameraInfo
    }

    fun setShouldThrowOnGetCameraInfo(shouldThrow: Boolean) {
        shouldThrowOnGetCameraInfo = shouldThrow
    }
}
