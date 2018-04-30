/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media.test.client;

import static androidx.media.test.lib.TestHelperUtil.ACTION_TEST_HELPER;
import static androidx.media.test.lib.TestHelperUtil.SERVICE_TEST_HELPER_COMPONENT_NAME;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.ITestHelperForServiceApp;
import android.util.Log;

import androidx.media.MediaSession2;
import androidx.media.SessionToken2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Interacts with service app's TestHelperService.
 */
public class TestHelper {

    private static final String TAG = "TestHelper";

    private Context mContext;
    private ServiceConnection mServiceConnection;
    private ITestHelperForServiceApp mBinder;

    private CountDownLatch mCountDownLatch;

    public TestHelper(Context context) {
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
    }

    /**
     * Connects to service app's TestHelperService. Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    public boolean connect(int timeoutMs) {
        mServiceConnection = new MyServiceConnection();

        final Intent intent = new Intent(ACTION_TEST_HELPER);
        intent.setComponent(SERVICE_TEST_HELPER_COMPONENT_NAME);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the test helper service of service app");
        }

        if (bound) {
            try {
                mCountDownLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Create a session2 in the service app, and gets its token.
     *
     * @return A {@link SessionToken2} object if succeeded, {@code null} if failed.
     */
    public SessionToken2 getSessionToken2(String testName) {
        SessionToken2 token = null;
        try {
            Bundle bundle = mBinder.getSessionToken2(testName);
            if (bundle != null) {
                bundle.setClassLoader(MediaSession2.class.getClassLoader());
            }
            token = SessionToken2.fromBundle(bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. testName=" + testName);
        }
        return token;
    }

    // These methods will run on main thread.
    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's TestHelperService.");
            mBinder = ITestHelperForServiceApp.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
