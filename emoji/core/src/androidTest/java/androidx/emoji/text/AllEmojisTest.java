/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.emoji.text;

import static androidx.emoji.util.EmojiMatcher.hasEmojiAt;
import static androidx.emoji.util.EmojiMatcher.hasEmojiCount;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Paint;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.text.Spanned;

import androidx.core.graphics.PaintCompat;
import androidx.emoji.util.TestString;

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
@SdkSuppress(minSdkVersion = 19)
public class AllEmojisTest {

    /**
     * String representation for a single emoji
     */
    private String mString;

    /**
     * Codepoints of emoji for better assert error message.
     */
    private String mCodepoints;

    /**
     * Paint object used to check if Typeface can render the given emoji.
     */
    private Paint mPaint;

    @BeforeClass
    public static void setup() {
        EmojiCompat.reset(TestConfigBuilder.config());
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        final Context context = InstrumentationRegistry.getTargetContext();
        final InputStream inputStream = context.getAssets().open("emojis.txt");
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final Collection<Object[]> data = new ArrayList<>();
            final StringBuilder stringBuilder = new StringBuilder();
            final StringBuilder codePointsBuilder = new StringBuilder();

            String s;
            while ((s = reader.readLine()) != null) {
                s = s.trim();
                // pass comments
                if (s.isEmpty() || s.startsWith("#")) continue;

                stringBuilder.setLength(0);
                codePointsBuilder.setLength(0);

                // emoji codepoints are space separated: i.e. 0x1f1e6 0x1f1e8
                final String[] split = s.split(" ");

                for (int index = 0; index < split.length; index++) {
                    final String part = split[index].trim();
                    codePointsBuilder.append(part);
                    codePointsBuilder.append(",");
                    stringBuilder.append(Character.toChars(Integer.parseInt(part, 16)));
                }
                data.add(new Object[]{stringBuilder.toString(), codePointsBuilder.toString()});
            }

            return data;
        } finally {
            inputStream.close();
        }

    }

    public AllEmojisTest(String string, String codepoints) {
        mString = string;
        mCodepoints = codepoints;
        mPaint = new Paint();
    }

    @Test
    public void testEmoji() {
        assertTrue("EmojiCompat should have emoji: " + mCodepoints,
                EmojiCompat.get().hasEmojiGlyph(mString));
        assertEmojiCompatAddsEmoji(mString);
        assertSpanCanRenderEmoji(mString);
    }

    private void assertSpanCanRenderEmoji(final String str) {
        final Spanned spanned = (Spanned) EmojiCompat.get().process(new TestString(str).toString());
        final EmojiSpan[] spans = spanned.getSpans(0, spanned.length(), EmojiSpan.class);
        final EmojiMetadata metadata = spans[0].getMetadata();
        mPaint.setTypeface(metadata.getTypeface());

        final String codepoint = String.valueOf(Character.toChars(metadata.getId()));
        assertTrue(metadata.toString() + " should be rendered",
                PaintCompat.hasGlyph(mPaint, codepoint));
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
