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
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.integration.core.CameraXActivity.BIND_IMAGE_CAPTURE
import androidx.camera.integration.core.CameraXActivity.BIND_PREVIEW
import androidx.camera.integration.core.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.core.util.StressTestUtil
import androidx.camera.integration.core.util.StressTestUtil.LARGE_STRESS_TEST_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.STRESS_TEST_OPERATION_REPEAT_COUNT
import androidx.camera.integration.core.util.StressTestUtil.launchCameraXActivityAndWaitForPreviewReady
import androidx.camera.integration.core.waitForViewfinderIdle
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.testing.impl.StressTestRule
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.RepeatRule
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = 21)
class ImageCaptureStressTest(
    val implName: String,
    val cameraConfig: CameraXConfig,
    val cameraId: String
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(cameraConfig)
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )

    @get:Rule
    val labTest: LabTestRule = LabTestRule()

    @get:Rule
    val repeatRule = RepeatRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var cameraProvider: ProcessCameraProvider

    companion object {
        @ClassRule
        @JvmField
        val stressTest = StressTestRule()

        @JvmStatic
        @Parameterized.Parameters(name = "config = {0}, cameraId = {2}")
        fun data() = StressTestUtil.getAllCameraXConfigCameraIdCombinations()
    }

    @Before
    fun setup(): Unit = runBlocking {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        CoreAppTestUtil.assumeNotUntestableFrontCamera(cameraId)

        // For running the ImageCaptureStressTest, we need to get the target test camera to check
        // whether the testing use case combination can be supported to skip unsupported cases. For
        // the purpose, we force configure the target testing config first
        // (Camera2Config/CameraPipeConfig) and gets the CameraProvider instance in the setup()
        // function. Then, the activity launched afterward will also run on the same config
        // environment. The setup config environment will be cleared after
        // CameraProvider#shutdown() is called in the tearDown() function.
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider = ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
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
        device.waitForIdle(StressTestUtil.HOME_TIMEOUT_MS)
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivityRepeatedly_checkImageCaptureInEachTime() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        // Pauses, resumes the activity, and then checks whether ImageCapture can capture images
        // successfully.
        with(activityScenario) {
            use {
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    // Go through pause/resume then check again for view to get frames then idle.
                    moveToState(Lifecycle.State.CREATED)
                    moveToState(Lifecycle.State.RESUMED)
                    waitForViewfinderIdle()

                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivityRepeatedly_takePictureInEachTime_withRandomDelay() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        // Pauses, resumes the activity, and then checks whether ImageCapture can capture images
        // successfully with random delay.
        with(activityScenario) {
            use {
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    // Go through pause/resume then check again for view to get frames then idle.
                    moveToState(Lifecycle.State.CREATED)
                    moveToState(Lifecycle.State.RESUMED)
                    waitForViewfinderIdle()

                    randomDelaySeconds()
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun pauseResumeActivityRepeatedly_thenTakePicture() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        // Pauses and resumes the activity repeatedly, and then checks whether ImageCapture can
        // capture images successfully.
        with(activityScenario) {
            use {
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    // Go through pause/resume then check again for view to get frames then idle.
                    moveToState(Lifecycle.State.CREATED)
                    moveToState(Lifecycle.State.RESUMED)
                    waitForViewfinderIdle()
                }

                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun launchActivity_thenTakeMultiplePictures() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        with(activityScenario) {
            use {
                // Checks whether multiple images can be captured successfully
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun launchActivity_thenTakeMultiplePictures_withRandomDelay() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        with(activityScenario) {
            use {
                // Checks whether multiple images can be captured successfully with random delay
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    randomDelaySeconds()
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    /**
     * Randomly delay for seconds between 1 and the specified number. Default is 3 seconds.
     */
    private fun randomDelaySeconds(maxDelaySeconds: Int = 3) = runBlocking {
        require(maxDelaySeconds > 0) {
            "The specified max delay seconds value should be larger than 1."
        }
        val randomDelayDuration = (Random.nextInt(maxDelaySeconds) + 1).toLong()
        delay(TimeUnit.SECONDS.toMillis(randomDelayDuration))
    }

    @LabTestRule.LabTestOnly
    @Test
    @RepeatRule.Repeat(times = LARGE_STRESS_TEST_REPEAT_COUNT)
    fun launchActivity_thenTakeMultiplePictures_withoutWaitingPreviousResults() {
        val useCaseCombination = BIND_PREVIEW or BIND_IMAGE_CAPTURE

        // Launches CameraXActivity and wait for the preview ready.
        val activityScenario =
            launchCameraXActivityAndWaitForPreviewReady(cameraId, useCaseCombination)

        with(activityScenario) {
            use {
                // Checks whether multiple images can be captured successfully
                repeat(STRESS_TEST_OPERATION_REPEAT_COUNT) {
                    takePictureAndWaitForImageSavedIdle(3)
                }
            }
        }
    }
}
