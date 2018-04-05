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
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.CoreMatchers.not;
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
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

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

import androidx.car.test.R;
import androidx.car.utils.CarUxRestrictionsTestUtils;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
* Tests the layout configuration in {@link TextListItem}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TextListItemTest {

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

    private void setupPagedListView(List<TextListItem> items) {
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

    private static void verifyViewIsHidden(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            final int childCount = viewGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                verifyViewIsHidden(viewGroup.getChildAt(i));
            }
        } else {
            assertThat(view.getVisibility(), is(equalTo(View.GONE)));
        }
    }

    private TextListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (TextListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(
                position);
    }

    @Test
    public void testEmptyItemHidesAllViews() {
        TextListItem item = new TextListItem(mActivity);
        setupPagedListView(Arrays.asList(item));
        verifyViewIsHidden(mPagedListView.getRecyclerView().getLayoutManager().getChildAt(0));
    }

    @Test
    public void testPrimaryActionVisible() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);

        List<TextListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextVisible() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setTitle("title");

        TextListItem item1 = new TextListItem(mActivity);
        item1.setBody("body");

        List<TextListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getTitle().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getBody().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSupplementalActionVisible() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setAction("text", true, v -> { /* Do nothing. */ });


        TextListItem item2 = new TextListItem(mActivity);
        item2.setActions("text", true, v -> { /* Do nothing. */ },
                        "text", true, v -> { /* Do nothing. */ });

        List<TextListItem> items = Arrays.asList(item0, item1, item2);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSupplementalIcon().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSupplementalIconDivider().getVisibility(),
                is(equalTo(View.VISIBLE)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.VISIBLE)));

        viewHolder = getViewHolderAtPosition(2);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2Divider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSetSupplementalActionWithDrawable() {
        Drawable drawable = mActivity.getDrawable(android.R.drawable.sym_def_app_icon);
        TextListItem item = new TextListItem(mActivity);
        item.setSupplementalIcon(drawable, true);

        setupPagedListView(Arrays.asList(item));

        assertThat(getViewHolderAtPosition(0).getSupplementalIcon().getDrawable(),
                is(equalTo(drawable)));
    }

    @Test
    public void testSwitchVisibleAndCheckedState() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setSwitch(true, true, null);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setSwitch(false, true, null);

        List<TextListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitch().isChecked(), is(equalTo(true)));
        assertThat(viewHolder.getSwitchDivider().getVisibility(), is(equalTo(View.VISIBLE)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitch().isChecked(), is(equalTo(false)));
        assertThat(viewHolder.getSwitchDivider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSetSwitchState() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setSwitch(true, true, null);

        setupPagedListView(Arrays.asList(item0));

        item0.setSwitchState(false);

        refreshUi();

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitch().isChecked(), is(equalTo(false)));
    }

    @Test
    public void testSetSwitchStateHasNoEffectIfSwitchIsNotEnabled() {
        TextListItem item0 = new TextListItem(mActivity);
        setupPagedListView(Arrays.asList(item0));

        item0.setSwitchState(false);

        refreshUi();

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSwitch().getVisibility(), is(not(equalTo(View.VISIBLE))));
    }

    @Test
    public void testDividersAreOptional() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, false);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setAction("text", false, v -> { /* Do nothing. */ });

        TextListItem item2 = new TextListItem(mActivity);
        item2.setActions("text", false, v -> { /* Do nothing. */ },
                "text", false, v -> { /* Do nothing. */ });

        TextListItem item3 = new TextListItem(mActivity);
        item3.setSwitch(true, false, null);

        List<TextListItem> items = Arrays.asList(item0, item1, item2, item3);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSupplementalIcon().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSupplementalIconDivider().getVisibility(),
                is(equalTo(View.GONE)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.GONE)));

        viewHolder = getViewHolderAtPosition(2);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getAction2().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2Divider().getVisibility(), is(equalTo(View.GONE)));

        viewHolder = getViewHolderAtPosition(3);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitchDivider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testTextStartMarginMatchesPrimaryActionType() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);

        TextListItem item1 = new TextListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);

        TextListItem item2 = new TextListItem(mActivity);
        item2.setPrimaryActionEmptyIcon();

        TextListItem item3 = new TextListItem(mActivity);
        item3.setPrimaryActionNoIcon();

        List<TextListItem> items = Arrays.asList(item0, item1, item2, item3);
        List<Integer> expectedStartMargin = Arrays.asList(R.dimen.car_keyline_4,
                R.dimen.car_keyline_3, R.dimen.car_keyline_3, R.dimen.car_keyline_1);
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(i);

            int expected = InstrumentationRegistry.getContext().getResources()
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
        TextListItem item0 = new TextListItem(mActivity);
        item0.setTitle(" ");

        // Underscore.
        TextListItem item1 = new TextListItem(mActivity);
        item1.setTitle("______");

        TextListItem item2 = new TextListItem(mActivity);
        item2.setTitle("ALL UPPER CASE");

        // String wouldn't fit in one line.
        TextListItem item3 = new TextListItem(mActivity);
        item3.setTitle(InstrumentationRegistry.getContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<TextListItem> items = Arrays.asList(item0, item1, item2, item3);
        setupPagedListView(items);

        double singleLineHeight = InstrumentationRegistry.getContext().getResources().getDimension(
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
        TextListItem item0 = new TextListItem(mActivity);
        item0.setBody(" ");

        // Underscore.
        TextListItem item1 = new TextListItem(mActivity);
        item1.setBody("____");

        // String wouldn't fit in one line.
        TextListItem item2 = new TextListItem(mActivity);
        item2.setBody(InstrumentationRegistry.getContext().getResources().getString(
                R.string.over_uxr_text_length_limit));

        List<TextListItem> items = Arrays.asList(item0, item1, item2);
        setupPagedListView(items);

        final int doubleLineHeight =
                (int) InstrumentationRegistry.getContext().getResources().getDimension(
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
        Drawable drawable = InstrumentationRegistry.getContext().getResources().getDrawable(
                android.R.drawable.sym_def_app_icon, null);

        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(drawable, true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable().getConstantState()
                .equals(drawable.getConstantState()));
    }

    @Test
    public void testLargePrimaryIconHasNoStartMargin() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(0)));
    }

    @Test
    public void testSmallPrimaryIconStartMargin() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        int expected = InstrumentationRegistry.getContext().getResources().getDimensionPixelSize(
                R.dimen.car_keyline_1);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));
    }

    @Test
    public void testSmallPrimaryIconTopMarginRemainsTheSameRegardlessOfTextLength() {
        final String longText = InstrumentationRegistry.getContext().getResources().getString(
                R.string.over_uxr_text_length_limit);

        // Single line item.
        TextListItem item0 = new TextListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        item0.setTitle("one line text");

        // Double line item with one line text.
        TextListItem item1 = new TextListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        item1.setTitle("one line text");
        item1.setBody("one line text");

        // Double line item with long text.
        TextListItem item2 = new TextListItem(mActivity);
        item2.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        item2.setTitle("one line text");
        item2.setBody(longText);

        // Body text only - long text.
        TextListItem item3 = new TextListItem(mActivity);
        item3.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        item3.setBody(longText);

        // Body text only - one line text.
        TextListItem item4 = new TextListItem(mActivity);
        item4.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        item4.setBody("one line text");

        List<TextListItem> items = Arrays.asList(item0, item1, item2, item3, item4);
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
    public void testClickingPrimaryActionIsSeparateFromSupplementalAction() {
        final boolean[] clicked = {false, false};

        TextListItem item0 = new TextListItem(mActivity);
        item0.setOnClickListener(v -> clicked[0] = true);
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                v -> clicked[1] = true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        assertTrue(clicked[0]);
        assertFalse(clicked[1]);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.supplemental_icon)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testClickingSupplementalIcon() {
        final boolean[] clicked = {false};

        TextListItem item0 = new TextListItem(mActivity);
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                v -> clicked[0] = true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.supplemental_icon)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testSupplementalIconWithoutClickListenerIsNotClickable() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setSupplementalIcon(android.R.drawable.sym_def_app_icon, true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getSupplementalIcon().isClickable());
    }

    @Test
    public void testCheckingSwitch() {
        final boolean[] clicked = {false, false};

        TextListItem item0 = new TextListItem(mActivity);
        item0.setSwitch(false, false, (button, isChecked) -> {
            // Initial value is false.
            assertTrue(isChecked);
            clicked[0] = true;
        });

        TextListItem item1 = new TextListItem(mActivity);
        item1.setSwitch(true, false, (button, isChecked) -> {
            // Initial value is true.
            assertFalse(isChecked);
            clicked[1] = true;
        });

        List<TextListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.switch_widget)));
        assertTrue(clicked[0]);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(1, clickChildViewWithId(R.id.switch_widget)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testClickingSupplementalAction() {
        final boolean[] clicked = {false};

        TextListItem item0 = new TextListItem(mActivity);
        item0.setAction("action", true, v -> clicked[0] = true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.action1)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testClickingBothSupplementalActions() {
        final boolean[] clicked = {false, false};

        TextListItem item0 = new TextListItem(mActivity);
        item0.setActions("action 1", true, v -> clicked[0] = true,
                        "action 2", true, v -> clicked[1] = true);

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.action1)));
        assertTrue(clicked[0]);
        assertFalse(clicked[1]);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.action2)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testCustomViewBinderBindsLast() {
        final String updatedTitle = "updated title";

        TextListItem item0 = new TextListItem(mActivity);
        item0.setTitle("original title");
        item0.addViewBinder((viewHolder) -> viewHolder.getTitle().setText(updatedTitle));

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(updatedTitle)));
    }

    @Test
    public void testCustomViewBinderOnUnusedViewsHasNoEffect() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.addViewBinder((viewHolder) -> viewHolder.getBody().setText("text"));

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBody().getVisibility(), is(equalTo(View.GONE)));
        // Custom binder interacts with body but has no effect.
        // Expect card height to remain single line.
        assertThat((double) viewHolder.itemView.getHeight(), is(closeTo(
                InstrumentationRegistry.getContext().getResources().getDimension(
                        R.dimen.car_single_line_list_item_height), 1.0d)));
    }

    @Test
    public void testRevertingViewBinder() throws Throwable {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setBody("one item");
        item0.addViewBinder(
                (viewHolder) -> viewHolder.getBody().setEllipsize(TextUtils.TruncateAt.END),
                (viewHolder -> viewHolder.getBody().setEllipsize(null)));

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Bind view holder to a new item - the customization made by item0 should be reverted.
        TextListItem item1 = new TextListItem(mActivity);
        item1.setBody("new item");
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        assertThat(viewHolder.getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testRemovingViewBinder() {
        TextListItem item0 = new TextListItem(mActivity);
        item0.setBody("one item");
        ListItem.ViewBinder<TextListItem.ViewHolder> binder =
                (viewHolder) -> viewHolder.getTitle().setEllipsize(TextUtils.TruncateAt.END);
        item0.addViewBinder(binder);

        assertTrue(item0.removeViewBinder(binder));

        List<TextListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testSettingTitleOrBodyAsPrimaryText() {
        // Create 2 items, one with Title as primary (default) and one with Body.
        // The primary text, regardless of view, should have consistent look (as primary).
        TextListItem item0 = new TextListItem(mActivity);
        item0.setTitle("title");
        item0.setBody("body");

        TextListItem item1 = new TextListItem(mActivity);
        item1.setTitle("title");
        item1.setBody("body", true);

        List<TextListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        TextListItem.ViewHolder titlePrimary = getViewHolderAtPosition(0);
        TextListItem.ViewHolder bodyPrimary = getViewHolderAtPosition(1);
        assertThat(titlePrimary.getTitle().getTextSize(),
                is(equalTo(bodyPrimary.getBody().getTextSize())));
        assertThat(titlePrimary.getTitle().getTextColors(),
                is(equalTo(bodyPrimary.getBody().getTextColors())));
    }

    @Test
    public void testNoCarriedOverLayoutParamsForTextView() throws Throwable {
        TextListItem singleLine = new TextListItem(mActivity);
        singleLine.setTitle("title");

        setupPagedListView(Arrays.asList(singleLine));

        // Manually rebind the view holder of a single line item to a double line item.
        TextListItem doubleLine = new TextListItem(mActivity);
        doubleLine.setTitle("title");
        doubleLine.setBody("body");
        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        mActivityRule.runOnUiThread(() -> doubleLine.bind(viewHolder));

        RelativeLayout.LayoutParams titleLayoutParams =
                (RelativeLayout.LayoutParams) viewHolder.getTitle().getLayoutParams();
        RelativeLayout.LayoutParams bodyLayoutParams =
                (RelativeLayout.LayoutParams) viewHolder.getTitle().getLayoutParams();
        assertThat(titleLayoutParams.getRule(RelativeLayout.CENTER_VERTICAL), is(equalTo(0)));
        assertThat(bodyLayoutParams.getRule(RelativeLayout.CENTER_VERTICAL), is(equalTo(0)));
    }

    @Test
    public void testNoCarriedOverLayoutParamsForPrimaryIcon() throws Throwable {
        TextListItem smallIcon = new TextListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false);
        smallIcon.setBody("body");  // Small icon of items with body text should use top margin.
        setupPagedListView(Arrays.asList(smallIcon));

        // Manually rebind the view holder.
        TextListItem largeIcon = new TextListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true);
        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        mActivityRule.runOnUiThread(() -> largeIcon.bind(viewHolder));

        RelativeLayout.LayoutParams iconLayoutParams =
                (RelativeLayout.LayoutParams) viewHolder.getPrimaryIcon().getLayoutParams();
        assertThat(iconLayoutParams.getRule(RelativeLayout.CENTER_VERTICAL),
                is(equalTo(RelativeLayout.TRUE)));
        assertThat(iconLayoutParams.topMargin, is(equalTo(0)));
    }

    @Test
    public void testNoCarriedOverOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};
        TextListItem item0 = new TextListItem(mActivity);
        item0.setOnClickListener(v -> clicked[0] = true);

        setupPagedListView(Arrays.asList(item0));

        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        assertTrue(clicked[0]);

        // item1 does not have onClickListener.
        TextListItem item1 = new TextListItem(mActivity);
        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Manually rebind the view holder.
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        // Reset for testing.
        clicked[0] = false;
        onView(withId(R.id.recycler_view)).perform(actionOnItemAtPosition(0, click()));
        assertFalse(clicked[0]);
    }

    @Test
    public void testUpdateItem() {
        TextListItem item = new TextListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        String title = "updated title";
        item.setTitle(title);

        refreshUi();

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(title)));
    }

    @Test
    public void testUxRestrictionsChange() {
        String longText = mActivity.getString(R.string.over_uxr_text_length_limit);
        TextListItem item = new TextListItem(mActivity);
        item.setBody(longText);

        setupPagedListView(Arrays.asList(item));

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Default behavior without UXR is unrestricted.
        assertThat(viewHolder.getBody().getText(), is(equalTo(longText)));

        viewHolder.complyWithUxRestrictions(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();

        // Verify that the body text length is limited.
        assertThat(viewHolder.getBody().getText().length(), is(lessThan(longText.length())));
    }

    @Test
    public void testUxRestrictionsChangesDoNotAlterExistingInputFilters() {
        InputFilter filter = new InputFilter.AllCaps(Locale.US);
        String bodyText = "bodytext";
        TextListItem item = new TextListItem(mActivity);
        item.setBody(bodyText);
        item.addViewBinder(vh -> vh.getBody().setFilters(new InputFilter[] {filter}));

        setupPagedListView(Arrays.asList(item));

        TextListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Toggle UX restrictions between fully restricted and unrestricted should not affect
        // existing filters.
        viewHolder.complyWithUxRestrictions(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));

        viewHolder.complyWithUxRestrictions(CarUxRestrictionsTestUtils.getBaseline());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));
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
