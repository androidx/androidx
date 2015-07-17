/*
 * Copyright (C) 2014 The Android Open Source Project
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


package android.support.v7.widget;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.test.TouchUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static android.support.v7.widget.RecyclerView.NO_POSITION;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static android.support.v7.widget.RecyclerView.getChildViewHolderInt;
import android.support.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class RecyclerViewLayoutTest extends BaseRecyclerViewInstrumentationTest {
    private static final int FLAG_HORIZONTAL = 1;
    private static final int FLAG_VERTICAL = 1 << 1;
    private static final int FLAG_FLING = 1 << 2;

    private static final boolean DEBUG = true;

    private static final String TAG = "RecyclerViewLayoutTest";

    public RecyclerViewLayoutTest() {
        super(DEBUG);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testFlingFrozen() throws Throwable {
        testScrollFrozen(true);
    }

    @Test
    public void testDragFrozen() throws Throwable {
        testScrollFrozen(false);
    }

    private void testScrollFrozen(boolean fling) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());

        final int horizontalScrollCount = 3;
        final int verticalScrollCount = 3;
        final int horizontalVelocity = 1000;
        final int verticalVelocity = 1000;
        final AtomicInteger horizontalCounter = new AtomicInteger(horizontalScrollCount);
        final AtomicInteger verticalCounter = new AtomicInteger(verticalScrollCount);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean canScrollHorizontally() {
                return true;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (verticalCounter.get() > 0) {
                    verticalCounter.decrementAndGet();
                    return dy;
                }
                return 0;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (horizontalCounter.get() > 0) {
                    horizontalCounter.decrementAndGet();
                    return dx;
                }
                return 0;
            }
        };
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);

        freezeLayout(true);

        if (fling) {
            assertFalse("fling should be blocked", fling(horizontalVelocity, verticalVelocity));
        } else { // drag
            TouchUtils.dragViewTo(this, recyclerView,
                    Gravity.LEFT | Gravity.TOP,
                    mRecyclerView.getWidth() / 2, mRecyclerView.getHeight() / 2);
        }
        assertEquals("rv's horizontal scroll cb must not run", horizontalScrollCount,
                horizontalCounter.get());
        assertEquals("rv's vertical scroll cb must not run", verticalScrollCount,
                verticalCounter.get());

        freezeLayout(false);

        if (fling) {
            assertTrue("fling should be started", fling(horizontalVelocity, verticalVelocity));
        } else { // drag
            TouchUtils.dragViewTo(this, recyclerView,
                    Gravity.LEFT | Gravity.TOP,
                    mRecyclerView.getWidth() / 2, mRecyclerView.getHeight() / 2);
        }
        assertEquals("rv's horizontal scroll cb must finishes", 0, horizontalCounter.get());
        assertEquals("rv's vertical scroll cb must finishes", 0, verticalCounter.get());
    }

    @Test
    public void testFocusSearchFailFrozen() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());

        final AtomicInteger focusSearchCalled = new AtomicInteger(0);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean canScrollHorizontally() {
                return true;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public View onFocusSearchFailed(View focused, int direction, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                focusSearchCalled.addAndGet(1);
                return null;
            }
        };
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);

        final View c = recyclerView.getChildAt(recyclerView.getChildCount() - 1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                c.requestFocus();
            }
        });
        assertTrue(c.hasFocus());

        freezeLayout(true);
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals("onFocusSearchFailed should not be called when layout is frozen",
                0, focusSearchCalled.get());

        freezeLayout(false);
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        assertEquals(1, focusSearchCalled.get());
    }

    @Test
    public void testFrozenAndChangeAdapter() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());

        final AtomicInteger focusSearchCalled = new AtomicInteger(0);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean canScrollHorizontally() {
                return true;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public View onFocusSearchFailed(View focused, int direction, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                focusSearchCalled.addAndGet(1);
                return null;
            }
        };
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);

        freezeLayout(true);
        TestAdapter adapter2 = new TestAdapter(1000);
        setAdapter(adapter2);
        assertFalse(recyclerView.isLayoutFrozen());
        assertSame(adapter2, recyclerView.getAdapter());

        freezeLayout(true);
        TestAdapter adapter3 = new TestAdapter(1000);
        swapAdapter(adapter3, true);
        assertFalse(recyclerView.isLayoutFrozen());
        assertSame(adapter3, recyclerView.getAdapter());
    }

    @Test
    public void testScrollToPositionCallback() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager tlm = new TestLayoutManager() {
            int scrollPos = RecyclerView.NO_POSITION;

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutLatch.countDown();
                if (scrollPos == RecyclerView.NO_POSITION) {
                    layoutRange(recycler, 0, 10);
                } else {
                    layoutRange(recycler, scrollPos, scrollPos + 10);
                }
            }

            @Override
            public void scrollToPosition(int position) {
                scrollPos = position;
                requestLayout();
            }
        };
        recyclerView.setLayoutManager(tlm);
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        final AtomicInteger rvCounter = new AtomicInteger(0);
        final AtomicInteger viewGroupCounter = new AtomicInteger(0);
        recyclerView.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        viewGroupCounter.incrementAndGet();
                    }
                });
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                rvCounter.incrementAndGet();
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        tlm.expectLayouts(1);

        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);
        // wait for draw :/
        Thread.sleep(1000);

        assertEquals("RV on scroll should be called for initialization", 1, rvCounter.get());
        assertEquals("VTO on scroll should be called for initialization", 1,
                viewGroupCounter.get());
        tlm.expectLayouts(1);
        freezeLayout(true);
        scrollToPosition(3);
        tlm.assertNoLayout("scrollToPosition should be ignored", 2);
        freezeLayout(false);
        scrollToPosition(3);
        tlm.waitForLayout(2);
        assertEquals("RV on scroll should be called", 2, rvCounter.get());
        assertEquals("VTO on scroll should be called", 2, viewGroupCounter.get());
        tlm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        tlm.waitForLayout(2);
        // wait for draw :/
        Thread.sleep(1000);
        assertEquals("on scroll should NOT be called", 2, rvCounter.get());
        assertEquals("on scroll should NOT be called", 2, viewGroupCounter.get());

    }

    @Test
    public void testScrollInBothDirectionEqual() throws Throwable {
        scrollInBothDirection(3, 3, 1000, 1000);
    }

    @Test
    public void testScrollInBothDirectionMoreVertical() throws Throwable {
        scrollInBothDirection(2, 3, 1000, 1000);
    }

    @Test
    public void testScrollInBothDirectionMoreHorizontal() throws Throwable {
        scrollInBothDirection(3, 2, 1000, 1000);
    }

    @Test
    public void testScrollHorizontalOnly() throws Throwable {
        scrollInBothDirection(3, 0, 1000, 0);
    }

    @Test
    public void testScrollVerticalOnly() throws Throwable {
        scrollInBothDirection(0, 3, 0, 1000);
    }

    @Test
    public void testScrollInBothDirectionEqualReverse() throws Throwable {
        scrollInBothDirection(3, 3, -1000, -1000);
    }

    @Test
    public void testScrollInBothDirectionMoreVerticalReverse() throws Throwable {
        scrollInBothDirection(2, 3, -1000, -1000);
    }

    @Test
    public void testScrollInBothDirectionMoreHorizontalReverse() throws Throwable {
        scrollInBothDirection(3, 2, -1000, -1000);
    }

    @Test
    public void testScrollHorizontalOnlyReverse() throws Throwable {
        scrollInBothDirection(3, 0, -1000, 0);
    }

    @Test
    public void testScrollVerticalOnlyReverse() throws Throwable {
        scrollInBothDirection(0, 3, 0, -1000);
    }

    public void scrollInBothDirection(int horizontalScrollCount, int verticalScrollCount,
            int horizontalVelocity, int verticalVelocity)
            throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        final AtomicInteger horizontalCounter = new AtomicInteger(horizontalScrollCount);
        final AtomicInteger verticalCounter = new AtomicInteger(verticalScrollCount);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean canScrollHorizontally() {
                return true;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (verticalCounter.get() > 0) {
                    verticalCounter.decrementAndGet();
                    return dy;
                }
                return 0;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                if (horizontalCounter.get() > 0) {
                    horizontalCounter.decrementAndGet();
                    return dx;
                }
                return 0;
            }
        };
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);
        assertTrue("test sanity, fling must run", fling(horizontalVelocity, verticalVelocity));
        assertEquals("rv's horizontal scroll cb must run " + horizontalScrollCount + " times'", 0,
                horizontalCounter.get());
        assertEquals("rv's vertical scroll cb must run " + verticalScrollCount + " times'", 0,
                verticalCounter.get());
    }

    @Test
    public void testDragHorizontal() throws Throwable {
        scrollInOtherOrientationTest(FLAG_HORIZONTAL);
    }

    @Test
    public void testDragVertical() throws Throwable {
        scrollInOtherOrientationTest(FLAG_VERTICAL);
    }

    @Test
    public void testFlingHorizontal() throws Throwable {
        scrollInOtherOrientationTest(FLAG_HORIZONTAL | FLAG_FLING);
    }

    @Test
    public void testFlingVertical() throws Throwable {
        scrollInOtherOrientationTest(FLAG_VERTICAL | FLAG_FLING);
    }

    @Test
    public void testNestedDragVertical() throws Throwable {
        TestedFrameLayout tfl = getActivity().mContainer;
        tfl.setNestedScrollMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        tfl.setNestedFlingMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        scrollInOtherOrientationTest(FLAG_VERTICAL, 0);
    }

    @Test
    public void testNestedDragHorizontal() throws Throwable {
        TestedFrameLayout tfl = getActivity().mContainer;
        tfl.setNestedScrollMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        tfl.setNestedFlingMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        scrollInOtherOrientationTest(FLAG_HORIZONTAL, 0);
    }

    @Test
    public void testNestedDragHorizontalCallsStopNestedScroll() throws Throwable {
        TestedFrameLayout tfl = getActivity().mContainer;
        tfl.setNestedScrollMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        tfl.setNestedFlingMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        scrollInOtherOrientationTest(FLAG_HORIZONTAL, 0);
        assertTrue("onStopNestedScroll called", tfl.stopNestedScrollCalled());
    }

    @Test
    public void testNestedDragVerticalCallsStopNestedScroll() throws Throwable {
        TestedFrameLayout tfl = getActivity().mContainer;
        tfl.setNestedScrollMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        tfl.setNestedFlingMode(TestedFrameLayout.TEST_NESTED_SCROLL_MODE_CONSUME);
        scrollInOtherOrientationTest(FLAG_VERTICAL, 0);
        assertTrue("onStopNestedScroll called", tfl.stopNestedScrollCalled());
    }

    private void scrollInOtherOrientationTest(int flags)
            throws Throwable {
        scrollInOtherOrientationTest(flags, flags);
    }

    private void scrollInOtherOrientationTest(final int flags, int expectedFlags) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        final AtomicBoolean scrolledHorizontal = new AtomicBoolean(false);
        final AtomicBoolean scrolledVertical = new AtomicBoolean(false);

        final TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean canScrollHorizontally() {
                return (flags & FLAG_HORIZONTAL) != 0;
            }

            @Override
            public boolean canScrollVertically() {
                return (flags & FLAG_VERTICAL) != 0;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                scrolledVertical.set(true);
                return super.scrollVerticallyBy(dy, recycler, state);
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                scrolledHorizontal.set(true);
                return super.scrollHorizontallyBy(dx, recycler, state);
            }
        };
        TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);
        if ( (flags & FLAG_FLING) != 0 ) {
            int flingVelocity = (mRecyclerView.getMaxFlingVelocity() +
                    mRecyclerView.getMinFlingVelocity()) / 2;
            assertEquals("fling started", (expectedFlags & FLAG_FLING) != 0,
                    fling(flingVelocity, flingVelocity));
        } else { // drag
            TouchUtils.dragViewTo(this, recyclerView, Gravity.LEFT | Gravity.TOP,
                    mRecyclerView.getWidth() / 2, mRecyclerView.getHeight() / 2);
        }
        assertEquals("horizontally scrolled: " + tlm.mScrollHorizontallyAmount,
                (expectedFlags & FLAG_HORIZONTAL) != 0, scrolledHorizontal.get());
        assertEquals("vertically scrolled: " + tlm.mScrollVerticallyAmount,
                (expectedFlags & FLAG_VERTICAL) != 0, scrolledVertical.get());
    }

    private boolean fling(final int velocityX, final int velocityY) throws Throwable {
        final AtomicBoolean didStart = new AtomicBoolean(false);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean result = mRecyclerView.fling(velocityX, velocityY);
                didStart.set(result);
            }
        });
        if (!didStart.get()) {
            return false;
        }
        // cannot set scroll listener in case it is subject to some test so instead doing a busy
        // loop until state goes idle
        while (mRecyclerView.getScrollState() != SCROLL_STATE_IDLE) {
            getInstrumentation().waitForIdleSync();
        }
        return true;
    }

    private void assertPendingUpdatesAndLayout(TestLayoutManager testLayoutManager,
            final Runnable runnable) throws Throwable {
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                assertTrue(mRecyclerView.hasPendingAdapterUpdates());
            }
        });
        testLayoutManager.waitForLayout(1);
        assertFalse(mRecyclerView.hasPendingAdapterUpdates());
    }

    private void setupBasic(RecyclerView recyclerView, TestLayoutManager tlm,
            TestAdapter adapter, boolean waitForFirstLayout) throws Throwable {
        recyclerView.setLayoutManager(tlm);
        recyclerView.setAdapter(adapter);
        if (waitForFirstLayout) {
            tlm.expectLayouts(1);
            setRecyclerView(recyclerView);
            tlm.waitForLayout(1);
        } else {
            setRecyclerView(recyclerView);
        }
    }

    @Test
    public void testHasPendingUpdatesBeforeFirstLayout() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, false);
        assertTrue(mRecyclerView.hasPendingAdapterUpdates());
    }

    @Test
    public void testNoPendingUpdatesAfterLayout() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, true);
        assertFalse(mRecyclerView.hasPendingAdapterUpdates());
    }

    @Test
    public void testHasPendingUpdatesWhenAdapterIsChanged() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        final TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, false);
        assertPendingUpdatesAndLayout(layoutManager, new Runnable() {
            @Override
            public void run() {
                testAdapter.notifyItemRemoved(1);
            }
        });
        assertPendingUpdatesAndLayout(layoutManager, new Runnable() {
            @Override
            public void run() {
                testAdapter.notifyItemInserted(2);
            }
        });

        assertPendingUpdatesAndLayout(layoutManager, new Runnable() {
            @Override
            public void run() {
                testAdapter.notifyItemMoved(2, 3);
            }
        });

        assertPendingUpdatesAndLayout(layoutManager, new Runnable() {
            @Override
            public void run() {
                testAdapter.notifyItemChanged(2);
            }
        });

        assertPendingUpdatesAndLayout(layoutManager, new Runnable() {
            @Override
            public void run() {
                testAdapter.notifyDataSetChanged();
            }
        });
    }

    @Test
    public void testTransientStateRecycleViaAdapter() throws Throwable {
        transientStateRecycleTest(true, false);
    }

    @Test
    public void testTransientStateRecycleViaTransientStateCleanup() throws Throwable {
        transientStateRecycleTest(false, true);
    }

    @Test
    public void testTransientStateDontRecycle() throws Throwable {
        transientStateRecycleTest(false, false);
    }

    public void transientStateRecycleTest(final boolean succeed, final boolean unsetTransientState)
            throws Throwable {
        final List<View> failedToRecycle = new ArrayList<View>();
        final List<View> recycled = new ArrayList<View>();
        TestAdapter testAdapter = new TestAdapter(10) {
            @Override
            public boolean onFailedToRecycleView(
                    TestViewHolder holder) {
                failedToRecycle.add(holder.itemView);
                if (unsetTransientState) {
                    setHasTransientState(holder.itemView, false);
                }
                return succeed;
            }

            @Override
            public void onViewRecycled(TestViewHolder holder) {
                recycled.add(holder.itemView);
                super.onViewRecycled(holder);
            }
        };
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (getChildCount() == 0) {
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, 0, 5);
                } else {
                    removeAndRecycleAllViews(recycler);
                }
                if (layoutLatch != null) {
                    layoutLatch.countDown();
                }
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        recyclerView.setItemAnimator(null);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        // make sure we have enough views after this position so that we'll receive the on recycled
        // callback
        View view = recyclerView.getChildAt(3);//this has to be greater than def cache size.
        setHasTransientState(view, true);
        tlm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        tlm.waitForLayout(2);

        assertTrue(failedToRecycle.contains(view));
        assertEquals(succeed || unsetTransientState, recycled.contains(view));
    }

    @Test
    public void testAdapterPositionInvalidation() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter adapter = new TestAdapter(10);
        final TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, state.getItemCount());
                layoutLatch.countDown();
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < tlm.getChildCount(); i++) {
                    assertNotSame("adapter positions should not be undefined",
                            recyclerView.getChildAdapterPosition(tlm.getChildAt(i)),
                            RecyclerView.NO_POSITION);
                }
                adapter.notifyDataSetChanged();
                for (int i = 0; i < tlm.getChildCount(); i++) {
                    assertSame("adapter positions should be undefined",
                            recyclerView.getChildAdapterPosition(tlm.getChildAt(i)),
                            RecyclerView.NO_POSITION);
                }
            }
        });
    }

    @Test
    public void testAdapterPositionsBasic() throws Throwable {
        adapterPositionsTest(null);
    }

    @Test
    public void testAdapterPositionsRemoveItems() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.deleteAndNotify(3, 4);
            }
        });
    }

    @Test
    public void testAdapterPositionsRemoveItemsBefore() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.deleteAndNotify(0, 1);
            }
        });
    }

    @Test
    public void testAdapterPositionsAddItemsBefore() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.addAndNotify(0, 5);
            }
        });
    }

    @Test
    public void testAdapterPositionsAddItemsInside() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.addAndNotify(3, 2);
            }
        });
    }

    @Test
    public void testAdapterPositionsMoveItems() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.moveAndNotify(3, 5);
            }
        });
    }

    @Test
    public void testAdapterPositionsNotifyDataSetChanged() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.mItems.clear();
                for (int i = 0; i < 20; i++) {
                    adapter.mItems.add(new Item(i, "added item"));
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Test
    public void testAvoidLeakingRecyclerViewIfViewIsNotRecycled() throws Throwable {
        final AtomicBoolean failedToRecycle = new AtomicBoolean(false);
        RecyclerView rv = new RecyclerView(getActivity());
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                layoutLatch.countDown();
            }
        };
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public boolean onFailedToRecycleView(
                    TestViewHolder holder) {
                failedToRecycle.set(true);
                return false;
            }
        };
        rv.setAdapter(adapter);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(1);
        final RecyclerView.ViewHolder vh = rv.getChildViewHolder(rv.getChildAt(0));
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.setHasTransientState(vh.itemView, true);
            }
        });
        tlm.expectLayouts(1);
        adapter.deleteAndNotify(0, 10);
        tlm.waitForLayout(2);
        final CountDownLatch animationsLatch = new CountDownLatch(1);
        rv.getItemAnimator().isRunning(
                new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        animationsLatch.countDown();
                    }
                });
        assertTrue(animationsLatch.await(2, TimeUnit.SECONDS));
        assertTrue(failedToRecycle.get());
        assertNull(vh.mOwnerRecyclerView);
        checkForMainThreadException();
    }

    @Test
    public void testAvoidLeakingRecyclerViewViaViewHolder() throws Throwable {
        RecyclerView rv = new RecyclerView(getActivity());
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                layoutLatch.countDown();
            }
        };
        TestAdapter adapter = new TestAdapter(10);
        rv.setAdapter(adapter);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(1);
        final RecyclerView.ViewHolder vh = rv.getChildViewHolder(rv.getChildAt(0));
        tlm.expectLayouts(1);
        adapter.deleteAndNotify(0, 10);
        tlm.waitForLayout(2);
        final CountDownLatch animationsLatch = new CountDownLatch(1);
        rv.getItemAnimator().isRunning(
                new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        animationsLatch.countDown();
                    }
                });
        assertTrue(animationsLatch.await(2, TimeUnit.SECONDS));
        assertNull(vh.mOwnerRecyclerView);
        checkForMainThreadException();
    }

    public void adapterPositionsTest(final AdapterRunnable adapterChanges) throws Throwable {
        final TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    layoutRange(recycler, Math.min(state.getItemCount(), 2)
                            , Math.min(state.getItemCount(), 7));
                    layoutLatch.countDown();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(tlm);
        recyclerView.setAdapter(testAdapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final int count = recyclerView.getChildCount();
                    Map<View, Integer> layoutPositions = new HashMap<View, Integer>();
                    assertTrue("test sanity", count > 0);
                    for (int i = 0; i < count; i++) {
                        View view = recyclerView.getChildAt(i);
                        TestViewHolder vh = (TestViewHolder) recyclerView.getChildViewHolder(view);
                        int index = testAdapter.mItems.indexOf(vh.mBoundItem);
                        assertEquals("should be able to find VH with adapter position " + index, vh,
                                recyclerView.findViewHolderForAdapterPosition(index));
                        assertEquals("get adapter position should return correct index", index,
                                vh.getAdapterPosition());
                        layoutPositions.put(view, vh.mPosition);
                    }
                    if (adapterChanges != null) {
                        adapterChanges.run(testAdapter);
                        for (int i = 0; i < count; i++) {
                            View view = recyclerView.getChildAt(i);
                            TestViewHolder vh = (TestViewHolder) recyclerView
                                    .getChildViewHolder(view);
                            int index = testAdapter.mItems.indexOf(vh.mBoundItem);
                            if (index >= 0) {
                                assertEquals("should be able to find VH with adapter position "
                                                + index, vh,
                                        recyclerView.findViewHolderForAdapterPosition(index));
                            }
                            assertSame("get adapter position should return correct index", index,
                                    vh.getAdapterPosition());
                            assertSame("should be able to find view with layout position",
                                    vh, mRecyclerView.findViewHolderForLayoutPosition(
                                            layoutPositions.get(view)));
                        }

                    }

                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        checkForMainThreadException();
    }

    @Test
    public void testScrollStateForSmoothScroll() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        final CountDownLatch latch = new CountDownLatch(2);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollBy(0, 500);
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1, stateCnts[SCROLL_STATE_SETTLING]);
        assertEquals(1, stateCnts[SCROLL_STATE_IDLE]);
        assertEquals(0, stateCnts[SCROLL_STATE_DRAGGING]);
    }

    @Test
    public void testScrollStateForSmoothScrollWithStop() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        final CountDownLatch latch = new CountDownLatch(1);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollBy(0, 500);
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.stopScroll();
            }
        });
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        assertEquals(1, stateCnts[SCROLL_STATE_SETTLING]);
        assertEquals(1, stateCnts[SCROLL_STATE_IDLE]);
        assertEquals(0, stateCnts[SCROLL_STATE_DRAGGING]);
    }

    @Test
    public void testScrollStateForFling() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        final CountDownLatch latch = new CountDownLatch(2);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        final ViewConfiguration vc = ViewConfiguration.get(getActivity());
        final float fling = vc.getScaledMinimumFlingVelocity()
                + (vc.getScaledMaximumFlingVelocity() - vc.getScaledMinimumFlingVelocity()) * .1f;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.fling(0, Math.round(fling));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertEquals(1, stateCnts[SCROLL_STATE_SETTLING]);
        assertEquals(1, stateCnts[SCROLL_STATE_IDLE]);
        assertEquals(0, stateCnts[SCROLL_STATE_DRAGGING]);
    }

    @Test
    public void testScrollStateForFlingWithStop() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        final CountDownLatch latch = new CountDownLatch(1);
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        final ViewConfiguration vc = ViewConfiguration.get(getActivity());
        final float fling = vc.getScaledMinimumFlingVelocity()
                + (vc.getScaledMaximumFlingVelocity() - vc.getScaledMinimumFlingVelocity()) * .8f;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.fling(0, Math.round(fling));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.stopScroll();
            }
        });
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        assertEquals(1, stateCnts[SCROLL_STATE_SETTLING]);
        assertEquals(1, stateCnts[SCROLL_STATE_IDLE]);
        assertEquals(0, stateCnts[SCROLL_STATE_DRAGGING]);
    }

    @Test
    public void testScrollStateDrag() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
            }
        });
        drag(mRecyclerView, 0, 0, 0, 500, 5);
        assertEquals(0, stateCnts[SCROLL_STATE_SETTLING]);
        assertEquals(1, stateCnts[SCROLL_STATE_IDLE]);
        assertEquals(1, stateCnts[SCROLL_STATE_DRAGGING]);
    }

    public void drag(ViewGroup view, float fromX, float toX, float fromY, float toY,
            int stepCount) throws Throwable {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();

        float y = fromY;
        float x = fromX;

        float yStep = (toY - fromY) / stepCount;
        float xStep = (toX - fromX) / stepCount;

        MotionEvent event = MotionEvent.obtain(downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0);
        sendTouch(view, event);
        for (int i = 0; i < stepCount; ++i) {
            y += yStep;
            x += xStep;
            eventTime = SystemClock.uptimeMillis();
            event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_MOVE, x, y, 0);
            sendTouch(view, event);
        }

        eventTime = SystemClock.uptimeMillis();
        event = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        sendTouch(view, event);
        getInstrumentation().waitForIdleSync();
    }

    private void sendTouch(final ViewGroup view, final MotionEvent event) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (view.onInterceptTouchEvent(event)) {
                    view.onTouchEvent(event);
                }
            }
        });
    }

    @Test
    public void testRecycleScrap() throws Throwable {
        recycleScrapTest(false);
        removeRecyclerView();
        recycleScrapTest(true);
    }

    public void recycleScrapTest(final boolean useRecycler) throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        final AtomicBoolean test = new AtomicBoolean(false);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (test.get()) {
                    try {
                        detachAndScrapAttachedViews(recycler);
                        for (int i = recycler.getScrapList().size() - 1; i >= 0; i--) {
                            if (useRecycler) {
                                recycler.recycleView(recycler.getScrapList().get(i).itemView);
                            } else {
                                removeAndRecycleView(recycler.getScrapList().get(i).itemView,
                                        recycler);
                            }
                        }
                        if (state.mOldChangedHolders != null) {
                            for (int i = state.mOldChangedHolders.size() - 1; i >= 0; i--) {
                                if (useRecycler) {
                                    recycler.recycleView(
                                            state.mOldChangedHolders.valueAt(i).itemView);
                                } else {
                                    removeAndRecycleView(
                                            state.mOldChangedHolders.valueAt(i).itemView, recycler);
                                }
                            }
                        }
                        assertEquals("no scrap should be left over", 0, recycler.getScrapCount());
                        assertEquals("pre layout map should be empty", 0,
                                state.mPreLayoutHolderMap.size());
                        assertEquals("post layout map should be empty", 0,
                                state.mPostLayoutHolderMap.size());
                        if (state.mOldChangedHolders != null) {
                            assertEquals("post old change map should be empty", 0,
                                    state.mOldChangedHolders.size());
                        }
                    } catch (Throwable t) {
                        postExceptionToInstrumentation(t);
                    }

                }
                layoutRange(recycler, 0, 5);
                layoutLatch.countDown();
                super.onLayoutChildren(recycler, state);
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(lm);
        recyclerView.getItemAnimator().setSupportsChangeAnimations(true);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        test.set(true);
        lm.expectLayouts(1);
        testAdapter.changeAndNotify(3, 1);
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testAccessRecyclerOnOnMeasure() throws Throwable {
        accessRecyclerOnOnMeasureTest(false);
        removeRecyclerView();
        accessRecyclerOnOnMeasureTest(true);
    }

    @Test
    public void testSmoothScrollWithRemovedItemsAndRemoveItem() throws Throwable {
        smoothScrollTest(true);
    }

    @Test
    public void testSmoothScrollWithRemovedItems() throws Throwable {
        smoothScrollTest(false);
    }

    public void smoothScrollTest(final boolean removeItem) throws Throwable {
        final LinearSmoothScroller[] lss = new LinearSmoothScroller[1];
        final CountDownLatch calledOnStart = new CountDownLatch(1);
        final CountDownLatch calledOnStop = new CountDownLatch(1);
        final int visibleChildCount = 10;
        TestLayoutManager lm = new TestLayoutManager() {
            int start = 0;

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutRange(recycler, start, visibleChildCount);
                layoutLatch.countDown();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                start++;
                if (DEBUG) {
                    Log.d(TAG, "on scroll, remove and recycling. start:" + start + ", cnt:"
                            + visibleChildCount);
                }
                removeAndRecycleAllViews(recycler);
                layoutRange(recycler, start,
                        Math.max(state.getItemCount(), start + visibleChildCount));
                return dy;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                    int position) {
                LinearSmoothScroller linearSmoothScroller =
                        new LinearSmoothScroller(recyclerView.getContext()) {
                            @Override
                            public PointF computeScrollVectorForPosition(int targetPosition) {
                                return new PointF(0, 1);
                            }

                            @Override
                            protected void onStart() {
                                super.onStart();
                                calledOnStart.countDown();
                            }

                            @Override
                            protected void onStop() {
                                super.onStop();
                                calledOnStop.countDown();
                            }
                        };
                linearSmoothScroller.setTargetPosition(position);
                lss[0] = linearSmoothScroller;
                startSmoothScroll(linearSmoothScroller);
            }
        };
        final RecyclerView rv = new RecyclerView(getActivity());
        TestAdapter testAdapter = new TestAdapter(500);
        rv.setLayoutManager(lm);
        rv.setAdapter(testAdapter);
        lm.expectLayouts(1);
        setRecyclerView(rv);
        lm.waitForLayout(1);
        // regular scroll
        final int targetPosition = visibleChildCount * (removeItem ? 30 : 4);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.smoothScrollToPosition(targetPosition);
            }
        });
        if (DEBUG) {
            Log.d(TAG, "scrolling to target position " + targetPosition);
        }
        assertTrue("on start should be called very soon", calledOnStart.await(2, TimeUnit.SECONDS));
        if (removeItem) {
            final int newTarget = targetPosition - 10;
            testAdapter.deleteAndNotify(newTarget + 1, testAdapter.getItemCount() - newTarget - 1);
            final CountDownLatch targetCheck = new CountDownLatch(1);
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewCompat.postOnAnimationDelayed(rv, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                assertEquals("scroll position should be updated to next available",
                                        newTarget, lss[0].getTargetPosition());
                            } catch (Throwable t) {
                                postExceptionToInstrumentation(t);
                            }
                            targetCheck.countDown();
                        }
                    }, 50);
                }
            });
            assertTrue("target position should be checked on time ",
                    targetCheck.await(10, TimeUnit.SECONDS));
            checkForMainThreadException();
            assertTrue("on stop should be called", calledOnStop.await(30, TimeUnit.SECONDS));
            checkForMainThreadException();
            assertNotNull("should scroll to new target " + newTarget
                    , rv.findViewHolderForLayoutPosition(newTarget));
            if (DEBUG) {
                Log.d(TAG, "on stop has been called on time");
            }
        } else {
            assertTrue("on stop should be called eventually",
                    calledOnStop.await(30, TimeUnit.SECONDS));
            assertNotNull("scroll to position should succeed",
                    rv.findViewHolderForLayoutPosition(targetPosition));
        }
        checkForMainThreadException();
    }

    @Test
    public void testConsecutiveSmoothScroll() throws Throwable {
        final AtomicInteger visibleChildCount = new AtomicInteger(10);
        final AtomicInteger totalScrolled = new AtomicInteger(0);
        final TestLayoutManager lm = new TestLayoutManager() {
            int start = 0;

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutRange(recycler, start, visibleChildCount.get());
                layoutLatch.countDown();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                totalScrolled.set(totalScrolled.get() + dy);
                return dy;
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }
        };
        final RecyclerView rv = new RecyclerView(getActivity());
        TestAdapter testAdapter = new TestAdapter(500);
        rv.setLayoutManager(lm);
        rv.setAdapter(testAdapter);
        lm.expectLayouts(1);
        setRecyclerView(rv);
        lm.waitForLayout(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.smoothScrollBy(0, 2000);
            }
        });
        Thread.sleep(250);
        final AtomicInteger scrollAmt = new AtomicInteger();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int soFar = totalScrolled.get();
                scrollAmt.set(soFar);
                rv.smoothScrollBy(0, 5000 - soFar);
            }
        });
        while (rv.getScrollState() != SCROLL_STATE_IDLE) {
            Thread.sleep(100);
        }
        final int soFar = totalScrolled.get();
        assertEquals("second scroll should be competed properly", 5000, soFar);
    }

    public void accessRecyclerOnOnMeasureTest(final boolean enablePredictiveAnimations)
            throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        final AtomicInteger expectedOnMeasureStateCount = new AtomicInteger(10);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                try {
                    layoutRange(recycler, 0, state.getItemCount());
                    layoutLatch.countDown();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }

            @Override
            public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                    int widthSpec, int heightSpec) {
                try {
                    // make sure we access all views
                    for (int i = 0; i < state.getItemCount(); i++) {
                        View view = recycler.getViewForPosition(i);
                        assertNotNull(view);
                        assertEquals(i, getPosition(view));
                    }
                    assertEquals(state.toString(),
                            expectedOnMeasureStateCount.get(), state.getItemCount());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
                super.onMeasure(recycler, state, widthSpec, heightSpec);
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return enablePredictiveAnimations;
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        checkForMainThreadException();
        lm.expectLayouts(1);
        if (!enablePredictiveAnimations) {
            expectedOnMeasureStateCount.set(15);
        }
        testAdapter.addAndNotify(4, 5);
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testSetCompatibleAdapter() throws Throwable {
        compatibleAdapterTest(true, true);
        removeRecyclerView();
        compatibleAdapterTest(false, true);
        removeRecyclerView();
        compatibleAdapterTest(true, false);
        removeRecyclerView();
        compatibleAdapterTest(false, false);
        removeRecyclerView();
    }

    private void compatibleAdapterTest(boolean useCustomPool, boolean removeAndRecycleExistingViews)
            throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        final AtomicInteger recycledViewCount = new AtomicInteger();
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    layoutRange(recycler, 0, state.getItemCount());
                    layoutLatch.countDown();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(testAdapter);
        recyclerView.setRecyclerListener(new RecyclerView.RecyclerListener() {
            @Override
            public void onViewRecycled(RecyclerView.ViewHolder holder) {
                recycledViewCount.incrementAndGet();
            }
        });
        lm.expectLayouts(1);
        setRecyclerView(recyclerView, !useCustomPool);
        lm.waitForLayout(2);
        checkForMainThreadException();
        lm.expectLayouts(1);
        swapAdapter(new TestAdapter(10), removeAndRecycleExistingViews);
        lm.waitForLayout(2);
        checkForMainThreadException();
        if (removeAndRecycleExistingViews) {
            assertTrue("Previous views should be recycled", recycledViewCount.get() > 0);
        } else {
            assertEquals("No views should be recycled if adapters are compatible and developer "
                    + "did not request a recycle", 0, recycledViewCount.get());
        }
    }

    @Test
    public void testSetIncompatibleAdapter() throws Throwable {
        incompatibleAdapterTest(true);
        incompatibleAdapterTest(false);
    }

    public void incompatibleAdapterTest(boolean useCustomPool) throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                try {
                    layoutRange(recycler, 0, state.getItemCount());
                    layoutLatch.countDown();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView, !useCustomPool);
        lm.waitForLayout(2);
        checkForMainThreadException();
        lm.expectLayouts(1);
        setAdapter(new TestAdapter2(10));
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testRecycleIgnored() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 5);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                View child1 = lm.findViewByPosition(0);
                View child2 = lm.findViewByPosition(1);
                lm.ignoreView(child1);
                lm.ignoreView(child2);

                lm.removeAndRecycleAllViews(recyclerView.mRecycler);
                assertEquals("ignored child should not be recycled or removed", 2,
                        lm.getChildCount());

                Throwable[] throwables = new Throwable[1];
                try {
                    lm.removeAndRecycleView(child1, mRecyclerView.mRecycler);
                } catch (Throwable t) {
                    throwables[0] = t;
                }
                assertTrue("Trying to recycle an ignored view should throw IllegalArgException "
                        , throwables[0] instanceof IllegalArgumentException);
                lm.removeAllViews();
                assertEquals("ignored child should be removed as well ", 0, lm.getChildCount());
            }
        });
    }

    @Test
    public void testFindIgnoredByPosition() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, 5);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        Thread.sleep(5000);
        final int pos = 1;
        final View[] ignored = new View[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                View child = lm.findViewByPosition(pos);
                lm.ignoreView(child);
                ignored[0] = child;
            }
        });
        assertNotNull("ignored child should not be null", ignored[0]);
        assertNull("find view by position should not return ignored child",
                lm.findViewByPosition(pos));
        lm.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        lm.waitForLayout(1);
        assertEquals("child count should be ", 6, lm.getChildCount());
        View replacement = lm.findViewByPosition(pos);
        assertNotNull("re-layout should replace ignored child w/ another one", replacement);
        assertNotSame("replacement should be a different view", replacement, ignored[0]);
    }

    @Test
    public void testInvalidateAllDecorOffsets() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final AtomicBoolean invalidatedOffsets = new AtomicBoolean(true);
        recyclerView.setAdapter(adapter);
        final AtomicInteger layoutCount = new AtomicInteger(4);
        final RecyclerView.ItemDecoration dummyItemDecoration = new RecyclerView.ItemDecoration() {
        };
        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    // test
                    for (int i = 0; i < getChildCount(); i++) {
                        View child = getChildAt(i);
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)
                                child.getLayoutParams();
                        assertEquals(
                                "Decor insets validation for VH should have expected value.",
                                invalidatedOffsets.get(), lp.mInsetsDirty);
                    }
                    for (RecyclerView.ViewHolder vh : mRecyclerView.mRecycler.mCachedViews) {
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)
                                vh.itemView.getLayoutParams();
                        assertEquals(
                                "Decor insets invalidation in cache for VH should have expected "
                                        + "value.",
                                invalidatedOffsets.get(), lp.mInsetsDirty);
                    }
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, 0, layoutCount.get());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        // first layout
        recyclerView.setItemViewCacheSize(5);
        recyclerView.setLayoutManager(testLayoutManager);
        testLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView, true, false);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        // re-layout w/o any change
        invalidatedOffsets.set(false);
        testLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();

        // invalidate w/o an item decorator

        invalidateDecorOffsets(recyclerView);
        testLayoutManager.expectLayouts(1);
        invalidateDecorOffsets(recyclerView);
        testLayoutManager.assertNoLayout("layout should not happen", 2);
        checkForMainThreadException();

        // set item decorator, should invalidate
        invalidatedOffsets.set(true);
        testLayoutManager.expectLayouts(1);
        addItemDecoration(mRecyclerView, dummyItemDecoration);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();

        // re-layout w/o any change
        invalidatedOffsets.set(false);
        testLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();

        // invalidate w/ item decorator
        invalidatedOffsets.set(true);
        invalidateDecorOffsets(recyclerView);
        testLayoutManager.expectLayouts(1);
        invalidateDecorOffsets(recyclerView);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        // trigger cache.
        layoutCount.set(3);
        invalidatedOffsets.set(false);
        testLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();
        assertEquals("a view should be cached", 1, mRecyclerView.mRecycler.mCachedViews.size());

        layoutCount.set(5);
        invalidatedOffsets.set(true);
        testLayoutManager.expectLayouts(1);
        invalidateDecorOffsets(recyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();

        // remove item decorator
        invalidatedOffsets.set(true);
        testLayoutManager.expectLayouts(1);
        removeItemDecoration(mRecyclerView, dummyItemDecoration);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();
    }

    public void addItemDecoration(final RecyclerView recyclerView, final
    RecyclerView.ItemDecoration itemDecoration) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.addItemDecoration(itemDecoration);
            }
        });
    }

    public void removeItemDecoration(final RecyclerView recyclerView, final
    RecyclerView.ItemDecoration itemDecoration) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.removeItemDecoration(itemDecoration);
            }
        });
    }

    public void invalidateDecorOffsets(final RecyclerView recyclerView) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.invalidateItemDecorations();
            }
        });
    }

    @Test
    public void testInvalidateDecorOffsets() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(true);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);

        final Map<Long, Boolean> changes = new HashMap<Long, Boolean>();

        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    if (changes.size() > 0) {
                        // test
                        for (int i = 0; i < getChildCount(); i++) {
                            View child = getChildAt(i);
                            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams)
                                    child.getLayoutParams();
                            RecyclerView.ViewHolder vh = lp.mViewHolder;
                            if (!changes.containsKey(vh.getItemId())) {
                                continue; //nothing to test
                            }
                            assertEquals(
                                    "Decord insets validation for VH should have expected value.",
                                    changes.get(vh.getItemId()).booleanValue(),
                                    lp.mInsetsDirty);
                        }
                    }
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, 0, state.getItemCount());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        recyclerView.setLayoutManager(testLayoutManager);
        testLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        testLayoutManager.waitForLayout(2);
        int itemAddedTo = 5;
        for (int i = 0; i < itemAddedTo; i++) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(i).getItemId(), false);
        }
        for (int i = itemAddedTo; i < mRecyclerView.getChildCount(); i++) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(i).getItemId(), true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.addAndNotify(5, 1);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        changes.clear();
        int[] changedItems = new int[]{3, 5, 6};
        for (int i = 0; i < adapter.getItemCount(); i++) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(i).getItemId(), false);
        }
        for (int i = 0; i < changedItems.length; i++) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(changedItems[i]).getItemId(),
                    true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.changePositionsAndNotify(changedItems);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        for (int i = 0; i < adapter.getItemCount(); i++) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(i).getItemId(), true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.dispatchDataSetChanged();
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testMovingViaStableIds() throws Throwable {
        stableIdsMoveTest(true);
        removeRecyclerView();
        stableIdsMoveTest(false);
        removeRecyclerView();
    }

    public void stableIdsMoveTest(final boolean supportsPredictive) throws Throwable {
        final TestAdapter testAdapter = new TestAdapter(10);
        testAdapter.setHasStableIds(true);
        final AtomicBoolean test = new AtomicBoolean(false);
        final int movedViewFromIndex = 3;
        final int movedViewToIndex = 6;
        final View[] movedView = new View[1];
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                try {
                    if (test.get()) {
                        if (state.isPreLayout()) {
                            View view = recycler.getViewForPosition(movedViewFromIndex, true);
                            assertSame("In pre layout, should be able to get moved view w/ old "
                                    + "position", movedView[0], view);
                            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                            assertTrue("it should come from scrap", holder.wasReturnedFromScrap());
                            // clear scrap flag
                            holder.clearReturnedFromScrapFlag();
                        } else {
                            View view = recycler.getViewForPosition(movedViewToIndex, true);
                            assertSame("In post layout, should be able to get moved view w/ new "
                                    + "position", movedView[0], view);
                            RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
                            assertTrue("it should come from scrap", holder.wasReturnedFromScrap());
                            // clear scrap flag
                            holder.clearReturnedFromScrapFlag();
                        }
                    }
                    layoutRange(recycler, 0, state.getItemCount());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }


            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return supportsPredictive;
            }
        };
        RecyclerView recyclerView = new RecyclerView(this.getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(1);

        movedView[0] = recyclerView.getChildAt(movedViewFromIndex);
        test.set(true);
        lm.expectLayouts(supportsPredictive ? 2 : 1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Item item = testAdapter.mItems.remove(movedViewFromIndex);
                testAdapter.mItems.add(movedViewToIndex, item);
                testAdapter.notifyItemRemoved(movedViewFromIndex);
                testAdapter.notifyItemInserted(movedViewToIndex);
            }
        });
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testAdapterChangeDuringLayout() throws Throwable {
        adapterChangeInMainThreadTest("notifyDataSetChanged", new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });

        adapterChangeInMainThreadTest("notifyItemChanged", new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getAdapter().notifyItemChanged(2);
            }
        });

        adapterChangeInMainThreadTest("notifyItemInserted", new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getAdapter().notifyItemInserted(2);
            }
        });
        adapterChangeInMainThreadTest("notifyItemRemoved", new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getAdapter().notifyItemRemoved(2);
            }
        });
    }

    public void adapterChangeInMainThreadTest(String msg,
            final Runnable onLayoutRunnable) throws Throwable {
        final AtomicBoolean doneFirstLayout = new AtomicBoolean(false);
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                try {
                    layoutRange(recycler, 0, state.getItemCount());
                    if (doneFirstLayout.get()) {
                        onLayoutRunnable.run();
                    }
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }

            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(testAdapter);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        doneFirstLayout.set(true);
        lm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        lm.waitForLayout(2);
        removeRecyclerView();
        assertTrue("Invalid data updates should be caught:" + msg,
                mainThreadException instanceof IllegalStateException);
        mainThreadException = null;
    }

    @Test
    public void testAdapterChangeDuringScroll() throws Throwable {
        for (int orientation : new int[]{OrientationHelper.HORIZONTAL,
                OrientationHelper.VERTICAL}) {
            adapterChangeDuringScrollTest("notifyDataSetChanged", orientation,
                    new Runnable() {
                        @Override
                        public void run() {
                            mRecyclerView.getAdapter().notifyDataSetChanged();
                        }
                    });
            adapterChangeDuringScrollTest("notifyItemChanged", orientation, new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.getAdapter().notifyItemChanged(2);
                }
            });

            adapterChangeDuringScrollTest("notifyItemInserted", orientation, new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.getAdapter().notifyItemInserted(2);
                }
            });
            adapterChangeDuringScrollTest("notifyItemRemoved", orientation, new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.getAdapter().notifyItemRemoved(2);
                }
            });
        }
    }

    public void adapterChangeDuringScrollTest(String msg, final int orientation,
            final Runnable onScrollRunnable) throws Throwable {
        TestAdapter testAdapter = new TestAdapter(100);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                try {
                    layoutRange(recycler, 0, 10);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }

            @Override
            public boolean canScrollVertically() {
                return orientation == OrientationHelper.VERTICAL;
            }

            @Override
            public boolean canScrollHorizontally() {
                return orientation == OrientationHelper.HORIZONTAL;
            }

            public int mockScroll() {
                try {
                    onScrollRunnable.run();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
                return 0;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                return mockScroll();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                return mockScroll();
            }
        };
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(testAdapter);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        lm.expectLayouts(1);
        scrollBy(200);
        lm.waitForLayout(2);
        removeRecyclerView();
        assertTrue("Invalid data updates should be caught:" + msg,
                mainThreadException instanceof IllegalStateException);
        mainThreadException = null;
    }

    @Test
    public void testRecycleOnDetach() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter testAdapter = new TestAdapter(10);
        final AtomicBoolean didRunOnDetach = new AtomicBoolean(false);
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutRange(recycler, 0, state.getItemCount() - 1);
                layoutLatch.countDown();
            }

            @Override
            public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
                super.onDetachedFromWindow(view, recycler);
                didRunOnDetach.set(true);
                removeAndRecycleAllViews(recycler);
            }
        };
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        removeRecyclerView();
        assertTrue("When recycler view is removed, detach should run", didRunOnDetach.get());
        assertEquals("All children should be recycled", recyclerView.getChildCount(), 0);
    }

    @Test
    public void testUpdatesWhileDetached() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int initialAdapterSize = 20;
        final TestAdapter adapter = new TestAdapter(initialAdapterSize);
        final AtomicInteger layoutCount = new AtomicInteger(0);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutRange(recycler, 0, 5);
                layoutCount.incrementAndGet();
                layoutLatch.countDown();
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        recyclerView.setHasFixedSize(true);
        lm.expectLayouts(1);
        adapter.addAndNotify(4, 5);
        lm.assertNoLayout("When RV is not attached, layout should not happen", 1);
    }

    @Test
    public void testUpdatesAfterDetach() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int initialAdapterSize = 20;
        final TestAdapter adapter = new TestAdapter(initialAdapterSize);
        final AtomicInteger layoutCount = new AtomicInteger(0);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutRange(recycler, 0, 5);
                layoutCount.incrementAndGet();
                layoutLatch.countDown();
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        recyclerView.setHasFixedSize(true);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        lm.expectLayouts(1);
        final int prevLayoutCount = layoutCount.get();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    adapter.addAndNotify(4, 5);
                    removeRecyclerView();
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });
        checkForMainThreadException();

        lm.assertNoLayout("When RV is not attached, layout should not happen", 1);
        assertEquals("No extra layout should happen when detached", prevLayoutCount,
                layoutCount.get());
    }

    @Test
    public void testNotifyDataSetChangedWithStableIds() throws Throwable {
        final int defaultViewType = 1;
        final Map<Item, Integer> viewTypeMap = new HashMap<Item, Integer>();
        final Map<Integer, Integer> oldPositionToNewPositionMapping =
                new HashMap<Integer, Integer>();
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                Integer type = viewTypeMap.get(mItems.get(position));
                return type == null ? defaultViewType : type;
            }

            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        final ArrayList<Item> previousItems = new ArrayList<Item>();
        previousItems.addAll(adapter.mItems);

        final AtomicInteger layoutStart = new AtomicInteger(50);
        final AtomicBoolean validate = new AtomicBoolean(false);
        final int childCount = 10;
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                    if (validate.get()) {
                        assertEquals("Cached views should be kept", 5, recycler
                                .mCachedViews.size());
                        for (RecyclerView.ViewHolder vh : recycler.mCachedViews) {
                            TestViewHolder tvh = (TestViewHolder) vh;
                            assertTrue("view holder should be marked for update",
                                    tvh.needsUpdate());
                            assertTrue("view holder should be marked as invalid", tvh.isInvalid());
                        }
                    }
                    detachAndScrapAttachedViews(recycler);
                    if (validate.get()) {
                        assertEquals("cache size should stay the same", 5,
                                recycler.mCachedViews.size());
                        assertEquals("all views should be scrapped", childCount,
                                recycler.getScrapList().size());
                        for (RecyclerView.ViewHolder vh : recycler.getScrapList()) {
                            // TODO create test case for type change
                            TestViewHolder tvh = (TestViewHolder) vh;
                            assertTrue("view holder should be marked for update",
                                    tvh.needsUpdate());
                            assertTrue("view holder should be marked as invalid", tvh.isInvalid());
                        }
                    }
                    layoutRange(recycler, layoutStart.get(), layoutStart.get() + childCount);
                    if (validate.get()) {
                        for (int i = 0; i < getChildCount(); i++) {
                            View view = getChildAt(i);
                            TestViewHolder tvh = (TestViewHolder) mRecyclerView
                                    .getChildViewHolder(view);
                            final int oldPos = previousItems.indexOf(tvh.mBoundItem);
                            assertEquals("view holder's position should be correct",
                                    oldPositionToNewPositionMapping.get(oldPos).intValue(),
                                    tvh.getLayoutPosition());
                            ;
                        }
                    }
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        recyclerView.setItemViewCacheSize(10);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        checkForMainThreadException();
        getInstrumentation().waitForIdleSync();
        layoutStart.set(layoutStart.get() + 5);//55
        lm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        lm.waitForLayout(2);
        validate.set(true);
        lm.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    adapter.moveItems(false,
                            new int[]{50, 56}, new int[]{51, 1}, new int[]{52, 2},
                            new int[]{53, 54}, new int[]{60, 61}, new int[]{62, 64},
                            new int[]{75, 58});
                    for (int i = 0; i < previousItems.size(); i++) {
                        Item item = previousItems.get(i);
                        oldPositionToNewPositionMapping.put(i, adapter.mItems.indexOf(item));
                    }
                    adapter.dispatchDataSetChanged();
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testCallbacksDuringAdapterSwap() throws Throwable {
        callbacksDuringAdapterChange(true);
    }

    @Test
    public void testCallbacksDuringAdapterSet() throws Throwable {
        callbacksDuringAdapterChange(false);
    }

    public void callbacksDuringAdapterChange(boolean swap) throws Throwable {
        final TestAdapter2 adapter1 = swap ? createBinderCheckingAdapter()
                : createOwnerCheckingAdapter();
        final TestAdapter2 adapter2 = swap ? createBinderCheckingAdapter()
                : createOwnerCheckingAdapter();

        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    layoutRange(recycler, 0, state.getItemCount());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
                layoutLatch.countDown();
            }
        };
        RecyclerView rv = new RecyclerView(getActivity());
        rv.setAdapter(adapter1);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(1);
        checkForMainThreadException();
        tlm.expectLayouts(1);
        if (swap) {
            swapAdapter(adapter2, true);
        } else {
            setAdapter(adapter2);
        }
        checkForMainThreadException();
        tlm.waitForLayout(1);
        checkForMainThreadException();
    }

    private TestAdapter2 createOwnerCheckingAdapter() {
        return new TestAdapter2(10) {
            @Override
            public void onViewRecycled(TestViewHolder2 holder) {
                assertSame("on recycled should be called w/ the creator adapter", this,
                        holder.mData);
                super.onViewRecycled(holder);
            }

            @Override
            public void onBindViewHolder(TestViewHolder2 holder, int position) {
                super.onBindViewHolder(holder, position);
                assertSame("on bind should be called w/ the creator adapter", this, holder.mData);
            }

            @Override
            public TestViewHolder2 onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final TestViewHolder2 vh = super.onCreateViewHolder(parent, viewType);
                vh.mData = this;
                return vh;
            }
        };
    }

    private TestAdapter2 createBinderCheckingAdapter() {
        return new TestAdapter2(10) {
            @Override
            public void onViewRecycled(TestViewHolder2 holder) {
                assertSame("on recycled should be called w/ the creator adapter", this,
                        holder.mData);
                holder.mData = null;
                super.onViewRecycled(holder);
            }

            @Override
            public void onBindViewHolder(TestViewHolder2 holder, int position) {
                super.onBindViewHolder(holder, position);
                holder.mData = this;
            }
        };
    }

    @Test
    public void testFindViewById() throws Throwable {
        findViewByIdTest(false);
        removeRecyclerView();
        findViewByIdTest(true);
    }

    public void findViewByIdTest(final boolean supportPredictive) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int initialAdapterSize = 20;
        final TestAdapter adapter = new TestAdapter(initialAdapterSize);
        final int deleteStart = 6;
        final int deleteCount = 5;
        recyclerView.setAdapter(adapter);
        final AtomicBoolean assertPositions = new AtomicBoolean(false);
        TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                if (assertPositions.get()) {
                    if (state.isPreLayout()) {
                        for (int i = 0; i < deleteStart; i++) {
                            View view = findViewByPosition(i);
                            assertNotNull("find view by position for existing items should work "
                                    + "fine", view);
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                        for (int i = 0; i < deleteCount; i++) {
                            View view = findViewByPosition(i + deleteStart);
                            assertNotNull("find view by position should work fine for removed "
                                    + "views in pre-layout", view);
                            assertTrue("view should be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                        for (int i = deleteStart + deleteCount; i < 20; i++) {
                            View view = findViewByPosition(i);
                            assertNotNull(view);
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                    } else {
                        for (int i = 0; i < initialAdapterSize - deleteCount; i++) {
                            View view = findViewByPosition(i);
                            assertNotNull("find view by position for existing item " + i +
                                    " should work fine. child count:" + getChildCount(), view);
                            TestViewHolder viewHolder =
                                    (TestViewHolder) mRecyclerView.getChildViewHolder(view);
                            assertSame("should be the correct item " + viewHolder
                                    , viewHolder.mBoundItem,
                                    adapter.mItems.get(viewHolder.mPosition));
                            assertFalse("view should not be marked as removed",
                                    ((RecyclerView.LayoutParams) view.getLayoutParams())
                                            .isItemRemoved());
                        }
                    }
                }
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, state.getItemCount() - 1, -1);
                layoutLatch.countDown();
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return supportPredictive;
            }
        };
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();

        assertPositions.set(true);
        lm.expectLayouts(supportPredictive ? 2 : 1);
        adapter.deleteAndNotify(new int[]{deleteStart, deleteCount - 1}, new int[]{deleteStart, 1});
        lm.waitForLayout(2);
    }

    @Test
    public void testTypeForCache() throws Throwable {
        final AtomicInteger viewType = new AtomicInteger(1);
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                return viewType.get();
            }

            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        final AtomicInteger layoutStart = new AtomicInteger(2);
        final int childCount = 10;
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, layoutStart.get(), layoutStart.get() + childCount);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        recyclerView.setItemViewCacheSize(10);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        layoutStart.set(4); // trigger a cache for 3,4
        lm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        lm.waitForLayout(2);
        //
        viewType.incrementAndGet();
        layoutStart.set(2); // go back to bring views from cache
        lm.expectLayouts(1);
        adapter.mItems.remove(1);
        adapter.dispatchDataSetChanged();
        lm.waitForLayout(2);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 2; i < 4; i++) {
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForLayoutPosition(i);
                    assertEquals("View holder's type should match latest type", viewType.get(),
                            vh.getItemViewType());
                }
            }
        });
    }

    @Test
    public void testTypeForExistingViews() throws Throwable {
        final AtomicInteger viewType = new AtomicInteger(1);
        final int invalidatedCount = 2;
        final int layoutStart = 2;
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                return viewType.get();
            }

            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (position >= layoutStart && position < invalidatedCount + layoutStart) {
                    try {
                        assertEquals("holder type should match current view type at position " +
                                position, viewType.get(), holder.getItemViewType());
                    } catch (Throwable t) {
                        postExceptionToInstrumentation(t);
                    }
                }
            }

            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);

        final int childCount = 10;
        final TestLayoutManager lm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, layoutStart, layoutStart + childCount);
                layoutLatch.countDown();
            }
        };
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(2);
        getInstrumentation().waitForIdleSync();
        viewType.incrementAndGet();
        lm.expectLayouts(1);
        adapter.changeAndNotify(layoutStart, invalidatedCount);
        lm.waitForLayout(2);
        checkForMainThreadException();
    }


    @Test
    public void testState() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        final AtomicInteger itemCount = new AtomicInteger();
        final AtomicBoolean structureChanged = new AtomicBoolean();
        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                itemCount.set(state.getItemCount());
                structureChanged.set(state.didStructureChange());
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(testLayoutManager);
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
        testLayoutManager.waitForLayout(2, TimeUnit.SECONDS);

        assertEquals("item count in state should be correct", adapter.getItemCount()
                , itemCount.get());
        assertEquals("structure changed should be true for first layout", true,
                structureChanged.get());
        Thread.sleep(1000); //wait for other layouts.
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.requestLayout();
            }
        });
        testLayoutManager.waitForLayout(2);
        assertEquals("in second layout,structure changed should be false", false,
                structureChanged.get());
        testLayoutManager.expectLayouts(1); //
        adapter.deleteAndNotify(3, 2);
        testLayoutManager.waitForLayout(2);
        assertEquals("when items are removed, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());
        testLayoutManager.expectLayouts(1);
        adapter.addAndNotify(2, 5);
        testLayoutManager.waitForLayout(2);

        assertEquals("when items are added, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());
    }

    @Test
    public void testDetachWithoutLayoutManager() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setRecyclerView(recyclerView);
                    removeRecyclerView();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        checkForMainThreadException();
    }

    @Test
    public void testUpdateHiddenView() throws Throwable {
        final RecyclerView.ViewHolder[] mTargetVH = new RecyclerView.ViewHolder[1];
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int[] preLayoutRange = new int[]{0, 10};
        final int[] postLayoutRange = new int[]{0, 10};
        final AtomicBoolean enableGetViewTest = new AtomicBoolean(false);
        final List<Integer> disappearingPositions = new ArrayList<Integer>();
        final TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    final int[] layoutRange = state.isPreLayout() ? preLayoutRange
                            : postLayoutRange;
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, layoutRange[0], layoutRange[1]);
                    if (!state.isPreLayout()) {
                        for (Integer position : disappearingPositions) {
                            // test sanity.
                            assertNull(findViewByPosition(position));
                            final View view = recycler.getViewForPosition(position);
                            addDisappearingView(view);
                            measureChildWithMargins(view, 0, 0);
                            // position item out of bounds.
                            view.layout(0, -500, view.getMeasuredWidth(),
                                    -500 + view.getMeasuredHeight());
                        }
                    }
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
                layoutLatch.countDown();
            }
        };

        recyclerView.getItemAnimator().setMoveDuration(2000);
        recyclerView.getItemAnimator().setRemoveDuration(2000);
        final TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);

        tlm.waitForLayout(1);
        checkForMainThreadException();
        mTargetVH[0] = recyclerView.findViewHolderForAdapterPosition(0);
        // now, a child disappears
        disappearingPositions.add(0);
        // layout one shifted
        postLayoutRange[0] = 1;
        postLayoutRange[1] = 11;
        tlm.expectLayouts(2);
        adapter.addAndNotify(8, 1);
        tlm.waitForLayout(2);
        checkForMainThreadException();

        tlm.expectLayouts(2);
        disappearingPositions.clear();
        // now that item should be moving, invalidate it and delete it.
        enableGetViewTest.set(true);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    adapter.changeAndNotify(0, 1);
                    adapter.deleteAndNotify(0, 1);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
        tlm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void testFocusBigViewOnTop() throws Throwable {
        focusTooBigViewTest(Gravity.TOP);
    }

    @Test
    public void testFocusBigViewOnLeft() throws Throwable {
        focusTooBigViewTest(Gravity.LEFT);
    }

    @Test
    public void testFocusBigViewOnRight() throws Throwable {
        focusTooBigViewTest(Gravity.RIGHT);
    }

    @Test
    public void testFocusBigViewOnBottom() throws Throwable {
        focusTooBigViewTest(Gravity.BOTTOM);
    }

    @Test
    public void testFocusBigViewOnLeftRTL() throws Throwable {
        focusTooBigViewTest(Gravity.LEFT, true);
        assertEquals("test sanity", ViewCompat.LAYOUT_DIRECTION_RTL,
                mRecyclerView.getLayoutManager().getLayoutDirection());
    }

    @Test
    public void testFocusBigViewOnRightRTL() throws Throwable {
        focusTooBigViewTest(Gravity.RIGHT, true);
        assertEquals("test sanity", ViewCompat.LAYOUT_DIRECTION_RTL,
                mRecyclerView.getLayoutManager().getLayoutDirection());
    }

    public void focusTooBigViewTest(final int gravity) throws Throwable {
        focusTooBigViewTest(gravity, false);
    }

    public void focusTooBigViewTest(final int gravity, final boolean rtl) throws Throwable {
        RecyclerView rv = new RecyclerView(getActivity());
        if (rtl) {
            ViewCompat.setLayoutDirection(rv, ViewCompat.LAYOUT_DIRECTION_RTL);
        }
        final AtomicInteger vScrollDist = new AtomicInteger(0);
        final AtomicInteger hScrollDist = new AtomicInteger(0);
        final AtomicInteger vDesiredDist = new AtomicInteger(0);
        final AtomicInteger hDesiredDist = new AtomicInteger(0);
        TestLayoutManager tlm = new TestLayoutManager() {

            @Override
            public int getLayoutDirection() {
                return rtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                final View view = recycler.getViewForPosition(0);
                addView(view);
                int left = 0, top = 0;
                view.setBackgroundColor(Color.rgb(0, 0, 255));
                switch (gravity) {
                    case Gravity.LEFT:
                    case Gravity.RIGHT:
                        view.measure(
                                View.MeasureSpec.makeMeasureSpec((int) (getWidth() * 1.5),
                                        View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec((int) (getHeight() * .9),
                                        View.MeasureSpec.AT_MOST));
                        left = gravity == Gravity.LEFT ? getWidth() - view.getMeasuredWidth() - 80
                                : 90;
                        top = 0;
                        if (ViewCompat.LAYOUT_DIRECTION_RTL == getLayoutDirection()) {
                            hDesiredDist.set((left + view.getMeasuredWidth()) - getWidth());
                        } else {
                            hDesiredDist.set(left);
                        }
                        break;
                    case Gravity.TOP:
                    case Gravity.BOTTOM:
                        view.measure(
                                View.MeasureSpec.makeMeasureSpec((int) (getWidth() * .9),
                                        View.MeasureSpec.AT_MOST),
                                View.MeasureSpec.makeMeasureSpec((int) (getHeight() * 1.5),
                                        View.MeasureSpec.EXACTLY));
                        top = gravity == Gravity.TOP ? getHeight() - view.getMeasuredHeight() -
                                80 : 90;
                        left = 0;
                        vDesiredDist.set(top);
                        break;
                }

                view.layout(left, top, left + view.getMeasuredWidth(),
                        top + view.getMeasuredHeight());
                layoutLatch.countDown();
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public boolean canScrollHorizontally() {
                return super.canScrollHorizontally();
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                vScrollDist.addAndGet(dy);
                getChildAt(0).offsetTopAndBottom(-dy);
                return dy;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                hScrollDist.addAndGet(dx);
                getChildAt(0).offsetLeftAndRight(-dx);
                return dx;
            }
        };
        TestAdapter adapter = new TestAdapter(10);
        rv.setAdapter(adapter);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(2);
        View view = rv.getChildAt(0);
        requestFocus(view);
        Thread.sleep(1000);
        assertEquals(vDesiredDist.get(), vScrollDist.get());
        assertEquals(hDesiredDist.get(), hScrollDist.get());
        assertEquals(mRecyclerView.getPaddingTop(), view.getTop());
        if (rtl) {
            assertEquals(mRecyclerView.getWidth() - mRecyclerView.getPaddingRight(),
                    view.getRight());
        } else {
            assertEquals(mRecyclerView.getPaddingLeft(), view.getLeft());
        }
    }

    @Test
    public void testFocusRectOnScreenWithDecorOffsets() throws Throwable {
        focusRectOnScreenTest(true);
    }

    @Test
    public void testFocusRectOnScreenWithout() throws Throwable {
        focusRectOnScreenTest(false);
    }


    public void focusRectOnScreenTest(boolean addItemDecors) throws Throwable {
        RecyclerView rv = new RecyclerView(getActivity());
        final AtomicInteger scrollDist = new AtomicInteger(0);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                final View view = recycler.getViewForPosition(0);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                view.layout(0, -20, view.getWidth(),
                        -20 + view.getHeight());// ignore decors on purpose
                layoutLatch.countDown();
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                scrollDist.addAndGet(dy);
                return dy;
            }
        };
        TestAdapter adapter = new TestAdapter(10);
        if (addItemDecors) {
            rv.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                        RecyclerView.State state) {
                    outRect.set(0, 10, 0, 10);
                }
            });
        }
        rv.setAdapter(adapter);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(2);

        View view = rv.getChildAt(0);
        requestFocus(view);
        Thread.sleep(1000);
        assertEquals(addItemDecors ? -30 : -20, scrollDist.get());
    }

    @Test
    public void testUnimplementedSmoothScroll() throws Throwable {
        final AtomicInteger receivedScrollToPosition = new AtomicInteger(-1);
        final AtomicInteger receivedSmoothScrollToPosition = new AtomicInteger(-1);
        final CountDownLatch cbLatch = new CountDownLatch(2);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, 10);
                layoutLatch.countDown();
            }

            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                    int position) {
                assertEquals(-1, receivedSmoothScrollToPosition.get());
                receivedSmoothScrollToPosition.set(position);
                RecyclerView.SmoothScroller ss =
                        new LinearSmoothScroller(recyclerView.getContext()) {
                            @Override
                            public PointF computeScrollVectorForPosition(int targetPosition) {
                                return null;
                            }
                        };
                ss.setTargetPosition(position);
                startSmoothScroll(ss);
                cbLatch.countDown();
            }

            @Override
            public void scrollToPosition(int position) {
                assertEquals(-1, receivedScrollToPosition.get());
                receivedScrollToPosition.set(position);
                cbLatch.countDown();
            }
        };
        RecyclerView rv = new RecyclerView(getActivity());
        rv.setAdapter(new TestAdapter(100));
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(2);
        freezeLayout(true);
        smoothScrollToPosition(35);
        assertEquals("smoothScrollToPosition should be ignored when frozen",
                -1, receivedSmoothScrollToPosition.get());
        freezeLayout(false);
        smoothScrollToPosition(35);
        assertTrue("both scrolls should be called", cbLatch.await(3, TimeUnit.SECONDS));
        checkForMainThreadException();
        assertEquals(35, receivedSmoothScrollToPosition.get());
        assertEquals(35, receivedScrollToPosition.get());
    }

    @Test
    public void testJumpingJackSmoothScroller() throws Throwable {
        jumpingJackSmoothScrollerTest(true);
    }

    @Test
    public void testJumpingJackSmoothScrollerGoesIdle() throws Throwable {
        jumpingJackSmoothScrollerTest(false);
    }

    private void jumpingJackSmoothScrollerTest(final boolean succeed) throws Throwable {
        final List<Integer> receivedScrollToPositions = new ArrayList<>();
        final TestAdapter testAdapter = new TestAdapter(200);
        final AtomicBoolean mTargetFound = new AtomicBoolean(false);
        TestLayoutManager tlm = new TestLayoutManager() {
            int pendingScrollPosition = -1;
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                final int pos = pendingScrollPosition < 0 ? 0: pendingScrollPosition;
                layoutRange(recycler, pos, pos + 10);
                if (layoutLatch != null) {
                    layoutLatch.countDown();
                }
            }

            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                    final int position) {
                RecyclerView.SmoothScroller ss =
                        new LinearSmoothScroller(recyclerView.getContext()) {
                            @Override
                            public PointF computeScrollVectorForPosition(int targetPosition) {
                                return new PointF(0, 1);
                            }

                            @Override
                            protected void onTargetFound(View targetView, RecyclerView.State state,
                                                         Action action) {
                                super.onTargetFound(targetView, state, action);
                                mTargetFound.set(true);
                            }

                            @Override
                            protected void updateActionForInterimTarget(Action action) {
                                int limit = succeed ? getTargetPosition() : 100;
                                if (pendingScrollPosition + 2 < limit) {
                                    if (pendingScrollPosition != NO_POSITION) {
                                        assertEquals(pendingScrollPosition,
                                                getChildViewHolderInt(getChildAt(0))
                                                        .getAdapterPosition());
                                    }
                                    action.jumpTo(pendingScrollPosition + 2);
                                }
                            }
                        };
                ss.setTargetPosition(position);
                startSmoothScroll(ss);
            }

            @Override
            public void scrollToPosition(int position) {
                receivedScrollToPositions.add(position);
                pendingScrollPosition = position;
                requestLayout();
            }
        };
        final RecyclerView rv = new RecyclerView(getActivity());
        rv.setAdapter(testAdapter);
        rv.setLayoutManager(tlm);

        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(2);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.smoothScrollToPosition(150);
            }
        });
        int limit = 100;
        while (rv.getLayoutManager().isSmoothScrolling() && --limit > 0) {
            Thread.sleep(200);
            checkForMainThreadException();
        }
        checkForMainThreadException();
        assertTrue(limit > 0);
        for (int i = 1; i < 100; i+=2) {
            assertTrue("scroll positions must include " + i, receivedScrollToPositions.contains(i));
        }

        assertEquals(succeed, mTargetFound.get());

    }

    private static class TestViewHolder2 extends RecyclerView.ViewHolder {

        Object mData;

        public TestViewHolder2(View itemView) {
            super(itemView);
        }
    }

    private static class TestAdapter2 extends RecyclerView.Adapter<TestViewHolder2> {

        List<Item> mItems;

        private TestAdapter2(int count) {
            mItems = new ArrayList<Item>(count);
            for (int i = 0; i < count; i++) {
                mItems.add(new Item(i, "Item " + i));
            }
        }

        @Override
        public TestViewHolder2 onCreateViewHolder(ViewGroup parent,
                int viewType) {
            return new TestViewHolder2(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(TestViewHolder2 holder, int position) {
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.mText + "(" + item.mAdapterIndex + ")");
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static interface AdapterRunnable {

        public void run(TestAdapter adapter) throws Throwable;
    }

}
