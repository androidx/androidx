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

import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

import android.graphics.Typeface;
import android.text.Spannable;

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

    TestTypefaceEmojiRasterizer mInitialCodepoint = new TestTypefaceEmojiRasterizer(new int[]{1}, 1,
            (short) 1);
    TestTypefaceEmojiRasterizer mAnotherInitial = new TestTypefaceEmojiRasterizer(new int[]{2}, 2,
            (short) 1);
    TestTypefaceEmojiRasterizer mAddedLast = new TestTypefaceEmojiRasterizer(new int[]{1, 2}, 3,
            (short) 2);
    TestTypefaceEmojiRasterizer mUnrelatedLast = new TestTypefaceEmojiRasterizer(new int[]{3, 4}, 4,
            (short) 2);
    TestTypefaceEmojiRasterizer mExactMatchLast = new TestTypefaceEmojiRasterizer(new int[]{5}, 5,
            (short) 2);

    TestTypefaceEmojiRasterizer mExcludedEmoji = new TestTypefaceEmojiRasterizer(new int[] {6}, 5,
            (short) 3);

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
        // 5 always fails, flushing 1
        int result = mProcessor.getEmojiMatch(sequenceFor(1, 5),
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
        // {1, 2} matches, 77 is unknown; c3 is unknown to this font instance
        int result = mProcessor.getEmojiMatch(sequenceFor(1, 2, 77), 3);
        assertEquals(EmojiCompat.EMOJI_FALLBACK, result);
    }

    @Test
    public void sequenceWithMatch_processReturns_spannable() {
        final CharSequence source = sequenceFor(1, 1, 2, 77);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertTrue(result instanceof Spannable);
        assertEquals(2, ((Spannable) result).getSpans(0, source.length(), EmojiSpan.class).length);
        assertEmojiSpan(mInitialCodepoint, 0, 1, (Spannable) result);
        assertEmojiSpan(mAddedLast, 1, 3, (Spannable) result);
    }

    @Test
    public void noMatch_processReturns_charSequence() {
        final CharSequence source = sequenceFor(3, 77, 4, 77);
        final CharSequence result = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);
        assertFalse(result instanceof Spannable);
        assertEquals(source, result);
    }

    @Test
    public void noReplacement_forExcludedEmoji() {
        CharSequence source = sequenceFor(6);
        CharSequence unExcluded = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);

        // again with exclusions
        Set<int[]> exclusions = new HashSet<>();
        exclusions.add(new int[] { 6 });
        init(exclusions);
        CharSequence excluded = mProcessor.process(source, 0, source.length(),
                EmojiCompat.EMOJI_COUNT_UNLIMITED, true);

        assertNotEquals(excluded, unExcluded);
        assertFalse(excluded instanceof Spannable);
        assertEquals(source, excluded);
    }

    @Test
    public void sequence_getEmojiStartEnd() {
        final CharSequence source = sequenceFor(77, 1, 1, 2, 77);
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
