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
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.ArcMotion;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

/**
 * This demonstrates usage of {@link ArcMotion}.
 */
public class ArcMotionUsage extends TransitionUsageBase {

    private FrameLayout mRoot;
    private View mTarget;
    private Transition mTransition;

    @Override
    int getLayoutResId() {
        return R.layout.arc_motion;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = findViewById(R.id.root);
        mTarget = findViewById(R.id.target);
        mTransition = new ChangeBounds();
        mTransition.setPathMotion(new ArcMotion());
        mTransition.setInterpolator(new FastOutSlowInInterpolator());
        mTransition.setDuration(500);
        findViewById(R.id.move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(mRoot, mTransition);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mTarget.getLayoutParams();
                if ((lp.gravity & Gravity.START) == Gravity.START) {
                    lp.gravity = Gravity.END | Gravity.BOTTOM;
                } else {
                    lp.gravity = Gravity.START | Gravity.TOP;
                }
                mTarget.setLayoutParams(lp);
            }
        });
    }

}
