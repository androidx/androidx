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
import android.content.IntentFilter;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StubMediaRouteProviderService extends MediaRouteProviderService {
    private static final String TAG = "StubMRPService";
    private static final Object sLock = new Object();

    public static final String CATEGORY_TEST = "androidx.mediarouter.media.CATEGORY_TEST";

    public static final String ROUTE_ID1 = "route_id1";
    public static final String ROUTE_NAME1 = "Sample Route 1";
    public static final String ROUTE_ID2 = "route_id2";
    public static final String ROUTE_NAME2 = "Sample Route 2";

    @GuardedBy("sLock")
    private static StubMediaRouteProviderService sInstance;

    private static final List<IntentFilter> CONTROL_FILTERS_TEST;

    static {
        IntentFilter f1 = new IntentFilter();
        f1.addCategory(CATEGORY_TEST);

        CONTROL_FILTERS_TEST = new ArrayList<>();
        CONTROL_FILTERS_TEST.add(f1);
    }

    public static StubMediaRouteProviderService getInstance() {
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
        return new StubMediaRouteProvider(this);
    }

    class StubMediaRouteProvider extends MediaRouteProvider {
        Map<String, MediaRouteDescriptor> mRoutes = new ArrayMap<>();
        Map<String, StubRouteController> mControllers = new ArrayMap<>();
        boolean mSupportsDynamicGroup = false;

        StubMediaRouteProvider(@NonNull Context context) {
            super(context);
        }

        @Override
        public RouteController onCreateRouteController(@NonNull String routeId) {
            StubRouteController newController = new StubRouteController(routeId);
            mControllers.put(routeId, newController);
            return newController;
        }

        public void initializeRoutes() {
            MediaRouteDescriptor route1 = new MediaRouteDescriptor.Builder(ROUTE_ID1, ROUTE_NAME1)
                    .addControlFilters(CONTROL_FILTERS_TEST)
                    .build();
            MediaRouteDescriptor route2 = new MediaRouteDescriptor.Builder(ROUTE_ID2, ROUTE_NAME2)
                    .addControlFilters(CONTROL_FILTERS_TEST)
                    .build();
            mRoutes.put(route1.getId(), route1);
            mRoutes.put(route2.getId(), route2);
        }

        public void publishRoutes() {
            setDescriptor(new MediaRouteProviderDescriptor.Builder()
                    .addRoutes(mRoutes.values())
                    .setSupportsDynamicGroupRoute(mSupportsDynamicGroup)
                    .build());
        }

        //TODO: Implement DynamicGroupRouteController
        class StubRouteController extends RouteController {
            final String mRouteId;
            @Nullable Integer mLastSetVolume;

            StubRouteController(String routeId) {
                mRouteId = routeId;
            }

            @Override
            public void onSelect() {
                mRoutes.put(mRouteId, new MediaRouteDescriptor.Builder(mRoutes.get(mRouteId))
                        .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_CONNECTED)
                        .build());
                publishRoutes();
            }

            @Override
            public void onUnselect(int reason) {
                mRoutes.put(mRouteId, new MediaRouteDescriptor.Builder(mRoutes.get(mRouteId))
                        .setConnectionState(MediaRouter.RouteInfo.CONNECTION_STATE_DISCONNECTED)
                        .build());
                publishRoutes();
            }

            @Override
            public void onSetVolume(int volume) {
                mLastSetVolume = volume;
            }
        }
    }
}
