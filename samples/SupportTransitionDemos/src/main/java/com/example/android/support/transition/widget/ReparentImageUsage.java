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
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.transition.ChangeImageTransform;
import androidx.transition.ChangeTransform;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.example.android.support.transition.R;

/**
 * Demonstrates combination usage of ChangeTransform and ChangeImageTransform.
 */
public class ReparentImageUsage extends TransitionUsageBase {

    FrameLayout mOuterFrame;
    FrameLayout mInnerFrame;
    TransitionSet mTransition;
    int mPhotoSize;

    @Override
    int getLayoutResId() {
        return R.layout.reparent_image;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mOuterFrame = findViewById(R.id.outer_frame);
        mInnerFrame = findViewById(R.id.inner_frame);
        mPhotoSize = getResources().getDimensionPixelSize(R.dimen.photo_size);

        mTransition = new TransitionSet();
        mTransition.addTransition(new ChangeImageTransform());
        mTransition.addTransition(new ChangeTransform());

        addImageView(mOuterFrame, ImageView.ScaleType.CENTER_CROP, mPhotoSize);
        findViewById(R.id.toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(mOuterFrame, mTransition);
                if (mInnerFrame.getChildCount() > 0) {
                    mInnerFrame.removeAllViews();
                    addImageView(mOuterFrame, ImageView.ScaleType.CENTER_CROP, mPhotoSize);
                } else {
                    mOuterFrame.removeViewAt(1);
                    addImageView(mInnerFrame, ImageView.ScaleType.FIT_XY,
                            FrameLayout.LayoutParams.MATCH_PARENT);
                }
            }
        });
    }

    private void addImageView(FrameLayout parent, ImageView.ScaleType scaleType, int size) {
        final ImageView photo = new ImageView(this);
        photo.setImageResource(R.drawable.photo);
        photo.setId(R.id.photo);
        photo.setScaleType(scaleType);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(size, size);
        parent.addView(photo, lp);
    }

}
