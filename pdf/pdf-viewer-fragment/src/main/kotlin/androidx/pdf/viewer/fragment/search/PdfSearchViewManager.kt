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

package androidx.pdf.viewer.fragment.search

import android.view.View
import androidx.annotation.RestrictTo
import androidx.pdf.view.search.PdfSearchView
import androidx.pdf.viewer.fragment.model.SearchViewUiState

/** A view manager class for [PdfSearchView] that updates it based on [SearchViewUiState]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class PdfSearchViewManager(private val pdfSearchView: PdfSearchView) {

    fun setState(uiState: SearchViewUiState) {
        when (uiState) {
            is SearchViewUiState.Closed -> closeView()
            is SearchViewUiState.Init -> initView()
            is SearchViewUiState.Active -> updateViewAfterSearch(uiState)
        }
    }

    private fun closeView() {
        pdfSearchView.apply {
            visibility = View.GONE
            searchQueryBox.setText(EMPTY_QUERY_STRING)
            matchStatusTextView.text = EMPTY_MATCH_STATUS_STRING
        }
    }

    private fun initView() {
        pdfSearchView.apply {
            // Hide match counter initially when search is not triggered
            matchStatusTextView.visibility = View.GONE
            // Disable prev and next button as no search results are present
            findPrevButton.isEnabled = false
            findNextButton.isEnabled = false

            visibility = View.VISIBLE
            // Explicitly request focus to open IME
            if (!searchQueryBox.hasFocus()) searchQueryBox.requestFocus()
        }
    }

    private fun updateViewAfterSearch(results: SearchViewUiState.Active) {
        pdfSearchView.apply {
            matchStatusTextView.text =
                prepareMatchStatusText(results.currentMatch, results.totalMatches)
            matchStatusTextView.contentDescription =
                context.getString(
                    androidx.pdf.R.string.match_status_description,
                    results.currentMatch,
                    results.totalMatches,
                )

            if (results.totalMatches == 0) {
                /*
                Disable prev and next buttons when totalMatches = 0, which represents
                no search results found for current search query.
                */
                findPrevButton.isEnabled = false
                findNextButton.isEnabled = false
            } else {
                // Enable prev and next buttons to iterate through search results
                findPrevButton.isEnabled = true
                findNextButton.isEnabled = true
            }

            matchStatusTextView.visibility = View.VISIBLE

            // Restores the search query in the EditText, if needed (e.g., after process death).
            if (searchQueryBox.text.toString() != results.query) {
                searchQueryBox.setText(results.query)
                searchQueryBox.setSelection(results.query.length)
            }

            visibility = View.VISIBLE
        }
    }

    private fun prepareMatchStatusText(currentSelection: Int, totalMatches: Int): String =
        pdfSearchView.context.getString(
            androidx.pdf.R.string.message_match_status,
            currentSelection, // selection-index
            totalMatches, // total matches
        )

    companion object {
        private const val EMPTY_QUERY_STRING = ""
        private const val EMPTY_MATCH_STATUS_STRING = ""
    }
}
