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

package androidx.camera.integration.extensions.camera2extensions

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.integration.extensions.Camera2ExtensionsActivity
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.util.Camera2ExtensionsTestUtil
import androidx.camera.integration.extensions.util.EXTENSIONS_TEST_APP_PACKAGE
import androidx.camera.integration.extensions.util.waitForCaptureSessionConfiguredIdle
import androidx.camera.integration.extensions.util.waitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewIdle
import androidx.camera.integration.extensions.utils.Camera2ExtensionsUtil.isCamera2ExtensionModeSupported
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.StressTestRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Assume
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Auto validation tests for Camera2 Extensions with [Camera2ExtensionsActivity]
 */
@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 31)
class Camera2ExtensionsActivityTest(
    private val cameraId: String,
    private val extensionMode: Int
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @Before
    fun setup() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clears the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Uses the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
    }

    @After
    fun tearDown() {
        // Unfreezes rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
    }

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = Camera2ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
    }

    @LabTestRule.LabTestOnly
    @Test
    fun checkPreviewUpdated() {
        val activityScenario = launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
            cameraId,
            extensionMode
        )
        with(activityScenario) { // Launches activity
            use { // Ensures that ActivityScenario is cleaned up properly
                // Waits for preview to receive enough frames for its IdlingResource to idle.
                waitForPreviewIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canCaptureSingleImage() {
        val activityScenario = launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
            cameraId,
            extensionMode
        )
        with(activityScenario) { // Launches activity
            use { // Ensures that ActivityScenario is cleaned up properly
                // Triggers the capture function and waits for the image being saved
                waitForImageSavedIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun checkPreviewUpdated_afterPauseResume() {
        val activityScenario = launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
            cameraId,
            extensionMode
        )
        with(activityScenario) { // Launches activity
            use { // Ensures that ActivityScenario is cleaned up properly
                // Waits for preview to receive enough frames for its IdlingResource to idle.
                waitForPreviewIdle()

                // Pauses and resumes the activity
                moveToState(Lifecycle.State.CREATED)
                moveToState(Lifecycle.State.RESUMED)

                // Waits for preview to receive enough frames again
                waitForPreviewIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canCaptureImage_afterPauseResume() {
        val activityScenario = launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
            cameraId,
            extensionMode
        )
        with(activityScenario) { // Launches activity
            use { // Ensures that ActivityScenario is cleaned up properly
                // Triggers the capture function and waits for the image being saved
                waitForImageSavedIdle()

                // Pauses and resumes the activity
                moveToState(Lifecycle.State.CREATED)
                moveToState(Lifecycle.State.RESUMED)

                // Waits for the capture session configured again after resuming the activity
                waitForCaptureSessionConfiguredIdle()

                // Triggers the capture function and waits for the image being saved again
                waitForImageSavedIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun canCaptureMultipleImages() {
        val activityScenario = launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
            cameraId,
            extensionMode
        )
        with(activityScenario) { // Launches activity
            use { // Ensures that ActivityScenario is cleaned up properly
                repeat(5) {
                    // Triggers the capture function and waits for the image being saved
                    waitForImageSavedIdle()
                }
            }
        }
    }

    private fun launchCamera2ExtensionsActivityAndWaitForCaptureSessionConfigured(
        cameraId: String,
        extensionMode: Int
    ): ActivityScenario<Camera2ExtensionsActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assumeTrue(isCamera2ExtensionModeSupported(context, cameraId, extensionMode))
        val intent = context.packageManager
            .getLaunchIntentForPackage(EXTENSIONS_TEST_APP_PACKAGE)!!.apply {
                putExtra(INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
                putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
                setClassName(context, Camera2ExtensionsActivity::class.java.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val activityScenario: ActivityScenario<Camera2ExtensionsActivity> =
            ActivityScenario.launch(intent)

        // Waits for the capture session being configured
        activityScenario.waitForCaptureSessionConfiguredIdle()

        return activityScenario
    }
}