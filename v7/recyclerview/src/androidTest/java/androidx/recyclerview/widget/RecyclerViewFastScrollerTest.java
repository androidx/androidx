/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.StateListDrawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewFastScrollerTest extends BaseRecyclerViewInstrumentationTest {
    private static final int FLAG_HORIZONTAL = 1;
    private static final int FLAG_VERTICAL = 1 << 1;
    private int mScrolledByY = -1000;
    private int mScrolledByX = -1000;
    private FastScroller mScroller;
    private boolean mHide;

    private void setContentView(final int layoutId) throws Throwable {
        final Activity activity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(layoutId);
            }
        });
    }

    @Test
    public void xml_fastScrollEnabled_startsInvisibleAndAtTop() throws Throwable {
        arrangeWithXml();

        assertTrue("Expected centerY to start == 0", mScroller.mVerticalThumbCenterY == 0);
        assertFalse("Expected thumb to start invisible", mScroller.isVisible());
    }

    @Test
    public void scrollBy_displaysAndMovesFastScrollerThumb() throws Throwable {
        arrangeWithXml();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollBy(0, 400);
            }
        });

        assertTrue("Expected centerY to be > 0" + mScroller.mVerticalThumbCenterY,
                mScroller.mVerticalThumbCenterY > 0);
        assertTrue("Expected thumb to be visible", mScroller.isVisible());
    }

    @Test
    public void ui_dragsThumb_scrollsRecyclerView() throws Throwable {
        arrangeWithXml();

        // RecyclerView#scrollBy(int, int) used to cause the scroller thumb to show up.
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollBy(0, 1);
                mRecyclerView.scrollBy(0, -1);
            }
        });
        int[] absoluteCoords = new int[2];
        mRecyclerView.getLocationOnScreen(absoluteCoords);
        TouchUtils.drag(InstrumentationRegistry.getInstrumentation(), mRecyclerView.getWidth() - 10,
                mRecyclerView.getWidth() - 10, mScroller.mVerticalThumbCenterY + absoluteCoords[1],
                mRecyclerView.getHeight() + absoluteCoords[1], 100);

        assertTrue("Expected dragging thumb to move recyclerView",
                mRecyclerView.computeVerticalScrollOffset() > 0);
    }

    @Test
    public void properCleanUp() throws Throwable {
        mRecyclerView = new RecyclerView(getActivity());
        final Activity activity = mActivityRule.getActivity();
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(
                        androidx.recyclerview.test.R.layout.fast_scrollbar_test_rv);
                mRecyclerView = (RecyclerView) activity.findViewById(
                        androidx.recyclerview.test.R.id.recycler_view);
                LinearLayoutManager layout = new LinearLayoutManager(activity.getBaseContext());
                layout.setOrientation(VERTICAL);
                mRecyclerView.setLayoutManager(layout);
                mRecyclerView.setAdapter(new TestAdapter(50));
                Resources res = getActivity().getResources();
                mScroller = new FastScroller(mRecyclerView, (StateListDrawable) res.getDrawable(
                        androidx.recyclerview.test.R.drawable.fast_scroll_thumb_drawable),
                        res.getDrawable(
                                androidx.recyclerview.test.R.drawable
                                        .fast_scroll_track_drawable),
                        (StateListDrawable) res.getDrawable(
                                androidx.recyclerview.test.R.drawable
                                        .fast_scroll_thumb_drawable),
                        res.getDrawable(
                                androidx.recyclerview.test.R.drawable
                                        .fast_scroll_track_drawable),
                        res.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
                        res.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
                        res.getDimensionPixelOffset(R.dimen.fastscroll_margin)) {
                    @Override
                    public void show() {
                        // Overriden to avoid animation calls in instrumentation thread
                    }

                    @Override
                    public void hide(int duration) {
                        latch.countDown();
                        mHide = true;
                    }
                };

            }
        });
        waitForIdleScroll(mRecyclerView);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollBy(0, 400);
                mScroller.attachToRecyclerView(new RecyclerView(getActivity()));
            }
        });
        assertFalse(latch.await(2, TimeUnit.SECONDS));
        assertFalse(mHide);
    }

    @Test
    public void inflationTest() throws Throwable {
        setContentView(androidx.recyclerview.test.R.layout.fast_scrollbar_test_rv);
        getInstrumentation().waitForIdleSync();
        RecyclerView view = (RecyclerView) getActivity().findViewById(
                androidx.recyclerview.test.R.id.recycler_view);
        assertTrue(view.getItemDecorationCount() == 1);
        assertTrue(view.getItemDecorationAt(0) instanceof FastScroller);
        FastScroller scroller = (FastScroller) view.getItemDecorationAt(0);
        assertNotNull(scroller.getHorizontalThumbDrawable());
        assertNotNull(scroller.getHorizontalTrackDrawable());
        assertNotNull(scroller.getVerticalThumbDrawable());
        assertNotNull(scroller.getVerticalTrackDrawable());
    }

    @Test
    public void removeFastScrollerSuccessful() throws Throwable {
        setContentView(androidx.recyclerview.test.R.layout.fast_scrollbar_test_rv);
        getInstrumentation().waitForIdleSync();
        final RecyclerView view = (RecyclerView) getActivity().findViewById(
                androidx.recyclerview.test.R.id.recycler_view);
        assertTrue(view.getItemDecorationCount() == 1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.removeItemDecorationAt(0);
                assertTrue(view.getItemDecorationCount() == 0);
            }
        });
    }

    @UiThreadTest
    @Test
    public void initWithBadDrawables() throws Throwable {
        arrangeWithCode();

        Throwable exception = null;
        try {
            mRecyclerView.initFastScroller(null, null, null, null);
        } catch (Throwable t) {
            exception = t;
        }
        assertTrue(exception instanceof IllegalArgumentException);
    }

    @Test
    public void verticalScrollUpdatesFastScrollThumb() throws Throwable {
        scrollUpdatesFastScrollThumb(FLAG_VERTICAL);
    }

    @Test
    public void horizontalScrollUpdatesFastScrollThumb() throws Throwable {
        scrollUpdatesFastScrollThumb(FLAG_HORIZONTAL);
    }

    private void scrollUpdatesFastScrollThumb(int direction) throws Throwable {
        arrangeWithCode();
        mScroller.updateScrollPosition(direction == FLAG_VERTICAL ? 0 : 250,
                direction == FLAG_VERTICAL ? 250 : 0);
        if (direction == FLAG_VERTICAL) {
            assertTrue("Expected 250 for centerY, got " + mScroller.mVerticalThumbCenterY,
                    mScroller.mVerticalThumbCenterY == 250);
            assertTrue("Expected 250 for thumb height, got " + mScroller.mVerticalThumbHeight,
                    mScroller.mVerticalThumbHeight == 250);
        } else if (direction == FLAG_HORIZONTAL) {
            assertTrue("Expected 250 for centerX, got " + mScroller.mHorizontalThumbCenterX,
                    mScroller.mHorizontalThumbCenterX == 250);
            assertTrue("Expected 250 for thumb width, got " + mScroller.mHorizontalThumbWidth,
                    mScroller.mHorizontalThumbWidth == 250);
        }
        assertTrue(mScroller.isVisible());

        mScroller.updateScrollPosition(direction == FLAG_VERTICAL ? 0 : 42,
                direction == FLAG_VERTICAL ? 42 : 0);
        if (direction == FLAG_VERTICAL) {
            assertTrue("Expected 146 for centerY, got " + mScroller.mVerticalThumbCenterY,
                    mScroller.mVerticalThumbCenterY == 146);
            assertTrue("Expected 250 for thumb height, got " + mScroller.mVerticalThumbHeight,
                    mScroller.mVerticalThumbHeight == 250);
        } else if (direction == FLAG_HORIZONTAL) {
            assertTrue("Expected 146 for centerX, got " + mScroller.mHorizontalThumbCenterX,
                    mScroller.mHorizontalThumbCenterX == 146);
            assertTrue("Expected 250 for thumb width, got " + mScroller.mHorizontalThumbWidth,
                    mScroller.mHorizontalThumbWidth == 250);
        }
        assertTrue(mScroller.isVisible());
    }

    @Test
    public void draggingDoesNotTriggerFastScrollIfNotInThumb() throws Throwable {
        arrangeWithCode();
        mScroller.updateScrollPosition(0, 250);
        final MotionEvent downEvent = MotionEvent.obtain(10, 10, MotionEvent.ACTION_DOWN, 250, 250,
                0);
        assertFalse(mScroller.onInterceptTouchEvent(mRecyclerView, downEvent));
        final MotionEvent moveEvent = MotionEvent.obtain(10, 10, MotionEvent.ACTION_MOVE, 250, 275,
                0);
        assertFalse(mScroller.onInterceptTouchEvent(mRecyclerView, moveEvent));
    }

    @Test
    public void verticalDraggingFastScrollThumbDoesActualScrolling() throws Throwable {
        draggingFastScrollThumbDoesActualScrolling(FLAG_VERTICAL);
    }

    @Test
    public void horizontalDraggingFastScrollThumbDoesActualScrolling() throws Throwable {
        draggingFastScrollThumbDoesActualScrolling(FLAG_HORIZONTAL);
    }

    private void draggingFastScrollThumbDoesActualScrolling(int direction) throws Throwable {
        arrangeWithCode();
        mScroller.updateScrollPosition(direction == FLAG_VERTICAL ? 0 : 250,
                direction == FLAG_VERTICAL ? 250 : 0);
        final MotionEvent downEvent = MotionEvent.obtain(10, 10, MotionEvent.ACTION_DOWN,
                direction == FLAG_VERTICAL ? 500 : 250, direction == FLAG_VERTICAL ? 250 : 500, 0);
        assertTrue(mScroller.onInterceptTouchEvent(mRecyclerView, downEvent));
        assertTrue(mScroller.isDragging());
        final MotionEvent moveEvent = MotionEvent.obtain(10, 10, MotionEvent.ACTION_MOVE,
                direction == FLAG_VERTICAL ? 500 : 221, direction == FLAG_VERTICAL ? 221 : 500, 0);
        mScroller.onTouchEvent(mRecyclerView, moveEvent);
        if (direction == FLAG_VERTICAL) {
            assertTrue("Expected to get -29, but got " + mScrolledByY, mScrolledByY == -29);
        } else {
            assertTrue("Expected to get -29, but got " + mScrolledByX, mScrolledByX == -29);
        }
    }

    private void arrangeWithXml() throws Throwable {

        final TestActivity activity = mActivityRule.getActivity();
        final TestedFrameLayout testedFrameLayout = activity.getContainer();

        RecyclerView recyclerView = (RecyclerView) LayoutInflater.from(activity).inflate(
                androidx.recyclerview.test.R.layout.fast_scrollbar_test_rv,
                testedFrameLayout,
                false);

        LinearLayoutManager layout = new LinearLayoutManager(activity.getBaseContext());
        layout.setOrientation(VERTICAL);
        recyclerView.setLayoutManager(layout);

        recyclerView.setAdapter(new TestAdapter(50));

        mScroller = (FastScroller) recyclerView.getItemDecorationAt(0);

        testedFrameLayout.expectLayouts(1);
        testedFrameLayout.expectDraws(1);
        setRecyclerView(recyclerView);
        testedFrameLayout.waitForLayout(2);
        testedFrameLayout.waitForDraw(2);
    }

    private void arrangeWithCode() throws Exception {
        final int width = 500;
        final int height = 500;

        mRecyclerView = new RecyclerView(getActivity()) {
            @Override
            public int computeVerticalScrollRange() {
                return 1000;
            }

            @Override
            public int computeVerticalScrollExtent() {
                return 500;
            }

            @Override
            public int computeVerticalScrollOffset() {
                return 250;
            }

            @Override
            public int computeHorizontalScrollRange() {
                return 1000;
            }

            @Override
            public int computeHorizontalScrollExtent() {
                return 500;
            }

            @Override
            public int computeHorizontalScrollOffset() {
                return 250;
            }

            @Override
            public void scrollBy(int x, int y) {
                mScrolledByY = y;
                mScrolledByX = x;
            }
        };
        mRecyclerView.setAdapter(new TestAdapter(50));
        mRecyclerView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        mRecyclerView.layout(0, 0, width, height);

        Resources res = getActivity().getResources();
        mScroller = new FastScroller(mRecyclerView, (StateListDrawable) res.getDrawable(
                androidx.recyclerview.test.R.drawable.fast_scroll_thumb_drawable),
                res.getDrawable(
                        androidx.recyclerview.test.R.drawable.fast_scroll_track_drawable),
                (StateListDrawable) res.getDrawable(
                        androidx.recyclerview.test.R.drawable.fast_scroll_thumb_drawable),
                res.getDrawable(
                        androidx.recyclerview.test.R.drawable.fast_scroll_track_drawable),
                res.getDimensionPixelSize(R.dimen.fastscroll_default_thickness),
                res.getDimensionPixelSize(R.dimen.fastscroll_minimum_range),
                res.getDimensionPixelOffset(R.dimen.fastscroll_margin)) {
            @Override
            public void show() {
                // Overriden to avoid animation calls in instrumentation thread
            }

            @Override
            public void hide(int duration) {
                mHide = true;
            }
        };
        mRecyclerView.mEnableFastScroller = true;

        // Draw it once so height/width gets updated
        mScroller.onDrawOver(null, mRecyclerView, null);
    }

    private static class TestAdapter extends RecyclerView.Adapter {
        private int mItemCount;

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView mTextView;

            ViewHolder(TextView v) {
                super(v);
                mTextView = v;
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mTextView.getText();
            }
        }

        TestAdapter(int itemCount) {
            mItemCount = itemCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            final ViewHolder h = new ViewHolder(new TextView(parent.getContext()));
            h.mTextView.setMinimumHeight(128);
            h.mTextView.setPadding(20, 0, 20, 0);
            h.mTextView.setFocusable(true);
            h.mTextView.setBackgroundColor(Color.BLUE);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.leftMargin = 10;
            lp.rightMargin = 5;
            lp.topMargin = 20;
            lp.bottomMargin = 15;
            h.mTextView.setLayoutParams(lp);
            return h;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            holder.itemView.setTag("pos " + position);
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }
}
