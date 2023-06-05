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
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;
import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteUtils;

import java.util.ArrayList;
import java.util.List;

/** Implements {@link SystemRoutesSource} using {@link MediaRouter}. */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
public final class MediaRouterSystemRoutesSource implements SystemRoutesSource {

    @NonNull
    private final MediaRouter mMediaRouter;

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

    @NonNull
    @Override
    public List<SystemRouteItem> fetchRoutes() {
        int count = mMediaRouter.getRouteCount();

        List<SystemRouteItem> out = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            MediaRouter.RouteInfo info = mMediaRouter.getRouteAt(i);

            if (!SystemRouteUtils.isSystemMediaRouterRoute(info)) {
                continue;
            }

            SystemRouteItem.Builder builder =
                    new SystemRouteItem.Builder(info.getName().toString() /* id */,
                            SystemRouteItem.ROUTE_SOURCE_MEDIA_ROUTER)
                            .setName(info.getName().toString());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                CharSequence description = Api18Impl.getDescription(info);

                if (description != null) {
                    builder.setDescription(String.valueOf(description));
                }
            }

            out.add(builder.build());
        }

        return out;
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    static class Api18Impl {
        private Api18Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        @Nullable
        static CharSequence getDescription(MediaRouter.RouteInfo routeInfo) {
            return routeInfo.getDescription();
        }

    }
}
