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

import java.util.Arrays;

/**
 * GridEngine class contains the main logic of the Grid Helper
 */
public class GridEngine {

    public static final int VERTICAL = 1;
    public static final int HORIZONTAL = 0;
    private static final int MAX_ROWS = 50; // maximum number of rows can be specified.
    private static final int MAX_COLUMNS = 50; // maximum number of columns can be specified.
    private static final int DEFAULT_SIZE = 3; // default rows and columns.

    /**
     * number of rows of the grid
     */
    private int mRows;

    /**
     * number of rows set by the XML or API
     */
    private int mRowsSet;

    /**
     * How many widgets need to be placed in the Grid
     */
    private int mNumWidgets;

    /**
     * number of columns of the grid
     */
    private int mColumns;

    /**
     * number of columns set by the XML or API
     */
    private int mColumnsSet;

    /**
     * string format of the input Spans
     */
    private String mStrSpans;

    /**
     * string format of the input Skips
     */
    private String mStrSkips;

    /**
     * orientation of the view arrangement - vertical or horizontal
     */
    private int mOrientation;

    /**
     * Indicates what is the next available position to place an widget
     */
    private int mNextAvailableIndex = 0;

    /**
     * A boolean matrix that tracks the positions that are occupied by skips and spans
     * true: available position
     * false: non-available position
     */
    private boolean[][] mPositionMatrix;

    /**
     * A int matrix that contains the positions where a widget would constraint to at each direction
     * Each row contains 4 values that indicate the position to constraint of a widget.
     * Example row: [left, top, right, bottom]
     */
    private int[][] mConstraintMatrix;

    public GridEngine() {}

    public GridEngine(int rows, int columns) {
        mRowsSet = rows;
        mColumnsSet = columns;
        if (rows > MAX_ROWS) {
            mRowsSet = DEFAULT_SIZE;
        }

        if (columns > MAX_COLUMNS) {
            mColumnsSet = DEFAULT_SIZE;
        }

        updateActualRowsAndColumns();
        initVariables();
    }

    public GridEngine(int rows, int columns, int numWidgets) {
        mRowsSet = rows;
        mColumnsSet = columns;
        mNumWidgets = numWidgets;

        if (rows > MAX_ROWS) {
            mRowsSet = DEFAULT_SIZE;
        }

        if (columns > MAX_COLUMNS) {
            mColumnsSet = DEFAULT_SIZE;
        }

        updateActualRowsAndColumns();

        if (numWidgets > mRows * mColumns || numWidgets < 1) {
            mNumWidgets = mRows * mColumns;
        }

        initVariables();
        fillConstraintMatrix(false);
    }

    /**
     * Initialize the relevant variables
     */
    private void initVariables() {
        mPositionMatrix = new boolean[mRows][mColumns];
        for (boolean[] row : mPositionMatrix) {
            Arrays.fill(row, true);
        }

        if (mNumWidgets > 0) {
            mConstraintMatrix = new int[mNumWidgets][4];
            for (int[] row : mConstraintMatrix) {
                Arrays.fill(row, -1);
            }
        }
    }

    /**
     * Convert a 1D index to a 2D index that has index for row and index for column
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
     * Convert a 1D index to a 2D index that has index for row and index for column
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
     * Check if the value of the spans/skips is valid
     *
     * @param str spans/skips in string format
     * @return true if it is valid else false
     */
    private boolean isSpansValid(CharSequence str) {
        if (str == null) {
            return false;
        }
        return true;
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
        if (!isSpansValid(str)) {
            return null;
        }

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

        if (mStrSkips != null && !mStrSkips.trim().isEmpty()) {
            int[][] mSkips = parseSpans(mStrSkips);
            if (mSkips != null) {
                handleSkips(mSkips);
            }
        }

        if (mStrSpans != null && !mStrSpans.trim().isEmpty()) {
            int[][] mSpans = parseSpans(mStrSpans);
            if (mSpans != null) {
                handleSpans(mSpans);
            }
        }

        addAllConstraintPositions();
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
     * add the constraint position info of a widget based on the input params
     *
     * @param widgetId the Id of the widget
     * @param row row position to place the view
     * @param column column position to place the view
     */
    private void addConstraintPosition(int widgetId, int row, int column,
                                       int rowSpan, int columnSpan) {

        mConstraintMatrix[widgetId][0] = column;
        mConstraintMatrix[widgetId][1] = row;
        mConstraintMatrix[widgetId][2] = column + columnSpan - 1;
        mConstraintMatrix[widgetId][3] = row + rowSpan - 1;
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
            addConstraintPosition(i, row, col,
                    spansMatrix[i][1], spansMatrix[i][2]);
        }
    }

