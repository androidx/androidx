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
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

/**
 * Given a map of keys and a list of acceptable values for each key, this checks if the given
 * [CaptureResult] has all of those keys and for every key the value for that key is one of the
 * acceptable values. If the key set is empty then the [update] method returns true. This is helpful
 * for use cases where the value in the [CaptureResult] might not be exactly equal to the value
 * requested via a capture request. In those cases, just knowing that the correct request was
 * submitted and that at least one capture result for that request was received via the [update]
 * method should suffice to confirm that the desired key value pairs were applied by the camera
 * device.
 *
 * This update method can be called multiple times as we get newer [CaptureResult]s from the camera
 * device. This class also exposes a [Deferred] to query the status of desired state.
 */
internal interface Result3AStateListener {
    fun onRequestSequenceCreated(requestNumber: RequestNumber)
    fun update(requestNumber: RequestNumber, frameMetadata: FrameMetadata): Boolean
}

internal class Result3AStateListenerImpl(
    private val exitConditionForKeys: Map<CaptureResult.Key<*>, List<Any>>,
    private val frameLimit: Int? = null,
    private val timeLimitNs: Long? = null
) : Result3AStateListener {

    private val _result = CompletableDeferred<Result3A>()
    val result: Deferred<Result3A>
        get() = _result

    @Volatile
    private var frameNumberOfFirstUpdate: FrameNumber? = null
    @Volatile
    private var timestampOfFirstUpdateNs: Long? = null

    @GuardedBy("this")
    private var initialRequestNumber: RequestNumber? = null

    override fun onRequestSequenceCreated(requestNumber: RequestNumber) {
        synchronized(this) {
            if (initialRequestNumber == null) {
                initialRequestNumber = requestNumber
            }
        }
    }

    override fun update(requestNumber: RequestNumber, frameMetadata: FrameMetadata): Boolean {
        // Save some compute if the task is already complete or has been canceled.
        if (_result.isCompleted || _result.isCancelled) {
            return true
        }

        // Ignore the update if the update is from a previously submitted request.
        synchronized(this) {
            val initialRequestNumber = initialRequestNumber
            if (initialRequestNumber == null || requestNumber.value < initialRequestNumber.value) {
                return false
            }
        }

        val currentTimestampNs: Long? = frameMetadata.get(CaptureResult.SENSOR_TIMESTAMP)
        val currentFrameNumber = frameMetadata.frameNumber

        if (currentTimestampNs != null && timestampOfFirstUpdateNs == null) {
            timestampOfFirstUpdateNs = currentTimestampNs
        }

        val timestampOfFirstUpdateNs = timestampOfFirstUpdateNs
        if (timeLimitNs != null &&
            timestampOfFirstUpdateNs != null &&
            currentTimestampNs != null &&
            currentTimestampNs - timestampOfFirstUpdateNs > timeLimitNs
        ) {
            _result.complete(Result3A(Result3A.Status.TIME_LIMIT_REACHED, frameMetadata))
            return true
        }

        if (frameNumberOfFirstUpdate == null) {
            frameNumberOfFirstUpdate = currentFrameNumber
        }

        val frameNumberOfFirstUpdate = frameNumberOfFirstUpdate
        if (frameNumberOfFirstUpdate != null && frameLimit != null &&
            currentFrameNumber.value - frameNumberOfFirstUpdate.value > frameLimit
        ) {
            _result.complete(Result3A(Result3A.Status.FRAME_LIMIT_REACHED, frameMetadata))
            return true
        }

        for ((k, v) in exitConditionForKeys) {
            val valueInCaptureResult = frameMetadata[k]
            if (!v.contains(valueInCaptureResult)) {
                return false
            }
        }
        _result.complete(Result3A(Result3A.Status.OK, frameMetadata))
        return true
    }

    fun getDeferredResult(): Deferred<Result3A> {
        return _result
    }
}
