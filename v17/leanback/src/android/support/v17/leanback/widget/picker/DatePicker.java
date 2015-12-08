/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link DatePicker} is a directly subclass of {@link Picker}.
 * This class is a widget for selecting a date. The date can be selected by a
 * year, month, and day Columns. The "minDate" and "maxDate" from which dates to be selected
 * can be customized.  The columns can be customized by attribute "datePickerFormat".
 *
 * @attr ref R.styleable#lbDatePicker_android_maxDate
 * @attr ref R.styleable#lbDatePicker_android_minDate
 * @attr ref R.styleable#lbDatePicker_datePickerFormat
 */

public class DatePicker extends Picker {

    static final String LOG_TAG = "DatePicker";

    PickerColumn mMonthColumn;
    PickerColumn mDayColumn;
    PickerColumn mYearColumn;
    int mColMonthIndex;
    int mColDayIndex;
    int mColYearIndex;

    final static String DATE_FORMAT = "MM/dd/yyyy";
    final DateFormat mDateFormat = new SimpleDateFormat(DATE_FORMAT);
    PickerConstant mConstant;

    Calendar mMinDate;
    Calendar mMaxDate;
    Calendar mCurrentDate;
    Calendar mTempDate;

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateCurrentLocale();

        final TypedArray attributesArray = context.obtainStyledAttributes(attrs,
                R.styleable.lbDatePicker);
        String minDate = attributesArray.getString(R.styleable.lbDatePicker_android_minDate);
        String maxDate = attributesArray.getString(R.styleable.lbDatePicker_android_maxDate);
        String datePickerFormat = attributesArray
                .getString(R.styleable.lbDatePicker_datePickerFormat);

