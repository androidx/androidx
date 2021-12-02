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

package androidx.core.math;

/**
 * A utility class providing functions useful for common mathematical operations.
 */
public class MathUtils {

    private MathUtils() {}

    /**
     * See {@link Math#addExact(int, int)}.
     */
    public static int addExact(int x, int y) {
        // copied from Math.java
        int r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /**
     * See {@link Math#addExact(long, long)}.
     */
    public static long addExact(long x, long y) {
        // copied from Math.java
        long r = x + y;
        // HD 2-12 Overflow iff both arguments have the opposite sign of the result
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }


    /**
     * See {@link Math#subtractExact(int, int)}.
     */
    public static int subtractExact(int x, int y) {
        // copied from Math.java
        int r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("integer overflow");
        }
        return r;
    }

    /**
     * See {@link Math#subtractExact(long, long)}.
     */
    public static long subtractExact(long x, long y) {
        // copied from Math.java
        long r = x - y;
        // HD 2-12 Overflow iff the arguments have different signs and
        // the sign of the result is different than the sign of x
        if (((x ^ y) & (x ^ r)) < 0) {
            throw new ArithmeticException("long overflow");
        }
        return r;
    }

    /**
     * See {@link Math#multiplyExact(int, int)}.
     */
    public static int multiplyExact(int x, int y) {
        // copied from Math.java
        long r = (long) x * (long) y;
        if ((int) r != r) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) r;
    }

    /**
     * See {@link Math#multiplyExact(long, long)}.
     */
    public static long multiplyExact(long x, long y) {
        // copied from Math.java
        long r = x * y;
        long ax = Math.abs(x);
        long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (((y != 0) && (r / y != x)) || (x == Long.MIN_VALUE && y == -1)) {
                throw new ArithmeticException("long overflow");
            }
        }
        return r;
    }

    /**
     * See {@link Math#incrementExact(int)}.
     */
    public static int incrementExact(int a) {
        // copied from Math.java
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a + 1;
    }

    /**
     * See {@link Math#incrementExact(long)}.
     */
    public static long incrementExact(long a) {
        // copied from Math.java
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a + 1L;
    }

    /**
     * See {@link Math#decrementExact(int)}.
     */
    public static int decrementExact(int a) {
        // copied from Math.java
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return a - 1;
    }

    /**
     * See {@link Math#decrementExact(long)}.
     */
    public static long decrementExact(long a) {
        // copied from Math.java
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return a - 1L;
    }

    /**
     * See {@link Math#negateExact(int)}.
     */
    public static int negateExact(int a) {
        // copied from Math.java
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        }

        return -a;
    }

    /**
     * See {@link Math#negateExact(long)}.
     */
    public static long negateExact(long a) {
        // copied from Math.java
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow");
        }

        return -a;
    }

    /**
     * See {@link Math#toIntExact(long)}.
     */
    public static int toIntExact(long value) {
        // copied from Math.java
        if ((int) value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int) value;
    }

    /**
     * This method takes a numerical value and ensures it fits in a given numerical range. If the
     * number is smaller than the minimum required by the range, then the minimum of the range will
     * be returned. If the number is higher than the maximum allowed by the range then the maximum
     * of the range will be returned.
     *
     * @param value the value to be clamped.
     * @param min minimum resulting value.
     * @param max maximum resulting value.
     *
     * @return the clamped value.
     */
    public static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * This method takes a numerical value and ensures it fits in a given numerical range. If the
     * number is smaller than the minimum required by the range, then the minimum of the range will
     * be returned. If the number is higher than the maximum allowed by the range then the maximum
     * of the range will be returned.
     *
     * @param value the value to be clamped.
     * @param min minimum resulting value.
     * @param max maximum resulting value.
     *
     * @return the clamped value.
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * This method takes a numerical value and ensures it fits in a given numerical range. If the
     * number is smaller than the minimum required by the range, then the minimum of the range will
     * be returned. If the number is higher than the maximum allowed by the range then the maximum
     * of the range will be returned.
     *
     * @param value the value to be clamped.
     * @param min minimum resulting value.
     * @param max maximum resulting value.
     *
     * @return the clamped value.
     */
    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }

    /**
     * This method takes a numerical value and ensures it fits in a given numerical range. If the
     * number is smaller than the minimum required by the range, then the minimum of the range will
     * be returned. If the number is higher than the maximum allowed by the range then the maximum
     * of the range will be returned.
     *
     * @param value the value to be clamped.
     * @param min minimum resulting value.
     * @param max maximum resulting value.
     *
     * @return the clamped value.
     */
    public static long clamp(long value, long min, long max) {
        if (value < min) {
            return min;
        } else if (value > max) {
            return max;
        }
        return value;
    }
}
