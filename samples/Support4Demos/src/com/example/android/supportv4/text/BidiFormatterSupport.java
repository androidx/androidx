/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.supportv4.text;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.text.BidiFormatter;
import android.widget.TextView;
import com.example.android.supportv4.R;

/**
 * This example illustrates a common usage of the BidiFormatter in the Android support library.
 */
public class BidiFormatterSupport extends Activity {

    private static String text = "%s הוא עסוק";
    private static String phone = "+1 650 253 0000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bidiformater_support);

        String formattedText = String.format(text, phone);

        TextView tv_sample = (TextView) findViewById(R.id.textview_without_bidiformatter);
        tv_sample.setText(formattedText);

        TextView tv_bidiformatter = (TextView) findViewById(R.id.textview_with_bidiformatter);
        String wrappedPhone = BidiFormatter.getInstance(true /* rtlContext */).unicodeWrap(phone);
        formattedText = String.format(text, wrappedPhone);
        tv_bidiformatter.setText(formattedText);
    }
}
