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

package androidx.camera.video.internal

import android.media.CamcorderProfile.QUALITY_1080P
import android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.EncoderProfiles.VideoProfile.HDR_NONE
import android.os.Build
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HDR_UNSPECIFIED_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_8
import androidx.camera.testing.impl.EncoderProfilesUtil
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeAudioProfileProxy
import androidx.camera.testing.impl.EncoderProfilesUtil.createFakeVideoProfileProxy
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class DynamicRangeMatchedEncoderProfilesProviderTest {

    private val defaultProvider = createFakeEncoderProfilesProvider(
        arrayOf(Pair(QUALITY_1080P, PROFILES_1080P_FULL_DYNAMIC_RANGE))
    )

    @Test
    fun hasNoProfile_canNotGetProfiles() {
        val emptyProvider = createFakeEncoderProfilesProvider()
        val sdrProvider = DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, SDR)
        val hlgProvider = DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, HLG_10_BIT)
        val hdr10Provider = DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, HDR10_10_BIT)
        val hdr10PlusProvider =
            DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, HDR10_PLUS_10_BIT)
        val dolbyProvider =
            DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, DOLBY_VISION_10_BIT)
        val hdrUnspecifiedProvider =
            DynamicRangeMatchedEncoderProfilesProvider(emptyProvider, HDR_UNSPECIFIED_10_BIT)

        assertThat(sdrProvider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(hlgProvider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(hdr10Provider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(hdr10PlusProvider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(dolbyProvider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(hdrUnspecifiedProvider.hasProfile(QUALITY_1080P)).isFalse()
        assertThat(sdrProvider.getAll(QUALITY_1080P)).isNull()
        assertThat(hlgProvider.getAll(QUALITY_1080P)).isNull()
        assertThat(hdr10Provider.getAll(QUALITY_1080P)).isNull()
        assertThat(hdr10PlusProvider.getAll(QUALITY_1080P)).isNull()
        assertThat(dolbyProvider.getAll(QUALITY_1080P)).isNull()
        assertThat(hdrUnspecifiedProvider.getAll(QUALITY_1080P)).isNull()
    }

    @Test
    fun sdr_onlyContainsSdrProfile() {
        val provider = DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, SDR)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 1).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_NONE).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_8).isTrue()
    }

    @Test
    fun hlg_onlyContainsHlgProfile() {
        val provider = DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, HLG_10_BIT)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 1).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_HLG).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_10).isTrue()
    }

    @Test
    fun hdr10_onlyContainsHdr10Profile() {
        val provider = DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, HDR10_10_BIT)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 1).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_HDR10).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_10).isTrue()
    }

    @Test
    fun hdr10Plus_onlyContainsHdr10PlusProfile() {
        val provider =
            DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, HDR10_PLUS_10_BIT)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 1).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_HDR10PLUS).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_10).isTrue()
    }

    @Test
    fun dolbyVision_onlyContainsDolbyVisionProfile() {
        val provider =
            DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, DOLBY_VISION_10_BIT)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 1).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_DOLBY_VISION).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_10).isTrue()
    }

    @Test
    fun hdrUnspecified_containsAllHdrProfiles() {
        val provider =
            DynamicRangeMatchedEncoderProfilesProvider(defaultProvider, HDR_UNSPECIFIED_10_BIT)

        assertThat(provider.hasProfile(QUALITY_1080P)).isTrue()
        val videoProfiles = provider.getAll(QUALITY_1080P)!!.videoProfiles
        assertThat(videoProfiles.size == 4).isTrue()
        assertThat(videoProfiles[0].hdrFormat == HDR_HLG).isTrue()
        assertThat(videoProfiles[1].hdrFormat == HDR_HDR10).isTrue()
        assertThat(videoProfiles[2].hdrFormat == HDR_HDR10PLUS).isTrue()
        assertThat(videoProfiles[3].hdrFormat == HDR_DOLBY_VISION).isTrue()
        assertThat(videoProfiles[0].bitDepth == BIT_DEPTH_10).isTrue()
        assertThat(videoProfiles[1].bitDepth == BIT_DEPTH_10).isTrue()
        assertThat(videoProfiles[2].bitDepth == BIT_DEPTH_10).isTrue()
        assertThat(videoProfiles[3].bitDepth == BIT_DEPTH_10).isTrue()
    }

    private fun createFakeEncoderProfilesProvider(
        qualityToProfilesPairs: Array<Pair<Int, EncoderProfilesProxy>> = emptyArray()
    ): EncoderProfilesProvider {
        return FakeEncoderProfilesProvider.Builder().also { builder ->
            for (pair in qualityToProfilesPairs) {
                builder.add(pair.first, pair.second)
            }
        }.build()
    }

    companion object {
        private val VIDEO_PROFILES_1080P_SDR =
            createFakeVideoProfileProxy(RESOLUTION_1080P.width, RESOLUTION_1080P.height)
        private val VIDEO_PROFILES_1080P_HLG =
            VIDEO_PROFILES_1080P_SDR.modifyDynamicRangeInfo(HDR_HLG, BIT_DEPTH_10)
        private val VIDEO_PROFILES_1080P_HDR10 =
            VIDEO_PROFILES_1080P_SDR.modifyDynamicRangeInfo(HDR_HDR10, BIT_DEPTH_10)
        private val VIDEO_PROFILES_1080P_HDR10_PLUS =
            VIDEO_PROFILES_1080P_SDR.modifyDynamicRangeInfo(HDR_HDR10PLUS, BIT_DEPTH_10)
        private val VIDEO_PROFILES_1080P_DOLBY_VISION =
            VIDEO_PROFILES_1080P_SDR.modifyDynamicRangeInfo(HDR_DOLBY_VISION, BIT_DEPTH_10)
        private val PROFILES_1080P_FULL_DYNAMIC_RANGE = ImmutableEncoderProfilesProxy.create(
            EncoderProfilesUtil.DEFAULT_DURATION,
            EncoderProfilesUtil.DEFAULT_OUTPUT_FORMAT,
            listOf(createFakeAudioProfileProxy()),
            listOf(
                VIDEO_PROFILES_1080P_SDR,
                VIDEO_PROFILES_1080P_HLG,
                VIDEO_PROFILES_1080P_HDR10,
                VIDEO_PROFILES_1080P_HDR10_PLUS,
                VIDEO_PROFILES_1080P_DOLBY_VISION
            )
        )

        private fun VideoProfileProxy.modifyDynamicRangeInfo(
            hdrFormat: Int,
            bitDepth: Int
        ): VideoProfileProxy {
            return VideoProfileProxy.create(
                this.codec,
                this.mediaType,
                this.bitrate,
                this.frameRate,
                this.width,
                this.height,
                this.profile,
                bitDepth,
                this.chromaSubsampling,
                hdrFormat
            )
        }
    }
}
