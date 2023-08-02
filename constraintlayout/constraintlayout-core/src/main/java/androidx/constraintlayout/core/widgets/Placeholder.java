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

package androidx.constraintlayout.core.widgets;

import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.AT_MOST;
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.EXACTLY;
import static androidx.constraintlayout.core.widgets.analyzer.BasicMeasure.UNSPECIFIED;

import androidx.constraintlayout.core.LinearSystem;

/**
 * Simple VirtualLayout that center the first referenced widget onto itself.
 */
public class Placeholder extends VirtualLayout {

    // @TODO: add description
    @Override
    public void measure(int widthMode, int widthSize, int heightMode, int heightSize) {
        int width = 0;
        int height = 0;
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        width += paddingLeft + paddingRight;
        height += paddingTop + paddingBottom;

        if (mWidgetsCount > 0) {
            // grab the first referenced widget size in case we are ourselves in wrap_content
            width += mWidgets[0].getWidth();
            height += mWidgets[0].getHeight();
        }
        width = Math.max(getMinWidth(), width);
        height = Math.max(getMinHeight(), height);

        int measuredWidth = 0;
        int measuredHeight = 0;

        if (widthMode == EXACTLY) {
            measuredWidth = widthSize;
        } else if (widthMode == AT_MOST) {
            measuredWidth = Math.min(width, widthSize);
        } else if (widthMode == UNSPECIFIED) {
            measuredWidth = width;
        }

        if (heightMode == EXACTLY) {
            measuredHeight = heightSize;
        } else if (heightMode == AT_MOST) {
            measuredHeight = Math.min(height, heightSize);
        } else if (heightMode == UNSPECIFIED) {
            measuredHeight = height;
        }

        setMeasure(measuredWidth, measuredHeight);
        setWidth(measuredWidth);
        setHeight(measuredHeight);
        needsCallbackFromSolver(mWidgetsCount > 0);
    }

    @Override
    public void addToSolver(LinearSystem system, boolean optimize) {
        super.addToSolver(system, optimize);

        if (mWidgetsCount > 0) {
            ConstraintWidget widget = mWidgets[0];
            widget.resetAllConstraints();
            widget.connect(ConstraintAnchor.Type.LEFT, this, ConstraintAnchor.Type.LEFT);
            widget.connect(ConstraintAnchor.Type.RIGHT, this, ConstraintAnchor.Type.RIGHT);
            widget.connect(ConstraintAnchor.Type.TOP, this, ConstraintAnchor.Type.TOP);
            widget.connect(ConstraintAnchor.Type.BOTTOM, this, ConstraintAnchor.Type.BOTTOM);
        }
    }
}
