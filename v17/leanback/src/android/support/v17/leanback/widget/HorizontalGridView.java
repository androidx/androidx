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
 * A view that shows items in a horizontal scrolling list. The items come from
 * the {@link RecyclerView.Adapter} associated with this view.
 */
public class HorizontalGridView extends BaseGridView {

    public HorizontalGridView(Context context) {
        this(context, null);
    }

    public HorizontalGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HorizontalGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        initAttributes(context, attrs);
    }

    protected void initAttributes(Context context, AttributeSet attrs) {
        initBaseGridViewAttributes(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.lbHorizontalGridView);
        setRowHeight(a.getDimensionPixelSize(R.styleable.lbHorizontalGridView_rowHeight, 0));
        setNumRows(a.getInt(R.styleable.lbHorizontalGridView_numberOfRows, 1));
        a.recycle();
    }

    /**
     * Set the number of rows.
     */
    public void setNumRows(int numRows) {
        mLayoutManager.setNumRows(numRows);
        requestLayout();
    }

    /**
     * Set the row height.
     */
    public void setRowHeight(int height) {
        mLayoutManager.setRowHeight(height);
        requestLayout();
    }
}
