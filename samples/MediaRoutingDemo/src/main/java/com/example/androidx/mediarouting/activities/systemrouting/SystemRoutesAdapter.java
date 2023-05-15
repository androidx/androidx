/*
 * Copyright 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting.activities.systemrouting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.mediarouting.R;

import java.util.List;

class SystemRoutesAdapter extends RecyclerView.Adapter<SystemRoutesAdapter.ViewHolder> {

    private final AsyncListDiffer<SystemRouteItem> mListDiffer =
            new AsyncListDiffer<>(this, new ItemCallback());

    public void setItems(@NonNull List<SystemRouteItem> newItems) {
        mListDiffer.submitList(newItems);
    }

    @NonNull
    public List<SystemRouteItem> getItems() {
        return mListDiffer.getCurrentList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_system_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SystemRouteItem route = getItems().get(position);

        holder.mRouteSourceTextView.setText(
                SystemRouteUtils.getDescriptionForSource(route.getType()));
        holder.mRouteNameTextView.setText(route.getName());
        holder.mRouteIdTextView.setText(route.getId());

        showViewIfNotNull(holder.mRouteAddressTextView, route.getAddress());
        holder.mRouteAddressTextView.setText(route.getAddress());

        showViewIfNotNull(holder.mRouteDescriptionTextView, route.getDescription());
        holder.mRouteDescriptionTextView.setText(route.getDescription());
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position,
            @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        for (Object rawPayload : payloads) {
            if (!(rawPayload instanceof Payload)) {
                continue;
            }

            Payload payload = (Payload) rawPayload;

            if (payload.mName != null) {
                holder.mRouteNameTextView.setText(payload.mName);
            }

            showViewIfNotNull(holder.mRouteAddressTextView, payload.mAddress);
            if (payload.mAddress != null) {
                holder.mRouteAddressTextView.setText(payload.mAddress);
            }

            showViewIfNotNull(holder.mRouteDescriptionTextView, payload.mDescription);
            if (payload.mDescription != null) {
                holder.mRouteDescriptionTextView.setText(payload.mDescription);
            }
        }
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        final AppCompatTextView mRouteSourceTextView;
        final AppCompatTextView mRouteNameTextView;
        final AppCompatTextView mRouteIdTextView;
        final AppCompatTextView mRouteAddressTextView;
        final AppCompatTextView mRouteDescriptionTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            mRouteSourceTextView = itemView.findViewById(R.id.route_source);
            mRouteNameTextView = itemView.findViewById(R.id.route_name);
            mRouteIdTextView = itemView.findViewById(R.id.route_id);
            mRouteAddressTextView = itemView.findViewById(R.id.route_address);
            mRouteDescriptionTextView = itemView.findViewById(R.id.route_description);
        }
    }

    private static class ItemCallback extends DiffUtil.ItemCallback<SystemRouteItem> {

        @Override
        public boolean areItemsTheSame(@NonNull SystemRouteItem oldItem,
                @NonNull SystemRouteItem newItem) {
            return oldItem.getId().equals(newItem.getId())
                    && oldItem.getType() == newItem.getType();
        }

        @Override
        public boolean areContentsTheSame(@NonNull SystemRouteItem oldItem,
                @NonNull SystemRouteItem newItem) {
            return oldItem.equals(newItem);
        }

        @Nullable
        @Override
        public Payload getChangePayload(@NonNull SystemRouteItem oldItem,
                @NonNull SystemRouteItem newItem) {
            return new Payload(takeIfChanged(oldItem.getName(), newItem.getName()),
                    takeIfChanged(oldItem.getAddress(), newItem.getAddress()),
                    takeIfChanged(oldItem.getDescription(), newItem.getDescription()));
        }
    }

    private static class Payload {

        @Nullable
        final String mName;

        @Nullable
        final String mAddress;

        @Nullable
        final String mDescription;


        Payload(@Nullable String name, @Nullable String address,
                @Nullable String description) {
            mName = name;
            mAddress = address;
            mDescription = description;
        }
    }

    private static <T, V extends View> void showViewIfNotNull(@NonNull V view, @Nullable T obj) {
        if (obj == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    private static <T> T takeIfChanged(@Nullable T oldObj, @Nullable T newObj) {
        if (oldObj == null && newObj == null) {
            return null;
        }

        if (oldObj == null || newObj == null) {
            return newObj;
        }

        if (oldObj.equals(newObj)) {
            return null;
        }

        return newObj;
    }
}
