/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.integration.avsync.model

import android.content.Context
import android.media.AudioTrack
import androidx.camera.testing.LabTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class AudioGeneratorDeviceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var audioGenerator: AudioGenerator

    // TODO(b/242820044): Remove the rule after the issue is solved.
    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @Before
    fun setUp() {
        audioGenerator = AudioGenerator()
    }

    @LabTestRule.LabTestOnly
    @Test(expected = IllegalArgumentException::class)
    fun initAudioTrack_throwExceptionWhenFrequencyNegative() = runTest {
        audioGenerator.initAudioTrack(context, -5300, 11.0)
    }

    @LabTestRule.LabTestOnly
    @Test(expected = IllegalArgumentException::class)
    fun initAudioTrack_throwExceptionWhenLengthNegative() = runTest {
        audioGenerator.initAudioTrack(context, 5300, -11.0)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun initAudioTrack_canWorkCorrectly() = runTest {
        initialAudioTrack(5300, 11.0)
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canStartAndStopAudioTrack_withoutExceptionAfterInitialized() = runBlocking {
        // Arrange.
        initialAudioTrack(5300, 11.0)

        // Act. and Verify.
        audioGenerator.start()
        delay(1000)
        assertThat(audioGenerator.audioTrack!!.playState).isEqualTo(AudioTrack.PLAYSTATE_PLAYING)
        assertThat(audioGenerator.audioTrack!!.playbackHeadPosition).isGreaterThan(0)

        audioGenerator.stop()
        assertThat(audioGenerator.audioTrack!!.playState).isEqualTo(AudioTrack.PLAYSTATE_STOPPED)
    }

    private suspend fun initialAudioTrack(frequency: Int, beepLengthInSec: Double) {
        val isInitialized = audioGenerator.initAudioTrack(context, frequency, beepLengthInSec)
        assertThat(isInitialized).isTrue()
        assertThat(audioGenerator.audioTrack!!.state).isEqualTo(AudioTrack.STATE_INITIALIZED)
        assertThat(audioGenerator.audioTrack!!.playbackHeadPosition).isEqualTo(0)
    }
}