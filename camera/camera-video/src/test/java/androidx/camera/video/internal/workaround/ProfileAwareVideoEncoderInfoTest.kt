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

package androidx.camera.video.internal.workaround

import android.media.CamcorderProfile
import android.media.EncoderProfiles
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.media.MediaFormat.MIMETYPE_VIDEO_HEVC
import android.media.MediaRecorder
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.camera.testing.impl.fakes.FakeVideoEncoderInfo
import com.google.common.truth.Truth.assertThat
import kotlin.test.DefaultAsserter.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowCamcorderProfile
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    sdk = [Config.ALL_SDKS],
    shadows =
        [
            ProfileAwareVideoEncoderInfoTest.ShadowEncoderProfilesApi31::class,
            ProfileAwareVideoEncoderInfoTest.ShadowVideoProfileApi31::class,
            ProfileAwareVideoEncoderInfoTest.ShadowCamcorderProfileApi31::class,
        ],
)
class ProfileAwareVideoEncoderInfoTest {

    private companion object {
        const val MIME_AVC = MIMETYPE_VIDEO_AVC
        const val MIME_HEVC = MIMETYPE_VIDEO_HEVC
        const val BASE_WIDTH_ALIGNMENT = 2
        const val BASE_HEIGHT_ALIGNMENT = 2
        val BASE_WIDTHS: Range<Int> = Range.create(BASE_WIDTH_ALIGNMENT, 640)
        val BASE_HEIGHTS: Range<Int> = Range.create(BASE_HEIGHT_ALIGNMENT, 480)
        val EXTRA_SIZE = Size(1920, 1080)
    }

    @Before
    fun setup() {
        ShadowCamcorderProfile.reset()
        ShadowCamcorderProfileApi31.reset()
        ProfileAwareVideoEncoderInfo.clearCache()
    }

    @After
    fun tearDown() {
        ShadowCamcorderProfile.reset()
        ShadowCamcorderProfileApi31.reset()
        ProfileAwareVideoEncoderInfo.clearCache()
    }

    @Test
    fun correctsCapabilities() {
        setupFakeProfile(EXTRA_SIZE.width, EXTRA_SIZE.height, MIME_AVC)

        val baseInfo = createFakeInfo()
        val wrapper = ProfileAwareVideoEncoderInfo.from(baseInfo)

        assertThat(wrapper.getSupportedWidths().upper).isEqualTo(EXTRA_SIZE.width)
        assertThat(wrapper.getSupportedHeights().upper).isEqualTo(EXTRA_SIZE.height)
        assertThat(wrapper.isSizeSupported(EXTRA_SIZE.width, EXTRA_SIZE.height)).isTrue()
    }

    @Test
    fun mimeIsolation_doesNotExpandRangeForDifferentMime() {
        // Profile exists for HEVC, but we are querying AVC
        setupFakeProfile(EXTRA_SIZE.width, EXTRA_SIZE.height, MIME_HEVC)

        val baseInfo = createFakeInfo(MIME_AVC)
        val wrapper = ProfileAwareVideoEncoderInfo.from(baseInfo)

        // Range should remain at BASE values, not EXTRA_SIZE
        assertThat(wrapper.getSupportedWidths().upper).isEqualTo(BASE_WIDTHS.upper)
        assertThat(wrapper.isSizeSupported(EXTRA_SIZE.width, EXTRA_SIZE.height)).isFalse()
    }

    @Test
    @Config(minSdk = 31)
    fun getSupportedWidthsFor_bridgesExceptionUsingProfile() {
        setupFakeProfile(EXTRA_SIZE.width, EXTRA_SIZE.height, MIME_AVC)

        // Create a fake info that throws for the "Extra" height
        val baseInfo =
            object :
                FakeVideoEncoderInfo(
                    mime = MIME_AVC,
                    supportedWidths = BASE_WIDTHS,
                    supportedHeights = BASE_HEIGHTS,
                ) {
                override fun getSupportedWidthsFor(height: Int): Range<Int> {
                    if (height == EXTRA_SIZE.height) throw IllegalArgumentException("Unsupported")
                    return super.getSupportedWidthsFor(height)
                }
            }

        val wrapper = ProfileAwareVideoEncoderInfo.from(baseInfo)

        // Should catch the exception and return the stretched global range
        val widths = wrapper.getSupportedWidthsFor(EXTRA_SIZE.height)
        assertThat(widths.upper).isEqualTo(EXTRA_SIZE.width)
    }

    @Test
    fun isSizeSupported_respectsAlignmentForNonProfileSizes() {
        val baseInfo = createFakeInfo()
        val wrapper = ProfileAwareVideoEncoderInfo.from(baseInfo)
        // 333 is not aligned to 2
        assertThat(wrapper.isSizeSupported(333, 333)).isFalse()
    }

