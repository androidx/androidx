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

import java.util.ArrayList;

/**
 * This defines to MotionScene container
 * It contains ConstraintSet and Transitions
 */
public class MotionScene {
    ArrayList<Transition> mTransitions = new ArrayList<>();
    ArrayList<ConstraintSet> mConstraintSets = new ArrayList<>();

    // todo add support for variables, generate and helpers
    public void addTransition(Transition transition) {
        mTransitions.add(transition);
    }

    public void addConstraintSet(ConstraintSet constraintSet) {
        mConstraintSets.add(constraintSet);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("{\n");
        if (!mTransitions.isEmpty()) {
            ret.append("Transitions:{\n");
            for (Transition transition : mTransitions) {
                ret.append(transition.toString());
            }
            ret.append("},\n");
        }
        if (!mConstraintSets.isEmpty()) {
            ret.append("ConstraintSets:{\n");
            for (ConstraintSet constraintSet : mConstraintSets) {
                ret.append(constraintSet.toString());
            }
            ret.append("},\n");
        }

        ret.append("}\n");
        return ret.toString();
    }
}
