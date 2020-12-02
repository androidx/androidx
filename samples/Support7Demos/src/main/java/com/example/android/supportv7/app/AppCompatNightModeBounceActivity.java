/*
 * Copyright 2019 The Android Open Source Project
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


import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv7.R;

/**
 * An Activity which has a DayNight theme, which allows us to 'bounce' to our
 * AppCompatDefaultNightModeActivity activity, and back. This was created to allow easy testing
 * of changing the default DayNight mode for stopped Activities.
 */
public class AppCompatNightModeBounceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_night_mode_bounce);
    }

    public void launchActivity(View view) {
        startActivity(new Intent(this, AppCompatDefaultNightModeActivity.class));
    }
}
