/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.slidingpanelayout.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcelable
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.graphics.alpha
import androidx.core.graphics.get
import androidx.core.view.get
import androidx.slidingpanelayout.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class UserResizeModeTest {
    @Test
    fun layoutWithUserResizeEnabled() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        val leftPane = spl[0]
        val rightPane = spl[1]

        assertWithMessage("leftPane width").that(leftPane.width).isEqualTo(50)
        assertWithMessage("rightPane width").that(rightPane.width).isEqualTo(50)
        assertWithMessage("isSlideable").that(spl.isSlideable).isFalse()
        assertWithMessage("SlidingPaneLayout width").that(spl.width).isEqualTo(100)
        assertWithMessage("SlidingPaneLayout height").that(spl.height).isEqualTo(100)

        spl.onTouchEvent(downEvent(50f, 50f))
        spl.onTouchEvent(moveEvent(25f, 50f))
        assertWithMessage("divider dragging").that(spl.isDividerDragging).isTrue()
        spl.onTouchEvent(upEvent(25f, 50f))
        assertWithMessage("visualDividerPosition").that(spl.visualDividerPosition).isEqualTo(30)
        assertWithMessage("SlidingPaneLayout.isLayoutRequested")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("splitDividerPosition").that(spl.splitDividerPosition).isEqualTo(30)
        assertWithMessage("leftPane width after drag").that(leftPane.width).isEqualTo(30)
        assertWithMessage("rightPane width after drag").that(rightPane.width).isEqualTo(70)
    }

    @Test
    fun layoutWithUserResizeEnabledLive() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.setUserResizeBehavior(SlidingPaneLayout.USER_RESIZE_RELAYOUT_WHEN_MOVED)
        val leftPane = spl[0]
        val rightPane = spl[1]

        assertWithMessage("leftPane width").that(leftPane.width).isEqualTo(50)
        assertWithMessage("rightPane width").that(rightPane.width).isEqualTo(50)

        spl.onTouchEvent(downEvent(50f, 50f))
        spl.onTouchEvent(moveEvent(25f, 50f))
        assertWithMessage("divider dragging").that(spl.isDividerDragging).isTrue()
        assertWithMessage("layout requested with drag in progress")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("splitDividerPosition").that(spl.splitDividerPosition).isEqualTo(30)
        assertWithMessage("leftPane width during drag").that(leftPane.width).isEqualTo(30)
        assertWithMessage("rightPane width during drag").that(rightPane.width).isEqualTo(70)

        spl.onTouchEvent(upEvent(25f, 50f))
        assertWithMessage("visualDividerPosition").that(spl.visualDividerPosition).isEqualTo(30)
        assertWithMessage("splitDividerPosition").that(spl.splitDividerPosition).isEqualTo(30)
        assertWithMessage("leftPane width after drag").that(leftPane.width).isEqualTo(30)
        assertWithMessage("rightPane width after drag").that(rightPane.width).isEqualTo(70)
    }

    @Test
    fun layoutTooSmallForPadding() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        val leftPane = spl[0]
        val rightPane = spl[1]

        spl.setPadding(4, 4, 4, 4)
        spl.measureAndLayoutForTest(0, 0)

        fun View.assertZeroSize(label: String) {
            assertWithMessage("$label width").that(width).isEqualTo(0)
            assertWithMessage("$label height").that(height).isEqualTo(0)
        }

        fun assertAllZeroSize() {
            spl.assertZeroSize("SlidingPaneLayout")
            leftPane.assertZeroSize("leftPane")
            rightPane.assertZeroSize("rightPane")
        }

        assertAllZeroSize()

        // Test different layout mode for weighted pane views
        spl.splitDividerPosition = 0
        spl.measureAndLayoutForTest(0, 0)

        assertAllZeroSize()
    }

    @Test
    fun dragDividerWithTouchCapturingPanes() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context, childPanesAcceptTouchEvents = true)

        spl.dispatchTouchEvent(downEvent(50f, 50f))
        spl.dispatchTouchEvent(moveEvent(25f, 50f))
        assertWithMessage("divider dragging").that(spl.isDividerDragging).isTrue()
        spl.dispatchTouchEvent(upEvent(25f, 50f))
        assertWithMessage("visualDividerPosition").that(spl.visualDividerPosition).isEqualTo(30)
        assertWithMessage("SlidingPaneLayout.isLayoutRequested")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("splitDividerPosition").that(spl.splitDividerPosition).isEqualTo(30)
    }

    @Test
    fun dividerClickListenerInvoked() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)

        var wasClicked = false
        spl.setOnUserResizingDividerClickListener { wasClicked = true }

        spl.onTouchEvent(downEvent(50f, 50f))
        spl.onTouchEvent(upEvent(50f, 50f))

        assertWithMessage("click listener invoked").that(wasClicked).isTrue()
    }

    @Test
    fun savedInstanceStateRestoredSameSize() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val sourceSpl = createTestSpl(context).apply { id = R.id.slidingPaneLayout }

        assertWithMessage("initial splitDividerPosition")
            .that(sourceSpl.splitDividerPosition)
            .isEqualTo(SlidingPaneLayout.SPLIT_DIVIDER_POSITION_AUTO)
        sourceSpl.splitDividerPosition = 35
        assertWithMessage("modified splitDividerPosition")
            .that(sourceSpl.splitDividerPosition)
            .isEqualTo(35)

        val savedState = SparseArray<Parcelable>()
        sourceSpl.saveHierarchyState(savedState)

        val destSpl = createTestSpl(context).apply { id = R.id.slidingPaneLayout }
        destSpl.restoreHierarchyState(savedState)

        assertWithMessage("restored splitDividerPosition")
            .that(destSpl.splitDividerPosition)
            .isEqualTo(35)
    }

    @Test
    fun visualDividerPositionClipsChildren() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.splitDividerPosition = 35
        spl.drawToBitmap()
        assertWithMessage("left child clip")
            .that((spl[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(0, 0, 35, 100))
        spl.splitDividerPosition = 65
        spl.drawToBitmap()
        assertWithMessage("right child clip")
            .that(((spl[1] as ViewGroup)[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(15, 0, 50, 100))
    }

    @Test
    fun disablingDividerClippingDoesNotClipChildren() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.splitDividerPosition = 35
        spl.isChildClippingToResizeDividerEnabled = false
        spl.drawToBitmap()
        assertWithMessage("left child clip")
            .that((spl[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(0, 0, 50, 100))
        spl.splitDividerPosition = 65
        spl.drawToBitmap()
        assertWithMessage("right child clip")
            .that(((spl[1] as ViewGroup)[0] as TestPaneView).clipBoundsAtLastDraw)
            .isEqualTo(Rect(0, 0, 50, 100))
    }

    @Test
    fun splitDividerPositionAffectsLayout() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        assertWithMessage("layout requested after SlidingPaneLayout creation")
            .that(spl.isLayoutRequested)
            .isFalse()
        spl.splitDividerPosition = 35
        assertWithMessage("layout requested by splitDividerPosition change")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("first child expected width").that(spl[0].width).isEqualTo(35)
        assertWithMessage("second child expected width").that(spl[1].width).isEqualTo(65)

        spl.splitDividerPosition = 70
        assertWithMessage("layout requested by splitDividerPosition change (2)")
            .that(spl.isLayoutRequested)
            .isTrue()
        spl.measureAndLayoutForTest()
        assertWithMessage("first child expected width").that(spl[0].width).isEqualTo(70)
        assertWithMessage("second child expected width").that(spl[1].width).isEqualTo(30)
    }

    @Test
    fun splitDividerPositionDoesNotChangePadding() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.setPadding(4, 0, 10, 0)
        spl.measureAndLayoutForTest()
        assertWithMessage("left padding").that(spl.paddingLeft).isEqualTo(4)
        assertWithMessage("right padding").that(spl.paddingRight).isEqualTo(10)

        fun testPadding(description: String) {
            assertWithMessage("left edge of first child $description")
                .that(spl[0].left)
                .isEqualTo(4)
            assertWithMessage("right edge of second child $description")
                .that(spl[1].right)
                .isEqualTo(spl.width - 10)
        }

        testPadding("before divider position change")
        val firstChildExpectedWidth = spl[0].width
        val secondChildExpectedWidth = spl[1].width
        spl.splitDividerPosition = spl.visualDividerPosition
        spl.measureAndLayoutForTest()
        assertWithMessage("first child width").that(spl[0].width).isEqualTo(firstChildExpectedWidth)
        assertWithMessage("second child width")
            .that(spl[1].width)
            .isEqualTo(secondChildExpectedWidth)
        testPadding("after divider position change")
    }

    @Test
    fun zeroSpaceForOnePane() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.splitDividerPosition = 0
        spl.measureAndLayoutForTest()
        assertWithMessage("left pane has zero width").that(spl[0].width).isEqualTo(0)
        spl.splitDividerPosition = spl.width
        spl.measureAndLayoutForTest()
        assertWithMessage("right pane has zero width").that(spl[1].width).isEqualTo(0)
    }

    @Test
    fun zeroSpaceForOnePanePaddedLayout() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)
        spl.splitDividerPosition = 0
        spl.setPadding(4, 0, 8, 0)
        spl.measureAndLayoutForTest()
        assertWithMessage("left pane has zero width").that(spl[0].width).isEqualTo(0)
        spl.splitDividerPosition = spl.width
        spl.measureAndLayoutForTest()
        assertWithMessage("right pane has zero width").that(spl[1].width).isEqualTo(0)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun gestureExclusionRectsUpdated() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context)

        fun View.assertHasExclusionRects() {
            assertWithMessage("systemGestureExclusionRects.size")
                .that(systemGestureExclusionRects.size)
                .isAtLeast(1)
        }

        val initialExclusionRect = Rect()
        spl.assertHasExclusionRects()
        // assumption: the divider rect is always the first in this config
        initialExclusionRect.set(spl.systemGestureExclusionRects[0])

        spl.splitDividerPosition = 0
        spl.measureAndLayoutForTest()
        spl.assertHasExclusionRects()
        val dividerZeroExclusionRect = Rect().apply { set(spl.systemGestureExclusionRects[0]) }

        assertWithMessage("initial rect/divider zero rect")
            .that(dividerZeroExclusionRect)
            .isNotEqualTo(initialExclusionRect)

        val hMidpoint = spl.height / 2
        assertWithMessage("exclusion $dividerZeroExclusionRect contains (0, $hMidpoint)")
            .that(dividerZeroExclusionRect.contains(0, hMidpoint))
            .isTrue()

        spl.splitDividerPosition = spl.width
        spl.measureAndLayoutForTest()
        val dividerWidthExclusionRect = Rect().apply { set(spl.systemGestureExclusionRects[0]) }

        assertWithMessage("exclusion $dividerZeroExclusionRect contains (${spl.width}, $hMidpoint)")
            .that(dividerWidthExclusionRect.contains(spl.width, hMidpoint))
            .isTrue()

        spl.isUserResizingEnabled = false
        spl.measureAndLayoutForTest()
        assertWithMessage("gesture exclusion omitted for isUserResizingEnabled = false")
            .that(spl.systemGestureExclusionRects.size)
            .isEqualTo(0)
    }

    @Test
    fun defaultDividerDraws() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val spl = createTestSpl(context, setDividerDrawable = false)
        val bitmap = spl.drawToBitmap()
        var hasNonTransparentPixel = false
        outer@ for (h in 0 until bitmap.height) {
            for (w in 0 until bitmap.width) {
                if (bitmap[w, h].alpha > 0) {
                    hasNonTransparentPixel = true
                    break@outer
                }
            }
        }
        assertWithMessage("non-transparent pixels were drawn").that(hasNonTransparentPixel).isTrue()
    }
}

