/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;

import com.example.android.support.vectordrawable.R;

/**
 * Demonstrates usage of {@link SeekableAnimatedVectorDrawable}.
 */
public class SeekableDemo extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seekable_demo);

        final ImageView image = findViewById(R.id.image);
        final Button start = findViewById(R.id.start);
        final Button stop = findViewById(R.id.stop);
        final SeekBar seekBar = findViewById(R.id.seek);

        final SeekableAnimatedVectorDrawable avd =
                SeekableAnimatedVectorDrawable.create(this, R.drawable.ic_hourglass_animation);

        if (avd == null) {
            finish();
            return;
        }
        avd.registerAnimationCallback(new SeekableAnimatedVectorDrawable.AnimationCallback() {

            @Override
            public void onAnimationStart(@NonNull SeekableAnimatedVectorDrawable drawable) {
                onAnimationRunning();
            }

            @Override
            public void onAnimationEnd(@NonNull SeekableAnimatedVectorDrawable drawable) {
                start.setEnabled(true);
                start.setText(R.string.start);
                stop.setEnabled(false);
                seekBar.setProgress(0);
            }

            @Override
            public void onAnimationPause(@NonNull SeekableAnimatedVectorDrawable drawable) {
                start.setEnabled(true);
                start.setText(R.string.resume);
                stop.setEnabled(true);
            }

            @Override
            public void onAnimationResume(@NonNull SeekableAnimatedVectorDrawable drawable) {
                onAnimationRunning();
            }

            private void onAnimationRunning() {
                start.setEnabled(true);
                start.setText(R.string.pause);
                stop.setEnabled(true);
            }

            @Override
            public void onAnimationUpdate(@NonNull SeekableAnimatedVectorDrawable drawable) {
                seekBar.setProgress((int) drawable.getCurrentPlayTime());
            }
        });

        image.setImageDrawable(avd);
        seekBar.setMax((int) avd.getTotalDuration());

        start.setOnClickListener((v) -> {
            if (!avd.isRunning()) {
                avd.start();
            } else if (!avd.isPaused()) {
                avd.pause();
            } else {
                avd.resume();
            }
        });
        stop.setOnClickListener((v) -> avd.stop());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    avd.setCurrentPlayTime(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
}
