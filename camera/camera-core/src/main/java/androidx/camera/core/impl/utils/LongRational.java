/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.core.impl.utils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * The rational data type of EXIF tag. Contains a pair of longs representing the
 * numerator and denominator of a Rational number.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class LongRational {

    private final long mNumerator;
    private final long mDenominator;

    /**
     * Create a Rational with a given numerator and denominator.
     */
    LongRational(long nominator, long denominator) {
        mNumerator = nominator;
        mDenominator = denominator;
    }

    /**
     * Creates a Rational from a double.
     */
    LongRational(double value) {
        this((long) (value * 10000), 10000);
    }

    /**
     * Gets the numerator of the rational.
     */
    long getNumerator() {
        return mNumerator;
    }

    /**
     * Gets the denominator of the rational
     */
    long getDenominator() {
        return mDenominator;
    }

    /**
     * Gets the rational value as type double. Will cause a divide-by-zero error
     * if the denominator is 0.
     */
    double toDouble() {
        return mNumerator / (double) mDenominator;
    }

    @NonNull
    @Override
    public String toString() {
        return mNumerator + "/" + mDenominator;
    }
}
