/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.misc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;

import androidx.annotation.Nullable;
import androidx.car.app.sample.showcase.R;

/** Displays a simple {@link Activity} on the phone to show phone/car interaction */
public class OnPhoneActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show Activity over lock screen and turn on device screen.
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.phone_activity);

        View button = findViewById(R.id.button);
        button.setOnClickListener(this::onClick);
    }

    private void onClick(View v) {
        sendBroadcast(
                new Intent(GoToPhoneScreen.PHONE_COMPLETE_ACTION).setPackage(getPackageName()));
        finish();
    }
}
