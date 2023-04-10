/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static androidx.constraintlayout.core.widgets.ConstraintWidget.BOTH;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.HORIZONTAL;
import static androidx.constraintlayout.core.widgets.ConstraintWidget.VERTICAL;

import androidx.constraintlayout.core.LinearSystem;
import androidx.constraintlayout.core.widgets.Chain;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.ConstraintWidgetContainer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Represents a group of widget for the grouping mechanism.
 */
public class WidgetGroup {
    private static final boolean DEBUG = false;
    ArrayList<ConstraintWidget> mWidgets = new ArrayList<>();
    static int sCount = 0;
    int mId = -1;
    boolean mAuthoritative = false;
    int mOrientation = HORIZONTAL;
    ArrayList<MeasureResult> mResults = null;
    private int mMoveTo = -1;

    public WidgetGroup(int orientation) {
        mId = sCount++;
        this.mOrientation = orientation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public int getId() {
        return mId;
    }

    // @TODO: add description
    public boolean add(ConstraintWidget widget) {
        if (mWidgets.contains(widget)) {
            return false;
        }
        mWidgets.add(widget);
        return true;
    }

    public void setAuthoritative(boolean isAuthoritative) {
        mAuthoritative = isAuthoritative;
    }

    public boolean isAuthoritative() {
        return mAuthoritative;
    }

    private String getOrientationString() {
        if (mOrientation == HORIZONTAL) {
            return "Horizontal";
        } else if (mOrientation == VERTICAL) {
            return "Vertical";
        } else if (mOrientation == BOTH) {
            return "Both";
        }
        return "Unknown";
    }

    @Override
    public String toString() {
        String ret = getOrientationString() + " [" + mId + "] <";
        for (ConstraintWidget widget : mWidgets) {
            ret += " " + widget.getDebugName();
        }
        ret += " >";
        return ret;
    }

    // @TODO: add description
    public void moveTo(int orientation, WidgetGroup widgetGroup) {
        if (DEBUG) {
            System.out.println("Move all widgets (" + this + ") from "
                    + mId + " to " + widgetGroup.getId() + "(" + widgetGroup + ")");
            System.out.println("" +
                    "do not call  "+ measureWrap(  orientation, new ConstraintWidget()));
        }
        for (ConstraintWidget widget : mWidgets) {
            widgetGroup.add(widget);
            if (orientation == HORIZONTAL) {
                widget.horizontalGroup = widgetGroup.getId();
            } else {
                widget.verticalGroup = widgetGroup.getId();
            }
        }
        mMoveTo = widgetGroup.mId;
    }

    // @TODO: add description
    public void clear() {
        mWidgets.clear();
    }

    private int measureWrap(int orientation, ConstraintWidget widget) {
        ConstraintWidget.DimensionBehaviour behaviour = widget.getDimensionBehaviour(orientation);
        if (behaviour == ConstraintWidget.DimensionBehaviour.WRAP_CONTENT
                || behaviour == ConstraintWidget.DimensionBehaviour.MATCH_PARENT
                || behaviour == ConstraintWidget.DimensionBehaviour.FIXED) {
            int dimension;
            if (orientation == HORIZONTAL) {
                dimension = widget.getWidth();
            } else {
                dimension = widget.getHeight();
            }
            return dimension;
        }
        return -1;
    }

    // @TODO: add description
    public int measureWrap(LinearSystem system, int orientation) {
        int count = mWidgets.size();
        if (count == 0) {
            return 0;
        }
        // TODO: add direct wrap computation for simpler cases instead of calling the solver
        return solverMeasure(system, mWidgets, orientation);
    }

    private int solverMeasure(LinearSystem system,
            ArrayList<ConstraintWidget> widgets,
            int orientation) {
        ConstraintWidgetContainer container =
                (ConstraintWidgetContainer) widgets.get(0).getParent();
        system.reset();
        @SuppressWarnings("unused") boolean prevDebug = LinearSystem.FULL_DEBUG;
        container.addToSolver(system, false);
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            widget.addToSolver(system, false);
        }
        if (orientation == HORIZONTAL) {
            if (container.mHorizontalChainsSize > 0) {
                Chain.applyChainConstraints(container, system, widgets, HORIZONTAL);
            }
        }
        if (orientation == VERTICAL) {
            if (container.mVerticalChainsSize > 0) {
                Chain.applyChainConstraints(container, system, widgets, VERTICAL);
            }
        }

        try {
            system.minimize();
        } catch (Exception e) {
            //TODO remove fancy version of e.printStackTrace()
            System.err.println(e.toString()+"\n"+Arrays.toString(e.getStackTrace())
                    .replace("[","   at ")
                    .replace(",","\n   at")
                    .replace("]",""));
        }

        // save results
        mResults = new ArrayList<>();
        for (int i = 0; i < widgets.size(); i++) {
            ConstraintWidget widget = widgets.get(i);
            MeasureResult result = new MeasureResult(widget, system, orientation);
            mResults.add(result);
        }

        if (orientation == HORIZONTAL) {
            int left = system.getObjectVariableValue(container.mLeft);
            int right = system.getObjectVariableValue(container.mRight);
            system.reset();
            return right - left;
        } else {
            int top = system.getObjectVariableValue(container.mTop);
            int bottom = system.getObjectVariableValue(container.mBottom);
            system.reset();
            return bottom - top;
        }
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    // @TODO: add description
    public void apply() {
        if (mResults == null) {
            return;
        }
        if (!mAuthoritative) {
            return;
        }
        for (int i = 0; i < mResults.size(); i++) {
            MeasureResult result = mResults.get(i);
            result.apply();
        }
    }

    // @TODO: add description
    public boolean intersectWith(WidgetGroup group) {
        for (int i = 0; i < mWidgets.size(); i++) {
            ConstraintWidget widget = mWidgets.get(i);
            if (group.contains(widget)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(ConstraintWidget widget) {
        return mWidgets.contains(widget);
    }

    // @TODO: add description
    public int size() {
        return mWidgets.size();
    }

    // @TODO: add description
    public void cleanup(ArrayList<WidgetGroup> dependencyLists) {
        final int count = mWidgets.size();
        if (mMoveTo != -1 && count > 0) {
            for (int i = 0; i < dependencyLists.size(); i++) {
                WidgetGroup group = dependencyLists.get(i);
                if (mMoveTo == group.mId) {
                    moveTo(mOrientation, group);
                }
            }
        }
        if (count == 0) {
            dependencyLists.remove(this);
            return;
        }
    }


    static class MeasureResult {
        WeakReference<ConstraintWidget> mWidgetRef;
        int mLeft;
        int mTop;
        int mRight;
        int mBottom;
        int mBaseline;
        int mOrientation;

        MeasureResult(ConstraintWidget widget, LinearSystem system, int orientation) {
            mWidgetRef = new WeakReference<>(widget);
            mLeft = system.getObjectVariableValue(widget.mLeft);
            mTop = system.getObjectVariableValue(widget.mTop);
            mRight = system.getObjectVariableValue(widget.mRight);
            mBottom = system.getObjectVariableValue(widget.mBottom);
            mBaseline = system.getObjectVariableValue(widget.mBaseline);
            this.mOrientation = orientation;
        }

        public void apply() {
            ConstraintWidget widget = mWidgetRef.get();
            if (widget != null) {
                widget.setFinalFrame(mLeft, mTop, mRight, mBottom, mBaseline, mOrientation);
            }
        }
    }
}
