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

import android.content.res.Resources;
import android.support.v17.leanback.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * Date/Time Picker related constants
 */
class PickerConstant {

    public final String[] months;
    public final String[] days;
    public final String[] hours12;
    public final String[] hours24;
    public final String[] minutes;
    public final String[] ampm;
    public final String dateSeparator;
    public final String timeSeparator;
    public final Locale locale;

    public PickerConstant(Locale locale, Resources resources) {
        this.locale = locale;
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
        months = symbols.getShortMonths();
        Calendar calendar = Calendar.getInstance(locale);
        days = createStringIntArrays(calendar.getMinimum(Calendar.DAY_OF_MONTH),
                calendar.getMaximum(Calendar.DAY_OF_MONTH), "%02d");
        hours12 = createStringIntArrays(1, 12, "%02d");
        hours24 = createStringIntArrays(0, 23, "%02d");
        minutes = createStringIntArrays(0, 59, "%02d");
        ampm = symbols.getAmPmStrings();
        dateSeparator = resources.getString(R.string.lb_date_separator);
        timeSeparator = resources.getString(R.string.lb_time_separator);
    }


    public static String[] createStringIntArrays(int firstNumber, int lastNumber, String format) {
        String[] array = new String[lastNumber - firstNumber + 1];
        for (int i = firstNumber; i <= lastNumber; i++) {
            if (format != null) {
                array[i - firstNumber] = String.format(format, i);
            } else {
                array[i - firstNumber] = String.valueOf(i);
            }
        }
        return array;
    }


}