/*
 * Copyright 2017 The Android Open Source Project
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
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.transition.AutoTransition;
import androidx.transition.SidePropagation;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

/**
 * Demonstrates usage of Slide.
 */
public class SidePropagationUsage extends TransitionUsageBase {

    private Transition mTransition;

    private LinearLayout mRoot;

    private boolean mVisible = true;

    @Override
    int getLayoutResId() {
        return R.layout.slide;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTransition = new AutoTransition();
        final SidePropagation propagation = new SidePropagation();
        propagation.setSide(Gravity.END);
        propagation.setPropagationSpeed(0.4f);
        mTransition.setPropagation(propagation);

        mRoot = findViewById(R.id.root);
        findViewById(R.id.toggle).setOnClickListener(v -> {
            mVisible = !mVisible;
            TransitionManager.beginDelayedTransition(mRoot, mTransition);
            for (int i = 0, count = mRoot.getChildCount(); i < count; i++) {
                mRoot.getChildAt(i).setVisibility(mVisible ? View.VISIBLE : View.GONE);
            }
        });
    }

}
