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

package androidx.camera.video

import android.media.CamcorderProfile.QUALITY_1080P
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.testing.fakes.FakeCameraInfoInternal
import androidx.camera.testing.impl.EncoderProfilesUtil.PROFILES_1080P
import androidx.camera.testing.impl.EncoderProfilesUtil.RESOLUTION_1080P
import androidx.camera.testing.impl.fakes.FakeEncoderProfilesProvider
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
class RecorderVideoCapabilitiesTest {

    private lateinit var fakeCameraInfo: FakeCameraInfoInternal
    private lateinit var profilesResolver: EncoderProfilesResolver

    @Before
    fun setUp() {
        val profilesProvider =
            FakeEncoderProfilesProvider.Builder().add(QUALITY_1080P, PROFILES_1080P).build()

        fakeCameraInfo = FakeCameraInfoInternal().apply { supportedDynamicRanges = setOf(SDR) }

        profilesResolver =
            EncoderProfilesResolver(
                profilesProvider,
                QUALITY_SOURCE_REGULAR,
                fakeCameraInfo.supportedDynamicRanges,
            )
    }

    @Test
    fun isStabilizationSupported_capturesValueFromCameraInfo() {
        // Test True
        fakeCameraInfo.isVideoStabilizationSupported = true
        var capabilities = RecorderVideoCapabilities(profilesResolver, fakeCameraInfo)
        assertThat(capabilities.isStabilizationSupported).isTrue()

        // Test False
        fakeCameraInfo.isVideoStabilizationSupported = false
        capabilities = RecorderVideoCapabilities(profilesResolver, fakeCameraInfo)
        assertThat(capabilities.isStabilizationSupported).isFalse()
    }

    @Test
    fun getSupportedDynamicRanges_returnsResolverData() {
        val capabilities = RecorderVideoCapabilities(profilesResolver, fakeCameraInfo)

        // Verifies the wrapper exposed the range we put in the resolver
        assertThat(capabilities.supportedDynamicRanges).containsExactly(SDR)
    }

    @Test
    fun getSupportedQualities_returnsResolverData() {
        val capabilities = RecorderVideoCapabilities(profilesResolver, fakeCameraInfo)

        // Verifies FHD is reported (mapping back to 1080p profile)
        assertThat(capabilities.getSupportedQualities(SDR)).containsExactly(FHD)
    }

    @Test
    fun getResolution_returnsCorrectSize() {
        val capabilities = RecorderVideoCapabilities(profilesResolver, fakeCameraInfo)

        assertThat(capabilities.getResolution(FHD, SDR)).isEqualTo(RESOLUTION_1080P)
    }
}
