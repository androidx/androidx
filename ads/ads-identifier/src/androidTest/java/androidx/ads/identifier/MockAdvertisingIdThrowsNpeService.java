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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.ads.identifier.provider.IAdvertisingIdService;
import androidx.annotation.Nullable;

/**
 * Provide a mock for {@link IAdvertisingIdService} which always throw {@link NullPointerException}.
 * To be used in unit tests.
 */
public class MockAdvertisingIdThrowsNpeService extends Service {

    private MockAdvertisingIdServiceImpl mAdvertisingIdServiceImpl;

    @Override
    public void onCreate() {
        mAdvertisingIdServiceImpl = new MockAdvertisingIdServiceImpl();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAdvertisingIdServiceImpl;
    }

    private static class MockAdvertisingIdServiceImpl extends IAdvertisingIdService.Stub {
        @Override
        public String getId() throws RemoteException {
            throw new NullPointerException();
        }

        @Override
        public boolean isLimitAdTrackingEnabled() throws RemoteException {
            throw new NullPointerException();
        }
    }
}
