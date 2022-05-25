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

package androidx.appcompat.widget;

import static androidx.appcompat.testutils.TestUtils.assertCenterPixelOfColor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtils;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link Toolbar}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class ToolbarTest {

    private Instrumentation mInstrumentation;
    private Activity mActivity;
    private Toolbar mToolbar;

    @Rule
    public ActivityTestRule<ToolbarTestActivity> mActivityRule =
            new ActivityTestRule<>(ToolbarTestActivity.class);

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mActivity = mActivityRule.getActivity();
        mToolbar = mActivity.findViewById(R.id.toolbar);
    }

    @Test
    public void testCollapseConfiguration() {
        // Inflate menu with action view to display the collapse button.
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mToolbar.inflateMenu(R.menu.search_action);
            }
        });
        final MenuItem searchMenuItem = mToolbar.getMenu().findItem(R.id.action_search);
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                searchMenuItem.expandActionView();
            }
        });

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mToolbar.setCollapseIcon(R.drawable.icon_green);
            }
        });
        Drawable toolbarCollapseIcon = mToolbar.getCollapseIcon();
        TestUtils.assertAllPixelsOfColor("Collapse icon is green", toolbarCollapseIcon,
                toolbarCollapseIcon.getIntrinsicWidth(),
                toolbarCollapseIcon.getIntrinsicHeight(),
                true, Color.GREEN, 1, false);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mToolbar.setCollapseIcon(R.drawable.icon_blue);
            }
        });
        toolbarCollapseIcon = mToolbar.getCollapseIcon();
        TestUtils.assertAllPixelsOfColor("Collapse icon is blue", toolbarCollapseIcon,
                toolbarCollapseIcon.getIntrinsicWidth(),
                toolbarCollapseIcon.getIntrinsicHeight(),
                true, Color.BLUE, 1, false);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mToolbar.setCollapseContentDescription(R.string.toolbar_collapse);
            }
        });
        assertEquals(mActivity.getResources().getString(R.string.toolbar_collapse),
                mToolbar.getCollapseContentDescription());

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mToolbar.setCollapseContentDescription("Collapse legend");
            }
        });
        assertEquals("Collapse legend", mToolbar.getCollapseContentDescription());
    }

    @Test
    public void testTitlesTextColorSetHex() {
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_textcolor_hex);

        final int expectedColor = 0xFFFF00FF;

        assertEquals(expectedColor, toolbar.getTitleTextView().getCurrentTextColor());
        assertEquals(expectedColor, toolbar.getSubtitleTextView().getCurrentTextColor());
    }

    @Test
    public void testTitlesTextColorSetColorStateList() {
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_textcolor_csl);

        final int expectedColor = AppCompatResources.getColorStateList(toolbar.getContext(),
                R.color.color_state_lilac_alpha).getDefaultColor();

        assertEquals(expectedColor, toolbar.getTitleTextView().getCurrentTextColor());
        assertEquals(expectedColor, toolbar.getSubtitleTextView().getCurrentTextColor());
    }

    @Test
    public void testToolbarMenuFromXml() {
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_menu);
        final Menu menu = toolbar.getMenu();

        assertNotEquals(0, menu.size());
        assertNotNull(menu.findItem(R.id.action_search));
    }

    @Test
    public void testToolbarOverflowIconWithThemedCSL() {
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_themedcsl_colorcontrolnormal);

        // Assert that the overflow icon is tinted magenta, as per the theme
        final Drawable icon = toolbar.getOverflowIcon();
        assertNotNull(icon);
        assertCenterPixelOfColor(
                "Overflow icon is not tinted",
                icon,
                icon.getIntrinsicWidth(),
                icon.getIntrinsicHeight(),
                false,
                0xFFFF00FF,
                10,
                false);
    }

    @Test
    @UiThreadTest
    public void testToolbarIconTint() {
        final ColorStateList colors = ColorStateList.valueOf(Color.RED);
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_icons);

        // `menu_first` and `menu_second` are children of the menu in `toolbar_icons`
        final ActionMenuItemView firstMenuItem = mActivity.findViewById(R.id.menu_first);
        assertNotEquals(firstMenuItem.getItemData().getIconTintList(), colors);

        final ActionMenuItemView secondMenuItem = mActivity.findViewById(R.id.menu_second);
        assertNotEquals(secondMenuItem.getItemData().getIconTintList(), colors);

        toolbar.setIconTint(colors);

        // check that the tint lists are updated
        assertEquals(firstMenuItem.getItemData().getIconTintList(), colors);
        assertEquals(secondMenuItem.getItemData().getIconTintList(), colors);
    }

    @Test
    @UiThreadTest
    public void testToolbarIconTintMenuAdded() {
        final ColorStateList colors = ColorStateList.valueOf(Color.RED);
        final Toolbar toolbar = mActivity.findViewById(R.id.toolbar_no_menu);
        toolbar.setIconTint(colors);

        // Calling getMenu makes the toolbar create a menu
        toolbar.getMenu();
        // Find the ActionMenuView to add a child
        final int childCount = toolbar.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View v = toolbar.getChildAt(i);
            if (v instanceof ActionMenuView) {
                final ActionMenuView actionMenuView = (ActionMenuView) v;

                // check that initially the tint of this icon is not the menu color
                final ActionMenuItemView firstMenuItem = mActivity.findViewById(R.id.menu_first);
                assertNotEquals(firstMenuItem.getItemData().getIconTintList(), colors);

                // add item to menu, check that tint is now set
                actionMenuView.attachViewToParent(firstMenuItem, actionMenuView.getChildCount(),
                        new ActionMenuView.LayoutParams(0, 0));
                assertEquals(firstMenuItem.getItemData().getIconTintList(), colors);

                // check that adding item without item data doesn't fail
                ActionMenuItemView secondMenuItem = new ActionMenuItemView(v.getContext());
                actionMenuView.addView(secondMenuItem);
                assertNull(secondMenuItem.getItemData());
            }
        }
    }
}
