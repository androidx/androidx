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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
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
            @NonNull AppCompatTextView textView, @ColorInt int color) {
        final Drawable background = textView.getBackground();
        final Rect backgroundBounds = background.getBounds();
        TestUtils.assertAllPixelsOfColor(description,
                background, backgroundBounds.width(), backgroundBounds.height(), false,
                color, false);
    }

    @SmallTest
    public void testBackgroundTintingWithNoBackground() {
        final AppCompatTextView textView =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_no_background);

        // Note that all the asserts in this test check that the AppCompatTextView background
        // is null. This is because the matching child in the activity doesn't define any
        // background on itself, and there is nothing to tint.

        assertNull("No background after XML loading", textView.getBackground());

        // Disable the text view and check that the background is still null.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        assertNull("No background after disabling", textView.getBackground());

        // Enable the text view and check that the background is still null.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        assertNull("No background after re-enabling", textView.getBackground());

        // Load a new color state list, set it on the text view and check that the background
        // is still null.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                getActivity().getResources(), R.color.color_state_list_sand, null);
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(sandColor));

        // Disable the text view and check that the background is still null.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        assertNull("No background after disabling", textView.getBackground());

        // Enable the text view and check that the background is still null.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        assertNull("No background after re-enabling", textView.getBackground());
    }

    @SmallTest
    public void testBackgroundTinting() {
        final Resources res = getActivity().getResources();

        @ColorInt int lilacDefault = ResourcesCompat.getColor(res, R.color.lilac_default, null);
        @ColorInt int lilacDisabled = ResourcesCompat.getColor(res, R.color.lilac_disabled, null);
        @ColorInt int sandDefault = ResourcesCompat.getColor(res, R.color.sand_default, null);
        @ColorInt int sandDisabled = ResourcesCompat.getColor(res, R.color.sand_disabled, null);
        @ColorInt int oceanDefault = ResourcesCompat.getColor(res, R.color.ocean_default, null);
        @ColorInt int oceanDisabled = ResourcesCompat.getColor(res, R.color.ocean_disabled, null);

        final AppCompatTextView textViewTinted =
                (AppCompatTextView) mContainer.findViewById(R.id.text_view_tinted);

        // Test the default state from tinting set up in the layout XML file.
        verifyBackgroundIsColoredAs("Default lilac tinting in enabled state", textViewTinted,
                lilacDefault);

        // Disable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("Default lilac tinting in disabled state", textViewTinted,
                lilacDisabled);

        // Enable the text view and check that the background has switched to the matching entry
        // in the default color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("Default lilac tinting in re-enabled state", textViewTinted,
                lilacDefault);

        // Load a new color state list, set it on the text view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList sandColor = ResourcesCompat.getColorStateList(
                getActivity().getResources(), R.color.color_state_list_sand, null);
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(sandColor));
        verifyBackgroundIsColoredAs("New sand tinting in enabled state", textViewTinted,
                sandDefault);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New sand tinting in disabled state", textViewTinted,
                sandDisabled);

        // Enable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("New sand tinting in re-enabled state", textViewTinted,
                sandDefault);

        // Load another color state list, set it on the text view and check that the background has
        // switched to the matching entry in newly set color state list.
        final ColorStateList oceanColor = ResourcesCompat.getColorStateList(
                getActivity().getResources(), R.color.color_state_list_ocean, null);
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setBackgroundTintList(oceanColor));
        verifyBackgroundIsColoredAs("New ocean tinting in enabled state", textViewTinted,
                oceanDefault);

        // Disable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(false));
        verifyBackgroundIsColoredAs("New ocean tinting in disabled state", textViewTinted,
                oceanDisabled);

        // Enable the text view and check that the background has switched to the matching entry
        // in the newly set color state list.
        onView(withId(R.id.text_view_tinted)).perform(
                AppCompatTintableViewActions.setEnabled(true));
        verifyBackgroundIsColoredAs("New ocean tinting in re-enabled state", textViewTinted,
                oceanDefault);
    }
}
