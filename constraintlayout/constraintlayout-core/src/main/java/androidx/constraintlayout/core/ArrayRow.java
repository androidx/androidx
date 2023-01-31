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

import static androidx.constraintlayout.core.SolverVariable.STRENGTH_EQUALITY;
import static androidx.constraintlayout.core.SolverVariable.STRENGTH_HIGH;
import static androidx.constraintlayout.core.SolverVariable.STRENGTH_HIGHEST;
import static androidx.constraintlayout.core.SolverVariable.STRENGTH_LOW;
import static androidx.constraintlayout.core.SolverVariable.STRENGTH_MEDIUM;

import java.util.ArrayList;

public class ArrayRow implements LinearSystem.Row {
    private static final boolean DEBUG = false;

    SolverVariable mVariable = null;
    float mConstantValue = 0;
    boolean mUsed = false;
    private static final boolean FULL_NEW_CHECK = false; // full validation (debug purposes)

    ArrayList<SolverVariable> mVariablesToUpdate = new ArrayList<>();

    public ArrayRowVariables variables;

    public interface ArrayRowVariables {

        // @TODO: add description
        int getCurrentSize();

        // @TODO: add description
        SolverVariable getVariable(int index);

        // @TODO: add description
        float getVariableValue(int index);

        // @TODO: add description
        float get(SolverVariable variable);

        // @TODO: add description
        int indexOf(SolverVariable variable);

        // @TODO: add description
        void display();

        // @TODO: add description
        void clear();

        // @TODO: add description
        boolean contains(SolverVariable variable);

        // @TODO: add description
        void put(SolverVariable variable, float value);

        // @TODO: add description
        int sizeInBytes();

        // @TODO: add description
        void invert();

        // @TODO: add description
        float remove(SolverVariable v, boolean removeFromDefinition);

        // @TODO: add description
        void divideByAmount(float amount);

        // @TODO: add description
        void add(SolverVariable v, float value, boolean removeFromDefinition);

        // @TODO: add description
        float use(ArrayRow definition, boolean removeFromDefinition);
    }

    boolean mIsSimpleDefinition = false;

    public ArrayRow() {
    }

    public ArrayRow(Cache cache) {
        variables = new ArrayLinkedVariables(this, cache);
        //variables = new OptimizedSolverVariableValues(this, cache);
    }

    boolean hasKeyVariable() {
        return !(
                (mVariable == null)
                        || (mVariable.mType != SolverVariable.Type.UNRESTRICTED
                        && mConstantValue < 0)
        );
    }

    // @TODO: add description
    @Override
    public String toString() {
        return toReadableString();
    }

    String toReadableString() {
        String s = "";
        if (mVariable == null) {
            s += "0";
        } else {
            s += mVariable;
        }
        s += " = ";
        boolean addedVariable = false;
        if (mConstantValue != 0) {
            s += mConstantValue;
            addedVariable = true;
        }
        int count = variables.getCurrentSize();
        for (int i = 0; i < count; i++) {
            SolverVariable v = variables.getVariable(i);
            if (v == null) {
                continue;
            }
            float amount = variables.getVariableValue(i);
            if (amount == 0) {
                continue;
            }
            String name = v.toString();
            if (!addedVariable) {
                if (amount < 0) {
                    s += "- ";
                    amount *= -1;
                }
            } else {
                if (amount > 0) {
                    s += " + ";
                } else {
                    s += " - ";
                    amount *= -1;
                }
            }
            if (amount == 1) {
                s += name;
            } else {
                s += amount + " " + name;
            }
            addedVariable = true;
        }
        if (!addedVariable) {
            s += "0.0";
        }
        if (DEBUG) {
            variables.display();
        }
        return s;
    }

    // @TODO: add description
    public void reset() {
        mVariable = null;
        variables.clear();
        mConstantValue = 0;
        mIsSimpleDefinition = false;
    }

    boolean hasVariable(SolverVariable v) {
        return variables.contains(v);
    }

