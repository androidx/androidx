/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequences.invokeOnRequests
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata

/** A CaptureSequence used for testing interactions with a [FakeCaptureSequenceProcessor] */
public data class FakeCaptureSequence(
    override val repeating: Boolean,
    override val cameraId: CameraId,
    override val captureRequestList: List<Request>,
    override val captureMetadataList: List<RequestMetadata>,
    val requestMetadata: Map<Request, RequestMetadata>,
    val defaultParameters: Map<*, Any?>,
    val requiredParameters: Map<*, Any?>,
    override val listeners: List<Request.Listener>,
    override val sequenceListener: CaptureSequence.CaptureSequenceListener,
    override var sequenceNumber: Int,
) : CaptureSequence<Request> {
    public fun invokeOnSequenceCreated(): Unit = invokeOnRequests { requestMetadata, _, listener ->
        listener.onRequestSequenceCreated(requestMetadata)
    }

    public fun invokeOnSequenceSubmitted(): Unit =
        invokeOnRequests { requestMetadata, _, listener ->
            listener.onRequestSequenceSubmitted(requestMetadata)
        }

    public fun invokeOnSequenceAborted(): Unit = invokeOnRequests { requestMetadata, _, listener ->
        listener.onRequestSequenceAborted(requestMetadata)
    }

    public fun invokeOnSequenceCompleted(frameNumber: FrameNumber): Unit =
        invokeOnRequests { requestMetadata, _, listener ->
            listener.onRequestSequenceCompleted(requestMetadata, frameNumber)
        }
}
