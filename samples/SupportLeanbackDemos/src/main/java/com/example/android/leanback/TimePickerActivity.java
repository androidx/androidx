/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.android.leanback;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.leanback.widget.picker.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Sample activity for {@link TimePicker}
 */
public class TimePickerActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_picker);
        final TimePicker picker = findViewById(R.id.example_picker);
        picker.setActivated(true);
        picker.setOnClickListener(v -> {
            final TextView result = findViewById(R.id.result);
            if (result != null) {
                final Calendar calendar = Calendar.getInstance();
                if (picker.is24Hour()) {
                    calendar.set(Calendar.HOUR_OF_DAY, picker.getHour() % 24);
                } else {
                    calendar.set(Calendar.HOUR, picker.getHour() % 12);
                    calendar.set(Calendar.AM_PM, picker.isPm() ? Calendar.PM : Calendar.AM);
                }
                calendar.set(Calendar.MINUTE, picker.getMinute());
                calendar.set(Calendar.SECOND, 0);
                result.setText(SimpleDateFormat.getTimeInstance().format(calendar.getTime()));
            }
        });
    }
}
