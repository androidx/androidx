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

import static org.mockito.Matchers.argThat;

import android.text.Spanned;
import android.text.TextUtils;

import androidx.emoji.text.EmojiSpan;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.ArgumentMatcher;

/**
 * Utility class that includes matchers specific to emojis and EmojiSpans.
 */
public class EmojiMatcher {

    public static Matcher<CharSequence> hasEmojiAt(final int id, final int start,
            final int end) {
        return new EmojiResourceMatcher(id, start, end);
    }

    public static Matcher<CharSequence> hasEmojiAt(final Emoji.EmojiMapping emojiMapping,
            final int start, final int end) {
        return new EmojiResourceMatcher(emojiMapping.id(), start, end);
    }

    public static Matcher<CharSequence> hasEmojiAt(final int start, final int end) {
        return new EmojiResourceMatcher(-1, start, end);
    }

    public static Matcher<CharSequence> hasEmoji(final int id) {
        return new EmojiResourceMatcher(id, -1, -1);
    }

    public static Matcher<CharSequence> hasEmoji(final Emoji.EmojiMapping emojiMapping) {
        return new EmojiResourceMatcher(emojiMapping.id(), -1, -1);
    }

    public static Matcher<CharSequence> hasEmoji() {
        return new EmojiSpanMatcher();
    }

    public static Matcher<CharSequence> hasEmojiCount(final int count) {
        return new EmojiCountMatcher(count);
    }

    public static <T extends CharSequence> T sameCharSequence(final T expected) {
        return argThat(new ArgumentMatcher<T>() {
            @Override
            public boolean matches(T o) {
                if (o instanceof CharSequence) {
                    return TextUtils.equals(expected, o);
                }
                return false;
            }

            @Override
            public String toString() {
                return "doesn't match " + expected;
            }
        });
    }

    private static class EmojiSpanMatcher extends TypeSafeMatcher<CharSequence> {

        private EmojiSpan[] mSpans;

        EmojiSpanMatcher() {
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("should have EmojiSpans");
        }

        @Override
        protected void describeMismatchSafely(final CharSequence charSequence,
                Description mismatchDescription) {
            mismatchDescription.appendText(" has no EmojiSpans");
        }

        @Override
        protected boolean matchesSafely(final CharSequence charSequence) {
            if (charSequence == null) return false;
            if (!(charSequence instanceof Spanned)) return false;
            mSpans = ((Spanned) charSequence).getSpans(0, charSequence.length(), EmojiSpan.class);
            return mSpans.length != 0;
        }
    }

    private static class EmojiCountMatcher extends TypeSafeMatcher<CharSequence> {

        private final int mCount;
        private EmojiSpan[] mSpans;

        EmojiCountMatcher(final int count) {
            mCount = count;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("should have ").appendValue(mCount).appendText(" EmojiSpans");
        }

        @Override
        protected void describeMismatchSafely(final CharSequence charSequence,
                Description mismatchDescription) {
            mismatchDescription.appendText(" has ");
            if (mSpans == null) {
                mismatchDescription.appendValue("no");
            } else {
                mismatchDescription.appendValue(mSpans.length);
            }

            mismatchDescription.appendText(" EmojiSpans");
        }

        @Override
        protected boolean matchesSafely(final CharSequence charSequence) {
            if (charSequence == null) return false;
            if (!(charSequence instanceof Spanned)) return false;
            mSpans = ((Spanned) charSequence).getSpans(0, charSequence.length(), EmojiSpan.class);
            return mSpans.length == mCount;
        }
    }

    private static class EmojiResourceMatcher extends TypeSafeMatcher<CharSequence> {
        private static final int ERR_NONE = 0;
        private static final int ERR_SPANNABLE_NULL = 1;
        private static final int ERR_NO_SPANS = 2;
        private static final int ERR_WRONG_INDEX = 3;
        private final int mResId;
        private final int mStart;
        private final int mEnd;
        private int mError = ERR_NONE;
        private int mActualStart = -1;
        private int mActualEnd = -1;

        EmojiResourceMatcher(int resId, int start, int end) {
            mResId = resId;
            mStart = start;
            mEnd = end;
        }

        @Override
        public void describeTo(final Description description) {
            if (mResId == -1) {
                description.appendText("should have EmojiSpan at ")
                        .appendValue("[" + mStart + "," + mEnd + "]");
            } else if (mStart == -1 && mEnd == -1) {
                description.appendText("should have EmojiSpan with resource id ")
                        .appendValue(Integer.toHexString(mResId));
            } else {
                description.appendText("should have EmojiSpan with resource id ")
                        .appendValue(Integer.toHexString(mResId))
                        .appendText(" at ")
                        .appendValue("[" + mStart + "," + mEnd + "]");
            }
        }

        @Override
        protected void describeMismatchSafely(final CharSequence charSequence,
                Description mismatchDescription) {
            int offset = 0;
            mismatchDescription.appendText("[");
            while (offset < charSequence.length()) {
                int codepoint = Character.codePointAt(charSequence, offset);
                mismatchDescription.appendText(Integer.toHexString(codepoint));
                offset += Character.charCount(codepoint);
                if (offset < charSequence.length()) {
                    mismatchDescription.appendText(",");
                }
            }
            mismatchDescription.appendText("]");

            switch (mError) {
                case ERR_NO_SPANS:
                    mismatchDescription.appendText(" had no spans");
                    break;
                case ERR_SPANNABLE_NULL:
                    mismatchDescription.appendText(" was null");
                    break;
                case ERR_WRONG_INDEX:
                    mismatchDescription.appendText(" had Emoji at ")
                            .appendValue("[" + mActualStart + "," + mActualEnd + "]");
                    break;
                default:
                    mismatchDescription.appendText(" does not have an EmojiSpan with given "
                            + "resource id ");
            }
        }

        @Override
        protected boolean matchesSafely(final CharSequence charSequence) {
            if (charSequence == null) {
                mError = ERR_SPANNABLE_NULL;
                return false;
            }

            if (!(charSequence instanceof Spanned)) {
                mError = ERR_NO_SPANS;
                return false;
            }

            Spanned spanned = (Spanned) charSequence;
            final EmojiSpan[] spans = spanned.getSpans(0, charSequence.length(), EmojiSpan.class);

            if (spans.length == 0) {
                mError = ERR_NO_SPANS;
                return false;
            }

            if (mStart == -1 && mEnd == -1) {
                for (int index = 0; index < spans.length; index++) {
                    if (mResId == spans[index].getId()) {
                        return true;
                    }
                }
                return false;
            } else {
                for (int index = 0; index < spans.length; index++) {
                    if (mResId == -1 || mResId == spans[index].getId()) {
                        mActualStart = spanned.getSpanStart(spans[index]);
                        mActualEnd = spanned.getSpanEnd(spans[index]);
                        if (mActualStart == mStart && mActualEnd == mEnd) {
                            return true;
                        }
                    }
                }

                if (mActualStart != -1 && mActualEnd != -1) {
                    mError = ERR_WRONG_INDEX;
                }

                return false;
            }
        }
    }
}
