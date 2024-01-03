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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.internal.CameraGraphCreator
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.concurrent.CameraCoordinator
import androidx.camera.core.concurrent.CameraCoordinator.CameraOperatingMode
import androidx.camera.core.impl.CameraInternal

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CameraCoordinatorAdapter(
    cameraDevices: CameraDevices,
    private val cameraGraphCreator: CameraGraphCreator
) : CameraCoordinator {
    private val cameraInternalMap = mutableMapOf<CameraId, CameraInternalAdapter>()

    private var concurrentCameraIdsSet: Set<Set<CameraId>> = mutableSetOf()
    private var concurrentCameraIdMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    private var activeConcurrentCameraInfosList: MutableList<CameraInfo> = mutableListOf()

    private var concurrentMode: Int = CameraCoordinator.CAMERA_OPERATING_MODE_UNSPECIFIED
    private var concurrentModeOn = false

    init {
        concurrentCameraIdsSet = cameraDevices.awaitConcurrentCameraIds()!!
        for (cameraIdSet in concurrentCameraIdsSet) {
            val cameraIdsList = cameraIdSet.toList()
            if (cameraIdsList.size >= 2) {
                val cameraId1: String = cameraIdsList[0].value
                val cameraId2: String = cameraIdsList[1].value
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

    fun registerCamera(cameraId: String, cameraInternal: CameraInternal) {
        cameraInternalMap[CameraId.fromCamera2Id(cameraId)] =
            cameraInternal as CameraInternalAdapter
    }

    @OptIn(ExperimentalCamera2Interop::class)
    override fun getConcurrentCameraSelectors(): MutableList<MutableList<CameraSelector>> {
        return concurrentCameraIdsSet.map { concurrentCameraIds ->
            concurrentCameraIds.map { cameraId ->
                CameraSelector.Builder().addCameraFilter { cameraInfos ->
                    cameraInfos.filter {
                        cameraId.value == Camera2CameraInfo.from(it).getCameraId()
                    }
                }.build()
            }.toMutableList()
        }.toMutableList()
    }

    override fun getActiveConcurrentCameraInfos(): MutableList<CameraInfo> {
        return activeConcurrentCameraInfosList
    }

    override fun setActiveConcurrentCameraInfos(cameraInfos: MutableList<CameraInfo>) {
        activeConcurrentCameraInfosList = cameraInfos
        for (cameraInternalAdapter in cameraInternalMap.values) {
            cameraInternalAdapter.resumeRefresh()
        }
    }

    @OptIn(ExperimentalCamera2Interop::class)
    override fun getPairedConcurrentCameraId(cameraId: String): String? {
        if (!concurrentCameraIdMap.containsKey(cameraId)) {
            return null
        }

        for (pairedCameraId in concurrentCameraIdMap[cameraId]!!) {
            for (cameraInfo in activeConcurrentCameraInfos) {
                if (pairedCameraId == Camera2CameraInfo.from(cameraInfo).getCameraId()) {
                    return pairedCameraId
                }
            }
        }
        return null
    }

    @CameraOperatingMode
    override fun getCameraOperatingMode(): Int {
        return concurrentMode
    }

    override fun setCameraOperatingMode(@CameraOperatingMode cameraOperatingMode: Int) {
        concurrentMode = cameraOperatingMode
        concurrentModeOn = cameraOperatingMode == CameraCoordinator.CAMERA_OPERATING_MODE_CONCURRENT
        cameraGraphCreator.setConcurrentModeOn(concurrentModeOn)
        for (cameraInternalAdapter in cameraInternalMap.values) {
            if (concurrentModeOn) {
                cameraInternalAdapter.pauseRefresh()
            } else {
                cameraInternalAdapter.resumeRefresh()
            }
        }
    }

    override fun addListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {
    }

    override fun removeListener(listener: CameraCoordinator.ConcurrentCameraModeListener) {
    }
}
