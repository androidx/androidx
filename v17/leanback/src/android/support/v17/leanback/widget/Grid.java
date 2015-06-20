/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.support.v4.util.CircularIntArray;
import android.util.Log;

import java.io.PrintWriter;

/**
 * A grid is representation of single or multiple rows layout data structure and algorithm.
 * Grid is the base class for single row, non-staggered grid and staggered grid.
 * <p>
 * To use the Grid, user must implement a Provider to create or remove visible item.
 * Grid maintains a list of visible items.  Visible items are created when
 * user calls appendVisibleItems() or prependVisibleItems() with certain limitation
 * (e.g. a max edge that append up to).  Visible items are deleted when user calls
 * removeInvisibleItemsAtEnd() or removeInvisibleItemsAtFront().  Grid's algorithm
 * uses size of visible item returned from Provider.createItem() to decide which row
 * to add a new visible item and may cache the algorithm results.   User must call
 * invalidateItemsAfter() when it detects item size changed to ask Grid to remove cached
 * results.
 */
abstract class Grid {

    /**
     * A constant representing a default starting index, indicating that the
     * developer did not provide a start index.
     */
    public static final int START_DEFAULT = -1;

    /**
     * When user uses Grid,  he should provide count of items and
     * the method to create item and remove item.
     */
    public static interface Provider {

        /**
         * Return how many items (are in the adapter).
         */
        public abstract int getCount();

        /**
         * Create visible item and where the provider should measure it.
         * The call is always followed by addItem().
         * @param index     0-based index of the item in provider
         * @param append  True if new item is after last visible item, false if new item is
         *                before first visible item.
         * @param item    item[0] returns created item that will be passed in addItem() call.
         * @return length of the item.
         */
        public abstract int createItem(int index, boolean append, Object[] item);

        /**
         * add item to given row and given edge.  The call is always after createItem().
         * @param item      The object returned by createItem()
         * @param index     0-based index of the item in provider
         * @param length    The size of the object
         * @param rowIndex  Row index to put the item
         * @param edge      min_edge if not reversed or max_edge if reversed.
         */
        public abstract void addItem(Object item, int index, int length, int rowIndex, int edge);

        /**
         * Remove visible item at index.
         * @param index     0-based index of the item in provider
         */
        public abstract void removeItem(int index);

        /**
         * Get edge of an existing visible item. edge will be the min_edge
         * if not reversed or the max_edge if reversed.
         * @param index     0-based index of the item in provider
         */
        public abstract int getEdge(int index);

        /**
         * Get size of an existing visible item.
         * @param index     0-based index of the item in provider
         */
        public abstract int getSize(int index);
    }

    /**
     * Cached representation of an item in Grid.  May be subclassed.
     */
    public static class Location {
        /**
         * The index of the row for this Location.
         */
        public int row;

        public Location(int row) {
            this.row = row;
        }
    }

    protected Provider mProvider;
    protected boolean mReversedFlow;
    protected int mMargin;
    protected int mNumRows;
    protected int mFirstVisibleIndex = -1;
    protected int mLastVisibleIndex = -1;

    protected CircularIntArray[] mTmpItemPositionsInRows;

    // the first index that grid will layout
    protected int mStartIndex = START_DEFAULT;

    /**
     * Creates a single or multiple rows (can be staggered or not staggered) grid
     */
    public static Grid createGrid(int rows) {
        Grid grid;
        if (rows == 1) {
            grid = new SingleRow();
        } else {
            // TODO support non staggered multiple rows grid
            grid = new StaggeredGridDefault();
            grid.setNumRows(rows);
        }
        return grid;
    }

    /**
     * Sets the margin between items in a row
     */
    public final void setMargin(int margin) {
        mMargin = margin;
    }

    /**
     * Sets if reversed flow (rtl)
     */
    public final void setReversedFlow(boolean reversedFlow) {
        mReversedFlow = reversedFlow;
    }

    /**
     * Returns true if reversed flow (rtl)
     */
    public boolean isReversedFlow() {
        return mReversedFlow;
    }

    /**
     * Sets the {@link Provider} for this grid.
     *
     * @param provider The provider for this grid.
     */
    public void setProvider(Provider provider) {
        mProvider = provider;
    }

    /**
     * Sets the first item index to create when there are no items.
     *
     * @param startIndex the index of the first item
     */
    public void setStart(int startIndex) {
        mStartIndex = startIndex;
    }

