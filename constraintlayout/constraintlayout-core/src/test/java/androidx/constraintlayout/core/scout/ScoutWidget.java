/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.constraintlayout.core.scout;

import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.Guideline;
import androidx.constraintlayout.core.widgets.WidgetContainer;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Main Wrapper class for Constraint Widgets
 */
public class ScoutWidget implements Comparable<ScoutWidget> {
    private static final boolean DEBUG = false;
    private static final float MAXIMUM_STRETCH_GAP = 0.6f; // percentage
    private float mX;
    private float mY;
    private float mWidth;
    private float mHeight;
    private float mBaseLine;
    private ScoutWidget mParent;
    private float mRootDistance;
    private float[] mDistToRootCache = new float[]{-1, -1, -1, -1};
    ConstraintWidget mConstraintWidget;
    private boolean mKeepExistingConnections = true;
    private Rectangle mRectangle;

    public ScoutWidget(ConstraintWidget constraintWidget, ScoutWidget parent) {
        this.mConstraintWidget = constraintWidget;
        this.mParent = parent;
        this.mX = constraintWidget.getX();
        this.mY = constraintWidget.getY();
        this.mWidth = constraintWidget.getWidth();
        this.mHeight = constraintWidget.getHeight();
        this.mBaseLine = mConstraintWidget.getBaselineDistance() + constraintWidget.getY();
        if (parent != null) {
            mRootDistance = distance(parent, this);
        }
    }

    /**
     * Sets the order root first
     * followed by outside to inside, top to bottom, left to right
     */
    @Override
    public int compareTo(ScoutWidget scoutWidget) {
        if (mParent == null) {
            return -1;
        }
        if (mRootDistance != scoutWidget.mRootDistance) {
            return Float.compare(mRootDistance, scoutWidget.mRootDistance);
        }
        if (mY != scoutWidget.mY) {
            return Float.compare(mY, scoutWidget.mY);
        }
        if (mX != scoutWidget.mX) {
            return Float.compare(mX, scoutWidget.mX);
        }
        return 0;
    }

    @Override
    public String toString() {
        return mConstraintWidget.getDebugName();
    }

    boolean isRoot() {
        return mParent == null;
    }

    /**
     * is this a guideline
     */
    public boolean isGuideline() {
        return mConstraintWidget instanceof Guideline;
    }

    /**
     * is guideline vertical
     */
    public boolean isVerticalGuideline() {
        if (mConstraintWidget instanceof Guideline) {
            Guideline g = (Guideline) mConstraintWidget;
            return g.getOrientation() == Guideline.VERTICAL;
        }
        return false;
    }

    /**
     * is this a horizontal guide line on the image
     */
    public boolean isHorizontalGuideline() {
        if (mConstraintWidget instanceof Guideline) {
            Guideline g = (Guideline) mConstraintWidget;
            return g.getOrientation() == Guideline.HORIZONTAL;
        }
        return false;
    }

    /**
     * Wrap an array of ConstraintWidgets into an array of InferWidgets
     */
    public static ScoutWidget[] create(ConstraintWidget[] array) {
        ScoutWidget[] ret = new ScoutWidget[array.length];
        ConstraintWidget root = array[0];

        ScoutWidget rootwidget = new ScoutWidget(root, null);
        ret[0] = rootwidget;
        int count = 1;
        for (int i = 0; i < ret.length; i++) {
            if (array[i] != root) {
                ret[count++] = new ScoutWidget(array[i], rootwidget);
            }
        }
        Arrays.sort(ret);
        if (DEBUG) {
            for (int i = 0; i < ret.length; i++) {
                System.out.println(
                        "[" + i + "] -> " + ret[i].mConstraintWidget + "    "
                                + ret[i].mRootDistance);
            }
        }
        return ret;
    }

    // above = 0, below = 1, left = 2, right = 3
    float getLocation(Direction dir) {
        switch (dir) {
            case NORTH:
                return mY;
            case SOUTH:
                return mY + mHeight;
            case WEST:
                return mX;
            case EAST:
                return mX + mWidth;
            case BASE:
                return mBaseLine;
        }
        return mBaseLine;
    }

    /**
     * simple accessor for the height
     *
     * @return the height of the widget
     */
    public float getHeight() {
        return mHeight;
    }

    /**
     * simple accessor for the width
     *
     * @return the width of the widget
     */
    public float getWidth() {
        return mWidth;
    }

