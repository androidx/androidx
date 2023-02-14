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

package androidx.constraintlayout.core.state.helpers;

import static androidx.constraintlayout.core.widgets.ConstraintWidget.UNKNOWN;

import androidx.constraintlayout.core.state.ConstraintReference;
import androidx.constraintlayout.core.state.State;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

public class HorizontalChainReference extends ChainReference {

    public HorizontalChainReference(State state) {
        super(state, State.Helper.HORIZONTAL_CHAIN);
    }

    // @TODO: add description
    @Override
    public void apply() {
        ConstraintReference first = null;
        ConstraintReference previous = null;
        for (Object key : mReferences) {
            ConstraintReference reference = mHelperState.constraints(key);
            reference.clearHorizontal();
        }

        for (Object key : mReferences) {
            ConstraintReference reference = mHelperState.constraints(key);

            if (first == null) {
                first = reference;
                if (mStartToStart != null) {
                    first.startToStart(mStartToStart)
                            .margin(mMarginStart)
                            .marginGone(mMarginStartGone);
                } else if (mStartToEnd != null) {
                    first.startToEnd(mStartToEnd).margin(mMarginStart).marginGone(mMarginStartGone);
                } else if (mLeftToLeft != null) {
                    // TODO: Hack until we support RTL properly
                    first.startToStart(mLeftToLeft).margin(mMarginLeft).marginGone(mMarginLeftGone);
                } else if (mLeftToRight != null) {
                    // TODO: Hack until we support RTL properly
                    first.startToEnd(mLeftToRight).margin(mMarginLeft).marginGone(mMarginLeftGone);
                } else {
                    // No constraint declared, default to Parent.
                    String refKey = reference.getKey().toString();
                    first.startToStart(State.PARENT).margin(getPreMargin(refKey)).marginGone(
                            getPreGoneMargin(refKey));
                }
            }
            if (previous != null) {
                String preKey = previous.getKey().toString();
                String refKey = reference.getKey().toString();
                previous.endToStart(reference.getKey()).margin(getPostMargin(preKey)).marginGone(
                        getPostGoneMargin(preKey));
                reference.startToEnd(previous.getKey()).margin(getPreMargin(refKey)).marginGone(
                        getPreGoneMargin(refKey));
            }
            float weight = getWeight(key.toString());
            if (weight != UNKNOWN) {
                reference.setHorizontalChainWeight(weight);
            }
            previous = reference;
        }

        if (previous != null) {
            if (mEndToStart != null) {
                previous.endToStart(mEndToStart).margin(mMarginEnd).marginGone(mMarginEndGone);
            } else if (mEndToEnd != null) {
                previous.endToEnd(mEndToEnd).margin(mMarginEnd).marginGone(mMarginEndGone);
            } else if (mRightToLeft != null) {
                // TODO: Hack until we support RTL properly
                previous.endToStart(mRightToLeft).margin(mMarginRight).marginGone(mMarginRightGone);
            } else if (mRightToRight != null) {
                // TODO: Hack until we support RTL properly
                previous.endToEnd(mRightToRight).margin(mMarginRight).marginGone(mMarginRightGone);
            } else {
                // No constraint declared, default to Parent.
                String preKey = previous.getKey().toString();
                previous.endToEnd(State.PARENT).margin(getPostMargin(preKey)).marginGone(
                        getPostGoneMargin(preKey));
            }
        }

        if (first == null) {
            return;
        }

        if (mBias != 0.5f) {
            first.horizontalBias(mBias);
        }

        switch (mStyle) {
            case SPREAD: {
                first.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD);
            }
            break;
            case SPREAD_INSIDE: {
                first.setHorizontalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
            }
            break;
            case PACKED: {
                first.setHorizontalChainStyle(ConstraintWidget.CHAIN_PACKED);
            }
        }
    }

}
