/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.util;

import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.Locale;

/**
 * Simple static methods to be called at the start of your own methods to verify
 * correct arguments and state.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Preconditions {
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensures that an expression checking an argument is true.
     *
     * @param expression the expression to check
     * @param errorMessage the exception message to use if the check fails; will
     *     be converted to a string using {@link String#valueOf(Object)}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression, @NonNull Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    /**
     * Ensures that an expression checking an argument is true.
     *
     * @param expression the expression to check
     * @param messageTemplate a printf-style message template to use if the check fails; will
     *     be converted to a string using {@link String#format(String, Object...)}
     * @param messageArgs arguments for {@code messageTemplate}
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(
            final boolean expression,
            final @NonNull String messageTemplate,
            final @NonNull Object... messageArgs) {
        if (!expression) {
            throw new IllegalArgumentException(String.format(messageTemplate, messageArgs));
        }
    }

    /**
     * Ensures that an string reference passed as a parameter to the calling
     * method is not empty.
     *
     * @param string an string reference
     * @return the string reference that was validated
     * @throws IllegalArgumentException if {@code string} is empty
     */
    public static @NonNull <T extends CharSequence> T checkStringNotEmpty(
            @Nullable final T string) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException();
        }
        return string;
    }

    /**
     * Ensures that an string reference passed as a parameter to the calling
     * method is not empty.
     *
     * @param string an string reference
     * @param errorMessage the exception message to use if the check fails; will
     *     be converted to a string using {@link String#valueOf(Object)}
     * @return the string reference that was validated
     * @throws IllegalArgumentException if {@code string} is empty
     */
    public static @NonNull <T extends CharSequence> T checkStringNotEmpty(
            @Nullable final T string, @NonNull final Object errorMessage) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
        return string;
    }

    /**
     * Ensures that an string reference passed as a parameter to the calling method is not empty.
     *
     * @param string an string reference
     * @param messageTemplate a printf-style message template to use if the check fails; will be
     *     converted to a string using {@link String#format(String, Object...)}
     * @param messageArgs arguments for {@code messageTemplate}
     * @return the string reference that was validated
     * @throws IllegalArgumentException if {@code string} is empty
     */
    public static @NonNull <T extends CharSequence> T checkStringNotEmpty(
            @Nullable final T string, @NonNull final String messageTemplate,
            @NonNull final Object... messageArgs) {
        if (TextUtils.isEmpty(string)) {
            throw new IllegalArgumentException(String.format(messageTemplate, messageArgs));
        }
        return string;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static @NonNull <T> T checkNotNull(@Nullable T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * Ensures that an object reference passed as a parameter to the calling
     * method is not null.
     *
     * @param reference an object reference
     * @param errorMessage the exception message to use if the check fails; will
     *     be converted to a string using {@link String#valueOf(Object)}
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static @NonNull <T> T checkNotNull(@Nullable T reference, @NonNull Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }

    /**
     * Ensures the truth of an expression involving the state of the calling
     * instance, but not involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @param message exception message
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(boolean expression, @Nullable String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Ensures the truth of an expression involving the state of the calling
     * instance, but not involving any parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if {@code expression} is false
     */
    public static void checkState(final boolean expression) {
        checkState(expression, null);
    }

    /**
     * Check the requested flags, throwing if any requested flags are outside the allowed set.
     *
     * @return the validated requested flags.
     */
    public static int checkFlagsArgument(final int requestedFlags, final int allowedFlags) {
        if ((requestedFlags & allowedFlags) != requestedFlags) {
            throw new IllegalArgumentException("Requested flags 0x"
                    + Integer.toHexString(requestedFlags) + ", but only 0x"
                    + Integer.toHexString(allowedFlags) + " are allowed");
        }
        return requestedFlags;
    }

    /**
     * Ensures that that the argument numeric value is non-negative.
     *
     * @param value a numeric int value
     * @param errorMessage the exception message to use if the check fails
     * @return the validated numeric value
     * @throws IllegalArgumentException if {@code value} was negative
     */
    public static @IntRange(from = 0) int checkArgumentNonnegative(final int value,
            @Nullable String errorMessage) {
        if (value < 0) {
            throw new IllegalArgumentException(errorMessage);
        }

        return value;
    }

    /**
     * Ensures that that the argument numeric value is non-negative.
     *
     * @param value a numeric int value
     *
     * @return the validated numeric value
     * @throws IllegalArgumentException if {@code value} was negative
     */
    public static @IntRange(from = 0) int checkArgumentNonnegative(final int value) {
        if (value < 0) {
            throw new IllegalArgumentException();
        }

        return value;
    }

    /**
     * Ensures that the argument int value is within the inclusive range.
     *
     * @param value a int value
     * @param lower the lower endpoint of the inclusive range
     * @param upper the upper endpoint of the inclusive range
     * @param valueName the name of the argument to use if the check fails
     *
     * @return the validated int value
     *
     * @throws IllegalArgumentException if {@code value} was not within the range
     */
    public static int checkArgumentInRange(int value, int lower, int upper,
            @NonNull String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%d, %d] (too low)", valueName, lower, upper));
        } else if (value > upper) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%d, %d] (too high)", valueName, lower, upper));
        }

        return value;
    }

    /**
     * Ensures that the argument long value is within the inclusive range.
     *
     * @param value a long value
     * @param lower the lower endpoint of the inclusive range
     * @param upper the upper endpoint of the inclusive range
     * @param valueName the name of the argument to use if the check fails
     *
     * @return the validated long value
     *
     * @throws IllegalArgumentException if {@code value} was not within the range
     */
    public static long checkArgumentInRange(long value, long lower, long upper,
            @NonNull String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%d, %d] (too low)", valueName, lower, upper));
        } else if (value > upper) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%d, %d] (too high)", valueName, lower, upper));
        }

        return value;
    }

    /**
     * Ensures that the argument float value is within the inclusive range.
     *
     * @param value a float value
     * @param lower the lower endpoint of the inclusive range
     * @param upper the upper endpoint of the inclusive range
     * @param valueName the name of the argument to use if the check fails
     *
     * @return the validated float value
     *
     * @throws IllegalArgumentException if {@code value} was not within the range
     */
    public static float checkArgumentInRange(float value, float lower, float upper,
            @NonNull String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%f, %f] (too low)", valueName, lower, upper));
        } else if (value > upper) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%f, %f] (too high)", valueName, lower, upper));
        }

        return value;
    }

    /**
     * Ensures that the argument double value is within the inclusive range.
     *
     * @param value a double value
     * @param lower the lower endpoint of the inclusive range
     * @param upper the upper endpoint of the inclusive range
     * @param valueName the name of the argument to use if the check fails
     *
     * @return the validated double value
     *
     * @throws IllegalArgumentException if {@code value} was not within the range
     */
    public static double checkArgumentInRange(double value, double lower, double upper,
            @NonNull String valueName) {
        if (value < lower) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%f, %f] (too low)", valueName, lower, upper));
        } else if (value > upper) {
            throw new IllegalArgumentException(
                    String.format(Locale.US,
                            "%s is out of range of [%f, %f] (too high)", valueName, lower, upper));
        }

        return value;
    }

    private Preconditions() {
    }
}
