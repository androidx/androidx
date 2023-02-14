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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Inference Probability tables
 * There are two major entry points in this class
 * computeConstraints - which build the Inference tables
 * applyConstraints - applies the constraints to the widgets
 */
public class ScoutProbabilities {

    private static final boolean DEBUG = false;
    private static final float BASELINE_ERROR = 4.0f;
    private static final int RESULT_PROBABILITY = 0;
    private static final int RESULT_MARGIN = 1;
    private static final boolean SUPPORT_CENTER_TO_NON_ROOT = true;
    private static final boolean SUPPORT_WEAK_TO_CENTER = true;
    private static final int NEGATIVE_GAP_FLAG = -3;
    private static final int CONSTRAINT_FAILED_FLAG = -2;
    private static final float CENTER_ERROR = 2;
    private static final float SLOPE_CENTER_CONNECTION = 20;
    private static final int MAX_DIST_FOR_CENTER_OVERLAP = 40;
    private static final int ROOT_MARGIN_DISCOUNT = 16;
    private static final int MAX_ROOT_OVERHANG = 10;
    private static final boolean SKIP_SPARSE_COLUMNS = true;

    float[][][] mProbability; // probability of a connection
    float[][][] mMargin; // margin needed for that connection
    float[][][][] mBinaryBias; // Ratio needed for binary connections (should be .5 for now)
    float[][][][] mBinaryProbability; // probability of a left_right/up_down
    int mLen;

    /**
     * This calculates a constraint tables
     *
     * @param list ordered list of widgets root must be list[0]
     */
    public void computeConstraints(ScoutWidget[] list) {
        if (list.length < 2) {
            throw new IllegalArgumentException("list must contain more than 1 widget");
        }
        if (!list[0].isRoot()) {
            throw new IllegalArgumentException("list[0] must be root");
        }

        mLen = list.length;

        mProbability = new float[mLen][][];
        mMargin = new float[mLen][][];

        // calculate probability for normal connections
        float[] result = new float[2]; // estimation function return 2 values probability & margin

        for (int i = 1; i < mLen; i++) { // for all non root widgets
            Direction[] all = Direction.getAllDirections();
            if (list[i].isGuideline()) {
                continue;
            }
            mProbability[i] = new float[all.length][];
            mMargin[i] = new float[all.length][];
            for (int dir = 0; dir < all.length; dir++) { // for all possible connections
                Direction direction = Direction.get(dir);
                int connectTypes = direction.connectTypes();

                // create the multidimensional array on the fly
                // to account for the variying size of the probability space
                mProbability[i][dir] = new float[mLen * connectTypes];
                mMargin[i][dir] = new float[mLen * connectTypes];

                // fill in all candidate connections
                for (int candidate = 0; candidate < mMargin[i][dir].length; candidate++) {
                    int widgetNumber = candidate / connectTypes;
                    int opposite = candidate % connectTypes;
                    Direction connectTo = (opposite == 0) ? direction : direction.getOpposite();

                    estimateProbability(list[i], direction, list[widgetNumber],
                            connectTo, list, result);
                    mProbability[i][dir][candidate] = result[RESULT_PROBABILITY];
                    mMargin[i][dir][candidate] = result[RESULT_MARGIN];
                }
            }
        }

        // calculate probability for "centered" connections
        mBinaryProbability = new float[mLen][2][mLen * 2][mLen * 2];
        mBinaryBias = new float[mLen][2][mLen * 2][mLen * 2];
        Direction[][] directions =
                {{Direction.NORTH, Direction.SOUTH}, {Direction.WEST, Direction.EAST}};
        for (int i = 1; i < mLen; i++) {
            for (int horizontal = 0; horizontal < 2; horizontal++) { // vert=0 or horizantal=1
                Direction[] sides = directions[horizontal];
                for (int candidate1 = 0; candidate1 < mLen * 2; candidate1++) {
                    for (int candidate2 = 0; candidate2 < mLen * 2; candidate2++) {

                        // candidates are 2 per widget (left/right or above/below)
                        int widget1Number = candidate1 / 2;
                        int widget2Number = candidate2 / 2;

                        // pick the sides to connect
                        Direction widget1Side = sides[candidate1 & 0x1];
                        Direction widget2Side = sides[candidate2 & 0x1];

                        estimateBinaryProbability(list[i], horizontal,
                                list[widget1Number], widget1Side,
                                list[widget2Number], widget2Side,
                                list, result);
                        mBinaryProbability[i][horizontal][candidate1][candidate2] =
                                result[RESULT_PROBABILITY];
                        mBinaryBias[i][horizontal][candidate1][candidate2] =
                                result[RESULT_MARGIN];
                    }
                }
            }
        }
        if (DEBUG) {
            printTable(list);
        }
    }

