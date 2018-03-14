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
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.transition.Explode;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.example.android.support.transition.R;

import java.util.ArrayList;

/**
 * This demonstrates usage of {@link Explode} Transition type.
 */
public class ExplodeUsage extends TransitionUsageBase {

    private FrameLayout mRoot;
    private final ArrayList<View> mViews = new ArrayList<>();
    private final Explode mExplode = new Explode();

    final Rect mRect = new Rect();

    @Override
    int getLayoutResId() {
        return R.layout.explode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExplode.setEpicenterCallback(new Transition.EpicenterCallback() {
            @Override
            public Rect onGetEpicenter(@NonNull Transition transition) {
                return mRect;
            }
        });
        mRoot = findViewById(R.id.root);
        if (mViews.isEmpty()) {
            mViews.add(findViewById(R.id.view_1));
            mViews.add(findViewById(R.id.view_2));
            mViews.add(findViewById(R.id.view_3));
            mViews.add(findViewById(R.id.view_4));
        }
        findViewById(R.id.toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.getGlobalVisibleRect(mRect);
                TransitionManager.beginDelayedTransition(mRoot, mExplode);
                int vis = mViews.get(0).getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                for (View view : mViews) {
                    view.setVisibility(vis);
                }
            }
        });
    }

}
