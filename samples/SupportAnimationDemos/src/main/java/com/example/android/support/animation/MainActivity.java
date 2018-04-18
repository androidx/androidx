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
 * Activity for chained spring animations.
 */
public class MainActivity extends Activity {
    private float mDampingRatio = 1.0f;
    private float mStiffness = 50.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chained_springs);
        final View lead = findViewById(R.id.lead);
        final View follow1 = findViewById(R.id.follow1);
        final View follow2 = findViewById(R.id.follow2);

        final SpringAnimation anim1X = new SpringAnimation(follow1, DynamicAnimation.TRANSLATION_X,
                lead.getTranslationX());
        final SpringAnimation anim1Y = new SpringAnimation(follow1, DynamicAnimation.TRANSLATION_Y,
                lead.getTranslationY());
        final SpringAnimation anim2X = new SpringAnimation(follow2, DynamicAnimation.TRANSLATION_X,
                follow1.getTranslationX());
        final SpringAnimation anim2Y = new SpringAnimation(follow2, DynamicAnimation.TRANSLATION_Y,
                follow1.getTranslationY());

        anim1X.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation dynamicAnimation, float value,
                                          float velocity) {
                anim2X.animateToFinalPosition(value);
            }
        });

        anim1Y.addUpdateListener(new DynamicAnimation.OnAnimationUpdateListener() {
            @Override
            public void onAnimationUpdate(DynamicAnimation dynamicAnimation, float value,
                                          float velocity) {
                anim2Y.animateToFinalPosition(value);
            }
        });

        ((View) lead.getParent()).setOnTouchListener(new View.OnTouchListener() {
            public float firstDownX = 0;
            public float firstDownY = 0;
            public VelocityTracker tracker;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {

                    if (motionEvent.getX() < lead.getX()
                            || motionEvent.getX() > lead.getX() + lead.getWidth()
                            || motionEvent.getY() < lead.getY()
                            || motionEvent.getY() > lead.getY() + lead.getHeight()) {
                        return false;
                    }

                    // Update the stiffness and damping ratio that are configured by user from the
                    // seekbar UI as needed.
                    anim1X.getSpring().setStiffness(mStiffness).setDampingRatio(mDampingRatio);
                    anim1Y.getSpring().setStiffness(mStiffness).setDampingRatio(mDampingRatio);
                    anim2X.getSpring().setStiffness(mStiffness).setDampingRatio(mDampingRatio);
                    anim2Y.getSpring().setStiffness(mStiffness).setDampingRatio(mDampingRatio);

                    firstDownX = motionEvent.getX() - lead.getTranslationX();
                    firstDownY = motionEvent.getY() - lead.getTranslationY();
                    tracker = VelocityTracker.obtain();
                    tracker.clear();
                    tracker.addMovement(motionEvent);
                } else if (motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                    float deltaX = motionEvent.getX() - firstDownX;
                    float deltaY = motionEvent.getY() - firstDownY;

                    // Directly manipulate the lead view.
                    lead.setTranslationX(deltaX);
                    lead.setTranslationY(deltaY);

                    // Animate the follow views to the new final position
                    anim1X.animateToFinalPosition(deltaX);
                    anim1Y.animateToFinalPosition(deltaY);

                    tracker.addMovement(motionEvent);
                }
                return true;
            }
        });
        setupSeekBars();
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
        dr.setProgress(80);
        stiff.setProgress(60);

    }
}
