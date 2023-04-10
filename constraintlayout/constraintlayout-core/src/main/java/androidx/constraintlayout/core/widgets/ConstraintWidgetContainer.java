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

package androidx.constraintlayout.core.widgets;

import static androidx.constraintlayout.core.LinearSystem.FULL_DEBUG;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.FIXED;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.DimensionBehaviour.WRAP_CONTENT;

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.Metrics;
import androidx.constraintlayout.core.SolverVariable;
import androidx.constraintlayout.core.widgets.analyzer.BasicMeasure;
import androidx.constraintlayout.core.widgets.analyzer.DependencyGraph;
import androidx.constraintlayout.core.widgets.analyzer.Direct;
import androidx.constraintlayout.core.widgets.analyzer.Grouping;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * A container of ConstraintWidget that can layout its children
 */
public class ConstraintWidgetContainer extends WidgetContainer {

    private static final int MAX_ITERATIONS = 8;

    private static final boolean DEBUG = FULL_DEBUG;
    private static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_GRAPH = false;

    BasicMeasure mBasicMeasureSolver = new BasicMeasure(this);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Graph measures
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public DependencyGraph mDependencyGraph = new DependencyGraph(this);
    private int mPass; // number of layout passes

    /**
     * Invalidate the graph of constraints
     */
    public void invalidateGraph() {
        mDependencyGraph.invalidateGraph();
    }

    /**
     * Invalidate the widgets measures
     */
    public void invalidateMeasures() {
        mDependencyGraph.invalidateMeasures();
    }


    // @TODO: add description
    public boolean directMeasure(boolean optimizeWrap) {
        return mDependencyGraph.directMeasure(optimizeWrap);
//        int paddingLeft = getX();
//        int paddingTop = getY();
//        if (mDependencyGraph.directMeasureSetup(optimizeWrap)) {
//            mDependencyGraph.measureWidgets();
//            boolean allResolved =
//                      mDependencyGraph.directMeasureWithOrientation(optimizeWrap, HORIZONTAL);
//            allResolved &= mDependencyGraph.directMeasureWithOrientation(optimizeWrap, VERTICAL);
//            for (ConstraintWidget child : mChildren) {
//                child.setDrawX(child.getDrawX() + paddingLeft);
//                child.setDrawY(child.getDrawY() + paddingTop);
//            }
//            setX(paddingLeft);
//            setY(paddingTop);
//            return allResolved;
//        }
//        return false;
    }

    // @TODO: add description
    public boolean directMeasureSetup(boolean optimizeWrap) {
        return mDependencyGraph.directMeasureSetup(optimizeWrap);
    }

    // @TODO: add description
    public boolean directMeasureWithOrientation(boolean optimizeWrap, int orientation) {
        return mDependencyGraph.directMeasureWithOrientation(optimizeWrap, orientation);
    }

