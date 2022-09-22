/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.constraintlayout.core.widgets.analyzer;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.GONE;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_SPREAD;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.MATCH_CONSTRAINT_WRAP;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.Helper;
import androidx.constraintlayout.core.widgets.Optimizer;
import androidx.constraintlayout.core.widgets.VirtualLayout;

import java.util.ArrayList;

/**
 * Implements basic measure for linear resolution
 */
public class BasicMeasure {

    private static final boolean DEBUG = false;
    private static final boolean DO_NOT_USE = false;
    private static final int MODE_SHIFT = 30;
    public static final int UNSPECIFIED = 0;
    public static final int EXACTLY = 1 << MODE_SHIFT;
    public static final int AT_MOST = 2 << MODE_SHIFT;

    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;
    public static final int FIXED = -3;

    private final ArrayList<ConstraintWidget> mVariableDimensionsWidgets = new ArrayList<>();
    private Measure mMeasure = new Measure();

    // @TODO: add description
    public void updateHierarchy(ConstraintWidgetContainer layout) {
        mVariableDimensionsWidgets.clear();
        final int childCount = layout.mChildren.size();
        for (int i = 0; i < childCount; i++) {
            ConstraintWidget widget = layout.mChildren.get(i);
            if (widget.getHorizontalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    || widget.getVerticalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT) {
                mVariableDimensionsWidgets.add(widget);
            }
        }
        layout.invalidateGraph();
    }

    private ConstraintWidgetContainer mConstraintWidgetContainer;

    public BasicMeasure(ConstraintWidgetContainer constraintWidgetContainer) {
        this.mConstraintWidgetContainer = constraintWidgetContainer;
    }

    private void measureChildren(ConstraintWidgetContainer layout) {
        final int childCount = layout.mChildren.size();
        boolean optimize = layout.optimizeFor(Optimizer.OPTIMIZATION_GRAPH);
        Measurer measurer = layout.getMeasurer();
        for (int i = 0; i < childCount; i++) {
            ConstraintWidget child = layout.mChildren.get(i);
            if (child instanceof Guideline) {
                continue;
            }
            if (child instanceof Barrier) {
                continue;
            }
            if (child.isInVirtualLayout()) {
                continue;
            }

            if (optimize && child.mHorizontalRun != null && child.mVerticalRun != null
                    && child.mHorizontalRun.mDimension.resolved
                    && child.mVerticalRun.mDimension.resolved) {
                continue;
            }

            ConstraintWidget.DimensionBehaviour widthBehavior =
                    child.getDimensionBehaviour(HORIZONTAL);
            ConstraintWidget.DimensionBehaviour heightBehavior =
                    child.getDimensionBehaviour(VERTICAL);

            boolean skip = widthBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    && child.mMatchConstraintDefaultWidth != MATCH_CONSTRAINT_WRAP
                    && heightBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    && child.mMatchConstraintDefaultHeight != MATCH_CONSTRAINT_WRAP;

            if (!skip && layout.optimizeFor(Optimizer.OPTIMIZATION_DIRECT)
                    && !(child instanceof VirtualLayout)) {
                if (widthBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && child.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                        && heightBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && !child.isInHorizontalChain()) {
                    skip = true;
                }

                if (heightBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && child.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD
                        && widthBehavior != ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        && !child.isInHorizontalChain()) {
                    skip = true;
                }

                // Don't measure yet -- let the direct solver have a shot at it.
                if ((widthBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                        || heightBehavior == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT)
                        && child.mDimensionRatio > 0) {
                    skip = true;
                }
            }

            if (skip) {
                // we don't need to measure here as the dimension of the widget
                // will be completely computed by the solver.
                continue;
            }

            measure(measurer, child, Measure.SELF_DIMENSIONS);
            if (layout.mMetrics != null) {
                layout.mMetrics.measuredWidgets++;
            }
        }
        measurer.didMeasures();
    }

    private void solveLinearSystem(ConstraintWidgetContainer layout,
            String reason,
            int pass,
            int w,
            int h) {
        long startLayout = 0;
        if (layout.mMetrics != null) {
            startLayout = System.nanoTime();
        }

        int minWidth = layout.getMinWidth();
        int minHeight = layout.getMinHeight();
        layout.setMinWidth(0);
        layout.setMinHeight(0);
        layout.setWidth(w);
        layout.setHeight(h);
        layout.setMinWidth(minWidth);
        layout.setMinHeight(minHeight);
        if (DEBUG) {
            System.out.println("### Solve <" + reason + "> ###");
        }
        mConstraintWidgetContainer.setPass(pass);
        mConstraintWidgetContainer.layout();
        if (layout.mMetrics != null) {
            long endLayout = System.nanoTime();
            layout.mMetrics.mSolverPasses++;
            layout.mMetrics.measuresLayoutDuration += (endLayout - startLayout);
        }
    }

