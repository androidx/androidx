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

import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.adapter.SimpleStringAdapter;
import com.example.android.supportv7.widget.util.ConfigToggle;

/**
 * A sample Activity to demonstrate capabilities of {@link GridLayoutManager}.
 */
public class GridLayoutManagerActivity extends BaseLayoutManagerActivity<GridLayoutManager> {
    SimpleStringAdapter mAdapter;
    @Override
    protected GridLayoutManager createLayoutManager() {
        GridLayoutManager lm = new GridLayoutManager(this, 3);
        lm.setReverseLayout(true);
        lm.setSpanSizeLookup(mSpanSizeLookup);
        return lm;
    }

    GridLayoutManager.SpanSizeLookup mSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            String item = mAdapter.getValueAt(position);
            return 1 + (Math.abs(item.hashCode()) % mLayoutManager.getSpanCount());
        }
    };

    @Override
    protected ConfigToggle[] createConfigToggles() {
        return new ConfigToggle[]{
                new ConfigToggle(this, R.string.checkbox_orientation) {
                    @Override
                    public boolean isChecked() {
                        return mLayoutManager.getOrientation() == LinearLayoutManager.HORIZONTAL;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mLayoutManager.setOrientation(newValue ? LinearLayoutManager.HORIZONTAL
                                : LinearLayoutManager.VERTICAL);
                    }
                },
                new ConfigToggle(this, R.string.checkbox_reverse) {
                    @Override
                    public boolean isChecked() {
                        return mLayoutManager.getReverseLayout();
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mLayoutManager.setReverseLayout(newValue);
                    }
                },
                new ConfigToggle(this, R.string.checkbox_layout_dir) {
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
                new ConfigToggle(this, R.string.checkbox_stack_from_end) {
                    @Override
                    public boolean isChecked() {
                        return mLayoutManager.getStackFromEnd();
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mLayoutManager.setStackFromEnd(newValue);
                    }
                }
        };
    }

    @Override
    protected void scrollToPositionWithOffset(boolean smooth, int position, int offset) {
        if (smooth) {
            super.scrollToPositionWithOffset(smooth, position, offset);
        } else {
            mLayoutManager.scrollToPositionWithOffset(position, offset);
        }
    }

    @Override
    protected RecyclerView.Adapter createAdapter() {
        mAdapter = new SimpleStringAdapter(this, Cheeses.sCheeseStrings) {
            @Override
            public SimpleStringAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                final SimpleStringAdapter.ViewHolder vh = super
                        .onCreateViewHolder(parent, viewType);
                vh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final int pos = vh.getAdapterPosition();
                        if (pos == RecyclerView.NO_POSITION) {
                            return;
                        }
                        if (pos + 1 < getItemCount()) {
                            swap(pos, pos + 1);
                        }
                    }
                });
                return vh;
            }
        };
        return mAdapter;
    }
}
