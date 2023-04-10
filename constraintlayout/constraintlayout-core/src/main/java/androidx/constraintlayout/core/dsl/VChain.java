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

public class VChain extends Chain {

    public class VAnchor extends Anchor {
        VAnchor(Constraint.VSide side) {
            super(Constraint.Side.valueOf(side.name()));
        }
    }

    private VAnchor mTop = new VAnchor(Constraint.VSide.TOP);
    private VAnchor mBottom = new VAnchor(Constraint.VSide.BOTTOM);
    private VAnchor mBaseline = new VAnchor(Constraint.VSide.BASELINE);

    public VChain(String name) {
        super(name);
        type = new HelperType(typeMap.get(Type.VERTICAL_CHAIN));
    }

    public VChain(String name, String config) {
        super(name);
        this.config = config;
        type = new HelperType(typeMap.get(Type.VERTICAL_CHAIN));
        configMap = convertConfigToMap();
        if (configMap.containsKey("contains")) {
            Ref.addStringToReferences(configMap.get("contains"), references);
        }
    }

    /**
     * Get the top anchor
     *
     * @return the top anchor
     */
    public VAnchor getTop() {
        return mTop;
    }

    /**
     * Connect anchor to Top
     *
     * @param anchor anchor to be connected
     */
    public void linkToTop(Constraint.VAnchor anchor) {
        linkToTop(anchor, 0);
    }

    /**
     * Connect anchor to Top
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToTop(Constraint.VAnchor anchor, int margin) {
        linkToTop(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Top
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToTop(Constraint.VAnchor anchor, int margin, int goneMargin) {
        mTop.mConnection = anchor;
        mTop.mMargin = margin;
        mTop.mGoneMargin = goneMargin;
        configMap.put("top", mTop.toString());
    }

    /**
     * Get the bottom anchor
     *
     * @return the bottom anchor
     */
    public VAnchor getBottom() {
        return mBottom;
    }

    /**
     * Connect anchor to Bottom
     *
     * @param anchor anchor to be connected
     */
    public void linkToBottom(Constraint.VAnchor anchor) {
        linkToBottom(anchor, 0);
    }

    /**
     * Connect anchor to Bottom
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToBottom(Constraint.VAnchor anchor, int margin) {
        linkToBottom(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Bottom
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToBottom(Constraint.VAnchor anchor, int margin, int goneMargin) {
        mBottom.mConnection = anchor;
        mBottom.mMargin = margin;
        mBottom.mGoneMargin = goneMargin;
        configMap.put("bottom", mBottom.toString());
    }

    /**
     * Get the baseline anchor
     *
     * @return the baseline anchor
     */
    public VAnchor getBaseline() {
        return mBaseline;
    }

    /**
     * Connect anchor to Baseline
     *
     * @param anchor anchor to be connected
     */
    public void linkToBaseline(Constraint.VAnchor anchor) {
        linkToBaseline(anchor, 0);
    }

    /**
     * Connect anchor to Baseline
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     */
    public void linkToBaseline(Constraint.VAnchor anchor, int margin) {
        linkToBaseline(anchor, margin, Integer.MIN_VALUE);
    }

    /**
     * Connect anchor to Baseline
     *
     * @param anchor anchor to be connected
     * @param margin value of the margin
     * @param goneMargin value of the goneMargin
     */
    public void linkToBaseline(Constraint.VAnchor anchor, int margin, int goneMargin) {
        mBaseline.mConnection = anchor;
        mBaseline.mMargin = margin;
        mBaseline.mGoneMargin = goneMargin;
        configMap.put("baseline", mBaseline.toString());
    }

}