    /**
     * This applies a constraint set suggested by the Inference tables
     */
    public void applyConstraints(ScoutWidget[] list) {

        // this provides the sequence of connections
        pickColumnWidgets(list);
        pickCenterOverlap(list);
        pickBaseLineConnections(list); // baseline first
        pickCenteredConnections(list, true); // centered connections that stretch
        pickMarginConnections(list, 10); // regular margin connections that are close
        pickCenteredConnections(list, false); // general centered connections
        pickMarginConnections(list, 100);  // all remaining margins
        //pickWeakConstraints(list); // weak constraints for ensuring wrap content

        if (DEBUG) {
            printBaseTable(list);
        }
    }

    /**
     * Find and connect widgets centered over other widgets
     */
    private void pickCenterOverlap(ScoutWidget[] list) {
        // find any widget centered over the edge of another
        for (int i = 0; i < list.length; i++) {
            ScoutWidget scoutWidget = list[i];
            float centerX = scoutWidget.getX() + scoutWidget.getWidth() / 2;
            float centerY = scoutWidget.getY() + scoutWidget.getHeight() / 2;
            for (int j = 0; j < list.length; j++) {
                if (i == j) continue;
                ScoutWidget widget = list[j];
                if (scoutWidget.isGuideline()) {
                    continue;
                }
                if (!widget.isGuideline()
                        && ScoutWidget.distance(scoutWidget, widget)
                        > MAX_DIST_FOR_CENTER_OVERLAP) {
                    continue;
                }
                if (!widget.isGuideline() || widget.isVerticalGuideline()) {
                    if (Math.abs(widget.getX() - centerX) < CENTER_ERROR) {
                        scoutWidget.setEdgeCentered(1, widget, Direction.WEST);
                    }
                    if (Math.abs(widget.getX() + widget.getWidth() - centerX) < CENTER_ERROR) {
                        scoutWidget.setEdgeCentered(1, widget, Direction.EAST);
                    }
                }
                if (!widget.isGuideline() || widget.isHorizontalGuideline()) {
                    if (Math.abs(widget.getY() - centerY) < CENTER_ERROR) {
                        scoutWidget.setEdgeCentered(0, widget, Direction.NORTH);
                    }

                    if (Math.abs(widget.getY() + widget.getHeight() - centerY) < CENTER_ERROR) {
                        scoutWidget.setEdgeCentered(0, widget, Direction.SOUTH);
                    }
                }
            }
        }
    }

