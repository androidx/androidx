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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.SpannedString;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import androidx.emoji2.bundled.util.Emoji;
import androidx.emoji2.bundled.util.EmojiMatcher;
import androidx.emoji2.bundled.util.KeyboardUtil;
import androidx.emoji2.bundled.util.TestString;
import androidx.emoji2.text.DefaultEmojiCompatConfig;
import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.EmojiSpan;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@MediumTest
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
    public void testInitWithContext_returnsNullWhenNotFound() {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.skipDefaultConfigurationLookup(false);

        Context context = mock(Context.class);
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory.class);
        when(factory.create(any())).thenReturn(null);

        EmojiCompat actual = EmojiCompat.init(context, factory);
        assertNull(actual);
    }

    @Test
    public void testInitWithContext_onlyQueriesOnce_onFailure() {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.skipDefaultConfigurationLookup(false);

        Context context = mock(Context.class);
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory.class);
        when(factory.create(any())).thenReturn(null);

        EmojiCompat.init(context, factory);
        verify(factory).create(eq(context));

        EmojiCompat.init(context, factory);
        verifyNoMoreInteractions(factory);
    }

    @Test
    public void testInitWithContext_returnsInstanceWhenFound() {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.skipDefaultConfigurationLookup(false);

        Context context = mock(Context.class);
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory.class);
        EmojiCompat.Config config = TestConfigBuilder.config();
        when(factory.create(any())).thenReturn(config);

        EmojiCompat actual = EmojiCompat.init(context, factory);
        assertNotNull(actual);
    }

    @Test
    public void testInitWithContext_onlyQueriesOnce_whenFound() {
        EmojiCompat.reset((EmojiCompat) null);
        EmojiCompat.skipDefaultConfigurationLookup(false);

        Context context = mock(Context.class);
        DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory factory = mock(
                DefaultEmojiCompatConfig.DefaultEmojiCompatConfigFactory.class);
        EmojiCompat.Config config = TestConfigBuilder.config();
        when(factory.create(any())).thenReturn(config);

        EmojiCompat.init(context, factory);
        verify(factory).create(eq(context));
        EmojiCompat.init(context, factory);
        verifyNoMoreInteractions(factory);
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
        assertThat(charSequence, Matchers.not(EmojiMatcher.hasEmoji()));
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
        assertThat(charSequence, Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testProcess_returnsSameCharSequence_pre19() {
        assertNull(EmojiCompat.get().process(null));

        CharSequence testString = "abc";
        assertSame(testString, EmojiCompat.get().process(testString));

        testString = new SpannableString("abc");
        assertSame(testString, EmojiCompat.get().process(testString));

        testString = new TestString(new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertSame(testString, EmojiCompat.get().process(testString));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSingleCodePointEmoji() {
        assertCodePointMatch(Emoji.EMOJI_SINGLE_CODEPOINT);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsFlagEmoji() {
        assertCodePointMatch(Emoji.EMOJI_FLAG);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsUnknownFlagEmoji() {
        assertCodePointMatch(Emoji.EMOJI_UNKNOWN_FLAG);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsRegionalIndicatorSymbol() {
        assertCodePointMatch(Emoji.EMOJI_REGIONAL_SYMBOL);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsKeyCapEmoji() {
        assertCodePointMatch(Emoji.EMOJI_DIGIT_KEYCAP);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbers() {
        assertCodePointDoesNotMatch(new int[] {Emoji.CHAR_DIGIT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbers_1() {
        final TestString string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).append('1', 'f');
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, EmojiMatcher.hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsVariantSelectorEmoji() {
        assertCodePointMatch(Emoji.EMOJI_DIGIT_ES);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddVariantSelectorTextStyle() {
        assertCodePointDoesNotMatch(new int[]{Emoji.CHAR_DIGIT, Emoji.CHAR_VS_TEXT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsVariantSelectorAndKeyCapEmoji() {
        assertCodePointMatch(Emoji.EMOJI_DIGIT_ES_KEYCAP);
    }

    @Test
    public void testProcess_doesNotAddEmoji_forVariantBaseWithoutSelector() {
        assertCodePointDoesNotMatch(new int[]{Emoji.CHAR_DIGIT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsAsteriskKeyCapEmoji() {
        assertCodePointMatch(Emoji.EMOJI_ASTERISK_KEYCAP);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji() {
        assertCodePointMatch(Emoji.EMOJI_SKIN_MODIFIER);
        assertCodePointMatch(Emoji.EMOJI_SKIN_MODIFIER_TYPE_ONE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji_withVariantSelector() {
        assertCodePointMatch(Emoji.EMOJI_SKIN_MODIFIER_WITH_VS);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsSkinModifierEmoji_270c_withVariantSelector() {
        // 0x270c is a Standardized Variant Base, Emoji Modifier Base and also Emoji
        // therefore it is different than i.e. 0x1f3c3. The code actually failed for this test
        // at first.
        assertCodePointMatch(0xF0734,
                new int[]{0x270C, Emoji.CHAR_VS_EMOJI, Emoji.CHAR_FITZPATRICK});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_defaultStyleDoesNotAddSpan() {
        assertCodePointDoesNotMatch(new int[]{Emoji.CHAR_DEFAULT_TEXT_STYLE});
        assertCodePointMatch(Emoji.DEFAULT_TEXT_STYLE);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_defaultEmojiStyle_withTextStyleVs() {
        assertCodePointMatch(Emoji.EMOJI_SINGLE_CODEPOINT.id(),
                new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE, Emoji.CHAR_VS_EMOJI});
        assertCodePointDoesNotMatch(new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE, Emoji.CHAR_VS_TEXT});
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_genderEmoji() {
        assertCodePointMatch(Emoji.EMOJI_GENDER);
        assertCodePointMatch(Emoji.EMOJI_GENDER_WITHOUT_VS);
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
        assertCodePointMatch(Emoji.EMOJI_WITH_ZWJ);
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_doesNotAddEmojiForNumbersAfterZwjEmo() {
        TestString string = new TestString(Emoji.EMOJI_WITH_ZWJ).append(0x20, 0x2B, 0x31)
                .withSuffix().withPrefix();
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence,
                EmojiMatcher.hasEmojiAt(Emoji.EMOJI_WITH_ZWJ, string.emojiStartIndex(),
                        string.emojiEndIndex() - 3));
        assertThat(charSequence, EmojiMatcher.hasEmojiCount(1));

        string = new TestString(Emoji.EMOJI_WITH_ZWJ).withSuffix().withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, EmojiMatcher.hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_addsEmojiThatFollowsDigit() {
        TestString string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).prepend('N', '5');
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, EmojiMatcher.hasEmojiAt(
                Emoji.EMOJI_SINGLE_CODEPOINT, string.emojiStartIndex() + 2,
                string.emojiEndIndex()));
        assertThat(charSequence, EmojiMatcher.hasEmojiCount(1));

        string = new TestString(Emoji.EMOJI_WITH_ZWJ).prepend('N', '5');
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, EmojiMatcher.hasEmojiAt(
                Emoji.EMOJI_WITH_ZWJ, string.emojiStartIndex() + 2,
                string.emojiEndIndex()));
        assertThat(charSequence, EmojiMatcher.hasEmojiCount(1));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withAppend() {
        final Editable editable = new SpannableStringBuilder(new TestString('a').withPrefix()
                .withSuffix().toString());
        final int start = 1;
        final int end = start + Emoji.EMOJI_SINGLE_CODEPOINT.charCount();
        editable.insert(start, new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString());
        EmojiCompat.get().process(editable, start, end);
        assertThat(editable, EmojiMatcher.hasEmojiCount(1));
        assertThat(editable, EmojiMatcher.hasEmojiAt(Emoji.EMOJI_SINGLE_CODEPOINT, start, end));
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
        final String string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT)
                .append(Emoji.EMOJI_SINGLE_CODEPOINT)
                .append(Emoji.EMOJI_SINGLE_CODEPOINT)
                .withPrefix().withSuffix().toString();

        Spannable processed = (Spannable) EmojiCompat.get().process(string);
        assertThat(processed, EmojiMatcher.hasEmojiCount(3));

        final EmojiSpan[] spans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        final Set<EmojiSpan> spanSet = new HashSet<>();
        Collections.addAll(spanSet, spans);

        processed = (Spannable) EmojiCompat.get().process(processed);
        assertThat(processed, EmojiMatcher.hasEmojiCount(3));
        // new spans should be new instances
        final EmojiSpan[] newSpans = processed.getSpans(0, processed.length(), EmojiSpan.class);
        for (int i = 0; i < newSpans.length; i++) {
            assertFalse(spanSet.contains(newSpans[i]));
        }
    }

    @SuppressLint("Range")
    @Test(expected = IllegalArgumentException.class)
    public void testProcess_throwsException_withMaxEmojiSetToNegative() {
        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                -1 /*maxEmojiCount*/);

        assertThat(processed, Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    public void testProcess_withMaxEmojiSetToZero() {
        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                0 /*maxEmojiCount*/);

        assertThat(processed, Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSetToOne() {
        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();

        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                1 /*maxEmojiCount*/);

        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed, EmojiMatcher.hasEmoji(Emoji.EMOJI_SINGLE_CODEPOINT));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSetToLessThenExistingSpanCount() {
        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT)
                .append(Emoji.EMOJI_SINGLE_CODEPOINT)
                .append(Emoji.EMOJI_SINGLE_CODEPOINT)
                .toString();

        // add 2 spans
        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(), 2);

        assertThat(processed, EmojiMatcher.hasEmojiCount(2));

        // use the Spannable with 2 spans, but use maxEmojiCount=1, start from the beginning of
        // last (3rd) emoji
        EmojiCompat.get().process(processed,
                original.length() - Emoji.EMOJI_SINGLE_CODEPOINT.charCount(),
                original.length(),
                1 /*maxEmojiCount*/);

        // expectation: there are still 2 emojis
        assertThat(processed, EmojiMatcher.hasEmojiCount(2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withMaxEmojiSet_withExistingEmojis() {
        // test string with two emoji characters
        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT)
                .append(Emoji.EMOJI_FLAG).toString();

        // process and add 1 EmojiSpan, maxEmojiCount=1
        CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                1 /*maxEmojiCount*/);

        // assert that there is a single emoji
        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed,
                EmojiMatcher.hasEmojiAt(
                        Emoji.EMOJI_SINGLE_CODEPOINT, 0, Emoji.EMOJI_SINGLE_CODEPOINT.charCount()));

        // call process again with the charSequence that already has 1 span
        processed = EmojiCompat.get().process(processed, Emoji.EMOJI_SINGLE_CODEPOINT.charCount(),
                processed.length(), 1 /*maxEmojiCount*/);

        // assert that there is still a single emoji
        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed,
                EmojiMatcher.hasEmojiAt(
                        Emoji.EMOJI_SINGLE_CODEPOINT, 0, Emoji.EMOJI_SINGLE_CODEPOINT.charCount()));

        // make the same call, this time with maxEmojiCount=2
        processed = EmojiCompat.get().process(processed, Emoji.EMOJI_SINGLE_CODEPOINT.charCount(),
                processed.length(), 2 /*maxEmojiCount*/);

        // assert that it contains 2 emojis
        assertThat(processed, EmojiMatcher.hasEmojiCount(2));
        assertThat(processed,
                EmojiMatcher.hasEmojiAt(
                        Emoji.EMOJI_SINGLE_CODEPOINT, 0, Emoji.EMOJI_SINGLE_CODEPOINT.charCount()));
        assertThat(processed,
                EmojiMatcher.hasEmojiAt(Emoji.EMOJI_FLAG, Emoji.EMOJI_SINGLE_CODEPOINT.charCount(),
                        original.length()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withReplaceNonExistent_callsGlyphChecker() {
        final EmojiCompat.GlyphChecker glyphChecker = mock(EmojiCompat.GlyphChecker.class);
        final EmojiCompat.Config config = TestConfigBuilder.freshConfig()
                .setReplaceAll(true)
                .setGlyphChecker(glyphChecker);
        EmojiCompat.reset(config);

        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(true);

        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();

        CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                Integer.MAX_VALUE /*maxEmojiCount*/, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        // when function overrides config level replaceAll, a call to GlyphChecker is expected.
        verify(glyphChecker, times(1))
                .hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt());

        // since replaceAll is false, there should be no EmojiSpans
        assertThat(processed, Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withReplaceDefault_doesNotCallGlyphChecker() {
        final EmojiCompat.GlyphChecker glyphChecker = mock(EmojiCompat.GlyphChecker.class);
        final EmojiCompat.Config config = TestConfigBuilder.freshConfig()
                .setReplaceAll(true)
                .setGlyphChecker(glyphChecker);
        EmojiCompat.reset(config);

        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(true);

        final String original = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();
        // call without replaceAll, config value (true) should be used
        final CharSequence processed = EmojiCompat.get().process(original, 0, original.length(),
                Integer.MAX_VALUE /*maxEmojiCount*/, EmojiCompat.REPLACE_STRATEGY_DEFAULT);

        // replaceAll=true should not call hasGlyph
        verify(glyphChecker, times(0))
                .hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt());

        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed, EmojiMatcher.hasEmoji(Emoji.EMOJI_SINGLE_CODEPOINT));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testProcess_withSpanned_replaceNonExistent() {
        final EmojiCompat.GlyphChecker glyphChecker = mock(EmojiCompat.GlyphChecker.class);
        final EmojiCompat.Config config = TestConfigBuilder.freshConfig()
                .setReplaceAll(false)
                .setGlyphChecker(glyphChecker);
        EmojiCompat.reset(config);

        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(false);

        final String string = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).append(
                Emoji.EMOJI_SINGLE_CODEPOINT).toString();
        CharSequence processed = EmojiCompat.get().process(string, 0, string.length(),
                Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_ALL);

        final SpannedString spanned = new SpannedString(processed);
        assertThat(spanned, EmojiMatcher.hasEmojiCount(2));

        // change glyphChecker to return true so that no emoji will be added
        when(glyphChecker.hasGlyph(any(CharSequence.class), anyInt(), anyInt(), anyInt()))
                .thenReturn(true);

        processed = EmojiCompat.get().process(spanned, 0, spanned.length(),
                Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        assertThat(processed, Matchers.not(EmojiMatcher.hasEmoji()));

        // start: 1 char after the first emoji (in the second emoji)
        processed = EmojiCompat.get().process(spanned, Emoji.EMOJI_SINGLE_CODEPOINT.charCount() + 1,
                spanned.length(), Integer.MAX_VALUE, EmojiCompat.REPLACE_STRATEGY_NON_EXISTENT);

        assertThat(processed, EmojiMatcher.hasEmojiCount(1));
        assertThat(processed, EmojiMatcher.hasEmoji(Emoji.EMOJI_SINGLE_CODEPOINT));
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
        String sequence = new TestString(new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 18)
    public void testHasEmojiGlyph_withMetaVersion_pre19() {
        String sequence = new TestString(new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence, Integer.MAX_VALUE));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHasEmojiGlyph_returnsTrueForExistingEmoji() {
        final String sequence = new TestString(Emoji.EMOJI_FLAG).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    public void testHasGlyph_returnsFalseForNonExistentEmoji() {
        final String sequence = new TestString(Emoji.EMOJI_FLAG).append(0x1111).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHashEmojiGlyph_withDefaultEmojiStyles() {
        String sequence = new TestString(new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(
                new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE, Emoji.CHAR_VS_EMOJI}).toString();
        assertTrue(EmojiCompat.get().hasEmojiGlyph(sequence));

        sequence = new TestString(
                new int[]{Emoji.CHAR_DEFAULT_EMOJI_STYLE, Emoji.CHAR_VS_TEXT}).toString();
        assertFalse(EmojiCompat.get().hasEmojiGlyph(sequence));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testHashEmojiGlyph_withMetadataVersion() {
        final String sequence = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT).toString();
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
        final EmojiCompat.MetadataRepoLoader loader =
                Mockito.spy(new TestConfigBuilder.TestEmojiDataLoader());
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
        final EmojiCompat.MetadataRepoLoader loader =
                Mockito.spy(new TestConfigBuilder.TestEmojiDataLoader());
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
        final EmojiCompat.MetadataRepoLoader loader =
                Mockito.spy(new TestConfigBuilder.TestEmojiDataLoader());
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
        final TestConfigBuilder.WaitingDataLoader loader = Mockito.spy(
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
        EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(false);
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
        final TestString testString = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT);
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
        final TestString testString = new TestString(Emoji.EMOJI_SINGLE_CODEPOINT);
        final Editable editable = spy(new SpannableStringBuilder(testString.toString()));
        Selection.setSelection(editable, testString.emojiEndIndex());
        final KeyEvent event = KeyboardUtil.del();

        reset(editable);
        verifyNoMoreInteractions(editable);

        assertFalse(EmojiCompat.handleOnKeyDown(editable, event.getKeyCode(), event));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_whenEmojiInTheMiddle() {
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true);
        EmojiCompat.reset(config);
        String s = new TestString(0x0061, Emoji.CHAR_DEFAULT_TEXT_STYLE, 0x0062).toString();
        // no span should be added as the emoji is text style presented by default
        assertThat(EmojiCompat.get().process(s), Matchers.not(EmojiMatcher.hasEmoji()));
        // a span should be added when we use the emoji style presentation as default
        EmojiCompat.reset(config.setUseEmojiAsDefaultStyle(true));
        assertThat(EmojiCompat.get().process(s),
                EmojiMatcher.hasEmojiAt(Emoji.DEFAULT_TEXT_STYLE, 1, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_whenEmojiAtTheEnd() {
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true);
        EmojiCompat.reset(config);
        String s = new TestString(0x0061, Emoji.CHAR_DEFAULT_TEXT_STYLE).toString();
        // no span should be added as the emoji is text style presented by default
        assertThat(EmojiCompat.get().process(s), Matchers.not(EmojiMatcher.hasEmoji()));
        // a span should be added when we use the emoji style presentation as default
        EmojiCompat.reset(config.setUseEmojiAsDefaultStyle(true));
        assertThat(EmojiCompat.get().process(s),
                EmojiMatcher.hasEmojiAt(Emoji.DEFAULT_TEXT_STYLE, 1, 2));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_noEmojisAdded_whenMarkedAsException() {
        final String s = new TestString(Emoji.CHAR_DEFAULT_TEXT_STYLE).toString();
        final List<Integer> exceptions =
                Arrays.asList(Emoji.CHAR_DEFAULT_TEXT_STYLE + 1, Emoji.CHAR_DEFAULT_TEXT_STYLE);
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true)
                .setUseEmojiAsDefaultStyle(true, exceptions);
        EmojiCompat.reset(config);
        // no span should be added as the text style codepoint is marked as exception
        assertThat(EmojiCompat.get().process(s), Matchers.not(EmojiMatcher.hasEmoji()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 19)
    public void testUseEmojiAsDefaultStyle_emojisAdded_whenNotMarkedAsException() {
        final String s = new TestString(Emoji.CHAR_DEFAULT_TEXT_STYLE).toString();
        final List<Integer> exceptions =
                Arrays.asList(Emoji.CHAR_DEFAULT_TEXT_STYLE - 1, Emoji.CHAR_DEFAULT_TEXT_STYLE + 1);
        final EmojiCompat.Config config = TestConfigBuilder.config().setReplaceAll(true)
                .setUseEmojiAsDefaultStyle(true, exceptions);
        EmojiCompat.reset(config);
        // a span should be added as the codepoint is not included in the set of exceptions
        assertThat(EmojiCompat.get().process(s),
                EmojiMatcher.hasEmojiAt(Emoji.DEFAULT_TEXT_STYLE, 0, 1));
    }

    private void assertCodePointMatch(Emoji.EmojiMapping emoji) {
        assertCodePointMatch(emoji.id(), emoji.codepoints());
    }

    private void assertCodePointMatch(int id, int[] codepoints) {
        TestString string = new TestString(codepoints);
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence,
                EmojiMatcher.hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is in the middle of string
        string = new TestString(codepoints).withPrefix().withSuffix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence,
                EmojiMatcher.hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is at the end of string
        string = new TestString(codepoints).withSuffix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence,
                EmojiMatcher.hasEmojiAt(id, string.emojiStartIndex(), string.emojiEndIndex()));
    }

    private void assertCodePointDoesNotMatch(int[] codepoints) {
        TestString string = new TestString(codepoints);
        CharSequence charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, Matchers.not(EmojiMatcher.hasEmoji()));

        string = new TestString(codepoints).withSuffix().withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, Matchers.not(EmojiMatcher.hasEmoji()));

        string = new TestString(codepoints).withPrefix();
        charSequence = EmojiCompat.get().process(string.toString());
        assertThat(charSequence, Matchers.not(EmojiMatcher.hasEmoji()));
    }
}
