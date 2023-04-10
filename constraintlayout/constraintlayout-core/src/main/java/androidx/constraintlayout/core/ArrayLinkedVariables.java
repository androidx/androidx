/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * Store a set of variables and their values in an array-based linked list.
 *
 * The general idea is that we want to store a list of variables that need to be ordered,
 * space efficient, and relatively fast to maintain (add/remove).
 *
 * ArrayBackedVariables implements a sparse array, so is rather space efficient, but maintaining
 * the array sorted is costly,
 * as we spend quite a bit of time recopying parts of the array on element deletion.
 *
 * LinkedVariables implements a standard linked list structure,
 * and is able to be faster than ArrayBackedVariables
 * even though it's more costly to set up (pool of objects...),
 * as the elements removal and maintenance of the structure is a lot more efficient.
 *
 * This ArrayLinkedVariables class takes inspiration from both of the above,
 * and implement a linked list stored in several arrays.
 * This allows us to be a lot more efficient in terms of setup (no need to deal with pool
 * of objects...), resetting the structure, and insertion/deletion of elements.
 */
public class ArrayLinkedVariables implements ArrayRow.ArrayRowVariables {
    private static final boolean DEBUG = false;

    static final int NONE = -1;
    // private static final boolean FULL_NEW_CHECK = false   full validation (debug purposes)

    int mCurrentSize = 0; // current size, accessed by ArrayRow and LinearSystem

    private final ArrayRow mRow; // our owner

    // pointer to the system-wide cache, allowing access to SolverVariables
    protected final Cache mCache;

    private int mRowSize = 8; // default array size

    private SolverVariable mCandidate = null;

    // mArrayIndices point to indexes in mCache.mIndexedVariables (i.e., the SolverVariables)
    private int[] mArrayIndices = new int[mRowSize];

    // mArrayNextIndices point to indexes in mArrayIndices
    private int[] mArrayNextIndices = new int[mRowSize];

    // mArrayValues contain the associated value from mArrayIndices
    private float[] mArrayValues = new float[mRowSize];

    // mHead point to indexes in mArrayIndices
    private int mHead = NONE;

    // mLast point to indexes in mArrayIndices
    //
    // While mDidFillOnce is not set, mLast is simply incremented
    // monotonically in order to be sure to traverse the entire array; the idea here is that
    // when we clear a linked list, we only set the counters to zero without traversing the array
    // to fill it with NONE values, which would be costly.
    // But if we do not fill the array with NONE values, we cannot safely simply check if an entry
    // is set to NONE to know if we can use it or not, as it might contains a previous value...
    // So, when adding elements, we first ensure with this mechanism of mLast/mDidFillOnce
    // that we do traverse the array linearly,
    // avoiding for that first pass the need to check for the value of the item in mArrayIndices.
    // This does mean that removed elements will leave empty spaces,
    // but we /then/ set the removed element to NONE,
    // so that once we did that first traversal filling the array,
    // we can safely revert to linear traversal
    // finding an empty spot by checking the values of mArrayIndices
    // (i.e. finding an item containing NONE).
    private int mLast = NONE;

    // flag to keep trace if we did a full pass of the array or not, see above description
    private boolean mDidFillOnce = false;
    private static float sEpsilon = 0.001f;

    // Example of a basic loop
    // current or previous point to mArrayIndices
    //
    // int current = mHead;
    // int counter = 0;
    // while (current != NONE && counter < currentSize) {
    //  SolverVariable currentVariable = mCache.mIndexedVariables[mArrayIndices[current]];
    //  float currentValue = mArrayValues[current];
    //  ...
    //  current = mArrayNextIndices[current]; counter++;
    // }

    /**
     * Constructor
     *
     * @param arrayRow the row owning us
     * @param cache    instances cache
     */
    ArrayLinkedVariables(ArrayRow arrayRow, Cache cache) {
        mRow = arrayRow;
        mCache = cache;
        if (DEBUG) {
            for (int i = 0; i < mArrayIndices.length; i++) {
                mArrayIndices[i] = NONE;
            }
        }
    }

