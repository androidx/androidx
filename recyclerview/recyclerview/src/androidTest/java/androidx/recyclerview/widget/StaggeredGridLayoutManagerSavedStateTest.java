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

import static androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(Parameterized.class)
@LargeTest
public class StaggeredGridLayoutManagerSavedStateTest extends BaseStaggeredGridLayoutManagerTest {
    private final Config mConfig;
    private final boolean mLoadDataAfterRestore;
    private final PostLayoutRunnable mPostLayoutOperations;
    private final PostRestoreRunnable mPostRestoreOperations;

    public StaggeredGridLayoutManagerSavedStateTest(
            Config config,
            boolean loadDataAfterRestore,
            PostLayoutRunnable postLayoutOperations,
            PostRestoreRunnable postRestoreOperations,
            int index) throws CloneNotSupportedException {
        this.mConfig = (Config) config.clone();
        this.mLoadDataAfterRestore = loadDataAfterRestore;
        this.mPostLayoutOperations = postLayoutOperations;
        this.mPostRestoreOperations = postRestoreOperations;
        if (postLayoutOperations != null) {
            postLayoutOperations.mTest = this;
        }
        if (mPostRestoreOperations != null) {
            mPostRestoreOperations.mTest = this;
        }
    }

    @Parameterized.Parameters(name = "config={0},loadDataAfterRestore={1},postLayoutRunnable={2}"
            + ",postRestoreRunnable={3},index={4}")
    public static List<Object[]> getParams() throws CloneNotSupportedException {
        List<Config> variations = createBaseVariations();

        final PostLayoutRunnable[] postLayoutOptions = new PostLayoutRunnable[]{
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        // do nothing
                    }

                    @Override
                    public String describe() {
                        return "doing_nothing";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        layoutManager().expectLayouts(1);
                        scrollToPosition(adapter().getItemCount() * 3 / 4);
                        layoutManager().waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll_to_position_item_count*3/4";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        layoutManager().expectLayouts(1);
                        scrollToPositionWithOffset(adapter().getItemCount() / 3,
                                50);
                        layoutManager().waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll_to_position_item_count/3_with_positive_offset";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        layoutManager().expectLayouts(1);
                        scrollToPositionWithOffset(adapter().getItemCount() * 2 / 3,
                                -50);
                        layoutManager().waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll_to_position_with_negative_offset";
                    }
                }
        };
        PostRestoreRunnable[] postRestoreOptions = new PostRestoreRunnable[]{
                new PostRestoreRunnable() {
                    @Override
                    String describe() {
                        return "doing_nothing";
                    }
                },
                new PostRestoreRunnable() {
                    int mPosition;

                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        mPosition = adapter().getItemCount() / 2;
                        layoutManager().scrollToPosition(mPosition);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return adapter().getItemCount() <= config.mSpanCount
                                && config.mGapStrategy != GAP_HANDLING_NONE;
                    }

                    @Override
                    void onAfterReLayout(Config config) {
                        if (adapter().getItemCount() > 0) {
                            assertNotNull(
                                    "view at " + mPosition + " should be visible",
                                    layoutManager().findViewByPosition(mPosition));
                        }
                    }

                    @Override
                    String describe() {
                        return "scroll_to_pos_" + mPosition;
                    }
                }
        };
        boolean[] loadDataAfterRestoreOptions = new boolean[]{false, true};
        List<Config> testVariations = new ArrayList<Config>();
        testVariations.addAll(variations);
        for (Config config : variations) {
            if (config.mSpanCount < 2) {
                continue;
            }
            final Config clone = (Config) config.clone();
            clone.mItemCount = clone.mSpanCount - 1;
            testVariations.add(clone);
        }

        int index = 0;
        List<Object[]> params = new ArrayList<>();
        for (Config config : testVariations) {
            for (PostLayoutRunnable postLayout : postLayoutOptions) {
                for (PostRestoreRunnable postRestore : postRestoreOptions) {
                    for (boolean loadDataAfterRestore : loadDataAfterRestoreOptions) {
                        params.add(new Object[]{config, loadDataAfterRestore,
                                postLayout, postRestore, index++});
                    }
                }
            }
        }
        return params;
    }

    @Test
    public void savedState() throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with config " + mConfig
                    + " post layout action " + mPostLayoutOperations.describe());
        }
        setupByConfig(mConfig);
        if (mLoadDataAfterRestore) {
            // We are going to re-create items, force non-random item size.
            mAdapter.mOnBindCallback = new OnBindCallback() {
                @Override
                void onBoundItem(TestViewHolder vh, int position) {
                }

                @Override
                boolean assignRandomSize() {
                    return false;
                }
            };
        }
        waitFirstLayout();
        mPostLayoutOperations.run();

        getInstrumentation().waitForIdleSync();
        final int firstCompletelyVisiblePosition = mLayoutManager.findFirstVisibleItemPositionInt();
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

        final int itemCount = mAdapter.getItemCount();
        List<Item> mItems = new ArrayList<>();
        if (mLoadDataAfterRestore) {
            mItems.addAll(mAdapter.mItems);
            mAdapter.deleteAndNotify(0, itemCount);
        }

        RecyclerView restored = new RecyclerView(getActivity());
        mLayoutManager = new WrappedLayoutManager(mConfig.mSpanCount, mConfig.mOrientation);
        mLayoutManager.setGapStrategy(mConfig.mGapStrategy);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mAdapter);
        restored.onRestoreInstanceState(savedState);

        if (mLoadDataAfterRestore) {
            mAdapter.resetItemsTo(mItems);
        }
        mPostRestoreOperations.onAfterRestore(mConfig);
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        assertEquals(mConfig + " on saved state, reverse layout should be preserved",
                mConfig.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals(mConfig + " on saved state, orientation should be preserved",
                mConfig.mOrientation, mLayoutManager.getOrientation());
        assertEquals(mConfig + " on saved state, span count should be preserved",
                mConfig.mSpanCount, mLayoutManager.getSpanCount());
        assertEquals(mConfig + " on saved state, gap strategy should be preserved",
                mConfig.mGapStrategy, mLayoutManager.getGapStrategy());
        if (mPostRestoreOperations.shouldLayoutMatch(mConfig)) {
            assertEquals(mConfig + " on saved state, first completely visible child "
                            + "position should be preserved", firstCompletelyVisiblePosition,
                    mLayoutManager.findFirstVisibleItemPositionInt());
        }

        final boolean strictItemEquality = !mLoadDataAfterRestore;
        if (mPostRestoreOperations.shouldLayoutMatch(mConfig)) {
            assertRectSetsEqual(mConfig + "\npost layout op:" + mPostLayoutOperations.describe()
                            + ": on restore, previous view positions should be preserved",
                    before, mLayoutManager.collectChildCoordinates(), strictItemEquality);

        } else {
            assertRectSetsNotEqual(mConfig + "\npost layout op:" + mPostLayoutOperations.describe()
                            + ": on restore, previous view positions should be different",
                    before, mLayoutManager.collectChildCoordinates());
        }
    }

    static abstract class PostLayoutRunnable {
        StaggeredGridLayoutManagerSavedStateTest mTest;

        public void setup(StaggeredGridLayoutManagerSavedStateTest test) {
            mTest = test;
        }

        public GridTestAdapter adapter() {
            return mTest.mAdapter;
        }

        public WrappedLayoutManager layoutManager() {
            return mTest.mLayoutManager;
        }

        public void scrollToPositionWithOffset(int position, int offset) throws Throwable {
            mTest.scrollToPositionWithOffset(position, offset);
        }

        public void scrollToPosition(int position) throws Throwable {
            mTest.scrollToPosition(position);
        }

        abstract void run() throws Throwable;

        abstract String describe();

        @Override
        public String toString() {
            return describe();
        }
    }

    abstract static class PostRestoreRunnable {
        StaggeredGridLayoutManagerSavedStateTest mTest;

        public void setup(StaggeredGridLayoutManagerSavedStateTest test) {
            mTest = test;
        }

        public WrappedLayoutManager layoutManager() {
            return mTest.mLayoutManager;
        }

        public GridTestAdapter adapter() {
            return mTest.mAdapter;
        }

        void onAfterRestore(Config config) throws Throwable {
        }

        abstract String describe();

        boolean shouldLayoutMatch(Config config) {
            return true;
        }

        void onAfterReLayout(Config config) {

        }

        @Override
        public String toString() {
            return describe();
        }
    }
}