    /**
     * force structure for column cases
     */
    private void pickColumnWidgets(ScoutWidget[] list) {
        ScoutWidget[] w = new ScoutWidget[list.length - 1];
        for (int i = 0; i < list.length - 1; i++) {
            w[i] = list[i + 1];
        }
        Arrays.sort(w, new Comparator<ScoutWidget>() {
            @Override
            public int compare(ScoutWidget w1, ScoutWidget w2) {
                int n = Integer.compare(w1.mConstraintWidget.getX(), w2.mConstraintWidget.getX());
                if (n == 0) {
                    n = Integer.compare(w1.mConstraintWidget.getWidth(),
                            w2.mConstraintWidget.getWidth());
                }
                return n;
            }
        });

        ArrayList<ArrayList<ScoutWidget>> groups = new ArrayList<ArrayList<ScoutWidget>>();
        ArrayList<ScoutWidget> current = new ArrayList<ScoutWidget>();
        for (int i = 2; i < w.length; i++) {
            ScoutWidget scoutWidget = w[i];
            if (sameCol(w[i], w[i - 1])) {
                if (current.isEmpty()) {
                    groups.add(current);
                    current.add(w[i - 1]);
                    current.add(w[i]);
                } else {
                    if (sameCol(current.get(0), w[i])) {
                        current.add(w[i]);
                    } else {
                        current = new ArrayList<ScoutWidget>();
                        groups.add(current);
                        current.add(w[i - 1]);
                        current.add(w[i]);
                    }
                }
            }
        }
        int[] dualIndex = new int[2];

        for (ArrayList<ScoutWidget> group : groups) {
            if (SKIP_SPARSE_COLUMNS) {
                // skip columns that have lot of space to reject accidental columns.
                Rectangle union = null;
                int area = 0;
                for (ScoutWidget scoutWidget : group) {
                    Rectangle r = scoutWidget.getRectangle();
                    area += r.width * r.height;
                    if (union == null) {
                        union = r;
                    } else {
                        union = union.union(r);
                    }
                }
                int unionArea = union.width * union.height;
                if (unionArea > 2 * area) { // more than have the area is empty
                    continue;
                }
            }

            ScoutWidget[] widgets =
                    (ScoutWidget[]) group.toArray(new ScoutWidget[group.size()]);
            Arrays.sort(widgets, ScoutWidget.sSortY);
            boolean reverse = widgets[0].rootDistanceY()
                    > widgets[widgets.length - 1].rootDistanceY();
            float[] max = new float[widgets.length];
            int[] map = new int[widgets.length];

            for (int i = 0; i < widgets.length; i++) {
                for (int j = 1; j < list.length; j++) {
                    if (widgets[i] == list[j]) {
                        map[i] = j;
                    }
                }
            }
            // zero out probabilities of connecting to each other
            // we are going to take care of it here
            for (int i = 0; i < widgets.length; i++) {
                for (int j = 0; j < widgets.length; j++) {
                    int l = map[j] * 2;
                    for (int k = 2; k < 2 * list.length; k++) {
                        mBinaryProbability[map[i]][1][l][k] = -1;
                        mBinaryProbability[map[i]][1][k][l] = -1;
                        mBinaryProbability[map[i]][1][l + 1][k] = -1;
                        mBinaryProbability[map[i]][1][k][l + 1] = -1;
                    }
                }
            }

            int bestToConnect = -1;
            float maxVal = -1;
            for (int i = 0; i < widgets.length; i++) {
                max[i] = Utils.max(mBinaryProbability[map[i]][1], dualIndex);
                if (maxVal < max[i]) {
                    bestToConnect = i;
                    maxVal = max[i];
                }
            }

            if (reverse) {
                for (int i = 1; i < widgets.length; i++) {
                    int gap = widgets[i].mConstraintWidget.getY();
                    gap -= widgets[i - 1].mConstraintWidget.getY();
                    gap -= widgets[i - 1].mConstraintWidget.getHeight();
                    widgets[i - 1].setConstraint(Direction.SOUTH.getDirection(),
                            widgets[i], Direction.NORTH.getDirection(), gap);
                }
            } else {
                for (int i = 1; i < widgets.length; i++) {
                    int gap = widgets[i].mConstraintWidget.getY();
                    gap -= widgets[i - 1].mConstraintWidget.getY();
                    gap -= widgets[i - 1].mConstraintWidget.getHeight();
                    widgets[i].setConstraint(Direction.NORTH.getDirection(),
                            widgets[i - 1], Direction.SOUTH.getDirection(), gap);
                }
            }

            if (bestToConnect >= 0) {
                Utils.max(mBinaryProbability[map[bestToConnect]][1], dualIndex);
                ScoutWidget w1 = list[dualIndex[0] / 2];
                ScoutWidget w2 = list[dualIndex[1] / 2];
                Direction dir1 = ((dualIndex[0] & 0x1) == 0) ? Direction.WEST : Direction.EAST;
                Direction dir2 = ((dualIndex[1] & 0x1) == 0) ? Direction.WEST : Direction.EAST;
                widgets[bestToConnect].setCentered(0, w1, w2, dir1, dir2, 0);

                for (int i = bestToConnect + 1; i < widgets.length; i++) {
                    widgets[i].setCentered(1, widgets[i - 1], widgets[i - 1],
                            Direction.WEST,
                            Direction.EAST, 0);
                }
                for (int i = 1; i <= bestToConnect; i++) {
                    widgets[i - 1].setCentered(1, widgets[i], widgets[i],
                            Direction.WEST,
                            Direction.EAST, 0);
                }
            } else {
                if (reverse) {
                    for (int i = 1; i < widgets.length; i++) {
                        widgets[i - 1].setCentered(0, widgets[i], widgets[i],
                                Direction.WEST,
                                Direction.EAST, 0);
                    }
                } else {
                    for (int i = 1; i < widgets.length; i++) {
                        widgets[i].setCentered(0, widgets[i - 1], widgets[i - 1],
                                Direction.WEST,
                                Direction.EAST, 0);
                    }
                }
            }
        }
    }

