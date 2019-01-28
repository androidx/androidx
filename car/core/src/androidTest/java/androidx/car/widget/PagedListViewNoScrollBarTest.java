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
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.test.R;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for a {@link PagedListView} that has scrollbars disabled. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PagedListViewNoScrollBarTest {
    /**
     * Used by {@link TestAdapter} to calculate ViewHolder height so N items appear in one page of
     * {@link PagedListView}. If you need to test behavior under multiple pages, set number of items
     * to ITEMS_PER_PAGE * desired_pages.
     */
    private static final int ITEMS_PER_PAGE = 5;

    @Rule
    public ActivityTestRule<PagedListViewNoScrollBarActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewNoScrollBarActivity.class);

    private PagedListViewNoScrollBarActivity mActivity;
    private PagedListView mPagedListView;
    private ViewGroup.MarginLayoutParams mRecyclerViewLayoutParams;
    private LinearLayoutManager mRecyclerViewLayoutManager;

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
        mRecyclerViewLayoutParams =
                (ViewGroup.MarginLayoutParams) mPagedListView.getRecyclerView().getLayoutParams();
        mRecyclerViewLayoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();

        IdlingRegistry.getInstance()
                .register(new PagedListViewScrollingIdlingResource(mPagedListView));
    }

    @After
    public void tearDown() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    /** Sets up {@link #mPagedListView} with the given number of items. */
    private void setUpPagedListView(int itemCount) {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(
                        new TestAdapter(itemCount, mPagedListView.getHeight()));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

        // Wait for the UI to lay itself out.
        waitForIdleSync();
    }

    @Test
    public void testSetGutterNone() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.NONE));

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterStart() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.START));

        waitForIdleSync();

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterEnd() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.END));
        waitForIdleSync();

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterBoth() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.BOTH));
        waitForIdleSync();

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterSizeNone() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.NONE);
            mPagedListView.setGutterSize(120);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterSizeStart() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int gutterSize = 120;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.START);
            mPagedListView.setGutterSize(gutterSize);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterSizeEnd() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int gutterSize = 120;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.END);
            mPagedListView.setGutterSize(gutterSize);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterSizeBoth() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int gutterSize = 120;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.BOTH);
            mPagedListView.setGutterSize(gutterSize);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetScrollBarContainerWidth_WithGutter() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int gutterSize = 120;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.START);
            mPagedListView.setGutterSize(gutterSize);
            mPagedListView.setScrollBarContainerWidth(200);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
    }

    /**
     * Waits until the main thread is idle. Usually this method is used to wait for views to lay
     * themselves out.
     */
    private void waitForIdleSync() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private static String getItemText(int index) {
        return "Data " + index;
    }

    /**
     * Checks that the first item in the list is completely shown and no part of a previous item
     * is shown.
     */
    private void verifyItemSnappedToListTop() {
        int firstVisiblePosition =
                mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstVisiblePosition > 1) {
            int lastInPreviousPagePosition = firstVisiblePosition - 1;
            onView(withText(getItemText(lastInPreviousPagePosition)))
                    .check(doesNotExist());
        }
    }

    /** A base adapter that will handle inflating the test view and binding data to it. */
    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {
        private final List<Boolean> mIsLongItem;
        private final List<String> mData;
        private int mParentHeight;

        TestAdapter(int itemCount, int parentHeight) {
            mData = new ArrayList<>(itemCount);
            mIsLongItem = new ArrayList<>(itemCount);

            for (int i = 0; i < itemCount; i++) {
                mData.add(getItemText(i));
                mIsLongItem.add(false);
            }
            mParentHeight = parentHeight;
        }

        /**
         * Sets the positions where the item in the list should be taller than the parent
         * PagedListView.
         */
        public void setLongItemPositions(List<Integer> positions) {
            for (int position : positions) {
                mIsLongItem.set(position, true);
            }
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new TestViewHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {
            int itemHeight;
            if (mIsLongItem.get(position)) {
                // Ensure the item is taller than the parent.
                itemHeight = mParentHeight + 100;
            } else {
                // Calculate height for an item so one page fits ITEMS_PER_PAGE items.
                itemHeight = (int) Math.floor(mParentHeight / ITEMS_PER_PAGE);
            }

            holder.itemView.setMinimumHeight(itemHeight);
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

    /**
     * Returns a matcher that matches {@link View}s that have the given width.
     *
     * @param width The width to match to.
     */
    @NonNull
    public static Matcher<View> withWidth(int width) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with width: " + width);
            }

            @Override
            public boolean matchesSafely(View view) {
                return width == view.getLayoutParams().width;
            }
        };
    }
}
