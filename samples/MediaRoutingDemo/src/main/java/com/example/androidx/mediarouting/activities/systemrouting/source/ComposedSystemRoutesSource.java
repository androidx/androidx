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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.androidx.mediarouting.activities.systemrouting.SystemRouteItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Unified source for different System Routes providers.
 */
public class ComposedSystemRoutesSource implements SystemRoutesSource {

    private final List<SystemRoutesSource> mSystemRoutesSources;

    /**
     * Creates an instance of {@link ComposedSystemRoutesSource}.
     */
    @NonNull
    public static ComposedSystemRoutesSource create(@NonNull Context context) {
        List<SystemRoutesSource> systemRoutesSources = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            systemRoutesSources.add(MediaRouterSystemRoutesSource.create(context));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            systemRoutesSources.add(MediaRouter2SystemRoutesSource.create(context));
        }

        systemRoutesSources.add(AndroidXMediaRouterSystemRoutesSource.create(context));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
            systemRoutesSources.add(BluetoothManagerSystemRoutesSource.create(context));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            systemRoutesSources.add(AudioManagerSystemRoutesSource.create(context));
        }

        return new ComposedSystemRoutesSource(systemRoutesSources);
    }

    ComposedSystemRoutesSource(@NonNull List<SystemRoutesSource> routesSources) {
        mSystemRoutesSources = Collections.unmodifiableList(routesSources);
    }


    @NonNull
    @Override
    public List<SystemRouteItem> fetchRoutes() {
        List<SystemRouteItem> out = new ArrayList<>();

        for (SystemRoutesSource source : mSystemRoutesSources) {
            out.addAll(source.fetchRoutes());
        }

        return out;
    }
}
