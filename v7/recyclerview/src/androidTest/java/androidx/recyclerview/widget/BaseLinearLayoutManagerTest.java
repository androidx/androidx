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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseLinearLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    protected static final boolean DEBUG = false;
    protected static final String TAG = "LinearLayoutManagerTest";

    protected static List<Config> createBaseVariations() {
        List<Config> variations = new ArrayList<>();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean stackFromBottom : new boolean[]{false, true}) {
                    for (boolean wrap : new boolean[]{false, true}) {
                        variations.add(
                                new Config(orientation, reverseLayout, stackFromBottom).wrap(wrap));
                    }

                }
            }
        }
        return variations;
    }

    WrappedLinearLayoutManager mLayoutManager;
    TestAdapter mTestAdapter;

    protected static List<Config> addConfigVariation(List<Config> base, String fieldName,
            Object... variations)
            throws CloneNotSupportedException, NoSuchFieldException, IllegalAccessException {
        List<Config> newConfigs = new ArrayList<Config>();
        Field field = Config.class.getDeclaredField(fieldName);
        for (Config config : base) {
            for (Object variation : variations) {
                Config newConfig = (Config) config.clone();
                field.set(newConfig, variation);
                newConfigs.add(newConfig);
            }
        }
        return newConfigs;
    }

    void setupByConfig(Config config, boolean waitForFirstLayout) throws Throwable {
        setupByConfig(config, waitForFirstLayout, null, null);
    }

    void setupByConfig(Config config, boolean waitForFirstLayout,
        @Nullable RecyclerView.LayoutParams childLayoutParams,
        @Nullable RecyclerView.LayoutParams parentLayoutParams) throws Throwable {
        mRecyclerView = inflateWrappedRV();

        mRecyclerView.setHasFixedSize(true);
        mTestAdapter = config.mTestAdapter == null
                ? new TestAdapter(config.mItemCount, childLayoutParams)
                : config.mTestAdapter;
        mRecyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), config.mOrientation,
            config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        mLayoutManager.setRecycleChildrenOnDetach(config.mRecycleChildrenOnDetach);
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (config.mWrap) {
            mRecyclerView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            config.mOrientation == HORIZONTAL ? WRAP_CONTENT : MATCH_PARENT,
                            config.mOrientation == VERTICAL ? WRAP_CONTENT : MATCH_PARENT
                    )
            );
        }
        if (parentLayoutParams != null) {
            mRecyclerView.setLayoutParams(parentLayoutParams);
        }

        if (waitForFirstLayout) {
            waitForFirstLayout();
        }
    }

    public void scrollToPositionWithPredictive(final int scrollPosition, final int scrollOffset)
            throws Throwable {
        setupByConfig(new Config(VERTICAL, false, false), true);

        mLayoutManager.mOnLayoutListener = new OnLayoutListener() {
            @Override
            void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
                if (state.isPreLayout()) {
                    assertEquals("pending scroll position should still be pending",
                            scrollPosition, mLayoutManager.mPendingScrollPosition);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("pending scroll position offset should still be pending",
                                scrollOffset, mLayoutManager.mPendingScrollPositionOffset);
                    }
                } else {
                    RecyclerView.ViewHolder vh =
                            mRecyclerView.findViewHolderForLayoutPosition(scrollPosition);
                    assertNotNull("scroll to position should work", vh);
                    if (scrollOffset != LinearLayoutManager.INVALID_OFFSET) {
                        assertEquals("scroll offset should be applied properly",
                                mLayoutManager.getPaddingTop() + scrollOffset +
                                        ((RecyclerView.LayoutParams) vh.itemView
                                                .getLayoutParams()).topMargin,
                                mLayoutManager.getDecoratedTop(vh.itemView));
                    }
                }
            }
        };
        mLayoutManager.expectLayouts(2);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mTestAdapter.addAndNotify(0, 1);
                    if (scrollOffset == LinearLayoutManager.INVALID_OFFSET) {
                        mLayoutManager.scrollToPosition(scrollPosition);
                    } else {
                        mLayoutManager.scrollToPositionWithOffset(scrollPosition,
                                scrollOffset);
                    }

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        });
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
    }

    protected void waitForFirstLayout() throws Throwable {
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
    }

    void scrollToPositionWithOffset(final int position, final int offset) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    public void assertRectSetsNotEqual(String message, Map<Item, Rect> before,
            Map<Item, Rect> after, boolean strictItemEquality) {
        Throwable throwable = null;
        try {
            assertRectSetsEqual("NOT " + message, before, after, strictItemEquality);
        } catch (Throwable t) {
            throwable = t;
        }
        assertNotNull(message + "\ntwo layout should be different", throwable);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after) {
        assertRectSetsEqual(message, before, after, true);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after,
            boolean strictItemEquality) {
        StringBuilder sb = new StringBuilder();
        sb.append("checking rectangle equality.\n");
        sb.append("before:\n");
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            sb.append(entry.getKey().mAdapterIndex + ":" + entry.getValue()).append("\n");
        }
        sb.append("after:\n");
        for (Map.Entry<Item, Rect> entry : after.entrySet()) {
            sb.append(entry.getKey().mAdapterIndex + ":" + entry.getValue()).append("\n");
        }
        message = message + "\n" + sb.toString();
        assertEquals(message + ":\nitem counts should be equal", before.size()
                , after.size());
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            final Item beforeItem = entry.getKey();
            Rect afterRect = null;
            if (strictItemEquality) {
                afterRect = after.get(beforeItem);
                assertNotNull(message + ":\nSame item should be visible after simple re-layout",
                        afterRect);
            } else {
                for (Map.Entry<Item, Rect> afterEntry : after.entrySet()) {
                    final Item afterItem = afterEntry.getKey();
                    if (afterItem.mAdapterIndex == beforeItem.mAdapterIndex) {
                        afterRect = afterEntry.getValue();
                        break;
                    }
                }
                assertNotNull(message + ":\nItem with same adapter index should be visible " +
                                "after simple re-layout",
                        afterRect);
            }
            assertEquals(message + ":\nItem should be laid out at the same coordinates",
                    entry.getValue(), afterRect);
        }
    }

    static class VisibleChildren {

        int firstVisiblePosition = RecyclerView.NO_POSITION;

        int firstFullyVisiblePosition = RecyclerView.NO_POSITION;

        int lastVisiblePosition = RecyclerView.NO_POSITION;

        int lastFullyVisiblePosition = RecyclerView.NO_POSITION;

        @Override
        public String toString() {
            return "VisibleChildren{" +
                    "firstVisiblePosition=" + firstVisiblePosition +
                    ", firstFullyVisiblePosition=" + firstFullyVisiblePosition +
                    ", lastVisiblePosition=" + lastVisiblePosition +
                    ", lastFullyVisiblePosition=" + lastFullyVisiblePosition +
                    '}';
        }
    }

    static class OnLayoutListener {

        void before(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }

        void after(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }
    }

    static class Config implements Cloneable {

        static final int DEFAULT_ITEM_COUNT = 250;

        boolean mStackFromEnd;

        int mOrientation = VERTICAL;

        boolean mReverseLayout = false;

        boolean mRecycleChildrenOnDetach = false;

        int mItemCount = DEFAULT_ITEM_COUNT;

        boolean mWrap = false;

        TestAdapter mTestAdapter;

        Config(int orientation, boolean reverseLayout, boolean stackFromEnd) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mStackFromEnd = stackFromEnd;
        }

        public Config() {

        }

        Config adapter(TestAdapter adapter) {
            mTestAdapter = adapter;
            return this;
        }

        Config recycleChildrenOnDetach(boolean recycleChildrenOnDetach) {
            mRecycleChildrenOnDetach = recycleChildrenOnDetach;
            return this;
        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        Config stackFromBottom(boolean stackFromBottom) {
            mStackFromEnd = stackFromBottom;
            return this;
        }

        Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }

        public Config itemCount(int itemCount) {
            mItemCount = itemCount;
            return this;
        }

        // required by convention
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        @Override
        public String toString() {
            return "Config{"
                    + "mStackFromEnd=" + mStackFromEnd
                    + ",mOrientation=" + mOrientation
                    + ",mReverseLayout=" + mReverseLayout
                    + ",mRecycleChildrenOnDetach=" + mRecycleChildrenOnDetach
                    + ",mItemCount=" + mItemCount
                    + ",wrap=" + mWrap
                    + '}';
        }

        public Config wrap(boolean wrap) {
            mWrap = wrap;
            return this;
        }
    }

    class WrappedLinearLayoutManager extends LinearLayoutManager {

        CountDownLatch layoutLatch;
        CountDownLatch snapLatch;
        CountDownLatch prefetchLatch;
        CountDownLatch callbackLatch;

        OrientationHelper mSecondaryOrientation;

        OnLayoutListener mOnLayoutListener;

        RecyclerView.OnScrollListener mCallbackListener = new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                callbackLatch.countDown();
                if (callbackLatch.getCount() == 0L) {
                    removeOnScrollListener(this);
                }
            }
        };

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void expectCallbacks(int count) throws Throwable {
            callbackLatch = new CountDownLatch(count);
            mRecyclerView.addOnScrollListener(mCallbackListener);
        }

        private void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
            mRecyclerView.removeOnScrollListener(listener);
        }

        public void waitForLayout(int seconds) throws Throwable {
            layoutLatch.await(seconds * (DEBUG ? 100 : 1), SECONDS);
            checkForMainThreadException();
            MatcherAssert.assertThat("all layouts should complete on time",
                    layoutLatch.getCount(), CoreMatchers.is(0L));
            // use a runnable to ensure RV layout is finished
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        public void assertNoCallbacks(String msg, long timeout) throws Throwable {
            callbackLatch.await(timeout, TimeUnit.SECONDS);
            long latchCount = callbackLatch.getCount();
            assertFalse(msg + " :" + latchCount, latchCount == 0);
            removeOnScrollListener(mCallbackListener);
        }

        public void expectPrefetch(int count) {
            prefetchLatch = new CountDownLatch(count);
        }

        public void waitForPrefetch(int seconds) throws Throwable {
            prefetchLatch.await(seconds * (DEBUG ? 100 : 1), SECONDS);
            checkForMainThreadException();
            MatcherAssert.assertThat("all prefetches should complete on time",
                    prefetchLatch.getCount(), CoreMatchers.is(0L));
            // use a runnable to ensure RV layout is finished
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                }
            });
        }

        public void expectIdleState(int count) {
            snapLatch = new CountDownLatch(count);
            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        snapLatch.countDown();
                        if (snapLatch.getCount() == 0L) {
                            mRecyclerView.removeOnScrollListener(this);
                        }
                    }
                }
            });
        }

        public void waitForSnap(int seconds) throws Throwable {
            snapLatch.await(seconds * (DEBUG ? 100 : 1), SECONDS);
            checkForMainThreadException();
            MatcherAssert.assertThat("all scrolling should complete on time",
                    snapLatch.getCount(), CoreMatchers.is(0L));
            // use a runnable to ensure RV layout is finished
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {}
            });
        }

        @Override
        public void setOrientation(int orientation) {
            super.setOrientation(orientation);
            mSecondaryOrientation = null;
        }

        @Override
        public void removeAndRecycleView(View child, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG, "recycling view " + mRecyclerView.getChildViewHolder(child));
            }
            super.removeAndRecycleView(child, recycler);
        }

        @Override
        public void removeAndRecycleViewAt(int index, RecyclerView.Recycler recycler) {
            if (DEBUG) {
                Log.d(TAG,
                        "recycling view at" + mRecyclerView.getChildViewHolder(getChildAt(index)));
            }
            super.removeAndRecycleViewAt(index, recycler);
        }

        @Override
        void ensureLayoutState() {
            super.ensureLayoutState();
            if (mSecondaryOrientation == null) {
                mSecondaryOrientation = OrientationHelper.createOrientationHelper(this,
                        1 - getOrientation());
            }
        }

        @Override
        LayoutState createLayoutState() {
            return new LayoutState() {
                @Override
                View next(RecyclerView.Recycler recycler) {
                    final boolean hadMore = hasMore(mRecyclerView.mState);
                    final int position = mCurrentPosition;
                    View next = super.next(recycler);
                    assertEquals("if has more, should return a view", hadMore, next != null);
                    assertEquals("position of the returned view must match current position",
                            position, RecyclerView.getChildViewHolderInt(next).getLayoutPosition());
                    return next;
                }
            };
        }

        public String getBoundsLog() {
            StringBuilder sb = new StringBuilder();
            sb.append("view bounds:[start:").append(mOrientationHelper.getStartAfterPadding())
                    .append(",").append(" end").append(mOrientationHelper.getEndAfterPadding());
            sb.append("\nchildren bounds\n");
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                sb.append("child (ind:").append(i).append(", pos:").append(getPosition(child))
                        .append("[").append("start:").append(
                        mOrientationHelper.getDecoratedStart(child)).append(", end:")
                        .append(mOrientationHelper.getDecoratedEnd(child)).append("]\n");
            }
            return sb.toString();
        }

        public void waitForAnimationsToEnd(int timeoutInSeconds) throws InterruptedException {
            RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
            if (itemAnimator == null) {
                return;
            }
            final CountDownLatch latch = new CountDownLatch(1);
            final boolean running = itemAnimator.isRunning(
                    new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                        @Override
                        public void onAnimationsFinished() {
                            latch.countDown();
                        }
                    }
            );
            if (running) {
                latch.await(timeoutInSeconds, TimeUnit.SECONDS);
            }
        }

        public VisibleChildren traverseAndFindVisibleChildren() {
            int childCount = getChildCount();
            final VisibleChildren visibleChildren = new VisibleChildren();
            final int start = mOrientationHelper.getStartAfterPadding();
            final int end = mOrientationHelper.getEndAfterPadding();
            for (int i = 0; i < childCount; i++) {
                View child = getChildAt(i);
                final int childStart = mOrientationHelper.getDecoratedStart(child);
                final int childEnd = mOrientationHelper.getDecoratedEnd(child);
                final boolean fullyVisible = childStart >= start && childEnd <= end;
                final boolean hidden = childEnd <= start || childStart >= end;
                if (hidden) {
                    continue;
                }
                final int position = getPosition(child);
                if (fullyVisible) {
                    if (position < visibleChildren.firstFullyVisiblePosition ||
                            visibleChildren.firstFullyVisiblePosition == RecyclerView.NO_POSITION) {
                        visibleChildren.firstFullyVisiblePosition = position;
                    }

                    if (position > visibleChildren.lastFullyVisiblePosition) {
                        visibleChildren.lastFullyVisiblePosition = position;
                    }
                }

                if (position < visibleChildren.firstVisiblePosition ||
                        visibleChildren.firstVisiblePosition == RecyclerView.NO_POSITION) {
                    visibleChildren.firstVisiblePosition = position;
                }

                if (position > visibleChildren.lastVisiblePosition) {
                    visibleChildren.lastVisiblePosition = position;
                }

            }
            return visibleChildren;
        }

        Rect getViewBounds(View view) {
            if (getOrientation() == HORIZONTAL) {
                return new Rect(
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedEnd(view),
                        mSecondaryOrientation.getDecoratedEnd(view));
            } else {
                return new Rect(
                        mSecondaryOrientation.getDecoratedStart(view),
                        mOrientationHelper.getDecoratedStart(view),
                        mSecondaryOrientation.getDecoratedEnd(view),
                        mOrientationHelper.getDecoratedEnd(view));
            }

        }

        Map<Item, Rect> collectChildCoordinates() throws Throwable {
            final Map<Item, Rect> items = new LinkedHashMap<Item, Rect>();
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    Rect layoutBounds = new Rect(0, 0,
                            mLayoutManager.getWidth(), mLayoutManager.getHeight());
                    for (int i = 0; i < childCount; i++) {
                        View child = getChildAt(i);
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child
                                .getLayoutParams();
                        TestViewHolder vh = (TestViewHolder) lp.mViewHolder;
                        Rect childBounds = getViewBounds(child);
                        if (new Rect(childBounds).intersect(layoutBounds)) {
                            items.put(vh.mBoundItem, childBounds);
                        }
                    }
                }
            });
            return items;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.before(recycler, state);
                }
                super.onLayoutChildren(recycler, state);
                if (mOnLayoutListener != null) {
                    mOnLayoutListener.after(recycler, state);
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            layoutLatch.countDown();
        }

        @Override
        public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                LayoutPrefetchRegistry layoutPrefetchRegistry) {
            if (prefetchLatch != null) prefetchLatch.countDown();
            super.collectAdjacentPrefetchPositions(dx, dy, state, layoutPrefetchRegistry);
        }
    }
}