private fun View.drawToBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

private fun createTestSpl(
    context: Context,
    setDividerDrawable: Boolean = true,
    childPanesAcceptTouchEvents: Boolean = false
): SlidingPaneLayout =
    SlidingPaneLayout(context).apply {
        addView(
            TestPaneView(context).apply {
                minimumWidth = 30
                acceptTouchEvents = childPanesAcceptTouchEvents
                layoutParams =
                    SlidingPaneLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.MATCH_PARENT
                        )
                        .apply { weight = 1f }
            }
        )
        addView(
            TestPaneView(context).apply {
                minimumWidth = 30
                acceptTouchEvents = childPanesAcceptTouchEvents
                layoutParams =
                    SlidingPaneLayout.LayoutParams(
                            LayoutParams.WRAP_CONTENT,
                            LayoutParams.MATCH_PARENT
                        )
                        .apply { weight = 1f }
            }
        )
        isUserResizingEnabled = true
        isOverlappingEnabled = false
        if (setDividerDrawable) {
            setUserResizingDividerDrawable(TestDividerDrawable())
        }
        measureAndLayoutForTest()
    }

private fun View.measureAndLayoutForTest(width: Int = 100, height: Int = 100) {
    measure(
        MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
    )
    layout(0, 0, measuredWidth, measuredHeight)
}

