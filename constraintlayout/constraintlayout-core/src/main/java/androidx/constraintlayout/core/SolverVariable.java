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

import static androidx.constraintlayout.core.LinearSystem.FULL_DEBUG;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Represents a given variable used in the {@link LinearSystem linear expression solver}.
 */
public class SolverVariable implements Comparable<SolverVariable> {

    private static final boolean INTERNAL_DEBUG = FULL_DEBUG;
    private static final boolean VAR_USE_HASH = false;
    private static final boolean DO_NOT_USE = false;


    @SuppressWarnings("WeakerAccess")
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_LOW = 1;
    public static final int STRENGTH_MEDIUM = 2;
    public static final int STRENGTH_HIGH = 3;
    @SuppressWarnings("WeakerAccess")
    public static final int STRENGTH_HIGHEST = 4;
    public static final int STRENGTH_EQUALITY = 5;
    public static final int STRENGTH_BARRIER = 6;
    public static final int STRENGTH_CENTERING = 7;
    public static final int STRENGTH_FIXED = 8;

    private static int sUniqueSlackId = 1;
    private static int sUniqueErrorId = 1;
    private static int sUniqueUnrestrictedId = 1;
    private static int sUniqueConstantId = 1;
    private static int sUniqueId = 1;
    public boolean inGoal;

    private String mName;

    public int id = -1;
    int mDefinitionId = -1;
    public int strength = 0;
    public float computedValue;
    public boolean isFinalValue = false;

    static final int MAX_STRENGTH = 9;
    float[] mStrengthVector = new float[MAX_STRENGTH];
    float[] mGoalStrengthVector = new float[MAX_STRENGTH];

    Type mType;

    ArrayRow[] mClientEquations = new ArrayRow[16];
    int mClientEquationsCount = 0;
    public int usageInRowCount = 0;
    boolean mIsSynonym = false;
    int mSynonym = -1;
    float mSynonymDelta = 0;

    /**
     * Type of variables
     */
    public enum Type {
        /**
         * The variable can take negative or positive values
         */
        UNRESTRICTED,
        /**
         * The variable is actually not a variable :) , but a constant number
         */
        CONSTANT,
        /**
         * The variable is restricted to positive values and represents a slack
         */
        SLACK,
        /**
         * The variable is restricted to positive values and represents an error
         */
        ERROR,
        /**
         * Unknown (invalid) type.
         */
        UNKNOWN
    }

    static void increaseErrorId() {
        sUniqueErrorId++;
    }

    private static String getUniqueName(Type type, String prefix) {
        if (prefix != null) {
            return prefix + sUniqueErrorId;
        }
        switch (type) {
            case UNRESTRICTED:
                return "U" + ++sUniqueUnrestrictedId;
            case CONSTANT:
                return "C" + ++sUniqueConstantId;
            case SLACK:
                return "S" + ++sUniqueSlackId;
            case ERROR: {
                return "e" + ++sUniqueErrorId;
            }
            case UNKNOWN:
                return "V" + ++sUniqueId;
        }
        throw new AssertionError(type.name());
    }

    /**
     * Base constructor
     *
     * @param name the variable name
     * @param type the type of the variable
     */
    public SolverVariable(String name, Type type) {
        mName = name;
        mType = type;
    }

    public SolverVariable(Type type, String prefix) {
        mType = type;
        if (INTERNAL_DEBUG) {
            //mName = getUniqueName(type, prefix);
        }
    }

    void clearStrengths() {
        for (int i = 0; i < MAX_STRENGTH; i++) {
            mStrengthVector[i] = 0;
        }
    }

    String strengthsToString() {
        String representation = this + "[";
        boolean negative = false;
        boolean empty = true;
        for (int j = 0; j < mStrengthVector.length; j++) {
            representation += mStrengthVector[j];
            if (mStrengthVector[j] > 0) {
                negative = false;
            } else if (mStrengthVector[j] < 0) {
                negative = true;
            }
            if (mStrengthVector[j] != 0) {
                empty = false;
            }
            if (j < mStrengthVector.length - 1) {
                representation += ", ";
            } else {
                representation += "] ";
            }
        }
        if (negative) {
            representation += " (-)";
        }
        if (empty) {
            representation += " (*)";
        }
        // representation += " {id: " + id + "}";
        return representation;
    }

    HashSet<ArrayRow> mInRows = VAR_USE_HASH ? new HashSet<ArrayRow>() : null;

