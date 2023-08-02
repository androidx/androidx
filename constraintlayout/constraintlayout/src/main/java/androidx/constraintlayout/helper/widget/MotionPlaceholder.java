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
package androidx.constraintlayout.helper.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;

import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Helper;
import androidx.constraintlayout.core.widgets.Placeholder;
import androidx.constraintlayout.widget.VirtualLayout;

public class MotionPlaceholder extends VirtualLayout {
    private static final String TAG = "MotionPlaceholder";
    Placeholder mPlaceholder;

    public MotionPlaceholder(Context context) {
        super(context);
    }

    public MotionPlaceholder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MotionPlaceholder(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MotionPlaceholder(Context context,
                             AttributeSet attrs,
                             int defStyleAttr,
                             int defStyleRes) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        onMeasure(mPlaceholder, widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void onMeasure(androidx.constraintlayout.core.widgets.VirtualLayout layout,
                          int widthMeasureSpec,
                          int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (layout != null) {
            layout.measure(widthMode, widthSize, heightMode, heightSize);
            setMeasuredDimension(layout.getMeasuredWidth(), layout.getMeasuredHeight());
        } else {
            setMeasuredDimension(0, 0);
        }
    }

    @Override
    public void updatePreLayout(ConstraintWidgetContainer container,
                                Helper helper,
                                SparseArray<ConstraintWidget> map) {
        // override to block the ids being replaced
    }

    @Override
    protected void init(AttributeSet attrs) {
        super.init(attrs);
        mHelperWidget = new Placeholder();
        validateParams();
    }
}
