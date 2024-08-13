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
import android.content.IntentFilter;
import android.os.Bundle;

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
    public static final String CATEGORY_DYNAMIC_PROVIDER_TEST =
            "androidx.mediarouter.media.CATEGORY_DYNAMIC_PROVIDER_TEST";

    public static final String ROUTE_ID_GROUP = "route_id_group";
    public static final String ROUTE_NAME_GROUP = "Group route name";
    public static final String ROUTE_ID_1 = "route_id1";
    public static final String ROUTE_NAME_1 = "Sample Route 1";
    public static final String ROUTE_ID_2 = "route_id2";
    public static final String ROUTE_NAME_2 = "Sample Route 2";

    private static final List<IntentFilter> CONTROL_FILTERS_TEST = new ArrayList<>();

    static {
        IntentFilter filter = new IntentFilter();
        filter.addCategory(CATEGORY_DYNAMIC_PROVIDER_TEST);
        CONTROL_FILTERS_TEST.add(filter);
    }

    @Override
    public MediaRouteProvider onCreateMediaRouteProvider() {
        return new Provider(/* context= */ this);
    }

    private static final class Provider extends MediaRouteProvider {
        private final Map<String, MediaRouteDescriptor> mRoutes = new ArrayMap<>();
        private final Map<String, StubRouteController> mControllers = new ArrayMap<>();
        private final MediaRouteDescriptor mGroupDescriptor;
        private final Set<String> mCurrentSelectedRouteIds = new HashSet<>();
        private boolean mCurrentlyScanning = false;
        @Nullable private DynamicGroupRouteController mGroupController;

        Provider(@NonNull Context context) {
            super(context);
            mGroupDescriptor =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_GROUP, ROUTE_NAME_GROUP)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .build();
            MediaRouteDescriptor route1 =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_1, ROUTE_NAME_1)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .build();
            mRoutes.put(route1.getId(), route1);
            MediaRouteDescriptor route2 =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_2, ROUTE_NAME_2)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .build();
            mRoutes.put(route2.getId(), route2);
        }

        // MediaRouteProvider implementation.

        @Override
        public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
            mCurrentlyScanning =
                    request != null
                            && request.isActiveScan()
                            && request.getSelector()
                                    .hasControlCategory(CATEGORY_DYNAMIC_PROVIDER_TEST);
            publishProviderState();
        }

        @Override
        public RouteController onCreateRouteController(@NonNull String routeId) {
            StubRouteController newController = new StubRouteController(routeId);
            mControllers.put(routeId, newController);
            return newController;
        }

        @Nullable
        @Override
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId, @Nullable Bundle controlHints) {
            mGroupController = new StubDynamicRouteController();
            return mGroupController;
        }

        // Internal methods.

        private void publishProviderState() {
            MediaRouteProviderDescriptor.Builder providerDescriptor =
                    new MediaRouteProviderDescriptor.Builder().setSupportsDynamicGroupRoute(true);
            if (mCurrentlyScanning) {
                providerDescriptor.addRoutes(mRoutes.values());
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
                                .build();
                result.add(dynamicDescriptor);
            }
            return result;
        }

        // Internal classes.

        private class StubRouteController extends RouteController {
            private final String mRouteId;

            private StubRouteController(String routeId) {
                mRouteId = routeId;
            }

            @Override
            public void onSelect() {
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
                mRoutes.put(
                        mRouteId,
                        new MediaRouteDescriptor.Builder(mRoutes.get(mRouteId))
                                .setConnectionState(
                                        MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                                .build());
                publishProviderState();
            }
        }

        private class StubDynamicRouteController extends DynamicGroupRouteController {

            @Override
            public void onSelect() {
                publishState();
            }

            @Override
            public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
                mCurrentSelectedRouteIds.clear();
                mCurrentSelectedRouteIds.addAll(routeIds);
                publishState();
            }

            @Override
            public void onAddMemberRoute(@NonNull String routeId) {
                mCurrentSelectedRouteIds.add(routeId);
                publishState();
            }

            @Override
            public void onRemoveMemberRoute(@NonNull String routeId) {
                mCurrentSelectedRouteIds.remove(routeId);
                publishState();
            }

            private void publishState() {
                notifyDynamicRoutesChanged(mGroupDescriptor, buildDynamicRouteDescriptors());
            }
        }
    }
}
