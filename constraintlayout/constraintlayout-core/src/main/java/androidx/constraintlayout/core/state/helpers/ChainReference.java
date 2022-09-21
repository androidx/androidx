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

import androidx.constraintlayout.core.state.HelperReference;
import androidx.constraintlayout.core.state.State;

import java.util.HashMap;

public class ChainReference extends HelperReference {

    protected float mBias = 0.5f;
    protected HashMap<String ,Float> mMapWeights;
    protected HashMap<String,Float> mMapPreMargin;
    protected HashMap<String,Float> mMapPostMargin;

    protected State.Chain mStyle = State.Chain.SPREAD;

    public ChainReference(State state, State.Helper type) {
        super(state, type);
    }

    public State.Chain getStyle() {
        return State.Chain.SPREAD;
    }

    // @TODO: add description
    public ChainReference style(State.Chain style) {
        mStyle = style;
        return this;
    }

    public void addChainElement(String id, float weight, float preMargin, float  postMargin ) {
        super.add(id);
        if (!Float.isNaN(weight)) {
            if (mMapWeights == null) {
                mMapWeights = new HashMap<>();
            }
            mMapWeights.put(id, weight);
        }
        if (!Float.isNaN(preMargin)) {
            if (mMapPreMargin == null) {
                mMapPreMargin = new HashMap<>();
            }
            mMapPreMargin.put(id, preMargin);
        }
        if (!Float.isNaN(postMargin)) {
            if (mMapPostMargin == null) {
                mMapPostMargin = new HashMap<>();
            }
            mMapPostMargin.put(id, postMargin);
        }
    }

  protected float getWeight(String id) {
       if (mMapWeights == null) {
           return UNKNOWN;
       }
       if (mMapWeights.containsKey(id)) {
           return mMapWeights.get(id);
       }
       return UNKNOWN;
    }

    protected float getPostMargin(String id) {
        if (mMapPostMargin != null  && mMapPostMargin.containsKey(id)) {
            return mMapPostMargin.get(id);
        }
        return 0;
    }

    protected float getPreMargin(String id) {
        if (mMapPreMargin != null  && mMapPreMargin.containsKey(id)) {
            return mMapPreMargin.get(id);
        }
        return 0;
    }

    public float getBias() {
        return mBias;
    }

    // @TODO: add description
    @Override
    public ChainReference bias(float bias) {
        mBias = bias;
        return this;
    }
}
