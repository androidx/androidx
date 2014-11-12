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

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = createLayoutManager();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(createAdapter());
        mRecyclerView.getItemAnimator().setSupportsChangeAnimations(true);
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
        RecyclerView configView = (RecyclerView) findViewById(R.id.config_recycler_view);
        configView.setAdapter(mConfigAdapter);
        configView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        configView.setHasFixedSize(true);
    }

    public void onScrollClicked(View view) {
        final EditText scrollOffset = (EditText) findViewById(R.id.scroll_offset);
        final CheckBox checkBox = (CheckBox) findViewById(R.id.enable_smooth_scroll);
        final Spinner spinner = (Spinner) findViewById(R.id.spinner);

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
        final Spinner spinner = (Spinner) findViewById(R.id.spinner);
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

    abstract ConfigToggle[] createConfigToggles();

    private class ConfigViewHolder extends RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener {

        private CheckBox mCheckBox;

        private ConfigToggle mConfigToggle;

        public ConfigViewHolder(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
            mCheckBox.setOnCheckedChangeListener(this);
        }

        public void bind(ConfigToggle toggle) {
            mConfigToggle = toggle;
            mCheckBox.setText(toggle.getText());
            mCheckBox.setChecked(toggle.isChecked());
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (mConfigToggle != null) {
                mConfigToggle.onChange(isChecked);
            }
        }
    }


    public abstract static class ConfigToggle {

        private String mLabel;

        protected ConfigToggle(Context context, int labelId) {
            mLabel = context.getResources().getString(labelId);
        }

        public String getText() {
            return mLabel;
        }

        abstract public boolean isChecked();

        abstract public void onChange(boolean newValue);
    }


    private RecyclerView.Adapter mConfigAdapter = new RecyclerView.Adapter<ConfigViewHolder>() {
        @Override
        public ConfigViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ConfigViewHolder(new CheckBox(parent.getContext()));
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
