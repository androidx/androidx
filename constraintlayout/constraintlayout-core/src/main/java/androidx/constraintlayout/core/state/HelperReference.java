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

package androidx.constraintlayout.core.state;

import androidx.constraintlayout.core.state.helpers.Facade;
import androidx.constraintlayout.core.widgets.ConstraintWidget;
import androidx.constraintlayout.core.widgets.HelperWidget;

import java.util.ArrayList;
import java.util.Collections;

public class HelperReference extends ConstraintReference implements Facade {
    protected final State mHelperState;
    final State.Helper mType;
    protected ArrayList<Object> mReferences = new ArrayList<>();
    private HelperWidget mHelperWidget;

    public HelperReference(State state, State.Helper type) {
        super(state);
        mHelperState = state;
        mType = type;
    }

    public State.Helper getType() {
        return mType;
    }

    // @TODO: add description
    public HelperReference add(Object... objects) {
        Collections.addAll(mReferences, objects);
        return this;
    }

    public void setHelperWidget(HelperWidget helperWidget) {
        mHelperWidget = helperWidget;
    }

    public HelperWidget getHelperWidget() {
        return mHelperWidget;
    }

    @Override
    public ConstraintWidget getConstraintWidget() {
        return getHelperWidget();
    }

    // @TODO: add description
    @Override
    public void apply() {
        // nothing
    }

    /**
     * Allows the derived classes to invoke the apply method in the ConstraintReference
     */
    public void applyBase() {
        super.apply();
    }
}
