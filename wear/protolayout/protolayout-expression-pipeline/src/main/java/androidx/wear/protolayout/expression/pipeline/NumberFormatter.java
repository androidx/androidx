/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import static java.lang.Math.max;

import android.icu.number.IntegerWidth;
import android.icu.number.LocalizedNumberFormatter;
import android.icu.number.NumberFormatter.GroupingStrategy;
import android.icu.number.Precision;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.icu.util.ULocale;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.wear.protolayout.expression.proto.DynamicProto.FloatFormatOp;
import androidx.wear.protolayout.expression.proto.DynamicProto.Int32FormatOp;

/** Utility to number formatting. */
class NumberFormatter {

    Formatter mFormatter;
    private static final String TAG = "NumberFormatter";
    private static final int DEFAULT_MIN_INTEGER_DIGITS = 1;
    private static final int DEFAULT_MAX_FRACTION_DIGITS = 3;

    @VisibleForTesting static final int MAX_INTEGER_PART_LENGTH = 15;
    @VisibleForTesting static final int MAX_FRACTION_PART_LENGTH = 15;

    private interface Formatter {
        String format(int value);

        String format(float value);
    }

    NumberFormatter(FloatFormatOp floatFormatOp, ULocale currentLocale) {
        int minIntegerDigits =
                floatFormatOp.hasMinIntegerDigits()
                        ? floatFormatOp.getMinIntegerDigits()
                        : DEFAULT_MIN_INTEGER_DIGITS;

        int minFractionDigits = floatFormatOp.getMinFractionDigits();
        if (minFractionDigits > MAX_FRACTION_PART_LENGTH) {
            logLargeParam("MinFractionDigits", minFractionDigits, MAX_FRACTION_PART_LENGTH);
            minFractionDigits = MAX_FRACTION_PART_LENGTH;
        }

        // maxFractionDigits should be larger or equal to minFractionDigits
        int maxFractionDigits =
                max(
                        floatFormatOp.hasMaxFractionDigits()
                                ? floatFormatOp.getMaxFractionDigits()
                                : DEFAULT_MAX_FRACTION_DIGITS,
                        minFractionDigits);

        if (maxFractionDigits > MAX_FRACTION_PART_LENGTH) {
            logLargeParam("MaxFractionDigits", maxFractionDigits, MAX_FRACTION_PART_LENGTH);
            maxFractionDigits = MAX_FRACTION_PART_LENGTH;
        }

        if (minIntegerDigits > MAX_INTEGER_PART_LENGTH) {
            logLargeParam("MinIntegerDigits", minIntegerDigits, MAX_INTEGER_PART_LENGTH);
            minIntegerDigits = MAX_INTEGER_PART_LENGTH;
        }

        mFormatter =
                buildFormatter(
                        minIntegerDigits,
                        minFractionDigits,
                        maxFractionDigits,
                        floatFormatOp.getGroupingUsed(),
                        currentLocale);
    }

    NumberFormatter(Int32FormatOp int32FormatOp, ULocale currentLocale) {
        int minIntegerDigits =
                int32FormatOp.hasMinIntegerDigits()
                        ? int32FormatOp.getMinIntegerDigits()
                        : DEFAULT_MIN_INTEGER_DIGITS;

        if (minIntegerDigits > MAX_INTEGER_PART_LENGTH) {
            logLargeParam("MinIntegerDigits", minIntegerDigits, MAX_INTEGER_PART_LENGTH);
            minIntegerDigits = MAX_INTEGER_PART_LENGTH;
        }

        mFormatter =
                buildFormatter(
                        minIntegerDigits,
                        /* minFractionDigits= */ 0,
                        /* maxFractionDigits= */ 0,
                        int32FormatOp.getGroupingUsed(),
                        currentLocale);
    }

    String format(float value) {
        return mFormatter.format(value);
    }

    String format(int value) {
        return mFormatter.format(value);
    }

    @RequiresApi(VERSION_CODES.R)
    private static class Api30Impl {
        @NonNull
        static String callFormatToString(LocalizedNumberFormatter mFmt, int value) {
            return mFmt.format(value).toString();
        }

        @NonNull
        static String callFormatToString(LocalizedNumberFormatter mFmt, float value) {
            return mFmt.format(value).toString();
        }

        @NonNull
        static LocalizedNumberFormatter buildLocalizedNumberFormatter(
                int minIntegerDigits,
                int minFractionDigits,
                int maxFractionDigits,
                boolean groupingUsed,
                ULocale currentLocale) {
            return android.icu.number.NumberFormatter.withLocale(currentLocale)
                    .grouping(groupingUsed ? GroupingStrategy.AUTO : GroupingStrategy.OFF)
                    .integerWidth(IntegerWidth.zeroFillTo(minIntegerDigits))
                    .precision(Precision.minMaxFraction(minFractionDigits, maxFractionDigits));
        }
    }

    private static void logLargeParam(String paramName, int oldValue, int newValue) {
        Log.w(
                TAG,
                String.format(
                        "%s (%d) is too large. Using the maximum allowed value instead: %d",
                        paramName, oldValue, newValue));
    }

    private static Formatter buildFormatter(
            int minIntegerDigits,
            int minFractionDigits,
            int maxFractionDigits,
            boolean groupingUsed,
            ULocale currentLocale) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            return new Formatter() {
                final LocalizedNumberFormatter mFmt =
                        Api30Impl.buildLocalizedNumberFormatter(
                                minIntegerDigits,
                                minFractionDigits,
                                maxFractionDigits,
                                groupingUsed,
                                currentLocale);

                @Override
                public String format(int value) {
                    return Api30Impl.callFormatToString(mFmt, value);
                }

                @Override
                public String format(float value) {
                    return Api30Impl.callFormatToString(mFmt, value);
                }
            };

        } else {
            return new Formatter() {
                final DecimalFormat mFmt =
                        buildDecimalFormat(
                                minIntegerDigits,
                                minFractionDigits,
                                maxFractionDigits,
                                groupingUsed,
                                currentLocale);

                @Override
                public String format(int value) {
                    return mFmt.format(value);
                }

                @Override
                public String format(float value) {
                    return mFmt.format(value);
                }
            };
        }
    }

    static DecimalFormat buildDecimalFormat(
            int minIntegerDigits,
            int minFractionDigits,
            int maxFractionDigits,
            boolean groupingUsed,
            ULocale currentLocale) {
        DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(currentLocale);
        decimalFormat.setMinimumIntegerDigits(minIntegerDigits);
        decimalFormat.setGroupingUsed(groupingUsed);
        decimalFormat.setMaximumFractionDigits(maxFractionDigits);
        decimalFormat.setMinimumFractionDigits(minFractionDigits);
        return decimalFormat;
    }
}
