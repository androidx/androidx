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

import android.text.method.KeyListener;
import android.view.KeyEvent;

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
                        .findViewById(androidx.appcompat.test.R.id.not_focusable);

        assertThat(notFocusable.isEnabled()).isFalse();
        assertThat(notFocusable.isFocusable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void respectsDigits() {
        AppCompatEditText textWithDigits = mActivityTestRule.getActivity()
                        .findViewById(androidx.appcompat.test.R.id.text_with_digits);

        int[] acceptedKeyCodes = {KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
                KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4};
        int[] disallowedKeyCodes = {KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
                KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9};
        int[] actions = {KeyEvent.ACTION_DOWN, KeyEvent.ACTION_UP};

        for (int action : actions) {
            for (int keycode : acceptedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigits, action, keycode)).isTrue();
            }
            for (int keycode : disallowedKeyCodes) {
                assertThat(listenerHandlesKeyEvent(textWithDigits, action, keycode)).isFalse();
            }
        }
    }

    private boolean listenerHandlesKeyEvent(AppCompatEditText textWithDigits, int action,
            int keycode) {
        KeyListener listener = textWithDigits.getKeyListener();
        return listener.onKeyDown(textWithDigits, textWithDigits.getText(),
                keycode, new KeyEvent(action, keycode));
    }
}
