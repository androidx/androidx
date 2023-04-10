/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

public class HChain extends Chain {
    public class HAnchor extends Anchor {
        HAnchor(Constraint.HSide side) {
            super(Constraint.Side.valueOf(side.name()));
        }
    }

    private HAnchor mLeft = new HAnchor(Constraint.HSide.LEFT);
    private HAnchor mRight = new HAnchor(Constraint.HSide.RIGHT);
    private HAnchor mStart = new HAnchor(Constraint.HSide.START);
    private HAnchor mEnd = new HAnchor(Constraint.HSide.END);

    public HChain(String name) {
        super(name);
        type = new HelperType(typeMap.get(Type.HORIZONTAL_CHAIN));
    }

    public HChain(String name, String config) {
        super(name);
        this.config = config;
        type = new HelperType(typeMap.get(Type.HORIZONTAL_CHAIN));
        configMap = convertConfigToMap();
        if (configMap.containsKey("contains")) {
            Ref.addStringToReferences(configMap.get("contains"), references);
        }
    }

    /**
     * Get the left anchor
     *
     * @return the left anchor
     */
    public HAnchor getLeft() {
        return mLeft;
    }

    /**
     * Connect anchor to Left
     *
     * @param anchor anchor to be connected
     */
    public void linkToLeft(Constraint.HAnchor anchor) {
        linkToLeft(anchor, 0);
    }

    /**
     * Connect anchor to Left
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToLeft(Constraint.HAnchor anchor, int margin) {
        linkToLeft(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Left
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToLeft(Constraint.HAnchor anchor, int margin, int goneMargin) {
        mLeft.mConnection = anchor;
        mLeft.mMargin = margin;
        mLeft.mGoneMargin = goneMargin;
        configMap.put("left", mLeft.toString());
    }

    /**
     * Get the right anchor
     *
     * @return the right anchor
     */
    public HAnchor getRight() {
        return mRight;
    }

    /**
     * Connect anchor to Right
     *
     * @param anchor anchor to be connected
     */
    public void linkToRight(Constraint.HAnchor anchor) {
        linkToRight(anchor, 0);
    }

    /**
     * Connect anchor to Right
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToRight(Constraint.HAnchor anchor, int margin) {
        linkToRight(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Right
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToRight(Constraint.HAnchor anchor, int margin, int goneMargin) {
        mRight.mConnection = anchor;
        mRight.mMargin = margin;
        mRight.mGoneMargin = goneMargin;
        configMap.put("right", mRight.toString());
    }

    /**
     * Get the start anchor
     *
     * @return the start anchor
     */
    public HAnchor getStart() {
        return mStart;
    }

    /**
     * Connect anchor to Start
     *
     * @param anchor anchor to be connected
     */
    public void linkToStart(Constraint.HAnchor anchor) {
        linkToStart(anchor, 0);
    }

    /**
     * Connect anchor to Start
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToStart(Constraint.HAnchor anchor, int margin) {
        linkToStart(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Start
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToStart(Constraint.HAnchor anchor, int margin, int goneMargin) {
        mStart.mConnection = anchor;
        mStart.mMargin = margin;
        mStart.mGoneMargin = goneMargin;
        configMap.put("start", mStart.toString());
    }

    /**
     * Get the end anchor
     *
     * @return the end anchor
     */
    public HAnchor getEnd() {
        return mEnd;
    }

    /**
     * Connect anchor to End
     *
     * @param anchor anchor to be connected
     */
    public void linkToEnd(Constraint.HAnchor anchor) {
        linkToEnd(anchor, 0);
    }

    /**
     * Connect anchor to End
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToEnd(Constraint.HAnchor anchor, int margin) {
        linkToEnd(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to End
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToEnd(Constraint.HAnchor anchor, int margin, int goneMargin) {
        mEnd.mConnection = anchor;
        mEnd.mMargin = margin;
        mEnd.mGoneMargin = goneMargin;
        configMap.put("end", mEnd.toString());
    }

}
