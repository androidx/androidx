/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.support.wear.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.example.android.support.wear.R;

/**
 * Main activity for the WearableRecyclerView demo.
 */
public class SimpleWearableRecyclerViewDemo extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wrv_demo);

        WearableRecyclerView wrv = findViewById(R.id.wrv_container);

        wrv.setLayoutManager(new WearableLinearLayoutManager(this));
        wrv.setAdapter(new DemoAdapter());
        wrv.setCircularScrollingGestureEnabled(true);
        wrv.setEdgeItemsCenteringEnabled(true);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView mView;
        ViewHolder(TextView itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    private class DemoAdapter extends WearableRecyclerView.Adapter<ViewHolder> {
        private static final int ITEM_COUNT = 100;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView view = new TextView(parent.getContext());
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mView.setText("Holder at position " + position);
            holder.mView.setTag(position);
        }


        @Override
        public int getItemCount() {
            return ITEM_COUNT;
        }
    }
}
