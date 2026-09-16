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

package androidx.camera.core

import androidx.camera.core.impl.DynamicRanges
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class DynamicRangeTest {

    @Test
    fun canCreateUnspecifiedDynamicRange() {
        val dynamicRange =
            DynamicRange(DynamicRange.ENCODING_HDR_UNSPECIFIED, DynamicRange.BIT_DEPTH_UNSPECIFIED)
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_HDR_UNSPECIFIED)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_UNSPECIFIED)
    }

    @Test
    fun sdrDynamicRange_is8Bit() {
        assertThat(DynamicRange.SDR.encoding).isEqualTo(DynamicRange.ENCODING_SDR)
        assertThat(DynamicRange.SDR.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_8_BIT)
    }

    @Test
    fun hlg10BitSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.HLG_10_BIT_SMPTE_2094_50
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_HLG_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_10_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isTrue()
        assertThat(dynamicRange.toString()).contains("HLG_SMPTE_2094_50")
    }

    @Test
    fun sdrSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.SDR_SMPTE_2094_50
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_SDR_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_8_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isFalse()
        assertThat(dynamicRange.toString()).contains("SDR_SMPTE_2094_50")
    }

    @Test
    fun hdr1010BitSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.HDR10_10_BIT_SMPTE_2094_50
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_HDR10_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_10_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isTrue()
        assertThat(dynamicRange.toString()).contains("HDR10_SMPTE_2094_50")
    }

    @Test
    fun hdr10Plus10BitSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.HDR10_PLUS_10_BIT_SMPTE_2094_50
        assertThat(dynamicRange.encoding).isEqualTo(DynamicRange.ENCODING_HDR10_PLUS_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_10_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isTrue()
        assertThat(dynamicRange.toString()).contains("HDR10_PLUS_SMPTE_2094_50")
    }

    @Test
    fun dolbyVision10BitSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.DOLBY_VISION_10_BIT_SMPTE_2094_50
        assertThat(dynamicRange.encoding)
            .isEqualTo(DynamicRange.ENCODING_DOLBY_VISION_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_10_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isTrue()
        assertThat(dynamicRange.toString()).contains("DOLBY_VISION_SMPTE_2094_50")
    }

    @Test
    fun dolbyVision8BitSmpte209450DynamicRange_propertiesAreCorrect() {
        val dynamicRange = DynamicRange.DOLBY_VISION_8_BIT_SMPTE_2094_50
        assertThat(dynamicRange.encoding)
            .isEqualTo(DynamicRange.ENCODING_DOLBY_VISION_SMPTE_2094_50)
        assertThat(dynamicRange.bitDepth).isEqualTo(DynamicRange.BIT_DEPTH_8_BIT)
        assertThat(dynamicRange.isFullySpecified).isTrue()
        assertThat(dynamicRange.is10BitHdr).isFalse()
        assertThat(dynamicRange.toString()).contains("DOLBY_VISION_SMPTE_2094_50")
    }

    @Test
    fun hdrUnspecified_cannotResolveToSdrSmpte209450() {
        val fullySpecified = setOf(DynamicRange.SDR_SMPTE_2094_50)
        assertThat(DynamicRanges.canResolve(DynamicRange.HDR_UNSPECIFIED_10_BIT, fullySpecified))
            .isFalse()
    }

    @Test
    fun hdrUnspecified_canResolveToHlg10BitSmpte209450() {
        val fullySpecified = setOf(DynamicRange.HLG_10_BIT_SMPTE_2094_50)
        assertThat(DynamicRanges.canResolve(DynamicRange.HDR_UNSPECIFIED_10_BIT, fullySpecified))
            .isTrue()
    }
}
