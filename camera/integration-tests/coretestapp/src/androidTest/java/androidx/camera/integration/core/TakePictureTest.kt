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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.AfterClass
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class TakePictureTest {
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
        fun tearDown() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val cameraProvider = ProcessCameraProvider.getInstance(context)[10, TimeUnit.SECONDS]
            cameraProvider.shutdown()[10, TimeUnit.SECONDS]
        }
    }

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    // Take a photo, wait for callback via imageSavedIdlingResource resource.
    @Test
    fun testPictureButton() {
        with(ActivityScenario.launch(CameraXActivity::class.java)) {
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
        with(ActivityScenario.launch(CameraXActivity::class.java)) {
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
}
