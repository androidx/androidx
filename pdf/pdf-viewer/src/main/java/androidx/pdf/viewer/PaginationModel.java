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
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.pdf.aidl.Dimensions;
import androidx.pdf.data.Range;
import androidx.pdf.util.ErrorLog;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.ProjectorContext;
import androidx.pdf.util.Screen;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Model representing PDF pages as a vertical, paginated layout.
 *
 * <p>Arranges the dimensions for each page in order, adds vertical spacing and can compute the
 * coordinates of a given page in the model {@link #getPageLocation}.
 *
 * <p>Left and right coordinates for a given page will be computed based on the current viewArea of
 * the screen so as to maximize the amount of each page that is visible. Therefore a page whose
 * width is smaller than the containing view will slide right or left when the viewArea is limited
 * to one side in order to show as much of the page as possible.
 *
 * <p>To configure this model:
 *
 * <ol>
 *   <li>{@link #initialize(int)} with the number of pages it will contain.
 *   <li>{@link #addPage(int, Dimensions)} to set the dimensions for each page.
 *   <li>{@link #setViewArea(Rect)} to report current visible area so pages can be moved
 *       horizontally for maximum visibility.
 * </ol>
 *
 * <p>This model is observable. Any classes implementing {@link PaginationModelObserver} can
 * register themselves via {@link #addObserver(PaginationModelObserver)} and will be notified when
 * pages are added or the {@link #mViewArea} is changed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PaginationModel {

    private static final String TAG = PaginationModel.class.getSimpleName();

    /** The spacing added before and after each page, in dip. */
    private static final int PAGE_SPACING_DP = 4;

    /**
     * The spacing added before and after each page (the actual space between 2 consecutive pages is
     * twice this distance), in pixels.
     */
    private final int mPageSpacingPx;

    /** The maximum number of pages this model can accommodate. */
    private int mMaxPages = -1;

    /** The Dimensions of each page in the list. */
    private Dimensions[] mPages;

    /** The vertical coordinates of the virtual tabs sitting in between pages. */
    private int[] mPageStops;

    /** The number of pages known to this model (i.e. their Dimensions are known). */
    private int mSize = 0;

    /** An estimate of the size of each unseen page. */
    private float mEstimatedPageHeight = 0;

    private int mAccumulatedPageSize = 0;

    /**
     * The portion of this model that is currently (or last we knew) exposed on the screen.
     *
     * <p>In the co-ordinates of this model - so if this entire model is within the visible area,
     * then
     * {@code viewArea} will contain the rect Rect(0, 0, getWidth, getHeight). Current visible area
     * should be reported to this model via {@link #setViewArea(Rect)}.
     */
    private final Rect mViewArea = new Rect();

    /**
     * A temp working instance for computing {@link #mViewArea} to avoid excessive object
     * creation.
     */
    private final Rect mTempViewArea = new Rect();

    private final Set<PaginationModelObserver> mObservers = new HashSet<>();

    public PaginationModel() {
        Screen screen = ProjectorContext.get().getScreen();
        mPageSpacingPx = screen.pxFromDp(PAGE_SPACING_DP);
    }

    /**
     * Initializes the model.
     *
     * <p>Can be called more than once (with the same value): subsequent calls will not do anything.
     *
     * @param numPages the total number of pages of the document the model will contain.
     */
    public void initialize(int numPages) {
        Preconditions.checkArgument(numPages >= 0, "Num pages should be >= 0, " + numPages);
        if (isInitialized()) {
            // Already initialized, don't overwrite existing data.
            ErrorLog.checkState(mMaxPages == numPages, TAG, "init",
                    String.format("called with different value %d, was %d.", numPages, mMaxPages));
            return;
        }

        mMaxPages = numPages;
        mPages = new Dimensions[mMaxPages];
        mPageStops = new int[mMaxPages];
    }

    /**
     * True if the maxPages value has been set meaning this model has page data, false otherwise
     * .
     */
    public boolean isInitialized() {
        return mMaxPages != -1;
    }

    /** The vertical coordinates of the tops of the known pages (excluding any spacing) */
    private final List<Integer> mPageTops =
            new AbstractList<Integer>() {
                @Override
                public Integer get(int index) {
                    return mPageStops[index] + mPageSpacingPx;
                }

                @Override
                public int size() {
                    return mSize;
                }
            };

    /** The vertical coordinates of the bottoms of the known pages (excluding any spacing) */
    private final List<Integer> mPageBottoms =
            new AbstractList<Integer>() {
                @Override
                public Integer get(int index) {
                    return mPageStops[index] + mPageSpacingPx + mPages[index].getHeight();
                }

                @Override
                public int size() {
                    return mSize;
                }
            };

    /** Adds the dimensions of the page at {@code pageNum} to this model. */
    public void addPage(int pageNum, Dimensions pageSize) {
        Preconditions.checkNotNull(pageSize);
        if (pageNum < mSize) {
            ErrorLog.log(TAG, "addPage", String.format("ignored add page#%d < %d", pageNum, mSize));
            return;
        }
        if (pageNum >= mMaxPages) {
            ErrorLog.log(TAG, "addPage",
                    String.format("cant add page - not initialized?" + "page#%d >= maxpages:%d",
                            pageNum, mMaxPages));
            return;
        }
        for (int i = mSize; i < pageNum; i++) {
            // Edge case: there are missing pages. Create them temporarily as clones of this one.
            ErrorLog.log(TAG, "Backfill page# " + i);
            mPages[i] = pageSize;
        }
        mPages[pageNum] = pageSize;
        mSize = pageNum + 1;
        mAccumulatedPageSize += pageSize.getHeight();
        mEstimatedPageHeight = mAccumulatedPageSize / mSize;

        computeStops();
        notifyPageAdded();
    }

    /** Computes the page stops: the vertical coordinates where each page is positioned. */
    private void computeStops() {
        mPageStops[0] = 0;
        int p = 0;
        while (p < mSize - 1) {
            if (mPages[p] == null) {
                ErrorLog.logAndThrow(TAG, "computeTops",
                        String.format("Missing page %d in (0,%d)", p, mSize));
            }
            mPageStops[p + 1] = mPageStops[p] + mPages[p].getHeight() + 2 * mPageSpacingPx;
            p++;
        }
    }

    /**
     * Returns where to look to find the given x coordinate on the given page.
     *
     * @param pageNum the page the point is on.
     * @param offsetX the x-coordinate of the point on the page, in page units, from the left edge.
     * @return the x-coordinate we should look to find the point, not where the point is now. The
     * point can move when the view moves, so this returns where the point will be when the view
     * is centered on it.
     */
    public int getLookAtX(int pageNum, int offsetX) {
        double xRatio = 1.0 * offsetX / mPages[pageNum].getWidth();
        return (int) (xRatio * getWidth());
    }

    /**
     * Returns where to look to find the given y coordinate on the given page.
     *
     * @param pageNum the page the point is on.
     * @param offsetY the y-coordinate of the point on the page, in page units, from the top edge.
     * @return the y-coordinate we should look to find the point.
     */
    public int getLookAtY(int pageNum, int offsetY) {
        return mPageStops[pageNum] + offsetY;
    }

    /**
     * Computes the range of pages that intersect the given window (given as an interval of vertical
     * coordinates, and widths are ignored in this computation).
     *
     * <p>Gives at least one page, even if includePartial == false and no page is entirely visible
     * (gives the one page that is partially visible in that case). If the interval doesn't overlap
     * the existing pages, it gives an empty range with range.last past the range of available pages
     * in this model.
     *
     * @param intervalPx     The interval of vertical coordinates (in pixels from the top).
     * @param includePartial If true, will include pages that are partially visible.
     * @return the range of visible pages (may be an empty range).
     */
    public Range getPagesInWindow(Range intervalPx, boolean includePartial) {
        if (intervalPx.getFirst() > mPageBottoms.get(mSize - 1)) {
            return new Range(mSize + 1, mSize);
        }
        List<Integer> startList = includePartial ? mPageBottoms : mPageTops;
        List<Integer> endList = includePartial ? mPageTops : mPageBottoms;

        int topResult = Collections.binarySearch(startList, intervalPx.getFirst());
        int rangeStart = Math.abs(topResult + 1); // Insertion point.

        int bottomResult = Collections.binarySearch(endList, intervalPx.getLast());
        int rangeEnd = Math.abs(bottomResult + 1) - 1; // Before insertion point.

        if (rangeEnd < rangeStart) {
            // No page is entirely visible.
            int midPoint = (intervalPx.getFirst() + intervalPx.getLast()) / 2;
            int midResult = Collections.binarySearch(mPageTops, midPoint);
            int page = Math.max(Math.abs(midResult + 1) - 1, 0); // Before insertion point.
            return new Range(page, page);
        }

        return new Range(rangeStart, rangeEnd);
    }

    /** Return the estimated full height. */
    public int getEstimatedFullHeight() {
        if (!isInitialized()) {
            return 0;
        }
        // If we've rendered the entire document, we know exactly how tall we are
        if (mSize == mMaxPages) {
            return mPageStops[mSize - 1] + mPages[mSize - 1].getHeight() + 2 * mPageSpacingPx;
        }
        // Otherwise, we have to guess
        return (int) (mPageStops[mSize - 1] + (mEstimatedPageHeight + 2 * mPageSpacingPx) * (
                mMaxPages - mSize + 1));
    }

    /**
     * Updates the portion of this model that is visible on the screen, in this model's
     * coordinates -
     * so relative to (0, 0)-(getWidth(), getHeight()).
     */
    public void setViewArea(Rect viewArea) {
        mTempViewArea.set(viewArea);
        if (!mTempViewArea.intersect(
                0, 0, getWidth(), getEstimatedFullHeight())) { // Modifies tempViewArea.
            Log.w(TAG, String.format("PaginationModel is outside view area. %s", mTempViewArea));
        }
        if (!mTempViewArea.equals(this.mViewArea)) {
            this.mViewArea.set(mTempViewArea);
            notifyViewAreaChanged();
        }
    }

    /**
     * Returns the location of the page in the model.
     *
     * <p>Accounts for current viewArea so that pages will slide left or right to maximize the
     * portion of the page that is visible on the screen. Each page will be positioned:
     *
     * <ul>
     *   <li>vertically at the given <code>top</code> coordinates (between top and top + height),
     *   <li>horizontally between <code>0</code> and {@link #getWidth()} if possible, in a way that
     *       maximizes the portion of that view that is visible on the screen
     * </ul>
     *
     * @param pageNum - index of requested page
     * @return - coordinates of the page within this model
     */
    public Rect getPageLocation(int pageNum) {
        int left = 0;
        int right = getWidth();
        int top = mPageStops[pageNum];
        int bottom = top + mPages[pageNum].getHeight();
        int width = mPages[pageNum].getWidth();
        if (width < right - left) {
            // this page is smaller than the view's width, it may slide left or right.
            if (width < mViewArea.width()) {
                // page is smaller than the view: center (but respect min left margin)
                left = Math.max(left, mViewArea.left + (mViewArea.width() - width) / 2);
            } else {
                // page is larger than view: scroll proportionally between the margins.
                if (mViewArea.right > right) {
                    left = right - width;
                } else if (mViewArea.left > left) {
                    left = mViewArea.left * (right - width) / (right - mViewArea.width());
                }
            }
            right = left + width;
        }

        return new Rect(left, top, right, bottom);
    }

    /** Returns the width of this model which will be the width of the widest page in the model. */
    public int getWidth() {
        int width = 0;
        for (int p = 0; p < mSize; p++) {
            width = Math.max(width, mPages[p].getWidth());
        }
        return width;
    }

    /** Returns the Dimensions of page {@code pageNum}. */
    public Dimensions getPageSize(int pageNum) {
        return mPages[pageNum];
    }

    /** Returns the number of pages known to this model. */
    public int getSize() {
        return mSize;
    }

    /**
     * Returns the intersection of this model and the last viewArea that was reported to this model
     * via {@link #setViewArea(Rect)}.
     */
    public Rect getViewArea() {
        return mViewArea;
    }

    /** Notify all observers that the {@code viewArea} has changed. */
    private void notifyViewAreaChanged() {
        Iterator<PaginationModelObserver> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().onViewAreaChanged();
        }
    }

    /** Notify all observers that a page has been added to the model. */
    private void notifyPageAdded() {
        Iterator<PaginationModelObserver> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next().onPageAdded();
        }
    }

    // Useful for testing.
    public int getPageSpacingPx() {
        return mPageSpacingPx;
    }

    /**
     * Adds an observer to the set that will be notified of updates by this model.
     *
     * <p>Allows duplicate registration. Action will be a no-op if the observer is already
     * registered.
     */
    public void addObserver(PaginationModelObserver observer) {
        synchronized (mObservers) {
            mObservers.add(observer);
        }
    }

    /**
     * Removes an observer from the set that will be notified of updates by this model.
     *
     * <p>Allows removal of unregistered observers. Action will be a no-op if the observer was never
     * registered with this model.
     */
    public void removeObserver(PaginationModelObserver observer) {
        synchronized (mObservers) {
            mObservers.remove(observer);
        }
    }

    /**
     * Provides an iterator over a copy of the references in {@link #mObservers} so they can be
     * notified of updates without synchronizing on {@link #mObservers}.
     */
    public Iterator<PaginationModelObserver> iterator() {
        synchronized (mObservers) {
            return new ArrayList<PaginationModelObserver>(mObservers).iterator();
        }
    }

    /** Just makes sure to clear any observers that have been set. */
    @Override
    protected void finalize() throws Throwable {
        if (!mObservers.isEmpty()) {
            ErrorLog.log(TAG, String.format(
                    "PaginationModel still has %d registered observers during garabage "
                            + "collection. They" + " will be removed.", mObservers.size()));
        }
        mObservers.clear();
        super.finalize();
    }
}
