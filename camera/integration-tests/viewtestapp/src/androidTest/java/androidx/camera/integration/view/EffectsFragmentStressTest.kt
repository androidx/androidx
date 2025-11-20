/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.integration.view

import android.Manifest
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraXConfig
import androidx.camera.core.Logger
import androidx.camera.integration.view.TestUtil.assertPreviewStreamingState
import androidx.camera.integration.view.TestUtil.getFragment
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraAvailabilityUtil.assumeDeviceHasFrontCamera
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.LabTestRule
import androidx.camera.view.PreviewView
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Instrument tests for [EffectsFragment]. */
@LargeTest
@RunWith(Parameterized::class)
class EffectsFragmentStressTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig,
) {
    @get:Rule
    val cameraPipeConfigTestRule =
        CameraPipeConfigTestRule(active = implName == CameraPipeConfig::class.simpleName)

    @get:Rule
    val useCameraRule =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraControllerFragmentTest.testCameraRule,
            CameraUtil.PreTestCameraIdList(cameraConfig),
        )

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    @get:Rule val labTest: LabTestRule = LabTestRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var fragment: EffectsFragment
    private lateinit var fragmentScenario: FragmentScenario<EffectsFragment>

    @Before
    fun setup() {
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(instrumentation)
        ProcessCameraProvider.configureInstance(cameraConfig)
        cameraProvider =
            ProcessCameraProvider.getInstance(ApplicationProvider.getApplicationContext())[
                    10000, TimeUnit.MILLISECONDS]
        fragmentScenario =
            FragmentScenario.launchInContainer(
                EffectsFragment::class.java,
                null,
                R.style.AppTheme,
                null,
            )
        fragment = fragmentScenario.getFragment()
    }

    @After
    fun tearDown() {
        if (::fragmentScenario.isInitialized) {
            fragmentScenario.moveToState(Lifecycle.State.DESTROYED)
        }
        if (::cameraProvider.isInitialized) {
            cameraProvider.shutdownAsync()[10000, TimeUnit.MILLISECONDS]
        }
    }

    @LabTestRule.LabTestOnly
    @Test
    fun toggleCameraLatencyTest() {
        assumeDeviceHasFrontCamera()

        // Arrange: use COMPATIBLE mode to get an accurate measurement.
        instrumentation.runOnMainSync {
            fragment.previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        val startTimeMillis = System.currentTimeMillis()
        fragment.assertPreviewStreamingState(PreviewView.StreamState.STREAMING, instrumentation)

        // Act: toggle camera for 10 times.
        val numberOfToggles = 10
        for (i in 0 until numberOfToggles) {
            instrumentation.runOnMainSync { fragment.toggleCamera() }
            fragment.assertPreviewStreamingState(PreviewView.StreamState.IDLE, instrumentation)
            fragment.assertPreviewStreamingState(PreviewView.StreamState.STREAMING, instrumentation)
        }

        // Record the average duration of the test, TODO: add a proper benchmark test for this
        val averageDuration = (System.currentTimeMillis() - startTimeMillis) / numberOfToggles
        val tag = "toggleCameraLatencyTest"
        Logger.d(tag, "Effects pipeline performance profiling. duration: [$averageDuration]")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() =
            listOf(
                arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
                arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig()),
            )
    }
}
