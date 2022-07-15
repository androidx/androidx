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

import static androidx.mediarouter.media.MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link MediaRouteProviderService}.
 */
@RunWith(AndroidJUnit4.class)
public class MediaRouteProviderServiceTest {

    private static final String FAKE_MEDIA_ROUTE_ID_1 = "fakeMediaRouteId1";
    private static final String FAKE_MEDIA_ROUTE_ID_2 = "fakeMediaRouteId2";
    private static final String FAKE_MEDIA_ROUTE_ID_3 = "fakeMediaRouteId3";
    private static final String FAKE_MEDIA_ROUTE_ID_4 = "fakeMediaRouteId4";
    private static final String FAKE_MEDIA_ROUTE_NAME_1 = "fakeMediaRouteName1";
    private static final String FAKE_MEDIA_ROUTE_NAME_2 = "fakeMediaRouteName2";
    private static final String FAKE_MEDIA_ROUTE_NAME_3 = "fakeMediaRouteName3";
    private static final String FAKE_MEDIA_ROUTE_NAME_4 = "fakeMediaRouteName4";
    private static final long TIME_OUT_MS = 3000;

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();
    private IBinder mService;
    private Messenger mServiceMessenger;
    private Messenger mReceiveMessenger1;
    private Messenger mReceiveMessenger2;
    private int mRequestId;
    private MediaRouteSelector mSelector;

    private static CountDownLatch sActiveScanCountDownLatch;
    private static CountDownLatch sPassiveScanCountDownLatch;
    private static MediaRouteDiscoveryRequest sLastDiscoveryRequest;

