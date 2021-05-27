/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.testing

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class FakeUseCaseCameraComponentBuilder : UseCaseCameraComponent.Builder {
    private var config: UseCaseCameraConfig = UseCaseCameraConfig(emptyList())

    override fun config(config: UseCaseCameraConfig): UseCaseCameraComponent.Builder {
        this.config = config
        return this
    }

    override fun build(): UseCaseCameraComponent {
        return FakeUseCaseCameraComponent(config.provideUseCaseList())
    }
}

class FakeUseCaseCameraComponent(useCases: List<UseCase>) : UseCaseCameraComponent {
    private val fakeUseCaseCamera = FakeUseCaseCamera(useCases.toSet())

    override fun getUseCaseCamera(): UseCaseCamera {
        return fakeUseCaseCamera
    }
}

// TODO: Further implement the methods in this class as needed
class FakeUseCaseCamera(override var activeUseCases: Set<UseCase> = emptySet()) : UseCaseCamera {

    override fun <T> setParameter(key: CaptureRequest.Key<T>, value: T) {
    }

    override fun <T> setParameterAsync(key: CaptureRequest.Key<T>, value: T): Deferred<Unit> {
        return CompletableDeferred<Unit>().apply { complete(Unit) }
    }

    override fun <T> setParameters(values: Map<CaptureRequest.Key<*>, Any>) {
    }

    override fun <T> setParametersAsync(values: Map<CaptureRequest.Key<*>, Any>): Deferred<Unit> {
        return CompletableDeferred<Unit>().apply { complete(Unit) }
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
        return CompletableDeferred<Result3A>().apply {
            complete(Result3A(status = Result3A.Status.OK))
        }
    }

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>
    ): Deferred<Result3A> {
        return CompletableDeferred<Result3A>().apply {
            complete(Result3A(status = Result3A.Status.OK))
        }
    }

    override fun capture(captureSequence: List<CaptureConfig>) {
    }

    override fun close() {
    }
}
