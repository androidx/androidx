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
import static androidx.media.test.lib.CommonConstants.MEDIA_SESSION_COMPAT_PROVIDER_SERVICE;
import static androidx.media.test.lib.TestUtils.TIMEOUT_MS;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaSessionCompat} in the service app's
 * MediaSessionCompatProviderService.
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
            fail("Failed to connect to the MediaSessionCompatProviderService.");
        }
        create();
    }

    public void cleanUp() {
        release();
        disconnect();
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

    /**
     * Since we cannot pass VolumeProviderCompat directly,
     * we pass volumeControl, maxVolume, currentVolume instead.
     */
    public void setPlaybackToRemote(int volumeControl, int maxVolume, int currentVolume) {
        try {
            mBinder.setPlaybackToRemote(mSessionTag, volumeControl, maxVolume, currentVolume);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setPlaybackToRemote()");
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

    public void sendSessionEvent(String event, Bundle extras) {
        try {
            mBinder.sendSessionEvent(mSessionTag, event, extras);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendSessionEvent()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to service app's MediaSessionCompatProviderService.
     * Should NOT be called in main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_SESSION_COMPAT);
        intent.setComponent(MEDIA_SESSION_COMPAT_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed binding to the MediaSessionCompatProviderService of the "
                    + "service app");
        }

        if (bound) {
            try {
                mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ex) {
                Log.e(TAG, "InterruptedException while waiting for onServiceConnected.", ex);
            }
        }
        return mBinder != null;
    }

    /**
     * Disconnects from service app's MediaSessionCompatProviderService.
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
            Log.d(TAG, "Connected to service app's MediaSessionCompatProviderService.");
            mBinder = IRemoteMediaSessionCompat.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from the service.");
        }
    }
}
