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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.filters.MediumTest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class only tests the RV's focus recovery logic as focus moves between two views that
 * represent the same item in the adapter. Keeping a focused view visible is up-to-the
 * LayoutManager and all FW LayoutManagers already have tests for it.
 */
@MediumTest
@RunWith(Parameterized.class)
public class RecyclerViewFocusRecoveryTest extends BaseRecyclerViewInstrumentationTest {
    TestLayoutManager mLayoutManager;
    TestAdapter mAdapter;
    int mChildCount = 10;

    // Parameter indicating whether RV's children are simple views (false) or ViewGroups (true).
    private final boolean mFocusOnChild;
    // Parameter indicating whether RV recovers focus after layout is finished.
    private final boolean mDisableRecovery;
    // Parameter indicating whether animation is enabled for the ViewHolder items.
    private final boolean mDisableAnimation;

    @Parameterized.Parameters(name = "focusSubChild:{0},disableRecovery:{1},"
            + "disableAnimation:{2}")
    public static List<Object[]> getParams() {
        return Arrays.asList(
                new Object[]{false, false, true},
                new Object[]{true, false, true},
                new Object[]{false, true, true},
                new Object[]{true, true, true},
                new Object[]{false, false, false},
                new Object[]{true, false, false},
                new Object[]{false, true, false},
                new Object[]{true, true, false}
        );
    }

    public RecyclerViewFocusRecoveryTest(boolean focusOnChild, boolean disableRecovery,
                                         boolean disableAnimation) {
        super(false);
        mFocusOnChild = focusOnChild;
        mDisableRecovery = disableRecovery;
        mDisableAnimation = disableAnimation;
    }

    void setupBasic() throws Throwable {
        setupBasic(false);
    }

    void setupBasic(boolean hasStableIds) throws Throwable {
        TestAdapter adapter = new FocusTestAdapter(mChildCount);
        adapter.setHasStableIds(hasStableIds);
        setupBasic(adapter, null);
    }

    void setupBasic(TestLayoutManager layoutManager) throws Throwable {
        setupBasic(null, layoutManager);
    }

    void setupBasic(TestAdapter adapter) throws Throwable {
        setupBasic(adapter, null);
    }

    void setupBasic(@Nullable TestAdapter adapter, @Nullable TestLayoutManager layoutManager)
            throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        if (layoutManager == null) {
            layoutManager = new FocusLayoutManager();
        }

