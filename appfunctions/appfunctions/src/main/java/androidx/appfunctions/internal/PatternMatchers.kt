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

package androidx.appfunctions.internal

import android.os.PatternMatcher
import androidx.annotation.RestrictTo

/** Thrown when [PatternMatcher] rejects a pattern. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InvalidPatternMatcherException(message: String, cause: Throwable) :
    IllegalArgumentException(message, cause)

/**
 * Creates [PatternMatcher]s from `(pattern, type)` pairs, failing with
 * [InvalidPatternMatcherException] if [PatternMatcher] rejects a pattern.
 *
 * Used by generated inventories and when reading indexed metadata. Types are passed through to
 * [PatternMatcher] as is, so it decides how each type is interpreted on the device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PatternMatchers {

    /**
     * Returns a [PatternMatcher] for [pattern] and [type].
     *
     * @throws InvalidPatternMatcherException if [PatternMatcher] rejects [pattern].
     */
    @JvmStatic
    public fun create(pattern: String, type: Int): PatternMatcher =
        try {
            PatternMatcher(pattern, type)
        } catch (e: IllegalArgumentException) {
            throw InvalidPatternMatcherException("Invalid pattern \"$pattern\" of type $type", e)
        } catch (e: IndexOutOfBoundsException) {
            // PatternMatcher's advanced glob parser can index past the end of truncated patterns.
            throw InvalidPatternMatcherException("Invalid pattern \"$pattern\" of type $type", e)
        }

    /**
     * Returns a [PatternMatcher] for each `(pattern, type)` pair in [patterns].
     *
     * @throws InvalidPatternMatcherException if [PatternMatcher] rejects any of the patterns.
     */
    @JvmStatic
    public fun createList(vararg patterns: Pair<String, Int>): List<PatternMatcher> =
        patterns.map { (pattern, type) ->
            create(pattern, type)
        }
}
