/*
 * Copyright 2017 The Android Open Source Project
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

package com.example.android.supportv7.widget.selection.simple;

import static androidx.core.util.Preconditions.checkArgument;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

final class SimpleSelectionDemoAdapter extends RecyclerView.Adapter<DemoHolder> {

    private static final String TAG = "SelectionDemos";
    private final Context mContext;
    private final ItemKeyProvider<Long> mKeyProvider;

    // This should be replaced at "bind" time with a real test that
    // asks SelectionTracker.
    private SelectionTest mSelTest;

    SimpleSelectionDemoAdapter(Context context, ItemKeyProvider<Long> keyProvider) {
        mContext = context;
        mKeyProvider = keyProvider;
        mSelTest = new SelectionTest() {
            @Override
            public boolean isSelected(Long id) {
                throw new IllegalStateException(
                        "Adapter must be initialized with SelectionTracker.");
            }
        };
    }

    // Glue together SelectionTracker and the adapter.
    public void bindSelectionHelper(final SelectionTracker<Long> selectionTracker) {
        checkArgument(selectionTracker != null);
        mSelTest = new SelectionTest() {
            @Override
            public boolean isSelected(Long id) {
                return selectionTracker.isSelected(id);
            }
        };
    }

    void loadData() {
        onDataReady();
    }

    private void onDataReady() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return Cheeses.sCheeseStrings.length;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(DemoHolder holder, int position) {
        Long key = getItemId(position);
        Log.v(TAG, "Just before rendering item position=" + position + ", key=" + key);
        holder.update(Cheeses.sCheeseStrings[position], mSelTest.isSelected(key));
    }

    @Override
    public DemoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout layout = inflateLayout(mContext, parent, R.layout.selection_demo_list_item);
        return new DemoHolder(layout);
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")  // Convenience to avoid clumsy cast.
    private static <V extends View> V inflateLayout(
            Context context, ViewGroup parent, int layout) {

        return (V) LayoutInflater.from(context).inflate(layout, parent, false);
    }

    private interface SelectionTest {
        boolean isSelected(Long id);
    }
}
