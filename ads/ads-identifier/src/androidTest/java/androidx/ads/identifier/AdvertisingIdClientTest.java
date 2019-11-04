/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ads.identifier;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;
import static androidx.ads.identifier.MockAdvertisingIdService.TESTING_AD_ID;
import static androidx.ads.identifier.testing.MockPackageManagerHelper.createServiceResolveInfo;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.ads.identifier.internal.BlockingServiceConnection;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.ads.identifier.testing.MockPackageManagerHelper;
import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AdvertisingIdClientTest {
    private static final String MOCK_SERVICE_NAME = MockAdvertisingIdService.class.getName();
    private static final String MOCK_THROWS_NPE_SERVICE_NAME =
            MockAdvertisingIdThrowsNpeService.class.getName();

    private MockPackageManagerHelper mMockPackageManagerHelper = new MockPackageManagerHelper();

    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockAdvertisingIdClient.sGetServiceConnectionThrowException = false;
        MockAdvertisingIdClient.sGetAdvertisingIdServiceThrowInterruptedException = false;

        Context applicationContext = ApplicationProvider.getApplicationContext();

        mContext = new ContextWrapper(applicationContext) {
            @Override
            public Context getApplicationContext() {
                return this;
            }

            @Override
            public PackageManager getPackageManager() {
                return mMockPackageManagerHelper.getMockPackageManager();
            }
        };

        mMockPackageManagerHelper.mockQueryGetAdIdServices(Lists.newArrayList(
                createServiceResolveInfo(mContext.getPackageName(), MOCK_SERVICE_NAME)));
    }

    @After
    public void tearDown() {
        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        serviceIntent.setClassName(mContext.getPackageName(), MOCK_SERVICE_NAME);
        mContext.stopService(serviceIntent);

        Intent npeServiceIntent = new Intent(GET_AD_ID_ACTION);
        npeServiceIntent.setClassName(mContext.getPackageName(), MOCK_THROWS_NPE_SERVICE_NAME);
        mContext.stopService(npeServiceIntent);
    }

    @Test
    public void getAdvertisingIdInfo() throws Exception {
        AdvertisingIdInfo info = AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();

        assertThat(info).isEqualTo(AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(mContext.getPackageName())
                .build());
    }

    public void getAdvertisingIdInfo_noProvider() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(Collections.<ResolveInfo>emptyList());

        try {
            AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseThat().isInstanceOf(AdvertisingIdNotAvailableException.class);
            return;
        }
        fail("Expected ExecutionException");
    }

    @Test
    public void getAdvertisingIdInfo_serviceThrowsNullPointerException() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(Lists.newArrayList(
                createServiceResolveInfo(mContext.getPackageName(), MOCK_THROWS_NPE_SERVICE_NAME)));

        try {
            AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseThat().isInstanceOf(AdvertisingIdNotAvailableException.class);
            return;
        }
        fail("Expected ExecutionException");
    }

    @Test
    public void getInfo_getInfoTwice() throws Exception {
        AdvertisingIdClient client = new AdvertisingIdClient(mContext);
        client.getInfoInternal();
        AdvertisingIdInfo info = client.getInfoInternal();
        client.finish();

        assertThat(info).isEqualTo(AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(mContext.getPackageName())
                .build());
    }

    @Test
    public void getInfo_twoClients() throws Exception {
        AdvertisingIdClient client1 = new AdvertisingIdClient(mContext);
        AdvertisingIdClient client2 = new AdvertisingIdClient(mContext);
        AdvertisingIdInfo info1 = client1.getInfoInternal();
        AdvertisingIdInfo info2 = client1.getInfoInternal();
        client1.finish();
        client2.finish();

        AdvertisingIdInfo expected = AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(mContext.getPackageName())
                .build();
        assertThat(info1).isEqualTo(expected);
        assertThat(info2).isEqualTo(expected);
    }

    @Test(timeout = 11000L)
    public void getAdvertisingIdInfo_connectionTimeout() throws Exception {
        try {
            MockAdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseThat().isInstanceOf(TimeoutException.class);
            return;
        }
        fail("Expected ExecutionException");
    }

    @Test
    public void getAdvertisingIdInfo_interrupted() throws Exception {
        MockAdvertisingIdClient.sGetAdvertisingIdServiceThrowInterruptedException = true;

        try {
            MockAdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseThat().isInstanceOf(InterruptedException.class);
            return;
        }
        fail("Expected ExecutionException");
    }

    @Test
    public void getAdvertisingIdInfo_connectionFailed() throws Exception {
        MockAdvertisingIdClient.sGetServiceConnectionThrowException = true;

        try {
            MockAdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        } catch (ExecutionException e) {
            assertThat(e).hasCauseThat().isInstanceOf(IOException.class);
            return;
        }
        fail("Expected ExecutionException");
    }

    private static class MockAdvertisingIdClient extends AdvertisingIdClient {

        static boolean sGetServiceConnectionThrowException = false;
        static boolean sGetAdvertisingIdServiceThrowInterruptedException = false;

        static Thread sCurrentThread;

        MockAdvertisingIdClient(Context context) {
            super(context);
        }

        @Override
        BlockingServiceConnection getServiceConnection() throws IOException {
            if (sGetServiceConnectionThrowException) {
                throw new IOException();
            }

            sCurrentThread = Thread.currentThread();

            // This connection does not bind to any service, so it always timeout.
            return new BlockingServiceConnection();
        }

        @Override
        IAdvertisingIdService getAdvertisingIdService(BlockingServiceConnection bsc)
                throws TimeoutException, InterruptedException {
            if (sGetAdvertisingIdServiceThrowInterruptedException) {
                throw new InterruptedException();
            }
            return super.getAdvertisingIdService(bsc);
        }

        @NonNull
        public static ListenableFuture<AdvertisingIdInfo> getAdvertisingIdInfo(
                @NonNull final Context context) {
            return CallbackToFutureAdapter.getFuture(
                    new CallbackToFutureAdapter.Resolver<AdvertisingIdInfo>() {
                        @Override
                        public Object attachCompleter(@NonNull final
                                CallbackToFutureAdapter.Completer<AdvertisingIdInfo> completer) {
                            EXECUTOR_SERVICE.execute(new Runnable() {
                                @Override
                                public void run() {
                                    MockAdvertisingIdClient client =
                                            new MockAdvertisingIdClient(context);
                                    try {
                                        completer.set(client.getInfoInternal());
                                    } catch (IOException | AdvertisingIdNotAvailableException
                                            | TimeoutException | InterruptedException e) {
                                        completer.setException(e);
                                    }
                                    // No need to call unbindService() here since not call
                                    // bindService() in this mock.
                                }
                            });
                            return "getAdvertisingIdInfo";
                        }
                    });
        }
    }

    @Test
    public void normalizeId() {
        String id = AdvertisingIdClient.normalizeId("abc");

        assertThat(id).isEqualTo("90015098-3cd2-3fb0-9696-3f7d28e17f72"); // UUID version 3 of "abc"
    }

    @Test
    public void isAdvertisingIdProviderAvailable() {
        assertThat(AdvertisingIdClient.isAdvertisingIdProviderAvailable(mContext)).isTrue();
    }

    @Test
    public void isAdvertisingIdProviderAvailable_noProvider() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(Collections.<ResolveInfo>emptyList());

        assertThat(AdvertisingIdClient.isAdvertisingIdProviderAvailable(mContext)).isFalse();
    }

    @Test
    public void isAdvertisingIdProviderAvailable_twoProviders() throws Exception {
        mMockPackageManagerHelper.mockQueryGetAdIdServices(Lists.newArrayList(
                createServiceResolveInfo("com.a", "A"),
                createServiceResolveInfo("com.b", "B")));

        assertThat(AdvertisingIdClient.isAdvertisingIdProviderAvailable(mContext)).isTrue();
    }

}
