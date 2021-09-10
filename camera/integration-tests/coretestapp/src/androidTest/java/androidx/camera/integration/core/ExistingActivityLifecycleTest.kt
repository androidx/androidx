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
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.RepeatRule
import androidx.testutils.withActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

private const val HOME_TIMEOUT_MS = 3000L
private const val ROTATE_TIMEOUT_MS = 2000L

// Test application lifecycle when using CameraX.
@RunWith(AndroidJUnit4::class)
@LargeTest
class ExistingActivityLifecycleTest {
    private val mDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val mUseCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val mPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )

    companion object {
        @AfterClass
        @JvmStatic
        fun shutdownCameraX() {
            CameraX.shutdown().get(10, TimeUnit.SECONDS)
        }
    }

    @Before
    fun setup() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
        // Use the natural orientation throughout these tests to ensure the activity isn't
        // recreated unexpectedly. This will also freeze the sensors until
        // mDevice.unfreezeRotation() in the tearDown() method. Any simulated rotations will be
        // explicitly initiated from within the test.
        mDevice.setOrientationNatural()
    }

    @After
    fun tearDown() {
        // Unfreeze rotation so the device can choose the orientation via its own policy. Be nice
        // to other tests :)
        mDevice.unfreezeRotation()
        mDevice.pressHome()
        mDevice.waitForIdle(HOME_TIMEOUT_MS)
    }

    // Check if Preview screen is updated or not, after Destroy-Create lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterDestroyRecreate() {
        with(ActivityScenario.launch(CameraXActivity::class.java)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()
                // Destroy previous activity, launch new activity and check for view idle.
                recreate()
                waitForViewfinderIdle()
            }
        }
    }

    // Check if Preview screen is updated or not, after Stop-Resume lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterStopResume() {
        with(ActivityScenario.launch(CameraXActivity::class.java)) { // Launch activity.
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

        with(ActivityScenario.launch(CameraXActivity::class.java)) { // Launch activity.
            use { // Ensure ActivityScenario is cleaned up properly
                // Wait for viewfinder to receive enough frames for its IdlingResource to idle.
                waitForViewfinderIdle()

                // Switch camera.
                Espresso.onView(ViewMatchers.withId(R.id.direction_toggle))
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

    // Check if Preview screen is updated or not, after rotate device, and Stop-Resume lifecycle.
    @Test
    @RepeatRule.Repeat(times = 5)
    fun checkPreviewUpdatedAfterRotateDeviceAndStopResume() {
        with(ActivityScenario.launch(CameraXActivity::class.java)) { // Launch activity.
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

    private fun rotateDeviceLeftAndWait() {
        // Create an ActivityMonitor to explicitly wait for the activity to be recreated after
        // rotating the device.
        val monitor =
            Instrumentation.ActivityMonitor(CameraXActivity::class.java.name, null, false)
        InstrumentationRegistry.getInstrumentation().addMonitor(monitor)
        mDevice.setOrientationLeft()
        // Wait for the rotation to complete
        InstrumentationRegistry.getInstrumentation().waitForMonitorWithTimeout(
            monitor,
            ROTATE_TIMEOUT_MS
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }
}
