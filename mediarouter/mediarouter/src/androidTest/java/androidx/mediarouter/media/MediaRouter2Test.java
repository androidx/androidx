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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.MediaRoute2ProviderService;
import android.media.RoutingSessionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Messenger;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.mediarouter.media.MediaRouter.RouteInfo;
import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

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

/**
 * Tests features related to {@link android.media.MediaRouter2}.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
public class MediaRouter2Test {
    private static final String TAG = "MR2Test";
    private static final int TIMEOUT_MS = 5_000;

    private Context mContext;
    private MediaRouter mRouter;
    private final MediaRouter.Callback mPlaceholderCallback = new MediaRouter.Callback() {};
    StubMediaRouteProviderService mMr1ProviderService;
    StubMediaRouteProviderService.StubMediaRouteProvider mMr1Provider;
    StubMediaRoute2ProviderService mMr2ProviderService;
    StubMediaRoute2ProviderService.StubMediaRoute2Provider mMr2Provider;
    MediaRouteProviderService.MediaRouteProviderServiceImplApi30 mServiceImpl;
    MediaRoute2ProviderServiceAdapter mMr2ProviderServiceAdapter;

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
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(StubMediaRouteProviderService.CATEGORY_TEST)
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
                mMr1ProviderService = StubMediaRouteProviderService.getInstance();
                boolean isMr1ProviderCreated = false;
                if (mMr1ProviderService != null
                        && mMr1ProviderService.getMediaRouteProvider() != null) {
                    mMr1Provider =
                            (StubMediaRouteProviderService.StubMediaRouteProvider)
                                    mMr1ProviderService.getMediaRouteProvider();
                    mServiceImpl =
                            (MediaRouteProviderService.MediaRouteProviderServiceImplApi30)
                                    mMr1ProviderService.mImpl;
                    mMr2ProviderServiceAdapter = mServiceImpl.mMR2ProviderServiceAdapter;
                    isMr1ProviderCreated = mMr2ProviderServiceAdapter != null;
                }

                mMr2ProviderService = StubMediaRoute2ProviderService.getInstance();
                boolean isMr2ProviderCreated = false;
                if (mMr2ProviderService != null
                        && mMr2ProviderService.getMediaRouteProvider() != null) {
                    mMr2Provider =
                            (StubMediaRoute2ProviderService.StubMediaRoute2Provider)
                                    mMr2ProviderService.getMediaRouteProvider();
                    isMr2ProviderCreated = mMr2Provider != null;
                }

                return isMr1ProviderCreated && isMr2ProviderCreated;
            }
        }.run();
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mMr1Provider.initializeRoutes();
                            mMr1Provider.publishRoutes();
                            mMr2Provider.initializeRoutes();
                            mMr2Provider.publishRoutes();
                        });
    }

    @After
    public void tearDown() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            for (RoutingSessionInfo sessionInfo :
                                    mMr2ProviderServiceAdapter.getAllSessionInfo()) {
                                mMr2ProviderServiceAdapter.onReleaseSession(
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
    public void selectRoute_withSelectedMr1Route_shouldBeNoOp() throws Exception {
        String descriptorId = StubMediaRouteProviderService.ROUTE_ID1;
        waitForRoutesAdded(descriptorId);
        assertNotNull(mRoutes);

        // Select the route for the first time.
        waitForRouteSelected(descriptorId, descriptorId, /* routeSelected= */ true);

        // Wait for a session being created.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> !mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());

        // Select the route for the second time, which should be no op.
        waitForRouteSelected(descriptorId, descriptorId, /* routeSelected= */ false);

        // Stop casting the session before casting to the same route again.
        waitForRouteUnselected(descriptorId, descriptorId);

        // Wait for a session being released.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());

        // Select the route for casting again.
        waitForRouteSelected(descriptorId, descriptorId, /* routeSelected= */ true);
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

        assertEquals(1, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Select the route for the second time, which should be no op.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ false);

        // Check that only one dynamic group route controller is created.
        assertEquals(1, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Stop casting the session before casting to the same route again.
        waitForRouteUnselected(mr2DescriptorId, StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getNumberOfCreatedControllers(descriptorId) == 0);

        assertEquals(0, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Select the route for casting again.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        assertEquals(1, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Unselect the route to prevent it interrupts other tests.
        waitForRouteUnselected(mr2DescriptorId, StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getNumberOfCreatedControllers(descriptorId) == 0);
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
        assertEquals(1, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Stop casting the session before casting to the same route again.
        waitForRouteUnselected(mr2DescriptorId, StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getNumberOfCreatedControllers(descriptorId) == 0);

        assertEquals(0, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Select the route for casting again.
        waitForRouteSelected(
                mr2DescriptorId,
                StubMediaRoute2ProviderService.ROUTE_ID_GROUP,
                /* routeSelected= */ true);

        assertEquals(1, mMr2Provider.getNumberOfCreatedControllers(descriptorId));

        // Unselect the route to prevent it interrupts other tests.
        waitForRouteUnselected(mr2DescriptorId, StubMediaRoute2ProviderService.ROUTE_ID_GROUP);
        // Wait for the route controller is removed from the media route provider.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> mMr2Provider.getNumberOfCreatedControllers(descriptorId) == 0);
    }

    @Test
    @MediumTest
    public void selectFromMr1AndStopFromSystem_unselect() throws Exception {
        CountDownLatch onRouteSelectedLatch = new CountDownLatch(1);
        CountDownLatch onRouteUnselectedLatch = new CountDownLatch(1);
        CountDownLatch onRouteEnabledLatch = new CountDownLatch(1);
        String descriptorId = StubMediaRouteProviderService.ROUTE_ID1;

        addCallback(
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteSelected(
                            @NonNull MediaRouter router,
                            @NonNull RouteInfo selectedRoute,
                            int reason,
                            @NonNull RouteInfo requestedRoute) {
                        if (TextUtils.equals(selectedRoute.getDescriptorId(), descriptorId)
                                && reason == MediaRouter.UNSELECT_REASON_ROUTE_CHANGED) {
                            onRouteSelectedLatch.countDown();
                        }
                    }

                    @Override
                    public void onRouteUnselected(
                            @NonNull MediaRouter router, @NonNull RouteInfo route, int reason) {
                        if (TextUtils.equals(route.getDescriptorId(), descriptorId)
                                && reason == MediaRouter.UNSELECT_REASON_STOPPED) {
                            onRouteUnselectedLatch.countDown();
                        }
                    }

                    @Override
                    public void onRouteChanged(
                            @NonNull MediaRouter router, @NonNull RouteInfo route) {
                        if (onRouteUnselectedLatch.getCount() == 0
                                && TextUtils.equals(route.getDescriptorId(), descriptorId)
                                && route.isEnabled()) {
                            onRouteEnabledLatch.countDown();
                        }
                    }
                });
        waitForRoutesAdded(descriptorId);
        assertNotNull(mRoutes);

        RouteInfo routeToSelect = mRoutes.get(descriptorId);
        assertNotNull(routeToSelect);

        getInstrumentation().runOnMainSync(() -> mRouter.selectRoute(routeToSelect));
        assertTrue(onRouteSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for a session being created.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> !mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());
        // TODO: Find a correct session info
        for (RoutingSessionInfo sessionInfo : mMr2ProviderServiceAdapter.getAllSessionInfo()) {
            getInstrumentation()
                    .runOnMainSync(
                            () ->
                                    mMr2ProviderServiceAdapter.onReleaseSession(
                                            MediaRoute2ProviderService.REQUEST_ID_NONE,
                                            sessionInfo.getId()));
        }
        assertTrue(onRouteUnselectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Make sure the route is enabled
        assertTrue(onRouteEnabledLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @MediumTest
    public void addUserRouteFromMr1_isSystemRoute_returnsFalse() {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            android.media.MediaRouter mediaRouter1 =
                                    (android.media.MediaRouter)
                                            mContext.getSystemService(Context.MEDIA_ROUTER_SERVICE);

                            android.media.MediaRouter.RouteCategory sampleRouteCategory =
                                    mediaRouter1.createRouteCategory(
                                            "SAMPLE_ROUTE_CATEGORY", /* isGroupable= */ false);

                            android.media.MediaRouter.UserRouteInfo sampleUserRoute =
                                    mediaRouter1.createUserRoute(sampleRouteCategory);
                            sampleUserRoute.setName("SAMPLE_USER_ROUTE");

                            mediaRouter1.addUserRoute(sampleUserRoute);

                            for (RouteInfo routeInfo : mRouter.getRoutes()) {
                                // We are checking for this route using getRoutes rather than
                                // through the onRouteAdded callback because of b/312700919
                                if (routeInfo.getName().equals("SAMPLE_USER_ROUTE")) {
                                    assertFalse(routeInfo.isSystemRoute());
                                }
                            }
                        });
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
    public void setRouteVolume_onStaticNonGroupRoute() {
        // We run session creation on the main thread to ensure the route creation from the setup
        // method happens before the session creation. Otherwise, this call may call into an
        // inconsistent adapter state.
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mMr2ProviderServiceAdapter.onCreateSession(
                                        MediaRoute2ProviderService.REQUEST_ID_NONE,
                                        mContext.getPackageName(),
                                        StubMediaRouteProviderService.ROUTE_ID1,
                                        /* sessionHints= */ null));
        StubMediaRouteProviderService.StubMediaRouteProvider.StubRouteController createdController =
                mMr1Provider.mControllers.get(StubMediaRouteProviderService.ROUTE_ID1);
        assertNotNull(createdController); // Avoids nullability warning.
        assertNull(createdController.mLastSetVolume);
        mMr2ProviderServiceAdapter.setRouteVolume(StubMediaRouteProviderService.ROUTE_ID1, 100);
        assertEquals(100, (int) createdController.mLastSetVolume);
        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                createdController.mRouteControllerOptions;
        assertNotNull(routeControllerOptions);
        assertEquals(mContext.getPackageName(), routeControllerOptions.getClientPackageName());
    }

    @SmallTest
    @Test
    public void onBinderDied_releaseRoutingSessions() throws Exception {
        String descriptorId = StubMediaRouteProviderService.ROUTE_ID1;

        waitForRoutesAdded(descriptorId);
        assertNotNull(mRoutes);

        RouteInfo routeToSelect = mRoutes.get(descriptorId);
        assertNotNull(routeToSelect);

        getInstrumentation().runOnMainSync(() -> mRouter.selectRoute(routeToSelect));

        // Wait for a session being created.
        PollingCheck.waitFor(
                TIMEOUT_MS, () -> !mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());

        try {
            List<Messenger> messengers =
                    mServiceImpl.mClients.stream()
                            .map(client -> client.mMessenger)
                            .collect(Collectors.toList());
            getInstrumentation()
                    .runOnMainSync(() -> messengers.forEach(mServiceImpl::onBinderDied));
            // It should have no session info.
            PollingCheck.waitFor(
                    TIMEOUT_MS, () -> mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());
        } finally {
            // Rebind for future tests
            getInstrumentation()
                    .runOnMainSync(
                            () -> {
                                MediaRouter.sGlobal.mRegisteredProviderWatcher.stop();
                                MediaRouter.sGlobal.mRegisteredProviderWatcher.start();
                            });
        }
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

    void addCallback(MediaRouter.Callback callback) {
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

    void waitForRoutesAdded(String descriptorId) throws Exception {
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

    void waitForRouteSelected(
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

    void waitForRouteUnselected(String descriptorIdToUnselect, String deselectedDescriptorId)
            throws Exception {
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

        RouteInfo routeToUnselect = mRoutes.get(descriptorIdToUnselect);
        assertNotNull(routeToUnselect);

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
