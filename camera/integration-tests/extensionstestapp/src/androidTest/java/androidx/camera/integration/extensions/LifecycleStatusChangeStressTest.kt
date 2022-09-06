/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.integration.extensions

import android.Manifest
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.HOME_TIMEOUT_MS
import androidx.camera.integration.extensions.util.pauseAndResumeActivity
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.LabTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.RepeatRule
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Stress tests to verify that Preview and ImageCapture can work well when changing lifecycle
 * status.
 */
@LargeTest
@RunWith(Parameterized::class)
class LifecycleStatusChangeStressTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val storagePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()

        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        // Checks whether the extension mode can be supported first before launching the activity.
        CameraXExtensionsTestUtil.assumeExtensionModeSupported(
            extensionsManager,
            cameraId,
            extensionMode
        )
    }

    @After
    fun tearDown(): Unit = runBlocking {
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]
        withContext(Dispatchers.Main) {
            cameraProvider.shutdown()
        }

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]
        extensionsManager.shutdown()

        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(HOME_TIMEOUT_MS)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = CameraXExtensionsTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivity_checkPreviewInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_PREVIEW)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = CameraXExtensionsTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivity_checkImageCaptureInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivity_checkOutput_repeatedly(
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        var activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        try {
            activityScenario.waitForPreviewViewStreaming()

            repeat(repeatCount) {
                activityScenario = activityScenario.pauseAndResumeActivity(cameraId, extensionMode)
                if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                    activityScenario.waitForPreviewViewStreaming()
                }

                if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                    activityScenario.takePictureAndWaitForImageSavedIdle()
                }
            }
        } finally {
            // Finish the activity
            activityScenario.close()
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = CameraXExtensionsTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_PREVIEW)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = CameraXExtensionsTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT)
    fun checkImageCapture_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivityRepeatedly_thenCheckOutput(
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        var activityScenario = launchCameraExtensionsActivity(cameraId, extensionMode)

        activityScenario.waitForPreviewViewStreaming()

        repeat(repeatCount) {
            activityScenario = activityScenario.pauseAndResumeActivity(cameraId, extensionMode)
            activityScenario.waitForPreviewViewStreaming()
        }

        with(activityScenario) {
            use {
                if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                    waitForPreviewViewStreaming()
                }

                if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }
}