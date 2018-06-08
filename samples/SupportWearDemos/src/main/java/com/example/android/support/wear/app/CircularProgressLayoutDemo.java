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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.CircularProgressLayout;

import com.example.android.support.wear.R;

import java.util.concurrent.TimeUnit;

/**
 * Main activity for the CircularProgressLayout demo.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class CircularProgressLayoutDemo extends Activity implements
        CircularProgressLayout.OnTimerFinishedListener, View.OnClickListener {

    private static final long TOTAL_TIME = TimeUnit.SECONDS.toMillis(10);

    CircularProgressLayout mCircularProgressLayout;
    TextView mChildView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cpl_demo);
        mCircularProgressLayout = findViewById(R.id.circularProgressLayout_layout);
        mChildView = findViewById(R.id.circularProgressLayout_child);

        mCircularProgressLayout.setOnClickListener(this);
        mCircularProgressLayout.setOnTimerFinishedListener(this);

        mCircularProgressLayout.setTotalTime(TOTAL_TIME);
        mCircularProgressLayout.startTimer();
    }

    @Override
    public void onTimerFinished(CircularProgressLayout layout) {
        if (layout == mCircularProgressLayout) {
            mChildView.setText(getString(R.string.cpl_finished));
            mCircularProgressLayout.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.cpl_light_green));
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mCircularProgressLayout && mCircularProgressLayout.isTimerRunning()) {
            mCircularProgressLayout.stopTimer();
            mChildView.setText(getString(R.string.cpl_clicked));
            mCircularProgressLayout.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.cpl_light_red));
        }
    }
}
