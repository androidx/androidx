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

package androidx.appcompat.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.MenuItemCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test icon tinting in {@link MenuItem}s
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatMenuItemIconTintingTest {
    private AppCompatMenuItemIconTintingTestActivity mActivity;
    private Resources mResources;
    private Menu mMenu;

    @Rule
    public ActivityTestRule<AppCompatMenuItemIconTintingTestActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatMenuItemIconTintingTestActivity.class);

    @Before
    public void setup() {
        mActivity = mActivityTestRule.getActivity();
        mResources = mActivity.getResources();
        mMenu = mActivity.getToolbarMenu();
    }

    @UiThreadTest
    @Test
    public void testIconTinting() throws Throwable {
        final MenuItem firstItem = mMenu.getItem(0);
        final MenuItem secondItem = mMenu.getItem(1);
        final MenuItem thirdItem = mMenu.getItem(2);

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

    private void verifyIconIsColoredAs(String description, @NonNull Drawable icon,
            @ColorInt int color, int allowedComponentVariance) {
        TestUtils.assertAllPixelsOfColor(description,
                icon, icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), true,
                color, allowedComponentVariance, false);
    }


    /**
     * This method tests that icon tinting is not applied when the
     * menu item has no icon.
     */
    @UiThreadTest
    @Test
    public void testIconTintingWithNoIcon() {
        final MenuItem sixthItem = mMenu.getItem(5);

        // Note that all the asserts in this test check that the menu item icon
        // is null. This is because the matching entry in the XML doesn't define any
        // icon, and there is nothing to tint.
        assertNull("No icon after XML loading", sixthItem.getIcon());

        // Load a new color state list, set it on the menu item icon and check that the icon
        // is still null.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        MenuItemCompat.setIconTintList(sixthItem, sandColor);
        assertNull("No icon after setting icon tint list", sixthItem.getIcon());

        // Set tint mode on the menu item icon and check that the icon is still null.
        MenuItemCompat.setIconTintMode(sixthItem, PorterDuff.Mode.MULTIPLY);
        assertNull("No icon after setting icon tint mode", sixthItem.getIcon());
    }

    /**
     * This method tests that icon tinting is applied across a number of
     * <code>ColorStateList</code>s set as icon tint lists on the same menu item.
     */
    @UiThreadTest
    @Test
    public void testIconTintingAcrossTintListChange() {
        final MenuItem firstItem = mMenu.getItem(0);

        final @ColorInt int sandDefault = ResourcesCompat.getColor(
                mResources, R.color.sand_default, null);
        final @ColorInt int oceanDefault = ResourcesCompat.getColor(
                mResources, R.color.ocean_default, null);

        // Test the default state for tinting set up in the menu XML file.
        verifyIconIsColoredAs("Default white tinting", firstItem.getIcon(), Color.WHITE, 0);

        // Load a new color state list, set it on the menu item and check that the icon has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        MenuItemCompat.setIconTintList(firstItem, sandColor);
        verifyIconIsColoredAs("Default white tinting", firstItem.getIcon(), sandDefault, 0);

        // Load another color state list, set it on the menu item and check that the icon has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_ocean, null);
        MenuItemCompat.setIconTintList(firstItem, oceanColor);
        verifyIconIsColoredAs("Default white tinting", firstItem.getIcon(), oceanDefault, 0);
    }

    /**
     * This method tests that opaque icon tinting is applied correctly after changing the icon
     * itself of the menu item.
     */
    @UiThreadTest
    @Test
    public void testIconOpaqueTintingAcrossIconChange() {
        final MenuItem secondItem = mMenu.getItem(1);

        // This is the fill color of R.drawable.icon_black set on our menu icon
        // that we'll be testing in this method
        final @ColorInt int iconColorBlack = 0xFF000000;

        // At this point we shouldn't have any tinting since it's not defined in the menu XML
        verifyIconIsColoredAs("Black icon before any tinting", secondItem.getIcon(),
                iconColorBlack, 0);

        // Now set up the tinting
        final ColorStateList lilacColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_lilac, null);
        final @ColorInt int lilacDefault = ResourcesCompat.getColor(
                mResources, R.color.lilac_default, null);
        MenuItemCompat.setIconTintList(secondItem, lilacColor);
        MenuItemCompat.setIconTintMode(secondItem, PorterDuff.Mode.SRC_OVER);

        // Check that the icon is now tinted
        verifyIconIsColoredAs("Lilac icon after tinting the black icon",
                secondItem.getIcon(), lilacDefault, 0);

        // Set a different icon on our menu item
        secondItem.setIcon(R.drawable.test_drawable_red);

        // Check that the icon is still tinted with the same color as before
        verifyIconIsColoredAs("Lilac icon after changing icon to red",
                secondItem.getIcon(), lilacDefault, 0);
    }

    /**
     * This method tests that translucent icon tinting is applied correctly after changing the icon
     * itself of the menu item.
     */
    @UiThreadTest
    @Test
    public void testIconTranslucentTintingAcrossIconChange() {
        final MenuItem secondItem = mMenu.getItem(1);

        // This is the fill color of R.drawable.icon_black set on our menu icon
        // that we'll be testing in this method
        final @ColorInt int iconColorBlack = 0xFF000000;

        // At this point we shouldn't have any tinting since it's not defined in the menu XML
        verifyIconIsColoredAs("Black icon before any tinting", secondItem.getIcon(),
                iconColorBlack, 0);

        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_emerald_translucent, null);
        final @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_default, null);
        // This is the fill color of R.drawable.test_background_red that will be set on our
        // menu icon that we'll be testing in this method
        final @ColorInt int iconColorRed = ResourcesCompat.getColor(
                mResources, R.color.test_red, null);

        // Set up the tinting of our menu item. The tint list is using translucent color, and the
        // tint mode is going to be src_over, which will create a "mix" of the original icon with
        // the translucent tint color.
        MenuItemCompat.setIconTintList(secondItem, emeraldColor);
        MenuItemCompat.setIconTintMode(secondItem, PorterDuff.Mode.SRC_OVER);

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the menu icon. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Test the tinting set up with the just loaded tint list.
        verifyIconIsColoredAs("Emerald tinting on green icon",
                secondItem.getIcon(), ColorUtils.compositeColors(emeraldDefault, iconColorBlack),
                allowedComponentVariance);

        // Set a different icon on our menu item
        secondItem.setIcon(R.drawable.test_drawable_red);

        // Test the tinting of the new menu icon with the same color state list
        verifyIconIsColoredAs("Emerald tinting on red icon",
                secondItem.getIcon(), ColorUtils.compositeColors(emeraldDefault, iconColorRed),
                allowedComponentVariance);
    }
}
