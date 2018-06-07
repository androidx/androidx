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

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Instrumentation;
import android.graphics.Rect;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.test.R;
import androidx.testutils.PollingCheck;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

abstract public class BaseRecyclerViewInstrumentationTest {

    private static final String TAG = "RecyclerViewTest";

    private boolean mDebug;

    protected RecyclerView mRecyclerView;

    protected AdapterHelper mAdapterHelper;

    private Throwable mMainThreadException;

    private boolean mIgnoreMainThreadException = false;

    Thread mInstrumentationThread;

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    public BaseRecyclerViewInstrumentationTest() {
        this(false);
    }

    public BaseRecyclerViewInstrumentationTest(boolean debug) {
        mDebug = debug;
    }

    void checkForMainThreadException() throws Throwable {
        if (!mIgnoreMainThreadException && mMainThreadException != null) {
            throw mMainThreadException;
        }
    }

    public void setIgnoreMainThreadException(boolean ignoreMainThreadException) {
        mIgnoreMainThreadException = ignoreMainThreadException;
    }

    public Throwable getMainThreadException() {
        return mMainThreadException;
    }

    protected TestActivity getActivity() {
        return mActivityRule.getActivity();
    }

    @Before
    public final void setUpInsThread() throws Exception {
        mInstrumentationThread = Thread.currentThread();
        Item.idCounter.set(0);
    }

