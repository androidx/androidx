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

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.util.ConfigToggle;

/**
 * A sample activity that uses {@link LinearLayoutManager}.
 */
public class LinearLayoutManagerActivity extends BaseLayoutManagerActivity<LinearLayoutManager> {
    private DividerItemDecoration mDividerItemDecoration;
    private LinearSnapHelper mLinearSnapHelper;
    private boolean mSnapHelperAttached;

    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    protected void onRecyclerViewInit(RecyclerView recyclerView) {
        mDividerItemDecoration = new DividerItemDecoration(this, mLayoutManager.getOrientation());
        recyclerView.addItemDecoration(mDividerItemDecoration);
        mLinearSnapHelper = new LinearSnapHelper();
    }

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
                        if (mDividerItemDecoration != null) {
                            mDividerItemDecoration.setOrientation(mLayoutManager.getOrientation());
                        }

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
                },
                new ConfigToggle(this, R.string.checkbox_snap) {
                    @Override
                    public boolean isChecked() {
                        return mSnapHelperAttached;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        mLinearSnapHelper.attachToRecyclerView(newValue ? mRecyclerView : null);
                        mSnapHelperAttached = newValue;
                    }
                }
        };
    }
}
