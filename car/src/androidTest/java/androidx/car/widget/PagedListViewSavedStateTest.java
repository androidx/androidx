/*
 * Copyright 2017 The Android Open Source Project
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
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.filters.SmallTest;
import android.support.test.filters.Suppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.car.test.R;

/** Unit tests for the ability of the {@link PagedListView} to save state. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PagedListViewSavedStateTest {
    /**
     * Used by {@link TestAdapter} to calculate ViewHolder height so N items appear in one page of
     * {@link PagedListView}. If you need to test behavior under multiple pages, set number of items
     * to ITEMS_PER_PAGE * desired_pages.
     *
     * <p>Actual value does not matter.
     */
    private static final int ITEMS_PER_PAGE = 5;

    /**
     * The total number of items to display in a list. This value just needs to be large enough
     * to ensure the scroll bar shows.
     */
    private static final int TOTAL_ITEMS_IN_LIST = 100;

    private static final int NUM_OF_PAGES = TOTAL_ITEMS_IN_LIST / ITEMS_PER_PAGE;

    @Rule
    public ActivityTestRule<PagedListViewSavedStateActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewSavedStateActivity.class);

    private PagedListViewSavedStateActivity mActivity;
    private PagedListView mPagedListView1;
    private PagedListView mPagedListView2;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mPagedListView1 = mActivity.findViewById(R.id.paged_list_view_1);
        mPagedListView2 = mActivity.findViewById(R.id.paged_list_view_2);

        setUpPagedListView(mPagedListView1);
        setUpPagedListView(mPagedListView2);
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void setUpPagedListView(PagedListView pagedListView) {
        try {
            mActivityRule.runOnUiThread(() -> {
                pagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                pagedListView.setAdapter(new TestAdapter(TOTAL_ITEMS_IN_LIST,
                        pagedListView.getMeasuredHeight()));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    @After
    public void tearDown() {
        for (IdlingResource idlingResource : IdlingRegistry.getInstance().getResources()) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Suppress
    @Test
    public void testPagePositionRememberedOnRotation() {
        LinearLayoutManager layoutManager1 =
                (LinearLayoutManager) mPagedListView1.getRecyclerView().getLayoutManager();
        LinearLayoutManager layoutManager2 =
                (LinearLayoutManager) mPagedListView2.getRecyclerView().getLayoutManager();

        Random random = new Random();
        IdlingRegistry.getInstance().register(new PagedListViewScrollingIdlingResource(
                mPagedListView1, mPagedListView2));

        // Add 1 to this random number to ensure it is a value between 1 and NUM_OF_PAGES.
        int numOfClicks = 2;
        clickPageDownButton(onPagedListView1(), numOfClicks);
        int topPositionOfPagedListView1 =
                layoutManager1.findFirstVisibleItemPosition();

        numOfClicks = 3;
        clickPageDownButton(onPagedListView2(), numOfClicks);
        int topPositionOfPagedListView2 =
                layoutManager2.findFirstVisibleItemPosition();

        // Perform a configuration change by rotating the screen.
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Check that the positions are the same after the change.
        assertEquals(topPositionOfPagedListView1,
                layoutManager1.findFirstVisibleItemPosition());
        assertEquals(topPositionOfPagedListView2,
                layoutManager2.findFirstVisibleItemPosition());
    }

    /** Clicks the page down button on the given PagedListView for the given number of times. */
    private void clickPageDownButton(Matcher<View> pagedListView, int times) {
        for (int i = 0; i < times; i++) {
            onView(allOf(withId(R.id.page_down), pagedListView)).perform(click());
        }
    }


    /** Convenience method for checking that a View is on the first PagedListView. */
    private Matcher<View> onPagedListView1() {
        return isDescendantOfA(withId(R.id.paged_list_view_1));
    }

    /** Convenience method for checking that a View is on the second PagedListView. */
    private Matcher<View> onPagedListView2() {
        return isDescendantOfA(withId(R.id.paged_list_view_2));
    }

    private static String getItemText(int index) {
        return "Data " + index;
    }

    /** An Adapter that ensures that there is {@link #ITEMS_PER_PAGE} displayed. */
    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder>
            implements PagedListView.ItemCap {
        private List<String> mData;
        private int mParentHeight;

        TestAdapter(int itemCount, int parentHeight) {
            mData = new ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                mData.add(getItemText(i));
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
            holder.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public void setMaxItems(int maxItems) {
            // No-op
        }
    }

    /** A ViewHolder that holds a View with a TextView. */
    private class TestViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextView;

        TestViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.paged_list_item_column_card, parent, false));
            mTextView = itemView.findViewById(R.id.text_view);
        }

        public void setText(String text) {
            mTextView.setText(text);
        }
    }

    // Registering IdlingResource in @Before method does not work - espresso doesn't actually wait
    // for ViewAction to finish. So each method that  clicks on button will need to register their
    // own IdlingResource.
    private class PagedListViewScrollingIdlingResource implements IdlingResource {
        private boolean mIsIdle = true;
        private ResourceCallback mResourceCallback;

        PagedListViewScrollingIdlingResource(PagedListView pagedListView1,
                PagedListView pagedListView2) {
            // Ensure the IdlingResource waits for both RecyclerViews to finish their movement.
            pagedListView1.getRecyclerView().addOnScrollListener(mOnScrollListener);
            pagedListView2.getRecyclerView().addOnScrollListener(mOnScrollListener);
        }

        @Override
        public String getName() {
            return PagedListViewScrollingIdlingResource.class.getName();
        }

        @Override
        public boolean isIdleNow() {
            return mIsIdle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback) {
            mResourceCallback = callback;
        }

        private final RecyclerView.OnScrollListener mOnScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(
                            RecyclerView recyclerView, int newState) {
                        super.onScrollStateChanged(recyclerView, newState);

                        // Treat dragging as idle, or Espresso will block itself when
                        // swiping.
                        mIsIdle = (newState == RecyclerView.SCROLL_STATE_IDLE
                                || newState == RecyclerView.SCROLL_STATE_DRAGGING);

                        if (mIsIdle && mResourceCallback != null) {
                            mResourceCallback.onTransitionToIdle();
                        }
                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {}
                };
    }
}