    // @TODO: add description
    public final void addToRow(ArrayRow row) {
        if (VAR_USE_HASH) {
            mInRows.add(row);
        } else {
            for (int i = 0; i < mClientEquationsCount; i++) {
                if (mClientEquations[i] == row) {
                    return;
                }
            }
            if (mClientEquationsCount >= mClientEquations.length) {
                mClientEquations = Arrays.copyOf(mClientEquations, mClientEquations.length * 2);
            }
            mClientEquations[mClientEquationsCount] = row;
            mClientEquationsCount++;
        }
    }

    // @TODO: add description
    public final void removeFromRow(ArrayRow row) {
        if (VAR_USE_HASH) {
            mInRows.remove(row);
        } else {
            final int count = mClientEquationsCount;
            for (int i = 0; i < count; i++) {
                if (mClientEquations[i] == row) {
                    for (int j = i; j < count - 1; j++) {
                        mClientEquations[j] = mClientEquations[j + 1];
                    }
                    mClientEquationsCount--;
                    return;
                }
            }
        }
    }

    // @TODO: add description
    public final void updateReferencesWithNewDefinition(LinearSystem system, ArrayRow definition) {
        if (VAR_USE_HASH) {
            for (ArrayRow row : mInRows) {
                row.updateFromRow(system, definition, false);
            }
            mInRows.clear();
        } else {
            final int count = mClientEquationsCount;
            for (int i = 0; i < count; i++) {
                mClientEquations[i].updateFromRow(system, definition, false);
            }
            mClientEquationsCount = 0;
        }
    }

    // @TODO: add description
    public void setFinalValue(LinearSystem system, float value) {
        if (DO_NOT_USE && INTERNAL_DEBUG) {
            System.out.println("Set final value for " + this + " of " + value);
        }
        computedValue = value;
        isFinalValue = true;
        mIsSynonym = false;
        mSynonym = -1;
        mSynonymDelta = 0;
        final int count = mClientEquationsCount;
        mDefinitionId = -1;
        for (int i = 0; i < count; i++) {
            mClientEquations[i].updateFromFinalVariable(system, this, false);
        }
        mClientEquationsCount = 0;
    }

    // @TODO: add description
    public void setSynonym(LinearSystem system, SolverVariable synonymVariable, float value) {
        if (INTERNAL_DEBUG) {
            System.out.println("Set synonym for " + this + " = " + synonymVariable + " + " + value);
        }
        mIsSynonym = true;
        mSynonym = synonymVariable.id;
        mSynonymDelta = value;
        final int count = mClientEquationsCount;
        mDefinitionId = -1;
        for (int i = 0; i < count; i++) {
            mClientEquations[i].updateFromSynonymVariable(system, this, false);
        }
        mClientEquationsCount = 0;
        system.displayReadableRows();
    }

    // @TODO: add description
    public void reset() {
        mName = null;
        mType = Type.UNKNOWN;
        strength = SolverVariable.STRENGTH_NONE;
        id = -1;
        mDefinitionId = -1;
        computedValue = 0;
        isFinalValue = false;
        mIsSynonym = false;
        mSynonym = -1;
        mSynonymDelta = 0;
        if (VAR_USE_HASH) {
            mInRows.clear();
        } else {
            final int count = mClientEquationsCount;
            for (int i = 0; i < count; i++) {
                mClientEquations[i] = null;
            }
            mClientEquationsCount = 0;
        }
        usageInRowCount = 0;
        inGoal = false;
        Arrays.fill(mGoalStrengthVector, 0);
    }

    /**
     * Accessor for the name
     *
     * @return the name of the variable
     */
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    // @TODO: add description
    public void setType(Type type, String prefix) {
        mType = type;
        if (INTERNAL_DEBUG && mName == null) {
            mName = getUniqueName(type, prefix);
        }
    }

    @Override
    public int compareTo(SolverVariable v) {
        return this.id - v.id;
    }

    /**
     * Override the toString() method to display the variable
     */
    @Override
    public String toString() {
        String result = "";
        if (INTERNAL_DEBUG) {
            result += mName + "(" + id + "):" + strength;
            if (mIsSynonym) {
                result += ":S(" + mSynonym + ")";
            }
            if (isFinalValue) {
                result += ":F(" + computedValue + ")";
            }
        } else {
            if (mName != null) {
                result += mName;
            } else {
                result += id;
            }
        }
        return result;
    }

}
