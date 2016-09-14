/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.example.android.support.transition.widget;

import android.os.Bundle;
import android.support.transition.TransitionManager;
import android.support.v4.view.GravityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import com.example.android.support.transition.R;

public class BeginDelayedUsage extends TransitionUsageBase {

    private FrameLayout mRoot;
    private Button mButton;

    @Override
    int getLayoutResId() {
        return R.layout.begin_delayed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = (FrameLayout) findViewById(R.id.root);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
    }

    private void toggle() {
        TransitionManager.beginDelayedTransition(mRoot);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mButton.getLayoutParams();
        if ((params.gravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) == GravityCompat.END) {
            params.gravity = params.gravity ^ GravityCompat.END | GravityCompat.START;
        } else {
            params.gravity = params.gravity ^ GravityCompat.START | GravityCompat.END;
        }
        mButton.setLayoutParams(params);
    }

}
