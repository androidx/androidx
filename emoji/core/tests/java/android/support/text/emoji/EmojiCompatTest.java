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
package android.support.text.emoji;

import static android.support.text.emoji.TestConfigBuilder.TestConfig;
import static android.support.text.emoji.TestConfigBuilder.WaitingDataLoader;
import static android.support.text.emoji.TestConfigBuilder.config;
import static android.support.text.emoji.util.Emoji.CHAR_DEFAULT_EMOJI_STYLE;
import static android.support.text.emoji.util.Emoji.CHAR_DEFAULT_TEXT_STYLE;
import static android.support.text.emoji.util.Emoji.CHAR_DIGIT;
import static android.support.text.emoji.util.Emoji.CHAR_FITZPATRICK;
import static android.support.text.emoji.util.Emoji.CHAR_VS_EMOJI;
import static android.support.text.emoji.util.Emoji.CHAR_VS_TEXT;
import static android.support.text.emoji.util.Emoji.DEFAULT_TEXT_STYLE;
import static android.support.text.emoji.util.Emoji.EMOJI_ASTERISK_KEYCAP;
import static android.support.text.emoji.util.Emoji.EMOJI_DIGIT_ES;
import static android.support.text.emoji.util.Emoji.EMOJI_DIGIT_ES_KEYCAP;
import static android.support.text.emoji.util.Emoji.EMOJI_DIGIT_KEYCAP;
import static android.support.text.emoji.util.Emoji.EMOJI_FLAG;
import static android.support.text.emoji.util.Emoji.EMOJI_GENDER;
import static android.support.text.emoji.util.Emoji.EMOJI_GENDER_WITHOUT_VS;
import static android.support.text.emoji.util.Emoji.EMOJI_REGIONAL_SYMBOL;
import static android.support.text.emoji.util.Emoji.EMOJI_SINGLE_CODEPOINT;
import static android.support.text.emoji.util.Emoji.EMOJI_SKIN_MODIFIER;
import static android.support.text.emoji.util.Emoji.EMOJI_SKIN_MODIFIER_TYPE_ONE;
import static android.support.text.emoji.util.Emoji.EMOJI_SKIN_MODIFIER_WITH_VS;
import static android.support.text.emoji.util.Emoji.EMOJI_UNKNOWN_FLAG;
import static android.support.text.emoji.util.Emoji.EMOJI_WITH_ZWJ;
import static android.support.text.emoji.util.EmojiMatcher.hasEmoji;
import static android.support.text.emoji.util.EmojiMatcher.hasEmojiAt;
import static android.support.text.emoji.util.EmojiMatcher.hasEmojiCount;

