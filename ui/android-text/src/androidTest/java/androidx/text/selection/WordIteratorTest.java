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

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.BreakIterator;
import java.util.Locale;

@SmallTest
@RunWith(JUnit4.class)
public class WordIteratorTest {
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_IndexOutOfBounds_too_big() {
        new WordIterator("text", 100, 100, Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_IndexOutOfBounds_too_small() {
        new WordIterator("text", -100, -100, Locale.ENGLISH);
    }

    @Test
    public void testConstructor_valid_full_text() {
        final String text = "text";

        new WordIterator(text, 0, text.length(), Locale.ENGLISH);
    }

    @Test
    public void testConstructor_valid_beginning() {
        final String text = "text";

        new WordIterator(text, 0, 0, Locale.ENGLISH);
    }

    @Test
    public void testConstructor_valid_end() {
        final String text = "text";

        new WordIterator(text, 0, text.length(), Locale.ENGLISH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextBoundary_out_of_boundary_too_small() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.nextBoundary(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNextBoundary_out_of_boundary_too_big() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.nextBoundary(text.length() + 1);
    }

    @Test
    public void testNextBoundary_iterate_through() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        // Start from the beginning.
        int currentOffset = 0;

        // The word is "abc".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('c') + 1, currentOffset);

        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('d'), currentOffset);

        // The word is "def".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('f') + 1, currentOffset);

        // The word is "-".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('g'), currentOffset);

        // The word is "ghi".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('i') + 1, currentOffset);

        // The word is ".".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('.') + 1, currentOffset);

        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('j'), currentOffset);

        // The word is "jkl".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.length(), currentOffset);

        // WordIterator reaches the end.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(BreakIterator.DONE, currentOffset);
    }

    @Test
    public void testNextBoundary_iterate_through_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(),
                new Locale("he", "IL"));

        // Start from the beginning.
        int currentOffset = 0;

        // The word is "\u05d0\u05d1\u05d2"("אבג")
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d2') + 1, currentOffset);

        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d3'), currentOffset);