    /**
     * simple accessor for the X position
     *
     * @return the X position of the widget
     */
    final float getX() {
        return mX;
    }

    /**
     * simple accessor for the Y position
     *
     * @return the Y position of the widget
     */
    final float getY() {
        return mY;
    }

    /**
     * This calculates a constraint tables and applies them to the widgets
     * TODO break up into creation of a constraint table and apply
     *
     * @param list ordered list of widgets root must be list[0]
     */
    public static void computeConstraints(ScoutWidget[] list) {
        ScoutProbabilities table = new ScoutProbabilities();
        table.computeConstraints(list);
        table.applyConstraints(list);
    }

    private static ConstraintAnchor.Type lookupType(int dir) {
        return lookupType(Direction.get(dir));
    }

    /**
     * map integer direction to ConstraintAnchor.Type
     *
     * @param dir integer direction
     */
    private static ConstraintAnchor.Type lookupType(Direction dir) {
        switch (dir) {
            case NORTH:
                return ConstraintAnchor.Type.TOP;
            case SOUTH:
                return ConstraintAnchor.Type.BOTTOM;
            case WEST:
                return ConstraintAnchor.Type.LEFT;
            case EAST:
                return ConstraintAnchor.Type.RIGHT;
            case BASE:
                return ConstraintAnchor.Type.BASELINE;
        }
        return ConstraintAnchor.Type.NONE;
    }

