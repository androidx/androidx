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

import static androidx.media.test.lib.CommonConstants.ACTION_MEDIA_CONTROLLER_COMPAT;
import static androidx.media.test.lib.CommonConstants.KEY_ARGUMENTS;
import static androidx.media.test.lib.CommonConstants.MEDIA_CONTROLLER_COMPAT_PROVIDER_SERVICE;
import static androidx.media.test.lib.TestUtils.TIMEOUT_MS;

import static junit.framework.TestCase.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.mediacompat.testlib.IRemoteMediaControllerCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Represents remote {@link MediaControllerCompat} the client app's
 * MediaControllerCompatProviderService.
 * <p>
 * Users can run {@link MediaControllerCompat} methods remotely with this object.
 */
public class RemoteMediaControllerCompat {
    static final String TAG = "RemoteMediaControllerCompat";

    final String mControllerId;
    final Context mContext;
    final CountDownLatch mCountDownLatch;

    ServiceConnection mServiceConnection;
    IRemoteMediaControllerCompat mBinder;
    TransportControls mTransportControls;

    /**
     * Create a {@link MediaControllerCompat} in the client app.
     * Should NOT be called main thread.
     *
     * @param waitForConnection true if the remote controller needs to wait for the connection,
     *                          false otherwise.
     */
    public RemoteMediaControllerCompat(Context context, MediaSessionCompat.Token token,
            boolean waitForConnection) {
        mContext = context;
        mControllerId = UUID.randomUUID().toString();
        mCountDownLatch = new CountDownLatch(1);
        mServiceConnection = new MyServiceConnection();
        if (!connect()) {
            fail("Failed to connect to the MediaControllerCompatProviderService.");
        }
        create(token, waitForConnection);
    }

    public void cleanUp() {
        disconnect();
    }

