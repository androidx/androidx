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

import static androidx.mediarouter.media.MediaRouteProviderProtocol.CLIENT_VERSION_4;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_DESCRIPTOR_CHANGED;
import static androidx.mediarouter.media.MediaRouteProviderProtocol.SERVICE_MSG_REGISTERED;
import static androidx.mediarouter.media.MediaRouterActiveScanThrottlingHelper.MAX_ACTIVE_SCAN_DURATION_MS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.mediarouter.media.MediaRouteProviderService.ClientInfo;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
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
    private static final String FAKE_MEDIA_ROUTE_ID_5 = "fakeMediaRouteId5";
    private static final String FAKE_MEDIA_ROUTE_NAME_1 = "fakeMediaRouteName1";
    private static final String FAKE_MEDIA_ROUTE_NAME_2 = "fakeMediaRouteName2";
    private static final String FAKE_MEDIA_ROUTE_NAME_3 = "fakeMediaRouteName3";
    private static final String FAKE_MEDIA_ROUTE_NAME_4 = "fakeMediaRouteName4";
    private static final String FAKE_MEDIA_ROUTE_NAME_5 = "fakeMediaRouteName5";
    private static final String REAL_CLIENT_PACKAGE_NAME = "androidx.mediarouter.test";
    private static final String FAKE_CLIENT_PACKAGE_NAME = "fake_client_package_name";
    private static final long TIME_OUT_MS = 3000;

    @Rule public final ServiceTestRule mServiceRule = new ServiceTestRule();

    private IBinder mService;
    private Messenger mServiceMessenger;
    private Messenger mReceiveMessenger1;
    private Messenger mReceiveMessenger2;
    private int mRequestId;
    private MediaRouteSelector mSelector;
    private ArrayList<Messenger> mRegisteredClients = new ArrayList<>();

    private static CountDownLatch sActiveScanCountDownLatch;
    private static CountDownLatch sPassiveScanCountDownLatch;
    private static CountDownLatch sClientInfoListenerAdditionCountDownLatch;
    private static CountDownLatch sClientInfoListenerRemovalCountDownLatch;
    private static CountDownLatch sRouteCreationCountDownLatch;
    private static MediaRouteDiscoveryRequest sLastDiscoveryRequest;
    private static List<ClientInfo> sLatestClientInfo = new ArrayList<>();
    private static MediaRouteProvider.RouteControllerOptions sRouteControllerOptions;

    @Before
    public void setUp() throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        resetClientInfoListenerAdditionCountDownLatch(2);
        resetClientInfoListenerRemovalCountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent =
                new Intent(context, MediaRouteProviderServiceImpl.class)
                        .setAction(MediaRouteProviderProtocol.SERVICE_INTERFACE);
        mService = mServiceRule.bindService(intent);
        mServiceMessenger = new Messenger(mService);
        mReceiveMessenger1 = new Messenger(new Handler(Looper.getMainLooper()));
        mReceiveMessenger2 = new Messenger(new Handler(Looper.getMainLooper()));
        mSelector =
                new MediaRouteSelector.Builder()
                        .addControlCategory(MediaControlIntent.CATEGORY_LIVE_VIDEO)
                        .build();
        registerClient(mReceiveMessenger1);
        registerClient(mReceiveMessenger2);
        assertTrue(
                sClientInfoListenerAdditionCountDownLatch.await(
                        TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @After
    public void tearDown() throws Exception {
        for (Messenger client : List.copyOf(mRegisteredClients)) {
            unregisterClient(client);
        }
        mServiceRule.unbindService();
        sLastDiscoveryRequest = null;
        sRouteControllerOptions = null;
    }

    @Test
    @SmallTest
    public void createDescriptorBundleForClientVersion() {
        MediaRouteProviderDescriptor descriptor = createMediaRouteProviderDescriptor();

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
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(MediaRouteSelector.EMPTY, false));

        Thread.sleep(TIME_OUT_MS);

        assertNull(sLastDiscoveryRequest);
    }

    @LargeTest
    @Test
    public void setEmptyActiveDiscoveryRequest_shouldRequestScan() throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(mSelector, /* activeScan= */ true));

        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRequestScreenOffActiveScanFromClients_shouldSuppressScreenOffScanRequest()
            throws Exception {
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(mSelector, /* activeScan= */ true,
                        /* shouldScanWithScreenOff= */ true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        PollingCheck.waitFor(TIME_OUT_MS, () -> sLastDiscoveryRequest != null);
        assertFalse(sLastDiscoveryRequest.shouldScanWithScreenOff());
    }

    @Test
    public void testPlatformRequestsActiveScanWithScreenOff_shouldUpdateCompositeRequest() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MediaRouteProviderService.MediaRouteProviderServiceImplBase mrProviderServiceImplBase =
                new MediaRouteProviderService.MediaRouteProviderServiceImplBase(
                        new MediaRouteProviderServiceImpl());

        mrProviderServiceImplBase.setBaseDiscoveryRequest(
                new MediaRouteDiscoveryRequest(
                        mSelector, /* activeScan= */ true, /* shouldScanWithScreenOff= */ true));

        assertTrue(mrProviderServiceImplBase.mCompositeDiscoveryRequest.isActiveScan());
        assertTrue(mrProviderServiceImplBase.mCompositeDiscoveryRequest.shouldScanWithScreenOff());
    }

    @Test
    public void testPlatformRequestsActiveScanWithOutScreenOff_shouldUpdateCompositeRequest() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        MediaRouteProviderService.MediaRouteProviderServiceImplBase mrProviderServiceImplBase =
                new MediaRouteProviderService.MediaRouteProviderServiceImplBase(
                        new MediaRouteProviderServiceImpl());

        mrProviderServiceImplBase.setBaseDiscoveryRequest(
                new MediaRouteDiscoveryRequest(
                        mSelector, /* activeScan= */ true, /* shouldScanWithScreenOff= */ false));

        assertTrue(mrProviderServiceImplBase.mCompositeDiscoveryRequest.isActiveScan());
        assertFalse(mrProviderServiceImplBase.mCompositeDiscoveryRequest.shouldScanWithScreenOff());
    }

    @LargeTest
    @Test
    public void testRequestActiveScan_suppressActiveScanAfter30Seconds() throws Exception {
        // Request active discovery.
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(mSelector, /* activeScan= */ true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        // Right before active scan duration passes, active scan flag should still be true.
        resetActiveAndPassiveScanCountDownLatches();
        assertFalse(
                sPassiveScanCountDownLatch.await(
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
        assertFalse(
                sPassiveScanCountDownLatch.await(
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
        assertFalse(
                sActiveScanCountDownLatch.await(
                        MAX_ACTIVE_SCAN_DURATION_MS - 1000, TimeUnit.MILLISECONDS));

        // Right after the active scan duration passed, active scan flag should be false.
        resetActiveAndPassiveScanCountDownLatches();
        assertTrue(sPassiveScanCountDownLatch.await(1000 + TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @LargeTest
    @Test
    public void testRegisterClient_clientRecordListenerCalled() throws Exception {
        resetClientInfoListenerAdditionCountDownLatch(1);
        resetClientInfoListenerRemovalCountDownLatch(1);
        int initialCount = sLatestClientInfo.size();
        Messenger messenger = new Messenger(new Handler(Looper.getMainLooper()));
        registerClient(messenger);

        assertTrue(
                sClientInfoListenerAdditionCountDownLatch.await(
                        TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, sLatestClientInfo.size() - initialCount);
        Context context = ApplicationProvider.getApplicationContext();
        for (ClientInfo clientInfo : sLatestClientInfo) {
            assertTrue(TextUtils.equals(context.getPackageName(), clientInfo.getPackageName()));
        }

        unregisterClient(messenger);
        assertTrue(
                sClientInfoListenerRemovalCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(initialCount, sLatestClientInfo.size());
    }

    @LargeTest
    @Test
    public void onCreateRouteController_shouldProvideRouteControllerOptions() throws Exception {
        sRouteCreationCountDownLatch = new CountDownLatch(1);
        // Request active discovery.
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(mSelector, /* activeScan= */ true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        final String key = "key";
        final String value = "value";
        Bundle controlHints = new Bundle();
        controlHints.putString(key, value);
        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                new MediaRouteProvider.RouteControllerOptions.Builder()
                        .setControlHints(controlHints)
                        .setClientPackageName(FAKE_CLIENT_PACKAGE_NAME)
                        .build();

        sendCreateRouteController(
                mReceiveMessenger1,
                FAKE_MEDIA_ROUTE_ID_1,
                /* groupRouteId= */ null,
                routeControllerOptions);

        // A route controller is created.
        assertTrue(sRouteCreationCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(value, sRouteControllerOptions.getControlHints().getString(key));
        // Verify that the client package name is reconstructed by the real package name.
        assertNotEquals(FAKE_CLIENT_PACKAGE_NAME, sRouteControllerOptions.getClientPackageName());
        assertEquals(REAL_CLIENT_PACKAGE_NAME, sRouteControllerOptions.getClientPackageName());
    }

    @LargeTest
    @Test
    public void onCreateDynamicGroupRouteController_shouldProvideRouteControllerOptions()
            throws Exception {
        sRouteCreationCountDownLatch = new CountDownLatch(1);
        // Request active discovery.
        resetActiveAndPassiveScanCountDownLatches();
        sendDiscoveryRequest(
                mReceiveMessenger1,
                new MediaRouteDiscoveryRequest(mSelector, /* activeScan= */ true));

        // Active scan should be true.
        assertTrue(sActiveScanCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        final String key = "key";
        final String value = "value";
        Bundle controlHints = new Bundle();
        controlHints.putString(key, value);
        MediaRouteProvider.RouteControllerOptions routeControllerOptions =
                new MediaRouteProvider.RouteControllerOptions.Builder()
                        .setControlHints(controlHints)
                        .setClientPackageName(FAKE_CLIENT_PACKAGE_NAME)
                        .build();

        sendCreateDynamicGroupRouteController(
                mReceiveMessenger1, FAKE_MEDIA_ROUTE_ID_1, routeControllerOptions);

        // A dynamic group route controller is created.
        assertTrue(sRouteCreationCountDownLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        assertEquals(value, sRouteControllerOptions.getControlHints().getString(key));
        // Verify that the client package name is reconstructed by the real package name.
        assertNotEquals(FAKE_CLIENT_PACKAGE_NAME, sRouteControllerOptions.getClientPackageName());
        assertEquals(REAL_CLIENT_PACKAGE_NAME, sRouteControllerOptions.getClientPackageName());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N, maxSdkVersion =
            Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    public void permissionFiltering_isNotEnforcedBeforeBaklava1() throws Exception {
        MediaRouteProviderServiceImpl service = MediaRouteProviderServiceImpl.sInstance;
        List<MediaRouteDescriptor> routes = List.of(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_5, FAKE_MEDIA_ROUTE_NAME_5)
                        .setMinClientVersion(CLIENT_VERSION_4)
                        .setRequiredPermissions(
                                List.of(Set.of(Manifest.permission.NEARBY_WIFI_DEVICES)))
                        .build());
        service.setActiveScanRoutes(routes);
        service.setPassiveRoutes(routes);
        when(service.mPackageManagerSpy.checkPermission(
                eq(Manifest.permission.NEARBY_WIFI_DEVICES),
                anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        CountDownLatch routeWasAdvertisedLatch = new CountDownLatch(1);
        FakeClient fakeClient = new FakeClient(new FakeClient.ServiceCallbackReceiver(){
            @Override
            void onDescriptorChanged(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    for (MediaRouteDescriptor route : descriptor.getRoutes()) {
                        if (route.getId().equals(FAKE_MEDIA_ROUTE_ID_5)) {
                            routeWasAdvertisedLatch.countDown();
                            break;
                        }
                    }
                }
            }
        });
        MediaRouteDiscoveryRequest discoveryRequest = new MediaRouteDiscoveryRequest(
                mSelector, /* activeScan= */ true);

        registerClient(fakeClient.mMessenger);
        sendDiscoveryRequest(fakeClient.mMessenger, discoveryRequest);

        assertTrue(routeWasAdvertisedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES_FULL.BAKLAVA_1, codeName = "Baklava")
    @Test
    public void permissionFiltering_clientDoesNotHavePermission_routeIsNotReturned()
            throws Exception {
        MediaRouteProviderServiceImpl service = MediaRouteProviderServiceImpl.sInstance;
        List<MediaRouteDescriptor> routes = List.of(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_5, FAKE_MEDIA_ROUTE_NAME_5)
                        .setMinClientVersion(CLIENT_VERSION_4)
                        .setRequiredPermissions(
                                List.of(Set.of(Manifest.permission.NEARBY_WIFI_DEVICES)))
                        .build());
        service.setActiveScanRoutes(routes);
        service.setPassiveRoutes(routes);
        when(service.mPackageManagerSpy.checkPermission(
                eq(Manifest.permission.NEARBY_WIFI_DEVICES),
                anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        CountDownLatch onRegisteredLatch = new CountDownLatch(1);
        CountDownLatch onDescriptorChangedLatch = new CountDownLatch(1);
        FakeClient fakeClient = new FakeClient(new FakeClient.ServiceCallbackReceiver(){
            @Override
            void onRegistered(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertThat(descriptor.getRoutes().stream().map(
                            r -> r.getId()).toList()).doesNotContain(FAKE_MEDIA_ROUTE_ID_5);
                }
                onRegisteredLatch.countDown();
            }

            @Override
            void onDescriptorChanged(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertThat(descriptor.getRoutes().stream().map(
                            r -> r.getId()).toList()).doesNotContain(FAKE_MEDIA_ROUTE_ID_5);
                }
                onDescriptorChangedLatch.countDown();
            }
        });
        MediaRouteDiscoveryRequest discoveryRequest = new MediaRouteDiscoveryRequest(
                mSelector, /* activeScan= */ true);

        registerClient(fakeClient.mMessenger);
        sendDiscoveryRequest(fakeClient.mMessenger, discoveryRequest);

        assertTrue(onRegisteredLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(onDescriptorChangedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES_FULL.BAKLAVA_1, codeName = "Baklava")
    @Test
    public void permissionFiltering_clientHasPermission_routeIsReturned() throws Exception {
        MediaRouteProviderServiceImpl service = MediaRouteProviderServiceImpl.sInstance;
        List<MediaRouteDescriptor> routes = List.of(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_5, FAKE_MEDIA_ROUTE_NAME_5)
                        .setMinClientVersion(CLIENT_VERSION_4)
                        .setRequiredPermissions(
                                List.of(Set.of(Manifest.permission.NEARBY_WIFI_DEVICES)))
                        .build());
        service.setActiveScanRoutes(routes);
        service.setPassiveRoutes(routes);
        when(service.mPackageManagerSpy.checkPermission(
                eq(Manifest.permission.NEARBY_WIFI_DEVICES),
                anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);
        CountDownLatch descriptorChangedLatch = new CountDownLatch(1);
        FakeClient fakeClient = new FakeClient(new FakeClient.ServiceCallbackReceiver() {
            @Override
            void onRegistered(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertTrue(descriptor.getRoutes().stream().map(r -> r.getId()).noneMatch(
                            FAKE_MEDIA_ROUTE_ID_5::equals));
                }
            }
            @Override
            void onDescriptorChanged(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null && descriptorChangedLatch.getCount() > 0) {
                    if (descriptor.getRoutes().stream().map(r -> r.getId()).anyMatch(
                            FAKE_MEDIA_ROUTE_ID_5::equals)) {
                        descriptorChangedLatch.countDown();
                    }
                }
            }
        });
        MediaRouteDiscoveryRequest discoveryRequest = new MediaRouteDiscoveryRequest(
                mSelector, /* activeScan= */ true);

        registerClient(fakeClient.mMessenger);
        sendDiscoveryRequest(fakeClient.mMessenger, discoveryRequest);

        assertTrue(descriptorChangedLatch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES_FULL.BAKLAVA_1, codeName = "Baklava")
    @Test
    public void permissionFiltering_twoClientsWithoutPermission_routeIsNotReturned()
            throws Exception {
        MediaRouteProviderServiceImpl service = MediaRouteProviderServiceImpl.sInstance;
        List<MediaRouteDescriptor> routes = List.of(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_5, FAKE_MEDIA_ROUTE_NAME_5)
                        .setMinClientVersion(CLIENT_VERSION_4)
                        .setRequiredPermissions(
                                List.of(Set.of(Manifest.permission.NEARBY_WIFI_DEVICES)))
                        .build());
        service.setActiveScanRoutes(routes);
        service.setPassiveRoutes(routes);
        when(service.mPackageManagerSpy.checkPermission(
                eq(Manifest.permission.NEARBY_WIFI_DEVICES),
                anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        CountDownLatch client1Registered = new CountDownLatch(1);
        FakeClient fakeClient1 = new FakeClient(new FakeClient.ServiceCallbackReceiver(){
            @Override
            void onRegistered(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertThat(descriptor.getRoutes().stream().map(
                            r -> r.getId()).toList()).doesNotContain(FAKE_MEDIA_ROUTE_ID_5);
                }
                client1Registered.countDown();
            }

            @Override
            void onDescriptorChanged(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertThat(descriptor.getRoutes().stream().map(
                            r -> r.getId()).toList()).doesNotContain(FAKE_MEDIA_ROUTE_ID_5);
                }
            }
        });
        MediaRouteDiscoveryRequest discoveryRequest = new MediaRouteDiscoveryRequest(
                mSelector, /* activeScan= */ true);
        registerClient(fakeClient1.mMessenger);
        sendDiscoveryRequest(fakeClient1.mMessenger, discoveryRequest);

        assertTrue(client1Registered.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));

        CountDownLatch client2Registered = new CountDownLatch(1);
        FakeClient fakeClient2 = new FakeClient(new FakeClient.ServiceCallbackReceiver() {
            @Override
            void onRegistered(@Nullable MediaRouteProviderDescriptor descriptor) {
                if (descriptor != null) {
                    assertThat(descriptor.getRoutes().stream().map(
                            r -> r.getId()).toList()).doesNotContain(FAKE_MEDIA_ROUTE_ID_5);
                    client2Registered.countDown();
                }
            }
        });
        registerClient(fakeClient2.mMessenger);

        assertTrue(client2Registered.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    private void registerClient(Messenger receiveMessenger) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_REGISTER;
        msg.arg1 = mRequestId++;
        msg.arg2 = MediaRouteProviderProtocol.CLIENT_VERSION_CURRENT;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
        mRegisteredClients.add(receiveMessenger);
    }

    private void unregisterClient(Messenger receiveMessenger) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_UNREGISTER;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
        mRegisteredClients.remove(receiveMessenger);
    }

    private void sendDiscoveryRequest(
            Messenger receiveMessenger, MediaRouteDiscoveryRequest request) throws Exception {
        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_SET_DISCOVERY_REQUEST;
        msg.arg1 = mRequestId++;
        msg.obj = (request != null) ? request.asBundle() : null;
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    private void sendCreateRouteController(
            Messenger receiveMessenger,
            String routeId,
            @Nullable String groupRouteId,
            MediaRouteProvider.RouteControllerOptions routeControllerOptions)
            throws Exception {
        Bundle data = new Bundle();
        data.putString(MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_ID, routeId);
        data.putString(MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_LIBRARY_GROUP, groupRouteId);
        data.putParcelable(
                MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_CONTROLLER_OPTIONS,
                routeControllerOptions.asBundle());

        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_CREATE_ROUTE_CONTROLLER;
        msg.arg1 = mRequestId++;
        msg.setData(data);
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    private void sendCreateDynamicGroupRouteController(
            Messenger receiveMessenger,
            String routeId,
            MediaRouteProvider.RouteControllerOptions routeControllerOptions)
            throws Exception {
        Bundle data = new Bundle();
        data.putString(MediaRouteProviderProtocol.CLIENT_DATA_MEMBER_ROUTE_ID, routeId);
        data.putParcelable(
                MediaRouteProviderProtocol.CLIENT_DATA_ROUTE_CONTROLLER_OPTIONS,
                routeControllerOptions.asBundle());

        Message msg = Message.obtain();
        msg.what = MediaRouteProviderProtocol.CLIENT_MSG_CREATE_DYNAMIC_GROUP_ROUTE_CONTROLLER;
        msg.arg1 = mRequestId++;
        msg.setData(data);
        msg.replyTo = receiveMessenger;

        mServiceMessenger.send(msg);
    }

    /** Fake {@link MediaRouteProviderService} implementation. */
    public static final class MediaRouteProviderServiceImpl extends MediaRouteProviderService {
        static MediaRouteProviderServiceTest.MediaRouteProviderServiceImpl sInstance = null;
        PackageManager mPackageManagerSpy;  // Note: only usable on SDK level N(24) or higher

        @Override
        public void onCreate() {
            super.onCreate();
            if (sClientInfoListenerAdditionCountDownLatch == null
                    || sClientInfoListenerRemovalCountDownLatch == null) {
                // This test resets both CountDownLatches to non-null at the test setup. If they are
                // null, then the onCreate comes from other tests.
                return;
            }
            sLatestClientInfo.clear();
            addClientInfoListener(
                    Executors.newSingleThreadExecutor(),
                    clients -> {
                        int previousSize = sLatestClientInfo.size();
                        int newSize = clients.size();
                        sLatestClientInfo = clients;
                        if (newSize > previousSize) {
                            sClientInfoListenerAdditionCountDownLatch.countDown();
                        } else if (previousSize > newSize) {
                            sClientInfoListenerRemovalCountDownLatch.countDown();
                        }
                    });
            assertNull(sInstance);
            sInstance = this;
        }

        @Override
        public void onDestroy() {
            // This test resets both CountDownLatches to non-null at the test setup. If they are
            // null, then the onDestroy comes from other tests and sInstance is null.
            if (sClientInfoListenerAdditionCountDownLatch != null
                    && sClientInfoListenerRemovalCountDownLatch != null) {
                assertNotNull(sInstance);
                sInstance = null;
            }
            super.onDestroy();
        }

        @Override
        protected void attachBaseContext(@NonNull Context context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                super.attachBaseContext(context);
            } else {
                // Mockito spy support only works on SDK level N (24) or higher.
                mPackageManagerSpy = spy(context.getPackageManager());
                ContextWrapper wrapper = new ContextWrapper(context) {
                    @Override
                    public PackageManager getPackageManager() {
                        return mPackageManagerSpy;
                    }
                };
                super.attachBaseContext(wrapper);
            }
        }

        @Override
        public MediaRouteProvider onCreateMediaRouteProvider() {
            return new MediaRouteProviderImpl(this);
        }

        void setActiveScanRoutes(List<MediaRouteDescriptor> activeScanRoutes) {
            ((MediaRouteProviderImpl) getMediaRouteProvider()).setActiveScanRoutes(
                    activeScanRoutes);
        }

        void setPassiveRoutes(List<MediaRouteDescriptor> passiveRoutes) {
            ((MediaRouteProviderImpl) getMediaRouteProvider()).setPassiveRoutes(passiveRoutes);
        }
    }

    /** Fake {@link MediaRouteProvider} implementation. */
    public static class MediaRouteProviderImpl extends MediaRouteProvider {
        List<MediaRouteDescriptor> mActiveScanRoutes;
        List<MediaRouteDescriptor> mPassiveRoutes;

        List<MediaRouteDescriptor> mExtraRoutes = Collections.emptyList();

        MediaRouteProviderImpl(Context context) {
            super(context);
            mActiveScanRoutes = createMediaRouteProviderDescriptor().getRoutes();
            mPassiveRoutes = List.of();
        }

        void setActiveScanRoutes(List<MediaRouteDescriptor> activeScanRoutes) {
            mActiveScanRoutes = activeScanRoutes;
        }

        void setPassiveRoutes(List<MediaRouteDescriptor> passiveRoutes) {
            mPassiveRoutes = passiveRoutes;
        }

        @Override
        public void onDiscoveryRequestChanged(MediaRouteDiscoveryRequest discoveryRequest) {
            if (sActiveScanCountDownLatch == null || sPassiveScanCountDownLatch == null) {
                // This test resets both active and passive scan CountDownLatch to non-null at the
                // test setup. If they are null, then the discovery request comes from other tests.
                return;
            }
            boolean wasActiveScan =
                    (sLastDiscoveryRequest != null) ? sLastDiscoveryRequest.isActiveScan() : false;
            boolean isActiveScan =
                    (discoveryRequest != null) ? discoveryRequest.isActiveScan() : false;
            if (wasActiveScan != isActiveScan) {
                MediaRouteProviderDescriptor descriptor =
                        new MediaRouteProviderDescriptor.Builder()
                                .addRoutes(isActiveScan ? mActiveScanRoutes : mPassiveRoutes)
                                .build();
                setDescriptor(descriptor);
                if (isActiveScan) {
                    sActiveScanCountDownLatch.countDown();
                } else {
                    sPassiveScanCountDownLatch.countDown();
                }
            }
            sLastDiscoveryRequest = discoveryRequest;
        }

        @Override
        @Nullable
        public RouteController onCreateRouteController(
                @NonNull String routeId, @NonNull RouteControllerOptions routeControllerOptions) {
            sRouteControllerOptions = routeControllerOptions;
            sRouteCreationCountDownLatch.countDown();
            return null;
        }

        @Override
        @Nullable
        public DynamicGroupRouteController onCreateDynamicGroupRouteController(
                @NonNull String initialMemberRouteId,
                @NonNull RouteControllerOptions routeControllerOptions) {
            sRouteControllerOptions = routeControllerOptions;
            sRouteCreationCountDownLatch.countDown();
            return null;
        }
    }

    private static MediaRouteProviderDescriptor createMediaRouteProviderDescriptor() {
        MediaRouteProviderDescriptor.Builder builder = new MediaRouteProviderDescriptor.Builder();
        builder.addRoute(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_1, FAKE_MEDIA_ROUTE_NAME_1)
                        .setMaxClientVersion(15)
                        .setMinClientVersion(10)
                        .build());
        builder.addRoute(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_2, FAKE_MEDIA_ROUTE_NAME_2)
                        .setMaxClientVersion(18)
                        .setMinClientVersion(11)
                        .build());
        builder.addRoute(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_3, FAKE_MEDIA_ROUTE_NAME_3)
                        .setMaxClientVersion(25)
                        .setMinClientVersion(16)
                        .build());
        builder.addRoute(
                new MediaRouteDescriptor.Builder(FAKE_MEDIA_ROUTE_ID_4, FAKE_MEDIA_ROUTE_NAME_4)
                        .setMaxClientVersion(12)
                        .setMinClientVersion(4)
                        .build());
        return builder.build();
    }

    private void resetActiveAndPassiveScanCountDownLatches() {
        sActiveScanCountDownLatch = new CountDownLatch(1);
        sPassiveScanCountDownLatch = new CountDownLatch(1);
    }

    private void resetClientInfoListenerAdditionCountDownLatch(int count) {
        sClientInfoListenerAdditionCountDownLatch = new CountDownLatch(count);
    }

    private void resetClientInfoListenerRemovalCountDownLatch(int count) {
        sClientInfoListenerRemovalCountDownLatch = new CountDownLatch(count);
    }

    /**
     * A helper class for handling messages sent to a client of a MessageRouteProviderService
     */
    private static class FakeClient {
        @Nullable private ServiceCallbackReceiver mServiceCallbackReceiver;
        private Messenger mMessenger;

        FakeClient(@Nullable ServiceCallbackReceiver callbackReceiver) {
            mServiceCallbackReceiver = callbackReceiver;
            mMessenger = new Messenger(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case SERVICE_MSG_REGISTERED: {
                            MediaRouteProviderDescriptor descriptor =
                                    MediaRouteProviderDescriptor.fromBundle((Bundle) msg.obj);
                            if (mServiceCallbackReceiver != null) {
                                mServiceCallbackReceiver.onRegistered(descriptor);
                            }
                            break;
                        }
                        case SERVICE_MSG_DESCRIPTOR_CHANGED: {
                            MediaRouteProviderDescriptor descriptor =
                                    MediaRouteProviderDescriptor.fromBundle((Bundle) msg.obj);
                            if (mServiceCallbackReceiver != null) {
                                mServiceCallbackReceiver.onDescriptorChanged(descriptor);
                            }
                            break;
                        }
                        default:
                            // ignore
                    }
                }
            });
        }

        static class ServiceCallbackReceiver {
            void onRegistered(@Nullable MediaRouteProviderDescriptor descriptor) {}
            void onDescriptorChanged(@Nullable MediaRouteProviderDescriptor descriptor) {}
        }
    }
}
