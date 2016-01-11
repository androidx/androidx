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
package android.support.v7.widget;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.appcompat.test.R;
import android.support.v7.testutils.AppCompatTextViewActions;
import android.support.v7.testutils.AppCompatTintableViewActions;
import android.support.v7.testutils.TestUtils;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.ViewGroup;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class AppCompatTextViewTest
        extends ActivityInstrumentationTestCase2<AppCompatTextViewActivity> {
    private ViewGroup mContainer;

    public AppCompatTextViewTest() {
        super(AppCompatTextViewActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final AppCompatTextViewActivity activity = getActivity();
        mContainer = (ViewGroup) activity.findViewById(R.id.container);
    }

    @SmallTest
    public void testAllCaps() throws Throwable {
        final Resources res = getActivity().getResources();
        final String text1 = res.getString(R.string.sample_text1);
        final String text2 = res.getString(R.string.sample_text2);

        final AppCompatTextView textView1 =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_caps1);
        final AppCompatTextView textView2 =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_caps2);

        // Note that TextView.getText() returns the original text. We are interested in
        // the transformed text that is set on the Layout object used to draw the final
        // (transformed) content.
        assertEquals("Text view starts in all caps on", text1.toUpperCase(),
                textView1.getLayout().getText());
        assertEquals("Text view starts in all caps off", text2,
                textView2.getLayout().getText());

        // Toggle all-caps mode on the two text views. Note that as with the core TextView,
        // setting a style with textAllCaps=false on a AppCompatTextView with all-caps on
        // will have no effect.
        onView(withId(R.id.text_view_caps1)).perform(
                AppCompatTextViewActions.setTextAppearance(R.style.TextStyleAllCapsOff));
        onView(withId(R.id.text_view_caps2)).perform(
                AppCompatTextViewActions.setTextAppearance(R.style.TextStyleAllCapsOn));

        assertEquals("Text view is still in all caps on", text1.toUpperCase(),
                textView1.getLayout().getText());
        assertEquals("Text view is in all caps on", text2.toUpperCase(),
                textView2.getLayout().getText());
    }

    private void verifyBackgroundIsColoredAs(String description,
            @NonNull AppCompatTextView textView, @ColorInt int color,
            int allowedComponentVariance) {
        Drawable background = textView.getBackground();
        TestUtils.assertAllPixelsOfColor(description,
                background, textView.getWidth(), textView.getHeight(), true,
                color, allowedComponentVariance, false);
    }

    /**
     * This method tests that background tinting is not applied when the
     * <code>AppCompatTextView</code> has no background.
     */
    @SmallTest
    public void testBackgroundTintingWithNoBackground() {
        final @IdRes int textViewId = R.id.text_view_tinted_no_background;
        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(textViewId);

        // Note that all the asserts in this test check that the AppCompatTextView background
        // is null. This is because the matching child in the activity doesn't define any
        // background on itself, and there is nothing to tint.

        assertNull("No background after XML loading", textView.getBackground());

        // Disable the text view and check that the background is still null.
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        assertNull("No background after disabling", textView.getBackground());

        // Enable the text view and check that the background is still null.
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        assertNull("No background after re-enabling", textView.getBackground());

        // Load a new color state list, set it on the text view and check that the background
        // is still null.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                getActivity().getResources(), R.color.color_state_list_sand, null);
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(sandColor));

        // Disable the text view and check that the background is still null.
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        assertNull("No background after disabling", textView.getBackground());

        // Enable the text view and check that the background is still null.
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        assertNull("No background after re-enabling", textView.getBackground());
    }

    /**
     * This method tests that background tinting is applied to <code>AppCompatTextView</code>
     * in enabled and disabled state across a number of <code>ColorStateList</code>s set as
     * background tint lists on the same background.
     */
    @SmallTest
    public void testBackgroundTintingAcrossStateChange() {
        final @IdRes int textViewId = R.id.text_view_tinted_background;
        final Resources res = getActivity().getResources();

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);
        @ColorInt int sandDefault = ResourcesCompat.getColor(res, R.color.sand_default, null);
        @ColorInt int sandDisabled = ResourcesCompat.getColor(res, R.color.sand_disabled, null);
        @ColorInt int oceanDefault = ResourcesCompat.getColor(res, R.color.ocean_default, null);
        @ColorInt int oceanDisabled = ResourcesCompat.getColor(res, R.color.ocean_disabled, null);

        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(textViewId);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state", textView,
                lilacDefault, 0);

        // Disable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state", textView,
                lilacDisabled, 0);

        // Enable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state", textView,
                lilacDefault, 0);

        // Load a new color state list, set it on the text view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_sand, null);
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(sandColor));
        verifyBackgroundIsColoredAs("New sand tinting in enabled state", textView,
                sandDefault, 0);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New sand tinting in disabled state", textView,
                sandDisabled, 0);

        // Enable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("New sand tinting in re-enabled state", textView,
                sandDefault, 0);

        // Load another color state list, set it on the text view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_ocean, null);
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(oceanColor));
        verifyBackgroundIsColoredAs("New ocean tinting in enabled state", textView,
                oceanDefault, 0);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New ocean tinting in disabled state", textView,
                oceanDisabled, 0);

        // Enable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("New ocean tinting in re-enabled state", textView,
                oceanDefault, 0);
    }

    /**
     * This method tests that background tinting applied to <code>AppCompatTextView</code>
     * in enabled and disabled state across the same background respects the currently set
     * background tinting mode.
     */
    @SmallTest
    public void testBackgroundTintingAcrossModeChange() {
        final @IdRes int textViewId = R.id.text_view_untinted_background;
        final Resources res = getActivity().getResources();

        @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_default, null);
        @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_background_green set on our text view
        // that we'll be testing in this method
        @ColorInt int backgroundColor = ResourcesCompat.getColor(
                res, R.color.test_green, null);

        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(textViewId);

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default no tinting in enabled state", textView,
                backgroundColor, 0);

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the text view background. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.
        final int allowedComponentVariance = 2;

        // Set src_in tint mode on our text view
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintMode(PorterDuff.Mode.SRC_IN));

        // Load a new color state list, set it on the text view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_emerald_translucent, null);
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(emeraldColor));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_in", textView,
                emeraldDefault, allowedComponentVariance);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_in", textView,
                emeraldDisabled, allowedComponentVariance);

        // Set src_over tint mode on our text view. As the currently set tint list is using
        // translucent colors, we expect the actual background of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER));

        // Enable the text view and check that the background has switched to the matching entry
        // in the color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("New emerald tinting in enabled state under src_over", textView,
                ColorUtils.compositeColors(emeraldDefault, backgroundColor),
                allowedComponentVariance);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New emerald tinting in disabled state under src_over",
                textView, ColorUtils.compositeColors(emeraldDisabled, backgroundColor),
                allowedComponentVariance);
    }

    /**
     * This method tests that opaque background tinting applied to <code>AppCompatTextView</code>
     * is applied correctly after changing the background itself of the view.
     */
    @SmallTest
    public void testBackgroundOpaqueTintingAcrossBackgroundChange() {
        final @IdRes int textViewId = R.id.text_view_tinted_no_background;
        final Resources res = getActivity().getResources();

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);

        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(textViewId);

        assertNull("No background after XML loading", textView.getBackground());

        // Set background on our text view
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setBackgroundDrawable(
                ResourcesCompat.getDrawable(res, R.drawable.test_background_green, null)));

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state on green BG",
                textView, lilacDefault, 0);

        // Disable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state on green BG",
                textView, lilacDisabled, 0);

        // Enable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state on green BG",
                textView, lilacDefault, 0);

        // Set a different background on our view based on resource ID
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setBackgroundResource(
                R.drawable.test_background_red));

        // Test the default state for tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state on red BG",
                textView, lilacDefault, 0);

        // Disable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state on red BG",
                textView, lilacDisabled, 0);

        // Enable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state on red BG",
                textView, lilacDefault, 0);
    }

    /**
     * This method tests that translucent background tinting applied to <code>AppCompatTextView</code>
     * is applied correctly after changing the background itself of the view.
     */
    @SmallTest
    public void testBackgroundTranslucentTintingAcrossBackgroundChange() {
        final @IdRes int textViewId = R.id.text_view_untinted_no_background;
        final Resources res = getActivity().getResources();

        @ColorInt int emeraldDefault = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_default, null);
        @ColorInt int emeraldDisabled = ResourcesCompat.getColor(
                res, R.color.emerald_translucent_disabled, null);
        // This is the fill color of R.drawable.test_background_green set on our text view
        // that we'll be testing in this method
        @ColorInt int backgroundColorGreen = ResourcesCompat.getColor(
                res, R.color.test_green, null);
        @ColorInt int backgroundColorRed = ResourcesCompat.getColor(
                res, R.color.test_red, null);

        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(textViewId);

        assertNull("No background after XML loading", textView.getBackground());

        // Set src_over tint mode on our text view. As the currently set tint list is using
        // translucent colors, we expect the actual background of the view to be different under
        // this new mode (unlike src_in and src_over that behave identically when the destination is
        // a fully filled rectangle and the source is an opaque color).
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER));
        // Load and set a translucent color state list as the background tint list
        final ColorStateList emeraldColor = ResourcesCompat.getColorStateList(
                res, R.color.color_state_list_emerald_translucent, null);
        onView(withId(textViewId)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(emeraldColor));

        // Set background on our text view
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setBackgroundDrawable(
                ResourcesCompat.getDrawable(res, R.drawable.test_background_green, null)));

        // From this point on in this method we're allowing a margin of error in checking the
        // color of the text view background. This is due to both translucent colors being used
        // in the color state list and off-by-one discrepancies of SRC_OVER when it's compositing
        // translucent color on top of solid fill color. This is where the allowed variance
        // value of 2 comes from - one for compositing and one for color translucency.

        // Test the default state for tinting set up with the just loaded tint list.
        verifyBackgroundIsColoredAs("Emerald tinting in enabled state on green BG",
                textView, ColorUtils.compositeColors(emeraldDefault, backgroundColorGreen), 2);

        // Disable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Emerald tinting in disabled state on green BG",
                textView, ColorUtils.compositeColors(emeraldDisabled, backgroundColorGreen), 2);

        // Enable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Emerald tinting in re-enabled state on green BG",
                textView, ColorUtils.compositeColors(emeraldDefault, backgroundColorGreen), 2);

        // Set a different background on our view based on resource ID
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setBackgroundResource(
                R.drawable.test_background_red));

        // Test the default state for tinting the new background with the same color state list
        verifyBackgroundIsColoredAs("Emerald tinting in enabled state on red BG",
                textView, ColorUtils.compositeColors(emeraldDefault, backgroundColorRed), 2);

        // Disable the text view and check that the background has switched to the matching entry
        // in our current color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Emerald tinting in disabled state on red BG",
                textView, ColorUtils.compositeColors(emeraldDisabled, backgroundColorRed), 2);

        // Enable the text view and check that the background has switched to the matching entry
        // in our current color state list.
        onView(withId(textViewId)).perform(AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Emerald tinting in re-enabled state on red BG",
                textView, ColorUtils.compositeColors(emeraldDefault, backgroundColorRed), 2);
    }
}
