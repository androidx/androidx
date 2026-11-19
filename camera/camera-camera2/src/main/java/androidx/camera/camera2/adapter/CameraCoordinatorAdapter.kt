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
import androidx.camera.camera2.adapter.CameraInfoAdapter.Companion.cameraId
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.internal.CameraCompatibilityFilter.isBackwardCompatible
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.common.CameraId
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.InitializationException
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
import androidx.camera.core.concurrent.CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode
import androidx.camera.core.impl.CameraRepository
import androidx.camera.core.impl.CameraUpdateException
import androidx.camera.core.impl.InternalCameraPresenceListener

public class CameraCoordinatorAdapter(
    @GuardedBy("lock") private var cameraPipe: CameraPipe?,
    private val cameraDevices: CameraDevices,
) : CameraCoordinator, InternalCameraPresenceListener {

    private val lock = Any()

    @GuardedBy("lock") private var cameraRepository: CameraRepository? = null

    @GuardedBy("lock") private var concurrentCameraIdsSet: Set<Set<CameraId>> = emptySet()

    @GuardedBy("lock") private var concurrentCameraIdMap: Map<String, List<String>> = emptyMap()

    @GuardedBy("lock") private var activeConcurrentCameraInfosList: List<CameraInfo> = emptyList()

    @GuardedBy("lock") private val pendingCameraIds: MutableSet<String> = mutableSetOf()

    @GuardedBy("lock")
    @CameraOperatingMode
    private var concurrentMode: Int = CAMERA_OPERATING_MODE_UNSPECIFIED

    /** Incremented whenever the operating mode changes or the coordinator is shut down. */
    @GuardedBy("lock") private var generation: Long = 0

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
        val request =
            synchronized(lock) {
                if (concurrentMode != CAMERA_OPERATING_MODE_CONCURRENT) {
                    return
                }
                pendingCameraIds.add(checkNotNull(cameraInfo.cameraId).value)
                takeConcurrentGraphRequestLocked()
            } ?: return
        startConcurrentGraphs(request)
    }

    override fun removePendingCameraInfo(cameraInfo: CameraInfo) {
        val cameraId = cameraInfo.cameraId?.value ?: return
        synchronized(lock) { pendingCameraIds.remove(cameraId) }
    }

    override fun setActiveConcurrentCameraInfos(cameraInfos: List<CameraInfo>) {
        val request =
            synchronized(lock) {
                activeConcurrentCameraInfosList = cameraInfos.toList()
                takeConcurrentGraphRequestLocked()
            } ?: return
        startConcurrentGraphs(request)
    }

    private class ConcurrentGraphRequest(
        val generation: Long,
        val cameraIds: List<String>,
        val repository: CameraRepository,
        val cameraPipe: CameraPipe,
    )

    /**
     * Returns a request to start the concurrent CameraGraphs if every active concurrent camera is
     * pending, and consumes the pending camera IDs. The pending camera IDs are kept if the
     * coordinator is not initialized, so that nothing is lost.
     */
    @GuardedBy("lock")
    private fun takeConcurrentGraphRequestLocked(): ConcurrentGraphRequest? {
        if (activeConcurrentCameraInfosList.isEmpty() || pendingCameraIds.isEmpty()) {
            return null
        }
        val activeCameraIds = activeConcurrentCameraInfosList.map {
            checkNotNull(it.cameraId).value
        }
        if (activeCameraIds.toSet() != pendingCameraIds) {
            return null
        }
        val repo = cameraRepository
        val pipe = cameraPipe
        if (repo == null || pipe == null) {
            Camera2Logger.error {
                "Coordinator has not been initialized with a CameraRepository and a CameraPipe."
            }
            return null
        }
        pendingCameraIds.clear()
        return ConcurrentGraphRequest(generation, activeCameraIds, repo, pipe)
    }

    private fun isStaleRequest(request: ConcurrentGraphRequest): Boolean =
        synchronized(lock) { generation != request.generation }

    /**
     * Starts the concurrent CameraGraphs. This must be called without holding [lock], because it
     * calls into [CameraRepository], [CameraPipe] and [CameraInternalAdapter], which take their own
     * locks and may call back into this coordinator.
     */
    private fun startConcurrentGraphs(request: ConcurrentGraphRequest) {
        val camerasToUpdate =
            request.cameraIds.mapNotNull { cameraId ->
                try {
                    request.repository.getCamera(cameraId) as? CameraInternalAdapter
                } catch (_: IllegalArgumentException) {
                    null
                }
            }

        val graphConfigs = buildList {
            for (camera in camerasToUpdate) {
                val config = camera.getDeferredCameraGraphConfig()
                if (config == null) {
                    Camera2Logger.warn {
                        "Deferred CameraGraph config missing before creation, aborting."
                    }
                    return
                }
                add(config)
            }
        }

        if (graphConfigs.isEmpty() || isStaleRequest(request)) {
            Camera2Logger.warn {
                "Concurrent camera state changed, aborting CameraGraph creation."
            }
            return
        }

        val cameraGraphs =
            request.cameraPipe.createCameraGraphs(CameraGraph.ConcurrentConfig(graphConfigs))
        check(cameraGraphs.size == graphConfigs.size)

        // Drop the CameraGraphs if the operating mode changed or the coordinator was shut down
        // while they were being created.
        if (isStaleRequest(request)) {
            Camera2Logger.warn { "Concurrent camera state changed, closing stale CameraGraphs." }
            cameraGraphs.forEach { it.close() }
            return
        }

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
        val isConcurrent = cameraOperatingMode == CAMERA_OPERATING_MODE_CONCURRENT
        val repo =
            synchronized(lock) {
                concurrentMode = cameraOperatingMode
                generation++
                if (!isConcurrent) {
                    // Drop concurrent state so that a later concurrent session starts clean.
                    activeConcurrentCameraInfosList = emptyList()
                    pendingCameraIds.clear()
                }
                cameraRepository
            } ?: return

        // Update all cameras known by the repository. CameraGraph creation is only deferred in
        // concurrent mode, so that all concurrent CameraGraphs can be created together.
        for (camera in repo.cameras) {
            (camera as? CameraInternalAdapter)?.setCameraGraphCreationMode(
                createImmediately = !isConcurrent
            )
        }
    }

    override fun addListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {}

    override fun removeListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {}

    override fun shutdown() {
        synchronized(lock) {
            generation++
            cameraPipe = null
            cameraRepository = null
            concurrentCameraIdsSet = emptySet()
            concurrentCameraIdMap = emptyMap()
            activeConcurrentCameraInfosList = emptyList()
            concurrentMode = CAMERA_OPERATING_MODE_UNSPECIFIED
            pendingCameraIds.clear()
        }
    }
}
