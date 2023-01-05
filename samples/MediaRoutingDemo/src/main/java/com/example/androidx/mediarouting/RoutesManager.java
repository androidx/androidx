/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.example.androidx.mediarouting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.androidx.mediarouting.data.RouteItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Holds the data needed to control the provider for the routes dynamically. */
public final class RoutesManager {

    private boolean mDynamicRoutingEnabled;
    private final Map<String, RouteItem> mRouteItems;
    private static RoutesManager sInstance;

    private RoutesManager() {
        this.mDynamicRoutingEnabled = true;
        this.mRouteItems = new HashMap<>();
    }

    /** Singleton method. */
    @NonNull
    public static RoutesManager getInstance() {
        synchronized (RoutesManager.class) {
            if (sInstance == null) {
                sInstance = new RoutesManager();
            }
        }
        return sInstance;
    }

    @NonNull
    public List<RouteItem> getRouteItems() {
        return new ArrayList<>(mRouteItems.values());
    }

    public boolean isDynamicRoutingEnabled() {
        return mDynamicRoutingEnabled;
    }

    public void setDynamicRoutingEnabled(boolean dynamicRoutingEnabled) {
        this.mDynamicRoutingEnabled = dynamicRoutingEnabled;
    }

    /**
     * Deletes the route with the passed id.
     *
     * @param id of the route to be deleted.
     */
    public void deleteRouteWithId(@NonNull String id) {
        mRouteItems.remove(id);
    }

    /**
     * Gets the route with the passed id or null if not exists.
     *
     * @param id of the route to search for.
     * @return the route with the passed id or null if not exists.
     */
    @Nullable
    public RouteItem getRouteWithId(@NonNull String id) {
        return mRouteItems.get(id);
    }

    /** Adds the given route to the manager, replacing any existing route with a matching id. */
    public void addOrUpdateRoute(@NonNull RouteItem routeItem) {
        mRouteItems.put(routeItem.getId(), routeItem);
    }
}
