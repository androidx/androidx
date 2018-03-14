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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.android.supportv7.R;

/**
 * This demonstrates usage of the Action Bar's hide on content scroll
 */
public class ActionBarHideOnScroll extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide on content scroll requires an overlay action bar, so request one
        supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.action_bar_hide_scroll);

        // Enable hide on scroll
        getSupportActionBar().setHideOnContentScrollEnabled(true);
    }

}
