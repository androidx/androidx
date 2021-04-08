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

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.camera.camera2.Camera2Config
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraX
import androidx.camera.core.CameraXConfig
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.activity.Camera2TestActivity
import androidx.camera.testing.activity.CameraXTestActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.TimeUnit

/** Tests for [CameraX] which varies use case combinations to run. */
@LargeTest
@RunWith(Parameterized::class)
class CameraDisconnectTest(
    private val implName: String,
    private val cameraConfig: CameraXConfig
) {

    @get:Rule
    val cameraRule = CameraUtil.grantCameraPermissionAndPreTest()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf(Camera2Config::class.simpleName, Camera2Config.defaultConfig()),
            arrayOf(CameraPipeConfig::class.simpleName, CameraPipeConfig.defaultConfig())
        )
    }

    @Suppress("DEPRECATION")
    @get:Rule
    val cameraXTestActivityRule = androidx.test.rule.ActivityTestRule(
        CameraXTestActivity::class.java, true, false
    )

    @Suppress("DEPRECATION")
    @get:Rule
    val camera2ActivityRule = androidx.test.rule.ActivityTestRule(
        Camera2TestActivity::class.java, true, false
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        IdlingPolicies.setIdlingResourceTimeout(10, TimeUnit.SECONDS)
        CoreAppTestUtil.assumeCompatibleDevice()
        CoreAppTestUtil.assumeCanTestCameraDisconnect()
        runBlocking {
            CameraX.initialize(context, cameraConfig).get(10, TimeUnit.SECONDS)
        }

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @After
    fun tearDown() {
        if (cameraXTestActivityRule.activity != null) {
            cameraXTestActivityRule.finishActivity()
        }

        if (camera2ActivityRule.activity != null) {
            camera2ActivityRule.finishActivity()
        }

        runBlocking {
            CameraX.shutdown().get(10, TimeUnit.SECONDS)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Known issue, checkout b/147393563.
    fun testCameraDisconnect() {

        // TODO(b/184603071): Migrate the ActivityTestRule to ActivityScenario
        // Launch CameraX test activity
        with(cameraXTestActivityRule.launchActivity(Intent())) {

            // Wait for preview to become active
            waitForCameraXPreview()

            // Get id of camera opened by CameraX test activity
            Truth.assertThat(cameraId).isNotNull()

            // Launch Camera2 test activity. It should cause the camera to disconnect from CameraX.
            val intent = Intent(
                context,
                Camera2TestActivity::class.java
            ).apply {
                putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, cameraId)
            }
            camera2ActivityRule.launchActivity(intent)

            // Wait for preview to become active
            camera2ActivityRule.activity.waitForCamera2Preview()

            // Close Camera2 test activity, and verify the CameraX Preview resumes successfully.
            camera2ActivityRule.finishActivity()

            // Verify the CameraX Preview can resume successfully.
            waitForCameraXPreview()
        }
    }

    private fun CameraXTestActivity.waitForCameraXPreview() {
        waitFor(previewReady)
    }

    private fun Camera2TestActivity.waitForCamera2Preview() {
        waitFor(mPreviewReady)
    }

    private fun waitFor(idlingResource: IdlingResource) {
        IdlingRegistry.getInstance().register(idlingResource)
        Espresso.onIdle()
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}