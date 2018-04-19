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

import androidx.leanback.widget.picker.PinPicker;

/**
 * Sample activity for {@link PinPicker}
 */
public class PinPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pin_picker);
        final PinPicker picker = findViewById(R.id.example_picker);
        picker.setActivated(true);
        picker.setOnClickListener(v -> {
            final TextView result = findViewById(R.id.result);
            if (result != null) {
                result.setText(picker.getPin());
            }
        });
    }
}
