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

package com.example.android.support.transition.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.ArcMotion;
import androidx.transition.ChangeTransform;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

/**
 * This demonstrates basic usage of the ChangeTransform Transition.
 */
public class ChangeTransformUsage extends TransitionUsageBase {

    private LinearLayout mRoot;
    private FrameLayout mContainer1;
    private FrameLayout mContainer2;
    private ChangeTransform mChangeTransform;

    @Override
    int getLayoutResId() {
        return R.layout.change_transform;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChangeTransform = new ChangeTransform();
        mChangeTransform.setInterpolator(new FastOutSlowInInterpolator());
        mChangeTransform.setPathMotion(new ArcMotion());
        mRoot = findViewById(R.id.root);
        mContainer1 = findViewById(R.id.container_1);
        mContainer2 = findViewById(R.id.container_2);
        findViewById(R.id.toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(mRoot, mChangeTransform);
                toggle();
            }
        });
        showRedSquare(mContainer1);
    }

    void toggle() {
        if (mContainer2.getChildCount() > 0) {
            mContainer2.removeAllViews();
            showRedSquare(mContainer1);
        } else {
            mContainer1.removeAllViews();
            showRedSquare(mContainer2);
            mContainer2.getChildAt(0).setRotation(45);
        }
    }

    private void showRedSquare(FrameLayout container) {
        final View view = LayoutInflater.from(this)
                .inflate(R.layout.red_square, container, false);
        container.addView(view);
    }

}
