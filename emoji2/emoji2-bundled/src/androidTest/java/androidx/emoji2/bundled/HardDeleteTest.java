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
package androidx.emoji2.bundled;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;

import androidx.emoji2.bundled.util.Emoji;
import androidx.emoji2.bundled.util.EmojiMatcher;
import androidx.emoji2.bundled.util.KeyboardUtil;
import androidx.emoji2.bundled.util.TestString;
import androidx.emoji2.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class HardDeleteTest {

    private TestString mTestString;
    private Editable mEditable;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mTestString = new TestString(Emoji.EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        EmojiCompat.get().process(mEditable);
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmojiCount(1));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenKeyCodeIsNotDelOrForwardDel() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.zero();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withOtherModifiers() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.fnDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withAltModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.altDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withCtrlModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.ctrlDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withShiftModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.shiftDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withSelectionLongerThanZeroLength() {
        // when there is a selection which is longer than 0, it should not delete.
        Selection.setSelection(mEditable, 0, mEditable.length());
        final KeyEvent event = KeyboardUtil.del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withoutEmojiSpans() {
        final Editable editable = new SpannableStringBuilder("abc");
        Selection.setSelection(editable, 1);
        final KeyEvent event = KeyboardUtil.del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenNoSpansBefore() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = KeyboardUtil.del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotForwardDeleteEmoji_withNoSpansAfter() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.forwardDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_forwardDeletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = KeyboardUtil.forwardDel();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji_ifSelectionIsInSpanBoundaries() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex() + 1);
        final KeyEvent delEvent = KeyboardUtil.del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, delEvent.getKeyCode(), delEvent));
        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji_ifSelectionIsInSpanBoundaries_withForwardDel() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex() + 1);
        final KeyEvent forwardDelEvent = KeyboardUtil.forwardDel();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, forwardDelEvent.getKeyCode(),
                forwardDelEvent));
        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesOnlyEmojiBeforeTheCursor() {
        // contains three emojis
        mTestString = new TestString(Emoji.EMOJI_FLAG)
                .append(Emoji.EMOJI_WITH_ZWJ)
                .append(Emoji.EMOJI_GENDER)
                .withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        EmojiCompat.get().process(mEditable);

        // put the cursor after the second emoji
        Selection.setSelection(mEditable, mTestString.emojiStartIndex()
                + Emoji.EMOJI_FLAG.charCount()
                + Emoji.EMOJI_WITH_ZWJ.charCount());

        // delete
        final KeyEvent forwardDelEvent = KeyboardUtil.del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, forwardDelEvent.getKeyCode(),
                forwardDelEvent));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmojiCount(2));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_FLAG));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_GENDER));

        assertEquals(new TestString(Emoji.EMOJI_FLAG).append(Emoji.EMOJI_GENDER)
                .withPrefix().withSuffix().toString(), mEditable.toString());
    }

}
