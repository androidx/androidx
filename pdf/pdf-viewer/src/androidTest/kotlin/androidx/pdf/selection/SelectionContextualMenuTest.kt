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

package androidx.pdf.selection

import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import androidx.pdf.PdfDocument.PdfPageLinks
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.pdf.content.PdfPageTextContent
import androidx.pdf.view.FakePdfDocument
import androidx.pdf.view.PdfView
import androidx.pdf.view.PdfViewTestActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
@LargeTest
class SelectionContextualMenuTest {

    @Before
    fun setUp() {
        val textContents = FAKE_PAGE_TEXT.map { text -> PdfPageTextContent(BOUNDS, text) }
        val emailLinkContent =
            PdfPageLinks(
                gotoLinks = emptyList(),
                externalLinks =
                    listOf(PdfPageLinkContent(bounds = BOUNDS, uri = Uri.parse(EMAIL_LINK))),
            )
        val goToLinkContent =
            PdfPageLinks(
                gotoLinks =
                    listOf(PdfPageGotoLinkContent(bounds = BOUNDS, destination = DESTINATION)),
                externalLinks = emptyList(),
            )
        val hyperLinkContent =
            PdfPageLinks(
                gotoLinks = emptyList(),
                externalLinks = listOf(PdfPageLinkContent(bounds = BOUNDS, uri = Uri.parse(LINK))),
            )
        val pageLinks: Map<Int, PdfPageLinks> =
            mapOf(3 to goToLinkContent, 4 to hyperLinkContent, 5 to emailLinkContent)
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(6) { Point(2000, 4000) },
                pageLinks = pageLinks,
                textContents = textContents,
            )

        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                        isFocusable = true
                        isFocusableInTouchMode = true
                    },
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
    }

    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    @Test
    fun testDoubleTapAfterSelection_stillshowsMenuOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode.
            Espresso.onView(withText("Copy"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))

            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                // Double Tap to zoom in.
                .perform(doubleClick())

            // Verify that the contextual menu is still visible.
            Espresso.onView(withText("Copy"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testEmailLinkSelection_doesNotshowCopyLinkOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.scrollToPage(5)
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected does not show copy link.
            Espresso.onView(withText("Copy link"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(doesNotExist())
        }
    }

    @Test
    fun testEmailSelection_showsEmailAddOptions() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode showing email add
            // options
            Espresso.onView(withText("Email"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testGoToLinkSelection_showsJumptoOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.scrollToPage(3)
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode showing Jump to option
            Espresso.onView(withText("Jump to"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testHyperLinkSelection_showsCopylinkOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.scrollToPage(4)
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode showing Copy link option
            Espresso.onView(withText("Copy link"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testLinkSelection_showsOpenOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.scrollToPage(1)
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode showing open
            Espresso.onView(withText("Open"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun testPhoneNumberSelection_showsCallOption() {
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            Espresso.onView(ViewMatchers.withId(PDF_VIEW_ID))
                .check { view, _ ->
                    val pdfView = view as PdfView
                    pdfView.scrollToPage(2)
                }
                // Create a selection by long-pressing the center of the view.
                .perform(longClick())
            // Verify that the long press selected started action mode showing call option
            Espresso.onView(withText("Call"))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
        }
    }

    private companion object {
        const val BACKTOEMAIL = "Back to Email"
        const val EMAIL = "androidpdf@gmail.com"
        const val EMAIL_LINK = "https://mailto:androidpdf@gmail.com"
        const val GOOGLE = "Google"
        const val LINK = "https://www.google.com"
        const val PDF_VIEW_ID = 123456789
        const val PHONE_NUMBER = "8244812290"

        val BOUNDS = listOf(RectF(0f, 0f, 2000f, 4000f))

        val DESTINATION =
            PdfPageGotoLinkContent.Destination(
                pageNumber = 0,
                xCoordinate = 0.0f,
                yCoordinate = 0.0f,
                zoom = 1.0f,
            )
        val FAKE_PAGE_TEXT =
            listOf<String>(EMAIL, LINK, PHONE_NUMBER, BACKTOEMAIL, GOOGLE, EMAIL_LINK)
    }
}
