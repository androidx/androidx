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

package androidx.camera.camera2.pipe.impl

import android.view.Surface
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.wrapper.CameraCaptureSessionWrapper
import kotlinx.atomicfu.atomic

internal val virtualSessionDebugIds = atomic(0)
class VirtualSessionState : CameraCaptureSessionWrapper.StateCallback, SurfaceMap {
    private val debugId = virtualSessionDebugIds.incrementAndGet()
    var surfaceMap: Map<StreamId, Surface>? = null

    override fun onActive(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Active" }
    }

    override fun onClosed(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Closed" }
    }

    override fun onConfigureFailed(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this ConfigureFailed" }
    }

    override fun onConfigured(session: CameraCaptureSessionWrapper) {
        Log.info { "$this Configured" }
    }

    override fun onReady(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Ready" }
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSessionWrapper) {
        Log.debug { "$this Active" }
    }

    /** Return a Surface that should be used for a specific stream */
    override fun get(streamId: StreamId): Surface? {
        TODO("Implemented in a future change.")
    }

    /**
     * This is used to disconnect the cached [CameraCaptureSessionWrapper] and put this object into
     * a closed state. This will not cancel repeating requests or abort captures.
     */
    fun disconnect() {
    }

    /**
     * This is used to disconnect the cached [CameraCaptureSessionWrapper] and put this object into
     * a closed state. This may stop the repeating request and abort captures.
     */
    fun shutdown() {
    }

    override fun toString(): String = "VirtualSessionState-$debugId"
}