    /**
     * Returns the number of rows in the grid.
     */
    public int getNumRows() {
        return mNumRows;
    }

    /**
     * Sets number of rows to fill into. For views that represent a
     * horizontal list, this will be the rows of the view. For views that
     * represent a vertical list, this will be the columns.
     *
     * @param numRows numberOfRows
     */
    void setNumRows(int numRows) {
        if (numRows <= 0) {
            throw new IllegalArgumentException();
        }
        if (mNumRows == numRows) {
            return;
        }
        mNumRows = numRows;
        mTmpItemPositionsInRows = new CircularIntArray[mNumRows];
        for (int i = 0; i < mNumRows; i++) {
            mTmpItemPositionsInRows[i] = new CircularIntArray();
        }
    }

    /**
     * Returns index of first visible item in the staggered grid.  Returns negative value
     * if no visible item.
     */
    public final int getFirstVisibleIndex() {
        return mFirstVisibleIndex;
    }

    /**
     * Returns index of last visible item in the staggered grid.  Returns negative value
     * if no visible item.
     */
    public final int getLastVisibleIndex() {
        return mLastVisibleIndex;
    }

    /**
     * Reset visible indices and keep cache (if exists)
     */
    public void resetVisibleIndex() {
        mFirstVisibleIndex = mLastVisibleIndex = -1;
    }

    /**
     * Invalidate items after or equal to index. This will remove visible items
     * after that and invalidate cache of layout results after that.
     */
    public void invalidateItemsAfter(int index) {
        if (index < 0) {
            return;
        }
        if (mLastVisibleIndex < 0) {
            return;
        }
        while (mLastVisibleIndex >= index) {
            mProvider.removeItem(mLastVisibleIndex);
            mLastVisibleIndex--;
        }
        resetVisbileIndexIfEmpty();
        if (getFirstVisibleIndex() < 0) {
            setStart(index);
        }
    }

    /**
     * Gets the row index of item at given index.
     */
    public final int getRowIndex(int index) {
        return getLocation(index).row;
    }

    /**
     * Gets {@link Location} of item.  The return object is read only and temporarily.
     */
    public abstract Location getLocation(int index);

    /**
     * Finds the largest or smallest row min edge of visible items,
     * the row index is returned in indices[0], the item index is returned in indices[1].
     */
    public final int findRowMin(boolean findLarge, int[] indices) {
        return findRowMin(findLarge, mReversedFlow ? mLastVisibleIndex : mFirstVisibleIndex,
                indices);
    }

    /**
     * Finds the largest or smallest row min edge of visible items, starts searching from
     * indexLimit, the row index is returned in indices[0], the item index is returned in indices[1].
     */
    protected abstract int findRowMin(boolean findLarge, int indexLimit, int[] rowIndex);

    /**
     * Finds the largest or smallest row max edge of visible items, the row index is returned in
     * indices[0], the item index is returned in indices[1].
     */
    public final int findRowMax(boolean findLarge, int[] indices) {
        return findRowMax(findLarge, mReversedFlow ? mFirstVisibleIndex : mLastVisibleIndex,
                indices);
    }

    /**
     * Find largest or smallest row max edge of visible items, starts searching from indexLimit,
     * the row index is returned in indices[0], the item index is returned in indices[1].
     */
    protected abstract int findRowMax(boolean findLarge, int indexLimit, int[] indices);

    /**
     * Returns true if appending item has reached "toLimit"
     */
    protected final boolean checkAppendOverLimit(int toLimit) {
        if (mLastVisibleIndex < 0) {
            return false;
        }
        return mReversedFlow ? findRowMin(true, null) <= toLimit + mMargin :
                    findRowMax(false, null) >= toLimit - mMargin;
    }

    /**
     * Returns true if prepending item has reached "toLimit"
     */
    protected final boolean checkPrependOverLimit(int toLimit) {
        if (mLastVisibleIndex < 0) {
            return false;
        }
        return mReversedFlow ? findRowMax(false, null) >= toLimit + mMargin :
                    findRowMin(true, null) <= toLimit - mMargin;
    }

