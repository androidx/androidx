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
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * RowContainerView wraps header and user defined row view
 */
final class RowContainerView extends LinearLayout {

    private ViewGroup mHeaderDock;
    private Drawable mForeground;
    private boolean mForegroundBoundsChanged = true;

    public RowContainerView(Context context) {
        this(context, null, 0);
    }

    public RowContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RowContainerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(VERTICAL);
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.lb_row_container, this);

        mHeaderDock = (ViewGroup) findViewById(R.id.lb_row_container_header_dock);
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void addHeaderView(View headerView) {
        if (mHeaderDock.indexOfChild(headerView) < 0) {
            mHeaderDock.addView(headerView, 0);
        }
    }

    public void removeHeaderView(View headerView) {
        if (mHeaderDock.indexOfChild(headerView) >= 0) {
            mHeaderDock.removeView(headerView);
        }
    }

    public void addRowView(View view) {
        addView(view);
    }

    public void showHeader(boolean show) {
        mHeaderDock.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setForeground(Drawable d) {
        mForeground = d;
        setWillNotDraw(mForeground == null);
        invalidate();
    }

    public void setForegroundColor(int color) {
        if (mForeground instanceof ColorDrawable) {
            ((ColorDrawable) mForeground.mutate()).setColor(color);
            invalidate();
        } else {
            setForeground(new ColorDrawable(color));
        }
    }

    public Drawable getForeground() {
        return mForeground;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundBoundsChanged = true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mForeground != null) {
            if (mForegroundBoundsChanged) {
                mForegroundBoundsChanged = false;
                mForeground.setBounds(0, 0, getWidth(), getHeight());
            }
            mForeground.draw(canvas);
        }
    }
}
