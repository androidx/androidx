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
     * Returns the sum of its arguments, throwing an exception if the result overflows an
     * {@code int}.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int addExact(int x, int y) {
        int sum = x + y;
        // If x and y have the same sign, their sum should have the same sign as well
        if ((x >= 0 == y >= 0) && (x >= 0 != sum >= 0)) {
            throw new ArithmeticException("integer overflow");
        } else {
            return sum;
        }
    }

    /**
     * Returns the sum of its arguments, throwing an exception if the result overflows a
     * {@code long}.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long addExact(long x, long y) {
        long sum = x + y;
        // If x and y have the same sign, their sum should have the same sign as well
        if ((x >= 0 == y >= 0) && (x >= 0 != sum >= 0)) {
            throw new ArithmeticException("integer overflow");
        } else {
            return sum;
        }
    }

    /**
     * Returns the difference of the arguments, throwing an exception if the result overflows an
     * {@code int}.
     *
     * @param x the first value
     * @param y the second value to subtract from the first
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int subtractExact(int x, int y) {
        int difference = x - y;
        // If only one of x or y is negative, the difference should have the same sign as x
        if ((x < 0 != y < 0) && (x < 0 != difference < 0)) {
            throw new ArithmeticException("integer overflow");
        }
        return difference;
    }

    /**
     * Returns the difference of the arguments, throwing an exception if the result overflows a
     * {@code long}.
     *
     * @param x the first value
     * @param y the second value to subtract from the first
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long subtractExact(long x, long y) {
        long difference = x - y;
        // If only one of x or y is negative, the difference should have the same sign as x
        if ((x < 0 != y < 0) && (x < 0 != difference < 0)) {
            throw new ArithmeticException("integer overflow");
        }
        return difference;
    }

    /**
     * Returns the product of the arguments, throwing an exception if the result overflows an
     * {@code int}.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int multiplyExact(int x, int y) {
        int product = x * y;
        // Dividing back by one of x or y should get the other back unless there was overflow
        if (x != 0 && y != 0 && (product / x != y || product / y != x)) {
            throw new ArithmeticException("integer overflow");
        }
        return product;
    }

    /**
     * Returns the product of the arguments, throwing an exception if the result overflows a
     * {@code long}.
     *
     * @param x the first value
     * @param y the second value
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long multiplyExact(long x, long y) {
        long product = x * y;
        // Dividing back by one of x or y should get the other back unless there was overflow
        if (x != 0 && y != 0 && (product / x != y || product / y != x)) {
            throw new ArithmeticException("integer overflow");
        }
        return product;
    }

    /**
     * Returns the argument incremented by one, throwing an exception if the result overflows an
     * {@code int}. The overflow only occurs for {@linkplain Integer#MAX_VALUE the maximum value}.
     *
     * @param a the value to increment
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int incrementExact(int a) {
        if (a == Integer.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return a + 1;
        }
    }

    /**
     * Returns the argument incremented by one, throwing an exception if the result overflows a
     * {@code long}. The overflow only occurs for {@linkplain Long#MAX_VALUE the maximum value}.
     *
     * @param a the value to increment
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long incrementExact(long a) {
        if (a == Long.MAX_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return a + 1;
        }
    }

    /**
     * Returns the argument decremented by one, throwing an exception if the result overflows an
     * {@code int}. The overflow only occurs for {@linkplain Integer#MIN_VALUE the minimum value}.
     *
     * @param a the value to decrement
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int decrementExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return a - 1;
        }
    }

    /**
     * Returns the argument decremented by one, throwing an exception if the result overflows a
     * {@code long}. The overflow only occurs for {@linkplain Long#MIN_VALUE the minimum value}.
     *
     * @param a the value to decrement
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long decrementExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return a - 1;
        }
    }

    /**
     * Returns the negation of the argument, throwing an exception if the result overflows an
     * {@code int}. The overflow only occurs for {@linkplain Integer#MIN_VALUE the minimum value}.
     *
     * @param a the value to negate
     * @return the result
     * @throws ArithmeticException if the result overflows an int
     */
    public static int negateExact(int a) {
        if (a == Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return -a;
        }
    }

    /**
     * Returns the negation of the argument, throwing an exception if the result overflows a
     * {@code long}. The overflow only occurs for {@linkplain Long#MIN_VALUE the minimum value}.
     *
     * @param a the value to negate
     * @return the result
     * @throws ArithmeticException if the result overflows a long
     */
    public static long negateExact(long a) {
        if (a == Long.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return -a;
        }
    }

    /**
     * Returns the value of the {@code long} argument, throwing an exception if the value
     * overflows an {@code int}.
     *
     * @param value the long value
     * @return the argument as an int
     * @throws ArithmeticException if the {@code argument} overflows an int
     */
    public static int toIntExact(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new ArithmeticException("integer overflow");
        } else {
            return (int) value;
        }
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