    // @TODO: add description
    public void defineTerminalWidgets() {
        mDependencyGraph.defineTerminalWidgets(getHorizontalDimensionBehaviour(),
                getVerticalDimensionBehaviour());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Measure the layout
     */
    public long measure(int optimizationLevel, int widthMode, int widthSize,
            int heightMode, int heightSize, int lastMeasureWidth,
            int lastMeasureHeight, int paddingX, int paddingY) {
        mPaddingLeft = paddingX;
        mPaddingTop = paddingY;
        return mBasicMeasureSolver.solverMeasure(this, optimizationLevel, paddingX, paddingY,
                widthMode, widthSize, heightMode, heightSize,
                lastMeasureWidth, lastMeasureHeight);
    }

    // @TODO: add description
    public void updateHierarchy() {
        mBasicMeasureSolver.updateHierarchy(this);
    }

    protected BasicMeasure.Measurer mMeasurer = null;

    // @TODO: add description
    public void setMeasurer(BasicMeasure.Measurer measurer) {
        mMeasurer = measurer;
        mDependencyGraph.setMeasurer(measurer);
    }

    public BasicMeasure.Measurer getMeasurer() {
        return mMeasurer;
    }

    private boolean mIsRtl = false;
    public Metrics mMetrics;

    // @TODO: add description
    public void fillMetrics(Metrics metrics) {
        mMetrics = metrics;
        mSystem.fillMetrics(metrics);
    }

    protected LinearSystem mSystem = new LinearSystem();

    int mPaddingLeft;
    int mPaddingTop;
    int mPaddingRight;
    int mPaddingBottom;

    public int mHorizontalChainsSize = 0;
    public int mVerticalChainsSize = 0;

    ChainHead[] mVerticalChainsArray = new ChainHead[4];
    ChainHead[] mHorizontalChainsArray = new ChainHead[4];

    public boolean mGroupsWrapOptimized = false;
    public boolean mHorizontalWrapOptimized = false;
    public boolean mVerticalWrapOptimized = false;
    public int mWrapFixedWidth = 0;
    public int mWrapFixedHeight = 0;

    private int mOptimizationLevel = Optimizer.OPTIMIZATION_STANDARD;
    public boolean mSkipSolver = false;

    private boolean mWidthMeasuredTooSmall = false;
    private boolean mHeightMeasuredTooSmall = false;

    /*-----------------------------------------------------------------------*/
    // Construction
    /*-----------------------------------------------------------------------*/

    /**
     * Default constructor
     */
    public ConstraintWidgetContainer() {
    }

    /**
     * Constructor
     *
     * @param x      x position
     * @param y      y position
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidgetContainer(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    /**
     * Constructor
     *
     * @param width  width of the layout
     * @param height height of the layout
     */
    public ConstraintWidgetContainer(int width, int height) {
        super(width, height);
    }

    public ConstraintWidgetContainer(String debugName, int width, int height) {
        super(width, height);
        setDebugName(debugName);
    }

    /**
     * Resolves the system directly when possible
     *
     * @param value optimization level
     */
    public void setOptimizationLevel(int value) {
        mOptimizationLevel = value;
        mSystem.USE_DEPENDENCY_ORDERING = optimizeFor(Optimizer.OPTIMIZATION_DEPENDENCY_ORDERING);
    }

    /**
     * Returns the current optimization level
     */
    public int getOptimizationLevel() {
        return mOptimizationLevel;
    }

    /**
     * Returns true if the given feature should be optimized
     */
    public boolean optimizeFor(int feature) {
        return (mOptimizationLevel & feature) == feature;
    }

    /**
     * Specify the xml type for the container
     */
    @Override
    public String getType() {
        return "ConstraintLayout";
    }

    @Override
    public void reset() {
        mSystem.reset();
        mPaddingLeft = 0;
        mPaddingRight = 0;
        mPaddingTop = 0;
        mPaddingBottom = 0;
        mSkipSolver = false;
        super.reset();
    }

    /**
     * Return true if the width given is too small for the content laid out
     */
    public boolean isWidthMeasuredTooSmall() {
        return mWidthMeasuredTooSmall;
    }

    /**
     * Return true if the height given is too small for the content laid out
     */
    public boolean isHeightMeasuredTooSmall() {
        return mHeightMeasuredTooSmall;
    }

    int mDebugSolverPassCount = 0;

    private WeakReference<ConstraintAnchor> mVerticalWrapMin = null;
    private WeakReference<ConstraintAnchor> mHorizontalWrapMin = null;
    private WeakReference<ConstraintAnchor> mVerticalWrapMax = null;
    private WeakReference<ConstraintAnchor> mHorizontalWrapMax = null;

    void addVerticalWrapMinVariable(ConstraintAnchor top) {
        if (mVerticalWrapMin == null || mVerticalWrapMin.get() == null
                || top.getFinalValue() > mVerticalWrapMin.get().getFinalValue()) {
            mVerticalWrapMin = new WeakReference<>(top);
        }
    }

    // @TODO: add description
    public void addHorizontalWrapMinVariable(ConstraintAnchor left) {
        if (mHorizontalWrapMin == null || mHorizontalWrapMin.get() == null
                || left.getFinalValue() > mHorizontalWrapMin.get().getFinalValue()) {
            mHorizontalWrapMin = new WeakReference<>(left);
        }
    }

    void addVerticalWrapMaxVariable(ConstraintAnchor bottom) {
        if (mVerticalWrapMax == null || mVerticalWrapMax.get() == null
                || bottom.getFinalValue() > mVerticalWrapMax.get().getFinalValue()) {
            mVerticalWrapMax = new WeakReference<>(bottom);
        }
    }

    // @TODO: add description
    public void addHorizontalWrapMaxVariable(ConstraintAnchor right) {
        if (mHorizontalWrapMax == null || mHorizontalWrapMax.get() == null
                || right.getFinalValue() > mHorizontalWrapMax.get().getFinalValue()) {
            mHorizontalWrapMax = new WeakReference<>(right);
        }
    }

    private void addMinWrap(ConstraintAnchor constraintAnchor, SolverVariable parentMin) {
        SolverVariable variable = mSystem.createObjectVariable(constraintAnchor);
        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;
        mSystem.addGreaterThan(variable, parentMin, 0, wrapStrength);
    }

    private void addMaxWrap(ConstraintAnchor constraintAnchor, SolverVariable parentMax) {
        SolverVariable variable = mSystem.createObjectVariable(constraintAnchor);
        int wrapStrength = SolverVariable.STRENGTH_EQUALITY;
        mSystem.addGreaterThan(parentMax, variable, 0, wrapStrength);
    }

    HashSet<ConstraintWidget> mWidgetsToAdd = new HashSet<>();

    /**
     * Add this widget to the solver
     *
     * @param system the solver we want to add the widget to
     */
    public boolean addChildrenToSolver(LinearSystem system) {
        if (DEBUG) {
            System.out.println("\n#######################################");
            System.out.println("##    ADD CHILDREN TO SOLVER  (" + mDebugSolverPassCount + ") ##");
            System.out.println("#######################################\n");
            mDebugSolverPassCount++;
        }

        boolean optimize = optimizeFor(Optimizer.OPTIMIZATION_GRAPH);
        addToSolver(system, optimize);
        final int count = mChildren.size();

        boolean hasBarriers = false;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.setInBarrier(HORIZONTAL, false);
            widget.setInBarrier(VERTICAL, false);
            if (widget instanceof Barrier) {
                hasBarriers = true;
            }
        }

        if (hasBarriers) {
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = mChildren.get(i);
                if (widget instanceof Barrier) {
                    ((Barrier) widget).markWidgets();
                }
            }
        }

