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
package androidx.constraintlayout.helper.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.R;
import androidx.constraintlayout.widget.VirtualLayout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A helper class that helps arrange widgets in a grid form
 *
 * <h2>Grid</h2>
 * <table summary="Grid attributes">
 *   <tr>
 *     <th>Attributes</th><th>Description</th>
 *   </tr>
 *   <tr>
 *     <td>grid_rows</td>
 *     <td>Indicates the number of rows will be created for the grid form.</td>
 *   </tr>
 *   <tr>
 *     <td>grid_columns</td>
 *     <td>Indicates the number of columns will be created for the grid form.</td>
 *   </tr>
 *   <tr>
 *     <td>grid_rowWeights</td>
 *     <td>Specifies the weight of each row in the grid form (default value is 1).</td>
 *   </tr>
 *   <tr>
 *     <td>grid_columnWeights</td>
 *     <td>Specifies the weight of each column in the grid form (default value is 1).</td>
 *   </tr>
 *   <tr>
 *     <td>grid_spans</td>
 *     <td>Offers the capability to span a widget across multiple rows and columns</td>
 *   </tr>
 *   <tr>
 *     <td>grid_skips</td>
 *     <td>Enables skip certain positions in the grid and leave them empty</td>
 *   </tr>
 *   <tr>
 *     <td>grid_orientation</td>
 *     <td>Defines how the associated widgets will be arranged - vertically or horizontally</td>
 *   </tr>
 *   <tr>
 *     <td>grid_horizontalGaps</td>
 *     <td>Adds margin horizontally between widgets</td>
 *   </tr>
 *   <tr>
 *      <td>grid_verticalGaps</td>
 *     <td>Adds margin vertically between widgets</td>
 *   </tr>
 * </table>
 */
public class Grid extends VirtualLayout {
    private static final String TAG = "Grid";
    public static final int VERTICAL = 1;
    public static final int HORIZONTAL = 0;
    private static final boolean DEBUG_BOXES = false;
    private final int mMaxRows = 50; // maximum number of rows can be specified.
    private final int mMaxColumns = 50; // maximum number of columns can be specified.
    // private final ConstraintSet mConstraintSet = new ConstraintSet();

    private View[] mBoxViews;
    ConstraintLayout mContainer;

    /**
     * number of rows of the grid
     */
    private int mRows;

    /**
     * number of rows set by the XML or API
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
     * string format of the input Spans
     */
    private String mStrSpans;

    /**
     * string format of the input Skips
     */
    private String mStrSkips;

    /**
     * string format of the row weight
     */
    private String mStrRowWeights;

    /**
     * string format of the column weight
     */
    private String mStrColumnWeights;

    /**
     * Horizontal gaps in Dp
     */
    private float mHorizontalGaps;

    /**
     * Vertical gaps in Dp
     */
    private float mVerticalGaps;

    /**
     * orientation of the view arrangement - vertical or horizontal
     */
    private int mOrientation;

    /**
     * Indicates what is the next available position to place an widget
     */
    private int mNextAvailableIndex = 0;

    /**
     * Indicates whether the input attributes need to be validated
     */
    private boolean mValidateInputs;

    /**
     * Indicates whether to use RTL layout direction
     */
    @SuppressWarnings("unused")
    private boolean mUseRtl;

    /**
     * A integer matrix that tracks the positions that are occupied by skips and spans
     * true: available position
     * false: non-available position
     */
    private boolean[][] mPositionMatrix;

    /**
     * Store the view ids of handled spans
     */
    Set<Integer> mSpanIds = new HashSet<>();

    /**
     * Ids of the boxViews
     */
    private int[] mBoxViewIds;

    public Grid(Context context) {
        super(context);
    }

