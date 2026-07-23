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

package androidx.camera.video.internal

import android.media.MediaCodec.BufferInfo
import androidx.camera.video.internal.encoder.FakeEncodedData
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class PauseResumeDataProcessorTest {

    private val processor = PauseResumeDataProcessor()
    private val fakeByteBuffer: ByteBuffer = ByteBuffer.allocate(16)

    @Test
    fun initialTimestamps_notAdjusted_whenNoPause() {
        val videoData = createEncodedData(1000L)
        val audioData = createEncodedData(1000L)

        assertThat(processor.processEncodedData(videoData, true)).isTrue()
        assertThat(processor.processEncodedData(audioData, false)).isTrue()

        assertThat(videoData.presentationTimeUs).isEqualTo(1000L)
        assertThat(audioData.presentationTimeUs).isEqualTo(1000L)
    }

    @Test
    fun dropBuffer_whenInPauseRange() {
        processor.pause(2000L)

        val videoData = createEncodedData(2500L)
        val audioData = createEncodedData(2500L)

        assertThat(processor.processEncodedData(videoData, true)).isFalse()
        assertThat(processor.processEncodedData(audioData, false)).isFalse()
    }

    @Test
    fun adjustTimestamps_afterResume() {
        processor.pause(2000L)
        processor.resume(5000L) // Paused duration = 3000L

        val videoData = createEncodedData(6000L)
        val audioData = createEncodedData(6500L)

        assertThat(processor.processEncodedData(videoData, true)).isTrue()
        assertThat(processor.processEncodedData(audioData, false)).isTrue()

        assertThat(videoData.presentationTimeUs).isEqualTo(3000L) // 6000 - 3000
        assertThat(audioData.presentationTimeUs).isEqualTo(3500L) // 6500 - 3000
    }

    @Test
    fun dropBuffer_whenNotMonotonic() {
        val firstVideo = createEncodedData(2000L)
        assertThat(processor.processEncodedData(firstVideo, true)).isTrue()
        assertThat(firstVideo.presentationTimeUs).isEqualTo(2000L)

        processor.pause(3000L)
        processor.resume(6000L) // Paused duration = 3000L

        // An out-of-order buffer whose adjusted timestamp would be <= last video timestamp (2000L)
        val outOfOrderVideo = createEncodedData(4500L) // Adjusted = 4500 - 3000 = 1500 <= 2000
        assertThat(processor.processEncodedData(outOfOrderVideo, true)).isFalse()

        // A valid subsequent buffer
        val validVideo = createEncodedData(6100L) // Adjusted = 3100 > 2000
        assertThat(processor.processEncodedData(validVideo, true)).isTrue()
        assertThat(validVideo.presentationTimeUs).isEqualTo(3100L)
    }

    @Test
    fun multiplePauseResumeRanges_accumulateTotalPausedDuration() {
        processor.pause(1000L)
        processor.resume(3000L) // Duration += 2000L

        val firstData = createEncodedData(4000L)
        assertThat(processor.processEncodedData(firstData, true)).isTrue()
        assertThat(firstData.presentationTimeUs).isEqualTo(2000L) // 4000 - 2000

        processor.pause(5000L)
        processor.resume(10000L) // Duration += 5000L (Total = 7000L)

        val secondData = createEncodedData(12000L)
        assertThat(processor.processEncodedData(secondData, true)).isTrue()
        assertThat(secondData.presentationTimeUs).isEqualTo(5000L) // 12000 - 7000
    }

    @Test
    fun outOfOrderSharedStreams_doNotPrematurelyConsumePauseRange() {
        processor.pause(1000L)
        processor.resume(3000L) // Paused duration = 2000L

        // Video buffer after the pause passes the upper bound (3000L)
        val videoData = createEncodedData(4000L)
        assertThat(processor.processEncodedData(videoData, true)).isTrue()
        assertThat(videoData.presentationTimeUs).isEqualTo(2000L) // 4000 - 2000

        // Audio stream was slightly delayed and now emits a buffer within the pause range
        val delayedAudioData = createEncodedData(2000L)
        // Must still be filtered out even though video has already passed 3000L!
        assertThat(processor.processEncodedData(delayedAudioData, false)).isFalse()

        // Subsequent valid audio buffer
        val validAudioData = createEncodedData(4500L)
        assertThat(processor.processEncodedData(validAudioData, false)).isTrue()
        assertThat(validAudioData.presentationTimeUs).isEqualTo(2500L) // 4500 - 2000
    }

    private fun createEncodedData(presentationTimeUs: Long): FakeEncodedData {
        val bufferInfo =
            BufferInfo().apply {
                this.presentationTimeUs = presentationTimeUs
                size = 16
                offset = 0
                flags = 0
            }
        return FakeEncodedData(fakeByteBuffer, bufferInfo)
    }
}
