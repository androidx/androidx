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

package androidx.media2.test.service;

import static androidx.media2.test.common.CommonConstants.ACTION_MEDIA_BROWSER_COMPAT;
import static androidx.media2.test.common.CommonConstants.MEDIA_BROWSER_COMPAT_PROVIDER_SERVICE;
import static androidx.media2.test.common.TestUtils.PROVIDER_SERVICE_CONNECTION_TIMEOUT_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.util.Log;

import androidx.media2.test.common.IRemoteMediaBrowserCompat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaBrowserCompat} the client app's MediaBrowserCompatProviderService.
 * Users can run {@link MediaBrowserCompat} methods remotely with this object.
 */
public class RemoteMediaBrowserCompat {
    private static final String TAG = "RemoteMediaBrowserCompat";

    private final String mBrowserId;
    private final Context mContext;
    private final CountDownLatch mCountDownLatch;

    private ServiceConnection mServiceConnection;
    private IRemoteMediaBrowserCompat mBinder;

    /**
     * Create a {@link MediaBrowserCompat} in the client app.
     * Should NOT be called main thread.
     */
    public RemoteMediaBrowserCompat(Context context, ComponentName serviceComponent) {
        mContext = context;
        mBrowserId = UUID.randomUUID().toString();
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();
        if (!connectToService()) {
            fail("Failed to connect to the MediaBrowserCompatProviderService.");
        }
        create(serviceComponent);
    }

    public void cleanUp() {
        disconnect();
        disconnectFromService();
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaBrowserCompat methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connect to the given media browser service.
     *
     * @param waitForConnection true if the remote browser needs to wait for the connection,
     *                          false otherwise.
     */
    public void connect(boolean waitForConnection) {
        try {
            mBinder.connect(mBrowserId, waitForConnection);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call connect()");
        }
    }

    public void disconnect() {
        try {
            mBinder.disconnect(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call disconnect()");
        }
    }

    public boolean isConnected() {
        try {
            return mBinder.isConnected(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call isConnected()");
            return false;
        }
    }

    public ComponentName getServiceComponent() {
        try {
            return mBinder.getServiceComponent(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getServiceComponent()");
            return null;
        }
    }

    public String getRoot() {
        try {
            return mBinder.getRoot(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getRoot()");
            return null;
        }
    }

    public Bundle getExtras() {
        try {
            return mBinder.getExtras(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getExtras()");
            return null;
        }
    }

    public Bundle getConnectedSessionToken() {
        try {
            return mBinder.getConnectedSessionToken(mBrowserId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getConnectedToken()");
            return null;
        }
    }

    public void subscribe(String parentId, Bundle options) {
        try {
            mBinder.subscribe(mBrowserId, parentId, options);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call subscribe()");
        }
    }

    public void unsubscribe(String parentId) {
        try {
            mBinder.unsubscribe(mBrowserId, parentId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call unsubscribe()");
        }
    }

    public void getItem(String mediaId) {
        try {
            mBinder.getItem(mBrowserId, mediaId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call getItem()");
        }
    }

    public void search(String query, Bundle extras) {
        try {
            mBinder.search(mBrowserId, query, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call search()");
        }
    }

    public void sendCustomAction(String action, Bundle extras) {
        try {
            mBinder.sendCustomAction(mBrowserId, action, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendCustomAction()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to client app's MediaBrowserCompatProviderService.
     * Should NOT be called main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connectToService() {
        final Intent intent = new Intent(ACTION_MEDIA_BROWSER_COMPAT);
        intent.setComponent(MEDIA_BROWSER_COMPAT_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to bind to the MediaBrowserCompatProviderService.");
        }

        if (bound) {
            try {
                mCountDownLatch.await(PROVIDER_SERVICE_CONNECTION_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from client app's MediaBrowserCompatProviderService.
     */
    private void disconnectFromService() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    /**
     * Create a {@link MediaBrowserCompat} in the client app.
     * Should be used after successful connection through {@link #connectToService()}.
     */
    private void create(ComponentName serviceComponent) {
        try {
            mBinder.create(mBrowserId, serviceComponent);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default browser with given serviceComponent.");
        }
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to client app's MediaBrowserCompatProviderService.");
            mBinder = IRemoteMediaBrowserCompat.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from client app's MediaBrowserCompatProviderService.");
        }
    }
}
