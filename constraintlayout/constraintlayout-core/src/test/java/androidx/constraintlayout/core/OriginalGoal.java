/*
 * Copyright (C) 2015 The Android Open Source Project
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

package androidx.constraintlayout.core;

import java.util.ArrayList;

/**
 * Represents a goal to minimize
 */
public class OriginalGoal {

    static int sMax = 6;

    class GoalElement {
        float[] mStrengths = new float[sMax];
        SolverVariable mVariable;

        void clearStrengths() {
            for (int i = 0; i < sMax; i++) {
                mStrengths[i] = 0;
            }
        }

        public String toString() {
            String representation = mVariable + "[";
            for (int j = 0; j < mStrengths.length; j++) {
                representation += mStrengths[j];
                if (j < mStrengths.length - 1) {
                    representation += ", ";
                } else {
                    representation += "] ";
                }
            }
            return representation;
        }
    }

    ArrayList<GoalElement> mVariables = new ArrayList<>();

    public SolverVariable getPivotCandidate() {
        final int count = mVariables.size();
        SolverVariable candidate = null;
        int strength = 0;

        for (int i = 0; i < count; i++) {
            GoalElement element = mVariables.get(i);
//            System.out.println("get pivot, looking at " + element);
            for (int k = sMax - 1; k >= 0; k--) {
                float value = element.mStrengths[k];
                if (candidate == null && value < 0 && (k >= strength)) {
                    strength = k;
                    candidate = element.mVariable;
//                    System.out.println("-> k: " + k + " strength: "
//                      + strength + " v: " + value + " candidate " + candidate);
                }
                if (value > 0 && k > strength) {
//                    System.out.println("-> reset, k: " + k + " strength: "
//                      + strength + " v: " + value + " candidate " + candidate);
                    strength = k;
                    candidate = null;
                }
            }
        }
        return candidate;
    }

    public void updateFromSystemErrors(LinearSystem system) {
        for (int i = 1; i < system.mNumColumns; i++) {
            SolverVariable variable = system.mCache.mIndexedVariables[i];
            if (variable.mType != SolverVariable.Type.ERROR) {
                continue;
            }
            GoalElement element = new GoalElement();
            element.mVariable = variable;
            element.mStrengths[variable.strength] = 1;
            mVariables.add(element);
        }
    }

    public void updateFromSystem(LinearSystem system) {
        mVariables.clear();
        updateFromSystemErrors(system);
        final int count = mVariables.size();
        for (int i = 0; i < count; i++) {
            GoalElement element = mVariables.get(i);
            if (element.mVariable.mDefinitionId != -1) {
                ArrayRow definition = system.getRow(element.mVariable.mDefinitionId);
                ArrayLinkedVariables variables =
                        (ArrayLinkedVariables) (Object) definition.variables;
                int size = variables.mCurrentSize;
                for (int j = 0; j < size; j++) {
                    SolverVariable var = variables.getVariable(j);
                    float value = variables.getVariableValue(j);
                    add(element, var, value);
                }
                element.clearStrengths();
            }
        }
    }

    public GoalElement getElement(SolverVariable variable) {
        final int count = mVariables.size();
        for (int i = 0; i < count; i++) {
            GoalElement element = mVariables.get(i);
            if (element.mVariable == variable) {
                return element;
            }
        }
        GoalElement element = new GoalElement();
        element.mVariable = variable;
        element.mStrengths[variable.strength] = 1;
        mVariables.add(element);
        return element;
    }

    public void add(GoalElement element, SolverVariable variable, float value) {
        GoalElement addition = getElement(variable);
        for (int i = 0; i < sMax; i++) {
            addition.mStrengths[i] += element.mStrengths[i] * value;
        }
    }

    public String toString() {
        String representation = "OriginalGoal: ";
        final int count = mVariables.size();
        for (int i = 0; i < count; i++) {
            GoalElement element = mVariables.get(i);
            representation += element.mVariable + "[";
            for (int j = 0; j < element.mStrengths.length; j++) {
                representation += element.mStrengths[j];
                if (j < element.mStrengths.length - 1) {
                    representation += ", ";
                } else {
                    representation += "], ";
                }
            }
        }
        return representation;
    }

}
