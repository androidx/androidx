/*
 * Copyright 2025 The Android Open Source Project
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
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.actions.TwoFingerSwipeDownAction
import androidx.pdf.actions.TwoFingerSwipeUpAction
import androidx.pdf.ink.R as InkR
import androidx.pdf.ink.model.ApplyInProgressException
import androidx.pdf.util.Preconditions
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.lessThan
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 18)
class EditablePdfViewerFragmentTestSuite {
    private lateinit var scenario: FragmentScenario<TestEditablePdfViewerFragment>

    @Before
    fun setup() {
        scenario =
            launchFragmentInContainer<TestEditablePdfViewerFragment>(
                    themeResId =
                        com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
                    initialState = Lifecycle.State.INITIALIZED,
                )
                .onFragment { fragment ->
                    IdlingRegistry.getInstance()
                        .register(fragment.pdfLoadingIdlingResource.countingIdlingResource)
                    IdlingRegistry.getInstance()
                        .register(fragment.pdfScrollIdlingResource.countingIdlingResource)
                    IdlingRegistry.getInstance()
                        .register(fragment.pdfApplyEditsIdlingResource.countingIdlingResource)
                }
    }

    @After
    fun cleanup() {
        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfScrollIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfApplyEditsIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    private fun loadDocumentAndSetupFragment() {
        scenarioLoadDocument(
            scenario = scenario,
            filename = TEST_DOCUMENT_FILE,
            nextState = Lifecycle.State.STARTED,
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ) {
            onView(withId(PdfR.id.pdfLoadingProgressBar)).check(matches(isDisplayed()))
        }
        onIdle() // Wait for document to load

        scenario.onFragment { fragment ->
            Preconditions.checkArgument(
                fragment.documentLoaded,
                "Unable to load document due to ${fragment.documentError?.message}",
            )
            fragment.setIsAnnotationIntentResolvable(true)
            fragment.isToolboxVisible = true
        }
    }

    @Test
    fun testEditablePdfViewerFragment_viewerMode_singleFingerNavigation() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        val initialPage = getCurrentPageNumber()

        // Scroll Up (to next page)
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeUp())
        scenario.onFragment { fragment -> fragment.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterSwipeUp = getCurrentPageNumber()
        assertThat(pageAfterSwipeUp, greaterThan(initialPage))

        // Scroll Down (to previous page)
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeDown())
        scenario.onFragment { fragment -> fragment.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterSwipeDown = getCurrentPageNumber()
        assertThat(pageAfterSwipeDown, lessThan(pageAfterSwipeUp))
    }

    @Test
    fun testEditablePdfViewerFragment_editMode_singleFingerAnnotation() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Perform a swipe (single touch) on the pdfContentLayout to create an annotation.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())

        // Press back and verify discard dialog appears due to unsaved changes to check if
        // annotation is created.
        onView(withId(PdfR.id.pdfContentLayout)).perform(pressBack())
        onIdle()

        onView(withText(InkR.string.discard_changes_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEditablePdfViewerFragment_editMode_twoFingerNavigation() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        val initialPageInEditMode = getCurrentPageNumber()

        // Perform a two-finger swipe up for scrolling.
        onView(withId(PdfR.id.pdfContentLayout)).perform(TwoFingerSwipeUpAction())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterTwoFingerSwipeUp = getCurrentPageNumber()
        assertThat(pageAfterTwoFingerSwipeUp, greaterThan(initialPageInEditMode))

        // Perform a two-finger swipe down.
        onView(withId(PdfR.id.pdfContentLayout)).perform(TwoFingerSwipeDownAction())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterTwoFingerSwipeDown = getCurrentPageNumber()
        assertThat(pageAfterTwoFingerSwipeDown, lessThan(pageAfterTwoFingerSwipeUp))
    }

    @Test
    fun testEditablePdfViewerFragment_exitEditModeWithNoUnsavedChanges_exitsWithoutShowingDialog() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // No annotations made, so no unsaved changes.
        onView(withId(PdfR.id.pdfContentLayout)).perform(pressBack())
        onIdle()

        onView(withText(InkR.string.discard_changes_dialog_title)).check(doesNotExist())
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed())) // Back in viewer mode
    }

    @Test
    fun testEditablePdfViewerFragment_exitEditModeWithUnsavedChanges_showsDiscardDialog() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Create an annotation
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())

        // 1. Press back, dialog should appear
        onView(withId(PdfR.id.pdfContentLayout)).perform(pressBack())
        onIdle()
        onView(withText(InkR.string.discard_changes_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        // 2. Click "Keep editing", dialog disappears, still in edit mode
        onView(withText(InkR.string.keep_editing_button)).perform(click())
        onIdle()
        onView(withText(InkR.string.discard_changes_dialog_title)).check(doesNotExist())
        onView(withId(R.id.edit_fab)).check(matches(not(isDisplayed()))) // Still in edit mode

        // 3. Press back again, dialog reappears
        onView(withId(PdfR.id.pdfContentLayout)).perform(pressBack())
        onIdle()
        onView(withText(InkR.string.discard_changes_dialog_title))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        // 4. Click "Discard", dialog disappears, back in viewer mode
        onView(withText(InkR.string.discard_button)).perform(click())
        onIdle()
        onView(withText(InkR.string.discard_changes_dialog_title)).check(doesNotExist())
        onView(withId(R.id.edit_fab)).check(matches(isDisplayed())) // Back to viewer mode
    }

    @Test
    fun testEditablePdfViewerFragment_enterAndExitEditMode_togglesState() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()

        // 1. Verify initial state (Viewer Mode)
        scenario.onFragment { fragment -> assertThat(fragment.isEditModeEnabled).isFalse() }

        // 2. Enter Edit Mode
        enterEditMode()

        scenario.onFragment { fragment ->
            assertThat(fragment.isEditModeEnabled).isTrue()
            assertThat(fragment.onEnterEditModeCalled).isTrue()
        }

        // 3. Exit Edit Mode
        scenario.onFragment { fragment -> fragment.isEditModeEnabled = false }
        onIdle()

        scenario.onFragment { fragment ->
            assertThat(fragment.isEditModeEnabled).isFalse()
            assertThat(fragment.onExitEditModeCalled).isTrue()
        }
    }

    @Test
    fun testEditablePdfViewerFragment_applyDraftEdits_callbacks() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Create an annotation
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        onIdle()

        // Apply draft edits
        scenario.onFragment { fragment ->
            assertThat(fragment.isApplyEditsInProgress).isFalse()
            fragment.pdfApplyEditsIdlingResource.increment()
            fragment.applyDraftEdits()
            assertThat(fragment.isApplyEditsInProgress).isTrue()
        }

        onIdle()

        // Verify success callback was called and progress is finished
        scenario.onFragment { fragment ->
            assertThat(fragment.onApplyEditsSuccessCalled).isTrue()
            assertThat(fragment.isApplyEditsInProgress).isFalse()
        }
    }

    @Test
    fun testEditablePdfViewerFragment_applyDraftEdits_throwsIfInProgress() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        onIdle()

        scenario.onFragment { fragment ->
            // 1. Start the first apply operation
            fragment.pdfApplyEditsIdlingResource.increment()
            fragment.applyDraftEdits()
            assertThat(fragment.isApplyEditsInProgress).isTrue()

            // 2. Attempt a second apply operation immediately and verify it throws
            try {
                fragment.applyDraftEdits()
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(ApplyInProgressException::class.java)
            }
        }
    }

    @Test
    fun testEditablePdfViewerFragment_hasUnsavedChanges() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()

        // 1. Initially, no unsaved changes
        var hasChanges = true
        scenario.onFragment { hasChanges = it.hasUnsavedChanges }
        assertThat(hasChanges).isFalse()

        // 2. Enter edit mode, still no changes
        enterEditMode()
        scenario.onFragment { hasChanges = it.hasUnsavedChanges }
        assertThat(hasChanges).isFalse()

        // 3. Create an annotation, now there are unsaved changes
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        onIdle()
        scenario.onFragment { hasChanges = it.hasUnsavedChanges }
        assertThat(hasChanges).isTrue()

        // 4. Apply edits, changes should be saved
        scenario.onFragment { it.applyDraftEdits() }
        onIdle()
        scenario.onFragment { hasChanges = it.hasUnsavedChanges }
        assertThat(hasChanges).isFalse()
    }

    private fun enterEditMode() {
        onView(withId(R.id.edit_fab)).apply {
            check(matches(isDisplayed()))
            perform(click())
            check(matches(not(isDisplayed())))
        }
        onIdle()
    }

    private fun getCurrentPageNumber(): Int {
        var pageNum = 0
        scenario.onFragment { fragment ->
            pageNum =
                fragment
                    .getPdfViewInstance()
                    .currentPageIndicatorLabel
                    .trim()
                    .split(" / ")[0]
                    .toInt()
        }
        return pageNum
    }

    companion object {
        private const val TEST_DOCUMENT_FILE = "sample.pdf"
        private const val REQUIRED_EXTENSION_VERSION = 18

        fun isRequiredSdkExtensionAvailable(): Boolean {
            // Get the device's version for the specified SDK extension
            val deviceExtensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)
            return deviceExtensionVersion >= REQUIRED_EXTENSION_VERSION
        }
    }
}
