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
package androidx.leanback.widget;

import androidx.collection.CircularIntArray;
import androidx.recyclerview.widget.GridLayoutManager;

import java.io.PrintWriter;

/**
 * A grid implementation that supports non-staggered layout and spans.
 */
class StandardGrid extends Grid {

    private final Location mTmpLocation = new Location(0);
    private Object[] mTmpItems;
    private int[] mTmpItemsSize;

    SpanSupport mSpanSupport;

    StandardGrid() {
        setNumRows(1);
    }

    void setSpanSizeLookup(GridLayoutManager.SpanSizeLookup spanSizeLookup) {
        if (spanSizeLookup != null
                && !(spanSizeLookup instanceof GridLayoutManager.DefaultSpanSizeLookup)) {
            mSpanSupport = new SpanSupport(spanSizeLookup);
            mSearchFocusInNextSpanGroup = true;
        } else {
            mSpanSupport = null;
            mSearchFocusInNextSpanGroup = false;
        }
    }

    @Override
    void setNumRows(int numRows) {
        if (mNumRows == numRows) {
            return;
        }
        super.setNumRows(numRows);
        mTmpItems = new Object[numRows];
        mTmpItemsSize = new int[numRows];
    }

    private int getSpanIndex(int index) {
        if (mSpanSupport != null) {
            return mSpanSupport.getCachedSpanIndex(index, mNumRows);
        }
        return index % mNumRows;
    }

    int getSpanGroupIndex(int index) {
        if (mSpanSupport != null) {
            return mSpanSupport.getCachedSpanGroupIndex(index, mNumRows);
        }
        return index / mNumRows;
    }

    private int getSpanSize(int index) {
        if (mSpanSupport != null) {
            return mSpanSupport.getSpanSize(index, mNumRows);
        }
        return 1;
    }

    @Override
    public Location getLocation(int index) {
        mTmpLocation.mRow = getSpanIndex(index);
        return mTmpLocation;
    }

    @Override
    protected int findRowMin(int indexLimit, int[] indices) {
        int value = Integer.MAX_VALUE;
        int rowIndex = -1;
        int itemIndex = -1;

        int groupIndex = getSpanGroupIndex(indexLimit);
        int start = indexLimit;
        int end = indexLimit;
        if (mReversedFlow) {
            // Search from indexLimit (mLastVisibleIndex) backward.
            while (start - 1 >= mFirstVisibleIndex && getSpanGroupIndex(start - 1) == groupIndex) {
                start--;
            }
        } else {
            // Search from indexLimit (mFirstVisibleIndex) forward.
            while (end + 1 <= mLastVisibleIndex && getSpanGroupIndex(end + 1) == groupIndex) {
                end++;
            }
        }

        for (int i = start; i <= end; i++) {
            int edge = mProvider.getEdge(i);
            if (mReversedFlow) {
                edge -= mProvider.getSize(i);
            }
            if (edge < value) {
                value = edge;
                rowIndex = getSpanIndex(i);
                itemIndex = i;
            }
        }
        if (indices != null) {
            indices[0] = rowIndex;
            indices[1] = itemIndex;
        }
        return value;
    }

    @Override
    protected int findRowMax(int indexLimit, int[] indices) {
        int value = Integer.MIN_VALUE;
        int rowIndex = -1;
        int itemIndex = -1;

        int groupIndex = getSpanGroupIndex(indexLimit);
        int start = indexLimit;
        int end = indexLimit;
        if (mReversedFlow) {
            // Search from indexLimit (mFirstVisibleIndex) forward.
            while (end + 1 <= mLastVisibleIndex && getSpanGroupIndex(end + 1) == groupIndex) {
                end++;
            }
        } else {
            // Search from indexLimit (mLastVisibleIndex) backward.
            while (start - 1 >= mFirstVisibleIndex && getSpanGroupIndex(start - 1) == groupIndex) {
                start--;
            }
        }

        for (int i = start; i <= end; i++) {
            int edge = mProvider.getEdge(i);
            if (!mReversedFlow) {
                edge += mProvider.getSize(i);
            }
            if (edge > value) {
                value = edge;
                rowIndex = getSpanIndex(i);
                itemIndex = i;
            }
        }
        if (indices != null) {
            indices[0] = rowIndex;
            indices[1] = itemIndex;
        }
        return value;
    }

