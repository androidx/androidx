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
package androidx.constraintlayout.core;

import java.util.ArrayList;

/**
 * Utility class to track metrics during the system resolution
 */
public class Metrics {
    public long measuresWidgetsDuration; // time spent in child measures in nanoseconds
    public long measuresLayoutDuration; // time spent in child measures in nanoseconds
    public long measuredWidgets;
    public long measuredMatchWidgets;
    public long measures;
    public long additionalMeasures;
    public long resolutions;
    public long tableSizeIncrease;
    public long minimize;
    public long constraints;
    public long simpleconstraints;
    public long optimize;
    public long iterations;
    public long pivots;
    public long bfs;
    public long variables;
    public long errors;
    public long slackvariables;
    public long extravariables;
    public long maxTableSize;
    public long fullySolved;
    public long graphOptimizer;
    public long graphSolved;
    public long linearSolved;
    public long resolvedWidgets;
    public long minimizeGoal;
    public long maxVariables;
    public long maxRows;
    public long nonresolvedWidgets;
    public ArrayList<String> problematicLayouts = new ArrayList<>();
    public long lastTableSize;
    public long widgets;
    public long measuresWrap;
    public long measuresWrapInfeasible;
    public long infeasibleDetermineGroups;
    public long determineGroups;
    public long layouts;
    public long grouping;
    public int mNumberOfLayouts; // the number of times ConstraintLayout onLayout gets called
    public int mNumberOfMeasures; // the number of times child measures gets called
    public long mMeasureDuration; // time spent in measure in nanoseconds
    public long mChildCount; // number of child Views of ConstraintLayout
    public long mMeasureCalls; // number of time CL onMeasure is called
    public long mSolverPasses;
    public long mEquations;
    public long mVariables;
    public long mSimpleEquations;

    // @TODO: add description
    @Override
    public String toString() {
        return "\n*** Metrics ***\n"
                + "measures: " + measures + "\n"
                + "measuresWrap: " + measuresWrap + "\n"
                + "measuresWrapInfeasible: " + measuresWrapInfeasible + "\n"
                + "determineGroups: " + determineGroups + "\n"
                + "infeasibleDetermineGroups: " + infeasibleDetermineGroups + "\n"
                + "graphOptimizer: " + graphOptimizer + "\n"
                + "widgets: " + widgets + "\n"
                + "graphSolved: " + graphSolved + "\n"
                + "linearSolved: " + linearSolved + "\n";
    }

    // @TODO: add description
    public void reset() {
        measures = 0;
        widgets = 0;
        additionalMeasures = 0;
        resolutions = 0;
        tableSizeIncrease = 0;
        maxTableSize = 0;
        lastTableSize = 0;
        maxVariables = 0;
        maxRows = 0;
        minimize = 0;
        minimizeGoal = 0;
        constraints = 0;
        simpleconstraints = 0;
        optimize = 0;
        iterations = 0;
        pivots = 0;
        bfs = 0;
        variables = 0;
        errors = 0;
        slackvariables = 0;
        extravariables = 0;
        fullySolved = 0;
        graphOptimizer = 0;
        graphSolved = 0;
        resolvedWidgets = 0;
        nonresolvedWidgets = 0;
        linearSolved = 0;
        problematicLayouts.clear();
        mNumberOfMeasures = 0;
        mNumberOfLayouts = 0;
        measuresWidgetsDuration = 0;
        measuresLayoutDuration = 0;
        mChildCount = 0;
        mMeasureDuration = 0;
        mMeasureCalls = 0;
        mSolverPasses = 0;
        mVariables = 0;
        mEquations = 0;
        mSimpleEquations = 0;
    }

    /**
     * Copy the values from and existing Metrics class
     * @param metrics
     */
    public void copy(Metrics metrics) {
        mVariables = metrics.mVariables;
        mEquations = metrics.mEquations;
        mSimpleEquations = metrics.mSimpleEquations;
        mNumberOfMeasures = metrics.mNumberOfMeasures;
        mNumberOfLayouts = metrics.mNumberOfLayouts;
        mMeasureDuration = metrics.mMeasureDuration;
        mChildCount = metrics.mChildCount;
        mMeasureCalls = metrics.mMeasureCalls;
        measuresWidgetsDuration = metrics.measuresWidgetsDuration;
        mSolverPasses = metrics.mSolverPasses;

        measuresLayoutDuration = metrics.measuresLayoutDuration;
        measures = metrics.measures;
        widgets = metrics.widgets;
        additionalMeasures = metrics.additionalMeasures;
        resolutions = metrics.resolutions;
        tableSizeIncrease = metrics.tableSizeIncrease;
        maxTableSize = metrics.maxTableSize;
        lastTableSize = metrics.lastTableSize;
        maxVariables = metrics.maxVariables;
        maxRows = metrics.maxRows;
        minimize = metrics.minimize;
        minimizeGoal = metrics.minimizeGoal;
        constraints = metrics.constraints;
        simpleconstraints = metrics.simpleconstraints;
        optimize = metrics.optimize;
        iterations = metrics.iterations;
        pivots = metrics.pivots;
        bfs = metrics.bfs;
        variables = metrics.variables;
        errors = metrics.errors;
        slackvariables = metrics.slackvariables;
        extravariables = metrics.extravariables;
        fullySolved = metrics.fullySolved;
        graphOptimizer = metrics.graphOptimizer;
        graphSolved = metrics.graphSolved;
        resolvedWidgets = metrics.resolvedWidgets;
        nonresolvedWidgets = metrics.nonresolvedWidgets;
    }
}
