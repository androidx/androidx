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

import static org.mockito.Mockito.mock;

import android.text.InputType;
import android.text.method.KeyListener;
import android.widget.TextView;

import androidx.appcompat.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppCompatEditTextEmojiTest
        extends AppCompatBaseTextViewEmojiTest<AppCompatEditTextEmojiActivity, AppCompatTextView> {

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
                mActivityTestRule.getActivity().findViewById(R.id.not_focusable);

        assertThat(notFocusable.isEnabled()).isFalse();
        assertThat(notFocusable.isFocusable()).isFalse();
    }

    @Test
    @UiThreadTest
    public void setKeyListener_hasSameFocusChangeBehavior_asPlatform() {
        TextView platformTextView = new TextView(mActivityTestRule.getActivity());
        platformTextView.setFocusable(false);
        platformTextView.setEnabled(false);

        AppCompatEditText notFocusable =
                mActivityTestRule.getActivity().findViewById(R.id.not_focusable);

        KeyListener keyListener = mock(KeyListener.class);
        notFocusable.setKeyListener(keyListener);
        platformTextView.setKeyListener(keyListener);

        assertThat(notFocusable.isFocusable()).isEqualTo(platformTextView.isFocusable());
        assertThat(notFocusable.isEnabled()).isEqualTo(platformTextView.isEnabled());
    }

    @Test
    @UiThreadTest
    public void constructor_doesntClobber_inputType() {
        AppCompatEditText textEmailAddress =
                mActivityTestRule.getActivity().findViewById(R.id.text_email_address);
        int actual = textEmailAddress.getInputType();
        assertThat(actual).isEqualTo(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    }

    @Test
    @UiThreadTest
    public void setKeyListener_hasSameInputTypeBehavior_asPlatform() {
        TextView platformTextView = new TextView(mActivityTestRule.getActivity());
        platformTextView.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        AppCompatEditText textEmailAddress =
                mActivityTestRule.getActivity().findViewById(R.id.text_email_address);

        KeyListener keyListener = mock(KeyListener.class);
        platformTextView.setKeyListener(keyListener);
        textEmailAddress.setKeyListener(keyListener);

        assertThat(textEmailAddress.getInputType()).isEqualTo(platformTextView.getInputType());
    }

    @Test
    @UiThreadTest
    public void setKeyListener_null_setsToNull() {
        AppCompatEditText textEmailAddress =
                mActivityTestRule.getActivity().findViewById(R.id.text_email_address);

        textEmailAddress.setKeyListener(null);
        assertThat(textEmailAddress.getKeyListener()).isNull();
    }
}
