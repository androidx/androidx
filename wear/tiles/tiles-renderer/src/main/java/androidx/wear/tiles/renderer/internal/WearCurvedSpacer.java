/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.renderer.R;
import androidx.wear.widget.ArcLayout;

/**
 * A lightweight curved widget that represents space between elements inside an Arc. This does no
 * rendering; it simply causes the parent {@link ArcLayout} to advance by {@code sweepAngleDegrees}.
 */
public class WearCurvedSpacer extends View implements ArcLayout.Widget {

    private static final float DEFAULT_SWEEP_ANGLE_DEGREES = 0f;
    private static final int DEFAULT_THICKNESS_PX = 0;

    private float mSweepAngleDegrees;
    private int mThicknessPx;

    public WearCurvedSpacer(@NonNull Context context) {
        this(context, null);
    }

    public WearCurvedSpacer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearCurvedSpacer(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearCurvedSpacer(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearCurvedSpacer);

        mSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedSpacer_sweepAngleDegrees,
                        DEFAULT_SWEEP_ANGLE_DEGREES);
        mThicknessPx =
                (int) a.getDimension(R.styleable.WearCurvedSpacer_thickness, DEFAULT_THICKNESS_PX);

        a.recycle();
    }

    @Override
    public float getSweepAngleDegrees() {
        return mSweepAngleDegrees;
    }

    @Override
    public int getThickness() {
        return mThicknessPx;
    }

    /** Sets the sweep angle of this spacer, in degrees. */
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        this.mSweepAngleDegrees = sweepAngleDegrees;
    }

    /** Sets the thickness of this spacer, in DP. */
    public void setThickness(int thickness) {
        this.mThicknessPx = thickness;
    }

    @Override
    public void checkInvalidAttributeAsChild() {}

    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        return false;
    }
}
