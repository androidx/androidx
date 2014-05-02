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
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;
import com.example.android.supportv7.widget.decorator.DividerItemDecoration;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.supportv7.R;

/**
 * A sample activity that uses {@link android.support.v7.widget.LinearLayoutManager}.
 */
public class LinearLayoutManagerActivity extends Activity {

    private RecyclerView.LayoutManager mListLayoutManager;

    private ListWrapper mListWrapper;

    private RecyclerView mRecyclerView;

    private DividerItemDecoration mDividerItemDecoration;

    private ConfigToggle[] mConfigToggles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_linear_layout_manager);
        initConfig();
        initRecyclerView();
        initSpinner();
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mListLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mListLayoutManager);
        mRecyclerView.setAdapter(new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public SimpleStringAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final SimpleStringAdapter.ViewHolder vh = super
                        .onCreateViewHolder(parent, viewType);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int pos = vh.getPosition();
                        if (pos + 1 < getItemCount()) {
                            swap(pos, pos + 1);
                        }
                        notifyItemChanged(pos);
                    }
                });
                return vh;
            }
        });
        initListForLayoutManager();
    }

    private void initListForLayoutManager() {
        mListWrapper = new ListWrapper() {
            @Override
            public int getOrientation() {
                return lm().getOrientation();
            }

            @Override
            public void setOrientation(int orientation) {
                lm().setOrientation(orientation);
            }

            @Override
            public boolean getReverseLayout() {
                return lm().getReverseLayout();
            }

            @Override
            public void setReverseLayout(boolean newValue) {
                lm().setReverseLayout(newValue);
            }

            @Override
            public boolean getStackFromEnd() {
                return lm().getStackFromEnd();
            }

            @Override
            public void setStackFromEnd(boolean newValue) {
                lm().setStackFromEnd(newValue);
            }

            private LinearLayoutManager lm() {
                return (LinearLayoutManager) mListLayoutManager;
            }
        };
        if (mDividerItemDecoration != null) {
            mRecyclerView.removeItemDecoration(mDividerItemDecoration);
        }
        mDividerItemDecoration = new DividerItemDecoration(this, mListWrapper.getOrientation());
        mRecyclerView.addItemDecoration(mDividerItemDecoration);
    }

    private void initConfig() {
        RecyclerView configView = (RecyclerView) findViewById(R.id.config_recycler_view);
        initToggles();
        configView.setAdapter(mConfigAdapter);
        configView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,
                false));
        configView.setHasFixedSize(true);
    }

    private void initSpinner() {
        final CheckBox checkBox = (CheckBox)
                findViewById(R.id.enable_smooth_scroll);

        final Spinner spinner = (Spinner) findViewById(R.id.spinner);
        final EditText scrollOffset = (EditText) findViewById(R.id.scroll_offset);
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
                ((TextView) convertView).setText("" + position);
                return convertView;
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int offset = Integer.MIN_VALUE;
                String offsetString = scrollOffset.getText().toString();
                try {
                    offset = Integer.parseInt(offsetString);
                } catch (NumberFormatException ex) {

                }

                if (offset == Integer.MIN_VALUE) {
                    if (checkBox.isChecked()) {
                        mRecyclerView.smoothScrollToPosition(position);
                    } else {
                        mRecyclerView.scrollToPosition(position);
                    }
                } else {
                    // ignore offset until we add recycling list view with smooth scroll to offset
                    mRecyclerView.smoothScrollToPosition(position);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initToggles() {
        mConfigToggles = new ConfigToggle[]{
                new ConfigToggle(R.string.checkbox_orientation) {
                    @Override
                    public boolean isChecked() {
                        return mListWrapper.getOrientation() == LinearLayoutManager.HORIZONTAL;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mListWrapper.setOrientation(newValue ? LinearLayoutManager.HORIZONTAL
                                : LinearLayoutManager.VERTICAL);
                        if (mDividerItemDecoration != null) {
                            mDividerItemDecoration.setOrientation(mListWrapper.getOrientation());
                        }

                    }
                },
                new ConfigToggle(R.string.checkbox_reverse) {
                    @Override
                    public boolean isChecked() {
                        return mListWrapper.getReverseLayout();
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mListWrapper.setReverseLayout(newValue);
                    }
                },
                new ConfigToggle(R.string.checkbox_layout_dir) {
                    @Override
                    public boolean isChecked() {
                        return ViewCompat.getLayoutDirection(mRecyclerView) ==
                                ViewCompat.LAYOUT_DIRECTION_RTL;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        ViewCompat.setLayoutDirection(mRecyclerView, newValue ?
                                ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR);
                    }
                },
                new ConfigToggle(R.string.checkbox_stack_from_end) {
                    @Override
                    public boolean isChecked() {
                        return mListWrapper.getStackFromEnd();
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mListWrapper.setStackFromEnd(newValue);
                    }
                }
        };
    }

    private class ConfigViewHolder extends RecyclerView.ViewHolder
            implements CompoundButton.OnCheckedChangeListener {

        private CheckBox mCheckBox;

        private ConfigToggle mConfigToggle;

        public ConfigViewHolder(View itemView) {
            super(itemView);
            mCheckBox = (CheckBox) itemView;
            mCheckBox.setOnCheckedChangeListener(this);
        }

        public void render(ConfigToggle toggle) {
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


    private abstract class ConfigToggle {

        private String mLabel;

        protected ConfigToggle(int labelId) {
            mLabel = getResources().getString(labelId);
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
            holder.render(toggle);
        }

        @Override
        public int getItemCount() {
            return mConfigToggles.length;
        }
    };


    /**
     * To avoid adding interfaces to LayoutManager for the demo, we use this wrapper class to
     * call different LayoutManagers
     */
    private static interface ListWrapper {

        int getOrientation();

        void setOrientation(int orientation);

        boolean getReverseLayout();

        void setReverseLayout(boolean newValue);

        boolean getStackFromEnd();

        void setStackFromEnd(boolean newValue);
    }


}