    void setHasTransientState(final View view, final boolean value) {
        try {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ViewCompat.setHasTransientState(view, value);
                }
            });
        } catch (Throwable throwable) {
            Log.e(TAG, "", throwable);
        }
    }

    public boolean canReUseActivity() {
        return true;
    }

    protected void enableAccessibility()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method getUIAutomation = Instrumentation.class.getMethod("getUiAutomation");
        getUIAutomation.invoke(InstrumentationRegistry.getInstrumentation());
    }

    void setAdapter(final RecyclerView.Adapter adapter) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(adapter);
            }
        });
    }

    public View focusSearch(final View focused, final int direction) throws Throwable {
        return focusSearch(focused, direction, false);
    }

    public View focusSearch(final View focused, final int direction, boolean waitForScroll)
            throws Throwable {
        final View[] result = new View[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = focused.focusSearch(direction);
                if (view != null && view != focused) {
                    view.requestFocus();
                }
                result[0] = view;
            }
        });
        if (waitForScroll && (result[0] != null)) {
            waitForIdleScroll(mRecyclerView);
        }
        return result[0];
    }

    protected WrappedRecyclerView inflateWrappedRV() {
        return (WrappedRecyclerView)
                LayoutInflater.from(getActivity()).inflate(R.layout.wrapped_test_rv,
                        getRecyclerViewContainer(), false);
    }

    void swapAdapter(final RecyclerView.Adapter adapter,
            final boolean removeAndRecycleExistingViews) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
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
        if (mMainThreadException != null) {
            Log.e(TAG, "receiving another main thread exception. dropping.", t);
        } else {
            Log.e(TAG, "captured exception on main thread", t);
            mMainThreadException = t;
        }

        if (mRecyclerView != null && mRecyclerView
                .getLayoutManager() instanceof TestLayoutManager) {
            TestLayoutManager lm = (TestLayoutManager) mRecyclerView.getLayoutManager();
            // finish all layouts so that we get the correct exception
            if (lm.layoutLatch != null) {
                while (lm.layoutLatch.getCount() > 0) {
                    lm.layoutLatch.countDown();
                }
            }
        }
    }

    public Instrumentation getInstrumentation() {
        return InstrumentationRegistry.getInstrumentation();
    }

    @After
    public final void tearDown() throws Exception {
        if (mRecyclerView != null) {
            try {
                removeRecyclerView();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
        getInstrumentation().waitForIdleSync();

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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    // do not run validation if we already have an error
                    if (mMainThreadException == null) {
                        final RecyclerView.Adapter adapter = mRecyclerView.getAdapter();
                        if (adapter instanceof AttachDetachCountingAdapter) {
                            ((AttachDetachCountingAdapter) adapter).getCounter()
                                    .validateRemaining(mRecyclerView);
                        }
                    }
                    getActivity().getContainer().removeAllViews();
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }
        });
        mRecyclerView = null;
    }

    void waitForAnimations(int seconds) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
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

    public void waitForIdleScroll(final RecyclerView recyclerView) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        if (newState == SCROLL_STATE_IDLE) {
                            latch.countDown();
                            recyclerView.removeOnScrollListener(this);
                        }
                    }
                };
                if (recyclerView.getScrollState() == SCROLL_STATE_IDLE) {
                    latch.countDown();
                } else {
                    recyclerView.addOnScrollListener(listener);
                }
            }
        });
        assertTrue("should go idle in 10 seconds", latch.await(10, TimeUnit.SECONDS));
    }

    public boolean requestFocus(final View view, boolean waitForScroll) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = view.requestFocus();
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return view.hasFocus();
            }
        });
        if (waitForScroll && result[0]) {
            waitForIdleScroll(mRecyclerView);
        }
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
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().getContainer().addView(recyclerView);
            }
        });
    }

    protected FrameLayout getRecyclerViewContainer() {
        return getActivity().getContainer();
    }

    protected void requestLayoutOnUIThread(final View view) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.requestLayout();
            }
        });
    }

    protected void scrollBy(final int dt) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView.getLayoutManager().canScrollHorizontally()) {
                    mRecyclerView.scrollBy(dt, 0);
                } else {
                    mRecyclerView.scrollBy(0, dt);
                }

            }
        });
    }

    protected void smoothScrollBy(final int dt) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecyclerView.getLayoutManager().canScrollHorizontally()) {
                    mRecyclerView.smoothScrollBy(dt, 0);
                } else {
                    mRecyclerView.smoothScrollBy(0, dt);
                }

            }
        });
        getInstrumentation().waitForIdleSync();
    }

    void scrollToPosition(final int position) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.getLayoutManager().scrollToPosition(position);
            }
        });
    }

    void smoothScrollToPosition(final int position) throws Throwable {
        smoothScrollToPosition(position, true);
    }

    void smoothScrollToPosition(final int position, boolean assertArrival) throws Throwable {
        if (mDebug) {
            Log.d(TAG, "SMOOTH scrolling to " + position);
        }
        final CountDownLatch viewAdded = new CountDownLatch(1);
        final RecyclerView.OnChildAttachStateChangeListener listener =
                new RecyclerView.OnChildAttachStateChangeListener() {
                    @Override
                    public void onChildViewAttachedToWindow(@NonNull View view) {
                        if (position == mRecyclerView.getChildAdapterPosition(view)) {
                            viewAdded.countDown();
                        }
                    }
                    @Override
                    public void onChildViewDetachedFromWindow(@NonNull View view) {
                    }
                };
        final AtomicBoolean addedListener = new AtomicBoolean(false);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder viewHolderForAdapterPosition =
                        mRecyclerView.findViewHolderForAdapterPosition(position);
                if (viewHolderForAdapterPosition != null) {
                    viewAdded.countDown();
                } else {
                    mRecyclerView.addOnChildAttachStateChangeListener(listener);
                    addedListener.set(true);
                }

            }
        });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(position);
            }
        });
        getInstrumentation().waitForIdleSync();
        assertThat("should be able to scroll in 10 seconds", !assertArrival ||
                        viewAdded.await(10, TimeUnit.SECONDS),
                CoreMatchers.is(true));
        waitForIdleScroll(mRecyclerView);
        if (mDebug) {
            Log.d(TAG, "SMOOTH scrolling done");
        }
        if (addedListener.get()) {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.removeOnChildAttachStateChangeListener(listener);
                }
            });
        }
        getInstrumentation().waitForIdleSync();
    }

    void freezeLayout(final boolean freeze) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setLayoutFrozen(freeze);
            }
        });
    }

    public void setVisibility(final View view, final int visibility) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setVisibility(visibility);
            }
        });
    }

    public class TestViewHolder extends RecyclerView.ViewHolder {

        Item mBoundItem;
        Object mData;

        public TestViewHolder(View itemView) {
            super(itemView);
            itemView.setFocusable(true);
        }

        @Override
        public String toString() {
            return super.toString() + " item:" + mBoundItem + ", data:" + mData;
        }

        public Object getData() {
            return mData;
        }

        public void setData(Object data) {
            mData = data;
        }
    }
    class DumbLayoutManager extends TestLayoutManager {
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            if (layoutLatch != null) {
                layoutLatch.countDown();
            }
        }
    }

    public class TestLayoutManager extends RecyclerView.LayoutManager {
        int mScrollVerticallyAmount;
        int mScrollHorizontallyAmount;
        protected CountDownLatch layoutLatch;
        private boolean mSupportsPredictive = false;

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(int seconds) throws Throwable {
            layoutLatch.await(seconds * (mDebug ? 1000 : 1), SECONDS);
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

        public boolean isSupportsPredictive() {
            return mSupportsPredictive;
        }

        public void setSupportsPredictive(boolean supportsPredictive) {
            mSupportsPredictive = supportsPredictive;
        }

        @Override
        public boolean supportsPredictiveItemAnimations() {
            return mSupportsPredictive;
        }

        public void assertLayoutCount(int count, String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertEquals(msg, count, layoutLatch.getCount());
        }

        public void assertNoLayout(String msg, long timeout) throws Throwable {
            layoutLatch.await(timeout, TimeUnit.SECONDS);
            assertFalse(msg, layoutLatch.getCount() == 0);
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

        // START MOCKITO OVERRIDES
        // We override package protected methods to make them public. This is necessary to run
        // mockito on Kitkat
        @Override
        public void setRecyclerView(RecyclerView recyclerView) {
            super.setRecyclerView(recyclerView);
        }

        @Override
        public void dispatchAttachedToWindow(RecyclerView view) {
            super.dispatchAttachedToWindow(view);
        }

        @Override
        public void dispatchDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
            super.dispatchDetachedFromWindow(view, recycler);
        }

        @Override
        public void setExactMeasureSpecsFrom(RecyclerView recyclerView) {
            super.setExactMeasureSpecsFrom(recyclerView);
        }

        @Override
        public void setMeasureSpecs(int wSpec, int hSpec) {
            super.setMeasureSpecs(wSpec, hSpec);
        }

        @Override
        public void setMeasuredDimensionFromChildren(int widthSpec, int heightSpec) {
            super.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
        }

        @Override
        public boolean shouldReMeasureChild(View child, int widthSpec, int heightSpec,
                RecyclerView.LayoutParams lp) {
            return super.shouldReMeasureChild(child, widthSpec, heightSpec, lp);
        }

        @Override
        public boolean shouldMeasureChild(View child, int widthSpec, int heightSpec,
                RecyclerView.LayoutParams lp) {
            return super.shouldMeasureChild(child, widthSpec, heightSpec, lp);
        }

        @Override
        public void removeAndRecycleScrapInt(RecyclerView.Recycler recycler) {
            super.removeAndRecycleScrapInt(recycler);
        }

        @Override
        public void stopSmoothScroller() {
            super.stopSmoothScroller();
        }

        // END MOCKITO OVERRIDES
    }

    static class Item {
        final static AtomicInteger idCounter = new AtomicInteger(0);
        final public int mId = idCounter.incrementAndGet();

        int mAdapterIndex;

        String mText;
        int mType = 0;
        boolean mFocusable;

        Item(int adapterIndex, String text) {
            mAdapterIndex = adapterIndex;
            mText = text;
            mFocusable = true;
        }

        public boolean isFocusable() {
            return mFocusable;
        }

        public void setFocusable(boolean mFocusable) {
            this.mFocusable = mFocusable;
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

    public class FocusableAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private int mCount;

        FocusableAdapter(int count) {
            mCount = count;
        }

        @Override
        public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final TextView textView = new TextView(parent.getContext());
            textView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setFocusable(true);
            textView.setBackgroundResource(R.drawable.item_bg);
            return new TestViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            ((TextView) holder.itemView).setText("Item " + position);
        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }

    public class TestAdapter extends RecyclerView.Adapter<TestViewHolder>
            implements AttachDetachCountingAdapter {

        public static final String DEFAULT_ITEM_PREFIX = "Item ";

        ViewAttachDetachCounter mAttachmentCounter = new ViewAttachDetachCounter();
        List<Item> mItems;
        final @Nullable RecyclerView.LayoutParams mLayoutParams;

        public TestAdapter(int count) {
            this(count, null);
        }

        public TestAdapter(int count, @Nullable RecyclerView.LayoutParams layoutParams) {
            mItems = new ArrayList<Item>(count);
            addItems(0, count, DEFAULT_ITEM_PREFIX);
            mLayoutParams = layoutParams;
        }

        void addItems(int pos, int count, String prefix) {
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
        public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            TextView itemView = new TextView(parent.getContext());
            itemView.setFocusableInTouchMode(true);
            itemView.setFocusable(true);
            return new TestViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            assertNotNull(holder.mOwnerRecyclerView);
            assertEquals(position, holder.getAdapterPosition());
            final Item item = mItems.get(position);
            ((TextView) (holder.itemView)).setText(item.mText + "(" + item.mId + ")");
            holder.mBoundItem = item;
            if (mLayoutParams != null) {
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(mLayoutParams));
            }
        }

        public Item getItemAt(int position) {
            return mItems.get(position);
        }

        @Override
        public void onViewRecycled(@NonNull TestViewHolder holder) {
            super.onViewRecycled(holder);
            final int adapterPosition = holder.getAdapterPosition();
            final boolean shouldHavePosition = !holder.isRemoved() && holder.isBound() &&
                    !holder.isAdapterPositionUnknown() && !holder.isInvalid();
            String log = "Position check for " + holder.toString();
            assertEquals(log, shouldHavePosition, adapterPosition != RecyclerView.NO_POSITION);
            if (shouldHavePosition) {
                assertTrue(log, mItems.size() > adapterPosition);
                // TODO: fix b/36042615 getAdapterPosition() is wrong in
                // consumePendingUpdatesInOnePass where it applies pending change to already
                // modified position.
                if (holder.mPreLayoutPosition == RecyclerView.NO_POSITION) {
                    assertSame(log, holder.mBoundItem, mItems.get(adapterPosition));
                }
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
            mActivityRule.runOnUiThread(new AddRemoveRunnable(startCountTuples));
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
            mActivityRule.runOnUiThread(
                    new AddRemoveRunnable(DEFAULT_ITEM_PREFIX, new int[]{0, count}));
        }

        public void resetItemsTo(final List<Item> testItems) throws Throwable {
            if (!mItems.isEmpty()) {
                deleteAndNotify(0, mItems.size());
            }
            mItems = testItems;
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeInserted(0, testItems.size());
                }
            });
        }

        public void addAndNotify(final int start, final int count) throws Throwable {
            addAndNotify(new int[]{start, count});
        }

        public void addAndNotify(final int[]... startCountTuples) throws Throwable {
            mActivityRule.runOnUiThread(new AddRemoveRunnable(startCountTuples));
        }

        public void dispatchDataSetChanged() throws Throwable {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        public void changeAndNotify(final int start, final int count) throws Throwable {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(start, count);
                }
            });
        }

        public void changeAndNotifyWithPayload(final int start, final int count,
                final Object payload) throws Throwable {
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyItemRangeChanged(start, count, payload);
                }
            });
        }

        public void changePositionsAndNotify(final int... positions) throws Throwable {
            mActivityRule.runOnUiThread(new Runnable() {
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
            mActivityRule.runOnUiThread(new AddRemoveRunnable(startCountTuples));
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
            mActivityRule.runOnUiThread(new Runnable() {
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
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    moveInUIThread(from, to);
                    notifyItemMoved(from, to);
                }
            });
        }

        void changeAllItemsAndNotifyDataSetChanged(int count) {
            assertEquals("clearOnUIThread called from a wrong thread",
                    Looper.getMainLooper(), Looper.myLooper());
            mItems = new ArrayList<>();
            addItems(0, count, DEFAULT_ITEM_PREFIX);
            notifyDataSetChanged();
        }

        public void clearOnUIThread() {
            changeAllItemsAndNotifyDataSetChanged(0);
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


    public static View findFirstFullyVisibleChild(RecyclerView parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (isViewFullyInBound(parent, child)) {
                return child;
            }
        }
        return null;
    }

    public static View findLastFullyVisibleChild(RecyclerView parent) {
        for (int i = parent.getChildCount() - 1; i >= 0; i--) {
            View child = parent.getChildAt(i);
            if (isViewFullyInBound(parent, child)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Returns whether a child of RecyclerView is partially in bound. A child is
     * partially in-bounds if it's either fully or partially visible on the screen.
     * @param parent The RecyclerView holding the child.
     * @param child The child view to be checked whether is partially (or fully) within RV's bounds.
     * @return True if the child view is partially (or fully) visible; false otherwise.
     */
    public static boolean isViewPartiallyInBound(RecyclerView parent, View child) {
        if (child == null) {
            return false;
        }
        final int parentLeft = parent.getPaddingLeft();
        final int parentTop = parent.getPaddingTop();
        final int parentRight = parent.getWidth() - parent.getPaddingRight();
        final int parentBottom = parent.getHeight() - parent.getPaddingBottom();

        final int childLeft = child.getLeft() - child.getScrollX();
        final int childTop = child.getTop() - child.getScrollY();
        final int childRight = child.getRight() - child.getScrollX();
        final int childBottom = child.getBottom() - child.getScrollY();

        if (childLeft >= parentRight || childRight <= parentLeft
                || childTop >= parentBottom || childBottom <= parentTop) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether a child of RecyclerView is fully in-bounds, that is it's fully visible
     * on the screen.
     * @param parent The RecyclerView holding the child.
     * @param child The child view to be checked whether is fully within RV's bounds.
     * @return True if the child view is fully visible; false otherwise.
     */
    public static boolean isViewFullyInBound(RecyclerView parent, View child) {
        if (child == null) {
            return false;
        }
        final int parentLeft = parent.getPaddingLeft();
        final int parentTop = parent.getPaddingTop();
        final int parentRight = parent.getWidth() - parent.getPaddingRight();
        final int parentBottom = parent.getHeight() - parent.getPaddingBottom();

        final int childLeft = child.getLeft() - child.getScrollX();
        final int childTop = child.getTop() - child.getScrollY();
        final int childRight = child.getRight() - child.getScrollX();
        final int childBottom = child.getBottom() - child.getScrollY();

        if (childLeft >= parentLeft && childRight <= parentRight
                && childTop >= parentTop && childBottom <= parentBottom) {
            return true;
        }
        return false;
    }
}
