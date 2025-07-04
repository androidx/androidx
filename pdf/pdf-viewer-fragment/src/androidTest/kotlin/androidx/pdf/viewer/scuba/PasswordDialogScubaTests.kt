/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.viewer.scuba

import android.content.pm.ActivityInfo
import android.os.Build
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.viewer.FragmentUtils.scenarioLoadDocument
import androidx.pdf.viewer.TestPdfViewerFragment
import androidx.pdf.viewer.fragment.R
import androidx.pdf.viewer.scuba.ScubaConstants.FILE_PASSWORD_DIALOG_KEYBOARD_LANDSCAPE
import androidx.pdf.viewer.scuba.ScubaConstants.FILE_PASSWORD_DIALOG_KEYBOARD_PORTRAIT
import androidx.pdf.viewer.scuba.ScubaConstants.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PASSWORD_PROTECTED_DOCUMENT_FILE = "sample-protected.pdf"

/**
 * Scuba tests for the [androidx.pdf.viewer.PdfPasswordDialog] to verify its visual rendering and
 * placement, including interaction with the soft keyboard, in different orientations.
 *
 * These tests focus purely on visual regression and do not cover functional aspects like submitting
 * correct/incorrect passwords or dialog dismissal behavior.
 */
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
@SdkSuppress(minSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@LargeTest
class PasswordDialogScubaTests {

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragment>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED,
            )
        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    /**
     * Verifies the visual rendering of the PasswordDialog in portrait orientation. This includes
     * the dialog's placement and its interaction with the soft keyboard.
     */
    @Test
    fun passwordDialog_keyboardLayoutDisplayedInPortraitOrientation() {
        // Load a password-protected document and wait for the initial loading to clear.
        // This will automatically trigger the PasswordDialog and the soft keyboard.
        scenarioLoadDocument(
            scenario = scenario,
            filename = PASSWORD_PROTECTED_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(R.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        // Decrement idling resource to indicate loading done
        // Manually decrementing idling resource because in this case the callback never arrives
        // unless a password is entered, causing Espresso to timeout without this.
        scenario.onFragment { fragment ->
            fragment.pdfLoadingIdlingResource.decrement()
            assertNull(fragment.documentError)
        }

        onView(withId(androidx.pdf.R.id.password)).inRoot(RootMatchers.isDialog()).perform(click())
        onView(withId(androidx.pdf.R.id.password))
            .inRoot(RootMatchers.isDialog())
            .perform(typeText("password"))

        // Capture a screenshot. This image will include the password dialog,
        // and its contents
        assertFullScreenshot(screenshotRule, FILE_PASSWORD_DIALOG_KEYBOARD_PORTRAIT)
    }

    /**
     * Verifies the visual rendering of the PasswordDialog in landscape orientation. This includes
     * the dialog's placement and its interaction with the soft keyboard.
     */
    @Test
    fun passwordDialog_keyboardLayoutDisplayedInLandscapeOrientation() {
        // Load a password-protected document and wait for the initial loading to clear.
        // This will automatically trigger the PasswordDialog and the soft keyboard.
        scenarioLoadDocument(
            scenario = scenario,
            filename = PASSWORD_PROTECTED_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ) {
            // Loading view assertion
            onView(withId(R.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        // Decrement idling resource to indicate loading done
        // Manually decrementing idling resource because in this case the callback never arrives
        // unless a password is entered, causing Espresso to timeout without this.
        scenario.onFragment { fragment ->
            fragment.pdfLoadingIdlingResource.decrement()
            assertNull(fragment.documentError)
        }

        onView(withId(androidx.pdf.R.id.password)).inRoot(RootMatchers.isDialog()).perform(click())
        onView(withId(androidx.pdf.R.id.password))
            .inRoot(RootMatchers.isDialog())
            .perform(typeText("password"))

        // Capture a screenshot. This image will include the password dialog,
        // and its contents
        assertFullScreenshot(screenshotRule, FILE_PASSWORD_DIALOG_KEYBOARD_LANDSCAPE)
    }
}
