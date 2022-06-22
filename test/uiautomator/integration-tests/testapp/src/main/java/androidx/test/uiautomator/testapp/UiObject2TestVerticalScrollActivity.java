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
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.widget.TextView;

public class UiObject2TestVerticalScrollActivity extends Activity {

    private static final String TAG = UiObject2TestVerticalScrollActivity.class.getSimpleName();

    private GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.uiobject2_testverticalscroll_activity);

        /*
        final TextView topText = (TextView)findViewById(R.id.top_text);
        Log.d("FOO", String.format("top_text: %s, %s, %s, %s", topText.getLeft(), topText.getTop(), topText.getRight(), topText.getBottom()));
        final TextView fromTop5000 = (TextView)findViewById(R.id.from_top_5000);
        Log.d("FOO", String.format("from_top_5000: %s, %s, %s, %s", fromTop5000.getLeft(), fromTop5000.getTop(), fromTop5000.getRight(), fromTop5000.getBottom()));
        final TextView fromTop10000 = (TextView)findViewById(R.id.from_top_10000);
        final TextView fromTop15000 = (TextView)findViewById(R.id.from_top_15000);
*/
        final TextView flingDetected = (TextView)findViewById(R.id.fling_detected);

        mGestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent event) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                Log.d("FOO", "Fling detected!");
                flingDetected.setText("true");
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }
}
