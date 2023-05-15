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

package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests basic UI operation when using CoreTest app.
 */
@LargeTest
@RunWith(Parameterized::class)
class BasicUITest(
    private val implName: String,
    private val cameraConfig: String
) {

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        CameraUtil.PreTestCameraIdList(
            if (implName == Camera2Config::class.simpleName) {
                Camera2Config.defaultConfig()
            } else {
                CameraPipeConfig.defaultConfig()
            }
        )
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    @get:Rule
    val detectLeaks = DetectLeaksAfterTestSuccess(TAG)

    private val launchIntent = Intent(
        ApplicationProvider.getApplicationContext(),
        CameraXActivity::class.java
    ).apply {
        putExtra(CameraXActivity.INTENT_EXTRA_CAMERA_IMPLEMENTATION, cameraConfig)
        putExtra(CameraXActivity.INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY, true)
    }

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        device.setOrientationNatural()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        // Returns to Home to restart next test.
        device.pressHome()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        cameraProvider.shutdown()[10, TimeUnit.SECONDS]
    }

    @Test
    fun testAnalysisButton() {
        ActivityScenario.launch<CameraXActivity>(launchIntent).use { scenario ->
            // Arrange.
            // Wait for the Activity to be created and Preview appears before starting the test.
            scenario.waitForViewfinderIdle()

            // Skips the test if ImageAnalysis can't be enabled when launching the activity. Some
            // devices use YUV stream to take image and Preview + ImageCapture + ImageAnalysis
            // might not be able to bind together.
            assumeTrue(scenario.withActivity { imageAnalysis != null })

            // Click to disable the imageAnalysis use case.
            if (scenario.withActivity { imageAnalysis != null }) {
                Espresso.onView(withId(R.id.AnalysisToggle)).perform(ViewActions.click())

                // Wait for Preview start to ensure the UseCase is bound to the camera.
                scenario.waitForViewfinderIdle()
            }

            if (scenario.withActivity { imageAnalysis == null }) {
                // Act. ImageAnalysis is null(disable), do click to enable use imageAnalysis case.
                Espresso.onView(withId(R.id.AnalysisToggle)).perform(ViewActions.click())

                // Assert. Verify the ImageAnalysis is processing output.
                scenario.waitForImageAnalysisIdle()
            }
        }
    }

    @Test
    fun testPreviewButton() {
        ActivityScenario.launch<CameraXActivity>(launchIntent).use { scenario ->
            // Arrange.
            // Wait for the Activity to be created and Preview appears before starting the test.
            scenario.waitForViewfinderIdle()
            // Click to disable the preview use case.
            if (scenario.withActivity { preview != null }) {
                Espresso.onView(withId(R.id.PreviewToggle)).perform(ViewActions.click())

                // Try to take a Picture to ensure the UseCase is bound to the camera.
                scenario.takePictureAndWaitForImageSavedIdle()
            }

            if (scenario.withActivity { preview == null }) {
                // Act. Preview is null(disable), do click to enable preview use case.
                Espresso.onView(withId(R.id.PreviewToggle)).perform(ViewActions.click())

                // Assert. Verify the Preview is processing output.
                scenario.waitForViewfinderIdle()
            }
        }
    }

    companion object {
        private const val IDLE_TIMEOUT_MS = 3_000L
        private const val TAG = "BasicUITest"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(
                Camera2Config::class.simpleName,
                CameraXViewModel.CAMERA2_IMPLEMENTATION_OPTION
            ),
            arrayOf(
                CameraPipeConfig::class.simpleName,
                CameraXViewModel.CAMERA_PIPE_IMPLEMENTATION_OPTION
            )
        )
    }
}