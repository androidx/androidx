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
import java.util.Collections;
import java.util.HashMap;

/**
 * LinearEquation is used to represent the linear equations fed into the solver.<br>
 * A linear equation can be an equality or
 * an inequation (left term &le; or &ge; to the right term).<br>
 * The general form will be similar to {@code a0x0 + a1x1 + ... = C + a2x2 + a3x3 + ... ,}
 * where {@code a0x0} is a term representing a variable x0 of an amount {@code a0},
 * and {@code C} represent a constant term.
 * The amount of terms on the left side or the right side of the equation is arbitrary.
 */
class LinearEquation {

    private ArrayList<EquationVariable> mLeftSide = new ArrayList<>();
    private ArrayList<EquationVariable> mRightSide = new ArrayList<>();
    private ArrayList<EquationVariable> mCurrentSide = null;

    public boolean isNull() {
        if (mLeftSide.size() == 0 && mRightSide.size() == 0) {
            return true;
        }
        if (mLeftSide.size() == 1 && mRightSide.size() == 1) {
            EquationVariable v1 = mLeftSide.get(0);
            EquationVariable v2 = mRightSide.get(0);
            if (v1.isConstant() && v2.isConstant()
                    && v1.getAmount().isNull() && v2.getAmount().isNull()) {
                return true;
            }
        }
        return false;
    }

    private enum Type {EQUALS, LOWER_THAN, GREATER_THAN}

    private Type mType = Type.EQUALS;

    private LinearSystem mSystem = null;

    private static int sArtificialIndex = 0;
    private static int sSlackIndex = 0;
    private static int sErrorIndex = 0;

    static String getNextArtificialVariableName() {
        return "a" + ++sArtificialIndex;
    }

    static String getNextSlackVariableName() {
        return "s" + ++sSlackIndex;
    }

    static String getNextErrorVariableName() {
        return "e" + ++sErrorIndex;
    }

    /**
     * Reset the counters for the automatic slack and error variable naming
     */
    public static void resetNaming() {
        sArtificialIndex = 0;
        sSlackIndex = 0;
        sErrorIndex = 0;
    }

    /**
     * Copy constructor
     *
     * @param equation to copy
     */
    LinearEquation(LinearEquation equation) {
        final ArrayList<EquationVariable> mLeftSide1 = equation.mLeftSide;
        for (int i = 0, mLeftSide1Size = mLeftSide1.size(); i < mLeftSide1Size; i++) {
            final EquationVariable v = mLeftSide1.get(i);
            EquationVariable v2 = new EquationVariable(v);
            mLeftSide.add(v2);
        }
        final ArrayList<EquationVariable> mRightSide1 = equation.mRightSide;
        for (int i = 0, mRightSide1Size = mRightSide1.size(); i < mRightSide1Size; i++) {
            final EquationVariable v = mRightSide1.get(i);
            EquationVariable v2 = new EquationVariable(v);
            mRightSide.add(v2);
        }
        mCurrentSide = mRightSide;
    }

    /**
     * Transform a LinearEquation into a Row
     *
     * @param e linear equation
     * @return a Row object
     */
    static ArrayRow createRowFromEquation(LinearSystem linearSystem, LinearEquation e) {
        e.normalize();
        e.moveAllToTheRight();
        // Let's build a row from the LinearEquation
        ArrayRow row = linearSystem.createRow();
        ArrayList<EquationVariable> eq = e.getRightSide();
        final int count = eq.size();
        for (int i = 0; i < count; i++) {
            EquationVariable v = eq.get(i);
            SolverVariable sv = v.getSolverVariable();
            if (sv != null) {
                float previous = row.variables.get(sv);
                row.variables.put(sv, previous + v.getAmount().toFloat());
            } else {
                row.mConstantValue = v.getAmount().toFloat();
            }
        }
        return row;
    }

