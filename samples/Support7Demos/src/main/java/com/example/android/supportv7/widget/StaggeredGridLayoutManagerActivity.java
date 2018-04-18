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

package com.example.android.supportv7.widget;

import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.android.supportv7.R;
import com.example.android.supportv7.widget.util.ConfigToggle;

public class StaggeredGridLayoutManagerActivity
        extends BaseLayoutManagerActivity<StaggeredGridLayoutManager> {

    private boolean mVertical = true;

    @Override
    protected StaggeredGridLayoutManager createLayoutManager() {
        if (mVertical) {
            return new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
        } else {
            return new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.HORIZONTAL);
        }
    }

    @Override
    protected ConfigToggle[] createConfigToggles() {
        return new ConfigToggle[] {
                new ConfigToggle(this, R.string.vertical) {
                    @Override
                    public boolean isChecked() {
                        return mVertical;
                    }

                    @Override
                    public void onChange(boolean newValue) {
                        if (mVertical == newValue) {
                            return;
                        }
                        mVertical = newValue;
                        mRecyclerView.setLayoutManager(createLayoutManager());
                    }
                }
        };
    }
}
