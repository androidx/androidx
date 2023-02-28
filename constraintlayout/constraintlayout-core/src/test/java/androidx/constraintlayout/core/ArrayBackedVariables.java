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

import java.util.Arrays;

/**
 * Store a set of variables and their values in an array.
 */
class ArrayBackedVariables {
    private static final boolean DEBUG = false;

    private SolverVariable[] mVariables = null;
    private float[] mValues = null;
    private int[] mIndexes = null;
    private final int mThreshold = 16;
    private int mMaxSize = 4;
    private int mCurrentSize = 0;
    private int mCurrentWriteSize = 0;
    private SolverVariable mCandidate = null;

    ArrayBackedVariables(ArrayRow arrayRow, Cache cache) {
        mVariables = new SolverVariable[mMaxSize];
        mValues = new float[mMaxSize];
        mIndexes = new int[mMaxSize];
    }

    public SolverVariable getPivotCandidate() {
        if (mCandidate == null) {
            for (int i = 0; i < mCurrentSize; i++) {
                int idx = mIndexes[i];
                if (mValues[idx] < 0) {
                    mCandidate = mVariables[idx];
                    break;
                }
            }
        }
        return mCandidate;
    }

    void increaseSize() {
        mMaxSize *= 2;
        mVariables = Arrays.copyOf(mVariables, mMaxSize);
        mValues = Arrays.copyOf(mValues, mMaxSize);
        mIndexes = Arrays.copyOf(mIndexes, mMaxSize);
    }

    public final int size() {
        return mCurrentSize;
    }

    public final SolverVariable getVariable(int index) {
        return mVariables[mIndexes[index]];
    }

    public final float getVariableValue(int index) {
        return mValues[mIndexes[index]];
    }

    public final void updateArray(ArrayBackedVariables target, float amount) {
        if (amount == 0) {
            return;
        }
        for (int i = 0; i < mCurrentSize; i++) {
            final int idx = mIndexes[i];
            SolverVariable v = mVariables[idx];
            float value = mValues[idx];
            target.add(v, (value * amount));
        }
    }

    public void setVariable(int index, float value) {
        int idx = mIndexes[index];
        mValues[idx] = value;
        if (value < 0) {
            mCandidate = mVariables[idx];
        }
    }

    public final float get(SolverVariable v) {
        if (mCurrentSize < mThreshold) {
            for (int i = 0; i < mCurrentSize; i++) {
                int idx = mIndexes[i];
                if (mVariables[idx] == v) {
                    return mValues[idx];
                }
            }
        } else {
            int start = 0;
            int end = mCurrentSize - 1;
            while (start <= end) {
                int index = start + (end - start) / 2;
                int idx = mIndexes[index];
                SolverVariable current = mVariables[idx];
                if (current == v) {
                    return mValues[idx];
                } else if (current.id < v.id) {
                    start = index + 1;
                } else {
                    end = index - 1;
                }
            }
        }
        return 0;
    }