    /**
     * Insert the equation in the system
     */
    public void i() {
        if (mSystem == null) {
            return;
        }
        ArrayRow row = createRowFromEquation(mSystem, this);
        mSystem.addConstraint(row);
    }

    /**
     * Set the current side to be the left side
     */
    public void setLeftSide() {
        mCurrentSide = mLeftSide;
    }

    /**
     * Remove any terms on the left side of the equation
     */
    public void clearLeftSide() {
        mLeftSide.clear();
    }

    /**
     * Remove {@link EquationVariable} pointing to {@link SolverVariable}
     *
     * @param v the {@link SolverVariable} we want to remove from the equation
     */
    public void remove(SolverVariable v) {
        EquationVariable ev = find(v, mLeftSide);
        if (ev != null) {
            mLeftSide.remove(ev);
        }
        ev = find(v, mRightSide);
        if (ev != null) {
            mRightSide.remove(ev);
        }
    }

    /**
     * Base constructor, set the current side to the left side.
     */
    LinearEquation() {
        mCurrentSide = mLeftSide;
    }

    /**
     * Base constructor, set the current side to the left side.
     */
    LinearEquation(LinearSystem system) {
        mCurrentSide = mLeftSide;
        mSystem = system;
    }

    /**
     * Set the current equation system for this equation
     *
     * @param system the equation system this equation belongs to
     */
    public void setSystem(LinearSystem system) {
        mSystem = system;
    }

    /**
     * Set the equality operator for the equation, and switch the current side to the right side
     *
     * @return this
     */
    public LinearEquation equalsTo() {
        mCurrentSide = mRightSide;
        return this;
    }

    /**
     * Set the greater than operator for the equation, and switch the current side to the right side
     *
     * @return this
     */
    public LinearEquation greaterThan() {
        mCurrentSide = mRightSide;
        mType = Type.GREATER_THAN;
        return this;
    }

    /**
     * Set the lower than operator for the equation, and switch the current side to the right side
     *
     * @return this
     */
    public LinearEquation lowerThan() {
        mCurrentSide = mRightSide;
        mType = Type.LOWER_THAN;
        return this;
    }

    /**
     * Normalize the linear equation. If the equation is an equality, transforms it into
     * an equality, adding automatically slack or error variables.
     */
    public void normalize() {
        if (mType == Type.EQUALS) {
            return;
        }
        mCurrentSide = mLeftSide;
        if (mType == Type.LOWER_THAN) {
            withSlack(1);
        } else if (mType == Type.GREATER_THAN) {
            withSlack(-1);
        }
        mType = Type.EQUALS;
        mCurrentSide = mRightSide;
    }

    /**
     * Will simplify the equation per side -- regroup similar variables into one.
     * E.g. 2a + b + 3a = b - c will be turned into 5a + b = b - c.
     */
    public void simplify() {
        simplifySide(mLeftSide);
        simplifySide(mRightSide);
    }

    /**
     * Simplify an array of {@link EquationVariable}
     *
     * @param side Array of EquationVariable
     */
    private void simplifySide(ArrayList<EquationVariable> side) {
        EquationVariable constant = null;
        HashMap<String, EquationVariable> variables = new HashMap<>();
        ArrayList<String> variablesNames = new ArrayList<>();
        for (int i = 0, sideSize = side.size(); i < sideSize; i++) {
            final EquationVariable v = side.get(i);
            if (v.isConstant()) {
                if (constant == null) {
                    constant = v;
                } else {
                    constant.add(v);
                }
            } else {
                if (variables.containsKey(v.getName())) {
                    EquationVariable original = variables.get(v.getName());
                    original.add(v);
                } else {
                    variables.put(v.getName(), v);
                    variablesNames.add(v.getName());
                }
            }
        }
        side.clear();
        if (constant != null) {
            side.add(constant);
        }
        Collections.sort(variablesNames);
        for (int i = 0, variablesNamesSize = variablesNames.size(); i < variablesNamesSize; i++) {
            final String name = variablesNames.get(i);
            EquationVariable v = variables.get(name);
            side.add(v);
        }
        removeNullTerms(side);
    }

