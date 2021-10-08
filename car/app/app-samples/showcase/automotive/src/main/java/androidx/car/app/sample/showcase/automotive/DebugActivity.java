/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.automotive;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.car.app.activity.CarAppActivity;
import androidx.car.app.sample.showcase.R;

/**
 * Demo activity, used to showcase the use of
 * {@link Activity#startActivityForResult(Intent, int, Bundle)} to receive results from a Car App.
 */
public class DebugActivity extends Activity {
    private TextView mTextView;
    private Button mButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_activity);

        mButton = findViewById(R.id.button);
        mTextView = findViewById(R.id.text);

        mButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, CarAppActivity.class);
            startActivityForResult(intent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mTextView.setText("Received code: " + resultCode + " action: "
                + (data != null ? data.getAction() : "null"));
    }
}
