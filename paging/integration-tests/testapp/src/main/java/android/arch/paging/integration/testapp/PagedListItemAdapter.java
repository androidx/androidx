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

package android.arch.paging.integration.testapp;

import android.arch.paging.PagedListAdapter;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Sample NullPaddedList adapter, which uses a PagedListAdapterHelper.
 */
class PagedListItemAdapter extends PagedListAdapter<Item, RecyclerView.ViewHolder> {

    PagedListItemAdapter() {
        super(Item.DIFF_CALLBACK);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(
                new TextView(parent.getContext())) {};
        holder.itemView.setMinimumHeight(400);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Item item = getItem(position);
        if (item != null) {
            ((TextView) (holder.itemView)).setText(item.text);
            holder.itemView.setBackgroundColor(item.bgColor);
        } else {
            ((TextView) (holder.itemView)).setText(R.string.loading);
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }
}
