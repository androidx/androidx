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

package androidx.paging.integration.testapp.custom;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.PagedListAdapter;
import androidx.paging.integration.testapp.R;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Sample PagedList item Adapter, which uses a AsyncPagedListDiffer.
 */
class PagedListItemAdapter extends PagedListAdapter<Item, RecyclerView.ViewHolder> {

    PagedListItemAdapter() {
        super(Item.DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(
                new TextView(parent.getContext())) {};
        holder.itemView.setMinimumHeight(400);
        holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
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
