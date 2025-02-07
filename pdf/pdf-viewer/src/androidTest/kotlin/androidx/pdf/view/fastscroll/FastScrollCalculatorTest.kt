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
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.pdf.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@SmallTest
@RunWith(AndroidJUnit4::class)
class FastScrollCalculatorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scrollerTopMarginDp: Int =
        context.getDimensions(R.dimen.scroller_top_margin).toInt()
    private var scrollerBottomMarginDp: Int =
        context.getDimensions(R.dimen.scroller_bottom_margin).toInt()

    @Test
    fun constrainScrollPosition_withinBounds_returnsSameValue() = runTest {
        val calculator = FastScrollCalculator(context)

        val constrainedPosition =
            calculator.constrainScrollPosition(scrollY = 200f, viewHeight = 500, thumbHeightPx = 50)

        val expectedValue = 200
        assertEquals(expectedValue, constrainedPosition)
    }

    @Test
    fun constrainScrollPosition_belowLowerBound_returnsTopMargin() = runTest {
        val calculator = FastScrollCalculator(context)

        val constrainedPosition =
            calculator.constrainScrollPosition(scrollY = -50f, viewHeight = 500, thumbHeightPx = 50)

        val expectedValue = scrollerTopMarginDp.dpToPx(context)
        assertEquals(expectedValue, constrainedPosition)
    }

    @Test
    fun constrainScrollPosition_aboveUpperBound_returnsHeightAdjustedBottomMargin() = runTest {
        val calculator = FastScrollCalculator(context)
        val viewHeight = 500
        val thumbHeightPx = 50

        val constrainedPosition =
            calculator.constrainScrollPosition(scrollY = 600f, viewHeight, thumbHeightPx)

        val expectedValue = viewHeight - (scrollerBottomMarginDp.dpToPx(context) + thumbHeightPx)
        assertEquals(expectedValue, constrainedPosition)
    }

    @Test
    fun computeThumbPosition() = runTest {
        val mockContext = mock<Context>()
        val mockResources = mock<Resources>()

        val displayMetrics = DisplayMetrics()
        displayMetrics.density = 2f
        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.displayMetrics).thenReturn(displayMetrics)

        val calculator = FastScrollCalculator(mockContext)

        val fastScrollY =
            calculator.computeThumbPosition(
                scrollY = 100,
                zoom = 1f,
                viewHeight = 500,
                thumbHeightPx = 50,
                estimatedFullHeight = 1000
            )

        val expectedScrollValue = 100
        assertEquals(expectedScrollValue, fastScrollY)
    }
}
