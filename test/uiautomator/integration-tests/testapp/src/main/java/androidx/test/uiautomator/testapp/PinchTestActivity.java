/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class PinchTestActivity extends Activity {

    private ScaleGestureDetector mScaleDetector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pinch_test_activity);

        final TextView scaleFactor = findViewById(R.id.scale_factor);

        mScaleDetector = new ScaleGestureDetector(this, new SimpleOnScaleGestureListener() {
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                scaleFactor.setText(Float.toString(scale));
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mScaleDetector.onTouchEvent(event);
    }
}
