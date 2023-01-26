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

package androidx.constraintlayout.core.utils;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.VirtualLayout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The Grid Helper in the Core library that helps to enable Grid in Compose
 */
public class GridCore extends VirtualLayout {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    private static final int DEFAULT_SIZE = 3; // default rows and columns.
    private static final int MAX_ROWS = 50; // maximum number of rows can be specified.
    private static final int MAX_COLUMNS = 50; // maximum number of columns can be specified.

    /**
     * Container for all the ConstraintWidgets
     */
    ConstraintWidgetContainer mContainer;

    /**
     * boxWidgets were created as anchor points for arranging the associated widgets
     */
    private ConstraintWidget[] mBoxWidgets;

    /**
     * number of rows of the grid
     */
    private int mRows;

    /**
     * number of rows set by the JSON or API
     */
    private int mRowsSet;

    /**
     * number of columns of the grid
     */
    private int mColumns;

    /**
     * number of columns set by the XML or API
     */
    private int mColumnsSet;

    /**
     * Horizontal gaps in Dp
     */
    private float mHorizontalGaps;

    /**
     * Vertical gaps in Dp
     */
    private float mVerticalGaps;

    /**
     * string format of the row weight
     */
    private String mRowWeights;

    /**
     * string format of the column weight
     */
    private String mColumnWeights;

    /**
     * string format of the input Spans
     */
    private String mSpans;

    /**
     * string format of the input Skips
     */
    private String mSkips;

    /**
     * orientation of the widget arrangement - vertical or horizontal
     */
    private int mOrientation;

    /**
     * Indicates what is the next available position to place a widget
     */
    private int mNextAvailableIndex = 0;

    /**
     * A boolean matrix that tracks the positions that are occupied by skips and spans
     * true: available position
     * false: non-available position
     */
    private boolean[][] mPositionMatrix;

    /**
     * Store the widget ids of handled spans
     */
    Set<String> mSpanIds = new HashSet<>();

    /**
     * A int matrix that contains the positions where a widget would constraint to at each direction
     * Each row contains 4 values that indicate the position to constraint of a widget.
     * Example row: [left, top, right, bottom]
     */
    private int[][] mConstraintMatrix;

    public GridCore() {
        updateActualRowsAndColumns();
        initMatrices();
    }

    public GridCore(int rows, int columns) {
        mRowsSet = rows;
        mColumnsSet = columns;
        if (rows > MAX_ROWS) {
            mRowsSet = DEFAULT_SIZE;
        }

        if (columns > MAX_COLUMNS) {
            mColumnsSet = DEFAULT_SIZE;
        }

        updateActualRowsAndColumns();
        initMatrices();
    }

    /**
     * get the parent ConstraintWidgetContainer
     *
     * @return the parent ConstraintWidgetContainer
     */
    @Nullable
    public ConstraintWidgetContainer getContainer() {
        return mContainer;
    }

    /**
     * Set the parent ConstraintWidgetContainer
     * @param container the parent ConstraintWidgetContainer
     */
    public void setContainer(@NonNull ConstraintWidgetContainer container) {
        mContainer = container;
    }

    /**
     * set new spans value
     *
     * @param spans new spans value
     */
    public void setSpans(@NonNull CharSequence spans) {
        if (mSpans != null && mSpans.equals(spans.toString())) {
            return;
        }

        mSpans = spans.toString();
    }

    /**
     * set new skips value
     *
     * @param skips new spans value
     */
    public void setSkips(@NonNull String skips) {
        if (mSkips != null && mSkips.equals(skips)) {
            return;
        }

        mSkips = skips;

    }

    /**
     * get the value of horizontalGaps
     *
     * @return the value of horizontalGaps
     */
    public float getHorizontalGaps() {
        return mHorizontalGaps;
    }

    /**
     * set new horizontalGaps value and also invoke invalidate
     *
     * @param horizontalGaps new horizontalGaps value
     */
    public void setHorizontalGaps(float horizontalGaps) {
        if (horizontalGaps < 0) {
            return;
        }

        if (mHorizontalGaps == horizontalGaps) {
            return;
        }

        mHorizontalGaps = horizontalGaps;
    }

    /**
     * get the value of verticalGaps
     *
     * @return the value of verticalGaps
     */
    public float getVerticalGaps() {
        return mVerticalGaps;
    }

    /**
     * set new verticalGaps value and also invoke invalidate
     *
     * @param verticalGaps new verticalGaps value
     */
    public void setVerticalGaps(float verticalGaps) {
        if (verticalGaps < 0) {
            return;
        }

        if (mVerticalGaps == verticalGaps) {
            return;
        }

        mVerticalGaps = verticalGaps;
    }

