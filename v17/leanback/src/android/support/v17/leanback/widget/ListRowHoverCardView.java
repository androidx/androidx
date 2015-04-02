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
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * ListRowHoverCardView contains a title and description.
 */
public final class ListRowHoverCardView extends LinearLayout {

    private final TextView mTitleView;
    private final TextView mDescriptionView;

    public ListRowHoverCardView(Context context) {
       this(context, null);
    }

    public ListRowHoverCardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListRowHoverCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.lb_list_row_hovercard, this);
        mTitleView = (TextView) findViewById(R.id.title);
        mDescriptionView = (TextView) findViewById(R.id.description);
    }

    /**
     * Returns the title text.
     */
    public final CharSequence getTitle() {
        return mTitleView.getText();
    }

    /**
     * Sets the title text.
     */
    public final void setTitle(CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            mTitleView.setText(text);
            mTitleView.setVisibility(View.VISIBLE);
        } else {
            mTitleView.setVisibility(View.GONE);
        }
    }

    /**
     * Returns the description text.
     */
    public final CharSequence getDescription() {
        return mDescriptionView.getText();
    }

    /**
     * Sets the description text.
     */
    public final void setDescription(CharSequence text) {
        if (!TextUtils.isEmpty(text)) {
            mDescriptionView.setText(text);
            mDescriptionView.setVisibility(View.VISIBLE);
        } else {
            mDescriptionView.setVisibility(View.GONE);
        }
    }
}
