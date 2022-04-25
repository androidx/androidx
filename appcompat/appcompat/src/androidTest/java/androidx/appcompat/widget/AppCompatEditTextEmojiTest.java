/*
 * Copyright 2021 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.NumberKeyListener;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextEmojiTest
        extends AppCompatBaseEditTextEmojiTest<AppCompatEditTextEmojiActivity, AppCompatTextView> {

    public AppCompatEditTextEmojiTest() {
        super(AppCompatEditTextEmojiActivity.class);
    }

    /**
     * setKeyListener can clear tho focusable attribute, which we call during initialization,
     * ensure we don't clobber the focusable from attributes.
     */
    @Test
    @UiThreadTest
    public void respectsFocusableAndEditableAttribute() {
        AppCompatEditText notFocusable =
                mActivityTestRule.getActivity()
                        .findViewById(R.id.not_focusable);

        assertThat(notFocusable.isEnabled()).isFalse();
        assertThat(notFocusable.isFocusable()).isFalse();
    }

    /**
     * Verify b/221094907 is fixed
     */
    @Test
    @UiThreadTest
    public void respectsClickable() {
        AppCompatEditText notClickable = mActivityTestRule.getActivity()
                .findViewById(R.id.not_clickable);

        assertThat(notClickable.isClickable()).isFalse();
        assertThat(notClickable.isLongClickable()).isTrue();
    }

    /**
     * Verify b/221094907 is fixed
     */
    @Test
    @UiThreadTest
    public void respectsLongClickable() {
        AppCompatEditText notLongClickable = mActivityTestRule.getActivity()
                .findViewById(R.id.not_long_clickable);

        assertThat(notLongClickable.isLongClickable()).isFalse();
        assertThat(notLongClickable.isClickable()).isTrue();
    }

    @Test
    @UiThreadTest
    public void respectsDigits() {
        AppCompatEditText textWithDigits = mActivityTestRule.getActivity()
                        .findViewById(R.id.text_with_digits);

        int[] acceptedKeyCodes = {KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4};
        int[] disallowedKeyCodes = {KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_COMMA,
                KeyEvent.KEYCODE_NUMPAD_DOT};
        int[] actions = {KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP};

        for (int action : actions) {
            for (int keycode : acceptedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigits, action, keycode)).isTrue();
            }
            for (int keycode : disallowedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigits, action, keycode)).isFalse();
            }
        }

        assertThat(textWithDigits.getKeyListener()).isInstanceOf(NumberKeyListener.class);
    }

    @Test
    @UiThreadTest
    public void respectsDigitsAndComma() {
        AppCompatEditText textWithDigitsAndComma = mActivityTestRule.getActivity()
                .findViewById(R.id.text_with_digits_and_comma);

        int[] acceptedKeyCodes = {KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_COMMA};
        int[] disallowedKeyCodes = {KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_DOT};
        int[] actions = {KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP};

        for (int action : actions) {
            for (int keycode : acceptedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigitsAndComma, action, keycode))
                        .isTrue();
            }
            for (int keycode : disallowedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigitsAndComma, action, keycode))
                        .isFalse();
            }
        }

        assertThat(textWithDigitsAndComma.getKeyListener()).isInstanceOf(NumberKeyListener.class);
    }

    @Test
    @UiThreadTest
    public void setKeyListener_doesNotWrap_numberKeyListener() {
        KeyListener digitsKeyListener = DigitsKeyListener.getInstance("123456");
        AppCompatEditText textWithDigits = mActivityTestRule.getActivity()
                .findViewById(R.id.text_with_digits);

        textWithDigits.setKeyListener(digitsKeyListener);
        assertThat(textWithDigits.getKeyListener()).isSameInstanceAs(digitsKeyListener);
        assertThat(textWithDigits.getKeyListener()).isInstanceOf(DigitsKeyListener.class);
    }


    private boolean listenerHandlesKeyEvent(AppCompatEditText textWithDigits, int action,
            int keycode) {
        KeyListener listener = textWithDigits.getKeyListener();
        return listener.onKeyDown(textWithDigits, textWithDigits.getText(),
                keycode, new KeyEvent(action, keycode));
    }

    @Test
    @UiThreadTest
    public void whenSubclassing_setKeyListener_notCalledDuringConstructor() {
        class MyEditText extends AppCompatEditText {
            private boolean mSetKeyListenerCalled = false;

            MyEditText(@NonNull Context context) {
                super(context);
            }

            @Override
            public void setKeyListener(@Nullable KeyListener keyListener) {
                super.setKeyListener(keyListener);
                mSetKeyListenerCalled = true;
            }
        }

        MyEditText myEditText = new MyEditText(mActivityTestRule.getActivity());
        assertThat(myEditText.mSetKeyListenerCalled).isFalse();

        myEditText.setKeyListener(DigitsKeyListener.getInstance("1234"));
        assertThat(myEditText.mSetKeyListenerCalled).isTrue();
    }
}