    /**
     * Insert a variable with a given value in the linked list
     *
     * @param variable the variable to add in the list
     * @param value    the value of the variable
     */
    @Override
    public final void put(SolverVariable variable, float value) {
        if (value == 0) {
            remove(variable, true);
            return;
        }
        // Special casing empty list...
        if (mHead == NONE) {
            mHead = 0;
            mArrayValues[mHead] = value;
            mArrayIndices[mHead] = variable.id;
            mArrayNextIndices[mHead] = NONE;
            variable.usageInRowCount++;
            variable.addToRow(mRow);
            mCurrentSize++;
            if (!mDidFillOnce) {
                // only increment mLast if we haven't done the first filling pass
                mLast++;
                if (mLast >= mArrayIndices.length) {
                    mDidFillOnce = true;
                    mLast = mArrayIndices.length - 1;
                }
            }
            return;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (mArrayIndices[current] == variable.id) {
                mArrayValues[current] = value;
                return;
            }
            if (mArrayIndices[current] < variable.id) {
                previous = current;
            }
            current = mArrayNextIndices[current];
            counter++;
        }

        // Not found, we need to insert

        // First, let's find an available spot
        int availableIndice = mLast + 1; // start from the previous spot
        if (mDidFillOnce) {
            // ... but if we traversed the array once, check the last index, which might have been
            // set by an element removed
            if (mArrayIndices[mLast] == NONE) {
                availableIndice = mLast;
            } else {
                availableIndice = mArrayIndices.length;
            }
        }
        if (availableIndice >= mArrayIndices.length) {
            if (mCurrentSize < mArrayIndices.length) {
                // find an available spot
                for (int i = 0; i < mArrayIndices.length; i++) {
                    if (mArrayIndices[i] == NONE) {
                        availableIndice = i;
                        break;
                    }
                }
            }
        }
        // ... make sure to grow the array as needed
        if (availableIndice >= mArrayIndices.length) {
            availableIndice = mArrayIndices.length;
            mRowSize *= 2;
            mDidFillOnce = false;
            mLast = availableIndice - 1;
            mArrayValues = Arrays.copyOf(mArrayValues, mRowSize);
            mArrayIndices = Arrays.copyOf(mArrayIndices, mRowSize);
            mArrayNextIndices = Arrays.copyOf(mArrayNextIndices, mRowSize);
        }

        // Finally, let's insert the element
        mArrayIndices[availableIndice] = variable.id;
        mArrayValues[availableIndice] = value;
        if (previous != NONE) {
            mArrayNextIndices[availableIndice] = mArrayNextIndices[previous];
            mArrayNextIndices[previous] = availableIndice;
        } else {
            mArrayNextIndices[availableIndice] = mHead;
            mHead = availableIndice;
        }
        variable.usageInRowCount++;
        variable.addToRow(mRow);
        mCurrentSize++;
        if (!mDidFillOnce) {
            // only increment mLast if we haven't done the first filling pass
            mLast++;
        }
        if (mCurrentSize >= mArrayIndices.length) {
            mDidFillOnce = true;
        }
        if (mLast >= mArrayIndices.length) {
            mDidFillOnce = true;
            mLast = mArrayIndices.length - 1;
        }
    }