private class TestDividerDrawable(
    private val intrinsicWidth: Int = 10,
    private val intrinsicHeight: Int = 20
) : Drawable() {

    override fun draw(canvas: Canvas) {}

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java") override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = intrinsicWidth

    override fun getIntrinsicHeight(): Int = intrinsicHeight
}

private class TestPaneView(context: Context) : View(context) {
    val clipBoundsAtLastDraw = Rect()

    var acceptTouchEvents = false

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event) || acceptTouchEvents
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        setMeasuredDimension(
            when (widthMode) {
                MeasureSpec.EXACTLY -> widthSize
                MeasureSpec.AT_MOST -> suggestedMinimumWidth.coerceAtMost(widthSize)
                MeasureSpec.UNSPECIFIED -> suggestedMinimumWidth
                else -> error("bad width mode $widthMode")
            },
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> suggestedMinimumHeight.coerceAtMost(heightSize)
                MeasureSpec.UNSPECIFIED -> suggestedMinimumHeight
                else -> error("bad width mode $heightMode")
            }
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.getClipBounds(clipBoundsAtLastDraw)
    }
}

/** Create a test [MotionEvent]; this will have bogus time values, no history */
private fun motionEvent(
    action: Int,
    x: Float,
    y: Float,
) = MotionEvent.obtain(0L, 0L, action, x, y, 0)

private fun downEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_DOWN, x, y)

private fun moveEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_MOVE, x, y)

private fun upEvent(x: Float, y: Float) = motionEvent(MotionEvent.ACTION_UP, x, y)

private fun cancelEvent() = motionEvent(MotionEvent.ACTION_CANCEL, 0f, 0f)
