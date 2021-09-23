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

package android.support.wearable.complications;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.wear.watchface.complications.data.R;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Class to generate string representations of time differences.
 *
 * <p>Time differences greater than {@link Integer#MAX_VALUE} days are not supported.
 *
 * @hide
 * @see ComplicationText.TimeDifferenceBuilder
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TimeDifferenceText implements TimeDependentText {

    private static final int ONLY_SHOW_DAYS_THRESHOLD = 10;
    private static final int SHORT_CHARACTER_LIMIT = 7;

    private static final int MINIMUM_UNIT_PARCELED_IS_NULL = -1;

    private final long mReferencePeriodStart;
    private final long mReferencePeriodEnd;

    @ComplicationText.TimeDifferenceStyle
    private final int mStyle;

    private final boolean mShowNowText;

    @Nullable
    private final TimeUnit mMinimumUnit;

    public TimeDifferenceText(
            long referencePeriodStart,
            long referencePeriodEnd,
            @ComplicationText.TimeDifferenceStyle int style,
            boolean showNowText,
            @Nullable TimeUnit minimumUnit) {
        mReferencePeriodStart = referencePeriodStart;
        mReferencePeriodEnd = referencePeriodEnd;
        mStyle = style;
        mShowNowText = showNowText;
        mMinimumUnit = minimumUnit;
    }

    @NonNull
    @Override
    public CharSequence getTextAt(@NonNull Resources resources, long dateTimeMillis) {
        long timeDifference = getTimeDifference(dateTimeMillis);

        if (timeDifference == 0 && mShowNowText) {
            return resources.getString(R.string.time_difference_now);
        }

        switch (mStyle) {
            case ComplicationText.DIFFERENCE_STYLE_STOPWATCH:
                return buildStopwatchText(timeDifference, resources);
            case ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT:
                return buildShortSingleUnitText(timeDifference, resources);
            case ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT:
                return shortDualUnlessTooLong(timeDifference, resources);
            case ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT:
                return buildWordsSingleUnitText(timeDifference, resources);
            case ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT:
                return wordsSingleUnlessTooLong(timeDifference, resources);
            default:
                return buildShortSingleUnitText(timeDifference, resources);
        }
    }

    @Override
    public boolean returnsSameText(long firstDateTimeMillis, long secondDateTimeMillis) {
        long precision = getPrecision();
        // If time / precision (rounded up) is the same in both cases, the text will be the same.
        return divRoundingUp(getTimeDifference(firstDateTimeMillis), precision)
                == divRoundingUp(getTimeDifference(secondDateTimeMillis), precision);
    }

    @Override
    public long getNextChangeTime(long fromTime) {
        long precision = getPrecision();
        return divRoundingUp(fromTime, precision) * precision + 1;
    }

    /**
     * Returns the time precision in milliseconds.
     */
    public long getPrecision() {
        long defaultPrecision;
        switch (mStyle) {
            case ComplicationText.DIFFERENCE_STYLE_STOPWATCH:
                defaultPrecision = TimeUnit.SECONDS.toMillis(1);
                break;
            case ComplicationText.DIFFERENCE_STYLE_SHORT_SINGLE_UNIT:
            case ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT:
            case ComplicationText.DIFFERENCE_STYLE_WORDS_SINGLE_UNIT:
            case ComplicationText.DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT:
            default:
                defaultPrecision = TimeUnit.MINUTES.toMillis(1);
        }
        if (mMinimumUnit == null) {
            return defaultPrecision;
        }
        return Math.max(defaultPrecision, mMinimumUnit.toMillis(1));
    }

    /** Returns the start of the reference period, as milliseconds since epoch. */
    long getReferencePeriodStart() {
        return mReferencePeriodStart;
    }

    /** Returns the end of the reference period, as milliseconds since epoch. */
    long getReferencePeriodEnd() {
        return mReferencePeriodEnd;
    }

    /** Returns the style to be used when formatting the text. */
    int getStyle() {
        return mStyle;
    }

    /** Returns true if text for "now" should be shown when within the reference period. */
    boolean shouldShowNowText() {
        return mShowNowText;
    }

    /** Returns the minimum unit specified, or {@code null} if none has been specified. */
    @Nullable
    public TimeUnit getMinimumUnit() {
        return mMinimumUnit;
    }

    private long getTimeDifference(long dateTimeMillis) {
        long timeDifference = 0;
        if (dateTimeMillis < mReferencePeriodStart) {
            timeDifference = mReferencePeriodStart - dateTimeMillis;
        } else if (dateTimeMillis > mReferencePeriodEnd) {
            timeDifference = dateTimeMillis - mReferencePeriodEnd;
        }
        return timeDifference;
    }

    private String buildShortSingleUnitText(long time, Resources res) {
        long timeRoundedToHours = roundUpToUnit(time, TimeUnit.HOURS);
        // First check if there are non-zero days. If there are, display just those.
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.DAYS) || days(timeRoundedToHours) > 0) {
            return buildShortDaysText(days(roundUpToUnit(time, TimeUnit.DAYS)), res);
        }

        long timeRoundedToMins = roundUpToUnit(time, TimeUnit.MINUTES);
        // Now we know there are no days, check if there are more than zero hours.
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.HOURS) || hours(timeRoundedToMins) > 0) {
            // Show just the number of hours.
            return buildShortHoursText(hours(timeRoundedToHours), res);
        }

        // No days or hours, so just show the minutes.
        return buildShortMinsText(minutes(timeRoundedToMins), res);
    }

    private String buildShortDualUnitText(long time, Resources res) {
        long timeRoundedToHours = roundUpToUnit(time, TimeUnit.HOURS);
        // If the number of days is more than a threshold, don't show the hours.
        // e.g. "10d" instead of "10d 2h"
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.DAYS)
                || days(timeRoundedToHours) >= ONLY_SHOW_DAYS_THRESHOLD) {
            return buildShortDaysText(days(roundUpToUnit(time, TimeUnit.DAYS)), res);
        }

        // Check if there are non-zero days. Must check this in minute precision, not hour
        // precision,
        // because we'd rather show "23h 5m" than "1d".
        long timeRoundedToMins = roundUpToUnit(time, TimeUnit.MINUTES);
        if (days(timeRoundedToMins) > 0) {
            // There are non-zero days, and this is displaying two units, so use time rounded to
            // hours.
            int hoursRoundedToHours = hours(timeRoundedToHours);
            if (hoursRoundedToHours > 0) {
                return buildShortDaysHoursText(days(timeRoundedToHours), hoursRoundedToHours, res);
            }
            // Zero hours, so just show the days ("1d" instead of "1d 0h").
            return buildShortDaysText(days(timeRoundedToHours), res);
        }

        // Zero days, so if minimum unit is hours, show just the hours.
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.HOURS)) {
            return buildShortHoursText(hours(timeRoundedToHours), res);
        }

        int hoursRoundedToMins = hours(timeRoundedToMins);
        int minutesRoundedToMins = minutes(timeRoundedToMins);
        if (hoursRoundedToMins > 0) {
            if (minutesRoundedToMins > 0) {
                return buildShortHoursMinsText(hoursRoundedToMins, minutesRoundedToMins, res);
            }
            // Zero minutes, so just show the hours ("12h" instead of "12h 0m").
            return buildShortHoursText(hoursRoundedToMins, res);
        }

        return buildShortMinsText(minutes(timeRoundedToMins), res);
    }

    private String shortDualUnlessTooLong(long time, Resources res) {
        String shortDual = buildShortDualUnitText(time, res);
        if (shortDual.length() <= SHORT_CHARACTER_LIMIT) {
            return shortDual;
        }
        return buildShortSingleUnitText(time, res);
    }

    @SuppressLint("TimeUnitMismatch")
    private String buildStopwatchText(long time, Resources res) {
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.DAYS)) {
            return buildShortDaysText(days(roundUpToUnit(time, TimeUnit.DAYS)), res);
        }

        long timeRoundedToMins = roundUpToUnit(time, TimeUnit.MINUTES);
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.HOURS) || days(timeRoundedToMins) > 0) {
            return buildShortDualUnitText(time, res);
        }

        // If in second precision there are non-zero hours, show hours and minutes.
        // (Can't check this in minutes precision, or we might show 1h 00m instead of 59m 1s)
        long timeRoundedToSecs = roundUpToUnit(time, TimeUnit.SECONDS);
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.MINUTES) || hours(timeRoundedToSecs) > 0) {
            return String.format(
                    Locale.US, "%d:%02d", hours(timeRoundedToMins), minutes(timeRoundedToMins));
        }

        return String.format(
                Locale.US, "%02d:%02d", minutes(timeRoundedToSecs), seconds(timeRoundedToSecs));
    }

    private String buildWordsSingleUnitText(long time, Resources res) {
        long timeRoundedToHours = roundUpToUnit(time, TimeUnit.HOURS);
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.DAYS) || days(timeRoundedToHours) > 0) {
            int daysRoundedToDays = days(roundUpToUnit(time, TimeUnit.DAYS));
            return res.getQuantityString(
                    R.plurals.time_difference_words_days, daysRoundedToDays, daysRoundedToDays);
        }

        long timeRoundedToMins = roundUpToUnit(time, TimeUnit.MINUTES);
        if (isGreaterOrEqual(mMinimumUnit, TimeUnit.HOURS) || hours(timeRoundedToMins) > 0) {
            int hoursRoundedToHours = hours(timeRoundedToHours);
            return res.getQuantityString(
                    R.plurals.time_difference_words_hours,
                    hoursRoundedToHours,
                    hoursRoundedToHours);
        }

        int mins = minutes(timeRoundedToMins);
        return res.getQuantityString(R.plurals.time_difference_words_minutes, mins, mins);
    }

    private String wordsSingleUnlessTooLong(long time, Resources res) {
        String wordsSingle = buildWordsSingleUnitText(time, res);
        if (wordsSingle.length() <= SHORT_CHARACTER_LIMIT) {
            return wordsSingle;
        }
        return buildShortSingleUnitText(time, res);
    }

    private static String buildShortDaysText(int days, Resources res) {
        return res.getQuantityString(R.plurals.time_difference_short_days, days, days);
    }

    private static String buildShortHoursText(int hours, Resources res) {
        return res.getQuantityString(R.plurals.time_difference_short_hours, hours, hours);
    }

    private static String buildShortMinsText(int mins, Resources res) {
        return res.getQuantityString(R.plurals.time_difference_short_minutes, mins, mins);
    }

    private static String buildShortDaysHoursText(int days, int hours, Resources res) {
        return res.getString(
                R.string.time_difference_short_days_and_hours,
                buildShortDaysText(days, res),
                buildShortHoursText(hours, res));
    }

    private static String buildShortHoursMinsText(int hours, int mins, Resources res) {
        return res.getString(
                R.string.time_difference_short_hours_and_minutes,
                buildShortHoursText(hours, res),
                buildShortMinsText(mins, res));
    }

    /**
     * Returns the amount of time {@code durationMillis} rounded up to a whole number of {@code
     * unit}s.
     */
    private static long roundUpToUnit(long durationMillis, TimeUnit unit) {
        long unitInMillis = unit.toMillis(1);
        return divRoundingUp(durationMillis, unitInMillis) * unitInMillis;
    }

    /**
     * Returns {@code num} divided by {@code divisor} rounded up, assuming that {@code num} and
     * {@code divisor} are both positive (or {@code num == 0}).
     */
    private static long divRoundingUp(long num, long divisor) {
        return (num / divisor) + (num % divisor == 0 ? 0 : 1);
    }

    /**
     * Returns the number of {@code unit}s in the standard representation of {@code durationMillis}
     * as a quantity of time broken down into milliseconds, seconds, minutes, hours, days. Time
     * units larger than days or smaller than milliseconds are not supported.
     */
    private static int modToUnit(long durationMillis, TimeUnit unit) {
        return (int) ((durationMillis / unit.toMillis(1)) % getUnitMaximum(unit));
    }

    /** Returns the number of days in the standard representation of {@code durationMillis} */
    private static int days(long durationMillis) {
        return modToUnit(durationMillis, TimeUnit.DAYS);
    }

    /** Returns the number of hours in the standard representation of {@code durationMillis} */
    private static int hours(long durationMillis) {
        return modToUnit(durationMillis, TimeUnit.HOURS);
    }

    /** Returns the number of minutes in the standard representation of {@code durationMillis} */
    private static int minutes(long durationMillis) {
        return modToUnit(durationMillis, TimeUnit.MINUTES);
    }

    /** Returns the number of seconds in the standard representation of {@code durationMillis} */
    private static int seconds(long durationMillis) {
        return modToUnit(durationMillis, TimeUnit.SECONDS);
    }

    /**
     * Returns true if {@code unit1} is not null, and represents an amount of time greater than or
     * equal to that represented by {@code unit2}.
     */
    private static boolean isGreaterOrEqual(@Nullable TimeUnit unit1, TimeUnit unit2) {
        if (unit1 == null) {
            return false;
        }
        return unit1.toMillis(1) >= unit2.toMillis(1);
    }

    /**
     * Returns the maximum number the given {@code unit} can hold before rolling into the next
     * higher unit. Only supports units from milliseconds to days.
     */
    private static int getUnitMaximum(TimeUnit unit) {
        switch (unit) {
            case MILLISECONDS:
                return 1000;
            case SECONDS:
            case MINUTES:
                return 60;
            case HOURS:
                return 24;
            case DAYS:
                return Integer.MAX_VALUE;
            default:
                throw new IllegalArgumentException("Unit not supported: " + unit);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(this.mReferencePeriodStart);
        dest.writeLong(this.mReferencePeriodEnd);
        dest.writeInt(this.mStyle);
        dest.writeByte(this.mShowNowText ? (byte) 1 : (byte) 0);
        dest.writeInt(
                this.mMinimumUnit == null
                        ? MINIMUM_UNIT_PARCELED_IS_NULL
                        : this.mMinimumUnit.ordinal());
    }

    protected TimeDifferenceText(@NonNull Parcel in) {
        this.mReferencePeriodStart = in.readLong();
        this.mReferencePeriodEnd = in.readLong();
        this.mStyle = in.readInt();
        this.mShowNowText = in.readByte() != 0;
        int tmpMMinimumUnit = in.readInt();
        this.mMinimumUnit =
                tmpMMinimumUnit == MINIMUM_UNIT_PARCELED_IS_NULL
                        ? null
                        : TimeUnit.values()[tmpMMinimumUnit];
    }

    public static final Creator<TimeDifferenceText> CREATOR =
            new Creator<TimeDifferenceText>() {
                @NonNull
                @Override
                public TimeDifferenceText createFromParcel(@NonNull Parcel source) {
                    return new TimeDifferenceText(source);
                }

                @NonNull
                @Override
                public TimeDifferenceText[] newArray(int size) {
                    return new TimeDifferenceText[size];
                }
            };
}
