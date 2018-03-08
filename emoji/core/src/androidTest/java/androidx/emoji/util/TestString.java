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

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class used to create strings with emojis during tests.
 */
public class TestString {

    private static final List<Integer> EMPTY_LIST = new ArrayList<>();

    private static final String EXTRA = "ab";
    private final List<Integer> mCodePoints;
    private String mString;
    private final String mValue;
    private boolean mHasSuffix;
    private boolean mHasPrefix;

    public TestString(int... codePoints) {
        if (codePoints.length == 0) {
            mCodePoints = EMPTY_LIST;
        } else {
            mCodePoints = new ArrayList<>();
            append(codePoints);
        }
        mValue = null;
    }

    public TestString(Emoji.EmojiMapping emojiMapping) {
        this(emojiMapping.codepoints());
    }

    public TestString(String string) {
        mCodePoints = EMPTY_LIST;
        mValue = string;
    }

    public TestString append(int... codePoints) {
        for (int i = 0; i < codePoints.length; i++) {
            mCodePoints.add(codePoints[i]);
        }
        return this;
    }

    public TestString prepend(int... codePoints) {
        for (int i = codePoints.length - 1; i >= 0; i--) {
            mCodePoints.add(0, codePoints[i]);
        }
        return this;
    }

    public TestString append(Emoji.EmojiMapping emojiMapping) {
        return append(emojiMapping.codepoints());
    }

    public TestString withSuffix() {
        mHasSuffix = true;
        return this;
    }

    public TestString withPrefix() {
        mHasPrefix = true;
        return this;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (mHasPrefix) {
            builder.append(EXTRA);
        }

        for (int index = 0; index < mCodePoints.size(); index++) {
            builder.append(Character.toChars(mCodePoints.get(index)));
        }

        if (mValue != null) {
            builder.append(mValue);
        }

        if (mHasSuffix) {
            builder.append(EXTRA);
        }
        mString = builder.toString();
        return mString;
    }

    public int emojiStartIndex() {
        if (mHasPrefix) return EXTRA.length();
        return 0;
    }

    public int emojiEndIndex() {
        if (mHasSuffix) return mString.lastIndexOf(EXTRA);
        return mString.length();
    }
}
