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
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.HOME_TIMEOUT_MS
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.integration.extensions.utils.CameraIdExtensionModePair
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
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
class LifecycleStatusChangeStressTest(private val config: CameraIdExtensionModePair) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    companion object {
        @Parameterized.Parameters(name = "config = {0}")
        @JvmStatic
        fun parameters() = CameraXExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    private var isTestStarted = false
    @Before
    fun setup() {
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        CoreAppTestUtil.assumeCompatibleDevice()
        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        // Checks whether the extension mode can be supported first before launching the activity.
        CameraXExtensionsTestUtil.assumeExtensionModeSupported(
            extensionsManager,
            config.cameraId,
            config.extensionMode
        )

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
        isTestStarted = true
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

        if (isTestStarted) {
            // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
            // to other tests :)
            device.unfreezeRotation()
            device.pressHome()
            device.waitForIdle(HOME_TIMEOUT_MS)
        }
    }

    @Test
    fun pauseResumeActivity_checkPreviewInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_PREVIEW)
    }

    @Test
    fun pauseResumeActivity_checkImageCaptureInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivity_checkOutput_repeatedly(
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
    ) {
        val activityScenario = launchCameraExtensionsActivity(config.cameraId, config.extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()

                repeat(repeatCount) {
                    withActivity {
                        resetPreviewViewIdleStateIdlingResource()
                        resetPreviewViewStreamingStateIdlingResource()
                    }
                    moveToState(Lifecycle.State.CREATED)
                    waitForPreviewViewIdle()
                    moveToState(Lifecycle.State.RESUMED)

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

    @Test
    fun checkPreview_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_PREVIEW)
    }

    @Test
    fun checkImageCapture_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivityRepeatedly_thenCheckOutput(
        verificationTarget: Int,
        repeatCount: Int = CameraXExtensionsTestUtil.getStressTestRepeatingCount()
    ) {
        val activityScenario = launchCameraExtensionsActivity(config.cameraId, config.extensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()

                repeat(repeatCount) {
                    withActivity {
                        resetPreviewViewIdleStateIdlingResource()
                        resetPreviewViewStreamingStateIdlingResource()
                    }
                    moveToState(Lifecycle.State.CREATED)
                    waitForPreviewViewIdle()
                    moveToState(Lifecycle.State.RESUMED)
                    waitForPreviewViewStreaming()
                }

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
