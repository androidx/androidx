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

package com.example.android.supportv7.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.graphics.drawable.AnimatedStateListDrawableCompat;

import com.example.android.supportv7.R;

/**
 * Demonstrating usage of
 * {@link AnimatedStateListDrawableCompat}.
 */
public class AppCompatAnimatedSelector extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appcompat_animated_selector);
        final CheckBox checkbox = findViewById(R.id.checkbox);
        if (checkbox != null) {
            final Drawable asl = AppCompatResources.getDrawable(this, R.drawable.asl_heart_checked);
            checkbox.setButtonDrawable(asl);
        }
    }
}
