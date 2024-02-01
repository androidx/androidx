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
import android.media.MediaRouter;

import androidx.annotation.NonNull;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRoutesSourceItem;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter}. */
public final class MediaRouterSystemRoutesSource extends SystemRoutesSource {

    @NonNull
    private final MediaRouter mMediaRouter;

    @NonNull
    private final MediaRouter.Callback mCallback = new MediaRouter.SimpleCallback() {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            super.onRouteAdded(router, info);
            mOnRoutesChangedListener.onRouteAdded(createRouteItemFor(info));
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            super.onRouteRemoved(router, info);
            mOnRoutesChangedListener.onRouteRemoved(createRouteItemFor(info));
        }
    };

    /** Returns a new instance. */
    @NonNull
    public static MediaRouterSystemRoutesSource create(@NonNull Context context) {
        MediaRouter mediaRouter =
                (MediaRouter) context.getSystemService(Context.MEDIA_ROUTER_SERVICE);
        return new MediaRouterSystemRoutesSource(mediaRouter);
    }

    MediaRouterSystemRoutesSource(@NonNull MediaRouter mediaRouter) {
        mMediaRouter = mediaRouter;
    }

    @Override
    public void start() {
        mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, mCallback);
    }

    @Override
    public void stop() {
        mMediaRouter.removeCallback(mCallback);
    }

    @NonNull
    @Override
    public SystemRoutesSourceItem getSourceItem() {
        return new SystemRoutesSourceItem(/* name= */ "Legacy MediaRouter");
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchSourceRouteItems() {
        int count = mMediaRouter.getRouteCount();

        List<SystemRouteItem> out = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            MediaRouter.RouteInfo info = mMediaRouter.getRouteAt(i);
            if (info.getPlaybackType() == MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL) {
                // We are only interested in system routes.
                out.add(createRouteItemFor(info));
            }
        }

        return out;
    }

    @NonNull
    private static SystemRouteItem createRouteItemFor(@NonNull MediaRouter.RouteInfo routeInfo) {
        SystemRouteItem.Builder builder =
                new SystemRouteItem.Builder(/* id= */ routeInfo.getName().toString())
                        .setName(routeInfo.getName().toString());

        CharSequence description = routeInfo.getDescription();
        if (description != null) {
            builder.setDescription(String.valueOf(description));
        }

        return builder.build();
    }
}
