/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.emoji.text;

import static androidx.emoji.util.Emoji.EMOJI_FLAG;
import static androidx.emoji.util.Emoji.EMOJI_GENDER;
import static androidx.emoji.util.Emoji.EMOJI_WITH_ZWJ;
import static androidx.emoji.util.EmojiMatcher.hasEmoji;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;
import static androidx.emoji.util.KeyboardUtil.altDel;
import static androidx.emoji.util.KeyboardUtil.ctrlDel;
import static androidx.emoji.util.KeyboardUtil.del;
import static androidx.emoji.util.KeyboardUtil.fnDel;
import static androidx.emoji.util.KeyboardUtil.forwardDel;
import static androidx.emoji.util.KeyboardUtil.shiftDel;
import static androidx.emoji.util.KeyboardUtil.zero;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.KeyEvent;

import androidx.emoji.util.TestString;

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
        mTestString = new TestString(EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        EmojiCompat.get().process(mEditable);
        assertThat(mEditable, hasEmojiCount(1));
        assertThat(mEditable, hasEmoji(EMOJI_WITH_ZWJ));
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenKeyCodeIsNotDelOrForwardDel() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = zero();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withOtherModifiers() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = fnDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withAltModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = altDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withCtrlModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = ctrlDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withShiftModifier() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = shiftDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withSelectionLongerThanZeroLength() {
        // when there is a selection which is longer than 0, it should not delete.
        Selection.setSelection(mEditable, 0, mEditable.length());
        final KeyEvent event = del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_withoutEmojiSpans() {
        final Editable editable = new SpannableStringBuilder("abc");
        Selection.setSelection(editable, 1);
        final KeyEvent event = del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotDelete_whenNoSpansBefore() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = del();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, not(hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_doesNotForwardDeleteEmoji_withNoSpansAfter() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());
        final KeyEvent event = forwardDel();
        assertFalse(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_forwardDeletesEmoji() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());
        final KeyEvent event = forwardDel();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, event.getKeyCode(), event));
        assertThat(mEditable, not(hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji_ifSelectionIsInSpanBoundaries() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex() + 1);
        final KeyEvent delEvent = del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, delEvent.getKeyCode(), delEvent));
        assertThat(mEditable, not(hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesEmoji_ifSelectionIsInSpanBoundaries_withForwardDel() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex() + 1);
        final KeyEvent forwardDelEvent = forwardDel();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, forwardDelEvent.getKeyCode(),
                forwardDelEvent));
        assertThat(mEditable, not(hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testOnKeyDown_deletesOnlyEmojiBeforeTheCursor() {
        // contains three emojis
        mTestString = new TestString(EMOJI_FLAG)
                .append(EMOJI_WITH_ZWJ)
                .append(EMOJI_GENDER)
                .withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        EmojiCompat.get().process(mEditable);

        // put the cursor after the second emoji
        Selection.setSelection(mEditable, mTestString.emojiStartIndex()
                + EMOJI_FLAG.charCount()
                + EMOJI_WITH_ZWJ.charCount());

        // delete
        final KeyEvent forwardDelEvent = del();
        assertTrue(EmojiCompat.handleOnKeyDown(mEditable, forwardDelEvent.getKeyCode(),
                forwardDelEvent));

        assertThat(mEditable, hasEmojiCount(2));
        assertThat(mEditable, hasEmoji(EMOJI_FLAG));
        assertThat(mEditable, hasEmoji(EMOJI_GENDER));

        assertEquals(new TestString(EMOJI_FLAG).append(EMOJI_GENDER)
                .withPrefix().withSuffix().toString(), mEditable.toString());
    }

}
