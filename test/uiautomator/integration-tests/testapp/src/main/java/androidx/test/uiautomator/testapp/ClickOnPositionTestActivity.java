/*
 * Copyright 2022 The Android Open Source Project
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
import android.widget.TextView;

import androidx.annotation.Nullable;

public class ClickOnPositionTestActivity extends Activity {
    private String mTouchMessage = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.click_on_position_test_activity);

        TextView clickRegion = findViewById(R.id.click_region);

        clickRegion.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mTouchMessage += motionEvent.getY() < view.getHeight() / 2.0 ? "top_" : "bottom_";
                mTouchMessage += motionEvent.getX() < view.getWidth() / 2.0 ? "left" : "right";
            }
            return false;
        });
        clickRegion.setOnClickListener(view -> clickRegion.setText(mTouchMessage + "_clicked"));
        clickRegion.setOnLongClickListener(view -> {
            clickRegion.setText(mTouchMessage + "_long_clicked");
            return true;
        });
    }
}
