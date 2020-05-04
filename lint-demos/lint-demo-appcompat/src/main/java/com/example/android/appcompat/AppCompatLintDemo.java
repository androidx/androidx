/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.appcompat;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

/**
 * Dummy activity for the AppCompat Lint demo
 */
public class AppCompatLintDemo extends AppCompatActivity {
    private class ResourceLoader {
        private ColorStateList getColorStateList(int resourceId) {
            return AppCompatResources.getColorStateList(AppCompatLintDemo.this, resourceId);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView dummy = findViewById(R.id.dummy);
        // The following call to getColorStateList should be flagged by our Lint rule, since
        // it's on the core Android Resources class
        ColorStateList csl =
                getResources().getColorStateList(R.color.color_state_list_missing_android_alpha);
        dummy.setTextColor(csl);

        // The following call to getColorStateList should not be flagged by our Lint rule, since
        // it's on our own custom inner class
        ColorStateList csl2 = new ResourceLoader().getColorStateList(
                R.color.color_state_list_missing_android_alpha);
        dummy.setTextColor(csl2);

        Drawable dr = getResources().getDrawable(R.drawable.app_sample_code);
        dummy.setCompoundDrawables(dr, null, null, null);

        if (Build.VERSION.SDK_INT >= 23) {
            // These should be flagged to use TextViewCompat
            dummy.setCompoundDrawableTintList(csl);
            dummy.setCompoundDrawableTintMode(PorterDuff.Mode.DST);
        }

        // The following usage of the core Switch widget should be flagged by our Lint rule
        Switch mySwitch = new Switch(this);
        mySwitch.setChecked(true);

        if (Build.VERSION.SDK_INT >= 21) {
            // The following call should be flagged since we're extending AppCompatActivity
            setActionBar(new Toolbar(this));
        }
    }
}
