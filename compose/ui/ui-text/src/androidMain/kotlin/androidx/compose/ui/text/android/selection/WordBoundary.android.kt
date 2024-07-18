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
package androidx.compose.ui.text.android.selection

import java.text.BreakIterator

/**
 * Returns the start of the word at the given offset. Characters not part of a word, such as spaces,
 * symbols, and punctuation, have word breaks on both sides. In such cases, this method will return
 * offset.
 *
 * @param offset the interested offset.
 * @return the offset of the start of the word.
 */
internal fun WordIterator.getWordStart(offset: Int): Int {
    // FIXME - For this and similar methods we're not doing anything to check if there's
    //  a LocaleSpan in the text, this may be something we should try handling or checking for.
    var retOffset = prevBoundary(offset)
    retOffset =
        if (isOnPunctuation(retOffset)) {
            // On punctuation boundary or within group of punctuation, find punctuation start.
            getPunctuationBeginning(offset)
        } else {
            // Not on a punctuation boundary, find the word start.
            getPrevWordBeginningOnTwoWordsBoundary(offset)
        }
    return if (retOffset == BreakIterator.DONE) {
        offset
    } else retOffset
}

/**
 * Returns the end of the word at the given offset. Characters not part of a word, such as spaces,
 * symbols, and punctuation, have word breaks on both sides. In such cases, this method will return
 * offset + 1.
 *
 * @param offset the interested offset.
 * @return the offset of the end of the word.
 */
internal fun WordIterator.getWordEnd(offset: Int): Int {
    var retOffset = nextBoundary(offset)
    retOffset =
        if (isAfterPunctuation(retOffset)) {
            // On punctuation boundary or within group of punctuation, find punctuation end.
            getPunctuationEnd(offset)
        } else { // Not on a punctuation boundary, find the word end.
            getNextWordEndOnTwoWordBoundary(offset)
        }
    return if (retOffset == BreakIterator.DONE) {
        offset
    } else retOffset
}
