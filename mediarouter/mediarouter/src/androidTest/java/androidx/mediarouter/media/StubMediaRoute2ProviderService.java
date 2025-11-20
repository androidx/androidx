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
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class StubMediaRoute2ProviderService extends MediaRouteProviderService {
    private static final Object sLock = new Object();

    public static final String CLIENT_PACKAGE_NAME = "androidx.mediarouter.test";
    public static final String CLIENT_CLASS_NAME =
            "androidx.mediarouter.media.StubMediaRoute2ProviderService";
    public static final String CATEGORY_TEST = "androidx.mediarouter.media.CATEGORY_TEST";

    public static final String ROUTE_ID_GROUP = "route_id_group";
    public static final String ROUTE_NAME_GROUP = "Group route name";
    public static final int VOLUME_INITIAL_VALUE = 8;
    public static final int VOLUME_MAX = 20;
    public static final String MR2_ROUTE_ID1 = "media_route2_id1";
    public static final String MR2_ROUTE_NAME1 = "MR2 Sample Route 1";
    public static final String MR2_ROUTE_ID2 = "media_route2_id2";
    public static final String MR2_ROUTE_NAME2 = "MR2 Sample Route 2";

    @GuardedBy("sLock")
    private static StubMediaRoute2ProviderService sInstance;

    private static final List<IntentFilter> CONTROL_FILTERS_TEST;

    static {
        IntentFilter f1 = new IntentFilter();
        f1.addCategory(CATEGORY_TEST);

        CONTROL_FILTERS_TEST = new ArrayList<>();
        CONTROL_FILTERS_TEST.add(f1);
    }

    /** Gets the instance of StubMediaRoute2ProviderService. */
    public static StubMediaRoute2ProviderService getInstance() {
        synchronized (sLock) {
            return sInstance;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (sLock) {
            sInstance = this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized (sLock) {
            if (sInstance == this) {
                sInstance = null;
            }
        }
    }

    @Override
    public MediaRouteProvider onCreateMediaRouteProvider() {
        return new StubMediaRoute2Provider(this);
    }

    static class StubMediaRoute2Provider extends MediaRouteProvider {
        private final Object mLock = new Object();

        @GuardedBy("mLock")
        private final Map<String, MediaRouteDescriptor> mRoutes = new ArrayMap<>();

        @GuardedBy("mLock")
        private final Map<String, List<StubDynamicGroupRouteController>>
                mDescriptorIdToControllers = new ArrayMap<>();

        private final AtomicInteger mNextControllerId;
        private final MediaRouteDescriptor mGroupDescriptor;
        boolean mSupportsDynamicGroup = true;

        StubMediaRoute2Provider(@NonNull Context context) {
            super(context);
            mNextControllerId = new AtomicInteger(0);
            mGroupDescriptor =
                    new MediaRouteDescriptor.Builder(ROUTE_ID_GROUP, ROUTE_NAME_GROUP)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .setVolumeMax(VOLUME_MAX)
                            .setVolume(VOLUME_INITIAL_VALUE)
                            .build();
        }

        @Override
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId,
                @NonNull RouteControllerOptions routeControllerOptions) {
            Log.i(
                    TAG,
                    "onCreateDynamicGroupRouteController with initialMemberRouteId = "
                            + initialMemberRouteId);
            StubDynamicGroupRouteController newDynamicRouteController =
                    new StubDynamicGroupRouteController(
                            initialMemberRouteId, routeControllerOptions);
            addController(initialMemberRouteId, newDynamicRouteController);

            return newDynamicRouteController;
        }

        public void initializeRoutes() {
            MediaRouteDescriptor route1 =
                    new MediaRouteDescriptor.Builder(MR2_ROUTE_ID1, MR2_ROUTE_NAME1)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .build();
            MediaRouteDescriptor route2 =
                    new MediaRouteDescriptor.Builder(MR2_ROUTE_ID2, MR2_ROUTE_NAME2)
                            .addControlFilters(CONTROL_FILTERS_TEST)
                            .build();

            synchronized (mLock) {
                mRoutes.put(route1.getId(), route1);
                mRoutes.put(route2.getId(), route2);
            }
        }

        public void publishRoutes() {
            synchronized (mLock) {
                setDescriptor(
                        new MediaRouteProviderDescriptor.Builder()
                                .addRoutes(mRoutes.values())
                                .setSupportsDynamicGroupRoute(mSupportsDynamicGroup)
                                .build());
            }
        }

        public void addController(String routeId, StubDynamicGroupRouteController controller) {
            Log.i(TAG, "addController with routeId = " + routeId + ", controller = " + controller);
            synchronized (mLock) {
                List<StubDynamicGroupRouteController> controllers =
                        mDescriptorIdToControllers.get(routeId);
                if (controllers == null) {
                    controllers = new ArrayList<>();
                }
                controllers.add(controller);
                mDescriptorIdToControllers.put(routeId, controllers);
            }
        }

        public void removeController(String routeId, StubDynamicGroupRouteController controller) {
            synchronized (mLock) {
                Log.i(
                        TAG,
                        "removeController with routeId = "
                                + routeId
                                + ", controller = "
                                + controller);
                List<StubDynamicGroupRouteController> controllers =
                        mDescriptorIdToControllers.get(routeId);
                if (controllers == null) {
                    return;
                }
                if (controllers.contains(controller)) {
                    controllers.remove(controller);
                    if (controllers.isEmpty()) {
                        mDescriptorIdToControllers.remove(routeId);
                    } else {
                        mDescriptorIdToControllers.put(routeId, controllers);
                    }
                }
            }
        }

        public List<StubDynamicGroupRouteController> getCreatedControllers(String descriptorId) {
            synchronized (mLock) {
                List<StubDynamicGroupRouteController> controllers =
                        mDescriptorIdToControllers.get(descriptorId);
                return (controllers != null) ? controllers : List.of();
            }
        }

        class StubDynamicGroupRouteController extends DynamicGroupRouteController {
            final String mRouteId;
            final RouteControllerOptions mRouteControllerOptions;
            final int mControllerId;
            private final Set<String> mCurrentSelectedRouteIds = new HashSet<>();

            StubDynamicGroupRouteController(
                    String routeId, RouteControllerOptions routeControllerOptions) {
                mRouteId = routeId;
                mRouteControllerOptions = routeControllerOptions;
                mControllerId = mNextControllerId.getAndIncrement();
                mCurrentSelectedRouteIds.add(routeId);
            }

            private void publishState() {
                Collection<DynamicRouteDescriptor> dynamicRoutes = buildDynamicRouteDescriptors();
                Log.i(
                        TAG,
                        "StubDynamicGroupRouteController.publishState() with dynamicRoutes.size() ="
                                + " "
                                + dynamicRoutes.size());
                notifyDynamicRoutesChanged(mGroupDescriptor, dynamicRoutes);
            }

            private Collection<DynamicGroupRouteController.DynamicRouteDescriptor>
                    buildDynamicRouteDescriptors() {
                synchronized (mLock) {
                    ArrayList<DynamicGroupRouteController.DynamicRouteDescriptor> result =
                            new ArrayList<>();
                    for (MediaRouteDescriptor route : mRoutes.values()) {
                        DynamicGroupRouteController.DynamicRouteDescriptor dynamicDescriptor =
                                new DynamicGroupRouteController.DynamicRouteDescriptor.Builder(
                                                route)
                                        .setSelectionState(
                                                mCurrentSelectedRouteIds.contains(route.getId())
                                                        ? DynamicGroupRouteController
                                                                .DynamicRouteDescriptor.SELECTED
                                                        : DynamicGroupRouteController
                                                                .DynamicRouteDescriptor.UNSELECTED)
                                        .setIsUnselectable(
                                                mCurrentSelectedRouteIds.contains(route.getId()))
                                        .build();
                        result.add(dynamicDescriptor);
                    }
                    return result;
                }
            }

            @Override
            public void onSelect() {
                Log.i(TAG, "StubDynamicGroupRouteController.onSelect() with routeId = " + mRouteId);
                publishState();
            }

            @Override
            public void onRelease() {
                Log.i(
                        TAG,
                        "StubDynamicGroupRouteController.onRelease() with routeId = " + mRouteId);
                removeController(mRouteId, this);
            }

            @Override
            public void onUpdateMemberRoutes(@Nullable List<String> routeIds) {
                if (routeIds == null) {
                    return;
                }
                Log.i(TAG, "StubDynamicGroupRouteController.onUpdateMemberRoutes()");
            }

            @Override
            public void onAddMemberRoute(@NonNull String routeId) {
                Log.i(
                        TAG,
                        "StubDynamicGroupRouteController.onAddMemberRoute with routeId = "
                                + routeId);
            }

            @Override
            public void onRemoveMemberRoute(@NonNull String routeId) {
                Log.i(
                        TAG,
                        "StubDynamicGroupRouteController.onRemoveMemberRoute with routeId = "
                                + routeId);
            }
        }
    }
}
