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


import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.BreakIterator;
import java.util.Locale;

@SmallTest
@RunWith(JUnit4.class)
public class WordBoundaryTest {
    @Test(expected = IllegalArgumentException.class)
    public void testGetWordStart_out_of_boundary_too_small() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordStart(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWordStart_out_of_boundary_too_big() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordStart(text.length() + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWordStart_DONE() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordStart(BreakIterator.DONE);
    }

    @Test
    public void testGetWordStart_Empty_String() {
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, "");

        assertEquals(0, wordBoundary.getWordStart(0));
    }

    @Test
    public void testGetWordStart() {
        final String text = "abc def-ghi. jkl";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf('a'), wordBoundary.getWordStart(text.indexOf('a')));
        assertEquals(text.indexOf('a'), wordBoundary.getWordStart(text.indexOf('c')));
        assertEquals(text.indexOf('a'), wordBoundary.getWordStart(text.indexOf(' ')));
        assertEquals(text.indexOf('d'), wordBoundary.getWordStart(text.indexOf('d')));
        assertEquals(text.indexOf('g'), wordBoundary.getWordStart(text.indexOf('i')));
        assertEquals(text.indexOf('j'), wordBoundary.getWordStart(text.indexOf('k')));
    }

    @Test
    public void testGetWordStart_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordBoundary wordBoundary = new WordBoundary(new Locale("he", "IL"), text);

        assertEquals(text.indexOf('\u05d0'), wordBoundary.getWordStart(text.indexOf('\u05d0')));
        assertEquals(text.indexOf('\u05d0'), wordBoundary.getWordStart(text.indexOf('\u05d2')));
        assertEquals(text.indexOf('\u05d0'), wordBoundary.getWordStart(text.indexOf(' ')));
        assertEquals(text.indexOf('\u05d3'), wordBoundary.getWordStart(text.indexOf('\u05d4')));
        assertEquals(text.indexOf('\u05d3'), wordBoundary.getWordStart(text.indexOf('-')));
        assertEquals(text.indexOf('-'), wordBoundary.getWordStart(text.indexOf('\u05d5')));
        assertEquals(text.indexOf('\u05d5'), wordBoundary.getWordStart(text.indexOf('\u05d6')));
        assertEquals(text.indexOf('\u05d7'), wordBoundary.getWordStart(text.indexOf('\u05d7')));
    }

    @Test
    public void testGetWordStart_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordBoundary wordBoundary = new WordBoundary(Locale.JAPANESE, text);

        assertEquals(text.indexOf('\u3042'), wordBoundary.getWordStart(text.indexOf('\u3042')));
        assertEquals(text.indexOf('\u3042'), wordBoundary.getWordStart(text.indexOf('\u30A2')));
        assertEquals(text.indexOf('\u30A2'), wordBoundary.getWordStart(text.indexOf('\u30A4')));
        assertEquals(text.indexOf('\u30A2'), wordBoundary.getWordStart(text.length()));
    }

    @Test
    public void testGetWordStart_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf('i'), wordBoundary.getWordStart(text.indexOf('i')));
        assertEquals(text.indexOf('i'), wordBoundary.getWordStart(text.indexOf('n')));
        assertEquals(text.indexOf('i'), wordBoundary.getWordStart(text.indexOf('\'')));
        assertEquals(text.indexOf('i'), wordBoundary.getWordStart(text.indexOf('t')));
        assertEquals(text.indexOf('i'), wordBoundary.getWordStart(text.indexOf('t') + 1));
        assertEquals(text.indexOf('h'), wordBoundary.getWordStart(text.indexOf('h')));
    }

    @Test
    public void testGetWordStart_isOnPunctuation() {
        final String text = "abc!? (^^;) def";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf('a'), wordBoundary.getWordStart(text.indexOf('!')));
        assertEquals(text.indexOf('!'), wordBoundary.getWordStart(text.indexOf('?') + 1));
        assertEquals(text.indexOf(';'), wordBoundary.getWordStart(text.indexOf(';')));
        assertEquals(text.indexOf(';'), wordBoundary.getWordStart(text.indexOf(')')));
        assertEquals(text.indexOf('d'), wordBoundary.getWordStart(text.length()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWordEnd_out_of_boundary_too_small() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordEnd(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWordEnd_out_of_boundary_too_big() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordEnd(text.length() + 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWordEnd_DONE() {
        final String text = "text";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        wordBoundary.getWordEnd(BreakIterator.DONE);
    }

    @Test
    public void testGetWordEnd_Empty_String() {
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, "");

        assertEquals(0, wordBoundary.getWordEnd(0));
    }

    @Test
    public void testGetWordEnd() {
        final String text = "abc def-ghi. jkl";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf('a')));
        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf('c')));
        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf(' ')));
        assertEquals(text.indexOf('-'), wordBoundary.getWordEnd(text.indexOf('d')));
        assertEquals(text.indexOf('.'), wordBoundary.getWordEnd(text.indexOf('i')));
        assertEquals(text.indexOf('l') + 1, wordBoundary.getWordEnd(text.indexOf('k')));
    }

    @Test
    public void testGetWordEnd_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordBoundary wordBoundary = new WordBoundary(new Locale("he", "IL"), text);

        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf('\u05d0')));
        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf('\u05d2')));
        assertEquals(text.indexOf(' '), wordBoundary.getWordEnd(text.indexOf(' ')));
        assertEquals(text.indexOf('-'), wordBoundary.getWordEnd(text.indexOf('\u05d4')));
        assertEquals(text.indexOf('-') + 1, wordBoundary.getWordEnd(text.indexOf('-')));
        assertEquals(text.indexOf('.'), wordBoundary.getWordEnd(text.indexOf('\u05d5')));
        assertEquals(text.indexOf('.'), wordBoundary.getWordEnd(text.indexOf('\u05d6')));
        assertEquals(text.length(), wordBoundary.getWordEnd(text.indexOf('\u05d7')));
    }

    @Test
    public void testGetWordEnd_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordBoundary wordBoundary = new WordBoundary(Locale.JAPANESE, text);

        assertEquals(text.indexOf('\u3042') + 1, wordBoundary.getWordEnd(text.indexOf('\u3042')));
        assertEquals(text.indexOf('\u30A4') + 1, wordBoundary.getWordEnd(text.indexOf('\u30A2')));
        assertEquals(text.indexOf('\u30A4') + 1, wordBoundary.getWordEnd(text.indexOf('\u30A4')));
        assertEquals(text.indexOf('\u30A4') + 1, wordBoundary.getWordEnd(text.length()));
    }

    @Test
    public void testGetWordEnd_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf('t') + 1, wordBoundary.getWordEnd(text.indexOf('i')));
        assertEquals(text.indexOf('t') + 1, wordBoundary.getWordEnd(text.indexOf('n')));
        assertEquals(text.indexOf('t') + 1, wordBoundary.getWordEnd(text.indexOf('\'')));
        assertEquals(text.indexOf('t') + 1, wordBoundary.getWordEnd(text.indexOf('t')));
        assertEquals(text.indexOf('e') + 1, wordBoundary.getWordEnd(text.indexOf('h')));
    }

    @Test
    public void testGetWordEnd_isOnPunctuation() {
        final String text = "abc!? (^^;) def";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertEquals(text.indexOf('!'), wordBoundary.getWordEnd(text.indexOf('a')));
        assertEquals(text.indexOf('?') + 1, wordBoundary.getWordEnd(text.indexOf('?') + 1));
        assertEquals(text.indexOf('(') + 1, wordBoundary.getWordEnd(text.indexOf('(')));
        assertEquals(text.indexOf('(') + 2, wordBoundary.getWordEnd(text.indexOf('(') + 2));
        assertEquals(text.indexOf(')') + 1, wordBoundary.getWordEnd(text.indexOf(')') + 1));
        assertEquals(text.length(), wordBoundary.getWordEnd(text.indexOf('d')));
        assertEquals(text.length(), wordBoundary.getWordEnd(text.length()));
    }
}
