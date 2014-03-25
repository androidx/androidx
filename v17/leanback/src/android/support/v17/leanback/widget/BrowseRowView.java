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

import android.support.v17.leanback.R;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * BrowseRowView contains a horizontal grid view.
 */
public final class BrowseRowView extends LinearLayout {

    private HorizontalGridView mGridView;

    public BrowseRowView(Context context) {
        this(context, null);
    }

    public BrowseRowView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.browseRowViewStyle);
    }

    public BrowseRowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.lb_browse_row, this);

        mGridView = (HorizontalGridView) findViewById(R.id.row_list);

        final Resources.Theme theme = context.getTheme();
        final TypedArray array = theme.obtainStyledAttributes(attrs, R.styleable.BrowseRowView,
                defStyle, R.style.Widget_Leanback_BrowseRowView);

        int n = array.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = array.getIndex(i);

            switch (attr) {
                case R.styleable.BrowseRowView_browseItemMargin:
                    int margin = array.getDimensionPixelSize(attr, 0);
                    mGridView.setItemMargin(margin);
                    break;
            }
        }
        array.recycle();

        setOrientation(LinearLayout.VERTICAL);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
    }

    public HorizontalGridView getGridView() {
        return mGridView;
    }

}
