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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.car.test.R;

/**
* Tests the layout configuration in {@link ListItem}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ListItemTest {

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

    private void setupPagedListView(List<ListItem> items) {
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

    private ListItemAdapter.ViewHolder getViewHolderAtPosition(int position) {
        return (ListItemAdapter.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(
                position);
    }

    @Test
    public void testEmptyItemHidesAllViews() {
        ListItem item = new ListItem.Builder(mActivity).build();
        setupPagedListView(Arrays.asList(item));
        verifyViewIsHidden(mPagedListView.getRecyclerView().getLayoutManager().getChildAt(0));
    }

    @Test
    public void testPrimaryActionVisible() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .build());
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextVisible() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withTitle("title")
                        .build(),
                new ListItem.Builder(mActivity)
                        .withBody("body")
                        .build());
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getTitle().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getBody().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSupplementalActionVisible() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withAction("text", true, v -> { /* Do nothing. */ })
                        .build(),
                new ListItem.Builder(mActivity)
                        .withActions("text", true, v -> { /* Do nothing. */ },
                                 "text", true, v -> { /* Do nothing. */ })
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
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
    public void testSwitchVisibleAndCheckedState() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSwitch(true, true, null)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withSwitch(false, true, null)
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitch().isChecked(), is(equalTo(true)));
        assertThat(viewHolder.getSwitchDivider().getVisibility(), is(equalTo(View.VISIBLE)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(viewHolder.getSwitch().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getSwitch().isChecked(), is(equalTo(false)));
        assertThat(viewHolder.getSwitchDivider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testDividersAreOptional() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSupplementalIcon(android.R.drawable.sym_def_app_icon, false)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withAction("text", false, v -> { /* Do nothing. */ })
                        .build(),
                new ListItem.Builder(mActivity)
                        .withActions("text", false, v -> { /* Do nothing. */ },
                                "text", false, v -> { /* Do nothing. */ })
                        .build(),
                new ListItem.Builder(mActivity)
                        .withSwitch(true, false, null)
                        .build());
        setupPagedListView(items);

        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
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
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .build(),
                new ListItem.Builder(mActivity)
                        .withPrimaryActionEmptyIcon()
                        .build(),
                new ListItem.Builder(mActivity)
                        .withPrimaryActionNoIcon()
                        .build());
        List<Integer> expectedStartMargin = Arrays.asList(R.dimen.car_keyline_4,
                R.dimen.car_keyline_3, R.dimen.car_keyline_3, R.dimen.car_keyline_1);
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(i);

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
        List<ListItem> items = Arrays.asList(
                // Only space
                new ListItem.Builder(mActivity)
                        .withTitle(" ")
                        .build(),
                // Underscore
                new ListItem.Builder(mActivity)
                        .withTitle("______")
                        .build(),
                new ListItem.Builder(mActivity)
                        .withTitle("ALL UPPER CASE")
                        .build(),
                // String wouldn't fit in one line
                new ListItem.Builder(mActivity)
                        .withTitle(InstrumentationRegistry.getContext().getResources().getString(
                                R.string.over_120_chars))
                        .build());
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
        List<ListItem> items = Arrays.asList(
                // Only space
                new ListItem.Builder(mActivity)
                        .withBody(" ")
                        .build(),
                // Underscore
                new ListItem.Builder(mActivity)
                        .withBody("____")
                        .build(),
                // String wouldn't fit in one line
                new ListItem.Builder(mActivity)
                        .withBody(InstrumentationRegistry.getContext().getResources().getString(
                                R.string.over_120_chars))
                        .build());
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
    public void testBodyTextLengthLimit() {
        final String longText = InstrumentationRegistry.getContext().getResources().getString(
                R.string.over_120_chars);
        final int limit = InstrumentationRegistry.getContext().getResources().getInteger(
                R.integer.car_list_item_text_length_limit);
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withBody(longText)
                        .build());
        setupPagedListView(items);

        // + 1 for appended ellipsis.
        assertThat(getViewHolderAtPosition(0).getBody().getText().length(),
                is(equalTo(limit + 1)));
    }

    @Test
    public void testPrimaryIconDrawable() {
        Drawable drawable = InstrumentationRegistry.getContext().getResources().getDrawable(
                android.R.drawable.sym_def_app_icon, null);
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(drawable, true)
                        .build());
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable().getConstantState()
                .equals(drawable.getConstantState()));
    }

    @Test
    public void testLargePrimaryIconHasNoStartMargin() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(0)));
    }

    @Test
    public void testSmallPrimaryIconStartMargin() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .build());
        setupPagedListView(items);

        int expected = InstrumentationRegistry.getContext().getResources().getDimensionPixelSize(
                R.dimen.car_keyline_1);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));
    }

    @Test
    public void testSmallPrimaryIconTopMarginRemainsTheSameRegardlessOfTextLength() {
        final String longText = InstrumentationRegistry.getContext().getResources().getString(
                R.string.over_120_chars);
        List<ListItem> items = Arrays.asList(
                // Single line item.
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .withTitle("one line text")
                        .build(),
                // Double line item with one line text.
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .withTitle("one line text")
                        .withBody("one line text")
                        .build(),
                // Double line item with long text.
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .withTitle("one line text")
                        .withBody(longText)
                        .build(),
                // Body text only - long text.
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .withBody(longText)
                        .build(),
                // Body text only - one line text.
                new ListItem.Builder(mActivity)
                        .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                        .withBody("one line text")
                        .build());
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
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withOnClickListener(v -> clicked[0] = true)
                        .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                                v -> clicked[1] = true)
                        .build());
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
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true,
                                v -> clicked[0] = true)
                        .build());
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.supplemental_icon)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testSupplementalIconWithoutClickListenerIsNotClickable() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSupplementalIcon(android.R.drawable.sym_def_app_icon, true)
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getSupplementalIcon().isClickable());
    }

    @Test
    public void testCheckingSwitch() {
        final boolean[] clicked = {false, false};
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withSwitch(false, false, (button, isChecked) -> {
                            // Initial value is false.
                            assertTrue(isChecked);
                            clicked[0] = true;
                        })
                        .build(),
                new ListItem.Builder(mActivity)
                        .withSwitch(true, false, (button, isChecked) -> {
                            // Initial value is true.
                            assertFalse(isChecked);
                            clicked[1] = true;
                        })
                        .build());
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
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withAction("action", true, v -> clicked[0] = true)
                        .build());
        setupPagedListView(items);

        onView(withId(R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(R.id.action1)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testClickingBothSupplementalActions() {
        final boolean[] clicked = {false, false};
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withActions("action 1", true, v -> clicked[0] = true,
                                "action 2", true, v -> clicked[1] = true)
                        .build());
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
    public void testCustomViewBinderAreCalledLast() {
        final String updatedTitle = "updated title";
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withTitle("original title")
                        .withViewBinder((viewHolder) -> viewHolder.getTitle().setText(updatedTitle))
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(updatedTitle)));
    }

    @Test
    public void testCustomViewBinderOnUnusedViewsHasNoEffect() {
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withViewBinder((viewHolder) -> viewHolder.getBody().setText("text"))
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBody().getVisibility(), is(equalTo(View.GONE)));
        // Custom binder interacts with body but has no effect.
        // Expect card height to remain single line.
        assertThat((double) viewHolder.itemView.getHeight(), is(closeTo(
                InstrumentationRegistry.getContext().getResources().getDimension(
                        R.dimen.car_single_line_list_item_height), 1.0d)));
    }

    @Test
    public void testSettingTitleOrBodyAsPrimaryText() {
        // Create 2 items, one with Title as primary (default) and one with Body.
        // The primary text, regardless of view, should have consistent look (as primary).
        List<ListItem> items = Arrays.asList(
                new ListItem.Builder(mActivity)
                        .withTitle("title")
                        .withBody("body")
                        .build(),
                new ListItem.Builder(mActivity)
                        .withTitle("title")
                        .withBody("body", true)
                        .build());
        setupPagedListView(items);

        ListItemAdapter.ViewHolder titlePrimary = getViewHolderAtPosition(0);
        ListItemAdapter.ViewHolder bodyPrimary = getViewHolderAtPosition(1);
        assertThat(titlePrimary.getTitle().getTextSize(),
                is(equalTo(bodyPrimary.getBody().getTextSize())));
        assertThat(titlePrimary.getTitle().getTextColors(),
                is(equalTo(bodyPrimary.getBody().getTextColors())));
    }

    @Test
    public void testNoCarriedOverLayoutParamsForTextView() throws Throwable {
        ListItem singleLine = new ListItem.Builder(mActivity).withTitle("t").build();
        setupPagedListView(Arrays.asList(singleLine));

        // Manually rebind the view holder of a single line item to a double line item.
        ListItem doubleLine = new ListItem.Builder(mActivity).withTitle("t").withBody("b").build();
        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
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
        ListItem smallIcon = new ListItem.Builder(mActivity)
                .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, false)
                .withBody("body")  // Small icon of items with body text should use top margin.
                .build();
        setupPagedListView(Arrays.asList(smallIcon));

        // Manually rebind the view holder.
        ListItem largeIcon = new ListItem.Builder(mActivity)
                .withPrimaryActionIcon(android.R.drawable.sym_def_app_icon, true)
                .build();
        ListItemAdapter.ViewHolder viewHolder = getViewHolderAtPosition(0);
        mActivityRule.runOnUiThread(() -> largeIcon.bind(viewHolder));

        RelativeLayout.LayoutParams iconLayoutParams =
                (RelativeLayout.LayoutParams) viewHolder.getPrimaryIcon().getLayoutParams();
        assertThat(iconLayoutParams.getRule(RelativeLayout.CENTER_VERTICAL),
                is(equalTo(RelativeLayout.TRUE)));
        assertThat(iconLayoutParams.topMargin, is(equalTo(0)));
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
