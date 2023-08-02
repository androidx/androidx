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

/**
 * Store a set of variables and their values in an array-based linked list coupled
 * with a custom hashmap.
 */
public class SolverVariableValues implements ArrayRow.ArrayRowVariables {

    private static final boolean DEBUG = false;
    @SuppressWarnings("unused") private static final boolean HASH = true;
    private static float sEpsilon = 0.001f;
    private final int mNone = -1;
    private int mSize = 16;
    private int mHashSize = 16;

    int[] mKeys = new int[mSize];
    int[] mNextKeys = new int[mSize];

    int[] mVariables = new int[mSize];
    float[] mValues = new float[mSize];
    int[] mPrevious = new int[mSize];
    int[] mNext = new int[mSize];
    int mCount = 0;
    int mHead = -1;

    private final ArrayRow mRow; // our owner
    // pointer to the system-wide cache, allowing access to SolverVariables
    protected final Cache mCache;

    SolverVariableValues(ArrayRow row, Cache cache) {
        mRow = row;
        mCache = cache;
        clear();
    }

    @Override
    public int getCurrentSize() {
        return mCount;
    }

    @Override
    public SolverVariable getVariable(int index) {
        final int count = mCount;
        if (count == 0) {
            return null;
        }
        int j = mHead;
        for (int i = 0; i < count; i++) {
            if (i == index && j != mNone) {
                return mCache.mIndexedVariables[mVariables[j]];
            }
            j = mNext[j];
            if (j == mNone) {
                break;
            }
        }
        return null;
    }

    @Override
    public float getVariableValue(int index) {
        final int count = mCount;
        int j = mHead;
        for (int i = 0; i < count; i++) {
            if (i == index) {
                return mValues[j];
            }
            j = mNext[j];
            if (j == mNone) {
                break;
            }
        }
        return 0;
    }

    @Override
    public boolean contains(SolverVariable variable) {
        return indexOf(variable) != mNone;
    }

    @Override
    public int indexOf(SolverVariable variable) {
        if (mCount == 0 || variable == null) {
            return mNone;
        }
        int id = variable.id;
        int key = id % mHashSize;
        key = mKeys[key];
        if (key == mNone) {
            return mNone;
        }
        if (mVariables[key] == id) {
            return key;
        }
        while (mNextKeys[key] != mNone && mVariables[mNextKeys[key]] != id) {
            key = mNextKeys[key];
        }
        if (mNextKeys[key] == mNone) {
            return mNone;
        }
        if (mVariables[mNextKeys[key]] == id) {
            return mNextKeys[key];
        }
        return mNone;
    }

    @Override
    public float get(SolverVariable variable) {
        final int index = indexOf(variable);
        if (index != mNone) {
            return mValues[index];
        }
        return 0;
    }

