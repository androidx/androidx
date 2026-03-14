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
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.Result3A
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.camera2.pipe.testing.RobolectricCameraPipeTestRunner
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
@OptIn(ExperimentalCoroutinesApi::class)
internal class Result3AStateListenerImplTest {

    private fun Result3AStateListenerImpl.simulatePartialUpdate(
        requestNumber: RequestNumber,
        frameMetadata: FrameMetadata,
    ) {
        val requestMetadata = FakeRequestMetadata(requestNumber = requestNumber)
        this.onPartialCaptureResult(requestMetadata, frameMetadata.frameNumber, frameMetadata)
    }

    @Test
    fun testWithEmptyExitConditionForKeys() {
        val listenerForKeys = Result3AStateListenerImpl(mapOf())
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata = FakeFrameMetadata()

        listenerForKeys.onRequestSequenceCreated(RequestNumber(2))

        // Even though we received an update, the request number is not correct, so the listener
        // will not be completed.
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        // Since the key set in listener is empty, any valid update will mark the listener as
        // completed.
        listenerForKeys.simulatePartialUpdate(RequestNumber(2), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testWithNoUpdate() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        assertThat(listenerForKeys.result.isCompleted).isFalse()
    }

    @Test
    fun testKeyWithUndesirableValueInFrameMetadata() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
                    )
            )

        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()
    }

    @Test
    fun testKeyWithDesirableValueInFrameMetadata() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testKeyNotPresentInFrameMetadata() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()
    }

    @Test
    fun testMultipleKeysWithDesiredValuesInFrameMetadata() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
                        ),
                    CaptureResult.CONTROL_AE_STATE to listOf(CaptureResult.CONTROL_AE_STATE_LOCKED),
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED,
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testMultipleKeysWithDesiredValuesInFrameMetadataForASubset() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
                        ),
                    CaptureResult.CONTROL_AE_STATE to listOf(CaptureResult.CONTROL_AE_STATE_LOCKED),
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()
    }

    @Test
    fun testMultipleUpdates() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
                        ),
                    CaptureResult.CONTROL_AE_STATE to listOf(CaptureResult.CONTROL_AE_STATE_LOCKED),
                )
            )
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata1 =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                        CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED,
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata1)
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testTimeLimit() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                exitConditionForKeys =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    ),
                timeLimitNs = 1000000000L,
            )
        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        listenerForKeys.onRequestSequenceCreated(requestMetadata.requestNumber)

        val frameMetadata1 =
            FakeFrameMetadata(
                frameNumber = FrameNumber(1),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 400000000L,
                    ),
            )
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata1)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata2 =
            FakeFrameMetadata(
                frameNumber = FrameNumber(2),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 900000000L,
                    ),
            )
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata2)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata3 =
            FakeFrameMetadata(
                frameNumber = FrameNumber(3),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 1500000000L,
                    ),
            )

        // This partial update crosses the time limit threshold
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata3)

        val completedDeferred = listenerForKeys.result
        assertThat(completedDeferred.isCompleted).isTrue()

        val result3A = completedDeferred.getCompleted()
        assertThat(result3A.status).isEqualTo(Result3A.Status.TIME_LIMIT_REACHED)

        // NEW BEHAVIOR: frameInfo is NOT completed yet. It is waiting for the total result of frame
        // 3.
        assertThat(result3A.frameInfo.isCompleted).isFalse()

        // Now, the total result for that timeout frame finally arrives
        val frameInfo3 = FakeFrameInfo(metadata = frameMetadata3)
        val shouldRemove =
            listenerForKeys.onTotalCaptureResult(
                requestMetadata,
                frameMetadata3.frameNumber,
                frameInfo3,
            )

        assertThat(shouldRemove).isTrue()
        assertThat(result3A.frameInfo.isCompleted).isTrue()
        assertThat(result3A.frameInfo.getCompleted()).isEqualTo(frameInfo3)
    }

    @Test
    fun testFrameLimit() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                exitConditionForKeys =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    ),
                frameLimit = 10,
            )
        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        listenerForKeys.onRequestSequenceCreated(requestMetadata.requestNumber)

        val frameMetadata1 =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 400000000L,
                    ),
                frameNumber = FrameNumber(1),
            )
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata1)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata2 =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 900000000L,
                    ),
                frameNumber = FrameNumber(3),
            )
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata2)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata3 =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 1500000000L,
                    ),
                frameNumber = FrameNumber(10),
            )
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata3)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        val frameMetadata4 =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 1700000000L,
                    ),
                frameNumber = FrameNumber(12),
            )

        // This partial update crosses the frame limit threshold
        listenerForKeys.simulatePartialUpdate(requestMetadata.requestNumber, frameMetadata4)

        val completedDeferred = listenerForKeys.result
        assertThat(completedDeferred.isCompleted).isTrue()

        val result3A = completedDeferred.getCompleted()
        assertThat(result3A.status).isEqualTo(Result3A.Status.FRAME_LIMIT_REACHED)

        // NEW BEHAVIOR: frameInfo is NOT completed yet. It is waiting for the total result of frame
        // 12.
        assertThat(result3A.frameInfo.isCompleted).isFalse()

        // Now, the total result for that timeout frame finally arrives
        val frameInfo4 = FakeFrameInfo(metadata = frameMetadata4)
        val shouldRemove =
            listenerForKeys.onTotalCaptureResult(
                requestMetadata,
                frameMetadata4.frameNumber,
                frameInfo4,
            )

        assertThat(shouldRemove).isTrue()
        assertThat(result3A.frameInfo.isCompleted).isTrue()
        assertThat(result3A.frameInfo.getCompleted()).isEqualTo(frameInfo4)
    }

    @Test
    fun testIgnoreUpdatesFromEarlierRequests() {
        val listenerForKeys =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )

        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
            )
        // The reference request number of not yet set on the listener, so the update will be
        // ignored.
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        // Update the reference request number for this listener.
        listenerForKeys.onRequestSequenceCreated(RequestNumber(3))

        // The update is coming from an earlier request so it will be ignored.
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        // The update is coming from an earlier request so it will be ignored.
        listenerForKeys.simulatePartialUpdate(RequestNumber(2), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isFalse()

        // The update is from the same or later request number so it will be accepted.
        listenerForKeys.simulatePartialUpdate(RequestNumber(3), frameMetadata)
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testExitFunctionWithDesiredValue() {
        val exitCondition: (FrameMetadata) -> Boolean = { frameMetadata ->
            frameMetadata[CaptureResult.CONTROL_AF_STATE] ==
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
        }

        val listenerForKeys = Result3AStateListenerImpl(exitCondition)
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))
        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    )
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)

        // Assert. Task is completed when receiving the desired value in the FrameMetadata.
        assertThat(listenerForKeys.result.isCompleted).isTrue()
    }

    @Test
    fun testExitFunctionWithUndesirableValue() {
        val exitCondition: (FrameMetadata) -> Boolean = { frameMetadata ->
            frameMetadata[CaptureResult.CONTROL_AF_STATE] ==
                CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
        }

        val listenerForKeys = Result3AStateListenerImpl(exitCondition)
        listenerForKeys.onRequestSequenceCreated(RequestNumber(1))
        val frameMetadata =
            FakeFrameMetadata(
                resultMetadata =
                    mapOf(CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_INACTIVE)
            )
        listenerForKeys.simulatePartialUpdate(RequestNumber(1), frameMetadata)

        // Assert. Task is completed when receiving the desired value in the FrameMetadata.
        assertThat(listenerForKeys.result.isCompleted).isFalse()
    }

    @Test
    fun testTotalCaptureResultCompletesFrameInfo() {
        val listener =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        val requestNumber = RequestNumber(1)
        val requestMetadata = FakeRequestMetadata(requestNumber = requestNumber)
        listener.onRequestSequenceCreated(requestNumber)

        val frameNumber = FrameNumber(100L)
        val frameMetadata =
            FakeFrameMetadata(
                frameNumber = frameNumber,
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    ),
            )

        // Partial result matches the condition, completing the main result but NOT the frame info
        listener.onPartialCaptureResult(requestMetadata, frameNumber, frameMetadata)

        assertThat(listener.result.isCompleted).isTrue()
        val result3A = listener.result.getCompleted()
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
        assertThat(result3A.frameInfo.isCompleted).isFalse()

        // Total result arrives for the target frame, fulfilling the frame info and unregistering
        val frameInfo = FakeFrameInfo(metadata = frameMetadata)
        val shouldRemove = listener.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)

        assertThat(shouldRemove).isTrue()
        assertThat(result3A.frameInfo.isCompleted).isTrue()
        assertThat(result3A.frameInfo.getCompleted()).isEqualTo(frameInfo)
    }

    @Test
    fun testTotalCaptureResultDirectlyCompletesResult3A() {
        val listener =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        val requestNumber = RequestNumber(1)
        val requestMetadata = FakeRequestMetadata(requestNumber = requestNumber)
        listener.onRequestSequenceCreated(requestNumber)

        val frameNumber = FrameNumber(100L)
        val frameMetadata =
            FakeFrameMetadata(
                frameNumber = frameNumber,
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                    ),
            )
        val frameInfo = FakeFrameInfo(metadata = frameMetadata)

        // Total result arrives and matches the condition (no partial result arrived first)
        val shouldRemove = listener.onTotalCaptureResult(requestMetadata, frameNumber, frameInfo)

        assertThat(shouldRemove).isTrue()
        assertThat(listener.result.isCompleted).isTrue()

        val result3A = listener.result.getCompleted()
        assertThat(result3A.status).isEqualTo(Result3A.Status.OK)
        assertThat(result3A.frameInfo.getCompleted()).isEqualTo(frameInfo)
    }

    @Test
    fun testCancelWithoutTotalResultCancelsFrameInfo() {
        val listener =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )

        // Cancel the graph before any total results arrive
        listener.onGraphStopped()

        assertThat(listener.result.isCompleted).isTrue()
        val result3A = listener.result.getCompleted()

        assertThat(result3A.status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        // Since no total result arrived, the frameInfo deferred should be cancelled
        assertThat(result3A.frameInfo.isCancelled).isTrue()
    }

    @Test
    fun testCancelWithPreviousTotalResultCancelsFrameInfo() {
        val listener =
            Result3AStateListenerImpl(
                mapOf(
                    CaptureResult.CONTROL_AF_STATE to
                        listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                )
            )
        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        listener.onRequestSequenceCreated(requestMetadata.requestNumber)

        val frameMetadata =
            FakeFrameMetadata(
                frameNumber = FrameNumber(1),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN
                    ),
            )
        val frameInfo = FakeFrameInfo(metadata = frameMetadata)

        // Send a partial and total result that do NOT meet the exit condition
        listener.onPartialCaptureResult(requestMetadata, frameMetadata.frameNumber, frameMetadata)
        listener.onTotalCaptureResult(requestMetadata, frameMetadata.frameNumber, frameInfo)

        // Now cancel the repeating request
        listener.onStopRepeating()

        assertThat(listener.result.isCompleted).isTrue()
        val result3A = listener.result.getCompleted()

        assertThat(result3A.status).isEqualTo(Result3A.Status.SUBMIT_CANCELLED)
        // Even though a previous frameInfo arrived, the full failure should cancel the deferred
        assertThat(result3A.frameInfo.isCancelled).isTrue()
    }

    @Test
    fun testTimeLimitWaitsForCurrentTotalResult() {
        val listener =
            Result3AStateListenerImpl(
                exitConditionForKeys =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            listOf(CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED)
                    ),
                timeLimitNs = 1000000000L,
            )
        val requestMetadata = FakeRequestMetadata(requestNumber = RequestNumber(1))
        listener.onRequestSequenceCreated(requestMetadata.requestNumber)

        // Update 1: Set the baseline time and provide a total capture result
        val frame1 =
            FakeFrameMetadata(
                frameNumber = FrameNumber(1),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 400000000L,
                    ),
            )
        val frameInfo1 = FakeFrameInfo(metadata = frame1)
        listener.onPartialCaptureResult(requestMetadata, frame1.frameNumber, frame1)
        listener.onTotalCaptureResult(requestMetadata, frame1.frameNumber, frameInfo1)

        // Update 2: Exceed the time limit
        val frame2 =
            FakeFrameMetadata(
                frameNumber = FrameNumber(2),
                resultMetadata =
                    mapOf(
                        CaptureResult.CONTROL_AF_STATE to
                            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN,
                        CaptureResult.SENSOR_TIMESTAMP to 1500000000L,
                    ),
            )

        // This partial update will trigger the timeout
        listener.onPartialCaptureResult(requestMetadata, frame2.frameNumber, frame2)

        assertThat(listener.result.isCompleted).isTrue()
        val result3A = listener.result.getCompleted()

        assertThat(result3A.status).isEqualTo(Result3A.Status.TIME_LIMIT_REACHED)

        // frameInfo is NOT completed yet. It is waiting for the total result of frame 2.
        assertThat(result3A.frameInfo.isCompleted).isFalse()

        // Send the actual total result for the frame that caused the timeout
        val frameInfo2 = FakeFrameInfo(metadata = frame2)
        val shouldRemove =
            listener.onTotalCaptureResult(requestMetadata, frame2.frameNumber, frameInfo2)

        assertThat(shouldRemove).isTrue()
        // It should successfully complete using frameInfo2, not frameInfo1
        assertThat(result3A.frameInfo.isCompleted).isTrue()
        assertThat(result3A.frameInfo.getCompleted()).isEqualTo(frameInfo2)
    }
}