    /**
     * Make positions in the grid unavailable based on the skips attr
     *
     * @param skipsMatrix a int matrix that contains skip information
     */
    private void handleSkips(int[][] skipsMatrix) {
        for (int i = 0; i < skipsMatrix.length; i++) {
            int row = getRowByIndex(skipsMatrix[i][0]);
            int col = getColByIndex(skipsMatrix[i][0]);
            if (!invalidatePositions(row, col,
                    skipsMatrix[i][1], skipsMatrix[i][2])) {
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
     * Arrange the views in the constraint_referenced_ids
     */
    private void addAllConstraintPositions() {
        int position;

        for (int i = 0; i < mNumWidgets; i++) {

            // Already added ConstraintPosition
            if (leftOfWidget(i) != -1) {
                continue;
            }

            position = getNextPosition();
            int row = getRowByIndex(position);
            int col = getColByIndex(position);
            if (position == -1) {
                // no more available position.
                return;
            }
            addConstraintPosition(i, row, col, 1, 1);
        }
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
                mRows = (mNumWidgets + mColumns - 1) / mColumnsSet; // round up
            } else  if (mRowsSet > 0) {
                mRows = mRowsSet;
                mColumns = (mNumWidgets + mRowsSet - 1) / mRowsSet; // round up
            } else { // as close to square as possible favoring more rows
                mRows = (int)  (1.5 + Math.sqrt(mNumWidgets));
                mColumns = (mNumWidgets + mRows - 1) / mRows;
            }
        } else {
            mRows = mRowsSet;
            mColumns = mColumnsSet;
        }
    }

    /**
     * Set up the Grid engine.
     */
    public void setup() {
        boolean isUpdate = true;

        if (mConstraintMatrix == null
                || mConstraintMatrix.length != mNumWidgets
                || mPositionMatrix == null
                || mPositionMatrix.length != mRows
                || mPositionMatrix[0].length != mColumns) {
            isUpdate = false;
        }

        if (!isUpdate) {
            initVariables();
        }

        fillConstraintMatrix(isUpdate);
    }

    /**
     * set new spans value
     *
     * @param spans new spans value
     */
    public void setSpans(CharSequence spans) {
        if (mStrSpans != null && mStrSpans.equals(spans.toString())) {
            return;
        }

        mStrSpans = spans.toString();
    }

    /**
     * set new skips value
     *
     * @param skips new spans value
     */
    public void setSkips(String skips) {
        if (mStrSkips != null && mStrSkips.equals(skips)) {
            return;
        }

        mStrSkips = skips;

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
     * Set new NumWidgets value
     * @param num how many widgets to be arranged in Grid
     */
    public void setNumWidgets(int num) {
        if (num > mRows * mColumns) {
            return;
        }

        mNumWidgets = num;
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
    }

    /**
     * Get the boxView for the widget i to add a constraint on the left
     *
     * @param i the widget that has the order as i in the constraint_reference_ids
     * @return the boxView to add a constraint on the left
     */
    public int leftOfWidget(int i) {
        if (mConstraintMatrix == null || i >= mConstraintMatrix.length) {
            return 0;
        }
        return mConstraintMatrix[i][0];
    }

    /**
     * Get the boxView for the widget i to add a constraint on the top
     *
     * @param i the widget that has the order as i in the constraint_reference_ids
     * @return the boxView to add a constraint on the top
     */
    public int topOfWidget(int i) {
        if (mConstraintMatrix == null || i >= mConstraintMatrix.length) {
            return 0;
        }
        return mConstraintMatrix[i][1];
    }

    /**
     * Get the boxView for the widget i to add a constraint on the right
     *
     * @param i the widget that has the order as i in the constraint_reference_ids
     * @return the boxView to add a constraint on the right
     */
    public int rightOfWidget(int i) {
        if (mConstraintMatrix == null || i >= mConstraintMatrix.length) {
            return 0;
        }
        return mConstraintMatrix[i][2];
    }

    /**
     * Get the boxView for the widget i to add a constraint on the bottom
     *
     * @param i the widget that has the order as i in the constraint_reference_ids
     * @return the boxView to add a constraint on the bottom
     */
    public int bottomOfWidget(int i) {
        if (mConstraintMatrix == null || i >= mConstraintMatrix.length) {
            return 0;
        }
        return mConstraintMatrix[i][3];
    }
}
