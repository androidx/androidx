/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;

import androidx.recyclerview.selection.SelectionTracker.SelectionPredicate;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a band selection item model for views within a RecyclerView. This class queries the
 * RecyclerView to determine where its items are placed; then, once band selection is underway,
 * it alerts listeners of which items are covered by the selections.
 *
 * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
 */
final class GridModel<K> {

    // Magical value indicating that a value has not been previously set. primitive null :)
    static final int NOT_SET = -1;

    // Enum values used to determine the corner at which the origin is located within the
    private static final int UPPER = 0x00;
    private static final int LOWER = 0x01;
    private static final int LEFT = 0x00;
    private static final int RIGHT = 0x02;
    private static final int UPPER_LEFT = UPPER | LEFT;
    private static final int UPPER_RIGHT = UPPER | RIGHT;
    private static final int LOWER_LEFT = LOWER | LEFT;
    private static final int LOWER_RIGHT = LOWER | RIGHT;

    private final GridHost<K> mHost;
    private final ItemKeyProvider<K> mKeyProvider;
    private final SelectionPredicate<K> mSelectionPredicate;

    private final List<SelectionObserver<K>> mOnSelectionChangedListeners = new ArrayList<>();

    // Map from the x-value of the left side of a SparseBooleanArray of adapter positions, keyed
    // by their y-offset. For example, if the first column of the view starts at an x-value of 5,
    // mColumns.get(5) would return an array of positions in that column. Within that array, the
    // value for key y is the adapter position for the item whose y-offset is y.
    private final SparseArray<SparseIntArray> mColumns = new SparseArray<>();

    // List of limits along the x-axis (columns).
    // This list is sorted from furthest left to furthest right.
    private final List<Limits> mColumnBounds = new ArrayList<>();

    // List of limits along the y-axis (rows). Note that this list only contains items which
    // have been in the viewport.
    private final List<Limits> mRowBounds = new ArrayList<>();

    // The adapter positions which have been recorded so far.
    private final SparseBooleanArray mKnownPositions = new SparseBooleanArray();

    // Array passed to registered OnSelectionChangedListeners. One array is created and reused
    // throughout the lifetime of the object.
    private final Set<K> mSelection = new LinkedHashSet<>();

    // The current pointer (in absolute positioning from the top of the view).
    private Point mPointer;

    // The bounds of the band selection.
    private RelativePoint mRelOrigin;
    private RelativePoint mRelPointer;

    private boolean mIsActive;

    // Tracks where the band select originated from. This is used to determine where selections
    // should expand from when Shift+click is used.
    private int mPositionNearestOrigin = NOT_SET;

    private final OnScrollListener mScrollListener;