        // The word is "\u05d3\u05d4"("דה")
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('-'), currentOffset);

        // The word is "-".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d5'), currentOffset);

        // The word is "\u05d5\u05d6("וז")
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('.'), currentOffset);

        // The word is ".".
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('.') + 1, currentOffset);

        // The word is space.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d7'), currentOffset);

        // The word is "\u05d7\u05d8"("חט")
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(text.length(), currentOffset);

        // WordIterator reaches the end.
        currentOffset = wordIterator.nextBoundary(currentOffset);
        assertEquals(BreakIterator.DONE, currentOffset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrevBoundary_out_of_boundary_too_small() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.prevBoundary(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPrevBoundary_out_of_boundary_too_big() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.prevBoundary(text.length() + 1);
    }

    @Test
    public void testPrevBoundary_iterate_through() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        // Start from the end.
        int currentOffset = text.length();

        // The word is "jkl".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('j'), currentOffset);

        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('.') + 1, currentOffset);

        // The word is ".".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('i') + 1, currentOffset);

        // The word is "ghi".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('g'), currentOffset);

        // The word is "-".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('f') + 1, currentOffset);

        // The word is "def".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('d'), currentOffset);

        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('c') + 1, currentOffset);

        // The word is "abc".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('a'), currentOffset);

        // WordIterator reaches the beginning.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(BreakIterator.DONE, currentOffset);
    }

    @Test
    public void testPrevBoundary_iterate_through_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(),
                new Locale("he", "IL"));

        // Start from the end.
        int currentOffset = text.length();

        // The word is "\u05d7\u05d8"("חט")
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d7'), currentOffset);

        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('.') + 1, currentOffset);

        // The word is '.'.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('.'), currentOffset);

        // The word is "\u05d5\u05d6("וז")
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d5'), currentOffset);

        // The word is "-".
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('-'), currentOffset);

        // The word is "\u05d3\u05d4"("דה")
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d3'), currentOffset);

        // The word is space.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf(' '), currentOffset);

        // The word is "\u05d0\u05d1\u05d2"("אבג")
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(text.indexOf('\u05d0'), currentOffset);

        // WordIterator reaches the beginning.
        currentOffset = wordIterator.prevBoundary(currentOffset);
        assertEquals(BreakIterator.DONE, currentOffset);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testGetPrevWordBeginningOnTwoWordsBoundary_out_of_boundary_too_small() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPrevWordBeginningOnTwoWordsBoundary(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPrevWordBeginningOnTwoWordsBoundary_out_of_boundary_too_big() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.length() + 1);
    }

    @Test
    public void testGetPrevWordBeginningOnTwoWordsBoundary_Empty_String() {
        final String text = "";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(BreakIterator.DONE, wordIterator.getPrevWordBeginningOnTwoWordsBoundary(0));
    }

    @Test
    public void testGetPrevWordBeginningOnTwoWordsBoundary() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(text.indexOf('a'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('a')));
        assertEquals(text.indexOf('a'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('c')));
        assertEquals(text.indexOf('d'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('d')));
        assertEquals(text.indexOf('d'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('f')));
        assertEquals(text.indexOf('d'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('-')));
        assertEquals(text.indexOf('g'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('g')));
        assertEquals(text.indexOf('g'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('.')));
    }

    @Test
    public void testGetPrevWordBeginningOnTwoWordsBoundary_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters -- "あアィイ"
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.JAPANESE);

        assertEquals(text.indexOf('\u3042'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u3042')));
        assertEquals(text.indexOf('\u3042'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u30A2')));
        assertEquals(text.indexOf('\u30A2'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\u30A4')));
        assertEquals(text.indexOf('\u30A2'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.length()));
    }

    @Test
    public void testGetPrevWordBeginningOnTwoWordsBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(text.indexOf('i'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('i')));
        assertEquals(text.indexOf('i'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('n')));
        assertEquals(text.indexOf('i'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('\'')));
        assertEquals(text.indexOf('i'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('t')));
        assertEquals(text.indexOf('i'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('t') + 1));
        assertEquals(text.indexOf('h'),
                wordIterator.getPrevWordBeginningOnTwoWordsBoundary(text.indexOf('h')));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextWordEndOnTwoWordBoundary_out_of_boundary_too_small() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getNextWordEndOnTwoWordBoundary(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetNextWordEndOnTwoWordBoundary_out_of_boundary_too_big() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getNextWordEndOnTwoWordBoundary(text.length() + 1);
    }

    @Test
    public void testGetNextWordEndOnTwoWordBoundary_Empty_String() {
        final String text = "";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(BreakIterator.DONE, wordIterator.getNextWordEndOnTwoWordBoundary(0));
    }

    @Test
    public void testGetNextWordEndOnTwoWordBoundary() {
        final String text = "abc def-ghi. jkl";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(text.indexOf(' '),
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('a')));
        assertEquals(text.indexOf(' '),
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('c')));
        assertEquals(text.indexOf('-'),
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('d')));
        assertEquals(text.indexOf('-'),
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('f')));
        assertEquals(text.indexOf('-'),
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('-')));
    }

    @Test
    public void testGetNextWordEndOnTwoWordBoundary_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters -- "あアィイ"
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.JAPANESE);

        assertEquals(text.indexOf('\u3042') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u3042')));
        assertEquals(text.indexOf('\u30A4') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A2')));
        assertEquals(text.indexOf('\u30A4') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A4')));
        assertEquals(text.indexOf('\u30A4') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\u30A4') + 1));
    }

    @Test
    public void testGetNextWordEndOnTwoWordBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(text.indexOf('t') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('i')));
        assertEquals(text.indexOf('t') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('n')));
        assertEquals(text.indexOf('t') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('\'')));
        assertEquals(text.indexOf('t') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('t')));
        assertEquals(text.indexOf('e') + 1,
                wordIterator.getNextWordEndOnTwoWordBoundary(text.indexOf('h')));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationBeginning_out_of_boundary_too_small() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationBeginning(-2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationBeginning_out_of_boundary_too_big() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationBeginning(text.length() + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationBeginning_DONE() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationBeginning(BreakIterator.DONE);
    }

    @Test
    public void testGetPunctuationBeginning() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(BreakIterator.DONE, wordIterator.getPunctuationBeginning(text.indexOf('a')));
        assertEquals(BreakIterator.DONE, wordIterator.getPunctuationBeginning(text.indexOf('c')));
        assertEquals(text.indexOf('!'), wordIterator.getPunctuationBeginning(text.indexOf('!')));
        assertEquals(text.indexOf('!'),
                wordIterator.getPunctuationBeginning(text.indexOf('?') + 1));
        assertEquals(text.indexOf(';'), wordIterator.getPunctuationBeginning(text.indexOf(';')));
        assertEquals(text.indexOf(';'), wordIterator.getPunctuationBeginning(text.indexOf(')')));
        assertEquals(text.indexOf(';'), wordIterator.getPunctuationBeginning(text.length()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationEnd_out_of_boundary_too_small() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationEnd(-2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationEnd_out_of_boundary_too_big() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationEnd(text.length() + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetPunctuationEnd_DONE() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        wordIterator.getPunctuationEnd(BreakIterator.DONE);
    }

    @Test
    public void testGetPunctuationEnd() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertEquals(text.indexOf('?') + 1, wordIterator.getPunctuationEnd(text.indexOf('a')));
        assertEquals(text.indexOf('?') + 1, wordIterator.getPunctuationEnd(text.indexOf('?') + 1));
        assertEquals(text.indexOf('(') + 1, wordIterator.getPunctuationEnd(text.indexOf('(')));
        assertEquals(text.indexOf(')') + 1, wordIterator.getPunctuationEnd(text.indexOf('(') + 2));
        assertEquals(text.indexOf(')') + 1, wordIterator.getPunctuationEnd(text.indexOf(')') + 1));
        assertEquals(BreakIterator.DONE, wordIterator.getPunctuationEnd(text.indexOf('d')));
        assertEquals(BreakIterator.DONE, wordIterator.getPunctuationEnd(text.length()));
    }

    @Test
    public void testIsAfterPunctuation() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertFalse(wordIterator.isAfterPunctuation(text.indexOf('a')));
        assertFalse(wordIterator.isAfterPunctuation(text.indexOf('!')));
        assertTrue(wordIterator.isAfterPunctuation(text.indexOf('?')));
        assertTrue(wordIterator.isAfterPunctuation(text.indexOf('?') + 1));
        assertFalse(wordIterator.isAfterPunctuation(text.indexOf('d')));

        assertFalse(wordIterator.isAfterPunctuation(BreakIterator.DONE));
        assertFalse(wordIterator.isAfterPunctuation(text.length() + 1));
    }

    @Test
    public void testIsOnPunctuation() {
        final String text = "abc!? (^^;) def";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        assertFalse(wordIterator.isOnPunctuation(text.indexOf('a')));
        assertTrue(wordIterator.isOnPunctuation(text.indexOf('!')));
        assertTrue(wordIterator.isOnPunctuation(text.indexOf('?')));
        assertFalse(wordIterator.isOnPunctuation(text.indexOf('?') + 1));
        assertTrue(wordIterator.isOnPunctuation(text.indexOf(')')));
        assertFalse(wordIterator.isOnPunctuation(text.indexOf(')') + 1));
        assertFalse(wordIterator.isOnPunctuation(text.indexOf('d')));

        assertFalse(wordIterator.isOnPunctuation(BreakIterator.DONE));
        assertFalse(wordIterator.isOnPunctuation(text.length()));
        assertFalse(wordIterator.isOnPunctuation(text.length() + 1));
    }

    @Test
    public void testOneWord() {
        final String text = "zen";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsWord(wordIterator, 0, 3);
    }

    @Test
    public void testSpacesOnly() {
        final String text = " ";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsNotWord(wordIterator, 0, 1);
    }

    @Test
    public void testCommaWithSpace() {
        final String text = ", ";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsNotWord(wordIterator, 0, 2);
    }

    @Test
    public void testSymbols() {
        final String text = ":-)";
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsNotWord(wordIterator, 0, 3);
    }

    @Test
    public void testBeginningEnd1() {
        final String text = "Well hello,   there! ";
        //                  0123456789012345678901
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsWord(wordIterator, 0, 4);
        verifyIsWord(wordIterator, 5, 10);
        verifyIsNotWord(wordIterator, 11, 13);
        verifyIsWord(wordIterator, 14, 19);
        verifyIsNotWord(wordIterator, 20, 21);
    }

    @Test
    public void testBeginningEnd2() {
        final String text = "  Another - sentence";
        //                  012345678901234567890
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsNotWord(wordIterator, 0, 1);
        verifyIsWord(wordIterator, 2, 9);
        verifyIsNotWord(wordIterator, 10, 11);
        verifyIsWord(wordIterator, 12, 20);
    }

    @Test
    public void testBeginningEnd3() {
        final String text = "This is \u0644\u0627 tested"; // Lama-aleph
        //                  012345678     9     01234567
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsWord(wordIterator, 0, 4);
        verifyIsWord(wordIterator, 5, 7);
        verifyIsWord(wordIterator, 8, 10);
        verifyIsWord(wordIterator, 11, 17);
    }

    @Test
    public void testSurrogate() {
        final String gothicBairkan = "\uD800\uDF31";
        final String text = "one " + gothicBairkan + "xxx word";
        //                  0123    45         678901234
        WordIterator wordIterator = new WordIterator(text, 0, text.length(), Locale.ENGLISH);

        verifyIsWord(wordIterator, 0, 3);
        verifyIsWordWithSurrogate(wordIterator, 4, 9, 5);
        verifyIsWord(wordIterator, 10, 14);
    }

    private void verifyIsWordWithSurrogate(WordIterator wordIterator, int beginning, int end,
            int surrogateIndex) {
        for (int i = beginning; i <= end; i++) {
            if (i == surrogateIndex) continue;
            assertEquals(beginning, wordIterator.getPrevWordBeginningOnTwoWordsBoundary(i));
            assertEquals(end, wordIterator.getNextWordEndOnTwoWordBoundary(i));
        }
    }

    private void verifyIsWord(WordIterator wordIterator, int beginning, int end) {
        verifyIsWordWithSurrogate(wordIterator, beginning, end, -1);
    }

    private void verifyIsNotWord(WordIterator wordIterator, int beginning, int end) {
        for (int i = beginning; i <= end; i++) {
            assertEquals(BreakIterator.DONE,
                    wordIterator.getPrevWordBeginningOnTwoWordsBoundary(i));
            assertEquals(BreakIterator.DONE, wordIterator.getNextWordEndOnTwoWordBoundary(i));
        }
    }
}
