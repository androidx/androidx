/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.car.test.R;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingResource;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for {@link GridLayoutManagerUtils}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public final class GridLayoutManagerUtilsTest {
    /**
     * Used by {@link TestAdapter} to calculate ViewHolder height so N items appear in one page of
     * {@link PagedListView}. If you need to test behavior under multiple pages, set number of items
     * to ITEMS_PER_PAGE * desired_pages.
     * Actual value does not matter.
     */
    private static final int ITEMS_PER_PAGE = 5;

    /**
     * The number of spans in the GridLayoutManager. This corresponds to the number of columns.
     */
    private static final int SPAN_COUNT = 3;

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;
    private GridLayoutManager mGridLayoutManager;

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
        mGridLayoutManager = new GridLayoutManager(mActivity, SPAN_COUNT);

        // Using deprecated Espresso methods instead of calling it on the IdlingRegistry because
        // the latter does not seem to work as reliably. Specifically, on the latter, it does
        // not always register and unregister.
        Espresso.registerIdlingResources(new PagedListViewScrollingIdlingResource(mPagedListView));
    }

    @After
    public void tearDown() {
        for (IdlingResource idlingResource : Espresso.getIdlingResources()) {
            Espresso.unregisterIdlingResources(idlingResource);
        }
    }

    @Test
    public void testIsOnLastRow_oneItem() {
        setUpPagedListView(1);

        // Wait for the UI to lay itself out.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        RecyclerView recyclerView = mPagedListView.getRecyclerView();
        View child = recyclerView.getChildAt(0);
        assertTrue(GridLayoutManagerUtils.isOnLastRow(child, recyclerView));
    }

    @Test
    public void testIsOnLastRow_oneRow() {
        // Initialize the PagedListView with enough items to fill up one row.
        setUpPagedListView(SPAN_COUNT);

        // Wait for the UI to lay itself out.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        RecyclerView recyclerView = mPagedListView.getRecyclerView();

        for (int i = 0; i < mPagedListView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            assertTrue(GridLayoutManagerUtils.isOnLastRow(child, recyclerView));
        }
    }

    @Test
    public void testIsOnLastRow_lastRowNotOnScreen() {
        // Initialize the PagedListView with enough items so the last row is pushed off screen.
        setUpPagedListView(ITEMS_PER_PAGE * 20);

        // Wait for the UI to lay itself out.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        RecyclerView recyclerView = mPagedListView.getRecyclerView();

        // All visible children should not register to be on the last row.
        for (int i = 0; i < mPagedListView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            assertFalse(GridLayoutManagerUtils.isOnLastRow(child, recyclerView));
        }
    }

    @Test
    public void testIsOnLastRow_scrollToLastRow() {
        int itemCount = SPAN_COUNT * 20;
        int lastItemIndex = itemCount - 1;

        // Initialize the PagedListView with enough items so the last row is pushed off screen.
        setUpPagedListView(itemCount);

        // Scroll to the end.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(lastItemIndex));

        RecyclerView recyclerView = mPagedListView.getRecyclerView();

        // Check that the last row returns true.
        for (int i = lastItemIndex; i > lastItemIndex + SPAN_COUNT; i++) {
            View child = recyclerView.getChildAt(i);
            assertTrue(GridLayoutManagerUtils.isOnLastRow(child, recyclerView));
        }
    }

    /** Sets up {@link #mPagedListView} with the given number of items. */
    private void setUpPagedListView(int itemCount) {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.getRecyclerView().setLayoutManager(mGridLayoutManager);
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(
                        new TestAdapter(itemCount, mPagedListView.getMeasuredHeight()));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    /** A base adapter that will handle inflating the test view and binding data to it. */
    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {
        private final List<String> mData;
        private final int mParentHeight;

        TestAdapter(int itemCount, int parentHeight) {
            mData = new ArrayList<>(itemCount);
            for (int i = 0; i < itemCount; i++) {
                mData.add("Data " + i);
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
            super(inflater.inflate(R.layout.grid_list_item, parent, false));
            mTextView = itemView.findViewById(R.id.text_view);
        }

        public void bind(String text) {
            mTextView.setText(text);
        }
    }

    /**
     * An {@link IdlingResource} that will prevent assertions from running while the
     * {@link #mPagedListView} is scrolling.
     */
    private class PagedListViewScrollingIdlingResource implements IdlingResource {
        private boolean mIdle = true;
        private ResourceCallback mResourceCallback;

        PagedListViewScrollingIdlingResource(PagedListView pagedListView) {
            pagedListView.getRecyclerView().addOnScrollListener(
                    new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(
                                RecyclerView recyclerView, int newState) {
                            super.onScrollStateChanged(recyclerView, newState);
                            mIdle = (newState == RecyclerView.SCROLL_STATE_IDLE
                                    // Treat dragging as idle, or Espresso will block itself when
                                    // swiping.
                                    || newState == RecyclerView.SCROLL_STATE_DRAGGING);
                            if (mIdle && mResourceCallback != null) {
                                mResourceCallback.onTransitionToIdle();
                            }
                        }

                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        }
                    });
        }

        @Override
        public String getName() {
            return PagedListViewScrollingIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return mIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mResourceCallback = callback;
        }
    }
}
