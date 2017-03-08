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

import static android.support.text.emoji.util.EmojiMatcher.hasEmojiAt;
import static android.support.text.emoji.util.EmojiMatcher.hasEmojiCount;

import static junit.framework.TestCase.assertTrue;

import static org.junit.Assert.assertThat;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.text.emoji.test.R;
import android.support.text.emoji.util.TestString;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Reads raw/allemojis.txt which includes all the emojis known to human kind and tests that
 * EmojiCompat creates EmojiSpans for each one of them.
 */
@SmallTest
@RunWith(Parameterized.class)
public class AllEmojisTest {

    /**
     * String representation for a single emoji
     */
    private String mString;

    /**
     * Codepoints of emoji for better assert error message.
     */
    private String mCodepoints;

    @BeforeClass
    public static void setup() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final InputStream inputStream = context.getResources().openRawResource(R.raw.all_emojis);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final Collection<Object[]> data = new ArrayList<>();
        final StringBuilder stringBuilder = new StringBuilder();
        final StringBuilder codePointsBuilder = new StringBuilder();
        final int hexPrefixLength = "0x".length();

        try {
            String s;
            while ((s = reader.readLine()) != null) {
                stringBuilder.setLength(0);
                codePointsBuilder.setLength(0);

                // emoji codepoints are space separated: i.e. 0x1f1e6 0x1f1e8
                final String[] split = s.split(" ");

                for (int index = 0; index < split.length; index++) {
                    final String part = split[index];
                    final String substring = part.substring(hexPrefixLength, part.length());
                    codePointsBuilder.append(substring);
                    codePointsBuilder.append(",");
                    stringBuilder.append(Character.toChars(Integer.parseInt(substring, 16)));
                }
                data.add(new Object[]{stringBuilder.toString(), codePointsBuilder.toString()});
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public AllEmojisTest(String string, String codepoints) {
        mString = string;
        mCodepoints = codepoints;
    }

    @Test
    public void testEmoji() {
        assertTrue("EmojiCompat should have emoji: " + mCodepoints,
                EmojiCompat.get().hasEmojiGlyph(mString));
        assertEmojiCompatAddsEmoji(mString);
    }

    private void assertEmojiCompatAddsEmoji(final String str) {
        TestString string = new TestString(str);
        CharSequence sequence = EmojiCompat.get().process(string.toString());
        assertThat(sequence, hasEmojiCount(1));
        assertThat(sequence, hasEmojiAt(string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is in the middle of string
        string = new TestString(str).withPrefix().withSuffix();
        sequence = EmojiCompat.get().process(string.toString());
        assertThat(sequence, hasEmojiCount(1));
        assertThat(sequence, hasEmojiAt(string.emojiStartIndex(), string.emojiEndIndex()));

        // case where Emoji is at the end of string
        string = new TestString(str).withSuffix();
        sequence = EmojiCompat.get().process(string.toString());
        assertThat(sequence, hasEmojiCount(1));
        assertThat(sequence, hasEmojiAt(string.emojiStartIndex(), string.emojiEndIndex()));
    }

}
