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

package androidx.camera.integration.core.stresstest

import androidx.camera.core.CameraXConfig
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_ANALYSIS
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_CAPTURE
import androidx.camera.integration.core.CameraXActivity.BIND_PREVIEW
import androidx.camera.integration.core.CameraXActivity.BIND_VIDEO_CAPTURE
import androidx.camera.integration.core.util.StressTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.testing.impl.LabTestRule
import androidx.test.filters.LargeTest
import androidx.testutils.RepeatRule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PreviewSwitchCameraStressTest
constructor(implName: String, cameraConfig: CameraXConfig, cameraId: String) :
    SwitchCameraStressTestBase(implName, cameraConfig, cameraId) {

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun switchCamera_checkPreviewInEachTime_withPreviewImageCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE
        switchCamera_checkOutput_repeatedly(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun switchCamera_checkPreviewInEachTime_withPreviewImageCaptureImageAnalysis() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE or BIND_IMAGE_ANALYSIS
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_checkOutput_repeatedly(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun switchCamera_checkPreviewInEachTime_withPreviewVideoCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE
        switchCamera_checkOutput_repeatedly(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun switchCamera_checkPreviewInEachTime_withPreviewVideoCaptureImageCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE or BIND_IMAGE_CAPTURE
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_checkOutput_repeatedly(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun switchCamera_checkPreviewInEachTime_withPreviewVideoCaptureImageAnalysis() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE or BIND_IMAGE_ANALYSIS
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_checkOutput_repeatedly(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterSwitchCameraRepeatedly_withPreviewImageCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE
        switchCamera_repeatedly_thenCheckOutput(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterSwitchCameraRepeatedly_withPreviewImageCaptureImageAnalysis() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE or BIND_IMAGE_ANALYSIS
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_repeatedly_thenCheckOutput(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterSwitchCameraRepeatedly_withPreviewVideoCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE
        switchCamera_repeatedly_thenCheckOutput(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterSwitchCameraRepeatedly_withPreviewVideoCaptureImageCapture() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE or BIND_IMAGE_CAPTURE
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_repeatedly_thenCheckOutput(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterSwitchCameraRepeatedly_withPreviewVideoCaptureImageAnalysis() {
        val useCaseCombination = BIND_PREVIEW or BIND_VIDEO_CAPTURE or BIND_IMAGE_ANALYSIS
        assumeBothLensFacingCamerasSupportUseCaseCombination(camera, useCaseCombination)
        switchCamera_repeatedly_thenCheckOutput(
            cameraId,
            useCaseCombination,
            VERIFICATION_TARGET_PREVIEW,
        )
    }
}