    ArrayRow createRowDefinition(SolverVariable variable, int value) {
        this.mVariable = variable;
        variable.computedValue = value;
        mConstantValue = value;
        mIsSimpleDefinition = true;
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowEquals(SolverVariable variable, int value) {
        if (value < 0) {
            mConstantValue = -1 * value;
            variables.put(variable, 1);
        } else {
            mConstantValue = value;
            variables.put(variable, -1);
        }
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowEquals(SolverVariable variableA,
            SolverVariable variableB,
            int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            mConstantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
        }
        return this;
    }

    // @TODO: add description
    ArrayRow addSingleError(SolverVariable error, int sign) {
        variables.put(error, (float) sign);
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowGreaterThan(SolverVariable variableA,
            SolverVariable variableB, SolverVariable slack,
            int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            mConstantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            variables.put(slack, 1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
            variables.put(slack, -1);
        }
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowGreaterThan(SolverVariable a, int b, SolverVariable slack) {
        mConstantValue = b;
        variables.put(a, -1);
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowLowerThan(SolverVariable variableA, SolverVariable variableB,
            SolverVariable slack, int margin) {
        boolean inverse = false;
        if (margin != 0) {
            int m = margin;
            if (m < 0) {
                m = -1 * m;
                inverse = true;
            }
            mConstantValue = m;
        }
        if (!inverse) {
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            variables.put(slack, -1);
        } else {
            variables.put(variableA, 1);
            variables.put(variableB, -1);
            variables.put(slack, 1);
        }
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowEqualMatchDimensions(float currentWeight,
            float totalWeights, float nextWeight,
            SolverVariable variableStartA,
            SolverVariable variableEndA,
            SolverVariable variableStartB,
            SolverVariable variableEndB) {
        mConstantValue = 0;
        if (totalWeights == 0 || (currentWeight == nextWeight)) {
            // endA - startA == endB - startB
            // 0 = startA - endA + endB - startB
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, 1);
            variables.put(variableStartB, -1);
        } else {
            if (currentWeight == 0) {
                variables.put(variableStartA, 1);
                variables.put(variableEndA, -1);
            } else if (nextWeight == 0) {
                variables.put(variableStartB, 1);
                variables.put(variableEndB, -1);
            } else {
                float cw = currentWeight / totalWeights;
                float nw = nextWeight / totalWeights;
                float w = cw / nw;

                // endA - startA == w * (endB - startB)
                // 0 = startA - endA + w * (endB - startB)
                variables.put(variableStartA, 1);
                variables.put(variableEndA, -1);
                variables.put(variableEndB, w);
                variables.put(variableStartB, -w);
            }
        }
        return this;
    }

    // @TODO: add description
    public ArrayRow createRowEqualDimension(float currentWeight,
            float totalWeights, float nextWeight,
            SolverVariable variableStartA, int marginStartA,
            SolverVariable variableEndA, int marginEndA,
            SolverVariable variableStartB, int marginStartB,
            SolverVariable variableEndB, int marginEndB) {
        if (totalWeights == 0 || (currentWeight == nextWeight)) {
            // endA - startA + marginStartA + marginEndA
            //      == endB - startB + marginStartB + marginEndB
            // 0 = startA - endA - marginStartA - marginEndA
            //      + endB - startB + marginStartB + marginEndB
            // 0 = (- marginStartA - marginEndA + marginStartB + marginEndB)
            //      + startA - endA + endB - startB
            mConstantValue = -marginStartA - marginEndA + marginStartB + marginEndB;
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, 1);
            variables.put(variableStartB, -1);
        } else {
            float cw = currentWeight / totalWeights;
            float nw = nextWeight / totalWeights;
            float w = cw / nw;
            // (endA - startA + marginStartA + marginEndA) =
            //      w * (endB - startB) + marginStartB + marginEndB;
            // 0 = (startA - endA - marginStartA - marginEndA)
            //      + w * (endB - startB) + marginStartB + marginEndB
            // 0 = (- marginStartA - marginEndA + marginStartB + marginEndB)
            //      + startA - endA + w * endB - w * startB
            mConstantValue = -marginStartA - marginEndA + w * marginStartB + w * marginEndB;
            variables.put(variableStartA, 1);
            variables.put(variableEndA, -1);
            variables.put(variableEndB, w);
            variables.put(variableStartB, -w);
        }
        return this;
    }

    ArrayRow createRowCentering(SolverVariable variableA, SolverVariable variableB,
            int marginA, float bias, SolverVariable variableC,
            SolverVariable variableD, int marginB) {
        if (variableB == variableC) {
            // centering on the same position
            // B - A == D - B
            // 0 = A + D - 2 * B
            variables.put(variableA, 1);
            variables.put(variableD, 1);
            variables.put(variableB, -2);
            return this;
        }
        if (bias == 0.5f) {
            // don't bother applying the bias, we are centered
            // A - B = C - D
            // 0 = A - B - C + D
            // with margin:
            // A - B - Ma = C - D - Mb
            // 0 = A - B - C + D - Ma + Mb
            variables.put(variableA, 1f);
            variables.put(variableB, -1f);
            variables.put(variableC, -1f);
            variables.put(variableD, 1f);
            if (marginA > 0 || marginB > 0) {
                mConstantValue = -marginA + marginB;
            }
        } else if (bias <= 0) {
            // A = B + m
            variables.put(variableA, -1);
            variables.put(variableB, 1);
            mConstantValue = marginA;
        } else if (bias >= 1) {
            // D = C - m
            variables.put(variableD, -1);
            variables.put(variableC, 1);
            mConstantValue = -marginB;
        } else {
            variables.put(variableA, 1 * (1 - bias));
            variables.put(variableB, -1 * (1 - bias));
            variables.put(variableC, -1 * bias);
            variables.put(variableD, 1 * bias);
            if (marginA > 0 || marginB > 0) {
                mConstantValue = -marginA * (1 - bias) + marginB * bias;
            }
        }
        return this;
    }

    // @TODO: add description
    public ArrayRow addError(LinearSystem system, int strength) {
        variables.put(system.createErrorVariable(strength, "ep"), 1);
        variables.put(system.createErrorVariable(strength, "em"), -1);
        return this;
    }

    ArrayRow createRowDimensionPercent(SolverVariable variableA,
            SolverVariable variableC, float percent) {
        variables.put(variableA, -1);
        variables.put(variableC, percent);
        return this;
    }

    /**
     * Create a constraint to express {@code A = B + (C - D)} * ratio
     * We use this for ratio, where for example {@code Right = Left + (Bottom - Top) * percent}
     *
     * @param variableA variable A
     * @param variableB variable B
     * @param variableC variable C
     * @param variableD variable D
     * @param ratio     ratio between AB and CD
     * @return the row
     */
    public ArrayRow createRowDimensionRatio(SolverVariable variableA, SolverVariable variableB,
            SolverVariable variableC, SolverVariable variableD,
            float ratio) {
        // A = B + (C - D) * ratio
        variables.put(variableA, -1);
        variables.put(variableB, 1);
        variables.put(variableC, ratio);
        variables.put(variableD, -ratio);
        return this;
    }

    /**
     * Create a constraint to express At + (Ab-At)/2 = Bt + (Bb-Bt)/2 - angle
     */
    public ArrayRow createRowWithAngle(SolverVariable at,
            SolverVariable ab,
            SolverVariable bt,
            SolverVariable bb,
            float angleComponent) {
        variables.put(bt, 0.5f);
        variables.put(bb, 0.5f);
        variables.put(at, -0.5f);
        variables.put(ab, -0.5f);
        mConstantValue = -angleComponent;
        return this;
    }

    int sizeInBytes() {
        int size = 0;
        if (mVariable != null) {
            size += 4; // object
        }
        size += 4; // constantValue
        size += 4; // used

        size += variables.sizeInBytes();
        return size;
    }

    void ensurePositiveConstant() {
        // Ensure that if we have a constant it's positive
        if (mConstantValue < 0) {
            // If not, simply multiply the equation by -1
            mConstantValue *= -1;
            variables.invert();
        }
    }

    /**
     * Pick a subject variable out of the existing ones.
     * - if a variable is unrestricted
     * - or if it's a negative new variable (not found elsewhere)
     * - otherwise we have to add a new additional variable
     *
     * @return true if we added an extra variable to the system
     */
    boolean chooseSubject(LinearSystem system) {
        boolean addedExtra = false;
        SolverVariable pivotCandidate = chooseSubjectInVariables(system);
        if (pivotCandidate == null) {
            // need to add extra variable
            addedExtra = true;
        } else {
            pivot(pivotCandidate);
        }
        if (variables.getCurrentSize() == 0) {
            mIsSimpleDefinition = true;
        }
        return addedExtra;
    }

    /**
     * Pick a subject variable out of the existing ones.
     * - if a variable is unrestricted
     * - or if it's a negative new variable (not found elsewhere)
     * - otherwise we return null
     *
     * @return a candidate variable we can pivot on or null if not found
     */
    SolverVariable chooseSubjectInVariables(LinearSystem system) {
        // if unrestricted, pick it
        // if restricted, needs to be < 0 and new
        //
        SolverVariable restrictedCandidate = null;
        SolverVariable unrestrictedCandidate = null;
        float unrestrictedCandidateAmount = 0;
        float restrictedCandidateAmount = 0;
        boolean unrestrictedCandidateIsNew = false;
        boolean restrictedCandidateIsNew = false;

        final int currentSize = variables.getCurrentSize();
        for (int i = 0; i < currentSize; i++) {
            float amount = variables.getVariableValue(i);
            SolverVariable variable = variables.getVariable(i);
            if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                if (unrestrictedCandidate == null) {
                    unrestrictedCandidate = variable;
                    unrestrictedCandidateAmount = amount;
                    unrestrictedCandidateIsNew = isNew(variable, system);
                } else if (unrestrictedCandidateAmount > amount) {
                    unrestrictedCandidate = variable;
                    unrestrictedCandidateAmount = amount;
                    unrestrictedCandidateIsNew = isNew(variable, system);
                } else if (!unrestrictedCandidateIsNew && isNew(variable, system)) {
                    unrestrictedCandidate = variable;
                    unrestrictedCandidateAmount = amount;
                    unrestrictedCandidateIsNew = true;
                }
            } else if (unrestrictedCandidate == null) {
                if (amount < 0) {
                    if (restrictedCandidate == null) {
                        restrictedCandidate = variable;
                        restrictedCandidateAmount = amount;
                        restrictedCandidateIsNew = isNew(variable, system);
                    } else if (restrictedCandidateAmount > amount) {
                        restrictedCandidate = variable;
                        restrictedCandidateAmount = amount;
                        restrictedCandidateIsNew = isNew(variable, system);
                    } else if (!restrictedCandidateIsNew && isNew(variable, system)) {
                        restrictedCandidate = variable;
                        restrictedCandidateAmount = amount;
                        restrictedCandidateIsNew = true;
                    }
                }
            }
        }

        if (unrestrictedCandidate != null) {
            return unrestrictedCandidate;
        }
        return restrictedCandidate;
    }

    /**
     * Returns true if the variable is new to the system, i.e. is already present
     * in one of the rows. This function is called while choosing the subject of a new row.
     *
     * @param variable the variable to check for
     * @param system   the linear system we check
     */
    private boolean isNew(SolverVariable variable, LinearSystem system) {
        if (FULL_NEW_CHECK) {
            boolean isNew = true;
            for (int i = 0; i < system.mNumRows; i++) {
                ArrayRow row = system.mRows[i];
                if (row.hasVariable(variable)) {
                    isNew = false;
                }
            }
            if (variable.usageInRowCount <= 1 != isNew) {
                System.out.println("Problem with usage tracking");
            }
            return isNew;
        }
        // We maintain a usage count -- variables are ref counted if they are present
        // in the right side of a row or not. If the count is zero or one, the variable
        // is new (if one, it means it exist in a row, but this is the row we insert)
        return variable.usageInRowCount <= 1;
    }

    void pivot(SolverVariable v) {
        if (mVariable != null) {
            // first, move back the variable to its column
            variables.put(mVariable, -1f);
            mVariable.mDefinitionId = -1;
            mVariable = null;
        }

        float amount = variables.remove(v, true) * -1;
        mVariable = v;
        if (amount == 1) {
            return;
        }
        mConstantValue = mConstantValue / amount;
        variables.divideByAmount(amount);
    }

    // Row compatibility

    @Override
    public boolean isEmpty() {
        return (mVariable == null && mConstantValue == 0 && variables.getCurrentSize() == 0);
    }

    @Override
    public void updateFromRow(LinearSystem system,
            ArrayRow definition,
            boolean removeFromDefinition) {
        float value = variables.use(definition, removeFromDefinition);

        mConstantValue += definition.mConstantValue * value;
        if (removeFromDefinition) {
            definition.mVariable.removeFromRow(this);
        }
        if (LinearSystem.SIMPLIFY_SYNONYMS
                && mVariable != null && variables.getCurrentSize() == 0) {
            mIsSimpleDefinition = true;
            system.hasSimpleDefinition = true;
        }
    }

    // @TODO: add description
    @Override
    public void updateFromFinalVariable(LinearSystem system,
            SolverVariable variable,
            boolean removeFromDefinition) {
        if (variable == null || !variable.isFinalValue) {
            return;
        }
        float value = variables.get(variable);
        mConstantValue += variable.computedValue * value;
        variables.remove(variable, removeFromDefinition);
        if (removeFromDefinition) {
            variable.removeFromRow(this);
        }
        if (LinearSystem.SIMPLIFY_SYNONYMS
                && variables.getCurrentSize() == 0) {
            mIsSimpleDefinition = true;
            system.hasSimpleDefinition = true;
        }
    }

    // @TODO: add description
    public void updateFromSynonymVariable(LinearSystem system,
            SolverVariable variable,
            boolean removeFromDefinition) {
        if (variable == null || !variable.mIsSynonym) {
            return;
        }
        float value = variables.get(variable);
        mConstantValue += variable.mSynonymDelta * value;
        variables.remove(variable, removeFromDefinition);
        if (removeFromDefinition) {
            variable.removeFromRow(this);
        }
        variables.add(system.mCache.mIndexedVariables[variable.mSynonym],
                value, removeFromDefinition);
        if (LinearSystem.SIMPLIFY_SYNONYMS
                && variables.getCurrentSize() == 0) {
            mIsSimpleDefinition = true;
            system.hasSimpleDefinition = true;
        }
    }

    private SolverVariable pickPivotInVariables(boolean[] avoid, SolverVariable exclude) {
        boolean all = true;
        float value = 0;
        SolverVariable pivot = null;
        SolverVariable pivotSlack = null;
        float valueSlack = 0;

        final int currentSize = variables.getCurrentSize();
        for (int i = 0; i < currentSize; i++) {
            float currentValue = variables.getVariableValue(i);
            if (currentValue < 0) {
                // We can return the first negative candidate as in ArrayLinkedVariables
                // they are already sorted by id
                SolverVariable v = variables.getVariable(i);
                if (!((avoid != null && avoid[v.id]) || (v == exclude))) {
                    if (all) {
                        if (v.mType == SolverVariable.Type.SLACK
                                || v.mType == SolverVariable.Type.ERROR) {
                            if (currentValue < value) {
                                value = currentValue;
                                pivot = v;
                            }
                        }
                    } else {
                        if (v.mType == SolverVariable.Type.SLACK) {
                            if (currentValue < valueSlack) {
                                valueSlack = currentValue;
                                pivotSlack = v;
                            }
                        } else if (v.mType == SolverVariable.Type.ERROR) {
                            if (currentValue < value) {
                                value = currentValue;
                                pivot = v;
                            }
                        }
                    }
                }
            }
        }
        if (all) {
            return pivot;
        }
        return pivot != null ? pivot : pivotSlack;
    }

    // @TODO: add description
    public SolverVariable pickPivot(SolverVariable exclude) {
        return pickPivotInVariables(null, exclude);
    }

    @Override
    public SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid) {
        return pickPivotInVariables(avoid, null);
    }

    @Override
    public void clear() {
        variables.clear();
        mVariable = null;
        mConstantValue = 0;
    }

    /**
     * Used to initiate a goal from a given row (to see if we can remove an extra var)
     */
    @Override
    public void initFromRow(@SuppressWarnings("HiddenTypeParameter") LinearSystem.Row row) {
        if (row instanceof ArrayRow) {
            ArrayRow copiedRow = (ArrayRow) row;
            mVariable = null;
            variables.clear();
            for (int i = 0; i < copiedRow.variables.getCurrentSize(); i++) {
                SolverVariable var = copiedRow.variables.getVariable(i);
                float val = copiedRow.variables.getVariableValue(i);
                variables.add(var, val, true);
            }
        }
    }

    @Override
    public void addError(SolverVariable error) {
        float weight = 1;
        if (error.strength == STRENGTH_LOW) {
            weight = 1F;
        } else if (error.strength == STRENGTH_MEDIUM) {
            weight = 1E3F;
        } else if (error.strength == STRENGTH_HIGH) {
            weight = 1E6F;
        } else if (error.strength == STRENGTH_HIGHEST) {
            weight = 1E9F;
        } else if (error.strength == STRENGTH_EQUALITY) {
            weight = 1E12F;
        }
        variables.put(error, weight);
    }

    @Override
    public SolverVariable getKey() {
        return mVariable;
    }

    @Override
    public void updateFromSystem(LinearSystem system) {
        if (system.mRows.length == 0) {
            return;
        }

        boolean done = false;
        while (!done) {
            int currentSize = variables.getCurrentSize();
            for (int i = 0; i < currentSize; i++) {
                SolverVariable variable = variables.getVariable(i);
                if (variable.mDefinitionId != -1 || variable.isFinalValue || variable.mIsSynonym) {
                    mVariablesToUpdate.add(variable);
                }
            }
            final int size = mVariablesToUpdate.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    SolverVariable variable = mVariablesToUpdate.get(i);
                    if (variable.isFinalValue) {
                        updateFromFinalVariable(system, variable, true);
                    } else if (variable.mIsSynonym) {
                        updateFromSynonymVariable(system, variable, true);
                    } else {
                        updateFromRow(system, system.mRows[variable.mDefinitionId], true);
                    }
                }
                mVariablesToUpdate.clear();
            } else {
                done = true;
            }
        }
        if (LinearSystem.SIMPLIFY_SYNONYMS
                && mVariable != null && variables.getCurrentSize() == 0) {
            mIsSimpleDefinition = true;
            system.hasSimpleDefinition = true;
        }
    }
}
