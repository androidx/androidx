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
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class UiObject2TestPinchActivity extends Activity {

    private ScaleGestureDetector mScaleDetector;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.uiobject2_testpinch_activity);

        final TextView scaleFactor = (TextView)findViewById(R.id.scale_factor);

        mScaleDetector = new ScaleGestureDetector(this, new SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                Log.d("FOO", String.format("Beginning scale: %s", scale));
                float span = detector.getCurrentSpan();
                Log.d("FOO", String.format("Beginning span: %s", span));
                Log.d("FOO", String.format("Beginning span, X: %s, Y:%s", detector.getCurrentSpanX(), detector.getCurrentSpanY()));
                Log.d("FOO", String.format("Beginning focus: %s, %s", detector.getFocusX(), detector.getFocusY()));
                return true;

            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                float scale = detector.getScaleFactor();
                Log.d("FOO", String.format("Ending scale: %s", scale));
                float span = detector.getCurrentSpan();
                Log.d("FOO", String.format("Ending span: %s", span));
                Log.d("FOO", String.format("Ending span, X: %s, Y:%s", detector.getCurrentSpanX(), detector.getCurrentSpanY()));
                Log.d("FOO", String.format("Ending focus: %s, %s", detector.getFocusX(), detector.getFocusY()));
                scaleFactor.setText(Float.toString(scale));
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mScaleDetector.onTouchEvent(event);
    }
}
