/*
 * Copyright 2020 The Android Open Source Project
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

import android.Manifest
import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.AudioUtil
import androidx.camera.video.internal.encoder.FakeInputBuffer
import androidx.camera.video.internal.encoder.noInvocation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import java.util.concurrent.Callable

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class AudioSourceTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @get:Rule
    var mAudioPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO
    )
    private lateinit var audioSource: AudioSource
    private lateinit var fakeBufferProvider: FakeBufferProvider
    private val bufferFactoryInvocations = mock(Callable::class.java)

    @Before
    fun setUp() {
        assumeTrue(AudioSource.isSettingsSupported(SAMPLE_RATE, CHANNEL_COUNT, AUDIO_FORMAT))
        assumeTrue(AudioUtil.canStartAudioRecord(AUDIO_SOURCE))

        fakeBufferProvider = FakeBufferProvider {
            bufferFactoryInvocations.call()
            FakeInputBuffer()
        }
        fakeBufferProvider.setActive(true)

        audioSource = AudioSource.Builder()
            .setExecutor(CameraXExecutors.ioExecutor())
            .setAudioSource(AUDIO_SOURCE)
            .setSampleRate(SAMPLE_RATE)
            .setChannelCount(CHANNEL_COUNT)
            .setAudioFormat(AUDIO_FORMAT)
            .setBufferProvider(fakeBufferProvider)
            .build()
    }

    @After
    fun tearDown() {
        if (this::audioSource.isInitialized) {
            audioSource.release()
        }
    }

    @Test
    fun canRestartAudioSource() {
        for (i in 0..2) {
            // Act.
            audioSource.start()

            // Assert.
            // It should continuously send audio data by invoking BufferProvider#acquireBuffer
            verify(bufferFactoryInvocations, timeout(10000L).atLeast(3)).call()

            // Act.
            audioSource.stop()

            // Assert.
            verify(bufferFactoryInvocations, noInvocation(3000L, 6000L)).call()
        }
    }

    @Test
    fun bufferProviderStateChange_acquireBufferOrNot() {
        // Arrange.
        audioSource.start()

        for (i in 0..2) {
            // Act.
            fakeBufferProvider.setActive(true)

            // Assert.
            // It should continuously send audio data by invoking BufferProvider#acquireBuffer
            verify(bufferFactoryInvocations, timeout(10000L).atLeast(3)).call()

            // Act.
            fakeBufferProvider.setActive(false)

            // Assert.
            verify(bufferFactoryInvocations, noInvocation(3000L, 6000L)).call()
        }
    }
}
