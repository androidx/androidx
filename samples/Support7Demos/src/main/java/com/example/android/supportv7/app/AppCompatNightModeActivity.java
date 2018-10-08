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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.android.supportv7.R;

/**
 * This demonstrates idiomatic usage of AppCompatActivity with Theme.AppCompat.DayNight
 */
public class AppCompatNightModeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_night_mode_activity);
        refreshIndicator();
    }

    public void setModeNightNo(View view) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        refreshIndicator();
    }

    public void setModeNightYes(View view) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        refreshIndicator();
    }

    public void setModeNightAuto(View view) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        refreshIndicator();
    }

    public void loadWebView(View view) {
        // Create a WebView, which will reset the Activity resources
        WebView webView = new WebView(this);
        refreshIndicator();
    }

    private void refreshIndicator() {
        final ViewGroup indicatorParent = findViewById(R.id.night_mode_indicator_parent);
        indicatorParent.removeAllViews();
        LayoutInflater.from(getThemedContext())
                .inflate(R.layout.appcompat_night_mode_indicator, indicatorParent);
    }
}