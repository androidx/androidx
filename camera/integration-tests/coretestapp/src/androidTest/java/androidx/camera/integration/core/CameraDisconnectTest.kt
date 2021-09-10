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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
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

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var cameraXActivityScenario: ActivityScenario<CameraXTestActivity>

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
        if (::cameraXActivityScenario.isInitialized) {
            cameraXActivityScenario.close()
        }

        runBlocking {
            CameraX.shutdown().get(10, TimeUnit.SECONDS)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M) // Known issue, checkout b/147393563.
    fun testCameraDisconnect() {

        // Launch CameraX test activity
        cameraXActivityScenario = ActivityScenario.launch(CameraXTestActivity::class.java)
        with(cameraXActivityScenario) {

            // Wait for preview to become active
            waitForCameraXPreview()

            // Launch Camera2 test activity. It should cause the camera to disconnect from CameraX.
            val intent = Intent(
                context,
                Camera2TestActivity::class.java
            ).apply {
                putExtra(Camera2TestActivity.EXTRA_CAMERA_ID, getCameraId())
            }

            CoreAppTestUtil.launchActivity(
                InstrumentationRegistry.getInstrumentation(),
                Camera2TestActivity::class.java,
                intent
            )?.apply {
                // Wait for preview to become active
                waitForCamera2Preview()

                // Close Camera2 test activity, and verify the CameraX Preview resumes successfully.
                finish()
            }

            // Verify the CameraX Preview can resume successfully.
            waitForCameraXPreview()
        }
    }

    private fun ActivityScenario<CameraXTestActivity>.getCameraId(): String {
        var cameraId: String? = null
        onActivity { cameraId = it.cameraId }
        return cameraId!!
    }

    private fun ActivityScenario<CameraXTestActivity>.waitForCameraXPreview() {
        var idlingResource: IdlingResource? = null
        onActivity { idlingResource = it.previewReady }

        waitFor(idlingResource!!)
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