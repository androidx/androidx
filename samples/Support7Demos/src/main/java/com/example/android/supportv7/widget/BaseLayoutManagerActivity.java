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

package com.example.android.supportv7.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;
import com.example.android.supportv7.widget.util.ConfigToggle;
import com.example.android.supportv7.widget.util.ConfigViewHolder;

/**
 * A simple activity that can be extended to demonstrate LayoutManagers.
 * <p>
 * It initializes a sample adapter and a list of configuration options. Extending activities can
 * define the {@link ConfigToggle} list depending on its functionality.
 */
abstract public class BaseLayoutManagerActivity<T extends RecyclerView.LayoutManager>
        extends Activity {

    protected T mLayoutManager;

    protected RecyclerView mRecyclerView;

    private ConfigToggle[] mConfigToggles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_layout_manager);
        initToggles();
        initRecyclerView();
        initSpinner();
    }

    abstract protected T createLayoutManager();

    private void initRecyclerView() {
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = createLayoutManager();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(createAdapter());
        ((DefaultItemAnimator)mRecyclerView.getItemAnimator()).setSupportsChangeAnimations(true);
        onRecyclerViewInit(mRecyclerView);
    }

    protected void onRecyclerViewInit(RecyclerView recyclerView) {

    }

    protected RecyclerView.Adapter createAdapter() {
        return new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public SimpleStringAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final SimpleStringAdapter.ViewHolder vh = super
                        .onCreateViewHolder(parent, viewType);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int pos = vh.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION && pos + 1 < getItemCount()) {
                            swap(pos, pos + 1);
                        }
                    }
                });
                return vh;
            }
        };
    }

    private void initToggles() {
        mConfigToggles = createConfigToggles();
        RecyclerView configView = findViewById(R.id.config_recycler_view);
        configView.setAdapter(mConfigAdapter);
        configView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        configView.setHasFixedSize(true);
    }

    public void onScrollClicked(View view) {
        final EditText scrollOffset = findViewById(R.id.scroll_offset);
        final CheckBox checkBox = findViewById(R.id.enable_smooth_scroll);
        final Spinner spinner = findViewById(R.id.spinner);

        Integer offset = null;
        String offsetString = scrollOffset.getText().toString();
        try {
            offset = Integer.parseInt(offsetString);
        } catch (NumberFormatException ex) {

        }
        final boolean smooth = checkBox.isChecked();
        if (offset == null) {
            scrollToPosition(smooth, spinner.getSelectedItemPosition());
        } else {
            scrollToPositionWithOffset(smooth, spinner.getSelectedItemPosition(), offset);
        }
    }

    private void initSpinner() {
        final Spinner spinner = findViewById(R.id.spinner);
        spinner.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return mRecyclerView.getAdapter().getItemCount();
            }

            @Override
            public Integer getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = new TextView(parent.getContext());
                }
                ((TextView) convertView).setText(" " + position + " ");
                return convertView;
            }
        });
    }

    protected void scrollToPosition(boolean smooth, int position) {
        if (smooth) {
            mRecyclerView.smoothScrollToPosition(position);
        } else {
            mRecyclerView.scrollToPosition(position);
        }
    }

    protected void scrollToPositionWithOffset(boolean smooth, int position, int offset) {
        scrollToPosition(smooth, position);
    }

    protected ConfigToggle[] createConfigToggles() {
        return new ConfigToggle[] {};
    }

    private RecyclerView.Adapter mConfigAdapter = new RecyclerView.Adapter<ConfigViewHolder>() {
        @Override
        public ConfigViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConfigViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.config_view_toggle, parent, false));
        }

        @Override
        public void onBindViewHolder(ConfigViewHolder holder, int position) {
            ConfigToggle toggle = mConfigToggles[position];
            holder.bind(toggle);
        }

        @Override
        public int getItemCount() {
            return mConfigToggles.length;
        }
    };
}
