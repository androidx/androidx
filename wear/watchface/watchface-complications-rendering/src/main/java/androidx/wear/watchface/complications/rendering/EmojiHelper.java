/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.complications.rendering;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Class to detect and replace emoji in CharSequences.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class EmojiHelper {

    private EmojiHelper() {}

    // Extracted from android.text.Emoji
    private static final int[][] EMOJI_RANGES = {
        {0x00a9, 0x00a9},
        {0x203c, 0x203c},
        {0x2049, 0x2049},
        {0x2122, 0x2122},
        {0x2139, 0x2139},
        {0x2194, 0x21aa},
        {0x231a, 0x2328},
        {0x23cf, 0x23fa},
        {0x24c2, 0x24c2},
        {0x25aa, 0x27bf},
        {0x2934, 0x2935},
        {0x2b05, 0x2b55},
        {0x3030, 0x3030},
        {0x303d, 0x303d},
        {0x3297, 0x3299},
        {0x1f004, 0x1f004},
        {0x1f0cf, 0x1f0cf},
        {0x1f170, 0x1f251},
        {0x1f300, 0x1f6f6},
        {0x1f910, 0x1f9c0}
    };

    /** Returns true if a character is an emoji. */
    static boolean isEmoji(int charCode) {
        for (int[] range : EMOJI_RANGES) {
            if (charCode >= range[0] && charCode <= range[1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a String with all emoji replaced with the given character. This method will remove
     * consequent emoji.
     */
    @Nullable
    static String replaceEmoji(@Nullable CharSequence text, int newCharacter) {
        if (text == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder(text.length());
        final int length = text.length();
        boolean isPreviousCharacterEmoji = false;
        for (int offset = 0; offset < length; ) {
            final int codePoint = Character.codePointAt(text, offset);
            if (!isEmoji(codePoint)) {
                buffer.appendCodePoint(codePoint);
            } else if (!isPreviousCharacterEmoji) {
                buffer.appendCodePoint(newCharacter);
            }
            isPreviousCharacterEmoji = isEmoji(codePoint);
            offset += Character.charCount(codePoint);
        }
        return buffer.toString();
    }
}
