/*
 * Copyright 2026 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.os.Build;

import androidx.mediarouter.testing.MediaRouterTestHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.platform.concurrent.DirectExecutor;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
public class DeviceSuggestionsTest {

    private static final int TIMEOUT_SECONDS = 10;

    private Context mContext;
    private MediaRouter mMediaRouterUnderTest;

    private final MediaRouter.Callback mCallback = new MediaRouter.Callback() {};

    private final MediaRouter.DeviceSuggestionsUpdatesCallback mDeviceSuggestionsUpdatesCallback =
            new MediaRouter.DeviceSuggestionsUpdatesCallback() {
                @Override
                public void onSuggestionsUpdated(
                        @NonNull String suggestingPackageName,
                        @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {}

                @Override
                public void onSuggestionsCleared(@NonNull String suggestingPackageName) {}

                @Override
                public void onSuggestionsRequested() {}
            };

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(StubMediaRoute2ProviderService.CATEGORY_TEST)
                        .build();
        MediaRouter2TestActivity.startActivity(mContext);

        runOnMain(
                () -> {
                    mMediaRouterUnderTest = MediaRouter.getInstance(mContext);
                    MediaRouteSelector placeholderSelector =
                            new MediaRouteSelector.Builder()
                                    .addControlCategory("placeholder category")
                                    .build();
                    mMediaRouterUnderTest.addCallback(
                            placeholderSelector,
                            mCallback,
                            MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                });

        setUpStubProviders();
        waitForRoutes(mMediaRouterUnderTest, selector);
    }

    @After
    public void tearDown() {
        runOnMain(
                () -> {
                    mMediaRouterUnderTest.clearDeviceSuggestions();
                    mMediaRouterUnderTest.removeCallback(mCallback);
                    StubMediaRouteProviderService registeredService =
                            StubMediaRouteProviderService.getInstance();
                    if (registeredService != null
                            && registeredService.getMediaRouteProvider() != null) {
                        registeredService.getMediaRouteProvider().setDescriptor(null);
                    }
                    StubMediaRoute2ProviderService mr2Service =
                            StubMediaRoute2ProviderService.getInstance();
                    if (mr2Service != null && mr2Service.getMediaRouteProvider() != null) {
                        mr2Service.getMediaRouteProvider().setDescriptor(null);
                    }
                    MediaRouterTestHelper.resetMediaRouter();
                });
        MediaRouter2TestActivity.finishActivity();
    }

    @Test
    @SmallTest
    public void getDeviceSuggestions_withoutSettingDeviceSuggestions_returnsEmptyMap() {
        runOnMain(
                () -> {
                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.isEmpty());
                });
    }

    @Test
    public void getDeviceSuggestions_afterSettingDeviceSuggestions_returnsDeviceSuggestions() {
        runOnMain(
                () -> {
                    MediaRouter.RouteInfo validRoute = getValidRoute(mMediaRouterUnderTest);
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(validRoute));

                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.containsKey(mContext.getPackageName()));
                    List<SuggestedDeviceInfo> suggestedDevices =
                            deviceSuggestionsMap.get(mContext.getPackageName());
                    assertNotNull(suggestedDevices);
                    assertEquals(1, suggestedDevices.size());
                    SuggestedDeviceInfo suggestedDeviceFetched = suggestedDevices.get(0);
                    assertEquals(
                            validRoute.getName(), suggestedDeviceFetched.getDeviceDisplayName());
                    assertEquals(validRoute.getId(), suggestedDeviceFetched.getRouteId());
                    assertEquals(validRoute.getDeviceType(), suggestedDeviceFetched.getType());
                });
    }

    @Test
    public void getDeviceSuggestions_afterClearingDeviceSuggestions_returnsEmptyMap() {
        runOnMain(
                () -> {
                    mMediaRouterUnderTest.setDeviceSuggestions(
                            List.of(getValidRoute(mMediaRouterUnderTest)));
                    mMediaRouterUnderTest.clearDeviceSuggestions();

                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.isEmpty());
                });
    }

    @Test
    public void setDeviceSuggestions_withSystemRoute_filtersOutSystemRoute() {
        runOnMain(
                () -> {
                    MediaRouter.RouteInfo defaultRoute = mMediaRouterUnderTest.getDefaultRoute();
                    assertTrue(defaultRoute.isSystemRoute());
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(defaultRoute));

                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.containsKey(mContext.getPackageName()));
                    List<SuggestedDeviceInfo> suggestedDevices =
                            deviceSuggestionsMap.get(mContext.getPackageName());
                    assertNotNull(suggestedDevices);
                    assertTrue(suggestedDevices.isEmpty());
                });
    }

    @Test
    public void
            unregisterDeviceSuggestionsUpdatesCallback_withoutCallbackRegistered_doesNotCrash() {
        runOnMain(
                () ->
                        mMediaRouterUnderTest.unregisterDeviceSuggestionsUpdatesCallback(
                                mDeviceSuggestionsUpdatesCallback));
    }

    @Test
    public void registerDeviceSuggestionsUpdatesCallback_getsCalledWhenDeviceSuggestionsAreSet()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final SuggestedDeviceInfo[] receivedSuggestion = new SuggestedDeviceInfo[1];
        final MediaRouter.RouteInfo[] validRoute = new MediaRouter.RouteInfo[1];
        MediaRouter.DeviceSuggestionsUpdatesCallback callback =
                new MediaRouter.DeviceSuggestionsUpdatesCallback() {
                    @Override
                    public void onSuggestionsUpdated(
                            @NonNull String suggestingPackageName,
                            @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {
                        receivedSuggestion[0] = suggestedDeviceInfo.get(0);
                        latch.countDown();
                    }

                    @Override
                    public void onSuggestionsCleared(@NonNull String suggestingPackageName) {}

                    @Override
                    public void onSuggestionsRequested() {}
                };

        runOnMain(
                () -> {
                    validRoute[0] = getValidRoute(mMediaRouterUnderTest);
                    mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                            callback, DirectExecutor.INSTANCE);
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(validRoute[0]));
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(validRoute[0].getName(), receivedSuggestion[0].getDeviceDisplayName());
        assertEquals(validRoute[0].getId(), receivedSuggestion[0].getRouteId());
        assertEquals(validRoute[0].getDeviceType(), receivedSuggestion[0].getType());
        runOnMain(() -> mMediaRouterUnderTest.unregisterDeviceSuggestionsUpdatesCallback(callback));
    }

    @Test
    public void registerDeviceSuggestionsUpdatesCallback_getsCalledWhenDeviceSuggestionsAreCleared()
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        MediaRouter.DeviceSuggestionsUpdatesCallback callback =
                new MediaRouter.DeviceSuggestionsUpdatesCallback() {
                    @Override
                    public void onSuggestionsUpdated(
                            @NonNull String suggestingPackageName,
                            @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {}

                    @Override
                    public void onSuggestionsCleared(@NonNull String suggestingPackageName) {
                        latch.countDown();
                    }

                    @Override
                    public void onSuggestionsRequested() {}
                };

        runOnMain(
                () -> {
                    mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                            callback, DirectExecutor.INSTANCE);
                    mMediaRouterUnderTest.setDeviceSuggestions(
                            List.of(getValidRoute(mMediaRouterUnderTest)));
                    mMediaRouterUnderTest.clearDeviceSuggestions();
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        runOnMain(() -> mMediaRouterUnderTest.unregisterDeviceSuggestionsUpdatesCallback(callback));
    }

    @Test
    public void
            registerDeviceSuggestionsUpdatesCallback_withAnotherExecutor_isCalledOnLatestExecutor()
                    throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callCount = new AtomicInteger(0);
        AtomicReference<String> callbackThread = new AtomicReference<>();
        MediaRouter.DeviceSuggestionsUpdatesCallback callback =
                new MediaRouter.DeviceSuggestionsUpdatesCallback() {
                    @Override
                    public void onSuggestionsUpdated(
                            @NonNull String suggestingPackageName,
                            @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {
                        callbackThread.set(Thread.currentThread().getName());
                        callCount.incrementAndGet();
                        latch.countDown();
                    }

                    @Override
                    public void onSuggestionsCleared(@NonNull String suggestingPackageName) {}

                    @Override
                    public void onSuggestionsRequested() {}
                };
        ExecutorService initialExecutor =
                Executors.newSingleThreadExecutor(r -> new Thread(r, "InitialExecutor"));
        ExecutorService newExecutor =
                Executors.newSingleThreadExecutor(r -> new Thread(r, "NewExecutor"));

        try {
            runOnMain(
                    () -> {
                        mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                                callback, initialExecutor);
                        mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                                callback, newExecutor);
                        mMediaRouterUnderTest.setDeviceSuggestions(
                                List.of(getValidRoute(mMediaRouterUnderTest)));
                    });

            assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            CountDownLatch flushLatch = new CountDownLatch(1);
            // Submit a dummy task to the initial executor and wait for it.
            // This ensures that if the callback was incorrectly queued on the initialExecutor,
            // it would have executed before we reach the assertions below.
            initialExecutor.submit(flushLatch::countDown);
            assertTrue(flushLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));

            assertEquals(1, callCount.get());
            assertEquals("NewExecutor", callbackThread.get());
        } finally {
            runOnMain(
                    () ->
                            mMediaRouterUnderTest.unregisterDeviceSuggestionsUpdatesCallback(
                                    callback));
            initialExecutor.shutdown();
            newExecutor.shutdown();
        }
    }

    @Test
    public void
            unregisterDeviceSuggestionsUpdatesCallback_doesNotGetCalledOnDeviceSuggestionsUpdate() {
        MediaRouter.DeviceSuggestionsUpdatesCallback callback =
                new MediaRouter.DeviceSuggestionsUpdatesCallback() {

                    @Override
                    public void onSuggestionsUpdated(
                            @NonNull String suggestingPackageName,
                            @NonNull List<SuggestedDeviceInfo> suggestedDeviceInfo) {
                        fail("onSuggestionsUpdated should not be called");
                    }

                    @Override
                    public void onSuggestionsCleared(@NonNull String suggestingPackageName) {}

                    @Override
                    public void onSuggestionsRequested() {}
                };

        runOnMain(
                () -> {
                    mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                            callback, DirectExecutor.INSTANCE);
                    mMediaRouterUnderTest.unregisterDeviceSuggestionsUpdatesCallback(callback);
                    mMediaRouterUnderTest.setDeviceSuggestions(
                            List.of(getValidRoute(mMediaRouterUnderTest)));
                });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    /**
     * Finds an MR2-backed route (from StubMediaRoute2Provider for example). Must be called on main
     * thread.
     */
    private static MediaRouter.@NonNull RouteInfo getValidRoute(@NonNull MediaRouter router) {
        for (MediaRouter.RouteInfo route : router.getRoutes()) {
            if (route.getProviderInstance() instanceof MediaRoute2Provider) {
                return route;
            }
        }
        throw new IllegalStateException("No MediaRoute2-backed route found.");
    }

    /** Blocks until an MR2 route has been discovered and added to the router. */
    private static void waitForRoutes(
            @NonNull MediaRouter router, @NonNull MediaRouteSelector selector) {
        MediaRouter.Callback callback = new MediaRouter.Callback() {};
        runOnMain(
                () ->
                        router.addCallback(
                                selector,
                                callback,
                                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY
                                        | MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN));
        try {
            new PollingCheck(TIMEOUT_SECONDS * 1000) {
                @Override
                protected boolean check() {
                    final boolean[] found = new boolean[1];
                    runOnMain(
                            () -> {
                                for (MediaRouter.RouteInfo route : router.getRoutes()) {
                                    if (route.getProviderInstance()
                                            instanceof MediaRoute2Provider) {
                                        found[0] = true;
                                        break;
                                    }
                                }
                            });
                    return found[0];
                }
            }.run();
        } finally {
            runOnMain(() -> router.removeCallback(callback));
        }
    }

    /**
     * Polls for the stub provider services and initializes their routes. This ensures the framework
     * is ready for testing.
     */
    private static void setUpStubProviders() {
        new PollingCheck(TIMEOUT_SECONDS * 1000) {
            @Override
            protected boolean check() {
                StubMediaRouteProviderService registeredProviderService =
                        StubMediaRouteProviderService.getInstance();
                StubMediaRoute2ProviderService mr2ProviderService =
                        StubMediaRoute2ProviderService.getInstance();

                if (registeredProviderService != null) {
                    MediaRouteProviderService.MediaRouteProviderServiceImplApi30 serviceImpl =
                            (MediaRouteProviderService.MediaRouteProviderServiceImplApi30)
                                    registeredProviderService.mImpl;
                    if (serviceImpl.mMR2ProviderServiceAdapter == null) {
                        return false;
                    }
                }

                return registeredProviderService != null
                        && registeredProviderService.getMediaRouteProvider() != null
                        && mr2ProviderService != null
                        && mr2ProviderService.getMediaRouteProvider() != null;
            }
        }.run();

        runOnMain(
                () -> {
                    StubMediaRouteProviderService.StubMediaRouteProvider registeredProvider =
                            StubMediaRouteProviderService.getInstance().getMediaRouteProvider();
                    StubMediaRoute2ProviderService.StubMediaRoute2Provider mr2Provider =
                            StubMediaRoute2ProviderService.getInstance().getMediaRouteProvider();

                    registeredProvider.initializeRoutes();
                    registeredProvider.publishRoutes();
                    mr2Provider.initializeRoutes();
                    mr2Provider.publishRoutes();
                });
    }

    private static void runOnMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
