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

package androidx.ads.identifier.provider.internal;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.ads.identifier.AdvertisingIdUtils;
import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class AdvertisingIdServiceTest {
    private static final String TESTING_AD_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private Intent mIntent;
    private ServiceConnection mServiceConnection;

    @Before
    public void setUp() {
        androidx.ads.identifier.provider.AdvertisingIdProviderManager.clearProviderCallable();

        mContext = ApplicationProvider.getApplicationContext();

        mIntent = new Intent(AdvertisingIdUtils.GET_AD_ID_ACTION);
        mIntent.setClassName(mContext.getPackageName(), AdvertisingIdService.class.getName());
    }

    @After
    public void tearDown() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mContext.stopService(mIntent);
    }

    private IAdvertisingIdService getService() throws InterruptedException {
        BlockingDeque<IAdvertisingIdService> blockingDeque = new LinkedBlockingDeque<>();
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                blockingDeque.add(IAdvertisingIdService.Stub.asInterface(iBinder));
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
            }
        };
        mContext.bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        return blockingDeque.poll(1, TimeUnit.SECONDS);
    }

    @Test
    public void getId() throws Exception {
        androidx.ads.identifier.provider.AdvertisingIdProviderManager.registerProviderCallable(
                () -> new MockAdvertisingIdProvider(TESTING_AD_ID, true));

        IAdvertisingIdService service = getService();

        assertThat(service.getId()).isEqualTo(TESTING_AD_ID);
        assertThat(service.isLimitAdTrackingEnabled()).isEqualTo(true);
    }

    @Test(expected = RuntimeException.class)
    public void getId_providerThrowsException() throws Exception {
        androidx.ads.identifier.provider.AdvertisingIdProviderManager.registerProviderCallable(
                () -> {
                    MockAdvertisingIdProvider mockAdvertisingIdProvider =
                            new MockAdvertisingIdProvider(TESTING_AD_ID, true);
                    mockAdvertisingIdProvider.mGetIdThrowsException = true;
                    return mockAdvertisingIdProvider;
                });

        IAdvertisingIdService service = getService();
        service.getId();
    }

    private static class MockAdvertisingIdProvider implements
            androidx.ads.identifier.provider.AdvertisingIdProvider {
        private final String mId;
        private final boolean mLimitAdTrackingEnabled;
        boolean mGetIdThrowsException = false;

        MockAdvertisingIdProvider(String id, boolean limitAdTrackingEnabled) {
            mId = id;
            mLimitAdTrackingEnabled = limitAdTrackingEnabled;
        }

        @NonNull
        @Override
        public String getId() {
            if (mGetIdThrowsException) {
                throw new RuntimeException();
            }
            return mId;
        }

        @Override
        public boolean isLimitAdTrackingEnabled() {
            return mLimitAdTrackingEnabled;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void getId_providerNotRegistered() {
        AdvertisingIdService.getAdvertisingIdProvider();
    }

    @Test(expected = RuntimeException.class)
    public void getId_providerCallableThrowsException() {
        androidx.ads.identifier.provider.AdvertisingIdProviderManager.registerProviderCallable(
                () -> {
                    throw new Exception();
                });

        AdvertisingIdService.getAdvertisingIdProvider();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getId_providerCallableReturnsNull() {
        androidx.ads.identifier.provider.AdvertisingIdProviderManager.registerProviderCallable(
                () -> null);

        AdvertisingIdService.getAdvertisingIdProvider();
    }
}
