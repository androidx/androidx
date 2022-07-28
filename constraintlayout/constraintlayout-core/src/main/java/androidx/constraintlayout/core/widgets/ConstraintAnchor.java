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

import androidx.constraintlayout.core.Cache;
import androidx.constraintlayout.core.SolverVariable;
import androidx.constraintlayout.core.widgets.analyzer.Grouping;
import androidx.constraintlayout.core.widgets.analyzer.WidgetGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Model a constraint relation. Widgets contains anchors, and a constraint relation between
 * two widgets is made by connecting one anchor to another. The anchor will contains a pointer
 * to the target anchor if it is connected.
 */
public class ConstraintAnchor {

    private static final boolean ALLOW_BINARY = false;

    private HashSet<ConstraintAnchor> mDependents = null;
    private int mFinalValue;
    private boolean mHasFinalValue;

    // @TODO: add description
    public void findDependents(int orientation, ArrayList<WidgetGroup> list, WidgetGroup group) {
        if (mDependents != null) {
            for (ConstraintAnchor anchor : mDependents) {
                Grouping.findDependents(anchor.mOwner, orientation, list, group);
            }
        }
    }

    public HashSet<ConstraintAnchor> getDependents() {
        return mDependents;
    }

    // @TODO: add description
    public boolean hasDependents() {
        if (mDependents == null) {
            return false;
        }
        return mDependents.size() > 0;
    }

    // @TODO: add description
    public boolean hasCenteredDependents() {
        if (mDependents == null) {
            return false;
        }
        for (ConstraintAnchor anchor : mDependents) {
            ConstraintAnchor opposite = anchor.getOpposite();
            if (opposite.isConnected()) {
                return true;
            }
        }
        return false;
    }

    // @TODO: add description
    public void setFinalValue(int finalValue) {
        this.mFinalValue = finalValue;
        this.mHasFinalValue = true;
    }

    // @TODO: add description
    public int getFinalValue() {
        if (!mHasFinalValue) {
            return 0;
        }
        return mFinalValue;
    }

    // @TODO: add description
    public void resetFinalResolution() {
        mHasFinalValue = false;
        mFinalValue = 0;
    }

    // @TODO: add description
    public boolean hasFinalValue() {
        return mHasFinalValue;
    }

    /**
     * Define the type of anchor
     */
    public enum Type {NONE, LEFT, TOP, RIGHT, BOTTOM, BASELINE, CENTER, CENTER_X, CENTER_Y}

    private static final int UNSET_GONE_MARGIN = Integer.MIN_VALUE;

    public final ConstraintWidget mOwner;
    public final Type mType;
    public ConstraintAnchor mTarget;
    public int mMargin = 0;
    int mGoneMargin = UNSET_GONE_MARGIN;

    SolverVariable mSolverVariable;

    // @TODO: add description
    public void copyFrom(ConstraintAnchor source, HashMap<ConstraintWidget, ConstraintWidget> map) {
        if (mTarget != null) {
            if (mTarget.mDependents != null) {
                mTarget.mDependents.remove(this);
            }
        }
        if (source.mTarget != null) {
            Type type = source.mTarget.getType();
            ConstraintWidget owner = map.get(source.mTarget.mOwner);
            mTarget = owner.getAnchor(type);
        } else {
            mTarget = null;
        }
        if (mTarget != null) {
            if (mTarget.mDependents == null) {
                mTarget.mDependents = new HashSet<>();
            }
            mTarget.mDependents.add(this);
        }
        mMargin = source.mMargin;
        mGoneMargin = source.mGoneMargin;
    }

    /**
     * Constructor
     *
     * @param owner the widget owner of this anchor.
     * @param type  the anchor type.
     */
    public ConstraintAnchor(ConstraintWidget owner, Type type) {
        mOwner = owner;
        mType = type;
    }

    /**
     * Return the solver variable for this anchor
     */
    public SolverVariable getSolverVariable() {
        return mSolverVariable;
    }

    /**
     * Reset the solver variable
     */
    public void resetSolverVariable(Cache cache) {
        if (mSolverVariable == null) {
            mSolverVariable = new SolverVariable(SolverVariable.Type.UNRESTRICTED, null);
        } else {
            mSolverVariable.reset();
        }
    }

    /**
     * Return the anchor's owner
     *
     * @return the Widget owning the anchor
     */
    public ConstraintWidget getOwner() {
        return mOwner;
    }

    /**
     * Return the type of the anchor
     *
     * @return type of the anchor.
     */
    public Type getType() {
        return mType;
    }

