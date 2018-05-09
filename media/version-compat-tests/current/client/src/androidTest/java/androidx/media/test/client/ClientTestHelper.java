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

import static androidx.media.test.lib.CommonConstants.ACTION_TEST_HELPER;
import static androidx.media.test.lib.CommonConstants.DEFAULT_TEST_NAME;
import static androidx.media.test.lib.CommonConstants
        .SERVICE_APP_TEST_HELPER_SERVICE_COMPONENT_NAME;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IServiceAppTestHelperService;
import android.util.Log;

import androidx.media.BaseMediaPlayer;
import androidx.media.MediaPlaylistAgent;
import androidx.media.MediaSession2;
import androidx.media.SessionToken2;
import androidx.media.test.lib.CommonConstants;
import androidx.media.test.lib.MediaSession2Constants.BaseMediaPlayerMethods;
import androidx.media.test.lib.MediaSession2Constants.PlaylistAgentMethods;
import androidx.media.test.lib.MediaSession2Constants.Session2Methods;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Interacts with service app's TestHelperService.
 */
public class ClientTestHelper {

    private static final String TAG = "ClientTestHelper";

    private Context mContext;
    private ServiceConnection mServiceConnection;
    private IServiceAppTestHelperService mBinder;

    private CountDownLatch mCountDownLatch;

    public ClientTestHelper(Context context) {
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
        intent.setComponent(SERVICE_APP_TEST_HELPER_SERVICE_COMPONENT_NAME);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the TestHelperService of the service app");
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
     * Disconnects from service app's TestHelperService.
     *
     * @param closeSession2 true if controller wants to close the session before disconnecting.
     */
    public void disconnect(boolean closeSession2) {
        if (closeSession2) {
            try {
                if (mBinder != null) {
                    mBinder.closeSession2();
                }
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to close the remote session");
            }
        }
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    /**
     * Create a session2 in the service app, and gets its token.
     *
     * @return A {@link SessionToken2} object if succeeded, {@code null} if failed.
     */
    public SessionToken2 createDefaultSession2() {
        SessionToken2 token = null;
        try {
            Bundle bundle = mBinder.createSession2(DEFAULT_TEST_NAME);
            if (bundle != null) {
                bundle.setClassLoader(MediaSession2.class.getClassLoader());
            }
            token = SessionToken2.fromBundle(bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get default session token.");
        }
        return token;
    }

    /**
     * Create a session2 in the service app for this specific test, and gets its token.
     *
     * @return A {@link SessionToken2} object if succeeded, {@code null} if failed.
     */
    public SessionToken2 createSession2(String sessionId) {
        SessionToken2 token = null;
        try {
            Bundle bundle = mBinder.createSession2(sessionId);
            if (bundle != null) {
                bundle.setClassLoader(MediaSession2.class.getClassLoader());
            }
            token = SessionToken2.fromBundle(bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionId=" + sessionId);
        }
        return token;
    }

    /**
     * Calls current {@link MediaSession2}'s method.
     *
     * @param method One of the constants in {@link Session2Methods}
     * @param args A bundle that contains arguments. Keys are defined in {@link CommonConstants}.
     */
    public void callMediaSession2Method(int method, Bundle args) {
        try {
            mBinder.callMediaSession2Method(method, args == null ? new Bundle() : args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call session method. method=" + method + ", args=" + args);
        }
    }

    /**
     * Calls current {@link MediaSession2}'s {@link BaseMediaPlayer} method.
     *
     * @param method One of the constants in {@link BaseMediaPlayerMethods}
     * @param args A bundle that contains arguments. Keys are defined in {@link CommonConstants}.
     */
    public void callMediaPlayerInterfaceMethod(int method, Bundle args) {
        try {
            mBinder.callMediaPlayerInterfaceMethod(method, args == null ? new Bundle() : args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call player method. method=" + method + ", args=" + args);
        }
    }

    /**
     * Calls current {@link MediaSession2}'s {@link MediaPlaylistAgent} method.
     *
     * @param method One of the constants in {@link PlaylistAgentMethods}
     * @param args A bundle that contains arguments. Keys are defined in {@link CommonConstants}.
     */
    public void callMediaPlaylistAgentMethod(int method, Bundle args) {
        try {
            mBinder.callMediaPlaylistAgentMethod(method, args == null ? new Bundle() : args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call agent method. method=" + method + ", args=" + args);
        }
    }

    // These methods will run on main thread.
    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's TestHelperService.");
            mBinder = IServiceAppTestHelperService.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
