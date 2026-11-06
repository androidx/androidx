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

package androidx.camera.core.internal.compat.quirk

import androidx.camera.core.DynamicRange
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.UseCaseConfig
import androidx.camera.core.impl.UseCaseConfigFactory.CaptureType
import androidx.camera.core.impl.stabilization.StabilizationMode
import androidx.camera.core.internal.compat.workaround.StreamSharingForceEnabler
import androidx.camera.testing.impl.fakes.FakeUseCaseConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowBuild

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class AbnormalStreamWhenHlg10WithStabilizationQuirkTest {

    @Test
    fun quirkLoaded_onSamsungSnapdragon8Gen2() {
        listOf("SM8550", "sm8550").forEach { socModel ->
            setDevice(brand = "samsung", socModel = socModel)

            assertThat(isQuirkLoaded()).isTrue()
        }
    }

    @Test
    fun quirkNotLoaded_onSamsungWithOtherChipset() {
        // Galaxy S24 Ultra (Snapdragon 8 Gen 3) and Galaxy S24 (Exynos 2400).
        listOf("SM8650", "s5e9945").forEach { socModel ->
            setDevice(brand = "samsung", socModel = socModel)

            assertThat(isQuirkLoaded()).isFalse()
        }
    }

    @Test
    fun quirkNotLoaded_onOtherManufacturerWithSnapdragon8Gen2() {
        setDevice(brand = "Xiaomi", socModel = "SM8550")

        assertThat(isQuirkLoaded()).isFalse()
    }

    @Test
    fun forceStreamSharing_hlg10VideoCaptureWithPreviewStabilization() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    previewWithStabilization(),
                    videoCapture(DynamicRange.HLG_10_BIT),
                )
            )
            .isTrue()
    }

    @Test
    fun forceStreamSharing_hlg10VideoCaptureWithVideoStabilization() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    Preview.Builder().build(),
                    videoCapture(DynamicRange.HLG_10_BIT, videoStabilization = true),
                )
            )
            .isTrue()
    }

    @Test
    fun forceStreamSharing_hdrUnspecifiedVideoCaptureWithStabilization() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    previewWithStabilization(),
                    videoCapture(DynamicRange.HDR_UNSPECIFIED_10_BIT),
                )
            )
            .isTrue()
    }

    @Test
    fun notForceStreamSharing_hlg10VideoCaptureWithoutStabilization() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    Preview.Builder().build(),
                    videoCapture(DynamicRange.HLG_10_BIT),
                )
            )
            .isFalse()
    }

    @Test
    fun notForceStreamSharing_stabilizationExplicitlyOff() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    Preview.Builder().setPreviewStabilizationEnabled(false).build(),
                    videoCapture(DynamicRange.HLG_10_BIT, videoStabilization = true),
                )
            )
            .isFalse()
    }

    @Test
    fun notForceStreamSharing_nonHlg10VideoCaptureWithStabilization() {
        setAffectedDevice()

        // Dynamic ranges other than HLG10 are not verified to be affected.
        listOf(DynamicRange.SDR, DynamicRange.HDR10_10_BIT, DynamicRange.HDR10_PLUS_10_BIT)
            .forEach { dynamicRange ->
                assertThat(
                        shouldForceEnableStreamSharing(
                            previewWithStabilization(),
                            videoCapture(dynamicRange),
                        )
                    )
                    .isFalse()
            }
    }

    @Test
    fun notForceStreamSharing_hlg10PreviewOnlyWithStabilization() {
        setAffectedDevice()

        assertThat(
                shouldForceEnableStreamSharing(
                    Preview.Builder()
                        .setPreviewStabilizationEnabled(true)
                        .setDynamicRange(DynamicRange.HLG_10_BIT)
                        .build()
                )
            )
            .isFalse()
    }

    @Test
    fun notForceStreamSharing_onUnaffectedDevice() {
        setDevice(brand = "samsung", socModel = "SM8650")

        assertThat(
                shouldForceEnableStreamSharing(
                    previewWithStabilization(),
                    videoCapture(DynamicRange.HLG_10_BIT),
                )
            )
            .isFalse()
    }

    private fun setAffectedDevice() = setDevice(brand = "samsung", socModel = "SM8550")

    private fun setDevice(brand: String, socModel: String) {
        ShadowBuild.setBrand(brand)
        ShadowBuild.setManufacturer(brand)
        ShadowBuild.setSystemOnChipModel(socModel)
    }

    private fun isQuirkLoaded() =
        DeviceQuirks.get(AbnormalStreamWhenHlg10WithStabilizationQuirk::class.java) != null

    private fun shouldForceEnableStreamSharing(vararg useCases: UseCase): Boolean =
        StreamSharingForceEnabler().shouldForceEnableStreamSharing(CAMERA_ID, useCases.toList())

    private fun previewWithStabilization(): Preview =
        Preview.Builder().setPreviewStabilizationEnabled(true).build()

    private fun videoCapture(
        dynamicRange: DynamicRange,
        videoStabilization: Boolean = false,
    ): UseCase =
        FakeUseCaseConfig.Builder()
            .setCaptureType(CaptureType.VIDEO_CAPTURE)
            .setDynamicRange(dynamicRange)
            .apply {
                if (videoStabilization) {
                    mutableConfig.insertOption(
                        UseCaseConfig.OPTION_VIDEO_STABILIZATION_MODE,
                        StabilizationMode.ON,
                    )
                }
            }
            .build()

    private companion object {
        const val CAMERA_ID = "0"
    }
}
