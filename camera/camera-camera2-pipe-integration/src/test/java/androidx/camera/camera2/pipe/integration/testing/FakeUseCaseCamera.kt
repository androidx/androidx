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
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.core.UseCase
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.SessionConfig
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

class FakeUseCaseCameraComponentBuilder : UseCaseCameraComponent.Builder {
    private var config: UseCaseCameraConfig = UseCaseCameraConfig(emptyList(), CameraStateAdapter())

    override fun config(config: UseCaseCameraConfig): UseCaseCameraComponent.Builder {
        this.config = config
        return this
    }

    override fun build(): UseCaseCameraComponent {
        return FakeUseCaseCameraComponent(config.provideUseCaseList())
    }
}

class FakeUseCaseCameraComponent(useCases: List<UseCase>) : UseCaseCameraComponent {
    private val fakeUseCaseCamera = FakeUseCaseCamera(MutableLiveData(useCases.toSet()))

    override fun getUseCaseCamera(): UseCaseCamera {
        return fakeUseCaseCamera
    }
}

// TODO: Further implement the methods in this class as needed
open class FakeUseCaseCameraRequestControl : UseCaseCameraRequestControl {
    override fun addParametersAsync(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>
    ): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override fun setConfigAsync(
        type: UseCaseCameraRequestControl.Type,
        config: Config?,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>
    ): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override fun setSessionConfigAsync(sessionConfig: SessionConfig): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
        return CompletableDeferred(Result3A(status = Result3A.Status.OK))
    }

    val focusMeteringCalls = mutableListOf<FocusMeteringParams>()
    var focusMeteringResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))
    var cancelFocusMeteringCallCount = 0
    var cancelFocusMeteringResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>,
        afRegions: List<MeteringRectangle>,
        awbRegions: List<MeteringRectangle>,
        afTriggerStartAeMode: AeMode?
    ): Deferred<Result3A> {
        focusMeteringCalls.add(
            FocusMeteringParams(aeRegions, afRegions, awbRegions, afTriggerStartAeMode)
        )
        return focusMeteringResult
    }

    override suspend fun cancelFocusAndMeteringAsync(): Deferred<Result3A> {
        cancelFocusMeteringCallCount++
        return cancelFocusMeteringResult
    }

    override suspend fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        captureMode: Int,
        flashType: Int,
        flashMode: Int,
    ): List<Deferred<Void?>> {
        return listOf(CompletableDeferred(null))
    }

    data class FocusMeteringParams(
        val aeRegions: List<MeteringRectangle> = emptyList(),
        val afRegions: List<MeteringRectangle> = emptyList(),
        val awbRegions: List<MeteringRectangle> = emptyList(),
        val afTriggerStartAeMode: AeMode? = null
    )
}

// TODO: Further implement the methods in this class as needed
class FakeUseCaseCamera(
    override val runningUseCasesLiveData: MutableLiveData<Set<UseCase>> =
        MutableLiveData(emptySet()),
    override var requestControl: UseCaseCameraRequestControl = FakeUseCaseCameraRequestControl(),
) : UseCaseCamera {

    override fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority
    ): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority
    ): Deferred<Unit> {
        return CompletableDeferred(Unit)
    }

    override fun close(): Job {
        return CompletableDeferred(Unit)
    }
}
