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
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_CAMERA_ID
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE
import androidx.camera.integration.extensions.IntentExtraKey.INTENT_EXTRA_KEY_EXTENSION_MODE
import androidx.camera.integration.extensions.util.ExtensionsTestUtil
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.extensions.util.ExtensionsTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.StressTestRule
import androidx.camera.testing.waitForIdle
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
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
import com.google.common.truth.Truth.assertThat
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
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    @get:Rule
    val storagePermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)!!

    private lateinit var activityScenario: ActivityScenario<CameraExtensionsActivity>

    private lateinit var initializationIdlingResource: CountingIdlingResource
    private lateinit var previewViewStreamingStateIdlingResource: CountingIdlingResource
    private lateinit var previewViewIdleStateIdlingResource: CountingIdlingResource
    private lateinit var takePictureIdlingResource: CountingIdlingResource

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraId = {0}, extensionMode = {1}")
        @JvmStatic
        fun parameters() = ExtensionsTestUtil.getAllCameraIdExtensionModeCombinations()
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
    fun pauseResumeActivity_checkPreviewInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_PREVIEW)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivity_checkImageCaptureInEachTime() {
        pauseResumeActivity_checkOutput_repeatedly(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivity_checkOutput_repeatedly(
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        launchActivityAndRetrieveIdlingResources()

        for (i in 1..repeatCount) {
            activityScenario.onActivity { it.resetPreviewViewIdleStateIdlingResource() }

            // Pauses and resumes activity
            activityScenario.moveToState(CREATED)
            activityScenario.moveToState(RESUMED)

            previewViewIdleStateIdlingResource.waitForIdle()
            activityScenario.onActivity { it.resetPreviewViewStreamingStateIdlingResource() }

            // Assert: checks PreviewView can enter STREAMING state
            if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                previewViewStreamingStateIdlingResource.waitForIdle()
            }

            // Assert: checks ImageCapture can take a picture
            if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                // Issues take picture.
                Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())

                // Waits for the take picture success callback.
                takePictureIdlingResource.waitForIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT)
    fun checkPreview_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_PREVIEW)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = ExtensionsTestUtil.STRESS_TEST_REPEAT_COUNT)
    fun checkImageCapture_afterPauseResumeActivityRepeatedly() {
        pauseResumeActivityRepeatedly_thenCheckOutput(VERIFICATION_TARGET_IMAGE_CAPTURE)
    }

    private fun pauseResumeActivityRepeatedly_thenCheckOutput(
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        launchActivityAndRetrieveIdlingResources()

        activityScenario.onActivity { it.resetPreviewViewIdleStateIdlingResource() }

        // Pauses and resumes activity 10 times
        for (i in 1..repeatCount) {
            activityScenario.moveToState(CREATED)
            activityScenario.moveToState(RESUMED)
        }

        if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
            previewViewStreamingStateIdlingResource.waitForIdle()
        }

        if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
            // Presses capture button
            Espresso.onView(ViewMatchers.withId(R.id.Picture)).perform(ViewActions.click())

            // Waits for the take picture success callback.
            takePictureIdlingResource.waitForIdle()
        }
    }

    private fun launchActivityAndRetrieveIdlingResources() {
        val intent = ApplicationProvider.getApplicationContext<Context>().packageManager
            .getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)?.apply {
                putExtra(INTENT_EXTRA_KEY_CAMERA_ID, cameraId)
                putExtra(INTENT_EXTRA_KEY_EXTENSION_MODE, extensionMode)
                putExtra(INTENT_EXTRA_KEY_DELETE_CAPTURED_IMAGE, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        activityScenario = ActivityScenario.launch(intent)

        activityScenario.onActivity {
            initializationIdlingResource = it.initializationIdlingResource
            previewViewStreamingStateIdlingResource = it.previewViewStreamingStateIdlingResource
            previewViewIdleStateIdlingResource = it.previewViewIdleStateIdlingResource
            takePictureIdlingResource = it.takePictureIdlingResource
        }

        // Waits for CameraExtensionsActivity's initialization to be complete
        initializationIdlingResource.waitForIdle()

        activityScenario.onActivity {
            assumeTrue(it.isExtensionModeSupported(cameraId, extensionMode))
        }

        // Waits for preview view turned to STREAMING state to make sure that the capture session
        // has been created and the capture stages can be retrieved from the vendor library
        // successfully.
        previewViewStreamingStateIdlingResource.waitForIdle()

        activityScenario.onActivity {
            // Checks that CameraExtensionsActivity's current extension mode is correct.
            assertThat(it.currentExtensionMode).isEqualTo(extensionMode)
        }
    }
}