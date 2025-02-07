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
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.TestUtils.waitFor
import androidx.pdf.matchers.SearchViewAssertions
import androidx.pdf.util.Preconditions
import androidx.pdf.view.PdfView
import androidx.pdf.view.fastscroll.FastScrollDrawer
import androidx.pdf.view.fastscroll.FastScroller
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class PdfViewerFragmentV2TestSuite {

    private lateinit var scenario: FragmentScenario<TestPdfViewerFragmentV2>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestPdfViewerFragmentV2>(
                themeResId =
                    com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                initialState = Lifecycle.State.INITIALIZED
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

    @Test
    fun testPdfViewerFragment_setDocumentUri() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
        }

        // Swipe actions
        onView(withId(PdfR.id.pdfView)).perform(swipeUp())

        // Check if the scrubber is visible
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }

        // Scrubber should auto hide after animation and delay ends
        val totalTimeForScubberToHide =
            FastScroller.HIDE_ANIMATION_DURATION_MILLIS + FastScroller.HIDE_DELAY_MS
        onView(isRoot()).perform(waitFor(totalTimeForScubberToHide))
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.GONE_ALPHA)
        }

        // Go back up and assert that the scrubber is visible again
        onView(withId(PdfR.id.pdfView)).perform(swipeDown())
        withPdfView(scenario) { _, _, fastScrollThumb ->
            assertTrue(fastScrollThumb.alpha == FastScrollDrawer.VISIBLE_ALPHA)
        }

        // Actions for scrolling by the scrubber
        var fastScrollScrubberClick: GeneralClickAction? = null
        var fastScrollScrubberSwipe: GeneralSwipeAction? = null

        // Used to compute the location of scrubber on view and set the gesture values
        withPdfView(scenario) { _, pdfView, fastScrollThumb ->
            val location = IntArray(2)
            pdfView.getLocationOnScreen(location) // Get location of the PDF View on the screen

            val thumbBounds = fastScrollThumb.bounds
            val thumbCenterX =
                location[0] +
                    pdfView.left +
                    thumbBounds.left +
                    thumbBounds.width() / 2 // X coordinate of the center
            val thumbCenterY =
                location[1] +
                    pdfView.top +
                    thumbBounds.top +
                    thumbBounds.height() / 2 // Y coordinate of the center

            fastScrollScrubberClick =
                GeneralClickAction(
                    Tap.SINGLE,
                    { floatArrayOf(thumbCenterX.toFloat(), thumbCenterY.toFloat()) },
                    Press.THUMB,
                    InputDevice.SOURCE_UNKNOWN,
                    MotionEvent.BUTTON_PRIMARY
                )

            fastScrollScrubberSwipe =
                GeneralSwipeAction(
                    Swipe.FAST,
                    { floatArrayOf(thumbCenterX.toFloat(), thumbCenterY.toFloat()) },
                    { view ->
                        val endY = (view.height * 0.8f) + thumbCenterY.toFloat()
                        val endX = thumbCenterX.toFloat()
                        floatArrayOf(endX, endY)
                    },
                    Press.FINGER
                )

            val expectedLabelWithoutSpace = "1/3"
            val expectedLabelWithSpace = "1 / 3"
            assertTrue(
                "Current page indicator label ${pdfView.currentPageIndicatorLabel.trim()} not equal " +
                    "to expected $expectedLabelWithoutSpace or $expectedLabelWithSpace",
                pdfView.currentPageIndicatorLabel.trim() == expectedLabelWithoutSpace ||
                    pdfView.currentPageIndicatorLabel.trim() == expectedLabelWithSpace
            )
        }

        onView(isRoot()).perform(fastScrollScrubberClick!!)
        onView(isRoot()).perform(fastScrollScrubberSwipe!!)

        withPdfView(scenario) { _, pdfView, _ ->
            val expectedLabelWithoutSpace = "2/3"
            val expectedLabelWithSpace = "2 / 3"
            assertTrue(
                "Current page indicator label ${pdfView.currentPageIndicatorLabel.trim()} not equal " +
                    "to expected $expectedLabelWithoutSpace or $expectedLabelWithSpace",
                pdfView.currentPageIndicatorLabel.trim() == expectedLabelWithoutSpace ||
                    pdfView.currentPageIndicatorLabel.trim() == expectedLabelWithSpace
            )
        }
    }

    @Test
    fun testPdfViewerFragment_isTextSearchActive_toggleMenu() {
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(PdfR.id.pdfLoadingProgressBar))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        scenario.onFragment {
            Preconditions.checkArgument(
                it.documentLoaded,
                "Unable to load document due to ${it.documentError?.message}"
            )
        }

        // Toggle search menu
        val searchViewAssertion = SearchViewAssertions()
        scenario.onFragment { it.isTextSearchActive = true }
        onView(withId(PdfR.id.pdfSearchView)).check(matches(isDisplayed()))

        onView(withId(R.id.searchQueryBox)).perform(typeText(SEARCH_QUERY))
        onView(withId(R.id.matchStatusTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.extractAndMatch())

        // Prev/next search results
        onView(withId(R.id.findPrevButton)).perform(click())
        // TODO: Cleanup when idling resource is added
        onView(isRoot()).perform(waitFor(50))

        val keyboard = uiDevice.findObject(UiSelector().descriptionContains(KEYBOARD_CONTENT_DESC))
        // Assert keyboard is dismissed on clicking prev/next
        assertFalse(keyboard.exists())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchPrevious())
        onView(withId(R.id.findNextButton)).perform(click())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchNext())
        onView(withId(R.id.findNextButton)).perform(click())
        onView(withId(R.id.matchStatusTextView)).check(searchViewAssertion.matchNext())

        // Assert for keyboard collapse
        onView(withId(R.id.searchQueryBox)).perform(click())
        onView(withId(R.id.closeButton)).perform(click())
        onView(withId(R.id.searchQueryBox))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun testPdfViewerFragment_onLoadDocumentError_corruptPdf() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_CORRUPTED_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            // Loading view assertion
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }

        onView(withId(R.id.errorTextView)).check(matches(isDisplayed()))
        scenario.onFragment { fragment ->
            Preconditions.checkArgument(
                fragment.documentError is RuntimeException,
                "Exception is of incorrect type ${fragment.documentError}"
            )
            Preconditions.checkArgument(
                fragment.documentError
                    ?.message
                    .equals(fragment.resources.getString(R.string.pdf_error)),
                "Incorrect exception returned ${fragment.documentError?.message}"
            )
        }
    }

    // TODO(b/392638037): Add immersive mode integration test

    private fun withPdfView(
        scenario: FragmentScenario<TestPdfViewerFragmentV2>,
        callback: (TestPdfViewerFragmentV2, PdfView, Drawable) -> Unit
    ) {
        scenario.onFragment { fragment ->
            val fastScrollThumb = fragment.getPdfViewInstance().fastScrollVerticalThumbDrawable!!
            assertNotNull("Fast scroll thumbnail cannot be null", fastScrollThumb)
            callback(fragment, fragment.getPdfViewInstance(), fastScrollThumb)
        }
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val TEST_CORRUPTED_DOCUMENT_FILE = "corrupted.pdf"
        private const val SEARCH_QUERY = "ipsum"
        private const val KEYBOARD_CONTENT_DESC = "keyboard"
    }
}
