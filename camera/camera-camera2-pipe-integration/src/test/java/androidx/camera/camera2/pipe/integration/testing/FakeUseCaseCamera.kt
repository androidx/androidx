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
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraStream
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraComponent
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphConfig
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class FakeUseCaseCameraComponentBuilder : UseCaseCameraComponent.Builder {
    var buildInvocationCount = 0
    private var sessionConfigAdapter = SessionConfigAdapter(emptyList())
    private var cameraGraph = FakeCameraGraph()
    private var streamConfigMap = mutableMapOf<CameraStream.Config, DeferrableSurface>()

    private var config: UseCaseCameraConfig =
        UseCaseCameraConfig(
            emptyList(),
            sessionConfigAdapter,
            CameraStateAdapter(),
            cameraGraph,
            streamConfigMap,
            sessionProcessorManager = null
        )

    override fun config(config: UseCaseCameraConfig): UseCaseCameraComponent.Builder {
        this.config = config
        return this
    }

    override fun build(): UseCaseCameraComponent {
        buildInvocationCount++
        return FakeUseCaseCameraComponent()
    }
}

class FakeUseCaseCameraComponent() : UseCaseCameraComponent {
    private val fakeUseCaseCamera = FakeUseCaseCamera()
    private val cameraGraph = FakeCameraGraph()
    private val cameraStateAdapter = CameraStateAdapter()

    override fun getUseCaseCamera(): UseCaseCamera {
        return fakeUseCaseCamera
    }

    override fun getUseCaseGraphConfig(): UseCaseGraphConfig {
        // TODO: Implement this properly once we need to use it with SessionProcessor enabled.
        return UseCaseGraphConfig(cameraGraph, emptyMap(), cameraStateAdapter)
    }
}

// TODO: Further implement the methods in this class as needed
open class FakeUseCaseCameraRequestControl(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : UseCaseCameraRequestControl {

    val addParameterCalls = mutableListOf<Map<CaptureRequest.Key<*>, Any>>()
    var addParameterResult = CompletableDeferred(Unit)
    var setConfigCalls = mutableListOf<RequestParameters>()
    var setConfigResult = CompletableDeferred(Unit)
    var setTorchResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))

    override fun setParametersAsync(
        type: UseCaseCameraRequestControl.Type,
        values: Map<CaptureRequest.Key<*>, Any>,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        addParameterCalls.add(values)
        return addParameterResult
    }

    override fun setConfigAsync(
        type: UseCaseCameraRequestControl.Type,
        config: Config?,
        tags: Map<String, Any>,
        streams: Set<StreamId>?,
        template: RequestTemplate?,
        listeners: Set<Request.Listener>,
        sessionConfig: SessionConfig?,
    ): Deferred<Unit> {
        setConfigCalls.add(RequestParameters(type, config, tags))
        return CompletableDeferred(Unit)
    }

    override suspend fun setTorchAsync(enabled: Boolean): Deferred<Result3A> {
        return setTorchResult
    }

    var aeRegions: List<MeteringRectangle>? = null
    var afRegions: List<MeteringRectangle>? = null
    var awbRegions: List<MeteringRectangle>? = null

    val focusMeteringCalls = mutableListOf<FocusMeteringParams>()
    var focusMeteringResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))
    var cancelFocusMeteringCallCount = 0
    var cancelFocusMeteringResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))

    var focusAutoCompletesAfterTimeout = true

    override suspend fun startFocusAndMeteringAsync(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
        aeLockBehavior: Lock3ABehavior?,
        afLockBehavior: Lock3ABehavior?,
        awbLockBehavior: Lock3ABehavior?,
        afTriggerStartAeMode: AeMode?,
        timeLimitNs: Long,
    ): Deferred<Result3A> {
        this.aeRegions = aeRegions
        this.afRegions = afRegions
        this.awbRegions = awbRegions

        focusMeteringCalls.add(
            FocusMeteringParams(
                aeRegions,
                afRegions,
                awbRegions,
                aeLockBehavior,
                afLockBehavior,
                awbLockBehavior,
                afTriggerStartAeMode,
                timeLimitNs
            )
        )

        if (focusAutoCompletesAfterTimeout) {
            scope.launch {
                withTimeoutOrNull(MILLISECONDS.convert(timeLimitNs, NANOSECONDS)) {
                        focusMeteringResult.await()
                    }
                    .let { result3A ->
                        if (result3A == null) {
                            focusMeteringResult.complete(
                                Result3A(status = Result3A.Status.TIME_LIMIT_REACHED)
                            )
                        }
                    }
            }
        }

        return focusMeteringResult
    }

    override suspend fun cancelFocusAndMeteringAsync(): Deferred<Result3A> {
        cancelFocusMeteringCallCount++
        return cancelFocusMeteringResult
    }

    override suspend fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>> {
        return captureSequence.map { CompletableDeferred<Void?>(null).apply { complete(null) } }
    }

    override suspend fun update3aRegions(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ): Deferred<Result3A> {
        this.aeRegions = aeRegions
        this.afRegions = afRegions
        this.awbRegions = awbRegions
        return CompletableDeferred(Result3A(status = Result3A.Status.OK))
    }

    override fun close() {}

    data class FocusMeteringParams(
        val aeRegions: List<MeteringRectangle>? = null,
        val afRegions: List<MeteringRectangle>? = null,
        val awbRegions: List<MeteringRectangle>? = null,
        val aeLockBehavior: Lock3ABehavior? = null,
        val afLockBehavior: Lock3ABehavior? = null,
        val awbLockBehavior: Lock3ABehavior? = null,
        val afTriggerStartAeMode: AeMode? = null,
        val timeLimitNs: Long = CameraGraph.Constants3A.DEFAULT_TIME_LIMIT_NS,
    )

    data class RequestParameters(
        val type: UseCaseCameraRequestControl.Type,
        val config: Config?,
        val tags: Map<String, Any> = emptyMap(),
    )
}

// TODO: Further implement the methods in this class as needed
class FakeUseCaseCamera(
    override var requestControl: UseCaseCameraRequestControl = FakeUseCaseCameraRequestControl(),
) : UseCaseCamera {

    override fun close(): Job {
        return CompletableDeferred(Unit)
    }
}
