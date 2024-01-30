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

import androidx.constraintlayout.core.widgets.Chain;
import androidx.constraintlayout.core.widgets.ConstraintAnchor;
import androidx.constraintlayout.core.widgets.ConstraintWidget;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents and solves a system of linear equations.
 */
public class LinearSystem {

    public static final boolean FULL_DEBUG = false;
    public static final boolean DEBUG = false;
    private static final boolean DO_NOT_USE = false;

    private static final boolean DEBUG_CONSTRAINTS = FULL_DEBUG;

    public static boolean USE_DEPENDENCY_ORDERING = false;
    public static boolean USE_BASIC_SYNONYMS = true;
    public static boolean SIMPLIFY_SYNONYMS = true;
    public static boolean USE_SYNONYMS = true;
    public static boolean SKIP_COLUMNS = true;
    public static boolean OPTIMIZED_ENGINE = false;

    /*
     * Default size for the object pools
     */
    private int mPoolSize = 1000;
    public boolean hasSimpleDefinition = false;

    /*
     * Variable counter
     */
    int mVariablesID = 0;

    /*
     * Store a map between name->SolverVariable and SolverVariable->Float for the resolution.
     */
    private HashMap<String, SolverVariable> mVariables = null;

    /*
     * The goal that is used when minimizing the system.
     */
    private Row mGoal;

    private int mTableSize = 32; // default table size for the allocation
    private int mMaxColumns = mTableSize;
    ArrayRow[] mRows = null;

    // if true, will use graph optimizations
    public boolean graphOptimizer = false;
    public boolean newgraphOptimizer = false;

    // Used in optimize()
    private boolean[] mAlreadyTestedCandidates = new boolean[mTableSize];

    int mNumColumns = 1;
    int mNumRows = 0;
    private int mMaxRows = mTableSize;

    final Cache mCache;

    private SolverVariable[] mPoolVariables = new SolverVariable[mPoolSize];
    private int mPoolVariablesCount = 0;

    public static Metrics sMetrics;
    private Row mTempGoal;

    static class ValuesRow extends ArrayRow {
        ValuesRow(Cache cache) {
            variables = new SolverVariableValues(this, cache);
        }
    }

    public LinearSystem() {
        mRows = new ArrayRow[mTableSize];
        releaseRows();
        mCache = new Cache();
        mGoal = new PriorityGoalRow(mCache);
        if (OPTIMIZED_ENGINE) {
            mTempGoal = new ValuesRow(mCache);
        } else {
            mTempGoal = new ArrayRow(mCache);
        }
    }

    // @TODO: add description
    public void fillMetrics(Metrics metrics) {
        sMetrics = metrics;
    }

    public static Metrics getMetrics() {
        return sMetrics;
    }

    interface Row {
        SolverVariable getPivotCandidate(LinearSystem system, boolean[] avoid);

        void clear();

        void initFromRow(Row row);

        void addError(SolverVariable variable);

        void updateFromSystem(LinearSystem system);

        SolverVariable getKey();

        boolean isEmpty();

        void updateFromRow(LinearSystem system, ArrayRow definition, boolean b);

        void updateFromFinalVariable(LinearSystem system,
                SolverVariable variable,
                boolean removeFromDefinition);
    }

    /*--------------------------------------------------------------------------------------------*/
    // Memory management
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Reallocate memory to accommodate increased amount of variables
     */
    private void increaseTableSize() {
        if (DEBUG) {
            System.out.println("###########################");
            System.out.println("### INCREASE TABLE TO " + (mTableSize * 2) + " (num rows: "
                    + mNumRows + ", num cols: " + mNumColumns + "/" + mMaxColumns + ")");
            System.out.println("###########################");
        }
        mTableSize *= 2;
        mRows = Arrays.copyOf(mRows, mTableSize);
        mCache.mIndexedVariables = Arrays.copyOf(mCache.mIndexedVariables, mTableSize);
        mAlreadyTestedCandidates = new boolean[mTableSize];
        mMaxColumns = mTableSize;
        mMaxRows = mTableSize;
        if (sMetrics != null) {
            sMetrics.tableSizeIncrease++;
            sMetrics.maxTableSize = Math.max(sMetrics.maxTableSize, mTableSize);
            sMetrics.lastTableSize = sMetrics.maxTableSize;
        }
    }

    /**
     * Release ArrayRows back to their pool
     */
    private void releaseRows() {
        if (OPTIMIZED_ENGINE) {
            for (int i = 0; i < mNumRows; i++) {
                ArrayRow row = mRows[i];
                if (row != null) {
                    mCache.mOptimizedArrayRowPool.release(row);
                }
                mRows[i] = null;
            }
        } else {
            for (int i = 0; i < mNumRows; i++) {
                ArrayRow row = mRows[i];
                if (row != null) {
                    mCache.mArrayRowPool.release(row);
                }
                mRows[i] = null;
            }
        }
    }

