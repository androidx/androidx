/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import androidx.wear.ambient.AmbientMode;

import com.example.android.support.wear.R;

/**
 * Main activity for the AmbientMode demo.
 */
public class AmbientModeDemo extends FragmentActivity implements
        AmbientMode.AmbientCallbackProvider {
    private TextView mStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ambient_demo);
        mStateTextView = findViewById(R.id.ambient_text);
        AmbientMode.AmbientController controller = AmbientMode.attachAmbientSupport(this);
    }

    @Override
    public AmbientMode.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }

    private class MyAmbientCallback extends AmbientMode.AmbientCallback {

        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            // Handle entering ambient mode
            mStateTextView.setText("Ambient");
            mStateTextView.setTextColor(Color.WHITE);
            mStateTextView.getPaint().setAntiAlias(false);
        }

        @Override
        public void onExitAmbient() {
            // Handle exiting ambient mode
            mStateTextView.setText("Interactive");
            mStateTextView.setTextColor(Color.GREEN);
            mStateTextView.getPaint().setAntiAlias(true);
        }
    }
}