    /**
     * set a centered constraint if possible return true if it did
     *
     * @param dir   direction 0 = vertical
     * @param to1   first widget  to connect to
     * @param to2   second widget to connect to
     * @param cDir1 the side of first widget to connect to
     * @param cDir2 the sed of the second widget to connect to
     * @param gap   the gap
     * @return true if it was able to connect
     */
    boolean setCentered(int dir, ScoutWidget to1, ScoutWidget to2, Direction cDir1, Direction cDir2,
            float gap) {
        Direction ori = (dir == 0) ? Direction.NORTH : Direction.WEST;
        ConstraintAnchor anchor1 = mConstraintWidget.getAnchor(lookupType(ori));
        ConstraintAnchor anchor2 = mConstraintWidget.getAnchor(lookupType(ori.getOpposite()));

        if (mKeepExistingConnections && (anchor1.isConnected() || anchor2.isConnected())) {
            if (anchor1.isConnected() ^ anchor2.isConnected()) {
                return false;
            }
            if (anchor1.isConnected()
                    && (anchor1.getTarget().getOwner() != to1.mConstraintWidget)) {
                return false;
            }
            if (anchor2.isConnected()
                    && (anchor2.getTarget().getOwner() != to2.mConstraintWidget)) {
                return false;
            }
        }

        if (anchor1.isConnectionAllowed(to1.mConstraintWidget)
                && anchor2.isConnectionAllowed(to2.mConstraintWidget)) {
            // Resize
            if (!isResizable(dir)) {
                if (dir == 0) {
                    int height = mConstraintWidget.getHeight();
                    float stretchRatio = (gap * 2) / (float) height;
                    if (isCandidateResizable(dir) && stretchRatio < MAXIMUM_STRETCH_GAP) {
                        mConstraintWidget.setVerticalDimensionBehaviour(
                                ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                    } else {
                        gap = 0;
                    }
                } else {
                    int width = mConstraintWidget.getWidth();
                    float stretchRatio = (gap * 2) / (float) width;
                    if (isCandidateResizable(dir) && stretchRatio < MAXIMUM_STRETCH_GAP) {
                        mConstraintWidget.setHorizontalDimensionBehaviour(
                                ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT);
                    } else {
                        gap = 0;
                    }
                }
            }

            if (to1.equals(to2)) {
                connect(mConstraintWidget, lookupType(cDir1), to1.mConstraintWidget,
                        lookupType(cDir1), (int) gap);
                connect(mConstraintWidget, lookupType(cDir2), to2.mConstraintWidget,
                        lookupType(cDir2), (int) gap);
            } else {

                float pos1 = to1.getLocation(cDir1);
                float pos2 = to2.getLocation(cDir2);
                Direction c1 = (pos1 < pos2) ? (ori) : (ori.getOpposite());
                Direction c2 = (pos1 > pos2) ? (ori) : (ori.getOpposite());
                int gap1 = gap(mConstraintWidget, c1, to1.mConstraintWidget, cDir1);
                int gap2 = gap(mConstraintWidget, c2, to2.mConstraintWidget, cDir2);

                connect(mConstraintWidget, lookupType(c1), to1.mConstraintWidget, lookupType(cDir1),
                        Math.max(0, gap1));
                connect(mConstraintWidget, lookupType(c2), to2.mConstraintWidget, lookupType(cDir2),
                        Math.max(0, gap2));
            }
            return true;
        } else {

            return false;
        }
    }

    /**
     * Get the gap between two specific edges of widgets
     *
     * @return distance in dp
     */
    private static int gap(ConstraintWidget widget1, Direction direction1,
            ConstraintWidget widget2, Direction direction2) {
        switch (direction1) {
            case NORTH:
            case WEST:
                return getPos(widget1, direction1) - getPos(widget2, direction2);
            case SOUTH:
            case EAST:
                return getPos(widget2, direction2) - getPos(widget1, direction1);
        }
        return 0;
    }

    /**
     * Get the position of a edge of a widget
     */
    private static int getPos(ConstraintWidget widget, Direction direction) {
        switch (direction) {
            case NORTH:
                return widget.getY();
            case SOUTH:
                return widget.getY() + widget.getHeight();
            case WEST:
                return widget.getX();
            case EAST:
                return widget.getX() + widget.getWidth();
        }
        return 0;
    }

    /**
     * set a centered constraint if possible return true if it did
     *
     * @param dir   direction 0 = vertical
     * @param to1   first widget  to connect to
     * @param cDir1 the side of first widget to connect to
     * @return true if it was able to connect
     */
    boolean setEdgeCentered(int dir, ScoutWidget to1, Direction cDir1) {
        Direction ori = (dir == 0) ? Direction.NORTH : Direction.WEST;
        ConstraintAnchor anchor1 = mConstraintWidget.getAnchor(lookupType(ori));
        ConstraintAnchor anchor2 = mConstraintWidget.getAnchor(lookupType(ori.getOpposite()));

        if (mKeepExistingConnections && (anchor1.isConnected() || anchor2.isConnected())) {
            if (anchor1.isConnected() ^ anchor2.isConnected()) {
                return false;
            }
            if (anchor1.isConnected()
                    && (anchor1.getTarget().getOwner() != to1.mConstraintWidget)) {
                return false;
            }
        }

        if (anchor1.isConnectionAllowed(to1.mConstraintWidget)) {
            connect(mConstraintWidget, lookupType(ori), to1.mConstraintWidget, lookupType(cDir1),
                    0);
            connect(mConstraintWidget, lookupType(ori.getOpposite()), to1.mConstraintWidget,
                    lookupType(cDir1),
                    0);
        }
        return true;
    }


    private static void connect(ConstraintWidget fromWidget, ConstraintAnchor.Type fromType,
            ConstraintWidget toWidget, ConstraintAnchor.Type toType, int gap) {
        fromWidget.connect(fromType, toWidget, toType, gap);
//        fromWidget.getAnchor(fromType).setConnectionCreator(ConstraintAnchor.SCOUT_CREATOR);
    }

    private static void connectWeak(ConstraintWidget fromWidget,
            ConstraintAnchor.Type fromType, ConstraintWidget toWidget,
            ConstraintAnchor.Type toType, int gap) {
        fromWidget.connect(fromType, toWidget, toType, gap);
//        fromWidget.connect(fromType, toWidget, toType, gap, ConstraintAnchor.Strength.WEAK);
//        fromWidget.getAnchor(fromType).setConnectionCreator(ConstraintAnchor.SCOUT_CREATOR);
    }

    /**
     * set a constraint if possible return true if it did
     *
     * @param dir  the direction of the connection
     * @param to   the widget to connect to
     * @param cDir the direction of
     * @return false if unable to apply
     */
    boolean setConstraint(int dir, ScoutWidget to, int cDir, float gap) {
        ConstraintAnchor.Type anchorType = lookupType(dir);
        if (to.isGuideline()) {
            cDir &= 0x2;
        }
        ConstraintAnchor anchor = mConstraintWidget.getAnchor(anchorType);

        if (mKeepExistingConnections) {
            if (anchor.isConnected()) {
                if (anchor.getTarget().getOwner() != to.mConstraintWidget) {
                    return false;
                }
                return true;
            }
            if (dir == Direction.BASE.getDirection()) {
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.BOTTOM).isConnected()) {
                    return false;
                }
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.TOP).isConnected()) {
                    return false;
                }
            } else if (dir == Direction.NORTH.getDirection()) {
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.BOTTOM).isConnected()) {
                    return false;
                }
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.BASELINE).isConnected()) {
                    return false;
                }
            } else if (dir == Direction.SOUTH.getDirection()) {
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.TOP).isConnected()) {
                    return false;
                }
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.BASELINE).isConnected()) {
                    return false;
                }
            } else if (dir == Direction.WEST.getDirection()) {
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.RIGHT).isConnected()) {
                    return false;
                }
            } else if (dir == Direction.EAST.getDirection()) {
                if (mConstraintWidget.getAnchor(ConstraintAnchor.Type.LEFT).isConnected()) {
                    return false;
                }

            }
        }

        if (anchor.isConnectionAllowed(to.mConstraintWidget)) {
            connect(mConstraintWidget, lookupType(dir), to.mConstraintWidget, lookupType(cDir),
                    (int) gap);
            return true;
        } else {
            return false;
        }
    }

    /**
     * set a Weak constraint if possible return true if it did
     *
     * @param dir  the direction of the connection
     * @param to   the widget to connect to
     * @param cDir the direction of
     * @return false if unable to apply
     */
    boolean setWeakConstraint(int dir, ScoutWidget to, int cDir) {
        ConstraintAnchor anchor = mConstraintWidget.getAnchor(lookupType(dir));
        float gap = 8f;

        if (mKeepExistingConnections && anchor.isConnected()) {
            if (anchor.getTarget().getOwner() != to.mConstraintWidget) {
                return false;
            }
            return true;
        }

        if (anchor.isConnectionAllowed(to.mConstraintWidget)) {
            if (DEBUG) {
                System.out.println(
                        "WEAK CONSTRAINT " + mConstraintWidget + " to " + to.mConstraintWidget);
            }
            connectWeak(mConstraintWidget, lookupType(dir), to.mConstraintWidget, lookupType(cDir),
                    (int) gap);
            return true;
        } else {
            return false;
        }
    }

    /**
     * calculates the distance between two widgets (assumed to be rectangles)
     *
     * @return the distance between two widgets at there closest point to each other
     */
    static float distance(ScoutWidget a, ScoutWidget b) {
        float ax1, ax2, ay1, ay2;
        float bx1, bx2, by1, by2;
        ax1 = a.mX;
        ax2 = a.mX + a.mWidth;
        ay1 = a.mY;
        ay2 = a.mY + a.mHeight;

        bx1 = b.mX;
        bx2 = b.mX + b.mWidth;
        by1 = b.mY;
        by2 = b.mY + b.mHeight;
        float xdiff11 = Math.abs(ax1 - bx1);
        float xdiff12 = Math.abs(ax1 - bx2);
        float xdiff21 = Math.abs(ax2 - bx1);
        float xdiff22 = Math.abs(ax2 - bx2);

        float ydiff11 = Math.abs(ay1 - by1);
        float ydiff12 = Math.abs(ay1 - by2);
        float ydiff21 = Math.abs(ay2 - by1);
        float ydiff22 = Math.abs(ay2 - by2);

        float xmin = Math.min(Math.min(xdiff11, xdiff12), Math.min(xdiff21, xdiff22));
        float ymin = Math.min(Math.min(ydiff11, ydiff12), Math.min(ydiff21, ydiff22));

        boolean yOverlap = ay1 <= by2 && by1 <= ay2;
        boolean xOverlap = ax1 <= bx2 && bx1 <= ax2;
        float xReturn = (yOverlap) ? xmin : (float) Math.hypot(xmin, ymin);
        float yReturn = (xOverlap) ? ymin : (float) Math.hypot(xmin, ymin);
        return Math.min(xReturn, yReturn);
    }

    public ScoutWidget getParent() {
        return mParent;
    }

    /**
     * Return true if the widget is a candidate to be marked
     * as resizable (ANY) -- i.e. if the current dimension is bigger than its minimum.
     *
     * @param dimension the dimension (vertical == 0, horizontal == 1) we are looking at
     * @return true if the widget is a good candidate for resize
     */
    public boolean isCandidateResizable(int dimension) {
        if (dimension == 0) {
            return mConstraintWidget.getVerticalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT
                    || ((mConstraintWidget.getVerticalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.FIXED)
                    && mConstraintWidget.getHeight() > mConstraintWidget.getMinHeight());
        } else {
            return (mConstraintWidget.getHorizontalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT)
                    || ((mConstraintWidget.getHorizontalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.FIXED)
                    && mConstraintWidget.getWidth() > mConstraintWidget.getMinWidth());
        }
    }

    public boolean isResizable(int horizontal) {
        if (horizontal == 0) {
            return mConstraintWidget.getVerticalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;
        } else {
            return mConstraintWidget.getHorizontalDimensionBehaviour()
                    == ConstraintWidget.DimensionBehaviour.MATCH_CONSTRAINT;

        }
    }

    public boolean hasBaseline() {
        return mConstraintWidget.hasBaseline();
    }

    /**
     * Gets the neighbour in that direction or root
     * TODO better support for large widgets with several neighbouring widgets
     */
    public ScoutWidget getNeighbor(Direction dir, ScoutWidget[] list) {
        ScoutWidget neigh = list[0];
        float minDist = Float.MAX_VALUE;

        switch (dir) {
            case WEST: {
                float ay1 = this.getLocation(Direction.NORTH);
                float ay2 = this.getLocation(Direction.SOUTH);
                float ax = this.getLocation(Direction.WEST);

                for (int i = 1; i < list.length; i++) {
                    ScoutWidget iw = list[i];
                    if (iw == this) {
                        continue;
                    }
                    float by1 = iw.getLocation(Direction.NORTH);
                    float by2 = iw.getLocation(Direction.SOUTH);
                    if (Math.max(ay1, by1) <= Math.min(ay2, by2)) { // overlap
                        float bx = iw.getLocation(Direction.EAST);
                        if (bx < ax && (ax - bx) < minDist) {
                            minDist = (ax - bx);
                            neigh = iw;
                        }
                    }
                }
                return neigh;
            }
            case EAST: {
                float ay1 = this.getLocation(Direction.NORTH);
                float ay2 = this.getLocation(Direction.SOUTH);
                float ax = this.getLocation(Direction.EAST);

                for (int i = 1; i < list.length; i++) {
                    ScoutWidget iw = list[i];
                    if (iw == this) {
                        continue;
                    }
                    float by1 = iw.getLocation(Direction.NORTH);
                    float by2 = iw.getLocation(Direction.SOUTH);
                    if (Math.max(ay1, by1) <= Math.min(ay2, by2)) { // overlap
                        float bx = iw.getLocation(Direction.WEST);
                        if (bx > ax && (bx - ax) < minDist) {
                            minDist = (bx - ax);
                            neigh = iw;
                        }
                    }
                }
                return neigh;
            }
            case SOUTH: {
                float ax1 = this.getLocation(Direction.WEST);
                float ax2 = this.getLocation(Direction.EAST);
                float ay = this.getLocation(Direction.SOUTH);

                for (int i = 1; i < list.length; i++) {
                    ScoutWidget iw = list[i];
                    if (iw == this) {
                        continue;
                    }
                    float bx1 = iw.getLocation(Direction.WEST);
                    float bx2 = iw.getLocation(Direction.EAST);
                    if (Math.max(ax1, bx1) <= Math.min(ax2, bx2)) { // overlap
                        float by = iw.getLocation(Direction.NORTH);
                        if (by > ay && (by - ay) < minDist) {
                            minDist = (by - ay);
                            neigh = iw;
                        }
                    }
                }
                return neigh;
            }
            case NORTH: {
                float ax1 = this.getLocation(Direction.WEST);
                float ax2 = this.getLocation(Direction.EAST);
                float ay = this.getLocation(Direction.NORTH);

                for (int i = 1; i < list.length; i++) {
                    ScoutWidget iw = list[i];
                    if (iw == this) {
                        continue;
                    }
                    float bx1 = iw.getLocation(Direction.WEST);
                    float bx2 = iw.getLocation(Direction.EAST);
                    if (Math.max(ax1, bx1) <= Math.min(ax2, bx2)) { // overlap
                        float by = iw.getLocation(Direction.SOUTH);
                        if (ay > by && (ay - by) < minDist) {
                            minDist = (ay - by);
                            neigh = iw;
                        }
                    }
                }
                return neigh;
            }
            case BASE:
            default:
                return null;
        }
    }

    /**
     * is the widet connected in that direction
     *
     * @return true if connected
     */
    public boolean isConnected(Direction direction) {
        return mConstraintWidget.getAnchor(lookupType(direction)).isConnected();
    }

    /**
     * is the distance to the Root Cached
     *
     * @return true if distance to root has been cached
     */
    private boolean isDistanceToRootCache(Direction direction) {
        int directionOrdinal = direction.getDirection();
        Float f = mDistToRootCache[directionOrdinal];
        if (f < 0) {  // depends on any comparison involving Float.NaN returns false
            return false;
        }
        return true;
    }

    /**
     * Get the cache distance to the root
     */
    private void cacheRootDistance(Direction d, float value) {
        mDistToRootCache[d.getDirection()] = value;
    }

    /**
     * get distance to the container in a direction
     * caches the distance
     *
     * @param list      list of widgets (container is list[0]
     * @param direction direction to check in
     * @return distance root or NaN if no connection available
     */
    public float connectedDistanceToRoot(ScoutWidget[] list, Direction direction) {
        float value = recursiveConnectedDistanceToRoot(list, direction);
        cacheRootDistance(direction, value);
        return value;
    }

    /**
     * Walk the widget connections to get the distance to the container in a direction
     *
     * @param list      list of widgets (container is list[0]
     * @param direction direction to check in
     * @return distance root or NaN if no connection available
     */
    private float recursiveConnectedDistanceToRoot(ScoutWidget[] list, Direction direction) {

        if (isDistanceToRootCache(direction)) {
            return mDistToRootCache[direction.getDirection()];
        }
        ConstraintAnchor.Type anchorType = lookupType(direction);
        ConstraintAnchor anchor = mConstraintWidget.getAnchor(anchorType);

        if (anchor == null || !anchor.isConnected()) {
            return Float.NaN;
        }
        float margin = anchor.getMargin();
        ConstraintAnchor toAnchor = anchor.getTarget();

        ConstraintWidget toWidget = toAnchor.getOwner();
        if (list[0].mConstraintWidget == toWidget) { // found the base return;
            return margin;
        }

        // if atached to the same side
        if (toAnchor.getType() == anchorType) {
            for (ScoutWidget scoutWidget : list) {
                if (scoutWidget.mConstraintWidget == toWidget) {
                    float dist = scoutWidget.recursiveConnectedDistanceToRoot(list, direction);
                    scoutWidget.cacheRootDistance(direction, dist);
                    return margin + dist;
                }
            }
        }
        // if atached to the other side (you will need to add the length of the widget
        if (toAnchor.getType() == lookupType(direction.getOpposite())) {
            for (ScoutWidget scoutWidget : list) {
                if (scoutWidget.mConstraintWidget == toWidget) {
                    margin += scoutWidget.getLength(direction);
                    float dist = scoutWidget.recursiveConnectedDistanceToRoot(list, direction);
                    scoutWidget.cacheRootDistance(direction, dist);
                    return margin + dist;
                }
            }
        }
        return Float.NaN;
    }

    /**
     * Get size of widget
     *
     * @param direction the direction north/south gets height east/west gets width
     * @return size of widget in that dimension
     */
    private float getLength(Direction direction) {
        switch (direction) {
            case NORTH:
            case SOUTH:
                return mHeight;
            case EAST:
            case WEST:
                return mWidth;
            default:
                return 0;
        }
    }

    /**
     * is the widget centered
     *
     * @param orientationVertical 1 = checking if vertical
     * @return true if centered
     */
    public boolean isCentered(int orientationVertical) {
        if (isGuideline()) return false;
        if (orientationVertical == Direction.ORIENTATION_VERTICAL) {
            return mConstraintWidget.getAnchor(ConstraintAnchor.Type.TOP).isConnected()
                    && mConstraintWidget.getAnchor(ConstraintAnchor.Type.BOTTOM).isConnected();
        }
        return mConstraintWidget.getAnchor(ConstraintAnchor.Type.LEFT).isConnected()
                && mConstraintWidget.getAnchor(ConstraintAnchor.Type.RIGHT).isConnected();
    }

    public boolean hasConnection(Direction dir) {
        ConstraintAnchor anchor = mConstraintWidget.getAnchor(lookupType(dir));
        return (anchor != null && anchor.isConnected());
    }

    public Rectangle getRectangle() {
        if (mRectangle == null) {
            mRectangle = new Rectangle();
        }
        mRectangle.x = mConstraintWidget.getX();
        mRectangle.y = mConstraintWidget.getY();
        mRectangle.width = mConstraintWidget.getWidth();
        mRectangle.height = mConstraintWidget.getHeight();
        return mRectangle;
    }

    static ScoutWidget[] getWidgetArray(WidgetContainer base) {
        ArrayList<ConstraintWidget> list = new ArrayList<>(base.getChildren());
        list.add(0, base);
        return create(list.toArray(new ConstraintWidget[list.size()]));
    }

    /**
     * Calculate the gap in to the nearest widget
     *
     * @param direction the direction to check
     * @param list      list of other widgets (root == list[0])
     * @return the distance on that side
     */
    public int gap(Direction direction, ScoutWidget[] list) {
        int rootWidth = list[0].mConstraintWidget.getWidth();
        int rootHeight = list[0].mConstraintWidget.getHeight();
        Rectangle rect = new Rectangle();

        switch (direction) {
            case NORTH: {
                rect.y = 0;
                rect.x = mConstraintWidget.getX() + 1;
                rect.width = mConstraintWidget.getWidth() - 2;
                rect.height = mConstraintWidget.getY();
            }
            break;
            case SOUTH: {
                rect.y = mConstraintWidget.getY() + mConstraintWidget.getHeight();
                rect.x = mConstraintWidget.getX() + 1;
                rect.width = mConstraintWidget.getWidth() - 2;
                rect.height = rootHeight - rect.y;
            }
            break;
            case WEST: {
                rect.y = mConstraintWidget.getY() + 1;
                rect.x = 0;
                rect.width = mConstraintWidget.getX();
                rect.height = mConstraintWidget.getHeight() - 2;

            }
            break;
            case EAST: {
                rect.y = mConstraintWidget.getY() + 1;
                rect.x = mConstraintWidget.getX() + mConstraintWidget.getWidth();
                rect.width = rootWidth - rect.x;
                rect.height = mConstraintWidget.getHeight() - 2;
            }
            break;

        }
        int min = Integer.MAX_VALUE;
        for (int i = 1; i < list.length; i++) {
            ScoutWidget scoutWidget = list[i];
            if (scoutWidget == this) {
                continue;
            }
            Rectangle r = scoutWidget.getRectangle();
            if (r.intersects(rect)) {
                int dist = (int) distance(scoutWidget, this);
                if (min > dist) {
                    min = dist;
                }
            }
        }

        if (min > Math.max(rootHeight, rootWidth)) {
            switch (direction) {
                case NORTH:
                    return mConstraintWidget.getY();
                case SOUTH:
                    return rootHeight - (mConstraintWidget.getY() + mConstraintWidget.getHeight());

                case WEST:
                    return mConstraintWidget.getX();

                case EAST:
                    return rootWidth - (mConstraintWidget.getX() + mConstraintWidget.getWidth());
            }
        }
        return min;
    }

    public void setX(int x) {
        mConstraintWidget.setX(x);
        mX = mConstraintWidget.getX();
    }

    public void setY(int y) {
        mConstraintWidget.setY(y);
        mY = mConstraintWidget.getY();
    }

    public void setWidth(int width) {
        mConstraintWidget.setWidth(width);
        mWidth = mConstraintWidget.getWidth();
    }

    public void setHeight(int height) {
        mConstraintWidget.setHeight(height);
        mHeight = mConstraintWidget.getHeight();
    }

    /**
     * Comparator to sort widgets by y
     */
    static Comparator<ScoutWidget> sSortY = new Comparator<ScoutWidget>() {
        @Override
        public int compare(ScoutWidget o1, ScoutWidget o2) {
            return o1.mConstraintWidget.getY() - o2.mConstraintWidget.getY();
        }
    };

    public int rootDistanceY() {
        if (mConstraintWidget == null || mConstraintWidget.getParent() == null) {
            return 0;
        }
        int rootHeight = mConstraintWidget.getParent().getHeight();
        int aY = mConstraintWidget.getY();
        int aHeight = mConstraintWidget.getHeight();
        return Math.min(aY, rootHeight - (aY + aHeight));
    }
}
