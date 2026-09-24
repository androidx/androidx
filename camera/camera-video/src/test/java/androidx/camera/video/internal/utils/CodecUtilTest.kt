/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.video.internal.utils

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_OPUS
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.createAudioFormat
import android.media.MediaFormat.createVideoFormat
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.ShadowMediaCodecList

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class CodecUtilTest {

    @Before
    fun setUp() {
        resetCodecState()
    }

    @After
    fun tearDown() {
        resetCodecState()
    }

    private fun resetCodecState() {
        CodecUtil.reset()
        ShadowMediaCodecList.reset()
    }

    @Config(minSdk = 29)
    @Test
    fun getVideoEncoderMimeTypes_returnsOnlyVideoEncoders() {
        // Arrange.
        addVideoCodecInfo(mimeType = MIMETYPE_VIDEO_AVC, isEncoder = true)
        addVideoCodecInfo(mimeType = MIMETYPE_VIDEO_HEVC, isEncoder = false)
        addAudioCodecInfo(mimeType = MIMETYPE_AUDIO_AAC, isEncoder = true)
        addAudioCodecInfo(mimeType = MIMETYPE_AUDIO_OPUS, isEncoder = false)

        // Act.
        val videoTypes = CodecUtil.getVideoEncoderMimeTypes()

        // Assert.
        assertThat(videoTypes).containsExactly(MIMETYPE_VIDEO_AVC)
    }

    @Config(minSdk = 29)
    @Test
    fun getAudioEncoderMimeTypes_returnsOnlyAudioEncoders() {
        // Arrange.
        addVideoCodecInfo(mimeType = MIMETYPE_VIDEO_AVC, isEncoder = true)
        addVideoCodecInfo(mimeType = MIMETYPE_VIDEO_HEVC, isEncoder = false)
        addAudioCodecInfo(mimeType = MIMETYPE_AUDIO_AAC, isEncoder = true)
        addAudioCodecInfo(mimeType = MIMETYPE_AUDIO_OPUS, isEncoder = false)

        // Act.
        val videoTypes = CodecUtil.getAudioEncoderMimeTypes()

        // Assert.
        assertThat(videoTypes).containsExactly(MIMETYPE_AUDIO_AAC)
    }

    @Config(minSdk = 29)
    @Test
    fun getVideoEncoderMimeTypes_shouldBeLowerCase() {
        // Arrange.
        addVideoCodecInfo(mimeType = "video/AVC", isEncoder = true)

        // Act.
        val videoTypes = CodecUtil.getVideoEncoderMimeTypes()

        // Assert.
        assertThat(videoTypes).containsExactly(MIMETYPE_VIDEO_AVC.lowercase())
    }

    @Config(minSdk = 29)
    @Test
    fun getAudioEncoderMimeTypes_shouldBeLowerCase() {
        // Arrange.
        addAudioCodecInfo(mimeType = "audio/MP4A-LATM", isEncoder = true)

        // Act.
        val audioTypes = CodecUtil.getAudioEncoderMimeTypes()

        // Assert.
        assertThat(audioTypes).containsExactly(MIMETYPE_AUDIO_AAC.lowercase())
    }

    @Config(minSdk = 29)
    @Test
    fun isHardwareAccelerated_api29Plus_delegatesToMediaCodecInfo() {
        val hwCodec = createCodecInfo(name = "c2.hw.encoder", isHardwareAccelerated = true)
        val swCodec = createCodecInfo(name = "c2.hw.encoder", isHardwareAccelerated = false)

        assertThat(CodecUtil.isHardwareAccelerated(hwCodec, MIMETYPE_VIDEO_AVC)).isTrue()
        assertThat(CodecUtil.isHardwareAccelerated(swCodec, MIMETYPE_VIDEO_AVC)).isFalse()
    }

    @Config(maxSdk = 28)
    @Test
    fun isHardwareAccelerated_preApi29_audioIsAlwaysFalse() {
        val codec = createCodecInfo(name = "OMX.qcom.audio.encoder")

        assertThat(CodecUtil.isHardwareAccelerated(codec, MIMETYPE_AUDIO_AAC)).isFalse()
        assertThat(CodecUtil.isHardwareAccelerated(codec, "Audio/AAC")).isFalse()
        assertThat(CodecUtil.isHardwareAccelerated(codec, "AUDIO/MP4A-LATM")).isFalse()
    }

    @Config(maxSdk = 28)
    @Test
    fun isHardwareAccelerated_preApi29_arcCodecReturnsTrue() {
        val codec = createCodecInfo(name = "arc.video.encoder")

        assertThat(CodecUtil.isHardwareAccelerated(codec, MIMETYPE_VIDEO_AVC)).isTrue()
    }

    @Config(maxSdk = 28)
    @Test
    fun isHardwareAccelerated_preApi29_softwareCodecsReturnFalse() {
        val softwareNames =
            listOf(
                "OMX.google.h264.encoder",
                "OMX.ffmpeg.video.encoder",
                "OMX.SEC.avc.sw.dec",
                "OMX.sec.avc.sw.codec",
                "omx.qcom.video.decoder.hevcswvdec",
                "c2.android.av1.encoder",
                "c2.google.av1.encoder",
                "other.software.encoder",
            )
        for (name in softwareNames) {
            val codec = createCodecInfo(name = name)
            assertThat(CodecUtil.isHardwareAccelerated(codec, MIMETYPE_VIDEO_AVC)).isFalse()
        }
    }

    @Config(maxSdk = 28)
    @Test
    fun isHardwareAccelerated_preApi29_hardwareCodecsReturnTrue() {
        val hardwareNames =
            listOf(
                "OMX.qcom.video.encoder.avc",
                "c2.qti.avc.encoder",
                "OMX.Exynos.avc.encoder",
            )
        for (name in hardwareNames) {
            val codec = createCodecInfo(name = name)
            assertThat(CodecUtil.isHardwareAccelerated(codec, MIMETYPE_VIDEO_AVC)).isTrue()
        }
    }

    private fun createCodecInfo(
        name: String,
        isEncoder: Boolean = true,
        isHardwareAccelerated: Boolean = false,
        isSoftwareOnly: Boolean = false,
    ): MediaCodecInfo {
        val builder = MediaCodecInfoBuilder.newBuilder().setName(name).setIsEncoder(isEncoder)
        if (Build.VERSION.SDK_INT >= 29) {
            builder.setIsHardwareAccelerated(isHardwareAccelerated)
            builder.setIsSoftwareOnly(isSoftwareOnly)
        }
        return builder.build()
    }

    @RequiresApi(29) // ShadowMediaCodecList#addCodec requires API 29
    private fun addVideoCodecInfo(
        mimeType: String = MIMETYPE_VIDEO_AVC,
        name: String = "FakeVideoEncoder",
        isEncoder: Boolean = true,
        width: Int = 1920,
        height: Int = 1080,
        colorFormats: IntArray = intArrayOf(COLOR_FormatSurface),
    ) {
        val format = createVideoFormat(mimeType, width, height)

        val caps =
            MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                .setMediaFormat(format)
                .setIsEncoder(isEncoder)
                .setColorFormats(colorFormats)
                .build()

        val codecInfo =
            MediaCodecInfoBuilder.newBuilder()
                .setName(name)
                .setIsEncoder(isEncoder)
                .setCapabilities(caps)
                .build()

        ShadowMediaCodecList.addCodec(codecInfo)
    }

    @RequiresApi(29) // ShadowMediaCodecList#addCodec requires API 29
    private fun addAudioCodecInfo(
        mimeType: String = MIMETYPE_AUDIO_AAC,
        name: String = "FakeAudioEncoder",
        isEncoder: Boolean = true,
        sampleRate: Int = 44100,
        channelCount: Int = 1,
    ) {
        val format = createAudioFormat(mimeType, sampleRate, channelCount)

        val caps =
            MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                .setMediaFormat(format)
                .setIsEncoder(isEncoder)
                .build()

        val codecInfo =
            MediaCodecInfoBuilder.newBuilder()
                .setName(name)
                .setIsEncoder(isEncoder)
                .setCapabilities(caps)
                .build()

        ShadowMediaCodecList.addCodec(codecInfo)
    }
}