    @Override
    public CircularIntArray[] getItemPositionsInRows(int startPos, int endPos) {
        for (int i = 0; i < mNumRows; i++) {
            mTmpItemPositionsInRows[i].clear();
        }
        if (startPos >= 0) {
            for (int i = startPos; i <= endPos; i++) {
                int row = getSpanIndex(i);
                CircularIntArray rowArr = mTmpItemPositionsInRows[row];
                if (!rowArr.isEmpty() && rowArr.getLast() == i - 1) {
                    rowArr.popLast();
                    rowArr.addLast(i);
                } else {
                    rowArr.addLast(i);
                    rowArr.addLast(i);
                }
            }
        }
        return mTmpItemPositionsInRows;
    }

    @Override
    protected boolean prependVisibleItems(int toLimit, boolean oneColumnMode) {
        if (mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkPrependOverLimit(toLimit)) {
            return false;
        }
        int minIndex = mProvider.getMinIndex();
        int index = getStartIndexForPrepend();
        boolean filledOne = false;

        while (index >= minIndex) {
            int groupIndex = getSpanGroupIndex(index);
            int nextGroupIndex = (index < mLastVisibleIndex) ? getSpanGroupIndex(index + 1) : -1;

            if (index < mLastVisibleIndex && groupIndex == nextGroupIndex) {
                // Same row as index + 1
                int edge = mProvider.getEdge(index + 1);
                int size = mProvider.createItem(index, getSpanSize(index), false, mTmpItem, false);
                mProvider.addItem(mTmpItem[0], index, size, getSpanIndex(index), edge);
                filledOne = true;
                mFirstVisibleIndex = index;
                index--;
            } else {
                // New row (above/left)
                int startOfRow = index;
                while (startOfRow > minIndex && getSpanGroupIndex(startOfRow - 1) == groupIndex) {
                    startOfRow--;
                }

                // Create items and calculate max size
                int maxSize = 0;
                int tmpItemIndex = 0;
                for (int i = index; i >= startOfRow; i--) {
                    int size = mProvider.createItem(i, getSpanSize(i), false, mTmpItem, false);
                    mTmpItems[tmpItemIndex] = mTmpItem[0];
                    mTmpItemsSize[tmpItemIndex] = size;
                    tmpItemIndex++;
                    if (size > maxSize) {
                        maxSize = size;
                    }
                }

                // Calculate edge
                int refEdge = mReversedFlow ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                int firstRowGroupIndex = getSpanGroupIndex(mFirstVisibleIndex);
                for (int i = mFirstVisibleIndex; i <= mLastVisibleIndex; i++) {
                    if (getSpanGroupIndex(i) != firstRowGroupIndex) {
                        break;
                    }
                    int e = mProvider.getEdge(i);
                    if (mReversedFlow) {
                        if (e > refEdge) refEdge = e;
                    } else {
                        if (e < refEdge) refEdge = e;
                    }
                }
                if (mReversedFlow) {
                    refEdge = refEdge + maxSize + mSpacing;
                } else {
                    refEdge = refEdge - maxSize - mSpacing;
                }
                // Add items
                tmpItemIndex = 0;
                for (int i = index; i >= startOfRow; i--) {
                    mProvider.addItem(mTmpItems[tmpItemIndex], i, mTmpItemsSize[tmpItemIndex],
                            getSpanIndex(i), refEdge, /* finishedCreateItems= */ i == startOfRow);
                    mTmpItems[tmpItemIndex] = null;
                    tmpItemIndex++;
                }

                filledOne = true;
                mFirstVisibleIndex = startOfRow;
                index = startOfRow - 1;
            }

            if (oneColumnMode || checkPrependOverLimit(toLimit)) {
                // Check if we finished a column (group)
                // If the next item (index) is in a different group, we finished the current group.
                if (index < minIndex || getSpanGroupIndex(index) != groupIndex) {
                    break;
                }
            }
        }
        return filledOne;
    }

