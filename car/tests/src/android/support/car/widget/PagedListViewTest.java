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

package android.support.car.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.car.test.R;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SmallTest;
import android.support.test.filters.Suppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for {@link PagedListView}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PagedListViewTest {

    /**
     * Used by {@link TestAdapter} to calculate ViewHolder height so N items appear in one page of
     * {@link PagedListView}. If you need to test behavior under multiple pages, set number of items
     * to ITEMS_PER_PAGE * desired_pages.
     * Actual value does not matter.
     */
    private static final int ITEMS_PER_PAGE = 5;

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    @After
    public void tearDown() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void setUpPagedListView(int itemCount) {
        setUpPagedListView(itemCount, PagedListView.ItemCap.UNLIMITED);
    }

    private void setUpPagedListView(int itemCount, int maxPages) {
        try {
            mActivityRule.runOnUiThread(
                    () -> {
                        mPagedListView.setMaxPages(maxPages);
                        mPagedListView.setAdapter(
                                new TestAdapter(itemCount, mPagedListView.getMeasuredHeight()));
                    });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    @Test
    public void scrollBarIsInvisibleIfItemsDoNotFillOnePage() {
        setUpPagedListView(1 /* itemCount */);

        onView(withId(R.id.paged_scroll_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void pageUpDownButtonIsDisabledOnListEnds() throws Throwable {
        final int itemCount = ITEMS_PER_PAGE * 3;
        setUpPagedListView(itemCount);
        // Initially page_up button is disabled.
        onView(withId(R.id.page_up)).check(matches(not(isEnabled())));

        // Moving to middle of list enables page_up button.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(itemCount / 2));
        onView(withId(R.id.page_up)).check(matches(isEnabled()));

        // Moving to page end, page_down button is disabled.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(itemCount));
        onView(withId(R.id.page_down)).check(matches(not(isEnabled())));
    }

    @Test
    public void testMaxPageGetterSetterDefaultValue() {
        final int maxPages = 2;
        final int defaultMaxPages = 3;

        // setMaxPages
        setUpPagedListView(ITEMS_PER_PAGE, maxPages);
        assertThat(mPagedListView.getMaxPages(), is(equalTo(maxPages)));

        // resetMaxPages
        mPagedListView.resetMaxPages();
        // Max pages is equal to max clicks - 1
        assertThat(mPagedListView.getMaxPages(), is(equalTo(PagedListView.DEFAULT_MAX_CLICKS - 1)));

        // setDefaultMaxPages
        mPagedListView.setDefaultMaxPages(defaultMaxPages);
        mPagedListView.resetMaxPages();
        assertThat(mPagedListView.getMaxPages(), is(equalTo(defaultMaxPages - 1)));
    }

    @Test
    public void setMaxPagesLimitsNumberOfClicks() {
        if (!isAutoDevice()) {
            return;
        }

        IdlingRegistry.getInstance()
                .register(new PagedListViewScrollingIdlingResource(mPagedListView));
        setUpPagedListView(ITEMS_PER_PAGE * 3 /* itemCount */, 2 /* maxPages */);

        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).check(matches(not(isEnabled())));
    }

    @Test
    public void resetMaxPagesToDefaultUnlimitedExtendsList() throws Throwable {
        if (!isAutoDevice()) {
            return;
        }

        IdlingRegistry.getInstance()
                .register(new PagedListViewScrollingIdlingResource(mPagedListView));
        final int itemCount = ITEMS_PER_PAGE * 4;
        setUpPagedListView(itemCount, 2 /* maxPages */);

        // Move to next page - should reach end of list.
        onView(withId(R.id.page_down)).perform(click()).check(matches(not(isEnabled())));

        // After resetting max pages (default unlimited), we scroll to the known total number of
        // items.
        mActivityRule.runOnUiThread(() -> mPagedListView.resetMaxPages());
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(itemCount - 1));

        // Verify the last item that would've been hidden due to max pages is now shown.
        onView(allOf(withId(R.id.text_view), withText(itemText(itemCount - 1))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void scrollbarKeepsItemSnappedToTopOfList() {
        if (!isAutoDevice()) {
            return;
        }

        IdlingRegistry.getInstance()
                .register(new PagedListViewScrollingIdlingResource(mPagedListView));
        // 2.5 so last page is not full
        setUpPagedListView((int) (ITEMS_PER_PAGE * 2.5 /* itemCount */));

        // Going down one page and first item is snapped to top
        onView(withId(R.id.page_down)).perform(click());
        verifyItemSnappedToListTop();

        // Go down another page and we reach the last page.
        onView(withId(R.id.page_down)).perform(click()).check(matches(not(isEnabled())));
        verifyItemSnappedToListTop();
    }

    @Suppress
    @Test
    public void swipeUpKeepsItemSnappedToTopOfList() {
        setUpPagedListView(ITEMS_PER_PAGE * 2 /* itemCount */);

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(1, swipeUp()));

        verifyItemSnappedToListTop();
    }

    @Suppress
    @Test
    public void swipeDownKeepsItemSnappedToTopOfList() throws Throwable {
        setUpPagedListView(ITEMS_PER_PAGE * 2 /* itemCount */);

        // Go down one page, then swipe down (going up).
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(ITEMS_PER_PAGE));
        onView(withId(R.id.recycler_view))
                .perform(actionOnItemAtPosition(ITEMS_PER_PAGE, swipeDown()));

        verifyItemSnappedToListTop();
    }

    @Test
    public void pageUpAndDownMoveSameDistance() {
        if (!isAutoDevice()) {
            return;
        }

        IdlingRegistry.getInstance()
                .register(new PagedListViewScrollingIdlingResource(mPagedListView));
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        // Move down one page so there will be sufficient pages for up and downs.
        onView(withId(R.id.page_down)).perform(click());
        final int topPosition = mPagedListView.getFirstFullyVisibleChildPosition();

        for (int i = 0; i < 3; i++) {
            onView(withId(R.id.page_down)).perform(click());
            onView(withId(R.id.page_up)).perform(click());
        }

        assertThat(mPagedListView.getFirstFullyVisibleChildPosition(), is(equalTo(topPosition)));
    }

    @Test
    public void setItemSpacing() throws Throwable {
        final int itemCount = 3;
        setUpPagedListView(itemCount /* itemCount */);

        // Initial spacing is 0.
        final View[] views = new View[itemCount];
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < itemCount; i++) {
                views[i] = mPagedListView.findViewByPosition(i);
            }
        });
        for (int i = 0; i < itemCount - 1; i++) {
            assertThat(views[i + 1].getTop() - views[i].getBottom(), is(equalTo(0)));
        }

        // Setting item spacing causes layout change.
        // Implicitly wait for layout by making two calls in UI thread.
        final int itemSpacing = 10;
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setItemSpacing(itemSpacing);
        });
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < itemCount; i++) {
                views[i] = mPagedListView.findViewByPosition(i);
            }
        });
        for (int i = 0; i < itemCount - 1; i++) {
            assertThat(views[i + 1].getTop() - views[i].getBottom(), is(equalTo(itemSpacing)));
        }

        // Re-setting spacing back to 0 also works.
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setItemSpacing(0);
        });
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < itemCount; i++) {
                views[i] = mPagedListView.findViewByPosition(i);
            }
        });
        for (int i = 0; i < itemCount - 1; i++) {
            assertThat(views[i + 1].getTop() - views[i].getBottom(), is(equalTo(0)));
        }
    }

    @Test
    @UiThreadTest
    public void testSetScrollBarButtonIcons() throws Throwable {
        // Set up a pagedListView with a large item count to ensure the scroll bar buttons are
        // always showing.
        setUpPagedListView(100 /* itemCount */);

        Drawable upDrawable = mActivity.getDrawable(R.drawable.ic_thumb_up);
        mPagedListView.setUpButtonIcon(upDrawable);

        ImageView upButton = mPagedListView.findViewById(R.id.page_up);
        ViewMatchers.assertThat(upButton.getDrawable().getConstantState(),
                is(equalTo(upDrawable.getConstantState())));

        Drawable downDrawable = mActivity.getDrawable(R.drawable.ic_thumb_down);
        mPagedListView.setDownButtonIcon(downDrawable);

        ImageView downButton = mPagedListView.findViewById(R.id.page_down);
        ViewMatchers.assertThat(downButton.getDrawable().getConstantState(),
                is(equalTo(downDrawable.getConstantState())));
    }

    private static String itemText(int index) {
        return "Data " + index;
    }

    private void verifyItemSnappedToListTop() {
        int firstVisiblePosition = mPagedListView.getFirstFullyVisibleChildPosition();
        if (firstVisiblePosition > 1) {
            int lastInPreviousPagePosition = firstVisiblePosition - 1;
            onView(withText(itemText(lastInPreviousPagePosition)))
                    .check(matches(not(isDisplayed())));
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder>
            implements PagedListView.ItemCap {

        private List<String> mData;
        private int mMaxItems;
        private int mParentHeight;

        TestAdapter(int itemCount, int parentHeight) {
            mData = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                mData.add(itemText(i));
            }
            mParentHeight = parentHeight;
        }

        @Override
        public void setMaxItems(int maxItems) {
            mMaxItems = maxItems;
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
            return mMaxItems > 0 ? Math.min(mData.size(), mMaxItems) : mData.size();
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

    // Registering IdlingResource in @Before method does not work - espresso doesn't actually wait
    // for ViewAction to finish. Shamefully I don't understand the reason. So each method that
    // clicks on button will need to register their own IdlingResource.
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
