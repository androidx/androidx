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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;

import com.example.android.support.design.R;

/**
 * This demonstrates custom usage of the snackbar
 */
public class CustomSnackbarUsage extends AppCompatActivity {

    private CoordinatorLayout mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_snackbar_with_fab);

        mContentView = (CoordinatorLayout) findViewById(R.id.content_view);
    }

    /** Shows a custom snackbar with no action. */
    public void show(View view) {
        final LayoutInflater inflater = LayoutInflater.from(mContentView.getContext());
        final CustomSnackbarMainContent content =
                (CustomSnackbarMainContent) inflater.inflate(
                        R.layout.custom_snackbar_include, mContentView, false);
        final BaseTransientBottomBar.ContentViewCallback contentViewCallback =
                new BaseTransientBottomBar.ContentViewCallback() {
                    @Override
                    public void animateContentIn(int delay, int duration) {
                        ViewCompat.setAlpha(content, 0f);
                        ViewCompat.animate(content).alpha(1f).setDuration(duration)
                                .setStartDelay(delay).start();
                    }

                    @Override
                    public void animateContentOut(int delay, int duration) {
                        ViewCompat.setAlpha(content, 1f);
                        ViewCompat.animate(content).alpha(0f).setDuration(duration)
                                .setStartDelay(delay).start();
                    }
                };
        new CustomSnackbar(mContentView, content, contentViewCallback).setTitle("Custom title")
                .setSubtitle("Custom subtitle").show();
    }
}
