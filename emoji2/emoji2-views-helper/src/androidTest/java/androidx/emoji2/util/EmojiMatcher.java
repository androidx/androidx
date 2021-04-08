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

import android.text.TextUtils;

import org.mockito.ArgumentMatcher;

/**
 * Utility class that includes matchers specific to emojis and EmojiSpans.
 */
public class EmojiMatcher {
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
}
