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
import android.view.Surface
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.integration.uiwidgets.rotations.CameraActivity
import androidx.camera.integration.uiwidgets.rotations.OrientationConfigChangesOverriddenActivity
import androidx.camera.integration.uiwidgets.rotations.get
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

/**
 * On device rotation, an Activity that overrides orientation configuration changes isn't
 * recreated, but it's UI is updated to match the screen rotation ({@link Display#getRotation()}).
 * <p>
 * Testing that the camera use cases produce images with the correct rotation as a device running
 * an Activity that overrides orientation configuration changes rotates can be done as follows:
 * - Launching the Activity
 * - Waiting for the camera setup to finish
 * - Simulating a physical device rotation by calling {@link UiAutomation#setRotation(int)}.
 * - Waiting for the camera use cases to receive new images
 * - Verifying the use cases' image rotation
 */
@RunWith(Parameterized::class)
@LargeTest
class OrientationConfigChangesOverriddenActivityTest(
    private val lensFacing: Int,
    private val rotation: Int
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}, rotation={1}")
        fun data() = mutableListOf<Array<Any?>>().apply {
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_0))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_90))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_180))
            add(arrayOf(CameraSelector.LENS_FACING_BACK, Surface.ROTATION_270))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_0))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_90))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_180))
            add(arrayOf(CameraSelector.LENS_FACING_FRONT, Surface.ROTATION_270))
        }
    }

    @get:Rule
    val mCameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Before
    fun setUp() {
        CoreAppTestUtil.assumeCompatibleDevice()
        Assume.assumeTrue(CameraUtil.hasCameraWithLensFacing(lensFacing))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = Camera2Config.defaultConfig()
        CameraX.initialize(context, config).get()
    }

    @After
    fun tearDown() {
        if (CameraX.isInitialized()) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { CameraX.unbindAll() }
        }
        CameraX.shutdown().get()
    }

    @Test
    fun analyzedImageRotation() {
        launchActivity(lensFacing).use { scenario ->

            // Wait until camera is set up, and Analyzer is running
            val analysisRunning = scenario.get { mAnalysisRunning }
            assertThat(analysisRunning.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

            // Rotate device
            rotateDevice(rotation)

            // Wait for Analyzer to receive new images
            assertThat(analysisRunning.tryAcquire(5, TimeUnit.SECONDS)).isTrue()

            // Image rotation is correct if equal to sensor rotation relative to target rotation
            val (sensorToTargetRotation, imageRotationDegrees) = scenario.get {
                Pair(getSensorRotationRelativeToAnalysisTargetRotation(), mAnalysisImageRotation)
            }
            assertThat(sensorToTargetRotation).isEqualTo(imageRotationDegrees)
        }
    }

    private fun launchActivity(lensFacing: Int):
            ActivityScenario<OrientationConfigChangesOverriddenActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            OrientationConfigChangesOverriddenActivity::class.java
        )
        intent.putExtra(CameraActivity.KEY_LENS_FACING, lensFacing)
        return ActivityScenario.launch<OrientationConfigChangesOverriddenActivity>(intent)
    }

    /**
     * Simulates a physical device rotation for an Activity that overrides orientation
     * configuration changes. This triggers {@link DisplayManager.DisplayListener#onDisplayChanged}.
     */
    private fun rotateDevice(rotation: Int) {
        InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(rotation)
    }
}