    @Before
    public void setUp() throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent =
                new Intent(context, MediaRouteProviderServiceImpl.class)
                        .setAction(MediaRouteProviderProtocol.SERVICE_INTERFACE);
        mService = mServiceRule.bindService(intent);
        mServiceMessenger = new Messenger(mService);
        mReceiveMessenger1 = new Messenger(new Handler(Looper.getMainLooper()));
        mReceiveMessenger2 = new Messenger(new Handler(Looper.getMainLooper()));
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO).build();
        registerClient(mReceiveMessenger1);
        registerClient(mReceiveMessenger2);
    }

    @After
    public void tearDown() throws Exception {
        unregisterClient(mReceiveMessenger1);
        unregisterClient(mReceiveMessenger2);
        mServiceRule.unbindService();
        sLastDiscoveryRequest = null;
    }

    @Test
    @SmallTest
    public void testCreateDescriptorBundleForClientVersion() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_1,
                FAKE_MEDIA_ROUTE_NAME_1).setMaxClientVersion(15).setMinClientVersion(10).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_2,
                FAKE_MEDIA_ROUTE_NAME_2).setMaxClientVersion(18).setMinClientVersion(11).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_3,
                FAKE_MEDIA_ROUTE_NAME_3).setMaxClientVersion(25).setMinClientVersion(16).build());
        builder.addRoute(new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_4,
                FAKE_MEDIA_ROUTE_NAME_4).setMaxClientVersion(12).setMinClientVersion(4).build());
        MediaRouteProviderDescriptor descriptor = builder.build();

        Bundle bundle = MediaRouteProviderService
                .createDescriptorBundleForClientVersion(descriptor, 3);
        MediaRouteProviderDescriptor resultDescriptor =
                MediaRouteProviderDescriptor.fromBundle(bundle);
        assertTrue(resultDescriptor.getRoutes().isEmpty());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 4);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        List<MediaRouteDescriptor> routes = resultDescriptor.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(0).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 10);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 12);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(3, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_4, routes.get(2).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_4, routes.get(2).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 15);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_1, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_1, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 16);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(2, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_2, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_2, routes.get(0).getName());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(1).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(1).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 19);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        routes = resultDescriptor.getRoutes();
        assertEquals(1, routes.size());
        assertEquals(FAKE_MEDIA_ROUTE_ID_3, routes.get(0).getId());
        assertEquals(FAKE_MEDIA_ROUTE_NAME_3, routes.get(0).getName());

        bundle = MediaRouteProviderService.createDescriptorBundleForClientVersion(descriptor, 26);
        resultDescriptor = MediaRouteProviderDescriptor.fromBundle(bundle);
        assertTrue(resultDescriptor.getRoutes().isEmpty());
    }

    @LargeTest
    @Test
    public void testSetEmptyPassiveDiscoveryRequest_shouldNotRequestScan() throws Exception {
        sendDiscoveryRequest(mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(MediaRouteSelector.EMPTY, false));

        Thread.sleep(TIME_OUT_MS);

        assertNull(sLastDiscoveryRequest);
    }

    @LargeTest
    @Test
    public void testSetEmptyActiveDiscoveryRequest_shouldRequestScan() throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(MediaRouteSelector.EMPTY, true));

        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @LargeTest
    @Test
    public void testRequestActiveScan_suppressActiveScanAfter30Seconds() throws Exception {
        // Request active discovery.
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(mReceiveMessenger1, new MediaRouteDiscoveryRequest(mSelector, true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Right before active scan duration passes, active scan flag should still be true.
        resetActiveAndPassiveScanCountDownLatches();
        assertFalse(sPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS - 1000, TimeUnit.MILLISECONDS));

        // After active scan duration passed, active scan flag should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(sPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Request active discovery again.
        sendDiscoveryRequest(mReceiveMessenger1, new MediaRouteDiscoveryRequest(mSelector, false));
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(mReceiveMessenger1, new MediaRouteDiscoveryRequest(mSelector, true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Right before active scan duration passes, active scan flag should still be true.
        resetActiveAndPassiveScanCountDownLatches();
        assertFalse(sPassiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS - 1000, TimeUnit.MILLISECONDS));

        // After active scan duration passed, active scan flag should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(sPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @LargeTest
    @Test
    public void testRequestActiveScanFromMultipleClients_suppressActiveScanAfter30Seconds()
            throws Exception {
        // Request active scan from client 1.
        sendDiscoveryRequest(mReceiveMessenger1, new MediaRouteDiscoveryRequest(mSelector, true));

        // Sleep 10 seconds and request active scan from client 2.
        Thread.sleep(10000);
        sendDiscoveryRequest(mReceiveMessenger2, new MediaRouteDiscoveryRequest(mSelector, true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Right before the last client times out, active scan flag should still be true.
        resetActiveAndPassiveScanCountDownLatches();
        assertFalse(sActiveScanCountDownLatch.await(
                MAX_ACTIVE_SCAN_DURATION_MS - 1000, TimeUnit.MILLISECONDS));

        // Right after the active scan duration passed, active scan flag should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(sPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    private void registerClient(Messenger receiveMessenger) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_REGISTER;
        msg.arg1 = mRequestId++;
        msg.arg2 = MediaRouteProviderProtocol.CLIENT_VERSION_CURRENT;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    private void unregisterClient(Messenger receiveMessenger) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_UNREGISTER;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    private void sendDiscoveryRequest(Messenger receiveMessenger,
            MediaRouteDiscoveryRequest request) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_SET_DISCOVERY_REQUEST;
        msg.arg1 = mRequestId++;
        msg.obj = (request != null) ? request.asBundle() : null;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    /** Fake {@link MediaRouteProviderService} implementation. */
    public static final class MediaRouteProviderServiceImpl extends MediaRouteProviderService {
        @Override
        public MediaRouteProvider onCreateMediaRouteProvider() {
            return new MediaRouteProviderImpl(this);
        }
    }

    /** Fake {@link MediaRouteProvider} implementation. */
    public static class MediaRouteProviderImpl extends MediaRouteProvider {
        MediaRouteProviderImpl(Context context) {
            super(context);
        }

        @Override
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest discoveryRequest) {
            boolean wasActiveScan =
                    (sLastDiscoveryRequest != null) ? sLastDiscoveryRequest.isActiveScan() : false;
            boolean isActiveScan =
                    (discoveryRequest != null) ? discoveryRequest.isActiveScan() : false;
            if (wasActiveScan != isActiveScan) {
                if (isActiveScan) {
                    sActiveScanCountDownLatch.countDown();
                } else {
                    sPassiveScanCountDownLatch.countDown();
                }
            }
            sLastDiscoveryRequest = discoveryRequest;
        }
    }

    private void resetActiveAndPassiveScanCountDownLatches() {
        sActiveScanCountDownLatch = new CountDownLatch(1);
        sPassiveScanCountDownLatch = new CountDownLatch(1);
    }
}
