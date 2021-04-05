/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.emoji2.util;

import static org.mockito.ArgumentMatchers.argThat;

import android.text.Spanned;
import android.text.TextUtils;

import androidx.emoji2.text.EmojiSpan;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.ArgumentMatcher;

/**
 * Utility class that includes matchers specific to emojis and EmojiSpans.
 */
public class EmojiMatcher {

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

}
