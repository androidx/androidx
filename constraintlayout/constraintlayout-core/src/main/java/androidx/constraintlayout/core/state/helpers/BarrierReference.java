/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.constraintlayout.core.widgets.Barrier;
import androidx.constraintlayout.core.widgets.HelperWidget;

public class BarrierReference extends HelperReference {

    private State.Direction mDirection;
    private int mMargin;
    private Barrier mBarrierWidget;

    public BarrierReference(State state) {
        super(state, State.Helper.BARRIER);
    }

    public void setBarrierDirection(State.Direction barrierDirection) {
        mDirection = barrierDirection;
    }

    @Override
    public ConstraintReference margin(Object marginValue) {
        margin(mHelperState.convertDimension(marginValue));
        return this;
    }

    // @TODO: add description
    @Override
    public ConstraintReference margin(int value) {
        mMargin = value;
        return this;
    }

    @Override
    public HelperWidget getHelperWidget() {
        if (mBarrierWidget == null) {
            mBarrierWidget = new Barrier();
        }
        return mBarrierWidget;
    }

    // @TODO: add description
    @Override
    public void apply() {
        getHelperWidget();
        int direction = Barrier.LEFT;
        switch (mDirection) {
            case LEFT:
            case START: {
                // TODO: handle RTL
            }
            break;
            case RIGHT:
            case END: {
                // TODO: handle RTL
                direction = Barrier.RIGHT;
            }
            break;
            case TOP: {
                direction = Barrier.TOP;
            }
            break;
            case BOTTOM: {
                direction = Barrier.BOTTOM;
            }
        }
        mBarrierWidget.setBarrierType(direction);
        mBarrierWidget.setMargin(mMargin);
    }
}
