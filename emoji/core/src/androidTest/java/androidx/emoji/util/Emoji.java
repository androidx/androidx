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

package androidx.emoji.util;

import androidx.annotation.NonNull;

public class Emoji {

    public static final int CHAR_KEYCAP = 0x20E3;
    public static final int CHAR_DIGIT = 0x0039;
    public static final int CHAR_ZWJ = 0x200D;
    public static final int CHAR_VS_EMOJI = 0xFE0f;
    public static final int CHAR_VS_TEXT = 0xFE0E;
    public static final int CHAR_FITZPATRICK = 0x1F3FE;
    public static final int CHAR_FITZPATRICK_TYPE_1 = 0x1F3fB;
    public static final int CHAR_DEFAULT_TEXT_STYLE = 0x26F9;
    public static final int CHAR_DEFAULT_EMOJI_STYLE = 0x1f3A2;
    public static final int CHAR_FEMALE_SIGN = 0x2640;
    public static final int CHAR_MAN = 0x1F468;
    public static final int CHAR_HEART = 0x2764;
    public static final int CHAR_KISS = 0x1F48B;
    public static final int CHAR_REGIONAL_SYMBOL = 0x1F1E8;
    public static final int CHAR_ASTERISK = 0x002A;

    public static final EmojiMapping EMOJI_SINGLE_CODEPOINT = new EmojiMapping(
            new int[]{CHAR_DEFAULT_EMOJI_STYLE}, 0xF01B4);

    public static final EmojiMapping EMOJI_WITH_ZWJ = new EmojiMapping(
            new int[]{CHAR_MAN, CHAR_ZWJ, CHAR_HEART, CHAR_VS_EMOJI, CHAR_ZWJ, CHAR_KISS, CHAR_ZWJ,
                    CHAR_MAN}, 0xF051F);

    public static final EmojiMapping EMOJI_GENDER = new EmojiMapping(new int[]{
            CHAR_DEFAULT_TEXT_STYLE, CHAR_VS_EMOJI, CHAR_ZWJ, CHAR_FEMALE_SIGN}, 0xF0950);

    public static final EmojiMapping EMOJI_FLAG = new EmojiMapping(
            new int[]{CHAR_REGIONAL_SYMBOL, CHAR_REGIONAL_SYMBOL}, 0xF03A0);

    public static final EmojiMapping EMOJI_GENDER_WITHOUT_VS = new EmojiMapping(
            new int[]{CHAR_DEFAULT_TEXT_STYLE, CHAR_ZWJ, CHAR_FEMALE_SIGN}, 0xF0950);

    public static final EmojiMapping DEFAULT_TEXT_STYLE = new EmojiMapping(
            new int[]{CHAR_DEFAULT_TEXT_STYLE, CHAR_VS_EMOJI}, 0xF04C6);

    public static final EmojiMapping EMOJI_REGIONAL_SYMBOL = new EmojiMapping(
            new int[]{CHAR_REGIONAL_SYMBOL}, 0xF0025);

    public static final EmojiMapping EMOJI_UNKNOWN_FLAG = new EmojiMapping(
            new int[]{0x1F1FA, 0x1F1F3}, 0xF0599);

    public static final EmojiMapping EMOJI_DIGIT_ES = new EmojiMapping(
            new int[]{CHAR_DIGIT, CHAR_VS_EMOJI}, 0xF0340);

    public static final EmojiMapping EMOJI_DIGIT_KEYCAP = new EmojiMapping(
            new int[]{CHAR_DIGIT, CHAR_KEYCAP}, 0xF0377);

    public static final EmojiMapping EMOJI_DIGIT_ES_KEYCAP = new EmojiMapping(
            new int[]{CHAR_DIGIT, CHAR_VS_EMOJI, CHAR_KEYCAP}, 0xF0377);

    public static final EmojiMapping EMOJI_ASTERISK_KEYCAP = new EmojiMapping(
            new int[]{CHAR_ASTERISK, CHAR_VS_EMOJI, CHAR_KEYCAP}, 0xF051D);

    public static final EmojiMapping EMOJI_SKIN_MODIFIER = new EmojiMapping(
            new int[]{CHAR_MAN, CHAR_FITZPATRICK}, 0xF0603);

    public static final EmojiMapping EMOJI_SKIN_MODIFIER_TYPE_ONE = new EmojiMapping(
            new int[]{CHAR_MAN, CHAR_FITZPATRICK_TYPE_1}, 0xF0606);

    public static final EmojiMapping EMOJI_SKIN_MODIFIER_WITH_VS = new EmojiMapping(
            new int[]{CHAR_MAN, CHAR_VS_EMOJI, CHAR_FITZPATRICK_TYPE_1}, 0xF0606);

    public static class EmojiMapping {
        private final int[] mCodepoints;
        private final int mId;

        private EmojiMapping(@NonNull final int[] codepoints, final int id) {
            mCodepoints = codepoints;
            mId = id;
        }

        public final int[] codepoints() {
            return mCodepoints;
        }

        public final int id() {
            return mId;
        }

        public final int charCount() {
            int count = 0;
            for (int i = 0; i < mCodepoints.length; i++) {
                count += Character.charCount(mCodepoints[i]);
            }
            return count;
        }
    }
}
