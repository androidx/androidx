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

/**
 * Represents a goal to minimize
 */
public class OptimizedGoal extends OriginalGoal {

    static int sMAX = 6;

    final LinearSystem mSystem;

    public OptimizedGoal(LinearSystem system) {
        mSystem = system;
    }

    public SolverVariable getPivotCandidate() {
        final int count = mSystem.mNumColumns;
        SolverVariable candidate = null;
        int strength = 0;
        for (int i = 1; i < count; i++) {
            SolverVariable element = mSystem.mCache.mIndexedVariables[i];
            if (element.mType != SolverVariable.Type.ERROR) {
                continue;
            }
            for (int k = sMAX - 1; k >= 0; k--) {
                float value = element.mStrengthVector[k];
                if (candidate == null && value < 0 && (k >= strength)) {
                    strength = k;
                    candidate = element;
//                    System.out.println("-> k: " + k + " strength: " + strength
//                    + " v: " + value + " candidate " + candidate);
                }
                if (value > 0 && k > strength) {
//                    System.out.println("-> reset, k: " + k + " strength: " + strength
//                    + " v: " + value + " candidate " + candidate);
                    strength = k;
                    candidate = null;
                }
            }
        }
        return candidate;
    }

    public void updateFromSystemErrors(LinearSystem system) {
        for (int i = 1; i < system.mVariablesID; i++) {
            SolverVariable variable = system.mCache.mIndexedVariables[i];
            if (variable.mType != SolverVariable.Type.ERROR) {
                continue;
            }
            for (int j = 0; j < sMAX; j++) {
                variable.mStrengthVector[j] = 0;
            }
            variable.mStrengthVector[variable.strength] = 1;
        }
    }

    public void updateFromSystem(LinearSystem system) {
        updateFromSystemErrors(system);
        final int count = system.mNumColumns;
        for (int i = 1; i < count; i++) {
            SolverVariable element = system.mCache.mIndexedVariables[i];
            if (element.mDefinitionId != -1) {
                ArrayRow definition = system.getRow(element.mDefinitionId);
                ArrayLinkedVariables variables =
                        (ArrayLinkedVariables) (Object) definition.variables;
                int size = variables.mCurrentSize;
                for (int j = 0; j < size; j++) {
                    SolverVariable var = variables.getVariable(j);
                    float value = variables.getVariableValue(j);
//                    add(element, var, value);
                    for (int k = 0; k < sMAX; k++) {
                        var.mStrengthVector[k] += element.mStrengthVector[k] * value;
                    }
                }
            }
        }
//        variables.clear();
//        initFromSystemErrors(system);
//        final int count = variables.size();
//        for (int i = 0; i < count; i++) {
//            GoalElement element = variables.get(i);
//            if (element.variable.definitionId != -1) {
//                ArrayRow definition = system.getRow(element.variable.definitionId);
//                ArrayLinkedVariables variables = definition.variables;
//                int size = variables.currentSize;
//                for (int j = 0; j < size; j++) {
//                    SolverVariable var = variables.getVariable(j);
//                    float value = variables.getVariableValue(j);
//                    add(element, var, value);
//                }
//                element.clearStrengths();
//            }
//        }
    }

    public String toString() {
        String representation = "OriginalGoal: ";
        for (int i = 1; i < mSystem.mNumColumns; i++) {
            SolverVariable variable = mSystem.mCache.mIndexedVariables[i];
            if (variable.mType != SolverVariable.Type.ERROR) {
                continue;
            }
            representation += variable + "[";
            for (int j = 0; j < variable.mStrengthVector.length; j++) {
                representation += variable.mStrengthVector[j];
                if (j < variable.mStrengthVector.length - 1) {
                    representation += ", ";
                } else {
                    representation += "], ";
                }
            }
        }
        return representation;
    }

}
