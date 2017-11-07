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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.example.android.support.wear.R;

/**
 * Demo for AlertDialog on Wear.
 */
public class AlertDialogDemo extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alert_dialog_demo);

        AlertDialog dialog = createDialog();

        Button trigger = findViewById(R.id.dialog_button);
        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });
    }

    private AlertDialog createDialog() {
        return new AlertDialog.Builder(this)
                .setTitle("AppCompatDialog")
                .setMessage("Lorem ipsum dolor...")
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancel", null)
                .create();
    }
}
