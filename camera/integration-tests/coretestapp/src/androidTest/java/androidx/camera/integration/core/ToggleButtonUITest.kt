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
import android.view.View
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.InternalTestConvenience.useInCameraTest
import androidx.camera.testing.impl.RequireForegroundRule
import androidx.lifecycle.Observer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Test toggle buttons in CoreTestApp. */
@LargeTest
@RunWith(Parameterized::class)
class ToggleButtonUITest(private val implName: String) {
    @get:Rule
    val requireForegroundRule = RequireForegroundRule {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
    }

    @get:Rule
    val useCamera =
        CameraUtil.grantCameraPermissionAndPreTestAndPostTest(
            CameraUtil.PreTestCameraIdList(Camera2Config.defaultConfig())
        )

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
        )

    private val launchIntent =
        Intent(ApplicationProvider.getApplicationContext(), CameraXActivity::class.java)

    @Before
    fun setUp() {
        requireForegroundRule.deferCleanup {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
            cameraProvider.shutdownAsync()[10, TimeUnit.SECONDS]
        }
    }

    @Test
    fun testFlashToggleButton() {
        ActivityScenario.launch<CameraXActivity>(launchIntent).useInCameraTest { scenario ->
            // Arrange.
            scenario.waitForViewfinderIdleDirect()
            assumeTrue(
                "Flash toggle button is not enabled",
                scenario.isButtonEnabled(R.id.flash_toggle),
            )
            val boundImageCapture =
                scenario.withActivity { listOfNotNull(imageCapture) }.singleOrNull()
            assumeTrue("ImageCapture is not bound", boundImageCapture != null)
            val useCase = checkNotNull(boundImageCapture)
            // There are 3 different states of flash mode: ON, OFF and AUTO.
            // By pressing flash mode toggle button, the flash mode would switch to the next state.
            // The flash mode would loop in following sequence: OFF -> AUTO -> ON -> OFF.
            // Act.
            @ImageCapture.FlashMode val mode1 = useCase.flashMode
            scenario.clickView(R.id.flash_toggle)
            @ImageCapture.FlashMode val mode2 = useCase.flashMode
            scenario.clickView(R.id.flash_toggle)
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
            scenario.waitForViewfinderIdleDirect()
            assumeTrue(
                "Torch toggle button is not enabled",
                scenario.isButtonEnabled(R.id.torch_toggle),
            )
            val boundCameraInfo = scenario.withActivity { listOfNotNull(cameraInfo) }.singleOrNull()
            assumeTrue("CameraInfo is not available", boundCameraInfo != null)
            val cameraInfo = checkNotNull(boundCameraInfo)
            val isTorchOn = scenario.withActivity { cameraInfo.isTorchOn() }
            scenario.clickView(R.id.torch_toggle)
            awaitTorchState(cameraInfo, !isTorchOn)
            // By pressing the torch toggle button two times, it should switch back to original
            // state.
            scenario.clickView(R.id.torch_toggle)
            awaitTorchState(cameraInfo, isTorchOn)
        }
    }

    @Test
    fun testSwitchCameraToggleButton() {
        assumeTrue(
            "Ignore the camera switch test since there's no front camera.",
            CameraUtil.hasCameraWithLensFacing(CameraSelector.LENS_FACING_FRONT),
        )
        ActivityScenario.launch<CameraXActivity>(launchIntent).useInCameraTest { scenario ->
            scenario.waitForViewfinderIdleDirect()
            assertThat(scenario.withActivity { preview != null }).isTrue()
            for (i in 0..4) {
                // Click switch camera button.
                scenario.switchCameraAndWaitForViewfinderIdleDirect()
            }
        }
    }

    private fun CameraInfo.isTorchOn(): Boolean = torchState.value == TorchState.ON

    private fun awaitTorchState(cameraInfo: CameraInfo, expectedOn: Boolean) {
        val expectedState = if (expectedOn) TorchState.ON else TorchState.OFF
        val latch = CountDownLatch(1)
        val observer =
            Observer<Int> { state ->
                if (state == expectedState) {
                    latch.countDown()
                }
            }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            cameraInfo.torchState.observeForever(observer)
        }
        try {
            assertWithMessage("Torch did not become $expectedState within 5s")
                .that(latch.await(5, TimeUnit.SECONDS))
                .isTrue()
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                cameraInfo.torchState.removeObserver(observer)
            }
        }
    }

    private fun ActivityScenario<CameraXActivity>.isButtonEnabled(resource: Int): Boolean =
        withActivity {
            findViewById<View>(resource)?.let { it.visibility == View.VISIBLE && it.isEnabled } ==
                true
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(arrayOf(Camera2Config::class.simpleName))
    }
}
