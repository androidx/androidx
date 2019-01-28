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

import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.view.textclassifier.TextClassificationManager;
import android.view.textclassifier.TextClassifier;

import androidx.appcompat.test.R;
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

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testGetTextClassifier() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        editText.getTextClassifier();
        DummyTextClassifier dummyTextClassifier = new DummyTextClassifier();

        TextClassificationManager textClassificationManager =
                mActivityTestRule.getActivity().getSystemService(TextClassificationManager.class);
        textClassificationManager.setTextClassifier(dummyTextClassifier);

        assertEquals(dummyTextClassifier, editText.getTextClassifier());
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    public void testSetTextClassifier() {
        final AppCompatEditText editText = new AppCompatEditText(mActivityTestRule.getActivity());
        DummyTextClassifier dummyTextClassifier = new DummyTextClassifier();

        editText.setTextClassifier(dummyTextClassifier);

        assertEquals(dummyTextClassifier, editText.getTextClassifier());
    }

    private static class DummyTextClassifier implements TextClassifier {}
}
