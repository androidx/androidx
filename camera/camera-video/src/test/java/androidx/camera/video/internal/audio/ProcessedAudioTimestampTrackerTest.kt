/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.video.internal.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class ProcessedAudioTimestampTrackerTest {

    companion object {
        private const val SAMPLE_RATE = 48000
        // 10 ms packets at 48 kHz.
        private const val FRAMES_PER_PACKET = 480
        // Capture timestamps start at a non-zero base and advance slightly faster than the audio
        // duration, so a timestamp only matches when anchored to the right input packet.
        private const val BASE_TIMESTAMP_US = 1_000_000L
        private const val PACKET_INTERVAL_US = 10_013L
    }

    @Test
    fun hasTrackedInput_reflectsQueuedInputAndReset() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)
        assertThat(tracker.hasTrackedInput()).isFalse()

        tracker.onInputQueued(BASE_TIMESTAMP_US, /* frameCount= */ 0)
        assertThat(tracker.hasTrackedInput()).isFalse()

        tracker.onInputQueued(BASE_TIMESTAMP_US, FRAMES_PER_PACKET)
        assertThat(tracker.hasTrackedInput()).isTrue()

        tracker.reset()
        assertThat(tracker.hasTrackedInput()).isFalse()
    }

    @Test
    fun takeOutput_withoutTrackedInput_throws() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)

        assertThrows(IllegalStateException::class.java) {
            tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET)
        }
    }

    @Test
    fun constructor_withInvalidArguments_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ProcessedAudioTimestampTracker(0, SAMPLE_RATE)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProcessedAudioTimestampTracker(SAMPLE_RATE, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE, /* capacity= */ 0)
        }
    }

    @Test
    fun synchronousOutput_inSlices_mapsToCaptureTime() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)

        for (packet in 0 until 3) {
            tracker.onInputQueued(packetTimestampUs(packet), FRAMES_PER_PACKET)
            // Take the packet's output in three slices of 160 frames (3.333 ms each).
            for (slice in 0 until 3) {
                val offsetFrames = slice * 160
                assertThat(tracker.takeOutputPresentationTimeUs(160))
                    .isEqualTo(packetTimestampUs(packet) + offsetFrames * 1_000_000L / SAMPLE_RATE)
            }
        }
    }

    @Test
    fun bufferedOutput_mapsToCaptureTimeOfEarlierPackets() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)
        // A lookahead processor emits nothing until the third input packet is queued.
        tracker.onInputQueued(packetTimestampUs(0), FRAMES_PER_PACKET)
        tracker.onInputQueued(packetTimestampUs(1), FRAMES_PER_PACKET)
        tracker.onInputQueued(packetTimestampUs(2), FRAMES_PER_PACKET)

        assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
            .isEqualTo(packetTimestampUs(0))
        assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
            .isEqualTo(packetTimestampUs(1))
    }

    @Test
    fun blockOutputMisalignedWithPackets_isMonotonicAndMapsToCaptureTime() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)
        tracker.onInputQueued(packetTimestampUs(0), FRAMES_PER_PACKET)
        tracker.onInputQueued(packetTimestampUs(1), FRAMES_PER_PACKET)

        // A 720-frame block spans packet 0 and the first half of packet 1, taken in 240-frame
        // slices.
        assertThat(tracker.takeOutputPresentationTimeUs(240)).isEqualTo(packetTimestampUs(0))
        assertThat(tracker.takeOutputPresentationTimeUs(240))
            .isEqualTo(packetTimestampUs(0) + 5_000L)
        assertThat(tracker.takeOutputPresentationTimeUs(240)).isEqualTo(packetTimestampUs(1))
        tracker.onInputQueued(packetTimestampUs(2), FRAMES_PER_PACKET)
        assertThat(tracker.takeOutputPresentationTimeUs(240))
            .isEqualTo(packetTimestampUs(1) + 5_000L)
        assertThat(tracker.takeOutputPresentationTimeUs(240)).isEqualTo(packetTimestampUs(2))
    }

    @Test
    fun upsampledOutput_hasDistinctIncreasingTimestampsAtOutputRate() {
        // 16 kHz input upsampled to 48 kHz output: 160 input frames become 480 output frames.
        val tracker = ProcessedAudioTimestampTracker(16000, 48000)
        tracker.onInputQueued(packetTimestampUs(0), 160)
        tracker.onInputQueued(packetTimestampUs(1), 160)

        var previousTimestampUs = Long.MIN_VALUE
        for (outputFrame in 0 until 960) {
            val timestampUs = tracker.takeOutputPresentationTimeUs(1)
            val packet = outputFrame / 480
            val expectedUs = packetTimestampUs(packet) + (outputFrame % 480) * 1_000_000L / 48000
            assertThat(timestampUs).isEqualTo(expectedUs)
            assertThat(timestampUs).isGreaterThan(previousTimestampUs)
            previousTimestampUs = timestampUs
        }
    }

    @Test
    fun downsampledOutput_mapsToCaptureTime() {
        // 48 kHz input downsampled to 16 kHz output: 480 input frames become 160 output frames.
        val tracker = ProcessedAudioTimestampTracker(48000, 16000)
        tracker.onInputQueued(packetTimestampUs(0), 480)
        tracker.onInputQueued(packetTimestampUs(1), 480)

        assertThat(tracker.takeOutputPresentationTimeUs(80)).isEqualTo(packetTimestampUs(0))
        assertThat(tracker.takeOutputPresentationTimeUs(80))
            .isEqualTo(packetTimestampUs(0) + 5_000L)
        assertThat(tracker.takeOutputPresentationTimeUs(80)).isEqualTo(packetTimestampUs(1))
    }

    @Test
    fun nonIntegerPacketDuration_mapsToCaptureTime() {
        // 1024-frame packets at 44.1 kHz last 23219.95 us, which isn't a whole number.
        val sampleRate = 44100
        val tracker = ProcessedAudioTimestampTracker(sampleRate, sampleRate)
        tracker.onInputQueued(1_000_000L, 1024)
        tracker.onInputQueued(1_023_300L, 1024)

        assertThat(tracker.takeOutputPresentationTimeUs(1024)).isEqualTo(1_000_000L)
        // Packet 1 starts at stream position floor(1024 * 1e6 / 44100) = 23219 us.
        assertThat(tracker.takeOutputPresentationTimeUs(1)).isEqualTo(1_023_300L)
    }

    @Test
    fun outputLaggingBeyondCapacity_extrapolatesMonotonicallyAndRecovers() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE, /* capacity= */ 4)
        // Queue 6 packets without taking output, so packets 0 and 1 are discarded and packet 1
        // becomes the anchor for the output frames of packets 0 and 1.
        for (packet in 0 until 6) {
            tracker.onInputQueued(packetTimestampUs(packet), FRAMES_PER_PACKET)
        }

        // Frames of discarded packets are extrapolated backwards from packet 1.
        assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
            .isEqualTo(packetTimestampUs(1) - 10_000L)
        assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
            .isEqualTo(packetTimestampUs(1))
        // Tracked packets map exactly again.
        for (packet in 2 until 6) {
            assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
                .isEqualTo(packetTimestampUs(packet))
        }
    }

    @Test
    fun reset_restartsMappingFromNewInput() {
        val tracker = ProcessedAudioTimestampTracker(SAMPLE_RATE, SAMPLE_RATE)
        tracker.onInputQueued(packetTimestampUs(0), FRAMES_PER_PACKET)
        tracker.onInputQueued(packetTimestampUs(1), FRAMES_PER_PACKET)
        tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET / 2)

        tracker.reset()
        tracker.onInputQueued(packetTimestampUs(5), FRAMES_PER_PACKET)

        assertThat(tracker.takeOutputPresentationTimeUs(FRAMES_PER_PACKET))
            .isEqualTo(packetTimestampUs(5))
    }

    private fun packetTimestampUs(packet: Int): Long =
        BASE_TIMESTAMP_US + packet * PACKET_INTERVAL_US
}
