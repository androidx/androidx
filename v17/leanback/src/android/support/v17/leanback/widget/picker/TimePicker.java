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

package android.support.v17.leanback.widget.picker;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.v17.leanback.R;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * {@link TimePicker} is a direct subclass of {@link Picker}.
 * <p>
 * This class is a widget for selecting time and displays it according to the formatting for the
 * current system locale. The time can be selected by hour, minute, and AM/PM picker columns.
 * The AM/PM mode is determined by either explicitly setting the current mode through
 * {@link #setIs24Hour(boolean)} or the widget attribute {@code is24HourFormat} (true for 24-hour
 * mode, false for 12-hour mode). Otherwise, TimePicker retrieves the mode based on the current
 * context. In 24-hour mode, TimePicker displays only the hour and minute columns.
 * <p>
 * This widget can show the current time as the initial value if {@code useCurrentTime} is set to
 * true. Each individual time picker field can be set at any time by calling {@link #setHour(int)},
 * {@link #setMinute(int)} using 24-hour time format. The time format can also be changed at any
 * time by calling {@link #setIs24Hour(boolean)}, and the AM/PM picker column will be activated or
 * deactivated accordingly.
 *
 * @attr ref R.styleable#lbTimePicker_is24HourFormat
 * @attr ref R.styleable#lbTimePicker_useCurrentTime
 */
public class TimePicker extends Picker {

    static final String TAG = "TimePicker";

    private static final int AM_INDEX = 0;
    private static final int PM_INDEX = 1;

    private static final int HOURS_IN_HALF_DAY = 12;
    PickerColumn mHourColumn;
    PickerColumn mMinuteColumn;
    PickerColumn mAmPmColumn;
    private ViewGroup mPickerView;
    private View mAmPmSeparatorView;
    int mColHourIndex;
    int mColMinuteIndex;
    int mColAmPmIndex;

    private final PickerUtility.TimeConstant mConstant;

    private boolean mIs24hFormat;

    private int mCurrentHour;
    private int mCurrentMinute;
    private int mCurrentAmPmIndex;

    /**
     * Constructor called when inflating a TimePicker widget. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme and the given
     * AttributeSet.
     *
     * @param context the context this TimePicker widget is associated with through which we can
     *                access the current theme attributes and resources
     * @param attrs the attributes of the XML tag that is inflating the TimePicker widget
     */
    public TimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor called when inflating a TimePicker widget.
     *
     * @param context the context this TimePicker widget is associated with through which we can
     *                access the current theme attributes and resources
     * @param attrs the attributes of the XML tag that is inflating the TimePicker widget
     * @param defStyleAttr An attribute in the current theme that contains a reference to a style
     *                     resource that supplies default values for the widget. Can be 0 to not
     *                     look for defaults.
     */
    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mConstant = PickerUtility.getTimeConstantInstance(Locale.getDefault(),
                context.getResources());

        setSeparator(mConstant.timeSeparator);
        mPickerView = findViewById(R.id.picker);
        final TypedArray attributesArray = context.obtainStyledAttributes(attrs,
                R.styleable.lbTimePicker);
        mIs24hFormat = attributesArray.getBoolean(R.styleable.lbTimePicker_is24HourFormat,
                DateFormat.is24HourFormat(context));
        boolean useCurrentTime = attributesArray.getBoolean(R.styleable.lbTimePicker_useCurrentTime,
                true);

        updateColumns(getTimePickerFormat());

        // The column range for the minute and AM/PM column is static and does not change, whereas
        // the hour column range can change depending on whether 12 or 24 hour format is set at
        // any given time.
        updateHourColumn(false);
        updateMin(mMinuteColumn, 0);
        updateMax(mMinuteColumn, 59);

        updateMin(mAmPmColumn, 0);
        updateMax(mAmPmColumn, 1);

        updateAmPmColumn();

        if (useCurrentTime) {
            Calendar currentDate = PickerUtility.getCalendarForLocale(null,
                    mConstant.locale);
            setHour(currentDate.get(Calendar.HOUR_OF_DAY));
            setMinute(currentDate.get(Calendar.MINUTE));
        }
    }

    private static boolean updateMin(PickerColumn column, int value) {
        if (value != column.getMinValue()) {
            column.setMinValue(value);
            return true;
        }
        return false;
    }

    private static boolean updateMax(PickerColumn column, int value) {
        if (value != column.getMaxValue()) {
            column.setMaxValue(value);
            return true;
        }
        return false;
    }

    /**
     *
     * @return the time picker format string based on the current system locale and the layout
     *         direction
     */
    private String getTimePickerFormat() {
        // Obtain the time format string per the current locale (e.g. h:mm a)
        String hmaPattern;
        if (Build.VERSION.SDK_INT >= 18) {
            hmaPattern = DateFormat.getBestDateTimePattern(mConstant.locale, "hma");
        } else {
            // getTimeInstance is not very reliable and it may not include 'a' (for AM/PM)
            // in the returned pattern string. In those cases, we assume that am/pm appears at the
            // end of the fields. Need to find a more reliable way for API below 18.
            hmaPattern  = ((SimpleDateFormat) java.text.DateFormat
                    .getTimeInstance(java.text.DateFormat.FULL, mConstant.locale)).toPattern();
        }

        boolean isRTL = TextUtils.getLayoutDirectionFromLocale(mConstant.locale) == View
                .LAYOUT_DIRECTION_RTL;
        boolean isAmPmAtEnd = (hmaPattern.indexOf('a') >= 0)
                ? (hmaPattern.indexOf("a") > hmaPattern.indexOf("m")) : true;
        // Hour will always appear to the left of minutes regardless of layout direction.
        String timePickerFormat = isRTL ? "mh" : "hm";

        return isAmPmAtEnd ? (timePickerFormat + "a") : ("a" + timePickerFormat);
    }

    private void updateColumns(String timePickerFormat) {
        if (TextUtils.isEmpty(timePickerFormat)) {
            timePickerFormat = "hma";
        }
        timePickerFormat = timePickerFormat.toUpperCase();

        mHourColumn = mMinuteColumn = mAmPmColumn = null;
        mColHourIndex = mColMinuteIndex = mColAmPmIndex = -1;

        ArrayList<PickerColumn> columns = new ArrayList<>(3);
        for (int i = 0; i < timePickerFormat.length(); i++) {
            switch (timePickerFormat.charAt(i)) {
                case 'H':
                    columns.add(mHourColumn = new PickerColumn());
                    mHourColumn.setStaticLabels(mConstant.hours24);
                    mColHourIndex = i;
                    break;
                case 'M':
                    columns.add(mMinuteColumn = new PickerColumn());
                    mMinuteColumn.setStaticLabels(mConstant.minutes);
                    mColMinuteIndex = i;
                    break;
                case 'A':
                    columns.add(mAmPmColumn = new PickerColumn());
                    mAmPmColumn.setStaticLabels(mConstant.ampm);
                    mColAmPmIndex = i;
                    updateMin(mAmPmColumn, 0);
                    updateMax(mAmPmColumn, 1);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid time picker format.");
            }
        }
        setColumns(columns);
        mAmPmSeparatorView = mPickerView.getChildAt(mColAmPmIndex == 0 ? 1 :
                (2 * mColAmPmIndex - 1));
    }

    /**
     * Updates the range in the hour column and notifies column changed if notifyChanged is true.
     * Hour column can have either [0-23] or [1-12] depending on whether the 24 hour format is set
     * or not.
     *
     * @param notifyChanged {code true} if we should notify data set changed on the hour column,
     *                      {@code false} otherwise.
     */
    private void updateHourColumn(boolean notifyChanged) {
        updateMin(mHourColumn, mIs24hFormat ? 0 : 1);
        updateMax(mHourColumn, mIs24hFormat ? 23 : 12);
        if (notifyChanged) {
            setColumnAt(mColHourIndex, mHourColumn);
        }
    }

    /**
     * Updates AM/PM column depending on whether the 24 hour format is set or not. The visibility of
     * this column is set to {@code GONE} for a 24 hour format, and {@code VISIBLE} in 12 hour
     * format. This method also updates the value of this column for a 12 hour format.
     */
    private void updateAmPmColumn() {
        if (mIs24hFormat) {
            mColumnViews.get(mColAmPmIndex).setVisibility(GONE);
            mAmPmSeparatorView.setVisibility(GONE);
        } else {
            mColumnViews.get(mColAmPmIndex).setVisibility(VISIBLE);
            mAmPmSeparatorView.setVisibility(VISIBLE);
            setColumnValue(mColAmPmIndex, mCurrentAmPmIndex, false);
        }
    }

    /**
     * Sets the currently selected hour using a 24-hour time.
     *
     * @param hour the hour to set, in the range (0-23)
     * @see #getHour()
     */
    public void setHour(@IntRange(from = 0, to = 23) int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour: " + hour + " is not in [0-23] range in");
        }
        mCurrentHour = hour;
        if (!mIs24hFormat) {
            if (mCurrentHour >= HOURS_IN_HALF_DAY) {
                mCurrentAmPmIndex = PM_INDEX;
                if (mCurrentHour > HOURS_IN_HALF_DAY) {
                    mCurrentHour -= HOURS_IN_HALF_DAY;
                }
            } else {
                mCurrentAmPmIndex = AM_INDEX;
                if (mCurrentHour == 0) {
                    mCurrentHour = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmColumn();
        }
        setColumnValue(mColHourIndex, mCurrentHour, false);
    }

    /**
     * Returns the currently selected hour using 24-hour time.
     *
     * @return the currently selected hour in the range (0-23)
     * @see #setHour(int)
     */
    public int getHour() {
        if (mIs24hFormat) {
            return mCurrentHour;
        }
        if (mCurrentAmPmIndex == AM_INDEX) {
            return mCurrentHour % HOURS_IN_HALF_DAY;
        }
        return (mCurrentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
    }

    /**
     * Sets the currently selected minute.
     *
     * @param minute the minute to set, in the range (0-59)
     * @see #getMinute()
     */
    public void setMinute(@IntRange(from = 0, to = 59) int minute) {
        if (mCurrentMinute == minute) {
            return;
        }
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("minute: " + minute + " is not in [0-59] range.");
        }
        mCurrentMinute = minute;
        setColumnValue(mColMinuteIndex, mCurrentMinute, false);
    }

    /**
     * Returns the currently selected minute.
     *
     * @return the currently selected minute, in the range (0-59)
     * @see #setMinute(int)
     */
    public int getMinute() {
        return mCurrentMinute;
    }

    /**
     * Sets whether this widget displays a 24-hour mode or a 12-hour mode with an AM/PM picker.
     *
     * @param is24Hour {@code true} to display in 24-hour mode,
     *                 {@code false} ti display in 12-hour mode with AM/PM.
     * @see #is24Hour()
     */
    public void setIs24Hour(boolean is24Hour) {
        if (mIs24hFormat == is24Hour) {
            return;
        }
        // the ordering of these statements is important
        int currentHour = getHour();
        mIs24hFormat = is24Hour;
        updateHourColumn(true);
        setHour(currentHour);
        updateAmPmColumn();
    }

    /**
     * @return {@code true} if this widget displays time in 24-hour mode,
     *         {@code false} otherwise.
     *
     * @see #setIs24Hour(boolean)
     */
    public boolean is24Hour() {
        return mIs24hFormat;
    }

    /**
     * Only meaningful for a 12-hour time.
     *
     * @return {@code true} if the currently selected time is in PM,
     *         {@code false} if the currently selected time in in AM.
     */
    public boolean isPm() {
        return (mCurrentAmPmIndex == PM_INDEX);
    }

    @Override
    public void onColumnValueChanged(int columnIndex, int newValue) {
        if (columnIndex == mColHourIndex) {
            mCurrentHour = newValue;
        } else if (columnIndex == mColMinuteIndex) {
            mCurrentMinute = newValue;
        } else if (columnIndex == mColAmPmIndex) {
            mCurrentAmPmIndex = newValue;
        } else {
            throw new IllegalArgumentException("Invalid column index.");
        }
    }
}
