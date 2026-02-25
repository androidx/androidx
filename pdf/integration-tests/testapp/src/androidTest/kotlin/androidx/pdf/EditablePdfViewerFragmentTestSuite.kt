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
import android.graphics.PointF
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.pdf.FragmentUtils.scenarioLoadDocument
import androidx.pdf.actions.TwoFingerSwipeDownAction
import androidx.pdf.actions.TwoFingerSwipeUpAction
import androidx.pdf.actions.clickOnPdfPoint
import androidx.pdf.ink.R as PdfInkR
import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.util.Preconditions
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.R as PdfR
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assume.assumeFalse
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
        assumeFalse(
            "Test fails on cuttlefish b/465861868",
            Build.MODEL.contains("Cuttlefish", ignoreCase = true),
        )
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
                    IdlingRegistry.getInstance()
                        .register(fragment.pdfFormFillingIdlingResource.countingIdlingResource)
                }
    }

    @After
    fun cleanup() {
        if (!::scenario.isInitialized) return

        scenario.onFragment { fragment ->
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfLoadingIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfScrollIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfApplyEditsIdlingResource.countingIdlingResource)
            IdlingRegistry.getInstance()
                .unregister(fragment.pdfFormFillingIdlingResource.countingIdlingResource)
        }
        scenario.close()
    }

    private fun loadDocumentAndSetupFragment(file: String = TEST_DOCUMENT_FILE) {
        scenarioLoadDocument(
            scenario = scenario,
            filename = file,
            nextState = Lifecycle.State.RESUMED,
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
    fun testEditablePdfViewerFragment_endToEndAnnotationFlow() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        lateinit var fragment: TestEditablePdfViewerFragment
        scenario.onFragment { fragment = it }
        loadDocumentAndSetupFragment()
        enterEditMode()

        // 1. Create an annotation and apply the edits.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        fragment.pdfApplyEditsIdlingResource.increment()
        fragment.applyDraftEdits()
        onIdle()

        // 2. Store the annotations from the current document.
        val originalAnnotations = fragment.fetchAnnotations(pageNum = 0)
        assertThat(originalAnnotations).isNotEmpty()

        // 3. Save the document with annotations to a new file.
        val destinationUri = TestUtils.createFile(DESTINATION_FILE_NAME)
        fragment.writeTo(destinationUri.toPfd())

        // 4. Load the newly saved document.
        fragment.pdfLoadingIdlingResource.increment()
        fragment.documentUri = destinationUri
        onIdle()

        // 5. Verify the annotations of the new document.
        val savedAnnotations = fragment.fetchAnnotations(0)
        assertThat(savedAnnotations).isEqualTo(originalAnnotations)
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
        assertThat(pageAfterSwipeUp).isGreaterThan(initialPage)

        // Scroll Down (to previous page)
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeDown())
        scenario.onFragment { fragment -> fragment.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterSwipeDown = getCurrentPageNumber()
        assertThat(pageAfterSwipeDown).isLessThan(pageAfterSwipeUp)
    }

    @Test
    fun testEditablePdfViewerFragment_editMode_singleFingerAnnotation() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        // Perform a swipe (single touch) on the pdfContentLayout to create an annotation.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())

        var hasUnsavedChanges = false
        scenario.onFragment { fragment -> hasUnsavedChanges = fragment.hasUnsavedChanges }
        assertThat(hasUnsavedChanges).isTrue()
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
        assertThat(pageAfterTwoFingerSwipeUp).isGreaterThan(initialPageInEditMode)

        // Perform a two-finger swipe down.
        onView(withId(PdfR.id.pdfContentLayout)).perform(TwoFingerSwipeDownAction())
        scenario.onFragment { it.pdfScrollIdlingResource.increment() }
        onIdle()

        val pageAfterTwoFingerSwipeDown = getCurrentPageNumber()
        assertThat(pageAfterTwoFingerSwipeDown).isLessThan(pageAfterTwoFingerSwipeUp)
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
            assertThat(fragment.hasUnsavedChanges).isTrue()
            assertThat(fragment.isApplyEditsInProgress).isFalse()
            fragment.pdfApplyEditsIdlingResource.increment()
            fragment.applyDraftEdits()
        }

        onIdle()

        // Verify success callback was called and progress is finished
        scenario.onFragment { fragment ->
            assertThat(fragment.hasUnsavedChanges).isFalse()
            assertThat(fragment.onApplyEditsSuccessCalled).isTrue()
            assertThat(fragment.isApplyEditsInProgress).isFalse()
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

    @Test
    fun testEditablePdfViewerFragment_testUndoRedo() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()

        enterEditMode()
        // Assert undo/redo buttons is initially disabled
        onView(withId(PdfInkR.id.undo_button)).check(matches(not(isEnabled())))
        onView(withId(PdfInkR.id.redo_button)).check(matches(not(isEnabled())))

        // Draw an annotation on the content view
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Assert AnnotationView is visible
        onView(withId(PdfInkR.id.pdf_annotation_view)).check(matches(isDisplayed()))
        // Assert undo button gets enabled after a stroke is drawn; but redo remains disabled
        onView(withId(PdfInkR.id.undo_button)).check(matches(isEnabled()))
        onView(withId(PdfInkR.id.redo_button)).check(matches(not(isEnabled())))

        // Undo last drawn annotation
        onView(withId(PdfInkR.id.undo_button)).perform(click())
        // Assert user cannot perform any more undo steps after last annotation is undone
        onView(withId(PdfInkR.id.undo_button)).check(matches(not(isEnabled())))
        // Assert redo button gets enabled
        onView(withId(PdfInkR.id.redo_button)).check(matches(isEnabled()))

        // Now perform a redo
        onView(withId(PdfInkR.id.redo_button)).perform(click())
        onView(withId(PdfInkR.id.redo_button)).check(matches(not(isEnabled())))
        // Assert undo button gets enabled
        onView(withId(PdfInkR.id.undo_button)).check(matches(isEnabled()))
    }

    @Test
    fun testEditablePdfViewerFragment_annotationDisabled() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        var pdfView: PdfView? = null
        scenario.onFragment { fragment -> pdfView = fragment.view?.findViewById(R.id.pdfView) }

        enterEditMode()
        // Draw an annotation on the content view
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Assert AnnotationView is visible
        onView(withId(PdfInkR.id.pdf_annotation_view)).check(matches(isDisplayed()))
        // Disable annotations
        onView(withId(PdfInkR.id.toggle_annotation_button)).perform(click())
        // Assert AnnotationView is hidden
        onView(withId(PdfInkR.id.pdf_annotation_view)).check(matches(not(isDisplayed())))

        val fitToScreenZoom = pdfView?.zoom
        // try interacting with the content rendered on the pdf view
        onView(withId(R.id.pdfView)).check(matches(isDisplayed()))
        onView(withId(PdfR.id.pdfContentLayout)).perform(doubleClick())
        // wait for gesture to complete
        onIdle()
        // assert interaction actually occurred on PdfView
        assertThat(pdfView?.zoom).isGreaterThan(fitToScreenZoom)

        // Toggle once again to enable annotations
        onView(withId(PdfInkR.id.toggle_annotation_button)).perform(click())
        onView(withId(PdfInkR.id.pdf_annotation_view)).check(matches(isDisplayed()))
        // Try same interaction when annotation interaction is enabled
        onView(withId(PdfR.id.pdfContentLayout)).perform(doubleClick())
        // assert double tapping again doesn't fit to screen as touch is consumed by wet stroke's
        // view
        assertNotEquals(fitToScreenZoom, pdfView?.zoom)
    }

    @Test
    fun testEditablePdfViewerFragment_toolbarPopupDismissed_OnContentTouch() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()

        enterEditMode()

        // Click again to show brush slider
        onView(withId(PdfInkR.id.pen_button)).perform(click())

        onView(withId(PdfInkR.id.brush_size_selector)).check(matches(isDisplayed()))
        // Draw an annotation on the content view
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Assert brush size selector is not displayed
        onView(withId(PdfInkR.id.brush_size_selector)).check(matches(not(isDisplayed())))

        onView(withId(PdfInkR.id.color_palette_button)).perform(click())
        onView(withId(PdfInkR.id.color_palette)).check(matches(isDisplayed()))
        // Draw an annotation on the content view
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Assert color palette is not displayed
        onView(withId(PdfInkR.id.color_palette)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testEditablePdfViewerFragment_annotationToolbar_isConfigPopupVisible() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment()
        enterEditMode()

        var annotationToolbar: AnnotationToolbar? = null
        scenario.onFragment { fragment ->
            fragment.view?.findViewById<AnnotationToolbar>(PdfInkR.id.annotationToolbar)?.let {
                annotationToolbar = it
            }
        }

        // open brush size selector
        onView(withId(PdfInkR.id.pen_button)).perform(click())

        assertNotNull(annotationToolbar)
        assertTrue(annotationToolbar.isConfigPopupVisible)

        onView(withId(PdfInkR.id.pdf_annotation_view)).perform(click())
        assertFalse(annotationToolbar.isConfigPopupVisible)

        // open color palette
        onView(withId(PdfInkR.id.color_palette_button)).perform(click())
        assertTrue(annotationToolbar.isConfigPopupVisible)

        onView(withId(PdfInkR.id.pdf_annotation_view)).perform(click())
        assertFalse(annotationToolbar.isConfigPopupVisible)
    }

    @Test
    fun testEditablePdfViewerFragment_eraserTool_preventsDoubleDeletion_onSingleTap() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        lateinit var fragment: TestEditablePdfViewerFragment
        scenario.onFragment { fragment = it }
        loadDocumentAndSetupFragment()
        enterEditMode()

        // 1. Create two annotations and apply the edits.
        // These two swipes, starting from roughly the center of the view, will create
        // two annotations that will overlap near the center of PdfR.id.pdfContentLayout.

        // First annotation: A horizontal stroke drawn by swiping left.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Second annotation: A vertical stroke drawn by swiping down.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeDown())

        // 2. Select the Eraser tool
        onView(withId(PdfInkR.id.eraser_button)).perform(click())

        // 3. Perform a single tap. By default, `click()` targets the center of the view.
        // Since both swipe gestures pass through the center, this tap
        // occurs within the bounding boxes of both created annotations.
        onView(withId(PdfR.id.pdfContentLayout)).perform(click())
        fragment.pdfApplyEditsIdlingResource.increment()
        fragment.applyDraftEdits()
        onIdle()

        // 4. Verify the annotations. When multiple annotations overlap at the tap point,
        // the eraser tool should only delete one of them per single tap, preventing
        // accidental deletion of multiple strokes. Thus, only one of the two annotations
        // should have been erased.
        val savedAnnotations = fragment.fetchAnnotations(0)
        assertThat(savedAnnotations).hasSize(1)
    }

    @Test
    fun testEditablePdfViewerFragment_whenFormFilling_toolBoxHidden() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment(file = FORM_PDF)

        // Click on a check-box type form-field. These are PDF-coordinates so they won't
        // change depending on device and can be safely hardcoded.
        onView(withId(PdfR.id.pdfContentLayout))
            .perform(clickOnPdfPoint(PdfPoint(0, PointF(145f, 80f))))
        scenario.onFragment { fragment -> fragment.pdfFormFillingIdlingResource.increment() }
        onIdle()

        onView(withId(R.id.edit_fab)).check(matches(not(isDisplayed())))
        scenario.onFragment { fragment ->
            assertThat(fragment.onFormWidgetInfoUpdatedCalled).isTrue()
            assertThat(fragment.isEditModeEnabled).isTrue()
            assertThat(fragment.onEnterEditModeCalled).isTrue()
        }
        onView(withId(PdfR.id.pdfContentLayout)).perform(click())
        onView(withId(R.id.edit_fab)).check(matches(not(isDisplayed())))
    }

    @Test
    fun testEnterEditModeCallback_isIdempotent() {
        if (!isRequiredSdkExtensionAvailable()) return

        loadDocumentAndSetupFragment(file = FORM_PDF)
        // Click on a check-box type form-field. These are PDF-coordinates so they won't
        // change depending on device and can be safely hardcoded.
        onView(withId(PdfR.id.pdfContentLayout))
            .perform(clickOnPdfPoint(PdfPoint(0, PointF(145f, 80f))))
        scenario.onFragment { fragment -> fragment.pdfFormFillingIdlingResource.increment() }
        onIdle()

        scenario.onFragment { fragment ->
            assertThat(fragment.onEnterEditModeCalled).isTrue()
            assertThat(fragment.onEnterEditModeCalledCount).isEqualTo(1)
        }

        // Click on the form-widget again. (Un-check the checkbox)
        onView(withId(PdfR.id.pdfContentLayout))
            .perform(clickOnPdfPoint(PdfPoint(0, PointF(145f, 80f))))
        scenario.onFragment { fragment -> fragment.pdfFormFillingIdlingResource.increment() }
        onIdle()

        // The enterEditMode callback count should still be 1
        scenario.onFragment { fragment ->
            assertThat(fragment.onEnterEditModeCalledCount).isEqualTo(1)
            // Try to set edit mode enabled to true externally (Defaults to annotations)
            fragment.isEditModeEnabled = true
            // Since form-filling journey is active should get no callback for onEnterEditMode
            assertThat(fragment.onEnterEditModeCalledCount).isEqualTo(1)
        }
    }

    @Test
    fun testEditablePdfViewerFragment_multipleCallsToApplyEdits() = runTest {
        if (!isRequiredSdkExtensionAvailable()) return@runTest

        lateinit var fragment: TestEditablePdfViewerFragment
        scenario.onFragment { fragment = it }
        loadDocumentAndSetupFragment()
        enterEditMode()

        // First annotation: A horizontal stroke drawn by swiping left.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeLeft())
        // Apply this annotation to the document
        fragment.syncApplyEdits()

        // Second annotation: A vertical stroke drawn by swiping down.
        onView(withId(PdfR.id.pdfContentLayout)).perform(swipeDown())
        // Again, apply the annotation to the document
        fragment.syncApplyEdits()

        // Select the Eraser tool and erase 2nd annotation
        onView(withId(PdfInkR.id.eraser_button)).perform(click())
        onView(withId(PdfR.id.pdfContentLayout)).perform(click())

        // Apply the removed annotation to document
        fragment.syncApplyEdits()

        // Save the document with annotations to a new file.
        val destinationUri = TestUtils.createFile(DESTINATION_FILE_NAME)
        fragment.writeTo(destinationUri.toPfd())

        // Load the newly saved document.
        fragment.pdfLoadingIdlingResource.increment()
        fragment.documentUri = destinationUri
        onIdle()

        // Verify the annotations, only 1 annotation should be persisted to the destination file
        val savedAnnotations = fragment.fetchAnnotations(0)
        assertThat(savedAnnotations).hasSize(1)
    }

    private fun TestEditablePdfViewerFragment.syncApplyEdits() {
        pdfApplyEditsIdlingResource.increment()
        applyDraftEdits()
        onIdle()
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
        private const val DESTINATION_FILE_NAME = "destination.pdf"
        private const val REQUIRED_EXTENSION_VERSION = 18
        private const val FORM_PDF = "click_form.pdf"

        fun isRequiredSdkExtensionAvailable(): Boolean {
            // Get the device's version for the specified SDK extension
            val deviceExtensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R)
            return deviceExtensionVersion >= REQUIRED_EXTENSION_VERSION
        }
    }
}
