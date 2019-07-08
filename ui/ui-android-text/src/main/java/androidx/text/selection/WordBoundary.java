/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.text.selection;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Helper class to get word boundary for offset.
 *
 * Returns the start and end of the word at the given offset. Characters not part of a word, such as
 * spaces, symbols, and punctuation, have word breaks on both sides. In such cases, this method will
 * return [offset, offset+1].
 *
 * Word boundaries are defined more precisely in Unicode Standard Annex #29
 * http://www.unicode.org/reports/tr29/#Word_Boundaries
 *
 * Note: The contents of this file is initially copied from
 * <a href="https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/Editor.java">
 * Editor.java
 * </a>.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class WordBoundary {
    /**
     * This word iterator is set with text and used to determine word boundaries when a user is
     * selecting text.
     */
    private final WordIterator mWordIterator;
    /** Locale of the current text. */
    private final Locale mLocale;
    /** Current text. */
    private final CharSequence mText;

    public WordBoundary(Locale locale, CharSequence text) {
        mLocale = locale;
        mText = text;
        // TODO(qqd): Find a minimum range to pass to WordIterator that covers offset but equals or
        // is more than a word, for performance reason.
        mWordIterator = new WordIterator(mText, 0, mText.length(), mLocale);
    }

    /**
     * Get the start of the word which the given offset is in.
     *
     * @return the offset of the start of the word.
     */
    public int getWordStart(int offset) {
        // FIXME - For this and similar methods we're not doing anything to check if there's
        // a LocaleSpan in the text, this may be something we should try handling or checking for.
        int retOffset = mWordIterator.prevBoundary(offset);
        if (mWordIterator.isOnPunctuation(retOffset)) {
            // On punctuation boundary or within group of punctuation, find punctuation start.
            retOffset = mWordIterator.getPunctuationBeginning(offset);
        } else {
            // Not on a punctuation boundary, find the word start.
            retOffset = mWordIterator.getPrevWordBeginningOnTwoWordsBoundary(offset);
        }
        if (retOffset == BreakIterator.DONE) {
            return offset;
        }
        return retOffset;
    }

    /**
     * Get the end of the word which the given offset is in.
     *
     * @return the offset of the end of the word.
     */
    public int getWordEnd(int offset) {
        int retOffset = mWordIterator.nextBoundary(offset);
        if (mWordIterator.isAfterPunctuation(retOffset)) {
            // On punctuation boundary or within group of punctuation, find punctuation end.
            retOffset = mWordIterator.getPunctuationEnd(offset);
        } else {
            // Not on a punctuation boundary, find the word end.
            retOffset = mWordIterator.getNextWordEndOnTwoWordBoundary(offset);
        }
        if (retOffset == BreakIterator.DONE) {
            return offset;
        }
        return retOffset;
    }
}
