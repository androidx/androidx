/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.recyclerview.test.R;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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

    private final boolean mFocusOnChild;
    private final boolean mDisableRecovery;

    @Parameterized.Parameters(name = "focusSubChild:{0}, disable:{1}")
    public static List<Object[]> getParams() {
        return Arrays.asList(
                new Object[]{false, false},
                new Object[]{true, false},
                new Object[]{false, true},
                new Object[]{true, true}
        );
    }

    public RecyclerViewFocusRecoveryTest(boolean focusOnChild, boolean disableRecovery) {
        super(false);
        mFocusOnChild = focusOnChild;
        mDisableRecovery = disableRecovery;
    }

    void setupBasic() throws Throwable {
        setupBasic(false);
    }

    void setupBasic(boolean hasStableIds) throws Throwable {
        TestAdapter adapter = new FocusTestAdapter(10);
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
            adapter = new FocusTestAdapter(10);
        }
        mLayoutManager = layoutManager;
        mAdapter = adapter;
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setPreserveFocusAfterLayout(!mDisableRecovery);
        mLayoutManager.expectLayouts(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(1);
    }

    @Test
    public void testFocusRecoveryInChange() throws Throwable {
        setupBasic();
        ((SimpleItemAnimator) (mRecyclerView.getItemAnimator())).setSupportsChangeAnimations(true);
        mLayoutManager.setSupportsPredictive(true);
        final RecyclerView.ViewHolder oldVh = focusVh(3);

        mLayoutManager.expectLayouts(2);
        mAdapter.changeAndNotify(3, 1);
        mLayoutManager.waitForLayout(2);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(3);
                assertFocusTransition(oldVh, newVh);

            }
        });
        mLayoutManager.expectLayouts(1);
    }

    private void assertFocusTransition(RecyclerView.ViewHolder oldVh,
            RecyclerView.ViewHolder newVh) {
        if (mDisableRecovery) {
            assertFocus(newVh, false);
            return;
        }
        assertThat("test sanity", newVh, notNullValue());
        assertThat(oldVh, not(sameInstance(newVh)));
        assertFocus(oldVh, false);
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
        ((SimpleItemAnimator) (mRecyclerView.getItemAnimator())).setSupportsChangeAnimations(true);
        mLayoutManager.setSupportsPredictive(withAnimation);
        final RecyclerView.ViewHolder oldVh = focusVh(3);
        mLayoutManager.expectLayouts(withAnimation ? 2 : 1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                Item item = mAdapter.mItems.get(3);
                item.mType += 2;
                mAdapter.notifyItemChanged(3);
            }
        });
        mLayoutManager.waitForLayout(2);

        RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForAdapterPosition(3);
        assertFocusTransition(oldVh, newVh);
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
        runTestOnUiThread(new Runnable() {
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

        RecyclerView.ViewHolder newVh = mRecyclerView.findViewHolderForItemId(itemId);
        if (changeType) {
            assertFocusTransition(oldVh, newVh);
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
        runTestOnUiThread(new Runnable() {
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
            public void onBindViewHolder(TestViewHolder holder,
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
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.mItems.get(3).mType = TYPE_NO_FOCUS;
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
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
            public void onBindViewHolder(TestViewHolder holder,
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
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.mItems.get(3).mType += 2;
                mAdapter.mItems.get(5).mType += 2;
                mAdapter.notifyDataSetChanged();
            }
        });
        mLayoutManager.waitForLayout(2);
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
        public FocusViewHolder onCreateViewHolder(ViewGroup parent,
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
        public void onBindViewHolder(TestViewHolder holder, int position) {
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
            onBind(item);
        }
    }
}
