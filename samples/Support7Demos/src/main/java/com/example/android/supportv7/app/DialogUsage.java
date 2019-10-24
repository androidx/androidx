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
package com.example.android.supportv7.app;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;

import com.example.android.supportv7.R;

/**
 * This demonstrates idiomatic usage of AppCompatDialog.
 */
public class DialogUsage extends AppCompatActivity {

    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_usage);

        mSpinner = findViewById(R.id.spinner_dialogs);

        // Add an OnClickListener to show our selected dialog
        findViewById(R.id.btn_show_dialog).setOnClickListener(view -> showSelectedDialog());
    }

    private void showSelectedDialog() {
        switch (mSpinner.getSelectedItemPosition()) {
            case 0:
                showSimpleDialog();
                break;
            case 1:
                showButtonBarDialog();
                break;
        }
    }

    private void showSimpleDialog() {
        Dialog dialog = new AppCompatDialog(this);
        dialog.setTitle(R.string.dialog_title);
        dialog.setContentView(R.layout.dialog_content);
        dialog.show();
    }

    private void showButtonBarDialog() {
        Dialog dialog = new AppCompatDialog(this);
        dialog.setTitle(R.string.dialog_title);
        dialog.setContentView(R.layout.dialog_content_buttons);
        dialog.show();
    }

}