    /**
     * Add value to an existing variable
     *
     * The code is broadly identical to the put() method, only differing
     * in in-line deletion, and of course doing an add rather than a put
     *
     * @param variable the variable we want to add
     * @param value    its value
     */
    @Override
    public void add(SolverVariable variable, float value, boolean removeFromDefinition) {
        if (value > -sEpsilon && value < sEpsilon) {
            return;
        }
        // Special casing empty list...
        if (mHead == NONE) {
            mHead = 0;
            mArrayValues[mHead] = value;
            mArrayIndices[mHead] = variable.id;
            mArrayNextIndices[mHead] = NONE;
            variable.usageInRowCount++;
            variable.addToRow(mRow);
            mCurrentSize++;
            if (!mDidFillOnce) {
                // only increment mLast if we haven't done the first filling pass
                mLast++;
                if (mLast >= mArrayIndices.length) {
                    mDidFillOnce = true;
                    mLast = mArrayIndices.length - 1;
                }
            }
            return;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            int idx = mArrayIndices[current];
            if (idx == variable.id) {
                float v = mArrayValues[current] + value;
                if (v > -sEpsilon && v < sEpsilon) {
                    v = 0;
                }
                mArrayValues[current] = v;
                // Possibly delete immediately
                if (v == 0) {
                    if (current == mHead) {
                        mHead = mArrayNextIndices[current];
                    } else {
                        mArrayNextIndices[previous] = mArrayNextIndices[current];
                    }
                    if (removeFromDefinition) {
                        variable.removeFromRow(mRow);
                    }
                    if (mDidFillOnce) {
                        // If we did a full pass already, remember that spot
                        mLast = current;
                    }
                    variable.usageInRowCount--;
                    mCurrentSize--;
                }
                return;
            }
            if (mArrayIndices[current] < variable.id) {
                previous = current;
            }
            current = mArrayNextIndices[current];
            counter++;
        }

        // Not found, we need to insert

        // First, let's find an available spot
        int availableIndice = mLast + 1; // start from the previous spot
        if (mDidFillOnce) {
            // ... but if we traversed the array once, check the last index, which might have been
            // set by an element removed
            if (mArrayIndices[mLast] == NONE) {
                availableIndice = mLast;
            } else {
                availableIndice = mArrayIndices.length;
            }
        }
        if (availableIndice >= mArrayIndices.length) {
            if (mCurrentSize < mArrayIndices.length) {
                // find an available spot
                for (int i = 0; i < mArrayIndices.length; i++) {
                    if (mArrayIndices[i] == NONE) {
                        availableIndice = i;
                        break;
                    }
                }
            }
        }
        // ... make sure to grow the array as needed
        if (availableIndice >= mArrayIndices.length) {
            availableIndice = mArrayIndices.length;
            mRowSize *= 2;
            mDidFillOnce = false;
            mLast = availableIndice - 1;
            mArrayValues = Arrays.copyOf(mArrayValues, mRowSize);
            mArrayIndices = Arrays.copyOf(mArrayIndices, mRowSize);
            mArrayNextIndices = Arrays.copyOf(mArrayNextIndices, mRowSize);
        }

        // Finally, let's insert the element
        mArrayIndices[availableIndice] = variable.id;
        mArrayValues[availableIndice] = value;
        if (previous != NONE) {
            mArrayNextIndices[availableIndice] = mArrayNextIndices[previous];
            mArrayNextIndices[previous] = availableIndice;
        } else {
            mArrayNextIndices[availableIndice] = mHead;
            mHead = availableIndice;
        }
        variable.usageInRowCount++;
        variable.addToRow(mRow);
        mCurrentSize++;
        if (!mDidFillOnce) {
            // only increment mLast if we haven't done the first filling pass
            mLast++;
        }
        if (mLast >= mArrayIndices.length) {
            mDidFillOnce = true;
            mLast = mArrayIndices.length - 1;
        }
    }

    /**
     * Update the current list with a new definition
     *
     * @param definition the row containing the definition
     */
    @Override
    public float use(ArrayRow definition, boolean removeFromDefinition) {
        float value = get(definition.mVariable);
        remove(definition.mVariable, removeFromDefinition);
        ArrayRow.ArrayRowVariables definitionVariables = definition.variables;
        int definitionSize = definitionVariables.getCurrentSize();
        for (int i = 0; i < definitionSize; i++) {
            SolverVariable definitionVariable = definitionVariables.getVariable(i);
            float definitionValue = definitionVariables.get(definitionVariable);
            this.add(definitionVariable, definitionValue * value, removeFromDefinition);
        }
        return value;
    }

