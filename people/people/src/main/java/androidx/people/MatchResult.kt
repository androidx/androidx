/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import androidx.annotation.RestrictTo

/**
 * Outcome of looking up a single [Criterion] in one People Directory application.
 *
 * Returned by [PersonQueryResult.getResultFor] (with [PersonMatch] elements) and
 * [GroupQueryResult.getResultFor] (with [GroupMatch] elements):
 * ```
 * when (val match = result.getResultFor("com.example.voip", Criterion.Phone(number))) {
 *     is MatchResult.Handled -> show(match.matches) // empty if evaluated with no match
 *     MatchResult.Unhandled -> showPending()
 * }
 * ```
 *
 * The two outcomes mirror `handledQuery` and `unhandledQuery` on a query result, with [Unhandled]
 * also returned for any criterion that was not part of the original query.
 *
 * @param T the match type, such as [PersonMatch] or [GroupMatch]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public sealed class MatchResult<out T> private constructor() {

    /**
     * Reports that the People Directory application evaluated the criterion and returned [matches],
     * which is empty when no record matched.
     *
     * @param T the match type, such as [PersonMatch] or [GroupMatch]
     */
    public class Handled<out T>(matches: Set<T>) : MatchResult<T>() {
        /**
         * The records that matched the criterion, or an empty set if the criterion was evaluated
         * and matched no record. The order is unspecified.
         */
        public val matches: Set<T> = matches.toSet()

        override fun equals(other: Any?): Boolean =
            this === other || (other is Handled<*> && matches == other.matches)

        override fun hashCode(): Int = matches.hashCode()

        override fun toString(): String = "MatchResult.Handled(matches=$matches)"
    }

    /**
     * Reports that the criterion was not evaluated in this query result — either because the People
     * Directory application did not handle it (for example while its data is not cached yet, or
     * when the People Directory is unavailable), or because the criterion was not part of the
     * query.
     */
    public object Unhandled : MatchResult<Nothing>() {
        override fun toString(): String = "MatchResult.Unhandled"
    }
}
