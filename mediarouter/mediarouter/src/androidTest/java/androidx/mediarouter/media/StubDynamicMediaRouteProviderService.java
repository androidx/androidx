/*
 * Copyright 2024 The Android Open Source Project
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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.mediarouter.media.MediaRouteProvider.DynamicGroupRouteController.DynamicRouteDescriptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stub {@link MediaRouteProviderService} implementation that supports dynamic groups. */
public final class StubDynamicMediaRouteProviderService extends MediaRouteProviderService {
    private static final String TAG = "StubDynamicMrps";
    public static final String CATEGORY_DYNAMIC_PROVIDER_TEST =
            "androidx.mediarouter.media.CATEGORY_DYNAMIC_PROVIDER_TEST";

    public static final int VOLUME_INITIAL_VALUE = 8;
    public static final int VOLUME_MAX = 20;
    public static final String ROUTE_ID_GROUP = "route_id_group";
    public static final String ROUTE_NAME_GROUP = "Group route name";
    public static final String ROUTE_ID_1 = "route_id1";
    public static final String ROUTE_NAME_1 = "Sample Route 1";
    public static final boolean ROUTE_GROUPABLE_1 = true;
    public static final boolean ROUTE_TRANSFERABLE_1 = false;
    public static final String ROUTE_ID_2 = "route_id2";
    public static final String ROUTE_NAME_2 = "Sample Route 2";
    public static final boolean ROUTE_GROUPABLE_2 = true;
    public static final boolean ROUTE_TRANSFERABLE_2 = false;
    public static final String ROUTE_ID_3 = "route_id3";
    public static final String ROUTE_NAME_3 = "Sample Route 3";
    public static final boolean ROUTE_GROUPABLE_3 = false;
    public static final boolean ROUTE_TRANSFERABLE_3 = true;
    public static final List<IntentFilter> CONTROL_FILTERS_TEST = new ArrayList<>();
    public static final String SEND_CONTROL_REQUEST_KEY = "send_control_request_key";
    public static final String SEND_CONTROL_REQUEST_VALUE = "send_control_request_value";
    public static final int NEW_CLIENT_MIN_VERSION = 2;
    public static final int OLD_CLIENT_MAX_VERSION = 1;
    public static final Bundle SEND_CONTROL_REQUEST_RESULT = new Bundle();

    static {
        IntentFilter filter = new IntentFilter();
        filter.addCategory(CATEGORY_DYNAMIC_PROVIDER_TEST);
        CONTROL_FILTERS_TEST.add(filter);

        SEND_CONTROL_REQUEST_RESULT.putString(SEND_CONTROL_REQUEST_KEY, SEND_CONTROL_REQUEST_VALUE);
    }

    @Override
    public MediaRouteProvider onCreateMediaRouteProvider() {
        return new Provider(/* context= */ this);
    }

    private static final class Provider extends MediaRouteProvider {
        private final Map<String, MediaRouteDescriptor> mRoutes = new ArrayMap<>();
        private final Map<String, StubRouteController> mControllers = new ArrayMap<>();
        private MediaRouteDescriptor mGroupDescriptor;
        private final Set<String> mCurrentSelectedRouteIds = new HashSet<>();
        private boolean mCurrentlyScanning = false;
        @Nullable private DynamicGroupRouteController mGroupController;

