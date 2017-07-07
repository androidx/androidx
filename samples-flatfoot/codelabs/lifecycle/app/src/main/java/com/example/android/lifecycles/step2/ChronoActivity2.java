/*
 * Copyright 2017, The Android Open Source Project
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

package com.example.android.lifecycles.step2;

import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Chronometer;

import android.arch.lifecycle.LifecycleActivity;
import android.arch.lifecycle.ViewModelProviders;
import com.example.android.codelabs.lifecycle.R;

public class ChronoActivity2 extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // The ViewModelStore provides a new ViewModel or one previously created.
        ChronometerViewModel chronometerViewModel
                = ViewModelProviders.of(this).get(ChronometerViewModel.class);

        // Get the chronometer reference
        Chronometer chronometer = (Chronometer) findViewById(R.id.chronometer);

        if (chronometerViewModel.getStartDate() == null) {
            // If the start date is not defined, it's a new ViewModel so set it.
            long startTime = SystemClock.elapsedRealtime();
            chronometerViewModel.setStartDate(startTime);
            chronometer.setBase(startTime);
        } else {
            // Otherwise the ViewModel has been retained, set the chronometer's base to the original
            // starting time.
            chronometer.setBase(chronometerViewModel.getStartDate());
        }

        chronometer.start();
    }
}
