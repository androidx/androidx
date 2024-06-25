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
import android.text.TextUtils;

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
    private final MediaRouter.Callback mCallback =
            new MediaRouter.SimpleCallback() {
                @Override
                public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
                    mOnRoutesChangedListener.run();
                }

                @Override
                public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
                    mOnRoutesChangedListener.run();
                }

                @Override
                public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
                    mOnRoutesChangedListener.run();
                }

                @Override
                public void onRouteUnselected(
                        MediaRouter router, int type, MediaRouter.RouteInfo info) {
                    mOnRoutesChangedListener.run();
                }

                @Override
                public void onRouteSelected(
                        MediaRouter router, int type, MediaRouter.RouteInfo info) {
                    mOnRoutesChangedListener.run();
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

        MediaRouter.RouteInfo selectedRoute =
                mMediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO);
        for (int i = 0; i < count; i++) {
            MediaRouter.RouteInfo info = mMediaRouter.getRouteAt(i);
            if (info.getPlaybackType() == MediaRouter.RouteInfo.PLAYBACK_TYPE_LOCAL) {
                // We are only interested in system routes.
                out.add(createRouteItemFor(info, /* isSelected= */ selectedRoute == info));
            }
        }

        return out;
    }

    @Override
    public boolean select(@NonNull SystemRouteItem item) {
        int routeCount = mMediaRouter.getRouteCount();
        for (int i = 0; i < routeCount; i++) {
            MediaRouter.RouteInfo route = mMediaRouter.getRouteAt(i);
            if (TextUtils.equals(route.getName().toString(), item.mId)) {
                mMediaRouter.selectRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO, route);
                return true;
            }
        }
        return false;
    }

    @NonNull
    private SystemRouteItem createRouteItemFor(
            @NonNull MediaRouter.RouteInfo routeInfo, boolean isSelected) {
        SystemRouteItem.Builder builder =
                new SystemRouteItem.Builder(getSourceId(), /* id= */ routeInfo.getName().toString())
                        .setName(routeInfo.getName().toString());
        builder.setSelectionSupportState(
                isSelected
                        ? SystemRouteItem.SelectionSupportState.RESELECTABLE
                        : SystemRouteItem.SelectionSupportState.SELECTABLE);
        CharSequence description = routeInfo.getDescription();
        if (description != null) {
            builder.setDescription(String.valueOf(description));
        }

        return builder.build();
    }
}
