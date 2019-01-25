/*
 * Copyright 2018 The Android Open Source Project
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

package com.example.androidx.car;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.car.widget.CarToolbar;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo activity for PagedListView with some rows having span size > 1.
 *
 * This also tests that the scrollbar scrolls properly in this context.
 */
public class MixedColumnPagedListViewActivity extends Activity {

    private static final int ITEM_COUNT = 25;
    private static final int NUM_OF_COLUMNS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mixed_column_paged_list_view);

        CarToolbar toolbar = findViewById(R.id.car_toolbar);
        toolbar.setTitle(R.string.paged_list_view_title);
        toolbar.setNavigationIconOnClickListener(v -> finish());

        PagedListView pagedListView = findViewById(R.id.paged_list_view);
        pagedListView.setAdapter(new DemoAdapter(ITEM_COUNT));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, NUM_OF_COLUMNS);
        SpanSizeLookup spanSizeLookup = new SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (position < ITEM_COUNT / 2) {
                    return NUM_OF_COLUMNS - 1;
                }
                return 1;
            }
        };
        spanSizeLookup.setSpanGroupIndexCacheEnabled(true);
        gridLayoutManager.setSpanSizeLookup(spanSizeLookup);
        gridLayoutManager.setUsingSpansToEstimateScrollbarDimensions(true);
        pagedListView.getRecyclerView().setLayoutManager(gridLayoutManager);
    }

    /**
     * Adapter that populates a number of items for demo purposes.
     */
    public static class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

        private final List<String> mItems = new ArrayList<>();

        /**
         * Generates a string for item text.
         */
        public static String getItemText(int index) {
            return "Item " + index;
        }

        public DemoAdapter(int itemCount) {
            for (int i = 0; i < itemCount; i++) {
                mItems.add(getItemText(i));
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.mixed_column_paged_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        /**
         * ViewHolder for DemoAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.text);
            }
        }
    }
}

