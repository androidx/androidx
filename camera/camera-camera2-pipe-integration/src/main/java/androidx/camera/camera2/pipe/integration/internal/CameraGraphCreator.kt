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

package androidx.camera.camera2.pipe.integration.internal

import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraPipe
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
class CameraGraphCreator @Inject constructor() {
    private val lock = Any()

    @GuardedBy("lock")
    var currentExpectedConfigs = 1

    @GuardedBy("lock")
    val currentConfigs = mutableListOf<CameraGraph.Config>()

    private var pendingDeferred: CompletableDeferred<CameraGraph>? = null

    fun setConcurrentModeOn(on: Boolean) = synchronized(lock) {
        currentExpectedConfigs = if (on) {
            2
        } else {
            1
        }
    }

    suspend fun createCameraGraph(cameraPipe: CameraPipe, config: CameraGraph.Config): CameraGraph {
        var deferred: CompletableDeferred<CameraGraph>? = null
        synchronized(lock) {
            currentConfigs.add(config)
            if (currentConfigs.size != currentExpectedConfigs) {
                deferred = CompletableDeferred()
                pendingDeferred = deferred
            }
        }
        if (deferred != null) {
            return deferred!!.await()
        }
        synchronized(lock) {
            if (currentExpectedConfigs == 1) {
                val cameraGraph = cameraPipe.create(config)
                currentConfigs.clear()
                return cameraGraph
            } else {
                val cameraGraphs = cameraPipe.createCameraGraphs(currentConfigs)
                pendingDeferred?.complete(cameraGraphs.first())
                currentConfigs.clear()
                return cameraGraphs[1]
            }
        }
    }
}
