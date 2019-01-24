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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.car.test.R;
import androidx.car.util.CarUxRestrictionsTestUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
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
import java.util.Locale;

/**
 * Tests the layout configuration in {@link ActionListItem}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ActionListItemTest {
    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;
    private ListItemAdapter mAdapter;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
    }

    @Test
    public void testPrimaryActionVisible() {
        ActionListItem largeIcon = new ActionListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        ActionListItem mediumIcon = new ActionListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionListItem smallIcon = new ActionListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        List<ActionListItem> items = Arrays.asList(largeIcon, mediumIcon, smallIcon);
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
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setTitle("title");

        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setBody("body");

        List<ActionListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getTitle().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getBody().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextStartMarginMatchesPrimaryActionType() {
        ActionListItem largeIcon = new ActionListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        ActionListItem mediumIcon = new ActionListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionListItem smallIcon = new ActionListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionListItem emptyIcon = new ActionListItem(mActivity);
        emptyIcon.setPrimaryActionEmptyIcon();

        ActionListItem noIcon = new ActionListItem(mActivity);
        noIcon.setPrimaryActionNoIcon();

        List<ActionListItem> items = Arrays.asList(
                largeIcon, mediumIcon, smallIcon, emptyIcon, noIcon);
        List<Integer> expectedStartMargin = Arrays.asList(
                R.dimen.car_keyline_4,  // Large icon.
                R.dimen.car_keyline_3,  // Medium icon.
                R.dimen.car_keyline_3,  // Small icon.
                R.dimen.car_keyline_3,  // Empty icon.
                R.dimen.car_keyline_1); // No icon.
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(i);

            int expected = ApplicationProvider.getApplicationContext().getResources()
                    .getDimensionPixelSize(expectedStartMargin.get(i));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getTitle().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getBody().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
        }
    }

    @Test
    public void testPrimaryActionButtonVisibility_withDividers_Borderless() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testPrimaryActionButtonVisibility_withDividers() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });
        item.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSecondaryActionButtonVisibility_withDividers_Borderless() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSecondaryActionButtonVisibility_withDividers() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });
        item.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_withDividers_Borderless() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getSecondaryActionDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_withDividers() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { /* Do nothing. */ });
        item.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryActionDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSingleActionButtonVisibility_noDividers_Borderless() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testSingleActionButtonVisibility_noDividers() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });
        item.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_noDividers_Borderless() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });
        item.setSecondaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_noDividers() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setPrimaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });
        item.setSecondaryAction("text", /* showDivider= */ false, v -> { /* Do nothing. */ });
        item.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBorderlessPrimaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedPrimaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getPrimaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderlessSecondaryAction().getVisibility(),
                is(equalTo(View.GONE)));
        assertThat(viewHolder.getBorderedSecondaryAction().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryAction().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSecondaryActionDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testClickInterceptor_ClickableIfOneActionSet() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setEnabled(true);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().isClickable());
    }

    @Test
    public void testClickInterceptor_VisibleIfOneActionSet() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setEnabled(true);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().getVisibility() == View.VISIBLE);
    }

    @Test
    public void testClickInterceptor_ClickableIfTwoActionsSet() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setEnabled(true);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { });
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().isClickable());
    }

    @Test
    public void testClickInterceptor_VisibleIfTwoActionsSet() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setEnabled(true);
        item.setPrimaryAction("text", /* showDivider= */ true, v -> { });
        item.setSecondaryAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().getVisibility() == View.VISIBLE);
    }



    @Test
    public void testItemWithOnlyTitleIsSingleLine() {
        // Only space.
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setTitle(" ");

        // Underscore.
        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setTitle("______");

        ActionListItem item2 = new ActionListItem(mActivity);
        item2.setTitle("ALL UPPER CASE");

        // String wouldn't fit in one line.
        ActionListItem item3 = new ActionListItem(mActivity);
        item3.setTitle(ApplicationProvider.getApplicationContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<ActionListItem> items = Arrays.asList(item0, item1, item2, item3);
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
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setBody(" ");

        // Underscore.
        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setBody("____");

        // String wouldn't fit in one line.
        ActionListItem item2 = new ActionListItem(mActivity);
        item2.setBody(ApplicationProvider.getApplicationContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<ActionListItem> items = Arrays.asList(item0, item1, item2);
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
    public void testPrimaryIconDrawable() {
        Drawable drawable = ApplicationProvider.getApplicationContext().getResources().getDrawable(
                android.R.drawable.sym_def_app_icon, null);

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryActionIcon(drawable,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable().getConstantState()
                .equals(drawable.getConstantState()));
    }

    @Test
    public void testPrimaryIconSizesInIncreasingOrder() {
        ActionListItem small = new ActionListItem(mActivity);
        small.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionListItem medium = new ActionListItem(mActivity);
        medium.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionListItem large = new ActionListItem(mActivity);
        large.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionListItem> items = Arrays.asList(small, medium, large);
        setupPagedListView(items);

        ActionListItem.ViewHolder smallVH = getViewHolderAtPosition(0);
        ActionListItem.ViewHolder mediumVH = getViewHolderAtPosition(1);
        ActionListItem.ViewHolder largeVH = getViewHolderAtPosition(2);

        assertThat(largeVH.getPrimaryIcon().getHeight(), is(greaterThan(
                mediumVH.getPrimaryIcon().getHeight())));
        assertThat(mediumVH.getPrimaryIcon().getHeight(), is(greaterThan(
                smallVH.getPrimaryIcon().getHeight())));
    }

    @Test
    public void testLargePrimaryIconHasNoStartMargin() {
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(0)));
    }

    @Test
    public void testSmallAndMediumPrimaryIconStartMargin() {
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        List<ActionListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        int expected =
                ApplicationProvider.getApplicationContext().getResources().getDimensionPixelSize(
                R.dimen.car_keyline_1);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
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
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item0.setTitle("one line text");

        // Double line item with one line text.
        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item1.setTitle("one line text");
        item1.setBody("one line text");

        // Double line item with long text.
        ActionListItem item2 = new ActionListItem(mActivity);
        item2.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item2.setTitle("one line text");
        item2.setBody(longText);

        // Body text only - long text.
        ActionListItem item3 = new ActionListItem(mActivity);
        item3.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item3.setBody(longText);

        // Body text only - one line text.
        ActionListItem item4 = new ActionListItem(mActivity);
        item4.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item4.setBody("one line text");

        List<ActionListItem> items = Arrays.asList(item0, item1, item2, item3, item4);
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
    public void testItemNotClickableWithNullOnClickListener() {
        ActionListItem item = new ActionListItem(mActivity);
        item.setOnClickListener(null);

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.itemView.isClickable());
    }

    @Test
    public void testClickingSupplementalAction_Borderless() {
        final boolean[] clicked = {false};

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryAction("action", /* showDivider= */ true, v -> clicked[0] = true);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.primary_action_borderless)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testClickingSupplementalAction() {
        final boolean[] clicked = {false};

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryAction("action", /* showDivider= */ true, v -> clicked[0] = true);
        item0.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.primary_action)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testClickingBothSupplementalActions_Borderless() {
        final boolean[] clicked = {false, false};

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryAction("action 1", /* showDivider= */ true, v -> clicked[0] = true);
        item0.setSecondaryAction("action 2", /* showDivider= */ true, v -> clicked[1] = true);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.primary_action_borderless)));
        assertTrue(clicked[0]);
        assertFalse(clicked[1]);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.secondary_action_borderless)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testClickingBothSupplementalActions() {
        final boolean[] clicked = {false, false};

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setPrimaryAction("action 1", /* showDivider= */ true, v -> clicked[0] = true);
        item0.setSecondaryAction("action 2", /* showDivider= */ true, v -> clicked[1] = true);
        item0.setActionBorderless(false);

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.primary_action)));
        assertTrue(clicked[0]);
        assertFalse(clicked[1]);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.secondary_action)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testCustomViewBinderBindsLast() {
        final String updatedTitle = "updated title";

        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setTitle("original title");
        item0.addViewBinder((viewHolder) -> viewHolder.getTitle().setText(updatedTitle));

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(updatedTitle)));
    }

    @Test
    public void testCustomViewBinderOnUnusedViewsHasNoEffect() {
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.addViewBinder((viewHolder) -> viewHolder.getBody().setText("text"));

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBody().getVisibility(), is(equalTo(View.GONE)));
        // Custom binder interacts with body but has no effect.
        // Expect card height to remain single line.
        assertThat((double) viewHolder.itemView.getHeight(), is(closeTo(
                ApplicationProvider.getApplicationContext().getResources().getDimension(
                        R.dimen.car_single_line_list_item_height), 1.0d)));
    }

    @Test
    public void testRevertingViewBinder() throws Throwable {
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setBody("one item");
        item0.addViewBinder(
                (viewHolder) -> viewHolder.getBody().setEllipsize(TextUtils.TruncateAt.END),
                (viewHolder -> viewHolder.getBody().setEllipsize(null)));

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Bind view holder to a new item - the customization made by item0 should be reverted.
        ActionListItem item1 = new ActionListItem(mActivity);
        item1.setBody("new item");
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        assertThat(viewHolder.getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testRemovingViewBinder() {
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setBody("one item");
        ListItem.ViewBinder<ActionListItem.ViewHolder> binder =
                (viewHolder) -> viewHolder.getTitle().setEllipsize(TextUtils.TruncateAt.END);
        item0.addViewBinder(binder);

        assertTrue(item0.removeViewBinder(binder));

        List<ActionListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testNoCarriedOverOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};
        ActionListItem item0 = new ActionListItem(mActivity);
        item0.setOnClickListener(v -> clicked[0] = true);

        setupPagedListView(Arrays.asList(item0));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));
        assertTrue(clicked[0]);

        // item1 does not have onClickListener.
        ActionListItem item1 = new ActionListItem(mActivity);
        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Manually rebind the view holder.
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        // Reset for testing.
        clicked[0] = false;
        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));
        assertFalse(clicked[0]);
    }

    @Test
    public void testUpdateItem() {
        ActionListItem item = new ActionListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        String title = "updated title";
        item.setTitle(title);

        refreshUi();

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(title)));
    }

    @Test
    public void testUxRestrictionsChange() {
        String longText = mActivity.getString(
                R.string.over_uxr_text_length_limit);
        ActionListItem item = new ActionListItem(mActivity);
        item.setBody(longText);

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
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
        String bodyText = "bodytext";
        ActionListItem item = new ActionListItem(mActivity);
        item.setBody(bodyText);
        item.addViewBinder(vh -> vh.getBody().setFilters(new InputFilter[] {filter}));

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

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
        ActionListItem item = new ActionListItem(mActivity);
        item.setOnClickListener(v -> { });
        item.setTitle("title");
        item.setBody("body");
        item.setPrimaryAction("action", false, v -> { });
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        ActionListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getTitle().isEnabled());
        assertFalse(viewHolder.getBody().isEnabled());
        assertFalse(viewHolder.getPrimaryAction().isEnabled());
    }

    @Test
    public void testDisabledItemDoesNotRespondToClick() {
        // Disabled view will not respond to touch event.
        // Current test setup makes it hard to test, since clickChildViewWithId() directly calls
        // performClick() on a view, bypassing the way UI handles disabled state.

        // We are explicitly setting itemView so test it here.
        boolean[] clicked = new boolean[]{false};
        ActionListItem item = new ActionListItem(mActivity);
        item.setOnClickListener(v -> clicked[0] = true);
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));

        assertFalse(clicked[0]);
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mAdapter.notifyDataSetChanged();
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private void setupPagedListView(List<ActionListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider(
                new ArrayList<>(items));
        try {
            mAdapter = new ListItemAdapter(mActivity, provider);
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setAdapter(mAdapter);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

        refreshUi();
    }

    private ActionListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (ActionListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
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
