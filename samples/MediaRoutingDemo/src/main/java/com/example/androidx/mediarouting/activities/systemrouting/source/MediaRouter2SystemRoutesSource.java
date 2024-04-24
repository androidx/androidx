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

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.media.RouteDiscoveryPreference;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter2}. */
@RequiresApi(Build.VERSION_CODES.R)
public final class MediaRouter2SystemRoutesSource extends SystemRoutesSource {

    @NonNull private final Context mContext;
    @NonNull private final MediaRouter2 mMediaRouter2;
    @NonNull private final Method mSuitabilityStatusMethod;
    @NonNull
    private final Map<String, MediaRoute2Info> mLastKnownRoutes = new HashMap<>();

    @NonNull
    private final MediaRouter2.RouteCallback mRouteCallback =
            new MediaRouter2.RouteCallback() {
                @Override
                public void onRoutesUpdated(@NonNull List<MediaRoute2Info> routes) {
                    Map<String, MediaRoute2Info> newRoutes = new HashMap<>();
                    boolean routesChanged = false;
                    for (MediaRoute2Info route : routes) {
                        routesChanged |= !mLastKnownRoutes.containsKey(route.getId());
                        newRoutes.put(route.getId(), route);
                    }

                    for (MediaRoute2Info route : mLastKnownRoutes.values()) {
                        routesChanged |= !newRoutes.containsKey(route.getId());
                    }

                    mLastKnownRoutes.clear();
                    mLastKnownRoutes.putAll(newRoutes);
                    if (routesChanged) {
                        mOnRoutesChangedListener.run();
                    }
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

        Method suitabilityStatusMethod = null;
        // TODO: b/336510942 - Remove reflection once these APIs are available in
        // androidx-platform-dev.
        try {
            suitabilityStatusMethod =
                    MediaRoute2Info.class.getDeclaredMethod("getSuitabilityStatus");
        } catch (NoSuchMethodException | IllegalAccessError e) {
        }
        mSuitabilityStatusMethod = suitabilityStatusMethod;
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
        return new SystemRoutesSourceItem(/* name= */ "MediaRouter2");
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
    private SystemRouteItem createRouteItemFor(@NonNull MediaRoute2Info routeInfo) {
        SystemRouteItem.Builder builder =
                new SystemRouteItem.Builder(routeInfo.getId())
                        .setName(String.valueOf(routeInfo.getName()))
                        .setDescription(String.valueOf(routeInfo.getDescription()));
        try {
            if (mSuitabilityStatusMethod != null) {
                // See b/336510942 for details on why reflection is needed.
                @SuppressLint("BanUncheckedReflection")
                int status = (Integer) mSuitabilityStatusMethod.invoke(routeInfo);
                builder.setSuitabilityStatus(getHumanReadableSuitabilityStatus(status));
                // TODO: b/319645714 - Populate wasTransferInitiatedBySelf. For that we need to
                // change the implementation of this class to use the routing controller instead
                // of a route callback.
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
        return builder.build();
    }

    @NonNull
    private String getHumanReadableSuitabilityStatus(int status) {
        switch (status) {
            case 0:
                return "SUITABLE_FOR_DEFAULT_TRANSFER";
            case 1:
                return "SUITABLE_FOR_MANUAL_TRANSFER";
            case 2:
                return "NOT_SUITABLE_FOR_TRANSFER";
            default:
                return "UNKNOWN(" + status + ")";
        }
    }
}
