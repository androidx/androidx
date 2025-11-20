/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.state.helpers;

import androidx.constraintlayout.core.state.HelperReference;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.utils.GridCore;
import androidx.constraintlayout.core.widgets.HelperWidget;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A HelperReference of a Grid Helper that helps enable Grid in Compose
 */
public class GridReference extends HelperReference {

    private static final String SPANS_RESPECT_WIDGET_ORDER_STRING = "spansrespectwidgetorder";
    private static final String SUB_GRID_BY_COL_ROW_STRING = "subgridbycolrow";

    public GridReference(@NonNull State state, State.@NonNull Helper type) {
        super(state, type);
        if (type == State.Helper.ROW) {
            this.mRowsSet = 1;
        } else if (type == State.Helper.COLUMN) {
            this.mColumnsSet = 1;
        }
    }

    /**
     * The Grid Object
     */
    private GridCore mGrid;

    /**
     * padding start
     */
    private int mPaddingStart = 0;

    /**
     * padding end
     */
    private int mPaddingEnd = 0;

    /**
     * padding top
     */
    private int mPaddingTop = 0;

    /**
     * padding bottom
     */
    private int mPaddingBottom = 0;

    /**
     * The orientation of the widgets arrangement horizontally or vertically
     */
    private int mOrientation;

    /**
     * Number of rows of the Grid
     */
    private int mRowsSet;

    /**
     * Number of columns of the Grid
     */
    private int mColumnsSet;

    /**
     * The horizontal gaps between widgets
     */
    private float mHorizontalGaps;

    /**
     * The vertical gaps between widgets
     */
    private float mVerticalGaps;

    /**
     * The weight of each widget in a row
     */
    private String mRowWeights;

    /**
     * The weight of each widget in a column
     */
    private String mColumnWeights;

    /**
     * Specify the spanned areas of widgets
     */
    private String mSpans;

    /**
     * Specify the positions to be skipped in the Grid
     */
    private String mSkips;

    /**
     * An int value containing flag information.
     */
    private int mFlags;

    /**
     * get padding left
     * @return padding left
     */
    public int getPaddingStart() {
        return mPaddingStart;
    }

    /**
     * set padding left
     * @param paddingStart padding left to be set
     */
    public void setPaddingStart(int paddingStart) {
        mPaddingStart = paddingStart;
    }

    /**
     * get padding right
     * @return padding right
     */
    public int getPaddingEnd() {
        return mPaddingEnd;
    }

    /**
     * set padding right
     * @param paddingEnd padding right to be set
     */
    public void setPaddingEnd(int paddingEnd) {
        mPaddingEnd = paddingEnd;
    }

    /**
     * get padding top
     * @return padding top
     */
    public int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * set padding top
     * @param paddingTop padding top to be set
     */
    public void setPaddingTop(int paddingTop) {
        mPaddingTop = paddingTop;
    }

    /**
     * get padding bottom
     * @return padding bottom
     */
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    /**
     * set padding bottom
     * @param paddingBottom padding bottom to be set
     */
    public void setPaddingBottom(int paddingBottom) {
        mPaddingBottom = paddingBottom;
    }

    /**
     * Get all the flags of a Grid
     * @return an int value containing flag information
     */
    public int getFlags() {
        return mFlags;
    }

    /**
     * Set flags of a Grid
     * @param flags an int value containing flag information
     */
    public void setFlags(int flags) {
        mFlags = flags;
    }

    /**
     * Set flags of a Grid
     * @param flags a String containing all the flags
     */
    public void setFlags(@NonNull String flags) {
        if (flags.isEmpty()) {
            return;
        }

        String[] strArr = flags.split("\\|");
        mFlags = 0;
        for (String str: strArr) {
            switch (str.toLowerCase()) {
                case SUB_GRID_BY_COL_ROW_STRING:
                    mFlags |= 1;
                    break;
                case SPANS_RESPECT_WIDGET_ORDER_STRING:
                    mFlags |= 2;
                    break;
            }
        }
    }

    /**
     * Get the number of rows
     * @return the number of rows
     */
    public int getRowsSet() {
        return mRowsSet;
    }

    /**
     * Set the number of rows
     * @param rowsSet the number of rows
     */
    public void setRowsSet(int rowsSet) {
        if (super.getType() == State.Helper.COLUMN) {
            return;
        }
        mRowsSet = rowsSet;
    }

    /**
     * Get the number of columns
     * @return the number of columns
     */
    public int getColumnsSet() {
        return mColumnsSet;
    }

