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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.StreamId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Fake implementation of a [Request.Listener] for tests.
 *
 * Events are exposed as SharedFlows of events to allow a test to block and wait for events to to be
 * sent.
 */
@Suppress("ListenerInterface")
public class FakeRequestListener(private val replayBuffer: Int = 10) : Request.Listener {

    private val _onStartedFlow = MutableSharedFlow<OnStarted>(replay = replayBuffer)
    public val onStartedFlow: SharedFlow<OnStarted> = _onStartedFlow.asSharedFlow()

    private val _onPartialCaptureResultFlow =
        MutableSharedFlow<OnPartialCaptureResult>(replay = replayBuffer)
    public val onPartialCaptureResultFlow: SharedFlow<OnPartialCaptureResult> =
        _onPartialCaptureResultFlow.asSharedFlow()

    private val _onTotalCaptureResultFlow =
        MutableSharedFlow<OnTotalCaptureResult>(replay = replayBuffer)
    public val onTotalCaptureResultFlow: SharedFlow<OnTotalCaptureResult> =
        _onTotalCaptureResultFlow.asSharedFlow()

    private val _onCompleteFlow = MutableSharedFlow<OnComplete>(replay = replayBuffer)
    public val onCompleteFlow: SharedFlow<OnComplete> = _onCompleteFlow.asSharedFlow()

    private val _onBufferLostFlow = MutableSharedFlow<OnBufferLost>(replay = replayBuffer)
    public val onBufferLostFlow: SharedFlow<OnBufferLost> = _onBufferLostFlow.asSharedFlow()

    private val _onAbortedFlow = MutableSharedFlow<OnAborted>(replay = replayBuffer)
    public val onAbortedFlow: SharedFlow<OnAborted> = _onAbortedFlow.asSharedFlow()

    private val _onFailedFlow = MutableSharedFlow<OnFailed>(replay = replayBuffer)
    public val onFailedFlow: SharedFlow<OnFailed> = _onFailedFlow.asSharedFlow()

    override fun onStarted(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        timestamp: CameraTimestamp
    ): Unit =
        check(_onStartedFlow.tryEmit(OnStarted(requestMetadata, frameNumber, timestamp))) {
            "Failed to emit onStarted event! The size of the replay buffer" +
                "($replayBuffer) may need to be increased."
        }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata
    ): Unit =
        check(
            _onPartialCaptureResultFlow.tryEmit(
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
    ): Unit =
        check(
            _onTotalCaptureResultFlow.tryEmit(
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
    ): Unit =
        check(_onCompleteFlow.tryEmit(OnComplete(requestMetadata, frameNumber, result))) {
            "Failed to emit onComplete event! The size of the replay buffer" +
                "($replayBuffer) may need to be increased."
        }

    override fun onAborted(request: Request): Unit =
        check(_onAbortedFlow.tryEmit(OnAborted(request))) {
            "Failed to emit OnAborted event! The size of the replay buffer" +
                "($replayBuffer) may need to be increased."
        }

    override fun onBufferLost(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        stream: StreamId
    ): Unit =
        check(_onBufferLostFlow.tryEmit(OnBufferLost(requestMetadata, frameNumber, stream))) {
            "Failed to emit OnBufferLost event! The size of the replay buffer" +
                "($replayBuffer) may need to be increased."
        }

    override fun onFailed(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        requestFailure: RequestFailure
    ): Unit =
        check(_onFailedFlow.tryEmit(OnFailed(requestMetadata, frameNumber, requestFailure))) {
            "Failed to emit OnFailed event! The size of the replay buffer" +
                "($replayBuffer) may need to be increased."
        }
}

public sealed class RequestListenerEvent

public class OnStarted(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val timestamp: CameraTimestamp
) : RequestListenerEvent()

public class OnPartialCaptureResult(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val frameMetadata: FrameMetadata
) : RequestListenerEvent()

public class OnTotalCaptureResult(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val frameInfo: FrameInfo
) : RequestListenerEvent()

public class OnComplete(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val frameInfo: FrameInfo
) : RequestListenerEvent()

public class OnAborted(public val request: Request) : RequestListenerEvent()

public class OnBufferLost(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val streamId: StreamId
) : RequestListenerEvent()

public class OnFailed(
    public val requestMetadata: RequestMetadata,
    public val frameNumber: FrameNumber,
    public val requestFailure: RequestFailure
) : RequestListenerEvent()
