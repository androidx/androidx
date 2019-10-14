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

package com.example.androidx.car;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.car.widget.CarMenuItem;
import androidx.car.widget.CarToolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for {@link androidx.car.widget.CarToolbar}.
 */
public class CarToolbarActivity extends AppCompatActivity {
    private static final String TAG = "CarToolbarActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_toolbar);

        CarToolbar carToolbar = findViewById(R.id.car_toolbar);
        carToolbar.setNavigationIconOnClickListener(v -> onSupportNavigateUp());

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onSupportNavigateUp());
        setSupportActionBar(toolbar);

        List<CarMenuItem> items = new ArrayList<>();

        CarMenuItem.OnClickListener onClickListener = item -> {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                if (item.isCheckable()) {
                    Log.d(TAG, "Switch checked? " + item.isChecked());
                } else {
                    Log.d(TAG, "Clicked title: " + item.getTitle());
                }
            }
        };

        findViewById(R.id.add_action).setOnClickListener(v -> {
            items.add(new CarMenuItem.Builder()
                    .setTitle("Action item")
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                    .setIcon(this, android.R.drawable.sym_def_app_icon)
                    .setOnClickListener(onClickListener)
                    .build());
            carToolbar.setMenuItems(items);
        });

        findViewById(R.id.add_checkable).setOnClickListener(v -> {
            items.add(new CarMenuItem.Builder()
                    .setTitle("Checkable item")
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.ALWAYS)
                    .setCheckable(true)
                    .setOnClickListener(onClickListener)
                    .build());
            carToolbar.setMenuItems(items);
        });

        findViewById(R.id.add_if_room).setOnClickListener(v -> {
            items.add(new CarMenuItem.Builder()
                    .setTitle("If-Room item")
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.IF_ROOM)
                    .setIcon(this, android.R.drawable.sym_def_app_icon)
                    .setOnClickListener(onClickListener)
                    .build());
            carToolbar.setMenuItems(items);
        });

        findViewById(R.id.add_overflow).setOnClickListener(v -> {
            items.add(new CarMenuItem.Builder()
                    .setTitle("Overflow item")
                    .setDisplayBehavior(CarMenuItem.DisplayBehavior.NEVER)
                    .setOnClickListener(onClickListener)
                    .build());
            carToolbar.setMenuItems(items);
        });

        findViewById(R.id.clear_menu).setOnClickListener(v -> {
            items.clear();
            carToolbar.setMenuItems(null);
        });
    }
}
