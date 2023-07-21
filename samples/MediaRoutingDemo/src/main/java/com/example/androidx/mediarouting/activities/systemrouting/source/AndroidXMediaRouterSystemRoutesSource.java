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

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter}. */
public final class AndroidXMediaRouterSystemRoutesSource extends SystemRoutesSource {

    @NonNull
    private final MediaRouter mMediaRouter;

    @NonNull
    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteAdded(@NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo route) {
            mOnRoutesChangedListener.onRouteAdded(createRouteItemFor(route));
        }

        @Override
        public void onRouteRemoved(@NonNull MediaRouter router,
                @NonNull MediaRouter.RouteInfo route) {
            mOnRoutesChangedListener.onRouteRemoved(createRouteItemFor(route));
        }
    };

    /** Returns a new instance. */
    @NonNull
    public static AndroidXMediaRouterSystemRoutesSource create(@NonNull Context context) {
        MediaRouter mediaRouter = MediaRouter.getInstance(context);
        return new AndroidXMediaRouterSystemRoutesSource(mediaRouter);
    }

    AndroidXMediaRouterSystemRoutesSource(@NonNull MediaRouter mediaRouter) {
        mMediaRouter = mediaRouter;
    }

    @Override
    public void start() {
        MediaRouteSelector selector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .build();

        mMediaRouter.addCallback(selector, mMediaRouterCallback);
    }

    @Override
    public void stop() {
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem.Builder(
                SystemRoutesSourceItem.ROUTE_SOURCE_ANDROIDX_ROUTER)
                .build();
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (MediaRouter.RouteInfo routeInfo : mMediaRouter.getRoutes()) {
            if (!routeInfo.isDefaultOrBluetooth()) {
                continue;
            }

            out.add(createRouteItemFor(routeInfo));
        }

        return out;
    }

    @NonNull
    private static SystemRouteItem createRouteItemFor(@NonNull MediaRouter.RouteInfo routeInfo) {
        SystemRouteItem.Builder builder = new SystemRouteItem.Builder(routeInfo.getId())
                .setName(routeInfo.getName());

        String description = routeInfo.getDescription();
        if (description != null) {
            builder.setDescription(description);
        }

        return builder.build();
    }
}
