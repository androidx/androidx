/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class FlingTestActivity extends Activity {

    private TextView mFlingRegion;
    private int mMinFlingVelocity;
    private MotionEvent mStartEvent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fling_test_activity);
        mFlingRegion = findViewById(R.id.fling_region);
        ViewConfiguration viewConfig = ViewConfiguration.get(getApplicationContext());
        mMinFlingVelocity = viewConfig.getScaledMinimumFlingVelocity();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // On slower devices, injected motion events may have large time gaps (>50ms), especially
        // for v1 gestures (UiScrollable). If these gaps are too large, velocity trackers may
        // drop events and report invalid velocities. Instead, this calculates the overall
        // velocity of the gesture and whether it is fast enough to be considered a fling.
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // Motion started, record start event.
                mStartEvent = event;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Motion ended, calculate average velocity (px/s) and whether it is a fling.
                long durationMs = event.getEventTime() - mStartEvent.getEventTime();
                float distanceX = event.getX() - mStartEvent.getX();
                float distanceY = event.getY() - mStartEvent.getY();
                float vX = 1000 * distanceX / durationMs;
                float vY = 1000 * distanceY / durationMs;
                if (Math.abs(vX) >= mMinFlingVelocity || Math.abs(vY) >= mMinFlingVelocity) {
                    boolean horizontal = Math.abs(vX) > Math.abs(vY);
                    if (horizontal) {
                        mFlingRegion.setText(vX > 0 ? "fling_left" : "fling_right");
                    } else {
                        mFlingRegion.setText(vY > 0 ? "fling_up" : "fling_down");
                    }
                }
        }
        return true;
    }
}
