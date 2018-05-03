/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.support.design.widget;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.android.support.design.R;
import com.google.android.material.tabs.TabLayout;

/**
 * This demonstrates usage of TabLayout with custom items
 */
public class TabLayoutCustomItemsUsage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_tabs_custom);

        // Retrieve the Toolbar from our content view, and set it as the action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create three tabs with custom views
        TabLayout tabLayout = findViewById(R.id.tabs);
        for (int i = 0; i < 3; i++) {
            TabLayout.Tab tab = tabLayout.newTab();

            tabLayout.addTab(tab);
            tab.setCustomView(R.layout.design_tab_custom);
        }
    }
}
