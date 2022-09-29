/*
 * Copyright (C) 2018 The Android Open Source Project
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
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.ViewParent;

import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.widget.R;

/**
 * A view that is useful for prototyping Views that will move in MotionLayout. <b>Added in 2.0</b>
 * <p>
 * This view works with MotionLayout to demonstrate the motion of 25 points on the view.
 * It is based on MockView which draws a label (by default the view id),
 * along with diagonals.
 *
 * Useful as a deeper understanding of the motion of a view in a MotionLayout
 *
 * </p>
 */
public class MotionTelltales extends MockView {
    private static final String TAG = "MotionTelltales";
    private Paint mPaintTelltales = new Paint();
    MotionLayout mMotionLayout;
    float[] mVelocity = new float[2];
    Matrix mInvertMatrix = new Matrix();
    int mVelocityMode = MotionLayout.VELOCITY_POST_LAYOUT;
    int mTailColor = Color.MAGENTA;
    float mTailScale = 0.25f;

    public MotionTelltales(Context context) {
        super(context);
        init(context, null);
    }

    public MotionTelltales(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MotionTelltales(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MotionTelltales);
            final int count = a.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = a.getIndex(i);
                if (attr == R.styleable.MotionTelltales_telltales_tailColor) {
                    mTailColor = a.getColor(attr, mTailColor);
                } else if (attr == R.styleable.MotionTelltales_telltales_velocityMode) {
                    mVelocityMode = a.getInt(attr, mVelocityMode);
                } else if (attr == R.styleable.MotionTelltales_telltales_tailScale) {
                    mTailScale = a.getFloat(attr, mTailScale);
                }
            }
            a.recycle();
        }
        mPaintTelltales.setColor(mTailColor);
        mPaintTelltales.setStrokeWidth(5);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    /**
     * set the text
     * @param text
     */
    public void setText(CharSequence text) {
        mText = text.toString();
        requestLayout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        postInvalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Matrix matrix = getMatrix();
        matrix.invert(mInvertMatrix);
        if (mMotionLayout == null) {
            ViewParent vp = getParent();
            if (vp instanceof MotionLayout) {
                mMotionLayout = (MotionLayout) vp;
            }
            return;
        }
        int width = getWidth();
        int height = getHeight();
        float[] f = {0.1f, 0.25f, 0.5f, 0.75f, 0.9f};
        for (int y = 0; y < f.length; y++) {
            float py = f[y];
            for (int x = 0; x < f.length; x++) {
                float px = f[x];
                mMotionLayout.getViewVelocity(this, px, py, mVelocity, mVelocityMode);
                mInvertMatrix.mapVectors(mVelocity);

                float sx = (width * px);
                float sy = (height * py);
                float ex = sx - (mVelocity[0] * mTailScale);
                float ey = sy - (mVelocity[1] * mTailScale);
                mInvertMatrix.mapVectors(mVelocity);
                canvas.drawLine(sx, sy, ex, ey, mPaintTelltales);
            }

        }
    }
}
