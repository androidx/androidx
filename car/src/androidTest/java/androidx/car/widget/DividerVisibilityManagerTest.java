/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.car.widget;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.car.test.R;
import androidx.recyclerview.widget.RecyclerView;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Unit tests for implementations of {@link PagedListView.DividerVisibilityManager}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public final class DividerVisibilityManagerTest {

    /**
     * Used by {@link TestAdapter} to calculate ViewHolder height so N items appear in one page of
     * {@link PagedListView}. If you need to test behavior under multiple pages, set number of items
     * to ITEMS_PER_PAGE * desired_pages.
     * Actual value does not matter.
     */
    private static final int ITEMS_PER_PAGE = 10;

    @Rule
    public ActivityTestRule<DividerVisibilityManagerTestActivity> mActivityRule =
            new ActivityTestRule<>(DividerVisibilityManagerTestActivity.class);

    private DividerVisibilityManagerTestActivity mActivity;
    private PagedListView mPagedListView;

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view_with_dividers);
    }

    /** Sets up {@link #mPagedListView} with the given number of items. */
    private void setUpPagedListView(int itemCount) {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(
                        new TestAdapter(itemCount, mPagedListView.getMeasuredHeight()));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    @Test
    public void setCustomDividerVisibilityManager() throws Throwable {
        final int itemCount = 8;
        setUpPagedListView(itemCount /* itemCount */);
        RecyclerView.LayoutManager layoutManager =
                mPagedListView.getRecyclerView().getLayoutManager();

        // Fetch divider height.
        final int dividerHeight = InstrumentationRegistry.getContext().getResources()
                .getDimensionPixelSize(R.dimen.car_list_divider_height);


        // Initially, dividers are present between each two items.
        final View[] views = new View[itemCount];
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                views[i] = layoutManager.getChildAt(i);
            }
        });
        for (int i = 0; i < itemCount - 1; i++) {
            assertThat((double) views[i + 1].getTop() - views[i].getBottom(),
                    is(closeTo(2 * (dividerHeight / 2), 1.0f)));
        }


        // Set DividerVisibilityManager on PagedListView.
        final PagedListView.DividerVisibilityManager dvm = new TestDividerVisibilityManager();
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setDividerVisibilityManager(dvm);
        });

        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                views[i] = layoutManager.getChildAt(i);
            }
        });

        for (int i = 0; i < itemCount - 1; i++) {
            int distance = views[i + 1].getTop() - views[i].getBottom();
            if (dvm.shouldHideDivider(i)) {
                assertEquals(distance, 0);
            } else {
                assertThat((double) distance, is(closeTo(2 * (dividerHeight / 2), 1.0f)));
            }
        }
    }

    @Test
    public void testListItemAdapterAsVisibilityManager() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setHideDivider(true);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setHideDivider(false);

        TextListItem item2 = new TextListItem(mActivity);
        item2.setHideDivider(true);

        TextListItem item3 = new TextListItem(mActivity);
        item3.setHideDivider(true);

        // Create and populate ListItemAdapter.
        ListItemProvider provider = new ListItemProvider.ListProvider(Arrays.asList(
                item0, item1, item2, item3));

        ListItemAdapter itemAdapter = new ListItemAdapter(mActivity, provider);
        assertTrue(itemAdapter.shouldHideDivider(0));
        assertFalse(itemAdapter.shouldHideDivider(1));
        assertTrue(itemAdapter.shouldHideDivider(2));
        assertTrue(itemAdapter.shouldHideDivider(3));
    }

    @Test
    public void testSettingItemDividersHidden() throws Throwable {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setHideDivider(true);

        TextListItem item1 = new TextListItem(mActivity);

        ListItemProvider provider = new ListItemProvider.ListProvider(Arrays.asList(item0, item1));
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setAdapter(new ListItemAdapter(mActivity, provider));
        });

        assertThat(item0.shouldHideDivider(), is(true));
        assertThat(item1.shouldHideDivider(), is(false));

        // First verify hiding divider works.
        PagedListView.DividerVisibilityManager dvm = (PagedListView.DividerVisibilityManager)
                mPagedListView.getAdapter();
        assertThat(dvm, is(notNullValue()));
        assertThat(dvm.shouldHideDivider(0), is(true));
        assertThat(dvm.shouldHideDivider(1), is(false));

        // Then verify we can show divider by checking the space between items reserved by
        // divider decorator.
        item0.setHideDivider(false);
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.getAdapter().notifyDataSetChanged();
        });

        assertThat(dvm.shouldHideDivider(0), is(false));
        int upper = mPagedListView.getRecyclerView().getLayoutManager()
                .findViewByPosition(0).getBottom();
        int lower = mPagedListView.getRecyclerView().getLayoutManager()
                .findViewByPosition(1).getTop();
        assertThat(lower - upper, is(greaterThan(0)));
    }

    private class TestDividerVisibilityManager implements PagedListView.DividerVisibilityManager {
        @Override
        public boolean shouldHideDivider(int position) {
            // Hide divider after items at even positions, show after items at odd positions.
            return position % 2 == 0;
        }
    }

    private static String itemText(int index) {
        return "Data " + index;
    }

    /** A base adapter that will handle inflating the test view and binding data to it. */
    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {
        private List<String> mData;
        private int mParentHeight;

        TestAdapter(int itemCount, int parentHeight) {
            mData = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                mData.add(itemText(i));
            }
            mParentHeight = parentHeight;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TestViewHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            // Calculate height for an item so one page fits ITEMS_PER_PAGE items.
            int height = (int) Math.floor(mParentHeight / ITEMS_PER_PAGE);
            holder.itemView.setMinimumHeight(height);
            holder.bind(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    private class TestViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        TestViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.paged_list_item_column_card, parent, false));
            mTextView = itemView.findViewById(R.id.text_view);
        }

        public void bind(String text) {
            mTextView.setText(text);
        }
    }
}
