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

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import javax.inject.Inject

/**
 * Holds the most recent 3A state for a single CameraGraph.
 *
 * This object is used to maintain the key-value pairs for the most recent 3A state that is used
 * when building the requests that are sent to a CameraCaptureSession.
 */
@CameraGraphScope
class GraphState3A @Inject constructor() {
    private var aeMode: AeMode? = null
    private var afMode: AfMode? = null
    private var awbMode: AwbMode? = null
    private var aeRegions: List<MeteringRectangle>? = null
    private var afRegions: List<MeteringRectangle>? = null
    private var awbRegions: List<MeteringRectangle>? = null

    fun update(
        aeMode: AeMode?,
        afMode: AfMode?,
        awbMode: AwbMode?,
        aeRegions: List<MeteringRectangle>?,
        afRegions: List<MeteringRectangle>?,
        awbRegions: List<MeteringRectangle>?
    ) {
        synchronized(this) {
            aeMode?.let { this.aeMode = it }
            afMode?.let { this.afMode = it }
            awbMode?.let { this.awbMode = it }
            aeRegions?.let { this.aeRegions = it }
            afRegions?.let { this.afRegions = it }
            awbRegions?.let { this.awbRegions = it }
        }
    }

    fun readState(): Map<CaptureRequest.Key<*>, Any> {
        synchronized(this) {
            val map = mutableMapOf<CaptureRequest.Key<*>, Any>()
            aeMode?.let { map.put(CaptureRequest.CONTROL_AE_MODE, it.value) }
            afMode?.let { map.put(CaptureRequest.CONTROL_AF_MODE, it.value) }
            awbMode?.let { map.put(CaptureRequest.CONTROL_AWB_MODE, it.value) }
            aeRegions?.let { map.put(CaptureRequest.CONTROL_AE_REGIONS, it.toTypedArray()) }
            afRegions?.let { map.put(CaptureRequest.CONTROL_AF_REGIONS, it.toTypedArray()) }
            awbRegions?.let { map.put(CaptureRequest.CONTROL_AWB_REGIONS, it.toTypedArray()) }
            return map
        }
    }

    fun writeTo(builder: CaptureRequest.Builder) {
        synchronized(this) {
            aeMode?.let { builder.set(CaptureRequest.CONTROL_AE_MODE, it.value) }
            afMode?.let { builder.set(CaptureRequest.CONTROL_AF_MODE, it.value) }
            awbMode?.let { builder.set(CaptureRequest.CONTROL_AWB_MODE, it.value) }
            aeRegions?.let { builder.set(CaptureRequest.CONTROL_AE_REGIONS, it.toTypedArray()) }
            afRegions?.let { builder.set(CaptureRequest.CONTROL_AF_REGIONS, it.toTypedArray()) }
            awbRegions?.let { builder.set(CaptureRequest.CONTROL_AWB_REGIONS, it.toTypedArray()) }
        }
    }
}