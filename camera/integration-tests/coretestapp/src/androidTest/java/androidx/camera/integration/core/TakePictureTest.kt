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
import android.content.Context
import android.content.Intent
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.impl.CameraPipeConfigTestRule
import androidx.camera.testing.impl.CameraUtil
import androidx.camera.testing.impl.CoreAppTestUtil
import androidx.camera.testing.impl.waitForIdle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TakePictureTest(
    private val implName: String,
    private val cameraConfig: String
) {
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

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
        cameraProvider.shutdown()[10, TimeUnit.SECONDS]
    }

    // Take a photo, wait for callback via imageSavedIdlingResource resource.
    @Test
    fun testPictureButton() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) {
            use { // Ensure ActivityScenario is cleaned up properly.
                waitForViewfinderIdle()
                takePictureAndWaitForImageSavedIdle()
            }
        }
    }

    // Initiate photo capture but close the lifecycle before photo completes to trigger
    // onError path.
    @Test
    fun testTakePictureAndRestartWhileCapturing() { // Launch activity check for view idle.
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) {
            use { // Ensure ActivityScenario is cleaned up properly.
                waitForViewfinderIdle()
                onView(withId(R.id.Picture)).perform(click())
                // Immediately .recreate() this allows the test to reach the onError callback path.
                // Note, moveToState(DESTROYED) doesn't trigger the same code path.
                it!!.recreate()
                waitForViewfinderIdle()
            }
        }
    }

    @Test
    fun testTakePictureQuickly() {
        with(ActivityScenario.launch<CameraXActivity>(launchIntent)) {
            use { // Ensure ActivityScenario is cleaned up properly.

                // Arrange, wait for camera starts processing output.
                waitForViewfinderIdle()

                // Act. continuously take 5 photos.
                withActivity {
                    cleanTakePictureErrorMessage()
                    imageSavedIdlingResource
                }.apply {
                    for (i in 5 downTo 1) {
                        onView(withId(R.id.Picture)).perform(click())
                    }
                    waitForIdle()
                }

                // Assert, there's no error message.
                withActivity {
                    deleteSessionImages()
                    lastTakePictureErrorMessage ?: ""
                }.let { errorMessage ->
                    assertWithMessage("Fail to take picture: $errorMessage").that(errorMessage)
                        .isEmpty()
                }
            }
        }
    }
}