    /**
     * Return the connection's margin from this anchor to its target.
     *
     * @return the margin value. 0 if not connected.
     */
    public int getMargin() {
        if (mOwner.getVisibility() == ConstraintWidget.GONE) {
            return 0;
        }
        if (mGoneMargin != UNSET_GONE_MARGIN && mTarget != null
                && mTarget.mOwner.getVisibility() == ConstraintWidget.GONE) {
            return mGoneMargin;
        }
        return mMargin;
    }

    /**
     * Return the connection's target (null if not connected)
     *
     * @return the ConstraintAnchor target
     */
    public ConstraintAnchor getTarget() {
        return mTarget;
    }

    /**
     * Resets the anchor's connection.
     */
    public void reset() {
        if (mTarget != null && mTarget.mDependents != null) {
            mTarget.mDependents.remove(this);
            if (mTarget.mDependents.size() == 0) {
                mTarget.mDependents = null;
            }
        }
        mDependents = null;
        mTarget = null;
        mMargin = 0;
        mGoneMargin = UNSET_GONE_MARGIN;
        mHasFinalValue = false;
        mFinalValue = 0;
    }

    /**
     * Connects this anchor to another one.
     *
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin, int goneMargin,
            boolean forceConnection) {
        if (toAnchor == null) {
            reset();
            return true;
        }
        if (!forceConnection && !isValidConnection(toAnchor)) {
            return false;
        }
        mTarget = toAnchor;
        if (mTarget.mDependents == null) {
            mTarget.mDependents = new HashSet<>();
        }
        if (mTarget.mDependents != null) {
            mTarget.mDependents.add(this);
        }
        mMargin = margin;
        mGoneMargin = goneMargin;
        return true;
    }


    /**
     * Connects this anchor to another one.
     *
     * @return true if the connection succeeds.
     */
    public boolean connect(ConstraintAnchor toAnchor, int margin) {
        return connect(toAnchor, margin, UNSET_GONE_MARGIN, false);
    }

    /**
     * Returns the connection status of this anchor
     *
     * @return true if the anchor is connected to another one.
     */
    public boolean isConnected() {
        return mTarget != null;
    }

    /**
     * Checks if the connection to a given anchor is valid.
     *
     * @param anchor the anchor we want to connect to
     * @return true if it's a compatible anchor
     */
    public boolean isValidConnection(ConstraintAnchor anchor) {
        if (anchor == null) {
            return false;
        }
        Type target = anchor.getType();
        if (target == mType) {
            if (mType == Type.BASELINE
                    && (!anchor.getOwner().hasBaseline() || !getOwner().hasBaseline())) {
                return false;
            }
            return true;
        }
        switch (mType) {
            case CENTER: {
                // allow everything but baseline and center_x/center_y
                return target != Type.BASELINE && target != Type.CENTER_X
                        && target != Type.CENTER_Y;
            }
            case LEFT:
            case RIGHT: {
                boolean isCompatible = target == Type.LEFT || target == Type.RIGHT;
                if (anchor.getOwner() instanceof Guideline) {
                    isCompatible = isCompatible || target == Type.CENTER_X;
                }
                return isCompatible;
            }
            case TOP:
            case BOTTOM: {
                boolean isCompatible = target == Type.TOP || target == Type.BOTTOM;
                if (anchor.getOwner() instanceof Guideline) {
                    isCompatible = isCompatible || target == Type.CENTER_Y;
                }
                return isCompatible;
            }
            case BASELINE: {
                if (target == Type.LEFT || target == Type.RIGHT) {
                    return false;
                }
                return true;
            }
            case CENTER_X:
            case CENTER_Y:
            case NONE:
                return false;
        }
        throw new AssertionError(mType.name());
    }

    /**
     * Return true if this anchor is a side anchor
     *
     * @return true if side anchor
     */
    public boolean isSideAnchor() {
        switch (mType) {
            case LEFT:
            case RIGHT:
            case TOP:
            case BOTTOM:
                return true;
            case BASELINE:
            case CENTER:
            case CENTER_X:
            case CENTER_Y:
            case NONE:
                return false;
        }
        throw new AssertionError(mType.name());
    }