    /**
     * Set the number of columns
     * @param columnsSet the number of columns
     */
    public void setColumnsSet(int columnsSet) {
        if (super.getType() == State.Helper.ROW) {
            return;
        }
        mColumnsSet = columnsSet;
    }

    /**
     * Get the horizontal gaps
     * @return the horizontal gaps
     */
    public float getHorizontalGaps() {
        return mHorizontalGaps;
    }

    /**
     * Set the horizontal gaps
     * @param horizontalGaps the horizontal gaps
     */
    public void setHorizontalGaps(float horizontalGaps) {
        mHorizontalGaps = horizontalGaps;
    }

    /**
     * Get the vertical gaps
     * @return the vertical gaps
     */
    public float getVerticalGaps() {
        return mVerticalGaps;
    }

    /**
     * Set the vertical gaps
     * @param verticalGaps  the vertical gaps
     */
    public void setVerticalGaps(float verticalGaps) {
        mVerticalGaps = verticalGaps;
    }

    /**
     * Get the row weights
     * @return the row weights
     */
    @SuppressWarnings("GetterSetterNullability")
    public @Nullable String getRowWeights() {
        return mRowWeights;
    }

    /**
     * Set the row weights
     * @param rowWeights the row weights
     */
    public void setRowWeights(@NonNull String rowWeights) {
        mRowWeights = rowWeights;
    }

    /**
     * Get the column weights
     * @return the column weights
     */
    @SuppressWarnings("GetterSetterNullability")
    public @Nullable String getColumnWeights() {
        return mColumnWeights;
    }

    /**
     * Set the column weights
     * @param columnWeights the column weights
     */
    public void setColumnWeights(@NonNull String columnWeights) {
        mColumnWeights = columnWeights;
    }

    /**
     * Get the spans
     * @return the spans
     */
    @SuppressWarnings("GetterSetterNullability")
    public @Nullable String getSpans() {
        return mSpans;
    }

    /**
     * Set the spans
     * @param spans the spans
     */
    public void setSpans(@NonNull String spans) {
        mSpans = spans;
    }

    /**
     * Get the skips
     * @return the skips
     */
    @SuppressWarnings("GetterSetterNullability")
    public @Nullable String getSkips() {
        return mSkips;
    }

    /**
     * Set the skips
     * @param skips the skips
     */
    public void setSkips(@NonNull String skips) {
        mSkips = skips;
    }

    /**
     * Get the helper widget (Grid)
     * @return the helper widget (Grid)
     */
    @SuppressWarnings("GetterSetterNullability")
    @Override
    public @NonNull HelperWidget getHelperWidget() {
        if (mGrid == null) {
            mGrid = new GridCore();
        }
        return mGrid;
    }

    /**
     * Set the helper widget (Grid)
     * @param widget the helper widget (Grid)
     */
    @Override
    public void setHelperWidget(@Nullable HelperWidget widget) {
        if (widget instanceof GridCore) {
            mGrid = (GridCore) widget;
        } else {
            mGrid = null;
        }
    }

    /**
     * Get the Orientation
     * @return the Orientation
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Set the Orientation
     * @param orientation the Orientation
     */
    public void setOrientation(int orientation) {
        mOrientation = orientation;

    }

    /**
     * Apply all the attributes to the helper widget (Grid)
     */
    @Override
    public void apply() {
        getHelperWidget();

        mGrid.setOrientation(mOrientation);

        if (mRowsSet != 0) {
            mGrid.setRows(mRowsSet);
        }

        if (mColumnsSet != 0) {
            mGrid.setColumns(mColumnsSet);
        }

        if (mHorizontalGaps != 0) {
            mGrid.setHorizontalGaps(mHorizontalGaps);
        }

        if (mVerticalGaps != 0) {
            mGrid.setVerticalGaps(mVerticalGaps);
        }

        if (mRowWeights != null && !mRowWeights.isEmpty()) {
            mGrid.setRowWeights(mRowWeights);
        }

        if (mColumnWeights != null && !mColumnWeights.isEmpty()) {
            mGrid.setColumnWeights(mColumnWeights);
        }

        if (mSpans != null && !mSpans.isEmpty()) {
            mGrid.setSpans(mSpans);
        }

        if (mSkips != null && !mSkips.isEmpty()) {
            mGrid.setSkips(mSkips);
        }

        mGrid.setFlags(mFlags);

        mGrid.setPaddingStart(mPaddingStart);
        mGrid.setPaddingEnd(mPaddingEnd);
        mGrid.setPaddingTop(mPaddingTop);
        mGrid.setPaddingBottom(mPaddingBottom);

        // General attributes of a widget
        applyBase();
    }
}