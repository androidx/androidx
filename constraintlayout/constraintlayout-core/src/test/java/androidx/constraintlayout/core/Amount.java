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
 * Represents the amount of a given {@link EquationVariable variable}, can be fractional.
 */
class Amount {
    private int mNumerator = 0;
    private int mDenominator = 1;

    /**
     * Base constructor, set the numerator and denominator.
     *
     * @param numerator   the numerator
     * @param denominator the denominator
     */
    Amount(int numerator, int denominator) {
        mNumerator = numerator;
        mDenominator = denominator;
        simplify();
    }

    /**
     * Alternate constructor, set the numerator, with the denominator set to one.
     *
     * @param numerator the amount's value
     */
    Amount(int numerator) {
        mNumerator = numerator;
        mDenominator = 1;
    }

    Amount(Amount amount) {
        mNumerator = amount.mNumerator;
        mDenominator = amount.mDenominator;
        simplify();
    }

    /**
     * Set the numerator and denominator directly
     *
     * @param numerator   numerator
     * @param denominator denominator
     */
    public void set(int numerator, int denominator) {
        mNumerator = numerator;
        mDenominator = denominator;
        simplify();
    }

    /**
     * Add an amount to the current one.
     *
     * @param amount amount to add
     * @return this
     */
    public Amount add(Amount amount) {
        if (mDenominator == amount.mDenominator) {
            mNumerator += amount.mNumerator;
        } else {
            mNumerator = mNumerator * amount.mDenominator + amount.mNumerator * mDenominator;
            mDenominator = mDenominator * amount.mDenominator;
        }
        simplify();
        return this;
    }

    /**
     * Add an integer amount
     *
     * @param amount amount to add
     * @return this
     */
    public Amount add(int amount) {
        mNumerator += amount * mDenominator;
        return this;
    }

    /**
     * Subtract an amount to the current one.
     *
     * @param amount amount to subtract
     * @return this
     */
    public Amount subtract(Amount amount) {
        if (mDenominator == amount.mDenominator) {
            mNumerator -= amount.mNumerator;
        } else {
            mNumerator = mNumerator * amount.mDenominator - amount.mNumerator * mDenominator;
            mDenominator = mDenominator * amount.mDenominator;
        }
        simplify();
        return this;
    }

    /**
     * Multiply an amount with the current one.
     *
     * @param amount amount to multiply by
     * @return this
     */
    public Amount multiply(Amount amount) {
        mNumerator = mNumerator * amount.mNumerator;
        mDenominator = mDenominator * amount.mDenominator;
        simplify();
        return this;
    }

    /**
     * Divide the current amount by the given amount.
     *
     * @param amount amount to divide by
     * @return this
     */
    public Amount divide(Amount amount) {
        int preN = mNumerator;
        int preD = mDenominator;
        mNumerator = mNumerator * amount.mDenominator;
        mDenominator = mDenominator * amount.mNumerator;
        simplify();
        return this;
    }

    /**
     * Inverse the current amount as a fraction (e.g. a/b becomes b/a)
     *
     * @return this
     */
    public Amount inverseFraction() {
        int n = mNumerator;
        mNumerator = mDenominator;
        mDenominator = n;
        simplify();
        return this;
    }

    /**
     * Inverse the current amount (positive to negative or negative to positive)
     *
     * @return this
     */
    public Amount inverse() {
        mNumerator *= -1;
        simplify();
        return this;
    }

    /**
     * Accessor for the numerator
     *
     * @return the numerator
     */
    public int getNumerator() {
        return mNumerator;
    }

    /**
     * Accessor for the denominator
     *
     * @return the denominator
     */
    public int getDenominator() {
        return mDenominator;
    }

    /**
     * Override equals method
     *
     * @param o compared object
     * @return true if the compared object is equals to this one (same numerator and denominator)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Amount)) {
            return false;
        }
        Amount a = (Amount) o;
        return mNumerator == a.mNumerator && mDenominator == a.mDenominator;
    }

    /**
     * Simplify the current amount. If the amount is fractional,
     * we calculate the GCD and divide numerator and denominator by it.
     * If both numerator and denominator are negative, turns things back
     * to positive. If only the denominator is negative, make it positive
     * and make the numerator negative instead.
     */
    private void simplify() {
        if (mNumerator < 0 && mDenominator < 0) {
            mNumerator *= -1;
            mDenominator *= -1;
        } else if (mNumerator >= 0 && mDenominator < 0) {
            mNumerator *= -1;
            mDenominator *= -1;
        }
        if (mDenominator > 1) {
            int commonDenominator;
            if (mDenominator == 2 && mNumerator % 2 == 0) {
                commonDenominator = 2;
            } else {
                commonDenominator = gcd(mNumerator, mDenominator);
            }
            mNumerator /= commonDenominator;
            mDenominator /= commonDenominator;
        }
    }

    /**
     * Iterative Binary GCD algorithm
     *
     * @param u first number
     * @param v second number
     * @return Greater Common Divisor
     */
    private static int gcd(int u, int v) {
        int shift;

        if (u < 0) {
            u *= -1;
        }

        if (v < 0) {
            v *= -1;
        }

        if (u == 0) {
            return v;
        }

        if (v == 0) {
            return u;
        }

        for (shift = 0; ((u | v) & 1) == 0; shift++) {
            u >>= 1;
            v >>= 1;
        }

        while ((u & 1) == 0) {
            u >>= 1;
        }

        do {
            while ((v & 1) == 0) {
                v >>= 1;
            }
            if (u > v) {
                int t = v;
                v = u;
                u = t;
            }
            v = v - u;
        } while (v != 0);
        return u << shift;
    }

    /**
     * Returns true if the Amount is equals to one
     *
     * @return true if the Amount is equals to one
     */
    public boolean isOne() {
        return (mNumerator == 1 && mDenominator == 1);
    }

    /**
     * Returns true if the Amount is equals to minus one
     *
     * @return true if the Amount is equals to minus one
     */
    public boolean isMinusOne() {
        return (mNumerator == -1 && mDenominator == 1);
    }

    /**
     * Returns true if the Amount is positive.
     *
     * @return true if the Amount is positive.
     */
    public boolean isPositive() {
        return (mNumerator >= 0 && mDenominator >= 0);
    }

    /**
     * Returns true if the Amount is negative.
     *
     * @return true if the Amount is negative.
     */
    public boolean isNegative() {
        return (mNumerator < 0);
    }

    /**
     * Returns true if the value is zero
     *
     * @return true if the value is zero
     */
    public boolean isNull() {
        return mNumerator == 0;
    }

    /**
     * Set the Amount to zero.
     */
    public void setToZero() {
        mNumerator = 0;
        mDenominator = 1;
    }

    /**
     * Returns the float value of the Amount
     *
     * @return the float value
     */
    public float toFloat() {
        if (mDenominator >= 1) {
            return mNumerator / (float) mDenominator;
        }
        return 0;
    }

    /**
     * Override the toString() method to display the amount (possibly as a fraction)
     *
     * @return formatted string
     */
    @Override
    public String toString() {
        if (mDenominator == 1) {
            if (mNumerator == 1 || mNumerator == -1) {
                return "";
            }
            if (mNumerator < 0) {
                return "" + (mNumerator * -1);
            }
            return "" + mNumerator;
        }
        if (mNumerator < 0) {
            return "" + (mNumerator * -1) + "/" + mDenominator;
        }
        return "" + mNumerator + "/" + mDenominator;
    }

    public String valueString() {
        if (mDenominator == 1) {
            return "" + mNumerator;
        }
        return "" + mNumerator + "/" + mDenominator;
    }
}
