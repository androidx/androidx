/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;
import com.example.android.supportv7.widget.util.ConfigToggle;

/**
 * A configurably janky activity that uses {@link LinearLayoutManager}.
 */
public class LinearLayoutManagerJankActivity extends LinearLayoutManagerActivity {

    private boolean mBindSlowdownEnabled = true;
    private boolean mInflateSlowdownEnabled = true;

    /**
     * Spin wait. Used instead of sleeping so a core is used up for the duration, and so
     * traces/sampled profiling show the sections as expensive, and not just a scheduling mistake.
     */
    private static void spinWaitMs(long ms) {
        long start = System.nanoTime();
        while (System.nanoTime() - start < ms * 1000L * 1000L);
    }

    @Override
    protected RecyclerView.Adapter createAdapter() {
        return new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public void onBindViewHolder(ViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                if (mBindSlowdownEnabled) spinWaitMs(8);
            }

            @Override
            public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                if (mInflateSlowdownEnabled) spinWaitMs(4);
                return super.onCreateViewHolder(parent, viewType);
            }
        };
    }

    @Override
    protected ConfigToggle[] createConfigToggles() {
        return new ConfigToggle[]{
                new ConfigToggle(this, R.string.enable_bind_slowdown) {
                    @Override
                    public boolean isChecked() { return mBindSlowdownEnabled; }

                    @Override
                    public void onChange(boolean newValue) { mBindSlowdownEnabled = newValue; }
                },
                new ConfigToggle(this, R.string.enable_inflate_slowdown) {
                    @Override
                    public boolean isChecked() { return mInflateSlowdownEnabled; }

                    @Override
                    public void onChange(boolean newValue) { mInflateSlowdownEnabled = newValue; }
                },
                new ConfigToggle(this, R.string.enable_prefetch) {
                    @Override
                    public boolean isChecked() { return mLayoutManager.isItemPrefetchEnabled(); }

                    @Override
                    public void onChange(boolean newValue) {
                        mLayoutManager.setItemPrefetchEnabled(newValue);
                    }
                },
        };
    }
}