    /**
     * Return true if the connection to the given anchor is in the
     * same dimension (horizontal or vertical)
     *
     * @param anchor the anchor we want to connect to
     * @return true if it's an anchor on the same dimension
     */
    public boolean isSimilarDimensionConnection(ConstraintAnchor anchor) {
        Type target = anchor.getType();
        if (target == mType) {
            return true;
        }
        switch (mType) {
            case CENTER: {
                return target != Type.BASELINE;
            }
            case LEFT:
            case RIGHT:
            case CENTER_X: {
                return target == Type.LEFT || target == Type.RIGHT || target == Type.CENTER_X;
            }
            case TOP:
            case BOTTOM:
            case CENTER_Y:
            case BASELINE: {
                return target == Type.TOP || target == Type.BOTTOM
                        || target == Type.CENTER_Y || target == Type.BASELINE;
            }
            case NONE:
                return false;
        }
        throw new AssertionError(mType.name());
    }

    /**
     * Set the margin of the connection (if there's one)
     *
     * @param margin the new margin of the connection
     */
    public void setMargin(int margin) {
        if (isConnected()) {
            mMargin = margin;
        }
    }

    /**
     * Set the gone margin of the connection (if there's one)
     *
     * @param margin the new margin of the connection
     */
    public void setGoneMargin(int margin) {
        if (isConnected()) {
            mGoneMargin = margin;
        }
    }

    /**
     * Utility function returning true if this anchor is a vertical one.
     *
     * @return true if vertical anchor, false otherwise
     */
    public boolean isVerticalAnchor() {
        switch (mType) {
            case LEFT:
            case RIGHT:
            case CENTER:
            case CENTER_X:
                return false;
            case CENTER_Y:
            case TOP:
            case BOTTOM:
            case BASELINE:
            case NONE:
                return true;
        }
        throw new AssertionError(mType.name());
    }

    /**
     * Return a string representation of this anchor
     *
     * @return string representation of the anchor
     */
    @Override
    public String toString() {
        return mOwner.getDebugName() + ":" + mType.toString();
    }

    /**
     * Return true if we can connect this anchor to this target.
     * We recursively follow connections in order to detect eventual cycles; if we
     * do we disallow the connection.
     * We also only allow connections to direct parent, siblings, and descendants.
     *
     * @param target the ConstraintWidget we are trying to connect to
     * @param anchor Allow anchor if it loops back to me directly
     * @return if the connection is allowed, false otherwise
     */
    public boolean isConnectionAllowed(ConstraintWidget target, ConstraintAnchor anchor) {
        if (ALLOW_BINARY) {
            if (anchor != null && anchor.getTarget() == this) {
                return true;
            }
        }
        return isConnectionAllowed(target);
    }

    /**
     * Return true if we can connect this anchor to this target.
     * We recursively follow connections in order to detect eventual cycles; if we
     * do we disallow the connection.
     * We also only allow connections to direct parent, siblings, and descendants.
     *
     * @param target the ConstraintWidget we are trying to connect to
     * @return true if the connection is allowed, false otherwise
     */
    public boolean isConnectionAllowed(ConstraintWidget target) {
        HashSet<ConstraintWidget> checked = new HashSet<>();
        if (isConnectionToMe(target, checked)) {
            return false;
        }
        ConstraintWidget parent = getOwner().getParent();
        if (parent == target) { // allow connections to parent
            return true;
        }
        if (target.getParent() == parent) { // allow if we share the same parent
            return true;
        }
        return false;
    }

    /**
     * Recursive with check for loop
     *
     * @param checked set of things already checked
     * @return true if it is connected to me
     */
    private boolean isConnectionToMe(ConstraintWidget target, HashSet<ConstraintWidget> checked) {
        if (checked.contains(target)) {
            return false;
        }
        checked.add(target);

        if (target == getOwner()) {
            return true;
        }
        ArrayList<ConstraintAnchor> targetAnchors = target.getAnchors();
        for (int i = 0, targetAnchorsSize = targetAnchors.size(); i < targetAnchorsSize; i++) {
            ConstraintAnchor anchor = targetAnchors.get(i);
            if (anchor.isSimilarDimensionConnection(this) && anchor.isConnected()) {
                if (isConnectionToMe(anchor.getTarget().getOwner(), checked)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the opposite anchor to this one
     *
     * @return opposite anchor
     */
    public final ConstraintAnchor getOpposite() {
        switch (mType) {
            case LEFT: {
                return mOwner.mRight;
            }
            case RIGHT: {
                return mOwner.mLeft;
            }
            case TOP: {
                return mOwner.mBottom;
            }
            case BOTTOM: {
                return mOwner.mTop;
            }
            case BASELINE:
            case CENTER:
            case CENTER_X:
            case CENTER_Y:
            case NONE:
                return null;
        }
        throw new AssertionError(mType.name());
    }
}