    private static boolean sameCol(ScoutWidget a, ScoutWidget b) {
        return a.mConstraintWidget.getX() == b.mConstraintWidget.getX()
                && a.mConstraintWidget.getWidth() == b.mConstraintWidget.getWidth();
    }

    /**
     * This searches for baseline connections with a very narrow tolerance
     */
    private void pickBaseLineConnections(ScoutWidget[] list) {
        final int baseline = Direction.BASE.getDirection();
        final int north = Direction.NORTH.getDirection();
        final int south = Direction.SOUTH.getDirection();
        final int east = Direction.EAST.getDirection();
        final int west = Direction.WEST.getDirection();

        // Search for baseline connections
        for (int i = 1; i < mLen; i++) {
            float[][] widgetProbability = mProbability[i];

            if (widgetProbability == null || widgetProbability[baseline] == null) {
                continue;
            }
            float maxValue = 0.0f;

            float maxNorth = widgetProbability[north][Utils.max(widgetProbability[north])];
            float maxSouth = widgetProbability[south][Utils.max(widgetProbability[south])];

            int maxIndex = Utils.max(widgetProbability[baseline]);
            float maxBaseline = widgetProbability[baseline][maxIndex];
            if (maxBaseline < maxNorth || maxBaseline < maxSouth) {
                continue;
            }

            String s;
            if (DEBUG) {
                System.out.println(" b check " + list[i] + " " + widgetProbability[4][maxIndex]);
                s = list[i] + "(" + Direction.toString(baseline) + ") -> " + list[maxIndex] + " "
                        + Direction.toString(baseline);
                System.out.println("try " + s);
            }

            if (list[i].setConstraint(baseline, list[maxIndex], baseline, 0)) {
                Utils.zero(mBinaryProbability[i][Direction.ORIENTATION_VERTICAL]);
                Arrays.fill(widgetProbability[baseline], 0.0f);
                widgetProbability[north] = null;
                Arrays.fill(widgetProbability[south], 0.0f);

                if (DEBUG) {
                    System.out.println("connect " + s);
                }
            }
        }
    }

    /**
     * This searches for centered connections
     *
     * @param list            widgets (0 is root)
     * @param checkResizeable if true will attempt to make a stretchable widget
     */
    private void pickCenteredConnections(ScoutWidget[] list, boolean checkResizeable) {
        Direction[][] side =
                {{Direction.NORTH, Direction.SOUTH}, {Direction.WEST, Direction.EAST}};
        int[] dualIndex = new int[2];
        for (int i = 1; i < mLen; i++) {
            float[][][] widgetBinaryProbability = mBinaryProbability[i];
            float[][][] widgetBinaryBias = mBinaryBias[i];

            for (int horizontal = 0; horizontal < widgetBinaryProbability.length;
                    horizontal++) { // vert=0 or horizontals=1
                float[][] pmatrix = widgetBinaryProbability[horizontal];
                float[][] bias = widgetBinaryBias[horizontal];
                if (pmatrix == null) {
                    continue;
                }
                boolean worked = false;
                while (!worked) {
                    Utils.max(pmatrix, dualIndex);
                    int max1 = dualIndex[0];
                    int max2 = dualIndex[1];
                    int wNo1 = max1 / 2;
                    int wNo2 = max2 / 2;
                    Direction widget1Side = side[horizontal][max1 & 0x1];
                    Direction widget2Side = side[horizontal][max2 & 0x1];

                    // pick the sides to connect
                    float centerProbability = pmatrix[max1][max2];
                    worked = true;
                    if (centerProbability > .9) {
                        if (checkResizeable && !list[i].isCandidateResizable(horizontal)) {
                            continue;
                        }

                        worked = list[i].setCentered(horizontal * 2, list[wNo1], list[wNo2],
                                widget1Side,
                                widget2Side,
                                bias[max1][max2]);
                        if (worked) {
                            mProbability[i][horizontal * 2] = null;
                            mProbability[i][horizontal * 2 + 1] = null;
                        } else {
                            pmatrix[max1][max2] = 0;
                        }
                    }
                }
            }
        }
    }

