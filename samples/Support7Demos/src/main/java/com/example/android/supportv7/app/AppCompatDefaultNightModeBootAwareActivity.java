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


import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.android.supportv7.R;

/**
 * This is used to test implicit direct boot violations. To test:
 *
 * 1. Install the demo APK
 * 2. Make sure that you have a screen lock method set to pin or biometric
 * 3. Reboot the device and do not unlock the screen
 * 4. Run "adb shell am start -a android.intent.action.DEV_CORE_DIRECT_BOOT_BUG_REPRO"
 * 5. Check logcat that there are no crashes
 * 6. Unlock the screen and verify that this activity is in the foreground
 */
public class AppCompatDefaultNightModeBootAwareActivity extends AppCompatActivity {
    private StrictMode.VmPolicy mDefaultVmPolicy;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_night_mode);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        mDefaultVmPolicy = StrictMode.getVmPolicy();
        // Set a VM policy that crashes the app on everything, including the
        // implicit direct boot violations.
        StrictMode.setVmPolicy(
                new StrictMode.VmPolicy.Builder()
                        .detectAll()
                        .detectImplicitDirectBoot()
                        .penaltyLog()
                        .penaltyDeath()
                        .build());
        super.attachBaseContext(newBase);
    }

    @Override
    protected void onDestroy() {
        StrictMode.setVmPolicy(mDefaultVmPolicy);
        super.onDestroy();
    }

    public void setModeNightFollowSystem(View view) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setModeNightNo(View view) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }

    public void setModeNightYes(View view) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }

    public void setModeNightAutoTime(View view) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_TIME);
    }

    public void setModeNightAutoBattery(View view) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
    }
}
