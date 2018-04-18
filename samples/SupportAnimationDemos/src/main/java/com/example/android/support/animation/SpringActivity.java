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

package com.example.android.support.animation;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;

/**
 * This is a single spring animation. It provides a UI to interact with the spring, and two seek
 * bars to tune the spring constants.
 */
public class SpringActivity extends Activity {
    private float mDampingRatio;
    private float mStiffness;
    private SpringView mSpringView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View v = findViewById(R.id.container);
        mSpringView = findViewById(R.id.actual_spring);

        final View img = findViewById(R.id.imageView);
        setupSeekBars();
        final SpringAnimation anim = new SpringAnimation(img, DynamicAnimation.TRANSLATION_Y,
                0 /* final position */);
        anim.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation dynamicAnimation, float v, float v1) {
                // Update the drawing of the spring.
                mSpringView.setMassHeight(img.getY());
            }
        });

        ((View) img.getParent()).setOnTouchListener(new View.OnTouchListener() {
            public float touchOffset;
            public VelocityTracker vt;
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    // check whether the touch happens inside of the img view.
                    boolean inside = motionEvent.getX() >= img.getX()
                            && motionEvent.getX() <= img.getX() + img.getWidth()
                            && motionEvent.getY() >= img.getY()
                            && motionEvent.getY() <= img.getY() + img.getHeight();

                    anim.cancel();

                    if (!inside) {
                        return false;
                    }
                    // Apply this offset to all the subsequent events
                    touchOffset = img.getTranslationY() - motionEvent.getY();
                    vt = VelocityTracker.obtain();
                    vt.clear();
                }

                vt.addMovement(motionEvent);

                if (motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    img.setTranslationY(motionEvent.getY() + touchOffset);
                    // Updates the drawing of the spring.
                    mSpringView.setMassHeight(img.getY());
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_CANCEL
                        || motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                    // Compute the velocity in unit: pixel/second
                    vt.computeCurrentVelocity(1000);
                    float velocity = vt.getYVelocity();
                    anim.getSpring().setDampingRatio(mDampingRatio).setStiffness(mStiffness);
                    anim.setStartVelocity(velocity).start();
                    vt.recycle();
                }
                return true;
            }
        });
    }

    // Setup seek bars so damping ratio and stiffness for the spring can be modified through the UI.
    void setupSeekBars() {
        SeekBar dr = findViewById(R.id.damping_ratio);
        dr.setMax(130);
        final TextView drTxt = findViewById(R.id.damping_ratio_txt);
        dr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i < 80) {
                    mDampingRatio = i / 80.0f;
                } else if (i > 90) {
                    mDampingRatio = (float) Math.exp((i - 90) / 10.0);
                } else {
                    mDampingRatio = 1;
                }
                drTxt.setText(String.format("%.4f", (float) mDampingRatio));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar stiff = findViewById(R.id.stiffness);
        stiff.setMax(110);
        final TextView nfTxt = findViewById(R.id.stiffness_txt);
        stiff.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float stiffness = (float) Math.exp(i / 10d);
                mStiffness = stiffness;
                nfTxt.setText(String.format("%.3f", (float) stiffness));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        dr.setProgress(40);
        stiff.setProgress(60);
    }
}
