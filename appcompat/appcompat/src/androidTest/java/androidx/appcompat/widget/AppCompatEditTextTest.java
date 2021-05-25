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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.Layout;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;

import androidx.annotation.ColorRes;
import androidx.appcompat.test.R;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;

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

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testHyphenationFrequencyDefaultValue_withDefaultConstructor() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, editText.getHyphenationFrequency());
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testHyphenationFrequencyDefaultValue_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.edit_text_default_values);
        assertEquals(Layout.HYPHENATION_FREQUENCY_NONE, editText.getHyphenationFrequency());
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testHyphenationFrequencyOverride_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.text_view_hyphen_break_override);
        assertEquals(Layout.HYPHENATION_FREQUENCY_FULL, editText.getHyphenationFrequency());
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testBreakStrategyDefaultValue_withDefaultConstructor() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        assertEquals(Layout.BREAK_STRATEGY_SIMPLE, editText.getBreakStrategy());
    }

    @SdkSuppress(minSdkVersion = 23)
    @Test
    public void testBreakStrategyDefaultValue_withInflator() {
        final AppCompatEditText editText = mActivityTestRule.getActivity().findViewById(
                R.id.edit_text_default_values);
        assertEquals(Layout.BREAK_STRATEGY_SIMPLE, editText.getBreakStrategy());
    }

    @SdkSuppress(minSdkVersion = 23)
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
}
