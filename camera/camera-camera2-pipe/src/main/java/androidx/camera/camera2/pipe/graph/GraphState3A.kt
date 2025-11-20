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
import androidx.camera.camera2.pipe.AeMode
import androidx.camera.camera2.pipe.AfMode
import androidx.camera.camera2.pipe.AwbMode
import androidx.camera.camera2.pipe.FlashMode
import androidx.camera.camera2.pipe.config.CameraGraphScope
import javax.inject.Inject
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/** An immutable data class to hold a snapshot of the 3A state. */
internal data class State3A(
    val aeMode: AeMode? = null,
    val afMode: AfMode? = null,
    val awbMode: AwbMode? = null,
    val flashMode: FlashMode? = null,
    val aeRegions: List<MeteringRectangle>? = null,
    val afRegions: List<MeteringRectangle>? = null,
    val awbRegions: List<MeteringRectangle>? = null,
    val aeLock: Boolean? = null,
    val awbLock: Boolean? = null,
)

/** Converts a State3A object into a map of parameters for a [CaptureRequest]. */
internal fun State3A.toCaptureRequestParameterMap(): Map<CaptureRequest.Key<*>, Any> {
    return mutableMapOf<CaptureRequest.Key<*>, Any>().apply {
        aeMode?.let { put(CaptureRequest.CONTROL_AE_MODE, it.value) }
        afMode?.let { put(CaptureRequest.CONTROL_AF_MODE, it.value) }
        awbMode?.let { put(CaptureRequest.CONTROL_AWB_MODE, it.value) }
        flashMode?.let { put(CaptureRequest.FLASH_MODE, it.value) }
        aeRegions?.let { put(CaptureRequest.CONTROL_AE_REGIONS, it.toTypedArray()) }
        afRegions?.let { put(CaptureRequest.CONTROL_AF_REGIONS, it.toTypedArray()) }
        awbRegions?.let { put(CaptureRequest.CONTROL_AWB_REGIONS, it.toTypedArray()) }
        aeLock?.let { put(CaptureRequest.CONTROL_AE_LOCK, it) }
        awbLock?.let { put(CaptureRequest.CONTROL_AWB_LOCK, it) }
    }
}

/** Checks if the AE lock state changed from unlocked to locked between two states. */
internal fun State3A.wasAeLocked(current: State3A): Boolean {
    // True if initial was effectively false AND current is effectively true.
    return !(this.aeLock ?: false) && (current.aeLock ?: false)
}

/** Checks if the AE lock state changed from locked to unlocked between two states. */
internal fun State3A.wasAeUnlocked(current: State3A): Boolean {
    // True if initial was effectively true AND current is effectively false.
    return (this.aeLock ?: false) && !(current.aeLock ?: false)
}

/** Checks if the AWB lock state changed from unlocked to locked between two states. */
internal fun State3A.wasAwbLocked(current: State3A): Boolean {
    return !(this.awbLock ?: false) && (current.awbLock ?: false)
}

/** Checks if the AWB lock state changed from locked to unlocked between two states. */
internal fun State3A.wasAwbUnlocked(current: State3A): Boolean {
    return (this.awbLock ?: false) && !(current.awbLock ?: false)
}

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
@CameraGraphScope
internal class GraphState3A @Inject constructor() {
    private val _state = atomic(State3A())

    /**
     * The current, immutable 3A state. This property can be read to get the current state and
     * assigned to overwrite the entire state atomically.
     */
    var current: State3A
        get() = _state.value
        set(value) {
            _state.value = value
        }

    /** Atomically updates the current state with the provided non-null values. */
    fun update(
        aeMode: AeMode? = null,
        afMode: AfMode? = null,
        awbMode: AwbMode? = null,
        flashMode: FlashMode? = null,
        aeRegions: List<MeteringRectangle>? = null,
        afRegions: List<MeteringRectangle>? = null,
        awbRegions: List<MeteringRectangle>? = null,
        aeLock: Boolean? = null,
        awbLock: Boolean? = null,
    ) {
        _state.update { currentState ->
            currentState.copy(
                aeMode = aeMode ?: currentState.aeMode,
                afMode = afMode ?: currentState.afMode,
                awbMode = awbMode ?: currentState.awbMode,
                flashMode = flashMode ?: currentState.flashMode,
                aeRegions = aeRegions?.ifEmpty { null } ?: currentState.aeRegions,
                afRegions = afRegions?.ifEmpty { null } ?: currentState.afRegions,
                awbRegions = awbRegions?.ifEmpty { null } ?: currentState.awbRegions,
                aeLock = aeLock ?: currentState.aeLock,
                awbLock = awbLock ?: currentState.awbLock,
            )
        }
    }

    /** Reads the current state and returns it as a map for building a [CaptureRequest]. */
    fun toCaptureRequestParametersMap(): Map<CaptureRequest.Key<*>, Any> =
        this.current.toCaptureRequestParameterMap()
}
