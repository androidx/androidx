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

import androidx.ads.identifier.testing.MockPackageManagerHelper;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

@LargeTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class AdvertisingIdClientTest {
    private static final String MOCK_SERVICE_NAME = MockAdvertisingIdService.class.getName();
    private static final String MOCK_THROWS_NPE_SERVICE_NAME =
            MockAdvertisingIdThrowsNpeService.class.getName();

    private MockPackageManagerHelper mMockPackageManagerHelper = new MockPackageManagerHelper();

    private Context mContext;

    @Before
    public void setUp() throws Exception {
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
        AdvertisingIdClient.clearConnectionClient();

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
    public void getAdvertisingIdInfo_getTwice() throws Exception {
        AdvertisingIdInfo info1 = AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        AdvertisingIdInfo info2 = AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();

        AdvertisingIdInfo expected = AdvertisingIdInfo.builder()
                .setId(TESTING_AD_ID)
                .setLimitAdTrackingEnabled(true)
                .setProviderPackageName(mContext.getPackageName())
                .build();
        assertThat(info1).isEqualTo(expected);
        assertThat(info2).isEqualTo(expected);
    }

    @Test
    public void notConnectedAtBeginning() {
        assertThat(AdvertisingIdClient.isConnected()).isFalse();
    }

    @Test
    public void scheduleAutoDisconnect() throws Exception {
        AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        assertThat(AdvertisingIdClient.isConnected()).isTrue();

        Thread.sleep(20000);
        assertThat(AdvertisingIdClient.isConnected()).isTrue();

        Thread.sleep(11000);
        assertThat(AdvertisingIdClient.isConnected()).isFalse();
    }

    @Test
    public void scheduleAutoDisconnect_extend() throws Exception {
        AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();
        assertThat(AdvertisingIdClient.isConnected()).isTrue();

        Thread.sleep(20000);
        assertThat(AdvertisingIdClient.isConnected()).isTrue();
        AdvertisingIdClient.getAdvertisingIdInfo(mContext).get();

        Thread.sleep(20000);
        assertThat(AdvertisingIdClient.isConnected()).isTrue();

        Thread.sleep(11000);
        assertThat(AdvertisingIdClient.isConnected()).isFalse();
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
