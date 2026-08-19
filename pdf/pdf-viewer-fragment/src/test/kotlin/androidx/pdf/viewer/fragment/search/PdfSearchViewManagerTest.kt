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

package androidx.pdf.viewer.fragment.search

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import androidx.pdf.view.search.PdfSearchView
import androidx.pdf.viewer.fragment.model.SearchViewUiState
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class PdfSearchViewManagerTest {

    private lateinit var context: Context
    private lateinit var pdfSearchView: PdfSearchView
    private lateinit var searchViewManager: PdfSearchViewManager

    @Before
    fun setup() {
        context =
            ContextThemeWrapper(
                ApplicationProvider.getApplicationContext(),
                com.google.android.material.R.style.Theme_Material3_DayNight_NoActionBar,
            )
        pdfSearchView = PdfSearchView(context)
        searchViewManager = PdfSearchViewManager(pdfSearchView)
    }

    @Test
    fun test_setState_closed() {
        searchViewManager.setState(SearchViewUiState.Closed)

        assertEquals(View.GONE, pdfSearchView.visibility)
    }

    @Test
    fun test_setState_init() {
        searchViewManager.setState(SearchViewUiState.Init)

        assertEquals(View.VISIBLE, pdfSearchView.visibility)
        assertEquals("", pdfSearchView.searchQueryBox.text.toString())
        assertFalse(pdfSearchView.findPrevButton.isEnabled)
        assertFalse(pdfSearchView.findNextButton.isEnabled)
    }

    @Test
    fun test_setState_active_withMatches_isSearchingTrue() {
        searchViewManager.setState(
            SearchViewUiState.Active(
                query = "test",
                currentMatch = 1,
                totalMatches = 5,
                isSearching = true,
            )
        )

        assertEquals(View.VISIBLE, pdfSearchView.visibility)
        assertEquals("1 / 5…", pdfSearchView.matchStatusTextView.text.toString())
        assertEquals(
            "1 of 5, searching",
            pdfSearchView.matchStatusTextView.contentDescription.toString(),
        )
        assertTrue(pdfSearchView.findPrevButton.isEnabled)
        assertTrue(pdfSearchView.findNextButton.isEnabled)
    }

    @Test
    fun test_setState_active_withMatches_isSearchingFalse() {
        searchViewManager.setState(
            SearchViewUiState.Active(
                query = "test",
                currentMatch = 1,
                totalMatches = 5,
                isSearching = false,
            )
        )

        assertEquals(View.VISIBLE, pdfSearchView.visibility)
        assertEquals("1 / 5", pdfSearchView.matchStatusTextView.text.toString())
        assertEquals("1 of 5", pdfSearchView.matchStatusTextView.contentDescription.toString())
        assertTrue(pdfSearchView.findPrevButton.isEnabled)
        assertTrue(pdfSearchView.findNextButton.isEnabled)
    }

    @Test
    fun test_setState_active_noMatches_isSearchingTrue() {
        searchViewManager.setState(
            SearchViewUiState.Active(
                query = "nonexistent",
                currentMatch = 0,
                totalMatches = 0,
                isSearching = true,
            )
        )

        assertEquals(View.VISIBLE, pdfSearchView.visibility)
        assertEquals("0 / 0…", pdfSearchView.matchStatusTextView.text.toString())
        assertEquals(
            "0 of 0, searching",
            pdfSearchView.matchStatusTextView.contentDescription.toString(),
        )
        assertFalse(pdfSearchView.findPrevButton.isEnabled)
        assertFalse(pdfSearchView.findNextButton.isEnabled)
    }

    @Test
    fun test_setState_active_noMatches_isSearchingFalse() {
        searchViewManager.setState(
            SearchViewUiState.Active(
                query = "nonexistent",
                currentMatch = 0,
                totalMatches = 0,
                isSearching = false,
            )
        )

        assertEquals(View.VISIBLE, pdfSearchView.visibility)
        assertEquals("0 / 0", pdfSearchView.matchStatusTextView.text.toString())
        assertEquals("0 of 0", pdfSearchView.matchStatusTextView.contentDescription.toString())
        assertFalse(pdfSearchView.findPrevButton.isEnabled)
        assertFalse(pdfSearchView.findNextButton.isEnabled)
    }

    @Test
    fun test_setState_active_doesNotMutateSearchQueryBoxText() {
        pdfSearchView.searchQueryBox.setText("user_input")

        searchViewManager.setState(
            SearchViewUiState.Active(
                query = "search_result_query",
                currentMatch = 1,
                totalMatches = 3,
                isSearching = true,
            )
        )

        assertEquals("user_input", pdfSearchView.searchQueryBox.text.toString())
    }
}
