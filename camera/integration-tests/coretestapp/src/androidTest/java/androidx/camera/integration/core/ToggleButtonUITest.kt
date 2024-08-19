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
package androidx.camera.integration.core

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.integration.core.idlingresource.WaitForViewToShow
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.InternalTestConvenience.useInCameraTest
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import junit.framework.AssertionFailedError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Test toggle buttons in CoreTestApp. */
@LargeTest
@RunWith(Parameterized::class)
class ToggleButtonUITest(private val implName: String, private val cameraConfig: String) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
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
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(
            active = implName == CameraPipeConfig::class.simpleName,
        )

    private val launchIntent =
        Intent(ApplicationProvider.getApplicationContext(), CameraXActivity::class.java).apply {
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
    fun tearDown(): Unit =
        runBlocking(Dispatchers.Main) {
            // Returns to Home to restart next test.
            device.pressHome()
            device.waitForIdle(IDLE_TIMEOUT_MS)
            // Unfreeze rotation so the device can choose the orientation via its own policy. Be
            // nice
            // to other tests :)
            device.unfreezeRotation()

            val context = ApplicationProvider.getApplicationContext<Context>()
            val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }

    @Test
    fun testFlashToggleButton() {
        ActivityScenario.launch<CameraXActivity>(launchIntent).useInCameraTest { scenario ->
            // Arrange.
            WaitForViewToShow(R.id.constraintLayout).wait()
            assumeTrue(isButtonEnabled(R.id.flash_toggle))
            val useCase = scenario.withActivity { imageCapture }
            // There are 3 different states of flash mode: ON, OFF and AUTO.
            // By pressing flash mode toggle button, the flash mode would switch to the next state.
            // The flash mode would loop in following sequence: OFF -> AUTO -> ON -> OFF.
            // Act.
            @ImageCapture.FlashMode val mode1 = useCase.flashMode
            onView(withId(R.id.flash_toggle)).perform(click())
            @ImageCapture.FlashMode val mode2 = useCase.flashMode
            onView(withId(R.id.flash_toggle)).perform(click())
            @ImageCapture.FlashMode val mode3 = useCase.flashMode

            // Assert.
            // After the switch, the mode2 should be different from mode1.
            assertThat(mode2).isNotEqualTo(mode1)
            // The mode3 should be different from first and second time.
            assertThat(mode3).isNoneOf(mode2, mode1)
        }
    }

    @Test
    fun testTorchToggleButton() {
        ActivityScenario.launch<CameraXActivity>(launchIntent).useInCameraTest { scenario ->
            WaitForViewToShow(R.id.constraintLayout).wait()
            assumeTrue(isButtonEnabled(R.id.torch_toggle))
            val cameraInfo = scenario.withActivity { cameraInfo!! }
            val isTorchOn = cameraInfo.isTorchOn()
            onView(withId(R.id.torch_toggle)).perform(click())
            assertThat(cameraInfo.isTorchOn()).isNotEqualTo(isTorchOn)
            // By pressing the torch toggle button two times, it should switch back to original
            // state.
            onView(withId(R.id.torch_toggle)).perform(click())
            assertThat(cameraInfo.isTorchOn()).isEqualTo(isTorchOn)
        }
    }

    @Test
    fun testSwitchCameraToggleButton() {
        assumeTrue(
            "Ignore the camera switch test since there's no front camera.",
            CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT)
        )
        ActivityScenario.launch<CameraXActivity>(launchIntent).useInCameraTest { scenario ->
            WaitForViewToShow(R.id.direction_toggle).wait()
            assertThat(scenario.withActivity { preview }).isNotNull()
            for (i in 0..4) {
                scenario.waitForViewfinderIdle()
                // Click switch camera button.
                onView(withId(R.id.direction_toggle)).perform(click())
            }
        }
    }

    private fun CameraInfo.isTorchOn(): Boolean = torchState.value == TorchState.ON

    private fun isButtonEnabled(resource: Int): Boolean {
        return try {
            onView(withId(resource)).check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            // View is in hierarchy
            true
        } catch (e: AssertionFailedError) {
            // View is not in hierarchy
            false
        } catch (e: Exception) {
            // View is not in hierarchy
            false
        }
    }

    private fun IdlingResource.wait() {
        IdlingRegistry.getInstance().register(this)
        onIdle()
        IdlingRegistry.getInstance().unregister(this)
    }

    companion object {
        private const val IDLE_TIMEOUT_MS = 1_000L

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
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
