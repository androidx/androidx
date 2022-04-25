/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.graph

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.MeteringRectangle
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.config.CameraGraphScope
import javax.inject.Inject

/**
 * Holds the most recent 3A state for a single CameraGraph.
 *
 * This object is used to maintain the key-value pairs for the most recent 3A state that is used
 * when building the requests that are sent to a CameraCaptureSession.
 *
 * The state is comprised of the modes, metering regions for ae, af and awb, and locks for ae and
 * awb. We don't track the lock for af since af lock is achieved by setting 'af trigger = start' in
 * in a request and then omitting the af trigger field in the subsequent requests doesn't disturb
 * the af state. However for ae and awb, the lock type is boolean and should be explicitly set to
 * 'true' in the subsequent requests once we have locked ae/awb and want them to stay locked.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraGraphScope
internal class GraphState3A @Inject constructor() {
    var aeMode: AeMode? = null
        get() = synchronized(this) { field }
        private set
    var afMode: AfMode? = null
        get() = synchronized(this) { field }
        private set
    var awbMode: AwbMode? = null
        get() = synchronized(this) { field }
        private set
    var flashMode: FlashMode? = null
        get() = synchronized(this) { field }
        private set
    var aeRegions: List<MeteringRectangle>? = null
        get() = synchronized(this) { field }
        private set
    var afRegions: List<MeteringRectangle>? = null
        get() = synchronized(this) { field }
        private set
    var awbRegions: List<MeteringRectangle>? = null
        get() = synchronized(this) { field }
        private set
    var aeLock: Boolean? = null
        get() = synchronized(this) { field }
        private set
    var awbLock: Boolean? = null
        get() = synchronized(this) { field }
        private set

    fun update(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        flashMode: FlashMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
        aeLock: Boolean? = null,
        awbLock: Boolean? = null
    ) {
        synchronized(this) {
            aeMode?.let { this.aeMode = it }
            afMode?.let { this.afMode = it }
            awbMode?.let { this.awbMode = it }
            flashMode?.let { this.flashMode = it }
            aeRegions?.let { this.aeRegions = it }
            afRegions?.let { this.afRegions = it }
            awbRegions?.let { this.awbRegions = it }
            aeLock?.let { this.aeLock = it }
            awbLock?.let { this.awbLock = it }
        }
    }

    fun readState(): Map<CaptureRequest.Key<*>, Any> {
        synchronized(this) {
            val map = mutableMapOf<CaptureRequest.Key<*>, Any>()
            aeMode?.let { map.put(CaptureRequest.CONTROL_AE_MODE, it.value) }
            afMode?.let { map.put(CaptureRequest.CONTROL_AF_MODE, it.value) }
            awbMode?.let { map.put(CaptureRequest.CONTROL_AWB_MODE, it.value) }
            flashMode?.let { map.put(CaptureRequest.FLASH_MODE, it.value) }
            aeRegions?.let { map.put(CaptureRequest.CONTROL_AE_REGIONS, it.toTypedArray()) }
            afRegions?.let { map.put(CaptureRequest.CONTROL_AF_REGIONS, it.toTypedArray()) }
            awbRegions?.let { map.put(CaptureRequest.CONTROL_AWB_REGIONS, it.toTypedArray()) }
            aeLock?.let { map.put(CaptureRequest.CONTROL_AE_LOCK, it) }
            awbLock?.let { map.put(CaptureRequest.CONTROL_AWB_LOCK, it) }
            return map
        }
    }

    fun writeTo(map: MutableMap<Any, Any?>) {
        synchronized(this) {
            aeMode?.let { map[CaptureRequest.CONTROL_AE_MODE] = it.value }
            afMode?.let { map[CaptureRequest.CONTROL_AF_MODE] = it.value }
            awbMode?.let { map[CaptureRequest.CONTROL_AWB_MODE] = it.value }
            flashMode?.let { map[CaptureRequest.FLASH_MODE] = it.value }
            aeRegions?.let { map[CaptureRequest.CONTROL_AE_REGIONS] = it.toTypedArray() }
            afRegions?.let { map[CaptureRequest.CONTROL_AF_REGIONS] = it.toTypedArray() }
            awbRegions?.let { map[CaptureRequest.CONTROL_AWB_REGIONS] = it.toTypedArray() }
            aeLock?.let { map[CaptureRequest.CONTROL_AE_LOCK] = it }
            awbLock?.let { map[CaptureRequest.CONTROL_AWB_LOCK] = it }
        }
    }
}