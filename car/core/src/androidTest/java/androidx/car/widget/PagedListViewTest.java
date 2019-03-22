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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

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
import java.util.Arrays;
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

    /**
     * Sets up {@link #mPagedListView} with the given number of items and positions where an item
     * should be taller than the containing {@code PagedListView}.
     */
    private void setUpPagedListViewWithLongItems(int itemCount, List<Integer> longItemPositions) {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);

                TestAdapter adapter =
                        new TestAdapter(itemCount, mPagedListView.getHeight());
                adapter.setLongItemPositions(longItemPositions);

                mPagedListView.setAdapter(adapter);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

        // Wait for the UI to lay itself out.
        waitForIdleSync();
    }

    @Test
    public void testScrollBarIsInvisibleIfItemsDoNotFillOnePage() {
        setUpPagedListView(1 /* itemCount */);
        onView(withId(R.id.paged_scroll_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollButtonCallback() {
        int itemCount = ITEMS_PER_PAGE * 3;
        setUpPagedListView(itemCount);

        PagedListView.Callback mockedCallbackOne = mock(PagedListView.Callback.class);
        PagedListView.Callback mockedCallbackTwo = mock(PagedListView.Callback.class);
        PagedListView.Callback mockedCallbackThree = mock(PagedListView.Callback.class);

        mPagedListView.registerCallback(mockedCallbackOne);
        mPagedListView.registerCallback(mockedCallbackTwo);
        mPagedListView.registerCallback(mockedCallbackThree);

        // Move one page down.
        onView(withId(R.id.page_down)).perform(click());
        verify(mockedCallbackOne, times(1)).onScrollDownButtonClicked();
        verify(mockedCallbackTwo, times(1)).onScrollDownButtonClicked();
        verify(mockedCallbackThree, times(1)).onScrollDownButtonClicked();

        // Move one page up.
        onView(withId(R.id.page_up)).perform(click());
        verify(mockedCallbackOne, times(1)).onScrollUpButtonClicked();
        verify(mockedCallbackTwo, times(1)).onScrollUpButtonClicked();
        verify(mockedCallbackThree, times(1)).onScrollUpButtonClicked();

        mPagedListView.unregisterCallback(mockedCallbackOne);
        onView(withId(R.id.page_down)).perform(click());
        verify(mockedCallbackOne, times(1)).onScrollDownButtonClicked();
        verify(mockedCallbackTwo, times(2)).onScrollDownButtonClicked();
        verify(mockedCallbackThree, times(2)).onScrollDownButtonClicked();
    }

    @Test
    public void testMultipleScrollButtonCallback() {
        int itemCount = ITEMS_PER_PAGE * 4;
        setUpPagedListView(itemCount);

        PagedListView.Callback mockedCallback = mock(PagedListView.Callback.class);
        mPagedListView.registerCallback(mockedCallback);

        // Move one page down.
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).perform(click());
        verify(mockedCallback, times(3)).onScrollDownButtonClicked();
    }

    @Test
    public void testReachBottomCallback() {
        int itemCount = ITEMS_PER_PAGE * 2;
        setUpPagedListView(itemCount);

        PagedListView.Callback mockedCallback = mock(PagedListView.Callback.class);
        mPagedListView.registerCallback(mockedCallback);

        // Moving down to bottom of list.
        onView(withId(R.id.page_down)).perform(click());
        onView(withId(R.id.page_down)).perform(click());

        verify(mockedCallback, times(1)).onReachBottom();

        // Moving up should not cause a onReachBottom event.
        onView(withId(R.id.page_up)).perform(click());
        verify(mockedCallback, times(1)).onReachBottom();

        // Move to bottom of list again.
        onView(withId(R.id.page_down)).perform(click());
        verify(mockedCallback, times(2)).onReachBottom();
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

    @LargeTest
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
        mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
        mPagedListView.setAdapter(
                new TestAdapter(/* itemCount= */ 100, mPagedListView.getHeight()));

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
    public void testNoVerticalPaddingOnScrollBar() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.paged_scroll_view))
                .check(matches(withTopPadding(0)))
                .check(matches(withBottomPadding(0)));
    }

    @Test
    public void testDefaultScrollBarTopMargin() {
        Resources res = ApplicationProvider.getApplicationContext().getResources();
        int defaultTopMargin = res.getDimensionPixelSize(R.dimen.car_padding_4);

        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(defaultTopMargin)));
    }

    @Test
    public void testSetScrollbarTopMargin() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int topMargin = 100;
        mActivityRule.runOnUiThread(() -> mPagedListView.setScrollBarTopMargin(topMargin));

        onView(withId(R.id.paged_scroll_view)).check(matches(withTopMargin(topMargin)));
    }

    @Test
    public void testScrollBarThumbShowByDefault() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        onView(withId(R.id.scrollbar_thumb)).check(matches(isDisplayed()));
    }

    @Test
    public void testScrollBarThumbIsHidden() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        mActivityRule.runOnUiThread(() -> mPagedListView.setScrollbarThumbEnabled(false));
        onView(withId(R.id.scrollbar_thumb)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSetGutterNone() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth =
                mPagedListView.findViewById(R.id.paged_scroll_view).getLayoutParams().width;

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.NONE));

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(),
                is(equalTo(scrollBarContainerWidth)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterStart() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.START));

        waitForIdleSync();

        Resources res = ApplicationProvider.getApplicationContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(0)));
    }

    @Test
    public void testSetGutterEnd() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth =
                mPagedListView.findViewById(R.id.paged_scroll_view).getLayoutParams().width;

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.END));
        waitForIdleSync();

        Resources res = ApplicationProvider.getApplicationContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(),
                is(equalTo(scrollBarContainerWidth)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterBoth() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        mActivityRule.runOnUiThread(() -> mPagedListView.setGutter(PagedListView.Gutter.BOTH));
        waitForIdleSync();

        Resources res = ApplicationProvider.getApplicationContext().getResources();
        int gutterSize = res.getDimensionPixelSize(R.dimen.car_margin);

        assertThat(mRecyclerViewLayoutParams.getMarginStart(), is(equalTo(gutterSize)));
        assertThat(mRecyclerViewLayoutParams.getMarginEnd(), is(equalTo(gutterSize)));
    }

    @Test
    public void testSetGutterSizeNone() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth =
                mPagedListView.findViewById(R.id.paged_scroll_view).getLayoutParams().width;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.NONE);
            mPagedListView.setGutterSize(120);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(),
                is(equalTo(scrollBarContainerWidth)));
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

        int scrollBarContainerWidth =
                mPagedListView.findViewById(R.id.paged_scroll_view).getLayoutParams().width;
        int gutterSize = 120;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.END);
            mPagedListView.setGutterSize(gutterSize);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(),
                is(equalTo(scrollBarContainerWidth)));
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
    public void setDefaultScrollBarContainerWidth() {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        Resources res = ApplicationProvider.getApplicationContext().getResources();
        int defaultWidth = res.getDimensionPixelSize(R.dimen.car_margin);

        onView(withId(R.id.paged_scroll_view)).check(matches(withWidth(defaultWidth)));
    }

    @Test
    public void testSetScrollBarContainerWidth() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth = 120;

        mActivityRule.runOnUiThread(
                () -> mPagedListView.setScrollBarContainerWidth(scrollBarContainerWidth));

        onView(withId(R.id.paged_scroll_view)).check(matches(withWidth(scrollBarContainerWidth)));
    }

    @Test
    public void testSetScrollBarContainerWidth_WithGutter() throws Throwable {
        // Just need enough items to ensure the scroll bar is showing.
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int scrollBarContainerWidth = 200;

        mActivityRule.runOnUiThread(() -> {
            mPagedListView.setGutter(PagedListView.Gutter.START);
            mPagedListView.setScrollBarContainerWidth(scrollBarContainerWidth);
        });

        waitForIdleSync();

        assertThat(mRecyclerViewLayoutParams.getMarginStart(),
                is(equalTo(scrollBarContainerWidth)));
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
    public void testGetTopOffset() throws Throwable {
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int topOffset = 50;
        mActivityRule.runOnUiThread(() -> mPagedListView.setListContentTopOffset(topOffset));

        // Wait for the UI to lay itself out.
        waitForIdleSync();

        assertEquals(topOffset, mPagedListView.getListContentTopOffset());
    }

    @Test
    public void testGetTopOffset_NoneSet() {
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        assertEquals(0, mPagedListView.getListContentTopOffset());
    }

    @Test
    public void testGetBottomOffset() throws Throwable {
        setUpPagedListView(ITEMS_PER_PAGE * 10);

        int bottomOffset = 50;
        mActivityRule.runOnUiThread(() -> mPagedListView.setListContentBottomOffset(bottomOffset));

        // Wait for the UI to lay itself out.
        waitForIdleSync();

        assertEquals(bottomOffset, mPagedListView.getListContentBottomOffset());
    }

    @Test
    public void testGetBottomOffset_NoneSet() {
        setUpPagedListView(ITEMS_PER_PAGE * 10);
        assertEquals(0, mPagedListView.getListContentBottomOffset());
    }

    @Test
    public void testPagedDownScrollsOverLongItem_itemEndAlignedToScreenBottom() {
        setUpPagedListViewWithLongItems(/* itemCount= */ 1,
                /* longItemPositions= */ Arrays.asList(0));

        View longItem = assertAndReturnLongItem();

        // Verify long item is at top.
        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(
                mPagedListView.getRecyclerView().getLayoutManager());
        assertThat(orientationHelper.getDecoratedStart(longItem), is(equalTo(0)));
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(mPagedListView.getBottom())));

        // Set a limit to avoid test stuck in non-moving state.
        int limit = 10;
        for (int pageCount = 0; pageCount < limit
                && mPagedListView.mScrollBarView.isDownEnabled();
                pageCount++) {
            onView(withId(R.id.page_down)).perform(click());
        }
        // Verify long item end is aligned to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(mPagedListView.getHeight())));
    }

    @Test
    public void testPageDownScrollsOverLongItem() throws Throwable {
        // Need enough items on both ends of long item so long item is not immediately shown.
        int itemCount = ITEMS_PER_PAGE * 6;

        // Position the long item in the middle.
        int longItemPosition = itemCount / 2;

        setUpPagedListViewWithLongItems(itemCount, Arrays.asList(longItemPosition));

        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(
                mPagedListView.getRecyclerView().getLayoutManager());

        // Scroll to a position where long item is partially visible.
        // Scrolling from top, scrollToPosition() either aligns the pos-1 item to bottom,
        // or scrolls to the center of long item. So we hack a bit by scrolling the distance of one
        // item height over pos-1 item.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(longItemPosition - 1));
        // Scroll by the height of an item so the long item is partially visible.
        mActivityRule.runOnUiThread(() -> mPagedListView.getRecyclerView().scrollBy(0,
                mPagedListView.getRecyclerView().getChildAt(0).getHeight()));

        // Verify long item is partially shown.

        View longItem = assertAndReturnLongItem();
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
    public void testPageUpScrollsOverLongItem() {
        // Need enough items on both ends of long item so long item is not immediately shown.
        int itemCount = ITEMS_PER_PAGE * 6;

        // Position the long item in the middle.
        int longItemPosition = itemCount / 2;

        setUpPagedListViewWithLongItems(itemCount, Arrays.asList(longItemPosition));

        OrientationHelper orientationHelper = OrientationHelper.createVerticalHelper(
                mPagedListView.getRecyclerView().getLayoutManager());

        // Scroll to a position just below the long item.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(longItemPosition + 1));

        // Verify long item is off-screen.
        View longItem = assertAndReturnLongItem();
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(greaterThan(mPagedListView.getRecyclerView().getTop())));

        onView(withId(R.id.page_up)).perform(click());

        // Verify long item is snapped to bottom.
        assertThat(orientationHelper.getDecoratedEnd(longItem),
                is(equalTo(mPagedListView.getHeight())));
        assertThat(orientationHelper.getDecoratedStart(longItem), is(lessThan(0)));

        // Set a limit to avoid test stuck in non-moving state.
        int limit = 10;
        int decoratedStart = orientationHelper.getDecoratedStart(longItem);
        for (int pageCount = 0; pageCount < limit && decoratedStart < 0; pageCount++) {
            onView(withId(R.id.page_up)).perform(click());
            decoratedStart = orientationHelper.getDecoratedStart(longItem);
        }
        // Verify long item top is aligned to top.
        assertThat(orientationHelper.getDecoratedStart(longItem), is(equalTo(0)));
    }

    /**
     * Asserts that there is an item in the current PagedListView whose height is taller than that
     * of the PagedListView. If that item exists, then it is returned; otherwise an
     * {@link IllegalStateException} is thrown.
     *
     * @return An item that is taller than the PagedListView.
     */
    private View assertAndReturnLongItem() {
        for (int i = 0; i < mPagedListView.getRecyclerView().getChildCount(); i++) {
            View item = mPagedListView.getRecyclerView().getChildAt(i);
            if (item.getHeight() > mPagedListView.getHeight()) {
                return item;
            }
        }

        throw new IllegalStateException("No item found that is longer than the height of the "
                + "PagedListView.");
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
