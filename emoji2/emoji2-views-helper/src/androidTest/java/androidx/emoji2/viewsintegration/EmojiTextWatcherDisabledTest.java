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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.text.Spannable;
import android.text.SpannableString;
import android.widget.EditText;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.util.EmojiMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19) // class is not instantiated prior to API19
public class EmojiTextWatcherDisabledTest {

    private EmojiTextWatcher mTextWatcher;
    private EmojiCompat mEmojiCompat;

    @Before
    public void setup() {
        EditText editText = mock(EditText.class);
        mEmojiCompat = mock(EmojiCompat.class);
        EmojiCompat.reset(mEmojiCompat);
        mTextWatcher = new EmojiTextWatcher(editText, /* expectInitializedEmojiCompat */ true);
        mTextWatcher.setEnabled(/* isEnabled */ false);
    }

    @Test
    public void testOnTextChanged_callsProcess() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);
        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void testOnTextChanged_whenEmojiCompatLoading() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_LOADING);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);
        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void testOnTextChanged_whenEmojiCompatLoadFailed() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_FAILED);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void testSetEmojiReplaceStrategy() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        assertEquals(EmojiCompat.REPLACE_STRATEGY_DEFAULT, mTextWatcher.getEmojiReplaceStrategy());

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void testFilter_withManualLoadStrategy() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_DEFAULT);

        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verifyNoMoreInteractions(mEmojiCompat);
    }

    @Test
    public void afterEnable_expectFurtherInteractions() {
        final Spannable testString = new SpannableString("abc");
        when(mEmojiCompat.getLoadState()).thenReturn(EmojiCompat.LOAD_STATE_SUCCEEDED);

        mTextWatcher.setEnabled(/* isEnabled */ true);
        mTextWatcher.onTextChanged(testString, 0, 0, 1);

        verify(mEmojiCompat, times(1)).process(
                EmojiMatcher.sameCharSequence(testString),
                eq(0),
                eq(1),
                eq(Integer.MAX_VALUE),
                anyInt());
        verify(mEmojiCompat, times(0)).registerInitCallback(any(EmojiCompat.InitCallback.class));
    }
}