    public void moveAllToTheRight() {
        for (int i = 0, mLeftSideSize = mLeftSide.size(); i < mLeftSideSize; i++) {
            final EquationVariable v = mLeftSide.get(i);
            mRightSide.add(v.inverse());
        }
        mLeftSide.clear();
    }

    /**
     * Balance an equation to have only one term on the left side.
     * The preference is to first pick an unconstrained variable,
     * then a slack variable, then an error variable.
     */
    public void balance() {
        if (mLeftSide.size() == 0 && mRightSide.size() == 0) {
            return;
        }
        mCurrentSide = mLeftSide;
        for (int i = 0, mLeftSideSize = mLeftSide.size(); i < mLeftSideSize; i++) {
            final EquationVariable v = mLeftSide.get(i);
            mRightSide.add(v.inverse());
        }
        mLeftSide.clear();
        simplifySide(mRightSide);
        EquationVariable found = null;
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            if (v.getType() == SolverVariable.Type.UNRESTRICTED) {
                found = v;
                break;
            }
        }
        if (found == null) {
            for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
                final EquationVariable v = mRightSide.get(i);
                if (v.getType() == SolverVariable.Type.SLACK) {
                    found = v;
                    break;
                }
            }
        }
        if (found == null) {
            for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
                final EquationVariable v = mRightSide.get(i);
                if (v.getType() == SolverVariable.Type.ERROR) {
                    found = v;
                    break;
                }
            }
        }
        if (found == null) {
            return;
        }
        mRightSide.remove(found);
        found.inverse();
        if (!found.getAmount().isOne()) {
            Amount foundAmount = found.getAmount();
            for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
                final EquationVariable v = mRightSide.get(i);
                v.getAmount().divide(foundAmount);
            }
            found.setAmount(new Amount(1));
        }
        simplifySide(mRightSide);
        mLeftSide.add(found);
    }

    /**
     * Check the equation to possibly remove null terms
     */
    private void removeNullTerms(ArrayList<EquationVariable> list) {
        boolean hasNullTerm = false;
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            final EquationVariable v = list.get(i);
            if (v.getAmount().isNull()) {
                hasNullTerm = true;
                break;
            }
        }
        if (hasNullTerm) {
            // if some elements are now zero, we need to remove them from the right side
            ArrayList<EquationVariable> newSide;
            newSide = new ArrayList<>();
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                final EquationVariable v = list.get(i);
                if (!v.getAmount().isNull()) {
                    newSide.add(v);
                }
            }
            list.clear();
            list.addAll(newSide);
        }
    }

    /**
     * Pivot this equation on the variable --
     * e.g. the variable will be the only term on the left side of the equation.
     *
     * @param variable variable pivoted on
     */
    public void pivot(SolverVariable variable) {
        if (mLeftSide.size() == 1
                && mLeftSide.get(0).getSolverVariable() == variable) {
            // no-op, we're already pivoted.
            return;
        }
        for (int i = 0, mLeftSideSize = mLeftSide.size(); i < mLeftSideSize; i++) {
            final EquationVariable v = mLeftSide.get(i);
            mRightSide.add(v.inverse());
        }
        mLeftSide.clear();
        simplifySide(mRightSide);
        EquationVariable found = null;
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            if (v.getSolverVariable() == variable) {
                found = v;
                break;
            }
        }
        if (found != null) {
            mRightSide.remove(found);
            found.inverse();
            if (!found.getAmount().isOne()) {
                Amount foundAmount = found.getAmount();
                for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
                    final EquationVariable v = mRightSide.get(i);
                    v.getAmount().divide(foundAmount);
                }
                found.setAmount(new Amount(1));
            }
            mLeftSide.add(found);
        }
    }

    /**
     * Returns true if the constant is negative
     *
     * @return true if the constant is negative.
     */
    public boolean hasNegativeConstant() {
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            if (v.isConstant()) {
                if (v.getAmount().isNegative()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * If present, returns the constant on the right side of the equation.
     * The equation is expected to be balanced before using this function.
     *
     * @return The equation constant
     */
    public Amount getConstant() {
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            if (v.isConstant()) {
                return v.getAmount();
            }
        }
        return null;
    }

    /**
     * Inverse the equation (multiply both left and right terms by -1)
     */
    public void inverse() {
        Amount amount = new Amount(-1);
        for (int i = 0, mLeftSideSize = mLeftSide.size(); i < mLeftSideSize; i++) {
            final EquationVariable v = mLeftSide.get(i);
            v.multiply(amount);
        }
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            v.multiply(amount);
        }
    }

    /**
     * Returns the first unconstrained variable encountered in this equation
     *
     * @return an unconstrained variable or null if none are found
     */
    public EquationVariable getFirstUnconstrainedVariable() {
        for (int i = 0, mLeftSideSize = mLeftSide.size(); i < mLeftSideSize; i++) {
            final EquationVariable v = mLeftSide.get(i);
            if (v.getType() == SolverVariable.Type.UNRESTRICTED) {
                return v;
            }
        }
        for (int i = 0, mRightSideSize = mRightSide.size(); i < mRightSideSize; i++) {
            final EquationVariable v = mRightSide.get(i);
            if (v.getType() == SolverVariable.Type.UNRESTRICTED) {
                return v;
            }
        }
        return null;
    }

    /**
     * Returns the basic variable of the equation
     *
     * @return basic variable
     */
    public EquationVariable getLeftVariable() {
        if (mLeftSide.size() == 1) {
            return mLeftSide.get(0);
        }
        return null;
    }

    /**
     * Replace the variable v in this equation (left or right side)
     * by the right side of the equation l
     *
     * @param v the variable to replace
     * @param l the equation we use to replace it with
     */
    public void replace(SolverVariable v, LinearEquation l) {
        replace(v, l, mLeftSide);
        replace(v, l, mRightSide);
    }

    /**
     * Convenience function to replace the variable v possibly contained inside list
     * by the right side of the equation l
     *
     * @param v    the variable to replace
     * @param l    the equation we use to replace it with
     * @param list the list of {@link EquationVariable} to work on
     */
    private void replace(SolverVariable v, LinearEquation l, ArrayList<EquationVariable> list) {
        EquationVariable toReplace = find(v, list);
        if (toReplace != null) {
            list.remove(toReplace);
            Amount amount = toReplace.getAmount();
            final ArrayList<EquationVariable> mRightSide1 = l.mRightSide;
            for (int i = 0, mRightSide1Size = mRightSide1.size(); i < mRightSide1Size; i++) {
                final EquationVariable lv = mRightSide1.get(i);
                list.add(new EquationVariable(amount, lv));
            }
        }
    }

    /**
     * Returns the {@link EquationVariable} associated to
     * the {@link SolverVariable} found in the
     * list of {@link EquationVariable}
     *
     * @param v    the variable to find
     * @param list list the list of {@link EquationVariable} to search in
     * @return the associated {@link EquationVariable}
     */
    private EquationVariable find(SolverVariable v, ArrayList<EquationVariable> list) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            final EquationVariable ev = list.get(i);
            if (ev.getSolverVariable() == v) {
                return ev;
            }
        }
        return null;
    }

    /**
     * Accessor for the right side of the equation.
     *
     * @return the equation's right side.
     */
    public ArrayList<EquationVariable> getRightSide() {
        return mRightSide;
    }

    /**
     * Returns true if this equation contains a give variable
     *
     * @param solverVariable the variable we are looking for
     * @return true if found, false if not.
     */
    public boolean contains(SolverVariable solverVariable) {
        if (find(solverVariable, mLeftSide) != null) {
            return true;
        }
        if (find(solverVariable, mRightSide) != null) {
            return true;
        }
        return false;
    }

    /**
     * Returns the {@link EquationVariable} associated with a given
     * {@link SolverVariable} in this equation
     *
     * @param solverVariable the variable we are looking for
     * @return the {@link EquationVariable} associated if found, otherwise null
     */
    public EquationVariable getVariable(SolverVariable solverVariable) {
        EquationVariable variable = find(solverVariable, mRightSide);
        if (variable != null) {
            return variable;
        }
        return find(solverVariable, mLeftSide);
    }

    /**
     * Add a constant to the current side of the equation
     *
     * @param amount the value of the constant
     * @return this
     */
    public LinearEquation var(int amount) {
        EquationVariable e = new EquationVariable(mSystem, amount);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add a fractional constant to the current side of the equation
     *
     * @param numerator   the value of the constant's numerator
     * @param denominator the value of the constant's denominator
     * @return this
     */
    public LinearEquation var(int numerator, int denominator) {
        EquationVariable e = new EquationVariable(new Amount(numerator, denominator));
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add an unrestricted variable to the current side of the equation
     *
     * @param name the name of the variable
     * @return this
     */
    public LinearEquation var(String name) {
        EquationVariable e = new EquationVariable(mSystem, name, SolverVariable.Type.UNRESTRICTED);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add an unrestricted variable to the current side of the equation
     *
     * @param amount the amount of the variable
     * @param name   the name of the variable
     * @return this
     */
    public LinearEquation var(int amount, String name) {
        EquationVariable e = new EquationVariable(mSystem,
                amount, name, SolverVariable.Type.UNRESTRICTED);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add an unrestricted fractional variable to the current side of the equation
     *
     * @param numerator   the value of the variable's numerator
     * @param denominator the value of the variable's denominator
     * @param name        the name of the variable
     * @return this
     */
    public LinearEquation var(int numerator, int denominator, String name) {
        Amount amount = new Amount(numerator, denominator);
        EquationVariable e = new EquationVariable(mSystem,
                amount, name, SolverVariable.Type.UNRESTRICTED);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Convenience function to add a variable, based on {@link LinearEquation#var(String) var)}
     *
     * @param name the variable's name
     * @return this
     */
    public LinearEquation plus(String name) {
        var(name);
        return this;
    }

    /**
     * Convenience function to add a variable, based on {@link LinearEquation#var(String) var)}
     *
     * @param amount the variable's amount
     * @param name   the variable's name
     * @return this
     */
    public LinearEquation plus(int amount, String name) {
        var(amount, name);
        return this;
    }

    /**
     * Convenience function to add a negative variable,
     * based on {@link LinearEquation#var(String) var)}
     *
     * @param name the variable's name
     * @return this
     */
    public LinearEquation minus(String name) {
        var(-1, name);
        return this;
    }

    /**
     * Convenience function to add a negative variable,
     * based on {@link LinearEquation#var(String) var)}
     *
     * @param amount the variable's amount
     * @param name   the variable's name
     * @return this
     */
    public LinearEquation minus(int amount, String name) {
        var(-1 * amount, name);
        return this;
    }

    /**
     * Convenience function to add a constant, based on {@link LinearEquation#var(int) var)}
     *
     * @param amount the constant's amount
     * @return this
     */
    public LinearEquation plus(int amount) {
        var(amount);
        return this;
    }

    /**
     * Convenience function to add a negative constant,
     * based on {@link LinearEquation#var(int) var)}
     *
     * @param amount the constant's amount
     * @return this
     */
    public LinearEquation minus(int amount) {
        var(amount * -1);
        return this;
    }

    /**
     * Convenience function to add a fractional constant,
     * based on {@link LinearEquation#var(int) var)}
     *
     * @param numerator   the value of the variable's numerator
     * @param denominator the value of the variable's denominator
     * @return this
     */
    public LinearEquation plus(int numerator, int denominator) {
        var(numerator, denominator);
        return this;
    }

    /**
     * Convenience function to add a negative fractional constant,
     * based on {@link LinearEquation#var(int) var)}
     *
     * @param numerator   the value of the constant's numerator
     * @param denominator the value of the constant's denominator
     * @return this
     */
    public LinearEquation minus(int numerator, int denominator) {
        var(numerator * -1, denominator);
        return this;
    }

    /**
     * Add an error variable to the current side
     *
     * @param name     the name of the error variable
     * @param strength the strength of the error variable
     * @return this
     */
    public LinearEquation withError(String name, int strength) {
        EquationVariable e = new EquationVariable(mSystem,
                strength, name, SolverVariable.Type.ERROR);
        mCurrentSide.add(e);
        return this;
    }

    public LinearEquation withError(Amount amount, String name) {
        EquationVariable e = new EquationVariable(mSystem, amount, name, SolverVariable.Type.ERROR);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add an error variable to the current side
     *
     * @return this
     */
    public LinearEquation withError() {
        String name = getNextErrorVariableName();
        withError(name + "+", 1);
        withError(name + "-", -1);
        return this;
    }

    public LinearEquation withPositiveError() {
        String name = getNextErrorVariableName();
        withError(name + "+", 1);
        return this;
    }

    public EquationVariable addArtificialVar() {
        EquationVariable e = new EquationVariable(mSystem, 1,
                getNextArtificialVariableName(), SolverVariable.Type.ERROR);
        mCurrentSide.add(e);
        return e;
    }

    /**
     * Add an error variable to the current side
     *
     * @param strength the strength of the error variable
     * @return this
     */
    public LinearEquation withError(int strength) {
        withError(getNextErrorVariableName(), strength);
        return this;
    }

    /**
     * Add a slack variable to the current side
     *
     * @param name     the name of the slack variable
     * @param strength the strength of the slack variable
     * @return this
     */
    public LinearEquation withSlack(String name, int strength) {
        EquationVariable e = new EquationVariable(mSystem,
                strength, name, SolverVariable.Type.SLACK);
        mCurrentSide.add(e);
        return this;
    }

    public LinearEquation withSlack(Amount amount, String name) {
        EquationVariable e = new EquationVariable(mSystem, amount, name, SolverVariable.Type.SLACK);
        mCurrentSide.add(e);
        return this;
    }

    /**
     * Add a slack variable to the current side
     *
     * @return this
     */
    public LinearEquation withSlack() {
        withSlack(getNextSlackVariableName(), 1);
        return this;
    }

    /**
     * Add a slack variable to the current side
     *
     * @param strength the strength of the slack variable
     * @return this
     */
    public LinearEquation withSlack(int strength) {
        withSlack(getNextSlackVariableName(), strength);
        return this;
    }

    /**
     * Override the toString() method to display the linear equation
     */
    @Override
    public String toString() {
        String result = "";
        result = sideToString(mLeftSide);
        switch (mType) {
            case EQUALS: {
                result += "= ";
                break;
            }
            case LOWER_THAN: {
                result += "<= ";
                break;
            }
            case GREATER_THAN: {
                result += ">= ";
                break;
            }
        }
        result += sideToString(mRightSide);
        return result.trim();
    }

    /**
     * Returns a string representation of an array of {@link EquationVariable}
     *
     * @param side array of {@link EquationVariable}
     * @return a String representation of the array of variables
     */
    private String sideToString(ArrayList<EquationVariable> side) {
        String result = "";
        boolean first = true;
        for (int i = 0, sideSize = side.size(); i < sideSize; i++) {
            final EquationVariable v = side.get(i);
            if (first) {
                if (v.getAmount().isPositive()) {
                    result += v + " ";
                } else {
                    result += v.signString() + " " + v + " ";
                }
                first = false;
            } else {
                result += v.signString() + " " + v + " ";
            }
        }
        if (side.size() == 0) {
            result = "0";
        }
        return result;
    }
}
