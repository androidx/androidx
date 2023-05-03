/*
 * Copyright 2019 The Android Open Source Project
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
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraPipeConfigTestRule
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CameraUtil.PreTestCameraIdList
import androidx.camera.testing.CoreAppTestUtil
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry.getInstance
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.RepeatRule
import androidx.testutils.withActivity
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val HOME_TIMEOUT_MS = 3000L
private const val ROTATE_TIMEOUT_MS = 2000L

// Test application lifecycle when using CameraX.
@RunWith(Parameterized::class)
@LargeTest
class ExistingActivityLifecycleTest(
    private val implName: String,
    private val cameraConfig: String
) {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val useCamera = CameraUtil.grantCameraPermissionAndPreTest(
        PreTestCameraIdList(Camera2Config.defaultConfig())
    )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    @get:Rule
    val repeatRule = RepeatRule()

    @get:Rule
    val cameraPipeConfigTestRule = CameraPipeConfigTestRule(
        active = implName == CameraPipeConfig::class.simpleName,
    )

    private val launchIntent = Intent(
        ApplicationProvider.getApplicationContext(),
        CameraXActivity::class.java
    ).apply {
        putExtra(CameraXActivity.INTENT_EXTRA_CAMERA_IMPLEMENTATION, cameraConfig)
        putExtra(CameraXActivity.INTENT_EXTRA_CAMERA_IMPLEMENTATION_NO_HISTORY, true)
    }

    @Before
    fun setup() {
        Assume.assumeFalse(
            "Ignore Cuttlefish",
            Build.MODEL.contains("Cuttlefish")
        )
        Assume.assumeFalse("See b/152082918, Wembley Api30 has a libjpeg issue which causes" +
            " the test failure.",
            Build.MODEL.equals("wembley", ignoreCase = true) && Build.VERSION.SDK_INT <= 30)
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
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
    fun tearDown() {
        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        device.unfreezeRotation()
        device.pressHome()
        device.waitForIdle(HOME_TIMEOUT_MS)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        cameraProvider.shutdown()[10, TimeUnit.SECONDS]
    }

    // Check if Preview screen is updated or not, after Destroy-Create lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterDestroyRecreate() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()
                // Destroy previous activity, launch new activity and check for view idle.
                recreate()
                waitForViewfinderIdle()
            }
        }
    }

    // Check ImageCapture to take pictures successfully, after the Destroy-Create lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkImageCaptureAfterDestroyRecreate() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use {
                // Arrange.
                // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Act. Destroy previous activity, launch new activity and check for view idle.
                recreate()
                waitForViewfinderIdle()

                // Assert.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    // Check if Preview screen is updated or not, after Stop-Resume lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterStopResume() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()
                // Go through pause/resume then check again for view to get frames then idle.
                moveToState(CREATED)

                withActivity { resetViewIdlingResource() }

                moveToState(RESUMED)

                waitForViewfinderIdle()
                // Go through pause/resume then check again for view to get frames then idle,
                // the second pass is used to protect against previous observed issues.
                moveToState(CREATED)

                withActivity { resetViewIdlingResource() }

                moveToState(RESUMED)

                waitForViewfinderIdle()
            }
        }
    }

    // Check ImageCapture to take pictures successfully, after Stop-Resume lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkImageCaptureAfterStopResume() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use {
                // Arrange.
                // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                for (i in 0..1) {
                    // Act. Go through pause/resume then check.
                    moveToState(CREATED)
                    moveToState(RESUMED)

                    // Assert.
                    takePictureAndWaitForImageSavedIdle()
                }
            }
        }
    }

    // Check if Preview screen is updated or not, after toggling camera,
    // then a Destroy-Create lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterToggleCameraAndStopResume() = runBlocking {
        // check have front camera
        Assume.assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                CameraSelector.LENS_FACING_FRONT
            )
        )

        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Switch camera.
                onView(withId(R.id.direction_toggle))
                    .perform(ViewActions.click())

                // Check front camera is now idle
                withActivity { resetViewIdlingResource() }
                waitForViewfinderIdle()

                // Go through pause/resume then check again for view to get frames then idle.
                moveToState(CREATED)
                withActivity { resetViewIdlingResource() }
                moveToState(RESUMED)
                waitForViewfinderIdle()
            }
        }
    }

    // Check ImageCapture to take pictures successfully, after toggling camera,
    // then a Destroy-Create lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkImageCaptureAfterToggleCameraAndStopResume() = runBlocking {
        // check have front camera
        Assume.assumeTrue(
            CameraUtil.hasCameraWithLensFacing(
                CameraSelector.LENS_FACING_FRONT
            )
        )

        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use {
                // Arrange.
                // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Act. Switch camera.
                onView(withId(R.id.direction_toggle))
                    .perform(ViewActions.click())

                // Assert.
                takePictureAndWaitForImageSavedIdle()

                // Act. Go through pause/resume then check again.
                moveToState(CREATED)
                moveToState(RESUMED)

                // Assert.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    // Check if Preview screen is updated or not, after rotate device, and Stop-Resume lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterRotateDeviceAndStopResume() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Rotate to the orientation left of natural and wait for the activity to be
                // recreated.
                rotateDeviceLeftAndWait()

                // Get idling from the re-created activity.
                withActivity { resetViewIdlingResource() }
                waitForViewfinderIdle()

                moveToState(CREATED)
                withActivity { resetViewIdlingResource() }
                moveToState(RESUMED)
                waitForViewfinderIdle()
            }
        }
    }

    // Check ImageCapture to take pictures successfully, after rotate device, and Stop-Resume
    // lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkImageCaptureAfterRotateDeviceAndStopResume() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) { // Launch activity.
            use {
                // Arrange.
                // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Act.
                // Rotate to the orientation left of natural and wait for the activity to be
                // recreated.
                rotateDeviceLeftAndWait()

                // Get idling from the re-created activity.
                withActivity { resetViewIdlingResource() }
                waitForViewfinderIdle()
                // Go through pause/resume then check again.
                moveToState(CREATED)
                moveToState(RESUMED)

                // Assert.
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    private fun rotateDeviceLeftAndWait() {
        // Create an ActivityMonitor to explicitly wait for the activity to be recreated after
        // rotating the device.
        val monitor =
            Instrumentation.ActivityMonitor(CameraXActivity::class.java.name, null, false)
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)
        device.setOrientationLeft()
        // Wait for the rotation to complete
        InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(
            monitor,
            ROTATE_TIMEOUT_MS
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun checkPreviewUpdatedWithNewInstance() {
        ActivityScenario.launchActivityForResult<CameraXActivity>(
            launchIntent
        ).use { firstActivity ->
            // Arrange. Check the 1st activity Preview.
            firstActivity.waitForViewfinderIdle()

            // Act. Make the 1st Activity stopped and create new Activity.
            device.pressHome()
            device.waitForIdle(HOME_TIMEOUT_MS)
            val secondActivity = CoreAppTestUtil.launchActivity(
                InstrumentationRegistry.getInstrumentation(),
                CameraXActivity::class.java,
                Intent(
                    ApplicationProvider.getApplicationContext(),
                    CameraXActivity::class.java
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )!!

            // Assert. Verify the preview of the New activity start successfully.
            try {
                secondActivity.resetViewIdlingResource()
                secondActivity.viewIdlingResource.also { idlingResource ->
                    try {
                        getInstance().register(idlingResource)
                        // Check the activity launched and Preview displays frames.
                        onView(withId(R.id.viewFinder)).check(matches(isDisplayed()))
                    } finally {
                        // Always release the idling resource, in case of timeout exceptions.
                        getInstance().unregister(idlingResource)
                    }
                }
            } finally {
                secondActivity.finish()
            }
        }
    }

    companion object {

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
