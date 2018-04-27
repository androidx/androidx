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

package androidx.media.test.service;

import static androidx.media.test.lib.CommonConstants.ACTION_TEST_HELPER;
import static androidx.media.test.lib.CommonConstants.CLIENT_APP_TEST_HELPER_SERVICE_COMPONENT_NAME;
import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IClientAppTestHelperService;
import android.util.Log;

import androidx.media.MediaController2;
import androidx.media.SessionToken2;
import androidx.media.test.lib.CommonConstants;
import androidx.media.test.lib.MediaController2Constants;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Interacts with client app's TestHelperService.
 */
public class ServiceTestHelper {

    private static final String TAG = "ServiceTestHelper";

    private Context mContext;
    private ServiceConnection mServiceConnection;
    private IClientAppTestHelperService mBinder;

    private CountDownLatch mCountDownLatch;

    public ServiceTestHelper(Context context) {
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
    }

    /**
     * Connects to client app's TestHelperService. Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    public boolean connect(int timeoutMs) {
        mServiceConnection = new MyServiceConnection();

        final Intent intent = new Intent(ACTION_TEST_HELPER);
        intent.setComponent(CLIENT_APP_TEST_HELPER_SERVICE_COMPONENT_NAME);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to bind to the TestHelperService of the service app");
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
     * Disconnects from client app's TestHelperService.
     */
    public void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
    }

    /**
     * Create a {@link MediaController2} in the client app, and wait until connected to the session.
     */
    public void createDefaultController2(SessionToken2 token) {
        try {
            mBinder.createMediaController2(DEFAULT_TEST_NAME, token.toBundle());
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default controller with given token.");
        }
    }

    /**
     * Create a {@link MediaController2} in the client app for this specific test.
     */
    public void createSession2(String testName, SessionToken2 token) {
        try {
            mBinder.createMediaController2(testName, token.toBundle());
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create the controller. testName=" + testName);
        }
    }

    /**
     * Calls current {@link MediaController2}'s method.
     *
     * @see MediaController2Constants
     * @see CommonConstants
     */
    public void callMediaController2Method(int method, Bundle args) {
        try {
            mBinder.callMediaController2Method(method, args == null ? new Bundle() : args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call controller2 method=" + method + ", args=" + args);
        }
    }

    // These methods will run on main thread.
    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to client app's TestHelperService.");
            mBinder = IClientAppTestHelperService.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from client app's TestHelperService.");
        }
    }
}
