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

import static androidx.emoji.util.Emoji.CHAR_DEFAULT_EMOJI_STYLE;
import static androidx.emoji.util.Emoji.CHAR_DEFAULT_TEXT_STYLE;
import static androidx.emoji.util.Emoji.CHAR_DIGIT;
import static androidx.emoji.util.Emoji.CHAR_FITZPATRICK;
import static androidx.emoji.util.Emoji.CHAR_VS_EMOJI;
import static androidx.emoji.util.Emoji.CHAR_VS_TEXT;
import static androidx.emoji.util.Emoji.DEFAULT_TEXT_STYLE;
import static androidx.emoji.util.Emoji.EMOJI_ASTERISK_KEYCAP;
import static androidx.emoji.util.Emoji.EMOJI_DIGIT_ES;
import static androidx.emoji.util.Emoji.EMOJI_DIGIT_ES_KEYCAP;
import static androidx.emoji.util.Emoji.EMOJI_DIGIT_KEYCAP;
import static androidx.emoji.util.Emoji.EMOJI_FLAG;
import static androidx.emoji.util.Emoji.EMOJI_GENDER;
import static androidx.emoji.util.Emoji.EMOJI_GENDER_WITHOUT_VS;
import static androidx.emoji.util.Emoji.EMOJI_REGIONAL_SYMBOL;
import static androidx.emoji.util.Emoji.EMOJI_SINGLE_CODEPOINT;
import static androidx.emoji.util.Emoji.EMOJI_SKIN_MODIFIER;
import static androidx.emoji.util.Emoji.EMOJI_SKIN_MODIFIER_TYPE_ONE;
import static androidx.emoji.util.Emoji.EMOJI_SKIN_MODIFIER_WITH_VS;
import static androidx.emoji.util.Emoji.EMOJI_UNKNOWN_FLAG;
import static androidx.emoji.util.Emoji.EMOJI_WITH_ZWJ;
import static androidx.emoji.util.EmojiMatcher.hasEmoji;
import static androidx.emoji.util.EmojiMatcher.hasEmojiAt;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;
import static androidx.emoji.util.KeyboardUtil.del;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.emoji.util.Emoji.EmojiMapping;
import androidx.emoji.util.TestString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiCompatTest {

    @Before
    public void setup() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Test(expected = IllegalStateException.class)
    public void testGet_throwsException() {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.get();
    }

    @Test
    public void testProcess_doesNothing_withNullCharSequence() {
        assertNull(EmojiCompat.get().process(null));
    }

    @Test
    public void testProcess_returnsEmptySpanned_withEmptyString() {
        final CharSequence charSequence = EmojiCompat.get().process("");
        assertNotNull(charSequence);
        assertEquals(0, charSequence.length());
        assertThat(charSequence, not(hasEmoji()));
    }

    @SuppressLint("Range")
    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withNegativeStartValue() {
        EmojiCompat.get().process("a", -1, 1);
    }

    @SuppressLint("Range")
    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withNegativeEndValue() {
        EmojiCompat.get().process("a", 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withStartSmallerThanEndValue() {
        EmojiCompat.get().process("aa", 1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withStartGreaterThanLength() {
        EmojiCompat.get().process("a", 2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withEndGreaterThanLength() {
        EmojiCompat.get().process("a", 0, 2);
    }

    @Test
    public void testProcessWithStartEnd_withNoOpValues() {
        final Spannable spannable = new SpannableString(new TestString('a')
                .withPrefix().withSuffix().toString());
        // early return check
        assertSame(spannable, EmojiCompat.get().process(spannable, 0, 0));
        assertSame(spannable, EmojiCompat.get().process(spannable, 1, 1));
        assertSame(spannable, EmojiCompat.get().process(spannable, spannable.length(),
                spannable.length()));
    }

    @Test
    public void testProcess_doesNotAddEmojiSpan() {
        final String string = "abc";
        final CharSequence charSequence = EmojiCompat.get().process(string);
        assertNotNull(charSequence);
        assertEquals(string, charSequence.toString());
        assertThat(charSequence, not(hasEmoji()));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testProcess_returnsSameCharSequence_pre19() {
        assertNull(EmojiCompat.get().process(null));

        CharSequence testString = "abc";
        assertSame(testString, EmojiCompat.get().process(testString));

        testString = new SpannableString("abc");
        assertSame(testString, EmojiCompat.get().process(testString));

        testString = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertSame(testString, EmojiCompat.get().process(testString));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSingleCodePointEmoji() {
        assertCodePointMatch(EMOJI_SINGLE_CODEPOINT);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsFlagEmoji() {
        assertCodePointMatch(EMOJI_FLAG);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsUnknownFlagEmoji() {
        assertCodePointMatch(EMOJI_UNKNOWN_FLAG);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsRegionalIndicatorSymbol() {
        assertCodePointMatch(EMOJI_REGIONAL_SYMBOL);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsKeyCapEmoji() {
        assertCodePointMatch(EMOJI_DIGIT_KEYCAP);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbers() {
        assertCodePointDoesNotMatch(new int[] {CHAR_DIGIT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbers_1() {
        final TestString string = new TestString(EMOJI_SINGLE_CODEPOINT).append('1', 'f');
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsVariantSelectorEmoji() {
        assertCodePointMatch(EMOJI_DIGIT_ES);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddVariantSelectorTextStyle() {
        assertCodePointDoesNotMatch(new int[]{CHAR_DIGIT, CHAR_VS_TEXT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsVariantSelectorAndKeyCapEmoji() {
        assertCodePointMatch(EMOJI_DIGIT_ES_KEYCAP);
    }

    @Test
    public void testProcess_doesNotAddEmoji_forVariantBaseWithoutSelector() {
        assertCodePointDoesNotMatch(new int[]{CHAR_DIGIT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsAsteriskKeyCapEmoji() {
        assertCodePointMatch(EMOJI_ASTERISK_KEYCAP);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji() {
        assertCodePointMatch(EMOJI_SKIN_MODIFIER);
        assertCodePointMatch(EMOJI_SKIN_MODIFIER_TYPE_ONE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji_withVariantSelector() {
        assertCodePointMatch(EMOJI_SKIN_MODIFIER_WITH_VS);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji_270c_withVariantSelector() {
        // 0x270c is a Standardized Variant Base, Emoji Modifier Base and also Emoji
        // therefore it is different than i.e. 0x1f3c3. The code actually failed for this test
        // at first.
        assertCodePointMatch(0xF0734, new int[]{0x270C, CHAR_VS_EMOJI, CHAR_FITZPATRICK});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_defaultStyleDoesNotAddSpan() {
        assertCodePointDoesNotMatch(new int[]{CHAR_DEFAULT_TEXT_STYLE});
        assertCodePointMatch(DEFAULT_TEXT_STYLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_defaultEmojiStyle_withTextStyleVs() {
        assertCodePointMatch(EMOJI_SINGLE_CODEPOINT.id(),
                new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_EMOJI});
        assertCodePointDoesNotMatch(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_TEXT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_genderEmoji() {
        assertCodePointMatch(EMOJI_GENDER);
        assertCodePointMatch(EMOJI_GENDER_WITHOUT_VS);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_standardizedVariantEmojiExceptions() {
        final int[][] exceptions = new int[][]{
                {0x2600, 0xF034D},
                {0x2601, 0xF0167},
                {0x260E, 0xF034E},
                {0x261D, 0xF0227},
                {0x263A, 0xF02A6},
                {0x2660, 0xF0350},
                {0x2663, 0xF033F},
                {0x2665, 0xF033B},
                {0x2666, 0xF033E},
                {0x270C, 0xF0079},
                {0x2744, 0xF0342},
                {0x2764, 0xF0362}
        };

        for (int i = 0; i < exceptions.length; i++) {
            final int[] codepoints = new int[]{exceptions[i][0]};
            assertCodePointMatch(exceptions[i][1], codepoints);
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsZwjEmoji() {
        assertCodePointMatch(EMOJI_WITH_ZWJ);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbersAfterZwjEmo() {
        TestString string = new TestString(EMOJI_WITH_ZWJ).append(0x20, 0x2B, 0x31)
                .withSuffix().withPrefix();
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(EMOJI_WITH_ZWJ, string.emojiStartIndex(),
                string.emojiEndIndex() - 3));
        assertThat(charSequence, hasEmojiCount(1));

        string = new TestString(EMOJI_WITH_ZWJ).withSuffix().withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsEmojiThatFollowsDigit() {
        TestString string = new TestString(EMOJI_SINGLE_CODEPOINT).prepend('N', '5');
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(EMOJI_SINGLE_CODEPOINT, string.emojiStartIndex() + 2,
                string.emojiEndIndex()));
        assertThat(charSequence, hasEmojiCount(1));

        string = new TestString(EMOJI_WITH_ZWJ).prepend('N', '5');
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(EMOJI_WITH_ZWJ, string.emojiStartIndex() + 2,
                string.emojiEndIndex()));
        assertThat(charSequence, hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withAppend() {
        final Editable editable = new SpannableStringBuilder(new TestString('a').withPrefix()
                .withSuffix().toString());
        final int start = 1;
        final int end = start + EMOJI_SINGLE_CODEPOINT.charCount();
        editable.insert(start, new TestString(EMOJI_SINGLE_CODEPOINT).toString());
        EmojiCompat.get().process(editable, start, end);
        assertThat(editable, hasEmojiCount(1));
        assertThat(editable, hasEmojiAt(EMOJI_SINGLE_CODEPOINT, start, end));
    }

    @Test
    public void testProcess_doesNotCreateSpannable_ifNoEmoji() {
        CharSequence processed = EmojiCompat.get().process("abc");
        assertNotNull(processed);
        assertThat(processed, instanceOf(String.class));

        processed = EmojiCompat.get().process(new SpannedString("abc"));
        assertNotNull(processed);
        assertThat(processed, instanceOf(SpannedString.class));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_reprocess() {
        final String string = new TestString(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .withPrefix().withSuffix().toString();

        Spannable processed = (Spannable) EmojiCompat.get().process(string);
        assertThat(processed, hasEmojiCount(3));

        final EmojiSpan[] spans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        final Set<EmojiSpan> spanSet = new HashSet<>();
        Collections.addAll(spanSet, spans);

        processed = (Spannable) EmojiCompat.get().process(processed);
        assertThat(processed, hasEmojiCount(3));
        // new spans should be new instances
        final EmojiSpan[] newSpans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        for (int i = 0; i < newSpans.length; i++) {
            assertFalse(spanSet.contains(newSpans[i]));
        }
    }

    @SuppressLint("Range")
    @Test(expected = IllegalArgumentException.class)
    public void testProcess_throwsException_withMaxEmojiSetToNegative() {
        final String original = new TestString(EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                -1 /*maxEmojiCount*/);

        assertThat(processed, not(hasEmoji()));
    }

    @Test
    public void testProcess_withMaxEmojiSetToZero() {
        final String original = new TestString(EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                0 /*maxEmojiCount*/);

        assertThat(processed, not(hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSetToOne() {
        final String original = new TestString(EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                1 /*maxEmojiCount*/);

        assertThat(processed, hasEmojiCount(1));
        assertThat(processed, hasEmoji(EMOJI_SINGLE_CODEPOINT));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSetToLessThenExistingSpanCount() {
        final String original = new TestString(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .toString();

        // add 2 spans
        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(), 2);

        assertThat(processed, hasEmojiCount(2));

        // use the Spannable with 2 spans, but use maxEmojiCount=1, start from the beginning of
        // last (3rd) emoji
        EmojiCompat.get().process(processed, original.length() - EMOJI_SINGLE_CODEPOINT.charCount(),
                original.length(), 1 /*maxEmojiCount*/);

        // expectation: there are still 2 emojis
        assertThat(processed, hasEmojiCount(2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSet_withExistingEmojis() {
        // test string with two emoji characters
        final String original = new TestString(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_FLAG).toString();

        // process and add 1 EmojiSpan, maxEmojiCount=1
        CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                1 /*maxEmojiCount*/);

        // assert that there is a single emoji
        assertThat(processed, hasEmojiCount(1));
        assertThat(processed,
                hasEmojiAt(EMOJI_SINGLE_CODEPOINT, 0, EMOJI_SINGLE_CODEPOINT.charCount()));

        // call process again with the charSequence that already has 1 span
        processed = EmojiCompat.get().process(processed, EMOJI_SINGLE_CODEPOINT.charCount(),
                processed.length(), 1 /*maxEmojiCount*/);

        // assert that there is still a single emoji
        assertThat(processed, hasEmojiCount(1));
        assertThat(processed,
                hasEmojiAt(EMOJI_SINGLE_CODEPOINT, 0, EMOJI_SINGLE_CODEPOINT.charCount()));

        // make the same call, this time with maxEmojiCount=2
        processed = EmojiCompat.get().process(processed, EMOJI_SINGLE_CODEPOINT.charCount(),
                processed.length(), 2 /*maxEmojiCount*/);

        // assert that it contains 2 emojis
        assertThat(processed, hasEmojiCount(2));
        assertThat(processed,
                hasEmojiAt(EMOJI_SINGLE_CODEPOINT, 0, EMOJI_SINGLE_CODEPOINT.charCount()));
        assertThat(processed,
                hasEmojiAt(EMOJI_FLAG, EMOJI_SINGLE_CODEPOINT.charCount(),
                        original.length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withReplaceNonExistent_callsGlyphChecker() {
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true);
        EmojiCompat.reset(config);

        final EmojiProcessor.GlyphChecker glyphChecker = mock(EmojiProcessor.GlyphChecker.class);
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt())).thenReturn(true);
        EmojiCompat.get().setGlyphChecker(glyphChecker);

        final String original = new TestString(EMOJI_SINGLE_CODEPOINT).toString();

        CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                Integer.MAX_VALUE /*maxEmojiCount*/, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        // when function overrides config level replaceAll, a call to GlyphChecker is expected.
        verify(glyphChecker, times(1)).hasGlyph(any(CharSequence.class), anyInt(), anyInt());

        // since replaceAll is false, there should be no EmojiSpans
        assertThat(processed, not(hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withReplaceDefault_doesNotCallGlyphChecker() {
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true);
        EmojiCompat.reset(config);

        final EmojiProcessor.GlyphChecker glyphChecker = mock(EmojiProcessor.GlyphChecker.class);
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt())).thenReturn(true);
        EmojiCompat.get().setGlyphChecker(glyphChecker);

        final String original = new TestString(EMOJI_SINGLE_CODEPOINT).toString();
        // call without replaceAll, config value (true) should be used
        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                Integer.MAX_VALUE /*maxEmojiCount*/, EmojiCompat.REPLACE_STRATEGY_DEFAULT);

        // replaceAll=true should not call hasGlyph
        verify(glyphChecker, times(0)).hasGlyph(any(CharSequence.class), anyInt(), anyInt());

        assertThat(processed, hasEmojiCount(1));
        assertThat(processed, hasEmoji(EMOJI_SINGLE_CODEPOINT));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withSpanned_replaceNonExistent() {
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(false);
        EmojiCompat.reset(config);

        final String string = new TestString(EMOJI_SINGLE_CODEPOINT).append(
                EMOJI_SINGLE_CODEPOINT).toString();
        CharSequence processed = EmojiCompat.get().process(string, 0, string.length(),
                Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_ALL);

        final SpannedString spanned = new SpannedString(processed);
        assertThat(spanned, hasEmojiCount(2));

        // mock GlyphChecker so that we can return true for hasGlyph
        final EmojiProcessor.GlyphChecker glyphChecker = mock(EmojiProcessor.GlyphChecker.class);
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt())).thenReturn(true);
        EmojiCompat.get().setGlyphChecker(glyphChecker);

        processed = EmojiCompat.get().process(spanned, 0, spanned.length(),
                Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        assertThat(processed, not(hasEmoji()));

        // start: 1 char after the first emoji (in the second emoji)
        processed = EmojiCompat.get().process(spanned, EMOJI_SINGLE_CODEPOINT.charCount() + 1,
                spanned.length(), Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        assertThat(processed, hasEmojiCount(1));
        assertThat(processed, hasEmoji(EMOJI_SINGLE_CODEPOINT));
    }

    @Test(expected = NullPointerException.class)
    public void testHasEmojiGlyph_withNullCharSequence() {
        EmojiCompat.get().hasEmojiGlyph(null);
    }

    @Test(expected = NullPointerException.class)
    public void testHasEmojiGlyph_withMetadataVersion_withNullCharSequence() {
        EmojiCompat.get().hasEmojiGlyph(null, Integer.MAX_VALUE);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testHasEmojiGlyph_pre19() {
        String sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testHasEmojiGlyph_withMetaVersion_pre19() {
        String sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence, Integer.MAX_VALUE));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHasEmojiGlyph_returnsTrueForExistingEmoji() {
        final String sequence = new TestString(EMOJI_FLAG).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    public void testHasGlyph_returnsFalseForNonExistentEmoji() {
        final String sequence = new TestString(EMOJI_FLAG).append(0x1111).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHashEmojiGlyph_withDefaultEmojiStyles() {
        String sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_EMOJI}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_TEXT}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHashEmojiGlyph_withMetadataVersion() {
        final String sequence = new TestString(EMOJI_SINGLE_CODEPOINT).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence, 0));
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence, Integer.MAX_VALUE));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testGetLoadState_returnsSuccess_pre19() {
        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_SUCCEEDED);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testGetLoadState_returnsSuccessIfLoadSuccess() throws InterruptedException {
        final TestConfigBuilder.WaitingDataLoader
                metadataLoader = new TestConfigBuilder.WaitingDataLoader(true /*success*/);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_LOADING);

        metadataLoader.getLoaderLatch().countDown();
        metadataLoader.getTestLatch().await();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_SUCCEEDED);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testGetLoadState_returnsFailIfLoadFail() throws InterruptedException {
        final TestConfigBuilder.WaitingDataLoader
                metadataLoader = new TestConfigBuilder.WaitingDataLoader(false/*fail*/);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_LOADING);

        metadataLoader.getLoaderLatch().countDown();
        metadataLoader.getTestLatch().await();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_FAILED);
    }

    @Test
    public void testUpdateEditorInfoAttrs_doesNotSetKeyIfNotInitialized() {
        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.extras = new Bundle();

        final TestConfigBuilder.WaitingDataLoader
                metadataLoader = new TestConfigBuilder.WaitingDataLoader();
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        final Bundle extras = editorInfo.extras;
        assertFalse(extras.containsKey(EmojiCompat.EDITOR_INFO_METAVERSION_KEY));
        assertFalse(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));

        metadataLoader.getLoaderLatch().countDown();
    }

    @Test(expected = IllegalStateException.class)
    public void testLoad_throwsException_whenLoadStrategyDefault() {
        final EmojiCompat.MetadataRepoLoader loader = mock(EmojiCompat.MetadataRepoLoader.class);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader);
        EmojiCompat.reset(config);

        EmojiCompat.get().load();
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testLoad_pre19() {
        final EmojiCompat.MetadataRepoLoader loader = spy(new TestConfigBuilder
                .TestEmojiDataLoader());
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);

        EmojiCompat.reset(config);

        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_DEFAULT, EmojiCompat.get().getLoadState());

        EmojiCompat.get().load();
        assertEquals(EmojiCompat.LOAD_STATE_SUCCEEDED, EmojiCompat.get().getLoadState());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_startsLoading() {
        final EmojiCompat.MetadataRepoLoader loader = spy(new TestConfigBuilder
                .TestEmojiDataLoader());
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);

        EmojiCompat.reset(config);

        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_DEFAULT, EmojiCompat.get().getLoadState());

        EmojiCompat.get().load();
        verify(loader, times(1)).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_SUCCEEDED, EmojiCompat.get().getLoadState());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_onceSuccessDoesNotStartLoading() {
        final EmojiCompat.MetadataRepoLoader loader = spy(new TestConfigBuilder
                .TestEmojiDataLoader());
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);

        EmojiCompat.reset(config);

        EmojiCompat.get().load();
        verify(loader, times(1)).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_SUCCEEDED, EmojiCompat.get().getLoadState());

        reset(loader);
        EmojiCompat.get().load();
        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.LOAD_STATE_SUCCEEDED, EmojiCompat.get().getLoadState());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testLoad_onceLoadingDoesNotStartLoading() throws InterruptedException {
        final TestConfigBuilder.WaitingDataLoader loader = spy(
                new TestConfigBuilder.WaitingDataLoader(true /*success*/));
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig(loader)
                .setMetadataLoadStrategy(EmojiCompat.LOAD_STRATEGY_MANUAL);

        EmojiCompat.reset(config);

        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));

        EmojiCompat.get().load();
        verify(loader, times(1)).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));
        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_LOADING);

        reset(loader);

        EmojiCompat.get().load();
        verify(loader, never()).load(any(EmojiCompat.MetadataRepoLoaderCallback.class));

        loader.getLoaderLatch().countDown();
        loader.getTestLatch().await();

        assertEquals(EmojiCompat.get().getLoadState(), EmojiCompat.LOAD_STATE_SUCCEEDED);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testGetAssetSignature() {
        final String signature = EmojiCompat.get().getAssetSignature();
        assertTrue(signature.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testGetAssetSignature_api19() {
        final String signature = EmojiCompat.get().getAssetSignature();
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUpdateEditorInfoAttrs_setsKeysIfInitialized() {
        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.extras = new Bundle();
        EmojiCompat.Config config = new TestConfigBuilder.TestConfig().setReplaceAll(false);
        EmojiCompat.reset(config);
        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        final Bundle extras = editorInfo.extras;
        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_METAVERSION_KEY));
        assertTrue(extras.getInt(EmojiCompat.EDITOR_INFO_METAVERSION_KEY) > 0);
        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
        assertFalse(extras.getBoolean(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));

        config = new TestConfigBuilder.TestConfig().setReplaceAll(true);
        EmojiCompat.reset(config);
        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
        assertTrue(extras.getBoolean(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testHandleDeleteSurroundingText_pre19() {
        final TestString testString = new TestString(EMOJI_SINGLE_CODEPOINT);
        final InputConnection inputConnection = mock(InputConnection.class);
        final Editable editable = spy(new SpannableStringBuilder(testString.toString()));

        Selection.setSelection(editable, testString.emojiEndIndex());

        reset(editable);
        reset(inputConnection);
        verifyNoMoreInteractions(editable);
        verifyNoMoreInteractions(inputConnection);

        // try backwards delete 1 character
        assertFalse(EmojiCompat.handleDeleteSurroundingText(inputConnection, editable,
                1 /*beforeLength*/, 0 /*afterLength*/, false /*inCodePoints*/));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testOnKeyDown_pre19() {
        final TestString testString = new TestString(EMOJI_SINGLE_CODEPOINT);
        final Editable editable = spy(new SpannableStringBuilder(testString.toString()));
        Selection.setSelection(editable, testString.emojiEndIndex());
        final KeyEvent event = del();

        reset(editable);
        verifyNoMoreInteractions(editable);

        assertFalse(EmojiCompat.handleOnKeyDown(editable, event.getKeyCode(), event));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_whenEmojiInTheMiddle() {
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig().setReplaceAll(true);
        EmojiCompat.reset(config);
        String s = new TestString(0x0061, CHAR_DEFAULT_TEXT_STYLE, 0x0062).toString();
        // no span should be added as the emoji is text style presented by default
        assertThat(EmojiCompat.get().process(s), not(hasEmoji()));
        // a span should be added when we use the emoji style presentation as default
        EmojiCompat.reset(config.setUseEmojiAsDefaultStyle(true));
        assertThat(EmojiCompat.get().process(s), hasEmojiAt(DEFAULT_TEXT_STYLE, 1, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_whenEmojiAtTheEnd() {
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig().setReplaceAll(true);
        EmojiCompat.reset(config);
        String s = new TestString(0x0061, CHAR_DEFAULT_TEXT_STYLE).toString();
        // no span should be added as the emoji is text style presented by default
        assertThat(EmojiCompat.get().process(s), not(hasEmoji()));
        // a span should be added when we use the emoji style presentation as default
        EmojiCompat.reset(config.setUseEmojiAsDefaultStyle(true));
        assertThat(EmojiCompat.get().process(s), hasEmojiAt(DEFAULT_TEXT_STYLE, 1, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_noEmojisAdded_whenMarkedAsException() {
        final String s = new TestString(CHAR_DEFAULT_TEXT_STYLE).toString();
        final List<Integer> exceptions =
                Arrays.asList(CHAR_DEFAULT_TEXT_STYLE + 1, CHAR_DEFAULT_TEXT_STYLE);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig().setReplaceAll(true)
                .setUseEmojiAsDefaultStyle(true, exceptions);
        EmojiCompat.reset(config);
        // no span should be added as the text style codepoint is marked as exception
        assertThat(EmojiCompat.get().process(s), not(hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_emojisAdded_whenNotMarkedAsException() {
        final String s = new TestString(CHAR_DEFAULT_TEXT_STYLE).toString();
        final List<Integer> exceptions =
                Arrays.asList(CHAR_DEFAULT_TEXT_STYLE - 1, CHAR_DEFAULT_TEXT_STYLE + 1);
        final EmojiCompat.Config config = new TestConfigBuilder.TestConfig().setReplaceAll(true)
                .setUseEmojiAsDefaultStyle(true, exceptions);
        EmojiCompat.reset(config);
        // a span should be added as the codepoint is not included in the set of exceptions
        assertThat(EmojiCompat.get().process(s), hasEmojiAt(DEFAULT_TEXT_STYLE, 0, 1));
    }

    private void assertCodePointMatch(EmojiMapping emoji) {
        assertCodePointMatch(emoji.id(), emoji.codepoints());
    }

    private void assertCodePointMatch(int id, int[] codepoints) {
        TestString string = new TestString(codepoints);
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is in the middle of string
        string = new TestString(codepoints).withPrefix().withSuffix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is at the end of string
        string = new TestString(codepoints).withSuffix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));
    }

    private void assertCodePointDoesNotMatch(int[] codepoints) {
        TestString string = new TestString(codepoints);
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, not(hasEmoji()));

        string = new TestString(codepoints).withSuffix().withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, not(hasEmoji()));

        string = new TestString(codepoints).withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, not(hasEmoji()));
    }
}
