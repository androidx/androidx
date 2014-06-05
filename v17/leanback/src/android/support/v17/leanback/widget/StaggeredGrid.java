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

import android.support.v4.util.CircularArray;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * A dynamic data structure that maintains staggered grid position information
 * for each individual child. The algorithm ensures that each row will be kept
 * as balanced as possible when prepending and appending a child.
 *
 * <p>
 * You may keep view {@link StaggeredGrid.Location} inside StaggeredGrid as much
 * as possible since prepending and appending views is not symmetric: layout
 * going from 0 to N will likely produce a different result than layout going
 * from N to 0 for the staggered cases. If a user scrolls from 0 to N then
 * scrolls back to 0 and we don't keep history location information, edges of
 * the very beginning of rows will not be aligned. It is recommended to keep a
 * list of tens of thousands of {@link StaggeredGrid.Location}s which will be
 * big enough to remember a typical user's scroll history. There are situations
 * where StaggeredGrid falls back to the simple case where we do not need save a
 * huge list of locations inside StaggeredGrid:
 * <ul>
 *   <li>Only one row (e.g., a single row listview)</li>
 *   <li> Each item has the same length (not staggered at all)</li>
 * </ul>
 *
 * <p>
 * This class is abstract and can be replaced with different implementations.
 */
abstract class StaggeredGrid {

    /**
     * TODO: document this
     */
    public static interface Provider {
        /**
         * Return how many items are in the adapter.
         */
        public abstract int getCount();

        /**
         * Create the object at a given row.
         */
        public abstract void createItem(int index, int row, boolean append);
    }

    /**
     * Location of an item in the grid. For now it only saves row index but
     * more information may be added in the future.
     */
    public final static class Location {
        /**
         * The index of the row for this Location.
         */
        public final int row;

        /**
         * Create a Location with the given row index.
         */
        public Location(int row) {
            this.row = row;
        }
    }

    /**
     * TODO: document this
     */
    public final static class Row {
        /**
         * first view start location
         */
        public int low;
        /**
         * last view end location
         */
        public int high;
    }

    protected Provider mProvider;
    protected int mNumRows = 1; // mRows.length
    protected Row[] mRows;
    protected CircularArray<Location> mLocations = new CircularArray<Location>(64);
    private ArrayList<Integer>[] mTmpItemPositionsInRows;

    /**
     * A constant representing a default starting index, indicating that the
     * developer did not provide a start index.
     */
    public static final int START_DEFAULT = -1;

    // the first index that grid will layout
    protected int mStartIndex = START_DEFAULT;
    // the row to layout the first index
    protected int mStartRow = START_DEFAULT;

    protected int mFirstIndex = -1;

    /**
     * Sets the {@link Provider} for this staggered grid.
     *
     * @param provider The provider for this staggered grid.
     */
    public void setProvider(Provider provider) {
        mProvider = provider;
    }

    /**
     * Sets the array of {@link Row}s to fill into. For views that represent a
     * horizontal list, this will be the rows of the view. For views that
     * represent a vertical list, this will be the columns.
     *
     * @param row The array of {@link Row}s to be filled.
     */
    public final void setRows(Row[] row) {
        if (row == null || row.length == 0) {
            throw new IllegalArgumentException();
        }
        mNumRows = row.length;
        mRows = row;
        mTmpItemPositionsInRows = new ArrayList[mNumRows];
        for (int i = 0; i < mNumRows; i++) {
            mTmpItemPositionsInRows[i] = new ArrayList(32);
        }
    }

    /**
     * Returns the number of rows in the staggered grid.
     */
    public final int getNumRows() {
        return mNumRows;
    }

    /**
     * Set the first item index and the row index to load when there are no
     * items.
     *
     * @param startIndex the index of the first item
     * @param startRow the index of the row
     */
    public final void setStart(int startIndex, int startRow) {
        mStartIndex = startIndex;
        mStartRow = startRow;
    }