    @SuppressWarnings("unchecked")
    GridModel(
            GridHost<K> host,
            ItemKeyProvider<K> keyProvider,
            SelectionPredicate<K> selectionPredicate) {

        checkArgument(host != null);
        checkArgument(keyProvider != null);
        checkArgument(selectionPredicate != null);

        mHost = host;
        mKeyProvider = keyProvider;
        mSelectionPredicate = selectionPredicate;

        mScrollListener = new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                GridModel.this.onScrolled(recyclerView, dx, dy);
            }
        };

        mHost.addOnScrollListener(mScrollListener);
    }

    /**
     * Start a band select operation at the given point.
     *
     * @param relativeOrigin The origin of the band select operation, relative to the viewport.
     *                       For example, if the view is scrolled to the bottom, the top-left of
     *                       the
     *                       viewport
     *                       would have a relative origin of (0, 0), even though its absolute point
     *                       has a higher
     *                       y-value.
     */
    void startCapturing(Point relativeOrigin) {
        recordVisibleChildren();
        if (isEmpty()) {
            // The selection band logic works only if there is at least one visible child.
            return;
        }

        mIsActive = true;
        mPointer = mHost.createAbsolutePoint(relativeOrigin);

        mRelOrigin = createRelativePoint(mPointer);
        mRelPointer = createRelativePoint(mPointer);
        computeCurrentSelection();
        notifySelectionChanged();
    }

    /**
     * Ends the band selection.
     */
    void stopCapturing() {
        mIsActive = false;
    }

    /**
     * Resizes the selection by adjusting the pointer (i.e., the corner of the selection
     * opposite the origin.
     *
     * @param relativePointer The pointer (opposite of the origin) of the band select operation,
     *                        relative to the viewport. For example, if the view is scrolled to the
     *                        bottom, the
     *                        top-left of the viewport would have a relative origin of (0, 0), even
     *                        though its
     *                        absolute point has a higher y-value.
     */
    void resizeSelection(Point relativePointer) {
        mPointer = mHost.createAbsolutePoint(relativePointer);
        // Should probably never been empty at this point, yet we guard against
        // known exceptions because wholesome goodness.
        if (!isEmpty()) {
            updateModel();
        }
    }

    /**
     * @return The adapter position for the item nearest the origin corresponding to the latest
     * band select operation, or NOT_SET if the selection did not cover any items.
     */
    int getPositionNearestOrigin() {
        return mPositionNearestOrigin;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (!mIsActive) {
            return;
        }

        mPointer.x += dx;
        mPointer.y += dy;
        recordVisibleChildren();

        // Should probably never been empty at this point, yet we guard against
        // known exceptions because wholesome goodness.
        if (!isEmpty()) {
            updateModel();
        }
    }

    /**
     * Queries the view for all children and records their location metadata.
     */
    private void recordVisibleChildren() {
        for (int i = 0; i < mHost.getVisibleChildCount(); i++) {
            int adapterPosition = mHost.getAdapterPositionAt(i);
            // Sometimes the view is not attached, as we notify the multi selection manager
            // synchronously, while views are attached asynchronously. As a result items which
            // are in the adapter may not actually have a corresponding view (yet).
            if (mHost.hasView(adapterPosition)
                    && mSelectionPredicate.canSetStateAtPosition(adapterPosition, true)
                    && !mKnownPositions.get(adapterPosition)) {
                mKnownPositions.put(adapterPosition, true);
                recordItemData(mHost.getAbsoluteRectForChildViewAt(i), adapterPosition);
            }
        }
    }

    /**
     * Checks if there are any recorded children.
     */
    private boolean isEmpty() {
        return mColumnBounds.size() == 0 || mRowBounds.size() == 0;
    }

    /**
     * Updates the limits lists and column map with the given item metadata.
     *
     * @param absoluteChildRect The absolute rectangle for the child view being processed.
     * @param adapterPosition   The position of the child view being processed.
     */
    private void recordItemData(Rect absoluteChildRect, int adapterPosition) {
        if (mColumnBounds.size() != mHost.getColumnCount()) {
            // If not all x-limits have been recorded, record this one.
            recordLimits(
                    mColumnBounds, new Limits(absoluteChildRect.left, absoluteChildRect.right));
        }

        recordLimits(mRowBounds, new Limits(absoluteChildRect.top, absoluteChildRect.bottom));

        SparseIntArray columnList = mColumns.get(absoluteChildRect.left);
        if (columnList == null) {
            columnList = new SparseIntArray();
            mColumns.put(absoluteChildRect.left, columnList);
        }
        columnList.put(absoluteChildRect.top, adapterPosition);
    }

    /**
     * Ensures limits exists within the sorted list limitsList, and adds it to the list if it
     * does not exist.
     */
    private void recordLimits(List<Limits> limitsList, Limits limits) {
        int index = Collections.binarySearch(limitsList, limits);
        if (index < 0) {
            limitsList.add(~index, limits);
        }
    }

    /**
     * Handles a moved pointer; this function determines whether the pointer movement resulted
     * in a selection change and, if it has, notifies listeners of this change.
     */
    private void updateModel() {
        checkState(!isEmpty());
        RelativePoint old = mRelPointer;

        mRelPointer = createRelativePoint(mPointer);
        if (mRelPointer.equals(old)) {
            return;
        }

        computeCurrentSelection();
        notifySelectionChanged();
    }

    /**
     * Computes the currently-selected items.
     */
    private void computeCurrentSelection() {
        if (areItemsCoveredByBand(mRelPointer, mRelOrigin)) {
            updateSelection(computeBounds());
        } else {
            mSelection.clear();
            mPositionNearestOrigin = NOT_SET;
        }
    }

    /**
     * Notifies all listeners of a selection change. Note that this function simply passes
     * mSelection, so computeCurrentSelection() should be called before this
     * function.
     */
    @SuppressWarnings("unchecked")
    private void notifySelectionChanged() {
        for (SelectionObserver<K> listener : mOnSelectionChangedListeners) {
            listener.onSelectionChanged(mSelection);
        }
    }

    /**
     * @param rect Rectangle including all covered items.
     */
    private void updateSelection(Rect rect) {
        int columnStart =
                Collections.binarySearch(mColumnBounds, new Limits(rect.left, rect.left));

        checkArgument(columnStart >= 0, "Rect doesn't intesect any known column.");

        int columnEnd = columnStart;

        for (int i = columnStart; i < mColumnBounds.size()
                && mColumnBounds.get(i).lowerLimit <= rect.right; i++) {
            columnEnd = i;
        }

        int rowStart = Collections.binarySearch(mRowBounds, new Limits(rect.top, rect.top));
        if (rowStart < 0) {
            mPositionNearestOrigin = NOT_SET;
            return;
        }

        int rowEnd = rowStart;
        for (int i = rowStart; i < mRowBounds.size()
                && mRowBounds.get(i).lowerLimit <= rect.bottom; i++) {
            rowEnd = i;
        }

        updateSelection(columnStart, columnEnd, rowStart, rowEnd);
    }

    /**
     * Computes the selection given the previously-computed start- and end-indices for each
     * row and column.
     */
    private void updateSelection(
            int columnStartIndex, int columnEndIndex, int rowStartIndex, int rowEndIndex) {

        if (BandSelectionHelper.DEBUG) {
            Log.d(BandSelectionHelper.TAG, String.format(
                    "updateSelection: %d, %d, %d, %d",
                    columnStartIndex, columnEndIndex, rowStartIndex, rowEndIndex));
        }

        mSelection.clear();
        for (int column = columnStartIndex; column <= columnEndIndex; column++) {
            SparseIntArray items = mColumns.get(mColumnBounds.get(column).lowerLimit);
            for (int row = rowStartIndex; row <= rowEndIndex; row++) {
                // The default return value for SparseIntArray.get is 0, which is a valid
                // position. Use a sentry value to prevent erroneously selecting item 0.
                final int rowKey = mRowBounds.get(row).lowerLimit;
                int position = items.get(rowKey, NOT_SET);
                if (position != NOT_SET) {
                    K key = mKeyProvider.getKey(position);
                    if (key != null) {
                        // The adapter inserts items for UI layout purposes that aren't
                        // associated with files. Those will have a null model ID.
                        // Don't select them.
                        if (canSelect(key)) {
                            mSelection.add(key);
                        }
                    }
                    if (isPossiblePositionNearestOrigin(column, columnStartIndex, columnEndIndex,
                            row, rowStartIndex, rowEndIndex)) {
                        // If this is the position nearest the origin, record it now so that it
                        // can be returned by endSelection() later.
                        mPositionNearestOrigin = position;
                    }
                }
            }
        }
    }

    private boolean canSelect(K key) {
        return mSelectionPredicate.canSetStateForKey(key, true);
    }

    /**
     * @return Returns true if the position is the nearest to the origin, or, in the case of the
     * lower-right corner, whether it is possible that the position is the nearest to the
     * origin. See comment below for reasoning for this special case.
     */
    private boolean isPossiblePositionNearestOrigin(int columnIndex, int columnStartIndex,
            int columnEndIndex, int rowIndex, int rowStartIndex, int rowEndIndex) {
        int corner = computeCornerNearestOrigin();
        switch (corner) {
            case UPPER_LEFT:
                return columnIndex == columnStartIndex && rowIndex == rowStartIndex;
            case UPPER_RIGHT:
                return columnIndex == columnEndIndex && rowIndex == rowStartIndex;
            case LOWER_LEFT:
                return columnIndex == columnStartIndex && rowIndex == rowEndIndex;
            case LOWER_RIGHT:
                // Note that in some cases, the last row will not have as many items as there
                // are columns (e.g., if there are 4 items and 3 columns, the second row will
                // only have one item in the first column). This function is invoked for each
                // position from left to right, so return true for any position in the bottom
                // row and only the right-most position in the bottom row will be recorded.
                return rowIndex == rowEndIndex;
            default:
                throw new RuntimeException("Invalid corner type.");
        }
    }

    /**
     * Listener for changes in which items have been band selected.
     */
    public abstract static class SelectionObserver<K> {
        abstract void onSelectionChanged(Set<K> updatedSelection);
    }

    void addOnSelectionChangedListener(SelectionObserver<K> listener) {
        mOnSelectionChangedListeners.add(listener);
    }

    /**
     * Called when {@link BandSelectionHelper} is finished with a GridModel.
     */
    void onDestroy() {
        mOnSelectionChangedListeners.clear();
        // Cleanup listeners to prevent memory leaks.
        mHost.removeOnScrollListener(mScrollListener);
    }

    /**
     * Limits of a view item. For example, if an item's left side is at x-value 5 and its right side
     * is at x-value 10, the limits would be from 5 to 10. Used to record the left- and right sides
     * of item columns and the top- and bottom sides of item rows so that it can be determined
     * whether the pointer is located within the bounds of an item.
     */
    private static class Limits implements Comparable<Limits> {
        public int lowerLimit;
        public int upperLimit;

        Limits(int lowerLimit, int upperLimit) {
            this.lowerLimit = lowerLimit;
            this.upperLimit = upperLimit;
        }

        @Override
        public int compareTo(Limits other) {
            return lowerLimit - other.lowerLimit;
        }

        @Override
        public int hashCode() {
            return lowerLimit ^ upperLimit;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Limits)) {
                return false;
            }

            return ((Limits) other).lowerLimit == lowerLimit
                    && ((Limits) other).upperLimit == upperLimit;
        }

        @Override
        public String toString() {
            return "(" + lowerLimit + ", " + upperLimit + ")";
        }
    }

    /**
     * The location of a coordinate relative to items. This class represents a general area of the
     * view as it relates to band selection rather than an explicit point. For example, two
     * different points within an item are considered to have the same "location" because band
     * selection originating within the item would select the same items no matter which point
     * was used. Same goes for points between items as well as those at the very beginning or end
     * of the view.
     *
     * Tracking a coordinate (e.g., an x-value) as a CoordinateLocation instead of as an int has the
     * advantage of tying the value to the Limits of items along that axis. This allows easy
     * selection of items within those Limits as opposed to a search through every item to see if a
     * given coordinate value falls within those Limits.
     */
    private static class RelativeCoordinate
            implements Comparable<RelativeCoordinate> {
        /**
         * Location describing points after the last known item.
         */
        static final int AFTER_LAST_ITEM = 0;

        /**
         * Location describing points before the first known item.
         */
        static final int BEFORE_FIRST_ITEM = 1;

        /**
         * Location describing points between two items.
         */
        static final int BETWEEN_TWO_ITEMS = 2;

        /**
         * Location describing points within the limits of one item.
         */
        static final int WITHIN_LIMITS = 3;

        /**
         * The type of this coordinate, which is one of AFTER_LAST_ITEM, BEFORE_FIRST_ITEM,
         * BETWEEN_TWO_ITEMS, or WITHIN_LIMITS.
         */
        public final int type;

        /**
         * The limits before the coordinate; only populated when type == WITHIN_LIMITS or type ==
         * BETWEEN_TWO_ITEMS.
         */
        public Limits limitsBeforeCoordinate;

        /**
         * The limits after the coordinate; only populated when type == BETWEEN_TWO_ITEMS.
         */
        public Limits limitsAfterCoordinate;

        // Limits of the first known item; only populated when type == BEFORE_FIRST_ITEM.
        public Limits mFirstKnownItem;
        // Limits of the last known item; only populated when type == AFTER_LAST_ITEM.
        public Limits mLastKnownItem;

        /**
         * @param limitsList The sorted limits list for the coordinate type. If this
         *                   CoordinateLocation is an x-value, mXLimitsList should be passed;
         *                   otherwise,
         *                   mYLimitsList should be pased.
         * @param value      The coordinate value.
         */
        RelativeCoordinate(List<Limits> limitsList, int value) {
            int index = Collections.binarySearch(limitsList, new Limits(value, value));

            if (index >= 0) {
                this.type = WITHIN_LIMITS;
                this.limitsBeforeCoordinate = limitsList.get(index);
            } else if (~index == 0) {
                this.type = BEFORE_FIRST_ITEM;
                this.mFirstKnownItem = limitsList.get(0);
            } else if (~index == limitsList.size()) {
                Limits lastLimits = limitsList.get(limitsList.size() - 1);
                if (lastLimits.lowerLimit <= value && value <= lastLimits.upperLimit) {
                    this.type = WITHIN_LIMITS;
                    this.limitsBeforeCoordinate = lastLimits;
                } else {
                    this.type = AFTER_LAST_ITEM;
                    this.mLastKnownItem = lastLimits;
                }
            } else {
                Limits limitsBeforeIndex = limitsList.get(~index - 1);
                if (limitsBeforeIndex.lowerLimit <= value
                        && value <= limitsBeforeIndex.upperLimit) {
                    this.type = WITHIN_LIMITS;
                    this.limitsBeforeCoordinate = limitsList.get(~index - 1);
                } else {
                    this.type = BETWEEN_TWO_ITEMS;
                    this.limitsBeforeCoordinate = limitsList.get(~index - 1);
                    this.limitsAfterCoordinate = limitsList.get(~index);
                }
            }
        }

        int toComparisonValue() {
            if (type == BEFORE_FIRST_ITEM) {
                return mFirstKnownItem.lowerLimit - 1;
            } else if (type == AFTER_LAST_ITEM) {
                return mLastKnownItem.upperLimit + 1;
            } else if (type == BETWEEN_TWO_ITEMS) {
                return limitsBeforeCoordinate.upperLimit + 1;
            } else {
                return limitsBeforeCoordinate.lowerLimit;
            }
        }

        @Override
        public int hashCode() {
            return mFirstKnownItem.lowerLimit
                    ^ mLastKnownItem.upperLimit
                    ^ limitsBeforeCoordinate.upperLimit
                    ^ limitsBeforeCoordinate.lowerLimit;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RelativeCoordinate)) {
                return false;
            }

            RelativeCoordinate otherCoordinate = (RelativeCoordinate) other;
            return toComparisonValue() == otherCoordinate.toComparisonValue();
        }

        @Override
        public int compareTo(RelativeCoordinate other) {
            return toComparisonValue() - other.toComparisonValue();
        }
    }

    RelativePoint createRelativePoint(Point point) {
        // mColumnBounds and mRowBounds is empty when there are no items in the view.
        // Clients have to verify items exist before calling this method.
        checkState(!mColumnBounds.isEmpty(), "Column bounds not established.");
        checkState(!mRowBounds.isEmpty(), "Row bounds not established.");

        return new RelativePoint(
                new RelativeCoordinate(mColumnBounds, point.x),
                new RelativeCoordinate(mRowBounds, point.y));
    }

    /**
     * The location of a point relative to the Limits of nearby items; consists of both an x- and
     * y-RelativeCoordinateLocation.
     */
    private static class RelativePoint {

        final RelativeCoordinate mX;
        final RelativeCoordinate mY;

        RelativePoint(@NonNull RelativeCoordinate x, @NonNull RelativeCoordinate y) {
            this.mX = x;
            this.mY = y;
        }

        @Override
        public int hashCode() {
            return mX.toComparisonValue() ^ mY.toComparisonValue();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (!(other instanceof RelativePoint)) {
                return false;
            }

            RelativePoint otherPoint = (RelativePoint) other;
            return mX.equals(otherPoint.mX) && mY.equals(otherPoint.mY);
        }
    }

    /**
     * Generates a rectangle which contains the items selected by the pointer and origin.
     *
     * @return The rectangle, or null if no items were selected.
     */
    private Rect computeBounds() {
        Rect rect = new Rect();
        rect.left = getCoordinateValue(
                min(mRelOrigin.mX, mRelPointer.mX),
                mColumnBounds,
                true);
        rect.right = getCoordinateValue(
                max(mRelOrigin.mX, mRelPointer.mX),
                mColumnBounds,
                false);
        rect.top = getCoordinateValue(
                min(mRelOrigin.mY, mRelPointer.mY),
                mRowBounds,
                true);
        rect.bottom = getCoordinateValue(
                max(mRelOrigin.mY, mRelPointer.mY),
                mRowBounds,
                false);
        return rect;
    }

    /**
     * Computes the corner of the selection nearest the origin.
     */
    private int computeCornerNearestOrigin() {
        int cornerValue = 0;

        if (mRelOrigin.mY.equals(min(mRelOrigin.mY, mRelPointer.mY))) {
            cornerValue |= UPPER;
        } else {
            cornerValue |= LOWER;
        }

        if (mRelOrigin.mX.equals(min(mRelOrigin.mX, mRelPointer.mX))) {
            cornerValue |= LEFT;
        } else {
            cornerValue |= RIGHT;
        }

        return cornerValue;
    }

    private RelativeCoordinate min(
            @NonNull RelativeCoordinate first, @NonNull RelativeCoordinate second) {
        return first.compareTo(second) < 0 ? first : second;
    }

    private RelativeCoordinate max(
            @NonNull RelativeCoordinate first, @NonNull RelativeCoordinate second) {
        return first.compareTo(second) > 0 ? first : second;
    }

    /**
     * @return The absolute coordinate (i.e., the x- or y-value) of the given relative
     * coordinate.
     */
    private int getCoordinateValue(
            @NonNull RelativeCoordinate coordinate,
            @NonNull List<Limits> limitsList,
            boolean isStartOfRange) {

        switch (coordinate.type) {
            case RelativeCoordinate.BEFORE_FIRST_ITEM:
                return limitsList.get(0).lowerLimit;
            case RelativeCoordinate.AFTER_LAST_ITEM:
                return limitsList.get(limitsList.size() - 1).upperLimit;
            case RelativeCoordinate.BETWEEN_TWO_ITEMS:
                if (isStartOfRange) {
                    return coordinate.limitsAfterCoordinate.lowerLimit;
                } else {
                    return coordinate.limitsBeforeCoordinate.upperLimit;
                }
            case RelativeCoordinate.WITHIN_LIMITS:
                return coordinate.limitsBeforeCoordinate.lowerLimit;
        }

        throw new RuntimeException("Invalid coordinate value.");
    }

    private boolean areItemsCoveredByBand(
            @NonNull RelativePoint first, @NonNull RelativePoint second) {

        return doesCoordinateLocationCoverItems(first.mX, second.mX)
                && doesCoordinateLocationCoverItems(first.mY, second.mY);
    }

    private boolean doesCoordinateLocationCoverItems(
            @NonNull RelativeCoordinate pointerCoordinate,
            @NonNull RelativeCoordinate originCoordinate) {

        if (pointerCoordinate.type == RelativeCoordinate.BEFORE_FIRST_ITEM
                && originCoordinate.type == RelativeCoordinate.BEFORE_FIRST_ITEM) {
            return false;
        }

        if (pointerCoordinate.type == RelativeCoordinate.AFTER_LAST_ITEM
                && originCoordinate.type == RelativeCoordinate.AFTER_LAST_ITEM) {
            return false;
        }

        if (pointerCoordinate.type == RelativeCoordinate.BETWEEN_TWO_ITEMS
                && originCoordinate.type == RelativeCoordinate.BETWEEN_TWO_ITEMS
                && pointerCoordinate.limitsBeforeCoordinate.equals(
                originCoordinate.limitsBeforeCoordinate)
                && pointerCoordinate.limitsAfterCoordinate.equals(
                originCoordinate.limitsAfterCoordinate)) {
            return false;
        }

        return true;
    }

    /**
     * Provides functionality for BandController. Exists primarily to tests that are
     * fully isolated from RecyclerView.
     *
     * @param <K> Selection key type. @see {@link StorageStrategy} for supported types.
     */
    abstract static class GridHost<K> extends BandSelectionHelper.BandHost<K> {

        /**
         * Remove the listener.
         *
         * @param listener
         */
        abstract void removeOnScrollListener(@NonNull OnScrollListener listener);

        /**
         * @param relativePoint for which to create absolute point.
         * @return absolute point.
         */
        abstract Point createAbsolutePoint(@NonNull Point relativePoint);

        /**
         * @param index index of child.
         * @return rectangle describing child at {@code index}.
         */
        abstract Rect getAbsoluteRectForChildViewAt(int index);

        /**
         * @param index index of child.
         * @return child adapter position for the child at {@code index}
         */
        abstract int getAdapterPositionAt(int index);

        /** @return column count. */
        abstract int getColumnCount();

        /** @return number of children visible in the view. */
        abstract int getVisibleChildCount();

        /**
         * @return true if the item at adapter position is attached to a view.
         */
        abstract boolean hasView(int adapterPosition);
    }
}
