/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

import static androidx.appcompat.testutils.AppCompatTintableViewActions.setBackgroundResource;
import static androidx.appcompat.testutils.AppCompatTintableViewActions.setBackgroundTintList;
import static androidx.appcompat.testutils.AppCompatTintableViewActions.setBackgroundTintMode;
import static androidx.appcompat.testutils.TestUtilsActions.setBackgroundTintListViewCompat;
import static androidx.appcompat.testutils.TestUtilsActions.setBackgroundTintModeViewCompat;
import static androidx.appcompat.testutils.TestUtilsActions.setEnabled;
import static androidx.appcompat.testutils.TestUtilsMatchers.isBackground;

import static org.junit.Assert.assertNull;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.AppCompatTintableViewActions;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Base class for testing custom view extensions in appcompat-v7 that implement the
 * <code>TintableBackgroundView</code> interface. Extensions of this class run all tests
 * from here and add test cases specific to the functionality they add to the relevant
 * base view class (such as <code>AppCompatTextView</code>'s all-caps support).
 */
@RunWith(AndroidJUnit4.class)
public abstract class AppCompatBaseViewTest<A extends BaseTestActivity, T extends View> {
    @Rule
    public final ActivityTestRule<A> mActivityTestRule;

    protected ViewGroup mContainer;

    protected A mActivity;
    protected Resources mResources;

    public AppCompatBaseViewTest(Class clazz) {
        mActivityTestRule = new ActivityTestRule<A>(clazz);
    }

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mContainer = mActivity.findViewById(R.id.container);
        mResources = mActivity.getResources();
    }

    /**
     * Subclasses should override this method to return true if by default the matching
     * view (such as, say, {@link AppCompatSpinner}) has background set it.
     */
    protected boolean hasBackgroundByDefault() {
        return false;
    }

    private void verifyBackgroundIsColoredAs(String description, @NonNull View view,
            @ColorInt int color, int allowedComponentVariance) {
        Drawable background = view.getBackground();
        TestUtils.assertAllPixelsOfColor(description,
                background, view.getWidth(), view.getHeight(), true,
                color, allowedComponentVariance, false);
    }

    /**
     * This method tests that background tinting is not applied when the
     * tintable view has no background.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingWithNoBackground() {
        if (hasBackgroundByDefault()) {
            return;
        }

        final @IdRes int viewId = R.id.view_tinted_no_background;
        final T view = (T) mContainer.findViewById(viewId);

        // Note that all the asserts in this test check that the view background
        // is null. This is because the matching child in the activity doesn't define any
        // background on itself, and there is nothing to tint.

        assertNull("No background after XML loading", view.getBackground());

        // Disable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(false));
        assertNull("No background after disabling", view.getBackground());

        // Enable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(true));
        assertNull("No background after re-enabling", view.getBackground());

        // Load a new color state list, set it on the view and check that the background
        // is still null.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        onView(withId(viewId)).perform(
                setBackgroundTintList(sandColor));

        // Disable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(false));
        assertNull("No background after disabling", view.getBackground());

        // Enable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(true));
        assertNull("No background after re-enabling", view.getBackground());
    }

    /**
     * This method tests that background tinting is not applied when the
     * tintable view has no background.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingViewCompatWithNoBackground() {
        if (hasBackgroundByDefault()) {
            return;
        }

        final @IdRes int viewId = R.id.view_tinted_no_background;
        final T view = (T) mContainer.findViewById(viewId);

        // Note that all the asserts in this test check that the view background
        // is null. This is because the matching child in the activity doesn't define any
        // background on itself, and there is nothing to tint.

        assertNull("No background after XML loading", view.getBackground());

        // Disable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(false));
        assertNull("No background after disabling", view.getBackground());

        // Enable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(true));
        assertNull("No background after re-enabling", view.getBackground());

        // Load a new color state list, set it on the view and check that the background
        // is still null.
        final ColorStateList lilacColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_lilac, null);
        onView(withId(viewId)).perform(setBackgroundTintListViewCompat(lilacColor));

        // Disable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(false));
        assertNull("No background after disabling", view.getBackground());

        // Enable the view and check that the background is still null.
        onView(withId(viewId)).perform(setEnabled(true));
        assertNull("No background after re-enabling", view.getBackground());
    }

    /**
     * This method tests that background tinting is applied to tintable view
     * in enabled and disabled state across a number of <code>ColorStateList</code>s set as
     * background tint lists on the same background.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingAcrossStateChange() {
        final @IdRes int viewId = R.id.view_tinted_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int lilacDefault = ResourcesCompat.getColor(
                mResources, R.color.lilac_default, null);
        final @ColorInt int lilacDisabled = ResourcesCompat.getColor(
                mResources, R.color.lilac_disabled, null);
        final @ColorInt int sandDefault = ResourcesCompat.getColor(
                mResources, R.color.sand_default, null);
        final @ColorInt int sandDisabled = ResourcesCompat.getColor(
                mResources, R.color.sand_disabled, null);
        final @ColorInt int oceanDefault = ResourcesCompat.getColor(
                mResources, R.color.ocean_default, null);
        final @ColorInt int oceanDisabled = ResourcesCompat.getColor(
                mResources, R.color.ocean_disabled, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state", view,
                lilacDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state", view,
                lilacDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state", view,
                lilacDefault, 0);

        // Load a new color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        onView(withId(viewId)).perform(setBackgroundTintList(sandColor));
        verifyBackgroundIsColoredAs("New sand tinting in enabled state", view,
                sandDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New sand tinting in disabled state", view,
                sandDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New sand tinting in re-enabled state", view,
                sandDefault, 0);

        // Load another color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_ocean, null);
        onView(withId(viewId)).perform(setBackgroundTintList(oceanColor));
        verifyBackgroundIsColoredAs("New ocean tinting in enabled state", view,
                oceanDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New ocean tinting in disabled state", view,
                oceanDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New ocean tinting in re-enabled state", view,
                oceanDefault, 0);
    }

    /**
     * This method tests that background tinting is applied to tintable view
     * in enabled and disabled state across a number of <code>ColorStateList</code>s set as
     * background tint lists on the same background.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingViewCompatAcrossStateChange() {
        final @IdRes int viewId = R.id.view_tinted_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int lilacDefault = ResourcesCompat.getColor(
                mResources, R.color.lilac_default, null);
        final @ColorInt int lilacDisabled = ResourcesCompat.getColor(
                mResources, R.color.lilac_disabled, null);
        final @ColorInt int sandDefault = ResourcesCompat.getColor(
                mResources, R.color.sand_default, null);
        final @ColorInt int sandDisabled = ResourcesCompat.getColor(
                mResources, R.color.sand_disabled, null);
        final @ColorInt int oceanDefault = ResourcesCompat.getColor(
                mResources, R.color.ocean_default, null);
        final @ColorInt int oceanDisabled = ResourcesCompat.getColor(
                mResources, R.color.ocean_disabled, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state", view,
                lilacDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state", view,
                lilacDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state", view,
                lilacDefault, 0);

        // Load a new color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        onView(withId(viewId)).perform(setBackgroundTintListViewCompat(sandColor));
        verifyBackgroundIsColoredAs("New sand tinting in enabled state", view,
                sandDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New sand tinting in disabled state", view,
                sandDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New sand tinting in re-enabled state", view,
                sandDefault, 0);

        // Load another color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_ocean, null);
        onView(withId(viewId)).perform(
                setBackgroundTintListViewCompat(oceanColor));
        verifyBackgroundIsColoredAs("New ocean tinting in enabled state", view,
                oceanDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New ocean tinting in disabled state", view,
                oceanDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New ocean tinting in re-enabled state", view,
                oceanDefault, 0);
    }

    /**
     * This method tests that background tinting applied to tintable view
     * in enabled and disabled state across the same background respects the currently set
     * background tinting mode.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingAcrossModeChange() {
        final @IdRes int viewId = R.id.view_untinted_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_default, null);
        final @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_background_green set on our view
        // that we'll be testing in this method
        final @ColorInt int backgroundColor = ResourcesCompat.getColor(
                mResources, R.color.test_green, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default no tinting in enabled state", view,
                backgroundColor, 0);

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the view background. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Set src_in tint mode on our view
        onView(withId(viewId)).perform(setBackgroundTintMode(PorterDuff.Mode.SRC_IN));

        // Load a new color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_emerald_translucent, null);
        onView(withId(viewId)).perform(setBackgroundTintList(emeraldColor));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_in", view,
                emeraldDefault, allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_in", view,
                emeraldDisabled, allowedComponentVariance);

        // Set src_over tint mode on our view. As the currently set tint list is using
        // translucent colors, we expect the actual background of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(viewId)).perform(setBackgroundTintMode(PorterDuff.Mode.SRC_OVER));

        // Enable the view and check that the background has switched to the matching entry
        // in the color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_over", view,
                ColorUtils.compositeColors(emeraldDefault, backgroundColor),
                allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_over",
                view, ColorUtils.compositeColors(emeraldDisabled, backgroundColor),
                allowedComponentVariance);
    }

    /**
     * This method tests that background tinting applied to tintable view
     * in enabled and disabled state across the same background respects the currently set
     * background tinting mode.
     */
    @Test
    @SmallTest
    public void testBackgroundTintingViewCompatAcrossModeChange() {
        final @IdRes int viewId = R.id.view_untinted_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_default, null);
        final @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_background_green set on our view
        // that we'll be testing in this method
        final @ColorInt int backgroundColor = ResourcesCompat.getColor(
                mResources, R.color.test_green, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default no tinting in enabled state", view,
                backgroundColor, 0);

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the view background. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Set src_in tint mode on our view
        onView(withId(viewId)).perform(setBackgroundTintModeViewCompat(PorterDuff.Mode.SRC_IN));

        // Load a new color state list, set it on the view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_emerald_translucent, null);
        onView(withId(viewId)).perform(setBackgroundTintListViewCompat(emeraldColor));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_in", view,
                emeraldDefault, allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_in", view,
                emeraldDisabled, allowedComponentVariance);

        // Set src_over tint mode on our view. As the currently set tint list is using
        // translucent colors, we expect the actual background of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(viewId)).perform(setBackgroundTintModeViewCompat(PorterDuff.Mode.SRC_OVER));

        // Enable the view and check that the background has switched to the matching entry
        // in the color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_over", view,
                ColorUtils.compositeColors(emeraldDefault, backgroundColor),
                allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_over",
                view, ColorUtils.compositeColors(emeraldDisabled, backgroundColor),
                allowedComponentVariance);
    }

    /**
     * This method tests that opaque background tinting applied to tintable view
     * is applied correctly after changing the background itself of the view.
     */
    @Test
    @SmallTest
    public void testBackgroundOpaqueTintingAcrossBackgroundChange() {
        final @IdRes int viewId = R.id.view_tinted_no_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int lilacDefault = ResourcesCompat.getColor(
                mResources, R.color.lilac_default, null);
        final @ColorInt int lilacDisabled = ResourcesCompat.getColor(
                mResources, R.color.lilac_disabled, null);

        if (!hasBackgroundByDefault()) {
            assertNull("No background after XML loading", view.getBackground());
        }

        // Set background on our view
        onView(withId(viewId)).perform(setBackgroundResource(R.drawable.test_background_green));

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state on green background",
                view, lilacDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state on green background",
                view, lilacDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state on green background",
                view, lilacDefault, 0);

        // Set a different background on our view based on resource ID
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setBackgroundResource(
                R.drawable.test_background_red));

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state on red background",
                view, lilacDefault, 0);

        // Disable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state on red background",
                view, lilacDisabled, 0);

        // Enable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state on red background",
                view, lilacDefault, 0);
    }

    /**
     * This method tests that translucent background tinting applied to tintable view
     * is applied correctly after changing the background itself of the view.
     */
    @Test
    @SmallTest
    public void testBackgroundTranslucentTintingAcrossBackgroundChange() {
        final @IdRes int viewId = R.id.view_untinted_no_background;
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_default, null);
        final @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                mResources, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_background_green set on our view
        // that we'll be testing in this method
        final @ColorInt int backgroundColorGreen = ResourcesCompat.getColor(
                mResources, R.color.test_green, null);
        final @ColorInt int backgroundColorRed = ResourcesCompat.getColor(
                mResources, R.color.test_red, null);

        if (!hasBackgroundByDefault()) {
            assertNull("No background after XML loading", view.getBackground());
        }

        // Set src_over tint mode on our view. As the currently set tint list is using
        // translucent colors, we expect the actual background of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(viewId)).perform(setBackgroundTintMode(PorterDuff.Mode.SRC_OVER));
        // Load and set a translucent color state list as the background tint list
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_emerald_translucent, null);
        onView(withId(viewId)).perform(
                setBackgroundTintList(emeraldColor));

        // Set background on our view
        onView(withId(viewId)).perform(setBackgroundResource(R.drawable.test_background_green));

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the view background. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Test the default state for tinting set up with the just loaded tint list.
        verifyBackgroundIsColoredAs("Emerald tinting in enabled state on green background",
                view, ColorUtils.compositeColors(emeraldDefault, backgroundColorGreen),
                allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Emerald tinting in disabled state on green background",
                view, ColorUtils.compositeColors(emeraldDisabled, backgroundColorGreen),
                allowedComponentVariance);

        // Enable the view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Emerald tinting in re-enabled state on green background",
                view, ColorUtils.compositeColors(emeraldDefault, backgroundColorGreen),
                allowedComponentVariance);

        // Set a different background on our view based on resource ID
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setBackgroundResource(
                R.drawable.test_background_red));

        // Test the default state for tinting the new background with the same color state list
        verifyBackgroundIsColoredAs("Emerald tinting in enabled state on red background",
                view, ColorUtils.compositeColors(emeraldDefault, backgroundColorRed),
                allowedComponentVariance);

        // Disable the view and check that the background has switched to the matching entry
        // in our current color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyBackgroundIsColoredAs("Emerald tinting in disabled state on red background",
                view, ColorUtils.compositeColors(emeraldDisabled, backgroundColorRed),
                allowedComponentVariance);

        // Enable the view and check that the background has switched to the matching entry
        // in our current color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyBackgroundIsColoredAs("Emerald tinting in re-enabled state on red background",
                view, ColorUtils.compositeColors(emeraldDefault, backgroundColorRed),
                allowedComponentVariance);
    }

    protected void testUntintedBackgroundTintingViewCompatAcrossStateChange(@IdRes int viewId) {
        final T view = (T) mContainer.findViewById(viewId);

        final @ColorInt int oceanDefault = ResourcesCompat.getColor(
                mResources, R.color.ocean_default, null);
        final @ColorInt int oceanDisabled = ResourcesCompat.getColor(
                mResources, R.color.ocean_disabled, null);

        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_ocean, null);
        onView(withId(viewId)).perform(setBackgroundTintListViewCompat(oceanColor));

        // Disable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false))
                .check(matches(isBackground(oceanDisabled, true)));

        // Enable the view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true))
                .check(matches(isBackground(oceanDefault, true)));
    }
}
