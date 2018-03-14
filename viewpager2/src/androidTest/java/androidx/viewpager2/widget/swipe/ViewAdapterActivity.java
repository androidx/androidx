/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.viewpager2.test.R;

public class ViewAdapterActivity extends BaseActivity {
    @Override
    protected void setAdapter() {
        mViewPager.setAdapter(new RecyclerView.Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                return new ViewHolder(
                        LayoutInflater.from(ViewAdapterActivity.this).inflate(
                                R.layout.item_test_layout, parent, false)) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                TextView view = (TextView) holder.itemView;
                view.setText(String.valueOf(position));
            }

            @Override
            public int getItemCount() {
                return mTotalPages;
            }
        });
    }

    @Override
    public void validateState() {
        // do nothing
    }

    @Override
    public void updatePage(int pageIx, int newValue) {
        throw new IllegalStateException("not implemented");
    }
}
