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

import static org.junit.Assert.assertEquals;

import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.filters.LargeTest;
import android.util.Log;

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
    private final boolean mWaitForLayout;
    private final boolean mLoadDataAfterRestore;
    private final PostLayoutRunnable mPostLayoutOperations;

    public StaggeredGridLayoutManagerSavedStateTest(
            Config config, boolean waitForLayout, boolean loadDataAfterRestore,
            PostLayoutRunnable postLayoutOperations) throws CloneNotSupportedException {
        this.mConfig = (Config) config.clone();
        this.mWaitForLayout = waitForLayout;
        this.mLoadDataAfterRestore = loadDataAfterRestore;
        this.mPostLayoutOperations = postLayoutOperations;
        if (postLayoutOperations != null) {
            postLayoutOperations.mTest = this;
        }
    }

    @Parameterized.Parameters(name = "config={0},waitForLayout={1},loadDataAfterRestore={2}"
            + ",postLayoutRunnable={3}")
    public static List<Object[]> getParams() throws CloneNotSupportedException {
        List<Config> variations = createBaseVariations();

        PostLayoutRunnable[] postLayoutOptions = new PostLayoutRunnable[]{
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
        boolean[] waitForLayoutOptions = new boolean[]{false, true};
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

        List<Object[]> params = new ArrayList<>();
        for (Config config : testVariations) {
            for (PostLayoutRunnable runnable : postLayoutOptions) {
                for (boolean waitForLayout : waitForLayoutOptions) {
                    for (boolean loadDataAfterRestore : loadDataAfterRestoreOptions) {
                        params.add(new Object[]{config, waitForLayout, loadDataAfterRestore,
                                runnable});
                    }
                }
            }
        }
        return params;
    }

    @Test
    public void savedState() throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with wait for layout = " + mWaitForLayout + " config "
                    + mConfig + " post layout action " + mPostLayoutOperations.describe());
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
        if (mWaitForLayout) {
            mPostLayoutOperations.run();
        }
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
        assertEquals(mConfig + " on saved state, first completely visible child position should"
                        + " be preserved", firstCompletelyVisiblePosition,
                mLayoutManager.findFirstVisibleItemPositionInt());
        if (mWaitForLayout) {
            final boolean strictItemEquality = !mLoadDataAfterRestore;
            assertRectSetsEqual(mConfig + "\npost layout op:" + mPostLayoutOperations.describe()
                            + ": on restore, previous view positions should be preserved",
                    before, mLayoutManager.collectChildCoordinates(), strictItemEquality);
        }
        // TODO add tests for changing values after restore before layout
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
}
