/*
 * Copyright 2018 The Android Open Source Project
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
import static androidx.core.view.ViewKt.drawToBitmap;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Layout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;

import androidx.annotation.ColorRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.test.R;
import androidx.appcompat.testutils.TestUtils;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.TextViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the {@link AppCompatEditText} class.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextTest {
    @Rule
    public final ActivityTestRule<AppCompatEditTextActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatEditTextActivity.class);

    private AppCompatEditTextActivity mActivity;
    private Resources mResources;

    @Before
    public void setUp() {
        mActivity = mActivityTestRule.getActivity();
        mResources = mActivity.getResources();
    }

    @Test
    @UiThreadTest
    public void testGetTextNonEditable() {
        // This subclass calls getText before the object is fully constructed. This should not cause
        // a null pointer exception.
        GetTextEditText editText = new GetTextEditText(mActivityTestRule.getActivity());
    }

    private class GetTextEditText extends AppCompatEditText {

        GetTextEditText(Context context) {
            super(context);
        }

        @Override
        public void setText(CharSequence text, BufferType type) {
            Editable currentText = getText();
            super.setText(text, type);
        }
    }

    @Test
    @UiThreadTest
    public void testGetTextBeforeConstructor() {
        // This subclass calls getText before the TextView constructor. This should not cause
        // a null pointer exception.
        GetTextEditText2 editText = new GetTextEditText2(mActivityTestRule.getActivity());
    }

    private class GetTextEditText2 extends AppCompatEditText {

        GetTextEditText2(Context context) {
            super(context);
        }

        @Override
        public void setOverScrollMode(int overScrollMode) {
            // This method is called by the View constructor before the TextView/EditText
            // constructors.
            Editable text = getText();
        }
    }

    @Test
    public void testHyphenationFrequencyDefaultValue_withDefaultConstructor() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, editText.getHyphenationFrequency());
    }

    @Test
    public void testHyphenationFrequencyDefaultValue_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.edit_text_default_values);
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, editText.getHyphenationFrequency());
    }

    @Test
    public void testHyphenationFrequencyOverride_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.text_view_hyphen_break_override);
        assertEquals(Layout.HYPHENATION_FREQUENCY_FULL, editText.getHyphenationFrequency());
    }

    @Test
    public void testBreakStrategyDefaultValue_withDefaultConstructor() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        assertEquals(Layout.BREAK_STRATEGY_SIMPLE, editText.getBreakStrategy());
    }

    @Test
    public void testBreakStrategyDefaultValue_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.edit_text_default_values);
        assertEquals(Layout.BREAK_STRATEGY_SIMPLE, editText.getBreakStrategy());
    }

    @Test
    public void testBreakStrategyOverride_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.text_view_hyphen_break_override);
        assertEquals(Layout.BREAK_STRATEGY_BALANCED, editText.getBreakStrategy());
    }

    private void verifyTextHintColor(EditText textView,
            @ColorRes int expectedEnabledColor, @ColorRes int expectedDisabledColor) {
        ColorStateList hintColorStateList = textView.getHintTextColors();
        assertEquals(ContextCompat.getColor(textView.getContext(), expectedEnabledColor),
                hintColorStateList.getColorForState(new int[]{android.R.attr.state_enabled}, 0));
        assertEquals(ContextCompat.getColor(textView.getContext(), expectedDisabledColor),
                hintColorStateList.getColorForState(new int[]{-android.R.attr.state_enabled}, 0));
    }

    @Test
    @UiThreadTest
    public void testTextHintColor() {
        EditText editLinkEnabledView = mActivityTestRule.getActivity().findViewById(
                R.id.view_edit_hint_enabled);
        EditText editLinkDisabledView = mActivityTestRule.getActivity().findViewById(
                R.id.view_edit_hint_disabled);

        // Verify initial enabled and disabled text hint colors set from the activity theme
        verifyTextHintColor(editLinkEnabledView, R.color.ocean_default, R.color.ocean_disabled);
        verifyTextHintColor(editLinkDisabledView, R.color.ocean_default, R.color.ocean_disabled);

        // Set new text appearance on the two views - the appearance has new text hint color
        // state list that references theme-level attributes. And verify that the new text
        // hint colors are correctly resolved.
        TextViewCompat.setTextAppearance(editLinkEnabledView, R.style.TextStyleNew);
        verifyTextHintColor(editLinkEnabledView, R.color.lilac_default, R.color.lilac_disabled);
        TextViewCompat.setTextAppearance(editLinkDisabledView, R.style.TextStyleNew);
        verifyTextHintColor(editLinkDisabledView, R.color.lilac_default, R.color.lilac_disabled);
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testGetTextClassifier() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        editText.getTextClassifier();
        NoOpTextClassifier noOpTextClassifier = new NoOpTextClassifier();

        TextClassificationManager textClassificationManager =
                mActivityTestRule.getActivity().getSystemService(TextClassificationManager.class);
        textClassificationManager.setTextClassifier(noOpTextClassifier);

        assertEquals(noOpTextClassifier, editText.getTextClassifier());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testSetTextClassifier() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        NoOpTextClassifier noOpTextClassifier = new NoOpTextClassifier();

        editText.setTextClassifier(noOpTextClassifier);

        assertEquals(noOpTextClassifier, editText.getTextClassifier());
    }

    private static class NoOpTextClassifier implements TextClassifier {}

    @UiThreadTest
    public void testSetCustomSelectionActionModeCallback() {
        final AppCompatEditText view = new AppCompatEditText(mActivityTestRule.getActivity());
        final ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        };

        // Default value is documented as null.
        assertNull(view.getCustomSelectionActionModeCallback());

        // Setter and getter should be symmetric.
        view.setCustomSelectionActionModeCallback(callback);
        assertEquals(callback, view.getCustomSelectionActionModeCallback());

        // Argument is nullable.
        view.setCustomSelectionActionModeCallback(null);
        assertNull(view.getCustomSelectionActionModeCallback());
    }

    @Test
    public void testCompoundDrawablesTint() {
        // Given an ACTV with a white drawableLeftCompat set and a #f0f drawableTint
        final AppCompatEditText textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Test
    public void testCompoundDrawableRelativeTint() {
        // Given an ACTV with a white drawableStartCompat set and a #f0f drawableTint
        final AppCompatEditText textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_relative_tint);
        final int tint = 0xffff00ff;
        // Then the drawable should be tinted
        final Drawable drawable = TextViewCompat.getCompoundDrawablesRelative(textView)[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(ColorStateList.valueOf(tint),
                TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @Ignore // b/259134876
    @Test
    public void testCompoundDrawablesTintList() {
        // Given an ACTV with a white drawableLeftCompat and a ColorStateList drawableTint set
        final AppCompatEditText textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_list);
        final int defaultTint = ResourcesCompat.getColor(mResources, R.color.lilac_default, null);
        final int disabledTint = ResourcesCompat.getColor(mResources, R.color.lilac_disabled, null);

        // Then the initial drawable tint is applied
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                defaultTint,
                0,
                true);

        // When the view is disabled
        onView(withId(textView.getId())).perform(scrollTo(), setEnabled(false));
        // Then the appropriate drawable tint is applied
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                disabledTint,
                0,
                true);
    }

    @Test
    public void testCompoundDrawablesTintMode() {
        // Given an ACTV with a red drawableLeft, a semi-transparent blue drawableTint
        // & drawableTintMode of src_over
        final AppCompatEditText textView = mActivity.findViewById(
                R.id.text_view_compound_drawable_tint_mode);
        final int expected = ColorUtils.compositeColors(0x800000ff, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending
        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter returns the mode
        assertEquals(PorterDuff.Mode.SRC_OVER,
                TextViewCompat.getCompoundDrawableTintMode(textView));
    }

    @UiThreadTest
    @Test
    public void testSetCompoundDrawablesTintList() {
        // Given an ACTV with a compound drawable
        final AppCompatEditText textView = new AppCompatEditText(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // When a tint is set programmatically
        final int tint = 0xffa4c639;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
        // Then the TextViewCompat getter should return the tint
        assertEquals(tintList, TextViewCompat.getCompoundDrawableTintList(textView));
    }

    @UiThreadTest
    @Test
    public void testSetCompoundDrawablesTintMode() {
        // Given an ACTV with a red compound drawable
        final AppCompatEditText textView = new AppCompatEditText(mActivity);
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.red_square), null, null, null);

        // When a semi-transparent blue tint is set programmatically with a mode of SRC_OVER
        final int tint = 0x800000ff;
        final PorterDuff.Mode mode = PorterDuff.Mode.SRC_OVER;
        final ColorStateList tintList = ColorStateList.valueOf(tint);
        TextViewCompat.setCompoundDrawableTintList(textView, tintList);
        TextViewCompat.setCompoundDrawableTintMode(textView, mode);
        final int expected = ColorUtils.compositeColors(tint, 0xffff0000);
        final int tolerance = 2; // allow some tolerance for the blending

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                expected,
                tolerance,
                true);
        // Then the TextViewCompat getter should return the tint mode
        assertEquals(mode, TextViewCompat.getCompoundDrawableTintMode(textView));
    }

    @UiThreadTest
    @Test
    public void testCompoundDrawablesSetAfterTint() {
        // Given an ACTV with a magenta tint
        final AppCompatEditText textView = new AppCompatEditText(mActivity);
        final int tint = 0xffff00ff;
        TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(tint));

        // When a white compound drawable is set
        textView.setCompoundDrawables(AppCompatResources.getDrawable(
                mActivity, R.drawable.white_square), null, null, null);

        // Then the drawable should be tinted
        final Drawable drawable = textView.getCompoundDrawables()[0];
        TestUtils.assertAllPixelsOfColor(
                "Tint not applied to AppCompatEditText compound drawable",
                drawable,
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                true,
                tint,
                0,
                true);
    }

    @RequiresApi(26)
    private void assertSystemHasVariableFont() {
        AppCompatEditText editText = new AppCompatEditText(mActivity);
        editText.setText("Hello, world.");
        editText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        editText.measure(
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST)
        );
        editText.layout(0, 0, editText.getMeasuredWidth(), editText.getMeasuredHeight());

        editText.setFontVariationSettings("'wght' 400");
        Bitmap regular = drawToBitmap(editText, Bitmap.Config.ARGB_8888);

        editText.setFontVariationSettings("'wght' 700");
        Bitmap bold = drawToBitmap(editText, Bitmap.Config.ARGB_8888);

        assumeFalse(regular.sameAs(bold));
    }

    @UiThreadTest
    @SdkSuppress(minSdkVersion = 31)
    @Test
    public void testFontAdjustment() {
        // Skip if the system default font is not a variable font.
        assertSystemHasVariableFont();

        AppCompatEditText editText = new AppCompatEditText(mActivity);
        editText.setText("Hello, world.");
        editText.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        editText.measure(
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.AT_MOST)
        );
        editText.layout(0, 0, editText.getMeasuredWidth(), editText.getMeasuredHeight());

        editText.setFontVariationSettings("'wght' 400.0");
        Bitmap before = drawToBitmap(editText, Bitmap.Config.ARGB_8888);

        editText.getFontVariationSettingsManager().setFontWeightAdjustmentForTesting(300);
        editText.setFontVariationSettings("'wght' 400.0");
        Bitmap after = drawToBitmap(editText, Bitmap.Config.ARGB_8888);

        // The font weight adjustment should change the weight of the variation settings.
        assertFalse(before.sameAs(after));
    }
}
