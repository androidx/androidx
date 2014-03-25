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

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * A view that shows items in a vertically scrolling list. The items come from
 * the {@link RecyclerView.Adapter} associated with this view.
 */
public class VerticalGridView extends BaseGridView {

    public VerticalGridView(Context context) {
        this(context, null);
    }

    public VerticalGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerticalGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager.setOrientation(RecyclerView.VERTICAL);
        initAttributes(context, attrs);
    }

    protected void initAttributes(Context context, AttributeSet attrs) {
        initBaseGridViewAttributes(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbVerticalGridView);
        setColumnWidth(a.getDimensionPixelSize(R.styleable.lbVerticalGridView_columnWidth, 0));
        setNumColumns(a.getInt(R.styleable.lbVerticalGridView_numberOfColumns, 1));
        a.recycle();
    }

    /**
     * Set the number of columns.
     */
    public void setNumColumns(int numColumns) {
        mLayoutManager.setNumRows(numColumns);
        requestLayout();
    }

    /**
     * Set the column width.
     */
    public void setColumnWidth(int width) {
        mLayoutManager.setRowHeight(width);
        requestLayout();
    }
}
