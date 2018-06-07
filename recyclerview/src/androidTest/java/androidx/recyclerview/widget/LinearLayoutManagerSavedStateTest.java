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
import android.support.test.InstrumentationRegistry;
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
public class LinearLayoutManagerSavedStateTest extends BaseLinearLayoutManagerTest {
    final Config mConfig;
    final boolean mWaitForLayout;
    final boolean mLoadDataAfterRestore;
    final PostLayoutRunnable mPostLayoutOperation;
    final PostRestoreRunnable mPostRestoreOperation;

    public LinearLayoutManagerSavedStateTest(Config config, boolean waitForLayout,
            boolean loadDataAfterRestore, PostLayoutRunnable postLayoutOperation,
            PostRestoreRunnable postRestoreOperation) {
        mConfig = config;
        mWaitForLayout = waitForLayout;
        mLoadDataAfterRestore = loadDataAfterRestore;
        mPostLayoutOperation = postLayoutOperation;
        mPostRestoreOperation = postRestoreOperation;
        mPostLayoutOperation.mLayoutManagerDelegate = new Delegate<WrappedLinearLayoutManager>() {
            @Override
            public WrappedLinearLayoutManager get() {
                return mLayoutManager;
            }
        };
        mPostLayoutOperation.mTestAdapterDelegate = new Delegate<TestAdapter>() {
            @Override
            public TestAdapter get() {
                return mTestAdapter;
            }
        };
        mPostRestoreOperation.mLayoutManagerDelegate = new Delegate<WrappedLinearLayoutManager>() {
            @Override
            public WrappedLinearLayoutManager get() {
                return mLayoutManager;
            }
        };
        mPostRestoreOperation.mTestAdapterDelegate = new Delegate<TestAdapter>() {
            @Override
            public TestAdapter get() {
                return mTestAdapter;
            }
        };
    }

    @Parameterized.Parameters(name = "{0},waitForLayout:{1},loadDataAfterRestore:{2}"
            + ",postLayout:{3},postRestore:{4}")
    public static Iterable<Object[]> params()
            throws IllegalAccessException, CloneNotSupportedException, NoSuchFieldException {
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
                        scrollToPosition(testAdapter().getItemCount() * 3 / 4);
                        layoutManager().waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll_to_position";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        layoutManager().expectLayouts(1);
                        scrollToPositionWithOffset(testAdapter().getItemCount() / 3,
                                50);
                        layoutManager().waitForLayout(2);
                    }

