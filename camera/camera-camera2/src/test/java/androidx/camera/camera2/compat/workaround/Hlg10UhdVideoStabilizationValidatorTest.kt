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

package androidx.camera.camera2.compat.workaround

import android.util.Size
import androidx.camera.camera2.compat.quirk.CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk
import androidx.camera.core.DynamicRange
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.SurfaceConfig
import androidx.camera.core.impl.SurfaceConfig.ConfigSize
import androidx.camera.core.impl.SurfaceConfig.ConfigType
import androidx.camera.core.impl.stabilization.VideoStabilization
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class Hlg10UhdVideoStabilizationValidatorTest {

    private val validator =
        Hlg10UhdVideoStabilizationValidator(
            Quirks(listOf(CameraCrashWhenHlg10UhdWithVideoStabilizationQuirk()))
        )

    @Test
    fun unsupported_hlg10UhdWithVideoStabilization() {
        val configs = listOf(PREVIEW_CONFIG, VIDEO_CONFIG)
        val dynamicRanges =
            mapOf(
                PREVIEW_CONFIG to DynamicRange.HLG_10_BIT,
                VIDEO_CONFIG to DynamicRange.HLG_10_BIT,
            )
        val sizes = mapOf(PREVIEW_CONFIG to PREVIEW_SIZE, VIDEO_CONFIG to UHD)

        assertThat(
                validator.isUnsupported(VideoStabilization.ON, configs, dynamicRanges) {
                    sizes.getValue(it)
                }
            )
            .isTrue()
    }

    @Test
    fun unsupported_hlg10LargerThanUhdWithVideoStabilization() {
        val configs = listOf(VIDEO_CONFIG)
        val dynamicRanges = mapOf(VIDEO_CONFIG to DynamicRange.HLG_10_BIT)
        val sizes = mapOf(VIDEO_CONFIG to Size(4080, 3060))

        assertThat(
                validator.isUnsupported(VideoStabilization.ON, configs, dynamicRanges) {
                    sizes.getValue(it)
                }
            )
            .isTrue()
    }

    @Test
    fun supported_hlg10BelowUhdWithVideoStabilization() {
        val configs = listOf(PREVIEW_CONFIG, VIDEO_CONFIG)
        val dynamicRanges =
            mapOf(
                PREVIEW_CONFIG to DynamicRange.HLG_10_BIT,
                VIDEO_CONFIG to DynamicRange.HLG_10_BIT,
            )
        val sizes = mapOf(PREVIEW_CONFIG to PREVIEW_SIZE, VIDEO_CONFIG to Size(2560, 1440))

        assertThat(
                validator.isUnsupported(VideoStabilization.ON, configs, dynamicRanges) {
                    sizes.getValue(it)
                }
            )
            .isFalse()
    }

    @Test
    fun supported_hlg10UhdWithoutVideoStabilization() {
        val configs = listOf(VIDEO_CONFIG)
        val dynamicRanges = mapOf(VIDEO_CONFIG to DynamicRange.HLG_10_BIT)

        listOf(
                VideoStabilization.UNSPECIFIED,
                VideoStabilization.OFF,
                VideoStabilization.PREVIEW,
            )
            .forEach { videoStabilization ->
                assertThat(
                        validator.isUnsupported(videoStabilization, configs, dynamicRanges) { UHD }
                    )
                    .isFalse()
            }
    }

    @Test
    fun supported_nonHlg10UhdWithVideoStabilization() {
        val configs = listOf(VIDEO_CONFIG)

        listOf(DynamicRange.SDR, DynamicRange.HDR10_10_BIT).forEach { dynamicRange ->
            assertThat(
                    validator.isUnsupported(
                        VideoStabilization.ON,
                        configs,
                        mapOf(VIDEO_CONFIG to dynamicRange),
                    ) {
                        UHD
                    }
                )
                .isFalse()
        }
    }

    @Test
    fun supported_whenQuirkNotEnabled() {
        val configs = listOf(VIDEO_CONFIG)
        val dynamicRanges = mapOf(VIDEO_CONFIG to DynamicRange.HLG_10_BIT)

        assertThat(
                Hlg10UhdVideoStabilizationValidator(Quirks(emptyList())).isUnsupported(
                    VideoStabilization.ON,
                    configs,
                    dynamicRanges,
                ) {
                    UHD
                }
            )
            .isFalse()
    }

    @Test
    fun resolutionProviderNotInvoked_whenQuirkNotEnabled() {
        var invoked = false
        val configs = listOf(VIDEO_CONFIG)
        val dynamicRanges = mapOf(VIDEO_CONFIG to DynamicRange.HLG_10_BIT)

        val isUnsupported =
            Hlg10UhdVideoStabilizationValidator(Quirks(emptyList())).isUnsupported(
                VideoStabilization.ON,
                configs,
                dynamicRanges,
            ) {
                invoked = true
                UHD
            }

        assertThat(isUnsupported).isFalse()
        assertThat(invoked).isFalse()
    }

    @Test
    fun resolutionProviderNotInvoked_withoutVideoStabilization() {
        val configs = listOf(VIDEO_CONFIG)
        val dynamicRanges = mapOf(VIDEO_CONFIG to DynamicRange.HLG_10_BIT)

        listOf(
                VideoStabilization.UNSPECIFIED,
                VideoStabilization.OFF,
                VideoStabilization.PREVIEW,
            )
            .forEach { videoStabilization ->
                var invoked = false

                val isUnsupported =
                    validator.isUnsupported(videoStabilization, configs, dynamicRanges) {
                        invoked = true
                        UHD
                    }

                assertThat(isUnsupported).isFalse()
                assertThat(invoked).isFalse()
            }
    }

    @Test
    fun resolutionProviderNotInvoked_forNonHlg10Streams() {
        val jpegrMaxConfig = SurfaceConfig.create(ConfigType.JPEG_R, ConfigSize.MAXIMUM)
        val hdr10UhdConfig = SurfaceConfig.create(ConfigType.PRIV, ConfigSize.UHD)
        val configs = listOf(jpegrMaxConfig, hdr10UhdConfig, VIDEO_CONFIG)
        val dynamicRanges =
            mapOf(
                jpegrMaxConfig to DynamicRange.SDR,
                hdr10UhdConfig to DynamicRange.HDR10_10_BIT,
                VIDEO_CONFIG to DynamicRange.HLG_10_BIT,
            )

        val resolvedBelowUhdConfigs = mutableListOf<SurfaceConfig>()
        val isBelowUhdUnsupported =
            validator.isUnsupported(VideoStabilization.ON, configs, dynamicRanges) { config ->
                if (config != VIDEO_CONFIG) {
                    throw AssertionError("Unexpected resolution lookup for $config")
                }
                resolvedBelowUhdConfigs.add(config)
                Size(2560, 1440)
            }
        assertThat(isBelowUhdUnsupported).isFalse()
        assertThat(resolvedBelowUhdConfigs).containsExactly(VIDEO_CONFIG)

        val resolvedUhdConfigs = mutableListOf<SurfaceConfig>()
        val isUhdUnsupported =
            validator.isUnsupported(VideoStabilization.ON, configs, dynamicRanges) { config ->
                if (config != VIDEO_CONFIG) {
                    throw AssertionError("Unexpected resolution lookup for $config")
                }
                resolvedUhdConfigs.add(config)
                UHD
            }
        assertThat(isUhdUnsupported).isTrue()
        assertThat(resolvedUhdConfigs).containsExactly(VIDEO_CONFIG)
    }

    private companion object {
        val PREVIEW_CONFIG = SurfaceConfig.create(ConfigType.PRIV, ConfigSize.PREVIEW)
        val VIDEO_CONFIG = SurfaceConfig.create(ConfigType.PRIV, ConfigSize.RECORD)
        val PREVIEW_SIZE = Size(1440, 1080)
        val UHD = Size(3840, 2160)
    }
}
