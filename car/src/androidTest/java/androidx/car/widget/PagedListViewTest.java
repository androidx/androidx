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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.test.R;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

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

    // For tests using GridLayoutManager - assuming each item takes one span, this is essentially
    // number of items per row.
    private static final int SPAN_COUNT = 5;

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
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

    /** Sets up {@link #mPagedListView} with given items. */
    private void setupPagedListView(List<ListItem> items) {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(new ListItemAdapter(mActivity,
                        new ListItemProvider.ListProvider(items)));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    @Test
    public void testScrollBarIsInvisibleIfItemsDoNotFillOnePage() {
        setUpPagedListView(1 /* itemCount */);
        onView(withId(R.id.paged_scroll_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testPageUpButtonDisabledAtTop() {
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
        // 2.5 so last page is not full
        setUpPagedListView((int) (ITEMS_PER_PAGE * 2.5 /* itemCount */));

        // Going down one page and first item is snapped to top
        onView(withId(R.id.page_down)).perform(click());
        verifyItemSnappedToListTop();
    }

    @Test
    public void testLastItemSnappedWhenBottomReached() {
        // 2.5 so last page is not full
        setUpPagedListView((int) (ITEMS_PER_PAGE * 2.5 /* itemCount */));

        // Go down 2 pages so the bottom is reached.
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).perform(click()).check(matches(not(isEnabled())));

        // Check that the last item is completely visible.
        assertEquals(mRecyclerViewLayoutManager.findLastCompletelyVisibleItemPosition(),
                mRecyclerViewLayoutManager.getItemCount() - 1);
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
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        // Move down one page so there will be sufficient pages for up and downs.
        onView(withId(R.id.page_down)).perform(click());

        int topPosition = mRecyclerViewLayoutManager.findFirstVisibleItemPosition();

        for (int i = 0; i < 3; i++) {
            onView(withId(R.id.page_down)).perform(click());
            onView(withId(R.id.page_up)).perform(click());
        }

        assertThat(mRecyclerViewLayoutManager.findFirstVisibleItemPosition(),
                is(equalTo(topPosition)));
    }

    @Test
    public void setItemSpacing() throws Throwable {
        final int itemCount = 3;
        setUpPagedListView(itemCount /* itemCount */);

        // Initial spacing is 0.
        final View[] views = new View[itemCount];
        mActivityRule.runOnUiThread(() -> {
            for (int i = 0; i < mRecyclerViewLayoutManager.getChildCount(); i++) {
                views[i] = mRecyclerViewLayoutManager.getChildAt(i);
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
            for (int i = 0; i < mRecyclerViewLayoutManager.getChildCount(); i++) {
                views[i] = mRecyclerViewLayoutManager.getChildAt(i);
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
            for (int i = 0; i < mRecyclerViewLayoutManager.getChildCount(); i++) {
                views[i] = mRecyclerViewLayoutManager.getChildAt(i);
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

    @Test
    public void testSettingAndResettingScrollbarColor() {
        setUpPagedListView(0);

        final int color = R.color.car_teal_700;

        // Setting non-zero res ID changes color.
        mPagedListView.setScrollbarColor(color);
        assertThat(mPagedListView.mScrollBarView.getScrollbarThumbColor(),
                is(equalTo(InstrumentationRegistry.getContext().getColor(color))));

        // Resets to default color.
        mPagedListView.resetScrollbarColor();
        assertThat(mPagedListView.mScrollBarView.getScrollbarThumbColor(),
                is(equalTo(InstrumentationRegistry.getContext().getColor(
                        R.color.car_scrollbar_thumb))));
    }

    @Test
    public void testSettingScrollbarColorIgnoresDayNightStyle() {
        setUpPagedListView(0);

        final int color = R.color.car_teal_700;
        mPagedListView.setScrollbarColor(color);

        int[] styles = new int[] {
                DayNightStyle.AUTO,
                DayNightStyle.AUTO_INVERSE,
                DayNightStyle.ALWAYS_LIGHT,
                DayNightStyle.ALWAYS_DARK,
                DayNightStyle.FORCE_DAY,
                DayNightStyle.FORCE_NIGHT,
        };

        for (int style : styles) {
            mPagedListView.setDayNightStyle(style);

            assertThat(mPagedListView.mScrollBarView.getScrollbarThumbColor(),
                    is(equalTo(InstrumentationRegistry.getContext().getColor(color))));
        }
    }

    @Test
    public void testNoVerticalPaddingOnScrollBar() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.paged_scroll_view))
                .check(matches(withTopPadding(0)))
                .check(matches(withBottomPadding(0)));
    }

    @Test
    public void testDefaultScrollBarTopMargin() {
        Resources res = InstrumentationRegistry.getContext().getResources();
        int defaultTopMargin = res.getDimensionPixelSize(R.dimen.car_padding_4);

        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(defaultTopMargin)));
    }

    @Test
    public void testSetScrollbarTopMargin() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int topMargin = 100;
        mPagedListView.setScrollBarTopMargin(topMargin);

        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(topMargin)));
    }

    @Test
    public void testSetGutterNone() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.NONE);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterStart() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.START);

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterEnd() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.END);

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterBoth() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.BOTH);

        Resources res = InstrumentationRegistry.getContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterSizeNone() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.NONE);
        mPagedListView.setGutterSize(120);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterSizeStart() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.START);

        int gutterSize = 120;
        mPagedListView.setGutterSize(gutterSize);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterSizeEnd() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.END);

        int gutterSize = 120;
        mPagedListView.setGutterSize(gutterSize);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(0)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterSizeBoth() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mPagedListView.setGutter(PagedListView.Gutter.BOTH);

        int gutterSize = 120;
        mPagedListView.setGutterSize(gutterSize);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void setDefaultScrollBarContainerWidth() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        Resources res = InstrumentationRegistry.getContext().getResources();
        int defaultWidth = res.getDimensionPixelSize(R.dimen.car_margin);

        onView(withId(R.id.paged_scroll_view)).check(matches(withWidth(defaultWidth)));
    }

    @Test
    public void testSetScrollBarContainerWidth() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth = 120;
        mPagedListView.setScrollBarContainerWidth(scrollBarContainerWidth);

        onView(withId(R.id.paged_scroll_view)).check(matches(withWidth(scrollBarContainerWidth)));
    }

    @Test
    public void testTopOffsetInGridLayoutManager() throws Throwable {
        int topOffset = mActivity.getResources().getDimensionPixelSize(R.dimen.car_padding_5);

        // Need enough items to fill the first row.
        setUpPagedListView(SPAN_COUNT * 3);
        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setListContentTopOffset(topOffset);
            mPagedListView.getRecyclerView().setLayoutManager(
                    new GridLayoutManager(mActivity, SPAN_COUNT));
            // Verify only items in first row have top offset. Setting no item spacing to avoid
            // additional offset.
            mPagedListView.setItemSpacing(0);
        });
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));

        for (int i = 0; i < SPAN_COUNT; i++) {
            assertThat(mPagedListView.getRecyclerView().getChildAt(i).getTop(),
                    is(equalTo(topOffset)));

            // i + SPAN_COUNT uses items in second row.
            assertThat(mPagedListView.getRecyclerView().getChildAt(i + SPAN_COUNT).getTop(),
                    is(equalTo(mPagedListView.getRecyclerView().getChildAt(i).getBottom())));
        }
    }

    @Test
    public void testPageDownScrollsOverLongItem() throws Throwable {
        // Verifies that page down button gradually steps over item longer than parent size.
        TextListItem item;
        List<ListItem> items = new ArrayList<>();

        // Need enough items on both ends of long item so long item is not immediately shown.
        int fillerItemCount = ITEMS_PER_PAGE * 6;
        for (int i = 0; i < fillerItemCount; i++) {
            item = new TextListItem(mActivity);
            item.setTitle("title " + i);
            items.add(item);
        }

        int longItemPos = fillerItemCount / 2;
        item = new TextListItem(mActivity);
        item.setBody(mActivity.getResources().getString(R.string.longer_than_screen_size));
        items.add(longItemPos, item);

        item = new TextListItem(mActivity);
        item.setTitle("title add item after long item");
        items.add(item);

        setupPagedListView(items);

        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(
                mPagedListView.getRecyclerView().getLayoutManager());

        // Scroll to a position where long item is partially visible.
        // Scrolling from top, scrollToPosition() either aligns the pos-1 item to bottom,
        // or scrolls to the center of long item. So we hack a bit by scrolling the distance of one
        // item height over pos-1 item.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(longItemPos - 1));
        // Scroll by the height of an item so the long item is partially visible.
        mActivityRule.runOnUiThread(() -> mPagedListView.getRecyclerView().scrollBy(0,
                mPagedListView.getRecyclerView().getChildAt(0).getHeight()));

        // Verify long item is partially shown.
        View longItem = findLongItem();
        assertThat(orientationHelper.getDecoratedStart(longItem),
                is(greaterThan(mPagedListView.getRecyclerView().getTop())));

        onView(withId(R.id.page_down)).perform(click());

        // Verify long item is snapped to top.
        assertThat(orientationHelper.getDecoratedStart(longItem), is(equalTo(0)));
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(mPagedListView.getBottom())));

        // Set a limit to avoid test stuck in non-moving state.
        int limit = 10;
        for (int pageCount = 0; pageCount < limit
                && orientationHelper.getDecoratedEnd(longItem)
                > mPagedListView.getRecyclerView().getBottom();
                pageCount++) {
            onView(withId(R.id.page_down)).perform(click());
        }
        // Verify long item end is aligned to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(mPagedListView.getHeight())));

        onView(withId(R.id.page_down)).perform(click());
        // Verify that the long item is no longer visible; Should be on the next child
        assertThat(orientationHelper.getDecoratedStart(longItem),
                is(lessThan(mPagedListView.getRecyclerView().getTop())));
    }

    @Test
    public void testPageUpScrollsOverLongItem() throws Throwable {
        // Verifies that page down button gradually steps over item longer than parent size.
        TextListItem item;
        List<ListItem> items = new ArrayList<>();

        // Need enough items on both ends of long item so long item is not immediately shown.
        int fillerItemCount = ITEMS_PER_PAGE * 6;
        for (int i = 0; i < fillerItemCount; i++) {
            item = new TextListItem(mActivity);
            item.setTitle("title " + i);
            items.add(item);
        }

        int longItemPos = fillerItemCount / 2;
        item = new TextListItem(mActivity);
        item.setBody(mActivity.getResources().getString(R.string.longer_than_screen_size));
        items.add(longItemPos, item);

        setupPagedListView(items);

        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(
                mPagedListView.getRecyclerView().getLayoutManager());

        // Scroll to a position where long item is partially shown.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(longItemPos + 1));

        // Verify long item is partially shown.
        View longItem = findLongItem();
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(mPagedListView.getRecyclerView().getTop())));

        onView(withId(R.id.page_up)).perform(click());

        // Verify long item is snapped to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(mPagedListView.getHeight())));
        assertThat(orientationHelper.getDecoratedStart(longItem), is(lessThan(0)));

        // Set a limit to avoid test stuck in non-moving state.
        int limit = 10;
        for (int pageCount = 0; pageCount < limit
                && orientationHelper.getDecoratedStart(longItem) < 0;
                pageCount++) {
            onView(withId(R.id.page_up)).perform(click());
        }
        // Verify long item top is aligned to top.
        assertThat(orientationHelper.getDecoratedStart(longItem), is(equalTo(0)));
    }

    private View findLongItem() {
        for (int i = 0; i < mPagedListView.getRecyclerView().getChildCount(); i++) {
            View item = mPagedListView.getRecyclerView().getChildAt(i);
            if (item.getHeight() > mPagedListView.getHeight()) {
                return item;
            }
        }
        return null;
    }

    private static String itemText(int index) {
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

    /**
     * Returns a matcher that matches {@link View}s that have the given top padding.
     *
     * @param topPadding The top padding value to match to.
     */
    @NonNull
    public static Matcher<View> withTopPadding(int topPadding) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with top padding: " + topPadding);
            }

            @Override
            public boolean matchesSafely(View view) {
                return topPadding == view.getPaddingTop();
            }
        };
    }

    /**
     * Returns a matcher that matches {@link View}s that have the given bottom padding.
     *
     * @param bottomPadding The bottom padding value to match to.
     */
    @NonNull
    public static Matcher<View> withBottomPadding(int bottomPadding) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with bottom padding: " + bottomPadding);
            }

            @Override
            public boolean matchesSafely(View view) {
                return bottomPadding == view.getPaddingBottom();
            }
        };
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
