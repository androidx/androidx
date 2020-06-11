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

package androidx.camera.integration.uiwidgets.rotations.imageanalysis

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
import android.content.res.Configuration
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.integration.uiwidgets.rotations.CameraActivity
import androidx.camera.integration.uiwidgets.rotations.UnlockedOrientationActivity
import androidx.camera.integration.uiwidgets.rotations.UnlockedOrientationActivity.Companion.mCreated
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * On device rotation, an Activity with an unlocked orientation is destroyed, and a new instance
 * of it is created.
 * <p>
 * Testing that the camera use cases produce images with the correct rotation as a device
 * running an Activity that has an unlocked orientation rotates can be done as follows:
 * - Launching the Activity
 * - Waiting for the camera setup to finish
 * - Setting a new orientation to the Activity. This results in the system destroying it, and
 * creating a new instance of it with the specified orientation.
 * - Waiting for the camera setup to finish
 * - Verifying the use cases' image rotation
 */
@RunWith(Parameterized::class)
@LargeTest
class UnlockedOrientationActivityTest(private val lensFacing: Int, private val orientation: Int) {

    companion object {
        private val ORIENTATION_MAP = hashMapOf(
            SCREEN_ORIENTATION_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            SCREEN_ORIENTATION_REVERSE_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            SCREEN_ORIENTATION_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE,
            SCREEN_ORIENTATION_REVERSE_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE
        )

        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, orientation={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(CameraSelector.LENS_FACING_BACK, SCREEN_ORIENTATION_PORTRAIT))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, SCREEN_ORIENTATION_REVERSE_PORTRAIT))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, SCREEN_ORIENTATION_LANDSCAPE))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, SCREEN_ORIENTATION_PORTRAIT))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, SCREEN_ORIENTATION_REVERSE_PORTRAIT))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, SCREEN_ORIENTATION_LANDSCAPE))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, SCREEN_ORIENTATION_REVERSE_LANDSCAPE))
        }
    }

    @get:Rule
    val mCameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Before
    fun setUp() {
        CoreAppTestUtil.assumeCompatibleDevice()
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        // Initialize CameraX
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Camera2Config.defaultConfig()
        CameraX.initialize(context, config).get()

        // Clear the device's UI and ensure it's in a natural orientation
        CoreAppTestUtil.clearDeviceUI(InstrumentationRegistry.getInstrumentation())
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.setOrientationNatural()
    }

    @After
    fun tearDown() {
        CameraX.shutdown().get()
    }

    @Test
    fun analyzedImageRotation() {
        launchActivity(lensFacing).use { scenario ->

            // Wait until camera is set up, and Analyzer is running
            val analysisRunning1 = scenario.withActivity { mAnalysisRunning }
            assertThat(analysisRunning1.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

            // Rotate to specified orientation
            if (scenario.rotate(orientation)) {

                // Wait until the activity is rotated (and recreated)
                assertThat(mCreated.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

                // Wait until camera is set up, and Analyzer is running
                val analysisRunning2 = scenario.withActivity { mAnalysisRunning }
                assertThat(analysisRunning1).isNotSameInstanceAs(analysisRunning2)
                assertThat(analysisRunning2.tryAcquire(5, TimeUnit.SECONDS)).isTrue()
            }

            // Image rotation is correct if equal to sensor rotation relative to target rotation
            val (sensorToTargetRotation, imageRotationDegrees) = scenario.withActivity {
                Pair(getSensorRotationRelativeToAnalysisTargetRotation(), mAnalysisImageRotation)
            }
            assertThat(sensorToTargetRotation).isEqualTo(imageRotationDegrees)
        }
    }

    private fun launchActivity(lensFacing: Int): ActivityScenario<UnlockedOrientationActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            UnlockedOrientationActivity::class.java
        )
        intent.putExtra(CameraActivity.KEY_LENS_FACING, lensFacing)
        return ActivityScenario.launch<UnlockedOrientationActivity>(intent)
    }

    private fun ActivityScenario<UnlockedOrientationActivity>.rotate(orientation: Int): Boolean {
        val currentOrientation = withActivity { resources.configuration.orientation }
        val didRotate = ORIENTATION_MAP[orientation] != currentOrientation
        // Rotate
        onActivity { activity ->
            mCreated = Semaphore(0)
            activity.requestedOrientation = orientation
        }
        return didRotate
    }
}
