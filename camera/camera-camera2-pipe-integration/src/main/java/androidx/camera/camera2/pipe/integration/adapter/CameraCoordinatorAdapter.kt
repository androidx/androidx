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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.adapter.CameraInfoAdapter.Companion.cameraId
import androidx.camera.camera2.pipe.integration.internal.CameraCompatibilityFilter.isBackwardCompatible
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.InitializationException
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode
import androidx.camera.core.impl.CameraRepository

public class CameraCoordinatorAdapter(
    private var cameraPipe: CameraPipe?,
    cameraDevices: CameraDevices,
) : CameraCoordinator {

    private val lock = Any()

    @GuardedBy("lock") @VisibleForTesting public var cameraRepository: CameraRepository? = null

    @VisibleForTesting public var concurrentCameraIdsSet: MutableSet<Set<CameraId>> = mutableSetOf()

    @VisibleForTesting
    public var concurrentCameraIdMap: MutableMap<String, MutableList<String>> = mutableMapOf()

    @GuardedBy("lock")
    @VisibleForTesting
    public var activeConcurrentCameraInfosList: List<CameraInfo> = emptyList()

    @GuardedBy("lock")
    @VisibleForTesting
    @CameraOperatingMode
    public var concurrentMode: Int = CAMERA_OPERATING_MODE_UNSPECIFIED

    @VisibleForTesting public var concurrentModeOn: Boolean = false

    override fun init(repository: CameraRepository) {
        synchronized(lock) { cameraRepository = repository }
    }

    init {
        val concurrentCameraIds = cameraDevices.awaitConcurrentCameraIds()!!.toMutableSet()
        for (cameraIdSet in concurrentCameraIds) {
            val cameraIdsList = cameraIdSet.toList()
            if (cameraIdsList.size >= 2) {
                val cameraId1: String = cameraIdsList[0].value
                val cameraId2: String = cameraIdsList[1].value
                var isBackwardCompatible = false
                try {
                    isBackwardCompatible =
                        isBackwardCompatible(cameraId1, cameraDevices) &&
                            isBackwardCompatible(cameraId2, cameraDevices)
                } catch (e: InitializationException) {
                    Log.debug {
                        "Concurrent camera id pair: " +
                            "($cameraId1, $cameraId2) is not backward compatible"
                    }
                }
                if (!isBackwardCompatible) {
                    continue
                }
                concurrentCameraIdsSet.add(cameraIdSet)
                if (!concurrentCameraIdMap.containsKey(cameraId1)) {
                    concurrentCameraIdMap[cameraId1] = mutableListOf()
                }
                if (!concurrentCameraIdMap.containsKey(cameraId2)) {
                    concurrentCameraIdMap[cameraId2] = mutableListOf()
                }
                concurrentCameraIdMap[cameraId1]?.add(cameraId2)
                concurrentCameraIdMap[cameraId2]?.add(cameraId1)
            }
        }
    }

    override fun getConcurrentCameraSelectors(): MutableList<MutableList<CameraSelector>> {
        return concurrentCameraIdsSet
            .map { concurrentCameraIds ->
                concurrentCameraIds
                    .map { cameraId ->
                        CameraSelector.Builder()
                            .addCameraFilter { cameraInfos ->
                                cameraInfos.filter { cameraInfo -> cameraId == cameraInfo.cameraId }
                            }
                            .build()
                    }
                    .toMutableList()
            }
            .toMutableList()
    }

    override fun getActiveConcurrentCameraInfos(): List<CameraInfo> =
        synchronized(lock) {
            return ArrayList(activeConcurrentCameraInfosList)
        }

    override fun setActiveConcurrentCameraInfos(cameraInfos: List<CameraInfo>) {
        val camerasToUpdate =
            synchronized(lock) {
                val repo = cameraRepository
                if (repo == null) {
                    Log.error { "Coordinator has not been initialized with a CameraRepository." }
                    return
                }
                activeConcurrentCameraInfosList = cameraInfos
                cameraInfos.mapNotNull { cameraInfo ->
                    try {
                        repo.getCamera(cameraInfo.cameraId!!.value) as? CameraInternalAdapter
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            }

        val graphConfigs =
            camerasToUpdate.map {
                checkNotNull(it.getDeferredCameraGraphConfig()) {
                    "Every CameraInternal instance is expected to have a deferred CameraGraph config"
                }
            }

        val cameraGraphs =
            checkNotNull(cameraPipe).createCameraGraphs(CameraGraph.ConcurrentConfig(graphConfigs))
        check(cameraGraphs.size == graphConfigs.size)

        for ((cameraInternalAdapter, cameraGraph) in camerasToUpdate.zip(cameraGraphs)) {
            cameraInternalAdapter.resumeDeferredCameraGraphCreation(cameraGraph)
        }
    }

    override fun getPairedConcurrentCameraId(cameraId: String): String? {
        return synchronized(lock) {
            val pairedCameraIds = concurrentCameraIdMap[cameraId] ?: return@synchronized null
            for (pairedCameraId in pairedCameraIds) {
                for (cameraInfo in activeConcurrentCameraInfosList) {
                    if (pairedCameraId == cameraInfo.cameraId?.value) {
                        return@synchronized pairedCameraId
                    }
                }
            }
            return@synchronized null
        }
    }

    @CameraOperatingMode
    override fun getCameraOperatingMode(): Int =
        synchronized(lock) {
            return concurrentMode
        }

    override fun setCameraOperatingMode(@CameraOperatingMode cameraOperatingMode: Int) {
        val repo =
            synchronized(lock) {
                concurrentMode = cameraOperatingMode
                cameraRepository
            }

        if (repo == null) return

        concurrentModeOn = cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT

        // Update all cameras known by the repository
        for (camera in repo.cameras) {
            (camera as? CameraInternalAdapter)?.let { camera ->
                if (cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT) {
                    camera.setCameraGraphCreationMode(createImmediately = false)
                } else if (cameraOperatingMode == CAMERA_OPERATING_MODE_SINGLE) {
                    camera.setCameraGraphCreationMode(createImmediately = true)
                }
            }
        }
    }

    override fun addListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {}

    override fun removeListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {}

    override fun shutdown() {
        cameraPipe = null
        concurrentCameraIdsSet.clear()
        concurrentCameraIdMap.clear()
        concurrentModeOn = false
        synchronized(lock) {
            cameraRepository = null
            activeConcurrentCameraInfosList = emptyList()
            concurrentMode = CAMERA_OPERATING_MODE_UNSPECIFIED
        }
    }
}