    /**
     * This searches for Normal margin connections
     *
     * @param list             list of scouts
     * @param maxMarginPercent only margins less than that percent will be connected
     */
    private void pickMarginConnections(ScoutWidget[] list, int maxMarginPercent) {
        final int baseline = Direction.BASE.getDirection();
        final int north = Direction.NORTH.getDirection();
        final int south = Direction.SOUTH.getDirection();
        final int east = Direction.EAST.getDirection();
        int width = list[0].mConstraintWidget.getWidth();
        int height = list[0].mConstraintWidget.getWidth();
        int maxWidthMargin = (width * maxMarginPercent) / 100;
        int maxHeightMargin = (height * maxMarginPercent) / 100;
        int[] maxMargin = {maxHeightMargin, maxWidthMargin};
        final int west = Direction.WEST.getDirection();
        // pick generic connections
        int[][] dirTypes = {{north, south}, {west, east}};
        for (int i = mLen - 1; i > 0; i--) {
            float[][] widgetProbability = mProbability[i];

            for (int horizontal = 0; horizontal < 2; horizontal++) {
                int[] dirs = dirTypes[horizontal];
                boolean found = false;
                while (!found) {
                    found = true;
                    int setlen = dirs.length;
                    if (DEBUG) {
                        System.out.println(" check " + list[i] + " " + horizontal);
                    }
                    int dir = dirs[0];
                    if (widgetProbability == null || widgetProbability[dir] == null) {
                        continue;
                    }
                    int maxIndex = 0;
                    int maxDirection = 0;
                    float maxValue = 0.0f;
                    int rowType = 0;
                    for (int j = 0; j < setlen; j++) {
                        int rowMaxIndex = Utils.max(widgetProbability[dirs[j]]);
                        if (maxValue < widgetProbability[dirs[j]][rowMaxIndex]) {
                            maxDirection = dirs[j];
                            maxIndex = rowMaxIndex;
                            maxValue = widgetProbability[dirs[j]][rowMaxIndex];
                        }
                    }
                    if (widgetProbability[maxDirection] == null) {
                        System.out.println(list[i] + " " + maxDirection);
                        continue;
                    }
                    int m, cDir;
                    if (maxDirection == baseline) { // baseline connection
                        m = maxIndex;
                        cDir = baseline; // always baseline
                    } else {
                        m = maxIndex / 2;
                        cDir = maxDirection;
                        if (maxIndex % 2 == 1) {
                            cDir = cDir ^ 1;
                        }
                    }
                    if (mMargin[i][maxDirection][maxIndex] > maxMargin[horizontal]) {
                        continue;
                    }
                    String s =
                            list[i] + "(" + Direction.toString(maxDirection) + ") -> " + list[m]
                                    + " "
                                    + Direction.toString(cDir);
                    if (DEBUG) {
                        System.out.println("try " + s);
                    }
                    if (!list[i]
                            .setConstraint(maxDirection, list[m], cDir,
                                    mMargin[i][maxDirection][maxIndex])) {
                        if (widgetProbability[maxDirection][maxIndex] >= 0) {
                            widgetProbability[maxDirection][maxIndex] = CONSTRAINT_FAILED_FLAG;
                            found = false;
                        }
                    } else {
                        mBinaryProbability[i][horizontal] = null;
                        if (DEBUG) {
                            System.out.println("connect " + s);
                        }
                    }
                }
            }
        }

    }

