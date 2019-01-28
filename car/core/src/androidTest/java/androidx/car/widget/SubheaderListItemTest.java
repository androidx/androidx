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

package androidx.car.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import android.content.pm.PackageManager;

import androidx.car.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the layout configuration in {@link SubheaderListItem}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SubheaderListItemTest {

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    private void setupPagedListView(List<? extends ListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider(
                new ArrayList<>(items));
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setAdapter(new ListItemAdapter(mActivity, provider));
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private SubheaderListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (SubheaderListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    private TextListItem.ViewHolder getTextViewHolderAtPosition(int position) {
        return (TextListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    @Test
    public void testEmptyStartMargin() {
        SubheaderListItem subheader = new SubheaderListItem(mActivity, "text");
        subheader.setTextStartMarginType(SubheaderListItem.TEXT_START_MARGIN_TYPE_NONE);

        TextListItem item = new TextListItem(mActivity);
        item.setTitle("title");
        item.setPrimaryActionNoIcon();

        setupPagedListView(Arrays.asList(subheader, item));

        assertThat(getViewHolderAtPosition(0).getText().getPaddingStart(),
                is(equalTo(getTextViewHolderAtPosition(1).getTitle().getLeft())));
    }

    @Test
    public void testStartMarginMatchesSmallIcon() {
        SubheaderListItem subheader = new SubheaderListItem(mActivity, "text");
        subheader.setTextStartMarginType(SubheaderListItem.TEXT_START_MARGIN_TYPE_SMALL);

        TextListItem item = new TextListItem(mActivity);
        item.setTitle("title");
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        setupPagedListView(Arrays.asList(subheader, item));

        assertThat(getViewHolderAtPosition(0).getText().getPaddingStart(),
                is(equalTo(getTextViewHolderAtPosition(1).getTitle().getLeft())));
    }

    @Test
    public void testStartMarginMatchesLargeIcon() {
        SubheaderListItem subheader = new SubheaderListItem(mActivity, "text");
        subheader.setTextStartMarginType(SubheaderListItem.TEXT_START_MARGIN_TYPE_LARGE);

        TextListItem item = new TextListItem(mActivity);
        item.setTitle("title");
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                    TextListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        setupPagedListView(Arrays.asList(subheader, item));

        assertThat(getViewHolderAtPosition(0).getText().getPaddingStart(),
                is(equalTo(getTextViewHolderAtPosition(1).getTitle().getLeft())));
    }

    @Test
    public void testDisabledItemDisablesViewHolder() {
        SubheaderListItem item = new SubheaderListItem(mActivity, "text");
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        SubheaderListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getText().isEnabled());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetTextThrowsExceptionOnNull() {
        SubheaderListItem item = new SubheaderListItem(mActivity, "text");
        item.setText(null);
    }

}
