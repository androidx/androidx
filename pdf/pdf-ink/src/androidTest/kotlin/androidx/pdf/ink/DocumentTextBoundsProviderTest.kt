/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.pdf.ink

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import androidx.pdf.FakeEditablePdfDocument
import androidx.pdf.content.PdfPageTextContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class DocumentTextBoundsProviderTest {

    @Test
    fun getTextBoundsBetweenPoints_withText_returnsCorrectRects() = runTest {
        val pageText =
            PdfPageTextContent(bounds = listOf(RectF(10f, 10f, 100f, 100f)), text = "Sample Text")
        val fakePdfDocument =
            FakeEditablePdfDocument(
                pages = listOf(Point(500, 500)),
                textContents = listOf(pageText),
            )
        val provider = DocumentTextBoundsProvider(fakePdfDocument)
        val startPoint = PointF(20f, 20f)
        val endPoint = PointF(80f, 80f)

        // For now, FakeEditablePdfDocument returns the whole pageText bounds regardless of points
        val rects = provider.getTextBoundsBetweenPoints(0, startPoint, endPoint)

        assertThat(rects).hasSize(1)
        assertThat(rects[0]).isEqualTo(RectF(10f, 10f, 100f, 100f))
    }

    @Test
    fun getTextBoundsBetweenPoints_featureNotSupported_returnsEmptyList() = runTest {
        val fakePdfDocument =
            FakeEditablePdfDocument(
                pages = listOf(Point(500, 500)),
                unsupportedFeatures = setOf(androidx.pdf.PdfFeature.TEXT_SELECTION),
            )
        val provider = DocumentTextBoundsProvider(fakePdfDocument)
        val startPoint = PointF(20f, 20f)
        val endPoint = PointF(80f, 80f)

        val rects = provider.getTextBoundsBetweenPoints(0, startPoint, endPoint)

        assertThat(rects).isEmpty()
    }

    @Test
    fun getTextBoundsBetweenPoints_noText_returnsEmptyList() = runTest {
        val fakePdfDocument =
            FakeEditablePdfDocument(pages = listOf(Point(500, 500)), textContents = emptyList())
        val provider = DocumentTextBoundsProvider(fakePdfDocument)
        val startPoint = PointF(20f, 20f)
        val endPoint = PointF(80f, 80f)

        val rects = provider.getTextBoundsBetweenPoints(0, startPoint, endPoint)

        assertThat(rects).isEmpty()
    }

    @Test(expected = Exception::class)
    fun getTextBoundsBetweenPoints_throwsExceptionOnFailure() = runTest {
        // Create a fake that throws when getSelectionBounds is called
        val fakePdfDocument =
            object : FakeEditablePdfDocument(pages = listOf(Point(500, 500))) {
                override suspend fun getSelectionBounds(
                    pageNumber: Int,
                    start: PointF,
                    stop: PointF,
                ) = throw Exception("Failure")
            }
        val provider = DocumentTextBoundsProvider(fakePdfDocument)
        provider.getTextBoundsBetweenPoints(0, PointF(0f, 0f), PointF(10f, 10f))
    }
}
