/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.emoji2.bundled;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.emoji2.text.EmojiCompat;
import androidx.emoji2.text.MetadataRepo;
import androidx.emoji2.text.flatbuffer.MetadataItem;
import androidx.emoji2.text.flatbuffer.MetadataList;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExhaustiveEmojiStateTest {

    @BeforeClass
    public static void setup() {
        EmojiCompat.init(TestConfigBuilder.config());
    }

    @Test
    public void testAllEmojiPermutations() throws Exception {
        Object helper = getField(EmojiCompat.get(), "mHelper");
        Object repoObj = getField(helper, "mMetadataRepo");
        MetadataRepo repo = (MetadataRepo) repoObj;
        MetadataList metadataList = repo.getMetadataList();

        int length = metadataList.listLength();
        MetadataItem item = new MetadataItem();

        String vsText = "\uFE0E";       // Text variation selector
        String vsEmoji = "\uFE0F";      // Emoji variation selector
        String zwj = "\u200D";          // Zero Width Joiner
        String digit = "5";             // Keycap candidate prefix
        String nonCandidateSp = new String(Character.toChars(0x20000)); // Plane 2 non-candidate
        String regionalIndicator = new String(Character.toChars(0x1F1E8)); // Regional Indicator

        for (int i = 0; i < length; i++) {
            metadataList.list(item, i);
            int cpLen = item.codepointsLength();
            if (cpLen == 0) continue;

            int[] codepoints = new int[cpLen];
            for (int j = 0; j < cpLen; j++) {
                codepoints[j] = item.codepoints(j);
            }
            String emojiStr = new String(codepoints, 0, cpLen);

            // 1. Bare emoji
            CharSequence p1 = EmojiCompat.get().process(emojiStr);
            assertNotNull(p1);

            // 2. Text Variation Selector (forces text rendering / bypass)
            CharSequence p2 = EmojiCompat.get().process(emojiStr + vsText);
            assertNotNull(p2);

            // 3. Emoji Variation Selector
            CharSequence p3 = EmojiCompat.get().process(emojiStr + vsEmoji);
            assertNotNull(p3);

            // 4. ASCII prefix & postfix
            CharSequence p4 = EmojiCompat.get().process("abc" + emojiStr + "xyz");
            assertNotNull(p4);

            // 5. Digit prefix (Keycap state machine candidate)
            CharSequence p5 = EmojiCompat.get().process(digit + emojiStr);
            assertNotNull(p5);

            // 6. ZWJ prefix & postfix (ZWJ sequence state transitions)
            CharSequence p6 = EmojiCompat.get().process(zwj + emojiStr + zwj);
            assertNotNull(p6);

            // 7. Non-candidate surrogate pair prefix/postfix
            CharSequence p7 = EmojiCompat.get().process(nonCandidateSp + emojiStr + nonCandidateSp);
            assertNotNull(p7);

            // 8. Regional Indicator prefix (Flag state transitions)
            CharSequence p8 = EmojiCompat.get().process(regionalIndicator + emojiStr);
            assertNotNull(p8);

            // 9. Test maxEmojiCount limit = 1 on double emoji string
            CharSequence p9 = EmojiCompat.get().process(emojiStr + " " + emojiStr, 0,
                    (emojiStr + " " + emojiStr).length(), 1);
            assertNotNull(p9);

            // 10. Test getEmojiMatch / ACTION_TYPE_LOOKUP
            int match = EmojiCompat.get().getEmojiMatch(emojiStr, 0);
            assertTrue(match == EmojiCompat.EMOJI_SUPPORTED
                    || match == EmojiCompat.EMOJI_UNSUPPORTED);
        }
    }

    private static Object getField(Object obj, String fieldName) throws Exception {
        java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }
}
