/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.android.support.wear.R;

/**
 * Demo for AlertDialog on Wear.
 */
public class AlertDialogDemo extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_demo);

        AlertDialog v7Dialog = createV7Dialog();
        android.app.AlertDialog frameworkDialog = createFrameworkDialog();

        Button v7Trigger = findViewById(R.id.v7_dialog_button);
        v7Trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v7Dialog.show();
            }
        });

        Button frameworkTrigger = findViewById(R.id.framework_dialog_button);
        frameworkTrigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                frameworkDialog.show();
            }
        });
    }

    private AlertDialog createV7Dialog() {
        Drawable drawable = getDrawable(R.drawable.app_sample_code);
        return new AlertDialog.Builder(this)
                .setTitle("AppCompatDialog")
                .setMessage("Lorem ipsum dolor...")
                .setPositiveButton("Ok", null)
                .setPositiveButtonIcon(drawable)
                .setNegativeButton("Cancel", null)
                .create();
    }

    private android.app.AlertDialog createFrameworkDialog() {
        return new android.app.AlertDialog.Builder(this)
                .setTitle("FrameworkDialog")
                .setMessage("Lorem ipsum dolor...")
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .create();
    }
}