    public void put(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable);
            return;
        }
        while (true) {
            int firstEmptyIndex = -1;
            for (int i = 0; i < mCurrentWriteSize; i++) {
                if (mVariables[i] == variable) {
                    mValues[i] = value;
                    if (value < 0) {
                        mCandidate = variable;
                    }
                    return;
                }
                if (firstEmptyIndex == -1 && mVariables[i] == null) {
                    firstEmptyIndex = i;
                }
            }
            if (firstEmptyIndex == -1 && mCurrentWriteSize < mMaxSize) {
                firstEmptyIndex = mCurrentWriteSize;
            }
            if (firstEmptyIndex != -1) {
                mVariables[firstEmptyIndex] = variable;
                mValues[firstEmptyIndex] = value;
                // insert the position...
                boolean inserted = false;
                for (int j = 0; j < mCurrentSize; j++) {
                    int index = mIndexes[j];
                    if (mVariables[index].id > variable.id) {
                        // this is our insertion point
                        System.arraycopy(mIndexes, j, mIndexes, j + 1, (mCurrentSize - j));
                        mIndexes[j] = firstEmptyIndex;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    mIndexes[mCurrentSize] = firstEmptyIndex;
                }
                mCurrentSize++;
                if (firstEmptyIndex + 1 > mCurrentWriteSize) {
                    mCurrentWriteSize = firstEmptyIndex + 1;
                }
                if (value < 0) {
                    mCandidate = variable;
                }
                return;
            } else {
                increaseSize();
            }
        }
    }

    public void add(SolverVariable variable, float value) {
        if (value == 0) {
            return;
        }
        while (true) {
            int firstEmptyIndex = -1;
            for (int i = 0; i < mCurrentWriteSize; i++) {
                if (mVariables[i] == variable) {
                    mValues[i] += value;
                    if (value < 0) {
                        mCandidate = variable;
                    }
                    if (mValues[i] == 0) {
                        remove(variable);
                    }
                    return;
                }
                if (firstEmptyIndex == -1 && mVariables[i] == null) {
                    firstEmptyIndex = i;
                }
            }
            if (firstEmptyIndex == -1 && mCurrentWriteSize < mMaxSize) {
                firstEmptyIndex = mCurrentWriteSize;
            }
            if (firstEmptyIndex != -1) {
                mVariables[firstEmptyIndex] = variable;
                mValues[firstEmptyIndex] = value;
                // insert the position...
                boolean inserted = false;
                for (int j = 0; j < mCurrentSize; j++) {
                    int index = mIndexes[j];
                    if (mVariables[index].id > variable.id) {
                        // this is our insertion point
                        System.arraycopy(mIndexes, j, mIndexes, j + 1, (mCurrentSize - j));
                        mIndexes[j] = firstEmptyIndex;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    mIndexes[mCurrentSize] = firstEmptyIndex;
                }
                mCurrentSize++;
                if (firstEmptyIndex + 1 > mCurrentWriteSize) {
                    mCurrentWriteSize = firstEmptyIndex + 1;
                }
                if (value < 0) {
                    mCandidate = variable;
                }
                return;
            } else {
                increaseSize();
            }
        }
    }

    public void clear() {
        for (int i = 0, length = mVariables.length; i < length; i++) {
            mVariables[i] = null;
        }
        mCurrentSize = 0;
        mCurrentWriteSize = 0;
    }

    public boolean containsKey(SolverVariable variable) {
        if (mCurrentSize < 8) {
            for (int i = 0; i < mCurrentSize; i++) {
                if (mVariables[mIndexes[i]] == variable) {
                    return true;
                }
            }
        } else {
            int start = 0;
            int end = mCurrentSize - 1;
            while (start <= end) {
                int index = start + (end - start) / 2;
                SolverVariable current = mVariables[mIndexes[index]];
                if (current == variable) {
                    return true;
                } else if (current.id < variable.id) {
                    start = index + 1;
                } else {
                    end = index - 1;
                }
            }
        }
        return false;
    }

    public float remove(SolverVariable variable) {
        if (DEBUG) {
            System.out.print("BEFORE REMOVE " + variable + " -> ");
            display();
        }
        if (mCandidate == variable) {
            mCandidate = null;
        }
        for (int i = 0; i < mCurrentWriteSize; i++) {
            int idx = mIndexes[i];
            if (mVariables[idx] == variable) {
                float amount = mValues[idx];
                mVariables[idx] = null;
                System.arraycopy(mIndexes, i + 1, mIndexes, i, (mCurrentWriteSize - i - 1));
                mCurrentSize--;
                if (DEBUG) {
                    System.out.print("AFTER REMOVE ");
                    display();
                }
                return amount;
            }
        }
        return 0;
    }

    public int sizeInBytes() {
        int size = 0;
        size += (mMaxSize * 4);
        size += (mMaxSize * 4);
        size += (mMaxSize * 4);
        size += 4 + 4 + 4 + 4;
        return size;
    }

    public void display() {
        int count = size();
        System.out.print("{ ");
        for (int i = 0; i < count; i++) {
            System.out.print(getVariable(i) + " = " + getVariableValue(i) + " ");
        }
        System.out.println(" }");
    }

    private String getInternalArrays() {
        String str = "";
        int count = size();
        str += "idx { ";
        for (int i = 0; i < count; i++) {
            str += mIndexes[i] + " ";
        }
        str += "}\n";
        str += "obj { ";
        for (int i = 0; i < count; i++) {
            str += mVariables[i] + ":" + mValues[i] + " ";
        }
        str += "}\n";
        return str;
    }

    public void displayInternalArrays() {
        int count = size();
        System.out.print("idx { ");
        for (int i = 0; i < count; i++) {
            System.out.print(mIndexes[i] + " ");
        }
        System.out.println("}");
        System.out.print("obj { ");
        for (int i = 0; i < count; i++) {
            System.out.print(mVariables[i] + ":" + mValues[i] + " ");
        }
        System.out.println("}");
    }

    public void updateFromRow(ArrayRow arrayRow, ArrayRow definition) {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
    }

    public SolverVariable pickPivotCandidate() {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
        return null;
    }

    public void updateFromSystem(ArrayRow goal, ArrayRow[] mRows) {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
    }

    public void divideByAmount(float amount) {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
    }

    public void updateClientEquations(ArrayRow arrayRow) {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
    }

    public boolean hasAtLeastOnePositiveVariable() {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
        return false;
    }

    public void invert() {
        // TODO -- only used when ArrayRow.USE_LINKED_VARIABLES is set to true
    }
}
