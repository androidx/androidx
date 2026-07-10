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

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.MediaRoute2ProviderService;
import android.media.MediaRouter2;
import android.media.RoutingSessionInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.mediarouter.media.MediaRouter.RouteInfo;
import androidx.mediarouter.media.StubMediaRoute2ProviderService.StubMediaRoute2Provider.StubDynamicGroupRouteController;
import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tests features related to {@link android.media.MediaRouter2}. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
public class MediaRouter2Test {
    private static final String TAG = "MR2Test";
    private static final int TIMEOUT_MS = 5_000;

    private Context mContext;
    private MediaRouter mRouter;
    private final MediaRouter.Callback mPlaceholderCallback = new MediaRouter.Callback() {};
    StubMediaRoute2ProviderService mMr2ProviderService;
    StubMediaRoute2ProviderService.StubMediaRoute2Provider mMr2Provider;

    List<MediaRouter.Callback> mCallbacks;
    MediaRouteSelector mSelector;

    // Maps descriptor ID to RouteInfo for convenience.
    Map<String, RouteInfo> mRoutes;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        getInstrumentation().runOnMainSync(() -> mRouter = MediaRouter.getInstance(mContext));

        mCallbacks = new ArrayList<>();
        // Set a default selector.
        mSelector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(StubMediaRoute2ProviderService.CATEGORY_TEST)
                        .build();
        MediaRouter2TestActivity.startActivity(mContext);

