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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeDown;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.car.test.R;

/** Unit tests for {@link PagedListView}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
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

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
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
    public void testScrollBarIsInvisibleIfItemsDoNotFillOnePage() {
        if (!isAutoDevice()) {
            return;
        }

        setUpPagedListView(1 /* itemCount */);
        onView(withId(R.id.paged_scroll_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testPageUpButtonDisabledAtTop() {
        if (!isAutoDevice()) {
            return;
        }

        int itemCount = ITEMS_PER_PAGE * 3;
        setUpPagedListView(itemCount);

        // Initially page_up button is disabled.
        onView(withId(R.id.page_up)).check(matches(not(isEnabled())));

        // Moving down, should enable the up bottom.
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_up)).check(matches(isEnabled()));

        // Move back up; this should disable the up bottom again.
        onView(withId(R.id.page_up)).perform(click())
                .check(matches(not(isEnabled())));
    }

    @Test
    public void testItemSnappedToTopOfListOnScroll() throws InterruptedException {
        if (!isAutoDevice()) {
            return;
        }

        // 2.5 so last page is not full
        setUpPagedListView((int) (ITEMS_PER_PAGE * 2.5 /* itemCount */));

        // Going down one page and first item is snapped to top
        onView(withId(R.id.page_down)).perform(click());
        verifyItemSnappedToListTop();
    }

    @Test
    public void testLastItemSnappedWhenBottomReached() {
        if (!isAutoDevice()) {
            return;
        }

        // 2.5 so last page is not full
        setUpPagedListView((int) (ITEMS_PER_PAGE * 2.5 /* itemCount */));

        // Go down 2 pages so the bottom is reached.
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).perform(click()).check(matches(not(isEnabled())));

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();

        // Check that the last item is completely visible.
        assertEquals(layoutManager.findLastCompletelyVisibleItemPosition(),
                layoutManager.getItemCount() - 1);
    }

    @Test
    public void testSwipeDownKeepsItemSnappedToTopOfList() {
        setUpPagedListView(ITEMS_PER_PAGE * 2 /* itemCount */);

        // Go down one page, then swipe down (going up).
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(ITEMS_PER_PAGE));
        onView(withId(R.id.recycler_view))
                .perform(actionOnItemAtPosition(ITEMS_PER_PAGE, swipeDown()));

        verifyItemSnappedToListTop();
    }

    @Test
    public void testSwipeUpKeepsItemSnappedToTopOfList() {
        setUpPagedListView(ITEMS_PER_PAGE * 2 /* itemCount */);

        // Swipe up (going down).
        onView(withId(R.id.recycler_view))
                .perform(actionOnItemAtPosition(ITEMS_PER_PAGE, swipeUp()));

        verifyItemSnappedToListTop();
    }

    @Test
    public void testPageUpAndDownMoveSameDistance() {
        if (!isAutoDevice()) {
            return;
        }

        setUpPagedListView(ITEMS_PER_PAGE * 10);

        // Move down one page so there will be sufficient pages for up and downs.
        onView(withId(R.id.page_down)).perform(click());

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();

        int topPosition = layoutManager.findFirstVisibleItemPosition();

        for (int i = 0; i < 3; i++) {
            onView(withId(R.id.page_down)).perform(click());
            onView(withId(R.id.page_up)).perform(click());
        }

        assertThat(layoutManager.findFirstVisibleItemPosition(), is(equalTo(topPosition)));
    }

    @Test
    public void setItemSpacing() throws Throwable {
        if (!isAutoDevice()) {
            return;
        }

        final int itemCount = 3;
        setUpPagedListView(itemCount /* itemCount */);
        RecyclerView.LayoutManager layoutManager =
                mPagedListView.getRecyclerView().getLayoutManager();

        // Initial spacing is 0.
        final View[] views = new View[itemCount];
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                views[i] = layoutManager.getChildAt(i);
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
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                views[i] = layoutManager.getChildAt(i);
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
            for (int i = 0; i < layoutManager.getChildCount(); i++) {
                views[i] = layoutManager.getChildAt(i);
            }
        });
        for (int i = 0; i < itemCount - 1; i++) {
            assertThat(views[i + 1].getTop() - views[i].getBottom(), is(equalTo(0)));
        }
    }

    @Test
    @UiThreadTest
    public void testSetScrollBarButtonIcons() throws Throwable {
        if (!isAutoDevice()) {
            return;
        }

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

    @Test
    public void testSettingAndResettingScrollbarColor() {
        setUpPagedListView(0);

        final int color = R.color.car_teal_700;

        // Setting non-zero res ID changes color.
        mPagedListView.setScrollbarColor(color);
        assertThat(((ColorDrawable)
                        mPagedListView.mScrollBarView.mScrollThumb.getBackground()).getColor(),
                is(equalTo(InstrumentationRegistry.getContext().getColor(color))));

        // Resets to default color.
        mPagedListView.resetScrollbarColor();
        assertThat(((ColorDrawable)
                        mPagedListView.mScrollBarView.mScrollThumb.getBackground()).getColor(),
                is(equalTo(InstrumentationRegistry.getContext().getColor(
                        R.color.car_scrollbar_thumb))));
    }

    @Test
    public void testSettingScrollbarColorIgnoresDayNightStyle() {
        setUpPagedListView(0);

        final int color = R.color.car_teal_700;
        mPagedListView.setScrollbarColor(color);

        for (int style : new int[] {DayNightStyle.AUTO, DayNightStyle.AUTO_INVERSE,
                DayNightStyle.FORCE_NIGHT, DayNightStyle.FORCE_DAY}) {
            mPagedListView.setDayNightStyle(style);

            assertThat(((ColorDrawable)
                            mPagedListView.mScrollBarView.mScrollThumb.getBackground()).getColor(),
                    is(equalTo(InstrumentationRegistry.getContext().getColor(color))));
        }
    }

    @Test
    public void testDefaultScrollBarTopMargin() {
        if (!isAutoDevice()) {
            return;
        }

        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(0)));
    }

    @Test
    public void testSetScrollbarTopMargin() {
        if (!isAutoDevice()) {
            return;
        }

        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int topMargin = 100;
        mPagedListView.setScrollBarTopMargin(topMargin);

        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(topMargin)));
    }

    private static String itemText(int index) {
        return "Data " + index;
    }

    /**
     * Checks that the first item in the list is completely shown and no part of a previous item
     * is shown.
     */
    private void verifyItemSnappedToListTop() {
        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();
        int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstVisiblePosition > 1) {
            int lastInPreviousPagePosition = firstVisiblePosition - 1;
            onView(withText(itemText(lastInPreviousPagePosition)))
                    .check(doesNotExist());
        }
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
     * Returns a matcher that matches {@link View}s that have the given top margin.
     *
     * @param topMargin The top margin value to match to.
     */
    @NonNull
    public static Matcher<View> withTopMargin(int topMargin) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with top margin: " + topMargin);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                return topMargin == params.topMargin;
            }
        };
    }
}
