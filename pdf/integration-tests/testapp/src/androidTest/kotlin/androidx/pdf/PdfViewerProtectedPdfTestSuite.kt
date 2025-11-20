/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf

import android.content.pm.ActivityInfo
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerProtectedPdfTestSuite {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragmentV1>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragmentV1>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED,
            )
        scenario.onFragment { fragment ->
            // Register idling resource
            IdlingRegistry.getInstance()
                .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            // Un-register idling resource
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    private fun scenarioLoadDocument(
        filename: String,
        nextState: Lifecycle.State,
        orientation: Int,
    ): FragmentScenario<TestPdfViewerFragmentV1> {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open(filename)

        scenario.moveToState(nextState)
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }

        // Loading view assertion
        onView(withId(R.id.loadingView)).check(matches(isDisplayed()))

        // Load the document in the fragment
        scenario.onFragment { fragment ->
            fragment.pdfLoadingIdlingResource.increment()
            fragment.documentUri = TestUtils.saveStream(inputStream, fragment.requireContext())
        }

        return scenario
    }

    private fun loadPdfAndTest(password: String, expectedResult: Boolean) {
        val scenario =
            scenarioLoadDocument(
                PROTECTECTED_DOCUMENT_FILENAME,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            )

        InstrumentationRegistry.getInstrumentation().waitForIdle {
            onView(withId(R.id.pdf_password_layout))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
            onView(withId(R.id.password)).perform(click())
            onView(withId(R.id.password)).perform(typeText(password))
            onView(withId(R.id.password)).perform(pressKey(KeyEvent.KEYCODE_ENTER))
        }

        InstrumentationRegistry.getInstrumentation().waitForIdle {
            scenario.onFragment { fragment -> fragment.pdfLoadingIdlingResource.increment() }

            onView(withId(R.id.loadingView))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

            scenario.onFragment { fragment ->
                // Use assertEquals for more informative error messages
                assert(expectedResult == fragment.documentLoaded) {
                    """Document loading state mismatch. Expected: $expectedResult, Actual:
                        ${fragment.documentLoaded}. Error: ${fragment.documentError?.message}"""
                }
            }
        }
    }

    @Test
    fun testPdfViewerFragment_loadProtectedPdf_Success() {
        loadPdfAndTest(password = PROTECTED_DOCUMENT_PASSWORD, expectedResult = true)
    }

    @Test
    fun testPdfViewerFragment_loadProtectedPdf_WrongPassword() {
        loadPdfAndTest(password = "wrong password", expectedResult = false)
    }

    companion object {
        private const val PROTECTECTED_DOCUMENT_FILENAME = "sample-protected.pdf"
        private const val PROTECTED_DOCUMENT_PASSWORD = "abcd1234"
    }
}
