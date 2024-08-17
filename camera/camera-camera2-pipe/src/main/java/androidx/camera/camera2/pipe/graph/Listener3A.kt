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

import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.config.CameraGraphScope
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

/**
 * A [Request.Listener] to receive partial and final metadata for each request sent to the camera
 * device. It maintains a list of [Result3AStateListener] to which it broadcasts the updates and
 * removes them as they are completed. This listener is useful for implementing 3A methods to look
 * for desired 3A state changes.
 */
@CameraGraphScope
internal class Listener3A @Inject constructor() : Request.Listener, GraphLoop.Listener {
    private val listeners: CopyOnWriteArrayList<Result3AStateListener> = CopyOnWriteArrayList()

    override fun onRequestSequenceCreated(requestMetadata: RequestMetadata) {
        for (listener in listeners) {
            listener.onRequestSequenceCreated(requestMetadata.requestNumber)
        }
    }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata
    ) {
        updateListeners(requestMetadata.requestNumber, captureResult)
    }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo
    ) {
        updateListeners(requestMetadata.requestNumber, totalCaptureResult.metadata)
    }

    fun addListener(listener: Result3AStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Result3AStateListener) {
        listeners.remove(listener)
    }

    private fun updateListeners(requestNumber: RequestNumber, metadata: FrameMetadata) {
        for (listener in listeners) {
            if (listener.update(requestNumber, metadata)) {
                listeners.remove(listener)
            }
        }
    }

    override fun onStopRepeating() {
        for (listener in listeners) {
            listener.onStopRepeating()
        }
    }

    override fun onGraphStopped() {
        for (listener in listeners) {
            listener.onStopRepeating()
        }
    }

    override fun onGraphShutdown() {
        for (listener in listeners) {
            listener.onStopRepeating()
        }
    }
}
