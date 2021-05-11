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
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.LocaleSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Represents a piece of text to be shown in a complication.
 *
 * <p>ComplicationText can be a plain string or it can contain a time-dependent value, for which the
 * value varies depending on the current date/time.
 *
 * <p>Instances of this class should be obtained either by calling {@link #plainText}, or by using
 * one of the provided builders: {@link TimeDifferenceBuilder} or {@link TimeFormatBuilder}.
 *
 * <p>Note this class is not thread safe.</p>
 *
 * @hide
 */
@SuppressLint("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ComplicationText implements Parcelable, TimeDependentText {

    /** @hide */
    @IntDef({
            DIFFERENCE_STYLE_STOPWATCH,
            DIFFERENCE_STYLE_SHORT_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_DUAL_UNIT,
            DIFFERENCE_STYLE_WORDS_SINGLE_UNIT,
            DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT
    })
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeDifferenceStyle {
    }

    /**
     * Style for time differences shown in a numeric fashion like a timer.
     *
     * <p>For time difference {@code t}:
     *
     * <p>If {@code t < 1 hour}, the value will be shown as minutes and seconds, such as {@code
     * 02:35} for 2 minutes and 35 seconds.
     *
     * <p>If {@code 1 hour <= t < 1 day}, the value will be shown as hours and minutes, such as
     * {@code 4:02} for 4 hours and 2 minutes, or as {@code 12:02} for 12 hours and 2 minutes.
     *
     * <p>If {@code 1 day <= t < 10 days}, the value will be shown as days and hours, such as {@code
     * 3d 4h} for 3 days 4 hours.
     *
     * <p>If {@code 10 days <= t}, the value will be shown as just days, such as {@code 13d} for 13
     * days.
     *
     * <p>The characters used will be localised to match the default locale.
     */
    public static final int DIFFERENCE_STYLE_STOPWATCH = 1;

    /**
     * Style for time differences shown in a short alpha-numeric style, with only the most
     * significant unit included.
     *
     * <p>For time difference {@code t}:
     *
     * <p>If {@code t < 1 hour}, the value will be shown as a number of minutes, such as {@code 2m}
     * for 2 minutes. Seconds are not displayed.
     *
     * <p>If {@code 1 hour <= t < 1 day}, the value will be shown as a number of hours, such as
     * {@code 4h} for 4 hours.
     *
     * <p>If {@code 1 days <= t}, the value will be shown as a number of days, such as {@code 13d}
     * for 13 days.
     *
     * <p>The characters used will be localised to match the default locale.
     */
    public static final int DIFFERENCE_STYLE_SHORT_SINGLE_UNIT = 2;

    /**
     * Style for time differences shown in a short alpha-numeric style, with up to two significant
     * units included.
     *
     * <p>For time difference {@code t}:
     *
     * <p>If {@code t < 1 hour}, the value will be shown as a number of minutes, such as {@code 2m}
     * for 2 minutes. Seconds are not displayed.
     *
     * <p>If {@code 1 hour <= t < 1 day}, the value will be shown as hours and minutes, such as
     * {@code 4h 2m} for 4 hours and 2 minutes.
     *
     * <p>If {@code 1 day <= t < 10 days}, the value will be shown as days and hours, such as {@code
     * 3d 4h} for 3 days 4 hours.
     *
     * <p>If {@code 10 days <= t}, the value will be shown as a number of days, such as {@code 13d}
     * for 13 days.
     *
     * <p>The characters used will be localised to match the default locale. If the representation
     * of the time difference with two units would be too long in the default locale, just a single
     * unit may be shown instead.
     */
    public static final int DIFFERENCE_STYLE_SHORT_DUAL_UNIT = 3;

    /**
     * Style for time differences shown using (possibly abbreviated) words, with only the most
     * significant unit included.
     *
     * <p>For time difference {@code t}:
     *
     * <p>If {@code t < 1 hour}, the value will be shown as a number of minutes, such as {@code 1
     * min} for 1 minute or {@code 2 mins} for 2 minutes. Seconds are not displayed.
     *
     * <p>If {@code 1 hour <= t < 1 day}, the value will be shown as a number of hours, such as
     * {@code 1 hour} for 1 hour or {@code 4 hours} for 4 hours.
     *
     * <p>If {@code 1 days <= t}, the value will be shown as a number of days, such as {@code 1 day}
     * for 1 day or {@code 13 days} for 13 days.
     *
     * <p>The words used will be localised to match the default locale.
     */
    public static final int DIFFERENCE_STYLE_WORDS_SINGLE_UNIT = 4;

    /**
     * Style for time differences shown using (possibly abbreviated) words, with only the most
     * significant unit included, that should fit within the character limit for a short text field.
     *
     * <p>The output will be the same as for {@link #DIFFERENCE_STYLE_WORDS_SINGLE_UNIT}, except
     * that if the text does not fit into the seven character limit then a shorter form will be used
     * instead, e.g. "1356d" instead of "1356 days".
     */
    public static final int DIFFERENCE_STYLE_SHORT_WORDS_SINGLE_UNIT = 5;

    /** @hide */
    @IntDef({FORMAT_STYLE_DEFAULT, FORMAT_STYLE_UPPER_CASE, FORMAT_STYLE_LOWER_CASE})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    public @interface TimeFormatStyle {
    }

    /** Style where the formatted date/time should be shown unchanged. */
    public static final int FORMAT_STYLE_DEFAULT = 1;

    /** Style where the formatted date/time should be capitalized. */
    public static final int FORMAT_STYLE_UPPER_CASE = 2;

    /** Style where the formatted date/time should be shown in lower case. */
    public static final int FORMAT_STYLE_LOWER_CASE = 3;

    // Keys used in the Bundle when parcelling this object.
    private static final String KEY_SURROUNDING_STRING = "surrounding_string";
    private static final String KEY_DIFFERENCE_PERIOD_START = "difference_period_start";
    private static final String KEY_DIFFERENCE_PERIOD_END = "difference_period_end";
    private static final String KEY_DIFFERENCE_STYLE = "difference_style";
    private static final String KEY_DIFFERENCE_SHOW_NOW_TEXT = "show_now_text";
    private static final String KEY_DIFFERENCE_MINIMUM_UNIT = "minimum_unit";
    private static final String KEY_FORMAT_FORMAT_STRING = "format_format_string";
    private static final String KEY_FORMAT_STYLE = "format_style";
    private static final String KEY_FORMAT_TIME_ZONE = "format_time_zone";

    @NonNull
    public static final Parcelable.Creator<ComplicationText> CREATOR =
            new Parcelable.Creator<ComplicationText>() {
                @Override
                @NonNull
                @SuppressLint("SyntheticAccessor")
                public ComplicationText createFromParcel(@NonNull Parcel in) {
                    return new ComplicationText(in);
                }

                @Override
                @NonNull
                public ComplicationText[] newArray(int size) {
                    return new ComplicationText[size];
                }
            };

    /**
     * The plain-text part of the complication text. If {@link #mTimeDependentText} is null, this is
     * required to be not null and {@link #getTextAt} will return this text as-is. If {@link
     * #mTimeDependentText} is not null, getText will return this text with {@code ^1} replaced by
     * the time-dependent string.
     */
    @Nullable private final CharSequence mSurroundingText;

    /**
     * The time-dependent part of the complication text. If {@link #mSurroundingText} is null, this
     * must be not null and {@link #getTextAt} will return just the time-dependent value relative to
     * the given time.
     */
    private final TimeDependentText mTimeDependentText;

    /** Used to replace occurrences of ^1 with time dependent text and ignore ^[2-9]. */
    private final CharSequence[] mTemplateValues =
            new CharSequence[]{"", "^2", "^3", "^4", "^5", "^6", "^7", "^8", "^9"};

    /** The timestamp of the stored TimeDependentText in the cache. */
    private long mDependentTextCacheTime;

    private CharSequence mDependentTextCache;

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public ComplicationText(@Nullable CharSequence surroundingText,
            @Nullable TimeDependentText timeDependentText) {
        mSurroundingText = surroundingText;
        mTimeDependentText = timeDependentText;
        checkFields();
    }

    private ComplicationText(@NonNull Parcel in) {
        Bundle bundle = in.readBundle(getClass().getClassLoader());
        mSurroundingText = bundle.getCharSequence(KEY_SURROUNDING_STRING);

        if (bundle.containsKey(KEY_DIFFERENCE_STYLE)
                && bundle.containsKey(KEY_DIFFERENCE_PERIOD_START)
                && bundle.containsKey(KEY_DIFFERENCE_PERIOD_END)) {
            mTimeDependentText =
                    new TimeDifferenceText(
                            bundle.getLong(KEY_DIFFERENCE_PERIOD_START),
                            bundle.getLong(KEY_DIFFERENCE_PERIOD_END),
                            bundle.getInt(KEY_DIFFERENCE_STYLE),
                            bundle.getBoolean(KEY_DIFFERENCE_SHOW_NOW_TEXT, true),
                            timeUnitFromName(bundle.getString(KEY_DIFFERENCE_MINIMUM_UNIT)));
        } else if (bundle.containsKey(KEY_FORMAT_FORMAT_STRING)
                && bundle.containsKey(KEY_FORMAT_STYLE)) {
            TimeZone timeZone = null;
            if (bundle.containsKey(KEY_FORMAT_TIME_ZONE)) {
                timeZone = TimeZone.getTimeZone(bundle.getString(KEY_FORMAT_TIME_ZONE));
            }
            mTimeDependentText =
                    new TimeFormatText(
                            bundle.getString(KEY_FORMAT_FORMAT_STRING),
                            bundle.getInt(KEY_FORMAT_STYLE),
                            timeZone);
        } else {
            mTimeDependentText = null;
        }
        checkFields();
    }

    /**
     * Returns the {@link TimeUnit} with the provided {@code name}. Returns null if {@code name} is
     * null, or is not a TimeUnit.
     */
    @Nullable
    private static TimeUnit timeUnitFromName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        try {
            return TimeUnit.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void checkFields() {
        if (mSurroundingText == null && mTimeDependentText == null) {
            throw new IllegalStateException(
                    "One of mSurroundingText and mTimeDependentText must be non-null");
        }
    }

    /**
     * Writes this {@link ComplicationProviderInfo} to a {@link Parcel}.
     *
     * @param out The {@link Parcel} to write to
     * @param flags Flags for writing the {@link Parcel}
     */
    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(KEY_SURROUNDING_STRING, mSurroundingText);

        if (mTimeDependentText instanceof TimeDifferenceText) {
            TimeDifferenceText timeDiffText = (TimeDifferenceText) mTimeDependentText;
            bundle.putLong(KEY_DIFFERENCE_PERIOD_START, timeDiffText.getReferencePeriodStart());
            bundle.putLong(KEY_DIFFERENCE_PERIOD_END, timeDiffText.getReferencePeriodEnd());
            bundle.putInt(KEY_DIFFERENCE_STYLE, timeDiffText.getStyle());
            bundle.putBoolean(KEY_DIFFERENCE_SHOW_NOW_TEXT, timeDiffText.shouldShowNowText());
            if (timeDiffText.getMinimumUnit() != null) {
                bundle.putString(KEY_DIFFERENCE_MINIMUM_UNIT, timeDiffText.getMinimumUnit().name());
            }
        } else if (mTimeDependentText instanceof TimeFormatText) {
            TimeFormatText timeFormatText = (TimeFormatText) mTimeDependentText;
            bundle.putString(KEY_FORMAT_FORMAT_STRING, timeFormatText.getFormatString());
            bundle.putInt(KEY_FORMAT_STYLE, timeFormatText.getStyle());
            TimeZone timeZone = timeFormatText.getTimeZone();
            if (timeZone != null) {
                bundle.putString(KEY_FORMAT_TIME_ZONE, timeZone.getID());
            }
        }

        out.writeBundle(bundle);
    }

    /**
     * Returns the time-dependent part of the complication text.
     *
     * @hide
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public TimeDependentText getTimeDependentText() {
        return mTimeDependentText;
    }

    /**
     * Note if this ComplicationText contains a TimeDifference text and the {@code dateTimeMillis}
     * is between {@code referencePeriodStart} and {@code referencePeriodEnd}, then the text
     * returned will be "now" (localised to the default locale).  If the time is before
     * {@code referencePeriodStart} then the text returned will represent the time difference
     * between {@code referencePeriodStart} and {@code dateTimeMillis}. If the time is after
     * {@code referencePeriodEnd} then the text returned will represent the time difference
     * between {@code referencePeriodStart} and {@code dateTimeMillis}.
     *
     * @param resources {@link Resources} from the current {@link Context}
     * @param dateTimeMillis milliseconds since epoch, e.g. from {@link System#currentTimeMillis}
     * @return Text appropriate for the given date time.
     */
    @NonNull
    @Override
    public CharSequence getTextAt(@NonNull Resources resources, long dateTimeMillis) {
        if (mTimeDependentText == null) {
            return mSurroundingText;
        }

        CharSequence timeDependentPart;
        if (mDependentTextCache != null
                && mTimeDependentText.returnsSameText(mDependentTextCacheTime, dateTimeMillis)) {
            timeDependentPart = mDependentTextCache;
        } else {
            timeDependentPart = mTimeDependentText.getTextAt(resources, dateTimeMillis);
            mDependentTextCacheTime = dateTimeMillis;
            mDependentTextCache = timeDependentPart;
        }

        if (mSurroundingText == null) {
            return timeDependentPart;
        }

        mTemplateValues[0] = timeDependentPart;
        return TextUtils.expandTemplate(mSurroundingText, mTemplateValues);
    }

    /**
     * Returns The text within which the time difference is displayed.
     */
    @Nullable
    CharSequence getSurroundingText() {
        return mSurroundingText;
    }

    /**
     * Returns true if the result of {@link #getTextAt} will be the same for both {@code
     * firstDateTimeMillis} and {@code secondDateTimeMillis}.
     */
    @Override
    public boolean returnsSameText(long firstDateTimeMillis, long secondDateTimeMillis) {
        if (mTimeDependentText == null) {
            // Only the surrounding text is left, which will never change based on time.
            return true;
        }
        return mTimeDependentText.returnsSameText(firstDateTimeMillis, secondDateTimeMillis);
    }

    /** Returns the next time after {@code fromTime} at which the text may change. */
    @Override
    public long getNextChangeTime(long fromTime) {
        if (mTimeDependentText == null) {
            return Long.MAX_VALUE;
        }
        return mTimeDependentText.getNextChangeTime(fromTime);
    }

    /**
     * Returns true if {@link #getTextAt(Resources, long)} will return the empty string for any
     * input.
     */
    public boolean isAlwaysEmpty() {
        return mTimeDependentText == null && TextUtils.isEmpty(mSurroundingText);
    }

    /** Returns true if the text has a time-dependent component. */
    boolean isTimeDependent() {
        return mTimeDependentText != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Returns a ComplicationText object that will display the given {@code text} for any input
     * time.
     *
     * <p>If the text contains spans, some of them may not be rendered by
     * {@link androidx.wear.watchface.complications.rendering.ComplicationDrawable}. Supported spans
     * are {@link ForegroundColorSpan}, {@link LocaleSpan}, {@link SubscriptSpan}, {@link
     * SuperscriptSpan}, {@link StyleSpan}, {@link StrikethroughSpan}, {@link TypefaceSpan} and
     * {@link UnderlineSpan}.
     *
     * @param text the text to be displayed
     */
    @NonNull
    public static ComplicationText plainText(@NonNull CharSequence text) {
        return new ComplicationText(text, null);
    }

    /**
     * Builder for a ComplicationText object that displays a text representation of the difference
     * between the given time and the specified time period, within a surrounding string if
     * required.
     *
     * <p>If the time passed in to {@link ComplicationText#getTextAt} on the resulting object is
     * between {@code referencePeriodStart} and {@code referencePeriodEnd}, then the text will be
     * "now" (localised to the default locale) if setShowNowText(true) has been called.
     *
     * <p>If the time {@code dateTimeMillis} passed in to {@link #getTextAt} is before {@code
     * referencePeriodStart}, then the text shown will represent the time difference between {@code
     * referencePeriodStart} and {@code dateTimeMillis}.
     *
     * <p>If the time {@code dateTimeMillis} passed in to {@link #getTextAt} is after {@code
     * referencePeriodEnd}, then the text shown will represent the time difference between {@code
     * dateTimeMillis} and {@code referencePeriodEnd}.
     *
     * <p>The way the time difference is represented depends on the {@code style}. See {@link
     * #DIFFERENCE_STYLE_SHORT_SINGLE_UNIT}, {@link #DIFFERENCE_STYLE_SHORT_DUAL_UNIT}, {@link
     * #DIFFERENCE_STYLE_STOPWATCH} and {@link #DIFFERENCE_STYLE_WORDS_SINGLE_UNIT}.
     */
    public static final class TimeDifferenceBuilder {
        private static final long NO_PERIOD_START = 0;
        private static final long NO_PERIOD_END = Long.MAX_VALUE;

        private long mReferencePeriodStartMillis = NO_PERIOD_START;
        private long mReferencePeriodEndMillis = NO_PERIOD_END;
        @TimeDifferenceStyle
        private int mStyle = ComplicationText.DIFFERENCE_STYLE_SHORT_DUAL_UNIT;
        private CharSequence mSurroundingText;
        private Boolean mShowNowText;
        private TimeUnit mMinimumUnit;

        public TimeDifferenceBuilder() {
        }

        /**
         * @param referencePeriodStartMillis The start of the reference period (in milliseconds
         *     since the epoch) from which the time difference will be calculated.
         * @param referencePeriodEndMillis The end of the reference period (in milliseconds since
         *     the epoch) from which the time difference will be calculated.
         */
        public TimeDifferenceBuilder(long referencePeriodStartMillis, long referencePeriodEndMillis) {
            mReferencePeriodStartMillis = referencePeriodStartMillis;
            mReferencePeriodEndMillis = referencePeriodEndMillis;
        }

        /**
         * Sets the start of the reference period from which the time difference will be calculated.
         * Defaults to 0, effectively making the period unbounded at the start.
         *
         * @param refPeriodStartMillis the end of the reference period, given as UTC milliseconds
         *                             since the epoch.
         * @return this builder for chaining.
         */
        @NonNull
        public TimeDifferenceBuilder setReferencePeriodStartMillis(long refPeriodStartMillis) {
            if (refPeriodStartMillis < 0) {
                throw new IllegalArgumentException("Reference period start cannot be negative");
            }
            mReferencePeriodStartMillis = refPeriodStartMillis;
            return this;
        }

        /**
         * Sets the end of the reference period from which the time difference will be calculated.
         * Defaults to {@code Long.MAX_VALUE}, effectively making the period unbounded at the end.
         *
         * @param refPeriodEndMillis the end of the reference period, given as UTC milliseconds
         *                           since the epoch.
         * @return this builder for chaining.
         */
        @NonNull
        public TimeDifferenceBuilder setReferencePeriodEndMillis(long refPeriodEndMillis) {
            if (refPeriodEndMillis < 0) {
                throw new IllegalArgumentException("Reference period end cannot be negative");
            }
            mReferencePeriodEndMillis = refPeriodEndMillis;
            return this;
        }

        /**
         * Sets the style in which the time difference will be displayed. If not set, defaults to
         * {@link #DIFFERENCE_STYLE_SHORT_DUAL_UNIT}.
         *
         * @return this builder for chaining.
         * @see #DIFFERENCE_STYLE_SHORT_SINGLE_UNIT
         * @see #DIFFERENCE_STYLE_SHORT_DUAL_UNIT
         * @see #DIFFERENCE_STYLE_STOPWATCH
         * @see #DIFFERENCE_STYLE_WORDS_SINGLE_UNIT
         */
        @NonNull
        public TimeDifferenceBuilder setStyle(@TimeDifferenceStyle int style) {
            mStyle = style;
            return this;
        }

        /**
         * Sets the text within which the time difference will be displayed. This is optional.
         *
         * <p>Within the text, {@code ^1} will be replaced with the time difference, so for example
         * to show a result like {@code "2 mins: meeting"} the surrounding text would be {@code "^1:
         * meeting"}.
         *
         * <p>To use the {@code ^} character within the text, escape it as {@code ^^}.
         *
         * <p>If the text contains spans, some of them may not be rendered by {@link
         * androidx.wear.watchface.complications.rendering.ComplicationDrawable}. Supported spans
         * are {@link ForegroundColorSpan}, {@link LocaleSpan}, {@link SubscriptSpan}, {@link
         * SuperscriptSpan}, {@link StyleSpan}, {@link StrikethroughSpan}, {@link TypefaceSpan} and
         * {@link UnderlineSpan}.
         *
         * @param surroundingText text within which the time difference value will be displayed,
         *                        with {@code ^1} in place of the time difference.
         * @return this builder for chaining.
         */
        @NonNull
        public TimeDifferenceBuilder setSurroundingText(@Nullable CharSequence surroundingText) {
            mSurroundingText = surroundingText;
            return this;
        }

        /**
         * Sets whether text saying "now" (appropriately localized) should be shown when the given
         * time is within the reference period. If {@code showNowText} is false, then text
         * representing zero (e.g. "0 mins" or "00:00") will be shown instead.
         *
         * <p>The default is true for all styles except for {@link #DIFFERENCE_STYLE_STOPWATCH}.
         */
        @NonNull
        public TimeDifferenceBuilder setShowNowText(boolean showNowText) {
            mShowNowText = showNowText;
            return this;
        }

        /**
         * Sets the smallest unit that may be shown in the time difference text. If specified, units
         * smaller than this minimum will not be included.
         *
         * <p>For example, if this is set to {@link TimeUnit#HOURS}, and the style is {@link
         * #DIFFERENCE_STYLE_SHORT_SINGLE_UNIT} then "12d" or "5h" would be shown as normal, but
         * "35m" would be shown as "1h".
         *
         * <p>This is optional. If not specified, or if set to null, the style will determine the
         * smallest unit that will be shown.
         *
         * <p>If the specified minimum is smaller than the smallest unit supported by the style,
         * then the minimum will be ignored. For example, if the style is {@link
         * #DIFFERENCE_STYLE_SHORT_SINGLE_UNIT}, then a minimum unit of {@link TimeUnit#SECONDS}
         * will have no effect.
         */
        @NonNull
        public TimeDifferenceBuilder setMinimumUnit(@Nullable TimeUnit minimumUnit) {
            mMinimumUnit = minimumUnit;
            return this;
        }

        /** Returns {@link ComplicationText} representing the time difference as specified. */
        @NonNull
        @SuppressLint("SyntheticAccessor")
        public ComplicationText build() {
            if (mReferencePeriodEndMillis < mReferencePeriodStartMillis) {
                throw new IllegalStateException("Reference period end must not be before start.");
            }
            boolean showNowText =
                    mShowNowText == null ? getDefaultShowNowTextForStyle(mStyle) : mShowNowText;
            return new ComplicationText(
                    mSurroundingText,
                    new TimeDifferenceText(
                            mReferencePeriodStartMillis,
                            mReferencePeriodEndMillis,
                            mStyle,
                            showNowText,
                            mMinimumUnit));
        }

        /** Returns the default value for the 'show now text' option for the given {@code style}. */
        private static boolean getDefaultShowNowTextForStyle(int style) {
            return style != DIFFERENCE_STYLE_STOPWATCH;
        }
    }

    /**
     * Builder for a ComplicationText object that displays a text representation of the given time,
     * using the provided format and time zone, within a surrounding string if specified.
     */
    public static final class TimeFormatBuilder {
        private String mFormat;
        @TimeFormatStyle
        private int mStyle = ComplicationText.FORMAT_STYLE_DEFAULT;
        private CharSequence mSurroundingText;
        private TimeZone mTimeZone;

        /**
         * Sets the format that should be applied to the date. This should be a pattern as used by
         * {@link java.text.SimpleDateFormat SimpleDateFormat}.
         */
        @NonNull
        public TimeFormatBuilder setFormat(@Nullable String format) {
            mFormat = format;
            return this;
        }

        /**
         * Sets the style in which the time format part will be displayed. If not set, defaults to
         * {@link #FORMAT_STYLE_DEFAULT}, which leaves the formatted date unchanged.
         *
         * @return this builder for chaining.
         * @see #FORMAT_STYLE_DEFAULT
         * @see #FORMAT_STYLE_UPPER_CASE
         * @see #FORMAT_STYLE_LOWER_CASE
         */
        @NonNull
        public TimeFormatBuilder setStyle(@TimeFormatStyle int style) {
            mStyle = style;
            return this;
        }

        /**
         * Sets the string within which the time difference will be displayed. This is optional.
         *
         * <p>Within the text, {@code ^1} will be replaced with the time format, so for example
         * to show a result like {@code "10:00: meeting"} the surrounding text would be {@code "^1:
         * meeting"}.
         *
         * <p>To use the {@code ^} character within the text, escape it as {@code ^^}.
         *
         * <p>If the text contains spans, some of them may not be rendered by
         * {@link androidx.wear.watchface.complications.rendering.ComplicationDrawable}. Supported
         * spans are {@link ForegroundColorSpan}, {@link LocaleSpan}, {@link SubscriptSpan}, {@link
         * SuperscriptSpan}, {@link StyleSpan}, {@link StrikethroughSpan}, {@link TypefaceSpan} and
         * {@link UnderlineSpan}.
         *
         * @param surroundingText string within which the time difference value will be displayed,
         *                        with {@code ^1} in place of the time difference.
         * @return this builder for chaining.
         */
        @NonNull
        public TimeFormatBuilder setSurroundingText(@Nullable CharSequence surroundingText) {
            mSurroundingText = surroundingText;
            return this;
        }

        /**
         * Sets the time zone that will be used for the formatted time. This is optional - if not
         * set, the system's default time zone will be used.
         *
         * @return this builder for chaining.
         */
        @NonNull
        public TimeFormatBuilder setTimeZone(
                @Nullable @SuppressWarnings("UseIcu") TimeZone timeZone) {
            mTimeZone = timeZone;
            return this;
        }

        /** Returns {@link ComplicationText} including the formatted time as specified. */
        @NonNull
        @SuppressLint("SyntheticAccessor")
        public ComplicationText build() {
            return new ComplicationText(
                    mSurroundingText, new TimeFormatText(mFormat, mStyle, mTimeZone));
        }
    }
}