        if (TextUtils.isEmpty(datePickerFormat)) {
            datePickerFormat = new String(
                    android.text.format.DateFormat.getDateFormatOrder(context));
        }
        datePickerFormat = datePickerFormat.toUpperCase();
        ArrayList<PickerColumn> columns = new ArrayList<PickerColumn>(3);
        for (int i = 0; i < datePickerFormat.length(); i++) {
            switch (datePickerFormat.charAt(i)) {
            case 'Y':
                if (mYearColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                columns.add(mYearColumn = new PickerColumn());
                mColYearIndex = i;
                mYearColumn.setValueLabelFormat("%d");
                break;
            case 'M':
                if (mMonthColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                columns.add(mMonthColumn = new PickerColumn());
                mMonthColumn.setValueStaticLabels(mConstant.months);
                mColMonthIndex = i;
                break;
            case 'D':
                if (mDayColumn != null) {
                    throw new IllegalArgumentException("datePicker format error");
                }
                columns.add(mDayColumn = new PickerColumn());
                mDayColumn.setValueLabelFormat("%02d");
                mColDayIndex = i;
                break;
            default:
                throw new IllegalArgumentException("datePicker format error");
            }
        }
        setColumns(columns);

        mTempDate.clear();
        if (!TextUtils.isEmpty(minDate)) {
            if (!parseDate(minDate, mTempDate)) {
                mTempDate.set(1900, 0, 1);
            }
        } else {
            mTempDate.set(1900, 0, 1);
        }
        setMinDate(mTempDate.getTimeInMillis());

        mTempDate.clear();
        if (!TextUtils.isEmpty(maxDate)) {
            if (!parseDate(maxDate, mTempDate)) {
                mTempDate.set(2100, 0, 1);
            }
        } else {
            mTempDate.set(2100, 0, 1);
        }
        setMaxDate(mTempDate.getTimeInMillis());

    }

    private boolean parseDate(String date, Calendar outDate) {
        try {
            outDate.setTime(mDateFormat.parse(date));
            return true;
        } catch (ParseException e) {
            Log.w(LOG_TAG, "Date: " + date + " not in format: " + DATE_FORMAT);
            return false;
        }
    }

    @Override
    protected String getSeparator() {
        return mConstant.dateSeparator;
    }


    private Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        } else {
            final long currentTimeMillis = oldCalendar.getTimeInMillis();
            Calendar newCalendar = Calendar.getInstance(locale);
            newCalendar.setTimeInMillis(currentTimeMillis);
            return newCalendar;
        }
    }

    private void updateCurrentLocale() {
        mConstant = new PickerConstant(Locale.getDefault(), getContext().getResources());
        mTempDate = getCalendarForLocale(mTempDate, mConstant.locale);
        mMinDate = getCalendarForLocale(mMinDate, mConstant.locale);
        mMaxDate = getCalendarForLocale(mMaxDate, mConstant.locale);
        mCurrentDate = getCalendarForLocale(mCurrentDate, mConstant.locale);

        if (mMonthColumn != null) {
            mMonthColumn.setValueStaticLabels(mConstant.months);
            updateAdapter(mColMonthIndex);
        }
    }

    @Override
    public void onColumnValueChange(int column, int newVal) {
        mTempDate.setTimeInMillis(mCurrentDate.getTimeInMillis());
        // take care of wrapping of days and months to update greater fields
        int oldVal = getColumnAt(column).getCurrentValue();
        if (column == mColDayIndex) {
            mTempDate.add(Calendar.DAY_OF_MONTH, newVal - oldVal);
        } else if (column == mColMonthIndex) {
            mTempDate.add(Calendar.MONTH, newVal - oldVal);
        } else if (column == mColYearIndex) {
            mTempDate.add(Calendar.YEAR, newVal - oldVal);
        } else {
            throw new IllegalArgumentException();
        }
        setDate(mTempDate.get(Calendar.YEAR), mTempDate.get(Calendar.MONTH),
                mTempDate.get(Calendar.DAY_OF_MONTH));
        updateSpinners(false);
    }


    /**
     * Sets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate.setTimeInMillis(minDate);
        if (mTempDate.get(Calendar.YEAR) == mMinDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMinDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        mMinDate.setTimeInMillis(minDate);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        }
        updateSpinners(false);
    }


    /**
     * Gets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default minimal date is 01/01/1900.
     * <p>
     *
     * @return The minimal supported date.
     */
    public Calendar getMinDate() {
        final Calendar minDate = Calendar.getInstance();
        minDate.setTimeInMillis(mMinDate.getTimeInMillis());
        return minDate;
    }

    /**
     * Sets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate.setTimeInMillis(maxDate);
        if (mTempDate.get(Calendar.YEAR) == mMaxDate.get(Calendar.YEAR)
                && mTempDate.get(Calendar.DAY_OF_YEAR) != mMaxDate.get(Calendar.DAY_OF_YEAR)) {
            return;
        }
        mMaxDate.setTimeInMillis(maxDate);
        if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
        updateSpinners(false);
    }

    /**
     * Gets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link TimeZone#getDefault()} time zone.
     * <p>
     * Note: The default maximal date is 12/31/2100.
     * <p>
     *
     * @return The maximal supported date.
     */
    public Calendar getMaxDate() {
        final Calendar maxDate = Calendar.getInstance();
        maxDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        return maxDate;
    }

    private void setDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(year, month, dayOfMonth);
        if (mCurrentDate.before(mMinDate)) {
            mCurrentDate.setTimeInMillis(mMinDate.getTimeInMillis());
        } else if (mCurrentDate.after(mMaxDate)) {
            mCurrentDate.setTimeInMillis(mMaxDate.getTimeInMillis());
        }
    }

    /**
     * Update the current date.
     *
     * @param year The year.
     * @param month The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     * @param animation True to run animation to scroll the column.
     */
    public void updateDate(int year, int month, int dayOfMonth, boolean animation) {
        if (!isNewDate(year, month, dayOfMonth)) {
            return;
        }
        setDate(year, month, dayOfMonth);
        updateSpinners(animation);
    }

    private boolean isNewDate(int year, int month, int dayOfMonth) {
        return (mCurrentDate.get(Calendar.YEAR) != year
                || mCurrentDate.get(Calendar.MONTH) != dayOfMonth
                || mCurrentDate.get(Calendar.DAY_OF_MONTH) != month);
    }

    private void updateSpinners(boolean animation) {
        // set the spinner ranges respecting the min and max dates
        boolean dayRangeChanged = false;
        boolean monthRangeChanged = false;
        if (mCurrentDate.equals(mMinDate)) {
            dayRangeChanged |= mDayColumn.setMinValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            dayRangeChanged |= mDayColumn.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            monthRangeChanged |= mMonthColumn.setMinValue(mCurrentDate.get(Calendar.MONTH));
            monthRangeChanged |= mMonthColumn.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
        } else if (mCurrentDate.equals(mMaxDate)) {
            dayRangeChanged |= mDayColumn.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            dayRangeChanged |= mDayColumn.setMaxValue(mCurrentDate.get(Calendar.DAY_OF_MONTH));
            monthRangeChanged |= mMonthColumn.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
            monthRangeChanged |= mMonthColumn.setMaxValue(mCurrentDate.get(Calendar.MONTH));
        } else {
            dayRangeChanged |= mDayColumn.setMinValue(mCurrentDate.getActualMinimum(Calendar.DAY_OF_MONTH));
            dayRangeChanged |= mDayColumn.setMaxValue(mCurrentDate.getActualMaximum(Calendar.DAY_OF_MONTH));
            monthRangeChanged |= mMonthColumn.setMinValue(mCurrentDate.getActualMinimum(Calendar.MONTH));
            monthRangeChanged |= mMonthColumn.setMaxValue(mCurrentDate.getActualMaximum(Calendar.MONTH));
        }

        // year spinner range does not change based on the current date
        boolean yearRangeChanged = false;
        yearRangeChanged |= mYearColumn.setMinValue(mMinDate.get(Calendar.YEAR));
        yearRangeChanged |= mYearColumn.setMaxValue(mMaxDate.get(Calendar.YEAR));

        if (dayRangeChanged) {
            updateAdapter(mColDayIndex);
        }
        if (monthRangeChanged) {
            updateAdapter(mColMonthIndex);
        }
        if (yearRangeChanged) {
            updateAdapter(mColYearIndex);
        }
        // set the spinner values
        updateValue(mColYearIndex, mCurrentDate.get(Calendar.YEAR), animation);
        updateValue(mColMonthIndex, mCurrentDate.get(Calendar.MONTH), animation);
        updateValue(mColDayIndex, mCurrentDate.get(Calendar.DAY_OF_MONTH), animation);

    }

}