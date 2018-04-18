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


import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.core.view.ViewCompat;
import androidx.transition.ChangeClipBounds;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

/**
 * This demonstrates usage of {@link ChangeClipBounds}.
 */
public class ChangeClipBoundsUsage extends TransitionUsageBase {

    private static final Rect BOUNDS = new Rect(20, 20, 100, 100);

    private final ChangeClipBounds mChangeClipBounds = new ChangeClipBounds();
    private ViewGroup mRoot;
    private ImageView mPhoto;

    @Override
    int getLayoutResId() {
        return R.layout.clip_bounds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = findViewById(R.id.root);
        mPhoto = findViewById(R.id.photo);
        findViewById(R.id.toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });
    }

    void toggle() {
        TransitionManager.beginDelayedTransition(mRoot, mChangeClipBounds);
        if (BOUNDS.equals(ViewCompat.getClipBounds(mPhoto))) {
            ViewCompat.setClipBounds(mPhoto, null);
        } else {
            ViewCompat.setClipBounds(mPhoto, BOUNDS);
        }
    }

}
