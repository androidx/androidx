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

import static androidx.media.test.lib.CommonConstants.ACTION_MOCK_MEDIA_LIBRARY_SESSION;
import static androidx.media.test.lib.CommonConstants.MOCK_MEDIA_LIBRARY_SERVICE;
import static androidx.media.test.lib.TestUtils.WAIT_TIME_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IRemoteMediaLibrarySession;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaLibraryService2.MediaLibrarySession;
import androidx.media.MediaSession2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaLibrarySession} in the service app's MockMediaLibraryService2.
 * Users can run some of {@link MediaLibrarySession} methods remotely with this object.
 */
public class RemoteMediaLibrarySession {
    private static final String TAG = "RemoteMediaLibrarySession";

    private final Context mContext;

    private ServiceConnection mServiceConnection;
    private IRemoteMediaLibrarySession mBinder;
    private CountDownLatch mCountDownLatch;

    /**
     * Gets {@link MediaLibrarySession} which is created in the service app.
     * Should NOT be called in main thread.
     */
    public RemoteMediaLibrarySession(Context context) {
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();

        if (!connect()) {
            fail("Failed to connect to the MockMediaLibraryService2.");
        }
    }

    public void cleanUp() {
        disconnect();
    }

    /////////////////////////////////////////////////
    ////       MediaLibrarySession Methods       ////
    /////////////////////////////////////////////////

    public void notifyChildrenChanged(MediaSession2.ControllerInfo controller /* unused */,
            @NonNull String parentId, int itemCount, @Nullable Bundle extras) {
        try {
            mBinder.notifyChildrenChangedToOne(parentId, itemCount, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call notifyChildrenChangedToOne()");
        }
    }

    public void notifyChildrenChanged(@NonNull String parentId, int itemCount,
            @Nullable Bundle extras) {
        try {
            mBinder.notifyChildrenChangedToAll(parentId, itemCount, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call notifyChildrenChangedToAll()");
        }
    }

    /////////////////////////////////////////
    ////      Test related Methods       ////
    /////////////////////////////////////////

    public boolean isOnSubscribeCalled(@NonNull String parentId, @Nullable Bundle extras) {
        try {
            return mBinder.isOnSubscribeCalled(parentId, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call isOnSubscribeCalled()");
        }
        return false;
    }

    public boolean isOnUnsubscribeCalled(@NonNull String parentId) {
        try {
            return mBinder.isOnUnsubscribeCalled(parentId);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call isOnUnsubscribeCalled()");
        }
        return false;
    }

    ///////////////////////////////
    ////    Private methods    ////
    ///////////////////////////////

    /**
     * Connects to service app's MockMediaLibraryService2.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MOCK_MEDIA_LIBRARY_SESSION);
        intent.setComponent(MOCK_MEDIA_LIBRARY_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the MockMediaLibraryService2 of the service app");
        }

        if (bound) {
            try {
                mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from service app's MockMediaLibraryService2.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    // These methods will run on main thread.
    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's MockMediaLibraryService2.");
            mBinder = IRemoteMediaLibrarySession.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
