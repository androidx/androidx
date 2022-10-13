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

import androidx.annotation.NonNull;
import androidx.constraintlayout.core.state.HelperReference;
import androidx.constraintlayout.core.state.State;

import java.util.HashMap;

public class ChainReference extends HelperReference {

    protected float mBias = 0.5f;

    /**
     * @deprecated Unintended visibility, use {@link #getWeight(String)} instead
     */
    @Deprecated // TODO(b/253515185): Change to private visibility once we change major version
    protected @NonNull HashMap<String, Float> mMapWeights = new HashMap<>();

    /**
     * @deprecated Unintended visibility, use {@link #getPreMargin(String)} instead
     */
    @Deprecated // TODO(b/253515185): Change to private visibility once we change major version
    protected @NonNull HashMap<String, Float> mMapPreMargin = new HashMap<>();

    /**
     * @deprecated Unintended visibility, use {@link #getPostMargin(String)} instead
     */
    @Deprecated // TODO(b/253515185): Change to private visibility once we change major version
    protected @NonNull HashMap<String, Float> mMapPostMargin = new HashMap<>();

    protected @NonNull State.Chain mStyle = State.Chain.SPREAD;

    public ChainReference(@NonNull State state, @NonNull State.Helper type) {
        super(state, type);
    }

    public @NonNull State.Chain getStyle() {
        return State.Chain.SPREAD;
    }

    /**
     * Sets the {@link State.Chain style}.
     *
     * @param style Defines the way the chain will lay out its elements
     * @return This same instance
     */
    @NonNull
    public ChainReference style(@NonNull State.Chain style) {
        mStyle = style;
        return this;
    }

    /**
     * Adds the element by the given id to the Chain.
     *
     * The order in which the elements are added is important. It will represent the element's
     * position in the Chain.
     *
     * @param id Id of the element to add
     * @param weight Weight used to distribute remaining space each element
     * @param preMargin Additional space in pixels between the added element and the previous one
     *                  (if any)
     * @param postMargin Additional space in pixels between the added element and the next one (if
     *                   any)
     */
    public void addChainElement(@NonNull String id,
            float weight,
            float preMargin,
            float postMargin) {
        super.add(id);
        if (!Float.isNaN(weight)) {
            mMapWeights.put(id, weight);
        }
        if (!Float.isNaN(preMargin)) {
            mMapPreMargin.put(id, preMargin);
        }
        if (!Float.isNaN(postMargin)) {
            mMapPostMargin.put(id, postMargin);
        }
    }

    protected float getWeight(@NonNull String id) {
        if (mMapWeights.containsKey(id)) {
            return mMapWeights.get(id);
        }
        return UNKNOWN;
    }

    protected float getPostMargin(@NonNull String id) {
        if (mMapPostMargin.containsKey(id)) {
            return mMapPostMargin.get(id);
        }
        return 0;
    }

    protected float getPreMargin(@NonNull String id) {
        if (mMapPreMargin.containsKey(id)) {
            return mMapPreMargin.get(id);
        }
        return 0;
    }

    public float getBias() {
        return mBias;
    }

    // @TODO: add description
    @NonNull
    @Override
    public ChainReference bias(float bias) {
        mBias = bias;
        return this;
    }
}
