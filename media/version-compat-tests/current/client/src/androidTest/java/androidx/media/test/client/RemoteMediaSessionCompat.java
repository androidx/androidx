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

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_SESSION_COMPAT;
import static androidx.media.test.lib.CommonConstants.KEY_METADATA_COMPAT;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYBACK_STATE_COMPAT;
import static androidx.media.test.lib.CommonConstants.KEY_QUEUE;
import static androidx.media.test.lib.CommonConstants.KEY_SESSION_COMPAT_TOKEN;
import static androidx.media.test.lib.CommonConstants.REMOTE_MEDIA_SESSION_COMPAT_SERVICE;
import static androidx.media.test.lib.TestUtils.WAIT_TIME_MS;

import static junit.framework.TestCase.fail;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.mediacompat.testlib.IRemoteMediaSessionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.test.lib.MediaSessionCompatConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaSessionCompat} in the service app's
 * RemoteMediaSessionCompatService.
 * Users can run {@link MediaSessionCompat} methods remotely with this object.
 */
public class RemoteMediaSessionCompat {
    private static final String TAG = "RemoteMediaSessionCompat";

    private final Context mContext;
    private final String mSessionTag;

    private ServiceConnection mServiceConnection;
    private IRemoteMediaSessionCompat mBinder;
    private final CountDownLatch mCountDownLatch;

    /**
     * Create a {@link MediaSessionCompat} in the service app.
     * Should NOT be called in main thread.
     */
    public RemoteMediaSessionCompat(@NonNull String sessionTag, Context context) {
        mSessionTag = sessionTag;
        mContext = context;
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();

        if (!connect()) {
            fail("Failed to connect to the RemoteMediaSessionCompatService.");
        }
        create();
    }

    public void cleanUp() {
        release();
        disconnect();
    }

    /**
     * Run a test-specific custom command in the service app.
     *
     * @param command Pre-defined command code.
     *                One of the constants in {@link MediaSessionCompatConstants}.
     * @param args A {@link Bundle} which contains pre-defined arguments.
     */
    public void runCustomTestCommands(int command, @Nullable Bundle args) {
        try {
            mBinder.runCustomTestCommands(mSessionTag, command, args);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call runCustomTestCommands(). command=" + command
                    + " , args=" + args);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaSessionCompat methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets {@link MediaSessionCompat.Token} from the service app.
     * Should be used after the creation of the session through {@link #create()}.
     *
     * @return A {@link MediaSessionCompat.Token} object if succeeded, {@code null} if failed.
     */
    public MediaSessionCompat.Token getSessionToken() {
        MediaSessionCompat.Token token = null;
        try {
            Bundle bundle = mBinder.getSessionToken(mSessionTag);
            if (bundle != null) {
                bundle.setClassLoader(MediaSessionCompat.class.getClassLoader());
                token = bundle.getParcelable(KEY_SESSION_COMPAT_TOKEN);
            }
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionTag=" + mSessionTag);
        }
        return token;
    }

    public void release() {
        try {
            mBinder.release(mSessionTag);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call release()");
        }
    }

    public void setPlaybackToLocal(int stream) {
        try {
            mBinder.setPlaybackToLocal(mSessionTag, stream);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setPlaybackToLocal()");
        }
    }

    public void setPlaybackState(PlaybackStateCompat state) {
        try {
            mBinder.setPlaybackState(mSessionTag,
                    createBundleWithParcelable(KEY_PLAYBACK_STATE_COMPAT, state));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setPlaybackState()");
        }
    }

    public void setMetadata(MediaMetadataCompat metadata) {
        try {
            mBinder.setMetadata(mSessionTag,
                    createBundleWithParcelable(KEY_METADATA_COMPAT, metadata));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setMetadata()");
        }
    }

    public void setQueue(List<QueueItem> queue) {
        try {
            Bundle bundle = new Bundle();
            ArrayList<QueueItem> queueAsArrayList = new ArrayList<>(queue);
            bundle.putParcelableArrayList(KEY_QUEUE, queueAsArrayList);
            mBinder.setQueue(mSessionTag, bundle);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setQueue()");
        }
    }

    public void setQueueTitle(CharSequence title) {
        try {
            mBinder.setQueueTitle(mSessionTag, title);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setQueueTitle()");
        }
    }

    public void setRepeatMode(int repeatMode) {
        try {
            mBinder.setRepeatMode(mSessionTag, repeatMode);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setRepeatMode()");
        }
    }

    public void setShuffleMode(int shuffleMode) {
        try {
            mBinder.setShuffleMode(mSessionTag, shuffleMode);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setShuffleMode()");
        }
    }

    public void setSessionActivity(PendingIntent intent) {
        try {
            mBinder.setSessionActivity(mSessionTag, intent);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setSessionActivity()");
        }
    }

    public void setFlags(int flags) {
        try {
            mBinder.setFlags(mSessionTag, flags);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setFlags()");
        }
    }

    public void setRatingType(int type) {
        try {
            mBinder.setRatingType(mSessionTag, type);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setRatingType()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to service app's RemoteMediaSessionCompatService.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_SESSION_COMPAT);
        intent.setComponent(REMOTE_MEDIA_SESSION_COMPAT_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the RemoteMediaSessionCompatService of the service app");
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
     * Disconnects from service app's RemoteMediaSessionCompatService.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
        }
        mServiceConnection = null;
    }

    /**
     * Create a {@link MediaSessionCompat} in the service app.
     * Should be used after successful connection through {@link #connect}.
     */
    private void create() {
        try {
            mBinder.create(mSessionTag);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to get session token. sessionTag=" + mSessionTag);
        }
    }

    private Bundle createBundleWithParcelable(String key, Parcelable value) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(key, value);
        return bundle;
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to service app's RemoteMediaSessionCompatService.");
            mBinder = IRemoteMediaSessionCompat.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
