/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.test.screenshot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.screenshot.OutputFileType.DIFF_TEXT_RESULT_PROTO
import androidx.test.screenshot.OutputFileType.IMAGE_ACTUAL
import androidx.test.screenshot.OutputFileType.IMAGE_DIFF
import androidx.test.screenshot.OutputFileType.IMAGE_EXPECTED
import androidx.test.screenshot.matchers.PixelPerfectMatcher
import androidx.test.screenshot.utils.loadBitmap
import com.google.common.truth.Truth.assertThat
import java.lang.AssertionError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ScreenshotTestRuleTest {

    @get:Rule val rule = ScreenshotTestRule()

    @Before
    fun setup() {
        rule.setCustomGoldenIdResolver { goldenId -> "$goldenId.png" }
    }

    @Test
    fun performDiff_sameBitmaps() {
        val first = loadBitmap("round_rect_gray")

        first.assertAgainstGolden(rule, "round_rect_gray", matcher = PixelPerfectMatcher())

        val diffTextResultProto = rule.getPathOnDeviceFor(DIFF_TEXT_RESULT_PROTO)
        assertThat(diffTextResultProto.readText()).contains("PASSED")
        assertThat(rule.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isFalse()
        assertThat(rule.getPathOnDeviceFor(IMAGE_DIFF).exists()).isFalse()
        assertThat(rule.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isFalse()
    }

    @Test
    fun performDiff_sameSizes_default_noMatch() {
        val first = loadBitmap("round_rect_gray")

        expectErrorMessage(
            "" +
                "Image mismatch! Comparison stats: '[MSSIM] Required SSIM: 0.98, Actual SSIM: " +
                "0.951'"
        ) {
            first.assertAgainstGolden(rule, "round_rect_green")
        }

        val diffTextResultProto = rule.getPathOnDeviceFor(DIFF_TEXT_RESULT_PROTO)
        assertThat(diffTextResultProto.readText()).contains("FAILED")
        assertThat(rule.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_DIFF).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isTrue()
    }

    @Test
    fun performDiff_sameSizes_pixelPerfect_noMatch() {
        val first = loadBitmap("round_rect_gray")

        expectErrorMessage(
            "" +
                "Image mismatch! Comparison stats: '[PixelPerfect] Same pixels: 1748, " +
                "Different pixels: 556'"
        ) {
            first.assertAgainstGolden(rule, "round_rect_green", matcher = PixelPerfectMatcher())
        }

        val diffTextResultProto = rule.getPathOnDeviceFor(DIFF_TEXT_RESULT_PROTO)
        assertThat(diffTextResultProto.readText()).contains("FAILED")
        assertThat(rule.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_DIFF).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isTrue()
    }

    @Test
    fun performDiff_differentSizes() {
        val first = loadBitmap("fullscreen_rect_gray")

        expectErrorMessage("Sizes are different! Expected: [48, 48], Actual: [720, 1184]") {
            first.assertAgainstGolden(rule, "round_rect_gray")
        }

        val diffTextResultProto = rule.getPathOnDeviceFor(DIFF_TEXT_RESULT_PROTO)
        assertThat(diffTextResultProto.readText()).contains("FAILED")
        assertThat(rule.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_DIFF).exists()).isFalse()
        assertThat(rule.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isTrue()
    }

    @Test(expected = IllegalArgumentException::class)
    fun performDiff_incorrectGoldenName() {
        val first = loadBitmap("fullscreen_rect_gray")

        first.assertAgainstGolden(rule, "round_rect_gray #")
    }

    @Test
    fun performDiff_missingGolden() {
        val first = loadBitmap("round_rect_gray")

        expectErrorMessage(
            "Missing golden image 'does_not_exist.png'. Did you mean to check in " + "a new image?"
        ) {
            first.assertAgainstGolden(rule, "does_not_exist")
        }

        val diffTextResultProto = rule.getPathOnDeviceFor(DIFF_TEXT_RESULT_PROTO)
        assertThat(diffTextResultProto.readText()).contains("MISSING_REFERENCE")
        assertThat(rule.getPathOnDeviceFor(IMAGE_ACTUAL).exists()).isTrue()
        assertThat(rule.getPathOnDeviceFor(IMAGE_DIFF).exists()).isFalse()
        assertThat(rule.getPathOnDeviceFor(IMAGE_EXPECTED).exists()).isFalse()
    }

    @After
    fun after() {
        rule.clearCustomGoldenIdResolver()
        // Clear all files we generated so we don't have dependencies between tests
        rule.deviceOutputDirectory.deleteRecursively()
    }

    private fun expectErrorMessage(expectedErrorMessage: String, block: () -> Unit) {
        try {
            block()
        } catch (e: AssertionError) {
            val received = e.localizedMessage!!
            assertThat(received).isEqualTo(expectedErrorMessage.trim())
            return
        }

        throw AssertionError("No AssertionError thrown!")
    }
}
