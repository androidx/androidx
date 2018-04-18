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

import static androidx.emoji.util.Emoji.EMOJI_WITH_ZWJ;
import static androidx.emoji.util.EmojiMatcher.hasEmoji;
import static androidx.emoji.util.KeyboardUtil.altDel;
import static androidx.emoji.util.KeyboardUtil.ctrlDel;
import static androidx.emoji.util.KeyboardUtil.del;
import static androidx.emoji.util.KeyboardUtil.fnDel;
import static androidx.emoji.util.KeyboardUtil.forwardDel;
import static androidx.emoji.util.KeyboardUtil.shiftDel;
import static androidx.emoji.util.KeyboardUtil.zero;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;

import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.TestConfigBuilder;
import androidx.emoji.util.TestString;

import org.junit.Before;
import org.junit.BeforeClass;
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

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mKeyListener = mock(KeyListener.class);
        mTestString = new TestString(EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        mEmojiKeyListener = new EmojiKeyListener(mKeyListener);
        EmojiCompat.get().process(mEditable);
        assertThat(mEditable, hasEmoji());

        when(mKeyListener.onKeyDown(any(View.class), any(Editable.class), anyInt(),
                any(KeyEvent.class))).thenReturn(false);
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenKeyCodeIsNotDelOrForwardDel() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = zero();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withOtherModifiers() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = fnDel();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withAltModifier() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = altDel();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withCtrlModifier() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = ctrlDel();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withShiftModifier() {
        Selection.setSelection(mEditable, 0);
        final KeyEvent event = shiftDel();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withSelection() {
        Selection.setSelection(mEditable, 0, mEditable.length());
        final KeyEvent event = del();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withoutEmojiSpans() {
        Editable editable = new SpannableStringBuilder("abc");
        Selection.setSelection(editable, 1);
        final KeyEvent event = del();
        assertFalse(mEmojiKeyListener.onKeyDown(null, editable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(editable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenNoSpansBefore() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = del();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_deletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = del();
        assertTrue(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verifyNoMoreInteractions(mKeyListener);
    }

    @Test
    public void testOnKeyDown_doesNotForwardDeleteEmoji_withNoSpansAfter() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = forwardDel();
        assertFalse(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verify(mKeyListener, times(1)).onKeyDown((View) eq(null), same(mEditable),
                eq(event.getKeyCode()), same(event));
    }

    @Test
    public void testOnKeyDown_forwardDeletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = forwardDel();
        assertTrue(mEmojiKeyListener.onKeyDown(null, mEditable, event.getKeyCode(), event));
        verifyNoMoreInteractions(mKeyListener);
    }
}
