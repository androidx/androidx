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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.example.android.support.design.R;

/**
 * This demonstrates idiomatic usage of the bottom navigation widget.
 */
public class BottomNavigationViewUsage extends AppCompatActivity {
    private ColorStateList mOriginalTint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_bottom_navigation_view);
        Button buttonDisable = (Button) findViewById(R.id.button_disable);
        final BottomNavigationView bottom =
                (BottomNavigationView) findViewById(R.id.bottom_navigation);
        mOriginalTint = bottom.getItemIconTintList();
        buttonDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottom.getMenu().getItem(0).setEnabled(!bottom.getMenu().getItem(0).isEnabled());
            }
        });
        Button buttonAdd = (Button) findViewById(R.id.button_add);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MenuItem item = bottom.getMenu().add("Bananas");
                item.setIcon(android.R.drawable.ic_lock_power_off);
            }
        });
        Button buttonTint = (Button) findViewById(R.id.button_tint);
        buttonTint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bottom.getItemIconTintList() == null) {
                    bottom.setItemIconTintList(mOriginalTint);
                } else {
                    bottom.setItemIconTintList(null);
                }
            }
        });
    }
}
