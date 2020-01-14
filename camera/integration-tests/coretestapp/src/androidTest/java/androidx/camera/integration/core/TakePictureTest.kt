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

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.testing.CoreAppTestUtil.clearDeviceUI
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@androidx.test.filters.Suppress
@LargeTest
@RunWith(AndroidJUnit4::class)
class TakePictureTest {
    private val mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mIntent = mContext.packageManager.getLaunchIntentForPackage(BASIC_SAMPLE_PACKAGE)

    @get:Rule
    var mCameraPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
    @get:Rule
    var mStoragePermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Rule
    @JvmField
    var mActivityRule = ActivityTestRule(
        CameraXActivity::class.java, true,
        false
    )

    @Before
    fun setUp() {
        assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()

        // Clear the device UI before start each test.
        clearDeviceUI(InstrumentationRegistry.getInstrumentation())

        // Launch Activity
        mActivityRule.launchActivity(mIntent)

        // Register idlingResource so Espresso will synchronize with it.
        IdlingRegistry.getInstance().register(mActivityRule.activity.imageSavedIdlingResource)
    }

    @After
    fun tearDown() {
        // Unregister idlingResource, no need to synchronize with it anymore.
        if (mActivityRule.activity != null) {
            IdlingRegistry.getInstance().unregister(mActivityRule.activity.imageSavedIdlingResource)
        }
        pressBackAndReturnHome()
        mActivityRule.finishActivity()
    }

    // Take a photo, wait for callback via imageSavedIdlingResource resource.
    @Test
    fun testPictureButton() {
        checkPreviewReady()

        val imageCapture: ImageCapture? = mActivityRule.activity.imageCapture

        if (imageCapture != null) {
            onView(withId(R.id.Picture)).perform(click())
        }
    }

    private fun pressBackAndReturnHome() {
        mDevice.pressBack()

        // Returns to Home to restart next test.
        mDevice.pressHome()
    }

    private fun checkPreviewReady() {
        onView(withId(R.id.textureView))
    }

    companion object {
        const val BASIC_SAMPLE_PACKAGE = "androidx.camera.integration.core"
    }
}
