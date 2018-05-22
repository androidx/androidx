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

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING;
import static androidx.recyclerview.widget.RecyclerView.getChildViewHolderInt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.test.NestedScrollingParent2Adapter;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewLayoutTest extends BaseRecyclerViewInstrumentationTest {
    private static final int FLAG_HORIZONTAL = 1;
    private static final int FLAG_VERTICAL = 1 << 1;
    private static final int FLAG_FLING = 1 << 2;

    private static final boolean DEBUG = false;

    private static final String TAG = "RecyclerViewLayoutTest";

    public RecyclerViewLayoutTest() {
        super(DEBUG);
    }

    @Test
    public void triggerFocusSearchInOnRecycledCallback() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity()) {
            @Override
            void consumePendingUpdateOperations() {
                try {
                    super.consumePendingUpdateOperations();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        };
        final AtomicBoolean receivedOnRecycled = new AtomicBoolean(false);
        final TestAdapter adapter = new TestAdapter(20) {
            @Override
            public void onViewRecycled(@NonNull TestViewHolder holder) {
                super.onViewRecycled(holder);
                if (receivedOnRecycled.getAndSet(true)) {
                    return;
                }
                rv.focusSearch(rv.getChildAt(0), View.FOCUS_FORWARD);
            }
        };
        final AtomicInteger layoutCnt = new AtomicInteger(5);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, layoutCnt.get());
                layoutLatch.countDown();
            }
        };
        rv.setLayoutManager(tlm);
        rv.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(2);

        layoutCnt.set(4);
        tlm.expectLayouts(1);
        requestLayoutOnUIThread(rv);
        tlm.waitForLayout(1);

        assertThat("test sanity", rv.mRecycler.mCachedViews.size(), is(1));
        tlm.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyItemChanged(4);
                rv.smoothScrollBy(0, 1);
            }
        });
        checkForMainThreadException();
        tlm.waitForLayout(2);
        assertThat("test sanity", rv.mRecycler.mCachedViews.size(), is(0));
        assertThat(receivedOnRecycled.get(), is(true));
    }

    @Test
    public void detachAttachGetReadyWithoutChanges() throws Throwable {
        detachAttachGetReady(false, false, false);
    }

    @Test
    public void detachAttachGetReadyRequireLayout() throws Throwable {
        detachAttachGetReady(true, false, false);
    }

    @Test
    public void detachAttachGetReadyRemoveAdapter() throws Throwable {
        detachAttachGetReady(false, true, false);
    }

    @Test
    public void detachAttachGetReadyRemoveLayoutManager() throws Throwable {
        detachAttachGetReady(false, false, true);
    }

    private void detachAttachGetReady(final boolean requestLayoutOnDetach,
            final boolean removeAdapter, final boolean removeLayoutManager) throws Throwable {
        final LinearLayout ll1 = new LinearLayout(getActivity());
        final LinearLayout ll2 = new LinearLayout(getActivity());
        final LinearLayout ll3 = new LinearLayout(getActivity());

        final RecyclerView rv = new RecyclerView(getActivity());
        ll1.addView(ll2);
        ll2.addView(ll3);
        ll3.addView(rv);
        TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutCompleted(RecyclerView.State state) {
                super.onLayoutCompleted(state);
                layoutLatch.countDown();
            }

            @Override
            public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
                super.onDetachedFromWindow(view, recycler);
                if (requestLayoutOnDetach) {
                    view.requestLayout();
                }
            }
        };
        rv.setLayoutManager(layoutManager);
        rv.setAdapter(new TestAdapter(10));
        layoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(ll1);
            }
        });
        layoutManager.waitForLayout(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll1.removeView(ll2);
            }
        });
        getInstrumentation().waitForIdleSync();
        if (removeLayoutManager) {
            rv.setLayoutManager(null);
            rv.setLayoutManager(layoutManager);
        }
        if (removeAdapter) {
            rv.setAdapter(null);
            rv.setAdapter(new TestAdapter(10));
        }
        final boolean requireLayout = requestLayoutOnDetach || removeAdapter || removeLayoutManager;
        layoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll1.addView(ll2);
                if (requireLayout) {
                    assertTrue(rv.hasPendingAdapterUpdates());
                    assertFalse(rv.mFirstLayoutComplete);
                } else {
                    assertFalse(rv.hasPendingAdapterUpdates());
                    assertTrue(rv.mFirstLayoutComplete);
                }
            }
        });
        if (requireLayout) {
            layoutManager.waitForLayout(2);
        } else {
            layoutManager.assertNoLayout("nothing is invalid, layout should not happen", 2);
        }
    }

    @Test
    public void setAdapter_afterSwapAdapter_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        rv.swapAdapter(testAdapter, true);
                        rv.setAdapter(testAdapter);
                    }
                });
        lm.waitForLayout(2);

        assertEquals(2, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void setAdapter_afterNotifyDataSetChanged_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        testAdapter.notifyDataSetChanged();
                        rv.setAdapter(testAdapter);
                    }
                });
        lm.waitForLayout(2);

        assertEquals(1, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void notifyDataSetChanged_afterSetAdapter_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        rv.setAdapter(testAdapter);
                        testAdapter.notifyDataSetChanged();
                    }
                });
        lm.waitForLayout(2);

        assertEquals(1, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void notifyDataSetChanged_afterSwapAdapter_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        rv.swapAdapter(testAdapter, true);
                        testAdapter.notifyDataSetChanged();
                    }
                });
        lm.waitForLayout(2);

        assertEquals(1, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void swapAdapter_afterSetAdapter_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        rv.setAdapter(testAdapter);
                        rv.swapAdapter(testAdapter, true);
                    }
                });
        lm.waitForLayout(2);

        assertEquals(2, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void swapAdapter_afterNotifyDataSetChanged_callsCorrectLmMethods() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        final TestAdapter testAdapter = new TestAdapter(1);

        lm.expectLayouts(1);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        setAdapter(testAdapter);
        lm.waitForLayout(2);

        lm.onAdapterChagnedCallCount = 0;
        lm.onItemsChangedCallCount = 0;

        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        testAdapter.notifyDataSetChanged();
                        rv.swapAdapter(testAdapter, true);
                    }
                });
        lm.waitForLayout(2);

        assertEquals(1, lm.onAdapterChagnedCallCount);
        assertEquals(1, lm.onItemsChangedCallCount);
    }

    @Test
    public void setAdapterNotifyItemRangeInsertedCrashTest() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final TestLayoutManager lm = new LayoutAllLayoutManager(true);
        lm.setSupportsPredictive(true);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        lm.expectLayouts(1);
        setAdapter(new TestAdapter(1));
        lm.waitForLayout(2);
        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TestAdapter adapter2 = new TestAdapter(0);
                rv.setAdapter(adapter2);
                adapter2.addItems(0, 1, "1");
                adapter2.notifyItemRangeInserted(0, 1);
            }
        });
        lm.waitForLayout(2);
    }

    @Test
    public void swapAdapterNotifyItemRangeInsertedCrashTest() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final TestLayoutManager lm = new LayoutAllLayoutManager(true);
        lm.setSupportsPredictive(true);
        rv.setLayoutManager(lm);
        setRecyclerView(rv);
        lm.expectLayouts(1);
        setAdapter(new TestAdapter(1));
        lm.waitForLayout(2);
        lm.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TestAdapter adapter2 = new TestAdapter(0);
                rv.swapAdapter(adapter2, true);
                adapter2.addItems(0, 1, "1");
                adapter2.notifyItemRangeInserted(0, 1);
            }
        });
        lm.waitForLayout(2);
    }

    @Test
    public void onDataSetChanged_doesntHaveStableIds_cachedViewHasNoPosition() throws Throwable {
        onDataSetChanged_handleCachedViews(false);
    }

    @Test
    public void onDataSetChanged_hasStableIds_noCachedViewsAreRecycled() throws Throwable {
        onDataSetChanged_handleCachedViews(true);
    }

    /**
     * If Adapter#setHasStableIds(boolean) is false, cached ViewHolders should be recycled in
     * response to RecyclerView.Adapter#notifyDataSetChanged() and should report a position of
     * RecyclerView#NO_POSITION inside of
     * RecyclerView.Adapter#onViewRecycled(RecyclerView.ViewHolder).
     *
     * If Adapter#setHasStableIds(boolean) is true, cached Views/ViewHolders should not be recycled.
     */
    public void onDataSetChanged_handleCachedViews(boolean hasStableIds) throws Throwable {
        final AtomicInteger cachedRecycleCount = new AtomicInteger(0);

        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemViewCacheSize(1);

        final TestAdapter adapter = new TestAdapter(2) {
            @Override
            public void onViewRecycled(@NonNull TestViewHolder holder) {
                // If the recycled holder is currently in the cache, then it's position in the
                // adapter should be RecyclerView.NO_POSITION.
                if (mRecyclerView.mRecycler.mCachedViews.contains(holder)) {
                    assertThat("ViewHolder's getAdapterPosition should be "
                                    + "RecyclerView.NO_POSITION",
                            holder.getAdapterPosition(),
                            is(RecyclerView.NO_POSITION));
                    cachedRecycleCount.incrementAndGet();
                }
                super.onViewRecycled(holder);
            }
        };
        adapter.setHasStableIds(hasStableIds);
        recyclerView.setAdapter(adapter);

        final AtomicInteger numItemsToLayout = new AtomicInteger(2);

        TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, 0, numItemsToLayout.get());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    this.layoutLatch.countDown();
                }
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        };
        recyclerView.setLayoutManager(layoutManager);

        // Layout 2 items and sanity check that no items are in the recycler's cache.
        numItemsToLayout.set(2);
        layoutManager.expectLayouts(1);
        setRecyclerView(recyclerView, true, false);
        layoutManager.waitForLayout(2);
        checkForMainThreadException();
        assertThat("Sanity check, no views should be cached at this time",
                mRecyclerView.mRecycler.mCachedViews.size(),
                is(0));

        // Now only layout 1 item and assert that 1 item is cached.
        numItemsToLayout.set(1);
        layoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        layoutManager.waitForLayout(1);
        checkForMainThreadException();
        assertThat("One view should be cached.",
                mRecyclerView.mRecycler.mCachedViews.size(),
                is(1));

        // Notify data set has changed then final assert.
        layoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        layoutManager.waitForLayout(1);
        checkForMainThreadException();
        // If hasStableIds, then no cached views should be recycled, otherwise just 1 should have
        // been recycled.
        assertThat(cachedRecycleCount.get(), is(hasStableIds ? 0 : 1));
    }

    @Test
    public void notifyDataSetChanged_hasStableIds_cachedViewsAreReusedForSamePositions()
            throws Throwable {
        final Map<Integer, TestViewHolder> positionToViewHolderMap = new HashMap<>();
        final AtomicInteger layoutItemCount = new AtomicInteger();
        final AtomicBoolean inFirstBindViewHolderPass = new AtomicBoolean();

        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemViewCacheSize(5);

        final TestAdapter adapter = new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
                // Only track the top 5 positions that are going to be cached and then reused.
                if (position >= 5) {
                    // If we are in the first phase, put the items in the map, if we are in the
                    // second phase, remove each one at the position and verify that it matches the
                    // provided ViewHolder.
                    if (inFirstBindViewHolderPass.get()) {
                        positionToViewHolderMap.put(position, holder);
                    } else {
                        TestViewHolder testViewHolder = positionToViewHolderMap.get(position);
                        assertThat(holder, is(testViewHolder));
                        positionToViewHolderMap.remove(position);
                    }
                }
                super.onBindViewHolder(holder, position);
            }
        };
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);

        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    detachAndScrapAttachedViews(recycler);
                    layoutRange(recycler, 0, layoutItemCount.get());
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

        // First layout 10 items, then verify that the map has all 5 ViewHolders in it that will
        // be cached, and sanity check that the cache is empty.
        inFirstBindViewHolderPass.set(true);
        layoutItemCount.set(10);
        testLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView, true, false);
        testLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        for (int i = 5; i < 10; i++) {
            assertThat(positionToViewHolderMap.get(i), notNullValue());
        }
        assertThat(mRecyclerView.mRecycler.mCachedViews.size(), is(0));

        // Now only layout the first 5 items and verify that the cache has 5 items in it.
        layoutItemCount.set(5);
        testLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();
        assertThat(mRecyclerView.mRecycler.mCachedViews.size(), is(5));

        // Trigger notifyDataSetChanged and wait for layout.
        testLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();

        // Layout 10 items again, via the onBindViewholder method, check that each one of the views
        // returned from the recycler for positions >= 5 was in our cache of views, and verify that
        // all 5 cached views were returned.
        inFirstBindViewHolderPass.set(false);
        layoutItemCount.set(10);
        testLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        testLayoutManager.waitForLayout(1);
        checkForMainThreadException();
        assertThat(positionToViewHolderMap.size(), is(0));
    }

    @Test
    public void predictiveMeasuredCrashTest() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true) {
            @Override
            public void onAttachedToWindow(RecyclerView view) {
                super.onAttachedToWindow(view);
                assertThat(view.mLayout, is((RecyclerView.LayoutManager) this));
            }

            @Override
            public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
                super.onDetachedFromWindow(view, recycler);
                assertThat(view.mLayout, is((RecyclerView.LayoutManager) this));
            }

            @Override
            public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                    int widthSpec,
                    int heightSpec) {
                if (state.getItemCount() > 0) {
                    // A typical LayoutManager will use a child view to measure the size.
                    View v = recycler.getViewForPosition(0);
                }
                super.onMeasure(recycler, state, widthSpec, heightSpec);
            }

            @Override
            public boolean isAutoMeasureEnabled() {
                return false;
            }
        };
        lm.setSupportsPredictive(true);
        rv.setHasFixedSize(false);
        final TestAdapter adapter = new TestAdapter(0);
        rv.setAdapter(adapter);
        rv.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(rv);
        lm.waitForLayout(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) rv.getParent();
                parent.removeView(rv);
                // setting RV as child of LinearLayout using MATCH_PARENT will cause
                // RV.onMeasure() being called twice before layout(). This may cause crash.
                LinearLayout linearLayout = new LinearLayout(parent.getContext());
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                parent.addView(linearLayout,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                linearLayout.addView(rv, ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);

            }
        });

        lm.expectLayouts(1);
        adapter.addAndNotify(1);
        lm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void detachRvAndLayoutManagerProperly() throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        final LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true) {
            @Override
            public void onAttachedToWindow(RecyclerView view) {
                super.onAttachedToWindow(view);
                assertThat(view.mLayout, is((RecyclerView.LayoutManager) this));
            }

            @Override
            public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
                super.onDetachedFromWindow(view, recycler);
                assertThat(view.mLayout, is((RecyclerView.LayoutManager) this));
            }
        };
        final Runnable check = new Runnable() {
            @Override
            public void run() {
                assertThat("bound between the RV and the LM should be disconnected at the"
                        + " same time", rv.mLayout == lm, is(lm.mRecyclerView == rv));
            }
        };
        final AtomicInteger detachCounter = new AtomicInteger(0);
        rv.setAdapter(new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                holder.itemView.setFocusable(true);
                holder.itemView.setFocusableInTouchMode(true);
            }

            @Override
            public void onViewDetachedFromWindow(TestViewHolder holder) {
                super.onViewDetachedFromWindow(holder);
                detachCounter.incrementAndGet();
                check.run();
            }

            @Override
            public void onViewRecycled(@NonNull TestViewHolder holder) {
                super.onViewRecycled(holder);
                check.run();
            }
        });
        rv.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(rv);
        lm.waitForLayout(2);
        assertThat("test sanity", rv.getChildCount(), is(10));

        final TestLayoutManager replacement = new LayoutAllLayoutManager(true);
        replacement.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.setLayoutManager(replacement);
            }
        });
        replacement.waitForLayout(2);
        assertThat("test sanity", rv.getChildCount(), is(10));
        assertThat("all initial views should be detached", detachCounter.get(), is(10));
        checkForMainThreadException();
    }

    @Test
    public void focusSearchWithOtherFocusables() throws Throwable {
        final LinearLayout container = new LinearLayout(getActivity());
        container.setOrientation(LinearLayout.VERTICAL);
        RecyclerView rv = new RecyclerView(getActivity());
        mRecyclerView = rv;
        rv.setAdapter(new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                holder.itemView.setFocusableInTouchMode(true);
                holder.itemView.setLayoutParams(
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        });
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, 1);
                layoutLatch.countDown();
            }

            @Nullable
            @Override
            public View onFocusSearchFailed(View focused, int direction,
                    RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                int expectedDir = Build.VERSION.SDK_INT <= 15 ? View.FOCUS_DOWN :
                        View.FOCUS_FORWARD;
                assertEquals(expectedDir, direction);
                assertEquals(1, getChildCount());
                View child0 = getChildAt(0);
                View view = recycler.getViewForPosition(1);
                addView(view);
                measureChild(view, 0, 0);
                layoutDecorated(view, 0, child0.getBottom(), getDecoratedMeasuredWidth(view),
                        child0.getBottom() + getDecoratedMeasuredHeight(view));
                return view;
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                super.scrollHorizontallyBy(dx, recycler, state);
                // offset by -dx because the views translate opposite of the scrolling direction
                mRecyclerView.offsetChildrenHorizontal(-dx);
                return dx;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                super.scrollVerticallyBy(dy, recycler, state);
                // offset by -dy because the views translate opposite of the scrolling direction
                mRecyclerView.offsetChildrenVertical(-dy);
                return dy;
            }

            @Override
            public boolean isAutoMeasureEnabled() {
                return true;
            }
        };
        rv.setLayoutManager(tlm);
        TextView viewAbove = new TextView(getActivity());
        viewAbove.setText("view above");
        viewAbove.setFocusableInTouchMode(true);
        container.addView(viewAbove);
        container.addView(rv);
        TextView viewBelow = new TextView(getActivity());
        viewBelow.setText("view below");
        viewBelow.setFocusableInTouchMode(true);
        container.addView(viewBelow);
        tlm.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(container);
            }
        });

        tlm.waitForLayout(2);
        requestFocus(viewAbove, true);
        assertTrue(viewAbove.hasFocus());
        View newFocused = focusSearch(viewAbove, View.FOCUS_FORWARD);
        assertThat(newFocused, sameInstance(rv.getChildAt(0)));
        newFocused = focusSearch(rv.getChildAt(0), View.FOCUS_FORWARD);
        assertThat(newFocused, sameInstance(rv.getChildAt(1)));
    }

    @Test
    public void boundingBoxNoTranslation() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                assertThat(getTransformedBoundingBox(view), is(new Rect(10, 10, 30, 50)));
            }
        });
    }

    @Test
    public void boundingBoxTranslateX() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                view.setTranslationX(10);
                assertThat(getTransformedBoundingBox(view), is(new Rect(20, 10, 40, 50)));
            }
        });
    }

    @Test
    public void boundingBoxTranslateY() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                view.setTranslationY(10);
                assertThat(getTransformedBoundingBox(view), is(new Rect(10, 20, 30, 60)));
            }
        });
    }

    @Test
    public void boundingBoxScaleX() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                view.setScaleX(2);
                assertThat(getTransformedBoundingBox(view), is(new Rect(0, 10, 40, 50)));
            }
        });
    }

    @Test
    public void boundingBoxScaleY() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                view.setScaleY(2);
                assertThat(getTransformedBoundingBox(view), is(new Rect(10, -10, 30, 70)));
            }
        });
    }

    @Test
    public void boundingBoxRotated() throws Throwable {
        transformedBoundingBoxTest(new ViewRunnable() {
            @Override
            public void run(View view) throws RuntimeException {
                view.layout(10, 10, 30, 50);
                view.setRotation(90);
                assertThat(getTransformedBoundingBox(view), is(new Rect(0, 20, 40, 40)));
            }
        });
    }

    @Test
    public void boundingBoxRotatedWithDecorOffsets() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter adapter = new TestAdapter(1);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                    RecyclerView.State state) {
                outRect.set(1, 2, 3, 4);
            }
        });
        TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                View view = recycler.getViewForPosition(0);
                addView(view);
                view.measure(
                        View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(40, View.MeasureSpec.EXACTLY)
                );
                // trigger decor offsets calculation
                calculateItemDecorationsForChild(view, new Rect());
                view.layout(10, 10, 30, 50);
                view.setRotation(90);
                assertThat(RecyclerViewLayoutTest.this.getTransformedBoundingBox(view),
                        is(new Rect(-4, 19, 42, 43)));

                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        layoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    private Rect getTransformedBoundingBox(View child) {
        Rect rect = new Rect();
        mRecyclerView.getLayoutManager().getTransformedBoundingBox(child, true, rect);
        return rect;
    }

    public void transformedBoundingBoxTest(final ViewRunnable layout) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter adapter = new TestAdapter(1);
        recyclerView.setAdapter(adapter);
        TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                View view = recycler.getViewForPosition(0);
                addView(view);
                view.measure(
                        View.MeasureSpec.makeMeasureSpec(20, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(40, View.MeasureSpec.EXACTLY)
                );
                layout.run(view);
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        layoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        layoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void flingFrozen() throws Throwable {
        testScrollFrozen(true);
    }

    @Test
    public void dragFrozen() throws Throwable {
        testScrollFrozen(false);
    }

    @Test
    public void requestRectOnScreenWithScrollOffset() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final LayoutAllLayoutManager tlm = spy(new LayoutAllLayoutManager());
        final int scrollY = 50;
        RecyclerView.Adapter adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                View view = new View(parent.getContext());
                view.setScrollY(scrollY);
                return new RecyclerView.ViewHolder(view) {};
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {}
            @Override
            public int getItemCount() {
                return 1;
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        final View child = recyclerView.getChildAt(0);
        assertThat(child.getScrollY(), CoreMatchers.is(scrollY));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.requestChildRectangleOnScreen(child, new Rect(3, 4, 5, 6), true);
                verify(tlm, times(1)).scrollVerticallyBy(eq(-46), any(RecyclerView.Recycler.class),
                        any(RecyclerView.State.class));
            }
        });
    }

    @Test
    public void reattachAndScrollCrash() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestLayoutManager tlm = new TestLayoutManager() {

            @Override
            public boolean canScrollVertically() {
                return true;
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, Math.min(state.getItemCount(), 10));
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                // Access views in the state (that might have been deleted).
                for (int  i = 10; i < state.getItemCount(); i++) {
                    recycler.getViewForPosition(i);
                }
                return dy;
            }
        };

        final TestAdapter adapter = new TestAdapter(12);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);

        setRecyclerView(recyclerView);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().removeView(recyclerView);
                getActivity().getContainer().addView(recyclerView);
                try {
                    adapter.deleteAndNotify(1, adapter.getItemCount() - 1);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
                recyclerView.scrollBy(0, 10);
            }
        });
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
            TouchUtils.dragViewTo(getInstrumentation(), recyclerView,
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
            TouchUtils.dragViewTo(getInstrumentation(), recyclerView,
                    Gravity.LEFT | Gravity.TOP,
                    mRecyclerView.getWidth() / 2, mRecyclerView.getHeight() / 2);
        }
        assertEquals("rv's horizontal scroll cb must finishes", 0, horizontalCounter.get());
        assertEquals("rv's vertical scroll cb must finishes", 0, verticalCounter.get());
    }

    @Test
    public void testFocusSearchAfterChangedData() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, 2);
                layoutLatch.countDown();
            }

            @Nullable
            @Override
            public View onFocusSearchFailed(View focused, int direction,
                    RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                try {
                    recycler.getViewForPosition(state.getItemCount() - 1);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
                return null;
            }
        };
        recyclerView.setLayoutManager(tlm);
        final TestAdapter adapter = new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                holder.itemView.setFocusable(false);
                holder.itemView.setFocusableInTouchMode(false);
            }
        };
        recyclerView.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.mItems.remove(9);
                adapter.notifyItemRemoved(9);
                recyclerView.focusSearch(recyclerView.getChildAt(1), View.FOCUS_DOWN);
            }
        });
        checkForMainThreadException();
    }

    @Test
    public void testFocusSearchWithRemovedFocusedItem() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemAnimator(null);
        TestLayoutManager tlm = new LayoutAllLayoutManager();
        recyclerView.setLayoutManager(tlm);
        final TestAdapter adapter = new TestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                holder.itemView.setFocusable(true);
                holder.itemView.setFocusableInTouchMode(true);
            }
        };
        recyclerView.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        final RecyclerView.ViewHolder toFocus = recyclerView.findViewHolderForAdapterPosition(9);
        requestFocus(toFocus.itemView, true);
        assertThat("test sanity", toFocus.itemView.hasFocus(), is(true));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.mItems.remove(9);
                adapter.notifyItemRemoved(9);
                recyclerView.focusSearch(toFocus.itemView, View.FOCUS_DOWN);
            }
        });
        checkForMainThreadException();
    }


    @Test
    public void  testFocusSearchFailFrozen() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        final CountDownLatch focusLatch = new CountDownLatch(1);
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
            public View onFocusSearchFailed(View focused, int direction,
                    RecyclerView.Recycler recycler, RecyclerView.State state) {
                focusSearchCalled.addAndGet(1);
                focusLatch.countDown();
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
        assertTrue("test sanity", requestFocus(c, true));
        assertTrue("test sanity", c.hasFocus());
        freezeLayout(true);
        focusSearch(recyclerView, c, View.FOCUS_DOWN);
        assertEquals("onFocusSearchFailed should not be called when layout is frozen",
                0, focusSearchCalled.get());
        freezeLayout(false);
        focusSearch(c, View.FOCUS_DOWN);
        assertTrue(focusLatch.await(2, TimeUnit.SECONDS));
        assertEquals(1, focusSearchCalled.get());
    }

    public View focusSearch(final ViewGroup parent, final View focused, final int direction)
            throws Throwable {
        final View[] result = new View[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = parent.focusSearch(focused, direction);
            }
        });
        return result[0];
    }

    @Test
    public void frozenAndChangeAdapter() throws Throwable {
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
    public void moveAndUpdateCachedViews() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setItemViewCacheSize(3);
        recyclerView.setItemAnimator(null);
        final TestAdapter adapter = new TestAdapter(1000);
        final CountDownLatch layoutLatch = new CountDownLatch(1);
        LinearLayoutManager tlm = new LinearLayoutManager(recyclerView.getContext()) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutLatch.countDown();
            }
        };
        tlm.setItemPrefetchEnabled(false);
        recyclerView.setLayoutManager(tlm);
        recyclerView.setAdapter(adapter);
        setRecyclerView(recyclerView);
        // wait first layout pass
        layoutLatch.await();
        // scroll and hide 0 and 1
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.smoothScrollBy(0, recyclerView.getChildAt(2).getTop() + 5);
            }
        });
        waitForIdleScroll(recyclerView);
        assertNull(recyclerView.findViewHolderForAdapterPosition(0));
        assertNull(recyclerView.findViewHolderForAdapterPosition(1));
        // swap 1 and 0 and update 0
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // swap 1 and 0
                    adapter.moveInUIThread(1, 0);
                    adapter.notifyItemMoved(1, 0);
                    // update 0
                    adapter.mItems.get(0).mText = "updated";
                    adapter.notifyItemChanged(0);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });
        // scroll back to 0
        smoothScrollToPosition(0);
        waitForIdleScroll(recyclerView);
        TestViewHolder vh = (TestViewHolder) recyclerView.findViewHolderForAdapterPosition(0);
        // assert updated right item
        assertTrue((((TextView) (vh.itemView)).getText()).toString().contains("updated"));
        checkForMainThreadException();
    }

    @Test
    public void noLayoutIf0ItemsAreChanged() throws Throwable {
        unnecessaryNotifyEvents(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.notifyItemRangeChanged(3, 0);
            }
        });
    }

    @Test
    public void noLayoutIf0ItemsAreChangedWithPayload() throws Throwable {
        unnecessaryNotifyEvents(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.notifyItemRangeChanged(0, 0, new Object());
            }
        });
    }

    @Test
    public void noLayoutIf0ItemsAreAdded() throws Throwable {
        unnecessaryNotifyEvents(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.notifyItemRangeInserted(3, 0);
            }
        });
    }

    @Test
    public void noLayoutIf0ItemsAreRemoved() throws Throwable {
        unnecessaryNotifyEvents(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.notifyItemRangeRemoved(3, 0);
            }
        });
    }

    @Test
    public void noLayoutIfItemMovedIntoItsOwnPlace() throws Throwable {
        unnecessaryNotifyEvents(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.notifyItemMoved(3, 3);
            }
        });
    }

    public void unnecessaryNotifyEvents(final AdapterRunnable action) throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter adapter = new TestAdapter(5);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(tlm);
        recyclerView.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        // ready
        tlm.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    action.run(adapter);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });
        tlm.assertNoLayout("dummy event should not trigger a layout", 1);
        checkForMainThreadException();
    }

    @Test
    public void scrollToPositionCallback() throws Throwable {

        class TestRecyclerView extends RecyclerView {

            private CountDownLatch mDrawLatch;

            TestRecyclerView(Context context) {
                super(context);
            }

            public void expectDraws(int count) {
                mDrawLatch = new CountDownLatch(count);
            }

            public void waitForDraw(int seconds) throws InterruptedException {
                mDrawLatch.await(seconds, TimeUnit.SECONDS);
            }

            @Override
            public void onDraw(Canvas c) {
                super.onDraw(c);
                if (mDrawLatch != null) {
                    mDrawLatch.countDown();
                }
            }
        }

        TestRecyclerView recyclerView = new TestRecyclerView(getActivity());
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                rvCounter.incrementAndGet();
            }
        });

        getRecyclerViewContainer().getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        viewGroupCounter.incrementAndGet();
                    }
                });

        recyclerView.expectDraws(1);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);
        recyclerView.waitForDraw(2);
        assertEquals("RV on scroll should be called for initialization", 1, rvCounter.get());
        assertEquals("VTO on scroll should be called for initialization", 1,
                viewGroupCounter.get());

        recyclerView.expectDraws(1);
        tlm.expectLayouts(1);
        freezeLayout(true);
        scrollToPosition(3);
        tlm.assertNoLayout("scrollToPosition should be ignored", 2);
        freezeLayout(false);
        scrollToPosition(3);
        tlm.waitForLayout(2);
        recyclerView.waitForDraw(2);
        assertEquals("RV on scroll should be called", 2, rvCounter.get());
        assertEquals("VTO on scroll should be called", 2, viewGroupCounter.get());

        recyclerView.expectDraws(1);
        tlm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        tlm.waitForLayout(2);
        recyclerView.waitForDraw(2);
        assertEquals("on scroll should NOT be called", 2, rvCounter.get());
        assertEquals("on scroll should NOT be called", 2, viewGroupCounter.get());
    }

    @Test
    public void scrollCallbackFromEmptyToSome() throws Throwable {
        scrollCallbackOnVisibleRangeChange(1, new int[]{0, 0}, new int[]{0, 1});
    }

    @Test
    public void scrollCallbackOnVisibleRangeExpand() throws Throwable {
        scrollCallbackOnVisibleRangeChange(10, new int[]{3, 5}, new int[]{3, 6});
    }

    @Test
    public void scrollCallbackOnVisibleRangeShrink() throws Throwable {
        scrollCallbackOnVisibleRangeChange(10, new int[]{3, 6}, new int[]{3, 5});
    }

    @Test
    public void scrollCallbackOnVisibleRangeExpand2() throws Throwable {
        scrollCallbackOnVisibleRangeChange(10, new int[]{3, 5}, new int[]{2, 5});
    }

    @Test
    public void scrollCallbackOnVisibleRangeShrink2() throws Throwable {
        scrollCallbackOnVisibleRangeChange(10, new int[]{3, 6}, new int[]{2, 6});
    }

    private void scrollCallbackOnVisibleRangeChange(int itemCount, final int[] beforeRange,
            final int[] afterRange) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity()) {
            @Override
            void dispatchLayout() {
                super.dispatchLayout();
                ((TestLayoutManager) getLayoutManager()).layoutLatch.countDown();
            }
        };
        final AtomicBoolean beforeState = new AtomicBoolean(true);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                int[] range = beforeState.get() ? beforeRange : afterRange;
                layoutRange(recycler, range[0], range[1]);
            }
        };
        recyclerView.setLayoutManager(tlm);
        final TestAdapter adapter = new TestAdapter(itemCount);
        recyclerView.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);

        RecyclerView.OnScrollListener mockListener = mock(RecyclerView.OnScrollListener.class);
        recyclerView.addOnScrollListener(mockListener);
        verify(mockListener, never()).onScrolled(any(RecyclerView.class), anyInt(), anyInt());

        tlm.expectLayouts(1);
        beforeState.set(false);
        requestLayoutOnUIThread(recyclerView);
        tlm.waitForLayout(2);
        checkForMainThreadException();
        verify(mockListener).onScrolled(recyclerView, 0, 0);
    }

    @Test
    public void addItemOnScroll() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        final AtomicInteger start = new AtomicInteger(0);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, start.get(), start.get() + 10);
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(tlm);
        final TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        final Throwable[] error = new Throwable[1];
        final AtomicBoolean calledOnScroll = new AtomicBoolean(false);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                calledOnScroll.set(true);
                try {
                    adapter.addAndNotify(5, 20);
                } catch (Throwable throwable) {
                    error[0] = throwable;
                }
            }
        });
        start.set(4);
        MatcherAssert.assertThat("test sanity", calledOnScroll.get(), CoreMatchers.is(false));
        tlm.expectLayouts(1);
        requestLayoutOnUIThread(recyclerView);
        tlm.waitForLayout(2);
        checkForMainThreadException();
        MatcherAssert.assertThat("test sanity", calledOnScroll.get(), CoreMatchers.is(true));
        MatcherAssert.assertThat(error[0], CoreMatchers.nullValue());
    }

    @Test
    public void scrollInBothDirectionEqual() throws Throwable {
        scrollInBothDirection(3, 3, 1000, 1000);
    }

    @Test
    public void scrollInBothDirectionMoreVertical() throws Throwable {
        scrollInBothDirection(2, 3, 1000, 1000);
    }

    @Test
    public void scrollInBothDirectionMoreHorizontal() throws Throwable {
        scrollInBothDirection(3, 2, 1000, 1000);
    }

    @Test
    public void scrollHorizontalOnly() throws Throwable {
        scrollInBothDirection(3, 0, 1000, 0);
    }

    @Test
    public void scrollVerticalOnly() throws Throwable {
        scrollInBothDirection(0, 3, 0, 1000);
    }

    @Test
    public void scrollInBothDirectionEqualReverse() throws Throwable {
        scrollInBothDirection(3, 3, -1000, -1000);
    }

    @Test
    public void scrollInBothDirectionMoreVerticalReverse() throws Throwable {
        scrollInBothDirection(2, 3, -1000, -1000);
    }

    @Test
    public void scrollInBothDirectionMoreHorizontalReverse() throws Throwable {
        scrollInBothDirection(3, 2, -1000, -1000);
    }

    @Test
    public void scrollHorizontalOnlyReverse() throws Throwable {
        scrollInBothDirection(3, 0, -1000, 0);
    }

    @Test
    public void scrollVerticalOnlyReverse() throws Throwable {
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
    public void dragHorizontal() throws Throwable {
        scrollInOtherOrientationTest(FLAG_HORIZONTAL);
    }

    @Test
    public void dragVertical() throws Throwable {
        scrollInOtherOrientationTest(FLAG_VERTICAL);
    }

    @Test
    public void flingHorizontal() throws Throwable {
        scrollInOtherOrientationTest(FLAG_HORIZONTAL | FLAG_FLING);
    }

    @Test
    public void flingVertical() throws Throwable {
        scrollInOtherOrientationTest(FLAG_VERTICAL | FLAG_FLING);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void nestedDragVertical() throws Throwable {
        final NestedScrollingParent2 nsp = spy(new FullyConsumingNestedScroller());
        getActivity().getContainer().setNestedScrollingDelegate(nsp);
        // Scroll and expect the RV to not scroll
        scrollInOtherOrientationTest(FLAG_VERTICAL, 0);

        // Verify that the touch nested scroll was started and finished
        verify(nsp, atLeastOnce()).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onStopNestedScroll(eq(mRecyclerView), eq(ViewCompat.TYPE_TOUCH));

        // Verify that the non-touch events were dispatched by the fling settle
        verify(nsp, times(1)).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void nestedDragHorizontal() throws Throwable {
        final NestedScrollingParent2 nsp = spy(new FullyConsumingNestedScroller());
        getActivity().getContainer().setNestedScrollingDelegate(nsp);
        // Scroll and expect the RV to not scroll
        scrollInOtherOrientationTest(FLAG_HORIZONTAL, 0);

        // Verify that the touch nested scroll was started and finished
        verify(nsp, atLeastOnce()).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, atLeastOnce()).onStopNestedScroll(eq(mRecyclerView), eq(ViewCompat.TYPE_TOUCH));

        // Verify that the non-touch events were dispatched by the fling settle
        verify(nsp, times(1)).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void nestedFlingVertical() throws Throwable {
        final NestedScrollingParent2 nsp = spy(new FullyConsumingNestedScroller());
        getActivity().getContainer().setNestedScrollingDelegate(nsp);
        // Fling and expect the RV to not scroll
        scrollInOtherOrientationTest(FLAG_VERTICAL | FLAG_FLING, FLAG_FLING);

        // Verify that the touch nested scroll was not started
        verify(nsp, never()).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_TOUCH);

        // Verify that the non-touch nested scroll was started and finished
        verify(nsp, times(1)).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_VERTICAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);
    }

    @SuppressWarnings("WrongConstant")
    @Test
    public void nestedFlingHorizontal() throws Throwable {
        final NestedScrollingParent2 nsp = spy(new FullyConsumingNestedScroller());
        getActivity().getContainer().setNestedScrollingDelegate(nsp);
        // Fling and expect the RV to not scroll
        scrollInOtherOrientationTest(FLAG_HORIZONTAL | FLAG_FLING, FLAG_FLING);

        // Verify that the touch nested scroll was not started
        verify(nsp, never()).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(nsp, never()).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_TOUCH);

        // Verify that the non-touch nested scroll was started and finished
        verify(nsp, times(1)).onStartNestedScroll(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onNestedScrollAccepted(eq(mRecyclerView), eq(mRecyclerView),
                eq(ViewCompat.SCROLL_AXIS_HORIZONTAL), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(nsp, times(1)).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_NON_TOUCH);
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
            TouchUtils.dragViewTo(getInstrumentation(), recyclerView, Gravity.LEFT | Gravity.TOP,
                    mRecyclerView.getWidth() / 2, mRecyclerView.getHeight() / 2);
        }
        assertEquals("horizontally scrolled: " + tlm.mScrollHorizontallyAmount,
                (expectedFlags & FLAG_HORIZONTAL) != 0, scrolledHorizontal.get());
        assertEquals("vertically scrolled: " + tlm.mScrollVerticallyAmount,
                (expectedFlags & FLAG_VERTICAL) != 0, scrolledVertical.get());
    }

    private boolean fling(final int velocityX, final int velocityY) throws Throwable {
        final AtomicBoolean didStart = new AtomicBoolean(false);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean result = mRecyclerView.fling(velocityX, velocityY);
                didStart.set(result);
            }
        });
        if (!didStart.get()) {
            return false;
        }
        waitForIdleScroll(mRecyclerView);
        return true;
    }

    private void assertPendingUpdatesAndLayoutTest(final AdapterRunnable runnable) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        final TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, false);
        layoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run(testAdapter);
                } catch (Throwable throwable) {
                    fail("runnable has thrown an exception");
                }
                assertTrue(mRecyclerView.hasPendingAdapterUpdates());
            }
        });
        layoutManager.waitForLayout(1);
        assertFalse(mRecyclerView.hasPendingAdapterUpdates());
        checkForMainThreadException();
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

    @Suppress
    @FlakyTest(bugId = 33949798)
    @Test
    @LargeTest
    public void hasPendingUpdatesBeforeFirstLayout() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, false);
        assertTrue(mRecyclerView.hasPendingAdapterUpdates());
    }

    @Test
    public void noPendingUpdatesAfterLayout() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        TestLayoutManager layoutManager = new DumbLayoutManager();
        TestAdapter testAdapter = new TestAdapter(10);
        setupBasic(recyclerView, layoutManager, testAdapter, true);
        assertFalse(mRecyclerView.hasPendingAdapterUpdates());
    }

    @Test
    public void hasPendingUpdatesAfterItemIsRemoved() throws Throwable {
        assertPendingUpdatesAndLayoutTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter testAdapter) throws Throwable {
                testAdapter.deleteAndNotify(1, 1);
            }
        });
    }
    @Test
    public void hasPendingUpdatesAfterItemIsInserted() throws Throwable {
        assertPendingUpdatesAndLayoutTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter testAdapter) throws Throwable {
                testAdapter.addAndNotify(2, 1);
            }
        });
    }
    @Test
    public void hasPendingUpdatesAfterItemIsMoved() throws Throwable {
        assertPendingUpdatesAndLayoutTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter testAdapter) throws Throwable {
                testAdapter.moveItem(2, 3, true);
            }
        });
    }
    @Test
    public void hasPendingUpdatesAfterItemIsChanged() throws Throwable {
        assertPendingUpdatesAndLayoutTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter testAdapter) throws Throwable {
                testAdapter.changeAndNotify(2, 1);
            }
        });
    }
    @Test
    public void hasPendingUpdatesAfterDataSetIsChanged() throws Throwable {
        assertPendingUpdatesAndLayoutTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter testAdapter) {
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void transientStateRecycleViaAdapter() throws Throwable {
        transientStateRecycleTest(true, false);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void transientStateRecycleViaTransientStateCleanup() throws Throwable {
        transientStateRecycleTest(false, true);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void transientStateDontRecycle() throws Throwable {
        transientStateRecycleTest(false, false);
    }

    public void transientStateRecycleTest(final boolean succeed, final boolean unsetTransientState)
            throws Throwable {
        final List<View> failedToRecycle = new ArrayList<>();
        final List<View> recycled = new ArrayList<>();
        TestAdapter testAdapter = new TestAdapter(10) {
            @Override
            public boolean onFailedToRecycleView(@NonNull TestViewHolder holder) {
                failedToRecycle.add(holder.itemView);
                if (unsetTransientState) {
                    setHasTransientState(holder.itemView, false);
                }
                return succeed;
            }

            @Override
            public void onViewRecycled(@NonNull TestViewHolder holder) {
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
    public void adapterPositionInvalidation() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void adapterPositionsBasic() throws Throwable {
        adapterPositionsTest(null);
    }

    @Test
    public void adapterPositionsRemoveItems() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.deleteAndNotify(3, 4);
            }
        });
    }

    @Test
    public void adapterPositionsRemoveItemsBefore() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.deleteAndNotify(0, 1);
            }
        });
    }

    @Test
    public void adapterPositionsAddItemsBefore() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.addAndNotify(0, 5);
            }
        });
    }

    @Test
    public void adapterPositionsAddItemsInside() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.addAndNotify(3, 2);
            }
        });
    }

    @Test
    public void adapterPositionsMoveItems() throws Throwable {
        adapterPositionsTest(new AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                adapter.moveAndNotify(3, 5);
            }
        });
    }

    @Test
    public void adapterPositionsNotifyDataSetChanged() throws Throwable {
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

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN) // transientState is API 16
    @Test
    public void avoidLeakingRecyclerViewIfViewIsNotRecycled() throws Throwable {
        final AtomicBoolean failedToRecycle = new AtomicBoolean(false);
        final AtomicInteger recycledViewCount = new AtomicInteger(0);
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
                    @NonNull TestViewHolder holder) {
                failedToRecycle.set(true);
                return false;
            }

            @Override
            public void onViewRecycled(@NonNull TestViewHolder holder) {
                recycledViewCount.incrementAndGet();
                super.onViewRecycled(holder);
            }
        };
        rv.setAdapter(adapter);
        rv.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(rv);
        tlm.waitForLayout(1);
        final RecyclerView.ViewHolder vh = rv.getChildViewHolder(rv.getChildAt(0));
        mActivityRule.runOnUiThread(new Runnable() {
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
        assertThat(recycledViewCount.get(), is(9));
        assertTrue(failedToRecycle.get());
        assertNull(vh.mOwnerRecyclerView);
        checkForMainThreadException();
    }

    @Test
    public void avoidLeakingRecyclerViewViaViewHolder() throws Throwable {
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

    @Test
    public void duplicateAdapterPositionTest() throws Throwable {
        final TestAdapter testAdapter = new TestAdapter(10);
        final TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                if (!state.isPreLayout()) {
                    while (!recycler.getScrapList().isEmpty()) {
                        RecyclerView.ViewHolder viewHolder = recycler.getScrapList().get(0);
                        addDisappearingView(viewHolder.itemView, 0);
                    }
                }
                layoutLatch.countDown();
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        final DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setSupportsChangeAnimations(true);
        animator.setChangeDuration(10000);
        testAdapter.setHasStableIds(true);
        final TestRecyclerView recyclerView = new TestRecyclerView(getActivity());
        recyclerView.setLayoutManager(tlm);
        recyclerView.setAdapter(testAdapter);
        recyclerView.setItemAnimator(animator);

        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(2);

        tlm.expectLayouts(2);
        testAdapter.mItems.get(2).mType += 2;
        final int itemId = testAdapter.mItems.get(2).mId;
        testAdapter.changeAndNotify(2, 1);
        tlm.waitForLayout(2);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertThat("test sanity", recyclerView.getChildCount(), CoreMatchers.is(11));
                // now mangle the order and run the test
                RecyclerView.ViewHolder hidden = null;
                RecyclerView.ViewHolder updated = null;
                for (int i = 0; i < recyclerView.getChildCount(); i ++) {
                    View view = recyclerView.getChildAt(i);
                    RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(view);
                    if (vh.getAdapterPosition() == 2) {
                        if (mRecyclerView.mChildHelper.isHidden(view)) {
                            assertThat(hidden, CoreMatchers.nullValue());
                            hidden = vh;
                        } else {
                            assertThat(updated, CoreMatchers.nullValue());
                            updated = vh;
                        }
                    }
                }
                assertThat(hidden, CoreMatchers.notNullValue());
                assertThat(updated, CoreMatchers.notNullValue());

                mRecyclerView.startInterceptRequestLayout();

                // first put the hidden child back
                int index1 = mRecyclerView.indexOfChild(hidden.itemView);
                int index2 = mRecyclerView.indexOfChild(updated.itemView);
                if (index1 < index2) {
                    // swap views
                    swapViewsAtIndices(recyclerView, index1, index2);
                }
                assertThat(tlm.findViewByPosition(2), CoreMatchers.sameInstance(updated.itemView));

                assertThat(recyclerView.findViewHolderForAdapterPosition(2),
                        CoreMatchers.sameInstance(updated));
                assertThat(recyclerView.findViewHolderForLayoutPosition(2),
                        CoreMatchers.sameInstance(updated));
                assertThat(recyclerView.findViewHolderForItemId(itemId),
                        CoreMatchers.sameInstance(updated));

                // now swap back
                swapViewsAtIndices(recyclerView, index1, index2);

                assertThat(tlm.findViewByPosition(2), CoreMatchers.sameInstance(updated.itemView));
                assertThat(recyclerView.findViewHolderForAdapterPosition(2),
                        CoreMatchers.sameInstance(updated));
                assertThat(recyclerView.findViewHolderForLayoutPosition(2),
                        CoreMatchers.sameInstance(updated));
                assertThat(recyclerView.findViewHolderForItemId(itemId),
                        CoreMatchers.sameInstance(updated));

                // now remove updated. re-assert fallback to the hidden one
                tlm.removeView(updated.itemView);

                assertThat(tlm.findViewByPosition(2), CoreMatchers.nullValue());
                assertThat(recyclerView.findViewHolderForAdapterPosition(2),
                        CoreMatchers.sameInstance(hidden));
                assertThat(recyclerView.findViewHolderForLayoutPosition(2),
                        CoreMatchers.sameInstance(hidden));
                assertThat(recyclerView.findViewHolderForItemId(itemId),
                        CoreMatchers.sameInstance(hidden));
            }
        });

    }

    private void swapViewsAtIndices(TestRecyclerView recyclerView, int index1, int index2) {
        if (index1 == index2) {
            return;
        }
        if (index2 < index1) {
            int tmp = index1;
            index1 = index2;
            index2 = tmp;
        }
        final View v1 = recyclerView.getChildAt(index1);
        final View v2 = recyclerView.getChildAt(index2);
        boolean v1Hidden = recyclerView.mChildHelper.isHidden(v1);
        boolean v2Hidden = recyclerView.mChildHelper.isHidden(v2);
        // must un-hide before swap otherwise bucket indices will become invalid.
        if (v1Hidden) {
            mRecyclerView.mChildHelper.unhide(v1);
        }
        if (v2Hidden) {
            mRecyclerView.mChildHelper.unhide(v2);
        }
        recyclerView.detachViewFromParent(index2);
        recyclerView.attachViewToParent(v2, index1, v2.getLayoutParams());
        recyclerView.detachViewFromParent(index1 + 1);
        recyclerView.attachViewToParent(v1, index2, v1.getLayoutParams());

        if (v1Hidden) {
            mRecyclerView.mChildHelper.hide(v1);
        }
        if (v2Hidden) {
            mRecyclerView.mChildHelper.hide(v2);
        }
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final int count = recyclerView.getChildCount();
                    Map<View, Integer> layoutPositions = new HashMap<>();
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
    public void scrollStateForSmoothScroll() throws Throwable {
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
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void scrollStateForSmoothScrollWithStop() throws Throwable {
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
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollBy(0, 500);
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void scrollStateForFling() throws Throwable {
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
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        final ViewConfiguration vc = ViewConfiguration.get(getActivity());
        final float fling = vc.getScaledMinimumFlingVelocity()
                + (vc.getScaledMaximumFlingVelocity() - vc.getScaledMinimumFlingVelocity()) * .1f;
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void scrollStateForFlingWithStop() throws Throwable {
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
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                stateCnts[newState] = stateCnts[newState] + 1;
                latch.countDown();
            }
        });
        final ViewConfiguration vc = ViewConfiguration.get(getActivity());
        final float fling = vc.getScaledMinimumFlingVelocity()
                + (vc.getScaledMaximumFlingVelocity() - vc.getScaledMinimumFlingVelocity()) * .8f;
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.fling(0, Math.round(fling));
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void scrollStateDrag() throws Throwable {
        TestAdapter testAdapter = new TestAdapter(10);
        TestLayoutManager tlm = new TestLayoutManager();
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(testAdapter);
        recyclerView.setLayoutManager(tlm);
        setRecyclerView(recyclerView);
        getInstrumentation().waitForIdleSync();
        assertEquals(SCROLL_STATE_IDLE, recyclerView.getScrollState());
        final int[] stateCnts = new int[10];
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (view.onInterceptTouchEvent(event)) {
                    view.onTouchEvent(event);
                }
            }
        });
    }

    @Test
    public void recycleScrap() throws Throwable {
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
                ViewInfoStore infoStore = mRecyclerView.mViewInfoStore;
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
                        if (infoStore.mOldChangedHolders != null) {
                            for (int i = infoStore.mOldChangedHolders.size() - 1; i >= 0; i--) {
                                if (useRecycler) {
                                    recycler.recycleView(
                                            infoStore.mOldChangedHolders.valueAt(i).itemView);
                                } else {
                                    removeAndRecycleView(
                                            infoStore.mOldChangedHolders.valueAt(i).itemView,
                                            recycler);
                                }
                            }
                        }
                        assertEquals("no scrap should be left over", 0, recycler.getScrapCount());
                        assertEquals("pre layout map should be empty", 0,
                                InfoStoreTrojan.sizeOfPreLayout(infoStore));
                        assertEquals("post layout map should be empty", 0,
                                InfoStoreTrojan.sizeOfPostLayout(infoStore));
                        if (infoStore.mOldChangedHolders != null) {
                            assertEquals("post old change map should be empty", 0,
                                    infoStore.mOldChangedHolders.size());
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
        ((SimpleItemAnimator)recyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
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
    public void aAccessRecyclerOnOnMeasureWithPredictive() throws Throwable {
        accessRecyclerOnOnMeasureTest(true);
    }

    @Test
    public void accessRecyclerOnOnMeasureWithoutPredictive() throws Throwable {
        accessRecyclerOnOnMeasureTest(false);
    }

    @Test
    public void smoothScrollWithRemovedItemsAndRemoveItem() throws Throwable {
        smoothScrollTest(true);
    }

    @Test
    public void smoothScrollWithRemovedItems() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
            mActivityRule.runOnUiThread(new Runnable() {
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
    public void smoothScrollToPosition_targetNotFoundSeekInXAndY_scrollsLayoutManagerBy1InXAndY()
            throws Throwable {
        smoothScrollToPosition_initialScroll(
                15,
                10,
                9432,
                1239,
                1,
                1,
                2);
    }

    @Test
    public void smoothScrollToPosition_targetNotFoundSeekNegative_scrollsLayoutManagerByMinus1()
            throws Throwable {
        smoothScrollToPosition_initialScroll(
                15,
                10,
                -9432,
                -1239,
                -1,
                -1,
                2);
    }

    @Test
    public void smoothScrollToPosition_targetNotFoundSeekInX_scrollsLayoutManagerBy1InX()
            throws Throwable {
        smoothScrollToPosition_initialScroll(
                15,
                10,
                0,
                1239,
                0,
                1,
                1);
    }

    @Test
    public void smoothScrollToPosition_targetNotFoundSeekInY_scrollsLayoutManagerBy1InY()
            throws Throwable {
        smoothScrollToPosition_initialScroll(
                15,
                10,
                0,
                1239,
                0,
                1,
                1);
    }

    @Test
    public void smoothScrollToPosition_targetFound_doesNotScrollLayoutManager()
            throws Throwable {
        smoothScrollToPosition_initialScroll(
                5,
                10,
                9432,
                1239,
                0,
                0,
                1);
    }

    @SuppressWarnings("SameParameterValue")
    private void smoothScrollToPosition_initialScroll(
            final int targetItemPosition,
            final int itemLayoutCount,
            final int dxIncrement,
            final int dyIncrement,
            final int expectedInitialScrollDx,
            final int expectedInitialScrollDy,
            final int eventCount)
            throws Throwable {
        final RecyclerView rv = new RecyclerView(getActivity());
        rv.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        TestAdapter testAdapter = new TestAdapter(itemLayoutCount * 2);
        rv.setAdapter(testAdapter);

        final CountDownLatch countDownLatch = new CountDownLatch(eventCount);
        final AtomicInteger actualInitialScrollDx = new AtomicInteger(0);
        final AtomicInteger actualInitialScrollDy = new AtomicInteger(0);

        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                    int position) {
                RecyclerView.SmoothScroller scroller = new RecyclerView.SmoothScroller() {
                    @Override
                    protected void onStart() {

                    }

                    @Override
                    protected void onStop() {

                    }

                    @Override
                    protected void onSeekTargetStep(int dx, int dy, RecyclerView.State state,
                            Action action) {
                    }

                    @Override
                    protected void onTargetFound(View targetView, RecyclerView.State state,
                            Action action) {
                        countDownLatch.countDown();
                    }

                    @Nullable
                    @Override
                    public PointF computeScrollVectorForPosition(int targetPosition) {
                        return new PointF(dxIncrement, dyIncrement);
                    }
                };
                scroller.setTargetPosition(position);
                startSmoothScroll(scroller);
            }

            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, itemLayoutCount);
            }

            @Override
            public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                actualInitialScrollDx.set(dx);
                countDownLatch.countDown();
                return 0;
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                actualInitialScrollDy.set(dy);
                countDownLatch.countDown();
                return 0;
            }
        };

        rv.setLayoutManager(testLayoutManager);

        getActivity().getContainer().expectLayouts(1);
        setRecyclerView(rv);
        getActivity().getContainer().waitForLayout(2);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.smoothScrollToPosition(targetItemPosition);
            }
        });
        assertTrue(countDownLatch.await(2, TimeUnit.SECONDS));

        assertThat(actualInitialScrollDx.get(), equalTo(expectedInitialScrollDx));
        assertThat(actualInitialScrollDy.get(), equalTo(expectedInitialScrollDy));
    }

    @Test
    public void consecutiveSmoothScroll() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rv.smoothScrollBy(0, 2000);
            }
        });
        Thread.sleep(250);
        final AtomicInteger scrollAmt = new AtomicInteger();
        mActivityRule.runOnUiThread(new Runnable() {
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
                    if (!state.isPreLayout()) {
                        assertEquals(state.toString(),
                                expectedOnMeasureStateCount.get(), state.getItemCount());
                    }
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
    public void setCompatibleAdapter() throws Throwable {
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
            public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
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
    public void setIncompatibleAdapter() throws Throwable {
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
    public void recycleIgnored() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void findIgnoredByPosition() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void itemDecorsWithPredictive() throws Throwable {
        LayoutAllLayoutManager lm = new LayoutAllLayoutManager(true);
        lm.setSupportsPredictive(true);
        final Object changePayload = new Object();
        final TestAdapter adapter = new TestAdapter(10) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position, List<Object> payloads) {
                super.onBindViewHolder(holder, position);
                holder.setData(payloads.isEmpty() ? null : payloads.get(0));
            }
        };
        final Map<Integer, Object> preLayoutData = new HashMap<>();
        final Map<Integer, Object> postLayoutData = new HashMap<>();

        final RecyclerView.ItemDecoration decoration = new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                    RecyclerView.State state) {
                try {
                    TestViewHolder tvh = (TestViewHolder) parent.getChildViewHolder(view);
                    Object data = tvh.getData();
                    int adapterPos = tvh.getAdapterPosition();
                    assertThat(adapterPos, is(not(NO_POSITION)));
                    if (state.isPreLayout()) {
                        preLayoutData.put(adapterPos, data);
                    } else {
                        postLayoutData.put(adapterPos, data);
                    }
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }

            }
        };
        RecyclerView rv = new RecyclerView(getActivity());
        rv.addItemDecoration(decoration);
        rv.setAdapter(adapter);
        rv.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(rv);
        lm.waitForLayout(2);

        preLayoutData.clear();
        postLayoutData.clear();
        lm.expectLayouts(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyItemChanged(3, changePayload);
            }
        });
        lm.waitForLayout(2);
        assertThat(preLayoutData.containsKey(3), is(false));
        assertThat(postLayoutData.get(3), is(changePayload));
        assertThat(preLayoutData.size(), is(0));
        assertThat(postLayoutData.size(), is(1));
        checkForMainThreadException();
    }

    @Test
    public void invalidateAllDecorOffsets() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.addItemDecoration(itemDecoration);
            }
        });
    }

    public void removeItemDecoration(final RecyclerView recyclerView, final
    RecyclerView.ItemDecoration itemDecoration) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.removeItemDecoration(itemDecoration);
            }
        });
    }

    public void invalidateDecorOffsets(final RecyclerView recyclerView) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.invalidateItemDecorations();
            }
        });
    }

    @Test
    public void invalidateDecorOffsets() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        adapter.setHasStableIds(true);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);

        final Map<Long, Boolean> changes = new HashMap<>();

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
                                    "Decor insets validation for VH should have expected value.",
                                    changes.get(vh.getItemId()), lp.mInsetsDirty);
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
        for (int changedItem : changedItems) {
            changes.put(mRecyclerView.findViewHolderForLayoutPosition(changedItem).getItemId(),
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
    public void movingViaStableIds() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void adapterChangeDuringLayout() throws Throwable {
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
        setIgnoreMainThreadException(true);
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
                getMainThreadException() instanceof IllegalStateException);
    }

    @Test
    public void adapterChangeDuringScroll() throws Throwable {
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
        setIgnoreMainThreadException(true);
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
                getMainThreadException() instanceof IllegalStateException);
    }

    @Test
    public void recycleOnDetach() throws Throwable {
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
    public void updatesWhileDetached() throws Throwable {
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
    public void updatesAfterDetach() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void notifyDataSetChangedWithStableIds() throws Throwable {
        final Map<Integer, Integer> oldPositionToNewPositionMapping = new HashMap<>();
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public long getItemId(int position) {
                return mItems.get(position).mId;
            }
        };
        adapter.setHasStableIds(true);
        final ArrayList<Item> previousItems = new ArrayList<>();
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void callbacksDuringAdapterSwap() throws Throwable {
        callbacksDuringAdapterChange(true);
    }

    @Test
    public void callbacksDuringAdapterSet() throws Throwable {
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
            public void onViewRecycled(@NonNull TestViewHolder2 holder) {
                assertSame("on recycled should be called w/ the creator adapter", this,
                        holder.mData);
                super.onViewRecycled(holder);
            }

            @Override
            public void onBindViewHolder(@NonNull TestViewHolder2 holder, int position) {
                super.onBindViewHolder(holder, position);
                assertSame("on bind should be called w/ the creator adapter", this, holder.mData);
            }

            @Override
            public TestViewHolder2 onCreateViewHolder(@NonNull ViewGroup parent,
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
            public void onViewRecycled(@NonNull TestViewHolder2 holder) {
                assertSame("on recycled should be called w/ the creator adapter", this,
                        holder.mData);
                holder.mData = null;
                super.onViewRecycled(holder);
            }

            @Override
            public void onBindViewHolder(@NonNull TestViewHolder2 holder, int position) {
                super.onBindViewHolder(holder, position);
                holder.mData = this;
            }
        };
    }

    @Test
    public void findViewById() throws Throwable {
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
    public void typeForCache() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void typeForExistingViews() throws Throwable {
        final AtomicInteger viewType = new AtomicInteger(1);
        final int invalidatedCount = 2;
        final int layoutStart = 2;
        final TestAdapter adapter = new TestAdapter(100) {
            @Override
            public int getItemViewType(int position) {
                return viewType.get();
            }

            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
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
    public void state() throws Throwable {
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(recyclerView);
            }
        });
        testLayoutManager.waitForLayout(2);

        assertEquals("item count in state should be correct", adapter.getItemCount()
                , itemCount.get());
        assertEquals("structure changed should be true for first layout", true,
                structureChanged.get());
        Thread.sleep(1000); //wait for other layouts.
        testLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void detachWithoutLayoutManager() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        mActivityRule.runOnUiThread(new Runnable() {
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
    public void updateHiddenView() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int[] preLayoutRange = new int[]{0, 10};
        final int[] postLayoutRange = new int[]{0, 10};
        final AtomicBoolean enableGetViewTest = new AtomicBoolean(false);
        final List<Integer> disappearingPositions = new ArrayList<>();
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
                            assertNotNull(view);
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
        recyclerView.getItemAnimator().setMoveDuration(4000);
        recyclerView.getItemAnimator().setRemoveDuration(4000);
        final TestAdapter adapter = new TestAdapter(100);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(tlm);
        tlm.expectLayouts(1);
        setRecyclerView(recyclerView);
        tlm.waitForLayout(1);
        checkForMainThreadException();
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    assertThat("test sanity, should still be animating",
                            mRecyclerView.isAnimating(), CoreMatchers.is(true));
                    adapter.changeAndNotify(0, 1);
                    adapter.deleteAndNotify(0, 1);
                } catch (Throwable throwable) {
                    fail(throwable.getMessage());
                }
            }
        });
        tlm.waitForLayout(2);
        checkForMainThreadException();
    }

    @Test
    public void focusBigViewOnTop() throws Throwable {
        focusTooBigViewTest(Gravity.TOP);
    }

    @Test
    public void focusBigViewOnLeft() throws Throwable {
        focusTooBigViewTest(Gravity.LEFT);
    }

    @Test
    public void focusBigViewOnRight() throws Throwable {
        focusTooBigViewTest(Gravity.RIGHT);
    }

    @Test
    public void focusBigViewOnBottom() throws Throwable {
        focusTooBigViewTest(Gravity.BOTTOM);
    }

    @Test
    public void focusBigViewOnLeftRTL() throws Throwable {
        focusTooBigViewTest(Gravity.LEFT, true);
        assertEquals("test sanity", ViewCompat.LAYOUT_DIRECTION_RTL,
                mRecyclerView.getLayoutManager().getLayoutDirection());
    }

    @Test
    public void focusBigViewOnRightRTL() throws Throwable {
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
        assertTrue("test sanity", requestFocus(view, true));
        assertTrue("test sanity", view.hasFocus());
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
    public void firstLayoutWithAdapterChanges() throws Throwable {
        final TestAdapter adapter = new TestAdapter(0);
        final RecyclerView rv = new RecyclerView(getActivity());
        setVisibility(rv, View.GONE);
        TestLayoutManager tlm = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                    layoutRange(recycler, 0, state.getItemCount());
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                } finally {
                    layoutLatch.countDown();
                }
            }

            @Override
            public boolean supportsPredictiveItemAnimations() {
                return true;
            }
        };
        rv.setLayoutManager(tlm);
        rv.setAdapter(adapter);
        rv.setHasFixedSize(true);
        setRecyclerView(rv);
        tlm.expectLayouts(1);
        tlm.assertNoLayout("test sanity, layout should not run", 1);
        getInstrumentation().waitForIdleSync();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    adapter.addAndNotify(2);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                rv.setVisibility(View.VISIBLE);
            }
        });
        checkForMainThreadException();
        tlm.waitForLayout(2);
        assertEquals(2, rv.getChildCount());
        checkForMainThreadException();
    }

    @Test
    public void computeScrollOffsetWithoutLayoutManager() throws Throwable {
        RecyclerView rv = new RecyclerView(getActivity());
        rv.setAdapter(new TestAdapter(10));
        setRecyclerView(rv);
        assertEquals(0, rv.computeHorizontalScrollExtent());
        assertEquals(0, rv.computeHorizontalScrollOffset());
        assertEquals(0, rv.computeHorizontalScrollRange());

        assertEquals(0, rv.computeVerticalScrollExtent());
        assertEquals(0, rv.computeVerticalScrollOffset());
        assertEquals(0, rv.computeVerticalScrollRange());
    }

    @Test
    public void computeScrollOffsetWithoutAdapter() throws Throwable {
        RecyclerView rv = new RecyclerView(getActivity());
        rv.setLayoutManager(new TestLayoutManager());
        setRecyclerView(rv);
        assertEquals(0, rv.computeHorizontalScrollExtent());
        assertEquals(0, rv.computeHorizontalScrollOffset());
        assertEquals(0, rv.computeHorizontalScrollRange());

        assertEquals(0, rv.computeVerticalScrollExtent());
        assertEquals(0, rv.computeVerticalScrollOffset());
        assertEquals(0, rv.computeVerticalScrollRange());
    }

    @Test
    public void focusRectOnScreenWithDecorOffsets() throws Throwable {
        focusRectOnScreenTest(true);
    }

    @Test
    public void focusRectOnScreenWithout() throws Throwable {
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
                view.layout(0, -20, view.getMeasuredWidth(),
                        -20 + view.getMeasuredHeight()); // ignore decors on purpose
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
        requestFocus(view, true);
        assertEquals(addItemDecors ? -30 : -20, scrollDist.get());
    }

    @Test
    public void unimplementedSmoothScroll() throws Throwable {
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
        smoothScrollToPosition(35, false);
        assertEquals("smoothScrollToPosition should be ignored when frozen",
                -1, receivedSmoothScrollToPosition.get());
        freezeLayout(false);
        smoothScrollToPosition(35, false);
        assertTrue("both scrolls should be called", cbLatch.await(3, TimeUnit.SECONDS));
        checkForMainThreadException();
        assertEquals(35, receivedSmoothScrollToPosition.get());
        assertEquals(35, receivedScrollToPosition.get());
    }

    @Test
    public void jumpingJackSmoothScroller() throws Throwable {
        jumpingJackSmoothScrollerTest(true);
    }

    @Test
    public void jumpingJackSmoothScrollerGoesIdle() throws Throwable {
        jumpingJackSmoothScrollerTest(false);
    }

    @Test
    public void testScrollByBeforeFirstLayout() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        TestAdapter adapter = new TestAdapter(10);
        recyclerView.setLayoutManager(new TestLayoutManager() {
            AtomicBoolean didLayout = new AtomicBoolean(false);
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                didLayout.set(true);
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                assertThat("should run layout before scroll",
                        didLayout.get(), CoreMatchers.is(true));
                return super.scrollVerticallyBy(dy, recycler, state);
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        recyclerView.setAdapter(adapter);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setRecyclerView(recyclerView);
                    recyclerView.scrollBy(10, 19);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });

        checkForMainThreadException();
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

        mActivityRule.runOnUiThread(new Runnable() {
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
            mItems = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                mItems.add(new Item(i, "Item " + i));
            }
        }

        @Override
        public TestViewHolder2 onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            return new TestViewHolder2(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder2 holder, int position) {
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.mText + "(" + item.mAdapterIndex + ")");
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    public interface AdapterRunnable {

        void run(TestAdapter adapter) throws Throwable;
    }

    public class LayoutAllLayoutManager extends TestLayoutManager {
        private final boolean mAllowNullLayoutLatch;
        public int onItemsChangedCallCount = 0;
        public int onAdapterChagnedCallCount = 0;

        public LayoutAllLayoutManager() {
            // by default, we don't allow unexpected layouts.
            this(false);
        }
        public LayoutAllLayoutManager(boolean allowNullLayoutLatch) {
            mAllowNullLayoutLatch = allowNullLayoutLatch;
        }

        @Override
        public void onItemsChanged(RecyclerView recyclerView) {
            super.onItemsChanged(recyclerView);
            onItemsChangedCallCount++;
        }

        @Override
        public void onAdapterChanged(RecyclerView.Adapter oldAdapter,
                RecyclerView.Adapter newAdapter) {
            super.onAdapterChanged(oldAdapter, newAdapter);
            onAdapterChagnedCallCount++;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            if (!mAllowNullLayoutLatch || layoutLatch != null) {
                layoutLatch.countDown();
            }
        }
    }

    /**
     * Proxy class to make protected methods public
     */
    public static class TestRecyclerView extends RecyclerView {

        public TestRecyclerView(Context context) {
            super(context);
        }

        public TestRecyclerView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public TestRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void detachViewFromParent(int index) {
            super.detachViewFromParent(index);
        }

        @Override
        public void attachViewToParent(View child, int index, ViewGroup.LayoutParams params) {
            super.attachViewToParent(child, index, params);
        }
    }

    private interface ViewRunnable {
        void run(View view) throws RuntimeException;
    }

    public static class FullyConsumingNestedScroller extends NestedScrollingParent2Adapter {
        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target,
                @ViewCompat.ScrollAxis int axes, @ViewCompat.NestedScrollType int type) {
            // Always start regardless of type
            return true;
        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy,
                @NonNull int[] consumed, @ViewCompat.NestedScrollType int type) {
            // Consume everything!
            consumed[0] = dx;
            consumed[1] = dy;
        }

        @Override
        public int getNestedScrollAxes() {
            return ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;
        }

        @Override
        public void onStopNestedScroll(View target) {
            super.onStopNestedScroll(target);
        }

        @Override
        public void onStopNestedScroll(@NonNull View target,
                @ViewCompat.NestedScrollType int type) {
            super.onStopNestedScroll(target, type);
        }
    }

    @Test
    public void testRemainingScrollInLayout() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final TestAdapter adapter = new TestAdapter(100);

        final CountDownLatch firstScrollDone = new CountDownLatch(1);
        final CountDownLatch scrollFinished = new CountDownLatch(1);
        final int[] totalScrollDistance = new int[] {0};
        recyclerView.setLayoutManager(new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (firstScrollDone.getCount() < 1 && scrollFinished.getCount() == 1) {
                    try {
                        assertTrue("layout pass has remaining scroll",
                                state.getRemainingScrollVertical() != 0);
                        assertEquals("layout pass has remaining scroll",
                                1000 - totalScrollDistance[0], state.getRemainingScrollVertical());
                    } catch (Throwable throwable) {
                        postExceptionToInstrumentation(throwable);
                    }
                }
                super.onLayoutChildren(recycler, state);
            }

            @Override
            public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                    RecyclerView.State state) {
                firstScrollDone.countDown();
                totalScrollDistance[0] += dy;
                if (state.getRemainingScrollVertical() == 0) {
                    // the last scroll pass will have remaining 0
                    scrollFinished.countDown();
                }
                return super.scrollVerticallyBy(dy, recycler, state);
            }

            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        recyclerView.setAdapter(adapter);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    setRecyclerView(recyclerView);
                    recyclerView.smoothScrollBy(0, 1000);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });

        firstScrollDone.await(1, TimeUnit.SECONDS);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    recyclerView.requestLayout();
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });
        waitForIdleScroll(recyclerView);
        assertTrue(scrollFinished.getCount() < 1);
        assertEquals(totalScrollDistance[0], 1000);
    }

}
