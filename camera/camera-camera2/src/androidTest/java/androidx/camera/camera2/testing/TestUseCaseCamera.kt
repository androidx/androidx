/*
 * Copyright 2022 The Android Open Source Project
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

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.adapter.CameraStateAdapter
import androidx.camera.camera2.adapter.SessionConfigAdapter
import androidx.camera.camera2.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.compat.quirk.CameraQuirks
import androidx.camera.camera2.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.UseCaseCameraConfig
import androidx.camera.camera2.config.UseCaseCameraContext
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.CameraCallbackMap
import androidx.camera.camera2.impl.CameraGraphConfigProvider
import androidx.camera.camera2.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.impl.CapturePipeline
import androidx.camera.camera2.impl.ComboRequestListener
import androidx.camera.camera2.impl.UseCaseCamera
import androidx.camera.camera2.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.impl.UseCaseCameraState
import androidx.camera.camera2.impl.UseCaseSurfaceManager
import androidx.camera.camera2.impl.UseCaseThreads
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.core.ImageCapture
import androidx.camera.core.UseCase
import androidx.camera.core.imagecapture.CameraCapturePipeline
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.core.impl.Config
import androidx.camera.testing.impl.FakeCameraCapturePipeline
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job

/** Open a [CameraGraph] for the desired [cameraId] and [useCases] */
class TestUseCaseCamera(
    private val context: Context,
    private val cameraId: String,
    private val threads: UseCaseThreads,
    private val useCases: List<UseCase>,
    private val cameraConfig: CameraConfig = CameraConfig(CameraId(cameraId)),
    val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context)),
    val useCaseSurfaceManager: UseCaseSurfaceManager =
        UseCaseSurfaceManager(
            threads,
            cameraPipe,
            NoOpInactiveSurfaceCloser,
            SessionConfigAdapter(useCases = useCases),
        ),
) : UseCaseCamera {
    val cameraMetadata =
        cameraPipe.cameras().awaitCameraMetadata(CameraId.fromCamera2Id(cameraId))!!
    val streamConfigurationMap =
        cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
    val cameraQuirks =
        CameraQuirks(
            cameraMetadata,
            StreamConfigurationMapCompat(
                streamConfigurationMap,
                OutputSizesCorrector(cameraMetadata, streamConfigurationMap),
            ),
        )
    val sessionConfigAdapter = SessionConfigAdapter(useCases)
    val useCaseCameraContext: UseCaseCameraContext

    init {
        val callbackMap = CameraCallbackMap()
        val requestListener = ComboRequestListener()
        val configProvider =
            CameraGraphConfigProvider(
                callbackMap = callbackMap,
                requestListener = requestListener,
                cameraConfig = cameraConfig,
                cameraQuirks = cameraQuirks,
                zslControl = ZslControlNoOpImpl(),
                templateParamsOverride = NoOpTemplateParamsOverride,
                cameraMetadata = cameraMetadata,
            )

        val cameraStateAdapter = CameraStateAdapter()
        val useCaseCameraConfig =
            UseCaseCameraConfig.create(
                cameraGraphConfigProvider = configProvider,
                cameraGraphFactory = { config -> cameraPipe.createCameraGraph(config) },
                cameraStateAdapter = cameraStateAdapter,
                sessionConfigAdapter = sessionConfigAdapter,
                extensionMode = null,
                sessionProcessor = null,
            )
        useCaseCameraContext = useCaseCameraConfig.provideUseCaseCameraContext(cameraStateAdapter)
        sessionConfigAdapter.getValidSessionConfigOrNull()?.let { sessionConfig ->
            CameraInteropStateCallbackRepository().updateCallbacks(sessionConfig)
        }
    }

    override val requestControl: UseCaseCameraRequestControl =
        UseCaseCameraRequestControlImpl(
                capturePipelineProvider = {
                    object : CapturePipeline {
                        override var template: Int = CameraDevice.TEMPLATE_PREVIEW

                        override suspend fun submitStillCaptures(
                            configs: List<CaptureConfig>,
                            requestTemplate: RequestTemplate,
                            sessionConfigOptions: Config,
                            @ImageCapture.CaptureMode captureMode: Int,
                            @ImageCapture.FlashType flashType: Int,
                            @ImageCapture.FlashMode flashMode: Int,
                        ): List<Deferred<Void?>> {
                            throw NotImplementedError("Not implemented")
                        }

                        override suspend fun getCameraCapturePipeline(
                            captureMode: Int,
                            flashMode: Int,
                            flashType: Int,
                        ): CameraCapturePipeline = FakeCameraCapturePipeline()
                    }
                },
                useCaseCameraStateProvider = {
                    UseCaseCameraState(
                        useCaseCameraContext,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    )
                },
                useCaseCameraContext = useCaseCameraContext,
                useCaseSurfaceManagerProvider = { useCaseSurfaceManager },
                threads = threads,
            )
            .apply {
                if (SessionConfigAdapter(useCases).isSessionConfigValid()) {
                    updateRepeatingRequestAsync(isPrimary = true, runningUseCases = useCases)
                }
            }

    override fun start() {
        threads.confineLaunch {
            val graph = useCaseCameraContext.graph

            useCaseCameraContext.configureCameraStateListener()

            graph.start()

            val surfaceToStreamMapResolved = useCaseCameraContext.surfaceToStreamMap

            Camera2Logger.debug { "Setting up Surfaces with UseCaseSurfaceManager" }
            if (sessionConfigAdapter.isSessionConfigValid()) {
                useCaseSurfaceManager
                    .setupAsync(graph, sessionConfigAdapter, surfaceToStreamMapResolved)
                    .invokeOnCompletion { throwable ->
                        // Only show logs for error cases, ignore CancellationException since
                        // the task could be cancelled by UseCaseSurfaceManager#stopAsync().
                        if (throwable != null && throwable !is CancellationException) {
                            Camera2Logger.error(throwable) { "Surface setup error!" }
                        }
                    }
            } else {
                Camera2Logger.error {
                    "Unable to create capture session due to conflicting configurations"
                }
            }
        }
    }

    override suspend fun getCameraCapturePipeline(
        captureMode: Int,
        flashMode: Int,
        flashType: Int,
    ): CameraCapturePipeline = FakeCameraCapturePipeline()

    override fun updateRepeatingRequestAsync(
        isPrimary: Boolean,
        runningUseCases: Collection<UseCase>,
    ): Job {
        throw UnsupportedOperationException("Not yet implemented.")
    }

    override fun close(): Job {
        return threads.confineLaunch {
            useCaseCameraContext.closeGraph()
            useCaseSurfaceManager.stopAsync().await()
        }
    }
}
