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

package com.example.androidx.car;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

/**
 * Demo activity for themes and custom views on a {@link TabLayout}.
 */
public class TabLayoutDemoActivity extends AppCompatActivity {
    private TabLayout mTabLayout;

    private boolean mShowText = true;
    private boolean mShowIcon = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tab_bar_view_activity);

        mTabLayout = findViewById(R.id.tab_layout);

        findViewById(R.id.tab_align_start).setOnClickListener(
                v -> mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE));

        findViewById(R.id.tab_align_center).setOnClickListener(
                v -> mTabLayout.setTabMode(TabLayout.MODE_FIXED));

        findViewById(R.id.add_tab).setOnClickListener(v -> addTab());
        findViewById(R.id.remove_tab).setOnClickListener(v -> removeTab());

        findViewById(R.id.tab_text_and_icon).setOnClickListener(v -> {
            mShowText = true;
            mShowIcon = true;
            refreshTabs();
        });

        findViewById(R.id.tab_text_only).setOnClickListener(v -> {
            mShowText = true;
            mShowIcon = false;
            refreshTabs();
        });

        findViewById(R.id.tab_icon_only).setOnClickListener(v -> {
            mShowText = false;
            mShowIcon = true;
            refreshTabs();
        });
    }

    private void addTab() {
        int tabCount = mTabLayout.getTabCount();

        TabLayout.Tab tab = mTabLayout.newTab()
                .setCustomView(R.layout.car_tab_view);

        if (mShowIcon) {
            tab.setIcon(R.drawable.ic_home);
        }

        if (mShowText) {
            tab.setText("Tab " + tabCount);
        }

        mTabLayout.addTab(tab);
    }

    private void removeTab() {
        int tabCount = mTabLayout.getTabCount();

        if (tabCount > 0) {
            mTabLayout.removeTabAt(tabCount - 1);
        }
    }

    private void refreshTabs() {
        int tabCount = mTabLayout.getTabCount();
        mTabLayout.removeAllTabs();

        for (int i = 0; i < tabCount; i++) {
            addTab();
        }
    }
}
