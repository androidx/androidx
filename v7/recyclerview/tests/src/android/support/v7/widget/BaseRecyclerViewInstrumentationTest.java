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

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.view.ViewCompat;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import android.support.v7.recyclerview.test.R;

abstract public class BaseRecyclerViewInstrumentationTest extends
        ActivityInstrumentationTestCase2<TestActivity> {

    private static final String TAG = "RecyclerViewTest";

    private boolean mDebug;

    protected RecyclerView mRecyclerView;

    protected AdapterHelper mAdapterHelper;

    Throwable mainThreadException;

    Thread mInstrumentationThread;

    public BaseRecyclerViewInstrumentationTest() {
        this(false);
    }

    public BaseRecyclerViewInstrumentationTest(boolean debug) {
        super("android.support.v7.recyclerview", TestActivity.class);
        mDebug = debug;
    }

    void checkForMainThreadException() throws Throwable {
        if (mainThreadException != null) {
            throw mainThreadException;
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mInstrumentationThread = Thread.currentThread();
    }

    void setHasTransientState(final View view, final boolean value) {
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewCompat.setHasTransientState(view, value);
                }
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
        }
    }

    protected void enableAccessibility()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getUIAutomation = Instrumentation.class.getMethod("getUiAutomation");
        getUIAutomation.invoke(getInstrumentation());
    }

    void setAdapter(final RecyclerView.Adapter adapter) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(adapter);
            }
        });
    }

    protected WrappedRecyclerView inflateWrappedRV() {
        return (WrappedRecyclerView)
                LayoutInflater.from(getActivity()).inflate(R.layout.wrapped_test_rv,
                        getRecyclerViewContainer(), false);
    }

    void swapAdapter(final RecyclerView.Adapter adapter,
            final boolean removeAndRecycleExistingViews) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mRecyclerView.swapAdapter(adapter, removeAndRecycleExistingViews);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        checkForMainThreadException();
    }

    void postExceptionToInstrumentation(Throwable t) {
        if (mInstrumentationThread == Thread.currentThread()) {
            throw new RuntimeException(t);
        }
        if (mainThreadException != null) {
            Log.e(TAG, "receiving another main thread exception. dropping.", t);
        } else {
            Log.e(TAG, "captured exception on main thread", t);
            mainThreadException = t;
        }

        if (mRecyclerView != null && mRecyclerView
                .getLayoutManager() instanceof TestLayoutManager) {
            TestLayoutManager lm = (TestLayoutManager) mRecyclerView.getLayoutManager();
            // finish all layouts so that we get the correct exception
            while (lm.layoutLatch.getCount() > 0) {
                lm.layoutLatch.countDown();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        if (mRecyclerView != null) {
            try {
                removeRecyclerView();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        getInstrumentation().waitForIdleSync();
        super.tearDown();

        try {
            checkForMainThreadException();
        } catch (Exception e) {
            throw e;
        } catch (Throwable throwable) {
            throw new Exception(Log.getStackTraceString(throwable));
        }
    }

    public Rect getDecoratedRecyclerViewBounds() {
        return new Rect(
                mRecyclerView.getPaddingLeft(),
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getWidth() - mRecyclerView.getPaddingRight(),
                mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom()
        );
    }

    public void removeRecyclerView() throws Throwable {
        if (mRecyclerView == null) {
            return;
        }
        if (!isMainThread()) {
            getInstrumentation().waitForIdleSync();
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                    if (adapter instanceof AttachDetachCountingAdapter) {
                        ((AttachDetachCountingAdapter) adapter).getCounter()
                                .validateRemaining(mRecyclerView);
                    }
                    getActivity().mContainer.removeAllViews();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        mRecyclerView = null;
    }

    void waitForAnimations(int seconds) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.mItemAnimator
                        .isRunning(new RecyclerView.ItemAnimator.ItemAnimatorFinishedListener() {
                            @Override
                            public void onAnimationsFinished() {
                                latch.countDown();
                            }
                        });
            }
        });

        assertTrue("animations didn't finish on expected time of " + seconds + " seconds",
                latch.await(seconds, TimeUnit.SECONDS));
    }

    public boolean requestFocus(final View view) {
        final boolean[] result = new boolean[1];
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = view.requestFocus();
            }
        });
        return result[0];
    }

    public void setRecyclerView(final RecyclerView recyclerView) throws Throwable {
        setRecyclerView(recyclerView, true);
    }
    public void setRecyclerView(final RecyclerView recyclerView, boolean assignDummyPool)
            throws Throwable {
        setRecyclerView(recyclerView, assignDummyPool, true);
    }
    public void setRecyclerView(final RecyclerView recyclerView, boolean assignDummyPool,
            boolean addPositionCheckItemAnimator)
            throws Throwable {
        mRecyclerView = recyclerView;
        if (assignDummyPool) {
            RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool() {
                @Override
                public RecyclerView.ViewHolder getRecycledView(int viewType) {
                    RecyclerView.ViewHolder viewHolder = super.getRecycledView(viewType);
                    if (viewHolder == null) {
                        return null;
                    }
                    viewHolder.addFlags(RecyclerView.ViewHolder.FLAG_BOUND);
                    viewHolder.mPosition = 200;
                    viewHolder.mOldPosition = 300;
                    viewHolder.mPreLayoutPosition = 500;
                    return viewHolder;
                }

                @Override
                public void putRecycledView(RecyclerView.ViewHolder scrap) {
                    assertNull(scrap.mOwnerRecyclerView);
                    super.putRecycledView(scrap);
                }
            };
            mRecyclerView.setRecycledViewPool(pool);
        }
        if (addPositionCheckItemAnimator) {
            mRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                        RecyclerView.State state) {
                    RecyclerView.ViewHolder vh = parent.getChildViewHolder(view);
                    if (!vh.isRemoved()) {
                        assertNotSame("If getItemOffsets is called, child should have a valid"
                                            + " adapter position unless it is removed : " + vh,
                                    vh.getAdapterPosition(), RecyclerView.NO_POSITION);
                    }
                }
            });
        }
        mAdapterHelper = recyclerView.mAdapterHelper;
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
    }

    protected FrameLayout getRecyclerViewContainer() {
        return getActivity().mContainer;
    }

    public void requestLayoutOnUIThread(final View view) {
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.requestLayout();
                }
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
        }
    }

    public void scrollBy(final int dt) {
        try {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mRecyclerView.getLayoutManager().canScrollHorizontally()) {
                        mRecyclerView.scrollBy(dt, 0);
                    } else {
                        mRecyclerView.scrollBy(0, dt);
                    }

                }
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
        }
    }

    void scrollToPosition(final int position) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(position);
            }
        });
    }

    void smoothScrollToPosition(final int position)
            throws Throwable {
        if (mDebug) {
            Log.d(TAG, "SMOOTH scrolling to " + position);
        }
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(position);
            }
        });
        getInstrumentation().waitForIdleSync();
        Thread.sleep(200); //give scroller some time so start
        while (mRecyclerView.getLayoutManager().isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            if (mDebug) {
                Log.d(TAG, "SMOOTH scrolling step");
            }
            Thread.sleep(200);
        }
        if (mDebug) {
            Log.d(TAG, "SMOOTH scrolling done");
        }
        getInstrumentation().waitForIdleSync();
    }

    void freezeLayout(final boolean freeze) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setLayoutFrozen(freeze);
            }
        });
    }

    class TestViewHolder extends RecyclerView.ViewHolder {

        Item mBoundItem;

        public TestViewHolder(View itemView) {
            super(itemView);
            itemView.setFocusable(true);
        }

        @Override
        public String toString() {
            return super.toString() + " item:" + mBoundItem;
        }
    }
    class DumbLayoutManager extends TestLayoutManager {
        ReentrantLock mLayoutLock = new ReentrantLock();
        public void blockLayout() {
            mLayoutLock.lock();
        }

        public void unblockLayout() {
            mLayoutLock.unlock();
        }
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            mLayoutLock.lock();
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            if (layoutLatch != null) {
                layoutLatch.countDown();
            }
            mLayoutLock.unlock();
        }
    }
    public class TestLayoutManager extends RecyclerView.LayoutManager {
        int mScrollVerticallyAmount;
        int mScrollHorizontallyAmount;
        protected CountDownLatch layoutLatch;

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout, TimeUnit timeUnit, boolean waitForIdle)
                throws Throwable {
            layoutLatch.await(timeout * (mDebug ? 100 : 1), timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
            if (waitForIdle) {
                getInstrumentation().waitForIdleSync();
            }
        }

        public void waitForLayout(long timeout, TimeUnit timeUnit)
                throws Throwable {
            waitForLayout(timeout, timeUnit, true);
        }

        public void assertLayoutCount(int count, String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertEquals(msg, count, layoutLatch.getCount());
        }

        public void assertNoLayout(String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertFalse(msg, layoutLatch.getCount() == 0);
        }

        public void waitForLayout(long timeout) throws Throwable {
            waitForLayout(timeout * (mDebug ? 10000 : 1), TimeUnit.SECONDS, true);
        }

        public void waitForLayout(long timeout, boolean waitForIdle) throws Throwable {
            waitForLayout(timeout * (mDebug ? 10000 : 1), TimeUnit.SECONDS, waitForIdle);
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        void assertVisibleItemPositions() {
            int i = getChildCount();
            TestAdapter testAdapter = (TestAdapter) mRecyclerView.getAdapter();
            while (i-- > 0) {
                View view = getChildAt(i);
                RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
                Item item = ((TestViewHolder) lp.mViewHolder).mBoundItem;
                if (mDebug) {
                    Log.d(TAG, "testing item " + i);
                }
                if (!lp.isItemRemoved()) {
                    RecyclerView.ViewHolder vh = mRecyclerView.getChildViewHolder(view);
                    assertSame("item position in LP should match adapter value :" + vh,
                            testAdapter.mItems.get(vh.mPosition), item);
                }
            }
        }

        RecyclerView.LayoutParams getLp(View v) {
            return (RecyclerView.LayoutParams) v.getLayoutParams();
        }

        protected void layoutRange(RecyclerView.Recycler recycler, int start, int end) {
            assertScrap(recycler);
            if (mDebug) {
                Log.d(TAG, "will layout items from " + start + " to " + end);
            }
            int diff = end > start ? 1 : -1;
            int top = 0;
            for (int i = start; i != end; i+=diff) {
                if (mDebug) {
                    Log.d(TAG, "laying out item " + i);
                }
                View view = recycler.getViewForPosition(i);
                assertNotNull("view should not be null for valid position. "
                        + "got null view at position " + i, view);
                if (!mRecyclerView.mState.isPreLayout()) {
                    RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view
                            .getLayoutParams();
                    assertFalse("In post layout, getViewForPosition should never return a view "
                            + "that is removed", layoutParams != null
                            && layoutParams.isItemRemoved());

                }
                assertEquals("getViewForPosition should return correct position",
                        i, getPosition(view));
                addView(view);

                measureChildWithMargins(view, 0, 0);
                if (getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL) {
                    layoutDecorated(view, getWidth() - getDecoratedMeasuredWidth(view), top,
                            getWidth(), top + getDecoratedMeasuredHeight(view));
                } else {
                    layoutDecorated(view, 0, top, getDecoratedMeasuredWidth(view)
                            , top + getDecoratedMeasuredHeight(view));
                }

                top += view.getMeasuredHeight();
            }
        }

        private void assertScrap(RecyclerView.Recycler recycler) {
            if (mRecyclerView.getAdapter() != null &&
                    !mRecyclerView.getAdapter().hasStableIds()) {
                for (RecyclerView.ViewHolder viewHolder : recycler.getScrapList()) {
                    assertFalse("Invalid scrap should be no kept", viewHolder.isInvalid());
                }
            }
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            mScrollHorizontallyAmount += dx;
            return dx;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            mScrollVerticallyAmount += dy;
            return dy;
        }
    }

    static class Item {
        final static AtomicInteger idCounter = new AtomicInteger(0);
        final public int mId = idCounter.incrementAndGet();

        int mAdapterIndex;

        final String mText;
        int mType = 0;

        Item(int adapterIndex, String text) {
            mAdapterIndex = adapterIndex;
            mText = text;
        }

        @Override
        public String toString() {
            return "Item{" +
                    "mId=" + mId +
                    ", originalIndex=" + mAdapterIndex +
                    ", text='" + mText + '\'' +
                    '}';
        }
    }

    public class TestAdapter extends RecyclerView.Adapter<TestViewHolder>
            implements AttachDetachCountingAdapter {

        public static final String DEFAULT_ITEM_PREFIX = "Item ";

        ViewAttachDetachCounter mAttachmentCounter = new ViewAttachDetachCounter();
        List<Item> mItems;

        public TestAdapter(int count) {
            mItems = new ArrayList<Item>(count);
            addItems(0, count, DEFAULT_ITEM_PREFIX);
        }

        private void addItems(int pos, int count, String prefix) {
            for (int i = 0; i < count; i++, pos++) {
                mItems.add(pos, new Item(pos, prefix));
            }
        }

        @Override
        public int getItemViewType(int position) {
            return getItemAt(position).mType;
        }

        @Override
        public void onViewAttachedToWindow(TestViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            mAttachmentCounter.onViewAttached(holder);
        }

        @Override
        public void onViewDetachedFromWindow(TestViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            mAttachmentCounter.onViewDetached(holder);
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            mAttachmentCounter.onAttached(recyclerView);
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mAttachmentCounter.onDetached(recyclerView);
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            assertNotNull(holder.mOwnerRecyclerView);
            assertEquals(position, holder.getAdapterPosition());
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.mText + "(" + item.mAdapterIndex + ")");
            holder.mBoundItem = item;
        }

        public Item getItemAt(int position) {
            return mItems.get(position);
        }

        @Override
        public void onViewRecycled(TestViewHolder holder) {
            super.onViewRecycled(holder);
            final int adapterPosition = holder.getAdapterPosition();
            final boolean shouldHavePosition = !holder.isRemoved() && holder.isBound() &&
                    !holder.isAdapterPositionUnknown() && !holder.isInvalid();
            String log = "Position check for " + holder.toString();
            assertEquals(log, shouldHavePosition, adapterPosition != RecyclerView.NO_POSITION);
            if (shouldHavePosition) {
                assertTrue(log, mItems.size() > adapterPosition);
                assertSame(log, holder.mBoundItem, mItems.get(adapterPosition));
            }
        }

        public void deleteAndNotify(final int start, final int count) throws Throwable {
            deleteAndNotify(new int[]{start, count});
        }

        /**
         * Deletes items in the given ranges.
         * <p>
         * Note that each operation affects the one after so you should offset them properly.
         * <p>
         * For example, if adapter has 5 items (A,B,C,D,E), and then you call this method with
         * <code>[1, 2],[2, 1]</code>, it will first delete items B,C and the new adapter will be
         * A D E. Then it will delete 2,1 which means it will delete E.
         */
        public void deleteAndNotify(final int[]... startCountTuples) throws Throwable {
            for (int[] tuple : startCountTuples) {
                tuple[1] = -tuple[1];
            }
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        @Override
        public long getItemId(int position) {
            return hasStableIds() ? mItems.get(position).mId : super.getItemId(position);
        }

        public void offsetOriginalIndices(int start, int offset) {
            for (int i = start; i < mItems.size(); i++) {
                mItems.get(i).mAdapterIndex += offset;
            }
        }

        /**
         * @param start inclusive
         * @param end exclusive
         * @param offset
         */
        public void offsetOriginalIndicesBetween(int start, int end, int offset) {
            for (int i = start; i < end && i < mItems.size(); i++) {
                mItems.get(i).mAdapterIndex += offset;
            }
        }

        public void addAndNotify(final int count) throws Throwable {
            assertEquals(0, mItems.size());
            new AddRemoveRunnable(DEFAULT_ITEM_PREFIX, new int[]{0, count}).runOnMainThread();
        }

        public void addAndNotify(final int start, final int count) throws Throwable {
            addAndNotify(new int[]{start, count});
        }

        public void addAndNotify(final int[]... startCountTuples) throws Throwable {
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        public void dispatchDataSetChanged() throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        public void changeAndNotify(final int start, final int count) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(start, count);
                }
            });
        }

        public void changeAndNotifyWithPayload(final int start, final int count,
                final Object payload) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(start, count, payload);
                }
            });
        }

        public void changePositionsAndNotify(final int... positions) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < positions.length; i += 1) {
                        TestAdapter.super.notifyItemRangeChanged(positions[i], 1);
                    }
                }
            });
        }

        /**
         * Similar to other methods but negative count means delete and position count means add.
         * <p>
         * For instance, calling this method with <code>[1,1], [2,-1]</code> it will first add an
         * item to index 1, then remove an item from index 2 (updated index 2)
         */
        public void addDeleteAndNotify(final int[]... startCountTuples) throws Throwable {
            new AddRemoveRunnable(startCountTuples).runOnMainThread();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        /**
         * Uses notifyDataSetChanged
         */
        public void moveItems(boolean notifyChange, int[]... fromToTuples) throws Throwable {
            for (int i = 0; i < fromToTuples.length; i += 1) {
                int[] tuple = fromToTuples[i];
                moveItem(tuple[0], tuple[1], false);
            }
            if (notifyChange) {
                dispatchDataSetChanged();
            }
        }

        /**
         * Uses notifyDataSetChanged
         */
        public void moveItem(final int from, final int to, final boolean notifyChange)
                throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    moveInUIThread(from, to);
                    if (notifyChange) {
                        notifyDataSetChanged();
                    }
                }
            });
        }

        /**
         * Uses notifyItemMoved
         */
        public void moveAndNotify(final int from, final int to) throws Throwable {
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    moveInUIThread(from, to);
                    notifyItemMoved(from, to);
                }
            });
        }

        public void clearOnUIThread() {
            assertEquals("clearOnUIThread called from a wrong thread",
                    Looper.getMainLooper(), Looper.myLooper());
            mItems = new ArrayList<Item>();
            notifyDataSetChanged();
        }

        protected void moveInUIThread(int from, int to) {
            Item item = mItems.remove(from);
            offsetOriginalIndices(from, -1);
            mItems.add(to, item);
            offsetOriginalIndices(to + 1, 1);
            item.mAdapterIndex = to;
        }


        @Override
        public ViewAttachDetachCounter getCounter() {
            return mAttachmentCounter;
        }


        private class AddRemoveRunnable implements Runnable {
            final String mNewItemPrefix;
            final int[][] mStartCountTuples;

            public AddRemoveRunnable(String newItemPrefix, int[]... startCountTuples) {
                mNewItemPrefix = newItemPrefix;
                mStartCountTuples = startCountTuples;
            }

            public AddRemoveRunnable(int[][] startCountTuples) {
                this("new item ", startCountTuples);
            }

            public void runOnMainThread() throws Throwable {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    run();
                } else {
                    runTestOnUiThread(this);
                }
            }

            @Override
            public void run() {
                for (int[] tuple : mStartCountTuples) {
                    if (tuple[1] < 0) {
                        delete(tuple);
                    } else {
                        add(tuple);
                    }
                }
            }

            private void add(int[] tuple) {
                // offset others
                offsetOriginalIndices(tuple[0], tuple[1]);
                addItems(tuple[0], tuple[1], mNewItemPrefix);
                notifyItemRangeInserted(tuple[0], tuple[1]);
            }

            private void delete(int[] tuple) {
                final int count = -tuple[1];
                offsetOriginalIndices(tuple[0] + count, tuple[1]);
                for (int i = 0; i < count; i++) {
                    mItems.remove(tuple[0]);
                }
                notifyItemRangeRemoved(tuple[0], count);
            }
        }
    }

    public boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    @Override
    public void runTestOnUiThread(Runnable r) throws Throwable {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            super.runTestOnUiThread(r);
        }
    }

    static class TargetTuple {

        final int mPosition;

        final int mLayoutDirection;

        TargetTuple(int position, int layoutDirection) {
            this.mPosition = position;
            this.mLayoutDirection = layoutDirection;
        }

        @Override
        public String toString() {
            return "TargetTuple{" +
                    "mPosition=" + mPosition +
                    ", mLayoutDirection=" + mLayoutDirection +
                    '}';
        }
    }

    public interface AttachDetachCountingAdapter {

        ViewAttachDetachCounter getCounter();
    }

    public class ViewAttachDetachCounter {

        Set<RecyclerView.ViewHolder> mAttachedSet = new HashSet<RecyclerView.ViewHolder>();

        public void validateRemaining(RecyclerView recyclerView) {
            final int childCount = recyclerView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(view);
                assertTrue("remaining view should be in attached set " + vh,
                        mAttachedSet.contains(vh));
            }
            assertEquals("there should not be any views left in attached set",
                    childCount, mAttachedSet.size());
        }

        public void onViewDetached(RecyclerView.ViewHolder viewHolder) {
            try {
                assertTrue("view holder should be in attached set",
                        mAttachedSet.remove(viewHolder));
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
        }

        public void onViewAttached(RecyclerView.ViewHolder viewHolder) {
            try {
                assertTrue("view holder should not be in attached set",
                        mAttachedSet.add(viewHolder));
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
        }

        public void onAttached(RecyclerView recyclerView) {
            // when a new RV is attached, clear the set and add all view holders
            mAttachedSet.clear();
            final int childCount = recyclerView.getChildCount();
            for (int i = 0; i < childCount; i ++) {
                View view = recyclerView.getChildAt(i);
                mAttachedSet.add(recyclerView.getChildViewHolder(view));
            }
        }

        public void onDetached(RecyclerView recyclerView) {
            validateRemaining(recyclerView);
        }
    }
}
