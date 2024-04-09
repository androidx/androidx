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
package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Timestamps.measureNow
import androidx.camera.camera2.pipe.integration.config.CameraAppComponent
import androidx.camera.camera2.pipe.integration.config.CameraAppConfig
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.DaggerCameraAppComponent
import androidx.camera.camera2.pipe.integration.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.pipe.integration.internal.CameraCompatibilityFilter
import androidx.camera.camera2.pipe.integration.internal.CameraSelectionOptimizer
import androidx.camera.core.CameraSelector
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig

/**
 * The [CameraFactoryAdapter] is responsible for creating the root dagger component that is used
 * to share resources across Camera instances.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class CameraFactoryAdapter(
    lazyCameraPipe: Lazy<CameraPipe>,
    context: Context,
    threadConfig: CameraThreadConfig,
    camera2InteropCallbacks: CameraInteropStateCallbackRepository,
    availableCamerasSelector: CameraSelector?,
) : CameraFactory {
    private val appComponent: CameraAppComponent by lazy {
        Debug.traceStart { "CameraFactoryAdapter#appComponent" }
        val timeSource = SystemTimeSource()
        val start = Timestamps.now(timeSource)
        val result = DaggerCameraAppComponent.builder()
            .config(
                CameraAppConfig(
                    context,
                    threadConfig,
                    lazyCameraPipe.value,
                    camera2InteropCallbacks
                )
            )
            .build()
        debug { "Created CameraFactoryAdapter in ${start.measureNow(timeSource).formatMs()}" }
        Debug.traceStop()
        result
    }
    private val availableCameraIds: LinkedHashSet<String>
    private val cameraCoordinator: CameraCoordinatorAdapter = CameraCoordinatorAdapter(
        appComponent.getCameraPipe(),
        appComponent.getCameraDevices(),
    )

    init {
        val optimizedCameraIds = CameraSelectionOptimizer.getSelectedAvailableCameraIds(
            this,
            availableCamerasSelector
        )

        // Use a LinkedHashSet to preserve order
        availableCameraIds = LinkedHashSet(
            CameraCompatibilityFilter.getBackwardCompatibleCameraIds(
                appComponent.getCameraDevices(),
                optimizedCameraIds
            )
        )
    }

    /**
     * The [getCamera] method is responsible for providing CameraInternal object based on cameraId.
     * Use cameraId from set of cameraIds provided by [getAvailableCameraIds] method.
     */
    override fun getCamera(cameraId: String): CameraInternal {
        val cameraInternal = appComponent.cameraBuilder()
            .config(CameraConfig(CameraId(cameraId)))
            .build()
            .getCameraInternal()
        cameraCoordinator.registerCamera(cameraId, cameraInternal)
        return cameraInternal
    }

    override fun getAvailableCameraIds(): Set<String> = availableCameraIds

    override fun getCameraCoordinator(): CameraCoordinator {
        return cameraCoordinator
    }

    /** This is an implementation specific object that is specific to the integration package */
    override fun getCameraManager(): Any = appComponent
}
