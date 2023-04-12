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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.media.MediaRoute2ProviderService;
import android.media.RoutingSessionInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Messenger;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.text.TextUtils;

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
    private MediaRouter.Callback mPlaceholderCallback = new MediaRouter.Callback() {};
    StubMediaRouteProviderService mService;
    StubMediaRouteProviderService.StubMediaRouteProvider mProvider;
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
                mService = StubMediaRouteProviderService.getInstance();
                if (mService != null && mService.getMediaRouteProvider() != null) {
                    mProvider = (StubMediaRouteProviderService.StubMediaRouteProvider)
                            mService.getMediaRouteProvider();
                    mServiceImpl = (MediaRouteProviderService.MediaRouteProviderServiceImplApi30)
                            mService.mImpl;
                    mMr2ProviderServiceAdapter = mServiceImpl.mMR2ProviderServiceAdapter;
                    return mMr2ProviderServiceAdapter != null;
                }
                return false;
            }
        }.run();
        getInstrumentation().runOnMainSync(() -> {
            mProvider.initializeRoutes();
            mProvider.publishRoutes();
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
    public void selectFromMr1AndStopFromSystem_unselect() throws Exception {
        CountDownLatch onRouteSelectedLatch = new CountDownLatch(1);
        CountDownLatch onRouteUnselectedLatch = new CountDownLatch(1);
        CountDownLatch onRouteEnabledLatch = new CountDownLatch(1);
        String descriptorId = StubMediaRouteProviderService.ROUTE_ID1;

        addCallback(new MediaRouter.Callback() {
            @Override
            public void onRouteSelected(@NonNull MediaRouter router,
                    @NonNull RouteInfo selectedRoute, int reason,
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
            public void onRouteChanged(@NonNull MediaRouter router, @NonNull RouteInfo route) {
                if (onRouteUnselectedLatch.getCount() == 0
                        && TextUtils.equals(route.getDescriptorId(), descriptorId)
                        && route.isEnabled()) {
                    onRouteEnabledLatch.countDown();
                }
            }
        });
        waitForRoutesAdded();
        assertNotNull(mRoutes);

        RouteInfo routeToSelect = mRoutes.get(descriptorId);
        assertNotNull(routeToSelect);

        getInstrumentation().runOnMainSync(() -> mRouter.selectRoute(routeToSelect));
        assertTrue(onRouteSelectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        // Wait for a session being created.
        PollingCheck.waitFor(TIMEOUT_MS,
                () -> !mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());
        //TODO: Find a correct session info
        for (RoutingSessionInfo sessionInfo : mMr2ProviderServiceAdapter.getAllSessionInfo()) {
            getInstrumentation().runOnMainSync(() ->
                    mMr2ProviderServiceAdapter.onReleaseSession(
                            MediaRoute2ProviderService.REQUEST_ID_NONE,
                            sessionInfo.getId()));
        }
        assertTrue(onRouteUnselectedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        // Make sure the route is enabled
        assertTrue(onRouteEnabledLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
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
                mProvider.mControllers.get(StubMediaRouteProviderService.ROUTE_ID1);
        assertNotNull(createdController); // Avoids nullability warning.
        assertNull(createdController.mLastSetVolume);
        mMr2ProviderServiceAdapter.setRouteVolume(StubMediaRouteProviderService.ROUTE_ID1, 100);
        assertEquals(100, (int) createdController.mLastSetVolume);
    }

    @SmallTest
    @Test
    public void onBinderDied_releaseRoutingSessions() throws Exception {
        String descriptorId = StubMediaRouteProviderService.ROUTE_ID1;

        waitForRoutesAdded();
        assertNotNull(mRoutes);

        RouteInfo routeToSelect = mRoutes.get(descriptorId);
        assertNotNull(routeToSelect);

        getInstrumentation().runOnMainSync(() -> mRouter.selectRoute(routeToSelect));

        // Wait for a session being created.
        PollingCheck.waitFor(TIMEOUT_MS,
                () -> !mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());

        try {
            List<Messenger> messengers =
                    mServiceImpl.mClients.stream()
                            .map(client -> client.mMessenger)
                            .collect(Collectors.toList());
            getInstrumentation().runOnMainSync(() ->
                    messengers.forEach(mServiceImpl::onBinderDied));
            // It should have no session info.
            PollingCheck.waitFor(TIMEOUT_MS,
                    () -> mMr2ProviderServiceAdapter.getAllSessionInfo().isEmpty());
        } finally {
            // Rebind for future tests
            getInstrumentation().runOnMainSync(
                    () -> {
                        MediaRouter.sGlobal.mRegisteredProviderWatcher.stop();
                        MediaRouter.sGlobal.mRegisteredProviderWatcher.start();
                    });
        }
    }

    @SmallTest
    @Test
    public void setRouterParams_onRouteParamsChangedCalled() throws Exception {
        CountDownLatch onRouterParmasChangedLatch = new CountDownLatch(1);
        final MediaRouterParams[] routerParams = {null};

        addCallback(new MediaRouter.Callback() {
            @Override
            public void onRouterParamsChanged(
                    @NonNull MediaRouter router, MediaRouterParams params) {
                routerParams[0] = params;
                onRouterParmasChangedLatch.countDown();
            }
        });

        Bundle extras = new Bundle();
        extras.putString("test-key", "test-value");
        MediaRouterParams params = new MediaRouterParams.Builder().setExtras(extras).build();
        getInstrumentation().runOnMainSync(() -> {
            mRouter.setRouterParams(params);
        });

        assertTrue(onRouterParmasChangedLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        Bundle actualExtras = routerParams[0].getExtras();
        assertNotNull(actualExtras);
        assertEquals("test-value", actualExtras.getString("test-key"));
    }

    void addCallback(MediaRouter.Callback callback) {
        getInstrumentation().runOnMainSync(() -> {
            mRouter.addCallback(mSelector, callback,
                    MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
                            | MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        });
        mCallbacks.add(callback);
    }

    void waitForRoutesAdded() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        MediaRouter.Callback callback = new MediaRouter.Callback() {
            @Override
            public void onRouteAdded(@NonNull MediaRouter router, @NonNull RouteInfo route) {
                if (!route.isDefaultOrBluetooth()) {
                    latch.countDown();
                }
            }
        };

        addCallback(callback);

        latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        getInstrumentation().runOnMainSync(() -> mRoutes = mRouter.getRoutes().stream().collect(
                Collectors.toMap(route -> route.getDescriptorId(), route -> route)));
    }
}
