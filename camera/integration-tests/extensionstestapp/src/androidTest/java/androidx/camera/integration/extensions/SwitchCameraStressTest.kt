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
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.StressTestRule
import androidx.camera.testing.waitForIdle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.RepeatRule
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.extensions"

/**
 * Stress tests to verify that Preview and ImageCapture can work well when switching cameras.
 */
@LargeTest
@RunWith(Parameterized::class)
class SwitchCameraStressTest(private val extensionMode: Int) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    @get:Rule
    val storagePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    private lateinit var activityScenario: ActivityScenario<CameraExtensionsActivity>

    private lateinit var initializationIdlingResource: CountingIdlingResource
    private lateinit var previewViewIdlingResource: CountingIdlingResource
    private lateinit var takePictureIdlingResource: CountingIdlingResource

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "extensionMode = {0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ExtensionMode.BOKEH,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.FACE_RETOUCH,
            ExtensionMode.AUTO
        )
    }

    @Before
    fun setUp() {
        assumeTrue(ExtensionsTestUtil.isTargetDeviceAvailableForExtensions())
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before starting the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
    }

    @After
    fun tearDown() {
        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()

        if (::activityScenario.isInitialized) {
            activityScenario.onActivity { it.finish() }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT)
    fun switchCameraTenTimes_canCaptureImageInEachTime() {
        launchActivityAndRetrieveIdlingResources()

        for (i in 1..STRESS_TEST_OPERATION_REPEAT_COUNT) {
            // Issues take picture.
            Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())

            // Waits for the take picture success callback.
            takePictureIdlingResource.waitForIdle()

            previewViewIdlingResource.increment()

            activityScenario.onActivity {
                // Switches camera
                it.switchCameras()
                // Switches to the target testing extension mode as possible because some extension
                // modes may not be supported in some lens facing of cameras.
                it.switchToExtensionMode(extensionMode)
            }

            // Waits for preview view turned to STREAMING state after switching camera
            previewViewIdlingResource.waitForIdle()
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT)
    fun canCaptureImage_afterSwitchCameraTenTimes() {
        launchActivityAndRetrieveIdlingResources()

        // Pauses and resumes activity 10 times
        for (i in 1..STRESS_TEST_OPERATION_REPEAT_COUNT) {
            activityScenario.onActivity {
                // Switches camera
                it.switchCameras()
                // Switches to the target testing extension mode as possible because some extension
                // modes may not be supported in some lens facing of cameras.
                it.switchToExtensionMode(extensionMode)
            }
        }

        // Presses capture button
        Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())

        // Waits for the take picture success callback.
        takePictureIdlingResource.waitForIdle()
    }

    private fun launchActivityAndRetrieveIdlingResources() {
        val intent = ApplicationProvider.getApplicationContext<Context>().packageManager
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)?.apply {
                putExtra(CameraExtensionsActivity.INTENT_EXTRA_CAMERA_ID, "0")
                putExtra(CameraExtensionsActivity.INTENT_EXTRA_EXTENSION_MODE, extensionMode)
                putExtra(CameraExtensionsActivity.INTENT_EXTRA_DELETE_CAPTURED_IMAGE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        activityScenario = ActivityScenario.launch(intent)

        activityScenario.onActivity {
            initializationIdlingResource = it.mInitializationIdlingResource
            previewViewIdlingResource = it.mPreviewViewIdlingResource
            takePictureIdlingResource = it.mTakePictureIdlingResource
        }

        // Waits for CameraExtensionsActivity's initialization to be complete
        initializationIdlingResource.waitForIdle()

        // Only runs the test when at least one of the back or front cameras support the target
        // testing extension mode
        activityScenario.onActivity {
            assumeTrue(
                it.isExtensionModeSupported(CameraSelector.DEFAULT_BACK_CAMERA, extensionMode) ||
                    it.isExtensionModeSupported(CameraSelector.DEFAULT_FRONT_CAMERA, extensionMode)
            )
        }

        // Waits for preview view turned to STREAMING state to make sure that the capture session
        // has been created and the capture stages can be retrieved from the vendor library
        // successfully.
        previewViewIdlingResource.waitForIdle()
    }
}
