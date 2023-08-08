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
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil
import androidx.camera.integration.extensions.util.CameraXExtensionsTestUtil.launchCameraExtensionsActivity
import androidx.camera.integration.extensions.util.HOME_TIMEOUT_MS
import androidx.camera.integration.extensions.util.takePictureAndWaitForImageSavedIdle
import androidx.camera.integration.extensions.util.waitForPreviewViewStreaming
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.StressTestRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
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
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Stress tests to verify that Preview and ImageCapture can work well when switching modes.
 */
@LargeTest
@RunWith(Parameterized::class)
class SwitchAvailableModesStressTest(private val cameraId: String) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private var firstSupportedExtensionMode: Int = ExtensionMode.NONE

    companion object {
        @ClassRule
        @JvmField
        val stressTest = StressTestRule()

        @Parameterized.Parameters(name = "cameraId = {0}")
        @JvmStatic
        fun parameters() = CameraUtil.getBackwardCompatibleCameraIdListOrThrow()
    }

    private var isTestStarted = false

    @Before
    fun setup() {
        assumeTrue(CameraUtil.deviceHasCamera())
        assumeTrue(CameraXExtensionsTestUtil.isTargetDeviceAvailableForExtensions())

        val cameraProvider =
            ProcessCameraProvider.getInstance(context)[10000, TimeUnit.MILLISECONDS]

        val extensionsManager = ExtensionsManager.getInstanceAsync(
            context,
            cameraProvider
        )[10000, TimeUnit.MILLISECONDS]

        // Checks whether any extension mode can be supported first before launching the activity.
        CameraXExtensionsTestUtil.assumeAnyExtensionModeSupported(
            extensionsManager,
            cameraId
        )

        firstSupportedExtensionMode =
            CameraXExtensionsTestUtil.getFirstSupportedExtensionMode(extensionsManager, cameraId)

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
    fun switchModeTenTimes_canCaptureImageInEachTime() {
        val activityScenario = launchCameraExtensionsActivity(cameraId, firstSupportedExtensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()

                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    withActivity { resetPreviewViewStreamingStateIdlingResource() }

                    // Switches to next available mode
                    Espresso.onView(ViewMatchers.withId(R.id.PhotoToggle))
                        .perform(ViewActions.click())

                    // Waits for preview view turned to STREAMING state after switching mode
                    waitForPreviewViewStreaming()

                    // Waits for the take picture success callback.
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    @Test
    fun canCaptureImage_afterSwitchModeTenTimes() {
        val activityScenario = launchCameraExtensionsActivity(cameraId, firstSupportedExtensionMode)

        with(activityScenario) {
            use {
                waitForPreviewViewStreaming()

                repeat(CameraXExtensionsTestUtil.getStressTestRepeatingCount()) {
                    withActivity { resetPreviewViewStreamingStateIdlingResource() }

                    // Switches to next available mode
                    Espresso.onView(ViewMatchers.withId(R.id.PhotoToggle))
                        .perform(ViewActions.click())

                    // Waits for preview view turned to STREAMING state after switching mode
                    waitForPreviewViewStreaming()
                }

                // Waits for the take picture success callback.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }
}
