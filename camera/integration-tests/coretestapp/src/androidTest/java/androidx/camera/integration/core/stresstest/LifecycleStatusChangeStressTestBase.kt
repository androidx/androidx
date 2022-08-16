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

import android.Manifest
import android.content.Context
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.integration.core.recordVideoAndWaitForVideoSavedIdle
import androidx.camera.integration.core.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.core.util.StressTestUtil.HOME_TIMEOUT_MS
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_IMAGE_ANALYSIS
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_IMAGE_CAPTURE
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_PREVIEW
import androidx.camera.integration.core.util.StressTestUtil.VERIFICATION_TARGET_VIDEO_CAPTURE
import androidx.camera.integration.core.util.StressTestUtil.createCameraSelectorById
import androidx.camera.integration.core.util.StressTestUtil.launchCameraXActivityAndWaitForPreviewReady
import androidx.camera.integration.core.waitForImageAnalysisIdle
import androidx.camera.integration.core.waitForViewfinderIdle
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.LabTestRule
import androidx.camera.testing.StressTestRule
import androidx.camera.testing.fakes.FakeLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
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
import org.junit.ClassRule
import org.junit.Rule
import org.junit.runners.Parameterized

abstract class LifecycleStatusChangeStressTestBase(
    val cameraId: String
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private lateinit var cameraProvider: ProcessCameraProvider
    protected lateinit var camera: Camera
    private lateinit var cameraIdCameraSelector: CameraSelector

    companion object {
        @ClassRule
        @JvmField val stressTest = StressTestRule()

        @JvmStatic
        @get:Parameterized.Parameters(name = "cameraId = {0}")
        val parameters: Collection<String>
            get() = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
    }

    @Before
    fun setup(): Unit = runBlocking {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        CoreAppTestUtil.assumeNotUntestableFrontCamera(cameraId)
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()

        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        cameraIdCameraSelector = createCameraSelectorById(cameraId)

        camera = withContext(Dispatchers.Main) {
            cameraProvider.bindToLifecycle(FakeLifecycleOwner(), cameraIdCameraSelector)
        }
    }

    @After
    fun tearDown(): Unit = runBlocking {
        if (::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.shutdown()[10000, TimeUnit.MILLISECONDS]
            }
        }

        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(HOME_TIMEOUT_MS)
    }

    /**
     * Repeatedly pause, resume the activity and checks the use cases' capture functions can work.
     */
    protected fun pauseResumeActivity_checkOutput_repeatedly(
        cameraId: String,
        useCaseCombination: Int,
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        // Pauses, resumes the activity, and then checks the test target use case can capture
        // images successfully.
        with(activityScenario) {
            use {
                for (i in 1..repeatCount) {
                    // Go through pause/resume then check again for view to get frames then idle.
                    moveToState(Lifecycle.State.CREATED)
                    moveToState(Lifecycle.State.RESUMED)

                    // Checks Preview can receive frames if it is the test target use case.
                    if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                        waitForViewfinderIdle()
                    }

                    // Checks ImageCapture can take a picture if it is the test target use case.
                    if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                        takePictureAndWaitForImageSavedIdle()
                    }

                    // Checks VideoCapture can record a video if it is the test target use case.
                    if (verificationTarget.and(VERIFICATION_TARGET_VIDEO_CAPTURE) != 0) {
                        recordVideoAndWaitForVideoSavedIdle()
                    }

                    // Checks ImageAnalysis can receive frames if it is the test target use case.
                    if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
                        waitForImageAnalysisIdle()
                    }
                }
            }
        }
    }

    /**
     * Pause and resume the activity repeatedly, and then checks the use cases' capture functions
     * can work.
     */
    protected fun pauseResumeActivityRepeatedly_thenCheckOutput(
        cameraId: String,
        useCaseCombination: Int,
        verificationTarget: Int,
        repeatCount: Int = STRESS_TEST_OPERATION_REPEAT_COUNT
    ) {
        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        // Pauses, resumes the activity repeatedly, and then checks the test target use case can
        // capture images successfully.
        with(activityScenario) {
            use {
                for (i in 1..repeatCount) {
                    moveToState(Lifecycle.State.CREATED)
                    moveToState(Lifecycle.State.RESUMED)
                }

                // Checks Preview can receive frames if it is the test target use case.
                if (verificationTarget.and(VERIFICATION_TARGET_PREVIEW) != 0) {
                    waitForViewfinderIdle()
                }

                // Checks ImageCapture can take a picture if it is the test target use case.
                if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_CAPTURE) != 0) {
                    takePictureAndWaitForImageSavedIdle()
                }

                // Checks VideoCapture can record a video if it is the test target use case.
                if (verificationTarget.and(VERIFICATION_TARGET_VIDEO_CAPTURE) != 0) {
                    recordVideoAndWaitForVideoSavedIdle()
                }

                // Checks ImageAnalysis can receive frames if it is the test target use case.
                if (verificationTarget.and(VERIFICATION_TARGET_IMAGE_ANALYSIS) != 0) {
                    waitForImageAnalysisIdle()
                }
            }
        }
    }
}