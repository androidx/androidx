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

package androidx.xr.scenecore.spatial.core

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.media3.common.C.ENCODING_PCM_16BIT
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.xr.scenecore.runtime.AudioTrackExtensionsWrapper
import androidx.xr.scenecore.runtime.SoundFieldAttributes
import androidx.xr.scenecore.runtime.SpatializerConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundFieldAudioComponentImplTest {
    private val activityController: ActivityController<Activity> =
        Robolectric.buildActivity(Activity::class.java)
    private val activity: Activity = activityController.create().start().get()

    @Test
    fun getAudioOutputProvider_returnsProvider() {
        val mockAudioTrackExtensions = mock<AudioTrackExtensionsWrapper>()
        val attributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)
        val component = SoundFieldAudioComponentImpl(activity, mockAudioTrackExtensions, attributes)

        val provider = component.getAudioOutputProvider()

        assertThat(provider).isNotNull()
    }

    @Test
    fun getAudioOutputProvider_setsAttributesOnTrackBuilder() {
        val mockAudioTrackExtensions = mock<AudioTrackExtensionsWrapper>()
        val attributes = SoundFieldAttributes(SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER)
        val component = SoundFieldAudioComponentImpl(activity, mockAudioTrackExtensions, attributes)
        val config =
            AudioOutputProvider.OutputConfig.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(ENCODING_PCM_16BIT)
                .setBufferSize(16)
                .setSampleRate(48000)
                .build()

        val outputProvider = component.getAudioOutputProvider()
        outputProvider.getAudioOutput(config)

        verify(mockAudioTrackExtensions)
            .setSoundFieldAttributes(any<AudioTrack.Builder>(), eq(attributes))
    }
}
