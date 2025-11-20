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
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.matchers.SearchViewAssertions
import androidx.pdf.util.Preconditions
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
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
class PdfViewerScreenRotationTestSuite {

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

    @Test
    fun testPdfViewerFragment_screenRotation_searchViewState() {

        val scenario =
            scenarioLoadDocument(
                TEST_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            )

        scenario.onFragment { it.isToolboxVisible = true }
        onView(withId(R.id.parent_pdf_container)).perform(swipeUp())
        onView(withId(R.id.parent_pdf_container)).perform(swipeDown())

        val searchViewAssertion = SearchViewAssertions()
        scenario.onFragment { it.isTextSearchActive = true }
        onView(withId(R.id.search_container)).check(matches(isDisplayed()))
        onView(withId(R.id.find_query_box)).perform(typeText(SEARCH_QUERY))
        onView(withId(R.id.match_status_textview)).check(matches(isDisplayed()))
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.extractAndMatch())

        changeOrientation(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

        onView(withId(R.id.match_status_textview)).check(matches(isDisplayed()))
        onView(withId(R.id.match_status_textview)).check(searchViewAssertion.extractAndMatch())
    }

    @Test
    fun testPdfViewerFragment_screenRotationToolBoxViewState() {
        val scenario =
            scenarioLoadDocument(
                TEST_DOCUMENT_FILE,
                Lifecycle.State.STARTED,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            )
        onView(withId(R.id.loadingView))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}",
            )
        }

        // Swipe actions
        onView(withId(R.id.parent_pdf_container)).perform(swipeUp())
        onView(withId(R.id.parent_pdf_container)).perform(swipeDown())

        scenario.onFragment { it.isToolboxVisible = true }

        assertEditFabAndSearchVisibility()
        changeOrientation(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        assertEditFabAndSearchVisibility()
        changeOrientation(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        assertEditFabAndSearchVisibility()
    }

    private fun changeOrientation(
        scenario: FragmentScenario<TestPdfViewerFragmentV1>,
        orientation: Int,
    ) {
        scenario.onFragment { it.requireActivity().requestedOrientation = orientation }
    }

    private fun assertEditFabAndSearchVisibility() {
        onView(withId(R.id.edit_fab))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
        onView(withId(androidx.pdf.testapp.R.id.host_Search))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val SEARCH_QUERY = "ipsum"
    }
}
