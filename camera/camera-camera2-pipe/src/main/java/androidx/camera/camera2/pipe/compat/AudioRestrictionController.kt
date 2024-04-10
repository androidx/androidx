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
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.compat.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_NONE
import androidx.camera.camera2.pipe.compat.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION
import androidx.camera.camera2.pipe.compat.AudioRestrictionMode.Companion.AUDIO_RESTRICTION_VIBRATION_SOUND
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class that keeps the global audio restriction mode and audio restriction mode on each
 * CameraGraph, and computes the final audio restriction mode based on the settings.
 */
@Singleton
@RequiresApi(30)
class AudioRestrictionController @Inject constructor() {
    private val lock = Any()
    var globalAudioRestrictionMode: AudioRestrictionMode = AUDIO_RESTRICTION_NONE
        get() = synchronized(lock) { field }
        set(value: AudioRestrictionMode) {
            synchronized(lock) {
                field = value
                updateListenersMode()
            }
        }

    private val audioRestrictionModeMap: MutableMap<CameraGraph, AudioRestrictionMode> =
        mutableMapOf()
    private val activeListeners: MutableSet<Listener> = mutableSetOf()

    fun getCameraGraphAudioRestriction(cameraGraph: CameraGraph): AudioRestrictionMode {
        return audioRestrictionModeMap.getOrDefault(cameraGraph, AUDIO_RESTRICTION_NONE)
    }

    fun setCameraGraphAudioRestriction(cameraGraph: CameraGraph, mode: AudioRestrictionMode) {
        synchronized(lock) {
            audioRestrictionModeMap[cameraGraph] = mode
            updateListenersMode()
        }
    }

    fun removeCameraGraph(cameraGraph: CameraGraph) {
        synchronized(lock) {
            audioRestrictionModeMap.remove(cameraGraph)
            updateListenersMode()
        }
    }

    @GuardedBy("lock")
    private fun computeAudioRestrictionMode(): AudioRestrictionMode {
        if (audioRestrictionModeMap.containsValue(AUDIO_RESTRICTION_VIBRATION_SOUND) ||
            globalAudioRestrictionMode == AUDIO_RESTRICTION_VIBRATION_SOUND
        ) {
            return AUDIO_RESTRICTION_VIBRATION_SOUND
        }
        if (audioRestrictionModeMap.containsValue(AUDIO_RESTRICTION_VIBRATION) ||
            globalAudioRestrictionMode == AUDIO_RESTRICTION_VIBRATION
        ) {
            return AUDIO_RESTRICTION_VIBRATION
        }
        return AUDIO_RESTRICTION_NONE
    }

    fun addListener(listener: Listener) {
        synchronized(lock) {
            activeListeners.add(listener)
            val mode = computeAudioRestrictionMode()
            listener.onCameraAudioRestrictionUpdated(mode)
        }
    }

    fun removeListener(listener: Listener?) {
        synchronized(lock) {
            activeListeners.remove(listener)
        }
    }

    @GuardedBy("lock")
    private fun updateListenersMode() {
        val mode = computeAudioRestrictionMode()
        for (listener in activeListeners) {
            listener.onCameraAudioRestrictionUpdated(mode)
        }
    }

    interface Listener {
        /** @see CameraDevice.getCameraAudioRestriction */
        fun onCameraAudioRestrictionUpdated(mode: AudioRestrictionMode)
    }
}