    @Override
    public void display() {
        final int count = mCount;
        System.out.print("{ ");
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v == null) {
                continue;
            }
            System.out.print(v + " = " + getVariableValue(i) + " ");
        }
        System.out.println(" }");
    }

    @Override
    public String toString() {
        String str = hashCode() + " { ";
        final int count = mCount;
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v == null) {
                continue;
            }
            str += v + " = " + getVariableValue(i) + " ";
            int index = indexOf(v);
            str += "[p: ";
            if (mPrevious[index] != mNone) {
                str += mCache.mIndexedVariables[mVariables[mPrevious[index]]];
            } else {
                str += "none";
            }
            str += ", n: ";
            if (mNext[index] != mNone) {
                str += mCache.mIndexedVariables[mVariables[mNext[index]]];
            } else {
                str += "none";
            }
            str += "]";
        }
        str += " }";
        return str;
    }

    @Override
    public void clear() {
        if (DEBUG) {
            System.out.println(this + " <clear>");
        }
        final int count = mCount;
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v != null) {
                v.removeFromRow(mRow);
            }
        }
        for (int i = 0; i < mSize; i++) {
            mVariables[i] = mNone;
            mNextKeys[i] = mNone;
        }
        for (int i = 0; i < mHashSize; i++) {
            mKeys[i] = mNone;
        }
        mCount = 0;
        mHead = -1;
    }

    private void increaseSize() {
        int size = this.mSize * 2;
        mVariables = Arrays.copyOf(mVariables, size);
        mValues = Arrays.copyOf(mValues, size);
        mPrevious = Arrays.copyOf(mPrevious, size);
        mNext = Arrays.copyOf(mNext, size);
        mNextKeys = Arrays.copyOf(mNextKeys, size);
        for (int i = this.mSize; i < size; i++) {
            mVariables[i] = mNone;
            mNextKeys[i] = mNone;
        }
        this.mSize = size;
    }

    private void addToHashMap(SolverVariable variable, int index) {
        if (DEBUG) {
            System.out.println(this.hashCode() + " hash add " + variable.id + " @ " + index);
        }
        int hash = variable.id % mHashSize;
        int key = mKeys[hash];
        if (key == mNone) {
            mKeys[hash] = index;
            if (DEBUG) {
                System.out.println(this.hashCode() + " hash add "
                        + variable.id + " @ " + index + " directly on keys " + hash);
            }
        } else {
            while (mNextKeys[key] != mNone) {
                key = mNextKeys[key];
            }
            mNextKeys[key] = index;
            if (DEBUG) {
                System.out.println(this.hashCode() + " hash add "
                        + variable.id + " @ " + index + " as nextkey of " + key);
            }
        }
        mNextKeys[index] = mNone;
        if (DEBUG) {
            displayHash();
        }
    }

    private void displayHash() {
        for (int i = 0; i < mHashSize; i++) {
            if (mKeys[i] != mNone) {
                String str = this.hashCode() + " hash [" + i + "] => ";
                int key = mKeys[i];
                boolean done = false;
                while (!done) {
                    str += " " + mVariables[key];
                    if (mNextKeys[key] != mNone) {
                        key = mNextKeys[key];
                    } else {
                        done = true;
                    }
                }
                System.out.println(str);
            }
        }
    }

    private void removeFromHashMap(SolverVariable variable) {
        if (DEBUG) {
            System.out.println(this.hashCode() + " hash remove " + variable.id);
        }
        int hash = variable.id % mHashSize;
        int key = mKeys[hash];
        if (key == mNone) {
            if (DEBUG) {
                displayHash();
            }
            return;
        }
        int id = variable.id;
        // let's first find it
        if (mVariables[key] == id) {
            mKeys[hash] = mNextKeys[key];
            mNextKeys[key] = mNone;
        } else {
            while (mNextKeys[key] != mNone && mVariables[mNextKeys[key]] != id) {
                key = mNextKeys[key];
            }
            int currentKey = mNextKeys[key];
            if (currentKey != mNone && mVariables[currentKey] == id) {
                mNextKeys[key] = mNextKeys[currentKey];
                mNextKeys[currentKey] = mNone;
            }
        }
        if (DEBUG) {
            displayHash();
        }
    }

    private void addVariable(int index, SolverVariable variable, float value) {
        mVariables[index] = variable.id;
        mValues[index] = value;
        mPrevious[index] = mNone;
        mNext[index] = mNone;
        variable.addToRow(mRow);
        variable.usageInRowCount++;
        mCount++;
    }

    private int findEmptySlot() {
        for (int i = 0; i < mSize; i++) {
            if (mVariables[i] == mNone) {
                return i;
            }
        }
        return -1;
    }

    private void insertVariable(int index, SolverVariable variable, float value) {
        int availableSlot = findEmptySlot();
        addVariable(availableSlot, variable, value);
        if (index != mNone) {
            mPrevious[availableSlot] = index;
            mNext[availableSlot] = mNext[index];
            mNext[index] = availableSlot;
        } else {
            mPrevious[availableSlot] = mNone;
            if (mCount > 0) {
                mNext[availableSlot] = mHead;
                mHead = availableSlot;
            } else {
                mNext[availableSlot] = mNone;
            }
        }
        if (mNext[availableSlot] != mNone) {
            mPrevious[mNext[availableSlot]] = availableSlot;
        }
        addToHashMap(variable, availableSlot);
    }

    @Override
    public void put(SolverVariable variable, float value) {
        if (DEBUG) {
            System.out.println(this + " <put> " + variable.id + " = " + value);
        }
        if (value > -sEpsilon && value < sEpsilon) {
            remove(variable, true);
            return;
        }
        if (mCount == 0) {
            addVariable(0, variable, value);
            addToHashMap(variable, 0);
            mHead = 0;
        } else {
            final int index = indexOf(variable);
            if (index != mNone) {
                mValues[index] = value;
            } else {
                if (mCount + 1 >= mSize) {
                    increaseSize();
                }
                final int count = mCount;
                int previousItem = -1;
                int j = mHead;
                for (int i = 0; i < count; i++) {
                    if (mVariables[j] == variable.id) {
                        mValues[j] = value;
                        return;
                    }
                    if (mVariables[j] < variable.id) {
                        previousItem = j;
                    }
                    j = mNext[j];
                    if (j == mNone) {
                        break;
                    }
                }
                insertVariable(previousItem, variable, value);
            }
        }
    }

    @Override
    public int sizeInBytes() {
        return 0;
    }

    @Override
    public float remove(SolverVariable v, boolean removeFromDefinition) {
        if (DEBUG) {
            System.out.println(this + " <remove> " + v.id);
        }
        int index = indexOf(v);
        if (index == mNone) {
            return 0;
        }
        removeFromHashMap(v);
        float value = mValues[index];
        if (mHead == index) {
            mHead = mNext[index];
        }
        mVariables[index] = mNone;
        if (mPrevious[index] != mNone) {
            mNext[mPrevious[index]] = mNext[index];
        }
        if (mNext[index] != mNone) {
            mPrevious[mNext[index]] = mPrevious[index];
        }
        mCount--;
        v.usageInRowCount--;
        if (removeFromDefinition) {
            v.removeFromRow(mRow);
        }
        return value;
    }

    @Override
    public void add(SolverVariable v, float value, boolean removeFromDefinition) {
        if (DEBUG) {
            System.out.println(this + " <add> " + v.id + " = " + value);
        }
        if (value > -sEpsilon && value < sEpsilon) {
            return;
        }
        final int index = indexOf(v);
        if (index == mNone) {
            put(v, value);
        } else {
            mValues[index] += value;
            if (mValues[index] > -sEpsilon && mValues[index] < sEpsilon) {
                mValues[index] = 0;
                remove(v, removeFromDefinition);
            }
        }
    }

    @Override
    public float use(ArrayRow definition, boolean removeFromDefinition) {
        float value = get(definition.mVariable);
        remove(definition.mVariable, removeFromDefinition);
        if (false) {
            ArrayRow.ArrayRowVariables definitionVariables = definition.variables;
            int definitionSize = definitionVariables.getCurrentSize();
            for (int i = 0; i < definitionSize; i++) {
                SolverVariable definitionVariable = definitionVariables.getVariable(i);
                float definitionValue = definitionVariables.get(definitionVariable);
                this.add(definitionVariable, definitionValue * value, removeFromDefinition);
            }
            return value;
        }
        SolverVariableValues localDef = (SolverVariableValues) definition.variables;
        final int definitionSize = localDef.getCurrentSize();
        int j = localDef.mHead;
        if (false) {
            for (int i = 0; i < definitionSize; i++) {
                float definitionValue = localDef.mValues[j];
                SolverVariable definitionVariable =
                        mCache.mIndexedVariables[localDef.mVariables[j]];
                add(definitionVariable, definitionValue * value, removeFromDefinition);
                j = localDef.mNext[j];
                if (j == mNone) {
                    break;
                }
            }
        } else {
            j = 0;
            for (int i = 0; j < definitionSize; i++) {
                if (localDef.mVariables[i] != mNone) {
                    float definitionValue = localDef.mValues[i];
                    SolverVariable definitionVariable =
                            mCache.mIndexedVariables[localDef.mVariables[i]];
                    add(definitionVariable, definitionValue * value, removeFromDefinition);
                    j++;
                }
            }
        }
        return value;
    }

    @Override
    public void invert() {
        final int count = mCount;
        int j = mHead;
        for (int i = 0; i < count; i++) {
            mValues[j] *= -1;
            j = mNext[j];
            if (j == mNone) {
                break;
            }
        }
    }

    @Override
    public void divideByAmount(float amount) {
        final int count = mCount;
        int j = mHead;
        for (int i = 0; i < count; i++) {
            mValues[j] /= amount;
            j = mNext[j];
            if (j == mNone) {
                break;
            }
        }
    }

}
