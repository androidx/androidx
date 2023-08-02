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

import static org.mockito.Mockito.mock;

import android.graphics.Typeface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 19)
public class EmojiProcessorTest extends TestCase {
    private EmojiProcessor mProcessor;

    TestEmojiMetadata mInitialCodepoint = new TestEmojiMetadata(new int[] {1}, 1, (short) 1);
    TestEmojiMetadata mAnotherInitial = new TestEmojiMetadata(new int[] {2}, 2, (short) 1);
    TestEmojiMetadata mAddedLast = new TestEmojiMetadata(new int[] {1, 2}, 3, (short) 2);
    TestEmojiMetadata mUnrelatedLast = new TestEmojiMetadata(new int[] {3, 4}, 4, (short) 2);
    TestEmojiMetadata mExactMatchLast = new TestEmojiMetadata(new int[] {5}, 5, (short) 2);

    @Before
    public void clearResourceIndex() {
        MetadataRepo metadataRepo = MetadataRepo.create(mock(Typeface.class));
        metadataRepo.put(mInitialCodepoint);
        metadataRepo.put(mAnotherInitial);
        metadataRepo.put(mAddedLast);
        metadataRepo.put(mUnrelatedLast);
        metadataRepo.put(mExactMatchLast);
        EmojiCompat.SpanFactory spanFactory = new EmojiCompat.SpanFactory();
        EmojiCompat.GlyphChecker glyphChecker = (charSequence, start, end, sdkAdded) -> true;
        mProcessor = new EmojiProcessor(metadataRepo, spanFactory, glyphChecker, true, null);
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

    private CharSequence sequenceFor(int... codepoints) {
        StringBuilder sb = new StringBuilder(codepoints.length);
        for (int i = 0; i < codepoints.length; i++) {
            sb.append(Character.toChars(codepoints[i]));
        }
        return sb.toString();
    }
}