    /**
     * Remove a variable from the list
     *
     * @param variable the variable we want to remove
     * @return the value of the removed variable
     */
    @Override
    public final float remove(SolverVariable variable, boolean removeFromDefinition) {
        if (mCandidate == variable) {
            mCandidate = null;
        }
        if (mHead == NONE) {
            return 0;
        }
        int current = mHead;
        int previous = NONE;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            int idx = mArrayIndices[current];
            if (idx == variable.id) {
                if (current == mHead) {
                    mHead = mArrayNextIndices[current];
                } else {
                    mArrayNextIndices[previous] = mArrayNextIndices[current];
                }

                if (removeFromDefinition) {
                    variable.removeFromRow(mRow);
                }
                variable.usageInRowCount--;
                mCurrentSize--;
                mArrayIndices[current] = NONE;
                if (mDidFillOnce) {
                    // If we did a full pass already, remember that spot
                    mLast = current;
                }
                return mArrayValues[current];
            }
            previous = current;
            current = mArrayNextIndices[current];
            counter++;
        }
        return 0;
    }

    /**
     * Clear the list of variables
     */
    @Override
    public final void clear() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            SolverVariable variable = mCache.mIndexedVariables[mArrayIndices[current]];
            if (variable != null) {
                variable.removeFromRow(mRow);
            }
            current = mArrayNextIndices[current];
            counter++;
        }

        mHead = NONE;
        mLast = NONE;
        mDidFillOnce = false;
        mCurrentSize = 0;
    }

    /**
     * Returns true if the variable is contained in the list
     *
     * @param variable the variable we are looking for
     * @return return true if we found the variable
     */
    @Override
    public boolean contains(SolverVariable variable) {
        if (mHead == NONE) {
            return false;
        }
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (mArrayIndices[current] == variable.id) {
                return true;
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return false;
    }

    @Override
    public int indexOf(SolverVariable variable) {
        if (mHead == NONE) {
            return -1;
        }
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (mArrayIndices[current] == variable.id) {
                return current;
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return -1;
    }


    /**
     * Returns true if at least one of the variable is positive
     *
     * @return true if at least one of the variable is positive
     */
    boolean hasAtLeastOnePositiveVariable() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (mArrayValues[current] > 0) {
                return true;
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return false;
    }

    /**
     * Invert the values of all the variables in the list
     */
    @Override
    public void invert() {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            mArrayValues[current] *= -1;
            current = mArrayNextIndices[current];
            counter++;
        }
    }

    /**
     * Divide the values of all the variables in the list
     * by the given amount
     *
     * @param amount amount to divide by
     */
    @Override
    public void divideByAmount(float amount) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            mArrayValues[current] /= amount;
            current = mArrayNextIndices[current];
            counter++;
        }
    }

    public int getHead() {
        return mHead;
    }

    @Override
    public int getCurrentSize() {
        return mCurrentSize;
    }

    /**
     * get Id in mCache.mIndexedVariables given the index
     */
    public final int getId(int index) {
        return mArrayIndices[index];
    }

    /**
     * get value in mArrayValues given the index
     */
    public final float getValue(int index) {
        return mArrayValues[index];
    }

    /**
     * Get the next index in mArrayIndices given the current one
     */
    public final int getNextIndice(int index) {
        return mArrayNextIndices[index];
    }

    /**
     * TODO: check if still needed
     * Return a pivot candidate
     *
     * @return return a variable we can pivot on
     */
    SolverVariable getPivotCandidate() {
        if (mCandidate == null) {
            // if no candidate is known, let's figure it out
            int current = mHead;
            int counter = 0;
            SolverVariable pivot = null;
            while (current != NONE && counter < mCurrentSize) {
                if (mArrayValues[current] < 0) {
                    // We can return the first negative candidate as in ArrayLinkedVariables
                    // they are already sorted by id

                    SolverVariable v = mCache.mIndexedVariables[mArrayIndices[current]];
                    if (pivot == null || pivot.strength < v.strength) {
                        pivot = v;
                    }
                }
                current = mArrayNextIndices[current];
                counter++;
            }
            return pivot;
        }
        return mCandidate;
    }

    /**
     * Return a variable from its position in the linked list
     *
     * @param index the index of the variable we want to return
     * @return the variable found, or null
     */
    @Override
    public SolverVariable getVariable(int index) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (counter == index) {
                return mCache.mIndexedVariables[mArrayIndices[current]];
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return null;
    }

    /**
     * Return the value of a variable from its position in the linked list
     *
     * @param index the index of the variable we want to look up
     * @return the value of the found variable, or 0 if not found
     */
    @Override
    public float getVariableValue(int index) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (counter == index) {
                return mArrayValues[current];
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return 0;
    }

    /**
     * Return the value of a variable, 0 if not found
     *
     * @param v the variable we are looking up
     * @return the value of the found variable, or 0 if not found
     */
    @Override
    public final float get(SolverVariable v) {
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            if (mArrayIndices[current] == v.id) {
                return mArrayValues[current];
            }
            current = mArrayNextIndices[current];
            counter++;
        }
        return 0;
    }

    /**
     * Show size in bytes
     *
     * @return size in bytes
     */
    @Override
    public int sizeInBytes() {
        int size = 0;
        size += 3 * (mArrayIndices.length * 4);
        size += 9 * 4;
        return size;
    }

    /**
     * print out the variables and their values
     */
    @Override
    public void display() {
        int count = mCurrentSize;
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

    /**
     * Returns a string representation of the list
     *
     * @return a string containing a representation of the list
     */
    @Override
    public String toString() {
        String result = "";
        int current = mHead;
        int counter = 0;
        while (current != NONE && counter < mCurrentSize) {
            result += " -> ";
            result += mArrayValues[current] + " : ";
            result += mCache.mIndexedVariables[mArrayIndices[current]];
            current = mArrayNextIndices[current];
            counter++;
        }
        return result;
    }

}
