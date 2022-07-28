/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.utils.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.constraintlayout.widget.R;

/**
 * A view that is useful for prototyping layouts. <b>Added in 2.0</b>
 * <p>
 * Basic view that can draw a label (by default the view id),
 * along with diagonals. Useful as a temporary mock view while building up a UI.
 * </p>
 */
public class MockView extends View {

    private Paint mPaintDiagonals = new Paint();
    private Paint mPaintText = new Paint();
    private Paint mPaintTextBackground = new Paint();
    private boolean mDrawDiagonals = true;
    private boolean mDrawLabel = true;
    protected String mText = null;
    private Rect mTextBounds = new Rect();
    private int mDiagonalsColor = Color.argb(255, 0, 0, 0);
    private int mTextColor = Color.argb(255, 200, 200, 200);
    private int mTextBackgroundColor = Color.argb(255, 50, 50, 50);
    private int mMargin = 4;

    public MockView(Context context) {
        super(context);
        init(context, null);
    }

    public MockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MockView);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MockView_mock_label) {
                    mText = a.getString(attr);
                } else if (attr == R.styleable.MockView_mock_showDiagonals) {
                    mDrawDiagonals = a.getBoolean(attr, mDrawDiagonals);
                } else if (attr == R.styleable.MockView_mock_diagonalsColor) {
                    mDiagonalsColor = a.getColor(attr, mDiagonalsColor);
                } else if (attr == R.styleable.MockView_mock_labelBackgroundColor) {
                    mTextBackgroundColor = a.getColor(attr, mTextBackgroundColor);
                } else if (attr == R.styleable.MockView_mock_labelColor) {
                    mTextColor = a.getColor(attr, mTextColor);
                } else if (attr == R.styleable.MockView_mock_showLabel) {
                    mDrawLabel = a.getBoolean(attr, mDrawLabel);
                }
            }
            a.recycle();
        }
        if (mText == null) {
            try {
                mText = context.getResources().getResourceEntryName(getId());
            } catch (Exception ex) {
            }
        }
        mPaintDiagonals.setColor(mDiagonalsColor);
        mPaintDiagonals.setAntiAlias(true);
        mPaintText.setColor(mTextColor);
        mPaintText.setAntiAlias(true);
        mPaintTextBackground.setColor(mTextBackgroundColor);
        mMargin = Math.round(mMargin * (getResources().getDisplayMetrics().xdpi
                / DisplayMetrics.DENSITY_DEFAULT));
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (mDrawDiagonals) {
            w--;
            h--;
            canvas.drawLine(0, 0, w, h, mPaintDiagonals);
            canvas.drawLine(0, h, w, 0, mPaintDiagonals);
            canvas.drawLine(0, 0, w, 0, mPaintDiagonals);
            canvas.drawLine(w, 0, w, h, mPaintDiagonals);
            canvas.drawLine(w, h, 0, h, mPaintDiagonals);
            canvas.drawLine(0, h, 0, 0, mPaintDiagonals);
        }
        if (mText != null && mDrawLabel) {
            mPaintText.getTextBounds(mText, 0, mText.length(), mTextBounds);
            float tx = (w - mTextBounds.width()) / 2f;
            float ty = (h - mTextBounds.height()) / 2f + mTextBounds.height();
            mTextBounds.offset((int) tx, (int) ty);
            mTextBounds.set(mTextBounds.left - mMargin, mTextBounds.top - mMargin,
                    mTextBounds.right + mMargin, mTextBounds.bottom + mMargin);
            canvas.drawRect(mTextBounds, mPaintTextBackground);
            canvas.drawText(mText, tx, ty, mPaintText);
        }
    }
}
