/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.Frame
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.StreamId
import kotlinx.atomicfu.atomic

internal class ListenerState(val listener: Frame.Listener) {
    private val isStarted = atomic(false)
    private val isImagesInvoked = atomic(false)
    private val isFrameInfoInvoked = atomic(false)
    private val isCompletedInvoked = atomic(false)

    /**
     * Invokes [listener.onFrameStarted].
     *
     * @param frameNumber The [FrameNumber] associated with the frame.
     * @param frameTimestamp The [CameraTimestamp] associated with the frame.
     */
    fun invokeOnStarted(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
        if (isStarted.compareAndSet(false, true)) {
            listener.onFrameStarted(frameNumber, frameTimestamp)
        }
    }

    /**
     * Invokes [listener.onImagesAvailable]. This method ensures that [listener.onFrameStarted] is
     * called before [listener.onImagesAvailable] is invoked by checking and potentially calling
     * [invokeOnStarted].
     *
     * @param frameNumber The [FrameNumber] associated with the frame.
     * @param frameTimestamp The [CameraTimestamp] associated with the frame.
     */
    fun invokeOnImagesAvailable(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
        invokeOnStarted(frameNumber, frameTimestamp)
        if (isImagesInvoked.compareAndSet(false, true)) {
            listener.onImagesAvailable()
        }
    }

    /**
     * Invokes [listener.onFrameInfoAvailable]. This method ensures that [listener.onFrameStarted]
     * is called before [listener.onFrameInfoAvailable] is invoked by checking and potentially
     * calling [invokeOnStarted].
     *
     * @param frameNumber The [FrameNumber] associated with the frame.
     * @param frameTimestamp The [CameraTimestamp] associated with the frame.
     */
    fun invokeOnFrameInfoAvailable(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
        invokeOnStarted(frameNumber, frameTimestamp)
        if (isFrameInfoInvoked.compareAndSet(false, true)) {
            listener.onFrameInfoAvailable()
        }
    }

    /**
     * Invokes [listener.onFrameComplete]. This method ensures that [listener.onFrameInfoAvailable]
     * and [Listener.onImagesAvailable] is called before [listener.onFrameComplete]
     *
     * @param frameNumber The [FrameNumber] associated with the frame.
     * @param frameTimestamp The [CameraTimestamp] associated with the frame.
     */
    fun invokeOnFrameComplete(frameNumber: FrameNumber, frameTimestamp: CameraTimestamp) {
        invokeOnImagesAvailable(frameNumber, frameTimestamp)
        invokeOnFrameInfoAvailable(frameNumber, frameTimestamp)
        if (isCompletedInvoked.compareAndSet(false, true)) {
            listener.onFrameComplete()
        }
    }

    /**
     * Invokes [listener.onImageAvailable(streamId)].
     *
     * @param streamId The [StreamId] that the image is available
     */
    fun invokeOnImageAvailable(streamId: StreamId) {
        listener.onImageAvailable(streamId)
    }
}
