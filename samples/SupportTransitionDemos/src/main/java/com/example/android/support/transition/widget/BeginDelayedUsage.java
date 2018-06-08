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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

public class BeginDelayedUsage extends TransitionUsageBase {

    private LinearLayout mRoot;
    private TextView mMessage;

    @Override
    int getLayoutResId() {
        return R.layout.begin_delayed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = findViewById(R.id.root);
        mMessage = findViewById(R.id.message);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });
    }

    private void toggle() {
        TransitionManager.beginDelayedTransition(mRoot);
        if (mMessage.getVisibility() != View.VISIBLE) {
            mMessage.setVisibility(View.VISIBLE);
        } else {
            mMessage.setVisibility(View.GONE);
        }
    }

}
