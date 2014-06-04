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

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Includes tests for {@link LinearLayoutManager}.
 * <p>
 * Since most UI tests are not practical, these tests are focused on internal data representation
 * and stability of LinearLayoutManager in response to different events (state change, scrolling
 * etc) where it is very hard to do manual testing.
 */
public class LinearLayoutManagerTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private static final String TAG = "LinearLayoutManagerTest";

    WrappedLinearLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    final List<Config> mBaseVariations = new ArrayList<Config>();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (int orientation : new int[]{LinearLayoutManager.VERTICAL,
                LinearLayoutManager.HORIZONTAL}) {
            for (boolean reverseLayout : new boolean[]{false, true}) {
                for (boolean stackFromBottom : new boolean[]{false, true}) {
                    mBaseVariations.add(new Config(orientation, reverseLayout, stackFromBottom));
                }
            }
        }
    }

    protected List<Config> addConfigVariation(List<Config> base, String fieldName,
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
        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView.setHasFixedSize(true);
        mTestAdapter = new TestAdapter(config.mItemCount);
        mRecyclerView.setAdapter(mTestAdapter);
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(), config.mOrientation,
                config.mReverseLayout);
        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (waitForFirstLayout) {
            waitForFirstLayout();
        }
    }

    private void waitForFirstLayout() throws Throwable {
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
    }


    public void testGetFirstLastChildrenTest() throws Throwable {
        for (Config config : mBaseVariations) {
            getFirstLastChildrenTest(config);
        }
    }

    public void getFirstLastChildrenTest(final Config config) throws Throwable {
        setupByConfig(config, true);
        Runnable viewInBoundsTest = new Runnable() {
            @Override
            public void run() {
                VisibleChildren visibleChildren = mLayoutManager.traverseAndFindVisibleChildren();
                final String boundsLog = mLayoutManager.getBoundsLog();
                assertEquals(config + ":\nfirst visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstVisiblePosition,
                        mLayoutManager.findFirstVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nfirst fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.firstFullyVisiblePosition,
                        mLayoutManager.findFirstCompletelyVisibleItemPosition()
                );

                assertEquals(config + ":\nlast visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastVisiblePosition,
                        mLayoutManager.findLastVisibleItemPosition()
                );
                assertEquals(
                        config + ":\nlast fully visible child should match traversal result\n"
                                + boundsLog, visibleChildren.lastFullyVisiblePosition,
                        mLayoutManager.findLastCompletelyVisibleItemPosition()
                );
            }
        };
        runTestOnUiThread(viewInBoundsTest);
        // smooth scroll to end of the list and keep testing meanwhile. This will test pre-caching
        // case
        final int scrollPosition = config.mStackFromEnd ? 0 : mTestAdapter.getItemCount();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(scrollPosition);
            }
        });
        while (mLayoutManager.isSmoothScrolling() ||
                mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_IDLE) {
            runTestOnUiThread(viewInBoundsTest);
            Thread.sleep(200);
        }
        // delete all items
        mLayoutManager.expectLayouts(2);
        mTestAdapter.deleteAndNotify(0, mTestAdapter.getItemCount());
        mLayoutManager.waitForLayout(2);
        // test empty case
        runTestOnUiThread(viewInBoundsTest);
        // set a new adapter with huge items to test full bounds check
        mLayoutManager.expectLayouts(1);
        final int totalSpace = mLayoutManager.mOrientationHelper.getTotalSpace();
        final TestAdapter newAdapter = new TestAdapter(100) {
            @Override
            public void onBindViewHolder(TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (config.mOrientation == LinearLayoutManager.HORIZONTAL) {
                    holder.itemView.setMinimumWidth(totalSpace + 5);
                } else {
                    holder.itemView.setMinimumHeight(totalSpace + 5);
                }
            }
        };
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.setAdapter(newAdapter);
            }
        });
        mLayoutManager.waitForLayout(2);
        runTestOnUiThread(viewInBoundsTest);
    }

    public void testSavedState() throws Throwable {
        PostLayoutRunnable[] postLayoutOptions = new PostLayoutRunnable[]{
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        // do nothing
                    }

                    @Override
                    public String describe() {
                        return "doing nothing";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPosition(mTestAdapter.getItemCount() * 3 / 4);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mTestAdapter.getItemCount() * 1 / 3,
                                50);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position with positive offset";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        mLayoutManager.expectLayouts(1);
                        scrollToPositionWithOffset(mTestAdapter.getItemCount() * 2 / 3,
                                -50);
                        mLayoutManager.waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll to position with negative offset";
                    }
                }
        };

        PostRestoreRunnable[] postRestoreOptions = new PostRestoreRunnable[]{
                new PostRestoreRunnable() {
                    @Override
                    public String describe() {
                        return "Doing nothing";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        // update config as well so that restore assertions will work
                        config.mOrientation = 1 - config.mOrientation;
                        mLayoutManager.setOrientation(config.mOrientation);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing orientation";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mStackFromEnd = !config.mStackFromEnd;
                        mLayoutManager.setStackFromEnd(config.mStackFromEnd);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return true; //stack from end should not move items on change
                    }

                    @Override
                    public String describe() {
                        return "Changing stack from end";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mReverseLayout = !config.mReverseLayout;
                        mLayoutManager.setReverseLayout(config.mReverseLayout);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing reverse layout";
                    }
                },
                new PostRestoreRunnable() {
                    int position;
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        position = mTestAdapter.getItemCount() / 2;
                        mLayoutManager.scrollToPosition(position);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return mTestAdapter.getItemCount() == 0;
                    }

                    @Override
                    String describe() {
                        return "Scroll to position";
                    }

                    @Override
                    void onAfterReLayout(Config config) {
                        if (mTestAdapter.getItemCount() > 0) {
                            assertEquals("scrolled view should be last completely visible position",
                                    position,
                                    mLayoutManager.findLastCompletelyVisibleItemPosition());
                        }
                    }
                }
        };
        boolean[] waitForLayoutOptions = new boolean[]{true, false};
        for (Config config : addConfigVariation(mBaseVariations, "mItemCount", 0, 300)) {
            for (PostLayoutRunnable postLayoutRunnable : postLayoutOptions) {
                for (boolean waitForLayout : waitForLayoutOptions) {
                    for (PostRestoreRunnable postRestoreRunnable : postRestoreOptions) {
                        savedStateTest((Config) config.clone(), waitForLayout, postLayoutRunnable,
                                postRestoreRunnable);
                        removeRecyclerView();
                        return;
                    }

                }
            }
        }
    }

    public void savedStateTest(Config config, boolean waitForLayout,
            PostLayoutRunnable postLayoutOperation, PostRestoreRunnable postRestoreOperation)
            throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with wait for layout = " + waitForLayout + " config " +
                    config + " post layout action " + postLayoutOperation.describe() +
                    "post restore action " + postRestoreOperation.describe());
        }
        setupByConfig(config, false);
        if (waitForLayout) {
            waitForFirstLayout();
            postLayoutOperation.run();
        }
        Map<Item, Rect> before = mLayoutManager.collectChildCoordinates();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);
        removeRecyclerView();
        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        removeRecyclerView();

        RecyclerView restored = new RecyclerView(getActivity());
        // this config should be no op.
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(),
                1 - config.mOrientation, !config.mReverseLayout);
        mLayoutManager.setStackFromEnd(!config.mStackFromEnd);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mTestAdapter);
        restored.onRestoreInstanceState(savedState);
        postRestoreOperation.onAfterRestore(config);
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        // calculate prefix here instead of above to include post restore changes
        final String logPrefix = config + "\npostLayout:" + postLayoutOperation.describe() +
                "\npostRestore:" + postRestoreOperation.describe() + "\n";
        assertEquals(logPrefix + " on saved state, reverse layout should be preserved",
                config.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals(logPrefix + " on saved state, orientation should be preserved",
                config.mOrientation, mLayoutManager.getOrientation());
        assertEquals(logPrefix + " on saved state, stack from end should be preserved",
                config.mStackFromEnd, mLayoutManager.getStackFromEnd());
        if (waitForLayout) {
            if (postRestoreOperation.shouldLayoutMatch(config)) {
                assertRectSetsEqual(
                        logPrefix + ": on restore, previous view positions should be preserved",
                        before, mLayoutManager.collectChildCoordinates());
            } else {
                assertRectSetsNotEqual(
                        logPrefix
                                + ": on restore with changes, previous view positions should NOT be preserved",
                        before, mLayoutManager.collectChildCoordinates());
            }
            postRestoreOperation.onAfterReLayout(config);
        }
    }

    void scrollToPositionWithOffset(final int position, final int offset) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLayoutManager.scrollToPositionWithOffset(position, offset);
            }
        });
    }

    public void assertRectSetsNotEqual(String message, Map<Item, Rect> before,
            Map<Item, Rect> after) {
        Throwable throwable = null;
        try {
            assertRectSetsEqual("NOT " + message, before, after);
        } catch (Throwable t) {
            throwable = t;
        }
        assertNotNull(message + "\ntwo layout should be different", throwable);
    }

    public void assertRectSetsEqual(String message, Map<Item, Rect> before, Map<Item, Rect> after) {
        if (DEBUG) {
            Log.d(TAG, "checking rectangle equality.");
            Log.d(TAG, "before:");
            for (Map.Entry<Item, Rect> entry : before.entrySet()) {
                Log.d(TAG, entry.getKey().originalIndex + ":" + entry.getValue());
            }
            Log.d(TAG, "after:");
            for (Map.Entry<Item, Rect> entry : after.entrySet()) {
                Log.d(TAG, entry.getKey().originalIndex + ":" + entry.getValue());
            }
        }
        assertEquals(message + ":\nitem counts should be equal", before.size()
                , after.size());
        for (Map.Entry<Item, Rect> entry : before.entrySet()) {
            Rect afterRect = after.get(entry.getKey());
            assertNotNull(message + ":\nSame item should be visible after simple re-layout",
                    afterRect);
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

    abstract private class PostLayoutRunnable {

        abstract void run() throws Throwable;

        abstract String describe();
    }

    abstract private class PostRestoreRunnable {

        void onAfterRestore(Config config) throws Throwable {
        }

        abstract String describe();

        boolean shouldLayoutMatch(Config config) {
            return true;
        }

        void onAfterReLayout(Config config) {

        };
    }

    class WrappedLinearLayoutManager extends LinearLayoutManager {

        CountDownLatch layoutLatch;

        OrientationHelper mSecondaryOrientation;

        public WrappedLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public void expectLayouts(int count) {
            layoutLatch = new CountDownLatch(count);
        }

        public void waitForLayout(long timeout) throws InterruptedException {
            waitForLayout(timeout, TimeUnit.SECONDS);
        }

        @Override
        public void setOrientation(int orientation) {
            super.setOrientation(orientation);
            mSecondaryOrientation = null;
        }

        @Override
        void ensureRenderState() {
            super.ensureRenderState();
            if (mSecondaryOrientation == null) {
                mSecondaryOrientation = getOrientation() == HORIZONTAL
                        ? createVerticalOrientationHelper()
                        : createHorizontalOrientationHelper();
            }
        }

        private void waitForLayout(long timeout, TimeUnit timeUnit) throws InterruptedException {
            layoutLatch.await(timeout, timeUnit);
            assertEquals("all expected layouts should be executed at the expected time",
                    0, layoutLatch.getCount());
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
            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int childCount = getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = getChildAt(i);
                        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child
                                .getLayoutParams();
                        TestViewHolder vh = (TestViewHolder) lp.mViewHolder;
                        items.put(vh.mBindedItem, getViewBounds(child));
                    }
                }
            });
            return items;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            super.onLayoutChildren(recycler, state);
            layoutLatch.countDown();
        }
    }

    static class Config implements Cloneable {

        private static final int DEFAULT_ITEM_COUNT = 300;

        private boolean mStackFromEnd;

        int mOrientation = LinearLayoutManager.VERTICAL;

        boolean mReverseLayout = false;

        int mItemCount = DEFAULT_ITEM_COUNT;

        Config(int orientation, boolean reverseLayout, boolean stackFromEnd) {
            mOrientation = orientation;
            mReverseLayout = reverseLayout;
            mStackFromEnd = stackFromEnd;
        }

        public Config() {

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
            return "Config{" +
                    "mStackFromEnd=" + mStackFromEnd +
                    ", mOrientation=" + mOrientation +
                    ", mReverseLayout=" + mReverseLayout +
                    ", mItemCount=" + mItemCount +
                    '}';
        }
    }
}
