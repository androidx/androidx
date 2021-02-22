/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.wear.test.R;

public class ConfirmationActivityTestActivity extends Activity {

    /**
     * Configurable duration in milliseconds to display the confirmation dialog for.
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    private int mDuration = ConfirmationActivity.DEFAULT_ANIMATION_DURATION_MILLIS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirmation_activity_test_layout);
        Button button = findViewById(R.id.show_confirmation_activity_button);

        button.setOnClickListener(
                v -> {
                    Intent intent = new Intent(ConfirmationActivityTestActivity.this,
                            ConfirmationActivity.class);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                            ConfirmationActivity.SUCCESS_ANIMATION);
                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_DURATION_MILLIS,
                            mDuration);
                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "A message");

                    startActivity(intent);
                }
        );
    }
}
