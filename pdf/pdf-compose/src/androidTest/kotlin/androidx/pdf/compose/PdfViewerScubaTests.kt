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

package androidx.pdf.compose

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.util.Size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class PdfViewerScubaTests {
    @get:Rule val composeTestRule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun testPdfViewer_withContentPadding_rendersCorrectly_onOpen() {
        val pages = List(3) { Point(400, 500) }
        val pdfDocument = DeterministicFakePdfDocument(pages)
        val contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp)

        lateinit var pdfViewerState: PdfViewerState
        composeTestRule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                pdfDocument = pdfDocument,
                state = pdfViewerState,
                contentPadding = contentPadding,
                modifier = Modifier.testTag(PDF_VIEW_TAG).fillMaxSize(),
            )
        }

        // Wait for first content load
        composeTestRule.waitUntil(timeoutMillis = 5000) { pdfViewerState.visiblePagesCount > 0 }

        // Initial state: Padding area should be visible and empty (content starts below padding)
        composeTestRule
            .onNodeWithTag(PDF_VIEW_TAG)
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, PDF_VIEW_CONTENT_PADDING_NO_SCROLL)
    }

    @Test
    fun testPdfViewer_withContentPadding_rendersCorrectly_onScrollToBottom() {
        val pages = List(3) { Point(400, 500) }
        val pdfDocument = DeterministicFakePdfDocument(pages)
        val contentPadding = PaddingValues(top = 100.dp, bottom = 200.dp)

        lateinit var pdfViewerState: PdfViewerState
        composeTestRule.setContent {
            pdfViewerState = remember { PdfViewerState() }
            PdfViewer(
                pdfDocument = pdfDocument,
                state = pdfViewerState,
                contentPadding = contentPadding,
                modifier = Modifier.testTag(PDF_VIEW_TAG).fillMaxSize(),
            )
        }

        // Wait for first content load
        composeTestRule.waitUntil(timeoutMillis = 5000) { pdfViewerState.visiblePagesCount > 0 }

        // Scroll to the bottom of the PDF.
        composeTestRule.onNodeWithTag(PDF_VIEW_TAG).performTouchInput { swipeUp() }

        composeTestRule.waitForIdle()

        // The content should have bled into the top padded area and the bottom of the PDF content
        // should have padding passed into contentPadding param.
        composeTestRule
            .onNodeWithTag(PDF_VIEW_TAG)
            .captureToImage()
            .asAndroidBitmap()
            .assertAgainstGolden(screenshotRule, PDF_VIEW_CONTENT_PADDING_SCROLL_TO_BOTTOM)
    }

    private class DeterministicFakePdfDocument(pages: List<Point>) : FakePdfDocument(pages) {
        override fun getPageBitmapSource(pageNumber: Int): PdfDocument.BitmapSource {
            return object : PdfDocument.BitmapSource {
                override val pageNumber: Int = pageNumber

                override suspend fun getBitmap(scaledPageSizePx: Size, tileRegion: Rect?): Bitmap {
                    val width = tileRegion?.width() ?: scaledPageSizePx.width
                    val height = tileRegion?.height() ?: scaledPageSizePx.height
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Use deterministic colors
                    val color =
                        when (pageNumber % 3) {
                            0 -> Color.argb(120, 255, 255, 0)
                            1 -> Color.argb(120, 0, 255, 255)
                            else -> Color.argb(120, 255, 0, 255)
                        }
                    bitmap.eraseColor(color)
                    return bitmap
                }

                override fun close() {}
            }
        }
    }

    companion object {
        internal const val SCREENSHOT_GOLDEN_DIRECTORY = "pdf/pdf-compose"
        private const val PDF_VIEW_TAG = "PdfView"
        private const val PDF_VIEW_CONTENT_PADDING_NO_SCROLL = "pdfViewer_contentPadding_no_scroll"
        private const val PDF_VIEW_CONTENT_PADDING_SCROLL_TO_BOTTOM =
            "pdfViewer_contentPadding_scroll_to_bottom"
    }
}
