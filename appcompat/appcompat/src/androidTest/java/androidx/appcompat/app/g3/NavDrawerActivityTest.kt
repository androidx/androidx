/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.appcompat.app.g3

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.NightModeActivity
import androidx.appcompat.test.R
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression test for b/235567649, adapted from Translate's own tests.
 */
@SdkSuppress(minSdkVersion = 18)
@LargeTest
@RunWith(AndroidJUnit4::class)
class NavDrawerActivityTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(
        OldTranslateActivity::class.java,
        /* initialTouchMode = */ false,
        /* launchActivity = */ false
    )

    private lateinit var activity: OldTranslateActivity
    private lateinit var originalScreenOrientation: AndroidTestUtil.ScreenOrientation

    @Before
    fun setUp() {
        activity = activityTestRule.launchActivity(null)
        originalScreenOrientation = AndroidTestUtil.getScreenOrientation(context)
    }

    @After
    fun tearDown() {
        AndroidTestUtil.setScreenOrientation(context, originalScreenOrientation)
    }

    @Test
    fun testSettingsBackRotateDevice() {
        activity.startActivity(
            Intent(context, NightModeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(NightModeActivity.KEY_TITLE, "TopActivity")
            }
        )

        // Starting activity is hidden, wait for it to stop.
        waitUntilState(activity, Lifecycle.State.CREATED)

        // Rotate the screen.
        UITestUtils.rotateScreen(
            InstrumentationRegistry.getInstrumentation(),
            originalScreenOrientation
        )

        // Close the top activity.
        pressBack()

        // Starting activity is in the foreground, wait for it to resume.
        waitUntilState(activity, Lifecycle.State.RESUMED)

        assertIsInHomeActivity()

        // press back, make sure that the app exits
        verifyPressBackAndExitAfterRotation()
    }

    private fun assertIsInHomeActivity() {
        onView(withId(R.id.btn_lang_picker_swap)).check(matches(isDisplayed()))
    }

    private fun verifyPressBackAndExitAfterRotation() {
        // On 5.1, back button doesn't exit the app, appears to be an emulator quirk.
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.LOLLIPOP_MR1) {
            assertThat(UITestUtils.verifyPressBackAndExit()).isTrue()
        }
    }
}
