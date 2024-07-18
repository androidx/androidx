/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.os.Build
import androidx.camera.camera2.pipe.CameraTimestamp
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.media.OutputDistributor.OutputCompleteListener
import com.google.common.truth.Truth.assertThat
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
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)

        // When an output becomes available, ensure it is not immediately finalized.
        assertThat(fakeOutput1.finalized).isFalse()
    }

    @Test
    fun onOutputAvailableEvictsAndFinalizesPreviousOutputs() {
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)
        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3)
        outputDistributor.onOutputAvailable(fakeOutput4.outputNumber, fakeOutput4)

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
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)
        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3)
        outputDistributor.onOutputAvailable(fakeOutput4.outputNumber, fakeOutput4)
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1) // Out of order

        // FIFO Order for outputs, regardless of the output number.
        // Note: Outputs are provided as [2, 3, 4, *1*]
        assertThat(fakeOutput2.finalized).isTrue()
        assertThat(fakeOutput3.finalized).isFalse()
        assertThat(fakeOutput4.finalized).isFalse()
        assertThat(fakeOutput1.finalized).isFalse()
    }

    @Test
    fun onOutputAvailableWithNullEvictsAndFinalizesOutputs() {
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)
        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3)

        outputDistributor.onOutputAvailable(fakeOutput4.outputNumber, null)
        outputDistributor.onOutputAvailable(fakeOutput5.outputNumber, null)
        outputDistributor.onOutputAvailable(fakeOutput6.outputNumber, null)

        // Dropped outputs (null) still evict old outputs.
        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
        assertThat(fakeOutput3.finalized).isTrue()
    }

    @Test
    fun closingOutputDistributorFinalizesCachedOutputs() {
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)

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
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun pendingResultsAreMatchedWithOutputs() {
        // When a a start event occurs and an output is also available, ensure the callback
        // is correctly invoked.
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isEqualTo(fakeOutput1)
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
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, null)

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
    }

    @Test
    fun previousOutputsAreCompletedWhenNewerOutputIsMatched() {
        outputDistributor.startWith(pendingOutput1)
        outputDistributor.startWith(pendingOutput2)
        outputDistributor.startWith(pendingOutput3)
        outputDistributor.startWith(pendingOutput4)

        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3) // Match 3

        assertThat(pendingOutput1.isComplete).isTrue() // #1 is Canceled
        assertThat(pendingOutput2.isComplete).isTrue() // #2 is Canceled
        assertThat(pendingOutput3.isComplete).isTrue()
        assertThat(pendingOutput4.isComplete).isFalse()

        assertThat(pendingOutput1.output).isNull() // #1 is Canceled
        assertThat(pendingOutput2.output).isNull() // #2 is Canceled
        assertThat(pendingOutput3.output).isEqualTo(fakeOutput3)
        assertThat(pendingOutput4.output).isNull() // #4 is still pending
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
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
    }

    @Test
    fun availableOutputsAreNotDistributedToStartedOutputsAfterClose() {
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)
        outputDistributor.close()
        outputDistributor.startWith(pendingOutput1) // Note: Would normally match fakeOutput1
        outputDistributor.startWith(pendingOutput2) // Note: Would normally match fakeOutput2

        // If we have valid outputs, but then receive close, and then receive matching start events
        // ensure the outputs are considered dropped.

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()

        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(fakeOutput2.finalized).isTrue()
    }

    @Test
    fun startedOutputsOutputsAreNotDistributedToAvailableOutputsAfterClose() {
        outputDistributor.startWith(pendingOutput1) // Note: Would normally match fakeOutput1
        outputDistributor.startWith(pendingOutput2) // Note: Would normally match fakeOutput2
        outputDistributor.close()
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)

        // If we have valid start events, but then receive close, and then receive matching outputs,
        // ensure all outputs are still considered dropped.

        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput2.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()

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
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)

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
        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3)

        assertThat(pendingOutput1.isComplete).isTrue() // Cancelled. 1 < 3
        assertThat(pendingOutput2.isComplete).isTrue() // Cancelled. 2 < 3
        assertThat(pendingOutput3.isComplete).isTrue() // Success: 3 = 3
        assertThat(pendingOutput4.isComplete).isFalse() // Ignored 4 > 3

        assertThat(pendingOutput1.output).isNull()
        assertThat(pendingOutput2.output).isNull()
        assertThat(pendingOutput3.output).isEqualTo(fakeOutput3)
    }

    @Test
    fun fullyCompletedOutputAfterOutOfOrderResultDoesNotCancelPendingOutput() {
        outputDistributor.startWith(pendingOutput2) // Normal (first)
        outputDistributor.startWith(pendingOutput1) // Out of order start
        outputDistributor.startWith(pendingOutput3) // Normal (> 2)
        outputDistributor.startWith(pendingOutput4) // Normal (> 3)

        // Normal outputs complete
        outputDistributor.onOutputAvailable(fakeOutput2.outputNumber, fakeOutput2)
        outputDistributor.onOutputAvailable(fakeOutput3.outputNumber, fakeOutput3)
        outputDistributor.onOutputAvailable(fakeOutput4.outputNumber, fakeOutput4)

        // Then the out of order event completes
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)

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
    }

    @Test
    fun previouslyAddedOutputIsClosedAfterFailure() {
        outputDistributor.onOutputAvailable(fakeOutput1.outputNumber, fakeOutput1)
        outputDistributor.onOutputFailure(pendingOutput1.cameraFrameNumber)

        // Output cannot be matched with frameNumber
        assertThat(fakeOutput1.finalized).isFalse()
        // Output can be matched after onOutputStart occurs
        outputDistributor.startWith(pendingOutput1)

        // Output should be finalized, and the pending output should NOT receive it:
        assertThat(fakeOutput1.finalized).isTrue()
        assertThat(pendingOutput1.isComplete).isTrue()
        assertThat(pendingOutput1.output).isNull()
    }

    /**
     * Utility class that implements [OutputCompleteListener] and can be used to observe when an
     * output is complete and the callback is invoked.
     */
    private class PendingOutput(
        val cameraFrameNumber: FrameNumber,
        val cameraTimestamp: CameraTimestamp,
        val outputNumber: Long
    ) : OutputCompleteListener<FakeOutput> {
        private val _complete = atomic(false)
        val isComplete: Boolean
            get() = _complete.value

        var outputSequence: Long? = null
        var output: FakeOutput? = null

        override fun onOutputComplete(
            cameraFrameNumber: FrameNumber,
            cameraTimestamp: CameraTimestamp,
            outputSequence: Long,
            outputNumber: Long,
            output: FakeOutput?
        ) {
            // Assert that this callback has only been invoked once.
            assertThat(_complete.compareAndSet(expect = false, update = true)).isTrue()

            // Assert that the parameters in the invoked callback match the expected values.
            assertThat(cameraFrameNumber).isEqualTo(cameraFrameNumber)
            assertThat(cameraTimestamp).isEqualTo(cameraTimestamp)
            assertThat(outputNumber).isEqualTo(outputNumber)

            // Record the actual output and outputSequence for future checks.
            this.outputSequence = outputSequence
            this.output = output
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
