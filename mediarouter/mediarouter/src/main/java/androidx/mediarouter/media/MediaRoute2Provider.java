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

import android.content.Context;
import android.media.MediaRoute2Info;
import android.media.MediaRouter2;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Provides routes and RouteController by using MediaRouter2.
 * This provider is added only when {@link MediaRouter#enableTransfer()} is called.
 */
@RequiresApi(30)
@SuppressWarnings({"unused", "ClassCanBeStatic"})
class MediaRoute2Provider extends MediaRouteProvider {
    private final MediaRouter2 mMediaRouter2Fwk;
    private final MediaRouter2.RouteCallback mMr2RouteCallbackFwk;
    private final Handler mHandler;
    private final Executor mHandlerExecutor;
    private final List<MediaRoute2Info> mRoutes = new ArrayList<>();

    private boolean mShouldRegisterCallback;

    MediaRoute2Provider(@NonNull Context context, @NonNull MediaRouter2 mediaRouter2Fwk) {
        super(context);
        mMediaRouter2Fwk = mediaRouter2Fwk;
        mMr2RouteCallbackFwk = new RouteCallback();
        mHandler = new Handler();
        mHandlerExecutor = mHandler::post;
    }

    @Override
    public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
        updateMr2FwkCallbacks();
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId) {
        return new MediaRoute2Controller(routeId, null);
    }

    @Nullable
    @Override
    public RouteController onCreateRouteController(@NonNull String routeId,
            @NonNull String routeGroupId) {
        return new MediaRoute2Controller(routeId, routeGroupId);
    }

    @Nullable
    @Override
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId) {
        return new DynamicMediaRoute2Controller(initialMemberRouteId);
    }

    void setShouldRegisterCallback(boolean shouldRegisterCallback) {
        mShouldRegisterCallback = shouldRegisterCallback;
        updateMr2FwkCallbacks();
    }

    // TODO: Also Handle TransferCallback here
    private void updateMr2FwkCallbacks() {
        if (mShouldRegisterCallback) {
            mMediaRouter2Fwk.registerRouteCallback(mHandlerExecutor, mMr2RouteCallbackFwk,
                    MediaRouter2Utils.toDiscoveryPreference(getDiscoveryRequest()));
        } else {
            mMediaRouter2Fwk.unregisterRouteCallback(mMr2RouteCallbackFwk);
        }
    }

    protected void refreshRoutes() {
        // TODO: 1. Exclude the system routes.
        //       2. Find if there was any changes from mRoutes. If none, return.
        //       3. Convert the [MediaRoute2Info]s to [RouteInfo]s
        //       4. Call setDescriptor()
    }

    private class RouteCallback extends MediaRouter2.RouteCallback {
        RouteCallback() {}

        @Override
        public void onRoutesAdded(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }

        @Override
        public void onRoutesRemoved(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }

        @Override
        public void onRoutesChanged(@NonNull List<MediaRoute2Info> routes) {
            refreshRoutes();
        }
    }

    // TODO: Implement this class by overriding every public method in RouteController.
    private class MediaRoute2Controller extends RouteController {
        final String mRouteId;
        final String mRouteGroupId;

        MediaRoute2Controller(@NonNull String routeId, @Nullable String routeGroupId) {
            mRouteId = routeId;
            mRouteGroupId = routeGroupId;
        }
    }

    // TODO: Implement this class by overriding every public method in DynamicGroupRouteController.
    private class DynamicMediaRoute2Controller extends DynamicGroupRouteController {
        final String mInitialMemberRouteId;

        DynamicMediaRoute2Controller(@NonNull String initialMemberRouteId) {
            mInitialMemberRouteId = initialMemberRouteId;
        }

        @Override
        public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {}

        @Override
        public void onAddMemberRoute(@NonNull String routeId) {}

        @Override
        public void onRemoveMemberRoute(String routeId) {}
    }
}
