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
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_UNKNOWN
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

private const val INVALID_FILE_PATH = "/invalid/file/path"
private val TEST_OUTPUT_OPTION = FileOutputOptions.Builder(File(INVALID_FILE_PATH)).build()
private val TEST_RECORDING_STATE =
    RecordingStats.of(0, 0, AudioStats.of(AudioStats.AUDIO_STATE_ACTIVE, null, 0.0))
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

        assertThat(event).isInstanceOf(VideoRecordEvent.Start::class.java)
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

        assertThat(event).isInstanceOf(VideoRecordEvent.Finalize::class.java)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
        assertThat(event.outputResults).isEqualTo(TEST_OUTPUT_RESULT)
        assertThat(event.hasError()).isFalse()
        assertThat(event.error).isEqualTo(ERROR_NONE)
        assertThat(event.cause).isNull()
    }

    @Test
    fun canCreateFinalizeWithError() {
        val error = ERROR_UNKNOWN
        val cause = RuntimeException()
        val event = VideoRecordEvent.finalizeWithError(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE,
            TEST_OUTPUT_RESULT,
            error,
            cause
        )

        assertThat(event).isInstanceOf(VideoRecordEvent.Finalize::class.java)
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
                ERROR_NONE,
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

        assertThat(event).isInstanceOf(VideoRecordEvent.Status::class.java)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }

    @Test
    fun canCreatePause() {
        val event = VideoRecordEvent.pause(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event).isInstanceOf(VideoRecordEvent.Pause::class.java)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }

    @Test
    fun canCreateResume() {
        val event = VideoRecordEvent.resume(
            TEST_OUTPUT_OPTION,
            TEST_RECORDING_STATE
        )

        assertThat(event).isInstanceOf(VideoRecordEvent.Resume::class.java)
        assertThat(event.outputOptions).isEqualTo(TEST_OUTPUT_OPTION)
        assertThat(event.recordingStats).isEqualTo(TEST_RECORDING_STATE)
    }
}
