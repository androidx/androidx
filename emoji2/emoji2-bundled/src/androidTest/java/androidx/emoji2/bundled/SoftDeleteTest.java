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
import static org.mockito.Mockito.mock;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.view.inputmethod.InputConnection;

import androidx.emoji2.bundled.util.Emoji;
import androidx.emoji2.bundled.util.EmojiMatcher;
import androidx.emoji2.bundled.util.TestString;
import androidx.emoji2.text.EmojiCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SoftDeleteTest {
    private InputConnection mInputConnection;
    private TestString mTestString;
    private Editable mEditable;

    @BeforeClass
    public static void setupEmojiCompat() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Before
    public void setup() {
        mInputConnection = mock(InputConnection.class);
        mTestString = new TestString(Emoji.EMOJI_WITH_ZWJ).withPrefix().withSuffix();
        mEditable = new SpannableStringBuilder(mTestString.toString());
        EmojiCompat.get().process(mEditable);
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmojiCount(1));
        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
    }

    @Test
    public void testDelete_doesNotDelete_whenSelectionIsUndefined() {
        // no selection is set on editable
        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_doesNotDelete_whenThereIsSelectionLongerThanZero() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex(),
                mTestString.emojiEndIndex() + 1);

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_withNullEditable() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, null,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_withNullInputConnection() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        assertFalse(EmojiCompat.handleDeleteSurroundingText(null, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @SuppressLint("Range")
    @Test
    public void testDelete_withInvalidLength() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                -1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @SuppressLint("Range")
    @Test
    public void testDelete_withInvalidAfterLength() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                0 /*beforeLength*/, -1 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji(Emoji.EMOJI_WITH_ZWJ));
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_backward() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        // backwards delete 1 character, it will delete the emoji
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testDelete_backward_inCodepoints() {
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        // backwards delete 1 character, it will delete the emoji
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, true /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testDelete_forward() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());

        // forward delete 1 character, it will dele the emoji.
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                0 /*beforeLength*/, 1 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testDelete_forward_inCodepoints() {
        Selection.setSelection(mEditable, mTestString.emojiStartIndex());

        // forward delete 1 codepoint, it will delete the emoji.
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                0 /*beforeLength*/, 1 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals(new TestString().withPrefix().withSuffix().toString(), mEditable.toString());
    }

    @Test
    public void testDelete_backward_doesNotDeleteWhenSelectionAtCharSequenceStart() {
        // make sure selection at 0 does not do something weird for backward delete
        Selection.setSelection(mEditable, 0);

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_forward_doesNotDeleteWhenSelectionAtCharSequenceEnd() {
        // make sure selection at end does not do something weird for forward delete
        Selection.setSelection(mEditable, mTestString.emojiEndIndex());

        assertFalse(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                0 /*beforeLength*/, 1 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, EmojiMatcher.hasEmoji());
        assertEquals(mTestString.toString(), mEditable.toString());
    }

    @Test
    public void testDelete_withMultipleCharacters() {
        // prepare string as abc[emoji]def
        mTestString = new TestString(Emoji.EMOJI_FLAG);
        mEditable = new SpannableStringBuilder("abc" + mTestString.toString() + "def");
        EmojiCompat.get().process(mEditable);

        // set the selection in the middle of emoji
        Selection.setSelection(mEditable, "abc".length() + Emoji.EMOJI_FLAG.charCount() / 2);

        // delete 4 characters forward, 4 character backwards
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                4 /*beforeLength*/, 4 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals("af", mEditable.toString());
    }

    @Test
    public void testDelete_withMultipleCodepoints() {
        // prepare string as abc[emoji]def
        mTestString = new TestString(Emoji.EMOJI_FLAG);
        mEditable = new SpannableStringBuilder("abc" + mTestString.toString() + "def");
        EmojiCompat.get().process(mEditable);

        // set the selection in the middle of emoji
        Selection.setSelection(mEditable, "abc".length() + Emoji.EMOJI_FLAG.charCount() / 2);

        // delete 3 codepoints forward, 3 codepoints backwards
        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                3 /*beforeLength*/, 3 /*afterLength*/, true /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals("af", mEditable.toString());
    }

    @Test
    public void testDelete_withMultipleCharacters_withDeleteLengthLongerThanString() {
        // prepare string as abc[emoji]def
        mTestString = new TestString(Emoji.EMOJI_FLAG);
        mEditable = new SpannableStringBuilder("abc" + mTestString.toString() + "def");
        EmojiCompat.get().process(mEditable);

        // set the selection in the middle of emoji
        Selection.setSelection(mEditable, "abc".length() + Emoji.EMOJI_FLAG.charCount() / 2);

        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                100 /*beforeLength*/, 100 /*afterLength*/, false /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals("", mEditable.toString());
    }

    @Test
    public void testDelete_withMultipleCodepoints_withDeleteLengthLongerThanString() {
        // prepare string as abc[emoji]def
        mTestString = new TestString(Emoji.EMOJI_FLAG);
        mEditable = new SpannableStringBuilder("abc" + mTestString.toString() + "def");
        EmojiCompat.get().process(mEditable);

        // set the selection in the middle of emoji
        Selection.setSelection(mEditable, "abc".length() + Emoji.EMOJI_FLAG.charCount() / 2);

        assertTrue(EmojiCompat.handleDeleteSurroundingText(mInputConnection, mEditable,
                100 /*beforeLength*/, 100 /*afterLength*/, true /*inCodePoints*/));

        MatcherAssert.assertThat(mEditable, IsNot.not(EmojiMatcher.hasEmoji()));
        assertEquals("", mEditable.toString());
    }
}
