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


import static com.google.common.truth.Truth.assertThat;

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

        assertThat(wordBoundary.getWordStart(0)).isEqualTo(0);
    }

    @Test
    public void testGetWordStart() {
        final String text = "abc def-ghi. jkl";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordStart(text.indexOf('a'))).isEqualTo(text.indexOf('a'));
        assertThat(wordBoundary.getWordStart(text.indexOf('c'))).isEqualTo(text.indexOf('a'));
        assertThat(wordBoundary.getWordStart(text.indexOf(' '))).isEqualTo(text.indexOf('a'));
        assertThat(wordBoundary.getWordStart(text.indexOf('d'))).isEqualTo(text.indexOf('d'));
        assertThat(wordBoundary.getWordStart(text.indexOf('i'))).isEqualTo(text.indexOf('g'));
        assertThat(wordBoundary.getWordStart(text.indexOf('k'))).isEqualTo(text.indexOf('j'));
    }

    @Test
    public void testGetWordStart_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordBoundary wordBoundary = new WordBoundary(new Locale("he", "IL"), text);

        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d0')))
                .isEqualTo(text.indexOf('\u05d0'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d2')))
                .isEqualTo(text.indexOf('\u05d0'));
        assertThat(wordBoundary.getWordStart(text.indexOf(' ')))
                .isEqualTo(text.indexOf('\u05d0'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d4')))
                .isEqualTo(text.indexOf('\u05d3'));
        assertThat(wordBoundary.getWordStart(text.indexOf('-')))
                .isEqualTo(text.indexOf('\u05d3'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d5')))
                .isEqualTo(text.indexOf('-'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d6')))
                .isEqualTo(text.indexOf('\u05d5'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u05d7')))
                .isEqualTo(text.indexOf('\u05d7'));
    }

    @Test
    public void testGetWordStart_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordBoundary wordBoundary = new WordBoundary(Locale.JAPANESE, text);

        assertThat(wordBoundary.getWordStart(text.indexOf('\u3042')))
                .isEqualTo(text.indexOf('\u3042'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u30A2')))
                .isEqualTo(text.indexOf('\u3042'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\u30A4')))
                .isEqualTo(text.indexOf('\u30A2'));
        assertThat(wordBoundary.getWordStart(text.length()))
                .isEqualTo(text.indexOf('\u30A2'));
    }

    @Test
    public void testGetWordStart_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordStart(text.indexOf('i'))).isEqualTo(text.indexOf('i'));
        assertThat(wordBoundary.getWordStart(text.indexOf('n'))).isEqualTo(text.indexOf('i'));
        assertThat(wordBoundary.getWordStart(text.indexOf('\''))).isEqualTo(text.indexOf('i'));
        assertThat(wordBoundary.getWordStart(text.indexOf('t'))).isEqualTo(text.indexOf('i'));
        assertThat(wordBoundary.getWordStart(text.indexOf('t') + 1)).isEqualTo(text.indexOf('i'));
        assertThat(wordBoundary.getWordStart(text.indexOf('h'))).isEqualTo(text.indexOf('h'));
    }

    @Test
    public void testGetWordStart_isOnPunctuation() {
        final String text = "abc!? (^^;) def";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordStart(text.indexOf('!'))).isEqualTo(text.indexOf('a'));
        assertThat(wordBoundary.getWordStart(text.indexOf('?') + 1)).isEqualTo(text.indexOf('!'));
        assertThat(wordBoundary.getWordStart(text.indexOf(';'))).isEqualTo(text.indexOf(';'));
        assertThat(wordBoundary.getWordStart(text.indexOf(')'))).isEqualTo(text.indexOf(';'));
        assertThat(wordBoundary.getWordStart(text.length())).isEqualTo(text.indexOf('d'));
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

        assertThat(wordBoundary.getWordEnd(0)).isEqualTo(0);
    }

    @Test
    public void testGetWordEnd() {
        final String text = "abc def-ghi. jkl";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordEnd(text.indexOf('a'))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf('c'))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf(' '))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf('d'))).isEqualTo(text.indexOf('-'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('i'))).isEqualTo(text.indexOf('.'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('k'))).isEqualTo(text.indexOf('l') + 1);
    }

    @Test
    public void testGetWordEnd_RTL() {
        // Hebrew -- "אבג דה-וז. חט"
        final String text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8";
        WordBoundary wordBoundary = new WordBoundary(new Locale("he", "IL"), text);

        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d0'))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d2'))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf(' '))).isEqualTo(text.indexOf(' '));
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d4'))).isEqualTo(text.indexOf('-'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('-'))).isEqualTo(text.indexOf('-') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d5'))).isEqualTo(text.indexOf('.'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d6'))).isEqualTo(text.indexOf('.'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u05d7'))).isEqualTo(text.length());
    }

    @Test
    public void testGetWordEnd_CJK() {
        // Japanese HIRAGANA letter + KATAKANA letters
        final String text = "\u3042\u30A2\u30A3\u30A4";
        WordBoundary wordBoundary = new WordBoundary(Locale.JAPANESE, text);

        assertThat(wordBoundary.getWordEnd(text.indexOf('\u3042')))
                .isEqualTo(text.indexOf('\u3042') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u30A2')))
                .isEqualTo(text.indexOf('\u30A4') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('\u30A4')))
                .isEqualTo(text.indexOf('\u30A4') + 1);
        assertThat(wordBoundary.getWordEnd(text.length()))
                .isEqualTo(text.indexOf('\u30A4') + 1);
    }

    @Test
    public void testGetWordEnd_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        final String text = "isn't he";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordEnd(text.indexOf('i'))).isEqualTo(text.indexOf('t') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('n'))).isEqualTo(text.indexOf('t') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('\''))).isEqualTo(text.indexOf('t') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('t'))).isEqualTo(text.indexOf('t') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('h'))).isEqualTo(text.indexOf('e') + 1);
    }

    @Test
    public void testGetWordEnd_isOnPunctuation() {
        final String text = "abc!? (^^;) def";
        WordBoundary wordBoundary = new WordBoundary(Locale.ENGLISH, text);

        assertThat(wordBoundary.getWordEnd(text.indexOf('a'))).isEqualTo(text.indexOf('!'));
        assertThat(wordBoundary.getWordEnd(text.indexOf('?') + 1)).isEqualTo(text.indexOf('?') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('('))).isEqualTo(text.indexOf('(') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('(') + 2)).isEqualTo(text.indexOf('(') + 2);
        assertThat(wordBoundary.getWordEnd(text.indexOf(')') + 1)).isEqualTo(text.indexOf(')') + 1);
        assertThat(wordBoundary.getWordEnd(text.indexOf('d'))).isEqualTo(text.length());
        assertThat(wordBoundary.getWordEnd(text.length())).isEqualTo(text.length());
    }
}
