/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.mediarouter.media;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaRoute2ProviderService;
import android.media.RouteDiscoveryPreference;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRoute2ProviderServiceStub extends MediaRoute2ProviderService {
    private static final String TAG = "MR2ProviderService";

    @SuppressLint("InlinedApi")
    public static final String SERVICE_INTERFACE = MediaRoute2ProviderService.SERVICE_INTERFACE;

    @Override
    public void attachBaseContext(Context context) {
        super.attachBaseContext(context);
    }

    @Override
    public void onSetRouteVolume(long requestId, @NonNull String routeId, int volume) {

    }

    @Override
    public void onSetSessionVolume(long requestId, @NonNull String sessionId, int volume) {

    }

    @Override
    public void onCreateSession(long requestId, @NonNull String packageName,
            @NonNull String routeId, @Nullable Bundle sessionHints) {

    }

    @Override
    public void onReleaseSession(long requestId, @NonNull String sessionId) {

    }

    @Override
    public void onSelectRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {

    }

    @Override
    public void onDeselectRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {

    }

    @Override
    public void onTransferToRoute(long requestId, @NonNull String sessionId,
            @NonNull String routeId) {

    }

    @Override
    public void onDiscoveryPreferenceChanged(@NonNull RouteDiscoveryPreference preference) {
    }

    public void publishRoutes(@Nullable MediaRouteProviderDescriptor descriptor) {
        List<MediaRouteDescriptor> routeDescriptors =
                (descriptor == null) ? Collections.emptyList() : descriptor.getRoutes();
        notifyRoutes(routeDescriptors.stream().map(MediaRouter2Utils::toFwkMediaRoute2Info)
                .collect(Collectors.toList()));
    }
}
