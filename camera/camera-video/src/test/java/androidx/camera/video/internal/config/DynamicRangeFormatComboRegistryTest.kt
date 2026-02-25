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

package androidx.camera.video.internal.config

import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_AUDIO_OPUS
import android.media.MediaFormat.MIMETYPE_VIDEO_AV1
import android.media.MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaFormat.MIMETYPE_VIDEO_VP8
import android.media.MediaFormat.MIMETYPE_VIDEO_VP9
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_MPEG_4
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class DynamicRangeFormatComboRegistryTest {

    companion object {
        private const val VIDEO_HEVC = MIMETYPE_VIDEO_HEVC
        private const val VIDEO_VP8 = MIMETYPE_VIDEO_VP8
        private const val VIDEO_VP9 = MIMETYPE_VIDEO_VP9
        private const val VIDEO_AV1 = MIMETYPE_VIDEO_AV1
        private const val VIDEO_DOLBY_VISION = MIMETYPE_VIDEO_DOLBY_VISION
        private const val VIDEO_UNKNOWN = "video/unknown-codec"
        private const val AUDIO_AAC = MIMETYPE_AUDIO_AAC
        private const val AUDIO_OPUS = MIMETYPE_AUDIO_OPUS
    }

    @Test
    @Config(maxSdk = 23)
    fun sdrRegistry_onOldSdk_excludesGatedCodecs() {
        val registry = DynamicRangeFormatComboRegistry.getRegistry(SDR)
        assertThat(registry).isNotNull()

        // HEVC is gated at SDK 24. On SDK 23, it should not be present.
        assertThat(registry!!.getCombosForVideo(VIDEO_HEVC)).isEmpty()
    }

    @Test
    @Config(minSdk = 24)
    fun sdrRegistry_onNewSdk_includesGatedCodecs() {
        val registry = DynamicRangeFormatComboRegistry.getRegistry(SDR)
        assertThat(registry).isNotNull()

        // HEVC should be available on SDK 24+
        val hevcCombos = registry!!.getCombosForVideo(VIDEO_HEVC)
        assertThat(hevcCombos).isNotEmpty()
    }

    @Test
    @Config(minSdk = 24)
    fun getRegistry_containsExpectedCodecs_onSupportedSdk() {
        val registry = DynamicRangeFormatComboRegistry.getRegistry(HLG_10_BIT)
        assertThat(registry).isNotNull()

        // HLG supports MP4/HEVC/AAC on these levels
        val mp4Combos = registry!!.getCombos(OUTPUT_FORMAT_MPEG_4, VIDEO_HEVC, AUDIO_AAC)
        assertThat(mp4Combos).isNotEmpty()

        // WebM is not supported for HLG
        val webmCombos = registry.getCombos(OUTPUT_FORMAT_WEBM, VIDEO_VP9, AUDIO_OPUS)
        assertThat(webmCombos).isEmpty()
    }

    @Test
    fun getRegistry_withUnsupportedProfile_returnsNull() {
        val registry = DynamicRangeFormatComboRegistry.getRegistry(DynamicRange.UNSPECIFIED)
        assertThat(registry).isNull()
    }

    @Test
    @Config(minSdk = 34)
    fun getDynamicRangesForVideoMime_av1_returnsHdrAndSdr() {
        // AV1 (SDK 34+) is supported in SDR, HLG, HDR10, and HDR10+
        val ranges = DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(VIDEO_AV1)

        assertThat(ranges).containsExactly(SDR, HLG_10_BIT, HDR10_10_BIT, HDR10_PLUS_10_BIT)
    }

    @Test
    @Config(minSdk = 33)
    fun getDynamicRangesForVideoMime_dolbyVision_returnsSpecificProfiles() {
        // Dolby Vision (SDK 33+) should return both 8-bit and 10-bit profiles
        val ranges =
            DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(VIDEO_DOLBY_VISION)

        assertThat(ranges).containsExactly(SDR, DOLBY_VISION_8_BIT, DOLBY_VISION_10_BIT)
    }

    @Test
    @Config(maxSdk = 23)
    fun getDynamicRangesForVideoMime_hevc_onOldSdk_returnsEmpty() {
        // HEVC is gated at SDK 24. On SDK 21, no registry should claim to support it.
        val ranges = DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(VIDEO_HEVC)

        assertThat(ranges).isEmpty()
    }

    @Test
    fun getDynamicRangesForVideoMime_vp8_returnsSdrOnly() {
        val ranges = DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(VIDEO_VP8)

        assertThat(ranges).containsExactly(SDR)
    }

    @Test
    fun getDynamicRangesForVideoMime_invalidMime_returnsEmpty() {
        val ranges = DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(VIDEO_UNKNOWN)
        assertThat(ranges).isEmpty()
    }
}
