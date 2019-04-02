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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.view.View;

import androidx.car.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the layout configuration in {@link RadioButtonListItem}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RadioButtonListItemTest {
    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Test
    public void testDisableItem() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setEnabled(false);
        setupPagedListView(Arrays.asList(item));

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isEnabled());
    }

    @Test
    public void testSetPrimaryActionIcon_NoIcon() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setPrimaryActionNoIcon();
        item.setTitle("text");

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getLeft(), is(equalTo(0)));
    }

    @Test
    public void testSetPrimaryActionIcon_SmallIconOffset() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
        item.setTitle("text");

        setupPagedListView(Arrays.asList(item));

        View itemView = getViewHolderAtPosition(0).getContainerLayout();
        int expected = itemView.getRight() - itemView.getPaddingRight()
                - ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(
                R.dimen.car_keyline_1);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getRight(), is(equalTo(expected)));
    }

    @Test
    public void testSetPrimaryActionIcon_LargeIconOffset() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);
        item.setTitle("text");

        setupPagedListView(Arrays.asList(item));

        View itemView = getViewHolderAtPosition(0).getContainerLayout();
        int expected = itemView.getRight() - itemView.getPaddingRight()
                - ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(
                R.dimen.car_keyline_1);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getRight(), is(equalTo(expected)));
    }

    @Test
    public void testSetText() {
        CharSequence text = "text";
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setTitle(text);
        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getTitle().getText(), is(equalTo(text)));
    }

    @Test
    public void testSetTextStartMargin_DefaultMargin() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setPrimaryActionIcon(null, RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);
        item.setTitle("text");

        setupPagedListView(Arrays.asList(item));
        int expected = ApplicationProvider.getApplicationContext().getResources()
                .getDimensionPixelSize(R.dimen.car_keyline_3);
        assertThat(getViewHolderAtPosition(0).getTitle().getLeft(), is(equalTo(expected)));
    }

    @Test
    public void testSetTextStartMargin_CustomMargin() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                RadioButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item.setTitle("text");
        item.setTextStartMargin(R.dimen.car_keyline_4);

        setupPagedListView(Arrays.asList(item));
        int expected = ApplicationProvider.getApplicationContext().getResources()
                .getDimensionPixelSize(R.dimen.car_keyline_4);

        assertThat(getViewHolderAtPosition(0).getTitle().getLeft(), is(equalTo(expected)));
    }

    @Test
    public void testSetChecked() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setChecked(true);
        setupPagedListView(Arrays.asList(item));

        assertTrue(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testSetChecked_uncheckIsSyncedToUiState() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setChecked(true);
        setupPagedListView(Arrays.asList(item));

        item.setChecked(false);
        refreshUi();

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testSetShowRadioButtonDivider() {
        RadioButtonListItem show = new RadioButtonListItem(mActivity);
        show.setShowRadioButtonDivider(true);

        setupPagedListView(Arrays.asList(show));

        assertThat(getViewHolderAtPosition(0).getRadioButtonDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSetShowRadioButtonDivider_noDivider() {
        RadioButtonListItem noShow = new RadioButtonListItem(mActivity);
        noShow.setShowRadioButtonDivider(false);

        setupPagedListView(Arrays.asList(noShow));

        assertThat(getViewHolderAtPosition(0).getRadioButtonDivider().getVisibility(),
                is(equalTo(View.GONE)));
    }

    @Test
    public void testRadioButton_DefaultIsUnchecked() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testClickingItemAlwaysCheckRadioButton() {
        boolean[] clicked = new boolean[]{false};

        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        // Set radio button listener, but we will click the item.
        item.setOnCheckedChangeListener((compoundButton, checked) -> clicked[0] = true);
        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, click()));

        assertTrue(getViewHolderAtPosition(0).getRadioButton().isChecked());
        // Verify the listener is also triggered.
        assertTrue(clicked[0]);
    }

    @Test
    public void testUncheckRadioButton() {
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // Programmatically uncheck radio button should be reflected in UI.
        item.setChecked(false);
        refreshUi();

        assertFalse(getViewHolderAtPosition(0).getRadioButton().isChecked());
    }

    @Test
    public void testOnCheckedChangedListener() {
        boolean[] clicked = new boolean[]{false};
        RadioButtonListItem item = new RadioButtonListItem(mActivity);
        item.setOnCheckedChangeListener((buttonView, isChecked) -> clicked[0] = true);
        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.radio_button)));

        assertTrue(clicked[0]);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.getAdapter().notifyDataSetChanged();
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private RadioButtonListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (RadioButtonListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    /**
     * Sets up {@link PagedListView} with given items for testing. The view will be rendered
     * and scrolled to the first item.
     *
     * @param items Items to show in PagedListView.
     */
    private void setupPagedListView(List<? extends ListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider<ListItem.ViewHolder>(
                new ArrayList(items));
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

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specific id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
