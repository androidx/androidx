/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.supportv4.graphics;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.example.android.supportv4.R;

/**
 * Demonstrates use of a {@link RoundedBitmapDrawable}'s ability to become circular.
 */
public class RoundedBitmapDrawableActivity extends Activity {

    private static final int IMAGE_RES = R.drawable.android_robot;
    private RoundedBitmapDrawable mRoundedBitmapDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rounded_bitmap);

        // Create a bitmap and set it circular.
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), IMAGE_RES);
        mRoundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), bitmap);

        // Get references to the inflated views.
        ToggleButton toggle = findViewById(R.id.toggle_round);
        ImageView image = findViewById(R.id.image);

        // Set up initial view state and on checked change listener.
        image.setImageDrawable(mRoundedBitmapDrawable);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRoundedBitmapDrawable.setCircular(isChecked);
            }
        });
    }

}
