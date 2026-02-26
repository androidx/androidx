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

package androidx.camera.camera2.testing

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.compat.workaround.TemplateParamsQuirkOverride
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.UseCaseCameraComponent
import androidx.camera.camera2.config.UseCaseCameraConfig
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.impl.CameraCallbackMap
import androidx.camera.camera2.impl.CameraGraphConfigProvider
import androidx.camera.camera2.impl.ComboRequestListener
import androidx.camera.camera2.impl.UseCaseCamera
import androidx.camera.camera2.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.impl.toMap
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Lock3ABehavior
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeCameraMetadata
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.testing.impl.FakeCameraCapturePipeline
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.robolectric.shadows.StreamConfigurationMapBuilder

class FakeUseCaseCameraComponentBuilder : UseCaseCameraComponent.Builder {
    var buildInvocationCount = 0
    private var sessionConfigAdapter = SessionConfigAdapter(emptyList())
    private var cameraGraph = FakeCameraGraph()
    private val cameraStateAdapter = CameraStateAdapter()
    private val cameraMetadata = FakeCameraMetadata()
    private val cameraQuirks =
        CameraQuirks(
            cameraMetadata,
            StreamConfigurationMapCompat(
                StreamConfigurationMapBuilder.newBuilder().build(),
                OutputSizesCorrector(
                    cameraMetadata,
                    StreamConfigurationMapBuilder.newBuilder().build(),
                ),
            ),
        )
    val configProvider =
        CameraGraphConfigProvider(
            callbackMap = CameraCallbackMap(),
            requestListener = ComboRequestListener(),
            cameraConfig = CameraConfig(cameraMetadata.camera),
            cameraQuirks = cameraQuirks,
            zslControl = ZslControlNoOpImpl(),
            templateParamsOverride = TemplateParamsQuirkOverride(cameraQuirks.quirks),
            cameraMetadata = cameraMetadata,
        )

    private var config: UseCaseCameraConfig =
        UseCaseCameraConfig.create(
            cameraGraphConfigProvider = configProvider,
            cameraGraphFactory = { _ -> cameraGraph },
            cameraStateAdapter = cameraStateAdapter,
            sessionConfigAdapter = sessionConfigAdapter,
            extensionMode = null,
            sessionProcessor = null,
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
    private val useCaseCameraContext =
        UseCaseCameraContext(
            cameraGraphProvider = { cameraGraph },
            cameraStateAdapter = cameraStateAdapter,
            graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
            streamConfigMapProvider = { emptyMap() },
            defaultSurfaceToStreamMap = emptyMap(),
        )

    override fun getUseCaseCamera(): UseCaseCamera {
        return fakeUseCaseCamera
    }

    override fun getUseCaseCameraContext(): UseCaseCameraContext {
        // TODO: Implement this properly once we need to use it with SessionProcessor enabled.
        return useCaseCameraContext
    }
}

// TODO: Further implement the methods in this class as needed
open class FakeUseCaseCameraRequestControl(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
) : UseCaseCameraRequestControl {
    val addParameterCalls = mutableListOf<Map<CaptureRequest.Key<*>, Any>>()
    var addParameterResult = CompletableDeferred(Unit)
    val removeParameterCalls = mutableListOf<CaptureRequest.Key<*>>()
    var removeParameterResult = CompletableDeferred(Unit)
    var setConfigCalls = mutableListOf<RequestParameters>()
    var setConfigResult = CompletableDeferred(Unit)
    var setTorchResult = CompletableDeferred(Result3A(status = Result3A.Status.OK))
    var setTorchCalls = mutableListOf<Boolean>()

    // TODO - Implement thread-safety in the functions annotated with @AnyThread in
    //  UseCaseCameraRequestControl

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        addParameterCalls.add(values)
        return addParameterResult
    }

    override fun submitParameters(
        values: Map<CaptureRequest.Key<*>, Any>,
        type: UseCaseCameraRequestControl.Type,
        optionPriority: Config.OptionPriority,
    ): Deferred<Unit> {
        addParameterCalls.add(values)
        return addParameterResult
    }

    override fun removeParametersAsync(
        keys: List<CaptureRequest.Key<*>>,
        type: UseCaseCameraRequestControl.Type,
    ): Deferred<Unit> {
        removeParameterCalls.addAll(keys)
        return removeParameterResult
    }

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Deferred<Unit> {
        val sessionConfig = SessionConfigAdapter(runningUseCases).getValidSessionConfigOrNull()
        setConfigCalls.add(
            RequestParameters(
                UseCaseCameraRequestControl.Type.SESSION_CONFIG,
                sessionConfig?.implementationOptions,
                sessionConfig?.repeatingCaptureConfig?.tagBundle?.toMap() ?: emptyMap(),
            )
        )
        return CompletableDeferred(Unit)
    }

    override fun updateCamera2ConfigAsync(config: Config, tags: Map<String, Any>): Deferred<Unit> {
        setConfigCalls.add(
            RequestParameters(UseCaseCameraRequestControl.Type.CAMERA2_CAMERA_CONTROL, config, tags)
        )
        return CompletableDeferred(Unit)
    }

    override fun setTorchOnAsync(): Deferred<Result3A> {
        setTorchCalls.add(true)
        return setTorchResult
    }

    override fun setTorchOffAsync(aeMode: AeMode): Deferred<Result3A> {
        setTorchCalls.add(false)
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

    override fun startFocusAndMeteringAsync(
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
                timeLimitNs,
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

    override fun cancelFocusAndMeteringAsync(): Deferred<Result3A> {
        cancelFocusMeteringCallCount++
        return cancelFocusMeteringResult
    }

    override fun issueSingleCaptureAsync(
        captureSequence: List<CaptureConfig>,
        @ImageCapture.CaptureMode captureMode: Int,
        @ImageCapture.FlashType flashType: Int,
        @ImageCapture.FlashMode flashMode: Int,
    ): List<Deferred<Void?>> {
        return captureSequence.map { CompletableDeferred<Void?>(null).apply { complete(null) } }
    }

    override fun update3aRegions(
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?,
    ): Deferred<Result3A> {
        this.aeRegions = aeRegions
        this.afRegions = afRegions
        this.awbRegions = awbRegions
        return CompletableDeferred(Result3A(status = Result3A.Status.OK))
    }

    override suspend fun awaitSurfaceSetup(): Boolean {
        return true
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
    override var requestControl: UseCaseCameraRequestControl = FakeUseCaseCameraRequestControl()
) : UseCaseCamera {
    override fun start() {}

    override suspend fun getCameraCapturePipeline(
        captureMode: Int,
        flashMode: Int,
        flashType: Int,
    ): CameraCapturePipeline = FakeCameraCapturePipeline()

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Job {
        return CompletableDeferred(Unit)
    }

    override fun close(): Job {
        return CompletableDeferred(Unit)
    }
}
