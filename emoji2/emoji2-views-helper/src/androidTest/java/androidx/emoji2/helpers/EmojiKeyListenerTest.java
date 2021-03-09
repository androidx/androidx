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
package androidx.emoji2.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;

import androidx.emoji2.util.Emoji;
import androidx.emoji2.util.KeyboardUtil;
import androidx.emoji2.util.TestString;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiKeyListenerTest {

    private KeyListener mKeyListener;
    private TestString mTestString;
    private Editable mEditable;
    private EmojiKeyListener mEmojiKeyListener;
    private EmojiKeyListener.EmojiCompatHandleKeyDownHelper mEmojiCompatKeydownHelper;

    @Before
    public void setup() {
        mKeyListener = mock(KeyListener.class);
        mTestString = new TestString(Emoji.EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        mEmojiCompatKeydownHelper = mock(EmojiKeyListener.EmojiCompatHandleKeyDownHelper.class);
        mEmojiKeyListener = new EmojiKeyListener(mKeyListener, mEmojiCompatKeydownHelper);

        when(mKeyListener.onKeyDown(any(View.class), any(Editable.class), anyInt(),
                any(KeyEvent.class))).thenReturn(false);
    }

    @Test
    public void whenEmojiCompat_handlesKeyDown_doesntCallKeyListener() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = KeyboardUtil.zero();
        when(mEmojiCompatKeydownHelper.handleKeyDown(any(), anyInt(), any())).thenReturn(true);
        assertTrue(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verifyNoMoreInteractions(mKeyListener);
    }

    @Test
    public void whenEmojiCompatDoesnt_handleKeyDown_callsListener() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = KeyboardUtil.zero();
        when(mEmojiCompatKeydownHelper.handleKeyDown(any(), anyInt(), any())).thenReturn(false);
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }
}
