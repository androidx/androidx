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

package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.video.internal.utils.CodecUtil
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaCodecList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class DefaultAudioEncoderFactoryTest {

    @Before
    fun setUp() {
        CodecUtil.reset()
        ShadowMediaCodecList.addCodec(
            MediaCodecInfoBuilder.newBuilder()
                .setName(CODEC_NAME)
                .setIsEncoder(true)
                .setCapabilities(
                    MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                        .setMediaFormat(
                            MediaFormat.createAudioFormat(
                                MIMETYPE_AUDIO_AAC,
                                SAMPLE_RATE,
                                CHANNEL_COUNT,
                            )
                        )
                        .setIsEncoder(true)
                        .setProfileLevels(
                            arrayOf(
                                MediaCodecInfo.CodecProfileLevel().apply {
                                    profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
                                }
                            )
                        )
                        .build()
                )
                .build()
        )
        ShadowMediaCodec.addEncoder(
            MIMETYPE_AUDIO_AAC,
            ShadowMediaCodec.CodecConfig(BUFFER_SIZE, BUFFER_SIZE) { input, output ->
                output.put(input)
            },
        )
    }

    @After
    fun tearDown() {
        ShadowMediaCodec.clearCodecs()
        ShadowMediaCodecList.reset()
        CodecUtil.reset()
    }

    @Test
    fun getEncoderInfo_returnsInfoOfCreatedCodec() {
        val encoderInfo =
            requireNotNull(DefaultAudioEncoderFactory.DEFAULT.getEncoderInfo(MIMETYPE_AUDIO_AAC))

        assertThat(encoderInfo.getName()).isEqualTo(CODEC_NAME)
        assertThat(encoderInfo.mime).isEqualTo(MIMETYPE_AUDIO_AAC)
    }

    @Test
    fun createEncoder_createsEncoderWithSameCodec() {
        val encoder =
            DefaultAudioEncoderFactory.DEFAULT.createEncoder(
                CameraXExecutors.directExecutor(),
                AudioEncoderConfig.builder()
                    .setMimeType(MIMETYPE_AUDIO_AAC)
                    .setProfile(MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    .setInputTimebase(Timebase.UPTIME)
                    .setBitrate(64000)
                    .setCaptureSampleRate(SAMPLE_RATE)
                    .setEncodeSampleRate(SAMPLE_RATE)
                    .setChannelCount(CHANNEL_COUNT)
                    .build(),
            )

        try {
            assertThat(encoder).isInstanceOf(EncoderImpl::class.java)
            assertThat(encoder.encoderInfo.getName()).isEqualTo(CODEC_NAME)
        } finally {
            encoder.release()
        }
    }

    private companion object {
        private const val CODEC_NAME = "fake.aac.encoder"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
        private const val BUFFER_SIZE = 1024
    }
}
