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

package androidx.pdf.view.fastscroll

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Range
import androidx.core.content.ContextCompat
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.view.FakePdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class FastScrollDrawerTest {
    private lateinit var context: Context

    private lateinit var pdfDocument: PdfDocument
    private lateinit var thumbDrawable: Drawable
    private lateinit var pageIndicatorBackgroundDrawable: Drawable
    private lateinit var spyCanvas: Canvas
    private lateinit var fastScrollDrawer: FastScrollDrawer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        pdfDocument = FakePdfDocument.newInstance()
        thumbDrawable = spy(ContextCompat.getDrawable(context, R.drawable.fastscroll_background)!!)
        pageIndicatorBackgroundDrawable =
            ContextCompat.getDrawable(context, R.drawable.page_indicator_background)!!
        val fastScrollVerticalThumbMarginEnd = 0
        val fastScrollPageIndicatorMarginEnd =
            context.getDimensions(R.dimen.page_indicator_right_margin).toInt()
        spyCanvas = spy(Canvas())

        fastScrollDrawer =
            FastScrollDrawer(
                context,
                pdfDocument,
                thumbDrawable,
                pageIndicatorBackgroundDrawable,
                fastScrollVerticalThumbMarginEnd,
                fastScrollPageIndicatorMarginEnd,
            )
    }

    @Test
    fun draw_withinVisibleArea_verifyDrawOnCanvas() {
        val xOffset = 500
        val yOffset = 100
        val visiblePages = Range(1, 5)

        fastScrollDrawer.draw(spyCanvas, xOffset, yOffset, visiblePages)

        val leftCaptor = ArgumentCaptor.forClass(Int::class.java)
        val topCaptor = ArgumentCaptor.forClass(Int::class.java)
        val rightCaptor = ArgumentCaptor.forClass(Int::class.java)
        val bottomCaptor = ArgumentCaptor.forClass(Int::class.java)
        verify(thumbDrawable)
            .setBounds(
                leftCaptor.capture(),
                topCaptor.capture(),
                rightCaptor.capture(),
                bottomCaptor.capture(),
            )
        verify(thumbDrawable).draw(spyCanvas)

        val expectedLeftRange = Range(350, 450)
        val expectedTopRange = Range(0, 100)
        val expectedRightRange = Range(450, 550)
        val expectedBottomRange = Range(100, 300)
        assertTrue(expectedLeftRange.contains(leftCaptor.value))
        assertTrue(expectedTopRange.contains(topCaptor.value))
        assertTrue(expectedRightRange.contains(rightCaptor.value))
        assertTrue(expectedBottomRange.contains(bottomCaptor.value))

        val textCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(spyCanvas).drawText(textCaptor.capture(), anyFloat(), anyFloat(), any())

        // Hyphens are being interpreted in unicode rather than ascii which is failing the assertion
        // hence using indices to find characters
        val pageIndicatorLabels = textCaptor.value.toString().trim().split('/')
        val pageRange = pageIndicatorLabels[0].trim()
        val totalPages = pageIndicatorLabels[1].trim()

        val expectedLowerPageRange = 2
        val expectedUpperPageRange = 6
        val expectedTotalPages = 10
        assertEquals(expectedLowerPageRange, pageRange[0].toString().toInt())
        assertEquals(expectedUpperPageRange, pageRange[2].toString().toInt())
        assertEquals(expectedTotalPages, totalPages.toInt())
    }

    @Test
    fun testFastScroll_draw_verifyThumbBounds() {
        val xOffset = 500
        val yOffset = 100
        val visiblePages = Range(1, 5)

        fastScrollDrawer.draw(spyCanvas, xOffset, yOffset, visiblePages)

        val expectedThumbLeftRange = Range(400, 600)
        val expectedThumbTopRange = Range(100, 200)
        val expectedThumbRightRange = Range(500, 700)
        val expectedThumbBottomRange = Range(100, 300)

        assertTrue(expectedThumbLeftRange.contains(fastScrollDrawer.thumbBounds.left.toInt()))
        assertTrue(expectedThumbTopRange.contains(fastScrollDrawer.thumbBounds.top.toInt()))
        assertTrue(expectedThumbRightRange.contains(fastScrollDrawer.thumbBounds.right.toInt()))
        assertTrue(expectedThumbBottomRange.contains(fastScrollDrawer.thumbBounds.bottom.toInt()))
    }

    @Test
    fun testFastScroll_draw_verifyPageIndicatorBounds() {
        val xOffset = 500
        val yOffset = 100
        val visiblePages = Range(1, 5)

        fastScrollDrawer.draw(spyCanvas, xOffset, yOffset, visiblePages)

        val expectedIndicatorLeftRange = Range(150, 450)
        val expectedIndicatorTopRange = Range(100, 250)
        val expectedIndicatorRightRange = Range(300, 650)
        val expectedIndicatorBottomRange = Range(150, 300)

        assertTrue(
            expectedIndicatorLeftRange.contains(fastScrollDrawer.pageIndicatorBounds.left.toInt())
        )
        assertTrue(
            expectedIndicatorTopRange.contains(fastScrollDrawer.pageIndicatorBounds.top.toInt())
        )
        assertTrue(
            expectedIndicatorRightRange.contains(fastScrollDrawer.pageIndicatorBounds.right.toInt())
        )
        assertTrue(
            expectedIndicatorBottomRange.contains(
                fastScrollDrawer.pageIndicatorBounds.bottom.toInt()
            )
        )
    }
}