        getInstrumentation().runOnMainSync(() -> {
            MediaRouteSelector placeholderSelector = new MediaRouteSelector.Builder()
                    .addControlCategory("placeholder category").build();
            mRouter.addCallback(placeholderSelector, mPlaceholderCallback,
                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        });

        new PollingCheck(TIMEOUT_MS) {
            @Override
            protected boolean check() {
                mMr2ProviderService = StubMediaRoute2ProviderService.getInstance();
                if (mMr2ProviderService == null
                        || mMr2ProviderService.getMediaRouteProvider() == null) {
                    return false;
                }
                mMr2Provider =
                        (StubMediaRoute2ProviderService.StubMediaRoute2Provider)
                                mMr2ProviderService.getMediaRouteProvider();
                return mMr2Provider != null;
            }
        }.run();
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mMr2Provider.initializeRoutes();
                            mMr2Provider.publishRoutes();
                        });
    }

    @After
    public void tearDown() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            MediaRouteProviderService.MediaRouteProviderServiceImplApi30 impl =
                                    (MediaRouteProviderService.MediaRouteProviderServiceImplApi30)
                                            mMr2ProviderService.mImpl;
                            for (RoutingSessionInfo sessionInfo :
                                    impl.mMR2ProviderServiceAdapter.getAllSessionInfo()) {
                                impl.mMR2ProviderServiceAdapter.onReleaseSession(
                                        MediaRoute2ProviderService.REQUEST_ID_NONE,
                                        sessionInfo.getId());
                            }
                            mRouter.removeCallback(mPlaceholderCallback);
                            for (MediaRouter.Callback callback : mCallbacks) {
                                mRouter.removeCallback(callback);
                            }
                            mCallbacks.clear();
                            MediaRouterTestHelper.resetMediaRouter();
                        });
        MediaRouter2TestActivity.finishActivity();
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void selectRoute_withSelectedMr2Route_shouldBeNoOp() throws Exception {
        String descriptorId = StubMediaRoute2ProviderService.MR2_ROUTE_ID1;
        String mr2DescriptorId = getMediaRoute2DescriptorId(descriptorId);
        waitForRoutesAdded(mr2DescriptorId);
        assertNotNull(mRoutes);

        // Select the route for the first time.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        List<StubDynamicGroupRouteController> createdControllers =
                mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        int controllerId = createdControllers.get(0).mControllerId;

        // Select the route for the second time, which should be no op.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ false);

        // Check that only one route controller is created for the first route selection.
        createdControllers = mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        assertEquals(controllerId, createdControllers.get(0).mControllerId);

        // Stop casting the session before casting to the same route again.
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getCreatedControllers(descriptorId).isEmpty());

        assertThat(mMr2Provider.getCreatedControllers(descriptorId)).isEmpty();

        // Select the route for casting again.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        createdControllers = mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        // Check that a new route controller is created after the previous one.
        assertNotEquals(controllerId, createdControllers.get(0).mControllerId);

        // Unselect the route to prevent it interrupts other tests.
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getCreatedControllers(descriptorId).isEmpty());
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void selectRoute_withSelectingMr2Route_shouldBeNoOp() throws Exception {
        String descriptorId = StubMediaRoute2ProviderService.MR2_ROUTE_ID1;
        String mr2DescriptorId = getMediaRoute2DescriptorId(descriptorId);
        waitForRoutesAdded(mr2DescriptorId);
        assertNotNull(mRoutes);

        RouteInfo routeToSelect = mRoutes.get(mr2DescriptorId);
        assertNotNull(routeToSelect);

        CountDownLatch onRouteSelectedLatch = new CountDownLatch(2);
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteSelected(
                            @NonNull MediaRouter router,
                            @NonNull RouteInfo selectedRoute,
                            int reason,
                            @NonNull RouteInfo requestedRoute) {
                        Log.i(
                                TAG,
                                "onRouteSelected with selectedRoute = "
                                        + selectedRoute
                                        + ", requestedRoute = "
                                        + requestedRoute
                                        + ", reason = "
                                        + reason);
                        if (TextUtils.equals(
                                        selectedRoute.getDescriptorId(),
                                        StubMediaRoute2ProviderService.ROUTE_ID_GROUP)
                                && reason == MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                            onRouteSelectedLatch.countDown();
                        }
                    }
                };
        addCallback(callback);

        // Select the same route twice.
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.selectRoute(routeToSelect);
                            mRouter.selectRoute(routeToSelect);
                        });

        // Check that only one dynamic group route controller is created.
        assertFalse(onRouteSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, onRouteSelectedLatch.getCount());
        List<StubDynamicGroupRouteController> createdControllers =
                mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        int controllerId = createdControllers.get(0).mControllerId;

        // Stop casting the session before casting to the same route again.
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getCreatedControllers(descriptorId).isEmpty());

        assertThat(mMr2Provider.getCreatedControllers(descriptorId)).isEmpty();

        // Select the route for casting again.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        createdControllers = mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        // Check that a new route controller is created after the previous one.
        assertNotEquals(controllerId, createdControllers.get(0).mControllerId);

        // Unselect the route to prevent it interrupts other tests.
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getCreatedControllers(descriptorId).isEmpty());
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void selectRoute_withDeselectingMr2Route_shouldCreateNewController() throws Exception {
        String descriptorId = StubMediaRoute2ProviderService.MR2_ROUTE_ID1;
        String mr2DescriptorId = getMediaRoute2DescriptorId(descriptorId);
        waitForRoutesAdded(mr2DescriptorId);
        assertNotNull(mRoutes);

        // Select the route.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        List<StubDynamicGroupRouteController> createdControllers =
                mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        int controllerId = createdControllers.get(0).mControllerId;

        CountDownLatch onRouteSelectedLatch = new CountDownLatch(1);
        CountDownLatch onRouteUnselectedLatch = new CountDownLatch(1);
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteSelected(
                            @NonNull MediaRouter router,
                            @NonNull RouteInfo selectedRoute,
                            int reason,
                            @NonNull RouteInfo requestedRoute) {
                        Log.i(
                                TAG,
                                "onRouteSelected with selectedRoute = "
                                        + selectedRoute
                                        + ", requestedRoute = "
                                        + requestedRoute
                                        + ", reason = "
                                        + reason);
                        if (TextUtils.equals(
                                        selectedRoute.getDescriptorId(),
                                        StubMediaRoute2ProviderService.ROUTE_ID_GROUP)
                                && reason == MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                            onRouteSelectedLatch.countDown();
                        }
                    }

                    @Override
                    public void onRouteUnselected(
                            @NonNull MediaRouter router, @NonNull RouteInfo route, int reason) {
                        Log.i(
                                TAG,
                                "onRouteUnselected with route = " + route + ", reason = " + reason);
                        if (TextUtils.equals(
                                        route.getDescriptorId(),
                                        StubMediaRoute2ProviderService.ROUTE_ID_GROUP)
                                && reason == MediaRouter.UNSELECT_REASON_STOPPED) {
                            onRouteUnselectedLatch.countDown();
                        }
                    }
                };
        addCallback(callback);

        // Deselect the route and select the same route again.
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            RouteInfo routeToSelect = mRoutes.get(mr2DescriptorId);
                            assertNotNull(routeToSelect);
                            mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
                            mRouter.selectRoute(routeToSelect);
                        });

        // Check that the route is deselected.
        assertTrue(onRouteUnselectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(onRouteSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        createdControllers = mMr2Provider.getCreatedControllers(descriptorId);
        assertEquals(1, createdControllers.size());
        // Check that a new route controller is created after the previous one.
        assertNotEquals(controllerId, createdControllers.get(0).mControllerId);

        // Unselect the route to prevent it interrupts other tests.
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getCreatedControllers(descriptorId).isEmpty());
    }


    @Test
    @MediumTest
    public void defaultAndBluetoothRoutes_isSystemRoute_returnsTrue() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            for (RouteInfo routeInfo : mRouter.getRoutes()) {
                                if (routeInfo.isDefaultOrBluetooth()) {
                                    assertTrue(routeInfo.isSystemRoute());
                                }
                            }
                        });
    }


    @SmallTest
    @Test
    public void setRouterParams_onRouteParamsChangedCalled() throws Exception {
        CountDownLatch onRouterParamsChangedLatch = new CountDownLatch(1);
        final MediaRouterParams[] routerParams = {null};

        addCallback(
                new MediaRouter.Callback() {
                    @Override
                    public void onRouterParamsChanged(
                            @NonNull MediaRouter router, MediaRouterParams params) {
                        routerParams[0] = params;
                        onRouterParamsChangedLatch.countDown();
                    }
                });

        Bundle extras = new Bundle();
        extras.putString("test-key", "test-value");
        MediaRouterParams params = new MediaRouterParams.Builder().setExtras(extras).build();
        getInstrumentation().runOnMainSync(() -> mRouter.setRouterParams(params));

        assertTrue(onRouterParamsChangedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Bundle actualExtras = routerParams[0].getExtras();
        assertNotNull(actualExtras);
        assertEquals("test-value", actualExtras.getString("test-key"));
    }

    @Test
    @MediumTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void selectedRouteFromMediaRoute2Provider_hasRoutingControllerId() throws Exception {
        String descriptorId = StubMediaRoute2ProviderService.MR2_ROUTE_ID1;
        String mr2DescriptorId = getMediaRoute2DescriptorId(descriptorId);
        waitForRoutesAdded(mr2DescriptorId);
        assertNotNull(mRoutes);

        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            RouteInfo selectedRoute = mRouter.getSelectedRoute();
                            assertEquals(
                                    StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                                    selectedRoute.getDescriptorId());
                            MediaRouteDescriptor selectedRouteDescriptor =
                                    selectedRoute.getMediaRouteDescriptor();
                            assertNotNull(selectedRouteDescriptor);
                            MediaRouteProviderService.MediaRouteProviderServiceImplApi30 impl =
                                    (MediaRouteProviderService.MediaRouteProviderServiceImplApi30)
                                            mMr2ProviderService.mImpl;
                            List<RoutingSessionInfo> sessions =
                                    impl.mMR2ProviderServiceAdapter.getAllSessionInfo();
                            assertEquals(1, sessions.size());
                            // Our newly created remote session is at position 1, and its controller
                            // id should match the routing controller id advertised by the selected
                            // route.
                            String expectedSessionId =
                                    MediaRouter2.getInstance(mContext)
                                            .getControllers()
                                            .get(1)
                                            .getId();
                            assertEquals(
                                    expectedSessionId,
                                    selectedRouteDescriptor.getRoutingControllerId());
                        });
        waitForRouteUnselected(StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
    }

    private void addCallback(MediaRouter.Callback callback) {
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mRouter.addCallback(
                                        mSelector,
                                        callback,
                                        MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
                                                | MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN));
        mCallbacks.add(callback);
    }

    private void waitForRoutesAdded(String descriptorId) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteAdded(
                            @NonNull MediaRouter router, @NonNull RouteInfo route) {
                        if (!route.isDefaultOrBluetooth()) {
                            MediaRouteDescriptor routeDescriptor = route.getMediaRouteDescriptor();
                            if (routeDescriptor != null
                                    && TextUtils.equals(routeDescriptor.getId(), descriptorId)) {
                                latch.countDown();
                            }
                        }
                    }
                };

        addCallback(callback);

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mRoutes =
                                        mRouter.getRoutes().stream()
                                                .collect(
                                                        Collectors.toMap(
                                                                RouteInfo::getDescriptorId,
                                                                Function.identity())));
    }

    private void waitForRouteSelected(
            String descriptorIdToSelect, String selectedDescriptorId, boolean routeSelected)
            throws Exception {
        CountDownLatch onRouteSelectedLatch = new CountDownLatch(1);
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteSelected(
                            @NonNull MediaRouter router,
                            @NonNull RouteInfo selectedRoute,
                            int reason,
                            @NonNull RouteInfo requestedRoute) {
                        Log.i(
                                TAG,
                                "onRouteSelected with selectedRoute = "
                                        + selectedRoute
                                        + ", requestedRoute = "
                                        + requestedRoute
                                        + ", reason = "
                                        + reason);
                        if (TextUtils.equals(selectedRoute.getDescriptorId(), selectedDescriptorId)
                                && reason == MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                            onRouteSelectedLatch.countDown();
                        }
                    }
                };
        addCallback(callback);

        RouteInfo routeToSelect = mRoutes.get(descriptorIdToSelect);
        assertNotNull(routeToSelect);

        getInstrumentation().runOnMainSync(() -> mRouter.selectRoute(routeToSelect));
        assertEquals(routeSelected, onRouteSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void waitForRouteUnselected(String deselectedDescriptorId) throws Exception {
        CountDownLatch onRouteUnselectedLatch = new CountDownLatch(1);
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteUnselected(
                            @NonNull MediaRouter router, @NonNull RouteInfo route, int reason) {
                        Log.i(
                                TAG,
                                "onRouteUnselected with route = " + route + ", reason = " + reason);
                        if (TextUtils.equals(route.getDescriptorId(), deselectedDescriptorId)
                                && reason == MediaRouter.UNSELECT_REASON_STOPPED) {
                            onRouteUnselectedLatch.countDown();
                        }
                    }
                };
        addCallback(callback);

        getInstrumentation()
                .runOnMainSync(() -> mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED));
        assertTrue(onRouteUnselectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private String getMediaRoute2DescriptorId(String descriptorId) {
        return StubMediaRoute2ProviderService.CLIENT_PACKAGE_NAME
                + "/"
                + StubMediaRoute2ProviderService.CLIENT_CLASS_NAME
                + ":"
                + descriptorId;
    }
}
