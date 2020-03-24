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

package com.example.android.support.vectordrawable.app;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.example.android.support.vectordrawable.R;

/**
 * A demo for AnimatedVectorDrawableCompat's listener support.
 */
public class AVDCListenerDemo extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.avdc_listener);
        final AppCompatImageView imageView1 = findViewById(R.id.imageView);
        final AppCompatImageView imageView2 = findViewById(R.id.imageView2);

        final TextView textView1 = findViewById(R.id.textView);
        textView1.setText("Should show start / end for first AVD");
        final TextView textView2 = findViewById(R.id.textView2);
        textView2.setText("Not affected by AVD, b/c removed after register");
        final TextView textView3 = findViewById(R.id.textView3);
        textView3.setText("Should show start / end for second AVD");
        final TextView textView4 = findViewById(R.id.textView4);
        textView4.setText("Not affected by AVD, b/c unregistered after register");

        final Drawable drawable1 = imageView1.getDrawable();
        final Drawable drawable2 = imageView2.getDrawable();

        Animatable2Compat.AnimationCallback textView1Callback = new
                Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationStart(Drawable drawable) {
                        textView1.setText("AVD 1 started");
                    }

                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        textView1.setText("AVD 1 Ended");
                    }
                };
        Animatable2Compat.AnimationCallback textView2Callback = new
                Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationStart(Drawable drawable) {
                        textView2.setText("AVD 1 started");
                    }

                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        textView2.setText("AVD 1 Ended");
                    }
                };
        AnimatedVectorDrawableCompat.registerAnimationCallback(drawable1, textView1Callback);
        AnimatedVectorDrawableCompat.registerAnimationCallback(drawable1, textView2Callback);
        AnimatedVectorDrawableCompat.clearAnimationCallbacks(drawable1);
        AnimatedVectorDrawableCompat.registerAnimationCallback(drawable1, textView1Callback);

        AnimatedVectorDrawableCompat.registerAnimationCallback(drawable2,
                new Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationStart(Drawable drawable) {
                        textView3.setText("AVD 2 started");
                    }

                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        textView3.setText("AVD 2 Ended");
                    }
                });

        Animatable2Compat.AnimationCallback textView4Callback = new
                Animatable2Compat.AnimationCallback() {
                    @Override
                    public void onAnimationStart(Drawable drawable) {
                        textView4.setText("AVD 2 started");
                    }

                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        textView4.setText("AVD 2 Ended");
                    }
                };

        AnimatedVectorDrawableCompat.registerAnimationCallback(drawable2, textView4Callback);
        AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable2, textView4Callback);

        // Touch the imageView will run the AVD.
        imageView1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!((Animatable) drawable1).isRunning()) {
                    ((Animatable) drawable1).start();
                }
                return true;
            }
        });

        imageView2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!((Animatable) drawable2).isRunning()) {
                    ((Animatable) drawable2).start();
                }
                return true;
            }
        });
    }
}
