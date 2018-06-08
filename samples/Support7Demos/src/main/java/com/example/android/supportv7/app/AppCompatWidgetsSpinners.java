/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv7.app;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

/**
 * This demonstrates the styled {@link android.widget.Spinner} widgets in AppCompat.
 */
public class AppCompatWidgetsSpinners extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_widgets_text_spinners);

        // Fetch the Spinners and set an adapter
        Spinner spinner = findViewById(R.id.widgets_spinner);
        spinner.setAdapter(new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, Cheeses.sCheeseStrings));

        spinner = findViewById(R.id.widgets_spinner_underlined);
        spinner.setAdapter(new ArrayAdapter<>(this,
                R.layout.support_simple_spinner_dropdown_item, Cheeses.sCheeseStrings));
    }

}
