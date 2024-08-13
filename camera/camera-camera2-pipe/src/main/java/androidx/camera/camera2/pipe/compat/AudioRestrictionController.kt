/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraDevice
import android.os.Build
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.AudioRestrictionMode
import androidx.camera.camera2.pipe.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_NONE
import androidx.camera.camera2.pipe.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION
import androidx.camera.camera2.pipe.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION_SOUND
import androidx.camera.camera2.pipe.CameraGraph
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioRestrictionController keeps the global audio restriction mode and audio restriction mode on
 * each CameraGraph, and computes the final audio restriction mode based on the settings.
 */
public interface AudioRestrictionController {
    /** Public global audio restriction mode across all CameraGraph instances. */
    public var globalAudioRestrictionMode: AudioRestrictionMode

    /** Update the audio restriction mode of the given CameraGraph. */
    public fun updateCameraGraphAudioRestrictionMode(
        cameraGraph: CameraGraph,
        mode: AudioRestrictionMode
    )

    /** Removes the CameraGraph from the local CameraGraph to audio restriction mode mapping. */
    public fun removeCameraGraph(cameraGraph: CameraGraph)

    /** Adds the listener to the controller's stored collection of listeners. */
    public fun addListener(listener: Listener)

    /** Removes the listener to the controller's stored collection of listeners. */
    public fun removeListener(listener: Listener)

    /**
     * [CameraDeviceWrapper] extends the [Listener]. When audio restriction mode changes, the
     * listener's update method would be invoked.
     */
    public interface Listener {
        /** @see CameraDevice.getCameraAudioRestriction */
        public fun onCameraAudioRestrictionUpdated(mode: AudioRestrictionMode)
    }
}

@Singleton
public class AudioRestrictionControllerImpl @Inject constructor() : AudioRestrictionController {
    private val lock = Any()
    override var globalAudioRestrictionMode: AudioRestrictionMode = AUDIO_RESTRICTION_NONE
        get() = synchronized(lock) { field }
        set(value: AudioRestrictionMode) {
            synchronized(lock) {
                field = value
                updateListenersMode()
            }
        }

    private val audioRestrictionModeMap = mutableMapOf<CameraGraph, AudioRestrictionMode>()
    private val activeListeners = mutableSetOf<AudioRestrictionController.Listener>()

    override fun updateCameraGraphAudioRestrictionMode(
        cameraGraph: CameraGraph,
        mode: AudioRestrictionMode
    ) {
        synchronized(lock) {
            audioRestrictionModeMap[cameraGraph] = mode
            updateListenersMode()
        }
    }

    override fun removeCameraGraph(cameraGraph: CameraGraph) {
        synchronized(lock) {
            audioRestrictionModeMap.remove(cameraGraph)
            updateListenersMode()
        }
    }

    @GuardedBy("lock")
    private fun computeAudioRestrictionMode(): AudioRestrictionMode {
        if (
            audioRestrictionModeMap.containsValue(AUDIO_RESTRICTION_VIBRATION_SOUND) ||
                globalAudioRestrictionMode == AUDIO_RESTRICTION_VIBRATION_SOUND
        ) {
            return AUDIO_RESTRICTION_VIBRATION_SOUND
        }
        if (
            audioRestrictionModeMap.containsValue(AUDIO_RESTRICTION_VIBRATION) ||
                globalAudioRestrictionMode == AUDIO_RESTRICTION_VIBRATION
        ) {
            return AUDIO_RESTRICTION_VIBRATION
        }
        return AUDIO_RESTRICTION_NONE
    }

    override fun addListener(listener: AudioRestrictionController.Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        synchronized(lock) {
            activeListeners.add(listener)
            val mode = computeAudioRestrictionMode()
            listener.onCameraAudioRestrictionUpdated(mode)
        }
    }

    override fun removeListener(listener: AudioRestrictionController.Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        synchronized(lock) { activeListeners.remove(listener) }
    }

    @GuardedBy("lock")
    private fun updateListenersMode() {
        val mode = computeAudioRestrictionMode()
        for (listener in activeListeners) {
            listener.onCameraAudioRestrictionUpdated(mode)
        }
    }
}
