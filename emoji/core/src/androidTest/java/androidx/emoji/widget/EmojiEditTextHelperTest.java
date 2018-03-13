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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.emoji.text.EmojiCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiEditTextHelperTest {
    EmojiEditTextHelper mEmojiEditTextHelper;
    EditText mEditText;

    @Before
    public void setup() {
        EmojiCompat.reset(mock(EmojiCompat.class));
        mEditText = new EditText(InstrumentationRegistry.getTargetContext());
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);
    }

    @Test(expected = NullPointerException.class)
    public void testGetKeyListener_withNull_throwsException() {
        mEmojiEditTextHelper.getKeyListener(null);
    }

    @Test
    public void testGetKeyListener_returnsEmojiKeyListener() {
        final KeyListener keyListener = mEmojiEditTextHelper.getKeyListener(
                mock(KeyListener.class));

        assertThat(keyListener, instanceOf(EmojiKeyListener.class));
    }

    @Test
    public void testGetKeyListener_doesNotCreateNewInstance() {
        KeyListener mockKeyListener = mock(KeyListener.class);
        final KeyListener keyListener1 = mEmojiEditTextHelper.getKeyListener(mockKeyListener);
        final KeyListener keyListener2 = mEmojiEditTextHelper.getKeyListener(keyListener1);
        assertSame(keyListener1, keyListener2);
    }

    @Test
    public void testGetOnCreateInputConnection_withNullAttrs_returnsInputConnection() {
        final InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(
                mock(InputConnection.class), null);
        assertNotNull(inputConnection);
        assertThat(inputConnection, instanceOf(EmojiInputConnection.class));
    }

    @Test
    public void testGetOnCreateInputConnection_withNullInputConnection_returnsNull() {
        InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(null,
                new EditorInfo());
        assertNull(inputConnection);
    }

    @Test
    public void testGetOnCreateInputConnection_returnsEmojiInputConnection() {
        final InputConnection inputConnection = mEmojiEditTextHelper.onCreateInputConnection(
                mock(InputConnection.class), null);
        assertNotNull(inputConnection);
        assertThat(inputConnection, instanceOf(EmojiInputConnection.class));
    }

    @Test
    public void testGetOnCreateInputConnection_doesNotCreateNewInstance() {
        final InputConnection ic1 = mEmojiEditTextHelper.onCreateInputConnection(
                mock(InputConnection.class), null);
        final InputConnection ic2 = mEmojiEditTextHelper.onCreateInputConnection(ic1, null);

        assertSame(ic1, ic2);
    }

    @Test
    public void testAttachesTextWatcher() {
        mEditText = mock(EditText.class);
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);

        final ArgumentCaptor<TextWatcher> argumentCaptor = ArgumentCaptor.forClass(
                TextWatcher.class);

        verify(mEditText, times(1)).addTextChangedListener(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), instanceOf(EmojiTextWatcher.class));
    }

    @Test
    public void testSetMaxCount() {
        mEditText = mock(EditText.class);
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);
        // capture TextWatcher
        final ArgumentCaptor<TextWatcher> argumentCaptor = ArgumentCaptor.forClass(
                TextWatcher.class);
        verify(mEditText, times(1)).addTextChangedListener(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), instanceOf(EmojiTextWatcher.class));
        final EmojiTextWatcher emojiTextWatcher = (EmojiTextWatcher) argumentCaptor.getValue();

        mEmojiEditTextHelper.setMaxEmojiCount(1);

        assertEquals(1, emojiTextWatcher.getMaxEmojiCount());
    }

    @Test
    public void testSetEmojiReplaceStrategy() {
        mEditText = mock(EditText.class);
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);

        //assert the default value
        assertEquals(EmojiCompat.REPLACE_STRATEGY_DEFAULT,
                mEmojiEditTextHelper.getEmojiReplaceStrategy());

        // capture TextWatcher
        final ArgumentCaptor<TextWatcher> argumentCaptor = ArgumentCaptor.forClass(
                TextWatcher.class);
        verify(mEditText, times(1)).addTextChangedListener(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), instanceOf(EmojiTextWatcher.class));
        final EmojiTextWatcher emojiTextWatcher = (EmojiTextWatcher) argumentCaptor.getValue();

        mEmojiEditTextHelper.setEmojiReplaceStrategy(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        assertEquals(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT,
                mEmojiEditTextHelper.getEmojiReplaceStrategy());

        assertEquals(EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT,
                emojiTextWatcher.getEmojiReplaceStrategy());
    }

}
