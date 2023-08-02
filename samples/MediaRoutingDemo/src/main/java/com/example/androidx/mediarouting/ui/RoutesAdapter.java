/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidx.mediarouting.R;
import com.example.androidx.mediarouting.data.RouteItem;

import java.util.List;

/** {@link RecyclerView.Adapter} for showing routes in the settings screen */
public final class RoutesAdapter extends RecyclerView.Adapter<RoutesAdapter.RouteViewHolder> {

    private List<RouteItem> mRouteItems;
    private final RouteItemListener mRouteItemListener;

    public RoutesAdapter(
            @NonNull List<RouteItem> routeItems, @NonNull RouteItemListener routeItemListener) {
        this.mRouteItems = routeItems;
        this.mRouteItemListener = routeItemListener;
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.route_item, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        holder.bind(mRouteItems.get(position), mRouteItemListener);
    }

    @Override
    public int getItemCount() {
        return mRouteItems.size();
    }

    /**
     * Updates the list of routes with new list.
     *
     * @param routeItems to replace the existing list in the adapter.
     */
    public void updateRoutes(@NonNull List<RouteItem> routeItems) {
        this.mRouteItems = routeItems;
        notifyDataSetChanged();
    }

    /** ViewHolder for the route item */
    static class RouteViewHolder extends RecyclerView.ViewHolder {

        private final TextView mNameTextView;
        private final TextView mDescriptionTextView;
        private final ImageButton mEditButton;
        private final ImageButton mDeleteButton;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            mNameTextView = itemView.findViewById(R.id.name_textview);
            mDescriptionTextView = itemView.findViewById(R.id.description_textview);
            mEditButton = itemView.findViewById(R.id.edit_button);
            mDeleteButton = itemView.findViewById(R.id.delete_button);
        }

        void bind(RouteItem routeItem, RouteItemListener routeItemListener) {
            mNameTextView.setText(routeItem.getName());
            mDescriptionTextView.setText(routeItem.getDescription());

            mEditButton.setOnClickListener(
                    view -> {
                        routeItemListener.onRouteEditClick(routeItem.getId());
                    });

            mDeleteButton.setOnClickListener(
                    view -> routeItemListener.onRouteDeleteClick(routeItem.getId()));
        }
    }

    /** Listener to pass action callbacks to the route item viewholder */
    public interface RouteItemListener {
        /**
         * Called when route's edit button is clicked
         *
         * @param routeId whose edit button is clicked
         */
        void onRouteEditClick(@NonNull String routeId);

        /**
         * Called when route's delete button is clicked
         *
         * @param routeId whose delete button is clicked
         */
        void onRouteDeleteClick(@NonNull String routeId);
    }
}