    @Override
    protected boolean appendVisibleItems(int toLimit, boolean oneColumnMode) {
        if (mProvider.getCount() == 0) {
            return false;
        }
        if (!oneColumnMode && checkAppendOverLimit(toLimit)) {
            return false;
        }
        int count = mProvider.getCount();
        int index = getStartIndexForAppend();
        boolean filledOne = false;

        while (index < count) {
            final int groupIndex = getSpanGroupIndex(index);
            final int prevGroupIndex = (index > mFirstVisibleIndex && index > 0)
                    ? getSpanGroupIndex(index - 1) : -1;
            int edge;

            if (mLastVisibleIndex < 0) {
                if (mReversedFlow) {
                    edge = Integer.MAX_VALUE;
                } else {
                    edge = Integer.MIN_VALUE;
                }
            } else if (index > mFirstVisibleIndex && groupIndex == prevGroupIndex) {
                // Same row as previous item
                edge = mProvider.getEdge(index - 1);
            } else {
                // Start of a new row
                int maxEnd = mReversedFlow ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                // Find max edge of previous row
                // Iterate backwards from index-1 until group changes
                for (int i = index - 1; i >= mFirstVisibleIndex; i--) {
                    if (getSpanGroupIndex(i) != prevGroupIndex) {
                        break;
                    }
                    int end;
                    if (mReversedFlow) {
                        end = mProvider.getEdge(i) - mProvider.getSize(i);
                        if (end < maxEnd) {
                            maxEnd = end;
                        }
                    } else {
                        end = mProvider.getEdge(i) + mProvider.getSize(i);
                        if (end > maxEnd) {
                            maxEnd = end;
                        }
                    }
                }

                if (mReversedFlow) {
                    edge = maxEnd - mSpacing;
                } else {
                    edge = maxEnd + mSpacing;
                }
            }

            int size = mProvider.createItem(index, getSpanSize(index), true, mTmpItem, false);
            mProvider.addItem(mTmpItem[0], index, size, getSpanIndex(index), edge);
            filledOne = true;
            mLastVisibleIndex = index;
            if (mFirstVisibleIndex < 0) {
                mFirstVisibleIndex = index;
            }
            index++;

            if (oneColumnMode || checkAppendOverLimit(toLimit)) {
                // Check if we finished a column (group)
                // If the next item (index) is in a different group, we finished the current group.
                if (index >= count || getSpanGroupIndex(index) != groupIndex) {
                    break;
                }
            }
        }
        return filledOne;
    }

    private int getStartIndexForAppend() {
        if (mLastVisibleIndex >= 0) {
            return mLastVisibleIndex + 1;
        } else if (mStartIndex != START_DEFAULT) {
            return Math.min(mStartIndex, mProvider.getCount() - 1);
        } else {
            return 0;
        }
    }

    private int getStartIndexForPrepend() {
        if (mFirstVisibleIndex >= 0) {
            return mFirstVisibleIndex - 1;
        } else if (mStartIndex != START_DEFAULT) {
            return Math.min(mStartIndex, mProvider.getCount() - 1);
        } else {
            return mProvider.getCount() - 1;
        }
    }

    @Override
    public void debugPrint(PrintWriter pw) {
        pw.print("StandardGrid<");
        pw.print(mFirstVisibleIndex);
        pw.print(",");
        pw.print(mLastVisibleIndex);
        pw.print(">");
        pw.println();
    }
}
