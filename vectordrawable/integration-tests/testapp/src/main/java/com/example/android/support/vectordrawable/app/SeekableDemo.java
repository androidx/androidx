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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.vectordrawable.graphics.drawable.Animatable2;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;

import com.example.android.support.vectordrawable.R;

/**
 * Demonstrates usage of {@link SeekableAnimatedVectorDrawable}.
 */
public class SeekableDemo extends AppCompatActivity {

    enum AvdState {
        STOPPED,
        STARTED,
        PAUSED,
    }

    AvdState mState = AvdState.STOPPED;

    private Button mStart;
    private Button mStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.seekable_demo);

        final ImageView image = findViewById(R.id.image);
        mStart = findViewById(R.id.start);
        mStop = findViewById(R.id.stop);
        final SeekBar seekBar = findViewById(R.id.seek);

        final SeekableAnimatedVectorDrawable avd =
                SeekableAnimatedVectorDrawable.create(this, R.drawable.ic_hourglass_animation);

        if (avd == null) {
            finish();
            return;
        }
        avd.registerAnimationCallback(new Animatable2.AnimationCallback() {
            @Override
            public void onAnimationEnd(@NonNull Drawable drawable) {
                mState = AvdState.STOPPED;
                updateButtons();
            }
        });

        image.setImageDrawable(avd);

        mStart.setOnClickListener((v) -> {
            switch (mState) {
                case STOPPED:
                    avd.start();
                    mState = AvdState.STARTED;
                    updateButtons();
                    break;
                case STARTED:
                    avd.pause();
                    mState = AvdState.PAUSED;
                    updateButtons();
                    break;
                case PAUSED:
                    avd.resume();
                    mState = AvdState.STARTED;
                    updateButtons();
                    break;
            }
        });
        mStop.setOnClickListener((v) -> {
            avd.stop();
            mState = AvdState.STOPPED;
            updateButtons();
        });

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

    void updateButtons() {
        switch (mState) {
            case STOPPED:
                mStart.setEnabled(true);
                mStart.setText(R.string.start);
                mStop.setEnabled(false);
                break;
            case STARTED:
                mStart.setEnabled(true);
                mStart.setText(R.string.pause);
                mStop.setEnabled(true);
                break;
            case PAUSED:
                mStart.setEnabled(true);
                mStart.setText(R.string.resume);
                mStop.setEnabled(true);
                break;
        }
    }
}
