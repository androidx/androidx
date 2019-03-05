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

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android.supportv7.R;

/**
 * This demonstrates changing background and elevation (on supported platforms) of ActionBar.
 */
public class ActionBarBackgroundChange extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_bar_background_change);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));

        findViewById(R.id.make_bg_transparent).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        actionBar.setBackgroundDrawable(new ColorDrawable(0x00FFFFFF));
                        actionBar.setElevation(0);
                    }
                });

        findViewById(R.id.make_bg_color).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        actionBar.setBackgroundDrawable(new ColorDrawable(0xFF80FFA0));
                        actionBar.setElevation(20);
                    }
                });
    }
}
