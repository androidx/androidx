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

package androidx.pdf.view

import android.content.Intent
import android.graphics.Point
import android.graphics.RectF
import android.net.Uri
import android.view.ViewGroup
import androidx.pdf.PdfDocument
import androidx.pdf.content.PdfPageGotoLinkContent
import androidx.pdf.content.PdfPageLinkContent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PdfViewNavigationTest {
    @After
    fun tearDown() {
        PdfViewTestActivity.onCreateCallback = {}
    }

    private fun setupPdfView(width: Int, height: Int, fakePdfDocument: FakePdfDocument?) {
        PdfViewTestActivity.onCreateCallback = { activity ->
            with(activity) {
                container.addView(
                    PdfView(activity).apply {
                        pdfDocument = fakePdfDocument
                        id = PDF_VIEW_ID
                    },
                    ViewGroup.LayoutParams(width, height),
                )
            }
        }
    }

    @Test
    fun testGotoLinkNavigation_withValidPage() = runTest {
        val linkBounds = RectF(0f, 0f, 100f, 200f)
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(20) { Point(100, 200) },
                pageLinks =
                    mapOf(
                        0 to
                            PdfDocument.PdfPageLinks(
                                gotoLinks =
                                    listOf(
                                        PdfPageGotoLinkContent(
                                            bounds = listOf(linkBounds),
                                            destination =
                                                PdfPageGotoLinkContent.Destination(
                                                    pageNumber = VALID_PAGE_NUMBER,
                                                    xCoordinate = 10f,
                                                    yCoordinate = 40f,
                                                    zoom = 1f,
                                                ),
                                        )
                                    ),
                                externalLinks = emptyList(),
                            )
                    ),
            )
        setupPdfView(100, 1000, fakePdfDocument)

        var firstVisiblePage = 0
        var visiblePagesCount = 0
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForLayout(untilPage = VALID_PAGE_NUMBER)
            fakePdfDocument.waitForRender(untilPage = 0)

            var tapX = 0f
            var tapY = 0f
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                val tapPoint = getTapPointFromContentBounds(pdfView, 0, linkBounds)
                tapX = tapPoint.x
                tapY = tapPoint.y
            }

            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(tapX, tapY))
            fakePdfDocument.waitForLayout(untilPage = VALID_PAGE_NUMBER)

            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                firstVisiblePage = pdfView.firstVisiblePage
                visiblePagesCount = pdfView.visiblePagesCount
            }
            close()
        }
        assertThat(VALID_PAGE_NUMBER).isAtLeast(firstVisiblePage)
        assertThat(VALID_PAGE_NUMBER).isAtMost(firstVisiblePage + visiblePagesCount - 1)
    }

    @Test
    fun testGotoLinkNavigation_withInvalidPage() = runTest {
        val linkBounds = RectF(0f, 0f, 100f, 200f)
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(5) { Point(100, 200) },
                pageLinks =
                    mapOf(
                        0 to
                            PdfDocument.PdfPageLinks(
                                gotoLinks =
                                    listOf(
                                        PdfPageGotoLinkContent(
                                            bounds = listOf(linkBounds),
                                            destination =
                                                PdfPageGotoLinkContent.Destination(
                                                    pageNumber = NON_EXISTENT_PAGE_NUMBER,
                                                    xCoordinate = 10f,
                                                    yCoordinate = 40f,
                                                    zoom = 1f,
                                                ),
                                        )
                                    ),
                                externalLinks = emptyList(),
                            )
                    ),
            )
        setupPdfView(100, 1000, fakePdfDocument)

        var firstVisiblePage = 0
        var visiblePagesCount = 0
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForRender(untilPage = 0)

            var tapX = 0f
            var tapY = 0f
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                val tapPoint = getTapPointFromContentBounds(pdfView, 0, linkBounds)
                tapX = tapPoint.x
                tapY = tapPoint.y
            }

            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(tapX, tapY))
            fakePdfDocument.waitForLayout(untilPage = 0)

            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                firstVisiblePage = pdfView.firstVisiblePage
                visiblePagesCount = pdfView.visiblePagesCount
            }
            close()
        }

        assertThat(firstVisiblePage).isEqualTo(0)
        assertThat(visiblePagesCount).isGreaterThan(0)
    }

    @Test
    fun testExternalLinkNavigation_withValidUri() = runTest {
        val linkBounds = RectF(0f, 0f, 100f, 200f)
        val uri = Uri.parse(URI_WITH_VALID_SCHEME)
        val fakePdfDocument =
            FakePdfDocument(
                pages = List(10) { Point(200, 200) },
                pageLinks =
                    mapOf(
                        0 to
                            PdfDocument.PdfPageLinks(
                                gotoLinks = emptyList(),
                                externalLinks =
                                    listOf(
                                        PdfPageLinkContent(bounds = listOf(linkBounds), uri = uri)
                                    ),
                            )
                    ),
            )
        setupPdfView(200, 1000, fakePdfDocument)

        Intents.init()
        with(ActivityScenario.launch(PdfViewTestActivity::class.java)) {
            fakePdfDocument.waitForLayout(1)
            fakePdfDocument.waitForRender(1)

            var tapX = 0f
            var tapY = 0f
            Espresso.onView(withId(PDF_VIEW_ID)).check { view, noViewFoundException ->
                view ?: throw noViewFoundException
                val pdfView = view as PdfView
                val tapPoint = getTapPointFromContentBounds(pdfView, 0, linkBounds)
                tapX = tapPoint.x
                tapY = tapPoint.y
            }

            Espresso.onView(withId(PDF_VIEW_ID)).perform(performSingleTapOnCoords(tapX, tapY))
            close()
        }
        Espresso.onIdle()
        Intents.intended(hasAction(Intent.ACTION_VIEW))
        Intents.intended(hasData(uri))
        Intents.release()
    }
}

/** Arbitrary fixed ID for PdfView */
private const val PDF_VIEW_ID = 123456789
private const val URI_WITH_VALID_SCHEME = "https://www.example.com"
private const val VALID_PAGE_NUMBER = 4
private const val NON_EXISTENT_PAGE_NUMBER = 11
