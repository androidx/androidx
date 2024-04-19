/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.inflater.WearCurvedLineView.getSignForClockwise;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcDirection;
import androidx.wear.protolayout.renderer.R;
import androidx.wear.widget.ArcLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A container, which can be added to a ArcLayout, which occupies a fixed size.
 *
 * <p>This can have a size set, which it will consume in the layout. A single child can then be
 * added, which can optionally be aligned to the left, center or right of that fixed size.
 */
class SizedArcContainer extends ViewGroup implements ArcLayout.Widget {
    private static final float DEFAULT_SWEEP_ANGLE_DEGREES = 0;
    @NonNull private ArcDirection mArcDirection = ArcDirection.ARC_DIRECTION_CLOCKWISE;

    private float mSweepAngleDegrees;

    /** Layout parameters for children of {@link SizedArcContainer}. */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        public static final int ANGULAR_ALIGNMENT_START = 0;
        public static final int ANGULAR_ALIGNMENT_CENTER = 1;
        public static final int ANGULAR_ALIGNMENT_END = 2;

        @IntDef({ANGULAR_ALIGNMENT_START, ANGULAR_ALIGNMENT_CENTER, ANGULAR_ALIGNMENT_END})
        @RestrictTo(Scope.LIBRARY_GROUP)
        @Retention(RetentionPolicy.SOURCE)
        @interface AngularAlignment {}

        private static final int ANGULAR_ALIGNMENT_DEFAULT = ANGULAR_ALIGNMENT_CENTER;

        int mAngularAlignment;

        LayoutParams(@NonNull Context context, @NonNull AttributeSet attrs) {
            super(context, attrs);

            TypedArray arr =
                    context.obtainStyledAttributes(attrs, R.styleable.SizedArcContainer_Layout);
            mAngularAlignment =
                    arr.getInt(
                            R.styleable.SizedArcContainer_Layout_angularAlignment,
                            ANGULAR_ALIGNMENT_DEFAULT);
            arr.recycle();
        }

        LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(@NonNull ViewGroup.LayoutParams source) {
            super(source);
        }

        void setAngularAlignment(int angularAlignment) {
            mAngularAlignment = angularAlignment;
        }
    }

    SizedArcContainer(@NonNull Context context) {
        this(context, null);
    }

    SizedArcContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    SizedArcContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    SizedArcContainer(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, R.styleable.SizedArcContainer, defStyleAttr, defStyleRes);

        mSweepAngleDegrees =
                a.getFloat(
                        R.styleable.SizedArcContainer_sweepAngleDegrees,
                        DEFAULT_SWEEP_ANGLE_DEGREES);

        a.recycle();
    }

    /**
     * Sets the arc direction for this container. This controls what is considered START or END for
     * alignment.
     */
    void setArcDirection(@NonNull ArcDirection arcDirection) {
        mArcDirection = arcDirection;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            child.measure(widthMeasureSpec, heightMeasureSpec);

            setMeasuredDimension(
                    resolveSizeAndState(
                            child.getMeasuredWidth(), widthMeasureSpec, child.getMeasuredState()),
                    resolveSizeAndState(
                            child.getMeasuredHeight(),
                            heightMeasureSpec,
                            child.getMeasuredState()));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0) {
            View child = getChildAt(0);

            child.layout(0, 0, r - l, b - t);
        }
    }

    @Override
    public void addView(
            @NonNull View child, int index, @NonNull ViewGroup.LayoutParams layoutParams) {
        if (!(child instanceof ArcLayout.Widget)) {
            throw new IllegalArgumentException(
                    "SizedArcContainer can only contain instances of ArcLayout.Widget");
        }

        if (getChildCount() > 0) {
            throw new IllegalStateException("SizedArcContainer can only have a single child");
        }

        super.addView(child, index, layoutParams);
    }

    @Override
    protected boolean checkLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    @NonNull
    protected ViewGroup.LayoutParams generateLayoutParams(@NonNull ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public float getSweepAngleDegrees() {
        return mSweepAngleDegrees;
    }

    @Override
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        mSweepAngleDegrees = sweepAngleDegrees;
        requestLayout();
    }

    @Override
    public int getThickness() {
        ArcLayout.Widget child = getChild();

        if (child != null) {
            return child.getThickness();
        } else {
            return 0;
        }
    }

    @Override
    public void checkInvalidAttributeAsChild() {
        ArcLayout.Widget child = getChild();

        if (child != null) {
            child.checkInvalidAttributeAsChild();
        }
    }

    @Nullable
    private ArcLayout.Widget getChild() {
        if (getChildCount() == 0) {
            return null;
        }

        return (ArcLayout.Widget) getChildAt(0);
    }

    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        return false;
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, @NonNull View child, long drawingTime) {
        // ArcLayout pre-rotates the canvas, and expects this View to draw its contents around the
        // 12 o clock position. Because the child may be smaller than that though, we need to rotate
        // again, respecting the child alignment.
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp == null) {
            // This shouldn't happen...just default out
            return super.drawChild(canvas, child, drawingTime);
        }
        int alignment = lp.mAngularAlignment;
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;

        // The angular offset to the child's center, in either direction.
        float childSweep = ((ArcLayout.Widget) child).getSweepAngleDegrees();
        float offsetDegrees = (mSweepAngleDegrees - childSweep) / 2;

        int sign = getSignForClockwise(mArcDirection, /* defaultValue= */ 1);

        switch (alignment) {
            case LayoutParams.ANGULAR_ALIGNMENT_START:
                canvas.rotate(-1 * sign * offsetDegrees, centerX, centerY);
                return super.drawChild(canvas, child, drawingTime);
            case LayoutParams.ANGULAR_ALIGNMENT_END:
                canvas.rotate(sign * offsetDegrees, centerX, centerY);
                return super.drawChild(canvas, child, drawingTime);
            default:
                return super.drawChild(canvas, child, drawingTime);
        }
    }
}
