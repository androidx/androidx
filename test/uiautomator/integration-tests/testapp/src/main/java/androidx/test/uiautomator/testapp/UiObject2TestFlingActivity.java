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
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class UiObject2TestFlingActivity extends Activity {

    private GestureDetector mGestureDetector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.uiobject2_testfling_activity);

        TextView flingRegion = findViewById(R.id.fling_region);

        mGestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                // Fling has an opposite direction of swipe.
                boolean horizontal = Math.abs(vX) > Math.abs(vY);
                if (horizontal) {
                    flingRegion.setText(vX > 0 ? "fling_left" : "fling_right");
                } else {
                    flingRegion.setText(vY > 0 ? "fling_up" : "fling_down");
                }

                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }
}
