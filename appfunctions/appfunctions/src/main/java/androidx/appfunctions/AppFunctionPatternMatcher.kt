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

package androidx.appfunctions

/**
 * Declares a [android.os.PatternMatcher] that string values can match, used in
 * [AppFunctionStringValueConstraint.patternMatchers].
 *
 * When multiple matchers are declared, a value is valid if it matches any of them (logical OR).
 *
 * The arguments mirror the [android.os.PatternMatcher] constructor and are translated into one at
 * compile time.
 *
 * ### Usage Example:
 * ```
 * // Accepts values that start with "acct_" OR consist only of digits.
 * @AppFunctionDeclaration
 * fun setAccountId(
 *     @AppFunctionStringValueConstraint(
 *         patternMatchers = [
 *             AppFunctionPatternMatcher("acct_", PatternMatcher.PATTERN_PREFIX),
 *             AppFunctionPatternMatcher("[0-9]+", PatternMatcher.PATTERN_ADVANCED_GLOB),
 *         ]
 *     )
 *     accountId: String
 * ) {
 *     // Function body
 * }
 * ```
 */
@Retention(AnnotationRetention.BINARY)
@Target
public annotation class AppFunctionPatternMatcher(
    /**
     * The pattern string, as passed to [android.os.PatternMatcher].
     *
     * The pattern is not validated at compile time. If [android.os.PatternMatcher] rejects it (for
     * example, a malformed [android.os.PatternMatcher.PATTERN_ADVANCED_GLOB]), the function using
     * it is omitted from agents' search results. If the pattern is on a property of an
     * [AppFunctionSerializable], all of the app's functions are omitted.
     */
    val pattern: String,
    /**
     * The pattern type: one of the `android.os.PatternMatcher.PATTERN_*` constants, such as
     * [android.os.PatternMatcher.PATTERN_PREFIX] or
     * [android.os.PatternMatcher.PATTERN_ADVANCED_GLOB].
     *
     * The type is interpreted by [android.os.PatternMatcher] on the device, so a type that the
     * device's SDK does not support matches no values.
     */
    val type: Int,
)
