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
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

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
        findViewById(R.id.btn_show_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSelectedDialog();
            }
        });
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

    /**
     * A simple {@link androidx.appcompat.app.AppCompatDialog} implementation which
     * inflates some items into it's options menu, and shows a toast when one is selected.
     */
    private class MenuDialog extends AppCompatDialog {

        public MenuDialog(Context context) {
            super(context);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.actions, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            Toast.makeText(getOwnerActivity(), "Dialog action selected: " + item.getTitle(),
                    Toast.LENGTH_SHORT).show();
            return true;
        }
    }

}
