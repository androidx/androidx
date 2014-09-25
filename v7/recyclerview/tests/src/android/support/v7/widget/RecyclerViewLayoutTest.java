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

import android.graphics.PointF;
import android.os.Debug;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.test.TouchUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

public class RecyclerViewLayoutTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "RecyclerViewLayoutTest";

    public RecyclerViewLayoutTest() {
        super(DEBUG);
    }

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

    public void testAccessRecyclerOnOnMeasure() throws Throwable {
        accessRecyclerOnOnMeasureTest(false);
        removeRecyclerView();
        accessRecyclerOnOnMeasureTest(true);
    }

    public void testSmoothScrollWithRemovedItems() throws Throwable {
        smoothScrollTest(false);
        removeRecyclerView();
        smoothScrollTest(true);
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
                        }
                    }, 200);
                }
            });
            checkForMainThreadException();
            assertTrue("on stop should be called", calledOnStop.await(30, TimeUnit.SECONDS));
            checkForMainThreadException();
            assertNotNull("should scroll to new target " + newTarget
                    , rv.findViewHolderForPosition(newTarget));
            if (DEBUG) {
                Log.d(TAG, "on stop has been called on time");
            }
        } else {
            assertTrue("on stop should be called eventually",
                    calledOnStop.await(30, TimeUnit.SECONDS));
            assertNotNull("scroll to position should succeed",
                    rv.findViewHolderForPosition(targetPosition));
        }
        checkForMainThreadException();
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
                } catch(Throwable t) {
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
                    for (int i = 0; i < getChildCount(); i ++) {
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
        setRecyclerView(recyclerView);
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
                        for (int i = 0; i < getChildCount(); i ++) {
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
            changes.put(mRecyclerView.findViewHolderForPosition(i).getItemId(), false);
        }
        for (int i = itemAddedTo; i < mRecyclerView.getChildCount(); i++) {
            changes.put(mRecyclerView.findViewHolderForPosition(i).getItemId(), true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.addAndNotify(5, 1);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        changes.clear();
        int[] changedItems = new int[]{3, 5, 6};
        for (int i = 0; i < adapter.getItemCount(); i ++) {
            changes.put(mRecyclerView.findViewHolderForPosition(i).getItemId(), false);
        }
        for (int i = 0; i < changedItems.length; i ++) {
            changes.put(mRecyclerView.findViewHolderForPosition(changedItems[i]).getItemId(), true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.changePositionsAndNotify(changedItems);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        for (int i = 0; i < adapter.getItemCount(); i ++) {
            changes.put(mRecyclerView.findViewHolderForPosition(i).getItemId(), true);
        }
        testLayoutManager.expectLayouts(1);
        adapter.dispatchDataSetChanged();
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

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
                    throwable.printStackTrace();
                }
            }
        });

        lm.assertNoLayout("When RV is not attached, layout should not happen", 1);
        assertEquals("No extra layout should happen when detached", prevLayoutCount,
                layoutCount.get());
    }

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
                            final int oldPos = previousItems.indexOf(tvh.mBindedItem);
                            assertEquals("view holder's position should be correct",
                                    oldPositionToNewPositionMapping.get(oldPos).intValue(),
                                    tvh.getPosition());
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
                                    , viewHolder.mBindedItem,
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
                    RecyclerView.ViewHolder vh = recyclerView.findViewHolderForPosition(i);
                    assertEquals("View holder's type should match latest type", viewType.get(),
                            vh.getItemViewType());
                }
            }
        });
    }

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

    private static class TestViewHolder2 extends RecyclerView.ViewHolder {
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

}
