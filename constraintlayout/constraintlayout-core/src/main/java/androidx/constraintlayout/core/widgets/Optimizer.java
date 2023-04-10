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
package androidx.constraintlayout.core.widgets;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.DIMENSION_HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DIMENSION_VERTICAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;

import androidx.constraintlayout.core.LinearSystem;

/**
 * Implements direct resolution without using the solver
 */
public class Optimizer {

    // Optimization levels (mask)
    public static final int OPTIMIZATION_NONE = 0;
    public static final int OPTIMIZATION_DIRECT = 1;
    public static final int OPTIMIZATION_BARRIER = 1 << 1;
    public static final int OPTIMIZATION_CHAIN = 1 << 2;
    public static final int OPTIMIZATION_DIMENSIONS = 1 << 3;
    public static final int OPTIMIZATION_RATIO = 1 << 4;
    public static final int OPTIMIZATION_GROUPS = 1 << 5;
    public static final int OPTIMIZATION_GRAPH = 1 << 6;
    public static final int OPTIMIZATION_GRAPH_WRAP = 1 << 7;
    public static final int OPTIMIZATION_CACHE_MEASURES = 1 << 8;
    public static final int OPTIMIZATION_DEPENDENCY_ORDERING = 1 << 9;
    public static final int OPTIMIZATION_GROUPING = 1 << 10;
    public static final int OPTIMIZATION_STANDARD = OPTIMIZATION_DIRECT
            /* | OPTIMIZATION_GROUPING */
            /* | OPTIMIZATION_DEPENDENCY_ORDERING */
            | OPTIMIZATION_CACHE_MEASURES
            /* | OPTIMIZATION_GRAPH */
            /* | OPTIMIZATION_GRAPH_WRAP */
            /* | OPTIMIZATION_DIMENSIONS */;

    // Internal use.
    static boolean[] sFlags = new boolean[3];
    static final int FLAG_USE_OPTIMIZE = 0; // simple enough to use optimizer
    static final int FLAG_CHAIN_DANGLING = 1;
    static final int FLAG_RECOMPUTE_BOUNDS = 2;

    /**
     * Looks at optimizing match_parent
     */
    static void checkMatchParent(ConstraintWidgetContainer container,
            LinearSystem system,
            ConstraintWidget widget) {
        widget.mHorizontalResolution = UNKNOWN;
        widget.mVerticalResolution = UNKNOWN;
        if (container.mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
                && widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {

            int left = widget.mLeft.mMargin;
            int right = container.getWidth() - widget.mRight.mMargin;

            widget.mLeft.mSolverVariable = system.createObjectVariable(widget.mLeft);
            widget.mRight.mSolverVariable = system.createObjectVariable(widget.mRight);
            system.addEquality(widget.mLeft.mSolverVariable, left);
            system.addEquality(widget.mRight.mSolverVariable, right);
            widget.mHorizontalResolution = ConstraintWidget.DIRECT;
            widget.setHorizontalDimension(left, right);
        }
        if (container.mListDimensionBehaviors[DIMENSION_VERTICAL]
                != ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
                && widget.mListDimensionBehaviors[DIMENSION_VERTICAL]
                == ConstraintWidget.DimensionBehaviour.MATCH_PARENT) {

            int top = widget.mTop.mMargin;
            int bottom = container.getHeight() - widget.mBottom.mMargin;

            widget.mTop.mSolverVariable = system.createObjectVariable(widget.mTop);
            widget.mBottom.mSolverVariable = system.createObjectVariable(widget.mBottom);
            system.addEquality(widget.mTop.mSolverVariable, top);
            system.addEquality(widget.mBottom.mSolverVariable, bottom);
            if (widget.mBaselineDistance > 0 || widget.getVisibility() == ConstraintWidget.GONE) {
                widget.mBaseline.mSolverVariable = system.createObjectVariable(widget.mBaseline);
                system.addEquality(widget.mBaseline.mSolverVariable,
                        top + widget.mBaselineDistance);
            }
            widget.mVerticalResolution = ConstraintWidget.DIRECT;
            widget.setVerticalDimension(top, bottom);
        }
    }

    // @TODO: add description
    public static final boolean enabled(int optimizationLevel, int optimization) {
        return (optimizationLevel & optimization) == optimization;
    }
}
