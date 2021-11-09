/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.supportv7.widget;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.EditText;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;

/**
 * Simple activity to test {@link RecyclerView#smoothScrollBy(int, int, Interpolator, int)}
 * functionality.
 */
public class RecyclerViewSmoothScrollByActivity extends Activity {

    private RecyclerView mRecyclerView;
    private EditText mEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_rv_smoothscrollby);

        mRecyclerView = findViewById(R.id.recyclerView);
        mEditText = findViewById(R.id.editTextDuration);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final ViewHolder vh = super
                        .onCreateViewHolder(parent, viewType);
                return vh;
            }
        });
        mRecyclerView
                .addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        findViewById(R.id.buttonUp).setOnClickListener(v -> scroll(false));
        findViewById(R.id.buttonDown).setOnClickListener(v -> scroll(true));
    }

    private void scroll(boolean down) {
        int duration = 100;
        Editable editable = mEditText.getText();
        if (editable != null) {
            duration = Integer.parseInt(editable.toString());
        }
        mRecyclerView.smoothScrollBy(0, down ? 1000 : -1000, null, duration);
    }
}
