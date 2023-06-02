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
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/** {@link Activity} for testing multi-window (split screen) functionality. */
@RequiresApi(32) // FLAG_ACTIVITY_LAUNCH_ADJACENT may not work below API 32.
public class SplitScreenTestActivity extends Activity {

    static final String WINDOW_ID = "WINDOW_ID";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.split_screen_test_activity);

        String windowId = getIntent().getStringExtra(WINDOW_ID);

        TextView text = findViewById(R.id.window_id);
        text.setText(windowId == null ? "first" : windowId);
        text.setOnClickListener(v -> text.setText("I've been clicked!"));
        text.setOnLongClickListener(v -> {
            startActivity(
                    new Intent(this, SplitScreenTestActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                            .putExtra(WINDOW_ID, "second"));
            return true;
        });
    }
}
