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

import android.media.MediaRouter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Utils for {@link SystemRouteItem}.
 */
public final class SystemRouteUtils {

    private SystemRouteUtils() {
        // Private on purpose.
    }

    /**
     * Checks whether {@link MediaRouter.RouteInfo} is a system route or not.
     */
    @RequiresApi(16)
    public static boolean isSystemMediaRouterRoute(@NonNull MediaRouter.RouteInfo routeInfo) {
        return routeInfo.getClass() == MediaRouter.RouteInfo.class;
    }

    /**
     * Converts {@link SystemRouteItem.Type} to a human-readable string.
     */
    @NonNull
    public static String getDescriptionForSource(@SystemRouteItem.Type int type) {
        switch (type) {
            case SystemRouteItem.ROUTE_SOURCE_MEDIA_ROUTER:
                return "Media Router";
            case SystemRouteItem.ROUTE_SOURCE_MEDIA_ROUTER2:
                return "Media Router 2";
            case SystemRouteItem.ROUTE_SOURCE_BLUETOOTH_MANAGER:
                return "Bluetooth Manager";
            case SystemRouteItem.ROUTE_SOURCE_ANDROIDX_ROUTER:
                return "Androidx Router";
            case SystemRouteItem.ROUTE_SOURCE_AUDIO_MANAGER:
                return "Audio Manager";
            default:
                throw new IllegalArgumentException("Unknown system route type: " + type);
        }
    }

}
