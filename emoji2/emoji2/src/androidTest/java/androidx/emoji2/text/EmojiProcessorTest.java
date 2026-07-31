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

package androidx.emoji2.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EmojiProcessorTest extends TestCase {
    private EmojiProcessor mProcessor;

    private static final int CODEPOINT_CLOUD = 0x2601;
    private static final int CODEPOINT_UMBRELLA = 0x2602;
    private static final int CODEPOINT_SNOWMAN = 0x2603;
    private static final int CODEPOINT_COMET = 0x2604;
    private static final int CODEPOINT_STAR = 0x2605;
    private static final int CODEPOINT_WHITE_STAR = 0x2606;
    private static final int CODEPOINT_DIGIT_ONE = 0x0031;

    private static final int CODEPOINT_VS15 = 0xFE0E;
    private static final int CODEPOINT_VS16 = 0xFE0F;
    private static final int CODEPOINT_KEYCAP = 0x20E3;

    TestTypefaceEmojiRasterizer mInitialCodepoint = new TestTypefaceEmojiRasterizer(
            new int[]{CODEPOINT_CLOUD}, 1, (short) 1);
    TestTypefaceEmojiRasterizer mAnotherInitial = new TestTypefaceEmojiRasterizer(
            new int[]{CODEPOINT_UMBRELLA}, 2, (short) 1);
    TestTypefaceEmojiRasterizer mAddedLast = new TestTypefaceEmojiRasterizer(
            new int[]{CODEPOINT_CLOUD, CODEPOINT_UMBRELLA}, 3, (short) 2);
    TestTypefaceEmojiRasterizer mUnrelatedLast = new TestTypefaceEmojiRasterizer(
            new int[]{CODEPOINT_SNOWMAN, CODEPOINT_COMET}, 4, (short) 2);
    TestTypefaceEmojiRasterizer mExactMatchLast = new TestTypefaceEmojiRasterizer(
            new int[]{CODEPOINT_STAR}, 5, (short) 2);

    TestTypefaceEmojiRasterizer mExcludedEmoji = new TestTypefaceEmojiRasterizer(
            new int[] {CODEPOINT_WHITE_STAR}, 5, (short) 3);

    TestTypefaceEmojiRasterizer mKeycapEmoji = new TestTypefaceEmojiRasterizer(
            new int[] {CODEPOINT_DIGIT_ONE, CODEPOINT_VS16, CODEPOINT_KEYCAP}, 6, (short) 1);

    @Before
    public void clearResourceIndex() {
        init(Collections.emptySet());
    }

    private void init(Set<int[]> emojiExclusions) {
        MetadataRepo metadataRepo = MetadataRepo.create(mock(Typeface.class));
        metadataRepo.put(mInitialCodepoint);
        metadataRepo.put(mAnotherInitial);
        metadataRepo.put(mAddedLast);
        metadataRepo.put(mUnrelatedLast);
        metadataRepo.put(mExactMatchLast);
        metadataRepo.put(mExcludedEmoji);
        metadataRepo.put(mKeycapEmoji);
        EmojiCompat.SpanFactory spanFactory = new EmojiCompat.DefaultSpanFactory();
        EmojiCompat.GlyphChecker glyphChecker = (charSequence, start, end, sdkAdded) -> true;
        mProcessor = new EmojiProcessor(metadataRepo,
                spanFactory,
                glyphChecker,
                true,
                null,
                emojiExclusions);
    }

    @Test
    public void whenNoMatch_getEmojiMatchReturns_noMatch() {
        int result = mProcessor.getEmojiMatch(sequenceFor(77, 77, 12),
                99999);
        assertEquals(EmojiCompat.EMOJI_UNSUPPORTED, result);
    }

    @Test
    public void definiteSubsequenceMatch_returns_decomposes() {
        // CODEPOINT_STAR always fails, flushing CODEPOINT_CLOUD
        int result = mProcessor.getEmojiMatch(sequenceFor(CODEPOINT_CLOUD, CODEPOINT_STAR),
                99999);
        assertEquals(EmojiCompat.EMOJI_FALLBACK, result);
    }

    @Test
    public void exactMatchOneCodepoint_atMetadataVersion_alwaysMatches() {
        int r1 = mProcessor.getEmojiMatch(mInitialCodepoint.asCharSequence(), 1);
        int r2 = mProcessor.getEmojiMatch(mExactMatchLast.asCharSequence(), 2);
        assertEquals(EmojiCompat.EMOJI_SUPPORTED, r1);
        assertEquals(EmojiCompat.EMOJI_SUPPORTED, r2);
    }

    @Test
    public void exactMatch_multipleCodepoints_atMetadataVersion_alwaysMatches() {
        int r1 = mProcessor.getEmojiMatch(mAddedLast.asCharSequence(), 2);
        int r2 = mProcessor.getEmojiMatch(mUnrelatedLast.asCharSequence(), 2);
        assertEquals(EmojiCompat.EMOJI_SUPPORTED, r1);
        assertEquals(EmojiCompat.EMOJI_SUPPORTED, r2);
    }

    @Test
    public void potentialMatch_withFailedExactMatch_decomposes() {
        // {1} {2} is in c1; but {1, 2} is in c2. This will match on previous level
        int result = mProcessor.getEmojiMatch(mAddedLast.asCharSequence(), 1);
        assertEquals(EmojiCompat.EMOJI_FALLBACK, result);
    }

    @Test
    public void sequence_extendedAtLaterMetadata_decomposes() {
        // {CODEPOINT_CLOUD, CODEPOINT_UMBRELLA} matches, 77 is unknown;
        // c3 is unknown to this font instance
        int result = mProcessor.getEmojiMatch(
                sequenceFor(CODEPOINT_CLOUD, CODEPOINT_UMBRELLA, 77), 3);
        assertEquals(EmojiCompat.EMOJI_FALLBACK, result);
    }

    @Test
    public void sequenceWithMatch_processReturns_spannable() {
        final CharSequence source = sequenceFor(
                CODEPOINT_CLOUD, CODEPOINT_CLOUD, CODEPOINT_UMBRELLA, 77);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertEquals(2, ((Spannable) result).getSpans(0, source.length(), EmojiSpan.class).length);
        assertEmojiSpan(mInitialCodepoint, 0, 1, (Spannable) result);
        assertEmojiSpan(mAddedLast, 1, 3, (Spannable) result);
    }

    @Test
    public void noMatch_processReturns_charSequence() {
        final CharSequence source = sequenceFor(CODEPOINT_SNOWMAN, 77, CODEPOINT_COMET, 77);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertFalse(result instanceof Spannable);
        assertEquals(source, result);
    }

    @Test
    public void noReplacement_forExcludedEmoji() {
        CharSequence source = sequenceFor(CODEPOINT_WHITE_STAR);
        CharSequence unExcluded = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);

        // again with exclusions
        Set<int[]> exclusions = new HashSet<>();
        exclusions.add(new int[] { CODEPOINT_WHITE_STAR });
        init(exclusions);
        CharSequence excluded = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);

        assertNotEquals(excluded, unExcluded);
        assertFalse(excluded instanceof Spannable);
        assertEquals(source, excluded);
    }

    @Test
    public void sequence_getEmojiStartEnd() {
        final CharSequence source = sequenceFor(
                77, CODEPOINT_CLOUD, CODEPOINT_CLOUD, CODEPOINT_UMBRELLA, 77);
        assertEquals(-1, mProcessor.getEmojiStart(source, 0));
        assertEquals(-1, mProcessor.getEmojiEnd(source, 0));

        assertEquals(1, mProcessor.getEmojiStart(source, 1));
        assertEquals(2, mProcessor.getEmojiEnd(source, 1));

        assertEquals(2, mProcessor.getEmojiStart(source, 2));
        assertEquals(4, mProcessor.getEmojiEnd(source, 2));

        assertEquals(2, mProcessor.getEmojiStart(source, 3));
        assertEquals(4, mProcessor.getEmojiEnd(source, 3));

        assertEquals(-1, mProcessor.getEmojiStart(source, 4));
        assertEquals(-1, mProcessor.getEmojiEnd(source, 4));
    }

    @Test
    public void noCandidates_processReturns_early() {
        final CharSequence source = "Hello World";
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertFalse(result instanceof Spannable);
        assertEquals(source, result);
    }

    @Test
    public void variationSelectors_behavior() {
        // [mInitialCodepoint] + FE0E (Text Style) -> should not match (returns original sequence)
        final CharSequence textStyleSeq = sequenceFor(CODEPOINT_CLOUD, CODEPOINT_VS15);
        CharSequence result = mProcessor.process(textStyleSeq, 0, textStyleSeq.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertFalse(result instanceof Spannable);
        assertEquals(textStyleSeq, result);

        // [mInitialCodepoint] + FE0F (Emoji Style) -> should match
        final CharSequence emojiStyleSeq = sequenceFor(CODEPOINT_CLOUD, CODEPOINT_VS16);
        result = mProcessor.process(emojiStyleSeq, 0, emojiStyleSeq.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertEquals(1, ((Spannable) result).getSpans(0, emojiStyleSeq.length(),
                EmojiSpan.class).length);
    }

    @Test
    public void spannableInput_noSpans_matchesEmoji() {
        final CharSequence source = sequenceFor(CODEPOINT_CLOUD, CODEPOINT_UMBRELLA);
        final SpannableString spannable = new SpannableString(source);
        final CharSequence result = mProcessor.process(spannable, 0, spannable.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertSame(spannable, result);
        assertEquals(1, spannable.getSpans(0, spannable.length(), EmojiSpan.class).length);
        assertEmojiSpan(mAddedLast, 0, 2, spannable);
    }

    @Test
    public void spannableInput_withSpans_replacesSpans() {
        final CharSequence source = sequenceFor(CODEPOINT_CLOUD, CODEPOINT_UMBRELLA);
        final SpannableString spannable = new SpannableString(source);
        final EmojiSpan mockSpan = new TypefaceEmojiSpan(mInitialCodepoint);
        spannable.setSpan(mockSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        final CharSequence result = mProcessor.process(spannable, 0, spannable.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertSame(spannable, result);
        final EmojiSpan[] spans = spannable.getSpans(0, spannable.length(), EmojiSpan.class);
        assertEquals(1, spans.length);
        assertNotEquals(mockSpan, spans[0]);
        assertEquals(mAddedLast.getId(), spans[0].getId());
    }

    @Test
    public void keycapEmoji_matches() {
        final CharSequence source = sequenceFor(CODEPOINT_DIGIT_ONE, CODEPOINT_VS16,
                CODEPOINT_KEYCAP);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertEquals(1, ((Spannable) result).getSpans(0, source.length(), EmojiSpan.class).length);
        assertEmojiSpan(mKeycapEmoji, 0, source.length(), (Spannable) result);
    }

    @Test
    public void process_startsWithNonCandidate_matchesEmoji() {
        final CharSequence source = sequenceFor(77, CODEPOINT_CLOUD);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertEquals(1, ((Spannable) result).getSpans(0, source.length(), EmojiSpan.class).length);
        assertEmojiSpan(mInitialCodepoint, 1, 2, (Spannable) result);
    }

    private CharSequence sequenceFor(int... codepoints) {
        StringBuilder sb = new StringBuilder(codepoints.length);
        for (int i = 0; i < codepoints.length; i++) {
            sb.append(Character.toChars(codepoints[i]));
        }
        return sb.toString();
    }

    private void assertEmojiSpan(TypefaceEmojiRasterizer expectedMetadata, int expectedStart,
            int expectedEnd,
            Spannable actual) {
        final EmojiSpan[] spans = actual.getSpans(expectedStart, expectedEnd, EmojiSpan.class);
        assertEquals(1, spans.length);
        assertEquals(expectedMetadata.getId(), spans[0].getId());
        assertEquals(expectedStart, actual.getSpanStart(spans[0]));
        assertEquals(expectedEnd, actual.getSpanEnd(spans[0]));
    }
}
