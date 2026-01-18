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

package androidx.camera.camera2.pipe.integration.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.GraphStateToCameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.ZslControlNoOpImpl
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpTemplateParamsOverride
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseGraphContext
import androidx.camera.camera2.pipe.integration.impl.Camera2Logger
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraGraphConfigProvider
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraState
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
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
    val useCaseGraphContext: UseCaseGraphContext

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
                graphStateToCameraStateAdapter = GraphStateToCameraStateAdapter(cameraStateAdapter),
                sessionConfigAdapter = sessionConfigAdapter,
                isExtensions = false,
                sessionProcessor = null,
            )
        useCaseGraphContext = useCaseCameraConfig.provideUseCaseGraphContext(cameraStateAdapter)
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
                        useCaseGraphContext,
                        templateParamsOverride = NoOpTemplateParamsOverride,
                    )
                },
                useCaseGraphContext = useCaseGraphContext,
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
            val graph = useCaseGraphContext.graph

            useCaseGraphContext.configureCameraStateListener()

            graph.start()

            val surfaceToStreamMapResolved = useCaseGraphContext.surfaceToStreamMap

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
            useCaseGraphContext.closeGraph()
            useCaseSurfaceManager.stopAsync().await()
        }
    }
}
