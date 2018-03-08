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

package androidx.emoji.widget;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(maxSdkVersion = 18)
public class EmojiEditTextHelperPre19Test {
    EmojiEditTextHelper mEmojiEditTextHelper;

    @Before
    public void setup() {
        final EditText editText = mock(EditText.class);
        mEmojiEditTextHelper = new EmojiEditTextHelper(editText);
        verifyNoMoreInteractions(editText);
    }

    @Test
    public void testGetKeyListener_returnsSameKeyListener() {
        final KeyListener param = mock(KeyListener.class);
        final KeyListener keyListener = mEmojiEditTextHelper.getKeyListener(
                param);

        assertSame(param, keyListener);
    }

    @Test
    public void testGetOnCreateInputConnection_returnsSameInputConnection() {
        final InputConnection param = mock(InputConnection.class);
        final InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(param,
                new EditorInfo());

        assertSame(param, inputConnection);
    }

    @Test
    public void testGetOnCreateInputConnection_withNullAttrs_returnsSameInputConnection() {
        final InputConnection param = mock(InputConnection.class);
        final InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(param,
                null);

        assertSame(param, inputConnection);
    }

    @Test
    public void testGetOnCreateInputConnection_withNullInputConnection_returnsNull() {
        final InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(null,
                new EditorInfo());
        assertNull(inputConnection);
    }

    @Test
    public void testDoesNotAttachTextWatcher() {
        final EditText editText = mock(EditText.class);

        mEmojiEditTextHelper = new EmojiEditTextHelper(editText);

        verify(editText, times(0)).addTextChangedListener(any(TextWatcher.class));
    }

}