    /**
     * get the string value of rowWeights
     *
     * @return the string value of rowWeights
     */
    @Nullable
    public String getRowWeights() {
        return mRowWeights;
    }

    /**
     * set new rowWeights value and also invoke invalidate
     *
     * @param rowWeights new rowWeights value
     */
    public void setRowWeights(@NonNull String rowWeights) {
        if (mRowWeights != null && mRowWeights.equals(rowWeights)) {
            return;
        }

        mRowWeights = rowWeights;
    }

    /**
     * get the string value of columnWeights
     *
     * @return the string value of columnWeights
     */
    @Nullable
    public String getColumnWeights() {
        return mColumnWeights;
    }

    /**
     * set new columnWeights value and also invoke invalidate
     *
     * @param columnWeights new columnWeights value
     */
    public void setColumnWeights(@NonNull String columnWeights) {
        if (mColumnWeights != null && mColumnWeights.equals(columnWeights)) {
            return;
        }

        mColumnWeights = columnWeights;
    }

    /**
     * get the value of orientation
     *
     * @return the value of orientation
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * set new orientation value
     *
     * @param orientation new orientation value
     */
    public void setOrientation(int orientation) {
        if (!(orientation == HORIZONTAL || orientation == VERTICAL)) {
            return;
        }

        if (mOrientation == orientation) {
            return;
        }

        mOrientation = orientation;
    }

    /**
     * set new rows value
     *
     * @param rows new rows value
     */
    public void setRows(int rows) {
        if (rows > MAX_ROWS) {
            return;
        }

        if (mRowsSet == rows) {
            return;
        }

        mRowsSet = rows;
        updateActualRowsAndColumns();
        initVariables();
    }

    /**
     * set new columns value
     *
     * @param columns new rows value
     */
    public void setColumns(int columns) {
        if (columns > MAX_COLUMNS) {
            return;
        }

        if (mColumnsSet == columns) {
            return;
        }

        mColumnsSet = columns;
        updateActualRowsAndColumns();
        initVariables();
    }

    /**
     * Handle the span use cases
     *
     * @param spansMatrix a int matrix that contains span information
     */
    private void handleSpans(int[][] spansMatrix) {
        for (int i = 0; i < spansMatrix.length; i++) {
            int row = getRowByIndex(spansMatrix[i][0]);
            int col = getColByIndex(spansMatrix[i][0]);
            if (!invalidatePositions(row, col,
                    spansMatrix[i][1], spansMatrix[i][2])) {
                return;
            }
            connectWidget(mWidgets[i], row, col,
                    spansMatrix[i][1], spansMatrix[i][2]);
            mSpanIds.add(mWidgets[i].stringId);
        }
    }

    /**
     * Arrange the widgets in the constraint_referenced_ids
     */
    private void arrangeWidgets() {
        int position;

        // @TODO handle RTL
        for (int i = 0; i < mWidgetsCount; i++) {
            if (mSpanIds.contains(mWidgets[i].stringId)) {
                // skip the widget Id that's already handled by handleSpans
                continue;
            }

            position = getNextPosition();
            int row = getRowByIndex(position);
            int col = getColByIndex(position);
            if (position == -1) {
                // no more available position.
                return;
            }

            connectWidget(mWidgets[i], row, col, 1, 1);
        }
    }

    /**
     * generate the Grid form based on the input attributes
     *
     * @param isUpdate whether to update the existing grid (true) or create a new one (false)
     */
    private void setupGrid(boolean isUpdate) {
        if (mRows < 1 || mColumns < 1) {
            return;
        }

        if (isUpdate) {
            for (int i = 0; i < mPositionMatrix.length; i++) {
                for (int j = 0; j < mPositionMatrix[0].length; j++) {
                    mPositionMatrix[i][j] = true;
                }
            }
            mSpanIds.clear();
        }

        mNextAvailableIndex = 0;
        createBoxes();

        if (mSkips != null && !mSkips.trim().isEmpty()) {
            int[][] mSkips = parseSpans(this.mSkips);
            if (mSkips != null) {
                handleSkips(mSkips);
            }
        }

        if (mSpans != null && !mSpans.trim().isEmpty()) {
            int[][] mSpans = parseSpans(this.mSpans);
            if (mSpans != null) {
                handleSpans(mSpans);
            }
        }
    }

    /**
     * Convert a 1D index to a 2D index that has index for row
     *
     * @param index index in 1D
     * @return row as its values.
     */
    private int getRowByIndex(int index) {
        if (mOrientation == 1) {
            return index % mRows;

        } else {
            return index / mColumns;
        }
    }