    /**
     * Gets {@link TransportControls} for interact with the remote MockPlayer.
     * Users can run MockPlayer methods remotely with this object.
     */
    public TransportControls getTransportControls() {
        return mTransportControls;
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaControllerCompat methods
    ////////////////////////////////////////////////////////////////////////////////

    public void addQueueItem(MediaDescriptionCompat description) {
        try {
            mBinder.addQueueItem(mControllerId, createBundleWithParcelable(description));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call addQueueItem()");
        }
    }

    public void addQueueItem(MediaDescriptionCompat description, int index) {
        try {
            mBinder.addQueueItemWithIndex(
                    mControllerId, createBundleWithParcelable(description), index);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call addQueueItemWithIndex()");
        }
    }

    public void removeQueueItem(MediaDescriptionCompat description) {
        try {
            mBinder.removeQueueItem(mControllerId, createBundleWithParcelable(description));
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call removeQueueItem()");
        }
    }

    public void setVolumeTo(int value, int flags) {
        try {
            mBinder.setVolumeTo(mControllerId, value, flags);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call setVolumeTo()");
        }
    }

    public void adjustVolume(int direction, int flags) {
        try {
            mBinder.adjustVolume(mControllerId, direction, flags);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call adjustVolume()");
        }
    }

    public void sendCommand(String command, Bundle params, ResultReceiver cb) {
        try {
            mBinder.sendCommand(mControllerId, command, params, cb);
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to call sendCommand()");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // MediaControllerCompat.TransportControls methods
    ////////////////////////////////////////////////////////////////////////////////

    public class TransportControls {
        public void prepare() {
            try {
                mBinder.prepare(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call prepare()");
            }
        }

        public void prepareFromMediaId(String mediaId, Bundle extras) {
            try {
                mBinder.prepareFromMediaId(mControllerId, mediaId, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call prepareFromMediaId()");
            }
        }

        public void prepareFromSearch(String query, Bundle extras) {
            try {
                mBinder.prepareFromSearch(mControllerId, query, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call prepareFromSearch()");
            }
        }

        public void prepareFromUri(Uri uri, Bundle extras) {
            try {
                mBinder.prepareFromUri(mControllerId, uri, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call prepareFromUri()");
            }
        }

        public void play() {
            try {
                mBinder.play(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call play()");
            }
        }

        public void playFromMediaId(String mediaId, Bundle extras) {
            try {
                mBinder.playFromMediaId(mControllerId, mediaId, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call playFromMediaId()");
            }
        }

        public void playFromSearch(String query, Bundle extras) {
            try {
                mBinder.playFromSearch(mControllerId, query, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call playFromSearch()");
            }
        }

        public void playFromUri(Uri uri, Bundle extras) {
            try {
                mBinder.playFromUri(mControllerId, uri, extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call playFromUri()");
            }
        }

        public void skipToQueueItem(long id) {
            try {
                mBinder.skipToQueueItem(mControllerId, id);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call skipToQueueItem()");
            }
        }

        public void pause() {
            try {
                mBinder.pause(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call pause()");
            }
        }

        public void stop() {
            try {
                mBinder.stop(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call stop()");
            }
        }

        public void seekTo(long pos) {
            try {
                mBinder.seekTo(mControllerId, pos);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call seekTo()");
            }
        }

        public void fastForward() {
            try {
                mBinder.fastForward(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call fastForward()");
            }
        }

        public void skipToNext() {
            try {
                mBinder.skipToNext(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call skipToNext()");
            }
        }

        public void rewind() {
            try {
                mBinder.rewind(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call rewind()");
            }
        }

        public void skipToPrevious() {
            try {
                mBinder.skipToPrevious(mControllerId);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call skipToPrevious()");
            }
        }

        public void setRating(RatingCompat rating) {
            try {
                mBinder.setRating(mControllerId, createBundleWithParcelable(rating));
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setRating()");
            }
        }

        public void setRating(RatingCompat rating, Bundle extras) {
            try {
                mBinder.setRatingWithExtras(
                        mControllerId, createBundleWithParcelable(rating), extras);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setRatingWithExtras()");
            }
        }

        public void setCaptioningEnabled(boolean enabled) {
            try {
                mBinder.setCaptioningEnabled(mControllerId, enabled);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setCaptioningEnabled()");
            }
        }

        public void setRepeatMode(int repeatMode) {
            try {
                mBinder.setRepeatMode(mControllerId, repeatMode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setRepeatMode()");
            }
        }

        public void setShuffleMode(int shuffleMode) {
            try {
                mBinder.setShuffleMode(mControllerId, shuffleMode);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call setShuffleMode()");
            }
        }

        public void sendCustomAction(PlaybackStateCompat.CustomAction customAction, Bundle args) {
            try {
                mBinder.sendCustomAction(
                        mControllerId, createBundleWithParcelable(customAction), args);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call sendCustomAction()");
            }
        }

        public void sendCustomAction(String action, Bundle args) {
            try {
                mBinder.sendCustomActionWithName(mControllerId, action, args);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to call sendCustomActionWithName()");
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Non-public methods
    ////////////////////////////////////////////////////////////////////////////////

    /**
     * Connects to client app's MediaControllerCompatProviderService.
     * Should NOT be called main thread.
     *
     * @return true if connected successfully, false if failed to connect.
     */
    private boolean connect() {
        final Intent intent = new Intent(ACTION_MEDIA_CONTROLLER_COMPAT);
        intent.setComponent(MEDIA_CONTROLLER_COMPAT_PROVIDER_SERVICE);

        boolean bound = false;
        try {
            bound = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to bind to the MediaControllerCompatProviderService.");
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
     * Disconnects from client app's MediaControllerCompatProviderService.
     */
    private void disconnect() {
        if (mServiceConnection != null) {
            mContext.unbindService(mServiceConnection);
            mServiceConnection = null;
        }
    }

    /**
     * Create a {@link MediaControllerCompat} in the client app.
     * Should be used after successful connection through {@link #connect()}.
     *
     * @param waitForConnection true if this method needs to wait for the connection,
     *                          false otherwise.
     */
    void create(MediaSessionCompat.Token token, boolean waitForConnection) {
        try {
            mBinder.create(mControllerId, createBundleWithParcelable(token), waitForConnection);
            mTransportControls = new TransportControls();
        } catch (RemoteException ex) {
            Log.e(TAG, "Failed to create default controller with given token.");
        }
    }

    private Bundle createBundleWithParcelable(Parcelable parcelable) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_ARGUMENTS, parcelable);
        return bundle;
    }

    class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Connected to client app's MediaControllerCompatProviderService.");
            mBinder = IRemoteMediaControllerCompat.Stub.asInterface(service);
            mCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Disconnected from client app's MediaControllerCompatProviderService.");
        }
    }
}
