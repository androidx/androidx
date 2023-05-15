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
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(Build.VERSION_CODES.R)
class MediaRouter2SystemRoutesSource implements SystemRoutesSource {

    @NonNull
    private MediaRouter2 mMediaRouter2;

    @NonNull
    static MediaRouter2SystemRoutesSource create(@NonNull Context context) {
        MediaRouter2 mediaRouter2 = MediaRouter2.getInstance(context);
        return new MediaRouter2SystemRoutesSource(mediaRouter2);
    }

    MediaRouter2SystemRoutesSource(@NonNull MediaRouter2 mediaRouter2) {
        mMediaRouter2 = mediaRouter2;
    }

    @NonNull
    @Override
    public List<SystemRouteItem> fetchRoutes() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (MediaRoute2Info routeInfo : mMediaRouter2.getRoutes()) {
            if (!routeInfo.isSystemRoute()) {
                continue;
            }

            out.add(new SystemRouteItem.Builder(routeInfo.getId(),
                    SystemRouteItem.ROUTE_SOURCE_MEDIA_ROUTER2)
                    .setName(String.valueOf(routeInfo.getName()))
                    .setDescription(String.valueOf(routeInfo.getDescription()))
                    .build());
        }

        return out;
    }
}