    /**
     * Reset the LinearSystem object so that it can be reused.
     */
    public void reset() {
        if (DEBUG) {
            System.out.println("##################");
            System.out.println("## RESET SYSTEM ##");
            System.out.println("##################");
        }
        for (int i = 0; i < mCache.mIndexedVariables.length; i++) {
            SolverVariable variable = mCache.mIndexedVariables[i];
            if (variable != null) {
                variable.reset();
            }
        }
        mCache.mSolverVariablePool.releaseAll(mPoolVariables, mPoolVariablesCount);
        mPoolVariablesCount = 0;

        Arrays.fill(mCache.mIndexedVariables, null);
        if (mVariables != null) {
            mVariables.clear();
        }
        mVariablesID = 0;
        mGoal.clear();
        mNumColumns = 1;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                mRows[i].mUsed = false;
            }
        }
        releaseRows();
        mNumRows = 0;
        if (OPTIMIZED_ENGINE) {
            mTempGoal = new ValuesRow(mCache);
        } else {
            mTempGoal = new ArrayRow(mCache);
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    // Creation of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    // @TODO: add description
    public SolverVariable createObjectVariable(Object anchor) {
        if (anchor == null) {
            return null;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = null;
        if (anchor instanceof ConstraintAnchor) {
            variable = ((ConstraintAnchor) anchor).getSolverVariable();
            if (variable == null) {
                ((ConstraintAnchor) anchor).resetSolverVariable(mCache);
                variable = ((ConstraintAnchor) anchor).getSolverVariable();
            }
            if (variable.id == -1
                    || variable.id > mVariablesID
                    || mCache.mIndexedVariables[variable.id] == null) {
                if (variable.id != -1) {
                    variable.reset();
                }
                mVariablesID++;
                mNumColumns++;
                variable.id = mVariablesID;
                variable.mType = SolverVariable.Type.UNRESTRICTED;
                mCache.mIndexedVariables[mVariablesID] = variable;
            }
        }
        return variable;
    }

    public static long ARRAY_ROW_CREATION = 0;
    public static long OPTIMIZED_ARRAY_ROW_CREATION = 0;

    // @TODO: add description
    public ArrayRow createRow() {
        ArrayRow row;
        if (OPTIMIZED_ENGINE) {
            row = mCache.mOptimizedArrayRowPool.acquire();
            if (row == null) {
                row = new ValuesRow(mCache);
                OPTIMIZED_ARRAY_ROW_CREATION++;
            } else {
                row.reset();
            }
        } else {
            row = mCache.mArrayRowPool.acquire();
            if (row == null) {
                row = new ArrayRow(mCache);
                ARRAY_ROW_CREATION++;
            } else {
                row.reset();
            }
        }
        SolverVariable.increaseErrorId();
        return row;
    }

    // @TODO: add description
    public SolverVariable createSlackVariable() {
        if (sMetrics != null) {
            sMetrics.slackvariables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    // @TODO: add description
    public SolverVariable createExtraVariable() {
        if (sMetrics != null) {
            sMetrics.extravariables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

//    private void addError(ArrayRow row)
//        row.addError(this, SolverVariable.STRENGTH_NONE);
//
//
//    private void addSingleError(ArrayRow row, int sign)
//        addSingleError(row, sign, SolverVariable.STRENGTH_NONE);
//

    void addSingleError(ArrayRow row, int sign, int strength) {
        String prefix = null;
        if (DEBUG) {
            if (sign > 0) {
                prefix = "ep";
            } else {
                prefix = "em";
            }
            prefix = "em";
        }
        SolverVariable error = createErrorVariable(strength, prefix);
        row.addSingleError(error, sign);
    }

    private SolverVariable createVariable(String name, SolverVariable.Type type) {
        if (sMetrics != null) {
            sMetrics.variables++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(type, null);
        variable.setName(name);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        if (mVariables == null) {
            mVariables = new HashMap<>();
        }
        mVariables.put(name, variable);
        mCache.mIndexedVariables[mVariablesID] = variable;
        return variable;
    }

    // @TODO: add description
    public SolverVariable createErrorVariable(int strength, String prefix) {
        if (sMetrics != null) {
            sMetrics.errors++;
        }
        if (mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.ERROR, prefix);
        mVariablesID++;
        mNumColumns++;
        variable.id = mVariablesID;
        variable.strength = strength;
        mCache.mIndexedVariables[mVariablesID] = variable;
        mGoal.addError(variable);
        return variable;
    }

    /**
     * Returns a SolverVariable instance of the given type
     *
     * @param type type of the SolverVariable
     * @return instance of SolverVariable
     */
    private SolverVariable acquireSolverVariable(SolverVariable.Type type, String prefix) {
        SolverVariable variable = mCache.mSolverVariablePool.acquire();
        if (variable == null) {
            variable = new SolverVariable(type, prefix);
            variable.setType(type, prefix);
        } else {
            variable.reset();
            variable.setType(type, prefix);
        }
        if (mPoolVariablesCount >= mPoolSize) {
            mPoolSize *= 2;
            mPoolVariables = Arrays.copyOf(mPoolVariables, mPoolSize);
        }
        mPoolVariables[mPoolVariablesCount++] = variable;
        return variable;
    }

    /*--------------------------------------------------------------------------------------------*/
    // Accessors of rows / variables / errors
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Simple accessor for the current goal. Used when minimizing the system's goal.
     *
     * @return the current goal.
     */
    Row getGoal() {
        return mGoal;
    }

    ArrayRow getRow(int n) {
        return mRows[n];
    }

    float getValueFor(String name) {
        SolverVariable v = getVariable(name, SolverVariable.Type.UNRESTRICTED);
        if (v == null) {
            return 0;
        }
        return v.computedValue;
    }

    // @TODO: add description
    public int getObjectVariableValue(Object object) {
        ConstraintAnchor anchor = (ConstraintAnchor) object;
        if (Chain.USE_CHAIN_OPTIMIZATION) {
            if (anchor.hasFinalValue()) {
                return anchor.getFinalValue();
            }
        }
        SolverVariable variable = anchor.getSolverVariable();
        if (variable != null) {
            return (int) (variable.computedValue + 0.5f);
        }
        return 0;
    }

    /**
     * Returns a SolverVariable instance given a name and a type.
     *
     * @param name name of the variable
     * @param type {@link SolverVariable.Type type} of the variable
     * @return a SolverVariable instance
     */
    SolverVariable getVariable(String name, SolverVariable.Type type) {
        if (mVariables == null) {
            mVariables = new HashMap<>();
        }
        SolverVariable variable = mVariables.get(name);
        if (variable == null) {
            variable = createVariable(name, type);
        }
        return variable;
    }

    /*--------------------------------------------------------------------------------------------*/
    // System resolution
    /*--------------------------------------------------------------------------------------------*/

    /**
     * Minimize the current goal of the system.
     */
    public void minimize() throws Exception {
        if (sMetrics != null) {
            sMetrics.minimize++;
        }
        if (mGoal.isEmpty()) {
            if (DEBUG) {
                System.out.println("\n*** SKIPPING MINIMIZE! ***\n");
            }
            computeValues();
            return;
        }
        if (DEBUG) {
            System.out.println("\n*** MINIMIZE ***\n");
        }
        if (graphOptimizer || newgraphOptimizer) {
            if (sMetrics != null) {
                sMetrics.graphOptimizer++;
            }
            boolean fullySolved = true;
            for (int i = 0; i < mNumRows; i++) {
                ArrayRow r = mRows[i];
                if (!r.mIsSimpleDefinition) {
                    fullySolved = false;
                    break;
                }
            }
            if (!fullySolved) {
                minimizeGoal(mGoal);
            } else {
                if (sMetrics != null) {
                    sMetrics.fullySolved++;
                }
                computeValues();
            }
        } else {
            minimizeGoal(mGoal);
        }
        if (DEBUG) {
            System.out.println("\n*** END MINIMIZE ***\n");
        }
    }

    /**
     * Minimize the given goal with the current system.
     *
     * @param goal the goal to minimize.
     */
    void minimizeGoal(Row goal) throws Exception {
        if (sMetrics != null) {
            sMetrics.minimizeGoal++;
            sMetrics.maxVariables = Math.max(sMetrics.maxVariables, mNumColumns);
            sMetrics.maxRows = Math.max(sMetrics.maxRows, mNumRows);
        }
        // First, let's make sure that the system is in Basic Feasible Solved Form (BFS), i.e.
        // all the constants of the restricted variables should be positive.
        if (DEBUG) {
            System.out.println("minimize goal: " + goal);
        }
        // we don't need this for now as we incrementally built the system
        // goal.updateFromSystem(this);
        if (DEBUG) {
            displayReadableRows();
        }
        enforceBFS(goal);
        if (DEBUG) {
            System.out.println("Goal after enforcing BFS " + goal);
            displayReadableRows();
        }
        optimize(goal, false);
        if (DEBUG) {
            System.out.println("Goal after optimization " + goal);
            displayReadableRows();
        }
        computeValues();
    }

    final void cleanupRows() {
        int i = 0;
        while (i < mNumRows) {
            ArrayRow current = mRows[i];
            if (current.variables.getCurrentSize() == 0) {
                current.mIsSimpleDefinition = true;
            }
            if (current.mIsSimpleDefinition) {
                current.mVariable.computedValue = current.mConstantValue;
                current.mVariable.removeFromRow(current);
                for (int j = i; j < mNumRows - 1; j++) {
                    mRows[j] = mRows[j + 1];
                }
                mRows[mNumRows - 1] = null;
                mNumRows--;
                i--;
                if (OPTIMIZED_ENGINE) {
                    mCache.mOptimizedArrayRowPool.release(current);
                } else {
                    mCache.mArrayRowPool.release(current);
                }
            }
            i++;
        }
    }

    /**
     * Add the equation to the system
     *
     * @param row the equation we want to add expressed as a system row.
     */
    public void addConstraint(ArrayRow row) {
        if (row == null) {
            return;
        }
        if (sMetrics != null) {
            sMetrics.constraints++;
            if (row.mIsSimpleDefinition) {
                sMetrics.simpleconstraints++;
            }
        }
        if (mNumRows + 1 >= mMaxRows || mNumColumns + 1 >= mMaxColumns) {
            increaseTableSize();
        }
        if (DEBUG) {
            System.out.println("addConstraint <" + row.toReadableString() + ">");
            displayReadableRows();
        }

        boolean added = false;
        if (!row.mIsSimpleDefinition) {
            // Update the equation with the variables already defined in the system
            row.updateFromSystem(this);

            if (row.isEmpty()) {
                return;
            }

            // First, ensure that if we have a constant it's positive
            row.ensurePositiveConstant();

            if (DEBUG) {
                System.out.println("addConstraint, updated row : " + row.toReadableString());
            }

            // Then pick a good variable to use for the row
            if (row.chooseSubject(this)) {
                // extra variable added... let's try to see if we can remove it
                SolverVariable extra = createExtraVariable();
                row.mVariable = extra;
                int numRows = mNumRows;
                addRow(row);
                if (mNumRows == numRows + 1) {
                    added = true;
                    mTempGoal.initFromRow(row);
                    optimize(mTempGoal, true);
                    if (extra.mDefinitionId == -1) {
                        if (DEBUG) {
                            System.out.println("row added is 0, so get rid of it");
                        }
                        if (row.mVariable == extra) {
                            // move extra to be parametric
                            SolverVariable pivotCandidate = row.pickPivot(extra);
                            if (pivotCandidate != null) {
                                if (sMetrics != null) {
                                    sMetrics.pivots++;
                                }
                                row.pivot(pivotCandidate);
                            }
                        }
                        if (!row.mIsSimpleDefinition) {
                            row.mVariable.updateReferencesWithNewDefinition(this, row);
                        }
                        if (OPTIMIZED_ENGINE) {
                            mCache.mOptimizedArrayRowPool.release(row);
                        } else {
                            mCache.mArrayRowPool.release(row);
                        }
                        mNumRows--;
                    }
                }
            }

            if (!row.hasKeyVariable()) {
                // Can happen if row resolves to nil
                if (DEBUG) {
                    System.out.println("No variable found to pivot on " + row.toReadableString());
                    displayReadableRows();
                }
                return;
            }
        }
        if (!added) {
            addRow(row);
        }
    }

    private void addRow(ArrayRow row) {
        if (SIMPLIFY_SYNONYMS && row.mIsSimpleDefinition) {
            row.mVariable.setFinalValue(this, row.mConstantValue);
        } else {
            mRows[mNumRows] = row;
            row.mVariable.mDefinitionId = mNumRows;
            mNumRows++;
            row.mVariable.updateReferencesWithNewDefinition(this, row);
        }
        if (DEBUG) {
            System.out.println("Row added: " + row);
            System.out.println("here is the system:");
            displayReadableRows();
        }
        if (SIMPLIFY_SYNONYMS && hasSimpleDefinition) {
            // compact the rows...
            for (int i = 0; i < mNumRows; i++) {
                if (mRows[i] == null) {
                    System.out.println("WTF");
                }
                if (mRows[i] != null && mRows[i].mIsSimpleDefinition) {
                    ArrayRow removedRow = mRows[i];
                    removedRow.mVariable.setFinalValue(this, removedRow.mConstantValue);
                    if (OPTIMIZED_ENGINE) {
                        mCache.mOptimizedArrayRowPool.release(removedRow);
                    } else {
                        mCache.mArrayRowPool.release(removedRow);
                    }
                    mRows[i] = null;
                    int lastRow = i + 1;
                    for (int j = i + 1; j < mNumRows; j++) {
                        mRows[j - 1] = mRows[j];
                        if (mRows[j - 1].mVariable.mDefinitionId == j) {
                            mRows[j - 1].mVariable.mDefinitionId = j - 1;
                        }
                        lastRow = j;
                    }
                    if (lastRow < mNumRows) {
                        mRows[lastRow] = null;
                    }
                    mNumRows--;
                    i--;
                }
            }
            hasSimpleDefinition = false;
        }
    }

    // @TODO: add description
    public void removeRow(ArrayRow row) {
        if (row.mIsSimpleDefinition && row.mVariable != null) {
            if (row.mVariable.mDefinitionId != -1) {
                for (int i = row.mVariable.mDefinitionId; i < mNumRows - 1; i++) {
                    SolverVariable rowVariable = mRows[i + 1].mVariable;
                    if (rowVariable.mDefinitionId == i + 1) {
                        rowVariable.mDefinitionId = i;
                    }
                    mRows[i] = mRows[i + 1];
                }
                mNumRows--;
            }
            if (!row.mVariable.isFinalValue) {
                row.mVariable.setFinalValue(this, row.mConstantValue);
            }
            if (OPTIMIZED_ENGINE) {
                mCache.mOptimizedArrayRowPool.release(row);
            } else {
                mCache.mArrayRowPool.release(row);
            }
        }
    }

    /**
     * Optimize the system given a goal to minimize. The system should be in BFS form.
     *
     * @param goal goal to optimize.
     * @return number of iterations.
     */
    private int optimize(Row goal, boolean b) {
        if (sMetrics != null) {
            sMetrics.optimize++;
        }
        boolean done = false;
        int tries = 0;
        for (int i = 0; i < mNumColumns; i++) {
            mAlreadyTestedCandidates[i] = false;
        }

        if (DEBUG) {
            System.out.println("\n****************************");
            System.out.println("*       OPTIMIZATION       *");
            System.out.println("* mNumColumns: " + mNumColumns);
            System.out.println("* GOAL: " + goal + " " + b);
            System.out.println("****************************\n");
        }

        while (!done) {
            if (sMetrics != null) {
                sMetrics.iterations++;
            }
            tries++;
            if (DEBUG) {
                System.out.println("\n******************************");
                System.out.println("* iteration: " + tries);
            }
            if (tries >= 2 * mNumColumns) {
                if (DEBUG) {
                    System.out.println("=> Exit optimization because tries "
                            + tries + " >= " + (2 * mNumColumns));
                }
                return tries;
            }

            if (goal.getKey() != null) {
                mAlreadyTestedCandidates[goal.getKey().id] = true;
            }
            SolverVariable pivotCandidate = goal.getPivotCandidate(this, mAlreadyTestedCandidates);
            if (DEBUG) {
                System.out.println("* Pivot candidate: " + pivotCandidate);
                System.out.println("******************************\n");
            }
            if (pivotCandidate != null) {
                if (mAlreadyTestedCandidates[pivotCandidate.id]) {
                    if (DEBUG) {
                        System.out.println("* Pivot candidate " + pivotCandidate
                                + " already tested, let's bail");
                    }
                    return tries;
                } else {
                    mAlreadyTestedCandidates[pivotCandidate.id] = true;
                }
            }

            if (pivotCandidate != null) {
                if (DEBUG) {
                    System.out.println("valid pivot candidate: " + pivotCandidate);
                }
                // there's a negative variable in the goal that we can pivot on.
                // We now need to select which equation of the system we should do
                // the pivot on.

                // Let's try to find the equation in the system that we can pivot on.
                // The rules are simple:
                // - only look at restricted variables equations (i.e. Cs)
                // - only look at equations containing the column we are trying to pivot on (duh)
                // - select preferably an equation with strong strength over weak strength

                float min = Float.MAX_VALUE;
                int pivotRowIndex = -1;

                for (int i = 0; i < mNumRows; i++) {
                    ArrayRow current = mRows[i];
                    SolverVariable variable = current.mVariable;
                    if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                        // skip unrestricted variables equations (to only look at Cs)
                        continue;
                    }
                    if (current.mIsSimpleDefinition) {
                        continue;
                    }

                    if (current.hasVariable(pivotCandidate)) {
                        if (DEBUG) {
                            System.out.println("equation " + i + " "
                                    + current + " contains " + pivotCandidate);
                        }
                        // the current row does contains the variable
                        // we want to pivot on
                        float a_j = current.variables.get(pivotCandidate);
                        if (a_j < 0) {
                            float value = -current.mConstantValue / a_j;
                            if (value < min) {
                                min = value;
                                pivotRowIndex = i;
                            }
                        }
                    }
                }
                // At this point, we ought to have an equation to pivot on

                if (pivotRowIndex > -1) {
                    // We found an equation to pivot on
                    if (DEBUG) {
                        System.out.println("We pivot on " + pivotRowIndex);
                    }
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    pivotEquation.mVariable.mDefinitionId = -1;
                    if (sMetrics != null) {
                        sMetrics.pivots++;
                    }
                    pivotEquation.pivot(pivotCandidate);
                    pivotEquation.mVariable.mDefinitionId = pivotRowIndex;
                    pivotEquation.mVariable.updateReferencesWithNewDefinition(this, pivotEquation);
                    if (DEBUG) {
                        System.out.println("new system after pivot:");
                        displayReadableRows();
                        System.out.println("optimizing: " + goal);
                    }
                    /*
                    try {
                        enforceBFS(goal);
                    } catch (Exception e) {
                        System.out.println("### EXCEPTION " + e);
                        e.printStackTrace();
                    }
                    */
                    // now that we pivoted, we're going to continue looping on the next goal
                    // columns, until we exhaust all the possibilities of improving the system
                } else {
                    if (DEBUG) {
                        System.out.println("we couldn't find an equation to pivot upon");
                    }
                }

            } else {
                // There is no candidate goals columns we should try to pivot on,
                // so let's exit the loop.
                if (DEBUG) {
                    System.out.println("no more candidate goals to pivot on, let's exit");
                }
                done = true;
            }
        }
        return tries;
    }

    /**
     * Make sure that the system is in Basic Feasible Solved form (BFS).
     *
     * @param goal the row representing the system goal
     * @return number of iterations
     */
    private int enforceBFS(Row goal) throws Exception {
        int tries = 0;
        boolean done;

        if (DEBUG) {
            System.out.println("\n#################");
            System.out.println("# ENFORCING BFS #");
            System.out.println("#################\n");
        }

        // At this point, we might not be in Basic Feasible Solved form (BFS),
        // i.e. one of the restricted equation has a negative constant.
        // Let's check if that's the case or not.
        boolean infeasibleSystem = false;
        for (int i = 0; i < mNumRows; i++) {
            SolverVariable variable = mRows[i].mVariable;
            if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                continue; // C can be either positive or negative.
            }
            if (mRows[i].mConstantValue < 0) {
                infeasibleSystem = true;
                break;
            }
        }

        // The system happens to not be in BFS form, we need to go back to it to properly solve it.
        if (infeasibleSystem) {
            if (DEBUG) {
                System.out.println("the current system is infeasible, let's try to fix this.");
            }

            // Going back to BFS form can be done by selecting any equations in Cs containing
            // a negative constant, then selecting a potential pivot variable that would remove
            // this negative constant. Once we have
            done = false;
            tries = 0;
            while (!done) {
                if (sMetrics != null) {
                    sMetrics.bfs++;
                }
                tries++;
                if (DEBUG) {
                    System.out.println("iteration on infeasible system " + tries);
                }
                float min = Float.MAX_VALUE;
                int strength = 0;
                int pivotRowIndex = -1;
                int pivotColumnIndex = -1;

                for (int i = 0; i < mNumRows; i++) {
                    ArrayRow current = mRows[i];
                    SolverVariable variable = current.mVariable;
                    if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                        // skip unrestricted variables equations, as C
                        // can be either positive or negative.
                        continue;
                    }
                    if (current.mIsSimpleDefinition) {
                        continue;
                    }
                    if (current.mConstantValue < 0) {
                        // let's examine this row, see if we can find a good pivot
                        if (DEBUG) {
                            System.out.println("looking at pivoting on row " + current);
                        }
                        if (SKIP_COLUMNS) {
                            final int size = current.variables.getCurrentSize();
                            for (int j = 0; j < size; j++) {
                                SolverVariable candidate = current.variables.getVariable(j);
                                float a_j = current.variables.get(candidate);
                                if (a_j <= 0) {
                                    continue;
                                }
                                if (DEBUG) {
                                    System.out.println("candidate for pivot " + candidate);
                                }
                                for (int k = 0; k < SolverVariable.MAX_STRENGTH; k++) {
                                    float value = candidate.mStrengthVector[k] / a_j;
                                    if ((value < min && k == strength) || k > strength) {
                                        min = value;
                                        pivotRowIndex = i;
                                        pivotColumnIndex = candidate.id;
                                        strength = k;
                                    }
                                }
                            }
                        } else {
                            for (int j = 1; j < mNumColumns; j++) {
                                SolverVariable candidate = mCache.mIndexedVariables[j];
                                float a_j = current.variables.get(candidate);
                                if (a_j <= 0) {
                                    continue;
                                }
                                if (DEBUG) {
                                    System.out.println("candidate for pivot " + candidate);
                                }
                                for (int k = 0; k < SolverVariable.MAX_STRENGTH; k++) {
                                    float value = candidate.mStrengthVector[k] / a_j;
                                    if ((value < min && k == strength) || k > strength) {
                                        min = value;
                                        pivotRowIndex = i;
                                        pivotColumnIndex = j;
                                        strength = k;
                                    }
                                }
                            }
                        }
                    }
                }

                if (pivotRowIndex != -1) {
                    // We have a pivot!
                    ArrayRow pivotEquation = mRows[pivotRowIndex];
                    if (DEBUG) {
                        System.out.println("Pivoting on " + pivotEquation.mVariable + " with "
                                + mCache.mIndexedVariables[pivotColumnIndex]);
                    }
                    pivotEquation.mVariable.mDefinitionId = -1;
                    if (sMetrics != null) {
                        sMetrics.pivots++;
                    }
                    pivotEquation.pivot(mCache.mIndexedVariables[pivotColumnIndex]);
                    pivotEquation.mVariable.mDefinitionId = pivotRowIndex;
                    pivotEquation.mVariable.updateReferencesWithNewDefinition(this, pivotEquation);

                    if (DEBUG) {
                        System.out.println("new goal after pivot: " + goal);
                        displayRows();
                    }
                } else {
                    done = true;
                }
                if (tries > mNumColumns / 2) {
                    // fail safe -- tried too many times
                    done = true;
                }
            }
        }

        if (DEBUG) {
            System.out.println("the current system should now be feasible ["
                    + infeasibleSystem + "] after " + tries + " iterations");
            displayReadableRows();

            // Let's make sure the system is correct
            //noinspection UnusedAssignment
            infeasibleSystem = false;
            for (int i = 0; i < mNumRows; i++) {
                SolverVariable variable = mRows[i].mVariable;
                if (variable.mType == SolverVariable.Type.UNRESTRICTED) {
                    continue; // C can be either positive or negative.
                }
                if (mRows[i].mConstantValue < 0) {
                    //noinspection UnusedAssignment
                    infeasibleSystem = true;
                    break;
                }
            }

            if (DEBUG && infeasibleSystem) {
                System.out.println("IMPOSSIBLE SYSTEM, WTF");
                throw new Exception();
            }
            if (infeasibleSystem) {
                return tries;
            }
        }

        return tries;
    }

    private void computeValues() {
        for (int i = 0; i < mNumRows; i++) {
            ArrayRow row = mRows[i];
            row.mVariable.computedValue = row.mConstantValue;
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    // Display utility functions
    /*--------------------------------------------------------------------------------------------*/

    @SuppressWarnings("unused")
    private void displayRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i];
            s += "\n";
        }
        s += mGoal + "\n";
        System.out.println(s);
    }

    // @TODO: add description
    public void displayReadableRows() {
        displaySolverVariables();
        String s = " num vars " + mVariablesID + "\n";
        for (int i = 0; i < mVariablesID + 1; i++) {
            SolverVariable variable = mCache.mIndexedVariables[i];
            if (variable != null && variable.isFinalValue) {
                s += " $[" + i + "] => " + variable + " = " + variable.computedValue + "\n";
            }
        }
        s += "\n";
        for (int i = 0; i < mVariablesID + 1; i++) {
            SolverVariable variable = mCache.mIndexedVariables[i];
            if (variable != null && variable.mIsSynonym) {
                SolverVariable synonym = mCache.mIndexedVariables[variable.mSynonym];
                s += " ~[" + i + "] => " + variable + " = "
                        + synonym + " + " + variable.mSynonymDelta + "\n";
            }
        }
        s += "\n\n #  ";
        for (int i = 0; i < mNumRows; i++) {
            s += mRows[i].toReadableString();
            s += "\n #  ";
        }
        if (mGoal != null) {
            s += "Goal: " + mGoal + "\n";
        }
        System.out.println(s);
    }

    // @TODO: add description
    @SuppressWarnings("unused")
    public void displayVariablesReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i].mVariable.mType == SolverVariable.Type.UNRESTRICTED) {
                s += mRows[i].toReadableString();
                s += "\n";
            }
        }
        s += mGoal + "\n";
        System.out.println(s);
    }


    // @TODO: add description
    @SuppressWarnings("unused")
    public int getMemoryUsed() {
        int actualRowSize = 0;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                actualRowSize += mRows[i].sizeInBytes();
            }
        }
        return actualRowSize;
    }

    @SuppressWarnings("unused")
    public int getNumEquations() {
        return mNumRows;
    }

    @SuppressWarnings("unused")
    public int getNumVariables() {
        return mVariablesID;
    }

    /**
     * Display current system information
     */
    void displaySystemInformation() {
        int count = 0;
        int rowSize = 0;
        for (int i = 0; i < mTableSize; i++) {
            if (mRows[i] != null) {
                rowSize += mRows[i].sizeInBytes();
            }
        }
        int actualRowSize = 0;
        for (int i = 0; i < mNumRows; i++) {
            if (mRows[i] != null) {
                actualRowSize += mRows[i].sizeInBytes();
            }
        }

        System.out.println("Linear System -> Table size: " + mTableSize
                + " (" + getDisplaySize(mTableSize * mTableSize)
                + ") -- row sizes: " + getDisplaySize(rowSize)
                + ", actual size: " + getDisplaySize(actualRowSize)
                + " rows: " + mNumRows + "/" + mMaxRows
                + " cols: " + mNumColumns + "/" + mMaxColumns
                + " " + count + " occupied cells, " + getDisplaySize(count)
        );
    }

    private void displaySolverVariables() {
        String s = "Display Rows (" + mNumRows + "x" + mNumColumns + ")\n";
        /*
        s += ":\n\t | C | ";
        for (int i = 1; i <= mNumColumns; i++) {
            SolverVariable v = mCache.mIndexedVariables[i];
            s += v;
            s += " | ";
        }
        s += "\n";
        */
        System.out.println(s);
    }

    private String getDisplaySize(int n) {
        int mb = (n * 4) / 1024 / 1024;
        if (mb > 0) {
            return "" + mb + " Mb";
        }
        int kb = (n * 4) / 1024;
        if (kb > 0) {
            return "" + kb + " Kb";
        }
        return "" + (n * 4) + " bytes";
    }

    public Cache getCache() {
        return mCache;
    }

    private String getDisplayStrength(int strength) {
        if (strength == SolverVariable.STRENGTH_LOW) {
            return "LOW";
        }
        if (strength == SolverVariable.STRENGTH_MEDIUM) {
            return "MEDIUM";
        }
        if (strength == SolverVariable.STRENGTH_HIGH) {
            return "HIGH";
        }
        if (strength == SolverVariable.STRENGTH_HIGHEST) {
            return "HIGHEST";
        }
        if (strength == SolverVariable.STRENGTH_EQUALITY) {
            return "EQUALITY";
        }
        if (strength == SolverVariable.STRENGTH_FIXED) {
            return "FIXED";
        }
        if (strength == SolverVariable.STRENGTH_BARRIER) {
            return "BARRIER";
        }
        return "NONE";
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // Equations
    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add an equation of the form a >= b + margin
     *
     * @param a        variable a
     * @param b        variable b
     * @param margin   margin
     * @param strength strength used
     */
    public void addGreaterThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> " + a + " >= " + b + (margin != 0 ? " + " + margin : "")
                    + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), strength);
        }
        addConstraint(row);
    }

    // @TODO: add description
    public void addGreaterBarrier(SolverVariable a,
            SolverVariable b,
            int margin,
            boolean hasMatchConstraintWidgets) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> Barrier " + a + " >= " + b);
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack, margin);
        addConstraint(row);
    }

    /**
     * Add an equation of the form a <= b + margin
     *
     * @param a        variable a
     * @param b        variable b
     * @param margin   margin
     * @param strength strength used
     */
    public void addLowerThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> " + a + " <= " + b + (margin != 0 ? " + " + margin : "")
                    + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowLowerThan(a, b, slack, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) (-1 * slackValue), strength);
        }
        addConstraint(row);
    }

    // @TODO: add description
    public void addLowerBarrier(SolverVariable a,
            SolverVariable b,
            int margin,
            boolean hasMatchConstraintWidgets) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> Barrier " + a + " <= " + b);
        }
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowLowerThan(a, b, slack, margin);
        addConstraint(row);
    }

    /**
     * Add an equation of the form (1 - bias) * (a - b) = bias * (c - d)
     *
     * @param a        variable a
     * @param b        variable b
     * @param m1       margin 1
     * @param bias     bias between ab - cd
     * @param c        variable c
     * @param d        variable d
     * @param m2       margin 2
     * @param strength strength used
     */
    public void addCentering(SolverVariable a, SolverVariable b, int m1, float bias,
            SolverVariable c, SolverVariable d, int m2, int strength) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> [center bias: " + bias + "] : " + a + " - " + b
                    + " - " + m1
                    + " = " + c + " - " + d + " - " + m2
                    + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowCentering(a, b, m1, bias, c, d, m2);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
        addConstraint(row);
    }

    // @TODO: add description
    public void addRatio(SolverVariable a,
            SolverVariable b,
            SolverVariable c,
            SolverVariable d,
            float ratio,
            int strength) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> [ratio: " + ratio + "] : " + a + " = " + b
                    + " + (" + c + " - " + d + ") * " + ratio + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowDimensionRatio(a, b, c, d, ratio);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
        addConstraint(row);
    }

    // @TODO: add description
    public void addSynonym(SolverVariable a, SolverVariable b, int margin) {
        if (a.mDefinitionId == -1 && margin == 0) {
            if (DEBUG_CONSTRAINTS) {
                System.out.println("(S) -> " + a + " = " + b + (margin != 0 ? " + " + margin : ""));
            }
            if (b.mIsSynonym) {
                margin += (int) b.mSynonymDelta;
                b = mCache.mIndexedVariables[b.mSynonym];
            }
            if (a.mIsSynonym) {
                margin -= (int) a.mSynonymDelta;
                a = mCache.mIndexedVariables[a.mSynonym];
            } else {
                a.setSynonym(this, b, 0);
            }
        } else {
            addEquality(a, b, margin, SolverVariable.STRENGTH_FIXED);
        }
    }

    /**
     * Add an equation of the form a = b + margin
     *
     * @param a        variable a
     * @param b        variable b
     * @param margin   margin used
     * @param strength strength used
     */
    public ArrayRow addEquality(SolverVariable a, SolverVariable b, int margin, int strength) {
        if (sMetrics != null) {
            sMetrics.mSimpleEquations++;
        }
        if (USE_BASIC_SYNONYMS && strength == SolverVariable.STRENGTH_FIXED
                && b.isFinalValue && a.mDefinitionId == -1) {
            if (DEBUG_CONSTRAINTS) {
                System.out.println("=> " + a + " = " + b + (margin != 0 ? " + " + margin : "")
                        + " = " + (b.computedValue + margin) + " (Synonym)");
            }
            a.setFinalValue(this, b.computedValue + margin);
            return null;
        }
        if (DO_NOT_USE && USE_SYNONYMS && strength == SolverVariable.STRENGTH_FIXED
                && a.mDefinitionId == -1 && margin == 0) {
            if (DEBUG_CONSTRAINTS) {
                System.out.println("(S) -> " + a + " = " + b + (margin != 0 ? " + " + margin : "")
                        + " " + getDisplayStrength(strength));
            }
            if (b.mIsSynonym) {
                margin += (int) b.mSynonymDelta;
                b = mCache.mIndexedVariables[b.mSynonym];
            }
            if (a.mIsSynonym) {
                margin -= (int) a.mSynonymDelta;
                a = mCache.mIndexedVariables[a.mSynonym];
            } else {
                a.setSynonym(this, b, margin);
                return null;
            }
        }
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> " + a + " = " + b + (margin != 0 ? " + " + margin : "")
                    + " " + getDisplayStrength(strength));
        }
        ArrayRow row = createRow();
        row.createRowEquals(a, b, margin);
        if (strength != SolverVariable.STRENGTH_FIXED) {
            row.addError(this, strength);
        }
        addConstraint(row);
        return row;
    }

    /**
     * Add an equation of the form a = value
     *
     * @param a     variable a
     * @param value the value we set
     */
    public void addEquality(SolverVariable a, int value) {
        if (sMetrics != null) {
            sMetrics.mSimpleEquations++;
        }
        if (USE_BASIC_SYNONYMS && a.mDefinitionId == -1) {
            if (DEBUG_CONSTRAINTS) {
                System.out.println("=> " + a + " = " + value + " (Synonym)");
            }
            a.setFinalValue(this, value);
            for (int i = 0; i < mVariablesID + 1; i++) {
                SolverVariable variable = mCache.mIndexedVariables[i];
                if (variable != null && variable.mIsSynonym && variable.mSynonym == a.id) {
                    variable.setFinalValue(this, value + variable.mSynonymDelta);
                }
            }
            return;
        }
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> " + a + " = " + value);
        }
        int idx = a.mDefinitionId;
        if (a.mDefinitionId != -1) {
            ArrayRow row = mRows[idx];
            if (row.mIsSimpleDefinition) {
                row.mConstantValue = value;
            } else {
                if (row.variables.getCurrentSize() == 0) {
                    row.mIsSimpleDefinition = true;
                    row.mConstantValue = value;
                } else {
                    ArrayRow newRow = createRow();
                    newRow.createRowEquals(a, value);
                    addConstraint(newRow);
                }
            }
        } else {
            ArrayRow row = createRow();
            row.createRowDefinition(a, value);
            addConstraint(row);
        }
    }

    /**
     * Create a constraint to express A = C * percent
     *
     * @param linearSystem the system we create the row on
     * @param variableA    variable a
     * @param variableC    variable c
     * @param percent      the percent used
     * @return the created row
     */
    public static ArrayRow createRowDimensionPercent(LinearSystem linearSystem,
            SolverVariable variableA,
            SolverVariable variableC,
            float percent) {
        if (DEBUG_CONSTRAINTS) {
            System.out.println("-> " + variableA + " = " + variableC + " * " + percent);
        }
        ArrayRow row = linearSystem.createRow();
        return row.createRowDimensionPercent(variableA, variableC, percent);
    }

    /**
     * Add the equations constraining a widget center to another widget center, positioned
     * on a circle, following an angle and radius
     *
     * @param angle  from 0 to 360
     * @param radius the distance between the two centers
     */
    public void addCenterPoint(ConstraintWidget widget,
            ConstraintWidget target,
            float angle,
            int radius) {

        SolverVariable Al = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable At = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Ar = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Ab = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.BOTTOM));

        SolverVariable Bl = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable Bt = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Br = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Bb = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.BOTTOM));

        ArrayRow row = createRow();
        float angleComponent = (float) (Math.sin(angle) * radius);
        row.createRowWithAngle(At, Ab, Bt, Bb, angleComponent);
        addConstraint(row);
        row = createRow();
        angleComponent = (float) (Math.cos(angle) * radius);
        row.createRowWithAngle(Al, Ar, Bl, Br, angleComponent);
        addConstraint(row);
    }

}
