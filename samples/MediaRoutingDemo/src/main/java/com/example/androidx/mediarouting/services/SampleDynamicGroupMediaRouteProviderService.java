/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.androidx.mediarouting.services;

import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaRouteProvider;
import androidx.mediarouter.media.MediaRouteProviderService;

import com.example.androidx.mediarouting.providers.SampleDynamicGroupMediaRouteProvider;

/**
 * Demonstrates how to register a custom media route provider service
 * using the support library.
 *
 * @see SampleDynamicGroupMediaRouteProvider
 */
public class SampleDynamicGroupMediaRouteProviderService extends MediaRouteProviderService {

    public static final String ACTION_BIND_LOCAL = "com.example.androidx.mediarouting.BIND_LOCAL";

    private SampleDynamicGroupMediaRouteProvider mDynamicGroupMediaRouteProvider;

    @NonNull
    @Override
    public MediaRouteProvider onCreateMediaRouteProvider() {
        mDynamicGroupMediaRouteProvider = new SampleDynamicGroupMediaRouteProvider(this);
        return mDynamicGroupMediaRouteProvider;
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        if (intent != null && ACTION_BIND_LOCAL.equals(intent.getAction())) {
            return new LocalBinder();
        } else {
            return super.onBind(intent);
        }
    }

    /** Reload all routes provided by this service. */
    public void reloadRoutes() {
        if (mDynamicGroupMediaRouteProvider != null) {
            mDynamicGroupMediaRouteProvider.reloadRoutes();
        }
    }

    /** Reload the flag for isDynamicRouteEnabled. */
    public void reloadDynamicRoutesEnabled() {
        if (mDynamicGroupMediaRouteProvider != null) {
            mDynamicGroupMediaRouteProvider.reloadDynamicRoutesEnabled();
        }
    }

    /**
     * Allows getting a direct reference to {@link SampleDynamicGroupMediaRouteProviderService} from
     * bindings within the same process.
     */
    public class LocalBinder extends Binder {
        @NonNull
        public SampleDynamicGroupMediaRouteProviderService getService() {
            return SampleDynamicGroupMediaRouteProviderService.this;
        }
    }
}
