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

package androidx.camera.video

import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON
import androidx.camera.core.Preview
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.testing.impl.FrameRateUtil.FPS_120_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_120
import androidx.camera.testing.impl.FrameRateUtil.FPS_30_30
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@OptIn(ExperimentalHighSpeedVideo::class)
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class HighSpeedVideoSessionConfigTest {

    private val defaultVideoCapture = createVideoCapture()
    private val defaultPreview = createPreview()

    @Test
    fun constructor_mirrorModeSet_throwsException() {
        val videoCapture = createVideoCapture(mirrorMode = MIRROR_MODE_ON)

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(videoCapture, frameRateRange = FPS_120_120)
        }
    }

    @Test
    fun constructor_targetFrameRateSetForVideoCapture_throwsException() {
        val videoCapture = createVideoCapture(targetFrameRate = FPS_30_30)

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(videoCapture, frameRateRange = FPS_120_120)
        }
    }

    @Test
    fun constructor_previewPresentAndTargetFrameRateSet_throwsException() {
        val preview = createPreview(targetFrameRate = FPS_30_30)

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(
                defaultVideoCapture,
                preview = preview,
                frameRateRange = FPS_120_120,
            )
        }
    }

    @Test
    fun constructor_previewPresentAndResolutionSelectorSet_throwsException() {
        val preview = createPreview(resolutionSelector = ResolutionSelector.Builder().build())

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(
                defaultVideoCapture,
                preview = preview,
                frameRateRange = FPS_120_120,
            )
        }
    }

    @Test
    fun constructor_previewPresentAndTargetResolutionSet_throwsException() {
        val preview = createPreview(targetResolution = RESOLUTION_480P)

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(
                defaultVideoCapture,
                preview = preview,
                frameRateRange = FPS_120_120,
            )
        }
    }

    @Test
    fun constructor_previewPresentAndTargetAspectRatioSet_throwsException() {
        val preview = createPreview(targetAspectRatio = AspectRatio.RATIO_4_3)

        assertThrows(IllegalArgumentException::class.java) {
            HighSpeedVideoSessionConfig(
                defaultVideoCapture,
                preview = preview,
                frameRateRange = FPS_120_120,
            )
        }
    }

    @Test
    fun getUseCaseList_containsVideoCapture() {
        val config = HighSpeedVideoSessionConfig(defaultVideoCapture, frameRateRange = FPS_120_120)

        assertThat(config.useCases).containsExactly(defaultVideoCapture)
    }

    @Test
    fun getUseCaseList_containsVideoCaptureAndPreview() {
        val config =
            HighSpeedVideoSessionConfig(
                defaultVideoCapture,
                preview = defaultPreview,
                frameRateRange = FPS_120_120,
            )

        assertThat(config.useCases).containsExactly(defaultVideoCapture, defaultPreview)
    }

    @Test
    fun builder_build_defaultIsSlowMotionEnabledFalse() {
        val config =
            HighSpeedVideoSessionConfig.Builder(defaultVideoCapture)
                .setFrameRateRange(FPS_120_120)
                .build()

        assertThat(config.isSlowMotionEnabled).isFalse()
    }

    @Test
    fun builder_setlSlowMotionEnabled_configHasIsSlowMotionEnabledTrue() {
        val config =
            HighSpeedVideoSessionConfig.Builder(defaultVideoCapture)
                .setFrameRateRange(FPS_120_120)
                .setSlowMotionEnabled(true)
                .build()

        assertThat(config.isSlowMotionEnabled).isTrue()
    }

    @Test
    fun builder_build_setsFrameRateAndVideoCapture() {
        val config =
            HighSpeedVideoSessionConfig.Builder(defaultVideoCapture)
                .setFrameRateRange(FPS_30_120)
                .build()

        assertThat(config.frameRateRange).isEqualTo(FPS_30_120)
        assertThat(config.videoCapture).isEqualTo(defaultVideoCapture)
        assertThat(config.useCases).containsExactly(defaultVideoCapture)
    }

    @Test
    fun builder_setPreview_configHasPreview() {
        val config =
            HighSpeedVideoSessionConfig.Builder(defaultVideoCapture)
                .setPreview(defaultPreview)
                .setFrameRateRange(FPS_120_120)
                .build()

        assertThat(config.preview).isEqualTo(defaultPreview)
        assertThat(config.useCases).containsExactly(defaultVideoCapture, defaultPreview)
    }

    private fun createRecorder() = Recorder.Builder().build()

    private fun createVideoCapture(
        recorder: Recorder = createRecorder(),
        mirrorMode: Int? = null,
        targetFrameRate: Range<Int>? = null,
    ) =
        VideoCapture.Builder(recorder)
            .apply {
                mirrorMode?.let { setMirrorMode(it) }
                targetFrameRate?.let { setTargetFrameRate(it) }
            }
            .build()

    @Suppress("DEPRECATION")
    private fun createPreview(
        targetFrameRate: Range<Int>? = null,
        resolutionSelector: ResolutionSelector? = null,
        targetResolution: Size? = null,
        targetAspectRatio: Int? = null,
    ) =
        Preview.Builder()
            .apply {
                targetFrameRate?.let { setTargetFrameRate(it) }
                resolutionSelector?.let { setResolutionSelector(it) }
                targetResolution?.let { setTargetResolution(it) }
                targetAspectRatio?.let { setTargetAspectRatio(it) }
            }
            .build()
}
