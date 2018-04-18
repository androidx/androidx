
/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.supportv7.Cheeses;
import com.example.android.supportv7.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A sample Stable Id RecyclerView activity.
 *
 * Stable Ids guarantee that items that remain onscreen due to a data set change are rebound to the
 * same views. This sample visually identifies views (independent from their data) to demonstrate
 * how RecyclerView does this rebinding. In addition, you can observe how RecyclerView recycles
 * items in response to a scroll or fling.
 *
 * Tapping an item will send it's data to the top of the list. RecyclerView will detect that the
 * data with that stable ID has moved position, and move the View accordingly, even though the only
 * signal sent to the RecyclerView was notifyDataSetChanged().
 *
 * Compared to DiffUtil or SortedList, stable Ids are often easier to use, but are less efficient,
 * and can only look at attached views to try and form reasonable animations for an update. Note
 * that notifyDataSetChanged() will still always cause every visible item to be rebound, since
 * RecyclerView isn't told what *inside the item* may have changed.
 *
 * It is suggested instead to use DiffUtil or SortedList to dispatch minimal updates from your
 * data set to the RecyclerView. SortedList allows you to express operations like moving or removing
 * an item very efficiently, without RecyclerView needing to find and compare the state of each
 * child before and after an operation. DiffUtil can compute minimal alterations (such as inserts
 * and moves) from lists that you pass it - useful if your data source or server doesn't provide
 * delta updates. Both DiffUtil and SortedList allow you to avoid rebinding each item when a small
 * change occurs.
 */
public class StableIdActivity extends BaseLayoutManagerActivity<LinearLayoutManager> {
    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
    }

    @Override
    protected RecyclerView.Adapter createAdapter() {
        return new StableIdAdapter(Cheeses.sCheeseStrings);
    }

    @Override
    protected void onRecyclerViewInit(RecyclerView recyclerView) {
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, mLayoutManager.getOrientation()));
    }

    static class StableIdAdapter extends RecyclerView.Adapter<StableIdAdapter.ViewHolder> {
        List<Pair<Integer, String>> mData = new ArrayList<>();

        public static class ViewHolder extends RecyclerView.ViewHolder {
            static int sHolderNumber = 0;

            private final TextView mHolderNumberView;
            private final TextView mDataView;

            public ViewHolder(View itemView) {
                super(itemView);
                mHolderNumberView = (TextView) itemView.findViewById(R.id.holder_number_text);
                mDataView = (TextView) itemView.findViewById(R.id.data_text);

                // Just for demonstration, we visually uniquely identify which ViewHolder is which,
                // so rebinding / moving due to stable IDs can be observed:
                mHolderNumberView.setText("View Nr: " + sHolderNumber++);
            }
        }

        StableIdAdapter(String[] strings) {
            // comment out this line to dispatch updates without using stable IDs -
            // this prevents RecyclerView from knowing what changed, so animations don't run
            setHasStableIds(true);


            for (int i = 0; i < 20; i++) {
                mData.add(new Pair<>(500 + i, strings[i]));
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final ViewHolder viewHolder = new ViewHolder(
                    inflater.inflate(R.layout.stable_id_item, parent, false));

            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int pos = viewHolder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // swap item to top, and notify data set changed
                        Pair<Integer, String> d = mData.remove(pos);
                        mData.add(0, d);

                        notifyDataSetChanged();
                    }
                }
            });
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mDataView.setText(mData.get(position).second);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).first;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }
}
