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
        assertThat(
                validator.isUnsupported(
                    VideoStabilization.ON,
                    listOf(PREVIEW_SIZE to DynamicRange.HLG_10_BIT, UHD to DynamicRange.HLG_10_BIT),
                )
            )
            .isTrue()
    }

    @Test
    fun unsupported_hlg10LargerThanUhdWithVideoStabilization() {
        assertThat(
                validator.isUnsupported(
                    VideoStabilization.ON,
                    listOf(Size(4080, 3060) to DynamicRange.HLG_10_BIT),
                )
            )
            .isTrue()
    }

    @Test
    fun supported_hlg10BelowUhdWithVideoStabilization() {
        assertThat(
                validator.isUnsupported(
                    VideoStabilization.ON,
                    listOf(
                        PREVIEW_SIZE to DynamicRange.HLG_10_BIT,
                        Size(2560, 1440) to DynamicRange.HLG_10_BIT,
                    ),
                )
            )
            .isFalse()
    }

    @Test
    fun supported_hlg10UhdWithoutVideoStabilization() {
        listOf(
                VideoStabilization.UNSPECIFIED,
                VideoStabilization.OFF,
                VideoStabilization.PREVIEW,
            )
            .forEach { videoStabilization ->
                assertThat(
                        validator.isUnsupported(
                            videoStabilization,
                            listOf(UHD to DynamicRange.HLG_10_BIT),
                        )
                    )
                    .isFalse()
            }
    }

    @Test
    fun supported_nonHlg10UhdWithVideoStabilization() {
        listOf(DynamicRange.SDR, DynamicRange.HDR10_10_BIT).forEach { dynamicRange ->
            assertThat(validator.isUnsupported(VideoStabilization.ON, listOf(UHD to dynamicRange)))
                .isFalse()
        }
    }

    @Test
    fun supported_whenQuirkNotEnabled() {
        assertThat(
                Hlg10UhdVideoStabilizationValidator(Quirks(emptyList()))
                    .isUnsupported(VideoStabilization.ON, listOf(UHD to DynamicRange.HLG_10_BIT))
            )
            .isFalse()
    }

    private companion object {
        val PREVIEW_SIZE = Size(1440, 1080)
        val UHD = Size(3840, 2160)
    }
}
