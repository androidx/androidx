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

package android.support.v7.app;

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.appcompat.test.R;
import android.view.Menu;
import android.view.MenuItem;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test shortcut trigger in case of MenuItems with non-default modifiers.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatMenuItemIconTintingTest {
    private AppCompatMenuItemIconTintingTestActivity mActivity;
    private Menu mMenu;

    @Rule
    public ActivityTestRule<AppCompatMenuItemIconTintingTestActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatMenuItemIconTintingTestActivity.class);

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
        mMenu = mActivity.getToolbarMenu();
    }

    @UiThreadTest
    @Test
    public void testIconTinting() throws Throwable {
        final MenuItem firstItem = mMenu.getItem(0);
        MenuItem secondItem = mMenu.getItem(1);
        MenuItem thirdItem = mMenu.getItem(2);

        // These are the default set in layout XML
        assertNull(MenuItemCompat.getIconTintMode(firstItem));
        assertEquals(Color.WHITE, MenuItemCompat.getIconTintList(firstItem).getDefaultColor());

        assertEquals(PorterDuff.Mode.SCREEN, MenuItemCompat.getIconTintMode(secondItem));
        assertNull(MenuItemCompat.getIconTintList(secondItem));

        assertNull(MenuItemCompat.getIconTintMode(thirdItem));
        assertNull(MenuItemCompat.getIconTintList(thirdItem));

        // Change tint color list and mode and verify that they are returned by the getters
        final ColorStateList colors = ColorStateList.valueOf(Color.RED);

        MenuItemCompat.setIconTintList(firstItem, colors);
        MenuItemCompat.setIconTintMode(firstItem, PorterDuff.Mode.XOR);
        assertSame(colors, MenuItemCompat.getIconTintList(firstItem));
        assertEquals(PorterDuff.Mode.XOR, MenuItemCompat.getIconTintMode(firstItem));

        // Ensure the tint is preserved across drawable changes.
        firstItem.setIcon(R.drawable.icon_yellow);
        assertSame(colors, MenuItemCompat.getIconTintList(firstItem));
        assertEquals(PorterDuff.Mode.XOR, MenuItemCompat.getIconTintMode(firstItem));

        // Change tint color list and mode again and verify that they are returned by the getters
        final ColorStateList colorsNew = ColorStateList.valueOf(Color.MAGENTA);
        MenuItemCompat.setIconTintList(firstItem, colorsNew);
        MenuItemCompat.setIconTintMode(firstItem, PorterDuff.Mode.SRC_IN);
        assertSame(colorsNew, MenuItemCompat.getIconTintList(firstItem));
        assertEquals(PorterDuff.Mode.SRC_IN, MenuItemCompat.getIconTintMode(firstItem));
    }
}