    /**
     * pick weak constraints
     */
    private void pickWeakConstraints(ScoutWidget[] list) {
        Direction[] directions =
                {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        ScoutWidget[][] candidates = new ScoutWidget[directions.length][]; // no arrays of generics
        ScoutWidget[] maxCandidate = new ScoutWidget[directions.length];
        ScoutWidget centeredVertical = null;
        ScoutWidget centeredHorizontal = null;
        float[] maxDist = {-1, -1, -1, -1};

        // find the biggest widget centered
        for (int i = 1; i < list.length; i++) {
            ScoutWidget widget = list[i];
            if (widget.isCentered(Direction.ORIENTATION_VERTICAL)) {
                if (centeredVertical == null || centeredVertical.getHeight() > widget.getHeight()) {
                    centeredVertical = widget;
                }
            }
            if (widget.isCentered(Direction.ORIENTATION_HORIZONTAL)) {
                if (centeredHorizontal == null
                        || centeredHorizontal.getWidth() > widget.getWidth()) {
                    centeredHorizontal = widget;
                }
            }
        }
        ScoutWidget[] centeredMax =
                {centeredVertical, centeredVertical, centeredHorizontal, centeredHorizontal};
        // build table of widgets open from each direction
        for (int j = 0; j < directions.length; j++) {
            Direction direction = directions[j];
            ArrayList<ScoutWidget> tmp = new ArrayList<ScoutWidget>();
            for (int i = 1; i < list.length; i++) {
                ScoutWidget widget = list[i];
                if (widget.isGuideline()) {
                    continue;
                }
                if (!widget.isConnected(directions[j].getOpposite())) {
                    float dist = widget.connectedDistanceToRoot(list, direction);
                    if (!Float.isNaN(dist)) {
                        if (dist > maxDist[j]) {
                            maxDist[j] = dist;
                            maxCandidate[j] = widget;
                        }
                        tmp.add(widget);
                    }
                }
            }
            candidates[j] = tmp.toArray(new ScoutWidget[tmp.size()]);
            if (DEBUG) {
                String s = "[" + direction + "]";
                s += "max=" + maxCandidate[j] + " ";
                for (int i = 0; i < candidates[j].length; i++) {
                    ScoutWidget c = candidates[j][i];
                    s += " " + c + " " + c.connectedDistanceToRoot(list, direction);
                }
                System.out.println(s);
            }
        }

        // when there is nothing on the other side add a constraint to the longest
        // optionally attaching to the center if it fits
        for (int j = 0; j < directions.length; j++) {
            if ((candidates[j].length > 0) && (candidates[j ^ 1].length == 0)) {

                int dirInt = directions[j].getOpposite().getDirection();
                int rootDirInt = directions[j].getOpposite().getDirection();
                ScoutWidget connect = list[0];
                int connectSide = rootDirInt;

                if (SUPPORT_WEAK_TO_CENTER) {
                    if (centeredMax[j] != null) {
                        float centerPos = centeredMax[j].getLocation(directions[j]);
                        float maxPos = maxCandidate[j].getLocation(directions[j].getOpposite());
                        float delta = centerPos - maxPos;
                        if (directions[j] == Direction.EAST || directions[j] == Direction.SOUTH) {
                            delta = -delta;
                        }
                        if (delta > 0) {
                            connectSide = directions[j].getDirection();
                            connect = centeredMax[j];
                        }
                    }
                }
                maxCandidate[j].setWeakConstraint(dirInt, connect, connectSide);
                candidates[j] = new ScoutWidget[0]; // prevent next step from using
            }
        }

        // Where there is no overlap
        for (int j = 0; j < directions.length; j += 2) {
            if ((candidates[j].length > 0) && (candidates[j + 1].length > 0)) {
                Direction side = directions[j].getOpposite();
                Direction otherSide = directions[j];
                if (maxCandidate[j].getLocation(side)
                        < maxCandidate[j + 1].getLocation(otherSide)) {
                    maxCandidate[j].setWeakConstraint(side.getDirection(), maxCandidate[j + 1],
                            otherSide.getDirection());
                    candidates[j] = new ScoutWidget[0]; // prevent next step from using
                    maxCandidate[j] = null;
                }
            }
        }

        //  pick the closest clean shot to the other side if it is a maximum
        for (int j = 0; j < directions.length; j += 2) {
            if ((candidates[j].length > 0) && (candidates[j + 1].length > 0)) {
                Direction side = directions[j].getOpposite();
                Direction otherSide = directions[j];

                boolean clearToRoot1 = maxCandidate[j].getNeighbor(otherSide, list).isRoot();
                boolean clearToRoot2 = maxCandidate[j].getNeighbor(side, list).isRoot();
                if (clearToRoot1 && clearToRoot2) {
                    if (maxDist[j] > maxDist[j + 1]) {
                        maxCandidate[j].setWeakConstraint(side.getDirection(), list[0],
                                side.getDirection());
                    } else {
                        maxCandidate[j + 1].setWeakConstraint(otherSide.getDirection(), list[0],
                                otherSide.getDirection());
                    }
                }
            }
        }
    }

    /*-----------------------------------------------------------------------*/
    // core probability estimators
    /*-----------------------------------------------------------------------*/

    /**
     * This defines the "probability" of a constraint between two widgets.
     *
     * @param from    source widget
     * @param fromDir direction on that widget
     * @param to      destination widget
     * @param toDir   destination side to connect
     * @param result  populates results with probability and offset
     */
    private static void estimateProbability(ScoutWidget from, Direction fromDir,
            ScoutWidget to, Direction toDir,
            ScoutWidget[] list,
            float[] result) {
        result[RESULT_PROBABILITY] = 0;
        result[RESULT_MARGIN] = 0;

        if (from == to) { // 0 probability of connecting to yourself
            return;
        }
        if (from.isGuideline()) {
            return;
        }

        if (to.isGuideline()) {
            if ((toDir == Direction.NORTH || toDir == Direction.SOUTH)
                    && to.isVerticalGuideline()) {
                return;
            }
            if ((toDir == Direction.EAST || toDir == Direction.WEST)
                    && to.isHorizontalGuideline()) {
                return;
            }
        }

        // if it already has a baseline do not connect to it
        if ((toDir == Direction.NORTH || toDir == Direction.SOUTH) & from.hasBaseline()) {
            if (from.hasConnection(Direction.BASE)) {
                return;
            }
        }

        if (fromDir == Direction.BASE) { // if baseline 0  probability of connecting to non baseline
            if (!from.hasBaseline() || !to.hasBaseline()) { // no base line
                return;
            }
        }

        float fromLocation = from.getLocation(fromDir);
        float toLocation = to.getLocation(toDir);
        float positionDiff =
                (fromDir.reverse()) ? fromLocation - toLocation : toLocation - fromLocation;
        float distance = 2 * ScoutWidget.distance(from, to);
        if (to.isRoot()) {
            distance = Math.abs(distance - ROOT_MARGIN_DISCOUNT);
        }
        // probability decreases with distance and margin distance
        float probability = 1 / (1 + distance * distance + positionDiff * positionDiff);
        if (fromDir == Direction.BASE) { // prefer baseline
            if (Math.abs(positionDiff) > BASELINE_ERROR) {
                return;
            }
            probability *= 2;
        }
        if (to.isRoot()) {
            probability *= 2;
        }
        result[RESULT_PROBABILITY] = (positionDiff >= 0) ? probability : NEGATIVE_GAP_FLAG;
        result[RESULT_MARGIN] = positionDiff;
    }

    /**
     * This defines the constraint between a widget and two widgets to the left and right of it.
     * Currently only encourages probability between widget and root for center purposes.
     *
     * @param from        source widget
     * @param orientation horizontal or vertical connections (1 is horizontal)
     * @param to1         connect to on one side
     * @param toDir1      direction on that widget
     * @param to2         connect to on other side
     * @param toDir2      direction on that widget
     * @param result      populates results with probability and offset
     */
    private static void estimateBinaryProbability(
            ScoutWidget from, int orientation, // 0 = north/south 1 = east/west
            ScoutWidget to1, Direction toDir1,
            ScoutWidget to2, Direction toDir2,
            ScoutWidget[] list,
            float[] result) {

        result[RESULT_PROBABILITY] = 0;
        result[RESULT_MARGIN] = 0;
        if (from == to1 || from == to2) { // cannot center on yourself
            return;
        }
        if (from.isGuideline()) {
            return;
        }
        // if it already has a baseline do not connect to it
        if ((orientation == Direction.ORIENTATION_VERTICAL) & from.hasBaseline()) {
            if (from.hasConnection(Direction.BASE)) {
                return;
            }
        }
        // distance normalizing scale factor
        float scale = 0.5f
                * ((orientation == Direction.ORIENTATION_VERTICAL) ? from.getParent().getHeight() :
                from.getParent().getWidth());
        Direction fromLeft = Direction.getDirections(orientation)[0];
        Direction fromRight = Direction.getDirections(orientation)[1];

        float location1 = from.getLocation(fromLeft);
        float location2 = from.getLocation(fromRight);
        float toLoc1 = to1.getLocation(toDir1);
        float toLoc2 = to2.getLocation(toDir2);
        float positionDiff1 = location1 - toLoc1;
        float positionDiff2 = toLoc2 - location2;

        if (positionDiff1 < 0 || positionDiff2 < 0) { // do not center if not aligned
            boolean badCandidate = true;
            if (positionDiff2 < 0 && to2.isRoot() && positionDiff2 > -MAX_ROOT_OVERHANG) {
                badCandidate = false;
                positionDiff2 = 0;
            }
            if (positionDiff1 < 0 && to1.isRoot() && positionDiff2 > -MAX_ROOT_OVERHANG) {
                badCandidate = false;
                positionDiff2 = 0;
            }
            if (badCandidate) {
                result[RESULT_PROBABILITY] = NEGATIVE_GAP_FLAG;
                return;
            }
        }

        float distance1 = ScoutWidget.distance(from, to1) / scale;
        float distance2 = ScoutWidget.distance(from, to2) / scale;
        float diff = Math.abs(positionDiff1 - positionDiff2);
        float probability = ((diff < SLOPE_CENTER_CONNECTION) ? 1 : 0); // favor close distance
        probability = probability / (1 + distance1 + distance2);
        probability += 1 / (1 + Math.abs(positionDiff1 - positionDiff2));
        probability *=
                (to1.isRoot() && to2.isRoot()) ? 2 : ((SUPPORT_CENTER_TO_NON_ROOT) ? 1f : 0);

        result[RESULT_PROBABILITY] = probability;
        result[RESULT_MARGIN] = Math.min(positionDiff1, positionDiff2);
    }

    /*-----------------------------------------------------------------------*/
    // Printing fuctions (for use in debugging)
    /*-----------------------------------------------------------------------*/

    /**
     * Print the Tables
     */
    public void printTable(ScoutWidget[] list) {
        printCenterTable(list);
        printBaseTable(list);
    }

    /**
     * Print the tables involved int centering the widgets
     */
    public void printCenterTable(ScoutWidget[] list) {
        // PRINT DEBUG
        System.out.println("----------------- BASE TABLE --------------------");
        final int size = 10;
        String padd = new String(new char[size]).replace('\0', ' ');

        System.out.print("  ");
        for (int i = 0; i < mLen; i++) {
            String dbg = "[" + i + "] " + list[i] + "-------------------------";
            dbg = dbg.substring(0, 20);
            System.out.print(dbg + ((i == mLen - 1) ? "\n" : ""));
        }

        String str = "[";
        for (int con = 0; con < mLen * 2; con++) {
            int opposite = con & 0x1;
            str += (con / 2 + ((opposite == 0) ? "->" : "<-") + "           ").substring(0, 10);
        }

        System.out.println("  " + str);

        for (int i = 1; i < mLen; i++) {
            for (int dir = 0; dir < mBinaryProbability[i].length;
                    dir++) { // above, below, left, right
                String tab = "";
                for (int k = 0; k < mBinaryProbability[i][dir].length; k++) {
                    tab += Utils.toS(mBinaryProbability[i][dir][k]) + "\n  ";
                }
                System.out.println(Direction.toString(dir) + " " + tab);
            }
        }
    }

    /**
     * Prints the tables involved in the normal widget asociations.
     */
    public void printBaseTable(ScoutWidget[] list) {
        // PRINT DEBUG
        System.out.println("----------------- CENTER TABLE --------------------");

        final int size = 10;
        String padd = new String(new char[size]).replace('\0', ' ');

        System.out.print(" ");
        for (int i = 0; i < mLen; i++) {
            String dbg = "[" + i + "] " + list[i] + "-------------------------";
            if (i == 0) {
                dbg = padd + dbg.substring(0, 20);
            } else {
                dbg = dbg.substring(0, 20);
            }
            System.out.print(dbg + ((i == mLen - 1) ? "\n" : ""));
        }

        String str = "[";
        for (int con = 0; con < mLen * 2; con++) {
            int opposite = con & 0x1;
            str += (con / 2 + ((opposite == 0) ? "->" : "<-") + "           ").substring(0, 10);
        }

        String header = ("Connection " + padd).substring(0, size);

        System.out.println(header + " " + str);

        for (int i = 1; i < mLen; i++) {
            if (mProbability[i] == null) {
                continue;
            }
            for (int dir = 0; dir < mProbability[i].length; dir++) { // above, below, left, right

                System.out.println(
                        Utils.leftTrim(padd + i + " " + Direction.toString(dir), size) + " "
                                + Utils.toS(mProbability[i][dir]));
                System.out.println(padd + " " + Utils.toS(mMargin[i][dir]));

            }
        }
    }
}
