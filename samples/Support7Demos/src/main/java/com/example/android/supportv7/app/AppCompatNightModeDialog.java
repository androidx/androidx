/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.AppCompatDialog;

import com.example.android.supportv7.R;

/**
 * This demonstrates idiomatic usage of Dialog with Theme.AppCompat.DayNight
 */
public class AppCompatNightModeDialog extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_night_mode);
    }

    public void setModeNightNo(View view) {
        AppCompatDialog dialog = new AppCompatDialog(this, R.style.Theme_AppCompat_DayNight_Dialog);
        dialog.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        dialog.setTitle(R.string.dialog_title);
        dialog.setContentView(R.layout.dialog_content);
        dialog.show();
    }

    public void setModeNightYes(View view) {
        AppCompatDialog dialog = new AppCompatDialog(this, R.style.Theme_AppCompat_DayNight_Dialog);
        dialog.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        dialog.setTitle(R.string.dialog_title);
        dialog.setContentView(R.layout.dialog_content);
        dialog.show();
    }

    public void setModeNightAuto(View view) {
        AppCompatDialog dialog = new AppCompatDialog(this, R.style.Theme_AppCompat_DayNight_Dialog);
        dialog.getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        dialog.setTitle(R.string.dialog_title);
        dialog.setContentView(R.layout.dialog_content);
        dialog.show();
    }


}
