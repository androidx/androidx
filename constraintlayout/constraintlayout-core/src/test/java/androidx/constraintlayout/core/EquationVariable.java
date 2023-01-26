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
 * EquationVariable is used to represent a variable in a {@link LinearEquation LinearEquation}
 */
class EquationVariable {

    private Amount mAmount = null;
    private SolverVariable mVariable = null;

    /**
     * Base constructor
     *
     * @param system the {@link LinearSystem linear system} this equation variable belongs to
     * @param amount the amount associated with this variable
     * @param name   the variable name
     * @param type   the variable type
     */
    EquationVariable(LinearSystem system,
            Amount amount,
            String name,
            SolverVariable.Type type) {
        mAmount = amount;
        mVariable = system.getVariable(name, type);
    }

    /**
     * Alternate constructor, will set the type to be {@link SolverVariable.Type CONSTANT}
     *
     * @param amount the amount associated with this variable
     */
    EquationVariable(Amount amount) {
        mAmount = amount;
    }

    /**
     * Alternate constructor, will construct an amount given an integer number
     *
     * @param system the {@link LinearSystem linear system} this equation variable belongs to
     * @param amount the amount associated with this variable
     * @param name   the variable name
     * @param type   the variable type
     */
    EquationVariable(LinearSystem system,
            int amount, String name,
            SolverVariable.Type type) {
        mAmount = new Amount(amount);
        mVariable = system.getVariable(name, type);
    }

    /**
     * Alternate constructor, will set the type to be {@link SolverVariable.Type CONSTANT}
     *
     * @param system the {@link LinearSystem linear system} this equation variable belongs to
     * @param amount the amount associated with this variable
     */
    EquationVariable(LinearSystem system, int amount) {
        mAmount = new Amount(amount);
    }

    /**
     * Alternate constructor, will set the factor to be one by default
     *
     * @param system the {@link LinearSystem linear system} this equation variable belongs to
     * @param name   the variable name
     * @param type   the variable type
     */
    EquationVariable(LinearSystem system, String name, SolverVariable.Type type) {
        mAmount = new Amount(1);
        mVariable = system.getVariable(name, type);
    }

    /**
     * Alternate constructor, will multiply an amount to a given {@link EquationVariable}
     *
     * @param amount   the amount given
     * @param variable the variable we'll multiply
     */
    EquationVariable(Amount amount, EquationVariable variable) {
        mAmount = new Amount(amount);
        mAmount.multiply(variable.mAmount);
        mVariable = variable.getSolverVariable();
    }

    /**
     * Copy constructor
     *
     * @param v variable to copy
     */
    EquationVariable(EquationVariable v) {
        mAmount = new Amount(v.mAmount);
        mVariable = v.getSolverVariable();
    }

    /**
     * Accessor for the variable's name
     *
     * @return the variable's name
     */
    public String getName() {
        if (mVariable == null) {
            return null;
        }
        return mVariable.getName();
    }

    /**
     * Accessor for the variable's type
     *
     * @return the variable's type
     */
    public SolverVariable.Type getType() {
        if (mVariable == null) {
            return SolverVariable.Type.CONSTANT;
        }
        return mVariable.mType;
    }

    /**
     * Accessor for the {@link SolverVariable}
     *
     * @return the {@link SolverVariable}
     */
    public SolverVariable getSolverVariable() {
        return mVariable;
    }

    /**
     * Returns true if this is a constant
     *
     * @return true if a constant
     */
    public boolean isConstant() {
        return (mVariable == null);
    }

    /**
     * Accessor to retrieve the amount associated with this variable
     *
     * @return amount
     */
    public Amount getAmount() {
        return mAmount;
    }

    /**
     * Accessor to set the amount associated with this variable
     *
     * @param amount the amount associated with this variable
     */
    public void setAmount(Amount amount) {
        mAmount = amount;
    }

    /**
     * Inverse the current amount (from negative to positive or the reverse)
     *
     * @return this
     */
    public EquationVariable inverse() {
        mAmount.inverse();
        return this;
    }

    /**
     * Returns true if the variables are isCompatible (same type, same name)
     *
     * @param variable another variable to compare this one to
     * @return true if isCompatible.
     */
    public boolean isCompatible(EquationVariable variable) {
        if (isConstant()) {
            return variable.isConstant();
        } else if (variable.isConstant()) {
            return false;
        }
        return (variable.getSolverVariable() == getSolverVariable());
    }

    /**
     * Add an amount from another variable to this variable
     *
     * @param variable variable added
     */
    public void add(EquationVariable variable) {
        if (variable.isCompatible(this)) {
            mAmount.add(variable.mAmount);
        }
    }

    /**
     * Subtract an amount from another variable to this variable
     *
     * @param variable variable added
     */
    public void subtract(EquationVariable variable) {
        if (variable.isCompatible(this)) {
            mAmount.subtract(variable.mAmount);
        }
    }

    /**
     * Multiply an amount from another variable to this variable
     *
     * @param variable variable multiplied
     */
    public void multiply(EquationVariable variable) {
        multiply(variable.getAmount());
    }

    /**
     * Multiply this variable by a given amount
     *
     * @param amount specified amount multiplied
     */
    public void multiply(Amount amount) {
        mAmount.multiply(amount);
    }

    /**
     * Divide an amount from another variable to this variable
     *
     * @param variable variable dividing
     */
    public void divide(EquationVariable variable) {
        mAmount.divide(variable.mAmount);
    }

    /**
     * Override the toString() method to display the variable
     */
    @Override
    public String toString() {
        if (isConstant()) {
            return "" + mAmount;
        }
        if (mAmount.isOne() || mAmount.isMinusOne()) {
            return "" + mVariable;
        }
        return "" + mAmount + " " + mVariable;
    }

    /**
     * Returns a string displaying the sign of the variable (positive or negative, e.g. + or -)
     *
     * @return sign of the variable as a string, either + or -
     */
    public String signString() {
        if (mAmount.isPositive()) {
            return "+";
        }
        return "-";
    }

}
