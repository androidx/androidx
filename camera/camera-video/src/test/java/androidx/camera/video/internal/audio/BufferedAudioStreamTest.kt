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

package androidx.camera.video.internal.audio

import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.Semaphore
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class BufferedAudioStreamTest {

    companion object {
        private const val COMMON_TIMEOUT_MS = 1000L
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SOURCE_BYTE_BUFFER_CAPACITY = 16
        private const val SOURCE_TIMESTAMP_OFFSET = 1L
        private const val DESTINATION_BYTE_BUFFER_CAPACITY = 16
    }

    private lateinit var audioSettings: AudioSettings
    private lateinit var byteBuffer: ByteBuffer
    private lateinit var baseAudioStream: FakeAudioStream
    private lateinit var bufferedAudioStream: BufferedAudioStream
    private lateinit var audioStreamCallback: FakeAudioStreamCallback

    @Before
    fun setUp() {
        audioSettings =
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(SAMPLE_RATE)
                .setEncodeSampleRate(SAMPLE_RATE)
                .setChannelCount(CHANNEL_COUNT)
                .setAudioFormat(AUDIO_FORMAT)
                .build()
        baseAudioStream = FakeAudioStream(createAudioDataProvider())
        bufferedAudioStream = BufferedAudioStream(baseAudioStream, audioSettings)
        audioStreamCallback = FakeAudioStreamCallback()
        bufferedAudioStream.setCallback(audioStreamCallback, CameraXExecutors.ioExecutor())
        byteBuffer = ByteBuffer.allocate(DESTINATION_BYTE_BUFFER_CAPACITY)
    }

    @After
    fun tearDown() {
        if (this::bufferedAudioStream.isInitialized) {
            bufferedAudioStream.release()
        }
    }

    @Test
    fun readBeforeStart_throwException() {
        assertThrows(IllegalStateException::class.java) { bufferedAudioStream.read(byteBuffer) }
    }

    @Test
    fun readAfterStop_throwException() {
        bufferedAudioStream.start()
        bufferedAudioStream.stop()
        assertThrows(IllegalStateException::class.java) { bufferedAudioStream.read(byteBuffer) }
    }

    @Test
    fun startAfterReleased_throwException() {
        bufferedAudioStream.release()
        assertThrows(IllegalStateException::class.java) { bufferedAudioStream.start() }
    }

    @Test
    fun setCallbackAfterStarted_throwException() {
        bufferedAudioStream.start()
        assertThrows(IllegalStateException::class.java) {
            bufferedAudioStream.setCallback(audioStreamCallback, CameraXExecutors.ioExecutor())
        }
    }

    @Test
    fun setCallbackAfterReleased_throwException() {
        bufferedAudioStream.release()
        assertThrows(IllegalStateException::class.java) {
            bufferedAudioStream.setCallback(audioStreamCallback, CameraXExecutors.ioExecutor())
        }
    }

    @Test
    fun canReadAudioStream() {
        // Act.
        bufferedAudioStream.start()

        // Assert.
        bufferedAudioStream.verifyMultipleReads(10, byteBuffer)

        // Clean up.
        bufferedAudioStream.stop()
    }

    @Test
    fun canReadAudioStreamWhenDestinationBufferSizeIsSmaller() {
        // Act.
        bufferedAudioStream.start()

        // Assert.
        val destinationByteBuffer = ByteBuffer.allocate(SOURCE_BYTE_BUFFER_CAPACITY - 5)
        bufferedAudioStream.verifyMultipleReads(10, destinationByteBuffer)

        // Clean up.
        bufferedAudioStream.stop()
    }

    @Test
    fun canReadAudioStreamWhenDestinationBufferSizeIsLarger() {
        // Act.
        bufferedAudioStream.start()

        // Assert.
        val destinationByteBuffer = ByteBuffer.allocate(SOURCE_BYTE_BUFFER_CAPACITY + 5)
        bufferedAudioStream.verifyMultipleReads(10, destinationByteBuffer)

        // Clean up.
        bufferedAudioStream.stop()
    }

    @Test
    fun canRestartAudioStream() {
        repeat(2) {
            // Act.
            bufferedAudioStream.start()

            // Assert.
            bufferedAudioStream.verifyMultipleReads(3, byteBuffer)

            // Act.
            bufferedAudioStream.stop()

            // Assert.
            assertThrows(IllegalStateException::class.java) {
                byteBuffer.clear()
                bufferedAudioStream.read(byteBuffer)
            }
        }
    }

    @Test
    fun canReceiveOnSilenceStateChangedAfterStarted() {
        // Act.
        bufferedAudioStream.start()

        // Assert: Initial isSilenced of FakeAudioStream is always false.
        audioStreamCallback.verifyOnSilenceStateChangedCall(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.first()).isFalse()
        }

        // Clean up.
        bufferedAudioStream.stop()
    }

    @Test
    fun reusesCollectionBuffersAfterBeingConsumed() {
        // Arrange: Only allow the producer to collect the next data after the consumer has read
        // the previous one, so the queue never overflows and every collected buffer is returned
        // to the pool after being read.
        val readPermits = Semaphore(1)
        bufferedAudioStream.release()
        baseAudioStream =
            FakeAudioStream(createAudioDataProvider(), onRead = { readPermits.acquire() })
        bufferedAudioStream = BufferedAudioStream(baseAudioStream, audioSettings)
        bufferedAudioStream.start()

        // Act: Each read consumes exactly one collected AudioData.
        val readCount = 30
        val firstBytes = mutableListOf<Byte>()
        try {
            repeat(readCount) {
                byteBuffer.clear()
                val packetInfo = bufferedAudioStream.read(byteBuffer)
                assertThat(packetInfo.sizeInBytes).isEqualTo(SOURCE_BYTE_BUFFER_CAPACITY)
                firstBytes.add(byteBuffer.get(0))
                readPermits.release()
            }
        } finally {
            // Queue stop() behind the blocked collection first, then unblock the producer so the
            // stop task can run. This bounds the producer to at most one more read.
            bufferedAudioStream.stop()
            readPermits.release(Int.MAX_VALUE / 2)
        }

        // Assert: Data is delivered in order without corruption, i.e. a buffer is never reused
        // while its data is still pending.
        assertThat(firstBytes).isEqualTo((0 until readCount).map { it.toByte() })
        // Assert: Collection buffers are reused instead of allocated for every read.
        val readByteBuffers = baseAudioStream.getReadByteBuffers()
        val distinctByteBuffers =
            Collections.newSetFromMap(IdentityHashMap<ByteBuffer, Boolean>()).apply {
                addAll(readByteBuffers)
            }
        assertThat(readByteBuffers.size).isAtLeast(readCount)
        assertThat(distinctByteBuffers.size).isLessThan(readCount)
    }

    @Test
    fun collectionBufferSizeFollowsReadBufferSizeChanges() {
        // Arrange: Slow down the producer to bound the number of collected buffers.
        bufferedAudioStream.release()
        baseAudioStream = FakeAudioStream(createAudioDataProvider(), readDelayMs = 1)
        bufferedAudioStream = BufferedAudioStream(baseAudioStream, audioSettings)

        try {
            repeat(2) {
                bufferedAudioStream.start()

                // Act & Assert: The collection buffer size follows the read buffer size, including
                // when it changes back to a previously requested size.
                listOf(16, 8, 16).forEach { size ->
                    val destination = ByteBuffer.allocate(size)
                    readUntilCollectionBufferCapacityIs(size, destination)

                    // Assert: Once the size has changed, pooled buffers of the old size are no
                    // longer reused.
                    val from = baseAudioStream.getReadByteBuffers().size - 1
                    repeat(5) {
                        destination.clear()
                        bufferedAudioStream.read(destination)
                    }
                    assertThat(
                            baseAudioStream
                                .getReadByteBuffers()
                                .drop(from)
                                .map { it.capacity() }
                                .distinct()
                        )
                        .containsExactly(size)
                }

                bufferedAudioStream.stop()
            }
        } finally {
            bufferedAudioStream.stop()
        }
    }

    private fun readUntilCollectionBufferCapacityIs(capacity: Int, destination: ByteBuffer) {
        val deadlineMs = System.currentTimeMillis() + COMMON_TIMEOUT_MS
        do {
            assertThat(System.currentTimeMillis()).isLessThan(deadlineMs)
            destination.clear()
            bufferedAudioStream.read(destination)
        } while (baseAudioStream.getReadByteBuffers().last().capacity() != capacity)
    }

    private fun AudioStream.verifyMultipleReads(verifyTimes: Int, byteBuffer: ByteBuffer) {
        repeat(verifyTimes) { index ->
            // Since the audio data producer and consumer are not on the same thread, waiting for
            // the
            // baseAudioStream to be read to ensure that the BufferAudioStream has at least one
            // AudioData that can be read.
            baseAudioStream.verifyReadCall(CallTimesAtLeast(index + 2), COMMON_TIMEOUT_MS)

            // Assert.
            byteBuffer.clear()
            this.readAndVerify(byteBuffer)
        }
    }

    private fun AudioStream.readAndVerify(byteBuffer: ByteBuffer) {
        val packetInfo = this.read(byteBuffer)

        // Assert.
        assertThat(packetInfo.sizeInBytes).isGreaterThan(0)
        assertThat(packetInfo.timestampNs).isGreaterThan(0)
    }

    private fun createAudioDataProvider(): (Int) -> FakeAudioStream.AudioData = { index ->
        val byteBuffer = ByteBuffer.allocate(SOURCE_BYTE_BUFFER_CAPACITY).put(0, index.toByte())
        val timestampNs = (index + SOURCE_TIMESTAMP_OFFSET)
        FakeAudioStream.AudioData(byteBuffer, timestampNs)
    }
}
