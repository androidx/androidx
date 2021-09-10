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

package androidx.ads.identifier.internal;

import static androidx.ads.identifier.AdvertisingIdUtils.GET_AD_ID_ACTION;
import static androidx.ads.identifier.MockAdvertisingIdService.TESTING_AD_ID;
import static androidx.ads.identifier.testing.MockPackageManagerHelper.createServiceResolveInfo;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.ads.identifier.MockAdvertisingIdService;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.ads.identifier.testing.MockPackageManagerHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class HoldingConnectionClientTest {
    private static final String MOCK_SERVICE_NAME = MockAdvertisingIdService.class.getName();

    private MockPackageManagerHelper mMockPackageManagerHelper = new MockPackageManagerHelper();

    private Context mContext;

    private HoldingConnectionClient mClient;

    @Before
    public void setUp() throws Exception {
        MockHoldingConnectionClient.sGetServiceConnectionThrowException = false;
        MockHoldingConnectionClient.sGetServiceFromConnectionThrowInterruptedException = false;

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

        mClient = new HoldingConnectionClient(mContext);
    }

    @After
    public void tearDown() {
        mClient.finish();

        Intent serviceIntent = new Intent(GET_AD_ID_ACTION);
        serviceIntent.setClassName(mContext.getPackageName(), MOCK_SERVICE_NAME);
        mContext.stopService(serviceIntent);
    }

    @Test
    public void connectedAtBeginning() {
        assertThat(mClient.isConnected()).isTrue();
    }

    @Test
    public void finish() {
        mClient.finish();

        assertThat(mClient.isConnected()).isFalse();
    }

    @Test
    public void getService() throws Exception {
        assertThat(mClient.getIdService().getId()).isEqualTo(TESTING_AD_ID);
    }

    @Test
    public void getPackageName() {
        assertThat(mClient.getPackageName()).isEqualTo(mContext.getPackageName());
    }

    @Test(expected = TimeoutException.class)
    public void getServiceWithPackageName_connectionTimeout() throws Exception {
        new MockHoldingConnectionClient(mContext);
    }

    @Test(expected = InterruptedException.class)
    public void getServiceWithPackageName_interrupted() throws Exception {
        MockHoldingConnectionClient.sGetServiceFromConnectionThrowInterruptedException = true;

        new MockHoldingConnectionClient(mContext);
    }

    @Test(expected = IOException.class)
    public void getServiceWithPackageName_connectionFailed() throws Exception {
        MockHoldingConnectionClient.sGetServiceConnectionThrowException = true;

        new MockHoldingConnectionClient(mContext);
    }

    private static class MockHoldingConnectionClient extends HoldingConnectionClient {

        static boolean sGetServiceConnectionThrowException = false;
        static boolean sGetServiceFromConnectionThrowInterruptedException = false;

        MockHoldingConnectionClient(Context context)
                throws InterruptedException, TimeoutException,
                androidx.ads.identifier.AdvertisingIdNotAvailableException,
                IOException {
            super(context);
        }

        @Override
        BlockingServiceConnection getServiceConnection(ComponentName componentName)
                throws IOException {
            if (sGetServiceConnectionThrowException) {
                throw new IOException();
            }

            // This connection does not bind to any service, so it always timeout.
            return new BlockingServiceConnection();
        }

        @Override
        IAdvertisingIdService getIdServiceFromConnection()
                throws TimeoutException, InterruptedException {
            if (sGetServiceFromConnectionThrowInterruptedException) {
                throw new InterruptedException();
            }
            return super.getIdServiceFromConnection();
        }
    }
}
