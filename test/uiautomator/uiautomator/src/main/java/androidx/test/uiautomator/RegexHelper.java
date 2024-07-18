/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.test.uiautomator;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

/**
 * This class contains static helper methods about regex.
 */
class RegexHelper {

    private RegexHelper(){}

    /**
     * Returns a {@link Pattern} that matches when content starts with given string
     * (case-sensitive).
     */
    static Pattern getPatternStartsWith(@NonNull String text) {
        return Pattern.compile(String.format("^%s.*$", Pattern.quote(text)), Pattern.DOTALL);
    }

    /**
     * Returns a {@link Pattern} that matches when content ends with given string
     * (case-sensitive).
     */
    static Pattern getPatternEndsWith(@NonNull String text) {
        return Pattern.compile(String.format("^.*%s$", Pattern.quote(text)), Pattern.DOTALL);
    }

    /**
     * Returns a {@link Pattern} that matches when content contains given string (case-sensitive).
     */
    static Pattern getPatternContains(@NonNull String text) {
        return Pattern.compile(String.format("^.*%s.*$", Pattern.quote(text)), Pattern.DOTALL);
    }
}