    /**
     * Called by ConstraintLayout onMeasure()
     */
    public long solverMeasure(ConstraintWidgetContainer layout,
            int optimizationLevel,
            int paddingX, int paddingY,
            int widthMode, int widthSize,
            int heightMode, int heightSize,
            int lastMeasureWidth,
            int lastMeasureHeight) {
        Measurer measurer = layout.getMeasurer();
        long layoutTime = 0;

        final int childCount = layout.mChildren.size();
        int startingWidth = layout.getWidth();
        int startingHeight = layout.getHeight();

        boolean optimizeWrap =
                Optimizer.enabled(optimizationLevel, Optimizer.OPTIMIZATION_GRAPH_WRAP);
        boolean optimize = optimizeWrap
                || Optimizer.enabled(optimizationLevel, Optimizer.OPTIMIZATION_GRAPH);

        if (optimize) {
            for (int i = 0; i < childCount; i++) {
                ConstraintWidget child = layout.mChildren.get(i);
                boolean matchWidth = child.getHorizontalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                boolean matchHeight = child.getVerticalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
                boolean ratio = matchWidth && matchHeight && child.getDimensionRatio() > 0;
                if (child.isInHorizontalChain() && ratio) {
                    optimize = false;
                    break;
                }
                if (child.isInVerticalChain() && ratio) {
                    optimize = false;
                    break;
                }
                if (child instanceof VirtualLayout) {
                    optimize = false;
                    break;
                }
                if (child.isInHorizontalChain()
                        || child.isInVerticalChain()) {
                    optimize = false;
                    break;
                }
            }
        }

        if (optimize && LinearSystem.sMetrics != null) {
            LinearSystem.sMetrics.measures++;
        }

        boolean allSolved = false;

        optimize &= (widthMode == EXACTLY && heightMode == EXACTLY) || optimizeWrap;

        int computations = 0;

        if (optimize) {
            // For non-optimizer this doesn't seem to be a problem.
            // For both cases, having the width address max size early seems to work
            //  (which makes sense).
            // Putting it specific to optimizer to reduce unnecessary risk.
            widthSize = Math.min(layout.getMaxWidth(), widthSize);
            heightSize = Math.min(layout.getMaxHeight(), heightSize);

            if (widthMode == EXACTLY && layout.getWidth() != widthSize) {
                layout.setWidth(widthSize);
                layout.invalidateGraph();
            }
            if (heightMode == EXACTLY && layout.getHeight() != heightSize) {
                layout.setHeight(heightSize);
                layout.invalidateGraph();
            }
            if (widthMode == EXACTLY && heightMode == EXACTLY) {
                allSolved = layout.directMeasure(optimizeWrap);
                computations = 2;
            } else {
                allSolved = layout.directMeasureSetup(optimizeWrap);
                if (widthMode == EXACTLY) {
                    allSolved &= layout.directMeasureWithOrientation(optimizeWrap, HORIZONTAL);
                    computations++;
                }
                if (heightMode == EXACTLY) {
                    allSolved &= layout.directMeasureWithOrientation(optimizeWrap, VERTICAL);
                    computations++;
                }
            }
            if (allSolved) {
                layout.updateFromRuns(widthMode == EXACTLY, heightMode == EXACTLY);
            }
        } else {
            if (false) {
                layout.mHorizontalRun.clear();
                layout.mVerticalRun.clear();
                for (ConstraintWidget child : layout.getChildren()) {
                    child.mHorizontalRun.clear();
                    child.mVerticalRun.clear();
                }
            }
        }

        if (!allSolved || computations != 2) {
            int optimizations = layout.getOptimizationLevel();
            if (childCount > 0) {
                measureChildren(layout);
            }
            if (layout.mMetrics != null) {
                layoutTime = System.nanoTime();
            }

            updateHierarchy(layout);

            // let's update the size dependent widgets if any...
            final int sizeDependentWidgetsCount = mVariableDimensionsWidgets.size();

            // let's solve the linear system.
            if (childCount > 0) {
                solveLinearSystem(layout, "First pass", 0, startingWidth, startingHeight);
            }

            if (DEBUG) {
                System.out.println("size dependent widgets: " + sizeDependentWidgetsCount);
            }

            if (sizeDependentWidgetsCount > 0) {
                boolean needSolverPass = false;
                boolean containerWrapWidth = layout.getHorizontalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                boolean containerWrapHeight = layout.getVerticalDimensionBehaviour()
                        == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;
                int minWidth = Math.max(layout.getWidth(),
                        mConstraintWidgetContainer.getMinWidth());
                int minHeight = Math.max(layout.getHeight(),
                        mConstraintWidgetContainer.getMinHeight());

                ////////////////////////////////////////////////////////////////////////////////////
                // Let's first apply sizes for VirtualLayouts if any
                ////////////////////////////////////////////////////////////////////////////////////
                for (int i = 0; i < sizeDependentWidgetsCount; i++) {
                    ConstraintWidget widget = mVariableDimensionsWidgets.get(i);
                    if (!(widget instanceof VirtualLayout)) {
                        continue;
                    }
                    int preWidth = widget.getWidth();
                    int preHeight = widget.getHeight();
                    needSolverPass |= measure(measurer, widget, Measure.TRY_GIVEN_DIMENSIONS);
                    if (layout.mMetrics != null) {
                        layout.mMetrics.measuredMatchWidgets++;
                    }
                    int measuredWidth = widget.getWidth();
                    int measuredHeight = widget.getHeight();
                    if (measuredWidth != preWidth) {
                        widget.setWidth(measuredWidth);
                        if (containerWrapWidth && widget.getRight() > minWidth) {
                            int w = widget.getRight()
                                    + widget.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                            minWidth = Math.max(minWidth, w);
                        }
                        needSolverPass = true;
                    }
                    if (measuredHeight != preHeight) {
                        widget.setHeight(measuredHeight);
                        if (containerWrapHeight && widget.getBottom() > minHeight) {
                            int h = widget.getBottom()
                                    + widget.getAnchor(ConstraintAnchor.Type.BOTTOM).getMargin();
                            minHeight = Math.max(minHeight, h);
                        }
                        needSolverPass = true;
                    }
                    VirtualLayout virtualLayout = (VirtualLayout) widget;
                    needSolverPass |= virtualLayout.needSolverPass();
                }
                ////////////////////////////////////////////////////////////////////////////////////

                int maxIterations = 2;
                for (int j = 0; j < maxIterations; j++) {
                    for (int i = 0; i < sizeDependentWidgetsCount; i++) {
                        ConstraintWidget widget = mVariableDimensionsWidgets.get(i);
                        if ((widget instanceof Helper && !(widget instanceof VirtualLayout))
                                || widget instanceof Guideline) {
                            continue;
                        }
                        if (widget.getVisibility() == GONE) {
                            continue;
                        }
                        if (optimize && widget.mHorizontalRun.mDimension.resolved
                                && widget.mVerticalRun.mDimension.resolved) {
                            continue;
                        }
                        if (widget instanceof VirtualLayout) {
                            continue;
                        }

                        int preWidth = widget.getWidth();
                        int preHeight = widget.getHeight();
                        int preBaselineDistance = widget.getBaselineDistance();

                        int measureStrategy = Measure.TRY_GIVEN_DIMENSIONS;
                        if (j == maxIterations - 1) {
                            measureStrategy = Measure.USE_GIVEN_DIMENSIONS;
                        }
                        boolean hasMeasure = measure(measurer, widget, measureStrategy);
                        if (DO_NOT_USE && !widget.hasDependencies()) {
                            hasMeasure = false;
                        }
                        needSolverPass |= hasMeasure;
                        if (DEBUG && hasMeasure) {
                            System.out.println("{#} Needs Solver pass as measure true for "
                                    + widget.getDebugName());
                        }
                        if (layout.mMetrics != null) {
                            layout.mMetrics.measuredMatchWidgets++;
                        }

                        int measuredWidth = widget.getWidth();
                        int measuredHeight = widget.getHeight();

                        if (measuredWidth != preWidth) {
                            widget.setWidth(measuredWidth);
                            if (containerWrapWidth && widget.getRight() > minWidth) {
                                int w = widget.getRight()
                                        + widget.getAnchor(ConstraintAnchor.Type.RIGHT).getMargin();
                                minWidth = Math.max(minWidth, w);
                            }
                            if (DEBUG) {
                                System.out.println("{#} Needs Solver pass as Width for "
                                        + widget.getDebugName() + " changed: "
                                        + measuredWidth + " != " + preWidth);
                            }
                            needSolverPass = true;
                        }
                        if (measuredHeight != preHeight) {
                            widget.setHeight(measuredHeight);
                            if (containerWrapHeight && widget.getBottom() > minHeight) {
                                int h = widget.getBottom()
                                        + widget.getAnchor(ConstraintAnchor.Type.BOTTOM)
                                        .getMargin();
                                minHeight = Math.max(minHeight, h);
                            }
                            if (DEBUG) {
                                System.out.println("{#} Needs Solver pass as Height for "
                                        + widget.getDebugName() + " changed: "
                                        + measuredHeight + " != " + preHeight);
                            }
                            needSolverPass = true;
                        }
                        if (widget.hasBaseline()
                                && preBaselineDistance != widget.getBaselineDistance()) {
                            if (DEBUG) {
                                System.out.println("{#} Needs Solver pass as Baseline for "
                                        + widget.getDebugName() + " changed: "
                                        + widget.getBaselineDistance() + " != "
                                        + preBaselineDistance);
                            }
                            needSolverPass = true;
                        }
                    }
                    if (needSolverPass) {
                        solveLinearSystem(layout, "intermediate pass",
                                1 + j, startingWidth, startingHeight);
                        needSolverPass = false;
                    } else {
                        break;
                    }
                }
            }
            layout.setOptimizationLevel(optimizations);
        }
        if (layout.mMetrics != null) {
            layoutTime = (System.nanoTime() - layoutTime);
        }
        return layoutTime;
    }

