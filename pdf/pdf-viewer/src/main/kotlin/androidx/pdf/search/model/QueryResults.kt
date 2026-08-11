/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.search.model

import android.util.SparseArray
import androidx.annotation.RestrictTo
import androidx.pdf.content.PageMatchBounds

/** A sealed interface that encapsulates the various states of a search operation's result. */
@RestrictTo(RestrictTo.Scope.LIBRARY) public sealed interface SearchResultState

/**
 * Represents the initial state when no query has been submitted to trigger a search operation. This
 * state occurs before any search is initiated.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) public object NoQuery : SearchResultState

/**
 * A sealed class representing the outcome of a search operation.
 *
 * @param query The search query that initiated the search.
 * @param pageRange The range of PDF pages involved in the search.
 * @param isSearching Indicates if the search operation is still in progress.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public sealed class QueryResults(
    public val query: String,
    public val pageRange: IntRange,
    public val isSearching: Boolean = false,
) : SearchResultState {

    /**
     * Represents the state when no results are found after a search operation. This indicates that
     * the search yielded no matching results.
     *
     * @param query The search query that was executed.
     * @param pageRange The range of PDF pages included in the search.
     * @param isSearching Indicates if the search operation is still in progress.
     */
    public class NoMatch(query: String, pageRange: IntRange, isSearching: Boolean = false) :
        QueryResults(query, pageRange, isSearching)

    /**
     * Represents the state when a search operation returns results.
     *
     * @param query The search query that was executed.
     * @param pageRange The range of PDF pages included in the search.
     * @param resultBounds A mapping of match bounds for the results, indexed by their position.
     * @param queryResultsIndex Represents an index pointer to an element in [resultBounds].
     * @param isSearching Indicates if the search operation is still in progress.
     */
    public class Matched(
        query: String,
        pageRange: IntRange,
        public val resultBounds: SparseArray<List<PageMatchBounds>>,
        public val queryResultsIndex: QueryResultsIndex,
        isSearching: Boolean = false,
    ) : QueryResults(query, pageRange, isSearching)
}
