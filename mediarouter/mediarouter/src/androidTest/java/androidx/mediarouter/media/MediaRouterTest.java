/*
* Copyright (C) 2017 The Android Open Source Project
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

import static androidx.mediarouter.media.MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.mediarouter.media.MediaRouter.RouteInfo;
import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaRouter}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouterTest {
    // The maximum time to wait for an operation.
    private static final long TIME_OUT_MS = 3000L;
    private static final String SESSION_TAG = "test-session";

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";

    private static final String TEST_CATEGORY = "mediarouter_test_category";

    private static final MediaRouteSelector TEST_ROUTE_SELECTOR =
            new MediaRouteSelector.Builder().addControlCategory(TEST_CATEGORY).build();

    private final Object mWaitLock = new Object();

    private Context mContext;
    private MediaRouter mRouter;
    private MediaSessionCompat mSession;
    private final MediaSessionCallback mSessionCallback = new MediaSessionCallback();
    private MediaRouteProviderImpl mProvider;
    private CountDownLatch mActiveScanCountDownLatch;
    private CountDownLatch mPassiveScanCountDownLatch;

    @Before
    public void setUp() {
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mContext = getApplicationContext();
                            mRouter = MediaRouter.getInstance(mContext);
                            mSession = new MediaSessionCompat(mContext, SESSION_TAG);
                            mProvider = new MediaRouteProviderImpl(mContext);
                        });
        assertTrue(MediaTransferReceiver.isDeclared(mContext));
    }

    @After
    public void tearDown() {
        mSession.release();
        getInstrumentation().runOnMainSync(MediaRouterTestHelper::resetMediaRouter);
    }

    /**
     * This test checks whether the session callback work properly after setMediaSessionCompat() is
     * called.
     */
    @Test
    @SmallTest
    public void setMediaSessionCompat_receivesCallbacks() throws Exception {
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mSession.setCallback(mSessionCallback);
                            mRouter.setMediaSessionCompat(mSession);
                        });

        MediaControllerCompat controller = mSession.getController();
        MediaControllerCompat.TransportControls controls = controller.getTransportControls();
        synchronized (mWaitLock) {
            mSessionCallback.reset();
            controls.play();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSessionCallback.mOnPlayCalled);

            mSessionCallback.reset();
            controls.pause();
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mSessionCallback.mOnPauseCalled);
        }
    }

    @Test
    @SmallTest
    @UiThreadTest
    public void getRouterParams_afterSetRouterParams_returnsSetParams() {
        final int dialogType = MediaRouterParams.DIALOG_TYPE_DYNAMIC_GROUP;
        final boolean isOutputSwitcherEnabled = true;
        final boolean transferToLocalEnabled = true;
        final boolean transferReceiverEnabled = false;
        final boolean mediaTransferRestrictedToSelfProviders = true;
        final Bundle paramExtras = new Bundle();
        paramExtras.putString(TEST_KEY, TEST_VALUE);

        MediaRouterParams expectedParams =
                new MediaRouterParams.Builder()
                        .setDialogType(dialogType)
                        .setOutputSwitcherEnabled(isOutputSwitcherEnabled)
                        .setTransferToLocalEnabled(transferToLocalEnabled)
                        .setMediaTransferReceiverEnabled(transferReceiverEnabled)
                        .setMediaTransferRestrictedToSelfProviders(
                                mediaTransferRestrictedToSelfProviders)
                        .setExtras(paramExtras)
                        .build();

        paramExtras.remove(TEST_KEY);
        mRouter.setRouterParams(expectedParams);

        MediaRouterParams actualParams = mRouter.getRouterParams();
        assertEquals(expectedParams, actualParams);
    }

    @SmallTest
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void setRouterParams_shouldSetMediaTransferRestrictToSelfProviders() {
        MediaRouterParams params =
                new MediaRouterParams.Builder()
                        .setMediaTransferRestrictedToSelfProviders(true)
                        .build();
        getInstrumentation()
                .runOnMainSync(() -> mRouter.setRouterParams(params));
        assertTrue(
                MediaRouter.getGlobalRouter()
                        .mRegisteredProviderWatcher
                        .isMediaTransferRestrictedToSelfProvidersForTesting());
        assertTrue(
                MediaRouter.getGlobalRouter()
                        .getMediaRoute2ProviderForTesting()
                        .isMediaTransferRestrictedToSelfProviders());
    }

    @Test
    @LargeTest
    public void testRegisterActiveScanCallback_suppressActiveScanAfter30Seconds() throws Exception {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO).build();
        MediaRouterCallbackImpl callback = new MediaRouterCallbackImpl();

        // Add the provider and callback.
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addProvider(mProvider);
                            mRouter.addCallback(
                                    selector,
                                    callback,
                                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                        });

        // Active scan should be true.
        assertTrue(mActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // After active scan duration, active scan should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(mPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Add the same callback again.
        resetActiveAndPassiveScanCountDownLatches();
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mRouter.addCallback(
                                        selector,
                                        callback,
                                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN));

        // Active scan should be true.
        assertTrue(mActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // After active scan duration, active scan should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(mPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testRegisterMultipleActiveScanCallbacks_suppressActiveScanAfter30Seconds()
            throws Exception {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO).build();
        MediaRouterCallbackImpl callback1 = new MediaRouterCallbackImpl();
        MediaRouterCallbackImpl callback2 = new MediaRouterCallbackImpl();

        // Add the provider and the first callback.
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addProvider(mProvider);
                            mRouter.addCallback(
                                    selector,
                                    callback1,
                                    MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                        });

        // Wait for 5 seconds, add the second callback.
        Thread.sleep(5000);
        getInstrumentation()
                .runOnMainSync(
                        () ->
                                mRouter.addCallback(
                                        selector,
                                        callback2,
                                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN));

        resetActiveAndPassiveScanCountDownLatches();
        // Wait for active scan duration to nearly end, active scan flag should be true.
        assertFalse(mPassiveScanCountDownLatch.await(MAX_ACTIVE_SCAN_DURATION_MS - 1000,
                TimeUnit.MILLISECONDS));

        // Wait until active scan duration ends, active scan flag should be false.
        assertTrue(mPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void unselect_whenRouteBecomesUnselectable_reasonIsDisconnected() throws Exception {
        String testRouteId = "route1";
        String testRouteName = "Route 1";
        String testCategory = "testCategory";
        IntentFilter filter = new IntentFilter();
        filter.addCategory(testCategory);
        MediaRouteProviderImpl[] providerWrapper = new MediaRouteProviderImpl[1];
        getInstrumentation()
                .runOnMainSync(() -> providerWrapper[0] = new MediaRouteProviderImpl(mContext));
        MediaRouteProviderImpl provider = providerWrapper[0];
        MediaRouteDescriptor routeDescriptor =
                new MediaRouteDescriptor.Builder(testRouteId, testRouteName)
                        .addControlFilter(filter)
                        .build();
        CountDownLatch addedLatch = new CountDownLatch(1);
        CountDownLatch selectedLatch = new CountDownLatch(1);
        CountDownLatch unselectedLatch = new CountDownLatch(1);
        int[] unselectReason = new int[] {MediaRouter.UNSELECT_REASON_UNKNOWN};
        MediaRouter.Callback callback =
                new MediaRouter.Callback() {
                    @Override
                    public void onRouteAdded(MediaRouter router, RouteInfo route) {
                        if (testRouteName.equals(route.getName())) {
                            addedLatch.countDown();
                        }
                    }

                    @Override
                    public void onRouteSelected(MediaRouter router, RouteInfo route, int reason) {
                        if (testRouteName.equals(route.getName())) {
                            selectedLatch.countDown();
                        }
                    }

                    @Override
                    public void onRouteUnselected(MediaRouter router, RouteInfo route, int reason) {
                        if (testRouteName.equals(route.getName())) {
                            unselectReason[0] = reason;
                            unselectedLatch.countDown();
                        }
                    }
                };
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addCallback(
                                    new MediaRouteSelector.Builder()
                                            .addControlCategory(testCategory)
                                            .build(),
                                    callback);
                            mRouter.addProvider(provider);
                            provider.setDescriptor(
                                    new MediaRouteProviderDescriptor.Builder()
                                            .addRoute(routeDescriptor)
                                            .build());
                        });
        assertTrue(addedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            for (RouteInfo route : mRouter.getRoutes()) {
                                if (testRouteName.equals(route.getName())) {
                                    mRouter.selectRoute(route);
                                    break;
                                }
                            }
                        });
        assertTrue(selectedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            IntentFilter filter2 = new IntentFilter();
                            filter2.addCategory(testCategory);
                            MediaRouteDescriptor unselectableRoute =
                                    new MediaRouteDescriptor.Builder(testRouteId, testRouteName)
                                            .addControlFilter(filter2)
                                            .setEnabled(false)
                                            .build();
                            provider.setDescriptor(
                                    new MediaRouteProviderDescriptor.Builder()
                                            .addRoute(unselectableRoute)
                                            .build());
                        });

        assertTrue(unselectedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(MediaRouter.UNSELECT_REASON_DISCONNECTED, unselectReason[0]);
    }

    @Test
    @UiThreadTest
    public void testReset() {
        assertNotNull(mRouter);
        assertNotNull(MediaRouter.sGlobal);

        MediaRouterTestHelper.resetMediaRouter();
        assertNull(MediaRouter.sGlobal);

        MediaRouter newInstance = MediaRouter.getInstance(mContext);
        assertNotNull(MediaRouter.sGlobal);
        assertFalse(newInstance.getRoutes().isEmpty());
    }

    @Test
    @SmallTest
    @UiThreadTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    public void setRouterParams_togglingMediaTransfer_registersDiscoveryRequestWithMr2Provider() {
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder().addControlCategory("test_category").build();
        mRouter.addCallback(
                selector,
                new MediaRouterCallbackImpl(),
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        MediaRouterParams.Builder builder = new MediaRouterParams.Builder();
        MediaRouterParams disableParams = builder.setMediaTransferReceiverEnabled(false).build();
        MediaRouterParams enableParams = builder.setMediaTransferReceiverEnabled(true).build();
        mRouter.setRouterParams(disableParams);

        mRouter.setRouterParams(enableParams);

        MediaRouteProvider mr2Provider = null;
        for (MediaRouter.ProviderInfo providerInfo : mRouter.getProviders()) {
            if (providerInfo.getProviderInstance() instanceof MediaRoute2Provider) {
                mr2Provider = providerInfo.getProviderInstance();
                break;
            }
        }
        assertNotNull(mr2Provider);
        assertNotNull(mr2Provider.getDiscoveryRequest());
        assertEquals(selector, mr2Provider.getDiscoveryRequest().getSelector());
        assertTrue(mr2Provider.getDiscoveryRequest().isActiveScan());
    }

    @Test
    @SmallTest
    public void selectionSource_appTriggered() throws Exception {
        String routeId = "test_route_id";
        TestCallback callback = new TestCallback(routeId);
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addProvider(mProvider);
                            mRouter.addCallback(TEST_ROUTE_SELECTOR, callback);
                        });
        publishTestRoutes(Collections.singletonList(routeId));
        assertTrue(callback.mAddedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        RouteInfo selectedRouteInfo = selectRouteWithId(routeId);

        assertNotNull(selectedRouteInfo);
        assertTrue(callback.mSelectionLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(selectedRouteInfo, callback.mSelectedRoute);
        assertNotNull(callback.mSelectionInfo);
        assertEquals(
                SelectionInfo.SELECTION_SOURCE_APP, callback.mSelectionInfo.getSelectionSource());
        assertEquals(
                MediaRouter.UNSELECT_REASON_ROUTE_CHANGED,
                callback.mSelectionInfo.getUnselectReason());
    }

    @Test
    @SmallTest
    public void selectionSource_providerTriggered() throws Exception {
        String routeId = "test_route_id";
        TestCallback callback = new TestCallback(routeId);
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addProvider(mProvider);
                            mRouter.addCallback(TEST_ROUTE_SELECTOR, callback);
                        });
        publishTestRoutes(Collections.singletonList(routeId));
        assertTrue(callback.mAddedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        RouteInfo selectedRouteInfo = selectRouteWithId(routeId);
        assertNotNull(selectedRouteInfo);
        assertTrue(callback.mSelectionLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        TestCallback fallbackCallback = new TestCallback(routeId);
        MediaRouteSelector fallbackSelector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                        .build();
        getInstrumentation()
                .runOnMainSync(() -> mRouter.addCallback(fallbackSelector, fallbackCallback));

        // Unpublish the selected route.
        publishTestRoutes(Collections.emptyList());

        assertTrue(fallbackCallback.mSelectionLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertNotNull(fallbackCallback.mSelectionInfo);
        assertEquals(
                SelectionInfo.SELECTION_SOURCE_PROVIDER,
                fallbackCallback.mSelectionInfo.getSelectionSource());
        assertEquals(
                MediaRouter.UNSELECT_REASON_DISCONNECTED,
                fallbackCallback.mSelectionInfo.getUnselectReason());
    }

    @Test
    @SmallTest
    public void selectionSource_systemTriggered() throws Exception {
        String routeId = "test_route_id";
        TestCallback callback = new TestCallback(routeId);
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            mRouter.addProvider(mProvider);
                            mRouter.addCallback(TEST_ROUTE_SELECTOR, callback);
                        });
        publishTestRoutes(Collections.singletonList(routeId));
        assertTrue(callback.mAddedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        RouteInfo selectedRouteInfo = selectRouteWithId(routeId);
        assertNotNull(selectedRouteInfo);
        assertTrue(callback.mSelectionLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        TestCallback systemCallback = new TestCallback(/* targetRouteId= */ null);
        MediaRouteSelector systemSelector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                        .build();
        getInstrumentation()
                .runOnMainSync(() -> mRouter.addCallback(systemSelector, systemCallback));
        String[] defaultRouteId = new String[1];
        getInstrumentation()
                .runOnMainSync(
                        () -> defaultRouteId[0] = mRouter.getDefaultRoute().getDescriptorId());

        getInstrumentation()
                .runOnMainSync(
                        () ->
                                MediaRouter.getGlobalRouter()
                                        .onPlatformRouteSelectedByDescriptorId(defaultRouteId[0]));

        assertTrue(systemCallback.mSelectionLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(
                SelectionInfo.SELECTION_SOURCE_SYSTEM,
                systemCallback.mSelectionInfo.getSelectionSource());
        assertEquals(
                MediaRouter.UNSELECT_REASON_ROUTE_CHANGED,
                systemCallback.mSelectionInfo.getUnselectReason());
    }

    /** Selects the route with the given id, and returns the selected {@link RouteInfo}. */
    private RouteInfo selectRouteWithId(String routeId) {
        RouteInfo[] routeInfoHolder = new RouteInfo[1];
        getInstrumentation()
                .runOnMainSync(
                        () -> {
                            for (RouteInfo route : mRouter.getRoutes()) {
                                if (routeId.equals(route.getDescriptorId())) {
                                    routeInfoHolder[0] = route;
                                    mRouter.selectRoute(route);
                                    break;
                                }
                            }
                        });
        return routeInfoHolder[0];
    }

    /** Publishes one fake route per given route id (possibly none) using mProvider. */
    private void publishTestRoutes(List<String> routeIds) {
        List<MediaRouteDescriptor> routes = new ArrayList<>();
        for (String routeId : routeIds) {
            String routeName = "test_route_name";
            IntentFilter filter = new IntentFilter();
            filter.addCategory(TEST_CATEGORY);
            MediaRouteDescriptor routeDescriptor =
                    new MediaRouteDescriptor.Builder(routeId, routeName)
                            .addControlFilter(filter)
                            .build();
            routes.add(routeDescriptor);
        }
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        for (MediaRouteDescriptor route : routes) {
            builder.addRoute(route);
        }
        getInstrumentation().runOnMainSync(() -> mProvider.setDescriptor(builder.build()));
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {
        private boolean mOnPlayCalled;
        private boolean mOnPauseCalled;

        public void reset() {
            mOnPlayCalled = false;
            mOnPauseCalled = false;
        }

        @Override
        public void onPlay() {
            synchronized (mWaitLock) {
                mOnPlayCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onPause() {
            synchronized (mWaitLock) {
                mOnPauseCalled = true;
                mWaitLock.notify();
            }
        }
    }

    private class MediaRouteProviderImpl extends MediaRouteProvider {
        private boolean mIsActiveScan;

        MediaRouteProviderImpl(Context context) {
            super(context);
        }

        @Override
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest discoveryRequest) {
            boolean isActiveScan = discoveryRequest != null && discoveryRequest.isActiveScan();
            if (mIsActiveScan != isActiveScan) {
                mIsActiveScan = isActiveScan;
                if (mIsActiveScan) {
                    mActiveScanCountDownLatch.countDown();
                } else {
                    mPassiveScanCountDownLatch.countDown();
                }
            }
        }
    }

    private void resetActiveAndPassiveScanCountDownLatches() {
        mActiveScanCountDownLatch = new CountDownLatch(1);
        mPassiveScanCountDownLatch = new CountDownLatch(1);
    }

    private static class MediaRouterCallbackImpl extends MediaRouter.Callback {}

    private static class TestCallback extends MediaRouter.Callback {
        public RouteInfo mSelectedRoute;
        public RouteInfo mRequestedRoute;
        public SelectionInfo mSelectionInfo;
        public final CountDownLatch mSelectionLatch = new CountDownLatch(1);
        public final CountDownLatch mAddedLatch = new CountDownLatch(1);
        private final String mTargetRouteId;

        TestCallback(String targetRouteId) {
            mTargetRouteId = targetRouteId;
        }

        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo route) {
            if (mTargetRouteId.equals(route.getDescriptorId())) {
                mAddedLatch.countDown();
            }
        }

        @Override
        public void onRouteSelected(
                @NonNull MediaRouter router,
                @NonNull RouteInfo selectedRoute,
                @NonNull RouteInfo requestedRoute,
                @NonNull SelectionInfo selectionInfo) {
            mSelectedRoute = selectedRoute;
            mRequestedRoute = requestedRoute;
            mSelectionInfo = selectionInfo;
            mSelectionLatch.countDown();
        }
    }
}