    public Grid(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Grid(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mUseViewMeasure = true;

        // Parse the relevant attributes from layout xml
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs,
                    R.styleable.Grid);
            final int n = a.getIndexCount();

            for (int i = 0; i < n; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.Grid_grid_rows) {
                    mRowsSet = a.getInteger(attr, 0);
                } else if (attr == R.styleable.Grid_grid_columns) {
                    mColumnsSet = a.getInteger(attr, 0);
                } else if (attr == R.styleable.Grid_grid_spans) {
                    mStrSpans = a.getString(attr);
                } else if (attr == R.styleable.Grid_grid_skips) {
                    mStrSkips = a.getString(attr);
                } else if (attr == R.styleable.Grid_grid_rowWeights) {
                    mStrRowWeights = a.getString(attr);
                } else if (attr == R.styleable.Grid_grid_columnWeights) {
                    mStrColumnWeights = a.getString(attr);
                } else if (attr == R.styleable.Grid_grid_orientation) {
                    mOrientation = a.getInt(attr, 0);
                } else if (attr == R.styleable.Grid_grid_horizontalGaps) {
                    mHorizontalGaps = a.getDimension(attr, 0);
                } else if (attr == R.styleable.Grid_grid_verticalGaps) {
                    mVerticalGaps = a.getDimension(attr, 0);
                } else if (attr == R.styleable.Grid_grid_validateInputs) {
                    // @TODO handle validation
                    mValidateInputs = a.getBoolean(attr, false);
                } else if (attr == R.styleable.Grid_grid_useRtl) {
                    // @TODO handle RTL
                    mUseRtl = a.getBoolean(attr, false);
                }
            }