        Provider(@NonNull Context context) {
            super(context);
            mGroupDescriptor =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_GROUP, ROUTE_NAME_GROUP)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolume(VOLUME_INITIAL_VALUE)
                            .build();
            MediaRouteDescriptor route1 =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_1, ROUTE_NAME_1)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolume(VOLUME_INITIAL_VALUE)
                            .setMinClientVersion(NEW_CLIENT_MIN_VERSION)
                            .setMaxClientVersion(Integer.MAX_VALUE)
                            .build();
            MediaRouteDescriptor route2 =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_2, ROUTE_NAME_2)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolume(VOLUME_INITIAL_VALUE)
                            .setMinClientVersion(NEW_CLIENT_MIN_VERSION)
                            .setMaxClientVersion(Integer.MAX_VALUE)
                            .build();
            MediaRouteDescriptor route3 =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_3, ROUTE_NAME_3)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolume(VOLUME_INITIAL_VALUE)
                            .build();
            mRoutes.put(route1.getId(), route1);
            mRoutes.put(route2.getId(), route2);
            mRoutes.put(route3.getId(), route3);
        }

        // MediaRouteProvider implementation.

        @Override
        public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
            Log.i(TAG, "onDiscoveryRequestChanged");
            mCurrentlyScanning =
                    request != null
                            && request.isActiveScan()
                            && request.getSelector()
                                    .hasControlCategory(CATEGORY_DYNAMIC_PROVIDER_TEST);
            publishProviderState();
        }

        @Override
        public RouteController onCreateRouteController(@NonNull String routeId) {
            Log.i(TAG, "onCreateRouteController with routeId = " + routeId);
            StubRouteController newController = new StubRouteController(routeId);
            mControllers.put(routeId, newController);
            return newController;
        }

        @Override
        public RouteController onCreateRouteController(
                @NonNull String routeId, @NonNull String routeGroupId) {
            Log.i(
                    TAG,
                    "onCreateRouteController with routeId = "
                            + routeId
                            + ", routeGroupId = "
                            + routeGroupId);
            StubRouteController newController = new StubRouteController(routeId);
            mControllers.put(routeId, newController);
            return newController;
        }

        @Nullable
        @Override
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId,
                @NonNull RouteControllerOptions routeControllerOptions) {
            Log.i(
                    TAG,
                    "onCreateDynamicGroupRouteController with initialMemberRouteId = "
                            + initialMemberRouteId);
            mGroupController = new StubDynamicRouteController();
            mCurrentSelectedRouteIds.add(initialMemberRouteId);
            return mGroupController;
        }

        // Internal methods.

        private void publishProviderState() {
            MediaRouteProviderDescriptor.Builder providerDescriptor =
                    new MediaRouteProviderDescriptor.Builder().setSupportsDynamicGroupRoute(true);

            List<MediaRouteDescriptor> publishedRoutes = new ArrayList<>();
            for (String key : mRoutes.keySet()) {
                MediaRouteDescriptor route = mRoutes.get(key);
                if (route == null) {
                    continue;
                }
                MediaRouteDescriptor routeForOldClient =
                        new MediaRouteDescriptor.Builder(route)
                                .setMinClientVersion(0)
                                .setMaxClientVersion(OLD_CLIENT_MAX_VERSION)
                                .build();
                if (ROUTE_ID_1.equals(route.getId())) {
                    // We first add the route for new client and then the route for old client.
                    publishedRoutes.add(route);
                    publishedRoutes.add(routeForOldClient);
                } else if (ROUTE_ID_2.equals(route.getId())) {
                    // We first add the route for old client and then the route for new client.
                    publishedRoutes.add(routeForOldClient);
                    publishedRoutes.add(route);
                } else {
                    publishedRoutes.add(route);
                }
            }

            if (mCurrentlyScanning) {
                providerDescriptor.addRoutes(publishedRoutes);
            }
            setDescriptor(providerDescriptor.build());
        }

        private Collection<DynamicRouteDescriptor> buildDynamicRouteDescriptors() {
            ArrayList<DynamicRouteDescriptor> result = new ArrayList<>();
            for (MediaRouteDescriptor route : mRoutes.values()) {
                DynamicRouteDescriptor dynamicDescriptor =
                        new DynamicRouteDescriptor.Builder(route)
                                .setSelectionState(
                                        mCurrentSelectedRouteIds.contains(route.getId())
                                                ? DynamicRouteDescriptor.SELECTED
                                                : DynamicRouteDescriptor.UNSELECTED)
                                .setIsGroupable(getIsGroupable(route.getId()))
                                .setIsTransferable(getIsTransferable(route.getId()))
                                .setIsUnselectable(mCurrentSelectedRouteIds.contains(route.getId()))
                                .build();
                result.add(dynamicDescriptor);
            }
            return result;
        }

        private boolean getIsGroupable(String routeId) {
            switch (routeId) {
                case ROUTE_ID_1:
                    return ROUTE_GROUPABLE_1;
                case ROUTE_ID_2:
                    return ROUTE_GROUPABLE_2;
                case ROUTE_ID_3:
                    return ROUTE_GROUPABLE_3;
                default:
                    return false;
            }
        }

        private boolean getIsTransferable(String routeId) {
            switch (routeId) {
                case ROUTE_ID_1:
                    return ROUTE_TRANSFERABLE_1;
                case ROUTE_ID_2:
                    return ROUTE_TRANSFERABLE_2;
                case ROUTE_ID_3:
                    return ROUTE_TRANSFERABLE_3;
                default:
                    return false;
            }
        }

        // Internal classes.

        private class StubRouteController extends RouteController {
            private final String mRouteId;

            private StubRouteController(String routeId) {
                mRouteId = routeId;
            }

            @Override
            public void onSelect() {
                Log.i(TAG, "StubRouteController.onSelect()");
                mRoutes.put(
                        mRouteId,
                        new MediaRouteDescriptor.Builder(mRoutes.get(mRouteId))
                                .setConnectionState(
                                        MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED)
                                .build());
                publishProviderState();
            }

            @Override
            public void onUnselect(int reason) {
                Log.i(TAG, "StubRouteController.onUnselect()");
                mRoutes.put(
                        mRouteId,
                        new MediaRouteDescriptor.Builder(mRoutes.get(mRouteId))
                                .setConnectionState(
                                        MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                                .build());
                publishProviderState();
            }

            @Override
            public void onSetVolume(int volume) {
                MediaRouteDescriptor route = mRoutes.get(mRouteId);
                if (route == null) {
                    return;
                }
                mRoutes.put(
                        mRouteId,
                        new MediaRouteDescriptor.Builder(route).setVolume(volume).build());
                publishProviderState();
            }

            @Override
            public void onUpdateVolume(int delta) {
                MediaRouteDescriptor route = mRoutes.get(mRouteId);
                if (route == null) {
                    return;
                }
                int currentVolume = route.getVolume();
                mRoutes.put(
                        mRouteId,
                        new MediaRouteDescriptor.Builder(route)
                                .setVolume(currentVolume + delta)
                                .build());
                publishProviderState();
            }
        }

        private class StubDynamicRouteController extends DynamicGroupRouteController {

            @Override
            public void onSelect() {
                Log.i(TAG, "StubDynamicRouteController.onSelect()");
                publishState();
            }

            @Override
            public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
                if (routeIds == null) {
                    return;
                }
                Log.i(TAG, "StubDynamicRouteController.onUpdateMemberRoutes()");
                mCurrentSelectedRouteIds.clear();
                mCurrentSelectedRouteIds.addAll(routeIds);
                publishState();
            }

            @Override
            public void onAddMemberRoute(@NonNull String routeId) {
                Log.i(TAG, "StubDynamicRouteController.onAddMemberRoute with routeId = " + routeId);
                mCurrentSelectedRouteIds.add(routeId);
                publishState();
            }

            @Override
            public void onRemoveMemberRoute(@NonNull String routeId) {
                Log.i(
                        TAG,
                        "StubDynamicRouteController.onRemoveMemberRoute with routeId = " + routeId);
                mCurrentSelectedRouteIds.remove(routeId);
                publishState();
            }

            @Override
            public boolean onControlRequest(
                    @NonNull Intent intent, @Nullable MediaRouter.ControlRequestCallback callback) {
                if (callback != null) {
                    callback.onResult(SEND_CONTROL_REQUEST_RESULT);
                }
                return true;
            }

            @Override
            public void onSetVolume(int volume) {
                mGroupDescriptor =
                        new MediaRouteDescriptor.Builder(mGroupDescriptor)
                                .setVolume(volume)
                                .build();
                publishState();
            }

            @Override
            public void onUpdateVolume(int delta) {
                int currentVolume = mGroupDescriptor.getVolume();
                mGroupDescriptor =
                        new MediaRouteDescriptor.Builder(mGroupDescriptor)
                                .setVolume(currentVolume + delta)
                                .build();
                publishState();
            }

            private void publishState() {
                Collection<DynamicRouteDescriptor> dynamicRoutes = buildDynamicRouteDescriptors();
                Log.i(
                        TAG,
                        "StubDynamicRouteController.publishState() with dynamicRoutes.size() = "
                                + dynamicRoutes.size());
                notifyDynamicRoutesChanged(mGroupDescriptor, dynamicRoutes);
            }
        }
    }
}
