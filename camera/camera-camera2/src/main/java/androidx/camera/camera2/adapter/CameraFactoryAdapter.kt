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
package androidx.camera.camera2.adapter

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.config.CameraAppComponent
import androidx.camera.camera2.config.CameraAppConfig
import androidx.camera.camera2.config.CameraConfig
import androidx.camera.camera2.config.DaggerCameraAppComponent
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.impl.CameraInteropStateCallbackRepository
import androidx.camera.camera2.internal.CameraCompatibilityFilter
import androidx.camera.camera2.internal.CameraSelectionOptimizer
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.Timestamps.measureNow
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraXConfig
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig
import androidx.camera.core.impl.CameraUpdateException
import androidx.camera.core.impl.Observable
import androidx.camera.core.internal.StreamSpecsCalculator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * The [CameraFactoryAdapter] is responsible for creating the root dagger component that is used to
 * share resources across Camera instances.
 */
internal class CameraFactoryAdapter(
    private val lazyCameraPipe: Lazy<CameraPipe>,
    context: Context,
    @get:VisibleForTesting internal val threadConfig: CameraThreadConfig,
    camera2InteropCallbacks: CameraInteropStateCallbackRepository,
    private val availableCamerasSelector: CameraSelector?,
    private val streamSpecsCalculator: StreamSpecsCalculator,
    private val cameraXConfig: CameraXConfig,
    /** Whether [threadConfig]'s camera executor runs tasks inline on the calling thread. */
    private val isDirectExecutor: Boolean = false,
) : CameraFactory, CameraFactory.Interrogator {
    private val cameraCoordinator: CameraCoordinatorAdapter =
        CameraCoordinatorAdapter(lazyCameraPipe.value, lazyCameraPipe.value.cameras())
    private val pipeCameraPresenceObservable: PipeCameraPresenceSource
    private val appComponent: CameraAppComponent by lazy {
        Debug.traceStart { "CameraFactoryAdapter#appComponent" }
        val timeSource = SystemTimeSource()
        val start = Timestamps.now(timeSource)
        val result =
            DaggerCameraAppComponent.builder()
                .config(
                    CameraAppConfig(
                        context,
                        threadConfig,
                        lazyCameraPipe.value,
                        camera2InteropCallbacks,
                        cameraCoordinator,
                        cameraXConfig,
                    )
                )
                .build()
        Camera2Logger.debug {
            "Created CameraFactoryAdapter in ${start.measureNow(timeSource).formatMs()}"
        }
        Debug.traceStop()
        result
    }
    private var availableCameraIds: Set<String> = emptySet()
    private val lock = Any()
    private val isShutdown = AtomicBoolean(false)

    init {
        val initialIds =
            appComponent.getCameraDevices().awaitCameraIds()?.map { it.value } ?: emptyList()
        pipeCameraPresenceObservable =
            PipeCameraPresenceSource(
                idFlow = lazyCameraPipe.value.cameras().cameraIdsFlow(),
                coroutineScope =
                    CoroutineScope(threadConfig.cameraExecutor.asCoroutineDispatcher()),
                initialCameraIds = initialIds,
                context = context,
            )
        onCameraIdsUpdated(initialIds)
    }

    override fun onCameraIdsUpdated(cameraIds: List<String>) {
        if (isShutdown.get()) {
            return
        }

        val filteredIds = calculateAvailableCameraIds(cameraIds)

        synchronized(lock) {
            if (isShutdown.get()) {
                return
            }
            if (availableCameraIds == filteredIds) {
                return // No change
            }
            Camera2Logger.debug {
                "Updated available camera list: $availableCameraIds -> $filteredIds"
            }
            availableCameraIds = filteredIds
        }
    }

    /** Previews the result of a camera ID update without changing state. */
    override fun getAvailableCameraIds(cameraIds: List<String>): List<String> {
        if (isShutdown.get()) {
            return emptyList()
        }
        // Call the shared helper and return the result as a list
        return calculateAvailableCameraIds(cameraIds).toList()
    }

    /** A new private helper that contains the shared filtering logic. */
    private fun calculateAvailableCameraIds(cameraIds: List<String>): Set<String> {
        val optimizedIds =
            CameraSelectionOptimizer.getSelectedAvailableCameraIds(
                appComponent,
                availableCamerasSelector,
                cameraIds.toList(),
                streamSpecsCalculator,
            )

        return LinkedHashSet(
            CameraCompatibilityFilter.getBackwardCompatibleCameraIds(
                appComponent.getCameraDevices(),
                optimizedIds,
            )
        )
    }

    /**
     * The [getCamera] method is responsible for providing CameraInternal object based on cameraId.
     * Use cameraId from set of cameraIds provided by [getAvailableCameraIds] method.
     */
    override fun getCamera(cameraId: String): CameraInternal {
        if (isShutdown.get()) {
            throw CameraUpdateException("CameraFactory has been shut down.")
        }
        val cameraInternal =
            appComponent
                .cameraBuilder()
                .config(CameraConfig(CameraId(cameraId)))
                .streamSpecsCalculator(streamSpecsCalculator)
                .build()
                .getCameraInternal()
        return cameraInternal
    }

    override fun getAvailableCameraIds(): Set<String> =
        synchronized(lock) {
            if (isShutdown.get()) {
                return emptySet()
            }
            // Return a copy
            LinkedHashSet(availableCameraIds)
        }

    override fun getCameraCoordinator(): CameraCoordinator {
        return cameraCoordinator
    }

    /** This is an implementation specific object that is specific to the integration package */
    override fun getCameraManager(): Any = appComponent

    override fun getCameraPresenceSource(): Observable<List<CameraIdentifier>> {
        return pipeCameraPresenceObservable
    }

    override fun shutdown() {
        if (isShutdown.getAndSet(true)) {
            return
        }
        cameraCoordinator.shutdown()
        pipeCameraPresenceObservable.stopMonitoring()
        if (!lazyCameraPipe.isInitialized()) {
            return
        }
        val cameraPipe = lazyCameraPipe.value
        if (isDirectExecutor) {
            // With a direct executor, camera work runs inline on whichever thread completes it,
            // so CameraX's shutdown chain (CameraInternalAdapter.release -> CameraRepository.deinit
            // -> CameraFactory.shutdown) can reach this method on a thread owned by CameraPipe.
            // CameraPipe.shutdown() stops CameraPipe's own threads (shutdownNow, awaitTermination,
            // join) and cannot do so from one of them: it interrupts and waits on itself until
            // timeouts expire (b/439976984). Tear CameraPipe down on a dedicated thread instead,
            // leaving all camera work on the user's executor as requested.
            thread(name = "CXCP-Shutdown") {
                try {
                    cameraPipe.shutdown()
                } catch (e: Exception) {
                    Camera2Logger.error(e) { "Failed to shut down $cameraPipe" }
                }
            }
        } else {
            cameraPipe.shutdown()
        }
    }
}
