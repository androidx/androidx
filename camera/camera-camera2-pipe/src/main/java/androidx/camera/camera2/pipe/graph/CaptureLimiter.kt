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

package androidx.camera.camera2.pipe.graph

import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.core.Log
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.atomicfu.updateAndGet

/**
 * On some devices, we need to wait for 10 frames to complete before we can guarantee the success of
 * single capture requests. This is a quirk identified as part of b/287020251 and reported in
 * b/289284907.
 *
 * During initialization, setting the graphLoop will disableCaptureProcessing until after the
 * required number of frames have been completed.
 */
internal class CaptureLimiter(private val requestsUntilActive: Long) :
    Request.Listener, GraphLoop.Listener {
    init {
        require(requestsUntilActive > 0)
    }

    private val frameCount = atomic(0L)
    private var _graphLoop: GraphLoop? = null
    var graphLoop: GraphLoop
        get() = _graphLoop!!
        set(value) {
            check(_graphLoop == null) { "GraphLoop has already been set!" }
            _graphLoop = value
            value.captureProcessingEnabled = false
            Log.warn {
                "Capture processing has been disabled for $value until $requestsUntilActive " +
                    "frames have been completed."
            }
        }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) {
        val count = frameCount.updateAndGet { if (it == -1L) -1 else it + 1 }
        if (count == requestsUntilActive) {
            Log.warn { "Capture processing is now enabled for $_graphLoop after $count frames." }
            graphLoop.captureProcessingEnabled = true
        }
    }

    override fun onStopRepeating() {
        // Ignored
    }

    override fun onGraphStopped() {
        // If the cameraGraph is stopped, reset the counter
        frameCount.update { if (it == -1L) -1 else 0 }
        graphLoop.captureProcessingEnabled = false
        Log.warn {
            "Capture processing has been disabled for $graphLoop until $requestsUntilActive " +
                "frames have been completed."
        }
    }

    override fun onGraphShutdown() {
        frameCount.value = -1
        graphLoop.captureProcessingEnabled = false
    }
}