    /**
     * Convert a 1D index to a 2D index that has index for column
     *
     * @param index index in 1D
     * @return column as its values.
     */
    private int getColByIndex(int index) {
        if (mOrientation == 1) {
            return index / mRows;
        } else {
            return index % mColumns;
        }
    }

    /**
     * Make positions in the grid unavailable based on the skips attr
     *
     * @param skipsMatrix a int matrix that contains skip information
     */
    private void handleSkips(int[][] skipsMatrix) {
        for (int[] matrix : skipsMatrix) {
            int row = getRowByIndex(matrix[0]);
            int col = getColByIndex(matrix[0]);
            if (!invalidatePositions(row, col,
                    matrix[1], matrix[2])) {
                return;
            }
        }
    }

    /**
     * Make the specified positions in the grid unavailable.
     *
     * @param startRow the row of the staring position
     * @param startColumn the column of the staring position
     * @param rowSpan how many rows to span
     * @param columnSpan how many columns to span
     * @return true if we could properly invalidate the positions else false
     */
    private boolean invalidatePositions(int startRow, int startColumn,
                                        int rowSpan, int columnSpan) {
        for (int i = startRow; i < startRow + rowSpan; i++) {
            for (int j = startColumn; j < startColumn + columnSpan; j++) {
                if (i >= mPositionMatrix.length || j >= mPositionMatrix[0].length
                        || !mPositionMatrix[i][j]) {
                    // the position is already occupied.
                    return false;
                }
                mPositionMatrix[i][j] = false;
            }
        }
        return true;
    }

    /**
     * parse the weights/pads in the string format into a float array
     *
     * @param size size of the return array
     * @param str  weights/pads in a string format
     * @return a float array with weights/pads values
     */
    private float[] parseWeights(int size, String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }

        String[] values = str.split(",");
        if (values.length != size) {
            return null;
        }

