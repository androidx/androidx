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

package androidx.camera.camera2.pipe.testing

import android.view.Surface
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.StreamId

internal class FakeCameraController : CameraController {
    var started = false
    var closed = false
    var surfaceMap: Map<StreamId, Surface>? = null
    override val cameraId: CameraId
        get() = CameraId.fromCamera2Id("0")
    override var isForeground = true

    override fun start() {
        started = true
    }

    override fun stop() {
        started = false
    }

    override fun tryRestart(cameraStatus: CameraStatusMonitor.CameraStatus) {
        stop()
        start()
    }

    override fun close() {
        closed = true
        started = false
    }

    override fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>) {
        this.surfaceMap = surfaceMap
    }
}
