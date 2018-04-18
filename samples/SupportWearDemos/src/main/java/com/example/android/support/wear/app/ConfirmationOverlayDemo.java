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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.wear.widget.ConfirmationOverlay;

import com.example.android.support.wear.R;

/** Main activity for the ConfirmationOverlay demo. */
public class ConfirmationOverlayDemo extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirmation_overlay_demo);
        final ViewGroup content = findViewById(R.id.content);
        final ConfirmationOverlay overlay = new ConfirmationOverlay();

        Button activityTrigger = findViewById(R.id.activity_overlay_button);
        activityTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.showOn(ConfirmationOverlayDemo.this);
            }
        });

        Button viewTrigger = findViewById(R.id.view_overlay_button);
        viewTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overlay.showAbove(content);
            }
        });
    }
}
