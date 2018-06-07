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

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class BaseGridLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    static final String TAG = "GridLayoutManagerTest";
    static final boolean DEBUG = false;

    WrappedGridLayoutManager mGlm;
    GridTestAdapter mAdapter;

    public RecyclerView setupBasic(Config config) throws Throwable {
        return setupBasic(config, new GridTestAdapter(config.mItemCount));
    }

    public RecyclerView setupBasic(Config config, GridTestAdapter testAdapter) throws Throwable {
        RecyclerView recyclerView = new WrappedRecyclerView(getActivity());
        mAdapter = testAdapter;
        mGlm = new WrappedGridLayoutManager(getActivity(), config.mSpanCount, config.mOrientation,
                config.mReverseLayout);
        mAdapter.assignSpanSizeLookup(mGlm);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(mGlm);
        return recyclerView;
    }

    public static List<Config> createBaseVariations() {
        List<Config> variations = new ArrayList<>();
        for (int orientation : new int[]{VERTICAL, HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (int spanCount : new int[]{1, 3, 4}) {
                    variations.add(new Config(spanCount, orientation, reverseLayout));
                }
            }
        }
        return variations;
    }

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

    public void waitForFirstLayout(RecyclerView recyclerView) throws Throwable {
        mGlm.expectLayout(1);
        setRecyclerView(recyclerView);
        mGlm.waitForLayout(2);
    }

    protected int getSize(View view) {
        if (mGlm.getOrientation() == GridLayoutManager.HORIZONTAL) {
            return view.getWidth();
        }
        return view.getHeight();
    }

    GridLayoutManager.LayoutParams getLp(View view) {
        return (GridLayoutManager.LayoutParams) view.getLayoutParams();
    }

    static class Config implements Cloneable {

        int mSpanCount;
        int mOrientation = GridLayoutManager.VERTICAL;
        int mItemCount = 1000;
        int mSpanPerItem = 1;
        boolean mReverseLayout = false;

        Config(int spanCount, int itemCount) {
            mSpanCount = spanCount;
            mItemCount = itemCount;
        }

        public Config(int spanCount, int orientation, boolean reverseLayout) {
            mSpanCount = spanCount;
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
        }

        Config orientation(int orientation) {
            mOrientation = orientation;
            return this;
        }

        @Override
        public String toString() {
            return "Config{"
                    + "mSpanCount=" + mSpanCount
                    + ",mOrientation=" + (mOrientation == GridLayoutManager.HORIZONTAL ? "h" : "v")
                    + ",mItemCount=" + mItemCount
                    + ",mReverseLayout=" + mReverseLayout
                    + '}';
        }

        public Config reverseLayout(boolean reverseLayout) {
            mReverseLayout = reverseLayout;
            return this;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    class WrappedGridLayoutManager extends GridLayoutManager {

        CountDownLatch mLayoutLatch;

        CountDownLatch prefetchLatch;

        OrientationHelper mSecondaryOrientation;

        List<GridLayoutManagerTest.Callback>
                mCallbacks = new ArrayList<GridLayoutManagerTest.Callback>();

        Boolean mFakeRTL;
        private CountDownLatch snapLatch;

        public WrappedGridLayoutManager(Context context, int spanCount) {
            super(context, spanCount);
        }

        public WrappedGridLayoutManager(Context context, int spanCount, int orientation,
                boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        protected boolean isLayoutRTL() {
            return mFakeRTL == null ? super.isLayoutRTL() : mFakeRTL;
        }

        public void setFakeRtl(Boolean fakeRtl) {
            mFakeRTL = fakeRtl;
            try {
                requestLayoutOnUIThread(mRecyclerView);
            } catch (Throwable throwable) {
                postExceptionToInstrumentation(throwable);
            }
        }

        @Override
        public void setOrientation(int orientation) {
            super.setOrientation(orientation);
            mSecondaryOrientation = null;
        }

        @Override
        void ensureLayoutState() {
            super.ensureLayoutState();
            if (mSecondaryOrientation == null) {
                if (getOrientation() == RecyclerView.HORIZONTAL) {
                    mSecondaryOrientation = OrientationHelper.createOrientationHelper(this,
                        RecyclerView.VERTICAL);
                } else {
                    mSecondaryOrientation = OrientationHelper.createOrientationHelper(this,
                        RecyclerView.HORIZONTAL);
                }
            }
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                for (GridLayoutManagerTest.Callback callback : mCallbacks) {
                    callback.onBeforeLayout(recycler, state);
                }
                super.onLayoutChildren(recycler, state);
                for (GridLayoutManagerTest.Callback callback : mCallbacks) {
                    callback.onAfterLayout(recycler, state);
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
            mLayoutLatch.countDown();
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

        public void expectLayout(int layoutCount) {
            mLayoutLatch = new CountDownLatch(layoutCount);
        }

        public void waitForLayout(int seconds) throws Throwable {
            mLayoutLatch.await(seconds * (DEBUG ? 1000 : 1), SECONDS);
            checkForMainThreadException();
            MatcherAssert.assertThat("all layouts should complete on time",
                    mLayoutLatch.getCount(), CoreMatchers.is(0L));
            // use a runnable to ensure RV layout is finished
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                }
            });
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
                public void run() {
                }
            });
        }

        @Override
        public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                LayoutPrefetchRegistry layoutPrefetchRegistry) {
            if (prefetchLatch != null) prefetchLatch.countDown();
            super.collectAdjacentPrefetchPositions(dx, dy, state, layoutPrefetchRegistry);
        }
    }

    class GridFocusableAdapter extends FocusableAdapter {

        Set<Integer> mFullSpanItems = new HashSet<Integer>();
        int mSpanPerItem = 1;

        GridFocusableAdapter(int count) {
            this(count, 1);
        }

        GridFocusableAdapter(int count, int spanPerItem) {
            super(count);
            mSpanPerItem = spanPerItem;
        }

        void setFullSpan(int... items) {
            for (int i : items) {
                mFullSpanItems.add(i);
            }
        }

        void assignSpanSizeLookup(final GridLayoutManager glm) {
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return mFullSpanItems.contains(position) ? glm.getSpanCount() : mSpanPerItem;
                }
            });
        }
    }

    class GridTestAdapter extends TestAdapter {

        Set<Integer> mFullSpanItems = new HashSet<Integer>();
        int mSpanPerItem = 1;

        GridTestAdapter(int count) {
            super(count);
        }

        GridTestAdapter(int count, int spanPerItem) {
            super(count);
            mSpanPerItem = spanPerItem;
        }

        void setFullSpan(int... items) {
            for (int i : items) {
                mFullSpanItems.add(i);
            }
        }

        void assignSpanSizeLookup(final GridLayoutManager glm) {
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return mFullSpanItems.contains(position) ? glm.getSpanCount() : mSpanPerItem;
                }
            });
        }
    }

    class Callback {

        public void onBeforeLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }

        public void onAfterLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        }
    }
}