    @Test
    fun isSizeSupported_whitelistsSpecificProfileSize() {
        setupFakeProfile(EXTRA_SIZE.width, EXTRA_SIZE.height, MIME_AVC)
        val baseInfo = createFakeInfo(MIME_AVC)
        val wrapper = ProfileAwareVideoEncoderInfo.from(baseInfo)

        // Whitelisted even if it's far outside BASE_WIDTHS/HEIGHTS
        assertThat(wrapper.isSizeSupported(EXTRA_SIZE.width, EXTRA_SIZE.height)).isTrue()
    }

    private fun setupFakeProfile(width: Int, height: Int, mime: String) {
        if (Build.VERSION.SDK_INT >= 31) {
            val mockProfiles = createFakeEncoderProfiles(width, height, mime)
            ShadowCamcorderProfileApi31.addEncoderProfiles(
                0,
                CamcorderProfile.QUALITY_1080P,
                mockProfiles,
            )
        } else {
            val profile = ReflectionHelpers.callConstructor(CamcorderProfile::class.java)
            ReflectionHelpers.setField(profile, "videoFrameWidth", width)
            ReflectionHelpers.setField(profile, "videoFrameHeight", height)
            val codec =
                when (mime) {
                    MIME_HEVC -> MediaRecorder.VideoEncoder.HEVC
                    MIME_AVC -> MediaRecorder.VideoEncoder.H264
                    else -> fail("Undefined MIME mapping: $mime")
                }
            ReflectionHelpers.setField(profile, "videoCodec", codec)
            ShadowCamcorderProfile.addProfile(0, CamcorderProfile.QUALITY_1080P, profile)
        }
    }

    private fun createFakeInfo(
        mime: String = MIME_AVC,
        supportedWidths: Range<Int> = BASE_WIDTHS,
        supportedHeights: Range<Int> = BASE_HEIGHTS,
    ) =
        FakeVideoEncoderInfo(
            mime = mime,
            supportedWidths = supportedWidths,
            supportedHeights = supportedHeights,
            widthAlignment = BASE_WIDTH_ALIGNMENT,
            heightAlignment = BASE_HEIGHT_ALIGNMENT,
        )

    @RequiresApi(31)
    private fun createFakeEncoderProfiles(
        width: Int,
        height: Int,
        mime: String = MIME_AVC,
    ): EncoderProfiles {
        val videoProfile =
            ReflectionHelpers.callConstructor(EncoderProfiles.VideoProfile::class.java)
        val shadowVideo: ShadowVideoProfileApi31 = Shadow.extract(videoProfile)
        shadowVideo.widthVar = width
        shadowVideo.heightVar = height
        shadowVideo.mediaTypeVar = mime

        val encoderProfiles = ReflectionHelpers.callConstructor(EncoderProfiles::class.java)
        val shadowProfiles: ShadowEncoderProfilesApi31 = Shadow.extract(encoderProfiles)
        shadowProfiles.videoProfilesList = listOf(videoProfile)

        return encoderProfiles
    }

    @Suppress("unused")
    @Implements(EncoderProfiles.VideoProfile::class, minSdk = 31)
    class ShadowVideoProfileApi31 {
        var widthVar = 0
        var heightVar = 0
        var mediaTypeVar = ""

        @Implementation fun getWidth() = widthVar

        @Implementation fun getHeight() = heightVar

        @Implementation fun getMediaType() = mediaTypeVar
    }

    @Suppress("unused")
    @Implements(EncoderProfiles::class, minSdk = 31)
    class ShadowEncoderProfilesApi31 {
        var videoProfilesList = listOf<EncoderProfiles.VideoProfile>()

        @Implementation fun getVideoProfiles() = videoProfilesList
    }

    @Implements(CamcorderProfile::class)
    class ShadowCamcorderProfileApi31 : ShadowCamcorderProfile() {
        companion object {
            private val profileMap = mutableMapOf<String, EncoderProfiles>()

            @JvmStatic
            @Implementation
            fun getAll(cameraId: String, quality: Int) = profileMap["$cameraId-$quality"]

            fun addEncoderProfiles(cameraId: Int, quality: Int, profiles: EncoderProfiles) {
                // This satisfies the internal 'hasProfile' check in the base Shadow
                addProfile(
                    cameraId,
                    quality,
                    ReflectionHelpers.callConstructor(CamcorderProfile::class.java),
                )

                profileMap["$cameraId-$quality"] = profiles
            }

            fun reset() {
                profileMap.clear()
            }
        }
    }
}