            updateActualRowsAndColumns();
            initVariables();
            a.recycle();
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
                mRows = (mCount + mColumns - 1) / mColumnsSet; // round up
            } else if (mRowsSet > 0) {
                mRows = mRowsSet;
                mColumns = (mCount + mRowsSet - 1) / mRowsSet; // round up
            } else { // as close to square as possible favoring more rows
                mRows = (int) (1.5 + Math.sqrt(mCount));
                mColumns = (mCount + mRows - 1) / mRows;
            }
        } else {
            mRows = mRowsSet;
            mColumns = mColumnsSet;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mContainer = (ConstraintLayout) getParent();

        generateGrid(false);
    }

    /**
     * generate the Grid form based on the input attributes
     *
     * @param isUpdate whether to update the existing grid (true) or create a new one (false)
     * @return true if all the inputs are valid else false
     */
    private boolean generateGrid(boolean isUpdate) {
        if (mContainer == null || mRows < 1 || mColumns < 1) {
            return false;
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
        boolean isSuccess = true;

        buildBoxes();

        if (mStrSkips != null && !mStrSkips.trim().isEmpty()) {
            int[][] mSkips = parseSpans(mStrSkips);
            if (mSkips != null) {
                isSuccess &= handleSkips(mSkips);
            }
        }

        if (mStrSpans != null && !mStrSpans.trim().isEmpty()) {
            int[][] mSpans = parseSpans(mStrSpans);
            if (mSpans != null) {
                isSuccess &= handleSpans(mIds, mSpans);
            }
        }
        isSuccess &= arrangeWidgets();
        return isSuccess || !mValidateInputs;
    }

    /**
     * Initialize the relevant variables
     */
    private void initVariables() {
        mPositionMatrix = new boolean[mRows][mColumns];
        for (boolean[] row : mPositionMatrix) {
            Arrays.fill(row, true);
        }
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

    private ConstraintLayout.LayoutParams params(View v) {
        return (ConstraintLayout.LayoutParams) v.getLayoutParams();
    }

    /**
     * Connect the view to the corresponding viewBoxes based on the input params
     *
     * @param view   the Id of the view
     * @param row    row position to place the view
     * @param column column position to place the view
     */

    private void connectView(View view, int row, int column, int rowSpan, int columnSpan) {
        ConstraintLayout.LayoutParams params = params(view);
        // @TODO handle RTL
        // Connect the 4 sides
        params.leftToLeft = mBoxViewIds[column];
        params.topToTop = mBoxViewIds[row];
        params.rightToRight = mBoxViewIds[column + columnSpan - 1];
        params.bottomToBottom = mBoxViewIds[row + rowSpan - 1];
        view.setLayoutParams(params);
    }

    /**
     * Arrange the views in the constraint_referenced_ids
     *
     * @return true if all the widgets can be arranged properly else false
     */
    private boolean arrangeWidgets() {
        int position;
        View[] views = getViews(mContainer);
        // @TODO handle RTL
        for (int i = 0; i < mCount; i++) {
            if (mSpanIds.contains(mIds[i])) {
                // skip the viewId that's already handled by handleSpans
                continue;
            }

            position = getNextPosition();
            int row = getRowByIndex(position);
            int col = getColByIndex(position);
            if (position == -1) {
                // no more available position.
                return false;
            }

            connectView(views[i], row, col, 1, 1);
        }
        return true;
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
     * Get the next available position for widget arrangement.
     *
     * @return int[] -> [row, column]
     */
    private int getNextPosition() {
        //  int[] position = new int[] {0, 0};
        int position = 0;
        boolean positionFound = false;

        while (!positionFound) {
            if (mNextAvailableIndex >= mRows * mColumns) {
                return -1;
            }

            // position = getPositionByIndex(mNextAvailableIndex);
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
     * Check if the value of the spans/skips is valid
     *
     * @param str spans/skips in string format
     * @return true if it is valid else false
     */
    private boolean isSpansValid(@SuppressWarnings("unused") CharSequence str) {
        // TODO: check string has a valid format.
        return true;
    }

    /**
     * Check if the value of the rowWeights or columnsWeights is valid
     *
     * @param str rowWeights/columnsWeights in string format
     * @return true if it is valid else false
     */
    private boolean isWeightsValid(@SuppressWarnings("unused") String str) {
        // TODO: check string has a valid format.
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
     * Handle the span use cases
     *
     * @param spansMatrix a int matrix that contains span information
     * @return true if the input spans is valid else false
     */
    private boolean handleSpans(int[] mId, int[][] spansMatrix) {
        View[] views = getViews(mContainer);
        for (int i = 0; i < spansMatrix.length; i++) {
            int row = getRowByIndex(spansMatrix[i][0]);
            int col = getColByIndex(spansMatrix[i][0]);
            if (!invalidatePositions(row, col,
                    spansMatrix[i][1], spansMatrix[i][2])) {
                return false;
            }

            connectView(views[i], row, col,
                    spansMatrix[i][1], spansMatrix[i][2]);

            mSpanIds.add(mId[i]);
        }
        return true;
    }

    /**
     * Make positions in the grid unavailable based on the skips attr
     *
     * @param skipsMatrix a int matrix that contains skip information
     * @return true if all the skips are valid else false
     */
    private boolean handleSkips(int[][] skipsMatrix) {
        for (int i = 0; i < skipsMatrix.length; i++) {
            int row = getRowByIndex(skipsMatrix[i][0]);
            int col = getColByIndex(skipsMatrix[i][0]);
            if (!invalidatePositions(row, col,
                    skipsMatrix[i][1], skipsMatrix[i][2])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make the specified positions in the grid unavailable.
     *
     * @param startRow    the row of the staring position
     * @param startColumn the column of the staring position
     * @param rowSpan     how many rows to span
     * @param columnSpan  how many columns to span
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
     * Visualize the boxViews that are used to constraint widgets.
     *
     * @param canvas canvas to visualize the boxViews
     */
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Visualize the viewBoxes if isInEditMode() is true
        if (!isInEditMode()) {
            return;
        }
        @SuppressLint("DrawAllocation")
        Paint paint = new Paint(); // used only during design time
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        int myTop = getTop();
        int myLeft = getLeft();
        int myBottom = getBottom();
        int myRight = getRight();
        for (View box : mBoxViews) {
            int l = box.getLeft() - myLeft;
            int t = box.getTop() - myTop;
            int r = box.getRight() - myLeft;
            int b = box.getBottom() - myTop;
            canvas.drawRect(l, 0, r, myBottom - myTop, paint);
            canvas.drawRect(0, t, myRight - myLeft, b, paint);
        }
    }

    /**
     * Set chain between boxView horizontally
     */
    private void setBoxViewHorizontalChains() {
        int gridId = getId();
        int maxVal = Math.max(mRows, mColumns);

        float[] columnWeights = parseWeights(mColumns, mStrColumnWeights);
        ConstraintLayout.LayoutParams params = params(mBoxViews[0]);
        // chain all the views on the longer side (either horizontal or vertical)
        if (mColumns == 1) {
            clearHParams(mBoxViews[0]);
            params.leftToLeft = gridId;
            params.rightToRight = gridId;
            mBoxViews[0].setLayoutParams(params);
            return;
        }


        //  chains are grid <- box <-> box <-> box -> grid

        for (int i = 0; i < mColumns; i++) {
            params = params(mBoxViews[i]);
            clearHParams(mBoxViews[i]);
            if (columnWeights != null) {
                params.horizontalWeight = columnWeights[i];
            }
            if (i > 0) {
                params.leftToRight = mBoxViewIds[i - 1];
            } else {
                params.leftToLeft = gridId;
            }
            if (i < mColumns - 1) {
                params.rightToLeft = mBoxViewIds[i + 1];
            } else {
                params.rightToRight = gridId;
            }
            if (i > 0) {
                params.leftMargin = (int) mHorizontalGaps;
            }
            mBoxViews[i].setLayoutParams(params);
        }
        // excess boxes are connected to grid those sides are not use
        // for efficiency they should be connected to parent
        for (int i = mColumns; i < maxVal; i++) {
            params = params(mBoxViews[i]);
            clearHParams(mBoxViews[i]);
            params.leftToLeft = gridId;
            params.rightToRight = gridId;
            mBoxViews[i].setLayoutParams(params);
        }
    }

    /**
     * Set chain between boxView vertically
     */
    private void setBoxViewVerticalChains() {
        int gridId = getId();
        int maxVal = Math.max(mRows, mColumns);

        float[] rowWeights = parseWeights(mRows, mStrRowWeights);
        ConstraintLayout.LayoutParams params;
        // chain all the views on the longer side (either horizontal or vertical)
        if (mRows == 1) {
            params = params(mBoxViews[0]);
            clearVParams(mBoxViews[0]);
            params.topToTop = gridId;
            params.bottomToBottom = gridId;
            mBoxViews[0].setLayoutParams(params);
            return;
        }
        // chains are constrained like this: grid <- box <-> box <-> box -> grid
        for (int i = 0; i < mRows; i++) {
            params = params(mBoxViews[i]);
            clearVParams(mBoxViews[i]);
            if (rowWeights != null) {
                params.verticalWeight = rowWeights[i];
            }
            if (i > 0) {
                params.topToBottom = mBoxViewIds[i - 1];
            } else {
                params.topToTop = gridId;
            }
            if (i < mRows - 1) {
                params.bottomToTop = mBoxViewIds[i + 1];
            } else {
                params.bottomToBottom = gridId;
            }
            if (i > 0) {
                params.topMargin = (int) mHorizontalGaps;
            }
            mBoxViews[i].setLayoutParams(params);
        }

        // excess boxes are connected to grid those sides are not use
        // for efficiency they should be connected to parent
        for (int i = mRows; i < maxVal; i++) {
            params = params(mBoxViews[i]);
            clearVParams(mBoxViews[i]);
            params.topToTop = gridId;
            params.bottomToBottom = gridId;
            mBoxViews[i].setLayoutParams(params);
        }
    }

    /**
     * Create a new boxView
     * @return boxView
     */
    private View makeNewView() {
        View v = new View(getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            v.setId(View.generateViewId());
        }
        v.setVisibility(INVISIBLE);
        if (DEBUG_BOXES) {
            v.setVisibility(VISIBLE);
            v.setBackgroundColor(0xFF880088);
        }
        ConstraintLayout.LayoutParams params =
                new ConstraintLayout.LayoutParams(0, 0);

        mContainer.addView(v, params);
        return v;
    }

    /**
     * Clear vertical related layout params
     * @param view view that has the layout params to be cleared
     */
    private void clearVParams(View view) {
        ConstraintLayout.LayoutParams params = params(view);

        params.verticalWeight  = ConstraintSet.UNSET;
        params.topToBottom = ConstraintSet.UNSET;
        params.topToTop = ConstraintSet.UNSET;
        params.bottomToTop = ConstraintSet.UNSET;
        params.bottomToBottom  = ConstraintSet.UNSET;
        params.topMargin = ConstraintSet.UNSET;

        view.setLayoutParams(params);
    }

    /**
     * Clear horizontal related layout params
     * @param view view that has the layout params to be cleared
     */
    private void clearHParams(View view) {
        ConstraintLayout.LayoutParams params = params(view);

        params.horizontalWeight = ConstraintSet.UNSET;
        params.leftToRight = ConstraintSet.UNSET;
        params.leftToLeft = ConstraintSet.UNSET;
        params.rightToLeft  = ConstraintSet.UNSET;
        params.rightToRight = ConstraintSet.UNSET;
        params.leftMargin  = ConstraintSet.UNSET;

        view.setLayoutParams(params);
    }

    /**
     * create boxViews for constraining widgets
     */
    private void buildBoxes() {
        int boxCount = Math.max(mRows, mColumns);
        if (mBoxViews == null) { // no box views build all
            mBoxViews = new View[boxCount];
            for (int i = 0; i < mBoxViews.length; i++) {
                mBoxViews[i] = makeNewView(); // need to remove old Views
            }
        } else {
            if (boxCount != mBoxViews.length) {
                View[] temp = new View[boxCount];
                for (int i = 0; i < boxCount; i++) {
                    if (i < mBoxViews.length) { // use old one
                        temp[i] = mBoxViews[i];
                    } else { // make new one
                        temp[i] = makeNewView();
                    }
                }
                // remove excess
                for (int j = boxCount; j < mBoxViews.length; j++) {
                    View view = mBoxViews[j];
                    mContainer.removeView(view);
                }
                mBoxViews = temp;
            }
        }

        mBoxViewIds = new int[boxCount];
        for (int i = 0; i < mBoxViews.length; i++) {
            mBoxViewIds[i] = mBoxViews[i].getId();
        }

        setBoxViewVerticalChains();
        setBoxViewHorizontalChains();
    }

    /**
     * get the value of rows
     *
     * @return the value of rows
     */
    public int getRows() {
        return mRowsSet;
    }

    /**
     * set new rows value and also invoke initVariables and invalidate
     *
     * @param rows new rows value
     */
    public void setRows(int rows) {
        if (rows > mMaxRows) {
            return;
        }

        if (mRowsSet == rows) {
            return;
        }

        mRowsSet = rows;
        updateActualRowsAndColumns();

        initVariables();
        generateGrid(false);
        invalidate();
    }

    /**
     * get the value of columns
     *
     * @return the value of columns
     */
    public int getColumns() {
        return mColumnsSet;
    }

    /**
     * set new columns value and also invoke initVariables and invalidate
     *
     * @param columns new rows value
     */
    public void setColumns(int columns) {
        if (columns > mMaxColumns) {
            return;
        }

        if (mColumnsSet == columns) {
            return;
        }

        mColumnsSet = columns;
        updateActualRowsAndColumns();

        initVariables();
        generateGrid(false);
        invalidate();
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
     * set new orientation value and also invoke invalidate
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
        generateGrid(true);
        invalidate();
    }

    /**
     * get the string value of spans
     *
     * @return the string value of spans
     */
    public String getSpans() {
        return mStrSpans;
    }

    /**
     * set new spans value and also invoke invalidate
     *
     * @param spans new spans value
     */
    public void setSpans(CharSequence spans) {
        if (!isSpansValid(spans)) {
            return;
        }

        if (mStrSpans != null && mStrSpans.contentEquals(spans)) {
            return;
        }

        mStrSpans = spans.toString();
        generateGrid(true);
        invalidate();
    }

    /**
     * get the string value of skips
     *
     * @return the string value of skips
     */
    public String getSkips() {
        return mStrSkips;
    }

    /**
     * set new skips value and also invoke invalidate
     *
     * @param skips new spans value
     */
    public void setSkips(String skips) {
        if (!isSpansValid(skips)) {
            return;
        }

        if (mStrSkips != null && mStrSkips.equals(skips)) {
            return;
        }

        mStrSkips = skips;
        generateGrid(true);
        invalidate();
    }

    /**
     * get the string value of rowWeights
     *
     * @return the string value of rowWeights
     */
    public String getRowWeights() {
        return mStrRowWeights;
    }

    /**
     * set new rowWeights value and also invoke invalidate
     *
     * @param rowWeights new rowWeights value
     */
    public void setRowWeights(String rowWeights) {
        if (!isWeightsValid(rowWeights)) {
            return;
        }

        if (mStrRowWeights != null && mStrRowWeights.equals(rowWeights)) {
            return;
        }

        mStrRowWeights = rowWeights;
        generateGrid(true);
        invalidate();
    }

    /**
     * get the string value of columnWeights
     *
     * @return the string value of columnWeights
     */
    public String getColumnWeights() {
        return mStrColumnWeights;
    }

    /**
     * set new columnWeights value and also invoke invalidate
     *
     * @param columnWeights new columnWeights value
     */
    public void setColumnWeights(String columnWeights) {
        if (!isWeightsValid(columnWeights)) {
            return;
        }

        if (mStrColumnWeights != null && mStrColumnWeights.equals(columnWeights)) {
            return;
        }

        mStrColumnWeights = columnWeights;
        generateGrid(true);
        invalidate();
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
        generateGrid(true);
        invalidate();
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
        generateGrid(true);
        invalidate();
    }
}
