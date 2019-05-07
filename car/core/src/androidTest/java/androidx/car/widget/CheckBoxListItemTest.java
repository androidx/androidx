/*
 * Copyright 2019 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.car.test.R;
import androidx.car.util.CarUxRestrictionsTestUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tests the layout configuration and checkbox functionality of {@link CheckBoxListItem}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CheckBoxListItemTest {

    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;
    private ListItemAdapter mAdapter;

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

    @Test
    public void testDefaultVisibility_EmptyItemShowsCheckBox() {
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        ViewGroup itemView = (ViewGroup)
                mPagedListView.getRecyclerView().getLayoutManager().getChildAt(0);
        int childCount = itemView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = itemView.getChildAt(i);
            // |view| could be container in view holder, so exempt ViewGroup.
            if (view instanceof CheckBox || view instanceof ViewGroup) {
                assertThat(view.getVisibility(), is(equalTo(View.VISIBLE)));
            } else {
                assertThat("Visibility of view "
                                + mActivity.getResources().getResourceEntryName(view.getId())
                                + " by default should be GONE.",
                        view.getVisibility(), is(equalTo(View.GONE)));
            }
        }
    }

    @Test
    public void testItemIsEnabledByDefault() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).itemView.isEnabled());
    }

    @Test
    public void testDisablingItem() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        item0.setEnabled(false);
        refreshUi();

        assertFalse(getViewHolderAtPosition(0).itemView.isEnabled());
    }

    @Test
    public void testClickableItem_DefaultNotClickable() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertFalse(getViewHolderAtPosition(0).itemView.isClickable());
    }

    @Test
    public void testClickableItem_setClickable() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setClickable(true);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).itemView.isClickable());
    }

    @Test
    public void testClickableItem_ClickingTogglesCheckBox() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setClickable(true);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));

        assertTrue(getViewHolderAtPosition(0).getCompoundButton().isChecked());
    }

    @Test
    public void testCheckBoxStatePersistsOnRebind() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        // CheckBox initially checked.
        item0.setChecked(true);

        setupPagedListView(Collections.singletonList(item0));
        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        toggleChecked(viewHolder.getCompoundButton());

        viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getCompoundButton().isChecked(), is(equalTo(false)));
    }

    @Test
    public void testSetCheckBoxState() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setChecked(true);

        setupPagedListView(Arrays.asList(item0));

        item0.setChecked(false);
        refreshUi();

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getCompoundButton().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getCompoundButton().isChecked(), is(equalTo(false)));
    }

    @Test
    public void testSetCheckBoxStateCallsListener() {
        CompoundButton.OnCheckedChangeListener listener =
                mock(CompoundButton.OnCheckedChangeListener.class);
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setOnCheckedChangeListener(listener);

        setupPagedListView(Collections.singletonList(item0));

        item0.setChecked(true);
        refreshUi();
        verify(listener).onCheckedChanged(any(CompoundButton.class), eq(true));
    }

    @Test
    public void testRefreshingUiDoesNotCallListener() {
        CompoundButton.OnCheckedChangeListener listener =
                mock(CompoundButton.OnCheckedChangeListener.class);
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setOnCheckedChangeListener(listener);

        setupPagedListView(Collections.singletonList(item0));

        refreshUi();
        verify(listener, never()).onCheckedChanged(any(CompoundButton.class), anyBoolean());
    }

    @Test
    public void testSetCheckBoxStateBeforeFirstBindCallsListener() {
        CompoundButton.OnCheckedChangeListener listener =
                mock(CompoundButton.OnCheckedChangeListener.class);
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setOnCheckedChangeListener(listener);
        item0.setChecked(true);

        setupPagedListView(Collections.singletonList(item0));

        verify(listener).onCheckedChanged(any(CompoundButton.class), eq(true));
    }

    @Test
    public void testCheckBoxToggleCallsListener() {
        CompoundButton.OnCheckedChangeListener listener =
                mock(CompoundButton.OnCheckedChangeListener.class);
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setOnCheckedChangeListener(listener);

        setupPagedListView(Collections.singletonList(item0));

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        toggleChecked(viewHolder.getCompoundButton());

        // Expect true because checkbox defaults to false.
        verify(listener).onCheckedChanged(any(CompoundButton.class), eq(true));
    }

    @Test
    public void testSetCheckBoxStateNotDirtyDoesNotCallListener() {
        CompoundButton.OnCheckedChangeListener listener =
                mock(CompoundButton.OnCheckedChangeListener.class);
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setChecked(true);
        item0.setOnCheckedChangeListener(listener);

        setupPagedListView(Collections.singletonList(item0));

        item0.setChecked(true);
        refreshUi();

        verify(listener, never()).onCheckedChanged(any(CompoundButton.class), anyBoolean());
    }

    @Test
    public void testCheckingCheckBox() {
        final boolean[] clicked = {false};
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setOnCheckedChangeListener((button, isChecked) -> {
            // Initial value is false.
            assertTrue(isChecked);
            clicked[0] = true;
        });

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.checkbox_widget)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testDividerVisibility() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setShowCompoundButtonDivider(true);

        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item0.setShowCompoundButtonDivider(false);

        List<CheckBoxListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getCompoundButton().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getCompoundButton().getVisibility(), is(equalTo(View.VISIBLE)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(viewHolder.getCompoundButton().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getCompoundButtonDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testPrimaryActionVisible() {
        CheckBoxListItem largeIcon = new CheckBoxListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        CheckBoxListItem mediumIcon = new CheckBoxListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        CheckBoxListItem smallIcon = new CheckBoxListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        List<CheckBoxListItem> items = Arrays.asList(largeIcon, mediumIcon, smallIcon);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(2).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextVisible() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setTitle("title");

        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setBody("body");

        List<CheckBoxListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getTitle().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getBody().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextStartMarginMatchesPrimaryActionType() {
        CheckBoxListItem largeIcon = new CheckBoxListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        CheckBoxListItem mediumIcon = new CheckBoxListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        CheckBoxListItem smallIcon = new CheckBoxListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        CheckBoxListItem emptyIcon = new CheckBoxListItem(mActivity);
        emptyIcon.setPrimaryActionEmptyIcon();

        CheckBoxListItem noIcon = new CheckBoxListItem(mActivity);
        noIcon.setPrimaryActionNoIcon();

        List<CheckBoxListItem> items = Arrays.asList(
                largeIcon, mediumIcon, smallIcon, emptyIcon, noIcon);
        List<Integer> expectedStartMargin = Arrays.asList(
                R.dimen.car_keyline_4,  // Large icon.
                R.dimen.car_keyline_3,  // Medium icon.
                R.dimen.car_keyline_3,  // Small icon.
                R.dimen.car_keyline_3,  // Empty icon.
                R.dimen.car_keyline_1); // No icon.
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(i);

            int expected = ApplicationProvider.getApplicationContext().getResources()
                    .getDimensionPixelSize(expectedStartMargin.get(i));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getTitle().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getBody().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
        }
    }

    @Test
    public void testItemWithOnlyTitleIsSingleLine() {
        // Only space.
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setTitle(" ");

        // Underscore.
        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setTitle("______");

        CheckBoxListItem item2 = new CheckBoxListItem(mActivity);
        item2.setTitle("ALL UPPER CASE");

        // String wouldn't fit in one line.
        CheckBoxListItem item3 = new CheckBoxListItem(mActivity);
        item3.setTitle(ApplicationProvider.getApplicationContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<CheckBoxListItem> items = Arrays.asList(item0, item1, item2, item3);
        setupPagedListView(items);

        double singleLineHeight =
                ApplicationProvider.getApplicationContext().getResources().getDimension(
                        R.dimen.car_single_line_list_item_height);

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();
        for (int i = 0; i < items.size(); i++) {
            assertThat((double) layoutManager.findViewByPosition(i).getHeight(),
                    is(closeTo(singleLineHeight, 1.0d)));
        }
    }

    @Test
    public void testItemWithBodyTextIsAtLeastDoubleLine() {
        // Only space.
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setBody(" ");

        // Underscore.
        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setBody("____");

        // String wouldn't fit in one line.
        CheckBoxListItem item2 = new CheckBoxListItem(mActivity);
        item2.setBody(ApplicationProvider.getApplicationContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<CheckBoxListItem> items = Arrays.asList(item0, item1, item2);
        setupPagedListView(items);

        final int doubleLineHeight =
                (int) ApplicationProvider.getApplicationContext().getResources().getDimension(
                        R.dimen.car_double_line_list_item_height);

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();
        for (int i = 0; i < items.size(); i++) {
            assertThat(layoutManager.findViewByPosition(i).getHeight(),
                    is(greaterThanOrEqualTo(doubleLineHeight)));
        }
    }

    @Test
    public void testSetPrimaryActionIcon_withIcon() {
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<CheckBoxListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable(), is(notNullValue()));
    }

    @Test
    public void testSetPrimaryActionIcon_withDrawable() {
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setPrimaryActionIcon(
                mActivity.getDrawable(android.R.drawable.sym_def_app_icon),
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<CheckBoxListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable(), is(notNullValue()));
    }

    @Test
    public void testPrimaryIconSizesInIncreasingOrder() {
        CheckBoxListItem small = new CheckBoxListItem(mActivity);
        small.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        CheckBoxListItem medium = new CheckBoxListItem(mActivity);
        medium.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        CheckBoxListItem large = new CheckBoxListItem(mActivity);
        large.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<CheckBoxListItem> items = Arrays.asList(small, medium, large);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder smallVH = getViewHolderAtPosition(0);
        CheckBoxListItem.ViewHolder mediumVH = getViewHolderAtPosition(1);
        CheckBoxListItem.ViewHolder largeVH = getViewHolderAtPosition(2);

        assertThat(largeVH.getPrimaryIcon().getHeight(), is(greaterThan(
                mediumVH.getPrimaryIcon().getHeight())));
        assertThat(mediumVH.getPrimaryIcon().getHeight(), is(greaterThan(
                smallVH.getPrimaryIcon().getHeight())));
    }

    @Test
    public void testLargePrimaryIconHasNoStartMargin() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(0)));
    }

    @Test
    public void testSmallAndMediumPrimaryIconStartMargin() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        List<CheckBoxListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        int expected =
                ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(
                        R.dimen.car_keyline_1);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));
    }

    @Test
    public void testSmallPrimaryIconTopMarginRemainsTheSameRegardlessOfTextLength() {
        final String longText =
                ApplicationProvider.getApplicationContext().getResources().getString(
                        R.string.over_uxr_text_length_limit);

        // Single line item.
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item0.setTitle("one line text");

        // Double line item with one line text.
        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item1.setTitle("one line text");
        item1.setBody("one line text");

        // Double line item with long text.
        CheckBoxListItem item2 = new CheckBoxListItem(mActivity);
        item2.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item2.setTitle("one line text");
        item2.setBody(longText);

        // Body text only - long text.
        CheckBoxListItem item3 = new CheckBoxListItem(mActivity);
        item3.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item3.setBody(longText);

        // Body text only - one line text.
        CheckBoxListItem item4 = new CheckBoxListItem(mActivity);
        item4.setPrimaryActionIcon(
                android.R.drawable.sym_def_app_icon,
                CheckBoxListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item4.setBody("one line text");

        List<CheckBoxListItem> items = Arrays.asList(item0, item1, item2, item3, item4);
        setupPagedListView(items);

        for (int i = 1; i < items.size(); i++) {
            onView(withId(R.id.recycler_view)).perform(scrollToPosition(i));
            // Implementation uses integer division so it may be off by 1 vs centered vertically.
            assertThat((double) getViewHolderAtPosition(i - 1).getPrimaryIcon().getTop(),
                    is(closeTo(
                            (double) getViewHolderAtPosition(i).getPrimaryIcon().getTop(), 1.0d)));
        }
    }

    @Test
    public void testCustomViewBinderBindsLast() {
        final String updatedTitle = "updated title";

        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setTitle("original title");
        item0.addViewBinder((viewHolder) -> viewHolder.getTitle().setText(updatedTitle));

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(updatedTitle)));
    }

    @Test
    public void testCustomViewBinderOnUnusedViewsHasNoEffect() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.addViewBinder((viewHolder) -> viewHolder.getBody().setText("text"));

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBody().getVisibility(), is(equalTo(View.GONE)));
        // Custom binder interacts with body but has no effect.
        // Expect card height to remain single line.
        assertThat((double) viewHolder.itemView.getHeight(), is(closeTo(
                ApplicationProvider.getApplicationContext().getResources().getDimension(
                        R.dimen.car_single_line_list_item_height), 1.0d)));
    }

    @Test
    public void testRevertingViewBinder() throws Throwable {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setBody("one item");
        item0.addViewBinder(
                (viewHolder) -> viewHolder.getBody().setEllipsize(TextUtils.TruncateAt.END),
                (viewHolder -> viewHolder.getBody().setEllipsize(null)));

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Bind view holder to a new item - the customization made by item0 should be reverted.
        CheckBoxListItem item1 = new CheckBoxListItem(mActivity);
        item1.setBody("new item");
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        assertThat(viewHolder.getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testRemovingViewBinder() {
        CheckBoxListItem item0 = new CheckBoxListItem(mActivity);
        item0.setBody("one item");
        ListItem.ViewBinder<CheckBoxListItem.ViewHolder> binder =
                (viewHolder) -> viewHolder.getTitle().setEllipsize(TextUtils.TruncateAt.END);
        item0.addViewBinder(binder);

        assertTrue(item0.removeViewBinder(binder));

        List<CheckBoxListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testUpdateItem() {
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        String title = "updated title";
        item.setTitle(title);

        refreshUi();

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(title)));
    }

    @Test
    public void testUxRestrictionsChange() {
        String longText = mActivity.getString(R.string.over_uxr_text_length_limit);
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setBody(longText);

        setupPagedListView(Arrays.asList(item));

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Default behavior without UXR is unrestricted.
        assertThat(viewHolder.getBody().getText(), is(equalTo(longText)));

        viewHolder.onUxRestrictionsChanged(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();

        // Verify that the body text length is limited.
        assertThat(viewHolder.getBody().getText().length(), is(lessThan(longText.length())));
    }

    @Test
    public void testUxRestrictionsChangesDoNotAlterExistingInputFilters() {
        InputFilter filter = new InputFilter.AllCaps(Locale.US);
        String bodyText = "body_text";
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setBody(bodyText);
        item.addViewBinder(vh -> vh.getBody().setFilters(new InputFilter[]{filter}));

        setupPagedListView(Arrays.asList(item));

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Toggle UX restrictions between fully restricted and unrestricted should not affect
        // existing filters.
        viewHolder.onUxRestrictionsChanged(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));

        viewHolder.onUxRestrictionsChanged(CarUxRestrictionsTestUtils.getBaseline());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));
    }

    @Test
    public void testDisabledItemDisablesViewHolder() {
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setTitle("title");
        item.setBody("body");
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        CheckBoxListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getTitle().isEnabled());
        assertFalse(viewHolder.getBody().isEnabled());
        assertFalse(viewHolder.getCompoundButton().isEnabled());
    }

    @Test
    public void testDisabledItemDoesNotRespondToClick() {
        // Disabled view will not respond to touch event.
        // Current test setup makes it hard to test, since clickChildViewWithId() directly calls
        // performClick() on a view, bypassing the way UI handles disabled state.

        // We are explicitly setting itemView so test it here.
        boolean[] clicked = new boolean[]{false};
        CheckBoxListItem item = new CheckBoxListItem(mActivity);
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));

        assertFalse(clicked[0]);
    }

    private Context getContext() {
        return mActivity;
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> mAdapter.notifyDataSetChanged());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void setupPagedListView(List<CheckBoxListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider(new ArrayList<>(items));
        try {
            mAdapter = new ListItemAdapter(mActivity, provider);
            mActivityRule.runOnUiThread(() -> mPagedListView.setAdapter(mAdapter));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private CheckBoxListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (CheckBoxListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    private void toggleChecked(CompoundButton button) {
        try {
            mActivityRule.runOnUiThread(button::toggle);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
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
