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

import android.graphics.Matrix;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.transition.ChangeImageTransform;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

/**
 * This demonstrates basic usage of the ChangeImageTransform Transition.
 */
public class ChangeImageTransformUsage extends TransitionUsageBase {

    private ViewGroup mRoot;
    private ImageView mPhoto;

    private static final Transition TRANSITION = new ChangeImageTransform();

    @Override
    int getLayoutResId() {
        return R.layout.image_transform;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoot = findViewById(R.id.container);
        mPhoto = findViewById(R.id.photo);
        final View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(mRoot, TRANSITION);
                switch (v.getId()) {
                    case R.id.fit_xy:
                        mPhoto.setScaleType(ImageView.ScaleType.FIT_XY);
                        break;
                    case R.id.center:
                        mPhoto.setScaleType(ImageView.ScaleType.CENTER);
                        break;
                    case R.id.center_crop:
                        mPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        break;
                    case R.id.fit_start:
                        mPhoto.setScaleType(ImageView.ScaleType.FIT_START);
                        break;
                    case R.id.fit_end:
                        mPhoto.setScaleType(ImageView.ScaleType.FIT_END);
                        break;
                    case R.id.matrix:
                        mPhoto.setScaleType(ImageView.ScaleType.MATRIX);
                        final Matrix matrix = new Matrix();
                        matrix.setRotate(45.f);
                        matrix.postTranslate(200, 10);
                        mPhoto.setImageMatrix(matrix);
                        break;
                }
            }
        };
        findViewById(R.id.fit_xy).setOnClickListener(listener);
        findViewById(R.id.center).setOnClickListener(listener);
        findViewById(R.id.center_crop).setOnClickListener(listener);
        findViewById(R.id.fit_start).setOnClickListener(listener);
        findViewById(R.id.fit_end).setOnClickListener(listener);
        findViewById(R.id.matrix).setOnClickListener(listener);
    }

}
