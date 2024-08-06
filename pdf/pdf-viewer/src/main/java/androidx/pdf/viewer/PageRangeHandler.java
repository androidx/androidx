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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.data.Range;
import androidx.pdf.util.Preconditions;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PageRangeHandler {
    private static final int PAGE_PREFETCH_RADIUS = 1;

    private final PaginationModel mPaginationModel;

    /** The range of currently visible pages. */
    private Range mVisiblePages = null;

    /** The highest number page reached. */
    private int mMaxPage = -1;

    PageRangeHandler(PaginationModel paginationModel) {
        this.mPaginationModel = paginationModel;
        this.mVisiblePages = new Range();
    }

    @Nullable
    public Range getVisiblePages() {
        return mVisiblePages;
    }

    public void setVisiblePages(@Nullable Range visiblePages) {
        mVisiblePages = visiblePages;
    }

    public int getMaxPage() {
        return mMaxPage;
    }

    public void setMaxPage(int maxPage) {
        mMaxPage = maxPage;
    }

    /**
     * Returns the page currently roughly centered in the view.
     */
    public int getVisiblePage() {
        return (mVisiblePages != null) ? (mVisiblePages.getFirst() + mVisiblePages.getLast()) / 2
                : 0;
    }

    /**
     * Updates the max page to the upper bound of the visible page range if the upper bound is
     * greater than the current max page
     */
    public void adjustMaxPageToUpperVisibleRange() {
        if (mVisiblePages != null) {
            mMaxPage = Math.max(mVisiblePages.getLast(), mMaxPage);
        }
    }

    /** Updates the visible page range based on the y-scroll, current zoom and the view height */
    public void refreshVisiblePageRange(int scrollY, float zoom, int viewHeight) {
        mVisiblePages = computeVisibleRange(scrollY, zoom, viewHeight, true);
    }

    /** Computes the range of visible pages in the given position. */
    @NonNull
    public Range computeVisibleRange(int scrollY, float zoom, int viewHeight,
            boolean includePartial) {
        Preconditions.checkArgument(zoom > 0, "Zoom factor must be positive!");

        int top = Math.round(scrollY / zoom);
        int bottom = Math.round((scrollY + viewHeight) / zoom);
        Range window = new Range(top, bottom);
        return mPaginationModel.getPagesInWindow(window, includePartial);
    }

    /** Returns the range of pages within the prefetch radius of the visible pages. */
    @NonNull
    public Range getNearPagesToVisibleRange() {
        Range allPages = new Range(0, mPaginationModel.getSize() - 1);
        return mVisiblePages.expand(PAGE_PREFETCH_RADIUS, allPages);
    }

    /** Returns the pages that are out of view and prefetch radius */
    @NonNull
    public Range[] getGonePageRanges(@NonNull Range nearPages) {
        Range allPages = new Range(0, mPaginationModel.getSize() - 1);
        return allPages.minus(nearPages);
    }

    /** Returns the range of pages near the visible pages that are invisible to the view port */
    @NonNull
    public Range[] getInvisibleNearPageRanges(@NonNull Range nearPages) {
        return nearPages.minus(mVisiblePages);
    }
}