    /**
     * Convenience function to fill in the measure spec
     *
     * @param measurer        the measurer callback
     * @param widget          the widget to measure
     * @param measureStrategy how to use the current ConstraintWidget dimensions during the measure
     * @return true if needs another solver pass
     */
    private boolean measure(Measurer measurer, ConstraintWidget widget, int measureStrategy) {
        mMeasure.horizontalBehavior = widget.getHorizontalDimensionBehaviour();
        mMeasure.verticalBehavior = widget.getVerticalDimensionBehaviour();
        mMeasure.horizontalDimension = widget.getWidth();
        mMeasure.verticalDimension = widget.getHeight();
        mMeasure.measuredNeedsSolverPass = false;
        mMeasure.measureStrategy = measureStrategy;

        boolean horizontalMatchConstraints = (mMeasure.horizontalBehavior
                == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        boolean verticalMatchConstraints = (mMeasure.verticalBehavior
                == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
        boolean horizontalUseRatio = horizontalMatchConstraints && widget.mDimensionRatio > 0;
        boolean verticalUseRatio = verticalMatchConstraints && widget.mDimensionRatio > 0;

        if (horizontalUseRatio) {
            if (widget.mResolvedMatchConstraintDefault[HORIZONTAL]
                    == ConstraintWidget.MATCH_CONSTRAINT_RATIO_RESOLVED) {
                mMeasure.horizontalBehavior = ConstraintWidget.DimensionBehaviour.FIXED;
            }
        }
        if (verticalUseRatio) {
            if (widget.mResolvedMatchConstraintDefault[VERTICAL]
                    == ConstraintWidget.MATCH_CONSTRAINT_RATIO_RESOLVED) {
                mMeasure.verticalBehavior = ConstraintWidget.DimensionBehaviour.FIXED;
            }
        }

        measurer.measure(widget, mMeasure);
        widget.setWidth(mMeasure.measuredWidth);
        widget.setHeight(mMeasure.measuredHeight);
        widget.setHasBaseline(mMeasure.measuredHasBaseline);
        widget.setBaselineDistance(mMeasure.measuredBaseline);
        mMeasure.measureStrategy = Measure.SELF_DIMENSIONS;
        return mMeasure.measuredNeedsSolverPass;
    }

    public interface Measurer {
        // @TODO: add description
        void measure(ConstraintWidget widget, Measure measure);

        // @TODO: add description
        void didMeasures();
    }

    public static class Measure {
        public static int SELF_DIMENSIONS = 0;
        public static int TRY_GIVEN_DIMENSIONS = 1;
        public static int USE_GIVEN_DIMENSIONS = 2;
        public ConstraintWidget.DimensionBehaviour horizontalBehavior;
        public ConstraintWidget.DimensionBehaviour verticalBehavior;
        public int horizontalDimension;
        public int verticalDimension;
        public int measuredWidth;
        public int measuredHeight;
        public int measuredBaseline;
        public boolean measuredHasBaseline;
        public boolean measuredNeedsSolverPass;
        public int measureStrategy;
    }
}
