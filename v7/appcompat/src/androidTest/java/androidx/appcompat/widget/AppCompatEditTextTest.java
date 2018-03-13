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

import android.content.Context;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;

import androidx.appcompat.app.AppCompatActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for the {@link AppCompatEditText} class.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextTest {
    @Rule
    public final ActivityTestRule<AppCompatActivity> mActivityTestRule =
            new ActivityTestRule<>(AppCompatActivity.class);

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
}
