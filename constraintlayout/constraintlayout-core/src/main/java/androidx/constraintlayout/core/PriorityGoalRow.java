/*
 * Copyright (C) 2020 The Android Open Source Project
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

import java.util.Arrays;
import java.util.Comparator;

/**
 * Implements a row containing goals taking in account priorities.
 */
public class PriorityGoalRow extends ArrayRow {
    private static final float EPSILON = 0.0001f;
    @SuppressWarnings("unused") private static final boolean DEBUG = false;

    private int mTableSize = 128;
    private SolverVariable[] mArrayGoals = new SolverVariable[mTableSize];
    private SolverVariable[] mSortArray = new SolverVariable[mTableSize];
    private int mNumGoals = 0;
    GoalVariableAccessor mAccessor = new GoalVariableAccessor(this);

    class GoalVariableAccessor {
        SolverVariable mVariable;
        PriorityGoalRow mRow;

        GoalVariableAccessor(PriorityGoalRow row) {
            this.mRow = row;
        }

        public void init(SolverVariable variable) {
            this.mVariable = variable;
        }

        public boolean addToGoal(SolverVariable other, float value) {
            if (mVariable.inGoal) {
                boolean empty = true;
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    mVariable.mGoalStrengthVector[i] += other.mGoalStrengthVector[i] * value;
                    float v = mVariable.mGoalStrengthVector[i];
                    if (Math.abs(v) < EPSILON) {
                        mVariable.mGoalStrengthVector[i] = 0;
                    } else {
                        empty = false;
                    }
                }
                if (empty) {
                    removeGoal(mVariable);
                }
            } else {
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    float strength = other.mGoalStrengthVector[i];
                    if (strength != 0) {
                        float v = value * strength;
                        if (Math.abs(v) < EPSILON) {
                            v = 0;
                        }
                        mVariable.mGoalStrengthVector[i] = v;
                    } else {
                        mVariable.mGoalStrengthVector[i] = 0;
                    }
                }
                return true;
            }
            return false;
        }

        public void add(SolverVariable other) {
            for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                mVariable.mGoalStrengthVector[i] += other.mGoalStrengthVector[i];
                float value = mVariable.mGoalStrengthVector[i];
                if (Math.abs(value) < EPSILON) {
                    mVariable.mGoalStrengthVector[i] = 0;
                }
            }
        }

        public final boolean isNegative() {
            for (int i = SolverVariable.MAX_STRENGTH - 1; i >= 0; i--) {
                float value = mVariable.mGoalStrengthVector[i];
                if (value > 0) {
                    return false;
                }
                if (value < 0) {
                    return true;
                }
            }
            return false;
        }

        public final boolean isSmallerThan(SolverVariable other) {
            for (int i = SolverVariable.MAX_STRENGTH - 1; i >= 0; i--) {
                float comparedValue = other.mGoalStrengthVector[i];
                float value = mVariable.mGoalStrengthVector[i];
                if (value == comparedValue) {
                    continue;
                }
                return value < comparedValue;
            }
            return false;
        }

        public final boolean isNull() {
            for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                if (mVariable.mGoalStrengthVector[i] != 0) {
                    return false;
                }
            }
            return true;
        }

        public void reset() {
            Arrays.fill(mVariable.mGoalStrengthVector, 0);
        }

        @Override
        public String toString() {
            String result = "[ ";
            if (mVariable != null) {
                for (int i = 0; i < SolverVariable.MAX_STRENGTH; i++) {
                    result += mVariable.mGoalStrengthVector[i] + " ";
                }
            }
            result += "] " + mVariable;
            return result;
        }

    }

    @Override
    public void clear() {
        mNumGoals = 0;
        mConstantValue = 0;
    }

    Cache mCache;

    public PriorityGoalRow(Cache cache) {
        super(cache);
        mCache = cache;
    }

    @Override
    public boolean isEmpty() {
        return mNumGoals == 0;
    }

    static final int NOT_FOUND = -1;

    @Override
    public SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid) {
        int pivot = NOT_FOUND;
        for (int i = 0; i < mNumGoals; i++) {
            SolverVariable variable = mArrayGoals[i];
            if (avoid[variable.id]) {
                continue;
            }
            mAccessor.init(variable);
            if (pivot == NOT_FOUND) {
                if (mAccessor.isNegative()) {
                    pivot = i;
                }
            } else if (mAccessor.isSmallerThan(mArrayGoals[pivot])) {
                pivot = i;
            }
        }
        if (pivot == NOT_FOUND) {
            return null;
        }
        return mArrayGoals[pivot];
    }

    @Override
    public void addError(SolverVariable error) {
        mAccessor.init(error);
        mAccessor.reset();
        error.mGoalStrengthVector[error.strength] = 1;
        addToGoal(error);
    }

    private void addToGoal(SolverVariable variable) {
        if (mNumGoals + 1 > mArrayGoals.length) {
            mArrayGoals = Arrays.copyOf(mArrayGoals, mArrayGoals.length * 2);
            mSortArray = Arrays.copyOf(mArrayGoals, mArrayGoals.length * 2);
        }
        mArrayGoals[mNumGoals] = variable;
        mNumGoals++;

        if (mNumGoals > 1 && mArrayGoals[mNumGoals - 1].id > variable.id) {
            for (int i = 0; i < mNumGoals; i++) {
                mSortArray[i] = mArrayGoals[i];
            }
            Arrays.sort(mSortArray, 0, mNumGoals, new Comparator<SolverVariable>() {
                @Override
                public int compare(SolverVariable variable1, SolverVariable variable2) {
                    return variable1.id - variable2.id;
                }
            });
            for (int i = 0; i < mNumGoals; i++) {
                mArrayGoals[i] = mSortArray[i];
            }
        }

        variable.inGoal = true;
        variable.addToRow(this);
    }

    private void removeGoal(SolverVariable variable) {
        for (int i = 0; i < mNumGoals; i++) {
            if (mArrayGoals[i] == variable) {
                for (int j = i; j < mNumGoals - 1; j++) {
                    mArrayGoals[j] = mArrayGoals[j + 1];
                }
                mNumGoals--;
                variable.inGoal = false;
                return;
            }
        }
    }

    @Override
    public void updateFromRow(LinearSystem system,
            ArrayRow definition,
            boolean removeFromDefinition) {
        SolverVariable goalVariable = definition.mVariable;
        if (goalVariable == null) {
            return;
        }

        ArrayRowVariables rowVariables = definition.variables;
        int currentSize = rowVariables.getCurrentSize();
        for (int i = 0; i < currentSize; i++) {
            SolverVariable solverVariable = rowVariables.getVariable(i);
            float value = rowVariables.getVariableValue(i);
            mAccessor.init(solverVariable);
            if (mAccessor.addToGoal(goalVariable, value)) {
                addToGoal(solverVariable);
            }
            mConstantValue += definition.mConstantValue * value;
        }
        removeGoal(goalVariable);
    }

    @Override
    public String toString() {
        String result = "";
        result += " goal -> (" + mConstantValue + ") : ";
        for (int i = 0; i < mNumGoals; i++) {
            SolverVariable v = mArrayGoals[i];
            mAccessor.init(v);
            result += mAccessor + " ";
        }
        return result;
    }
}
