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

import com.example.android.supportv7.R;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

/**
 * This demonstrates the use of tinted navigation and overflow items.
 */
public class ToolbarTintIcons extends AppCompatActivity implements View.OnClickListener {

    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toolbar_tint_icons);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.btn_tint_navigation).setOnClickListener(this);
        findViewById(R.id.btn_tint_navigation_clear).setOnClickListener(this);
        findViewById(R.id.btn_tint_overflow).setOnClickListener(this);
        findViewById(R.id.btn_tint_overflow_clear).setOnClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions_tint, menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_tint_navigation:
                mToolbar.setNavigationTintList(ColorStateList.valueOf(Color.BLUE));
                break;
            case R.id.btn_tint_navigation_clear:
                mToolbar.setNavigationTintList(null);
                break;
            case R.id.btn_tint_overflow:
                mToolbar.setOverflowTintList(ColorStateList.valueOf(Color.RED));
                break;
            case R.id.btn_tint_overflow_clear:
                mToolbar.setOverflowTintList(null);
                break;
        }
    }
}
