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

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.platform.concurrent.DirectExecutor;

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
public class DeviceSuggestionsTest {

    private static final String SUGGESTED_DEVICE_DISPLAY_NAME = "Suggested Device";
    private static final String SUGGESTED_DEVICE_ID = "suggested_device_id";
    private static final @MediaRouter.RouteInfo.DeviceType int SUGGESTED_DEVICE_TYPE =
            MediaRouter.RouteInfo.DEVICE_TYPE_REMOTE_SPEAKER;
    private static final int TIMEOUT_SECONDS = 10;

    private Context mContext;
    private MediaRouter mMediaRouterUnderTest;

    private final SuggestedDeviceInfo mSuggestedDeviceInfo =
            new SuggestedDeviceInfo.Builder(
                            SUGGESTED_DEVICE_DISPLAY_NAME,
                            SUGGESTED_DEVICE_ID,
                            SUGGESTED_DEVICE_TYPE)
                    .build();

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
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        MediaRouteSelector selector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(StubMediaRouteProviderService.CATEGORY_TEST)
                        .build();
        runOnMain(
                () -> {
                    mMediaRouterUnderTest = MediaRouter.getInstance(mContext);
                    mMediaRouterUnderTest.addCallback(
                            selector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
                });
    }

    @After
    public void tearDown() {
        runOnMain(
                () -> {
                    mMediaRouterUnderTest.clearDeviceSuggestions();
                    mMediaRouterUnderTest.removeCallback(mCallback);
                });
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
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(mSuggestedDeviceInfo));

                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.containsKey(mContext.getPackageName()));
                    List<SuggestedDeviceInfo> suggestedDevices =
                            deviceSuggestionsMap.get(mContext.getPackageName());
                    assertNotNull(suggestedDevices);
                    assertEquals(1, suggestedDevices.size());
                    SuggestedDeviceInfo suggestedDeviceFetched = suggestedDevices.get(0);
                    assertEquals(
                            mSuggestedDeviceInfo.getDeviceDisplayName(),
                            suggestedDeviceFetched.getDeviceDisplayName());
                    assertEquals(
                            mSuggestedDeviceInfo.getRouteId(), suggestedDeviceFetched.getRouteId());
                    assertEquals(mSuggestedDeviceInfo.getType(), suggestedDeviceFetched.getType());
                });
    }

    @Test
    public void getDeviceSuggestions_afterClearingDeviceSuggestions_returnsEmptyMap() {
        runOnMain(
                () -> {
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(mSuggestedDeviceInfo));
                    mMediaRouterUnderTest.clearDeviceSuggestions();

                    Map<String, List<SuggestedDeviceInfo>> deviceSuggestionsMap =
                            mMediaRouterUnderTest.getDeviceSuggestions();

                    assertTrue(deviceSuggestionsMap.isEmpty());
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
                    mMediaRouterUnderTest.registerDeviceSuggestionsUpdatesCallback(
                            callback, DirectExecutor.INSTANCE);
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(mSuggestedDeviceInfo));
                });

        assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertEquals(
                mSuggestedDeviceInfo.getDeviceDisplayName(),
                receivedSuggestion[0].getDeviceDisplayName());
        assertEquals(mSuggestedDeviceInfo.getRouteId(), receivedSuggestion[0].getRouteId());
        assertEquals(mSuggestedDeviceInfo.getType(), receivedSuggestion[0].getType());
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
                        mMediaRouterUnderTest.setDeviceSuggestions(List.of(mSuggestedDeviceInfo));
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
                    mMediaRouterUnderTest.setDeviceSuggestions(List.of(mSuggestedDeviceInfo));
                });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void runOnMain(Runnable runnable) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(runnable);
    }
}
