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

package com.example.androidx.mediarouting.activities.systemrouting.source;

import android.content.Context;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.media.RouteDiscoveryPreference;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter2}. */
@RequiresApi(Build.VERSION_CODES.R)
public final class MediaRouter2SystemRoutesSource extends SystemRoutesSource {

    @NonNull
    private Context mContext;
    @NonNull
    private MediaRouter2 mMediaRouter2;

    @NonNull
    private final Map<String, MediaRoute2Info> mLastKnownRoutes = new HashMap<>();
    @NonNull
    private final MediaRouter2.RouteCallback mRouteCallback = new MediaRouter2.RouteCallback() {
        @Override
        public void onRoutesUpdated(@NonNull List<MediaRoute2Info> routes) {
            super.onRoutesUpdated(routes);

            Map<String, MediaRoute2Info> routesLookup = new HashMap<>();
            for (MediaRoute2Info route: routes) {
                if (!mLastKnownRoutes.containsKey(route.getId())) {
                    mOnRoutesChangedListener.onRouteAdded(createRouteItemFor(route));
                }
                routesLookup.put(route.getId(), route);
            }

            for (MediaRoute2Info route: mLastKnownRoutes.values()) {
                if (!routesLookup.containsKey(route.getId())) {
                    mOnRoutesChangedListener.onRouteRemoved(createRouteItemFor(route));
                }
            }

            mLastKnownRoutes.clear();
            mLastKnownRoutes.putAll(routesLookup);
        }
    };

    /** Returns a new instance. */
    @NonNull
    public static MediaRouter2SystemRoutesSource create(@NonNull Context context) {
        MediaRouter2 mediaRouter2 = MediaRouter2.getInstance(context);
        return new MediaRouter2SystemRoutesSource(context, mediaRouter2);
    }

    MediaRouter2SystemRoutesSource(@NonNull Context context,
            @NonNull MediaRouter2 mediaRouter2) {
        mContext = context;
        mMediaRouter2 = mediaRouter2;
    }

    @Override
    public void start() {
        RouteDiscoveryPreference routeDiscoveryPreference =
                new RouteDiscoveryPreference.Builder(
                        /* preferredFeatures= */ Collections.emptyList(),
                        /* activeScan= */ false)
                        .build();

        mMediaRouter2.registerRouteCallback(mContext.getMainExecutor(),
                mRouteCallback, routeDiscoveryPreference);
    }

    @Override
    public void stop() {
        mMediaRouter2.unregisterRouteCallback(mRouteCallback);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem.Builder(SystemRoutesSourceItem.ROUTE_SOURCE_MEDIA_ROUTER2)
                .build();
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (MediaRoute2Info routeInfo : mMediaRouter2.getRoutes()) {
            if (!routeInfo.isSystemRoute()) {
                continue;
            }

            if (!mLastKnownRoutes.containsKey(routeInfo.getId())) {
                mLastKnownRoutes.put(routeInfo.getId(), routeInfo);
            }

            out.add(createRouteItemFor(routeInfo));
        }

        return out;
    }

    @NonNull
    private static SystemRouteItem createRouteItemFor(@NonNull MediaRoute2Info routeInfo) {
        return new SystemRouteItem.Builder(routeInfo.getId())
                .setName(String.valueOf(routeInfo.getName()))
                .setDescription(String.valueOf(routeInfo.getDescription()))
                .build();
    }
}
