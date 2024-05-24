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

package androidx.camera.camera2.pipe.internal

import android.os.Build
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.OutputStatus
import androidx.camera.camera2.pipe.internal.OutputDistributor.OutputListener
import androidx.camera.camera2.pipe.media.Finalizer
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.atomicfu.atomic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [OutputDistributor] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class OutputDistributorTest {
    private val fakeOutput1 = FakeOutput(101)
    private val fakeOutput2 = FakeOutput(102)
    private val fakeOutput3 = FakeOutput(103)
    private val fakeOutput4 = FakeOutput(104)
    private val fakeOutput5 = FakeOutput(105)
    private val fakeOutput6 = FakeOutput(106)

    private val pendingOutput1 =
        PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
    private val pendingOutput2 =
        PendingOutput(FrameNumber(2), CameraTimestamp(12), outputNumber = 102)
    private val pendingOutput3 =
        PendingOutput(FrameNumber(3), CameraTimestamp(13), outputNumber = 103)
    private val pendingOutput4 =
        PendingOutput(FrameNumber(4), CameraTimestamp(14), outputNumber = 104)

    private val outputDistributor =
        OutputDistributor(
            maximumCachedOutputs = 3,
            outputFinalizer =
                object : Finalizer<FakeOutput> {
                    override fun finalize(value: FakeOutput?) {
                        value?.finalize()
                    }
                }
        )

    @Test
    fun onOutputAvailableDoesNotFinalizeOutputs() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))

        // When an output becomes available, ensure it is not immediately finalized.
        assertThat(fakeOutput1.finalized).isFalse()
    }

    @Test
    fun onOutputAvailableEvictsAndFinalizesPreviousOutputs() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))
        outputDistributor.onOutputResult(fakeOutput4.outputNumber, OutputResult.from(fakeOutput4))

        // outputDistributor will only cache up to three outputs without matching start events.

        // Ensure the oldest output is finalized:
        assertThat(fakeOutput1.finalized).isTrue()

        // The newest outputs are not finalized:
        assertThat(fakeOutput2.finalized).isFalse()
        assertThat(fakeOutput3.finalized).isFalse()
        assertThat(fakeOutput4.finalized).isFalse()
    }

    @Test
    fun onOutputAvailableEvictsAndFinalizesOutputsInSequence() {
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))
        outputDistributor.onOutputResult(fakeOutput4.outputNumber, OutputResult.from(fakeOutput4))
        outputDistributor.onOutputResult(
            fakeOutput1.outputNumber,
            OutputResult.from(fakeOutput1)
        ) // Out of order

        // FIFO Order for outputs, regardless of the output number.
        // Note: Outputs are provided as [2, 3, 4, *1*]
        assertThat(fakeOutput2.finalized).isTrue()
        assertThat(fakeOutput3.finalized).isFalse()
        assertThat(fakeOutput4.finalized).isFalse()
        assertThat(fakeOutput1.finalized).isFalse()
    }

    @Test
    fun onOutputAvailableWithNullEvictsAndFinalizesOutputs() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))

        outputDistributor.onOutputResult(
            fakeOutput4.outputNumber,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED)
        )
        outputDistributor.onOutputResult(
            fakeOutput5.outputNumber,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED)
        )
        outputDistributor.onOutputResult(
            fakeOutput6.outputNumber,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED)
        )

        // Dropped outputs (null) still evict old outputs.
        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
        assertThat(fakeOutput3.finalized).isTrue()
    }

    @Test
    fun closingOutputDistributorFinalizesCachedOutputs() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))

        // Outputs that have not been matched with started events must be closed when the
        // outputDistributor is closed.
        outputDistributor.close()

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun closingOutputDistributorBeforeOnOutputAvailableFinalizesNewOutputs() {
        outputDistributor.close()

        // Outputs that occur after close must always be finalized immediately.
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun pendingResultsAreMatchedWithOutputs() {
        // When a a start event occurs and an output is also available, ensure the callback
        // is correctly invoked.
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isEqualTo(fakeOutput1)
        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun onOutputStartedEventsAreQueuedUp() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        assertThat(pendingOutput1.isComplete).isFalse()
        assertThat(pendingOutput2.isComplete).isFalse()
    }

    @Test
    fun pendingResultsAreMatchedWithNullOutputs() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.onOutputResult(
            fakeOutput1.outputNumber,
            OutputResult.failure(OutputStatus.ERROR_OUTPUT_DROPPED)
        )

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_DROPPED)
    }

    @Test
    fun previousOutputsAreCompletedWhenNewerOutputIsMatched() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)
        outputDistributor.startWith(pendingOutput4)

        outputDistributor.onOutputResult(
            fakeOutput3.outputNumber,
            OutputResult.from(fakeOutput3)
        ) // Match 3

        assertThat(pendingOutput1.isComplete).isTrue() // #1 is Canceled
        assertThat(pendingOutput2.isComplete).isTrue() // #2 is Canceled
        assertThat(pendingOutput3.isComplete).isTrue()
        assertThat(pendingOutput4.isComplete).isFalse()

        assertThat(pendingOutput1.output).isNull() // #1 is Canceled
        assertThat(pendingOutput2.output).isNull() // #2 is Canceled
        assertThat(pendingOutput3.output).isEqualTo(fakeOutput3)
        assertThat(pendingOutput4.output).isNull() // #4 is still pending

        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_MISSING)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_MISSING)
    }

    @Test
    fun closingOutputDistributorBeforeOnOutputStartedCompletesOutputs() {
        outputDistributor.close()

        // Outputs that are started after the outputDistributor is closed have the callback invoked,
        // but are immediately completed with a null output.
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
    }

    @Test
    fun closingOutputDistributorAfterOnOutputStartedCompletesOutputs() {
        // Outputs that are started before the outputDistributor is closed have the callback
        // invoked, and are completed with the correct values an null output.
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        outputDistributor.close()

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
    }

    @Test
    fun availableOutputsAreNotDistributedToStartedOutputsAfterClose() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        outputDistributor.close()
        outputDistributor.startWith(pendingOutput1) // Note: Would normally match fakeOutput1
        outputDistributor.startWith(pendingOutput2) // Note: Would normally match fakeOutput2

        // If we have valid outputs, but then receive close, and then receive matching start events
        // ensure the outputs are considered dropped.

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()

        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()

        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun startedOutputsOutputsAreNotDistributedToAvailableOutputsAfterClose() {
        outputDistributor.startWith(pendingOutput1) // Note: Would normally match fakeOutput1
        outputDistributor.startWith(pendingOutput2) // Note: Would normally match fakeOutput2
        outputDistributor.close()
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))

        // If we have valid start events, but then receive close, and then receive matching outputs,
        // ensure all outputs are still considered dropped.

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()

        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()

        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_ABORTED)

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun outOfOrderOutputsAreNotImmediatelyCanceled() {
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)
        outputDistributor.startWith(pendingOutput4)
        outputDistributor.startWith(pendingOutput1) // Note! Out of order start event

        // Complete Output 1
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isFalse() // Since 1 was out of order, do not cancel
        assertThat(pendingOutput3.isComplete).isFalse() // Since 1 was out of order, do not cancel
        assertThat(pendingOutput4.isComplete).isFalse() // Since 1 was out of order, do not cancel

        assertThat(pendingOutput1.output).isEqualTo(fakeOutput1)
        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput3.output).isNull()
        assertThat(pendingOutput4.output).isNull()
    }

    @Test
    fun multipleOutOfOrderOutputsAreCanceledWhenOldOutputCompletes() {
        outputDistributor.startWith(pendingOutput4)

        outputDistributor.startWith(pendingOutput1) // Out of order (relative to 4)
        outputDistributor.startWith(pendingOutput2) // Out of order (relative to 4)
        outputDistributor.startWith(pendingOutput3) // Out of order (relative to 4)

        // Complete output 3!
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))

        assertThat(pendingOutput1.isComplete).isTrue() // Cancelled. 1 < 3
        assertThat(pendingOutput2.isComplete).isTrue() // Cancelled. 2 < 3
        assertThat(pendingOutput3.isComplete).isTrue() // Success: 3 = 3
        assertThat(pendingOutput4.isComplete).isFalse() // Ignored 4 > 3

        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput3.output).isEqualTo(fakeOutput3)

        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_MISSING)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_MISSING)
        assertThat(pendingOutput3.outputStatus).isEqualTo(OutputStatus.AVAILABLE)
    }

    @Test
    fun fullyCompletedOutputAfterOutOfOrderResultDoesNotCancelPendingOutput() {
        outputDistributor.startWith(pendingOutput2) // Normal (first)
        outputDistributor.startWith(pendingOutput1) // Out of order start
        outputDistributor.startWith(pendingOutput3) // Normal (> 2)
        outputDistributor.startWith(pendingOutput4) // Normal (> 3)

        // Normal outputs complete
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))
        outputDistributor.onOutputResult(fakeOutput4.outputNumber, OutputResult.from(fakeOutput4))

        // Then the out of order event completes
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))

        // All of the outputs are correctly distributed:
        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput3.isComplete).isTrue()
        assertThat(pendingOutput4.isComplete).isTrue()

        assertThat(pendingOutput1.output).isEqualTo(fakeOutput1)
        assertThat(pendingOutput2.output).isEqualTo(fakeOutput2)
        assertThat(pendingOutput3.output).isEqualTo(fakeOutput3)
        assertThat(pendingOutput4.output).isEqualTo(fakeOutput4)

        // Sequence is based on the order the outputs were started:
        assertThat(pendingOutput1.outputSequence).isEqualTo(2)
        assertThat(pendingOutput2.outputSequence).isEqualTo(1)
        assertThat(pendingOutput3.outputSequence).isEqualTo(3)
        assertThat(pendingOutput4.outputSequence).isEqualTo(4)
    }

    @Test
    fun failedOutputFailPendingOutputs() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)

        outputDistributor.onOutputFailure(pendingOutput2.cameraFrameNumber)

        assertThat(pendingOutput1.isComplete).isFalse()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput3.isComplete).isFalse()

        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun failedOutputFailMultiplePendingOutputs() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)

        outputDistributor.onOutputFailure(pendingOutput2.cameraFrameNumber)
        outputDistributor.onOutputFailure(pendingOutput3.cameraFrameNumber)
        outputDistributor.onOutputFailure(pendingOutput1.cameraFrameNumber)

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput3.isComplete).isTrue()

        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput3.output).isNull()

        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
        assertThat(pendingOutput3.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun outputFailureBeforeStartFailsPendingOutput() {
        outputDistributor.onOutputFailure(pendingOutput2.cameraFrameNumber)

        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)

        assertThat(pendingOutput1.isComplete).isFalse()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput3.isComplete).isFalse()

        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput2.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun previouslyAddedOutputIsClosedAfterFailure() {
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        outputDistributor.onOutputFailure(pendingOutput1.cameraFrameNumber)

        // Output cannot be matched with frameNumber
        assertThat(fakeOutput1.finalized).isFalse()
        // Output can be matched after onOutputStart occurs
        outputDistributor.startWith(pendingOutput1)

        // Output should be finalized, and the pending output should NOT receive it:
        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput1.outputStatus).isEqualTo(OutputStatus.ERROR_OUTPUT_FAILED)
    }

    @Test
    fun outputDistributorIgnoresIdenticalFrameNumbersButDifferentOutputNumbers() {
        val pendingOutput1 = PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
        val pendingOutput2 = PendingOutput(FrameNumber(1), CameraTimestamp(12), outputNumber = 102)
        outputDistributor.startWith(pendingOutput1)
        // We shouldn't throw when OutputDistributor is started with identical frame numbers.
        outputDistributor.startWith(pendingOutput2)

        // OutputDistributor receives the capture result for output 1, and should complete it.
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        // OutputDistributor receives the capture result for output 2, but since the onOutputStarted
        // call for output 2 is ignored, pendingOutput2 should not be completed.
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)
    }

    @Test
    fun outputDistributorIgnoresIdenticalFrameNumbersAndIdenticalOutputNumbers() {
        val pendingOutput1 = PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
        val pendingOutput2 = PendingOutput(FrameNumber(1), CameraTimestamp(12), outputNumber = 101)
        outputDistributor.startWith(pendingOutput1)
        // We shouldn't throw when OutputDistributor is started with identical frame numbers.
        outputDistributor.startWith(pendingOutput2)

        // OutputDistributor receives the capture result for output 1, and should complete it.
        val fakeOutput1 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        // OutputDistributor receives the capture result for output 2. Even though the output number
        // is the same should normally result in a match, the onOutputStarted call for output 2 is
        // ignored due to their identical frame numbers, and thus pendingOutput2 should not be
        // completed.
        val fakeOutput2 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)
    }

    @Test
    fun outputDistributorFinalizesDuplicateResultsEventually() {
        val pendingOutput1 = PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
        val pendingOutput2 = PendingOutput(FrameNumber(1), CameraTimestamp(12), outputNumber = 101)
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        val fakeOutput1 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        // Simulate OutputDistributor getting a duplicated OutputResult. In this case, the
        // OutputResult will be cached, since its corresponding onOutputStarted event is ignored.
        val fakeOutput2 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        // Simulate getting 3 more OutputResults, which in this case, should cause the
        // OutputDistributor to exceed its maximum number of cached OutputResults (by default), and
        // |fakeOutput2|, being the oldest OutputResult, should be finalized.
        val fakeOutput3 = FakeOutput(103)
        val fakeOutput4 = FakeOutput(104)
        val fakeOutput5 = FakeOutput(105)
        outputDistributor.onOutputResult(fakeOutput3.outputNumber, OutputResult.from(fakeOutput3))
        outputDistributor.onOutputResult(fakeOutput4.outputNumber, OutputResult.from(fakeOutput4))
        outputDistributor.onOutputResult(fakeOutput5.outputNumber, OutputResult.from(fakeOutput5))
        assertTrue(fakeOutput2.finalized)
    }

    @Test
    fun pendingOutputCompletesOnIdenticalTimestamps() {
        val pendingOutput1 = PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
        val pendingOutput2 = PendingOutput(FrameNumber(2), CameraTimestamp(11), outputNumber = 102)
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        assertTrue(pendingOutput1.isComplete)
        assertTrue(pendingOutput2.isComplete)
    }

    @Test
    fun pendingOutputCompletesOnIdenticalOutputNumbers() {
        val pendingOutput1 = PendingOutput(FrameNumber(1), CameraTimestamp(11), outputNumber = 101)
        val pendingOutput2 = PendingOutput(FrameNumber(2), CameraTimestamp(12), outputNumber = 101)
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)

        val fakeOutput1 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput1.outputNumber, OutputResult.from(fakeOutput1))
        assertTrue(pendingOutput1.isComplete)
        assertFalse(pendingOutput2.isComplete)

        val fakeOutput2 = FakeOutput(101)
        outputDistributor.onOutputResult(fakeOutput2.outputNumber, OutputResult.from(fakeOutput2))
        assertTrue(pendingOutput1.isComplete)
        assertTrue(pendingOutput2.isComplete)
    }

    /**
     * Utility class that implements [OutputListener] and can be used to observe when an output is
     * complete and the callback is invoked.
     */
    private class PendingOutput(
        val cameraFrameNumber: FrameNumber,
        val cameraTimestamp: CameraTimestamp,
        val outputNumber: Long
    ) : OutputListener<FakeOutput> {
        private val _complete = atomic(false)
        val isComplete: Boolean
            get() = _complete.value

        var outputSequence: Long? = null
        var output: FakeOutput? = null
        var outputStatus: OutputStatus? = null

        override fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            outputResult: OutputResult<FakeOutput>
        ) {
            // Assert that this callback has only been invoked once.
            assertThat(_complete.compareAndSet(expect = false, update = true)).isTrue()

            // Assert that the parameters in the invoked callback match the expected values.
            assertThat(cameraFrameNumber).isEqualTo(cameraFrameNumber)
            assertThat(cameraTimestamp).isEqualTo(cameraTimestamp)
            assertThat(outputNumber).isEqualTo(outputNumber)

            // Record the actual output and outputSequence for future checks.
            this.outputSequence = outputSequence
            this.outputStatus = outputResult.status
            this.output = outputResult.output
        }
    }

    /** Utility function for invoking [OutputDistributor.onOutputStarted] with a test output */
    private fun OutputDistributor<FakeOutput>.startWith(pendingOutput: PendingOutput) {
        this.onOutputStarted(
            pendingOutput.cameraFrameNumber,
            pendingOutput.cameraTimestamp,
            pendingOutput.outputNumber,
            pendingOutput
        )
    }

    /** Utility class for testing if an output was finalized (closed) or not */
    private class FakeOutput(
        val outputNumber: Long,
    ) {
        private val _finalized = atomic(false)
        val finalized: Boolean
            get() = _finalized.value

        fun finalize() {
            // Check that this is only ever finalized once
            assertThat(_finalized.compareAndSet(expect = false, update = true)).isTrue()
        }
    }
}
