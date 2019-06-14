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

package androidx.appcompat.widget;

import static androidx.appcompat.testutils.TestUtilsActions.setEnabled;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.AppCompatTintableViewActions;
import androidx.appcompat.testutils.BaseTestActivity;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.test.filters.SmallTest;

import org.junit.Test;

/**
 * In addition to all tinting-related tests done by the base class, this class provides
 * testing for tinting image resources on appcompat-v7 view classes that extend directly
 * or indirectly the core {@link ImageView} class.
 */
public abstract class AppCompatBaseImageViewTest<T extends ImageView>
        extends AppCompatBaseViewTest<BaseTestActivity, T> {
    @SuppressWarnings("unchecked")
    public AppCompatBaseImageViewTest(Class<? extends BaseTestActivity> clazz) {
        super((Class<BaseTestActivity>) clazz);
    }

    private void verifyImageSourceIsColoredAs(String description, @NonNull ImageView imageView,
            @ColorInt int color, int allowedComponentVariance) {
        Drawable imageSource = imageView.getDrawable();
        TestUtils.assertAllPixelsOfColor(description,
                imageSource, imageSource.getIntrinsicWidth(), imageSource.getIntrinsicHeight(),
                true, color, allowedComponentVariance, false);
    }

    /**
     * This method tests that image tinting is applied to tintable image view
     * in enabled and disabled state across a number of <code>ColorStateList</code>s set as
     * image source tint lists on the same image source.
     */
    @Test
    @SmallTest
    public void testImageTintingAcrossStateChange() {
        final @IdRes int viewId = R.id.view_tinted_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);
        @ColorInt int sandDefault = ResourcesCompat.getColor(res, R.color.sand_default, null);
        @ColorInt int sandDisabled = ResourcesCompat.getColor(res, R.color.sand_disabled, null);
        @ColorInt int oceanDefault = ResourcesCompat.getColor(res, R.color.ocean_default, null);
        @ColorInt int oceanDisabled = ResourcesCompat.getColor(res, R.color.ocean_disabled, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default lilac tinting in enabled state", view,
                lilacDefault, 0);

        // Disable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Default lilac tinting in disabled state", view,
                lilacDisabled, 0);

        // Enable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("Default lilac tinting in re-enabled state", view,
                lilacDefault, 0);

        // Load a new color state list, set it on the view and check that the image has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_sand, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(sandColor));
        verifyImageSourceIsColoredAs("New sand tinting in enabled state", view,
                sandDefault, 0);

        // Disable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("New sand tinting in disabled state", view,
                sandDisabled, 0);

        // Enable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("New sand tinting in re-enabled state", view,
                sandDefault, 0);

        // Load another color state list, set it on the view and check that the image has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_ocean, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(oceanColor));
        verifyImageSourceIsColoredAs("New ocean tinting in enabled state", view,
                oceanDefault, 0);

        // Disable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("New ocean tinting in disabled state", view,
                oceanDisabled, 0);

        // Enable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("New ocean tinting in re-enabled state", view,
                oceanDefault, 0);
    }

    /**
     * This method tests that image tinting is applied to tintable image view
     * in enabled and disabled state across the same image source respects the currently set
     * image source tinting mode.
     */
    @Test
    @SmallTest
    public void testImageTintingAcrossModeChange() {
        final @IdRes int viewId = R.id.view_untinted_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_default, null);
        @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_drawable_blue set on our view
        // that we'll be testing in this method
        @ColorInt int sourceColor = ResourcesCompat.getColor(
                res, R.color.test_blue, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default no tinting in enabled state", view,
                sourceColor, 0);

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the image source. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Set src_in tint mode on our view
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintMode(PorterDuff.Mode.SRC_IN));

        // Load a new color state list, set it on the view and check that the image has
        // switched to the matching entry in newly set color state list.
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_emerald_translucent, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(emeraldColor));
        verifyImageSourceIsColoredAs("New emerald tinting in enabled state under src_in", view,
                emeraldDefault, allowedComponentVariance);

        // Disable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("New emerald tinting in disabled state under src_in", view,
                emeraldDisabled, allowedComponentVariance);

        // Set src_over tint mode on our view. As the currently set tint list is using
        // translucent colors, we expect the actual image source of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintMode(PorterDuff.Mode.SRC_OVER));

        // Enable the view and check that the image has switched to the matching entry
        // in the color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("New emerald tinting in enabled state under src_over", view,
                ColorUtils.compositeColors(emeraldDefault, sourceColor),
                allowedComponentVariance);

        // Disable the view and check that the image has switched to the matching entry
        // in the newly set color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("New emerald tinting in disabled state under src_over",
                view, ColorUtils.compositeColors(emeraldDisabled, sourceColor),
                allowedComponentVariance);
    }

    /**
     * Tests for behavior around setting a tint list without setting a tint mode
     */
    @Test
    @SmallTest
    public void testImageTintingWithDefaultMode() {
        final @IdRes int viewId = R.id.view_untinted_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt final int sandDefault = ResourcesCompat.getColor(
                res, R.color.sand_default, null);
        @ColorInt final int sandDisabled = ResourcesCompat.getColor(
                res, R.color.sand_disabled, null);

        // This is the fill color of R.drawable.test_drawable_blue set on our view
        // that we'll be testing in this method
        @ColorInt final int sourceColor = ResourcesCompat.getColor(
                res, R.color.test_blue, null);

        final ColorStateList sandColorStateList = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_sand, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default no tinting in enabled state",
                view, sourceColor, 0);

        // Applying the tint should immediately switch colors
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(sandColorStateList));
        verifyImageSourceIsColoredAs("Enabled sand tint after supplying tint list",
                view, sandDefault, 0);

        // Disabling the view should now switch colors
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Disabled sand tint after disabling view",
                view, sandDisabled, 0);
    }

    /**
     * This method tests that opaque tinting applied to tintable image source
     * is applied correctly after changing the image source itself.
     */
    @Test
    @SmallTest
    public void testImageOpaqueTintingAcrossImageChange() {
        final @IdRes int viewId = R.id.view_tinted_no_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);

        // Set image source on our view
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setImageResource(
                R.drawable.test_drawable_green));

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default lilac tinting in enabled state on green source",
                view, lilacDefault, 0);

        // Disable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Default lilac tinting in disabled state on green source",
                view, lilacDisabled, 0);

        // Enable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("Default lilac tinting in re-enabled state on green source",
                view, lilacDefault, 0);

        // Set a different image source on our view based on resource ID
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setImageResource(
                R.drawable.test_drawable_red));

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default lilac tinting in enabled state on red source",
                view, lilacDefault, 0);

        // Disable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Default lilac tinting in disabled state on red source",
                view, lilacDisabled, 0);

        // Enable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("Default lilac tinting in re-enabled state on red source",
                view, lilacDefault, 0);
    }

    /**
     * This method tests that translucent tinting applied to tintable image source
     * is applied correctly after changing the image source itself.
     */
    @Test
    @SmallTest
    public void testImageTranslucentTintingAcrossImageChange() {
        final @IdRes int viewId = R.id.view_untinted_no_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_default, null);
        @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_drawable_green that will be set on our view
        // that we'll be testing in this method
        @ColorInt int colorGreen = ResourcesCompat.getColor(
                res, R.color.test_green, null);
        // This is the fill color of R.drawable.test_drawable_red that will be set on our view
        // that we'll be testing in this method
        @ColorInt int colorRed = ResourcesCompat.getColor(
                res, R.color.test_red, null);

        // Set src_over tint mode on our view. As the currently set tint list is using
        // translucent colors, we expect the actual image source of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintMode(PorterDuff.Mode.SRC_OVER));
        // Load and set a translucent color state list as the image source tint list
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_emerald_translucent, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(emeraldColor));

        // Set image source on our view
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setImageResource(
                R.drawable.test_drawable_green));

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the image source. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Test the default state for tinting set up with the just loaded tint list.
        verifyImageSourceIsColoredAs("Emerald tinting in enabled state on green source",
                view, ColorUtils.compositeColors(emeraldDefault, colorGreen),
                allowedComponentVariance);

        // Disable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Emerald tinting in disabled state on green source",
                view, ColorUtils.compositeColors(emeraldDisabled, colorGreen),
                allowedComponentVariance);

        // Enable the view and check that the image has switched to the matching entry
        // in the default color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("Emerald tinting in re-enabled state on green source",
                view, ColorUtils.compositeColors(emeraldDefault, colorGreen),
                allowedComponentVariance);

        // Set a different image source on our view based on resource ID
        onView(withId(viewId)).perform(AppCompatTintableViewActions.setImageResource(
                R.drawable.test_drawable_red));

        // Test the default state for tinting the new image with the same color state list
        verifyImageSourceIsColoredAs("Emerald tinting in enabled state on red source",
                view, ColorUtils.compositeColors(emeraldDefault, colorRed),
                allowedComponentVariance);

        // Disable the view and check that the image has switched to the matching entry
        // in our current color state list.
        onView(withId(viewId)).perform(setEnabled(false));
        verifyImageSourceIsColoredAs("Emerald tinting in disabled state on red source",
                view, ColorUtils.compositeColors(emeraldDisabled, colorRed),
                allowedComponentVariance);

        // Enable the view and check that the image has switched to the matching entry
        // in our current color state list.
        onView(withId(viewId)).perform(setEnabled(true));
        verifyImageSourceIsColoredAs("Emerald tinting in re-enabled state on red source",
                view, ColorUtils.compositeColors(emeraldDefault, colorRed),
                allowedComponentVariance);
    }

    /**
     * This method tests that background tinting applied on a tintable image view does not
     * affect the tinting of the image source.
     */
    @Test
    @SmallTest
    public void testImageTintingAcrossBackgroundTintingChange() {
        final @IdRes int viewId = R.id.view_untinted_source;
        final Resources res = mActivity.getResources();
        final T view = (T) mContainer.findViewById(viewId);

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);
        // This is the fill color of R.drawable.test_drawable_blue set on our view
        // that we'll be testing in this method
        @ColorInt int sourceColor = ResourcesCompat.getColor(
                res, R.color.test_blue, null);
        @ColorInt int newSourceColor = ResourcesCompat.getColor(
                res, R.color.test_red, null);

        // Test the default state for tinting set up in the layout XML file.
        verifyImageSourceIsColoredAs("Default no tinting in enabled state", view,
                sourceColor, 0);

        // Change background tinting of our image
        final ColorStateList lilacColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_lilac, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setBackgroundResource(
                        R.drawable.test_background_green));
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintMode(PorterDuff.Mode.SRC_IN));
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(lilacColor));

        // Verify that the image still has the original color (untinted)
        verifyImageSourceIsColoredAs("No image tinting after change in background tinting", view,
                sourceColor, 0);

        // Now set a different image source
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageResource(R.drawable.test_drawable_red));
        // And verify that the image has the new color (untinted)
        verifyImageSourceIsColoredAs("No image tinting after change of image source", view,
                newSourceColor, 0);

        // Change the background tinting again
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                mResources, R.color.color_state_list_sand, null);
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(sandColor));
        // And verify that the image still has the same new color (untinted)
        verifyImageSourceIsColoredAs("No image tinting after change in background tinting", view,
                newSourceColor, 0);

        // Now set up image tinting on our view. We're using a color state list with fully
        // opaque colors, and we expect the matching entry in that list to be applied on the
        // image source (ignoring the background tinting)
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintMode(PorterDuff.Mode.SRC_IN));
        onView(withId(viewId)).perform(
                AppCompatTintableViewActions.setImageSourceTintList(lilacColor));
        verifyImageSourceIsColoredAs("New lilac image tinting", view,
                lilacDefault, 0);
    }
}
