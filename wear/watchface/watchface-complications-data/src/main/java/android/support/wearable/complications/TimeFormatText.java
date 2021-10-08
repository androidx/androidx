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

import android.content.res.Resources;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Class to generate string representations of dates/times according to a specified format.
 *
 * @hide
 * @see ComplicationText.TimeFormatBuilder
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class TimeFormatText implements TimeDependentText {

    private static class DateTimeFormat {
        final String[] mFormatSymbols;
        long mPrecision;

        DateTimeFormat(String[] dateTimeFormatSymbols, long precision) {
            mFormatSymbols = dateTimeFormatSymbols;
            mPrecision = precision;
        }
    }

    private static final DateTimeFormat[] DATE_TIME_FORMATS = {
            new DateTimeFormat(new String[]{"S", "s"}, TimeUnit.SECONDS.toMillis(1)),
            new DateTimeFormat(new String[]{"m"}, TimeUnit.MINUTES.toMillis(1)),
            new DateTimeFormat(new String[]{"H", "K", "h", "k", "j", "J", "C"},
                    TimeUnit.HOURS.toMillis(1)),
            new DateTimeFormat(new String[]{"a", "b", "B"}, TimeUnit.HOURS.toMillis(12)),
    };

    private final SimpleDateFormat mDateFormat;

    @ComplicationText.TimeFormatStyle
    private final int mStyle;
    private final TimeZone mTimeZone;
    private final Date mDate;
    private long mTimePrecision;

    public TimeFormatText(
            @NonNull String format, @ComplicationText.TimeFormatStyle int style,
            @Nullable TimeZone timeZone) {
        if (format == null) {
            throw new IllegalArgumentException("Format must be specified.");
        }
        mDateFormat = new SimpleDateFormat(format);
        mStyle = style;
        mTimePrecision = -1;
        if (timeZone != null) {
            mDateFormat.setTimeZone(timeZone);
            mTimeZone = timeZone;
        } else {
            mTimeZone = mDateFormat.getTimeZone();
        }
        mDate = new Date();
    }

    @Override
    @NonNull
    public CharSequence getTextAt(@NonNull Resources resources, long dateTimeMillis) {
        String formattedDate = mDateFormat.format(new Date(dateTimeMillis));

        switch (mStyle) {
            case ComplicationText.FORMAT_STYLE_UPPER_CASE:
                return formattedDate.toUpperCase();
            case ComplicationText.FORMAT_STYLE_LOWER_CASE:
                return formattedDate.toLowerCase();
            case ComplicationText.FORMAT_STYLE_DEFAULT:
            default:
                return formattedDate;
        }
    }

    @Override
    public boolean returnsSameText(long firstDateTimeMillis, long secondDateTimeMillis) {
        long precision = getPrecision();
        firstDateTimeMillis += getOffset(firstDateTimeMillis);
        secondDateTimeMillis += getOffset(secondDateTimeMillis);
        return firstDateTimeMillis / precision == secondDateTimeMillis / precision;
    }

    @Override
    public long getNextChangeTime(long fromTime) {
        long precision = getPrecision();
        long offset = getOffset(fromTime);
        return (((fromTime + offset) / precision) + 1) * precision - offset;
    }

    /**
     * @return The time precision in milliseconds
     */
    public long getPrecision() {
        if (mTimePrecision == -1) {
            String format = getDateFormatWithoutText(mDateFormat.toPattern());
            for (int i = 0; i < DATE_TIME_FORMATS.length && mTimePrecision == -1; i++) {
                for (int j = 0; j < DATE_TIME_FORMATS[i].mFormatSymbols.length; j++) {
                    if (format.contains(DATE_TIME_FORMATS[i].mFormatSymbols[j])) {
                        mTimePrecision = DATE_TIME_FORMATS[i].mPrecision;
                        break;
                    }
                }
            }
            if (mTimePrecision == -1) {
                mTimePrecision = TimeUnit.DAYS.toMillis(1);
            }
        }
        return mTimePrecision;
    }

    @NonNull
    public String getFormatString() {
        return mDateFormat.toPattern();
    }

    public int getStyle() {
        return mStyle;
    }

    @Nullable
    public TimeZone getTimeZone() {
        return mTimeZone;
    }

    private long getOffset(long date) {
        mDate.setTime(date);
        if (mTimeZone.inDaylightTime(mDate)) {
            return (long) mTimeZone.getRawOffset() + mTimeZone.getDSTSavings();
        }
        return mTimeZone.getRawOffset();
    }

    @NonNull
    private String getDateFormatWithoutText(String format) {
        StringBuilder result = new StringBuilder();
        boolean isTextPart = false;
        int index = 0;
        while (index < format.length()) {
            if (format.charAt(index) == '\'') {
                if (index + 1 < format.length() && format.charAt(index + 1) == '\'') {
                    index += 2;
                } else {
                    index++;
                    isTextPart = !isTextPart;
                }
            } else {
                if (!isTextPart) {
                    result.append(format.charAt(index));
                }
                index++;
            }
        }
        return result.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeSerializable(this.mDateFormat);
        dest.writeInt(this.mStyle);
        dest.writeSerializable(this.mTimeZone);
    }

    protected TimeFormatText(@NonNull Parcel in) {
        this.mDateFormat = (SimpleDateFormat) in.readSerializable();
        this.mStyle = in.readInt();
        this.mTimeZone = (TimeZone) in.readSerializable();
        this.mTimePrecision = -1;
        this.mDate = new Date();
    }

    public static final Creator<TimeFormatText> CREATOR =
            new Creator<TimeFormatText>() {
                @Override
                @NonNull
                public TimeFormatText createFromParcel(@NonNull Parcel source) {
                    return new TimeFormatText(source);
                }

                @Override
                @NonNull
                public TimeFormatText[] newArray(int size) {
                    return new TimeFormatText[size];
                }
            };
}