                    @Override
                    public String describe() {
                        return "scroll_to_position_with_positive_offset";
                    }
                },
                new PostLayoutRunnable() {
                    @Override
                    public void run() throws Throwable {
                        layoutManager().expectLayouts(1);
                        scrollToPositionWithOffset(testAdapter().getItemCount() * 2 / 3,
                                -10);  // Some tests break if this value is below the item height.
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
                    public String describe() {
                        return "Doing nothing";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        // update config as well so that restore assertions will work
                        config.mOrientation = 1 - config.mOrientation;
                        layoutManager().setOrientation(config.mOrientation);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing_orientation";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mStackFromEnd = !config.mStackFromEnd;
                        layoutManager().setStackFromEnd(config.mStackFromEnd);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return true; //stack from end should not move items on change
                    }

                    @Override
                    public String describe() {
                        return "Changing_stack_from_end";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mReverseLayout = !config.mReverseLayout;
                        layoutManager().setReverseLayout(config.mReverseLayout);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return config.mItemCount == 0;
                    }

                    @Override
                    public String describe() {
                        return "Changing_reverse_layout";
                    }
                },
                new PostRestoreRunnable() {
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        config.mRecycleChildrenOnDetach = !config.mRecycleChildrenOnDetach;
                        layoutManager().setRecycleChildrenOnDetach(config.mRecycleChildrenOnDetach);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return true;
                    }

                    @Override
                    String describe() {
                        return "Change_should_recycle_children";
                    }
                },
                new PostRestoreRunnable() {
                    int position;
                    @Override
                    void onAfterRestore(Config config) throws Throwable {
                        position = testAdapter().getItemCount() / 2;
                        layoutManager().scrollToPosition(position);
                    }

                    @Override
                    boolean shouldLayoutMatch(Config config) {
                        return testAdapter().getItemCount() == 0;
                    }

                    @Override
                    String describe() {
                        return "Scroll_to_position_" + position;
                    }

                    @Override
                    void onAfterReLayout(Config config) {
                        if (testAdapter().getItemCount() > 0) {
                            assertEquals(config + ":scrolled view should be last completely visible",
                                    position,
                                    config.mStackFromEnd ?
                                            layoutManager().findLastCompletelyVisibleItemPosition()
                                            : layoutManager().findFirstCompletelyVisibleItemPosition());
                        }
                    }
                }
        };
        boolean[] waitForLayoutOptions = new boolean[]{true, false};
        boolean[] loadDataAfterRestoreOptions = new boolean[]{true, false};
        List<Config> variations = addConfigVariation(createBaseVariations(), "mItemCount", 0, 300);
        variations = addConfigVariation(variations, "mRecycleChildrenOnDetach", true);

        List<Object[]> params = new ArrayList<>();
        for (Config config : variations) {
            for (PostLayoutRunnable postLayoutRunnable : postLayoutOptions) {
                for (boolean waitForLayout : waitForLayoutOptions) {
                    for (PostRestoreRunnable postRestoreRunnable : postRestoreOptions) {
                        for (boolean loadDataAfterRestore : loadDataAfterRestoreOptions) {
                            params.add(new Object[]{
                                    config.clone(), waitForLayout,
                                    loadDataAfterRestore, postLayoutRunnable, postRestoreRunnable
                            });
                        }
                    }

                }
            }
        }
        return params;
    }

    @Test
    public void savedStateTest()
            throws Throwable {
        if (DEBUG) {
            Log.d(TAG, "testing saved state with wait for layout = " + mWaitForLayout + " config " +
                    mConfig + " post layout action " + mPostLayoutOperation.describe() +
                    "post restore action " + mPostRestoreOperation.describe());
        }
        setupByConfig(mConfig, false);

        if (mWaitForLayout) {
            waitForFirstLayout();
            mPostLayoutOperation.run();
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

        final int itemCount = mTestAdapter.getItemCount();
        List<Item> testItems = new ArrayList<>();
        if (mLoadDataAfterRestore) {
            // we cannot delete and re-add since new items may have different sizes. We need the
            // exact same adapter.
            testItems.addAll(mTestAdapter.mItems);
            mTestAdapter.deleteAndNotify(0, itemCount);
        }

        RecyclerView restored = new RecyclerView(getActivity());
        // this config should be no op.
        mLayoutManager = new WrappedLinearLayoutManager(getActivity(),
                mConfig.mOrientation, mConfig.mReverseLayout);
        mLayoutManager.setStackFromEnd(mConfig.mStackFromEnd);
        restored.setLayoutManager(mLayoutManager);
        // use the same adapter for Rect matching
        restored.setAdapter(mTestAdapter);
        restored.onRestoreInstanceState(savedState);

        if (mLoadDataAfterRestore) {
            // add the same items back
            mTestAdapter.resetItemsTo(testItems);
        }

        mPostRestoreOperation.onAfterRestore(mConfig);
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        mLayoutManager.expectLayouts(1);
        setRecyclerView(restored);
        mLayoutManager.waitForLayout(2);
        // calculate prefix here instead of above to include post restore changes
        final String logPrefix = mConfig + "\npostLayout:" + mPostLayoutOperation.describe() +
                "\npostRestore:" + mPostRestoreOperation.describe() + "\n";
        assertEquals(logPrefix + " on saved state, reverse layout should be preserved",
                mConfig.mReverseLayout, mLayoutManager.getReverseLayout());
        assertEquals(logPrefix + " on saved state, orientation should be preserved",
                mConfig.mOrientation, mLayoutManager.getOrientation());
        assertEquals(logPrefix + " on saved state, stack from end should be preserved",
                mConfig.mStackFromEnd, mLayoutManager.getStackFromEnd());
        if (mWaitForLayout) {
            final boolean strictItemEquality = !mLoadDataAfterRestore;
            if (mPostRestoreOperation.shouldLayoutMatch(mConfig)) {
                assertRectSetsEqual(
                        logPrefix + ": on restore, previous view positions should be preserved",
                        before, mLayoutManager.collectChildCoordinates(), strictItemEquality);
            } else {
                assertRectSetsNotEqual(
                        logPrefix
                                + ": on restore with changes, previous view positions should NOT "
                                + "be preserved",
                        before, mLayoutManager.collectChildCoordinates(), strictItemEquality);
            }
            mPostRestoreOperation.onAfterReLayout(mConfig);
        }
    }

    protected static abstract class PostLayoutRunnable {
        private Delegate<WrappedLinearLayoutManager> mLayoutManagerDelegate;
        private Delegate<TestAdapter> mTestAdapterDelegate;
        protected WrappedLinearLayoutManager layoutManager() {
            return mLayoutManagerDelegate.get();
        }
        protected TestAdapter testAdapter() {
            return mTestAdapterDelegate.get();
        }

        abstract void run() throws Throwable;
        void scrollToPosition(final int position) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    layoutManager().scrollToPosition(position);
                }
            });
        }
        void scrollToPositionWithOffset(final int position, final int offset) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    layoutManager().scrollToPositionWithOffset(position, offset);
                }
            });
        }
        abstract String describe();

        @Override
        public String toString() {
            return describe();
        }
    }

    protected static abstract class PostRestoreRunnable {
        private Delegate<WrappedLinearLayoutManager> mLayoutManagerDelegate;
        private Delegate<TestAdapter> mTestAdapterDelegate;
        protected WrappedLinearLayoutManager layoutManager() {
            return mLayoutManagerDelegate.get();
        }
        protected TestAdapter testAdapter() {
            return mTestAdapterDelegate.get();
        }

        void onAfterRestore(Config config) throws Throwable {
        }

        abstract String describe();

        boolean shouldLayoutMatch(Config config) {
            return true;
        }

        void onAfterReLayout(Config config) {

        };

        @Override
        public String toString() {
            return describe();
        }
    }

    private interface Delegate<T> {
        T get();
    }
}
