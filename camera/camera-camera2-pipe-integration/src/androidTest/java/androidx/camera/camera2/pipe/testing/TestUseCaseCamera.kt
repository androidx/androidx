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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.testing

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.integration.adapter.CameraStateAdapter
import androidx.camera.camera2.pipe.integration.adapter.CaptureConfigAdapter
import androidx.camera.camera2.pipe.integration.adapter.SessionConfigAdapter
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.NoOpInactiveSurfaceCloser
import androidx.camera.camera2.pipe.integration.compat.workaround.OutputSizesCorrector
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.UseCaseCameraConfig
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.impl.CameraPipeCameraProperties
import androidx.camera.camera2.pipe.integration.impl.CapturePipeline
import androidx.camera.camera2.pipe.integration.impl.ComboRequestListener
import androidx.camera.camera2.pipe.integration.impl.UseCaseCamera
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraRequestControlImpl
import androidx.camera.camera2.pipe.integration.impl.UseCaseCameraState
import androidx.camera.camera2.pipe.integration.impl.UseCaseSurfaceManager
import androidx.camera.camera2.pipe.integration.impl.UseCaseThreads
import androidx.camera.camera2.pipe.integration.impl.toMap
import androidx.camera.core.UseCase
import androidx.camera.core.impl.Config
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Open a [CameraGraph] for the desired [cameraId] and [useCases]
 */
class TestUseCaseCamera(
    private val context: Context,
    private val cameraId: String,
    private val threads: UseCaseThreads,
    private val useCases: List<UseCase>,
    private val cameraConfig: CameraConfig = CameraConfig(CameraId(cameraId)),
    val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context)),
    val callbackMap: CameraCallbackMap = CameraCallbackMap(),
    val useCaseSurfaceManager: UseCaseSurfaceManager = UseCaseSurfaceManager(
        threads,
        cameraPipe,
        NoOpInactiveSurfaceCloser,
    ),
) : UseCaseCamera {
    val cameraMetadata =
        cameraPipe.cameras().awaitCameraMetadata(CameraId.fromCamera2Id(cameraId))!!
    val streamConfigurationMap =
        cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
    val cameraQuirks = CameraQuirks(
        cameraMetadata,
        StreamConfigurationMapCompat(
            streamConfigurationMap,
            OutputSizesCorrector(cameraMetadata, streamConfigurationMap)
        )
    )
    val useCaseCameraGraphConfig =
        UseCaseCameraConfig(
            useCases,
            CameraStateAdapter(),
            cameraQuirks,
            CameraGraph.Flags()
        ).provideUseCaseGraphConfig(
            callbackMap = callbackMap,
            cameraConfig = cameraConfig,
            cameraPipe = cameraPipe,
            requestListener = ComboRequestListener(),
            useCaseSurfaceManager = useCaseSurfaceManager,
            cameraInteropStateCallbackRepository = CameraInteropStateCallbackRepository()
        )

    override val requestControl: UseCaseCameraRequestControl = UseCaseCameraRequestControlImpl(
        configAdapter = CaptureConfigAdapter(
            CameraPipeCameraProperties(cameraPipe, cameraConfig),
            useCaseCameraGraphConfig,
            threads
        ),
        capturePipeline = object : CapturePipeline {
            override var template: Int = CameraDevice.TEMPLATE_PREVIEW

            override suspend fun submitStillCaptures(
                requests: List<Request>,
                captureMode: Int,
                flashType: Int,
                flashMode: Int
            ): List<Deferred<Void?>> {
                throw NotImplementedError("Not implemented")
            }
        },
        state = UseCaseCameraState(useCaseCameraGraphConfig, threads),
        threads = threads,
        useCaseGraphConfig = useCaseCameraGraphConfig,
    ).apply {
        SessionConfigAdapter(useCases).getValidSessionConfigOrNull()?.let { sessionConfig ->
            setConfigAsync(
                type = UseCaseCameraRequestControl.Type.SESSION_CONFIG,
                config = sessionConfig.implementationOptions,
                tags = sessionConfig.repeatingCaptureConfig.tagBundle.toMap(),
                listeners = setOf(
                    CameraCallbackMap.createFor(
                        sessionConfig.repeatingCameraCaptureCallbacks,
                        threads.backgroundExecutor
                    )
                ),
                template = RequestTemplate(sessionConfig.repeatingCaptureConfig.templateType),
                streams = useCaseCameraGraphConfig.getStreamIdsFromSurfaces(
                    sessionConfig.repeatingCaptureConfig.surfaces
                ),
            )
        }
    }

    override var runningUseCases = useCases.toSet()
    override fun <T> setParameterAsync(
        key: CaptureRequest.Key<T>,
        value: T,
        priority: Config.OptionPriority
    ): Deferred<Unit> {
        throw NotImplementedError("Not implemented")
    }

    override fun setParametersAsync(
        values: Map<CaptureRequest.Key<*>, Any>,
        priority: Config.OptionPriority
    ): Deferred<Unit> {
        throw NotImplementedError("Not implemented")
    }

    override fun close(): Job {
        return threads.scope.launch {
            useCaseCameraGraphConfig.graph.close()
            useCaseSurfaceManager.stopAsync().await()
        }
    }
}