    /**
     * Returns the first index in the staggered grid.
     */
    public final int getFirstIndex() {
        return mFirstIndex;
    }

    /**
     * Returns the last index in the staggered grid.
     */
    public final int getLastIndex() {
        return mFirstIndex + mLocations.size() - 1;
    }

    /**
     * Returns the size of the saved {@link Location}s.
     */
    public final int getSize() {
        return mLocations.size();
    }

    /**
     * Returns the {@link Location} at the given index.
     */
    public final Location getLocation(int index) {
        if (mLocations.size() == 0) {
            return null;
        }
        return mLocations.get(index - mFirstIndex);
    }

    /**
     * Removes the first element.
     */
    public final void removeFirst() {
        mFirstIndex++;
        mLocations.popFirst();
    }

    /**
     * Removes the last element.
     */
    public final void removeLast() {
        mLocations.popLast();
    }

    public final void debugPrint(PrintWriter pw) {
        for (int i = 0, size = mLocations.size(); i < size; i++) {
            Location loc = mLocations.get(i);
            pw.print("<" + (mFirstIndex + i) + "," + loc.row + ">");
            pw.print(" ");
            pw.println();
        }
    }

    protected final int getMaxHighRowIndex() {
        int maxHighRowIndex = 0;
        for (int i = 1; i < mNumRows; i++) {
            if (mRows[i].high > mRows[maxHighRowIndex].high) {
                maxHighRowIndex = i;
            }
        }
        return maxHighRowIndex;
    }

    protected final int getMinHighRowIndex() {
        int minHighRowIndex = 0;
        for (int i = 1; i < mNumRows; i++) {
            if (mRows[i].high < mRows[minHighRowIndex].high) {
                minHighRowIndex = i;
            }
        }
        return minHighRowIndex;
    }

    protected final Location appendItemToRow(int itemIndex, int rowIndex) {
        Location loc = new Location(rowIndex);
        if (mLocations.size() == 0) {
            mFirstIndex = itemIndex;
        }
        mLocations.addLast(loc);
        mProvider.createItem(itemIndex, rowIndex, true);
        return loc;
    }

    /**
     * Append items until the high edge reaches upTo.
     */
    public abstract void appendItems(int upTo);

    protected final int getMaxLowRowIndex() {
        int maxLowRowIndex = 0;
        for (int i = 1; i < mNumRows; i++) {
            if (mRows[i].low > mRows[maxLowRowIndex].low) {
                maxLowRowIndex = i;
            }
        }
        return maxLowRowIndex;
    }

    protected final int getMinLowRowIndex() {
        int minLowRowIndex = 0;
        for (int i = 1; i < mNumRows; i++) {
            if (mRows[i].low < mRows[minLowRowIndex].low) {
                minLowRowIndex = i;
            }
        }
        return minLowRowIndex;
    }

    protected final Location prependItemToRow(int itemIndex, int rowIndex) {
        Location loc = new Location(rowIndex);
        mFirstIndex = itemIndex;
        mLocations.addFirst(loc);
        mProvider.createItem(itemIndex, rowIndex, false);
        return loc;
    }

    /**
     * Return array of Lists for all rows, each List contains item positions
     * on that row between startPos(included) and endPositions(included).
     * Returned value is read only, do not change it.
     */
    public final List<Integer>[] getItemPositionsInRows(int startPos, int endPos) {
        for (int i = 0; i < mNumRows; i++) {
            mTmpItemPositionsInRows[i].clear();
        }
        if (startPos >= 0) {
            for (int i = startPos; i <= endPos; i++) {
                mTmpItemPositionsInRows[getLocation(i).row].add(i);
            }
        }
        return mTmpItemPositionsInRows;
    }

    /**
     * Prepend items until the low edge reaches downTo.
     */
    public abstract void prependItems(int downTo);

    /**
     * Strip items, keep a contiguous subset of items; the subset should include
     * at least one item on every row that currently has at least one item.
     *
     * <p>
     * TODO: document this better
     */
    public abstract void stripDownTo(int itemIndex);
}
