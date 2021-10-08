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

package androidx.emoji2.viewsintegration;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.text.TextWatcher;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;

import androidx.emoji2.text.EmojiCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiEditTextHelperDisabledTest {
    EmojiEditTextHelper mEmojiEditTextHelper;
    EditText mEditText;

    @Before
    public void setup() {
        EmojiCompat.reset(mock(EmojiCompat.class));
        mEditText = new EditText(ApplicationProvider.getApplicationContext());
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);
        mEmojiEditTextHelper.setEnabled(/* isEnabled */ false);
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

    @Test
    public void textWatcher_isDisabled_whenDisabled() {
        mEditText = mock(EditText.class);
        mEmojiEditTextHelper = new EmojiEditTextHelper(mEditText);
        mEmojiEditTextHelper.setEnabled(false);
        // capture TextWatcher
        final ArgumentCaptor<TextWatcher> argumentCaptor = ArgumentCaptor.forClass(
                TextWatcher.class);
        verify(mEditText, times(1)).addTextChangedListener(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), instanceOf(EmojiTextWatcher.class));
        final EmojiTextWatcher emojiTextWatcher = (EmojiTextWatcher) argumentCaptor.getValue();
        assertFalse(emojiTextWatcher.isEnabled());
    }
}
