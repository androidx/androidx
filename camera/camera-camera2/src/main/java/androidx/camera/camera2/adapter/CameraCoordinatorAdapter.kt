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

package androidx.camera.camera2.adapter

import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.adapter.CameraInfoAdapter.Companion.cameraId
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.internal.CameraCompatibilityFilter.isBackwardCompatible
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.InitializationException
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_SINGLE
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode
import androidx.camera.core.impl.CameraRepository
import androidx.camera.core.impl.CameraUpdateException
import androidx.camera.core.impl.InternalCameraPresenceListener

public class CameraCoordinatorAdapter(
    private var cameraPipe: CameraPipe?,
    private val cameraDevices: CameraDevices,
) : CameraCoordinator, InternalCameraPresenceListener {

    private val lock = Any()

    @GuardedBy("lock") @VisibleForTesting public var cameraRepository: CameraRepository? = null

    @GuardedBy("lock")
    @VisibleForTesting
    public var concurrentCameraIdsSet: Set<Set<CameraId>> = emptySet()

    @GuardedBy("lock")
    @VisibleForTesting
    public var concurrentCameraIdMap: Map<String, List<String>> = emptyMap()

    @GuardedBy("lock")
    @VisibleForTesting
    public var activeConcurrentCameraInfosList: List<CameraInfo> = emptyList()
    @GuardedBy("lock") public var pendingCameraIds: MutableList<String> = mutableListOf()

    @GuardedBy("lock")
    @VisibleForTesting
    @CameraOperatingMode
    public var concurrentMode: Int = CAMERA_OPERATING_MODE_UNSPECIFIED

    @VisibleForTesting public var concurrentModeOn: Boolean = false

    override fun init(repository: CameraRepository) {
        synchronized(lock) { cameraRepository = repository }
        val initialIds = cameraDevices.awaitCameraIds()?.map { it.value } ?: emptyList()
        onCamerasUpdated(initialIds)
    }

    override fun onCamerasUpdated(cameraIds: List<String>) {
        val tempConcurrentCameraIdsSet = mutableSetOf<Set<CameraId>>()
        val tempConcurrentCameraIdMap = mutableMapOf<String, MutableList<String>>()

        try {
            val allConcurrentSets = cameraDevices.awaitConcurrentCameraIds() ?: emptySet()
            for (concurrentCameraIdSet in allConcurrentSets) {
                val stringIdSet = concurrentCameraIdSet.map { it.value }.toSet()
                if (!cameraIds.containsAll(stringIdSet)) {
                    Camera2Logger.warn {
                        "Failed to retrieve concurrent camera: $stringIdSet from $cameraIds"
                    }
                    continue
                }

                val concurrentCameraIdsList = concurrentCameraIdSet.toList()
                if (concurrentCameraIdsList.size >= 2) {
                    val cameraId1 = concurrentCameraIdsList[0]
                    val cameraId2 = concurrentCameraIdsList[1]
                    try {
                        if (
                            isBackwardCompatible(cameraId1.value, cameraDevices) &&
                                isBackwardCompatible(cameraId2.value, cameraDevices)
                        ) {
                            tempConcurrentCameraIdsSet.add(concurrentCameraIdSet)
                            if (!tempConcurrentCameraIdMap.containsKey(cameraId1.value)) {
                                tempConcurrentCameraIdMap[cameraId1.value] = mutableListOf()
                            }
                            tempConcurrentCameraIdMap[cameraId1.value]!!.add(cameraId2.value)
                            if (!tempConcurrentCameraIdMap.containsKey(cameraId2.value)) {
                                tempConcurrentCameraIdMap[cameraId2.value] = mutableListOf()
                            }
                            tempConcurrentCameraIdMap[cameraId2.value]!!.add(cameraId1.value)
                        }
                    } catch (e: InitializationException) {
                        Camera2Logger.warn {
                            "Skipping incompatible concurrent" +
                                " pair: $concurrentCameraIdSet due to ${e.message}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw CameraUpdateException(
                "Failed to retrieve concurrent camera id info for camera-pipe.",
                e,
            )
        }

        synchronized(lock) {
            concurrentCameraIdsSet = tempConcurrentCameraIdsSet
            concurrentCameraIdMap = tempConcurrentCameraIdMap
        }
    }

    override fun getConcurrentCameraSelectors(): List<List<CameraSelector>> {
        return synchronized(lock) {
            concurrentCameraIdsSet
                .map { concurrentCameraIds ->
                    concurrentCameraIds
                        .map { cameraId ->
                            CameraSelector.of(CameraIdentifier.Factory.create(cameraId.value))
                        }
                        .toList()
                }
                .toList()
        }
    }

    override fun getActiveConcurrentCameraInfos(): List<CameraInfo> =
        synchronized(lock) {
            return ArrayList(activeConcurrentCameraInfosList)
        }

    override fun addPendingCameraInfo(cameraInfo: CameraInfo): Unit {
        synchronized(lock) {
            if (concurrentModeOn) {
                pendingCameraIds.add(checkNotNull(cameraInfo.cameraId).value)
                tryStartConcurrentGraph()
            }
        }
    }

    override fun removePendingCameraInfo(cameraInfo: CameraInfo) {
        synchronized(lock) {
            if (concurrentModeOn) {
                pendingCameraIds.remove(checkNotNull(cameraInfo.cameraId).value)
            }
        }
    }

    override fun setActiveConcurrentCameraInfos(cameraInfos: List<CameraInfo>) {
        synchronized(lock) {
            activeConcurrentCameraInfosList = cameraInfos
            tryStartConcurrentGraph()
        }
    }

    private fun tryStartConcurrentGraph() {
        val concurrentCameraInfoList =
            synchronized(lock) {
                if (activeConcurrentCameraInfosList.isEmpty() || pendingCameraIds.isEmpty()) {
                    return
                }
                val activeConcurrentCameraIdsList =
                    activeConcurrentCameraInfosList.map { checkNotNull(it.cameraId).value }
                if (activeConcurrentCameraIdsList.toSet() != pendingCameraIds.toSet()) {
                    return
                }
                pendingCameraIds.clear()
                activeConcurrentCameraInfosList
            }

        // start the concurrent camera graph.
        val camerasToUpdate =
            synchronized(lock) {
                val repo = cameraRepository
                if (repo == null) {
                    Camera2Logger.error {
                        "Coordinator has not been initialized with a CameraRepository."
                    }
                    return
                }
                concurrentCameraInfoList.mapNotNull { cameraInfo ->
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
        if (!concurrentModeOn) {
            activeConcurrentCameraInfosList = emptyList()
        }

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
        concurrentModeOn = false
        synchronized(lock) {
            cameraRepository = null
            concurrentCameraIdsSet = emptySet()
            concurrentCameraIdMap = emptyMap()
            activeConcurrentCameraInfosList = emptyList()
            concurrentMode = CAMERA_OPERATING_MODE_UNSPECIFIED
            pendingCameraIds.clear()
        }
    }
}
