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

package androidx.pdf.viewer.fragment.model

import androidx.annotation.RestrictTo

/** A sealed interface representing the various UI states of the PdfSearchView. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal sealed interface SearchViewUiState {
    /**
     * Represents the state where the search functionality is closed.
     *
     * This state should be used to hide the search view and reset any associated states, indicating
     * that the search is no longer active or visible to the user.
     */
    object Closed : SearchViewUiState

    /**
     * Represents the state where the search functionality is initialized, but no search operation
     * has been triggered yet.
     *
     * This state can be used to display the search view to the user, indicating that the search is
     * ready for interaction, but no query has been entered or processed.
     */
    object Init : SearchViewUiState

    /**
     * Represents the state when the search view is actively interacted with by the user.
     *
     * In this state, the search view contains a non-empty query and results if the search operation
     * has finished.
     *
     * @param query for the current search operation.
     * @param currentMatch current match out of total matches.
     * @param totalMatches total number of matches after search operation.
     */
    data class Active(val query: String, val currentMatch: Int, val totalMatches: Int) :
        SearchViewUiState
}
