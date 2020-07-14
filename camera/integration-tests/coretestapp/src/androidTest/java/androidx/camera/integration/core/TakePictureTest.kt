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

import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.CoreAppTestUtil.clearDeviceUI
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TakePictureTest {
    @get:Rule
    var mCameraPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get:Rule
    var mStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    @get:Rule
    var mRecordAudioRule =
        GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO)

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        // Clear the device UI before start each test.
        clearDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    // Take a photo, wait for callback via imageSavedIdlingResource resource.
    @Test
    fun testPictureButton() {
        ActivityScenario.launch(CameraXActivity::class.java).use {
            it?.onActivity { activity ->
                IdlingRegistry.getInstance().register(activity.viewIdlingResource)
                IdlingRegistry.getInstance().register(activity.imageSavedIdlingResource)
            }
            onView(withId(R.id.Picture)).perform(click())
            onView(withId(R.id.viewFinder))
            it?.onActivity { activity ->
                IdlingRegistry.getInstance().unregister(activity.viewIdlingResource)
                IdlingRegistry.getInstance().unregister(activity.imageSavedIdlingResource)
                activity.deleteSessionImages()
            }
        }
    }

    // Initiate photo capture but close the lifecycle before photo completes to trigger
    // onError path.
    @Test
    fun testTakePictureAndRestartWhileCapturing() { // Launch activity check for view idle.
        ActivityScenario.launch(CameraXActivity::class.java).use {
            checkForViewIdle(it)
            onView(withId(R.id.Picture)).perform(click())
            // Immediately .recreate() this allows the test to reach the onError callback path.
            // Note, moveToState(DESTROYED) doesn't trigger the same code path.
            checkForViewIdle(it!!.recreate())
        }
    }

    private fun checkForViewIdle(activityScenario: ActivityScenario<CameraXActivity>):
            ActivityScenario<CameraXActivity>? {
        activityScenario.onActivity { activity ->
            IdlingRegistry.getInstance().register(activity.viewIdlingResource)
        }
        // Check the activity launched and Preview displays frames.
        onView(withId(R.id.viewFinder))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.onActivity { activity ->
            IdlingRegistry.getInstance().unregister(activity.viewIdlingResource)
        }
        return activityScenario
    }
}
