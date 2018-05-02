/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.support.design.R;
import com.google.android.material.snackbar.Snackbar;

/**
 * This demonstrates idiomatic usage of the snackbar
 */
public class SnackbarUsage extends AppCompatActivity {

    private ViewGroup mContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());

        mContentView = findViewById(R.id.content_view);
    }

    protected int getLayoutId() {
        return R.layout.design_snackbar;
    }

    public void showShort(View view) {
        Snackbar.make(mContentView, "Short snackbar message", Snackbar.LENGTH_SHORT).show();
    }

    public void showAction(View view) {
        Snackbar.make(mContentView, "Short snackbar message", Snackbar.LENGTH_SHORT)
                .setAction("Action", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(SnackbarUsage.this, "Snackbar Action pressed",
                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    public void showLong(View view) {
        Snackbar.make(mContentView, "Long snackbar message which wraps onto another line and"
                + "makes the Snackbar taller", Snackbar.LENGTH_SHORT).show();
    }

    public void showLongAction(View view) {
        Snackbar.make(mContentView, "Long snackbar message which wraps onto another line and"
                + "makes the Snackbar taller", Snackbar.LENGTH_SHORT)
                .setAction("Action", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(SnackbarUsage.this, "Snackbar Action pressed",
                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    public void showLongLongAction(View view) {
        Snackbar.make(mContentView, "Long snackbar message which wraps onto another line and"
                + "makes the Snackbar taller", Snackbar.LENGTH_SHORT)
                .setAction("Action which wraps", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(SnackbarUsage.this, "Snackbar Action pressed",
                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

}