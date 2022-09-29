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

import androidx.constraintlayout.core.state.ConstraintReference;
import androidx.constraintlayout.core.state.HelperReference;
import androidx.constraintlayout.core.state.State;

public class AlignVerticallyReference extends HelperReference {

    private float mBias = 0.5f;

    public AlignVerticallyReference(State state) {
        super(state, State.Helper.ALIGN_VERTICALLY);
    }

    // @TODO: add description
    @Override
    public void apply() {
        for (Object key : mReferences) {
            ConstraintReference reference = mHelperState.constraints(key);
            reference.clearVertical();
            if (mTopToTop != null) {
                reference.topToTop(mTopToTop);
            } else if (mTopToBottom != null) {
                reference.topToBottom(mTopToBottom);
            } else {
                reference.topToTop(State.PARENT);
            }
            if (mBottomToTop != null) {
                reference.bottomToTop(mBottomToTop);
            } else if (mBottomToBottom != null) {
                reference.bottomToBottom(mBottomToBottom);
            } else {
                reference.bottomToBottom(State.PARENT);
            }
            if (mBias != 0.5f) {
                reference.verticalBias(mBias);
            }
        }
    }

}
