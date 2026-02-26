/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.utilities;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;

/** Utilities for string manipulation */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class StringUtils {
    public static final byte GROUPING_NONE = 0; // e.g. 1234567890.12
    public static final byte GROUPING_BY3 = 1;   // e.g. 1,234,567,890.12
    public static final byte GROUPING_BY4 = 2;  // e.g. 12,3456,7890.12
    public static final byte GROUPING_BY32 = 3; // e.g. 1,23,45,67,890.12

    public static final byte SEPARATOR_COMMA_PERIOD = 0; // e.g. 123,456.12
    public static final byte SEPARATOR_PERIOD_COMMA = 1; // e.g. 123.456,12
    public static final byte SEPARATOR_SPACE_COMMA = 2;  // e.g. 123 456,12
    public static final byte SEPARATOR_UNDER_PERIOD = 3; // e.g. 123_456.12

    public static final int NO_OPTIONS = 0;         // e.g. -890.12
    public static final int NEGATIVE_PARENTHESES = 1; // e.g. (890.12)
    public static final int ROUNDING = 2; // Default is simple clipping
    public static final int POINT_ZERO = 4; // Default is simple clipping


    public static final char PAD_NONE = 0;
    public static final char PAD_ZERO = '0';
    public static final char PAD_SPACE = ' ';

    private StringUtils() {
    }

    /**
     * Converts a float into a string. Providing a defined number of characters before and after the
     * decimal point.
     *
     * @param value              The value to convert to string
     * @param beforeDecimalPoint digits before the decimal point
     * @param afterDecimalPoint  digits after the decimal point
     * @param pre                character to pad width 0 = no pad typically ' ' or '0'
     * @param post               character to pad width 0 = no pad typically ' ' or '0'
     * @return The formatted string representation of the float.
     */
    @NonNull
    public static String floatToString(
            float value, int beforeDecimalPoint, int afterDecimalPoint, char pre, char post) {
        boolean isNeg = value < 0;
        if (isNeg) {
            value = -value;
        }
        int integerPart = (int) value;
        float fractionalPart = value % 1;

        // Convert integer part to string and pad with spaces
        String integerPartString = String.valueOf(integerPart);
        int iLen = integerPartString.length();
        if (iLen < beforeDecimalPoint) {
            int spacesToPad = beforeDecimalPoint - iLen;
            if (pre != 0) {
                char[] pad = new char[spacesToPad];
                Arrays.fill(pad, pre);
                integerPartString = new String(pad) + integerPartString;
            }

        } else if (iLen > beforeDecimalPoint) {
            integerPartString = integerPartString.substring(iLen - beforeDecimalPoint);
        }
        if (afterDecimalPoint == 0) {
            return (isNeg ? "-" : "") + integerPartString;
        }
        // Convert fractional part to string and pad with zeros

        for (int i = 0; i < afterDecimalPoint; i++) {
            fractionalPart *= 10;
        }
        fractionalPart = Math.round(fractionalPart);

        for (int i = 0; i < afterDecimalPoint; i++) {
            fractionalPart *= .1F;
        }

        String fact = Float.toString(fractionalPart);
        fact = fact.substring(2, Math.min(fact.length(), afterDecimalPoint + 2));
        int trim = fact.length();
        for (int i = fact.length() - 1; i >= 0; i--) {
            if (fact.charAt(i) != '0') {
                break;
            }
            trim--;
        }
        if (trim != fact.length()) {
            fact = fact.substring(0, trim);
        }
        int len = fact.length();
        if (post != 0 && len < afterDecimalPoint) {
            char[] c = new char[afterDecimalPoint - len];
            Arrays.fill(c, post);
            fact = fact + new String(c);
        }

        return (isNeg ? "-" : "") + integerPartString + "." + fact;
    }


    /**
     * Converts a float into a string. Providing a defined number of characters
     * before and after the
     * decimal point.
     *
     * @param value              The value to convert to string
     * @param beforeDecimalPoint digits before the decimal point
     * @param afterDecimalPoint  digits after the decimal point
     * @param pre                character to pad width 0 = no pad typically ' ' or '0'
     * @param post               character to pad width 0 = no pad typically ' ' or '0'
     * @return The formatted string representation of the float.
     */
    @NonNull
    public static String floatToString(
            float value,
            int beforeDecimalPoint,
            int afterDecimalPoint,
            char pre,
            char post,
            byte separator,
            byte grouping,
            int options) {
        char groupSep = ',';
        char decSep = '.';
        switch (separator) {
            case SEPARATOR_PERIOD_COMMA:
                groupSep = '.';
                decSep = ',';
                break;
            case SEPARATOR_SPACE_COMMA:
                groupSep = ' ';
                decSep = ',';
                break;
            case SEPARATOR_UNDER_PERIOD:
                groupSep = '_';
                decSep = '.';
                break;
            // default is SEPARATOR_COMMA_PERIOD
        }
        boolean useParenthesesForNeg = (options & NEGATIVE_PARENTHESES) != 0;
        boolean rounding = (options & ROUNDING) != 0;
        boolean isNeg = value < 0;
        if (isNeg) {
            value = -value;
        }
        char[] chars = tochars(value, beforeDecimalPoint, afterDecimalPoint, rounding);
        String str = new String(chars);
        float fractionalPart = value % 1;

        // Convert integer part to string and pad with spaces
        String integerPartString = str.substring(0, str.indexOf('.'));
        if (grouping != GROUPING_NONE) {
            int len = integerPartString.length();
            switch (grouping) {
                case GROUPING_BY3:
                    for (int i = len - 3; i > 0; i -= 3) {
                        integerPartString = integerPartString.substring(0, i)
                                + groupSep + integerPartString.substring(i);
                    }
                    break;
                case GROUPING_BY4:
                    for (int i = len - 4; i > 0; i -= 4) {
                        integerPartString = integerPartString.substring(0, i)
                                + groupSep + integerPartString.substring(i);
                    }
                    break;
                case GROUPING_BY32:
                    for (int i = len - 3; i > 0; i -= 2) {
                        integerPartString = integerPartString.substring(0, i)
                                + groupSep + integerPartString.substring(i);
                    }
                    break;
            }
        }
        int iLen = integerPartString.length();
        if (iLen < beforeDecimalPoint) {
            int spacesToPad = beforeDecimalPoint - iLen;
            if (pre != 0) {
                char[] pad = new char[spacesToPad];
                Arrays.fill(pad, pre);
                integerPartString = new String(pad) + integerPartString;
            }

        } else if (iLen > beforeDecimalPoint) {
            integerPartString = integerPartString.substring(iLen - beforeDecimalPoint);
        }
        int trimAfter = afterDecimalPoint;
        if (iLen + afterDecimalPoint > 9) {
            trimAfter = Math.max(1, 9 - iLen);
        }
        if (post == 0) {
            afterDecimalPoint = trimAfter;
        }

        if (afterDecimalPoint == 0) {
            if (!isNeg) {
                return integerPartString;
            }
            if (useParenthesesForNeg) {
                return "(" + integerPartString + ")";
            }
            return "-" + integerPartString;
        }
        // Convert fractional part to string and pad with zeros

        for (int i = 0; i < trimAfter; i++) {
            fractionalPart *= 10;
        }
        fractionalPart = Math.round(fractionalPart);

        for (int i = 0; i < trimAfter; i++) {
            fractionalPart *= .1F;
        }

        String fact = Float.toString(fractionalPart);
        fact = fact.substring(2, Math.min(fact.length(), afterDecimalPoint + 2));
        int trim = fact.length();
        for (int i = fact.length() - 1; i > 0; i--) {
            if (fact.charAt(i) != '0') {
                break;
            }
            trim--;
        }
        if (trim != fact.length()) {
            fact = fact.substring(0, trim);
        }
        int len = fact.length();
        if (post != 0 && len < afterDecimalPoint) {
            char[] c = new char[afterDecimalPoint - len];
            Arrays.fill(c, post);
            fact = fact + new String(c);
        }
        if (!isNeg) {
            return integerPartString + decSep + fact;
        }
        if (useParenthesesForNeg) {
            return "(" + integerPartString + decSep + fact + ")";
        }
        return "-" + integerPartString + decSep + fact;
    }

    private static char[] tochars(float value,
            int beforeDecimalPoint,
            int afterDecimalPoint,
            boolean rounding) {
        boolean isNegative = false;
        if (value < 0) {
            isNegative = true;
            value = -value;
        }

        // Calculate power of 10 to scale the fractional part.
        long powerOf10 = 1;
        for (int i = 0; i < afterDecimalPoint; i++) {
            powerOf10 *= 10;
        }

        // Apply rounding to the original value. This is more accurate.
        if (rounding) {
            float roundingFactor = 0.5f;
            for (int i = 0; i < afterDecimalPoint; i++) {
                roundingFactor /= 10.0f;
            }
            value += roundingFactor;
        }

        // Separate integer and fractional parts
        long integerPart = (long) value;
        float fractionalPart = value - integerPart;

        // Convert the integer part to characters
        int intLength = 1;
        if (integerPart > 0) {
            long tempInt = integerPart;
            intLength = 0;
            while (tempInt > 0) {
                tempInt /= 10;
                intLength++;
            }
        }
        int actualBefore = Math.min(beforeDecimalPoint, intLength);

        char[] integerChars = new char[actualBefore];
        long tempInt = integerPart;
        for (int i = actualBefore - 1; i >= 0; i--) {
            integerChars[i] = (char) ('0' + tempInt % 10);
            tempInt /= 10;
        }

        // Convert the fractional part to characters, after scaling
        long tempFrac = (long) (fractionalPart * powerOf10);

        int fracLength = 0;
        if (afterDecimalPoint > 0) {
            long temp = tempFrac;
            if (temp == 0) {
                fracLength = 1;
            } else {
                while (temp > 0 && temp % 10 == 0) {
                    temp /= 10;
                }
                if (temp > 0) {
                    // Manual length calculation to avoid String.valueOf
                    long t = temp;
                    while (t > 0) {
                        t /= 10;
                        fracLength++;
                    }
                } else {
                    fracLength = 0;
                }
            }
        }

        int actualAfter = Math.min(afterDecimalPoint, fracLength);

        char[] fractionalChars = new char[actualAfter];
        tempFrac = (long) (fractionalPart * powerOf10);
        for (int i = actualAfter - 1; i >= 0; i--) {
            fractionalChars[i] = (char) ('0' + tempFrac % 10);
            tempFrac /= 10;
        }

        // Combine parts into the final array
        int totalLength = (isNegative ? 1 : 0) + actualBefore + 1 + actualAfter;
        char[] result = new char[totalLength];
        int currentIndex = 0;

        if (isNegative) {
            result[currentIndex++] = '-';
        }

        for (int i = 0; i < actualBefore; i++) {
            result[currentIndex++] = integerChars[i];
        }

        result[currentIndex++] = '.';

        for (int i = 0; i < actualAfter; i++) {
            result[currentIndex++] = fractionalChars[i];
        }

        return result;
    }
}
