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
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import androidx.pdf.PdfDocument
import androidx.pdf.R
import androidx.pdf.view.FakePdfDocument
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class FastScrollGestureDetectorTest {
    private lateinit var gestureDetector: FastScrollGestureDetector

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pdfDocument: PdfDocument = FakePdfDocument.newInstance()
    private val thumbDrawable =
        ContextCompat.getDrawable(context, R.drawable.fastscroll_background)!!
    private val pageIndicatorBackgroundDrawable =
        ContextCompat.getDrawable(context, R.drawable.page_indicator_background)!!
    private val fastScrollVerticalThumbMarginEnd = 0
    private val fastScrollPageIndicatorMarginEnd =
        context.getDimensions(R.dimen.page_indicator_right_margin).toInt()
    private val fastScrollDrawer =
        FastScrollDrawer(
            context,
            pdfDocument,
            thumbDrawable,
            pageIndicatorBackgroundDrawable,
            fastScrollVerticalThumbMarginEnd,
            fastScrollPageIndicatorMarginEnd,
        )
    private val fastScrollCalculator = FastScrollCalculator(context)
    private val fastScroller = FastScroller(fastScrollDrawer, fastScrollCalculator)

    private val gestureHandler = spy(FakeFastScrollGestureHandler())

    @Before
    fun setup() {
        gestureDetector = FastScrollGestureDetector(fastScroller, gestureHandler)
    }

    @Test
    fun testHandleEvent_actionDown_outsideBounds_doesNotTrack() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        val viewWidth = 500

        val result = gestureDetector.handleEvent(event, parent = null, viewWidth)

        assertFalse(result)
        assertFalse(
            gestureDetector.handleEvent(
                MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 10f, 10f, 0),
                parent = null,
                viewWidth,
            )
        )
        verify(gestureHandler, never()).onFastScrollDetected(10f)
    }

    @Test
    fun testHandleEvent_actionDown_withinBounds_tracksAndHandlesMove() {
        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 450f, 50f, 0)
        val viewWidth = 500

        val result = gestureDetector.handleEvent(downEvent, parent = null, viewWidth)

        assertTrue(result)

        val moveEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 450f, 400f, 0)
        assertTrue(gestureDetector.handleEvent(moveEvent, parent = null, viewWidth))
        verify(gestureHandler).onFastScrollDetected(400f)
    }

    @Test
    fun testHandleEvent_actionUp_stopsTracking() {
        val downEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 450f, 50f, 0)
        val viewWidth = 500
        gestureDetector.handleEvent(downEvent, parent = null, viewWidth)

        val moveEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 450f, 400f, 0)
        assertTrue(gestureDetector.handleEvent(moveEvent, parent = null, viewWidth))
        verify(gestureHandler).onFastScrollDetected(400f)

        val upEvent = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_UP, 450f, 400f, 0)
        assertTrue(gestureDetector.handleEvent(upEvent, parent = null, viewWidth))
    }

    open class FakeFastScrollGestureHandler : FastScrollGestureDetector.FastScrollGestureHandler {
        var lastScrollY: Float = 0f

        override fun onFastScrollDetected(eventY: Float) {
            lastScrollY = eventY
        }
    }
}