        float[] arr = new float[size];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = Float.parseFloat(values[i].trim());
        }
        return arr;
    }

    /**
     * Get the next available position for widget arrangement.
     * @return int[] -> [row, column]
     */
    private int getNextPosition() {
        int position = 0;
        boolean positionFound = false;

        while (!positionFound) {
            if (mNextAvailableIndex >= mRows * mColumns) {
                return -1;
            }

            position = mNextAvailableIndex;
            int row = getRowByIndex(mNextAvailableIndex);
            int col = getColByIndex(mNextAvailableIndex);
            if (mPositionMatrix[row][col]) {
                mPositionMatrix[row][col] = false;
                positionFound = true;
            }

            mNextAvailableIndex++;
        }
        return position;
    }

    /**
     * Compute the actual rows and columns given what was set
     * if 0,0 find the most square rows and columns that fits
     * if 0,n or n,0 scale to fit
     */
    private void updateActualRowsAndColumns() {
        if (mRowsSet == 0 || mColumnsSet == 0) {
            if (mColumnsSet > 0) {
                mColumns = mColumnsSet;
                mRows = (mWidgetsCount + mColumns - 1) / mColumnsSet; // round up
            } else  if (mRowsSet > 0) {
                mRows = mRowsSet;
                mColumns = (mWidgetsCount + mRowsSet - 1) / mRowsSet; // round up
            } else { // as close to square as possible favoring more rows
                mRows = (int)  (1.5 + Math.sqrt(mWidgetsCount));
                mColumns = (mWidgetsCount + mRows - 1) / mRows;
            }
        } else {
            mRows = mRowsSet;
            mColumns = mColumnsSet;
        }
    }

    /**
     * Create a new boxWidget for constraining widgets
     * @return the created boxWidget
     */
    private ConstraintWidget makeNewWidget() {
        ConstraintWidget widget = new ConstraintWidget();
        widget.mListDimensionBehaviors[HORIZONTAL] = MATCH_CONSTRAINT;
        widget.mListDimensionBehaviors[VERTICAL] = MATCH_CONSTRAINT;
        widget.stringId = String.valueOf(widget.hashCode());
        return widget;
    }

    /**
     * Connect the widget to the corresponding widgetBoxes based on the input params
     *
     * @param widget the widget that we want to add constraints to
     * @param row    row position to place the widget
     * @param column column position to place the widget
     */
    private void connectWidget(ConstraintWidget widget, int row, int column,
                               int rowSpan, int columnSpan) {
        // Connect the 4 sides
        widget.mLeft.connect(mBoxWidgets[column].mLeft, 0);
        widget.mTop.connect(mBoxWidgets[row].mTop, 0);
        widget.mRight.connect(mBoxWidgets[column + columnSpan - 1].mRight, 0);
        widget.mBottom.connect(mBoxWidgets[row + rowSpan - 1].mBottom, 0);
    }

    /**
     * Set chain between boxWidget horizontally
     */
    private void setBoxWidgetHorizontalChains() {
        int maxVal = Math.max(mRows, mColumns);

        ConstraintWidget widget = mBoxWidgets[0];
        float[] columnWeights = parseWeights(mColumns, mColumnWeights);
        // chain all the widgets on the longer side (either horizontal or vertical)
        if (mColumns == 1) {
            clearHorizontalAttributes(widget);
            widget.mLeft.connect(mLeft, 0);
            widget.mRight.connect(mRight, 0);
            return;
        }

        //  chains are grid <- box <-> box <-> box -> grid
        for (int i = 0; i < mColumns; i++) {
            widget = mBoxWidgets[i];
            clearHorizontalAttributes(widget);
            if (columnWeights != null) {
                widget.setHorizontalWeight(columnWeights[i]);
            }
            if (i > 0) {
                widget.mLeft.connect(mBoxWidgets[i - 1].mRight, 0);
            } else {
                widget.mLeft.connect(mLeft, 0);
            }
            if (i < mColumns - 1) {
                widget.mRight.connect(mBoxWidgets[i + 1].mLeft, 0);
            } else {
                widget.mRight.connect(mRight, 0);
            }
            if (i > 0) {
                widget.mLeft.mMargin = (int) mHorizontalGaps;
            }
        }
        // excess boxes are connected to grid those sides are not use
        // for efficiency they should be connected to parent
        for (int i = mColumns; i < maxVal; i++) {
            widget = mBoxWidgets[i];
            clearHorizontalAttributes(widget);
            widget.mLeft.connect(mLeft, 0);
            widget.mRight.connect(mRight, 0);
        }
    }

    /**
     * Set chain between boxWidget vertically
     */
    private void setBoxWidgetVerticalChains() {
        int maxVal = Math.max(mRows, mColumns);

        ConstraintWidget widget = mBoxWidgets[0];
        float[] rowWeights = parseWeights(mRows, mRowWeights);
        // chain all the widgets on the longer side (either horizontal or vertical)
        if (mRows == 1) {
            clearVerticalAttributes(widget);
            widget.mTop.connect(mTop, 0);
            widget.mBottom.connect(mBottom, 0);
            return;
        }

        // chains are constrained like this: grid <- box <-> box <-> box -> grid
        for (int i = 0; i < mRows; i++) {
            widget = mBoxWidgets[i];
            clearVerticalAttributes(widget);
            if (rowWeights != null) {
                widget.setVerticalWeight(rowWeights[i]);
            }
            if (i > 0) {
                widget.mTop.connect(mBoxWidgets[i - 1].mBottom, 0);
            } else {
                widget.mTop.connect(mTop, 0);
            }
            if (i < mRows - 1) {
                widget.mBottom.connect(mBoxWidgets[i + 1].mTop, 0);
            } else {
                widget.mBottom.connect(mBottom, 0);
            }
            if (i > 0) {
                widget.mTop.mMargin = (int) mVerticalGaps;
            }
        }

        // excess boxes are connected to grid those sides are not use
        // for efficiency they should be connected to parent
        for (int i = mRows; i < maxVal; i++) {
            widget = mBoxWidgets[i];
            clearVerticalAttributes(widget);
            widget.mTop.connect(mTop, 0);
            widget.mBottom.connect(mBottom, 0);
        }
    }

    /**
     * Chains the boxWidgets and add constraints to the widgets
     */
    private void addConstraints() {
        setBoxWidgetVerticalChains();
        setBoxWidgetHorizontalChains();
        arrangeWidgets();
    }

    /**
     * Create all the boxWidgets that will be used to constrain widgets
     */
    private void createBoxes() {
        int boxCount = Math.max(mRows, mColumns);
        if (mBoxWidgets == null) { // no box widgets build all
            mBoxWidgets = new ConstraintWidget[boxCount];
            for (int i = 0; i < mBoxWidgets.length; i++) {
                mBoxWidgets[i] = makeNewWidget(); // need to remove old Widgets
            }
        } else {
            if (boxCount != mBoxWidgets.length) {
                ConstraintWidget[] temp = new ConstraintWidget[boxCount];
                for (int i = 0; i < boxCount; i++) {
                    if (i < mBoxWidgets.length) { // use old one
                        temp[i] = mBoxWidgets[i];
                    } else { // make new one
                        temp[i] = makeNewWidget();
                    }
                }
                // remove excess
                for (int j = boxCount; j < mBoxWidgets.length; j++) {
                    ConstraintWidget widget = mBoxWidgets[j];
                    mContainer.remove(widget);
                }
                mBoxWidgets = temp;
            }
        }
    }

    /**
     * Clear the vertical related attributes
     * @param widget widget that has the attributes to be cleared
     */
    private void clearVerticalAttributes(ConstraintWidget widget) {
        widget.setVerticalWeight(UNKNOWN);
        widget.mTop.reset();
        widget.mBottom.reset();
        widget.mBaseline.reset();
    }

    /**
     * Clear the horizontal related attributes
     * @param widget widget that has the attributes to be cleared
     */
    private void clearHorizontalAttributes(ConstraintWidget widget) {
        widget.setHorizontalWeight(UNKNOWN);
        widget.mLeft.reset();
        widget.mRight.reset();
    }

    /**
     * Initialize the relevant variables
     */
    private void initVariables() {
        mPositionMatrix = new boolean[mRows][mColumns];
        for (boolean[] row : mPositionMatrix) {
            Arrays.fill(row, true);
        }

        if (mWidgetsCount > 0) {
            mConstraintMatrix = new int[mWidgetsCount][4];
            for (int[] row : mConstraintMatrix) {
                Arrays.fill(row, -1);
            }
        }
    }

    /**
     * parse the skips/spans in the string format into a int matrix
     * that each row has the information - [index, row_span, col_span]
     * the format of the input string is index:row_spanxcol_span.
     * index - the index of the starting position
     * row_span - the number of rows to span
     * col_span- the number of columns to span
     *
     * @param str string format of skips or spans
     * @return a int matrix that contains skip information.
     */
    private int[][] parseSpans(String str) {
        try {
            String[] spans = str.split(",");
            int[][] spanMatrix = new int[spans.length][3];

            String[] indexAndSpan;
            String[] rowAndCol;
            for (int i = 0; i < spans.length; i++) {
                indexAndSpan = spans[i].trim().split(":");
                rowAndCol = indexAndSpan[1].split("x");
                spanMatrix[i][0] = Integer.parseInt(indexAndSpan[0]);
                spanMatrix[i][1] = Integer.parseInt(rowAndCol[0]);
                spanMatrix[i][2] = Integer.parseInt(rowAndCol[1]);
            }
            return spanMatrix;
        } catch (Exception e) {
            return null;
        }

    }

    /**
     * fill the constraintMatrix based on the input attributes
     *
     * @param isUpdate whether to update the existing grid (true) or create a new one (false)
     */
    private void fillConstraintMatrix(boolean isUpdate) {
        if (isUpdate) {
            for (int i = 0; i < mPositionMatrix.length; i++) {
                for (int j = 0; j < mPositionMatrix[0].length; j++) {
                    mPositionMatrix[i][j] = true;
                }
            }

            for (int i = 0; i < mConstraintMatrix.length; i++) {
                for (int j = 0; j < mConstraintMatrix[0].length; j++) {
                    mConstraintMatrix[i][j] = -1;
                }
            }
        }

        mNextAvailableIndex = 0;

        if (mSkips != null && !mSkips.trim().isEmpty()) {
            int[][] mSkips = parseSpans(this.mSkips);
            if (mSkips != null) {
                handleSkips(mSkips);
            }
        }

        if (mSpans != null && !mSpans.trim().isEmpty()) {
            int[][] mSpans = parseSpans(this.mSpans);
            if (mSpans != null) {
                handleSpans(mSpans);
            }
        }
    }

    /**
     * Set up the Grid engine.
     */
    private void initMatrices() {
        boolean isUpdate = mConstraintMatrix != null
                && mConstraintMatrix.length == mWidgetsCount
                && mPositionMatrix != null
                && mPositionMatrix.length == mRows
                && mPositionMatrix[0].length == mColumns;

        if (!isUpdate) {
            initVariables();
        }

        fillConstraintMatrix(isUpdate);
    }


    @Override
    public void measure(int widthMode, int widthSize, int heightMode, int heightSize) {
        super.measure(widthMode, widthSize, heightMode, heightSize);
        mContainer = (ConstraintWidgetContainer) getParent();
        setupGrid(false);
        mContainer.add(mBoxWidgets);
    }

    @Override
    public void addToSolver(@Nullable LinearSystem system, boolean optimize) {
        super.addToSolver(system, optimize);
        addConstraints();
    }
}
