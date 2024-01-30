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

/**
 * @link RecyclerView.Adapter} for showing system route sources and the routes discovered by each
 * source.
 */
class SystemRoutesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    private final AsyncListDiffer<SystemRoutesAdapterItem> mListDiffer =
            new AsyncListDiffer<>(this, new ItemCallback());

    public void setItems(@NonNull List<SystemRoutesAdapterItem> newItems) {
        mListDiffer.submitList(newItems);
    }

    @NonNull
    public List<SystemRoutesAdapterItem> getItems() {
        return mListDiffer.getCurrentList();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_system_route_header,
                    parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_system_route, parent,
                    false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SystemRoutesAdapterItem routeItem = getItems().get(position);
        if (routeItem instanceof SystemRoutesSourceItem && holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((SystemRoutesSourceItem) routeItem);
        } else if (routeItem instanceof SystemRouteItem && holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind((SystemRouteItem) routeItem);
        }
    }

    @Override
    public int getItemViewType(int position) {
        SystemRoutesAdapterItem routeItem = getItems().get(position);
        if (routeItem instanceof SystemRoutesSourceItem) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return getItems().size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatTextView mHeaderTitleTextView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            mHeaderTitleTextView = itemView.findViewById(R.id.header_title);
        }

        void bind(SystemRoutesSourceItem systemRoutesSourceItem) {
            mHeaderTitleTextView.setText(
                    SystemRouteUtils.getDescriptionForSource(systemRoutesSourceItem.getType()));
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {

        private final AppCompatTextView mRouteNameTextView;
        private final AppCompatTextView mRouteIdTextView;
        private final AppCompatTextView mRouteAddressTextView;
        private final AppCompatTextView mRouteDescriptionTextView;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);

            mRouteNameTextView = itemView.findViewById(R.id.route_name);
            mRouteIdTextView = itemView.findViewById(R.id.route_id);
            mRouteAddressTextView = itemView.findViewById(R.id.route_address);
            mRouteDescriptionTextView = itemView.findViewById(R.id.route_description);
        }

        void bind(SystemRouteItem systemRouteItem) {
            mRouteNameTextView.setText(systemRouteItem.getName());
            mRouteIdTextView.setText(systemRouteItem.getId());

            showViewIfNotNull(mRouteAddressTextView, systemRouteItem.getAddress());
            if (systemRouteItem.getAddress() != null) {
                mRouteAddressTextView.setText(systemRouteItem.getAddress());
            }

            showViewIfNotNull(mRouteDescriptionTextView, systemRouteItem.getDescription());
            if (systemRouteItem.getDescription() != null) {
                mRouteDescriptionTextView.setText(systemRouteItem.getDescription());
            }
        }
    }

    private static class ItemCallback extends DiffUtil.ItemCallback<SystemRoutesAdapterItem> {
        @Override
        public boolean areItemsTheSame(@NonNull SystemRoutesAdapterItem oldItem,
                @NonNull SystemRoutesAdapterItem newItem) {
            if (oldItem instanceof SystemRouteItem && newItem instanceof SystemRouteItem) {
                return ((SystemRouteItem) oldItem).getId().equals(
                        ((SystemRouteItem) newItem).getId());
            } else if (oldItem instanceof SystemRoutesSourceItem
                    && newItem instanceof SystemRoutesSourceItem) {
                return ((SystemRoutesSourceItem) oldItem).getType()
                        == ((SystemRoutesSourceItem) newItem).getType();
            } else {
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(@NonNull SystemRoutesAdapterItem oldItem,
                @NonNull SystemRoutesAdapterItem newItem) {
            if (oldItem instanceof SystemRouteItem && newItem instanceof SystemRouteItem) {
                return oldItem.equals(newItem);
            } else if (oldItem instanceof SystemRoutesSourceItem
                    && newItem instanceof SystemRoutesSourceItem) {
                return oldItem.equals(newItem);
            } else {
                return false;
            }
        }
    }

    private static <T, V extends View> void showViewIfNotNull(@NonNull V view, @Nullable T obj) {
        if (obj == null) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
        }
    }
}
