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

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTimestamp
import android.media.AudioTimestamp.TIMEBASE_MONOTONIC
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.Logger
import androidx.camera.testing.impl.RequiresDevice
import androidx.camera.video.internal.compat.quirk.AudioTimestampFramePositionIncorrectQuirk
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class AudioRecordCompatibilityTest {

    companion object {
        private const val TAG = "AudioRecordCompatibilityTest"
        private const val DEFAULT_READ_TEST_TIMES = 50
        private const val DEFAULT_SAMPLE_RATE = 48000
        private const val DEFAULT_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val DEFAULT_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val DEFAULT_AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private val DEFAULT_BUFFER_SIZE_IN_BYTE = getBufferSizeInByte()

        private fun getBufferSizeInByte(
            sampleRate: Int = DEFAULT_SAMPLE_RATE,
            channelConfig: Int = DEFAULT_CHANNEL_CONFIG,
            audioFormat: Int = DEFAULT_AUDIO_FORMAT,
        ): Int {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            return minBufferSize * 2
        }
    }

    @get:Rule
    var audioPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO
    )

    private val bufferSizeInBytes: Int = DEFAULT_BUFFER_SIZE_IN_BYTE
    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    private var audioRecord: AudioRecord? = null

    @Before
    fun setUp() {
        audioRecord = createAudioRecord()
        assumeNotNull(audioRecord)
    }

    @After
    fun tearDown() {
        audioRecord?.release()
        audioRecord = null
    }

    @RequiresDevice
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun read_withNoNegativeFramePositionIssue_whenRecordingMultipleTimes() {
        assumeFalse(hasAudioTimestampQuirk())

        audioRecord!!.apply {
            assumeTrue(state == AudioRecord.STATE_INITIALIZED)

            repeat(5) {
                Logger.i(TAG, "Starting audio recording, round: $it")

                // Arrange.
                startRecording()
                assumeTrue(recordingState == AudioRecord.RECORDSTATE_RECORDING)

                // Assert.
                readAndVerifyFramePositionMultipleTimes()

                // Act.
                stop()
                assumeTrue(recordingState == AudioRecord.RECORDSTATE_STOPPED)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun readAndVerifyFramePositionMultipleTimes(times: Int = DEFAULT_READ_TEST_TIMES) {
        repeat(times) {
            byteBuffer.clear()
            audioRecord!!.apply {
                val readSizeInBytes = read(byteBuffer, bufferSizeInBytes)
                if (readSizeInBytes > 0) {
                    val audioTimestamp = AudioTimestamp()
                    if (getTimestamp(audioTimestamp, TIMEBASE_MONOTONIC) == AudioRecord.SUCCESS) {
                        assertThat(audioTimestamp.framePosition).isAtLeast(0)
                    }
                }
            }
        }
    }

    private fun createAudioRecord(
        audioSource: Int = DEFAULT_AUDIO_SOURCE,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        channelConfig: Int = DEFAULT_CHANNEL_CONFIG,
        audioFormat: Int = DEFAULT_AUDIO_FORMAT,
        bufferSizeInByte: Int = DEFAULT_BUFFER_SIZE_IN_BYTE
    ): AudioRecord? {
        if (bufferSizeInByte <= 0) {
            return null
        }

        return try {
            AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSizeInByte,
            )
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun hasAudioTimestampQuirk(): Boolean {
        return DeviceQuirks.get(AudioTimestampFramePositionIncorrectQuirk::class.java) != null
    }
}
