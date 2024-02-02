/*
 * Copyright 2024 The Android Open Source Project
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
import android.graphics.Point;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

/** {@link Activity} for testing gesture margins by tracking relative touch coordinates. */
public class MarginTestActivity extends Activity {

    private Point mLastTouch;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.margin_test_activity);
        Button button = (Button) findViewById(R.id.button);
        button.setOnTouchListener((v, e) -> {
            mLastTouch = new Point((int) e.getX(), (int) e.getY());
            return true;
        });
    }

    @Nullable
    public Point getLastTouch() {
        return mLastTouch;
    }
}
