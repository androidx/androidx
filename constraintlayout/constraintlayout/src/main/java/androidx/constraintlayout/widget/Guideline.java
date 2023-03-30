/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.constraintlayout.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * Utility class representing a Guideline helper object for
 * {@link ConstraintLayout}.
 * Helper objects are not displayed on device
 * (they are marked as {@code View.GONE}) and are only used
 * for layout purposes. They only work within a
 * {@link ConstraintLayout}.
 *<p>
 * A Guideline can be either horizontal or vertical:
 * <ul>
 *     <li>Vertical Guidelines have a width of zero and the height of their
 *     {@link ConstraintLayout} parent</li>
 *     <li>Horizontal Guidelines have a height of zero and the width of their
 *     {@link ConstraintLayout} parent</li>
 * </ul>
 *<p>
 * Positioning a Guideline is possible in three different ways:
 * <ul>
 *     <li>specifying a fixed distance from the left or the top of a layout
 *     ({@code layout_constraintGuide_begin})</li>
 *     <li>specifying a fixed distance from the right or the bottom of a layout
 *     ({@code layout_constraintGuide_end})</li>
 *     <li>specifying a percentage of the width or the height of a layout
 *     ({@code layout_constraintGuide_percent})</li>
 * </ul>
 * <p>
 * Widgets can then be constrained to a Guideline,
 * allowing multiple widgets to be positioned easily from
 * one Guideline, or allowing reactive layout behavior by using percent positioning.
 * <p>
 * See the list of attributes in
 * {@link androidx.constraintlayout.widget.ConstraintLayout.LayoutParams} to set a Guideline
 * in XML, as well as the corresponding {@link ConstraintSet#setGuidelineBegin},
 * {@link ConstraintSet#setGuidelineEnd}
 * and {@link ConstraintSet#setGuidelinePercent} functions in {@link ConstraintSet}.
 * <p>
 *   Example of a {@code Button} constrained to a vertical {@code Guideline}:
 *   <pre>
 *     <androidx.constraintlayout.widget.ConstraintLayout
 *         xmlns:android="http://schemas.android.com/apk/res/android"
 *         xmlns:app="http://schemas.android.com/apk/res-auto"
 *         xmlns:tools="http://schemas.android.com/tools"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent">
 *
 *         <androidx.constraintlayout.widget.Guideline
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:id="@+id/guideline"
 *             app:layout_constraintGuide_begin="100dp"
 *             android:orientation="vertical"/>
 *         <Button
 *             android:text="Button"
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:id="@+id/button"
 *             app:layout_constraintLeft_toLeftOf="@+id/guideline"
 *             android:layout_marginTop="16dp"
 *             app:layout_constraintTop_toTopOf="parent" />
 *     </androidx.constraintlayout.widget.ConstraintLayout>
 *  </pre>
 * <p/>
 */
public class Guideline extends View {
    private boolean mFilterRedundantCalls = true;
    public Guideline(Context context) {
        super(context);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
    }

    public Guideline(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        super.setVisibility(View.GONE);
    }

    /**
     *
     */
    @Override
    public void setVisibility(int visibility) {
    }

    /**
     * We are overriding draw and not calling super.draw() here because
     * Helper objects are not displayed on device.
     *
     *
     */
    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(@NonNull Canvas canvas) {

    }

    /**
     *
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    /**
     * Set the guideline's distance from the top or left edge.
     *
     * @param margin the distance to the top or left edge
     */
    public void setGuidelineBegin(int margin) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        if (mFilterRedundantCalls && params.guideBegin == margin) {
            return;
        }
        params.guideBegin = margin;
        setLayoutParams(params);
    }

    /**
     * Set a guideline's distance to end.
     *
     * @param margin the margin to the right or bottom side of container
     */
    public void setGuidelineEnd(int margin) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        if (mFilterRedundantCalls && params.guideEnd == margin) {
            return;
        }
        params.guideEnd = margin;
        setLayoutParams(params);
    }

    /**
     * Set a Guideline's percent.
     * @param ratio the ratio between the gap on the left and right 0.0 is top/left 0.5 is middle
     */
    public void setGuidelinePercent(float ratio) {
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) getLayoutParams();
        if (mFilterRedundantCalls && params.guidePercent == ratio) {
            return;
        }
        params.guidePercent = ratio;
        setLayoutParams(params);
    }

    /**
     * filter redundant calls to setGuidelineBegin, setGuidelineEnd & setGuidelinePercent.
     *
     * By default calling setGuidelineStart,setGuideLineEnd and setGuidelinePercent will do nothing
     * if the value is the same as the current value. This can disable that behaviour and call
     * setLayoutParams(..) while will call requestLayout
     *
     * @param filter default true set false to always generate a setLayoutParams
     */
    public void setFilterRedundantCalls(boolean filter) {
        this.mFilterRedundantCalls = filter;
    }
}
