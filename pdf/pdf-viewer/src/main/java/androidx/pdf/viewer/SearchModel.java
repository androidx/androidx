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

package androidx.pdf.viewer;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.find.MatchCount;
import androidx.pdf.models.MatchRects;
import androidx.pdf.util.CycleRange;
import androidx.pdf.util.CycleRange.Direction;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.util.Observables;
import androidx.pdf.util.Observables.ExposedValue;
import androidx.pdf.util.Preconditions;
import androidx.pdf.viewer.loader.PdfLoader;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stores data relevant to the current search, including the query and the selected match, and the
 * number of matches on each page.
 *
 * <p>SearchModel is responsible for starting SearchPageTextTasks for every page that it needs data
 * for. It uses the pdfLoader to do this. Whenever the user updates a query or selects "find next"
 * or "find previous", this class will update data about the number of matches on each page, and
 * about which match is selected. The viewer can listen to changes in the selected data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SearchModel {

    /**
     * The current query. Null if the user is not performing a search, or searches for
     * whitespace.
     */
    private final ExposedValue<String> mQuery = Observables.newExposedValueWithInitialValue(null);

    /**
     * The currently selected match. Null if there the search query is null or there are no matches
     * for the current query, or a match for the current query has not yet been found.
     */
    private final ExposedValue<SelectedMatch> mSelectedMatch =
            Observables.newExposedValueWithInitialValue(null);

    /**
     * We store the last selected match so that when the next search finishes, we can select the
     * match
     * that is as close as possible to the last selected match.
     */
    private SelectedMatch mLastSelectedMatch = null;

    /** The index of the current match out of the total matches found ie, match 3 of 8. */
    private final ExposedValue<MatchCount> mMatchCount =
            Observables.newExposedValueWithInitialValue(null);

    private final PdfLoader mPdfLoader;

    /**
     * The number of matches of the current query found on each page. Remains {@code null} until the
     * document is loaded and so the array length is known.
     */
    private int[] mPageToMatchCount;
    /** The total number of matches of the current query, found on all pages so far. */
    private int mTotalMatches = 0;

    /**
     * An iterator that spreads OUTWARDS from the current location. If null, counting all the
     * matches
     * hasn't started. If !hasNext(), counting the matches has finished - otherwise it is in
     * progress.
     */
    private CycleRange.Iterator mNextPageToCount;

    /**
     * An iterator that spreads FORWARDS or BACKWARDS from the last selected match. If null, it
     * means
     * no find-next or find-previous operation in progress. If !hasNext(), it means the entire
     * document was searched and no match was found. Otherwise, a find-next or find-previous is in
     * progress, and if a match is found, then the selected match will be updated.
     */
    private CycleRange.Iterator mNextSelectedPage;

    public SearchModel(@NonNull PdfLoader pdfLoader) {
        this.mPdfLoader = pdfLoader;
    }

    /** Set the number of pages the document has. */
    public void setNumPages(int numPages) {
        mPageToMatchCount = new int[numPages];
        clearPageToMatchCount();
    }

    /** Return the number of pages the document has, or -1 if not yet known. */
    public int getNumPages() {
        return (mPageToMatchCount != null) ? mPageToMatchCount.length : -1;
    }

    /** Return the current search query. */
    @NonNull
    public ObservableValue<String> query() {
        return mQuery;
    }

    /** Return the currently selected match. */
    @NonNull
    public ObservableValue<SelectedMatch> selectedMatch() {
        return mSelectedMatch;
    }

    /** Return index of the current match out of the total matches found. */
    @NonNull
    public ObservableValue<MatchCount> matchCount() {
        return mMatchCount;
    }

    /**
     * Returns the page that the currently selected match is on, or -1 if there is no currently
     * selected match.
     */
    public int getSelectedPage() {
        SelectedMatch value = mSelectedMatch.get();
        return (value != null) ? value.getPage() : -1;
    }

    /** Set query for new search. */
    public void setQuery(@Nullable String newQuery, int viewingPage) {
        newQuery = whiteSpaceToNull(newQuery);
        if (!Objects.equals(mQuery.get(), newQuery)) {
            mQuery.set(newQuery);
            mSelectedMatch.set(null);
            clearPageToMatchCount();
            if (newQuery != null) {
                startNewSearch(newQuery, viewingPage);
            } else {
                mLastSelectedMatch = null;
            }
        }
    }

    private void startNewSearch(String newQuery, int viewingPage) {
        if (getNumPages() < 0) {
            return; // Cannot search until setNumPages is called.
        }

        // Start on the page the last selected match was on, if there was one.
        // If not then start on the page the user is viewing.
        int startPage = (mLastSelectedMatch != null) ? mLastSelectedMatch.getPage() : viewingPage;

        // Make a plan to select a match, starting here and going forwards until we find the match.
        mNextSelectedPage = CycleRange.of(startPage, getNumPages(), Direction.FORWARDS).iterator();
        // Make a plan to count all matches on every page, starting here and going outwards until
        // there are no more pages to count.
        mNextPageToCount = CycleRange.of(startPage, getNumPages(), Direction.OUTWARDS).iterator();
        mPdfLoader.searchPageText(startPage, newQuery);
    }

    /** Clears pageToMatchCount array, nextPageToCount and totalMatches. */
    private void clearPageToMatchCount() {
        if (mPageToMatchCount != null) {
            Arrays.fill(mPageToMatchCount, -1);
        }
        mNextPageToCount = null;
        mTotalMatches = 0;
    }

    /**
     * Add these search results into the model. Returns true if another search task was started now
     * that these results have arrived, false if no further searching is necessary.
     */
    public boolean updateMatches(@NonNull String matchesQuery, int page,
            @NonNull MatchRects matches) {
        Preconditions.checkState(
                getNumPages() >= 0, "updateMatches should only be called after setNumPages");

        String currentQuery = this.mQuery.get();
        if (!Objects.equals(matchesQuery, currentQuery)) {
            return false; // This data is irrelevant as it is for an old query - ignore.
        }

        // Update pageToMatchCount and totalMatches with data from this page, if it is new data.
        if (mPageToMatchCount[page] == -1) {
            mPageToMatchCount[page] = matches.size();
            mTotalMatches += matches.size();
        }

        // If a search is ongoing and we've found the next match on this page, we update
        // selectedMatch and stop the search by setting nextSelectedPage iterator to null.
        if (mNextSelectedPage != null
                && mNextSelectedPage.hasNext()
                && mNextSelectedPage.peekNext() == page
                && !matches.isEmpty()) {

            if (mLastSelectedMatch != null && mLastSelectedMatch.getPage() == page) {
                // The last search result was on this page too - find the new match closest to
                // the previous:
                mSelectedMatch.set(mLastSelectedMatch.nearestMatch(currentQuery, matches));
            } else {
                // Select either the first or last match on the page, depending on which direction
                // we are searching:
                int selectedIndex =
                        (mNextSelectedPage.getDirection() == Direction.BACKWARDS) ? matches.size()
                                - 1 : 0;
                mSelectedMatch.set(new SelectedMatch(currentQuery, page, matches, selectedIndex));
            }
            mLastSelectedMatch = mSelectedMatch.get();
            // Clear the nextSelectedPage iterator, indicating we have found selected a match.
            mNextSelectedPage = null;
        }

        // Search for the next selected match, or if that isn't needed, continue counting matches.
        boolean newSearchStarted =
                searchNextPageThat(Condition.IS_MATCH_COUNT_UNKNOWN_OR_POSITIVE, mNextSelectedPage)
                        || searchNextPageThat(Condition.IS_MATCH_COUNT_UNKNOWN, mNextPageToCount);
        updateMatchCount();
        return newSearchStarted;
    }

    private void updateMatchCount() {
        SelectedMatch currentMatch = mSelectedMatch.get();
        int overallSelectedIndex = -1;
        if (currentMatch != null) {
            overallSelectedIndex = currentMatch.getSelected();
            for (int p = 0; p < currentMatch.getPage(); p++) {
                if (mPageToMatchCount[p] > 0) {
                    overallSelectedIndex += mPageToMatchCount[p];
                }
            }
        }
        boolean isAllPagesCounted = mNextPageToCount != null && !mNextPageToCount.hasNext();
        MatchCount newMatchCount =
                new MatchCount(overallSelectedIndex, mTotalMatches, isAllPagesCounted);
        if (!Objects.equals(newMatchCount, mMatchCount.get())) {
            mMatchCount.set(newMatchCount);
        }
    }

    /**
     * Selects the next match - may succeed immediately, if the next match is on the same page,
     * or may
     * request it from the PdfLoader, which will run asynchronously and eventually call {@link
     * #updateMatches}.
     */
    public void selectNextMatch(@NonNull Direction direction, int viewingPage) {
        if (getNumPages() < 0) {
            return; // Cannot search until setNumPages is called.
        }

        String currentQuery = mQuery.get();
        SelectedMatch currentMatch = mSelectedMatch.get();

        if (currentQuery != null) {
            if (selectNextMatchOnPage(direction)) {
                return;
            }
            int startPage = viewingPage;
            if (currentMatch != null) {
                startPage = currentMatch.getPage() + direction.sign;
            }
            mNextSelectedPage = CycleRange.of(startPage, getNumPages(), direction).iterator();
            searchNextPageThat(Condition.IS_MATCH_COUNT_UNKNOWN_OR_POSITIVE, mNextSelectedPage);
        }
    }

    private boolean selectNextMatchOnPage(Direction direction) {
        if (mSelectedMatch.get() != null) {
            SelectedMatch nextMatch = mSelectedMatch.get().selectNextMatchOnPage(direction);
            if (nextMatch != null) {
                mSelectedMatch.set(nextMatch);
                mLastSelectedMatch = nextMatch;
                updateMatchCount();
                return true;
            }
        }
        return false;
    }

    /** Return the page overlay for the selection. */
    @Nullable
    public PdfHighlightOverlay getOverlay(@NonNull String matchesQuery, int page,
            @NonNull MatchRects matches) {
        if (page == getSelectedPage()) {
            return mSelectedMatch.get().getOverlay();
        }
        if (Objects.equals(matchesQuery, mQuery.get())) {
            return new PdfHighlightOverlay(matches);
        }
        return null;
    }

    /**
     * Walks through the given iterator, and launches a search task as soon as a page is found that
     * meets the given condition.
     *
     * @return true if such a page is found and a search task is started.
     */
    private boolean searchNextPageThat(Condition condition,
            @Nullable CycleRange.Iterator iterator) {
        if (iterator == null) {
            return false;
        }
        while (iterator.hasNext() && !condition.apply(mPageToMatchCount[iterator.peekNext()])) {
            iterator.next();
        }
        if (iterator.hasNext()) {
            mPdfLoader.searchPageText(iterator.peekNext(), mQuery.get());
            return true;
        }
        return false;
    }

    /** Different conditions relating to the number of matches known to be on a certain page. */
    private enum Condition {
        IS_MATCH_COUNT_UNKNOWN {
            @Override
            boolean apply(int matchCount) {
                return matchCount == -1;
            }
        },
        IS_MATCH_COUNT_UNKNOWN_OR_POSITIVE {
            @Override
            boolean apply(int matchCount) {
                return matchCount != 0;
            }
        };

        abstract boolean apply(int matchCount);
    }

    /**
     * Treat whitespace-only strings the same as null, which means, don't search: whitespace can
     * match
     * newlines, which don't have a highlightable area.
     */
    @Nullable
    public static String whiteSpaceToNull(@NonNull String query) {
        return (query != null && TextUtils.isGraphic(query)) ? query : null;
    }

    /** Update the current selected match */
    public void setSelectedMatch(@NonNull SelectedMatch selectedMatch) {
        mSelectedMatch.set(selectedMatch);
    }
}
