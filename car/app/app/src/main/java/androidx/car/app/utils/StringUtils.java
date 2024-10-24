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

package androidx.car.app.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Assorted string manipulation utilities.
 *
 */
@RestrictTo(LIBRARY)
public final class StringUtils {
    private static final int MAX_SHORT_STRING_LENGTH = 16;

    /**
     * Shortens a string to a maximum length.
     *
     * <p>This can be used for making logs less noisy.
     *
     * <p>For example, "Bananas are so yummy", may be shortened to "Banan~yummy".
     */
    public static @NonNull String shortenString(@NonNull String s) {
        int length = s.length();
        if (length <= MAX_SHORT_STRING_LENGTH) {
            return s;
        }
        int visible = MAX_SHORT_STRING_LENGTH / 2;
        return s.substring(0, visible) + "~" + s.substring(s.length() - visible);
    }

    private StringUtils() {
    }
}
