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

package androidx.camera.camera2.pipe.compat

import android.graphics.ColorSpace
import android.graphics.ImageFormat
import android.hardware.camera2.params.ColorSpaceProfiles
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import androidx.camera.camera2.pipe.CameraColorSpace
import androidx.camera.camera2.pipe.OutputStream
import androidx.camera.camera2.pipe.StreamFormat
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.rules.MethodRule
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [Camera2ColorSpaceProfiles] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class Camera2ColorSpaceProfilesTest {
    @Rule @JvmField val mocks: MethodRule = MockitoJUnit.rule()

    @Mock private lateinit var mockColorSpaceProfiles: ColorSpaceProfiles

    @InjectMocks private lateinit var camera2ColorSpaceProfiles: Camera2ColorSpaceProfiles

    @Test
    fun getSupportedColorSpaces() {
        val imageFormat = StreamFormat(ImageFormat.JPEG)
        val frameworkColorSpaces = setOf(ColorSpace.Named.SRGB, ColorSpace.Named.DISPLAY_P3)
        whenever(mockColorSpaceProfiles.getSupportedColorSpaces(imageFormat.value))
            .thenReturn(frameworkColorSpaces)

        val result = camera2ColorSpaceProfiles.getSupportedColorSpaces(imageFormat)

        verify(mockColorSpaceProfiles, times(1)).getSupportedColorSpaces(imageFormat.value)
        Truth.assertThat(result)
            .containsExactly(CameraColorSpace.Companion.SRGB, CameraColorSpace.Companion.DISPLAY_P3)
    }

    @Test
    fun getSupportedImageFormatsForColorSpace() {
        val cameraColorSpace = CameraColorSpace.Companion.SRGB
        val frameworkImageFormats = setOf(ImageFormat.JPEG, ImageFormat.YUV_420_888)
        val expectedResult = setOf(StreamFormat.Companion.JPEG, StreamFormat.Companion.YUV_420_888)
        val frameworkColorSpace = ColorSpace.Named.SRGB
        whenever(mockColorSpaceProfiles.getSupportedImageFormatsForColorSpace(frameworkColorSpace))
            .thenReturn(frameworkImageFormats)

        val result =
            camera2ColorSpaceProfiles.getSupportedImageFormatsForColorSpace(cameraColorSpace)

        verify(mockColorSpaceProfiles, times(1))
            .getSupportedImageFormatsForColorSpace(frameworkColorSpace)
        Truth.assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun getSupportedDynamicRangeProfiles() {
        val cameraColorSpace = CameraColorSpace.Companion.BT2020_HLG
        val imageFormat = StreamFormat(ImageFormat.HEIC)
        val expectedResult =
            setOf(OutputStream.DynamicRangeProfile.HLG10, OutputStream.DynamicRangeProfile.STANDARD)
        val frameworkColorSpace = ColorSpace.Named.BT2020_HLG
        val frameworkDynamicProfiles =
            setOf(
                OutputStream.DynamicRangeProfile.HLG10.value,
                OutputStream.DynamicRangeProfile.STANDARD.value,
            )
        whenever(
                mockColorSpaceProfiles.getSupportedDynamicRangeProfiles(
                    frameworkColorSpace,
                    imageFormat.value,
                )
            )
            .thenReturn(frameworkDynamicProfiles)

        val result =
            camera2ColorSpaceProfiles.getSupportedDynamicRangeProfiles(
                cameraColorSpace,
                imageFormat,
            )

        verify(mockColorSpaceProfiles, times(1))
            .getSupportedDynamicRangeProfiles(frameworkColorSpace, imageFormat.value)
        Truth.assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    fun getSupportedColorSpacesForDynamicRange() {
        val imageFormat = StreamFormat(ImageFormat.PRIVATE)
        val dynamicRangeProfile = OutputStream.DynamicRangeProfile(DynamicRangeProfiles.HDR10)
        val frameworkColorSpaces = setOf(ColorSpace.Named.BT2020, ColorSpace.Named.DCI_P3)

        whenever(
                mockColorSpaceProfiles.getSupportedColorSpacesForDynamicRange(
                    imageFormat.value,
                    dynamicRangeProfile.value,
                )
            )
            .thenReturn(frameworkColorSpaces)

        val result =
            camera2ColorSpaceProfiles.getSupportedColorSpacesForDynamicRange(
                imageFormat,
                dynamicRangeProfile,
            )
        verify(mockColorSpaceProfiles, times(1))
            .getSupportedColorSpacesForDynamicRange(imageFormat.value, dynamicRangeProfile.value)
        Truth.assertThat(result)
            .containsExactly(CameraColorSpace.Companion.BT2020, CameraColorSpace.Companion.DCI_P3)
    }

    @Test
    fun unwrapAs_supportedType() {
        val unwrapped = camera2ColorSpaceProfiles.unwrapAs(ColorSpaceProfiles::class)

        Truth.assertThat(unwrapped).isNotNull()
        Truth.assertThat(unwrapped).isSameInstanceAs(mockColorSpaceProfiles)
    }

    @Test
    fun unwrapAs_UnsupportedType() {
        Truth.assertThat(camera2ColorSpaceProfiles.unwrapAs(ColorSpace::class)).isNull()
    }
}
