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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Callable;

/**
 * The internal service of AndroidX Advertising ID Provider library to provide the Advertising ID.
 */
@SuppressWarnings("deprecation")
public class AdvertisingIdService extends Service {

    private AdvertisingIdAidlServiceImpl mAdvertisingIdAidlServiceImpl;

    @Override
    public void onCreate() {
        mAdvertisingIdAidlServiceImpl =
                new AdvertisingIdAidlServiceImpl(getAdvertisingIdProvider());
    }

    @Override
    @NonNull
    public IBinder onBind(@NonNull Intent intent) {
        return mAdvertisingIdAidlServiceImpl;
    }

    @VisibleForTesting
    @NonNull
    static androidx.ads.identifier.provider.AdvertisingIdProvider getAdvertisingIdProvider() {
        Callable<androidx.ads.identifier.provider.AdvertisingIdProvider> providerCallable =
                androidx.ads.identifier.provider.AdvertisingIdProviderManager.getProviderCallable();
        if (providerCallable == null) {
            throw new IllegalStateException("Advertising ID Provider not registered.");
        }
        androidx.ads.identifier.provider.AdvertisingIdProvider advertisingIdProvider;
        try {
            advertisingIdProvider = providerCallable.call();
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch the Advertising ID Provider.", e);
        }
        if (advertisingIdProvider == null) {
            throw new IllegalArgumentException("Fetched Advertising ID Provider is null.");
        }
        return advertisingIdProvider;
    }
}
