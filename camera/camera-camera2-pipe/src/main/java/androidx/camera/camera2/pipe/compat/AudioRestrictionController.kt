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
import androidx.camera.camera2.pipe.config.CameraPipeJob
import androidx.camera.camera2.pipe.core.CoroutineMutex
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.withLockLaunch
import androidx.camera.camera2.pipe.internal.CameraPipeLifetime
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * AudioRestrictionController keeps the global audio restriction mode and audio restriction mode on
 * each CameraGraph, and computes the final audio restriction mode based on the settings.
 */
public interface AudioRestrictionController {
    /** Public global audio restriction mode across all CameraGraph instances. */
    public var globalAudioRestrictionMode: AudioRestrictionMode?

    /** Update the audio restriction mode of the given CameraGraph. */
    public fun updateCameraGraphAudioRestrictionMode(
        cameraGraph: CameraGraph,
        mode: AudioRestrictionMode,
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
internal class AudioRestrictionControllerImpl
@Inject
internal constructor(
    threads: Threads,
    cameraPipeLifetime: CameraPipeLifetime,
    @CameraPipeJob cameraPipeJob: Job,
) : AudioRestrictionController {

    private val scope =
        CoroutineScope(
            SupervisorJob(cameraPipeJob) +
                threads.lightweightDispatcher.plus(
                    CoroutineName("CXCP-AudioRestrictionControllerImpl")
                )
        )
    private val coroutineMutex = CoroutineMutex()
    private val lock = Any()
    override var globalAudioRestrictionMode: AudioRestrictionMode? = null
        get() = synchronized(lock) { field }
        set(value: AudioRestrictionMode?) {
            requireNotNull(value) { "Unsupported setting AudioRestrictionMode to null." }
            synchronized(lock) {
                val previousMode = computeAudioRestrictionMode()
                field = value
                updateListenersMode(previousMode)
            }
        }

    private val audioRestrictionModeMap = mutableMapOf<CameraGraph, AudioRestrictionMode>()
    private val activeListeners: CopyOnWriteArrayList<AudioRestrictionController.Listener> =
        CopyOnWriteArrayList<AudioRestrictionController.Listener>()

    init {
        cameraPipeLifetime.addShutdownAction(CameraPipeLifetime.ShutdownType.SCOPE) {
            scope.cancel()
        }
    }

    override fun updateCameraGraphAudioRestrictionMode(
        cameraGraph: CameraGraph,
        mode: AudioRestrictionMode,
    ) {
        synchronized(lock) {
            val previousMode = computeAudioRestrictionMode()
            audioRestrictionModeMap[cameraGraph] = mode
            updateListenersMode(previousMode)
        }
    }

    override fun removeCameraGraph(cameraGraph: CameraGraph) {
        synchronized(lock) {
            val previousMode = computeAudioRestrictionMode()
            audioRestrictionModeMap.remove(cameraGraph)
            updateListenersMode(previousMode)
        }
    }

    @GuardedBy("lock")
    private fun computeAudioRestrictionMode(): AudioRestrictionMode? {
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
        if (
            audioRestrictionModeMap.containsValue(AUDIO_RESTRICTION_NONE) ||
                globalAudioRestrictionMode == AUDIO_RESTRICTION_NONE
        ) {
            return AUDIO_RESTRICTION_NONE
        }
        return null
    }

    override fun addListener(listener: AudioRestrictionController.Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        synchronized(lock) {
            activeListeners.add(listener)
            val mode = computeAudioRestrictionMode()
            if (mode != null) {
                coroutineMutex.withLockLaunch(scope) {
                    listener.onCameraAudioRestrictionUpdated(mode)
                }
            }
        }
    }

    override fun removeListener(listener: AudioRestrictionController.Listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        activeListeners.remove(listener)
    }

    @GuardedBy("lock")
    private fun updateListenersMode(previousMode: AudioRestrictionMode?) {
        val mode = computeAudioRestrictionMode()
        if (mode != null && mode != previousMode) {
            coroutineMutex.withLockLaunch(scope) {
                for (listener in activeListeners) {
                    listener.onCameraAudioRestrictionUpdated(mode)
                }
            }
        }
    }
}
