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

import android.hardware.camera2.CaptureResult
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Given a map of keys and a list of acceptable values for each key, this checks if the given
 * [CaptureResult] has all of those keys and for every key the value for that key is one of the
 * acceptable values. If the key set is empty then the condition is immediately met. This is helpful
 * for use cases where the value in the [CaptureResult] might not be exactly equal to the value
 * requested via a capture request. In those cases, just knowing that the correct request was
 * submitted and that at least one capture result for that request was received should suffice to
 * confirm that the desired key value pairs were applied by the camera device.
 *
 * This listener receives updates via [onPartialCaptureResult] and [onTotalCaptureResult] as we get
 * newer [CaptureResult]s from the camera device. This class also exposes a [Deferred] to query the
 * status of desired state.
 */
internal interface Result3AStateListener : GraphLoop.Listener {
    fun onRequestSequenceCreated(requestNumber: RequestNumber)

    fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata,
    )

    fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ): Boolean
}

internal class Result3AStateListenerImpl(
    private val exitCondition: (FrameMetadata) -> Boolean,
    private val frameLimit: Int? = null,
    private val timeLimitNs: Long? = null,
) : Result3AStateListener {

    internal constructor(
        exitConditionForKeys: Map<CaptureResult.Key<*>, List<Any>>,
        frameLimit: Int? = null,
        timeLimitNs: Long? = null,
    ) : this(
        exitCondition = exitConditionForKeys.toConditionChecker(),
        frameLimit = frameLimit,
        timeLimitNs = timeLimitNs,
    )

    private val _result = CompletableDeferred<Result3A>()
    val result: Deferred<Result3A>
        get() = _result

    private val _frameInfo = CompletableDeferred<FrameInfo>()

    @Volatile private var matchedFrameNumber: FrameNumber? = null
    @Volatile private var frameNumberOfFirstUpdate: FrameNumber? = null
    @Volatile private var timestampOfFirstUpdateNs: Long? = null
    @Volatile private var lastTotalCaptureResult: FrameInfo? = null
    @GuardedBy("this") private var initialRequestNumber: RequestNumber? = null

    override fun onRequestSequenceCreated(requestNumber: RequestNumber) {
        synchronized(this) {
            if (initialRequestNumber == null) {
                initialRequestNumber = requestNumber
            }
        }
    }

    override fun onPartialCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        captureResult: FrameMetadata,
    ) {
        if (_result.isCompleted) return
        matchConditions(requestMetadata.requestNumber, captureResult)
    }

    override fun onTotalCaptureResult(
        requestMetadata: RequestMetadata,
        frameNumber: FrameNumber,
        totalCaptureResult: FrameInfo,
    ): Boolean {
        lastTotalCaptureResult = totalCaptureResult
        if (_result.isCompleted) {
            val target = matchedFrameNumber
            if (target != null && frameNumber.value >= target.value) {
                _frameInfo.complete(totalCaptureResult)
                return true
            }
            return target == null
        }

        if (matchConditions(requestMetadata.requestNumber, totalCaptureResult.metadata)) {
            _frameInfo.complete(totalCaptureResult)
            return true
        }

        return false
    }

    /**
     * Checks if the current frame hits a timeout limit or fulfills the 3A exit conditions.
     * Completes the primary `_result` deferred if a terminal state is reached.
     *
     * @return true if a terminal state was reached, false otherwise.
     */
    private fun matchConditions(
        requestNumber: RequestNumber,
        frameMetadata: FrameMetadata,
    ): Boolean {
        if (!isUpdateValid(requestNumber)) return false

        if (checkLimits(frameMetadata)) return true

        if (exitCondition(frameMetadata)) {
            matchedFrameNumber = frameMetadata.frameNumber
            _result.complete(Result3A(Result3A.Status.OK, frameMetadata, _frameInfo))
            return true
        }

        return false
    }

    private fun isUpdateValid(requestNumber: RequestNumber): Boolean {
        synchronized(this) {
            val initialReqNum = initialRequestNumber
            return !(initialReqNum == null || requestNumber.value < initialReqNum.value)
        }
    }

    private fun checkLimits(frameMetadata: FrameMetadata): Boolean {
        val currentTimestampNs: Long? = frameMetadata.get(CaptureResult.SENSOR_TIMESTAMP)
        val currentFrameNumber = frameMetadata.frameNumber

        if (currentTimestampNs != null && timestampOfFirstUpdateNs == null) {
            timestampOfFirstUpdateNs = currentTimestampNs
        }

        val timestampFirstNs = timestampOfFirstUpdateNs
        if (
            timeLimitNs != null &&
                timestampFirstNs != null &&
                currentTimestampNs != null &&
                currentTimestampNs - timestampFirstNs > timeLimitNs
        ) {
            matchedFrameNumber = currentFrameNumber
            _result.complete(
                Result3A(Result3A.Status.TIME_LIMIT_REACHED, frameMetadata, _frameInfo)
            )
            return true
        }

        if (frameNumberOfFirstUpdate == null) {
            frameNumberOfFirstUpdate = currentFrameNumber
        }

        val frameFirstUpdate = frameNumberOfFirstUpdate
        if (
            frameFirstUpdate != null &&
                frameLimit != null &&
                currentFrameNumber.value - frameFirstUpdate.value > frameLimit
        ) {
            matchedFrameNumber = currentFrameNumber
            _result.complete(
                Result3A(Result3A.Status.FRAME_LIMIT_REACHED, frameMetadata, _frameInfo)
            )
            return true
        }

        return false
    }

    override fun onStopRepeating() {
        _frameInfo.cancel()
        _result.complete(Result3A(Result3A.Status.SUBMIT_CANCELLED, frameInfo = _frameInfo))
    }

    override fun onGraphStopped() {
        _frameInfo.cancel()
        _result.complete(Result3A(Result3A.Status.SUBMIT_CANCELLED, frameInfo = _frameInfo))
    }

    override fun onGraphShutdown() {
        _frameInfo.cancel()
        _result.complete(Result3A(Result3A.Status.SUBMIT_CANCELLED, frameInfo = _frameInfo))
    }
}

internal fun Map<CaptureResult.Key<*>, List<Any>>.toConditionChecker(): (FrameMetadata) -> Boolean {
    return conditionChecker@{ frameMetadata ->
        for ((k, v) in this) {
            val valueInCaptureResult = frameMetadata[k]
            if (!v.contains(valueInCaptureResult)) {
                return@conditionChecker false
            }
        }
        return@conditionChecker true
    }
}