        if (adapter == null) {
            adapter = new FocusTestAdapter(mChildCount);
        }
        mLayoutManager = layoutManager;
        mAdapter = adapter;
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setPreserveFocusAfterLayout(!mDisableRecovery);
        if (mDisableAnimation) {
            recyclerView.setItemAnimator(null);
        }
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(1);
    }

    @Test
    public void testFocusRecoveryInChange() throws Throwable {
        setupBasic();
        mLayoutManager.setSupportsPredictive(true);
        final RecyclerView.ViewHolder oldVh = focusVh(3);

        mLayoutManager.expectLayouts(mDisableAnimation ? 1 : 2);
        mAdapter.changeAndNotify(3, 1);
        mLayoutManager.waitForLayout(2);
        if (!mDisableAnimation) {
            // waiting for RV's ItemAnimator to finish the animation of the removed item
            waitForAnimations(2);
        }

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(3);
                assertFocusTransition(oldVh, newVh, false);

            }
        });
    }

    @Test
    public void testFocusRecoveryAfterRemovingFocusedChild() throws Throwable {
        setupBasic(true);
        FocusViewHolder fvh = cast(focusVh(4));

        assertThat("test sanity", fvh, notNullValue());
        assertThat("RV should have focus", mRecyclerView.hasFocus(), is(true));

        assertThat("RV should pass the focus down to its children",
                mRecyclerView.isFocused(), is(false));
        assertThat("Viewholder did not receive focus", fvh.itemView.hasFocus(),
                is(true));
        assertThat("Viewholder is not focused", fvh.getViewToFocus().isFocused(),
                is(true));

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // removing focused child
                mAdapter.mItems.remove(4);
                mAdapter.notifyItemRemoved(4);
            }
        });
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            // waiting for RV's ItemAnimator to finish the animation of the removed item
            waitForAnimations(2);
        }
        assertThat("RV should have " + (mChildCount - 1) + " instead of "
                        + mRecyclerView.getChildCount() + " children",
                mChildCount - 1, is(mRecyclerView.getChildCount()));
        assertFocusAfterLayout(4, 0);
    }

    @Test
    public void testFocusRecoveryAfterMovingFocusedChild() throws Throwable {
        setupBasic(true);
        FocusViewHolder fvh = cast(focusVh(3));

        assertThat("test sanity", fvh, notNullValue());
        assertThat("RV should have focus", mRecyclerView.hasFocus(), is(true));

        assertThat("RV should pass the focus down to its children",
                mRecyclerView.isFocused(), is(false));
        assertThat("Viewholder did not receive focus", fvh.itemView.hasFocus(),
                is(true));
        assertThat("Viewholder is not focused", fvh.getViewToFocus().isFocused(),
                is(true));

        mLayoutManager.expectLayouts(1);
        mAdapter.moveAndNotify(3, 1);
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            // waiting for RV's ItemAnimator to finish the animation of the removed item
            waitForAnimations(2);
        }
        assertFocusAfterLayout(1, 1);
    }

    @Test
    public void testFocusRecoveryAfterRemovingLastChild() throws Throwable {
        mChildCount = 1;
        setupBasic(true);
        FocusViewHolder fvh = cast(focusVh(0));

        assertThat("test sanity", fvh, notNullValue());
        assertThat("RV should have focus", mRecyclerView.hasFocus(), is(true));

        assertThat("RV should pass the focus down to its children",
                mRecyclerView.isFocused(), is(false));
        assertThat("Viewholder did not receive focus", fvh.itemView.hasFocus(),
                is(true));
        assertThat("Viewholder is not focused", fvh.getViewToFocus().isFocused(),
                is(true));

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // removing focused child
                mAdapter.mItems.remove(0);
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            // waiting for RV's ItemAnimator to finish the animation of the removed item
            waitForAnimations(2);
        }
        assertThat("RV should have " + (mChildCount - 1) + " instead of "
                        + mRecyclerView.getChildCount() + " children",
                mChildCount - 1, is(mRecyclerView.getChildCount()));
        assertFocusAfterLayout(-1, -1);
    }

    @Test
    public void testFocusRecoveryAfterAddingFirstChild() throws Throwable {
        mChildCount = 0;
        setupBasic(true);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requestFocusOnRV();
            }
        });

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // adding first child
                mAdapter.mItems.add(0, new Item(0, TestAdapter.DEFAULT_ITEM_PREFIX));
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            // waiting for RV's ItemAnimator to finish the animation of the removed item
            waitForAnimations(2);
        }
        assertFocusAfterLayout(0, -1);
    }

    @Test
    public void testFocusRecoveryAfterChangingFocusableFlag() throws Throwable {
        setupBasic(true);
        FocusViewHolder fvh = cast(focusVh(6));

        assertThat("test sanity", fvh, notNullValue());
        assertThat("RV should have focus", mRecyclerView.hasFocus(), is(true));

        assertThat("RV should pass the focus down to its children",
                mRecyclerView.isFocused(), is(false));
        assertThat("Viewholder did not receive focus", fvh.itemView.hasFocus(),
                is(true));
        assertThat("Viewholder is not focused", fvh.getViewToFocus().isFocused(),
                is(true));

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Item item = mAdapter.mItems.get(6);
                item.setFocusable(false);
                mAdapter.notifyItemChanged(6);
            }
        });
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            waitForAnimations(2);
        }
        FocusViewHolder newVh = cast(mRecyclerView.findViewHolderForAdapterPosition(6));
        assertThat("VH should no longer be focusable", newVh.getViewToFocus().isFocusable(),
                is(false));
        assertFocusAfterLayout(7, 0);
    }

    @Test
    public void testFocusRecoveryBeforeLayoutWithFocusBefore() throws Throwable {
        testFocusRecoveryBeforeLayout(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
    }

    @Test
    public void testFocusRecoveryBeforeLayoutWithFocusAfter() throws Throwable {
        testFocusRecoveryBeforeLayout(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    @Test
    public void testFocusRecoveryBeforeLayoutWithFocusBlocked() throws Throwable {
        testFocusRecoveryBeforeLayout(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    @Test
    public void testFocusRecoveryDuringLayoutWithFocusBefore() throws Throwable {
        testFocusRecoveryDuringLayout(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
    }

    @Test
    public void testFocusRecoveryDuringLayoutWithFocusAfter() throws Throwable {
        testFocusRecoveryDuringLayout(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    @Test
    public void testFocusRecoveryDuringLayoutWithFocusBlocked() throws Throwable {
        testFocusRecoveryDuringLayout(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
    }

    /**
     * Tests whether the focus is correctly recovered when requestFocus on RV is called before
     * laying out the children.
     * @throws Throwable
     */
    private void testFocusRecoveryBeforeLayout(int descendantFocusability) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setDescendantFocusability(descendantFocusability);
        mLayoutManager = new FocusLayoutManager();
        mAdapter = new FocusTestAdapter(10);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setPreserveFocusAfterLayout(!mDisableRecovery);
        if (mDisableAnimation) {
            recyclerView.setItemAnimator(null);
        }
        setRecyclerView(recyclerView);
        assertThat("RV should always be focusable", mRecyclerView.isFocusable(), is(true));

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                requestFocusOnRV();
                mRecyclerView.setAdapter(mAdapter);
            }
        });
        mLayoutManager.waitForLayout(1);
        assertFocusAfterLayout(0, -1);
    }

    /**
     * Tests whether the focus is correctly recovered when requestFocus on RV is called during
     * laying out the children.
     * @throws Throwable
     */
    private void testFocusRecoveryDuringLayout(int descendantFocusability) throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setDescendantFocusability(descendantFocusability);
        mLayoutManager = new FocusLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                requestFocusOnRV();
            }
        };
        mAdapter = new FocusTestAdapter(10);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(mLayoutManager);
        if (mDisableAnimation) {
            recyclerView.setItemAnimator(null);
        }
        recyclerView.setPreserveFocusAfterLayout(!mDisableRecovery);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(1);
        assertFocusAfterLayout(0, -1);
    }

    private void requestFocusOnRV() {
        assertThat("RV initially has no focus", mRecyclerView.hasFocus(), is(false));
        assertThat("RV initially is not focused", mRecyclerView.isFocused(), is(false));
        mRecyclerView.requestFocus();
        String msg = !mRecyclerView.isComputingLayout() ? " before laying out the children"
                : " during laying out the children";
        assertThat("RV should have focus after calling requestFocus()" + msg,
                mRecyclerView.hasFocus(), is(true));
        assertThat("RV after calling requestFocus() should become focused" + msg,
                mRecyclerView.isFocused(), is(true));
    }

    /**
     * Asserts whether RV and one of its children have the correct focus flags after the layout is
     * complete. This is normally called once the RV layout is complete after initiating
     * notifyItemChanged.
     * @param focusedChildIndexWhenRecoveryEnabled
     * This index is relevant when mDisableRecovery is false. In that case, it refers to the index
     * of the child that should have focus if the ancestors allow passing down the focus. -1
     * indicates none of the children can receive focus even if the ancestors don't block focus, in
     * which case RV holds and becomes focused.
     * @param focusedChildIndexWhenRecoveryDisabled
     * This index is relevant when mDisableRecovery is true. In that case, it refers to the index
     * of the child that should have focus if the ancestors allow passing down the focus. -1
     * indicates none of the children can receive focus even if the ancestors don't block focus, in
     * which case RV holds and becomes focused.
     */
    private void assertFocusAfterLayout(int focusedChildIndexWhenRecoveryEnabled,
                                        int focusedChildIndexWhenRecoveryDisabled) {
        if (mDisableAnimation && mDisableRecovery) {
            // This case is not quite handled properly at the moment. For now, RV may become focused
            // without re-delivering the focus down to the children. Skip the checks for now.
            return;
        }
        if (mRecyclerView.getChildCount() == 0) {
            assertThat("RV should have focus when it has no children",
                    mRecyclerView.hasFocus(), is(true));
            assertThat("RV should be focused when it has no children",
                    mRecyclerView.isFocused(), is(true));
            return;
        }

        assertThat("RV should still have focus after layout", mRecyclerView.hasFocus(), is(true));
        if ((mDisableRecovery && focusedChildIndexWhenRecoveryDisabled == -1)
                || (!mDisableRecovery && focusedChildIndexWhenRecoveryEnabled == -1)
                || mRecyclerView.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS
                || mRecyclerView.getDescendantFocusability()
                == ViewGroup.FOCUS_BEFORE_DESCENDANTS) {
            FocusViewHolder fvh = cast(mRecyclerView.findViewHolderForAdapterPosition(0));
            String msg1 = " when focus recovery is disabled";
            String msg2 = " when descendant focusability is FOCUS_BLOCK_DESCENDANTS";
            String msg3 = " when descendant focusability is FOCUS_BEFORE_DESCENDANTS";

            assertThat("RV should not pass the focus down to its children"
                            + (mDisableRecovery ? msg1 : (mRecyclerView.getDescendantFocusability()
                                    == ViewGroup.FOCUS_BLOCK_DESCENDANTS ? msg2 : msg3)),
                    mRecyclerView.isFocused(), is(true));
            assertThat("RV's first child should not have focus"
                            + (mDisableRecovery ? msg1 : (mRecyclerView.getDescendantFocusability()
                                    == ViewGroup.FOCUS_BLOCK_DESCENDANTS ? msg2 : msg3)),
                    fvh.itemView.hasFocus(), is(false));
            assertThat("RV's first child should not be focused"
                            + (mDisableRecovery ? msg1 : (mRecyclerView.getDescendantFocusability()
                                    == ViewGroup.FOCUS_BLOCK_DESCENDANTS ? msg2 : msg3)),
                    fvh.getViewToFocus().isFocused(), is(false));
        } else {
            FocusViewHolder fvh = mDisableRecovery
                    ? cast(mRecyclerView.findViewHolderForAdapterPosition(
                            focusedChildIndexWhenRecoveryDisabled)) :
                    (focusedChildIndexWhenRecoveryEnabled != -1
                            ? cast(mRecyclerView.findViewHolderForAdapterPosition(
                                    focusedChildIndexWhenRecoveryEnabled)) :
                    cast(mRecyclerView.findViewHolderForAdapterPosition(0)));

            assertThat("test sanity", fvh, notNullValue());
            assertThat("RV's first child should be focusable", fvh.getViewToFocus().isFocusable(),
                    is(true));
            String msg = " when descendant focusability is FOCUS_AFTER_DESCENDANTS";
            assertThat("RV should pass the focus down to its children after layout" + msg,
                    mRecyclerView.isFocused(), is(false));
            assertThat("RV's child #" + focusedChildIndexWhenRecoveryEnabled + " should have focus"
                            + " after layout" + msg,
                    fvh.itemView.hasFocus(), is(true));
            if (mFocusOnChild) {
                assertThat("Either the ViewGroup or the TextView within the first child of RV"
                                + "should be focused after layout" + msg,
                        fvh.itemView.isFocused() || fvh.getViewToFocus().isFocused(), is(true));
            } else {
                assertThat("RV's first child should be focused after layout" + msg,
                        fvh.getViewToFocus().isFocused(), is(true));
            }

        }
    }

    private void assertFocusTransition(RecyclerView.ViewHolder oldVh,
            RecyclerView.ViewHolder newVh, boolean typeChanged) {
        if (mDisableRecovery) {
            if (mDisableAnimation) {
                return;
            }
            assertFocus(newVh, false);
            return;
        }
        assertThat("test sanity", newVh, notNullValue());
        if (!typeChanged && mDisableAnimation) {
            assertThat(oldVh, sameInstance(newVh));
        } else {
            assertThat(oldVh, not(sameInstance(newVh)));
            assertFocus(oldVh, false);
        }
        assertFocus(newVh, true);
    }

    @Test
    public void testFocusRecoveryInTypeChangeWithPredictive() throws Throwable {
        testFocusRecoveryInTypeChange(true);
    }

    @Test
    public void testFocusRecoveryInTypeChangeWithoutPredictive() throws Throwable {
        testFocusRecoveryInTypeChange(false);
    }

    private void testFocusRecoveryInTypeChange(boolean withAnimation) throws Throwable {
        setupBasic();
        if (!mDisableAnimation) {
            ((SimpleItemAnimator) (mRecyclerView.getItemAnimator()))
                    .setSupportsChangeAnimations(true);
        }
        mLayoutManager.setSupportsPredictive(withAnimation);
        final RecyclerView.ViewHolder oldVh = focusVh(3);
        mLayoutManager.expectLayouts(!mDisableAnimation && withAnimation ? 2 : 1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Item item = mAdapter.mItems.get(3);
                item.mType += 2;
                mAdapter.notifyItemChanged(3);
            }
        });
        mLayoutManager.waitForLayout(2);

        RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(3);
        assertFocusTransition(oldVh, newVh, true);
        assertThat("test sanity", oldVh.getItemViewType(), not(newVh.getItemViewType()));
    }

    @Test
    public void testRecoverAdapterChangeViaStableIdOnDataSetChanged() throws Throwable {
        recoverAdapterChangeViaStableId(false, false);
    }

    @Test
    public void testRecoverAdapterChangeViaStableIdOnSwap() throws Throwable {
        recoverAdapterChangeViaStableId(true, false);
    }

    @Test
    public void testRecoverAdapterChangeViaStableIdOnDataSetChangedWithTypeChange()
            throws Throwable {
        recoverAdapterChangeViaStableId(false, true);
    }

    @Test
    public void testRecoverAdapterChangeViaStableIdOnSwapWithTypeChange() throws Throwable {
        recoverAdapterChangeViaStableId(true, true);
    }

    private void recoverAdapterChangeViaStableId(final boolean swap, final boolean changeType)
            throws Throwable {
        setupBasic(true);
        RecyclerView.ViewHolder oldVh = focusVh(4);
        long itemId = oldVh.getItemId();

        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Item item = mAdapter.mItems.get(4);
                if (changeType) {
                    item.mType += 2;
                }
                if (swap) {
                    mAdapter = new FocusTestAdapter(8);
                    mAdapter.setHasStableIds(true);
                    mAdapter.mItems.add(2, item);
                    mRecyclerView.swapAdapter(mAdapter, false);
                } else {
                    mAdapter.mItems.remove(0);
                    mAdapter.mItems.remove(0);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        mLayoutManager.waitForLayout(1);
        if (!mDisableAnimation) {
            waitForAnimations(2);
        }

        RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForItemId(itemId);
        if (changeType) {
            assertFocusTransition(oldVh, newVh, true);
        } else {
            // in this case we should use the same VH because we have stable ids
            assertThat(oldVh, sameInstance(newVh));
            assertFocus(newVh, true);
        }
    }

    @Test
    public void testDoNotRecoverViaPositionOnSetAdapter() throws Throwable {
        testDoNotRecoverViaPositionOnNewDataSet(new RecyclerViewLayoutTest.AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                mRecyclerView.setAdapter(new FocusTestAdapter(10));
            }
        });
    }

    @Test
    public void testDoNotRecoverViaPositionOnSwapAdapterWithRecycle() throws Throwable {
        testDoNotRecoverViaPositionOnNewDataSet(new RecyclerViewLayoutTest.AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                mRecyclerView.swapAdapter(new FocusTestAdapter(10), true);
            }
        });
    }

    @Test
    public void testDoNotRecoverViaPositionOnSwapAdapterWithoutRecycle() throws Throwable {
        testDoNotRecoverViaPositionOnNewDataSet(new RecyclerViewLayoutTest.AdapterRunnable() {
            @Override
            public void run(TestAdapter adapter) throws Throwable {
                mRecyclerView.swapAdapter(new FocusTestAdapter(10), false);
            }
        });
    }

    public void testDoNotRecoverViaPositionOnNewDataSet(
            final RecyclerViewLayoutTest.AdapterRunnable runnable) throws Throwable {
        setupBasic(false);
        assertThat("test sanity", mAdapter.hasStableIds(), is(false));
        focusVh(4);
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run(mAdapter);
                } catch (Throwable throwable) {
                    postExceptionToInstrumentation(throwable);
                }
            }
        });

        mLayoutManager.waitForLayout(1);
        RecyclerView.ViewHolder otherVh = mRecyclerView.findViewHolderForAdapterPosition(4);
        checkForMainThreadException();
        // even if the VH is re-used, it will be removed-reAdded so focus will go away from it.
        assertFocus("should not recover focus if data set is badly invalid", otherVh, false);

    }

    @Test
    public void testDoNotRecoverIfReplacementIsNotFocusable() throws Throwable {
        final int TYPE_NO_FOCUS = 1001;
        TestAdapter adapter = new FocusTestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (holder.getItemViewType() == TYPE_NO_FOCUS) {
                    cast(holder).setFocusable(false);
                }
            }
        };
        adapter.setHasStableIds(true);
        setupBasic(adapter);
        RecyclerView.ViewHolder oldVh = focusVh(3);
        final long itemId = oldVh.getItemId();
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.mItems.get(3).mType = TYPE_NO_FOCUS;
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
        if (!mDisableAnimation) {
            waitForAnimations(2);
        }
        RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForItemId(itemId);
        assertFocus(newVh, false);
    }

    @NonNull
    private RecyclerView.ViewHolder focusVh(int pos) throws Throwable {
        final RecyclerView.ViewHolder oldVh = mRecyclerView.findViewHolderForAdapterPosition(pos);
        assertThat("test sanity", oldVh, notNullValue());
        requestFocus(oldVh);
        assertFocus("test sanity", oldVh, true);
        getInstrumentation().waitForIdleSync();
        return oldVh;
    }

    @Test
    public void testDoNotOverrideAdapterRequestedFocus() throws Throwable {
        final AtomicLong toFocusId = new AtomicLong(-1);

        FocusTestAdapter adapter = new FocusTestAdapter(10) {
            @Override
            public void onBindViewHolder(@NonNull TestViewHolder holder,
                    int position) {
                super.onBindViewHolder(holder, position);
                if (holder.getItemId() == toFocusId.get()) {
                    try {
                        requestFocus(holder);
                    } catch (Throwable throwable) {
                        postExceptionToInstrumentation(throwable);
                    }
                }
            }
        };
        adapter.setHasStableIds(true);
        toFocusId.set(adapter.mItems.get(3).mId);
        long firstFocusId = toFocusId.get();
        setupBasic(adapter);
        RecyclerView.ViewHolder oldVh = mRecyclerView.findViewHolderForItemId(toFocusId.get());
        assertFocus(oldVh, true);
        toFocusId.set(mAdapter.mItems.get(5).mId);
        mLayoutManager.expectLayouts(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.mItems.get(3).mType += 2;
                mAdapter.mItems.get(5).mType += 2;
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
        if (!mDisableAnimation) {
            waitForAnimations(2);
        }
        RecyclerView.ViewHolder requested = mRecyclerView.findViewHolderForItemId(toFocusId.get());
        assertFocus(oldVh, false);
        assertFocus(requested, true);
        RecyclerView.ViewHolder oldReplacement = mRecyclerView
                .findViewHolderForItemId(firstFocusId);
        assertFocus(oldReplacement, false);
        checkForMainThreadException();
    }

    @Test
    public void testDoNotOverrideLayoutManagerRequestedFocus() throws Throwable {
        final AtomicLong toFocusId = new AtomicLong(-1);
        FocusTestAdapter adapter = new FocusTestAdapter(10);
        adapter.setHasStableIds(true);

        FocusLayoutManager lm = new FocusLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                detachAndScrapAttachedViews(recycler);
                layoutRange(recycler, 0, state.getItemCount());
                RecyclerView.ViewHolder toFocus = mRecyclerView
                        .findViewHolderForItemId(toFocusId.get());
                if (toFocus != null) {
                    try {
                        requestFocus(toFocus);
                    } catch (Throwable throwable) {
                        postExceptionToInstrumentation(throwable);
                    }
                }
                layoutLatch.countDown();
            }
        };

        toFocusId.set(adapter.mItems.get(3).mId);
        long firstFocusId = toFocusId.get();
        setupBasic(adapter, lm);

        RecyclerView.ViewHolder oldVh = mRecyclerView.findViewHolderForItemId(toFocusId.get());
        assertFocus(oldVh, true);
        toFocusId.set(mAdapter.mItems.get(5).mId);
        mLayoutManager.expectLayouts(1);
        requestLayoutOnUIThread(mRecyclerView);
        mLayoutManager.waitForLayout(2);
        RecyclerView.ViewHolder requested = mRecyclerView.findViewHolderForItemId(toFocusId.get());
        assertFocus(oldVh, false);
        assertFocus(requested, true);
        RecyclerView.ViewHolder oldReplacement = mRecyclerView
                .findViewHolderForItemId(firstFocusId);
        assertFocus(oldReplacement, false);
        checkForMainThreadException();
    }

    private void requestFocus(RecyclerView.ViewHolder viewHolder) throws Throwable {
        FocusViewHolder fvh = cast(viewHolder);
        requestFocus(fvh.getViewToFocus(), false);
    }

    private void assertFocus(RecyclerView.ViewHolder viewHolder, boolean hasFocus) {
        assertFocus("", viewHolder, hasFocus);
    }

    private void assertFocus(String msg, RecyclerView.ViewHolder vh, boolean hasFocus) {
        FocusViewHolder fvh = cast(vh);
        assertThat(msg, fvh.getViewToFocus().hasFocus(), is(hasFocus));
    }

    private <T extends FocusViewHolder> T cast(RecyclerView.ViewHolder vh) {
        assertThat(vh, instanceOf(FocusViewHolder.class));
        //noinspection unchecked
        return (T) vh;
    }

    private class FocusTestAdapter extends TestAdapter {

        public FocusTestAdapter(int count) {
            super(count);
        }

        @Override
        public FocusViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            final FocusViewHolder fvh;
            if (mFocusOnChild) {
                fvh = new FocusViewHolderWithChildren(
                        LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.focus_test_item_view, parent, false));
            } else {
                fvh = new SimpleFocusViewHolder(new TextView(parent.getContext()));
            }
            fvh.setFocusable(true);
            return fvh;
        }

        @Override
        public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
            cast(holder).bindTo(mItems.get(position));
        }
    }

    private class FocusLayoutManager extends TestLayoutManager {
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            layoutLatch.countDown();
        }
    }

    private class FocusViewHolderWithChildren extends FocusViewHolder {
        public final ViewGroup root;
        public final ViewGroup parent1;
        public final ViewGroup parent2;
        public final TextView textView;

        public FocusViewHolderWithChildren(View view) {
            super(view);
            root = (ViewGroup) view;
            parent1 = (ViewGroup) root.findViewById(R.id.parent1);
            parent2 = (ViewGroup) root.findViewById(R.id.parent2);
            textView = (TextView) root.findViewById(R.id.text_view);

        }

        @Override
        void setFocusable(boolean focusable) {
            parent1.setFocusableInTouchMode(focusable);
            parent2.setFocusableInTouchMode(focusable);
            textView.setFocusableInTouchMode(focusable);
            root.setFocusableInTouchMode(focusable);

            parent1.setFocusable(focusable);
            parent2.setFocusable(focusable);
            textView.setFocusable(focusable);
            root.setFocusable(focusable);
        }

        @Override
        void onBind(Item item) {
            textView.setText(getText(item));
        }

        @Override
        View getViewToFocus() {
            return textView;
        }
    }

    private class SimpleFocusViewHolder extends FocusViewHolder {

        public SimpleFocusViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        void setFocusable(boolean focusable) {
            itemView.setFocusableInTouchMode(focusable);
            itemView.setFocusable(focusable);
        }

        @Override
        View getViewToFocus() {
            return itemView;
        }

        @Override
        void onBind(Item item) {
            ((TextView) (itemView)).setText(getText(item));
        }
    }

    private abstract class FocusViewHolder extends TestViewHolder {

        public FocusViewHolder(View itemView) {
            super(itemView);
        }

        protected String getText(Item item) {
            return item.mText + "(" + item.mId + ")";
        }

        abstract void setFocusable(boolean focusable);

        abstract View getViewToFocus();

        abstract void onBind(Item item);

        final void bindTo(Item item) {
            mBoundItem = item;
            setFocusable(item.isFocusable());
            onBind(item);
        }
    }
}
