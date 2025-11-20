/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.ConcurrentCameraGraphs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex

/**
 * A class that creates and manages [ConcurrentSessionSequencer] which helps sequence capture
 * session creations during concurrent camera scenarios.
 *
 * Please refer to b/422329694 for context. It is discovered that one is less likely to run into
 * issues during concurrent capture session configuration if capture sessions are created serially
 * (one after another).
 */
@Singleton
internal class ConcurrentSessionSequencers @Inject constructor() {
    private val lock = Any()
    private val sequencers = mutableMapOf<ConcurrentCameraGraphs, ConcurrentSessionSequencer>()
    private val pending = mutableSetOf<CameraGraphId>()

    /**
     * Returns the [ConcurrentSessionSequencer] associated with [concurrentCameraGraphs], with
     * [cameraGraphId] being the camera graph requesting it. The camera controller for each camera
     * graph is expected to get a sequencer. When all camera graphs retrieve their sequencer, the
     * entry is removed from our internal mapping.
     *
     * For example: With 2 camera graphs with ids {"10", "11"}, the former would make a call with
     * ({"10"}, {"10", "11"}), while the latter would make a call with ({"11"}, {"10", "11"}).
     */
    fun getSequencer(
        cameraGraphId: CameraGraphId,
        concurrentCameraGraphs: ConcurrentCameraGraphs,
    ): ConcurrentSessionSequencer {
        synchronized(lock) {
            if (sequencers.contains(concurrentCameraGraphs)) {
                pending.remove(cameraGraphId)
                return if (!concurrentCameraGraphs.cameraGraphIds.any { pending.contains(it) }) {
                    checkNotNull(sequencers.remove(concurrentCameraGraphs))
                } else {
                    checkNotNull(sequencers[concurrentCameraGraphs])
                }
            }

            val sequencer = ConcurrentSessionSequencer()
            sequencers[concurrentCameraGraphs] = sequencer
            pending.addAll(concurrentCameraGraphs.cameraGraphIds - cameraGraphId)

            return sequencer
        }
    }
}

/**
 * A class that houses the shared [Mutex] for multiple sessions. This is used to ensure that only
 * one session configures its capture session at any given time.
 */
internal class ConcurrentSessionSequencer() {
    val sharedMutex = Mutex()
}

// New instance created by CaptureSessionState
/**
 * A class that is created by [CaptureSessionState] with a "link" to a [ConcurrentSessionSequencer].
 * It ensures that the shared [Mutex] held by the concurrent session sequencer is locked and
 * unlocked "in pairs", while allowing [release] to invoked multiple times given the multiple
 * possible terminating conditions during capture session creation.
 */
internal class SessionSequencer(private val concurrentSequencer: ConcurrentSessionSequencer) {
    enum class State {
        PENDING,
        CREATING,
        CREATED,
    }

    private val state = atomic<State>(State.PENDING)

    /** Should be called before attempting to create a capture session. */
    suspend fun awaitSessionLock() {
        concurrentSequencer.sharedMutex.lock()

        if (!state.compareAndSet(expect = State.PENDING, update = State.CREATING)) {
            concurrentSequencer.sharedMutex.unlock()
        }
    }

    /**
     * Should be called when the capture session creation finishes (including when it fails), and
     * when the [CaptureSessionState] is disconnected. The function can be invoked multiple times.
     */
    fun release() {
        if (state.getAndSet(State.CREATED) == State.CREATING) {
            concurrentSequencer.sharedMutex.unlock()
        }
    }
}
