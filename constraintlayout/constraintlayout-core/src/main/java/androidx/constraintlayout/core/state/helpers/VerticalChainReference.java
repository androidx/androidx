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

public class VerticalChainReference extends ChainReference {

    public VerticalChainReference(State state) {
        super(state, State.Helper.VERTICAL_CHAIN);
    }

    // @TODO: add description
    @Override
    public void apply() {
        ConstraintReference first = null;
        ConstraintReference previous = null;
        for (Object key : mReferences) {
            ConstraintReference reference = mHelperState.constraints(key);
            reference.clearVertical();
        }

        for (Object key : mReferences) {
            ConstraintReference reference = mHelperState.constraints(key);
            if (first == null) {
                first = reference;
                if (mTopToTop != null) {
                    first.topToTop(mTopToTop).margin(mMarginTop).marginGone(mMarginTopGone);
                } else if (mTopToBottom != null) {
                    first.topToBottom(mTopToBottom).margin(mMarginTop).marginGone(mMarginTopGone);
                } else {
                    // No constraint declared, default to Parent.
                    String refKey = reference.getKey().toString();
                    first.topToTop(State.PARENT).margin(getPreMargin(refKey));
                }
            }
            if (previous != null) {
                String preKey = previous.getKey().toString();
                String refKey = reference.getKey().toString();
                previous.bottomToTop(reference.getKey()).margin(getPostMargin(preKey));
                reference.topToBottom(previous.getKey()).margin(getPreMargin(refKey));
            }
            float weight = getWeight(key.toString());
            if (weight != UNKNOWN) {
                reference.setVerticalChainWeight(weight);
            }
            previous = reference;
        }

        if (previous != null) {
            if (mBottomToTop != null) {
                previous.bottomToTop(mBottomToTop)
                        .margin(mMarginBottom)
                        .marginGone(mMarginBottomGone);
            } else if (mBottomToBottom != null) {
                previous.bottomToBottom(mBottomToBottom)
                        .margin(mMarginBottom)
                        .marginGone(mMarginBottomGone);
            } else {
                // No constraint declared, default to Parent.
                String preKey = previous.getKey().toString();
                previous.bottomToBottom(State.PARENT).margin(getPostMargin(preKey));
            }
        }

        if (first == null) {
            return;
        }

        if (mBias != 0.5f) {
            first.verticalBias(mBias);
        }

        switch (mStyle) {
            case SPREAD: {
                first.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD);
            }
            break;
            case SPREAD_INSIDE: {
                first.setVerticalChainStyle(ConstraintWidget.CHAIN_SPREAD_INSIDE);
            }
            break;
            case PACKED: {
                first.setVerticalChainStyle(ConstraintWidget.CHAIN_PACKED);
            }
        }
    }
}