    /**
     * Return array of int array for all rows, each int array contains visible item positions
     * in pair on that row between startPos(included) and endPositions(included).
     * Returned value is read only, do not change it.
     * <p>
     * E.g. First row has 3,7,8, second row has 4,5,6.
     * getItemPositionsInRows(3, 8) returns { {3,3,7,8}, {4,6} }
     */
    public abstract CircularIntArray[] getItemPositionsInRows(int startPos, int endPos);

    /**
     * Return array of int array for all rows, each int array contains visible item positions
     * in pair on that row.
     * Returned value is read only, do not change it.
     * <p>
     * E.g. First row has 3,7,8, second row has 4,5,6  { {3,3,7,8}, {4,6} }
     */
    public final CircularIntArray[] getItemPositionsInRows() {
        return getItemPositionsInRows(getFirstVisibleIndex(), getLastVisibleIndex());
    }

    /**
     * Prepends items and stops after one column is filled.
     * (i.e. filled items from row 0 to row mNumRows - 1)
     * @return true if at least one item is filled.
     */
    public final boolean prependOneColumnVisibleItems() {
        return prependVisibleItems(mReversedFlow ? Integer.MIN_VALUE : Integer.MAX_VALUE, true);
    }

    /**
     * Prepends items until first item or reaches toLimit (min edge when not reversed or
     * max edge when reversed)
     */
    public final void prependVisibleItems(int toLimit) {
        prependVisibleItems(toLimit, false);
    }

    /**
     * Prepends items until first item or reaches toLimit (min edge when not reversed or
     * max edge when reversed).
     * @param oneColumnMode  true when fills one column and stops,  false
     * when checks if condition matches before filling first column.
     * @return true if at least one item is filled.
     */
    protected abstract boolean prependVisibleItems(int toLimit, boolean oneColumnMode);

    /**
     * Appends items and stops after one column is filled.
     * (i.e. filled items from row 0 to row mNumRows - 1)
     * @return true if at least one item is filled.
     */
    public boolean appendOneColumnVisibleItems() {
        return appendVisibleItems(mReversedFlow ? Integer.MAX_VALUE : Integer.MIN_VALUE, true);
    }

    /**
     * Append items until last item or reaches toLimit (max edge when not
     * reversed or min edge when reversed)
     */
    public final void appendVisibleItems(int toLimit) {
        appendVisibleItems(toLimit, false);
    }

    /**
     * Appends items until last or reaches toLimit (high edge when not
     * reversed or low edge when reversed).
     * @param oneColumnMode True when fills one column and stops,  false
     * when checks if condition matches before filling first column.
     * @return true if filled at least one item
     */
    protected abstract boolean appendVisibleItems(int toLimit, boolean oneColumnMode);

    /**
     * Removes invisible items from end until reaches item at aboveIndex or toLimit.
     */
    public void removeInvisibleItemsAtEnd(int aboveIndex, int toLimit) {
        while(mLastVisibleIndex >= mFirstVisibleIndex && mLastVisibleIndex > aboveIndex) {
            boolean offEnd = !mReversedFlow ? mProvider.getEdge(mLastVisibleIndex) >= toLimit
                    : mProvider.getEdge(mLastVisibleIndex) <= toLimit;
            if (offEnd) {
                mProvider.removeItem(mLastVisibleIndex);
                mLastVisibleIndex--;
            } else {
                break;
            }
        }
        resetVisbileIndexIfEmpty();
    }

    /**
     * Removes invisible items from front until reaches item at belowIndex or toLimit.
     */
    public void removeInvisibleItemsAtFront(int belowIndex, int toLimit) {
        while(mLastVisibleIndex >= mFirstVisibleIndex && mFirstVisibleIndex < belowIndex) {
            boolean offFront = !mReversedFlow ? mProvider.getEdge(mFirstVisibleIndex)
                    + mProvider.getSize(mFirstVisibleIndex) <= toLimit
                    : mProvider.getEdge(mFirstVisibleIndex)
                            - mProvider.getSize(mFirstVisibleIndex) >= toLimit;
            if (offFront) {
                mProvider.removeItem(mFirstVisibleIndex);
                mFirstVisibleIndex++;
            } else {
                break;
            }
        }
        resetVisbileIndexIfEmpty();
    }

    private void resetVisbileIndexIfEmpty() {
        if (mLastVisibleIndex < mFirstVisibleIndex) {
            resetVisibleIndex();
        }
    }

    public abstract void debugPrint(PrintWriter pw);
}
