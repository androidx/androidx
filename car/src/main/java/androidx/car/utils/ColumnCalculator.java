/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.car.utils;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.Px;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.car.R;

/**
 * Utility class that calculates the size of the columns that will fit on the screen. A column's
 * width is determined by the size of the margins and gutters (space between the columns) that fit
 * on-screen.
 *
 * <p>Refer to the appropriate dimens and integers for the size of the margins and number of
 * columns.
 */
public class ColumnCalculator {
    private static final String TAG = "ColumnCalculator";

    private static ColumnCalculator sInstance;
    private static int sScreenWidth;

    private int mNumOfColumns;
    private int mNumOfGutters;
    private int mColumnWidth;
    private int mGutterSize;

    /**
     * Gets an instance of the {@link ColumnCalculator}. If this is the first time that this
     * method has been called, then the given {@link Context} will be used to retrieve resources.
     *
     * @param context The current calling Context.
     * @return An instance of {@link ColumnCalculator}.
     */
    public static ColumnCalculator getInstance(Context context) {
        if (sInstance == null) {
            WindowManager windowManager = (WindowManager) context.getSystemService(
                    Context.WINDOW_SERVICE);
            DisplayMetrics displayMetrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            sScreenWidth = displayMetrics.widthPixels;

            sInstance = new ColumnCalculator(context);
        }

        return sInstance;
    }

    private ColumnCalculator(Context context) {
        Resources res = context.getResources();
        int marginSize = res.getDimensionPixelSize(R.dimen.car_margin);
        mGutterSize = res.getDimensionPixelSize(R.dimen.car_gutter_size);
        mNumOfColumns = res.getInteger(R.integer.car_column_number);

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, String.format("marginSize: %d; numOfColumns: %d; gutterSize: %d",
                    marginSize, mNumOfColumns, mGutterSize));
        }

        // The gutters appear between each column. As a result, the number of gutters is one less
        // than the number of columns.
        mNumOfGutters = mNumOfColumns - 1;

        // Determine the spacing that is allowed to be filled by the columns by subtracting margins
        // on both size of the screen and the space taken up by the gutters.
        int spaceForColumns = sScreenWidth - (2 * marginSize) - (mNumOfGutters * mGutterSize);

        mColumnWidth = spaceForColumns / mNumOfColumns;

        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "mColumnWidth: " + mColumnWidth);
        }
    }

    /**
     * Returns the total number of columns that fit on the current screen.
     *
     * @return The total number of columns that fit on the screen.
     */
    public int getNumOfColumns() {
        return mNumOfColumns;
    }

    /**
     * Returns the size in pixels of each column. The column width is determined by the size of the
     * screen divided by the number of columns, size of gutters and margins.
     *
     * @return The width of a single column in pixels.
     */
    @Px
    public int getColumnWidth() {
        return mColumnWidth;
    }

    /**
     * Returns the total number of gutters that fit on screen. A gutter is the space between each
     * column. This value is always one less than the number of columns.
     *
     * @return The number of gutters on screen.
     */
    public int getNumOfGutters() {
        return mNumOfGutters;
    }

    /**
     * Returns the size of each gutter in pixels. A gutter is the space between each column.
     *
     * @return The size of a single gutter in pixels.
     */
    @Px
    public int getGutterSize() {
        return mGutterSize;
    }

    /**
     * Returns the size in pixels for the given number of columns. This value takes into account
     * the size of the gutter between the columns as well. For example, for a column span of four,
     * the size returned is the sum of four columns and three gutters.
     *
     * @return The size in pixels for a given column span.
     */
    @Px
    public int getSizeForColumnSpan(int columnSpan) {
        int gutterSpan = columnSpan - 1;
        return columnSpan * mColumnWidth + gutterSpan * mGutterSize;
    }
}
