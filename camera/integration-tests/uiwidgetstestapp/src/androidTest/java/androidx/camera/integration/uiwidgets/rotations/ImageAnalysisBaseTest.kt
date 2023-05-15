/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets.rotations

import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Rule

/**
 * Base class for rotation image analysis tests.
 *
 * The flow of the tests is:
 * - Launch the activity
 * - Wait fo the camera to be set up
 * - Rotate
 * - Wait a couple of frames
 * - Verify the image analysis image rotation
 */
abstract class ImageAnalysisBaseTest<A : CameraActivity>(
    cameraXConfig: String = CameraActivity.CAMERA2_IMPLEMENTATION_OPTION
) {

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = cameraXConfig == CameraActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION,
    )

    @get:Rule
    val useCameraRule = CameraUtil.grantCameraPermissionAndPreTest(
        testCameraRule, CameraUtil.PreTestCameraIdList(
            if (cameraXConfig == CameraActivity.CAMERA2_IMPLEMENTATION_OPTION) {
                Camera2Config.defaultConfig()
            } else {
                CameraPipeConfig.defaultConfig()
            }
        )
    )

    @get:Rule
    val mCameraActivityRules: GrantPermissionRule =
        GrantPermissionRule.grant(*CameraActivity.PERMISSIONS)

    protected val mDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    protected fun setUp(lensFacing: Int) {
        CoreAppTestUtil.assumeCompatibleDevice()
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        // Ensure it's in a natural orientation. This change could delay around 1 sec, please
        // call this earlier before launching the test activity.
        mDevice.setOrientationNatural()

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    protected fun tearDown(): Unit = runBlocking {
        withContext(Dispatchers.Main) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
            cameraProvider.shutdown()[10, TimeUnit.SECONDS]
        }
        mDevice.unfreezeRotation()
    }

    protected inline fun <reified A : CameraActivity> verifyRotation(
        lensFacing: Int,
        cameraXConfig: String = CameraActivity.CAMERA2_IMPLEMENTATION_OPTION,
        rotate: ActivityScenario<A>.() -> Unit,
    ) {
        val activityScenario: ActivityScenario<A> = launchActivity(lensFacing, cameraXConfig)
        activityScenario.use { scenario ->

            // Wait until the camera is set up and analysis starts receiving frames
            scenario.waitOnCameraFrames()

            // Rotate
            rotate(scenario)

            // Reset received camera frames count
            scenario.resetFramesCount()

            // Wait a couple of frames after rotation
            scenario.waitOnCameraFrames()

            // Image rotation is correct if equal to sensor rotation relative to target rotation
            val (sensorToTargetRotation, imageRotationDegrees) = scenario.withActivity {
                Pair(getSensorRotationRelativeToAnalysisTargetRotation(), mAnalysisImageRotation)
            }
            assertWithMessage(
                "The image rotation degrees [$imageRotationDegrees] was expected to" +
                    " be equal to [$sensorToTargetRotation]"
            )
                .that(imageRotationDegrees)
                .isEqualTo(sensorToTargetRotation)
        }
    }

    protected inline fun <reified A : CameraActivity> launchActivity(
        lensFacing: Int,
        cameraXConfig: String,
    ):
        ActivityScenario<A> {
            val intent = Intent(
                ApplicationProvider.getApplicationContext(),
                A::class.java
            ).apply {
                putExtra(CameraActivity.KEY_LENS_FACING, lensFacing)
                putExtra(CameraActivity.KEY_CAMERA_IMPLEMENTATION, cameraXConfig)
                putExtra(CameraActivity.KEY_CAMERA_IMPLEMENTATION_NO_HISTORY, true)
            }
            return ActivityScenario.launch<A>(intent)
        }

    protected inline fun <reified A : CameraActivity> ActivityScenario<A>.waitOnCameraFrames() {
        val analysisRunning = withActivity { mAnalysisRunning }
        assertWithMessage("Timed out waiting on image analysis frames on $analysisRunning")
            .that(analysisRunning.tryAcquire(IMAGES_COUNT, TIMEOUT, TimeUnit.SECONDS))
            .isTrue()
    }

    protected inline fun <reified A : CameraActivity> ActivityScenario<A>.resetFramesCount() {
        withActivity { mAnalysisRunning.drainPermits() }
    }

    companion object {
        protected const val IMAGES_COUNT = 30
        protected const val TIMEOUT = 20L

        @JvmStatic
        protected val lensFacingList =
            arrayOf(CameraSelector.LENS_FACING_BACK, CameraSelector.LENS_FACING_FRONT)

        @JvmStatic
        protected val cameraXConfigList = arrayOf(
            CameraActivity.CAMERA2_IMPLEMENTATION_OPTION,
            CameraActivity.CAMERA_PIPE_IMPLEMENTATION_OPTION
        )

        @JvmStatic
        lateinit var testCameraRule: CameraUtil.PreTestCamera

        @BeforeClass
        @JvmStatic
        fun classSetup() {
            testCameraRule = CameraUtil.PreTestCamera()
        }
    }
}