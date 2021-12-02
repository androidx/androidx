/*
 * Copyright 2020 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.testing

import android.hardware.camera2.CaptureFailure
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake implementation of a [Request.Listener] for tests.
 *
 * Events are exposed as SharedFlows of events to allow a test to block and wait for events to
 * to be sent.
 */
@Suppress("ListenerInterface")
public class FakeRequestListener(private val replayBuffer: Int = 10) : Request.Listener {

    private val _onStartedFlow = MutableSharedFlow<OnStarted>(replay = replayBuffer)
    val onStartedFlow = _onStartedFlow.asSharedFlow()

    private val _onPartialCaptureResultFlow =
        MutableSharedFlow<OnPartialCaptureResult>(replay = replayBuffer)
    val onPartialCaptureResultFlow = _onPartialCaptureResultFlow.asSharedFlow()

    private val _onTotalCaptureResultFlow =
        MutableSharedFlow<OnTotalCaptureResult>(replay = replayBuffer)
    val onTotalCaptureResultFlow = _onTotalCaptureResultFlow.asSharedFlow()

    private val _onCompleteFlow =
        MutableSharedFlow<OnComplete>(replay = replayBuffer)
    val onCompleteFlow = _onCompleteFlow.asSharedFlow()

    private val _onBufferLostFlow =
        MutableSharedFlow<OnBufferLost>(replay = replayBuffer)
    val onBufferLostFlow = _onBufferLostFlow.asSharedFlow()

    private val _onAbortedFlow =
        MutableSharedFlow<OnAborted>(replay = replayBuffer)
    val onAbortedFlow = _onAbortedFlow.asSharedFlow()

    private val _onFailedFlow =
        MutableSharedFlow<OnFailed>(replay = replayBuffer)
    val onFailedFlow = _onFailedFlow.asSharedFlow()

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp
    ) = check(
        _onStartedFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnStarted(requestMetadata, frameNumber, timestamp)
        )
    ) {
        "Failed to emit onStarted event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata
    ) = check(
        _onPartialCaptureResultFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnPartialCaptureResult(requestMetadata, frameNumber, captureResult)
        )
    ) {
        "Failed to emit OnPartialCaptureResult event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo
    ) = check(
        _onTotalCaptureResultFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnTotalCaptureResult(requestMetadata, frameNumber, totalCaptureResult)
        )
    ) {
        "Failed to emit OnTotalCaptureResult event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onComplete(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        result: FrameInfo
    ) = check(
        _onCompleteFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnComplete(requestMetadata, frameNumber, result)
        )
    ) {
        "Failed to emit onComplete event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onAborted(
        request: Request
    ) = check(
        _onAbortedFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnAborted(request)
        )
    ) {
        "Failed to emit OnAborted event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        stream: StreamId
    ) = check(
        _onBufferLostFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnBufferLost(requestMetadata, frameNumber, stream)
        )
    ) {
        "Failed to emit OnBufferLost event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureFailure: CaptureFailure
    ) = check(
        _onFailedFlow.tryEmit(
            @Suppress("SyntheticAccessor")
            OnFailed(requestMetadata, frameNumber, captureFailure)
        )
    ) {
        "Failed to emit OnFailed event! The size of the replay buffer" +
            "($replayBuffer) may need to be increased."
    }
}

sealed class RequestListenerEvent
class OnStarted(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val timestamp: CameraTimestamp
) : RequestListenerEvent()

class OnPartialCaptureResult(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val frameMetadata: FrameMetadata
) : RequestListenerEvent()

class OnTotalCaptureResult(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val frameInfo: FrameInfo
) : RequestListenerEvent()

class OnComplete(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val frameInfo: FrameInfo
) : RequestListenerEvent()

class OnAborted(
    val request: Request
) : RequestListenerEvent()

class OnBufferLost(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val streamId: StreamId
) : RequestListenerEvent()

class OnFailed(
    val requestMetadata: RequestMetadata,
    val frameNumber: FrameNumber,
    val captureFailure: CaptureFailure
) : RequestListenerEvent()
