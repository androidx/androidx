/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyLeftOf;
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyRightOf;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.car.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link CarToolbar}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CarToolbarTest {

    @Rule
    public ActivityTestRule<CarToolbarTestActivity> mActivityRule =
            new ActivityTestRule<>(CarToolbarTestActivity.class);
    private CarToolbarTestActivity mActivity;
    private CarToolbar mToolbar;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mToolbar = mActivity.findViewById(R.id.car_toolbar);
    }

    @Test
    public void testConstructor_doesNotThrowError() {
        new CarToolbar(mActivity);

        new CarToolbar(mActivity, /* attrs= */ null);

        new CarToolbar(mActivity, /* attrs= */ null, R.attr.carToolbarStyle);

        new CarToolbar(mActivity, /* attrs= */ null, R.attr.carToolbarStyle,
                R.style.Widget_Car_CarToolbar);
    }

    @Test
    public void testMinimumHeight_fixedHeight() throws Throwable {
        int expected = mActivity.getResources().getDimensionPixelSize(R.dimen.car_app_bar_height);
        // Set all widgets to null - Toolbar should still be minimum-height tall.
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setTitle(null);
            mToolbar.setNavigationIcon(null);
        });

        assertThat(mToolbar.getHeight(), is(equalTo(expected)));
    }

    @Test
    public void testSetTitleContent() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitle("title"));

        // Verify view is updated, and getTitle() returns expected value.
        onView(withId(R.id.title)).check(matches(withText("title")));
        assertEquals("title", mToolbar.getTitle());

        mActivityRule.runOnUiThread(() -> mToolbar.setTitle("new title"));
        onView(withId(R.id.title)).check(matches(withText("new title")));
        assertEquals("new title", mToolbar.getTitle());
    }

    @Test
    public void testSetTitleTextAppearance_doesNotThrowError() throws Throwable {
        // Since there are no APIs to get reference to the underlying implementation of
        // title, here we are testing that calling the relevant APIs doesn't crash.
        mActivityRule.runOnUiThread(() ->
                mToolbar.setTitleTextAppearance(R.style.TextAppearance_Car_Body1));
    }

    @Test
    public void testSetTitle_NullValueHidesText() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitle(null));

        assertEquals(View.GONE, getTitleView().getVisibility());
    }

    @Test
    public void testSetNavigationIcon_doesNotThrowError() throws Throwable {
        // Since there is no easy way to compare drawable, here we are testing that calling the
        // relevant APIs doesn't crash.
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIcon(android.R.drawable.sym_def_app_icon));
    }

    @Test
    public void testSetNavigationIconContainerWidth() throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setNavigationIcon(R.drawable.ic_nav_arrow_back);
            // Set title to verify icon space on right.
            mToolbar.setTitle("title");
        });

        int sideWidth = 10;
        // Container width is icon width plus |sideWidth| on both ends.
        int containerWidth = getNavigationIconView().getWidth() + (sideWidth * 2);
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconContainerWidth(containerWidth));

        onView(withId(R.id.nav_button)).check(matches(withLeft(sideWidth)));
        onView(withId(R.id.title)).check(matches(withLeft(containerWidth)));
    }

    @Test
    public void testSetNavigationIconContainerWidth_NoContainerKeepsIconCompletelyVisible()
            throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setNavigationIcon(R.drawable.ic_nav_arrow_back);
            // Set title to verify icon space on right.
            mToolbar.setTitle("title");
        });

        int containerWidth = 0;
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconContainerWidth(containerWidth));

        onView(withId(R.id.nav_button)).check(matches(withLeft(0)));
        onView(withId(R.id.title)).check(isCompletelyRightOf(withId(R.id.nav_button)));
    }

    @Test
    public void testSetNavigationIconOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[]{false};
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconOnClickListener(v -> clicked[0] = true));

        onView(withId(R.id.nav_button)).perform(click());
        assertTrue(clicked[0]);
    }

    @Test
    public void testSetTitleIconShowsAndHidesTitleIconView() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitleIcon(
                android.R.drawable.sym_def_app_icon));

        onView(withId(R.id.title_icon)).check(matches(isDisplayed()));

        mActivityRule.runOnUiThread(() -> mToolbar.setTitleIcon(null));

        onView(withId(R.id.title_icon)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testTitleIconHasZeroDefaultMargins() throws Throwable {
        mActivityRule.runOnUiThread(() ->
                mToolbar.setTitleIcon(android.R.drawable.sym_def_app_icon));

        onView(withId(R.id.title_icon)).check(matches(withHorizontalMargins(0)));
    }

    @Test
    public void testSetTitleIconStartMargin() throws Throwable {
        int startMargin = 100;
        int navIconWidth = 100;
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setNavigationIconContainerWidth(navIconWidth);
            mToolbar.setTitleIcon(android.R.drawable.sym_def_app_icon);
            mToolbar.setTitleIconStartMargin(startMargin);
        });

        onView(withId(R.id.title_icon)).check(matches(withLeft(navIconWidth + startMargin)));
    }

    @Test
    public void testSetTitleIconEndMargin() throws Throwable {
        int endMargin = 100;
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setTitleIcon(android.R.drawable.sym_def_app_icon);
            mToolbar.setTitleIconEndMargin(endMargin);
            mToolbar.setTitle("title");
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        int iconEnd = mActivity.findViewById(R.id.title_icon).getRight();

        onView(withId(R.id.title)).check(matches(withLeft(iconEnd + endMargin)));
    }

    @Test
    public void testTitleIconHasCorrectDefaultWidth() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitleIcon(
                android.R.drawable.sym_def_app_icon));

        onView(withId(R.id.title_icon)).check(matches(withWidth(
                mActivity.getResources()
                        .getDimensionPixelSize(R.dimen.car_application_icon_size))));
    }

    @Test
    public void testSetTitleIconSizeSetsCorrectSize() throws Throwable {
        int size = mActivity.getResources().getDimensionPixelSize(R.dimen.car_avatar_icon_size);
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setTitleIcon(android.R.drawable.sym_def_app_icon);
            mToolbar.setTitleIconSize(size);
        });

        onView(withId(R.id.title_icon)).check(matches(withWidth(size)));
    }

    private ImageButton getNavigationIconView() {
        return mActivity.findViewById(R.id.nav_button);
    }

    @Test
    public void testSubtitleGetAndSetMethod() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setSubtitle("this is subtitle"));
        CharSequence subtitle = mToolbar.getSubtitle();
        assertEquals(subtitle, "this is subtitle");
    }

    @Test
    public void testSubtitleDoesNotShowWhenContentIsEmpty() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setSubtitle(""));
        onView(withId(R.id.subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSubtitleDoesNotShowWhenContentIsNull() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setSubtitle(null));
        onView(withId(R.id.subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testSubtitleShowsWhenContentNotEmpty() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setSubtitle("this is subtitle"));
        onView(withId(R.id.subtitle)).check(matches(isDisplayed()));
    }

    @Test
    public void testActionItemDisplayedOnToolbar() throws Throwable {
        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        onView(withText(actionItemText)).check(matches(isDisplayed()));
    }

    @Test
    public void testSetMenuItems_NullClearsItems() throws Throwable {
        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        // Wait for item to show.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(null));

        onView(withText(actionItemText)).check(doesNotExist());
    }

    @Test
    public void testActionItemWithIconDisplaysIcon() throws Throwable {
        String actionItemText = "action_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setIcon(mActivity, android.R.drawable.sym_def_app_icon)
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        onView(allOf(withText(actionItemText), hasLeftCompoundDrawable()))
                .check(matches((isDisplayed())));
    }

    @Test
    public void testSwitchActionItemDisplaysSwitchWidget() throws Throwable {
        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setCheckable(true)
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        onView(allOf(instanceOf(Switch.class), hasSibling(withText(actionItemText))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSwitchActionItemSetCheckable() throws Throwable {
        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setCheckable(true)
                .setChecked(true) // Check the item
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        onView(allOf(instanceOf(Switch.class), hasSibling(withText(actionItemText))))
                .check(matches(isChecked()));
    }

    @Test
    public void testActionItemSetEnabled_False() throws Throwable {
        boolean[] clicked = new boolean[] {false};

        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setTitle(actionItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setCheckable(true)
                .setEnabled(false)
                .setOnClickListener(item -> clicked[0] = item.isChecked())
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        // 1. Check that the Switch is disabled.
        onView(allOf(instanceOf(Switch.class), hasSibling(withText(actionItemText))))
                .check(matches(not(isEnabled())));

        // 2. Check that the OnClickListener is not called.
        //    Since the Switch is toggled programmatically.
        onView(withText(actionItemText)).perform(click());
        assertFalse(clicked[0]);
    }

    @Test
    public void testActionItemClickInvokesItemOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};

        String actionItemText = "action_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setTitle(actionItemText)
                .setOnClickListener(item -> clicked[0] = true)
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        // Click action item.
        onView(withText(actionItemText)).perform(click());

        assertTrue(clicked[0]);
    }

    @Test
    public void testCheckableActionItemToggleInvokesItemOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};

        String actionItemText = "checkable_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setTitle(actionItemText)
                .setCheckable(true)
                .setOnClickListener(item -> clicked[0] = item.isChecked())
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        // Toggle the Switch ON.
        onView(withText(actionItemText)).perform(click());

        assertTrue(clicked[0]);

        // Toggle the switch OFF.
        onView(withText(actionItemText)).perform(click());

        assertFalse(clicked[0]);
    }

    @Test
    public void testOverflowButtonShownIfOverflowItems() throws Throwable {
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(overflowItem)));

        onView(withId(R.id.overflow_menu)).check(matches(isDisplayed()));
    }

    @Test
    public void testOverflowButtonHiddenIfNoOverflowItems() throws Throwable {
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(actionItem)));

        onView(withId(R.id.overflow_menu)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testOverflowMenuDisplaysNeverItem() throws Throwable {
        String overflowItemText = "overflow_item_text";
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .setTitle(overflowItemText)
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(overflowItem)));
        // Open overflow menu.
        onView(withId(R.id.overflow_menu)).perform(click());

        onView(withText(overflowItemText)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    @Test
    public void testOverflowMenuDoesNotDisplayActionItem() throws Throwable {
        String overflowItemText = "overflow_item_text";
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .setTitle(overflowItemText)
                .build();

        String actionItemText = "action_item_text";
        CarMenuItem actionItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS) // Action item
                .setTitle(actionItemText)
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Arrays.asList(overflowItem, actionItem)));
        // Open overflow menu.
        onView(withId(R.id.overflow_menu)).perform(click());

        onView(withText(actionItemText)).inRoot(isDialog()).check(doesNotExist());
    }

    @Test
    public void testIsOverflowMenuShowing() throws Throwable {
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(overflowItem)));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertFalse(mToolbar.isOverflowMenuShowing());

        // Open overflow menu.
        onView(withId(R.id.overflow_menu)).perform(click());

        assertTrue(mToolbar.isOverflowMenuShowing());
    }

    @Test
    public void testShowOverflowMenu() throws Throwable {
        String overflowItemText = "overflow_item_text";
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .setTitle(overflowItemText)
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(overflowItem)));

        // Since overflow items are set in onMeasure, need to wait for the views to be laid out.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        mActivityRule.runOnUiThread(() -> mToolbar.setOverflowMenuShown(true));

        onView(withText(overflowItemText)).inRoot(isDialog()).check(matches(isDisplayed()));
    }

    @Test
    public void testHideOverflowMenu() throws Throwable {
        String overflowItemText = "overflow_item_text";
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .setTitle(overflowItemText)
                .build();

        mActivityRule.runOnUiThread(() -> {
            mToolbar.setMenuItems(Collections.singletonList(overflowItem));
            mToolbar.setOverflowMenuShown(true);
            mToolbar.setOverflowMenuShown(false);
        });

        onView(withText(overflowItemText)).check(doesNotExist());
    }

    @Test
    public void testOverflowMenuClickInvokesItemOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};

        String overflowItemText = "overflow_item_text";
        CarMenuItem overflowItem = new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER) // Overflow menu item
                .setTitle(overflowItemText)
                .setOnClickListener(item -> clicked[0] = true)
                .build();
        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(overflowItem)));
        // Open overflow menu.
        onView(withId(R.id.overflow_menu)).perform(click());

        // Click overflow menu item.
        onView(withText(overflowItemText)).perform(click());

        assertTrue(clicked[0]);
    }

    @Test
    public void testIfRoomItemDisplayedInOrderProvided() throws Throwable {
        List<CarMenuItem> items = new ArrayList<>();
        String action1Text = "action_item_1";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(action1Text)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                .build());

        String ifRoomItemText = "if_room_item_text";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(ifRoomItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                .build());

        String action2Text = "action_item_2";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(action2Text)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                .build());


        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(items));

        onView(withText(ifRoomItemText)).check(isCompletelyLeftOf(withText(action1Text)));
        onView(withText(ifRoomItemText)).check(isCompletelyRightOf(withText(action2Text)));
    }

    @Test
    public void testIfRoomItemDisplayedIfRoomAvailable() throws Throwable {
        String ifRoomItemText = "if_room_item_text";
        CarMenuItem ifRoomItem = new CarMenuItem
                .Builder()
                .setTitle(ifRoomItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                .build();

        mActivityRule.runOnUiThread(() ->
                mToolbar.setMenuItems(Collections.singletonList(ifRoomItem)));

        onView(withText(ifRoomItemText)).check(matches(isDisplayed()));
    }

    @Test
    public void testIfRoomItemAddedToOverflowIfOverCountLimit() throws Throwable {
        List<CarMenuItem> items = new ArrayList<>();
        for (int i = 0; i < CarToolbar.ACTION_ITEM_COUNT_LIMIT; i++) {
            items.add(new CarMenuItem
                    .Builder()
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                    .build());
        }

        String ifRoomItemText = "if_room_item_text";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(ifRoomItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                .build());

        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(items));

        onView(withText(ifRoomItemText)).check(doesNotExist());

        // Open overflow menu.
        onView(withId(R.id.overflow_menu)).perform(click());

        onView(withText(ifRoomItemText)).check(matches(isDisplayed()));
    }

    @Test
    public void testIfRoomItemAddedToOverflowIfOverWidthPercentageLimit() throws Throwable {
        // Add just one ALWAYS item with long text so that ACTION_ITEM_COUNT_LIMIT is not reached.
        String longText = mActivity.getString(R.string.over_uxr_text_length_limit);
        List<CarMenuItem> items = new ArrayList<>();
        items.add(new CarMenuItem
                .Builder()
                .setTitle(longText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                .build());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        String ifRoomItemText = "if_room_item_text";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(ifRoomItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                .build());

        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(items));

        onView(withText(ifRoomItemText)).check(doesNotExist());

        mActivityRule.runOnUiThread(() -> mToolbar.setOverflowMenuShown(true));

        onView(withText(ifRoomItemText)).check(matches(isDisplayed()));
    }

    @Test
    public void testIfRoomItemPushedFromActionToOverflowIfLimitExceeded() throws Throwable {
        List<CarMenuItem> items = new ArrayList<>();
        items.add(new CarMenuItem
                .Builder()
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                .build());

        String ifRoomItemText = "if_room_item_text";
        items.add(new CarMenuItem
                .Builder()
                .setTitle(ifRoomItemText)
                .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                .build());

        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(items));

        onView(withText(ifRoomItemText)).check(matches(isDisplayed()));

        for (int i = 0; i < CarToolbar.ACTION_ITEM_COUNT_LIMIT - 1; i++) {
            items.add(new CarMenuItem
                    .Builder()
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                    .build());
        }
        mActivityRule.runOnUiThread(() -> mToolbar.setMenuItems(items));

        onView(withText(ifRoomItemText)).check(doesNotExist());
        onView(withId(R.id.overflow_menu)).check(matches(isDisplayed()));
    }

    private TextView getTitleView() {
        return mActivity.findViewById(R.id.title);
    }

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    /**
     * Returns a {@link Matcher} that checks the left position of a view relative to its parent.
     *
     * @param expected Expected left position in pixels.
     * @return A {@link Matcher} for verification.
     */
    private static Matcher<View> withLeft(int expected) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return item.getLeft() == expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is " + expected + " pixel to its parent");
            }
        };
    }

    /**
     * Returns a {@link Matcher} that matches {@link View}s that have the given width.
     *
     * @param width The width in pixels to match to.
     * @return A {@link Matcher} for verification.
     */
    @NonNull
    private static Matcher<View> withWidth(int width) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                return width == view.getWidth();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has width: " + width);
            }
        };
    }

    /**
     * Returns a {@link Matcher} that matches {@link Button}s that have a compound {@link Drawable}.
     *
     * @return A {@link Matcher} for verification.
     */
    @NonNull
    private static Matcher<View> hasLeftCompoundDrawable() {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                if (view instanceof Button) {
                    Button button = (Button) view;
                    // Action items have their icons on the left, so check the first index.
                    return button.getCompoundDrawables()[0] != null;
                } else {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has a left compound drawable");
            }
        };
    }

    /**
     * Returns a matcher that matches {@link View}s that have the given horizontal margins.
     *
     * @param horizontalMargin The horizontal margin value to match to.
     * @return A {@link Matcher} for verification.
     */
    @NonNull
    public static Matcher<View> withHorizontalMargins(int horizontalMargin) {
        return new TypeSafeMatcher<View>() {
            @Override
            public boolean matchesSafely(View view) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) view.getLayoutParams();
                return horizontalMargin == params.leftMargin
                        && horizontalMargin == params.rightMargin;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has horizontal margins: " + horizontalMargin);
            }
        };
    }
}
