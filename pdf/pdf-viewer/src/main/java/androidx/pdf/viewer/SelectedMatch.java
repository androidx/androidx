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

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.MatchRects;
import androidx.pdf.util.CycleRange.Direction;
import androidx.pdf.util.Preconditions;

/**
 * Represents a currently selected match, including the query that was matched, the page the match
 * is on, all the matches on that page, and which of these matches on the page is currently
 * selected.
 *
 * <p>If no match is selected, then a null is used instead of a SelectedMatch.
 *
 * <p>Immutable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SelectedMatch {
    private final String mQuery;
    private final int mPage;
    private final MatchRects mPageMatches;
    private final int mSelected;

    /**
     * Construct a new immutable SelectedMatch - either one with at least one match, where one of
     * the
     * matches is selected, or one with no matches.
     */
    public SelectedMatch(@Nullable String query, int page, @Nullable MatchRects pageMatches,
            int selected) {
        Preconditions.checkNotNull(query);
        Preconditions.checkNotNull(pageMatches);
        Preconditions.checkArgument(!pageMatches.isEmpty(), "Cannot select empty matches");
        Preconditions.checkArgument(
                selected >= 0 && selected < pageMatches.size(), "selected match is out of range");

        this.mQuery = query;
        this.mPage = page;
        this.mPageMatches = pageMatches;
        this.mSelected = selected;
    }

    public int getPage() {
        return mPage;
    }

    @NonNull
    public MatchRects getPageMatches() {
        return mPageMatches;
    }

    public int getSelected() {
        return mSelected;
    }

    public boolean isEmpty() {
        return mPageMatches.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SelectedMatch)) {
            return false;
        }
        SelectedMatch that = (SelectedMatch) other;
        return this.mQuery.equals(that.mQuery)
                && this.mPage == that.mPage
                && this.mPageMatches.equals(that.mPageMatches)
                && this.mSelected == that.mSelected;
    }

    @Override
    public int hashCode() {
        return mQuery.hashCode() + 31 * mPage + 101 * mPageMatches.hashCode() + 313 * mSelected;
    }

    @Nullable
    public Rect getFirstSelectionRect() {
        return isEmpty() ? null : mPageMatches.getFirstRect(mSelected);
    }

    /** Returns the page overlay for this selection. */
    @Nullable
    public PdfHighlightOverlay getOverlay() {
        return isEmpty() ? null : new PdfHighlightOverlay(mPageMatches, mSelected);
    }

    @Nullable
    public SelectedMatch selectNextMatchOnPage(@NonNull Direction direction) {
        if (direction == Direction.BACKWARDS && mSelected > 0) {
            return withSelected(mSelected - 1);
        } else if (direction == Direction.FORWARDS && mSelected < mPageMatches.size() - 1) {
            return withSelected(mSelected + 1);
        }
        return null;
    }

    private SelectedMatch withSelected(int selected) {
        return new SelectedMatch(mQuery, mPage, mPageMatches, selected);
    }

    /**
     * Given a new set of matches, selects the one that is closest to the old selected match (if
     * any).
     */
    @NonNull
    public SelectedMatch nearestMatch(@NonNull String newQuery, @NonNull MatchRects newMatches) {
        if (newMatches.isEmpty()) {
            return noMatches(newQuery, mPage);
        }
        if (this.isEmpty()) {
            return firstMatch(newQuery, mPage, newMatches);
        }

        int oldCharIndex = isEmpty() ? 0 : mPageMatches.getCharIndex(mSelected);
        int newMatch = newMatches.getMatchNearestCharIndex(oldCharIndex);
        return new SelectedMatch(newQuery, mPage, newMatches, newMatch);
    }

    /** Returns a SelectedMatch that contains no matches and so nothing is selected. */
    @NonNull
    public static SelectedMatch noMatches(@NonNull String query, int page) {
        return new SelectedMatch(query, page, MatchRects.NO_MATCHES, -1);
    }

    /** Selects the first match from the given matches. */
    @NonNull
    public static SelectedMatch firstMatch(@NonNull String query, int page,
            @NonNull MatchRects matches) {
        return matches.isEmpty() ? noMatches(query, page) : new SelectedMatch(query, page, matches,
                0);
    }
}