import static junit.framework.TestCase.assertFalse;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.text.emoji.EmojiCompat.Config;
import android.support.text.emoji.util.Emoji.EmojiMapping;
import android.support.text.emoji.util.TestString;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.view.inputmethod.EditorInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EmojiCompatTest {

    @Before
    public void setup() {
        EmojiCompat.reset(config());
    }

    @Test(expected = IllegalStateException.class)
    public void testGet_throwsException() throws Exception {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.get();
    }

    @Test
    public void testProcess_doesNothing_withNullCharSequence() throws Exception {
        assertNull(EmojiCompat.get().process(null));
    }

    @Test
    public void testProcess_returnsEmptySpanned_withEmptyString() throws Exception {
        final CharSequence charSequence = EmojiCompat.get().process("");
        assertNotNull(charSequence);
        assertEquals(0, charSequence.length());
        assertThat(charSequence, not(hasEmoji()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withNegativeStartValue() throws Exception {
        EmojiCompat.get().process("a", -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withNegativeEndValue() throws Exception {
        EmojiCompat.get().process("a", 1, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withStartSmallerThanEndValue() throws Exception {
        EmojiCompat.get().process("aa", 1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withStartGreaterThanLength() throws Exception {
        EmojiCompat.get().process("a", 2, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcess_withEndGreaterThanLength() throws Exception {
        EmojiCompat.get().process("a", 0, 2);
    }

    @Test
    public void testProcessWithStartEnd_withNoOpValues() throws Exception {
        final Spannable spannable = new SpannableString(new TestString('a')
                .withPrefix().withSuffix().toString());
        // early return check
        assertSame(spannable, EmojiCompat.get().process(spannable, 0, 0));
        assertSame(spannable, EmojiCompat.get().process(spannable, 1, 1));
        assertSame(spannable, EmojiCompat.get().process(spannable, spannable.length(),
                spannable.length()));
    }


    @Test
    public void testProcess_doesNotAddEmojiSpan() throws Exception {
        final String string = "abc";
        final CharSequence charSequence = EmojiCompat.get().process(string);
        assertNotNull(charSequence);
        assertEquals(string, charSequence.toString());
        assertThat(charSequence, not(hasEmoji()));
    }

    @Test
    public void testProcess_addsSingleCodePointEmoji() throws Exception {
        assertCodePointMatch(EMOJI_SINGLE_CODEPOINT);
    }

    @Test
    public void testProcess_addsFlagEmoji() throws Exception {
        assertCodePointMatch(EMOJI_FLAG);
    }

    @Test
    public void testProcess_addsUnknownFlagEmoji() throws Exception {
        assertCodePointMatch(EMOJI_UNKNOWN_FLAG);
    }

    @Test
    public void testProcess_addsRegionalIndicatorSymbol() throws Exception {
        assertCodePointMatch(EMOJI_REGIONAL_SYMBOL);
    }

    @Test
    public void testProcess_addsKeyCapEmoji() throws Exception {
        assertCodePointMatch(EMOJI_DIGIT_KEYCAP);
    }

    @Test
    public void testProcess_doesNotAddEmojiForNumbers() throws Exception {
        assertCodePointDoesNotMatch(new int[] {CHAR_DIGIT});
    }

    @Test
    public void testProcess_doesNotAddEmojiForNumbers_1() throws Exception {
        final TestString string = new TestString(EMOJI_SINGLE_CODEPOINT).append('1', 'f');
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, hasEmojiCount(1));
    }

    @Test
    public void testProcess_addsVariantSelectorEmoji() throws Exception {
        assertCodePointMatch(EMOJI_DIGIT_ES);
    }

    @Test
    public void testProcess_doesNotAddVariantSelectorTextStyle() throws Exception {
        assertCodePointDoesNotMatch(new int[]{CHAR_DIGIT, CHAR_VS_TEXT});
    }

    @Test
    public void testProcess_addsVariantSelectorAndKeyCapEmoji() throws Exception {
        assertCodePointMatch(EMOJI_DIGIT_ES_KEYCAP);
    }

    @Test
    public void testProcess_doesNotAddEmoji_forVariantBaseWithoutSelector() throws Exception {
        assertCodePointDoesNotMatch(new int[]{CHAR_DIGIT});
    }

    @Test
    public void testProcess_addsAsteriskKeyCapEmoji() throws Exception {
        assertCodePointMatch(EMOJI_ASTERISK_KEYCAP);
    }

    @Test
    public void testProcess_addsSkinModifierEmoji() throws Exception {
        assertCodePointMatch(EMOJI_SKIN_MODIFIER);
        assertCodePointMatch(EMOJI_SKIN_MODIFIER_TYPE_ONE);
    }

    @Test
    public void testProcess_addsSkinModifierEmoji_withVariantSelector() throws Exception {
        assertCodePointMatch(EMOJI_SKIN_MODIFIER_WITH_VS);
    }

    @Test
    public void testProcess_addsSkinModifierEmoji_270c_withVariantSelector() throws Exception {
        // 0x270c is a Standardized Variant Base, Emoji Modifier Base and also Emoji
        // therefore it is different than i.e. 0x1f3c3. The code actually failed for this test
        // at first.
        assertCodePointMatch(0xF0734, new int[]{0x270C, CHAR_VS_EMOJI, CHAR_FITZPATRICK});
    }

    @Test
    public void testProcess_defaultStyleDoesNotAddSpan() throws Exception {
        assertCodePointDoesNotMatch(new int[]{CHAR_DEFAULT_TEXT_STYLE});
        assertCodePointMatch(DEFAULT_TEXT_STYLE);
    }

    @Test
    public void testProcess_defaultEmojiStyle_withTextStyleVs() throws Exception {
        assertCodePointMatch(EMOJI_SINGLE_CODEPOINT.id(),
                new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_EMOJI});
        assertCodePointDoesNotMatch(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_TEXT});
    }

    @Test
    public void testProcess_genderEmoji() throws Exception {
        assertCodePointMatch(EMOJI_GENDER);
        assertCodePointMatch(EMOJI_GENDER_WITHOUT_VS);
    }

    @Test
    public void testProcess_standardizedVariantEmojiExceptions() throws Exception {
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
    public void testProcess_addsZwjEmoji() throws Exception {
        assertCodePointMatch(EMOJI_WITH_ZWJ);
    }

    @Test
    public void testProcess_doesNotAddEmojiForNumbersAfterZwjEmo() throws Exception {
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
    public void testProcess_withAppend() throws Exception {
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
    public void testProcess_doesNotCreateSpannable_ifNoEmoji() throws Exception {
        CharSequence processed = EmojiCompat.get().process("abc");
        assertNotNull(processed);
        assertThat(processed, instanceOf(String.class));

        processed = EmojiCompat.get().process(new SpannedString("abc"));
        assertNotNull(processed);
        assertThat(processed, instanceOf(SpannedString.class));
    }

    @Test
    public void testProcess_reprocess() throws Exception {
        final String string = new TestString(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .append(EMOJI_SINGLE_CODEPOINT)
                .withPrefix().withSuffix().toString();

        Spannable processed = (Spannable) EmojiCompat.get().process(string);
        assertThat(processed, hasEmojiCount(3));

        final EmojiSpan[] spans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        final Set<EmojiSpan> spanSet = new HashSet<>();
        for (int i = 0; i < spans.length; i++) {
            spanSet.add(spans[i]);
        }

        processed = (Spannable) EmojiCompat.get().process(processed);
        assertThat(processed, hasEmojiCount(3));
        // new spans should be new instances
        final EmojiSpan[] newSpans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        for (int i = 0; i < newSpans.length; i++) {
            assertFalse(spanSet.contains(newSpans[i]));
        }
    }

    @Test
    public void testHasGlyph_returnsMetadata() throws Exception {
        final String sequence = new TestString(EMOJI_FLAG).toString();
        assertNotNull(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    public void testHasGlyph_returnsNullForNonExistentEmoji() throws Exception {
        final String sequence = new TestString(EMOJI_FLAG).append(0x1111).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    public void testHashGlyph_withDefaultEmojiStyles() throws Exception {
        String sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_EMOJI}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(new int[]{CHAR_DEFAULT_EMOJI_STYLE, CHAR_VS_TEXT}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    public void testHashGlyph_withMetadataVersion() throws Exception {
        final String sequence = new TestString(EMOJI_SINGLE_CODEPOINT).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence, 0));
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence, Integer.MAX_VALUE));
    }

    @Test
    public void testIsInitialized_returnsTrueIfLoadSuccess() throws InterruptedException {
        final WaitingDataLoader metadataLoader = new WaitingDataLoader(true /*success*/);
        final Config config = new TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        assertFalse(EmojiCompat.get().isInitialized());

        metadataLoader.getLoaderLatch().countDown();
        metadataLoader.getTestLatch().await();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(EmojiCompat.get().isInitialized());
    }

    @Test
    public void testIsInitialized_returnsFalseIfLoadFail() throws InterruptedException {
        final WaitingDataLoader metadataLoader = new WaitingDataLoader(false/*fail*/);
        final Config config = new TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        assertFalse(EmojiCompat.get().isInitialized());

        metadataLoader.getLoaderLatch().countDown();
        metadataLoader.getTestLatch().await();
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertFalse(EmojiCompat.get().isInitialized());
    }

    @Test
    public void testUpdateEditorInfoAttrs_doesNotSetKeyIfNotInitialized() {
        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.extras = new Bundle();

        final WaitingDataLoader metadataLoader = new WaitingDataLoader();
        final Config config = new TestConfig(metadataLoader);
        EmojiCompat.reset(config);

        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        final Bundle extras = editorInfo.extras;
        assertFalse(extras.containsKey(EmojiCompat.EDITOR_INFO_METAVERSION_KEY));
        assertFalse(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));

        metadataLoader.getLoaderLatch().countDown();
    }

    @Test
    public void testUpdateEditorInfoAttrs_setsKeysIfInitialized() {
        final EditorInfo editorInfo = new EditorInfo();
        editorInfo.extras = new Bundle();
        Config config = new TestConfig().setReplaceAll(false);
        EmojiCompat.reset(config);
        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        final Bundle extras = editorInfo.extras;
        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_METAVERSION_KEY));
        assertTrue(extras.getInt(EmojiCompat.EDITOR_INFO_METAVERSION_KEY) > 0);
        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
        assertFalse(extras.getBoolean(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));

        config = new TestConfig().setReplaceAll(true);
        EmojiCompat.reset(config);
        EmojiCompat.get().updateEditorInfoAttrs(editorInfo);

        assertTrue(extras.containsKey(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
        assertTrue(extras.getBoolean(EmojiCompat.EDITOR_INFO_REPLACE_ALL_KEY));
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

    //FAILS: CHAR_DIGIT, CHAR_VS_EMOJI, CHAR_VS_TEXT
}