        mWidgetsToAdd.clear();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget.addFirst()) {
                if (widget instanceof VirtualLayout) {
                    mWidgetsToAdd.add(widget);
                } else {
                    widget.addToSolver(system, optimize);
                }
            }
        }

        // If we have virtual layouts, we need to add them to the solver in the correct
        // order (in case they reference one another)
        while (mWidgetsToAdd.size() > 0) {
            int numLayouts = mWidgetsToAdd.size();
            VirtualLayout layout = null;
            for (ConstraintWidget widget : mWidgetsToAdd) {
                layout = (VirtualLayout) widget;

                // we'll go through the virtual layouts that references others first, to give
                // them a shot at setting their constraints.
                if (layout.contains(mWidgetsToAdd)) {
                    layout.addToSolver(system, optimize);
                    mWidgetsToAdd.remove(layout);
                    break;
                }
            }
            if (numLayouts == mWidgetsToAdd.size()) {
                // looks we didn't find anymore dependency, let's add everything.
                for (ConstraintWidget widget : mWidgetsToAdd) {
                    widget.addToSolver(system, optimize);
                }
                mWidgetsToAdd.clear();
            }
        }

        if (LinearSystem.USE_DEPENDENCY_ORDERING) {
            HashSet<ConstraintWidget> widgetsToAdd = new HashSet<>();
            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = mChildren.get(i);
                if (!widget.addFirst()) {
                    widgetsToAdd.add(widget);
                }
            }
            int orientation = VERTICAL;
            if (getHorizontalDimensionBehaviour() == WRAP_CONTENT) {
                orientation = HORIZONTAL;
            }
            addChildrenToSolverByDependency(this, system, widgetsToAdd, orientation, false);
            for (ConstraintWidget widget : widgetsToAdd) {
                Optimizer.checkMatchParent(this, system, widget);
                widget.addToSolver(system, optimize);
            }
        } else {

            for (int i = 0; i < count; i++) {
                ConstraintWidget widget = mChildren.get(i);
                if (widget instanceof ConstraintWidgetContainer) {
                    DimensionBehaviour horizontalBehaviour =
                            widget.mListDimensionBehaviors[DIMENSION_HORIZONTAL];
                    DimensionBehaviour verticalBehaviour =
                            widget.mListDimensionBehaviors[DIMENSION_VERTICAL];
                    if (horizontalBehaviour == WRAP_CONTENT) {
                        widget.setHorizontalDimensionBehaviour(FIXED);
                    }
                    if (verticalBehaviour == WRAP_CONTENT) {
                        widget.setVerticalDimensionBehaviour(FIXED);
                    }
                    widget.addToSolver(system, optimize);
                    if (horizontalBehaviour == WRAP_CONTENT) {
                        widget.setHorizontalDimensionBehaviour(horizontalBehaviour);
                    }
                    if (verticalBehaviour == WRAP_CONTENT) {
                        widget.setVerticalDimensionBehaviour(verticalBehaviour);
                    }
                } else {
                    Optimizer.checkMatchParent(this, system, widget);
                    if (!widget.addFirst()) {
                        widget.addToSolver(system, optimize);
                    }
                }
            }
        }

        if (mHorizontalChainsSize > 0) {
            Chain.applyChainConstraints(this, system, null, HORIZONTAL);
        }
        if (mVerticalChainsSize > 0) {
            Chain.applyChainConstraints(this, system, null, VERTICAL);
        }
        return true;
    }

    /**
     * Update the frame of the layout and its children from the solver
     *
     * @param system the solver we get the values from.
     */
    public boolean updateChildrenFromSolver(LinearSystem system, boolean[] flags) {
        flags[Optimizer.FLAG_RECOMPUTE_BOUNDS] = false;
        boolean optimize = optimizeFor(Optimizer.OPTIMIZATION_GRAPH);
        updateFromSolver(system, optimize);
        final int count = mChildren.size();
        boolean hasOverride = false;
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromSolver(system, optimize);
            if (widget.hasDimensionOverride()) {
                hasOverride = true;
            }
        }
        return hasOverride;
    }

    @Override
    public void updateFromRuns(boolean updateHorizontal, boolean updateVertical) {
        super.updateFromRuns(updateHorizontal, updateVertical);
        final int count = mChildren.size();
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            widget.updateFromRuns(updateHorizontal, updateVertical);
        }
    }

    /**
     * Set the padding on this container. It will apply to the position of the children.
     *
     * @param left   left padding
     * @param top    top padding
     * @param right  right padding
     * @param bottom bottom padding
     */
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    /**
     * Set the rtl status. This has implications for Chains.
     *
     * @param isRtl true if we are in RTL.
     */
    public void setRtl(boolean isRtl) {
        mIsRtl = isRtl;
    }

    /**
     * Returns the rtl status.
     *
     * @return true if in RTL, false otherwise.
     */
    public boolean isRtl() {
        return mIsRtl;
    }

    /*-----------------------------------------------------------------------*/
    // Overloaded methods from ConstraintWidget
    /*-----------------------------------------------------------------------*/

    public BasicMeasure.Measure mMeasure = new BasicMeasure.Measure();

    // @TODO: add description
    public static boolean measure(int level,
            ConstraintWidget widget,
            BasicMeasure.Measurer measurer,
            BasicMeasure.Measure measure,
            int measureStrategy) {
        if (DEBUG) {
            System.out.println(Direct.ls(level) + "(M) call to measure " + widget.getDebugName());
        }
        if (measurer == null) {
            return false;
        }
        if (widget.getVisibility() == GONE
                || widget instanceof Guideline
                || widget instanceof Barrier) {
            if (DEBUG) {
                System.out.println(Direct.ls(level)
                        + "(M) no measure needed for " + widget.getDebugName());
            }
            measure.measuredWidth = 0;
            measure.measuredHeight = 0;
            return false;
        }

        measure.horizontalBehavior = widget.getHorizontalDimensionBehaviour();
        measure.verticalBehavior = widget.getVerticalDimensionBehaviour();
        measure.horizontalDimension = widget.getWidth();
        measure.verticalDimension = widget.getHeight();
        measure.measuredNeedsSolverPass = false;
        measure.measureStrategy = measureStrategy;

        boolean horizontalMatchConstraints =
                (measure.horizontalBehavior == DimensionBehaviour.MATCH_CONSTRAINT);
        boolean verticalMatchConstraints =
                (measure.verticalBehavior == DimensionBehaviour.MATCH_CONSTRAINT);

        boolean horizontalUseRatio = horizontalMatchConstraints && widget.mDimensionRatio > 0;
        boolean verticalUseRatio = verticalMatchConstraints && widget.mDimensionRatio > 0;

        if (horizontalMatchConstraints && widget.hasDanglingDimension(HORIZONTAL)
                && widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD
                && !horizontalUseRatio) {
            horizontalMatchConstraints = false;
            measure.horizontalBehavior = WRAP_CONTENT;
            if (verticalMatchConstraints
                    && widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD) {
                // if match x match, size would be zero.
                measure.horizontalBehavior = FIXED;
            }
        }

        if (verticalMatchConstraints && widget.hasDanglingDimension(VERTICAL)
                && widget.mMatchConstraintDefaultHeight == MATCH_CONSTRAINT_SPREAD
                && !verticalUseRatio) {
            verticalMatchConstraints = false;
            measure.verticalBehavior = WRAP_CONTENT;
            if (horizontalMatchConstraints
                    && widget.mMatchConstraintDefaultWidth == MATCH_CONSTRAINT_SPREAD) {
                // if match x match, size would be zero.
                measure.verticalBehavior = FIXED;
            }
        }

        if (widget.isResolvedHorizontally()) {
            horizontalMatchConstraints = false;
            measure.horizontalBehavior = FIXED;
        }
        if (widget.isResolvedVertically()) {
            verticalMatchConstraints = false;
            measure.verticalBehavior = FIXED;
        }

        if (horizontalUseRatio) {
            if (widget.mResolvedMatchConstraintDefault[HORIZONTAL]
                    == ConstraintWidget.MATCH_CONSTRAINT_RATIO_RESOLVED) {
                measure.horizontalBehavior = FIXED;
            } else if (!verticalMatchConstraints) {
                // let's measure here
                int measuredHeight;
                if (measure.verticalBehavior == FIXED) {
                    measuredHeight = measure.verticalDimension;
                } else {
                    measure.horizontalBehavior = WRAP_CONTENT;
                    measurer.measure(widget, measure);
                    measuredHeight = measure.measuredHeight;
                }
                measure.horizontalBehavior = FIXED;
                // regardless of which side we are using for the ratio, getDimensionRatio() already
                // made sure that it's expressed in WxH format, so we can simply go and multiply
                measure.horizontalDimension = (int) (widget.getDimensionRatio() * measuredHeight);
                if (DEBUG) {
                    System.out.println("(M) Measured once for ratio on horizontal side...");
                }
            }
        }
        if (verticalUseRatio) {
            if (widget.mResolvedMatchConstraintDefault[VERTICAL]
                    == ConstraintWidget.MATCH_CONSTRAINT_RATIO_RESOLVED) {
                measure.verticalBehavior = FIXED;
            } else if (!horizontalMatchConstraints) {
                // let's measure here
                int measuredWidth;
                if (measure.horizontalBehavior == FIXED) {
                    measuredWidth = measure.horizontalDimension;
                } else {
                    measure.verticalBehavior = WRAP_CONTENT;
                    measurer.measure(widget, measure);
                    measuredWidth = measure.measuredWidth;
                }
                measure.verticalBehavior = FIXED;
                if (widget.getDimensionRatioSide() == -1) {
                    // regardless of which side we are using for the ratio,
                    //  getDimensionRatio() already
                    // made sure that it's expressed in WxH format,
                    //  so we can simply go and divide
                    measure.verticalDimension = (int) (measuredWidth / widget.getDimensionRatio());
                } else {
                    // getDimensionRatio() already got reverted, so we can simply multiply
                    measure.verticalDimension = (int) (widget.getDimensionRatio() * measuredWidth);
                }
                if (DEBUG) {
                    System.out.println("(M) Measured once for ratio on vertical side...");
                }
            }
        }

        measurer.measure(widget, measure);
        widget.setWidth(measure.measuredWidth);
        widget.setHeight(measure.measuredHeight);
        widget.setHasBaseline(measure.measuredHasBaseline);
        widget.setBaselineDistance(measure.measuredBaseline);
        measure.measureStrategy = BasicMeasure.Measure.SELF_DIMENSIONS;
        if (DEBUG) {
            System.out.println("(M) Measured " + widget.getDebugName() + " with : "
                    + widget.getHorizontalDimensionBehaviour() + " x "
                    + widget.getVerticalDimensionBehaviour() + " => "
                    + widget.getWidth() + " x " + widget.getHeight());
        }
        return measure.measuredNeedsSolverPass;
    }

    static int sMyCounter = 0;

    /**
     * Layout the tree of widgets
     */
    @Override
    public void layout() {
        if (DEBUG) {
            System.out.println("\n#####################################");
            System.out.println("##          CL LAYOUT PASS           ##");
            System.out.println("#####################################\n");
            mDebugSolverPassCount = 0;
        }

        mX = 0;
        mY = 0;

        mWidthMeasuredTooSmall = false;
        mHeightMeasuredTooSmall = false;
        final int count = mChildren.size();

        int preW = Math.max(0, getWidth());
        int preH = Math.max(0, getHeight());
        DimensionBehaviour originalVerticalDimensionBehaviour =
                mListDimensionBehaviors[DIMENSION_VERTICAL];
        DimensionBehaviour originalHorizontalDimensionBehaviour =
                mListDimensionBehaviors[DIMENSION_HORIZONTAL];

        if (DEBUG_LAYOUT) {
            System.out.println("layout with preW: " + preW + " ("
                    + mListDimensionBehaviors[DIMENSION_HORIZONTAL] + ") preH: " + preH
                    + " (" + mListDimensionBehaviors[DIMENSION_VERTICAL] + ")");
        }

        if (mMetrics != null) {
            mMetrics.layouts++;
        }


        boolean wrap_override = false;

        if (FULL_DEBUG) {
            System.out.println("OPTIMIZATION LEVEL " + mOptimizationLevel);
        }

        // Only try the direct optimization in the first layout pass
        if (mPass == 0 && Optimizer.enabled(mOptimizationLevel, Optimizer.OPTIMIZATION_DIRECT)) {
            if (FULL_DEBUG) {
                System.out.println("Direct pass " + sMyCounter++);
            }
            Direct.solvingPass(this, getMeasurer());
            if (FULL_DEBUG) {
                System.out.println("Direct pass done.");
            }
            for (int i = 0; i < count; i++) {
                ConstraintWidget child = mChildren.get(i);
                if (FULL_DEBUG) {
                    if (child.isInHorizontalChain()) {
                        System.out.print("H");
                    } else {
                        System.out.print(" ");
                    }
                    if (child.isInVerticalChain()) {
                        System.out.print("V");
                    } else {
                        System.out.print(" ");
                    }
                    if (child.isResolvedHorizontally() && child.isResolvedVertically()) {
                        System.out.print("*");
                    } else {
                        System.out.print(" ");
                    }
                    System.out.println("[" + i + "] child " + child.getDebugName()
                            + " H: " + child.isResolvedHorizontally()
                            + " V: " + child.isResolvedVertically());
                }
                if (child.isMeasureRequested()
                        && !(child instanceof Guideline)
                        && !(child instanceof Barrier)
                        && !(child instanceof VirtualLayout)
                        && !child.isInVirtualLayout()) {
                    DimensionBehaviour widthBehavior = child.getDimensionBehaviour(HORIZONTAL);
                    DimensionBehaviour heightBehavior = child.getDimensionBehaviour(VERTICAL);

                    boolean skip = widthBehavior == DimensionBehaviour.MATCH_CONSTRAINT
                            && child.mMatchConstraintDefaultWidth != MATCH_CONSTRAINT_WRAP
                            && heightBehavior == DimensionBehaviour.MATCH_CONSTRAINT
                            && child.mMatchConstraintDefaultHeight != MATCH_CONSTRAINT_WRAP;
                    if (!skip) {
                        BasicMeasure.Measure measure = new BasicMeasure.Measure();
                        ConstraintWidgetContainer.measure(0, child, mMeasurer,
                                measure, BasicMeasure.Measure.SELF_DIMENSIONS);
                    }
                }
            }
            // let's measure children
            if (FULL_DEBUG) {
                System.out.println("Direct pass all done.");
            }
        } else {
            if (FULL_DEBUG) {
                System.out.println("No DIRECT PASS");
            }
        }

        if (count > 2 && (originalHorizontalDimensionBehaviour == WRAP_CONTENT
                || originalVerticalDimensionBehaviour == WRAP_CONTENT)
                && Optimizer.enabled(mOptimizationLevel, Optimizer.OPTIMIZATION_GROUPING)) {
            if (Grouping.simpleSolvingPass(this, getMeasurer())) {
                if (originalHorizontalDimensionBehaviour == WRAP_CONTENT) {
                    if (preW < getWidth() && preW > 0) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("Override width " + getWidth() + " to " + preH);
                        }
                        setWidth(preW);
                        mWidthMeasuredTooSmall = true;
                    } else {
                        preW = getWidth();
                    }
                }
                if (originalVerticalDimensionBehaviour == WRAP_CONTENT) {
                    if (preH < getHeight() && preH > 0) {
                        if (DEBUG_LAYOUT) {
                            System.out.println("Override height " + getHeight() + " to " + preH);
                        }
                        setHeight(preH);
                        mHeightMeasuredTooSmall = true;
                    } else {
                        preH = getHeight();
                    }
                }
                wrap_override = true;
                if (DEBUG_LAYOUT) {
                    System.out.println("layout post opt, preW: " + preW
                            + " (" + mListDimensionBehaviors[DIMENSION_HORIZONTAL]
                            + ") preH: " + preH + " (" + mListDimensionBehaviors[DIMENSION_VERTICAL]
                            + "), new size " + getWidth() + " x " + getHeight());
                }
            }
        }
        boolean useGraphOptimizer = optimizeFor(Optimizer.OPTIMIZATION_GRAPH)
                || optimizeFor(Optimizer.OPTIMIZATION_GRAPH_WRAP);

        mSystem.graphOptimizer = false;
        mSystem.newgraphOptimizer = false;

        if (mOptimizationLevel != Optimizer.OPTIMIZATION_NONE
                && useGraphOptimizer) {
            mSystem.newgraphOptimizer = true;
        }

        @SuppressWarnings("unused") int countSolve = 0;
        final List<ConstraintWidget> allChildren = mChildren;
        boolean hasWrapContent = getHorizontalDimensionBehaviour() == WRAP_CONTENT
                || getVerticalDimensionBehaviour() == WRAP_CONTENT;

        // Reset the chains before iterating on our children
        resetChains();
        countSolve = 0;

        // Before we solve our system, we should call layout() on any
        // of our children that is a container.
        for (int i = 0; i < count; i++) {
            ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof WidgetContainer) {
                ((WidgetContainer) widget).layout();
            }
        }
        boolean optimize = optimizeFor(Optimizer.OPTIMIZATION_GRAPH);

        // Now let's solve our system as usual
        boolean needsSolving = true;
        while (needsSolving) {
            countSolve++;
            try {
                mSystem.reset();
                resetChains();
                if (DEBUG) {
                    String debugName = getDebugName();
                    if (debugName == null) {
                        debugName = "root";
                    }
                    setDebugSolverName(mSystem, debugName);
                    for (int i = 0; i < count; i++) {
                        ConstraintWidget widget = mChildren.get(i);
                        if (widget.getDebugName() != null) {
                            widget.setDebugSolverName(mSystem, widget.getDebugName());
                        }
                    }
                } else {
                    createObjectVariables(mSystem);
                    for (int i = 0; i < count; i++) {
                        ConstraintWidget widget = mChildren.get(i);
                        widget.createObjectVariables(mSystem);
                    }
                }
                needsSolving = addChildrenToSolver(mSystem);
                if (mVerticalWrapMin != null && mVerticalWrapMin.get() != null) {
                    addMinWrap(mVerticalWrapMin.get(), mSystem.createObjectVariable(mTop));
                    mVerticalWrapMin = null;
                }
                if (mVerticalWrapMax != null && mVerticalWrapMax.get() != null) {
                    addMaxWrap(mVerticalWrapMax.get(), mSystem.createObjectVariable(mBottom));
                    mVerticalWrapMax = null;
                }
                if (mHorizontalWrapMin != null && mHorizontalWrapMin.get() != null) {
                    addMinWrap(mHorizontalWrapMin.get(), mSystem.createObjectVariable(mLeft));
                    mHorizontalWrapMin = null;
                }
                if (mHorizontalWrapMax != null && mHorizontalWrapMax.get() != null) {
                    addMaxWrap(mHorizontalWrapMax.get(), mSystem.createObjectVariable(mRight));
                    mHorizontalWrapMax = null;
                }
                if (needsSolving) {
                    mSystem.minimize();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("EXCEPTION : " + e);
            }
            if (needsSolving) {
                needsSolving = updateChildrenFromSolver(mSystem, Optimizer.sFlags);
            } else {
                updateFromSolver(mSystem, optimize);
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    widget.updateFromSolver(mSystem, optimize);
                }
                needsSolving = false;
            }

            if (hasWrapContent && countSolve < MAX_ITERATIONS
                    && Optimizer.sFlags[Optimizer.FLAG_RECOMPUTE_BOUNDS]) {
                // let's get the new bounds
                int maxX = 0;
                int maxY = 0;
                for (int i = 0; i < count; i++) {
                    ConstraintWidget widget = mChildren.get(i);
                    maxX = Math.max(maxX, widget.mX + widget.getWidth());
                    maxY = Math.max(maxY, widget.mY + widget.getHeight());
                }
                maxX = Math.max(mMinWidth, maxX);
                maxY = Math.max(mMinHeight, maxY);
                if (originalHorizontalDimensionBehaviour == WRAP_CONTENT) {
                    if (getWidth() < maxX) {
                        if (DEBUG_LAYOUT) {
                            System.out.println( countSolve +
                                    "layout override width from " + getWidth() + " vs " + maxX);
                        }
                        setWidth(maxX);
                        // force using the solver
                        mListDimensionBehaviors[DIMENSION_HORIZONTAL] = WRAP_CONTENT;
                        wrap_override = true;
                        needsSolving = true;
                    }
                }
                if (originalVerticalDimensionBehaviour == WRAP_CONTENT) {
                    if (getHeight() < maxY) {
                        if (DEBUG_LAYOUT) {
                            System.out.println(
                                    "layout override height from " + getHeight() + " vs " + maxY);
                        }
                        setHeight(maxY);
                        // force using the solver
                        mListDimensionBehaviors[DIMENSION_VERTICAL] = WRAP_CONTENT;
                        wrap_override = true;
                        needsSolving = true;
                    }
                }
            }
            if (true) {
                int width = Math.max(mMinWidth, getWidth());
                if (width > getWidth()) {
                    if (DEBUG_LAYOUT) {
                        System.out.println(
                                "layout override 2, width from " + getWidth() + " vs " + width);
                    }
                    setWidth(width);
                    mListDimensionBehaviors[DIMENSION_HORIZONTAL] = FIXED;
                    wrap_override = true;
                    needsSolving = true;
                }
                int height = Math.max(mMinHeight, getHeight());
                if (height > getHeight()) {
                    if (DEBUG_LAYOUT) {
                        System.out.println(
                                "layout override 2, height from " + getHeight() + " vs " + height);
                    }
                    setHeight(height);
                    mListDimensionBehaviors[DIMENSION_VERTICAL] = FIXED;
                    wrap_override = true;
                    needsSolving = true;
                }

                if (!wrap_override) {
                    if (mListDimensionBehaviors[DIMENSION_HORIZONTAL] == WRAP_CONTENT
                            && preW > 0) {
                        if (getWidth() > preW) {
                            if (DEBUG_LAYOUT) {
                                System.out.println(
                                        "layout override 3, width from " + getWidth() + " vs "
                                                + preW);
                            }
                            mWidthMeasuredTooSmall = true;
                            wrap_override = true;
                            mListDimensionBehaviors[DIMENSION_HORIZONTAL] = FIXED;
                            setWidth(preW);
                            needsSolving = true;
                        }
                    }
                    if (mListDimensionBehaviors[DIMENSION_VERTICAL] == WRAP_CONTENT
                            && preH > 0) {
                        if (getHeight() > preH) {
                            if (DEBUG_LAYOUT) {
                                System.out.println(
                                        "layout override 3, height from " + getHeight() + " vs "
                                                + preH);
                            }
                            mHeightMeasuredTooSmall = true;
                            wrap_override = true;
                            mListDimensionBehaviors[DIMENSION_VERTICAL] = FIXED;
                            setHeight(preH);
                            needsSolving = true;
                        }
                    }
                }

                if (countSolve > MAX_ITERATIONS) {
                    needsSolving = false;
                }
            }
        }
        if (DEBUG_LAYOUT) {
            System.out.println(
                    "Solved system in " + countSolve + " iterations (" + getWidth() + " x "
                            + getHeight() + ")");
        }

        mChildren = (ArrayList<ConstraintWidget>) allChildren;

        if (wrap_override) {
            mListDimensionBehaviors[DIMENSION_HORIZONTAL] = originalHorizontalDimensionBehaviour;
            mListDimensionBehaviors[DIMENSION_VERTICAL] = originalVerticalDimensionBehaviour;
        }

        resetSolverVariables(mSystem.getCache());
    }

    /**
     * Indicates if the container knows how to layout its content on its own
     *
     * @return true if the container does the layout, false otherwise
     */
    public boolean handlesInternalConstraints() {
        return false;
    }

    /*-----------------------------------------------------------------------*/
    // Guidelines
    /*-----------------------------------------------------------------------*/

    /**
     * Accessor to the vertical guidelines contained in the table.
     *
     * @return array of guidelines
     */
    public ArrayList<Guideline> getVerticalGuidelines() {
        ArrayList<Guideline> guidelines = new ArrayList<>();
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof Guideline) {
                Guideline guideline = (Guideline) widget;
                if (guideline.getOrientation() == Guideline.VERTICAL) {
                    guidelines.add(guideline);
                }
            }
        }
        return guidelines;
    }

    /**
     * Accessor to the horizontal guidelines contained in the table.
     *
     * @return array of guidelines
     */
    public ArrayList<Guideline> getHorizontalGuidelines() {
        ArrayList<Guideline> guidelines = new ArrayList<>();
        for (int i = 0, mChildrenSize = mChildren.size(); i < mChildrenSize; i++) {
            final ConstraintWidget widget = mChildren.get(i);
            if (widget instanceof Guideline) {
                Guideline guideline = (Guideline) widget;
                if (guideline.getOrientation() == Guideline.HORIZONTAL) {
                    guidelines.add(guideline);
                }
            }
        }
        return guidelines;
    }

    public LinearSystem getSystem() {
        return mSystem;
    }

    /*-----------------------------------------------------------------------*/
    // Chains
    /*-----------------------------------------------------------------------*/

    /**
     * Reset the chains array. Need to be called before layout.
     */
    private void resetChains() {
        mHorizontalChainsSize = 0;
        mVerticalChainsSize = 0;
    }

    /**
     * Add the chain which constraintWidget is part of. Called by ConstraintWidget::addToSolver()
     *
     * @param type HORIZONTAL or VERTICAL chain
     */
    void addChain(ConstraintWidget constraintWidget, int type) {
        ConstraintWidget widget = constraintWidget;
        if (type == HORIZONTAL) {
            addHorizontalChain(widget);
        } else if (type == VERTICAL) {
            addVerticalChain(widget);
        }
    }

    /**
     * Add a widget to the list of horizontal chains. The widget is the left-most widget
     * of the chain which doesn't have a left dual connection.
     *
     * @param widget widget starting the chain
     */
    private void addHorizontalChain(ConstraintWidget widget) {
        if (mHorizontalChainsSize + 1 >= mHorizontalChainsArray.length) {
            mHorizontalChainsArray = Arrays
                    .copyOf(mHorizontalChainsArray, mHorizontalChainsArray.length * 2);
        }
        mHorizontalChainsArray[mHorizontalChainsSize] = new ChainHead(widget, HORIZONTAL, isRtl());
        mHorizontalChainsSize++;
    }

    /**
     * Add a widget to the list of vertical chains. The widget is the top-most widget
     * of the chain which doesn't have a top dual connection.
     *
     * @param widget widget starting the chain
     */
    private void addVerticalChain(ConstraintWidget widget) {
        if (mVerticalChainsSize + 1 >= mVerticalChainsArray.length) {
            mVerticalChainsArray = Arrays
                    .copyOf(mVerticalChainsArray, mVerticalChainsArray.length * 2);
        }
        mVerticalChainsArray[mVerticalChainsSize] = new ChainHead(widget, VERTICAL, isRtl());
        mVerticalChainsSize++;
    }

    /**
     * Keep track of the # of passes
     */
    public void setPass(int pass) {
        this.mPass = pass;
    }

    // @TODO: add description
    @Override
    public void getSceneString(StringBuilder ret) {

        ret.append(stringId + ":{\n");
        ret.append("  actualWidth:" + mWidth);
        ret.append("\n");
        ret.append("  actualHeight:" + mHeight);
        ret.append("\n");

        ArrayList<ConstraintWidget> children = getChildren();
        for (ConstraintWidget child : children) {
            child.getSceneString(ret);
            ret.append(",\n");
        }
        ret.append("}");

    }
}
