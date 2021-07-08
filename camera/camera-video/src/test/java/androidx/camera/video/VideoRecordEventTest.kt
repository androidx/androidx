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

package androidx.camera.video

import android.net.Uri
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.io.File

private const val INVALID_FILE_PATH = "/invalid/file/path"
private val TEST_OUTPUT_OPTION =
    FileOutputOptions.builder().setFile(File(INVALID_FILE_PATH)).build()
private val TEST_RECORDING_STATE = RecordingStats.of(0, 0, RecordingStats.AUDIO_RECORDING)
private val TEST_OUTPUT_RESULT = OutputResults.of(Uri.EMPTY)

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class VideoRecordEventTest {

    @Test
    fun canCreateStart() {
        val event = VideoRecordEvent.start(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_START)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }

    @Test
    fun canCreateFinalize() {
        val event = VideoRecordEvent.finalize(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE,
            TEST_OUTPUT_RESULT
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_FINALIZE)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
        assertThat(event.outputResults).isEqualTo(TEST_OUTPUT_RESULT)
        assertThat(event.hasError()).isFalse()
        assertThat(event.error).isEqualTo(VideoRecordEvent.ERROR_NONE)
        assertThat(event.cause).isNull()
    }

    @Test
    fun canCreateFinalizeWithError() {
        val error = VideoRecordEvent.ERROR_UNKNOWN
        val cause = RuntimeException()
        val event = VideoRecordEvent.finalizeWithError(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE,
            TEST_OUTPUT_RESULT,
            error,
            cause
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_FINALIZE)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
        assertThat(event.outputResults).isEqualTo(TEST_OUTPUT_RESULT)
        assertThat(event.hasError()).isTrue()
        assertThat(event.error).isEqualTo(error)
        assertThat(event.cause).isEqualTo(cause)
    }

    @Test
    fun createFinalizeWithError_withErrorNone_throwException() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            VideoRecordEvent.finalizeWithError(
                TEST_OUTPUT_OPTION,
                TEST_RECORDING_STATE,
                TEST_OUTPUT_RESULT,
                VideoRecordEvent.ERROR_NONE,
                RuntimeException()
            )
        }
    }

    @Test
    fun canCreateStatus() {
        val event = VideoRecordEvent.status(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_STATUS)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }

    @Test
    fun canCreatePause() {
        val event = VideoRecordEvent.pause(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_PAUSE)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }

    @Test
    fun canCreateResume() {
        val event = VideoRecordEvent.resume(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event.eventType).isEqualTo(VideoRecordEvent.EVENT_TYPE_RESUME)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }
}
