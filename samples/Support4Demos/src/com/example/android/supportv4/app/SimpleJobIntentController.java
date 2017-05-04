/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.supportv4.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.android.supportv4.R;

/**
 * UI controller for SimpleJobIntentService.
 */
public class SimpleJobIntentController extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.simple_job_intent_controller);

        // Watch for button clicks.
        findViewById(R.id.worka).setOnClickListener(new WorkListener("ACTION_A",
                R.string.schedule_work_a));
        findViewById(R.id.workb).setOnClickListener(new WorkListener("ACTION_B",
                R.string.schedule_work_b));
        findViewById(R.id.workc).setOnClickListener(new WorkListener("ACTION_C",
                R.string.schedule_work_c));
    }

    class WorkListener implements View.OnClickListener {
        final String mAction;
        final String mLabel;

        WorkListener(String action, int label) {
            mAction = action;
            mLabel = getText(label).toString();
        }

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(mAction);
            intent.putExtra("label", mLabel);
            SimpleJobIntentService.enqueueWork(SimpleJobIntentController.this, intent);
        }
    }
}
